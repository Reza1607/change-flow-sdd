package com.changeflow.changerequest.api;

import com.changeflow.support.DraftApiSupport;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PaginationIT extends DraftApiSupport {
    @Test void paginationCountsOnlyVisibleRowsAndUsesDatabaseUuidOrderAcrossPages() throws Exception {
        var owner=account(true,false);var other=account(true,false);var expected=new ArrayList<String>();
        for(int i=0;i<25;i++) expected.add(seed(owner,i%2==0?"Draft":"Approved","t","d","r",0).toString());
        for(int i=0;i<8;i++) seed(other,"PendingReview","HIDDEN_PAGE","d","r",0);
        // PostgreSQL UUID order is unsigned byte order; UUID.compareTo uses signed longs.
        Collections.sort(expected);var collected=new ArrayList<String>();
        for(int page=0;page<4;page++) {
            var response=send(request(BASE+"?page="+page+"&size=10",owner));assertEquals(200,response.statusCode());
            assertEquals("no-store",response.headers().firstValue("Cache-Control").orElseThrow());var body=json(response.body());
            assertEquals(page,body.get("page").asInt());assertEquals(10,body.get("size").asInt());assertEquals(25,body.get("totalElements").asLong());assertEquals(3,body.get("totalPages").asLong());
            var items=body.get("items");assertEquals(page<2?10:page==2?5:0,items.size());for(var item:items) collected.add(item.get("id").asString());
        }
        assertEquals(expected,collected);assertEquals(25,new HashSet<>(collected).size());
        var defaults=send(request(BASE,owner));assertEquals(200,defaults.statusCode());assertEquals(0,json(defaults.body()).get("page").asInt());assertEquals(20,json(defaults.body()).get("size").asInt());assertEquals(20,json(defaults.body()).get("items").size());
        var maximum=send(request(BASE+"?size=100",owner));assertEquals(200,maximum.statusCode());assertEquals(25,json(maximum.body()).get("items").size());
        var filtered=send(request(BASE+"?status=Draft&size=10",owner));assertEquals(200,filtered.statusCode());assertEquals(13,json(filtered.body()).get("totalElements").asLong());assertEquals(2,json(filtered.body()).get("totalPages").asLong());
        var far=send(request(BASE+"?page=2147483647&size=100",owner));assertEquals(200,far.statusCode());assertEquals(0,json(far.body()).get("items").size());assertEquals(25,json(far.body()).get("totalElements").asLong());
    }
    @Test void emptyPagesAndInvalidQueriesHaveContractResponses() throws Exception {
        var owner=account(true,false);var empty=send(request(BASE,owner));assertEquals(200,empty.statusCode());assertEquals(0,json(empty.body()).get("items").size());assertEquals(0,json(empty.body()).get("totalPages").asInt());
        for(String query:List.of("page=-1","size=0","size=101","page=abc","size=abc","page=2147483648","status=invalid"))
            problem(send(request(BASE+"?"+query,owner)),400,"BAD_REQUEST");
        problem(send(request(BASE,null)),401,"UNAUTHENTICATED");
    }
    @Test void presentEmptyQueriesFailWhileOmittedPaginationKeepsDefaults() throws Exception {
        var owner=account(true,false);var id=seed(owner,"Draft","t","d","r",0);var before=snapshot(id);
        // expectedVersion has no default: both absent and empty remain BAD_REQUEST.
        problem(mutate(owner,"DELETE",BASE+"/"+id+"?expectedVersion=",null),400,"BAD_REQUEST");
        problem(mutate(owner,"DELETE",BASE+"/"+id,null),400,"BAD_REQUEST");
        var checks=new ArrayList<org.junit.jupiter.api.function.Executable>();
        for(var path:List.of(BASE,BASE+"/"+id+"/history")) {
            var defaults=send(request(path,owner));assertEquals(200,defaults.statusCode());
            assertEquals(0,json(defaults.body()).get("page").asInt());assertEquals(20,json(defaults.body()).get("size").asInt());
            for(var query:List.of("page=","size=","status="))
                checks.add(()->problem(send(request(path+"?"+query,owner)),400,"BAD_REQUEST"));
        }
        assertAll(checks);
        assertEquals(before,snapshot(id));assertEquals(0,auditCount(id));
    }

}
