package com.changeflow.changerequest.api;

import com.changeflow.support.HistoryApiSupport;
import com.changeflow.identity.domain.ActorId;
import com.changeflow.identity.infrastructure.AccessProvisioner;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import static org.junit.jupiter.api.Assertions.*;

class HistoryVisibilityIT extends HistoryApiSupport {
    @Autowired AccessProvisioner provisioner;
    @Test void invisibleParentAndAbsentParentHaveIdenticalNoLeakErrors() throws Exception {
        var owner=account(true,false);var other=account(true,false);var reviewer=account(false,true);var none=account(false,false);
        for(String status:List.of("Draft","Approved","ChangesRequested")) {
            var id=UUID.randomUUID();
            // Compare the same URL before/after the invisible parent exists, including RFC 9457 instance.
            var absentByActor=Map.of(other.id(),send(request(BASE+"/"+id+"/history",other)),
                reviewer.id(),send(request(BASE+"/"+id+"/history",reviewer)));
            jdbc.update("insert into change_request(id,requester_id,status,title,description,reason,version) values (?,?,?,?,?,?,?)",
                id,owner.id(),status,"PRIVATE_HISTORY_TITLE","d","r",status.equals("Draft")?0:1);
            if(!status.equals("Draft")) history(id,reviewer,"PendingReview",status,1,"PRIVATE_HISTORY_EXPLANATION");
            for(var actor:List.of(other,reviewer)) {
                var hidden=send(request(BASE+"/"+id+"/history",actor));var absent=absentByActor.get(actor.id());
                problem(hidden,404,"NOT_FOUND");problem(absent,404,"NOT_FOUND");assertEquals(json(absent.body()),json(hidden.body()));
                assertFalse(hidden.body().contains("PRIVATE_HISTORY"));assertNull(json(hidden.body()).get("items"));
            }
            assertEquals(200,send(request(BASE+"/"+id+"/history",owner)).statusCode());
            problem(send(request(BASE+"/"+id+"/history",none)),403,"FORBIDDEN");
        }
    }
    @Test void reviewerLosesHistoryWhenParentLeavesPendingAndFreshRolesControlDualOwner() throws Exception {
        var owner=account(true,true);var reviewer=account(false,true);var id=seed(owner,"PendingReview","t","d","r",1);
        history(id,owner,"Draft","PendingReview",1,"");
        assertEquals(200,send(request(BASE+"/"+id+"/history",reviewer)).statusCode());
        assertEquals(200,mutate(reviewer,"POST",BASE+"/"+id+"/request-changes","{\"expectedVersion\":1,\"explanation\":\"fix\"}").statusCode());
        problem(send(request(BASE+"/"+id+"/history",reviewer)),404,"NOT_FOUND");
        var visible=send(request(BASE+"/"+id+"/history",owner));assertEquals(200,visible.statusCode());assertEquals(2,json(visible.body()).get("totalElements").asLong());
        provisioner.change(new ActorId(owner.id()),0,true,false,true);
        problem(send(request(BASE+"/"+id+"/history",owner)),404,"NOT_FOUND");assertEquals(2,auditCount(id));
    }
    @Test void draftHistoryIsEmptyAndReadRequiresAuthentication() throws Exception {
        var owner=account(true,false);var reviewer=account(false,true);var id=seed(owner,"Draft",null,null,null,0);
        var response=send(request(BASE+"/"+id+"/history",owner));assertEquals(200,response.statusCode());
        assertEquals(0,json(response.body()).get("items").size());assertEquals(0,json(response.body()).get("totalElements").asLong());assertEquals(0,json(response.body()).get("totalPages").asInt());
        problem(send(request(BASE+"/"+id+"/history",reviewer)),404,"NOT_FOUND");problem(send(request(BASE+"/"+id+"/history",null)),401,"UNAUTHENTICATED");
    }
}
