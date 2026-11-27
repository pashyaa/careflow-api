package com.careflow.serviceops.support;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

/**
 * Deterministic fixtures = whatever V1-V9 seed. The first call snapshots every table into the
 * test_baseline schema (before any test has mutated anything); every call restores from it.
 */
final class DatabaseFixtures {

    /** Parents before children, so inserts satisfy foreign keys. */
    private static final List<String> TABLES = List.of(
            "organizations", "roles", "users", "user_roles",
            "service_sites", "assets", "technicians",
            "sla_policies", "sla_holidays", "reason_codes",
            "work_orders", "work_order_status_history");

    private static boolean snapshotTaken = false;

    private DatabaseFixtures() {}

    static synchronized void reset(JdbcTemplate jdbc) {
        if (!snapshotTaken) {
            jdbc.execute("CREATE SCHEMA IF NOT EXISTS test_baseline");
            for (String t : TABLES) {
                jdbc.execute("DROP TABLE IF EXISTS test_baseline." + t);
                jdbc.execute("CREATE TABLE test_baseline." + t + " AS TABLE public." + t);
            }
            snapshotTaken = true;
        }
        jdbc.execute("TRUNCATE TABLE " + String.join(", ", TABLES));
        for (String t : TABLES) {
            jdbc.execute("INSERT INTO public." + t + " SELECT * FROM test_baseline." + t);
        }
    }
}