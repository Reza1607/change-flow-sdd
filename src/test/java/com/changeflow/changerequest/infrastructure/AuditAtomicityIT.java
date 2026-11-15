package com.changeflow.changerequest.infrastructure;

import com.changeflow.changerequest.application.AuditTrail;
import com.changeflow.support.DraftApiSupport;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;

@Import(AuditAtomicityIT.FaultConfiguration.class)
class AuditAtomicityIT extends DraftApiSupport {
    private static final Instant NOW=Instant.parse("2026-11-15T10:00:00.123456Z");
    static class AuditProbe {
        volatile UUID requestId;
        volatile Map<String,Object> rowBeforeInsert;
    }
    @TestConfiguration(proxyBeanMethods=false)
    static class FaultConfiguration {
        @Bean @Primary Clock auditClock() {return Clock.fixed(NOW,ZoneOffset.UTC);}
        @Bean AuditProbe auditProbe() {return new AuditProbe();}
        @Bean @Primary AuditTrail observedAudit(JpaAuditTrail actual,JdbcTemplate jdbc,AuditProbe probe) {
            return entry->{
                if(entry.requestId().equals(probe.requestId))
                    probe.rowBeforeInsert=jdbc.queryForMap("select * from change_request where id=?",entry.requestId());
                actual.append(entry);
            };
        }
    }
    @Autowired AuditProbe probe;
    private void sawUpdatedRowBeforeAudit(long version) {
        assertNotNull(probe.rowBeforeInsert);
        assertEquals("Approved",probe.rowBeforeInsert.get("status"));assertEquals(version,probe.rowBeforeInsert.get("version"));
    }
    @Test void successfulApprovalCommitsExactlyOneCompleteAuditWithServerUtcAndEmptyDefaultExplanation() throws Exception {
        var owner=account(true,false);var reviewer=account(false,true);var id=seed(owner,"PendingReview","t","d","r",4);
        probe.requestId=id;probe.rowBeforeInsert=null;
        var response=mutate(reviewer,"POST",BASE+"/"+id+"/approve","{\"expectedVersion\":4}");assertEquals(200,response.statusCode());
        assertEquals(5,json(response.body()).get("version").asLong());assertEquals("Approved",snapshot(id).get("status"));sawUpdatedRowBeforeAudit(5L);
        assertEquals(1,auditCount(id));var row=jdbc.queryForMap("select * from status_history where request_id=?",id);
        assertNotNull(row.get("id"));assertEquals(id,row.get("request_id"));assertEquals(reviewer.id(),row.get("actor_id"));
        assertEquals("PendingReview",row.get("from_status"));assertEquals("Approved",row.get("to_status"));assertEquals("",row.get("explanation"));assertEquals(5L,row.get("resulting_version"));
        assertEquals(NOW,jdbc.queryForObject("select occurred_at from status_history where request_id=?",(r,n)->r.getObject(1,OffsetDateTime.class).toInstant(),id));
    }
    @Test void rejectedAuditInsertRollsBackAlreadyFlushedStatusAndVersion() throws Exception {
        var owner=account(true,false);var reviewer=account(false,true);var id=seed(owner,"PendingReview","t","d","r",4);var before=snapshot(id);
        probe.requestId=id;probe.rowBeforeInsert=null;
        jdbc.execute("alter table status_history add constraint reject_us2_audit check (request_id <> '"+id+"')");
        try {
            problem(mutate(reviewer,"POST",BASE+"/"+id+"/approve","{\"expectedVersion\":4,\"explanation\":\"private decision\"}"),500,"INTERNAL_ERROR");
            sawUpdatedRowBeforeAudit(5L);assertEquals(before,snapshot(id));assertEquals(0,auditCount(id));
        } finally {jdbc.execute("alter table status_history drop constraint reject_us2_audit");}
    }
    @Test void auditFailureDeferredUntilCommitCannotReturnASuccessReceipt() throws Exception {
        var owner=account(true,false);var reviewer=account(false,true);var id=seed(owner,"PendingReview","t","d","r",4);var before=snapshot(id);
        probe.requestId=id;probe.rowBeforeInsert=null;
        jdbc.execute("""
            create function reject_us2_commit_fn() returns trigger language plpgsql as $$
            begin
                raise exception 'injected audit commit failure' using errcode='23514';
                return new;
            end $$
            """);
        try {
            jdbc.execute("create constraint trigger reject_us2_commit after insert on status_history deferrable initially deferred for each row when (new.request_id = '"+id+"'::uuid) execute function reject_us2_commit_fn()");
            try {
                problem(mutate(reviewer,"POST",BASE+"/"+id+"/approve","{\"expectedVersion\":4}"),500,"INTERNAL_ERROR");
                sawUpdatedRowBeforeAudit(5L);assertEquals(before,snapshot(id));assertEquals(0,auditCount(id));
            } finally {jdbc.execute("drop trigger reject_us2_commit on status_history");}
        } finally {jdbc.execute("drop function reject_us2_commit_fn()");}
    }
}
