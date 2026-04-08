package com.briefen.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Runs after Hibernate's ddl-auto:update but before ApplicationReadyEvent listeners.
 *
 * Handles schema migrations that Hibernate cannot perform on SQLite because
 * SQLite does not support ALTER TABLE DROP CONSTRAINT.
 *
 * Currently manages: changing summaries UNIQUE(url) → UNIQUE(url, user_id)
 * to support per-user article libraries (multi-user support).
 */
@Component
@Order(Integer.MIN_VALUE)
public class SchemaInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SchemaInitializer.class);

    private final DataSource dataSource;

    public SchemaInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            migrateSummariesConstraint(conn);
            addMainAdminColumnIfMissing(conn);
        }
    }

    /**
     * If summaries has the old UNIQUE(url) constraint (without user_id), recreate the
     * table with UNIQUE(url, user_id). Idempotent — safe to run on every startup.
     */
    private void migrateSummariesConstraint(Connection conn) throws Exception {
        String tableSql = getSummariesCreateSql(conn);
        if (tableSql == null) return; // table doesn't exist yet — Hibernate will create it correctly

        boolean needsMigration = tableSql.toUpperCase().contains("UNIQUE")
                && !tableSql.toLowerCase().contains("user_id");
        if (!needsMigration) return;

        log.info("Migrating summaries UNIQUE constraint from (url) to (url, user_id)");
        boolean autoCommit = conn.getAutoCommit();
        conn.setAutoCommit(false);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("""
                    CREATE TABLE summaries_new (
                        id         VARCHAR PRIMARY KEY,
                        user_id    VARCHAR,
                        url        VARCHAR,
                        title      VARCHAR,
                        summary    TEXT,
                        model_used VARCHAR,
                        created_at TIMESTAMP,
                        is_read    BOOLEAN NOT NULL DEFAULT 0,
                        saved_at   TIMESTAMP,
                        notes      TEXT,
                        UNIQUE (url, user_id)
                    )
                    """);
            stmt.execute("""
                    INSERT INTO summaries_new
                        SELECT id, user_id, url, title, summary, model_used,
                               created_at, is_read, saved_at, notes
                        FROM summaries
                    """);
            stmt.execute("DROP TABLE summaries");
            stmt.execute("ALTER TABLE summaries_new RENAME TO summaries");
            conn.commit();
            log.info("Summaries constraint migration complete");
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(autoCommit);
        }
    }

    /**
     * Adds the main_admin column to the users table if it doesn't already exist.
     * Required because SQLite's ALTER TABLE ADD COLUMN rejects NOT NULL columns
     * without a DEFAULT, so Hibernate's ddl-auto:update silently skips it.
     */
    private void addMainAdminColumnIfMissing(Connection conn) throws Exception {
        if (!tableExists(conn, "users")) return;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info(users)")) {
            while (rs.next()) {
                if ("main_admin".equals(rs.getString("name"))) return; // already exists
            }
        }
        log.info("Adding main_admin column to users table");
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("ALTER TABLE users ADD COLUMN main_admin BOOLEAN NOT NULL DEFAULT 0");
        }
        log.info("main_admin column added");
    }

    private boolean tableExists(Connection conn, String tableName) throws Exception {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT 1 FROM sqlite_master WHERE type='table' AND name='" + tableName + "'")) {
            return rs.next();
        }
    }

    private String getSummariesCreateSql(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT sql FROM sqlite_master WHERE type='table' AND name='summaries'")) {
            return rs.next() ? rs.getString("sql") : null;
        }
    }
}
