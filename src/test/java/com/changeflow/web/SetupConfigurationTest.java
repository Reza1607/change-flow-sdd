package com.changeflow.web;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;

class SetupConfigurationTest {

    private List<PropertySource<?>> configuration() throws Exception {
        return new YamlPropertySourceLoader().load("setup", new ClassPathResource("application.yaml"));
    }

    @Test
    void sharedEnvironmentRequiresExternalSecretsAndTls() throws Exception {
        var environment = new MockEnvironment();
        environment.getPropertySources().addFirst(configuration().getFirst());

        assertEquals("validate", environment.getProperty("spring.jpa.hibernate.ddl-auto"));
        assertEquals("false", environment.getProperty("spring.jpa.open-in-view"));
        assertEquals("true", environment.getProperty("spring.flyway.enabled"));
        assertEquals("true", environment.getProperty("server.ssl.enabled"));
        for (var key : List.of("spring.datasource.url", "spring.datasource.username",
                "spring.datasource.password")) {
            assertNull(environment.getProperty(key), key);
        }
        assertThrows(IllegalArgumentException.class, () -> environment.getProperty("server.ssl.bundle"));
    }

    @Test
    void onlyDevOverridesTlsAndPinsLoopback() throws Exception {
        var sources = configuration();
        assertEquals(2, sources.size());
        var environment = new MockEnvironment();
        environment.getPropertySources().addFirst(sources.getFirst());
        environment.getPropertySources().addFirst(sources.getLast());

        assertEquals("dev", environment.getProperty("spring.config.activate.on-profile"));
        assertEquals("127.0.0.1", environment.getProperty("server.address"));
        assertEquals("false", environment.getProperty("server.ssl.enabled"));
        assertEquals("never", environment.getProperty("server.error.include-stacktrace"));
    }
}
