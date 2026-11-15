package com.changeflow.identity.infrastructure;

import java.util.concurrent.atomic.AtomicReference;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;

/** Test-only faults after a real identity lookup; no authentication or DB mock. */
@TestConfiguration(proxyBeanMethods=false)
public class IdentityFaultConfiguration {
    public enum Failure { None, Connection, Unexpected }
    public static class Faults {public final AtomicReference<Failure> next=new AtomicReference<>(Failure.None);}
    @Bean Faults identityFaults() {return new Faults();}
    public static class FaultIdentity extends DatabaseIdentityAdapter {
        private final JdbcTemplate jdbc;
        private final Faults faults;
        FaultIdentity(IdentityAccountRepository accounts,JdbcTemplate jdbc,Faults faults) {super(accounts);this.jdbc=jdbc;this.faults=faults;}
        @Override @Transactional(readOnly=true)
        public UserDetails loadUserByUsername(String username) {
            var result=super.loadUserByUsername(username);
            switch(faults.next.getAndSet(Failure.None)) {
                case Connection->jdbc.execute("select pg_terminate_backend(pg_backend_pid())");
                case Unexpected->throw new IllegalStateException("PRIVATE_IDENTITY_FAILURE");
                case None->{}
            }
            return result;
        }
    }
    @Bean @Primary DatabaseIdentityAdapter faultIdentity(IdentityAccountRepository accounts,JdbcTemplate jdbc,Faults faults) {
        return new FaultIdentity(accounts,jdbc,faults);
    }
}
