package com.changeflow.changerequest.api;

import com.changeflow.support.*;
import com.changeflow.support.ConcurrencyTestConfiguration.*;
import com.changeflow.identity.domain.ActorId;
import com.changeflow.identity.infrastructure.AccessProvisioner;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import static org.junit.jupiter.api.Assertions.*;

@Import(ConcurrencyTestConfiguration.class)
class CorrectionSafetyIT extends DraftApiSupport {
    @Autowired Controls controls;
    @Autowired AccessProvisioner provisioner;
    @Test void requestChangesChecksVersionInputAndRoleWithoutEffects() throws Exception {
        var owner=account(true,false);var reviewer=account(false,true);var none=account(false,false);var id=seed(owner,"PendingReview","t","d","r",2);var before=snapshot(id);
        problem(mutate(reviewer,"POST",BASE+"/"+id+"/request-changes","{\"expectedVersion\":1,\"explanation\":\"fix\"}"),409,"CONCURRENT_MODIFICATION");
        for(String body:List.of("{}","{\"expectedVersion\":2,\"explanation\":7}","{\"expectedVersion\":2,\"actorId\":\"forged\"}"))
            problem(mutate(reviewer,"POST",BASE+"/"+id+"/request-changes",body),400,"BAD_REQUEST");
        problem(mutate(none,"POST",BASE+"/"+id+"/request-changes","{\"expectedVersion\":2,\"explanation\":\"fix\"}"),403,"FORBIDDEN");
        assertEquals(before,snapshot(id));assertEquals(0,auditCount(id));
    }
    @Test void rejectedAuditRollsBackRequestChangesStatusAndVersion() throws Exception {
        var owner=account(true,false);var reviewer=account(false,true);var id=seed(owner,"PendingReview","t","d","r",0);var before=snapshot(id);
        jdbc.execute("alter table status_history add constraint reject_us3_audit check (request_id <> '"+id+"')");
        try {
            problem(mutate(reviewer,"POST",BASE+"/"+id+"/request-changes","{\"expectedVersion\":0,\"explanation\":\"fix\"}"),500,"INTERNAL_ERROR");
            assertEquals(before,snapshot(id));assertEquals(0,auditCount(id));
        } finally {jdbc.execute("alter table status_history drop constraint reject_us3_audit");}
    }
    @Test void committedRevocationBeforeRequestChangesGuardRejectsAlreadyStartedOperation() throws Exception {
        var owner=account(true,false);var reviewer=account(true,true);var id=seed(owner,"PendingReview","t","d","r",0);var before=snapshot(id);
        var gate=new GuardGate(new ActorId(reviewer.id()),id,false);controls.guard=gate;
        try(var pool=Executors.newSingleThreadExecutor()) {
            try {
                var operation=pool.submit(()->mutate(reviewer,"POST",BASE+"/"+id+"/request-changes","{\"expectedVersion\":0,\"explanation\":\"fix\"}"));
                gate.awaitEntered();provisioner.change(new ActorId(reviewer.id()),0,true,true,false);gate.release.countDown();
                problem(operation.get(30,TimeUnit.SECONDS),403,"FORBIDDEN");assertEquals(before,snapshot(id));assertEquals(0,auditCount(id));
                assertEquals(201,mutate(reviewer,"POST",BASE,"{}").statusCode());
            } finally {gate.release.countDown();controls.guard=null;}
        }
    }
    @Test void changesRequestedOwnerNeedsCurrentRequesterForEditAndSubmitAndNoDeleteIsAdded() throws Exception {
        var owner=account(true,true);var other=account(true,false);var id=seed(owner,"ChangesRequested","t","d","r",0);var before=snapshot(id);
        var edit="{\"expectedVersion\":0,\"title\":\"fixed\",\"description\":\"d\",\"reason\":\"r\"}";
        problem(mutate(other,"PUT",BASE+"/"+id,edit),404,"NOT_FOUND");
        provisioner.change(new ActorId(owner.id()),0,true,false,true);
        problem(mutate(owner,"PUT",BASE+"/"+id,edit),404,"NOT_FOUND");
        problem(mutate(owner,"POST",BASE+"/"+id+"/submit","{\"expectedVersion\":0}"),404,"NOT_FOUND");
        assertEquals(before,snapshot(id));assertEquals(0,auditCount(id));
    }
}
