package com.changeflow.changerequest.api;

import com.changeflow.support.DraftApiSupport;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RequestChangesIT extends DraftApiSupport {
    @Test void explanationIsRequiredIncludingUnicodeWhitespaceWithoutAnyEffects() throws Exception {
        var owner=account(true,false);var reviewer=account(false,true);
        var id=seed(owner,"PendingReview","t","d","r",0);var before=snapshot(id);
        var bodies=new ArrayList<String>();bodies.add("{\"expectedVersion\":0}");
        for(String explanation:List.of(""," \t\n","\u00a0\u2007\u202f\u3000")) bodies.add(encode(Map.of("expectedVersion",0,"explanation",explanation)));
        for(var body:bodies) {
            var response=mutate(reviewer,"POST",BASE+"/"+id+"/request-changes",body);
            problem(response,422,"VALIDATION_FAILED");assertEquals("explanation",json(response.body()).get("errors").get(0).get("field").asString());
            assertEquals(before,snapshot(id));assertEquals(0,auditCount(id));
        }
    }
    @Test void validReturnIsAtomicAndDualOwnerCannotReviewSelf() throws Exception {
        var owner=account(true,true);var reviewer=account(false,true);var id=seed(owner,"PendingReview","t","d","r",0);var before=snapshot(id);
        problem(mutate(owner,"POST",BASE+"/"+id+"/request-changes",encode(Map.of("expectedVersion",0,"explanation","fix"))),403,"SELF_REVIEW_FORBIDDEN");
        assertEquals(before,snapshot(id));assertEquals(0,auditCount(id));
        var response=mutate(reviewer,"POST",BASE+"/"+id+"/request-changes",encode(Map.of("expectedVersion",0,"explanation","اصلاح دلیل")));
        assertEquals(200,response.statusCode());assertEquals("ChangesRequested",json(response.body()).get("status").asString());
        assertEquals(1L,snapshot(id).get("version"));assertEquals(1,auditCount(id));
        var audit=jdbc.queryForMap("select * from status_history where request_id=?",id);
        assertEquals(reviewer.id(),audit.get("actor_id"));assertEquals("اصلاح دلیل",audit.get("explanation"));assertEquals(1L,audit.get("resulting_version"));
        assertEquals("PendingReview",audit.get("from_status"));assertEquals("ChangesRequested",audit.get("to_status"));
        // Visible owner lacks independent identity, so self-review remains forbidden before state validation.
        problem(mutate(owner,"POST",BASE+"/"+id+"/request-changes","{\"expectedVersion\":1,\"explanation\":\"fix\"}"),403,"SELF_REVIEW_FORBIDDEN");
        assertEquals(1,auditCount(id));
    }
}
