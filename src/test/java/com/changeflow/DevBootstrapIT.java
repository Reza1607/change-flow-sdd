package com.changeflow;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import static org.junit.jupiter.api.Assertions.*;

class DevBootstrapIT {
    private SpringApplicationBuilder application() {
        return new SpringApplicationBuilder(ChangeFlowApplication.class,DevServicesConfiguration.class)
            .profiles("dev").properties("spring.main.banner-mode=off") ;
    }
    private String[] args(String secret,String address) {
        return new String[]{"--server.port=0","--server.address="+address,"--server.ssl.enabled=false",
            "--server.ssl.bundle=",
            "--CHANGEFLOW_DEV_PASSWORD="+secret};
    }
    @Test void missingOrEmptySecretFailsStartup() {
        // The explicit empty value also overrides any inherited developer environment secret.
        var ex=assertThrows(Exception.class,()-> {
            try(var ignored=application().run(args("","127.0.0.1"))) {}
        });
        assertTrue(causes(ex).contains("CHANGEFLOW_DEV_PASSWORD is required"));
    }
    @Test void refusesNonLoopbackOverrideBeforeServing() {
        var ex=assertThrows(Exception.class,()-> {
            try(var ignored=application().run(args(UUID.randomUUID().toString(),"0.0.0.0"))) {}
        });
        assertTrue(causes(ex).contains("loopback"));
    }
    @Test void provisionsOnlyThreeAccountsWithHashedExternalSecret() {
        String secret=UUID.randomUUID().toString();
        try(var context=application().run(args(secret,"127.0.0.1"))) {
            var jdbc=context.getBean(JdbcTemplate.class);
            var encoder=PasswordEncoderFactories.createDelegatingPasswordEncoder();
            for(var username:new String[]{"requester","reviewer","dual"}) {
                var rows=jdbc.queryForList("select * from identity_account where username=?",username);
                assertEquals(1,rows.size(),username+" must be provisioned");
                var row=rows.getFirst();
                assertEquals(!username.equals("reviewer"),row.get("requester"));
                assertEquals(!username.equals("requester"),row.get("reviewer"));
                assertEquals(true,row.get("enabled"));
                var hash=(String)row.get("password_hash");
                assertNotEquals(secret,hash);assertTrue(encoder.matches(secret,hash));
                assertEquals(0L,row.get("access_version"));
            }
            assertEquals(3,jdbc.queryForObject("select count(*) from identity_account",Integer.class));
            assertEquals("127.0.0.1",context.getEnvironment().getProperty("server.address"));
        }
    }
    private String causes(Throwable ex) {
        var text=new StringBuilder();for(var t=ex;t!=null;t=t.getCause())text.append(t.getMessage());return text.toString();
    }
}
