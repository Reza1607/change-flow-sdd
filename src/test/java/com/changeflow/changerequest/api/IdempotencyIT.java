package com.changeflow.changerequest.api;

import com.changeflow.support.DraftApiSupport;
import com.changeflow.identity.domain.ActorId;
import com.changeflow.identity.infrastructure.AccessProvisioner;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import static org.junit.jupiter.api.Assertions.*;

class IdempotencyIT extends DraftApiSupport {
    @Autowired AccessProvisioner provisioner;
    @Test void submitRepeatChecksCurrentRoleOwnerAndStateBeforeVersionWithoutNewEffects() throws Exception {
        var owner=account(true,false);var outsider=account(true,false);var reviewer=account(false,true);
        var id=seed(owner,"Draft","private title","d","r",0);
        assertEquals(200,mutate(owner,"POST",BASE+"/"+id+"/submit","{\"expectedVersion\":0}").statusCode());
        var before=snapshot(id);var history=jdbc.queryForList("select * from status_history where request_id=?",id);
        for(long version:new long[]{0,1}) {
            var response=mutate(owner,"POST",BASE+"/"+id+"/submit",encode(Map.of("expectedVersion",version)));
            problem(response,409,"INVALID_TRANSITION");noDto(response.body());
            problem(mutate(outsider,"POST",BASE+"/"+id+"/submit",encode(Map.of("expectedVersion",version))),404,"NOT_FOUND");
            problem(mutate(reviewer,"POST",BASE+"/"+id+"/submit",encode(Map.of("expectedVersion",version))),403,"FORBIDDEN");
        }
        provisioner.change(new ActorId(owner.id()),0,true,false,false);
        problem(mutate(owner,"POST",BASE+"/"+id+"/submit","{\"expectedVersion\":1}"),403,"FORBIDDEN");
        assertEquals(before,snapshot(id));assertEquals(history,jdbc.queryForList("select * from status_history where request_id=?",id));
    }
    @Test void approveRepeatRecognizesOnlySuccessfulActorAndDoesNotSurviveRoleRevocation() throws Exception {
        var owner=account(true,false);var actor=account(false,true);var other=account(false,true);
        var id=seed(owner,"PendingReview","private title","d","r",0);
        assertEquals(200,mutate(actor,"POST",BASE+"/"+id+"/approve","{\"expectedVersion\":0}").statusCode());
        var before=snapshot(id);var history=jdbc.queryForList("select * from status_history where request_id=?",id);
        for(long version:new long[]{0,1}) {
            var response=mutate(actor,"POST",BASE+"/"+id+"/approve",encode(Map.of("expectedVersion",version)));
            problem(response,409,"INVALID_TRANSITION");noDto(response.body());
            problem(mutate(other,"POST",BASE+"/"+id+"/approve",encode(Map.of("expectedVersion",version))),404,"NOT_FOUND");
        }
        problem(send(request(BASE+"/"+id,actor)),404,"NOT_FOUND");
        provisioner.change(new ActorId(actor.id()),0,true,false,false);
        for(long version:new long[]{0,1}) problem(mutate(actor,"POST",BASE+"/"+id+"/approve",encode(Map.of("expectedVersion",version))),403,"FORBIDDEN");
        assertEquals(before,snapshot(id));assertEquals(history,jdbc.queryForList("select * from status_history where request_id=?",id));
    }
    @Test void existingRequestChangesFixtureKeepsItsAuditAcrossSubmitAndRepeats() throws Exception {
        // Fixture only: no RequestChanges HTTP endpoint, DTO or use case is added in US5.
        var owner=account(true,false);var reviewer=account(false,true);var id=seed(owner,"ChangesRequested","t","d","r",1);
        jdbc.update("insert into status_history(id,request_id,actor_id,occurred_at,from_status,to_status,explanation,resulting_version) values (?,?,?,?,'PendingReview','ChangesRequested',?,1)",
            UUID.randomUUID(),id,reviewer.id(),java.time.OffsetDateTime.parse("2026-11-15T00:00:00Z"),"clarify impact");
        var previous=jdbc.queryForMap("select * from status_history where request_id=?",id);
        assertEquals(200,mutate(owner,"POST",BASE+"/"+id+"/submit","{\"expectedVersion\":1}").statusCode());
        assertEquals(2,auditCount(id));assertEquals(previous,jdbc.queryForMap("select * from status_history where request_id=? and resulting_version=1",id));
        var before=snapshot(id);var history=jdbc.queryForList("select * from status_history where request_id=? order by resulting_version",id);
        for(long version:new long[]{1,2}) problem(mutate(owner,"POST",BASE+"/"+id+"/submit",encode(Map.of("expectedVersion",version))),409,"INVALID_TRANSITION");
        assertEquals(before,snapshot(id));assertEquals(history,jdbc.queryForList("select * from status_history where request_id=? order by resulting_version",id));
    }
    private void noDto(String body) {
        assertFalse(body.contains("private title"));assertNull(json(body).get("id"));assertNull(json(body).get("requesterId"));
        assertNull(json(body).get("version"));assertNull(json(body).get("explanation"));
    }
}
