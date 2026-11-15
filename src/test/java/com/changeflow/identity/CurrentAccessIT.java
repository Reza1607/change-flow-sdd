package com.changeflow.identity;

import com.changeflow.support.HttpIntegrationSupport;
import com.changeflow.identity.application.CurrentAccess;
import com.changeflow.identity.domain.*;
import com.changeflow.identity.infrastructure.AccessProvisioner;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.List;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class CurrentAccessIT extends HttpIntegrationSupport {
    @jakarta.persistence.PersistenceContext jakarta.persistence.EntityManager em;
    @Autowired CurrentAccess access;
    @Autowired AccessProvisioner provisioner;
    @Autowired PlatformTransactionManager manager;
    private TransactionTemplate tx() {var t=new TransactionTemplate(manager);t.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);return t;}
    @Test
    void readsFreshRolesRegardlessOfOldAuthenticationAuthoritiesAndPersistsAccount() {
        var a=account(true,true);var id=new ActorId(a.id());
        var context=SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(a.username(),null,List.of(new SimpleGrantedAuthority("ROLE_Requester"))));
        SecurityContextHolder.setContext(context);
        try {
            assertTrue(access.read(id).has(Role.Requester));
            tx().executeWithoutResult(s->{
                assertTrue(access.read(id).has(Role.Requester));
                var cached=em.createQuery("from IdentityAccountEntity where actorId=:id",Object.class).setParameter("id",a.id()).getSingleResult();
                assertTrue(em.contains(cached));
                provisioner.change(id,0,true,false,true);
                assertFalse(access.guard(id).has(Role.Requester));
                assertTrue(access.guard(id).has(Role.Reviewer));
                assertEquals(1,access.guard(id).accessVersion());
            });
            assertThrows(org.springframework.dao.OptimisticLockingFailureException.class,()->provisioner.change(id,0,true,true,true));
            provisioner.change(id,1,true,false,false);
            assertFalse(access.read(id).has(Role.Reviewer));
            assertEquals(1,jdbc.queryForObject("select count(*) from identity_account where actor_id=?",Integer.class,a.id()));
            assertThrows(org.springframework.transaction.IllegalTransactionStateException.class,()->access.guard(id));
        } finally {SecurityContextHolder.clearContext();}
    }
    @Test
    void sharedGuardBlocksWriterUntilCommitAndAllowsConcurrentReaders() throws Exception {
        var a=account(true,true);var id=new ActorId(a.id());
        var locked=new CountDownLatch(1);var finish=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)) {
            var reader=pool.submit(()->tx().execute(s->{access.guard(id);locked.countDown();await(finish);return true;}));
            try {
                assertTrue(locked.await(10,TimeUnit.SECONDS));
                tx().executeWithoutResult(s->assertTrue(access.guard(id).has(Role.Requester)));
                var writer=pool.submit(()->{provisioner.change(id,0,true,false,true);return true;});
                // Observe an actual DB lock wait, rather than scheduling guesses or sleeps.
                long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);boolean blocked=false;
                while(System.nanoTime()<deadline) {
                    blocked=Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from pg_stat_activity where datname=current_database() and wait_event_type='Lock' and query like '%identity_account%')",Boolean.class));
                    if(blocked) break;
                    Thread.onSpinWait();
                }
                assertTrue(blocked,"writer must wait for the shared guard");assertFalse(writer.isDone());
                finish.countDown();assertTrue(reader.get(10,TimeUnit.SECONDS));assertTrue(writer.get(10,TimeUnit.SECONDS));
                assertFalse(access.read(id).has(Role.Requester));
            } finally {finish.countDown();}
        }
    }
    @Test
    void disabledAndMissingAccountsFailClosedAtGuard() {
        var a=account(true,true);var id=new ActorId(a.id());provisioner.change(id,0,false,true,true);
        assertFalse(access.read(id).has(Role.Requester));
        assertThrows(org.springframework.security.access.AccessDeniedException.class,()->tx().execute(s->access.guard(id)));
        assertThrows(org.springframework.security.access.AccessDeniedException.class,()->tx().execute(s->access.guard(new ActorId(java.util.UUID.randomUUID()))));
    }
    private static void await(CountDownLatch latch) {try {if(!latch.await(15,TimeUnit.SECONDS)) throw new AssertionError("barrier timeout");}catch(InterruptedException e){Thread.currentThread().interrupt();throw new AssertionError(e);}}
}
