package com.careflow.serviceops.support;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PostgresTestcontainersSetupTest extends AbstractPostgresIntegrationTest {

    @Test
    void runsOnPostgres18InsideTheContainerNotTheHostDatabase() {
        assertThat(jdbc.queryForObject("select version()", String.class)).contains("PostgreSQL 18");
        assertThat(jdbc.queryForObject("select current_user", String.class)).isEqualTo(POSTGRES.getUsername());
        assertThat(jdbc.queryForObject("select current_database()", String.class)).isEqualTo("careflow_test");
    }

    @Test
    void flywayAppliedMigrationsV1ToV9() {
        List<String> versions = jdbc.queryForList(
                "select version from flyway_schema_history where success and version is not null order by installed_rank",
                String.class);
        assertThat(versions).containsExactly("1", "2", "3", "4", "5", "6", "7", "8", "9");
    }

    @Test
    @Order(1)
    void leaksStateOnPurpose() {
        jdbc.update("update work_orders set title = 'LEAKED'");
        jdbc.update("delete from work_order_status_history");
        assertThat(jdbc.queryForObject("select count(*) from work_order_status_history", Long.class)).isZero();
    }

    @Test
    @Order(2)
    void nextTestSeesTheMigrationBaselineAgain() {
        assertThat(jdbc.queryForObject("select count(*) from work_orders where title = 'LEAKED'", Long.class)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from work_order_status_history", Long.class))
                .isEqualTo(jdbc.queryForObject("select count(*) from test_baseline.work_order_status_history", Long.class))
                .isPositive();
    }
}