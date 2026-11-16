package com.changeflow.changerequest.api;

import com.changeflow.support.HistoryApiSupport;
import com.changeflow.identity.domain.ActorId;
import com.changeflow.identity.infrastructure.AccessProvisioner;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import static org.junit.jupiter.api.Assertions.*;

class DeleteDraftIT extends HistoryApiSupport {
    @Autowired AccessProvisioner provisioner;
    @Test void ownerDeletesDraftWithoutAuditAndRepeatedDeleteAndReadAreNotFound() throws Exception {
        var owner=account(true,false);var id=seed(owner,"Draft","t",null,null,0);
        var response=mutate(owner,"DELETE",BASE+"/"+id+"?expectedVersion=0",null);
        assertEquals(204,response.statusCode());assertEquals("",response.body());assertEquals("no-store",response.headers().firstValue("Cache-Control").orElseThrow());
        assertEquals(0L,jdbc.queryForObject("select count(*) from change_request where id=?",Long.class,id));assertEquals(0,auditCount(id));
        problem(send(request(BASE+"/"+id,owner)),404,"NOT_FOUND");
        problem(send(request(BASE+"/"+id+"/history",owner)),404,"NOT_FOUND");
        problem(mutate(owner,"DELETE",BASE+"/"+id+"?expectedVersion=0",null),404,"NOT_FOUND");
    }
    @Test void missingStaleMalformedAndNegativeVersionCannotDelete() throws Exception {
        var owner=account(true,false);var id=seed(owner,"Draft","t","d","r",2);var before=snapshot(id);
        for(String query:List.of("","?expectedVersion=-1","?expectedVersion=abc","?expectedVersion=9223372036854775808"))
            problem(mutate(owner,"DELETE",BASE+"/"+id+query,null),400,"BAD_REQUEST");
        problem(mutate(owner,"DELETE",BASE+"/"+id+"?expectedVersion=1",null),409,"CONCURRENT_MODIFICATION");
        assertEquals(before,snapshot(id));assertEquals(0,auditCount(id));
    }
    @Test void visibilityRoleAndOwnershipPrecedeStateAndVersion() throws Exception {
        var owner=account(true,true);var other=account(true,false);var reviewer=account(false,true);var dual=account(true,true);var none=account(false,false);
        var draft=seed(owner,"Draft","PRIVATE_DELETE","d","r",0);var pending=seed(owner,"PendingReview","t","d","r",0);
        for(var actor:List.of(other,reviewer,dual)) problem(mutate(actor,"DELETE",BASE+"/"+draft+"?expectedVersion=0",null),404,"NOT_FOUND");
        for(var actor:List.of(reviewer,dual)) problem(mutate(actor,"DELETE",BASE+"/"+pending+"?expectedVersion=0",null),403,"FORBIDDEN");
        problem(mutate(none,"DELETE",BASE+"/"+draft+"?expectedVersion=0",null),403,"FORBIDDEN");
        provisioner.change(new ActorId(owner.id()),0,true,false,true);
        problem(mutate(owner,"DELETE",BASE+"/"+draft+"?expectedVersion=0",null),404,"NOT_FOUND");
        problem(mutate(owner,"DELETE",BASE+"/"+pending+"?expectedVersion=0",null),403,"FORBIDDEN");
        assertEquals("Draft",snapshot(draft).get("status"));assertEquals("PendingReview",snapshot(pending).get("status"));assertEquals(0,auditCount(draft));assertEquals(0,auditCount(pending));
    }
    @Test void everyNonDraftStateIncludingApprovedStaysUnchangedWithItsAudit() throws Exception {
        var owner=account(true,false);var reviewer=account(false,true);
        for(String status:List.of("PendingReview","ChangesRequested","Approved")) {
            var id=seed(owner,status,"t","d","r",1);
            history(id,status.equals("PendingReview")?owner:reviewer,status.equals("PendingReview")?"Draft":"PendingReview",status,1,"existing explanation");
            var before=snapshot(id);var audit=jdbc.queryForList("select * from status_history where request_id=?",id);
            for(long version:new long[]{0,1}) problem(mutate(owner,"DELETE",BASE+"/"+id+"?expectedVersion="+version,null),409,"INVALID_TRANSITION");
            assertEquals(before,snapshot(id));assertEquals(audit,jdbc.queryForList("select * from status_history where request_id=?",id));
        }
    }
    @Test void hiddenAndAbsentDeleteReturnIdenticalProblemAndCsrfIsRequired() throws Exception {
        var owner=account(true,false);var other=account(true,false);var id=UUID.randomUUID();
        var missing=mutate(other,"DELETE",BASE+"/"+id+"?expectedVersion=0",null);problem(missing,404,"NOT_FOUND");
        jdbc.update("insert into change_request(id,requester_id,status,version) values (?,?,'Draft',0)",id,owner.id());
        var hidden=mutate(other,"DELETE",BASE+"/"+id+"?expectedVersion=0",null);problem(hidden,404,"NOT_FOUND");assertEquals(missing.body(),hidden.body());
        problem(send(request(BASE+"/"+id+"?expectedVersion=0",owner).DELETE()),403,"CSRF_INVALID");
        assertEquals(0L,snapshot(id).get("version"));assertEquals(0,auditCount(id));
    }
}
