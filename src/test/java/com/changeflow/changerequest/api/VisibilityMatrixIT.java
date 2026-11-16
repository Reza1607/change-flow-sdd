package com.changeflow.changerequest.api;

import com.changeflow.support.HistoryApiSupport;
import com.changeflow.identity.domain.ActorId;
import com.changeflow.identity.infrastructure.AccessProvisioner;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import static org.junit.jupiter.api.Assertions.*;

class VisibilityMatrixIT extends HistoryApiSupport {
    @Autowired AccessProvisioner provisioner;
    record Row(UUID id,UUID owner,String status) {}
    @Test void fullRoleOwnerStateMatrixAndFreshDualRoleRevocationAndGrant() throws Exception {
        var owner=account(true,false);var dual=account(true,true);var reviewer=account(false,true);var none=account(false,false);
        var rows=new ArrayList<Row>();
        for(var requester:List.of(owner,dual)) for(String status:List.of("Draft","PendingReview","ChangesRequested","Approved")) {
            var id=seed(requester,status,"PRIVATE_MATRIX","d","r",status.equals("Draft")?0:1);rows.add(new Row(id,requester.id(),status));
            if(!status.equals("Draft")) history(id,status.equals("PendingReview")?requester:reviewer,status.equals("PendingReview")?"Draft":"PendingReview",status,1,"PRIVATE_MATRIX_REASON");
        }
        check(owner,true,false,rows);check(dual,true,true,rows);check(reviewer,false,true,rows);check(none,false,false,rows);
        provisioner.change(new ActorId(dual.id()),0,true,false,true);check(dual,false,true,rows);
        provisioner.change(new ActorId(dual.id()),1,true,true,false);check(dual,true,false,rows);
        provisioner.change(new ActorId(dual.id()),2,true,true,true);check(dual,true,true,rows);
        provisioner.change(new ActorId(none.id()),0,true,false,true);check(none,false,true,rows);
    }
    private void check(Account actor,boolean requester,boolean reviewer,List<Row> rows) throws Exception {
        for(var row:rows) {
            boolean visible=requester && actor.id().equals(row.owner()) || reviewer && row.status().equals("PendingReview");
            for(String suffix:List.of("","/history")) {
                var response=send(request(BASE+"/"+row.id()+suffix,actor));
                if(!requester&&!reviewer) problem(response,403,"FORBIDDEN");
                else if(!visible) {problem(response,404,"NOT_FOUND");assertFalse(response.body().contains("PRIVATE_MATRIX"));}
                else {assertEquals(200,response.statusCode());assertEquals("no-store",response.headers().firstValue("Cache-Control").orElseThrow());}
            }
        }
        for(String status:List.of("","Draft","PendingReview","ChangesRequested","Approved")) {
            var response=send(request(BASE+(status.isEmpty()?"":"?status="+status),actor));
            if(!requester&&!reviewer) {problem(response,403,"FORBIDDEN");continue;}
            assertEquals(200,response.statusCode());var body=json(response.body());var actual=new HashSet<String>();
            for(var item:body.get("items")) {actual.add(item.get("id").asString());assertEquals(7,item.size());}
            var expected=new HashSet<String>();
            for(var row:rows) if((requester&&row.owner().equals(actor.id())||reviewer&&row.status().equals("PendingReview"))&&(status.isEmpty()||status.equals(row.status()))) expected.add(row.id().toString());
            assertEquals(expected,actual);assertEquals(expected.size(),body.get("totalElements").asLong());assertEquals(expected.isEmpty()?0:1,body.get("totalPages").asLong());
        }
        System.out.println("US4 visibility requester="+requester+" reviewer="+reviewer+" rows=8 detail/history/list/statusFilters=PASS");
    }
}
