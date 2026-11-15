package com.changeflow.changerequest.api;

import com.changeflow.support.DraftApiSupport;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@org.springframework.context.annotation.Import(DraftFlowIT.FixedTime.class)
@org.junit.jupiter.api.extension.ExtendWith(org.springframework.boot.test.system.OutputCaptureExtension.class)
class DraftFlowIT extends DraftApiSupport {
    @org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods=false)
    static class FixedTime {
        @org.springframework.context.annotation.Bean @org.springframework.context.annotation.Primary
        java.time.Clock us1Clock() {return java.time.Clock.fixed(java.time.Instant.parse("2026-11-15T00:00:00Z"),java.time.ZoneOffset.UTC);}
    }
    @org.springframework.beans.factory.annotation.Autowired com.changeflow.identity.infrastructure.AccessProvisioner provisioner;
    @Test void createsIncompleteDraftThenReplacesTextAndSubmitsWithOneAudit() throws Exception {
        var actor=account(true,false);
        var r=mutate(actor,"POST",BASE,"{}");assertEquals(201,r.statusCode());
        var draft=json(r.body());var id=UUID.fromString(draft.get("id").asString());
        assertEquals(BASE+"/"+id,r.headers().firstValue("Location").orElseThrow());
        assertEquals(actor.id().toString(),draft.get("requesterId").asString());
        assertEquals("Draft",draft.get("status").asString());assertEquals(0,draft.get("version").asLong());
        assertTrue(draft.get("title").isNull());assertTrue(draft.get("description").isNull());assertTrue(draft.get("reason").isNull());
        assertEquals(0,auditCount(id));
        var edit=mutate(actor,"PUT",BASE+"/"+id,"{\"expectedVersion\":0,\"title\":\"عنوان\",\"description\":\"شرح\",\"reason\":\"دلیل\"}");
        assertEquals(200,edit.statusCode());assertEquals(1,json(edit.body()).get("version").asLong());assertEquals(0,auditCount(id));
        var submit=mutate(actor,"POST",BASE+"/"+id+"/submit","{\"expectedVersion\":1}");
        assertEquals(200,submit.statusCode());assertEquals("PendingReview",json(submit.body()).get("status").asString());
        assertEquals(2,json(submit.body()).get("version").asLong());assertEquals(1,auditCount(id));
        var audit=jdbc.queryForMap("select * from status_history where request_id=?",id);
        assertEquals(actor.id(),audit.get("actor_id"));assertEquals("Draft",audit.get("from_status"));
        assertEquals("PendingReview",audit.get("to_status"));assertEquals("",audit.get("explanation"));assertEquals(2L,audit.get("resulting_version"));
        assertNotNull(audit.get("occurred_at"));
        assertEquals(java.time.Instant.parse("2026-11-15T00:00:00Z"),jdbc.queryForObject("select occurred_at from status_history where request_id=?",
            (row,n)->row.getObject(1,java.time.OffsetDateTime.class).toInstant(),id));
        assertFalse(submit.body().contains("password"));assertFalse(submit.body().contains("username"));
    }
    @Test void everyInvalidSubmitLeavesContentStateVersionAndAuditUnchanged() throws Exception {
        var actor=account(true,false);
        for(var field:List.of("title","description","reason")) for(String invalid:new String[]{null,""," \t\n","\u00a0\u2007\u202f\u3000"}) {
            var content=new LinkedHashMap<String,Object>();content.put("title","title");content.put("description","description");content.put("reason","reason");content.put(field,invalid);
            var draft=create(actor,encode(content));var id=UUID.fromString(draft.get("id").asString());var before=snapshot(id);
            var r=mutate(actor,"POST",BASE+"/"+id+"/submit","{\"expectedVersion\":0}");
            problem(r,422,"VALIDATION_FAILED");assertEquals(field,json(r.body()).get("errors").get(0).get("field").asString());
            assertEquals(before,snapshot(id));assertEquals(0,auditCount(id));
        }
        var draft=create(actor,"{}");var id=UUID.fromString(draft.get("id").asString());var before=snapshot(id);
        problem(mutate(actor,"POST",BASE+"/"+id+"/submit","{\"expectedVersion\":0}"),422,"VALIDATION_FAILED");
        assertEquals(before,snapshot(id));assertEquals(0,auditCount(id));
    }
    @Test void duplicateTitlesAreIndependentAndSuccessfulEqualEditIncrementsVersion() throws Exception {
        var actor=account(true,false);String body="{\"title\":\"same\",\"description\":\"d\",\"reason\":\"r\"}";
        var one=create(actor,body);var two=create(actor,body);assertNotEquals(one.get("id"),two.get("id"));
        var id=UUID.fromString(one.get("id").asString());
        var equal=mutate(actor,"PUT",BASE+"/"+id,"{\"expectedVersion\":0,\"title\":\"same\",\"description\":\"d\",\"reason\":\"r\"}");
        assertEquals(200,equal.statusCode());assertEquals(1,json(equal.body()).get("version").asLong());assertEquals(1L,snapshot(id).get("version"));
        assertEquals(200,mutate(actor,"POST",BASE+"/"+id+"/submit","{\"expectedVersion\":1}").statusCode());
        assertEquals(200,mutate(actor,"POST",BASE+"/"+two.get("id").asString()+"/submit","{\"expectedVersion\":0}").statusCode());
        assertEquals(1,auditCount(id));
    }
    @Test void rejectsUnknownFieldsBadBodiesAndMissingOrNegativeVersionsWithoutWrites() throws Exception {
        var actor=account(true,false);var draft=create(actor,"{}");var id=UUID.fromString(draft.get("id").asString());var before=snapshot(id);
        for(var body:List.of("{\"actorId\":\"forged\"}","{\"requesterId\":null}","{\"role\":\"Reviewer\"}","{\"status\":\"Approved\"}","{\"title\":42}","{", "null"))
            problem(mutate(actor,"POST",BASE,body),400,"BAD_REQUEST");
        problem(mutate(actor,"POST",BASE,null),400,"BAD_REQUEST");
        for(var body:List.of("{}","{\"expectedVersion\":null}","{\"expectedVersion\":-1}","{\"expectedVersion\":\"0\"}","{\"expectedVersion\":0.5}","{\"expectedVersion\":0,\"explanation\":null}","{\"expectedVersion\":0,\"actorId\":\"forged\"}"))
            problem(mutate(actor,"POST",BASE+"/"+id+"/submit",body),400,"BAD_REQUEST");
        problem(mutate(actor,"PUT",BASE+"/"+id,"{\"expectedVersion\":0}"),400,"BAD_REQUEST");
        assertEquals(before,snapshot(id));assertEquals(0,auditCount(id));
    }
    @Test void enforcesOwnershipCurrentRoleStateAndVersionInContractOrder() throws Exception {
        var owner=account(true,false);var other=account(true,true);var reviewer=account(false,true);var noRole=account(false,false);
        var id=seed(owner,"Draft","t","d","r",3);var before=snapshot(id);
        problem(mutate(other,"PUT",BASE+"/"+id,"{\"expectedVersion\":3,\"title\":null,\"description\":null,\"reason\":null}"),404,"NOT_FOUND");
        problem(mutate(owner,"POST",BASE+"/"+id+"/submit","{\"expectedVersion\":2}"),409,"CONCURRENT_MODIFICATION");
        problem(mutate(noRole,"POST",BASE,"{}"),403,"FORBIDDEN");
        problem(mutate(reviewer,"POST",BASE,"{}"),403,"FORBIDDEN");
        provisioner.change(new com.changeflow.identity.domain.ActorId(owner.id()),0,true,false,false);
        problem(mutate(owner,"POST",BASE+"/"+id+"/submit","{\"expectedVersion\":3}"),403,"FORBIDDEN");
        assertEquals(before,snapshot(id));assertEquals(0,auditCount(id));
        for(var status:List.of("PendingReview","Approved")) {
            var own=seed(other,status,"t","d","r",4);var unchanged=snapshot(own);
            problem(mutate(other,"PUT",BASE+"/"+own,"{\"expectedVersion\":0,\"title\":null,\"description\":null,\"reason\":null}"),409,"INVALID_TRANSITION");
            problem(mutate(other,"POST",BASE+"/"+own+"/submit","{\"expectedVersion\":0}"),409,"INVALID_TRANSITION");
            assertEquals(unchanged,snapshot(own));assertEquals(0,auditCount(own));
        }
        var pending=seed(owner,"PendingReview","t","d","r",1);
        problem(mutate(other,"POST",BASE+"/"+pending+"/submit","{\"expectedVersion\":1}"),403,"FORBIDDEN");
    }
    @Test void nullableReplacementCanClearDraftAndAuditFailureRollsBackSubmit(org.springframework.boot.test.system.CapturedOutput output) throws Exception {
        var actor=account(true,false);var id=seed(actor,"Draft","t","d","r",0);
        var cleared=mutate(actor,"PUT",BASE+"/"+id,"{\"expectedVersion\":0,\"title\":null,\"description\":null,\"reason\":null}");
        assertEquals(200,cleared.statusCode());assertTrue(json(cleared.body()).get("title").isNull());assertEquals(1L,snapshot(id).get("version"));assertEquals(0,auditCount(id));
        var valid=seed(actor,"Draft","t","d","r",0);var before=snapshot(valid);
        jdbc.execute("alter table status_history add constraint reject_us1_audit check (request_id <> '"+valid+"')");
        var privateExplanation="PRIVATE_AUDIT_"+UUID.randomUUID();
        try {
            problem(mutate(actor,"POST",BASE+"/"+valid+"/submit",encode(Map.of("expectedVersion",0,"explanation",privateExplanation))),500,"INTERNAL_ERROR");
            assertEquals(before,snapshot(valid));assertEquals(0,auditCount(valid));
            assertFalse(output.getAll().contains(privateExplanation),"failed Audit must not leak explanation to logs");
        } finally {jdbc.execute("alter table status_history drop constraint reject_us1_audit");}
    }
}
