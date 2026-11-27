package com.careflow.serviceops.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base class for every full-context test. One PostgreSQL 18 container per JVM, shared by all
 * Spring contexts. Flyway applies V1-V9 on context start. The DB is reset to the migration
 * baseline before every test.
 *
 * Unit tests must NOT extend this class, so they run without Docker.
 * Run only unit tests:  mvn test -DexcludedGroups=integration
 */
@Tag("integration")
@SpringBootTest
public abstract class AbstractPostgresIntegrationTest {

    // Keep in sync with docker-compose.yml
    private static final String IMAGE = "postgres:18.6-alpine";

    protected static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(IMAGE)
            .withDatabaseName("careflow_test")
            .withUsername("careflow_test")
            .withPassword("careflow_test_" + Long.toHexString(System.nanoTime()));

    static {
        POSTGRES.start(); // stopped by Testcontainers' Ryuk when the JVM exits
    }

    /** Highest precedence: overrides application.yml AND any DB_URL / DB_* env vars on the host. */
    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    protected JdbcTemplate jdbc;

    @BeforeEach
    void resetDatabaseToMigrationBaseline() {
        DatabaseFixtures.reset(jdbc);
    }
}