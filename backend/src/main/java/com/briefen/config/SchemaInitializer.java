package com.briefen.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
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
@ConditionalOnProperty(name = "briefen.db.type", havingValue = "sqlite", matchIfMissing = true)
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
            addOidcColumnsIfMissing(conn);
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

    /**
     * Adds the nullable OIDC identity columns (email, oidc_issuer, oidc_subject) to the
     * users table if missing, plus a UNIQUE index on (oidc_issuer, oidc_subject).
     *
     * <p>Nullable columns are normally added by Hibernate's ddl-auto:update, but we add
     * them defensively here (SQLite is quiet about failures) before creating the index.
     * SQLite and Postgres both treat NULLs as distinct in unique indexes, so the many
     * password-only rows with (NULL, NULL) never collide.
     */
    private void addOidcColumnsIfMissing(Connection conn) throws Exception {
        if (!tableExists(conn, "users")) return;
        addColumnIfMissing(conn, "users", "email", "ALTER TABLE users ADD COLUMN email VARCHAR");
        addColumnIfMissing(conn, "users", "oidc_issuer", "ALTER TABLE users ADD COLUMN oidc_issuer VARCHAR");
        addColumnIfMissing(conn, "users", "oidc_subject", "ALTER TABLE users ADD COLUMN oidc_subject VARCHAR");
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_users_oidc ON users(oidc_issuer, oidc_subject)");
        }
    }

    private void addColumnIfMissing(Connection conn, String table, String column, String ddl) throws Exception {
        if (columnExists(conn, table, column)) return;
        log.info("Adding {} column to {} table", column, table);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(ddl);
        }
    }

    private boolean columnExists(Connection conn, String table, String column) throws Exception {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) {
                if (column.equals(rs.getString("name"))) return true;
            }
        }
        return false;
    }

    private boolean tableExists(Connection conn, String tableName) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM sqlite_master WHERE type='table' AND name=?")) {
            ps.setString(1, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private String getSummariesCreateSql(Connection conn) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT sql FROM sqlite_master WHERE type='table' AND name=?")) {
            ps.setString(1, "summaries");
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("sql") : null;
            }
        }
    }
}
