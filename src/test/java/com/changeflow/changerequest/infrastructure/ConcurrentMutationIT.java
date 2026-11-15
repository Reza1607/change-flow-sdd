package com.changeflow.changerequest.infrastructure;

import com.changeflow.support.*;
import com.changeflow.support.ConcurrencyTestConfiguration.*;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import static org.junit.jupiter.api.Assertions.*;

@Import(ConcurrencyTestConfiguration.class)
class ConcurrentMutationIT extends DraftApiSupport {
    @Autowired Controls controls;
    enum Race { EditEdit, EditSubmit, ApproveApprove, EqualEditEqualEdit }
    @ParameterizedTest @EnumSource(Race.class)
    void competingMutationsPreserveExactlyOneWinner(Race race) throws Exception {
        var owner=account(true,false);var firstReviewer=account(false,true);var secondReviewer=account(false,true);
        try(var pool=Executors.newFixedThreadPool(2)) {
            for(int iteration=0;iteration<20;iteration++) {
                var id=seed(owner,race==Race.ApproveApprove?"PendingReview":"Draft","t","d","r",0);
                var gate=new ReadRace(id);controls.race=gate;
                HttpResponse<String> first,second;
                try {
                    var one=pool.submit(()->operation(race,true,id,owner,firstReviewer));
                    var two=pool.submit(()->operation(race,false,id,owner,secondReviewer));
                    first=one.get(30,TimeUnit.SECONDS);second=two.get(30,TimeUnit.SECONDS);
                } finally {controls.race=null;}
                assertEquals(2,gate.pids.size(),"independent PostgreSQL connections");
                assertEquals(2,gate.sessions.size(),"independent EntityManagers");assertEquals(2,gate.reads.get(),"no reload or retry");
                var responses=List.of(first,second);
                assertEquals(1,responses.stream().filter(r->r.statusCode()==200).count(),"exactly one accepted mutation");
                var loser=responses.stream().filter(r->r.statusCode()!=200).findFirst().orElseThrow();problem(loser,409,"CONCURRENT_MODIFICATION");
                var winner=responses.stream().filter(r->r.statusCode()==200).findFirst().orElseThrow();var receipt=json(winner.body());
                assertEquals(1,receipt.get("version").asLong());var saved=snapshot(id);assertEquals(1L,saved.get("version"));
                assertEquals(owner.id(),saved.get("requester_id"));assertEquals("d",saved.get("description"));assertEquals("r",saved.get("reason"));
                assertEquals(receipt.get("title").asString(),saved.get("title"));assertEquals(receipt.get("status").asString(),saved.get("status"));
                boolean transitioned=!saved.get("status").equals("Draft");assertEquals(transitioned?1:0,auditCount(id));
                if(race==Race.EditEdit) assertEquals(first.statusCode()==200?"first":"second",saved.get("title"));
                if(race==Race.EqualEditEqualEdit) assertEquals("t",saved.get("title"));
                if(race==Race.ApproveApprove) {
                    assertEquals("Approved",saved.get("status"));var winningActor=first.statusCode()==200?firstReviewer:secondReviewer;var losingActor=first.statusCode()==200?secondReviewer:firstReviewer;
                    var audit=jdbc.queryForMap("select * from status_history where request_id=?",id);
                    assertEquals(winningActor.id(),audit.get("actor_id"));assertEquals(1L,audit.get("resulting_version"));assertEquals("PendingReview",audit.get("from_status"));assertEquals("Approved",audit.get("to_status"));
                    assertEquals(0L,jdbc.queryForObject("select count(*) from status_history where request_id=? and actor_id=?",Long.class,id,losingActor.id()));
                } else if(transitioned) {
                    assertEquals(Race.EditSubmit,race);assertEquals("t",saved.get("title"));assertEquals("PendingReview",saved.get("status"));
                    var audit=jdbc.queryForMap("select * from status_history where request_id=?",id);assertEquals(owner.id(),audit.get("actor_id"));
                    assertEquals("Draft",audit.get("from_status"));assertEquals("PendingReview",audit.get("to_status"));assertEquals(1L,audit.get("resulting_version"));
                }
                if(race==Race.EditSubmit && first.statusCode()==200) {assertEquals("edited",saved.get("title"));assertEquals(0,auditCount(id));}
                System.out.println("US5 race="+race+" iteration="+(iteration+1)+" accepted=1 conflict=1 reads=2 audit="+auditCount(id));
            }
        }
    }
    private HttpResponse<String> operation(Race race,boolean first,UUID id,Account owner,Account reviewer) throws Exception {
        if(race==Race.ApproveApprove) return mutate(reviewer,"POST",BASE+"/"+id+"/approve","{\"expectedVersion\":0}");
        if(race==Race.EditSubmit && !first) return mutate(owner,"POST",BASE+"/"+id+"/submit","{\"expectedVersion\":0}");
        String title=race==Race.EqualEditEqualEdit?"t":race==Race.EditSubmit?"edited":first?"first":"second";
        return mutate(owner,"PUT",BASE+"/"+id,encode(Map.of("expectedVersion",0,"title",title,"description","d","reason","r")));
    }
    @Test void staleHttpEditAndApproveNeverOverwriteCurrentRows() throws Exception {
        var owner=account(true,false);var reviewer=account(false,true);var draft=seed(owner,"Draft","t","d","r",4);var pending=seed(owner,"PendingReview","t","d","r",4);
        for(var id:List.of(draft,pending)) {
            var before=snapshot(id);
            var response=id.equals(draft)?mutate(owner,"PUT",BASE+"/"+id,"{\"expectedVersion\":3,\"title\":\"old\",\"description\":null,\"reason\":null}")
                :mutate(reviewer,"POST",BASE+"/"+id+"/approve","{\"expectedVersion\":3}");
            problem(response,409,"CONCURRENT_MODIFICATION");assertEquals(before,snapshot(id));assertEquals(0,auditCount(id));
        }
    }
}
