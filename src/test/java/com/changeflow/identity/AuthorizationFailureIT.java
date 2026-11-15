package com.changeflow.identity;

import com.changeflow.support.*;
import com.changeflow.support.ConcurrencyTestConfiguration.*;
import com.changeflow.identity.domain.ActorId;
import com.changeflow.identity.infrastructure.IdentityFaultConfiguration;
import com.changeflow.identity.infrastructure.IdentityFaultConfiguration.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import static org.junit.jupiter.api.Assertions.*;

@Import({ConcurrencyTestConfiguration.class,IdentityFaultConfiguration.class})
class AuthorizationFailureIT extends DraftApiSupport {
    @Autowired Controls controls;
    @Autowired Faults faults;
    @Test void lostRoleConnectionDuringFinalGuardFailsClosedWithoutCommit() throws Exception {
        var actor=account(true,false);var id=seed(actor,"Draft","private title","d","r",0);var before=snapshot(id);
        var gate=new GuardGate(new ActorId(actor.id()),id,false);controls.guard=gate;
        try(var pool=Executors.newSingleThreadExecutor()) {
            try {
                var mutation=pool.submit(()->mutate(actor,"POST",BASE+"/"+id+"/submit","{\"expectedVersion\":0}"));gate.awaitEntered();
                assertEquals(Boolean.TRUE,jdbc.queryForObject("select pg_terminate_backend(?,5000)",Boolean.class,gate.pid));
                gate.release.countDown();var response=mutation.get(30,TimeUnit.SECONDS);
                problem(response,503,"DEPENDENCY_UNAVAILABLE");safe(response.body());
                assertEquals(before,snapshot(id));assertEquals(0,auditCount(id));
            } finally {gate.release.countDown();controls.guard=null;}
        }
        assertEquals(200,send(request(BASE+"/"+id,actor)).statusCode());
    }
    @Test void identityConnectionFailureUsesTheSame503ProblemAsApi() throws Exception {
        var actor=account(true,false);var id=seed(actor,"Draft","private title","d","r",0);var before=snapshot(id);
        faults.next.set(Failure.Connection);
        var response=send(request(BASE+"/"+id,actor));problem(response,503,"DEPENDENCY_UNAVAILABLE");safe(response.body());
        assertEquals(before,snapshot(id));assertEquals(0,auditCount(id));assertEquals(200,send(request(BASE+"/"+id,actor)).statusCode());
    }
    @Test void unexpectedIdentityFailureIs500AndNeverLeakingItsCause() throws Exception {
        var actor=account(true,false);var id=seed(actor,"Draft","private title","d","r",0);var before=snapshot(id);
        faults.next.set(Failure.Unexpected);
        var response=send(request(BASE+"/"+id,actor));problem(response,500,"INTERNAL_ERROR");safe(response.body());
        assertEquals(before,snapshot(id));assertEquals(0,auditCount(id));
    }
    private void safe(String body) {
        for(var forbidden:List.of("PRIVATE_IDENTITY_FAILURE","private title","password","select ","SQL","stackTrace","SQLException")) assertFalse(body.contains(forbidden));
        assertNull(json(body).get("id"));assertNull(json(body).get("requesterId"));
    }
}
