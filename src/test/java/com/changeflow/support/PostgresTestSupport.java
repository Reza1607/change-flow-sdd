package com.changeflow.support;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Real PostgreSQL fixture; credentials are ephemeral and never logged by tests. */
@Testcontainers
public abstract class PostgresTestSupport {
    @Container
    @ServiceConnection
    protected static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.12")
            .withDatabaseName("changeflow_test")
            .withUsername("test_" + UUID.randomUUID().toString().replace("-", ""))
            .withPassword(UUID.randomUUID().toString());

    protected static Connection independentConnection() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }
}
