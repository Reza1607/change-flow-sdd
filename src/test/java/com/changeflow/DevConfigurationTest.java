package com.changeflow;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import static org.junit.jupiter.api.Assertions.*;

class DevConfigurationTest {
    @Test void absentSecretIsRejectedBeforeContainerCreation() {
        var environment=new MockEnvironment().withProperty("server.address","127.0.0.1");
        var error=assertThrows(IllegalStateException.class,()->new DevServicesConfiguration().devPostgres(environment));
        assertEquals("CHANGEFLOW_DEV_PASSWORD is required",error.getMessage());
    }
    @Test void unspecifiedBindingCannotFallBackToAllInterfaces() {
        var environment=new MockEnvironment().withProperty("CHANGEFLOW_DEV_PASSWORD",UUID.randomUUID().toString())
            .withProperty("server.address","");
        var error=assertThrows(IllegalStateException.class,()->new DevServicesConfiguration().devPostgres(environment));
        assertTrue(error.getMessage().contains("loopback"));
    }
}
