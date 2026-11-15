package com.changeflow.changerequest.api;

import com.changeflow.support.DraftApiSupport;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class GetRequestIT extends DraftApiSupport {
    @org.springframework.beans.factory.annotation.Autowired com.changeflow.identity.infrastructure.AccessProvisioner provisioner;
    @Test void requesterOwnsVisibilityAcrossAllStatesAndDtoHasOnlyContractFields() throws Exception {
        var owner=account(true,false);var other=account(true,false);
        for(var status:List.of("Draft","PendingReview","ChangesRequested","Approved")) {
            var id=seed(owner,status,null,"شرح",null,2);
            var r=send(request(BASE+"/"+id,owner));assertEquals(200,r.statusCode());
            var dto=json(r.body());assertEquals(7,dto.size());assertEquals(id.toString(),dto.get("id").asString());
            assertEquals(owner.id().toString(),dto.get("requesterId").asString());assertEquals(status,dto.get("status").asString());
            assertEquals(2,dto.get("version").asLong());assertTrue(dto.get("title").isNull());
            problem(send(request(BASE+"/"+id+"?actorId="+owner.id(),other)),404,"NOT_FOUND");
        }
    }
    @Test void reviewerSeesOnlyPendingAndDualRolesAggregateWithFreshRevocation() throws Exception {
        var owner=account(true,false);var reviewer=account(false,true);var dual=account(true,true);
        for(var status:List.of("Draft","PendingReview","ChangesRequested","Approved")) {
            var id=seed(owner,status,"t","d","r",0);var own=seed(dual,status,"t","d","r",0);
            assertEquals(status.equals("PendingReview")?200:404,send(request(BASE+"/"+id,reviewer)).statusCode());
            assertEquals(200,send(request(BASE+"/"+own,dual)).statusCode());
        }
        var approved=seed(dual,"Approved","t","d","r",0);var pending=seed(dual,"PendingReview","t","d","r",0);
        provisioner.change(new com.changeflow.identity.domain.ActorId(dual.id()),0,true,false,true);
        problem(send(request(BASE+"/"+approved,dual)),404,"NOT_FOUND");assertEquals(200,send(request(BASE+"/"+pending,dual)).statusCode());
        provisioner.change(new com.changeflow.identity.domain.ActorId(dual.id()),1,true,false,false);
        problem(send(request(BASE+"/"+pending,dual)),403,"FORBIDDEN");
        var ownDraft=seed(reviewer,"Draft","t","d","r",0);problem(send(request(BASE+"/"+ownDraft,reviewer)),404,"NOT_FOUND");
    }
    @Test void missingAndInvisibleHaveIdenticalProblemWithoutLeakingParent() throws Exception {
        var owner=account(true,false);var other=account(true,false);var noRole=account(false,false);
        var id=seed(owner,"Draft","private title","private description","private reason",0);
        var hidden=send(request(BASE+"/"+id,other));problem(hidden,404,"NOT_FOUND");assertFalse(hidden.body().contains("private"));
        jdbc.update("delete from change_request where id=?",id);
        var missing=send(request(BASE+"/"+id,other));problem(missing,404,"NOT_FOUND");assertEquals(hidden.body(),missing.body());
        problem(send(request(BASE+"/"+id,noRole)),403,"FORBIDDEN");
        problem(send(request(BASE+"/not-a-uuid",other)),400,"BAD_REQUEST");
        problem(send(request(BASE+"/"+id,null)),401,"UNAUTHENTICATED");
    }
}
