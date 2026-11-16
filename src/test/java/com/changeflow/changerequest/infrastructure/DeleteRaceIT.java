package com.changeflow.changerequest.infrastructure;

import com.changeflow.support.*;
import com.changeflow.support.ConcurrencyTestConfiguration.*;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import static org.junit.jupiter.api.Assertions.*;

@Import(ConcurrencyTestConfiguration.class)
class DeleteRaceIT extends DraftApiSupport {
    @Autowired Controls controls;
    enum Race { EditDelete, SubmitDelete, DeleteDelete }
    @ParameterizedTest @EnumSource(Race.class)
    void exactlyOneVersionCheckedMutationWinsAndNoOrphanAuditExists(Race race) throws Exception {
        var owner=account(true,false);
        try(var pool=Executors.newFixedThreadPool(2)) {
            for(int iteration=1;iteration<=20;iteration++) {
                var id=seed(owner,"Draft","t","d","r",0);var gate=new ReadRace(id);controls.race=gate;
                HttpResponse<String> first,second;
                try {
                    var a=pool.submit(()->first(race,owner,id));
                    var b=pool.submit(()->mutate(owner,"DELETE",BASE+"/"+id+"?expectedVersion=0",null));
                    first=a.get(30,TimeUnit.SECONDS);second=b.get(30,TimeUnit.SECONDS);
                } finally {controls.race=null;}
                assertEquals(2,gate.pids.size(),"independent PostgreSQL connections");assertEquals(2,gate.sessions.size(),"independent EntityManagers");assertEquals(2,gate.reads.get(),"no reload/retry");
                var responses=List.of(first,second);assertEquals(1,responses.stream().filter(r->r.statusCode()==200||r.statusCode()==204).count(),"exactly one accepted mutation");
                var loser=responses.stream().filter(r->r.statusCode()!=200&&r.statusCode()!=204).findFirst().orElseThrow();problem(loser,409,"CONCURRENT_MODIFICATION");
                boolean deleted=first.statusCode()==204 || second.statusCode()==204;
                if(race==Race.DeleteDelete) assertTrue(deleted);
                if(deleted) {
                    assertTrue(jdbc.queryForList("select * from change_request where id=?",id).isEmpty());assertEquals(0,auditCount(id));
                    problem(send(request(BASE+"/"+id,owner)),404,"NOT_FOUND");problem(send(request(BASE+"/"+id+"/history",owner)),404,"NOT_FOUND");
                } else {
                    assertEquals(200,first.statusCode());var row=snapshot(id);assertEquals(1L,row.get("version"));assertEquals(owner.id(),row.get("requester_id"));
                    assertEquals(race==Race.EditDelete?"edited":"t",row.get("title"));assertEquals("d",row.get("description"));assertEquals("r",row.get("reason"));
                    assertEquals(race==Race.SubmitDelete?"PendingReview":"Draft",row.get("status"));assertEquals(1,json(first.body()).get("version").asLong());
                    assertEquals(race==Race.SubmitDelete?1:0,auditCount(id));
                    if(race==Race.SubmitDelete) {
                        var audit=jdbc.queryForMap("select * from status_history where request_id=?",id);
                        assertEquals(owner.id(),audit.get("actor_id"));assertEquals("Draft",audit.get("from_status"));assertEquals("PendingReview",audit.get("to_status"));assertEquals(1L,audit.get("resulting_version"));
                    }
                    // The losing DELETE cannot erase the winner's data or create a transition audit.
                    assertEquals(200,send(request(BASE+"/"+id,owner)).statusCode());
                }
                assertEquals(0L,jdbc.queryForObject("select count(*) from status_history h left join change_request r on r.id=h.request_id where r.id is null",Long.class));
                System.out.println("US4 race="+race+" iteration="+iteration+" accepted=1 conflict=1 deleted="+deleted+" audit="+auditCount(id)+" orphanAudit=0 reads=2");
            }
        }
    }
    private HttpResponse<String> first(Race race,Account owner,UUID id) throws Exception {
        return switch(race) {
            case EditDelete->mutate(owner,"PUT",BASE+"/"+id,"{\"expectedVersion\":0,\"title\":\"edited\",\"description\":\"d\",\"reason\":\"r\"}");
            case SubmitDelete->mutate(owner,"POST",BASE+"/"+id+"/submit","{\"expectedVersion\":0}");
            case DeleteDelete->mutate(owner,"DELETE",BASE+"/"+id+"?expectedVersion=0",null);
        };
    }
}
