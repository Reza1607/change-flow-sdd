package com.changeflow.identity;

import com.changeflow.support.*;
import com.changeflow.support.ConcurrencyTestConfiguration.*;
import com.changeflow.identity.domain.ActorId;
import com.changeflow.identity.infrastructure.AccessProvisioner;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import static org.junit.jupiter.api.Assertions.*;

@Import(ConcurrencyTestConfiguration.class)
class InFlightRevocationIT extends DraftApiSupport {
    @Autowired Controls controls;
    @Autowired AccessProvisioner provisioner;
    enum Operation { Edit, Submit, Approve, Read }
    private HttpResponse<String> perform(Operation op,Account actor,UUID id) throws Exception {
        return switch(op) {
            case Edit->mutate(actor,"PUT",BASE+"/"+id,"{\"expectedVersion\":0,\"title\":\"edited\",\"description\":\"d\",\"reason\":\"r\"}");
            case Submit->mutate(actor,"POST",BASE+"/"+id+"/submit","{\"expectedVersion\":0}");
            case Approve->mutate(actor,"POST",BASE+"/"+id+"/approve","{\"expectedVersion\":0}");
            case Read->send(request(BASE+"/"+id,actor));
        };
    }
    private void revoke(Operation op,Account actor) {provisioner.change(new ActorId(actor.id()),0,true,op==Operation.Approve,op!=Operation.Approve);}
    private UUID fixture(Operation op,Account actor,Account other) {return seed(op==Operation.Approve?other:actor,op==Operation.Approve?"PendingReview":"Draft","t","d","r",0);}
    @ParameterizedTest @EnumSource(Operation.class)
    void committedRevocationBeforeFinalGuardRejectsInFlightAndLaterOperation(Operation op) throws Exception {
        var other=account(true,false);
        try(var pool=Executors.newFixedThreadPool(2)) {
            for(int iteration=0;iteration<20;iteration++) {
                var actor=account(true,true);var id=fixture(op,actor,other);var before=snapshot(id);
                var gate=new GuardGate(new ActorId(actor.id()),id,false);controls.guard=gate;
                HttpResponse<String> response;
                try {
                    var operation=pool.submit(()->perform(op,actor,id));gate.awaitEntered();
                    assertTrue(gate.cachedAccount,"JPA contains the old account while role source changes");
                    revoke(op,actor);assertNotEquals(gate.pid,controls.writerPid,"independent connections");assertNotSame(gate.session,controls.writerSession,"independent EntityManagers");
                    gate.release.countDown();response=operation.get(30,TimeUnit.SECONDS);
                } finally {gate.release.countDown();controls.guard=null;}
                if(op==Operation.Read) problem(response,404,"NOT_FOUND");else problem(response,403,"FORBIDDEN");
                assertEquals(before,snapshot(id));assertEquals(0,auditCount(id));
                assertLaterRejectedAndOtherRoleWorks(op,actor,other,id);
                System.out.println("US5 revocation-first operation="+op+" iteration="+(iteration+1)+" effect=0 audit=0");
            }
        }
    }
    @ParameterizedTest @EnumSource(Operation.class)
    void earlierSharedGuardMakesRevocationWaitUntilTheAcceptedOperationCommits(Operation op) throws Exception {
        var other=account(true,false);
        try(var pool=Executors.newFixedThreadPool(2)) {
            for(int iteration=0;iteration<20;iteration++) {
                var actor=account(true,true);var id=fixture(op,actor,other);var gate=new GuardGate(new ActorId(actor.id()),id,true);
                controls.guard=gate;controls.writerPid=0;controls.writerObservedRow=null;
                try {
                    var operation=pool.submit(()->perform(op,actor,id));gate.awaitEntered();
                    var revocation=pool.submit(()->revoke(op,actor));awaitWriterLock(gate.pid);assertNotSame(gate.session,controls.writerSession);
                    assertFalse(revocation.isDone(),"writer cannot commit through an active shared guard");
                    gate.release.countDown();assertEquals(200,operation.get(30,TimeUnit.SECONDS).statusCode());revocation.get(30,TimeUnit.SECONDS);
                } finally {gate.release.countDown();controls.guard=null;}
                var expectedStatus=switch(op) {case Edit,Read->"Draft";case Submit->"PendingReview";case Approve->"Approved";};
                assertNotNull(controls.writerObservedRow);assertEquals(expectedStatus,controls.writerObservedRow.get("status"));
                assertEquals(op==Operation.Read?0L:1L,controls.writerObservedRow.get("version"));
                var committed=snapshot(id);
                assertEquals(expectedStatus,committed.get("status"));assertEquals(op==Operation.Read?0L:1L,committed.get("version"));
                assertEquals(op==Operation.Approve?other.id():actor.id(),committed.get("requester_id"));
                assertEquals(op==Operation.Edit?"edited":"t",committed.get("title"));assertEquals("d",committed.get("description"));assertEquals("r",committed.get("reason"));
                assertEquals(op==Operation.Submit||op==Operation.Approve?1:0,auditCount(id));
                if(op==Operation.Submit||op==Operation.Approve) {
                    var audit=jdbc.queryForMap("select * from status_history where request_id=?",id);
                    assertEquals(actor.id(),audit.get("actor_id"));assertEquals(1L,audit.get("resulting_version"));assertEquals(expectedStatus,audit.get("to_status"));
                }
                assertLaterRejectedAndOtherRoleWorks(op,actor,other,id);
                System.out.println("US5 guard-first operation="+op+" iteration="+(iteration+1)+" writerObservedCommittedOperation=true");
            }
        }
    }
    private void awaitWriterLock(int guardPid) {
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(15);boolean blocked=false;
        while(System.nanoTime()<deadline) {
            int pid=controls.writerPid;
            if(pid!=0 && Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from pg_stat_activity where pid=? and wait_event_type='Lock' and ?=any(pg_blocking_pids(pid)))",Boolean.class,pid,guardPid))) {blocked=true;break;}
            Thread.onSpinWait();
        }
        assertTrue(blocked,"the actual role writer must be blocked by this operation's PostgreSQL backend");
        assertNotEquals(guardPid,controls.writerPid);
    }
    private void assertLaterRejectedAndOtherRoleWorks(Operation op,Account actor,Account other,UUID id) throws Exception {
        var before=snapshot(id);var history=jdbc.queryForList("select * from status_history where request_id=?",id);
        var later=perform(op,actor,id);assertTrue(later.statusCode()==403||later.statusCode()==404,"later operation must lose its required role/access");
        assertEquals(before,snapshot(id));assertEquals(history,jdbc.queryForList("select * from status_history where request_id=?",id));
        assertEquals(1L,jdbc.queryForObject("select access_version from identity_account where actor_id=?",Long.class,actor.id()));
        if(op==Operation.Approve) {
            var draft=create(actor,"{}");assertEquals(actor.id().toString(),draft.get("requesterId").asString());
        } else {
            var pending=seed(other,"PendingReview","t","d","r",0);
            assertEquals(200,mutate(actor,"POST",BASE+"/"+pending+"/approve","{\"expectedVersion\":0}").statusCode());assertEquals(1,auditCount(pending));
        }
    }
}
