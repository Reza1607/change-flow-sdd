package com.changeflow;

import java.net.InetAddress;
import java.util.UUID;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Explicitly imported by the test entry point; not component-scanned or packaged. */
@TestConfiguration(proxyBeanMethods=false)
@Profile("dev")
public class DevServicesConfiguration {
    @Bean @ServiceConnection
    PostgreSQLContainer devPostgres(Environment environment) {
        validate(environment);
        return new PostgreSQLContainer("postgres:17.12")
            .withDatabaseName("changeflow_dev")
            .withUsername("dev_"+UUID.randomUUID().toString().replace("-",""))
            .withPassword(UUID.randomUUID().toString());
    }
    @Bean ApplicationRunner devAccounts(Environment environment,JdbcTemplate jdbc,PasswordEncoder encoder) {
        return args-> {
            validate(environment);
            String hash=encoder.encode(environment.getProperty("CHANGEFLOW_DEV_PASSWORD"));
            for(String name:new String[]{"requester","reviewer","dual"}) {
                jdbc.update("insert into identity_account(actor_id,username,password_hash,enabled,requester,reviewer) values (?,?,?,true,?,?)",
                    UUID.randomUUID(),name,hash,!name.equals("reviewer"),!name.equals("requester"));
            }
        };
    }
    private static void validate(Environment environment) {
        String secret=environment.getProperty("CHANGEFLOW_DEV_PASSWORD");
        if(secret==null || secret.isBlank())throw new IllegalStateException("CHANGEFLOW_DEV_PASSWORD is required");
        String address=environment.getProperty("server.address","");
        try {
            if(address.isBlank() || !InetAddress.getByName(address).isLoopbackAddress())throw new IllegalStateException("Dev requires loopback binding");
        } catch(java.net.UnknownHostException ex) {
            throw new IllegalStateException("Dev requires loopback binding");
        }
    }
}
