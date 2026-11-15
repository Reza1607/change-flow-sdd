package com.changeflow.changerequest.api;

import com.changeflow.support.DraftApiSupport;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ApprovalIT extends DraftApiSupport {
    @Test void successfulApprovalIsAReceiptWithCompleteAuditAndDoesNotGrantGetAccess() throws Exception {
        var owner=account(true,false);var reviewer=account(false,true);var id=seed(owner,"PendingReview","t","d","r",3);
        var response=mutate(reviewer,"POST",BASE+"/"+id+"/approve","{\"expectedVersion\":3,\"explanation\":\"looks good\"}");
        assertEquals(200,response.statusCode());assertEquals("no-store",response.headers().firstValue("Cache-Control").orElseThrow());
        var dto=json(response.body());assertEquals(7,dto.size());assertEquals("Approved",dto.get("status").asString());assertEquals(4,dto.get("version").asLong());
        assertEquals(owner.id().toString(),dto.get("requesterId").asString());assertEquals(1,auditCount(id));
        var audit=jdbc.queryForMap("select * from status_history where request_id=?",id);
        assertEquals(reviewer.id(),audit.get("actor_id"));assertEquals("PendingReview",audit.get("from_status"));assertEquals("Approved",audit.get("to_status"));
        assertEquals("looks good",audit.get("explanation"));assertEquals(4L,audit.get("resulting_version"));assertNotNull(audit.get("occurred_at"));
        problem(send(request(BASE+"/"+id,reviewer)),404,"NOT_FOUND");assertEquals(200,send(request(BASE+"/"+id,owner)).statusCode());
    }
    @Test void dualRoleOwnerCannotApproveAndApprovedCannotBeEdited() throws Exception {
        var owner=account(true,true);var pending=seed(owner,"PendingReview","t","d","r",2);var before=snapshot(pending);
        problem(mutate(owner,"POST",BASE+"/"+pending+"/approve","{\"expectedVersion\":0}"),403,"SELF_REVIEW_FORBIDDEN");
        assertEquals(before,snapshot(pending));assertEquals(0,auditCount(pending));
        var approved=seed(owner,"Approved","t","d","r",4);before=snapshot(approved);
        problem(mutate(owner,"PUT",BASE+"/"+approved,"{\"expectedVersion\":4,\"title\":\"new\",\"description\":null,\"reason\":null}"),409,"INVALID_TRANSITION");
        assertEquals(before,snapshot(approved));assertEquals(0,auditCount(approved));
    }
    @Test void authorizationVisibilityStateAndVersionAreCheckedWithoutWrites() throws Exception {
        var owner=account(true,false);var reviewer=account(false,true);var none=account(false,false);
        var id=seed(owner,"PendingReview","t","d","r",2);var before=snapshot(id);
        problem(mutate(owner,"POST",BASE+"/"+id+"/approve","{\"expectedVersion\":2}"),403,"FORBIDDEN");
        problem(mutate(none,"POST",BASE+"/"+id+"/approve","{\"expectedVersion\":2}"),403,"FORBIDDEN");
        problem(mutate(reviewer,"POST",BASE+"/"+id+"/approve","{\"expectedVersion\":1}"),409,"CONCURRENT_MODIFICATION");
        assertEquals(before,snapshot(id));assertEquals(0,auditCount(id));
        for(var status:List.of("Draft","ChangesRequested","Approved")) {
            var hidden=seed(owner,status,"t","d","r",2);
            problem(mutate(reviewer,"POST",BASE+"/"+hidden+"/approve","{\"expectedVersion\":2}"),404,"NOT_FOUND");
            assertEquals(status,snapshot(hidden).get("status"));assertEquals(0,auditCount(hidden));
        }
        var dual=account(true,true);
        for(var status:List.of("Draft","ChangesRequested","Approved")) {
            var own=seed(dual,status,"t","d","r",2);
            problem(mutate(dual,"POST",BASE+"/"+own+"/approve","{\"expectedVersion\":2}"),403,"SELF_REVIEW_FORBIDDEN");
            assertEquals(status,snapshot(own).get("status"));assertEquals(0,auditCount(own));
        }
    }
    @Test void repeatedApprovalGivesGenericConflictOnlyToTheSuccessfulActor() throws Exception {
        var owner=account(true,false);var reviewer=account(false,true);var other=account(false,true);
        var id=seed(owner,"PendingReview","private title","private description","private reason",0);
        assertEquals(200,mutate(reviewer,"POST",BASE+"/"+id+"/approve","{\"expectedVersion\":0}").statusCode());
        var before=snapshot(id);var history=jdbc.queryForList("select * from status_history where request_id=?",id);
        for(long version:new long[]{0,1}) {
            var repeat=mutate(reviewer,"POST",BASE+"/"+id+"/approve",encode(Map.of("expectedVersion",version)));
            problem(repeat,409,"INVALID_TRANSITION");assertFalse(repeat.body().contains("private"));assertNull(json(repeat.body()).get("id"));
            assertFalse(repeat.body().contains("requesterId"));assertFalse(repeat.body().contains("Approved"));
            problem(mutate(other,"POST",BASE+"/"+id+"/approve",encode(Map.of("expectedVersion",version))),404,"NOT_FOUND");
        }
        problem(send(request(BASE+"/"+id,reviewer)),404,"NOT_FOUND");assertEquals(before,snapshot(id));
        assertEquals(history,jdbc.queryForList("select * from status_history where request_id=?",id));
    }
    @Test void rejectsBadJsonAndMissingCsrfWithoutWrites() throws Exception {
        var owner=account(true,false);var reviewer=account(false,true);var id=seed(owner,"PendingReview","t","d","r",0);var before=snapshot(id);
        for(var body:List.of("{}","{\"expectedVersion\":-1}","{\"expectedVersion\":\"0\"}","{\"expectedVersion\":null}","{\"expectedVersion\":0,\"actorId\":\"forged\"}","{\"expectedVersion\":0,\"status\":\"Approved\"}","{\"expectedVersion\":0,\"explanation\":null}","{\"expectedVersion\":0,\"explanation\":42}","null","{"))
            problem(mutate(reviewer,"POST",BASE+"/"+id+"/approve",body),400,"BAD_REQUEST");
        problem(mutate(reviewer,"POST",BASE+"/"+id+"/approve",null),400,"BAD_REQUEST");
        problem(send(request(BASE+"/"+id+"/approve",reviewer).header("Content-Type","application/json").POST(java.net.http.HttpRequest.BodyPublishers.ofString("{\"expectedVersion\":0}"))),403,"CSRF_INVALID");
        problem(send(request(BASE+"/"+id+"/approve",null).POST(java.net.http.HttpRequest.BodyPublishers.ofString("{}"))),401,"UNAUTHENTICATED");
        assertEquals(before,snapshot(id));assertEquals(0,auditCount(id));
    }
}
