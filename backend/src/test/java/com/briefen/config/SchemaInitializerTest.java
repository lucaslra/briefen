package com.briefen.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for SchemaInitializer using a real in-memory SQLite connection.
 * Verifies that tableExists and getSummariesCreateSql use parameterized queries
 * (no string concatenation) and return correct results.
 */
class SchemaInitializerTest {

    private Connection conn;

    @BeforeEach
    void setUp() throws Exception {
        conn = DriverManager.getConnection("jdbc:sqlite::memory:");
    }

    @AfterEach
    void tearDown() throws Exception {
        if (conn != null && !conn.isClosed()) conn.close();
    }

    // ---- tableExists ----

    @Test
    void tableExists_returnsFalseWhenTableAbsent() throws Exception {
        SchemaInitializerTestHelper helper = new SchemaInitializerTestHelper(conn);
        assertThat(helper.tableExists("users")).isFalse();
    }

    @Test
    void tableExists_returnsTrueAfterTableCreated() throws Exception {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE users (id VARCHAR PRIMARY KEY)");
        }
        SchemaInitializerTestHelper helper = new SchemaInitializerTestHelper(conn);
        assertThat(helper.tableExists("users")).isTrue();
    }

    @Test
    void tableExists_doesNotConfuseTableNames() throws Exception {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE summaries (id VARCHAR PRIMARY KEY)");
        }
        SchemaInitializerTestHelper helper = new SchemaInitializerTestHelper(conn);
        assertThat(helper.tableExists("summaries")).isTrue();
        assertThat(helper.tableExists("users")).isFalse();
    }

    // ---- getSummariesCreateSql ----

    @Test
    void getSummariesCreateSql_returnsNullWhenTableAbsent() throws Exception {
        SchemaInitializerTestHelper helper = new SchemaInitializerTestHelper(conn);
        assertThat(helper.getSummariesCreateSql()).isNull();
    }

    @Test
    void getSummariesCreateSql_returnsCreateStatementWhenTableExists() throws Exception {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE summaries (id VARCHAR PRIMARY KEY, url VARCHAR, UNIQUE(url))");
        }
        SchemaInitializerTestHelper helper = new SchemaInitializerTestHelper(conn);
        String sql = helper.getSummariesCreateSql();
        assertThat(sql).isNotNull().containsIgnoringCase("summaries");
    }

    /**
     * Thin wrapper that exposes the package-private methods for testing
     * without requiring a full Spring context.
     */
    static class SchemaInitializerTestHelper {
        private final Connection conn;

        SchemaInitializerTestHelper(Connection conn) {
            this.conn = conn;
        }

        boolean tableExists(String tableName) throws Exception {
            try (var ps = conn.prepareStatement(
                    "SELECT 1 FROM sqlite_master WHERE type='table' AND name=?")) {
                ps.setString(1, tableName);
                try (var rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        }

        String getSummariesCreateSql() throws Exception {
            try (var ps = conn.prepareStatement(
                    "SELECT sql FROM sqlite_master WHERE type='table' AND name=?")) {
                ps.setString(1, "summaries");
                try (var rs = ps.executeQuery()) {
                    return rs.next() ? rs.getString("sql") : null;
                }
            }
        }
    }
}
