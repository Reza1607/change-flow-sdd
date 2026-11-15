package com.changeflow.changerequest.api;

import com.changeflow.support.DraftApiSupport;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResubmissionIT extends DraftApiSupport {
    @Test void editsAndSubmitKeepEarlierAuditAndValidateContentAndCurrentOwnerRole() throws Exception {
        var owner=account(true,false);var other=account(true,false);var reviewer=account(false,true);
        var id=seed(owner,"PendingReview","t","d","r",0);
        assertEquals(200,mutate(reviewer,"POST",BASE+"/"+id+"/request-changes","{\"expectedVersion\":0,\"explanation\":\"clarify\"}").statusCode());
        var earlier=jdbc.queryForMap("select * from status_history where request_id=?",id);
        assertEquals(200,mutate(owner,"PUT",BASE+"/"+id,"{\"expectedVersion\":1,\"title\":null,\"description\":null,\"reason\":null}").statusCode());
        var before=snapshot(id);
        problem(mutate(owner,"POST",BASE+"/"+id+"/submit","{\"expectedVersion\":2}"),422,"VALIDATION_FAILED");
        problem(mutate(other,"POST",BASE+"/"+id+"/submit","{\"expectedVersion\":2}"),404,"NOT_FOUND");
        assertEquals(before,snapshot(id));assertEquals(1,auditCount(id));
        assertEquals(200,mutate(owner,"PUT",BASE+"/"+id,"{\"expectedVersion\":2,\"title\":\"fixed\",\"description\":\"d\",\"reason\":\"r\"}").statusCode());
        assertEquals(200,mutate(owner,"POST",BASE+"/"+id+"/submit","{\"expectedVersion\":3}").statusCode());
        assertEquals(earlier,jdbc.queryForMap("select * from status_history where request_id=? and resulting_version=1",id));
        assertEquals(2,auditCount(id));assertEquals(4L,snapshot(id).get("version"));
    }
    @Test void historySurvivesRealCorrectionCycle() throws Exception {
        var owner=account(true,false);var reviewer=account(false,true);var id=seed(owner,"PendingReview","t","d","r",0);
        assertEquals(200,mutate(reviewer,"POST",BASE+"/"+id+"/request-changes","{\"expectedVersion\":0,\"explanation\":\"clarify\"}").statusCode());
        assertEquals(200,mutate(owner,"PUT",BASE+"/"+id,"{\"expectedVersion\":1,\"title\":\"fixed\",\"description\":\"d\",\"reason\":\"r\"}").statusCode());
        assertEquals(200,mutate(owner,"POST",BASE+"/"+id+"/submit","{\"expectedVersion\":2}").statusCode());
        var history=send(request(BASE+"/"+id+"/history",owner));assertEquals(200,history.statusCode());
        var items=json(history.body()).get("items");assertEquals(2,items.size());
        assertEquals(1,items.get(0).get("resultingVersion").asLong());assertEquals("clarify",items.get(0).get("explanation").asString());
        assertEquals(3,items.get(1).get("resultingVersion").asLong());
    }
}
