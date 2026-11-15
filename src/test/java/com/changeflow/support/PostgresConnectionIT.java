package com.changeflow.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PostgresConnectionIT extends PostgresTestSupport {
    @Test
    void connectsToPostgres17UsingTwoIndependentBackends() throws Exception {
        try (var first = independentConnection(); var second = independentConnection()) {
            assertEquals("PostgreSQL", first.getMetaData().getDatabaseProductName());
            assertEquals(17, first.getMetaData().getDatabaseMajorVersion());
            try (var one = first.createStatement(); var two = second.createStatement();
                 var a = one.executeQuery("select pg_backend_pid(), current_setting('server_version')");
                 var b = two.executeQuery("select pg_backend_pid()")) {
                assertTrue(a.next());
                assertTrue(b.next());
                assertNotEquals(a.getInt(1), b.getInt(1));
                assertTrue(a.getString(2).startsWith("17.12"));
            }
        }
    }
}
