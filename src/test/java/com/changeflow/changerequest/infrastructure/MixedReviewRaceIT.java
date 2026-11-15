package com.changeflow.changerequest.infrastructure;

import com.changeflow.support.*;
import com.changeflow.support.ConcurrencyTestConfiguration.*;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import static org.junit.jupiter.api.Assertions.*;

@Import(ConcurrencyTestConfiguration.class)
class MixedReviewRaceIT extends DraftApiSupport {
    @Autowired Controls controls;
    @ParameterizedTest @ValueSource(booleans={false,true})
    void twoIndependentReviewersAcceptExactlyOneDecision(boolean mixed) throws Exception {
        var owner=account(true,false);var one=account(false,true);var two=account(false,true);
        try(var pool=Executors.newFixedThreadPool(2)) {
            for(int iteration=1;iteration<=20;iteration++) {
                var id=seed(owner,"PendingReview","t","d","r",0);var gate=new ReadRace(id);controls.race=gate;
                HttpResponse<String> first,second;
                try {
                    var a=pool.submit(()->mutate(one,"POST",BASE+"/"+id+(mixed?"/approve":"/request-changes"),"{\"expectedVersion\":0,\"explanation\":\"first decision\"}"));
                    var b=pool.submit(()->mutate(two,"POST",BASE+"/"+id+"/request-changes","{\"expectedVersion\":0,\"explanation\":\"second decision\"}"));
                    first=a.get(30,TimeUnit.SECONDS);second=b.get(30,TimeUnit.SECONDS);
                } finally {controls.race=null;}
                assertEquals(2,gate.pids.size(),"two PostgreSQL connections");assertEquals(2,gate.sessions.size(),"two EntityManagers");assertEquals(2,gate.reads.get(),"no reload/retry");
                var results=List.of(first,second);assertEquals(1,results.stream().filter(r->r.statusCode()==200).count(),"exactly one accepted decision");
                var loser=results.stream().filter(r->r.statusCode()!=200).findFirst().orElseThrow();problem(loser,409,"CONCURRENT_MODIFICATION");
                var winner=first.statusCode()==200?first:second;var winningActor=first.statusCode()==200?one:two;var losingActor=first.statusCode()==200?two:one;
                var status=mixed && first.statusCode()==200?"Approved":"ChangesRequested";var explanation=first.statusCode()==200?"first decision":"second decision";
                var row=snapshot(id);assertEquals(1L,row.get("version"));assertEquals(status,row.get("status"));assertEquals(owner.id(),row.get("requester_id"));
                assertEquals("t",row.get("title"));assertEquals("d",row.get("description"));assertEquals("r",row.get("reason"));
                assertEquals(1,json(winner.body()).get("version").asLong());assertEquals(status,json(winner.body()).get("status").asString());assertEquals(1,auditCount(id));
                var audit=jdbc.queryForMap("select * from status_history where request_id=?",id);assertEquals(winningActor.id(),audit.get("actor_id"));
                assertEquals("PendingReview",audit.get("from_status"));assertEquals(status,audit.get("to_status"));assertEquals(explanation,audit.get("explanation"));assertEquals(1L,audit.get("resulting_version"));
                assertEquals(0L,jdbc.queryForObject("select count(*) from status_history where request_id=? and actor_id=?",Long.class,id,losingActor.id()));
                System.out.println("US3 race="+(mixed?"ApproveRequestChanges":"RequestChangesRequestChanges")+" iteration="+iteration+" accepted=1 conflict=1 audit=1 loserAudit=0 reads=2");
            }
        }
    }
}
