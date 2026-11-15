package com.changeflow.support;

import com.changeflow.changerequest.application.ChangeRequestStore;
import com.changeflow.changerequest.domain.ChangeRequest;
import com.changeflow.changerequest.infrastructure.JpaChangeRequestStore;
import com.changeflow.identity.application.CurrentAccess;
import com.changeflow.identity.domain.*;
import com.changeflow.identity.infrastructure.*;
import jakarta.persistence.EntityManager;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.*;

/** Test-only barriers around real adapters; no repository, DB, role or decision is mocked. */
@TestConfiguration(proxyBeanMethods=false)
public class ConcurrencyTestConfiguration {
    public static class Controls {
        public volatile ReadRace race;
        public volatile GuardGate guard;
        public volatile int writerPid;
        public volatile Object writerSession;
        public volatile Map<String,Object> writerObservedRow;
    }
    public static class ReadRace {
        public final UUID requestId;
        public final Set<Integer> pids=ConcurrentHashMap.newKeySet();
        public final Set<Object> sessions=ConcurrentHashMap.newKeySet();
        public final java.util.concurrent.atomic.AtomicInteger reads=new java.util.concurrent.atomic.AtomicInteger();
        private final CyclicBarrier loaded=new CyclicBarrier(2);
        public ReadRace(UUID requestId) {this.requestId=requestId;}
        void loaded(int pid,Object session) {
            pids.add(pid);sessions.add(session);reads.incrementAndGet();
            try {loaded.await(20,TimeUnit.SECONDS);}
            catch(Exception ex) {throw new IllegalStateException("test read barrier failed",ex);}
        }
    }
    public static class GuardGate {
        public final ActorId actor;
        public final UUID requestId;
        public final boolean afterGuard;
        public final CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
        private final AtomicBoolean used=new AtomicBoolean();
        public volatile int pid;
        public volatile Object session;
        public volatile boolean cachedAccount;
        public GuardGate(ActorId actor,UUID requestId,boolean afterGuard) {this.actor=actor;this.requestId=requestId;this.afterGuard=afterGuard;}
        public void awaitEntered() {await(entered);}
        void pause() {entered.countDown();await(release);}
    }
    public static void await(CountDownLatch latch) {
        try {if(!latch.await(20,TimeUnit.SECONDS)) throw new IllegalStateException("test latch timed out");}
        catch(InterruptedException ex) {Thread.currentThread().interrupt();throw new IllegalStateException(ex);}
    }
    @Bean Controls concurrencyControls() {return new Controls();}
    @Bean @Primary ChangeRequestStore observedStore(JpaChangeRequestStore actual,Controls controls,EntityManager em,JdbcTemplate jdbc) {
        return new ChangeRequestStore() {
            public ChangeRequest create(ChangeRequest r) {return actual.create(r);}
            public Optional<ChangeRequest> find(UUID id) {
                var result=actual.find(id);var race=controls.race;
                if(race!=null && race.requestId.equals(id)) race.loaded(jdbc.queryForObject("select pg_backend_pid()",Integer.class),em.getDelegate());
                return result;
            }
            public ChangeRequest update(ChangeRequest r,long version) {return actual.update(r,version);}
            public void delete(UUID id,long version) {actual.delete(id,version);}
        };
    }
    @Bean @Primary CurrentAccess observedAccess(DatabaseCurrentAccess actual,Controls controls,EntityManager em,JdbcTemplate jdbc) {
        return new CurrentAccess() {
            public AccessSnapshot read(ActorId id) {return actual.read(id);}
            public AccessSnapshot guard(ActorId id) {
                var gate=controls.guard;
                boolean selected=gate!=null && gate.actor.equals(id) && gate.used.compareAndSet(false,true);
                if(selected) {
                    gate.pid=jdbc.queryForObject("select pg_backend_pid()",Integer.class);gate.session=em.getDelegate();
                    var cached=em.createQuery("from IdentityAccountEntity where actorId=:actor",Object.class).setParameter("actor",id.value()).getSingleResult();
                    gate.cachedAccount=em.contains(cached);
                    if(!gate.afterGuard) gate.pause();
                }
                var result=actual.guard(id);
                if(selected && gate.afterGuard) gate.pause();
                return result;
            }
        };
    }
    /** Only instruments the real writer's transaction; super.change executes the production SQL. */
    public static class ObservedWriter extends AccessProvisioner {
        private final JdbcTemplate jdbc;
        private final Controls controls;
        private final EntityManager em;
        public ObservedWriter(JdbcTemplate jdbc,Controls controls,EntityManager em) {super(jdbc);this.jdbc=jdbc;this.controls=controls;this.em=em;}
        @Override @Transactional(propagation=Propagation.REQUIRES_NEW,isolation=Isolation.READ_COMMITTED)
        public void change(ActorId id,long version,boolean enabled,boolean requester,boolean reviewer) {
            controls.writerSession=em.getDelegate();
            controls.writerPid=jdbc.queryForObject("select pg_backend_pid()",Integer.class);
            super.change(id,version,enabled,requester,reviewer);
            var gate=controls.guard;
            if(gate!=null && gate.actor.equals(id)) controls.writerObservedRow=jdbc.queryForMap("select * from change_request where id=?",gate.requestId);
        }
    }
    @Bean @Primary AccessProvisioner observedWriter(JdbcTemplate jdbc,Controls controls,EntityManager em) {return new ObservedWriter(jdbc,controls,em);}
}
