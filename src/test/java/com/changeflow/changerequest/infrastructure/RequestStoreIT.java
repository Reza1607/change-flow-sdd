package com.changeflow.changerequest.infrastructure;

import com.changeflow.support.HttpIntegrationSupport;
import com.changeflow.changerequest.application.ChangeRequestStore;
import com.changeflow.changerequest.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class RequestStoreIT extends HttpIntegrationSupport {
    @Autowired ChangeRequestStore store;
    @Autowired PlatformTransactionManager manager;
    private TransactionTemplate tx() {return new TransactionTemplate(manager);}
    private ChangeRequest create() {
        var actor=account(true,false);
        return tx().execute(s->store.create(ChangeRequest.draft(UUID.randomUUID(),actor.id(),"t","d","r")));
    }
    @Test void persistsNewRequestAndAdvancesVersionIncludingEqualEdits() {
        var initial=create();
        assertEquals(1,jdbc.queryForObject("select count(*) from change_request where id=?",Integer.class,initial.id()));
        assertEquals(initial,tx().execute(s->store.find(initial.id()).orElseThrow()));
        var equal=tx().execute(s->store.update(initial,0));assertEquals(1,equal.version());
        assertEquals(1L,jdbc.queryForObject("select version from change_request where id=?",Long.class,initial.id()));
        var edited=equal.edit(equal.requesterId(),null,"new",null);
        var saved=tx().execute(s->store.update(edited,1));assertEquals(2,saved.version());
        assertEquals(saved,tx().execute(s->store.find(initial.id()).orElseThrow()));
        assertThrows(OptimisticLockingFailureException.class,()->tx().execute(s->store.update(initial,0)));
        assertEquals(saved,tx().execute(s->store.find(initial.id()).orElseThrow()));
    }
    @Test void preservesNewerCommitAgainstAnAlreadyLoadedManagedEntity() {
        var initial=create();var separate=new TransactionTemplate(manager);separate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        assertThrows(OptimisticLockingFailureException.class,()->tx().execute(s->{
            var old=store.find(initial.id()).orElseThrow();
            separate.executeWithoutResult(inner->store.update(old.edit(old.requesterId(),"winner","d","r"),0));
            return store.update(old.edit(old.requesterId(),"loser","d","r"),0);
        }));
        var saved=tx().execute(s->store.find(initial.id()).orElseThrow());assertEquals("winner",saved.title());assertEquals(1,saved.version());
    }
    @Test void versionCheckedDeleteCannotRemoveNewerData() {
        var initial=create();tx().executeWithoutResult(s->store.update(initial,0));
        assertThrows(OptimisticLockingFailureException.class,()->tx().executeWithoutResult(s->store.delete(initial.id(),0)));
        assertEquals(Boolean.TRUE,tx().execute(s->store.find(initial.id()).isPresent()));
        tx().executeWithoutResult(s->store.delete(initial.id(),1));assertEquals(Boolean.TRUE,tx().execute(s->store.find(initial.id()).isEmpty()));
        assertThrows(OptimisticLockingFailureException.class,()->tx().executeWithoutResult(s->store.update(initial,0)));
    }
}
