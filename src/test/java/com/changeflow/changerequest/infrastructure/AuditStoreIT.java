package com.changeflow.changerequest.infrastructure;

import com.changeflow.support.HttpIntegrationSupport;
import com.changeflow.changerequest.application.*;
import com.changeflow.changerequest.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class AuditStoreIT extends HttpIntegrationSupport {
    @Autowired AuditTrail audit;
    @Autowired ChangeRequestStore store;
    @Autowired PlatformTransactionManager manager;
    @Test void appendsCompleteAuditAndRejectsDuplicateResultVersion() {
        var actor=account(true,false);var tx=new TransactionTemplate(manager);
        var request=tx.execute(s->store.create(ChangeRequest.draft(UUID.randomUUID(),actor.id(),"t","d","r")));
        var time=Instant.parse("2026-11-15T00:00:00.123456Z");
        var entry=new StatusHistoryEntry(UUID.randomUUID(),request.id(),actor.id(),time,ChangeRequestStatus.Draft,ChangeRequestStatus.PendingReview,"explanation",1);
        tx.executeWithoutResult(s->audit.append(entry));
        assertEquals(1,jdbc.queryForObject("select count(*) from status_history where request_id=?",Integer.class,request.id()));
        var row=jdbc.queryForMap("select * from status_history where id=?",entry.id());
        assertEquals(actor.id(),row.get("actor_id"));assertEquals("explanation",row.get("explanation"));
        assertEquals(1L,row.get("resulting_version"));
        var actual=jdbc.queryForObject("select occurred_at from status_history where id=?",(r,n)->r.getObject(1,java.time.OffsetDateTime.class).toInstant(),entry.id());
        assertEquals(time,actual);
        var duplicate=new StatusHistoryEntry(UUID.randomUUID(),request.id(),actor.id(),time,entry.fromStatus(),entry.toStatus(),"",1);
        assertThrows(DataIntegrityViolationException.class,()->tx.executeWithoutResult(s->audit.append(duplicate)));
        assertEquals(1,jdbc.queryForObject("select count(*) from status_history where request_id=?",Integer.class,request.id()));
    }
    @Test void appendParticipatesInCallerTransactionAndCannotCommitIndependently() {
        var actor=account(true,false);var id=UUID.randomUUID();var tx=new TransactionTemplate(manager);
        assertThrows(IllegalStateException.class,()->tx.executeWithoutResult(s->{
            store.create(ChangeRequest.draft(id,actor.id(),"t","d","r"));
            audit.append(new StatusHistoryEntry(UUID.randomUUID(),id,actor.id(),Instant.now(),ChangeRequestStatus.Draft,ChangeRequestStatus.PendingReview,"",1));
            throw new IllegalStateException("test rollback");
        }));
        assertEquals(0,jdbc.queryForObject("select count(*) from change_request where id=?",Integer.class,id));
        assertEquals(0,jdbc.queryForObject("select count(*) from status_history where request_id=?",Integer.class,id));
    }
}
