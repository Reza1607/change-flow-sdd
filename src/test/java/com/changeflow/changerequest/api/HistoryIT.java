package com.changeflow.changerequest.api;

import com.changeflow.support.HistoryApiSupport;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HistoryIT extends HistoryApiSupport {
    @Test void historyIsPagedByResultingVersionWithCompleteUtcDtoAndNoMutationRoutes() throws Exception {
        var owner=account(true,false);var reviewer=account(false,true);var id=seed(owner,"PendingReview","t","d","r",5);
        // Insert out of order, with version gaps representing intervening edits.
        history(id,owner,"ChangesRequested","PendingReview",5,"");
        history(id,owner,"Draft","PendingReview",1,"submit");
        history(id,reviewer,"PendingReview","ChangesRequested",3,"اصلاح دلیل");
        var before=jdbc.queryForList("select * from status_history where request_id=? order by resulting_version",id);
        for(int page=0;page<3;page++) {
            var response=send(request(BASE+"/"+id+"/history?page="+page+"&size=1",owner));assertEquals(200,response.statusCode());
            assertEquals("no-store",response.headers().firstValue("Cache-Control").orElseThrow());
            var body=json(response.body());assertEquals(page,body.get("page").asInt());assertEquals(1,body.get("size").asInt());
            assertEquals(3,body.get("totalElements").asLong());assertEquals(3,body.get("totalPages").asInt());
            var item=body.get("items").get(0);assertEquals(8,item.size());
            assertEquals(id.toString(),item.get("requestId").asString());UUID.fromString(item.get("id").asString());
            assertEquals((page==1?reviewer:owner).id().toString(),item.get("actorId").asString());
            assertEquals(1+2*page,item.get("resultingVersion").asLong());
            assertEquals(page==0?"Draft":page==1?"PendingReview":"ChangesRequested",item.get("fromStatus").asString());
            assertEquals(page==1?"ChangesRequested":"PendingReview",item.get("toStatus").asString());
            assertEquals(page==0?"submit":page==1?"اصلاح دلیل":"",item.get("explanation").asString());
            assertTrue(item.get("occurredAt").asString().endsWith("Z"));assertEquals(Instant.parse("2026-11-15T10:00:00Z"),Instant.parse(item.get("occurredAt").asString()));
        }
        var defaultPage=json(send(request(BASE+"/"+id+"/history",owner)).body());assertEquals(20,defaultPage.get("size").asInt());
        var beyond=send(request(BASE+"/"+id+"/history?page=3&size=1",owner));assertEquals(200,beyond.statusCode());assertEquals(0,json(beyond.body()).get("items").size());
        for(String method:List.of("PUT","DELETE","POST")) {
            var response=mutate(owner,method,BASE+"/"+id+"/history","{}");assertEquals(405,response.statusCode());
        }
        assertEquals(before,jdbc.queryForList("select * from status_history where request_id=? order by resulting_version",id));
    }
    @Test void invalidPaginationIsBadRequestAndCannotMutateAudit() throws Exception {
        var owner=account(true,false);var id=seed(owner,"Draft","t","d","r",0);
        for(String query:List.of("page=-1","size=0","size=101","page=abc","size=abc","page=2147483648"))
            problem(send(request(BASE+"/"+id+"/history?"+query,owner)),400,"BAD_REQUEST");
        assertEquals(0,auditCount(id));
    }
}
