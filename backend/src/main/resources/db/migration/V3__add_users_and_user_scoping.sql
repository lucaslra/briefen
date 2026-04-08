-- Create users table
CREATE TABLE IF NOT EXISTS users (
    id           VARCHAR PRIMARY KEY,
    username     VARCHAR NOT NULL UNIQUE,
    password_hash VARCHAR NOT NULL,
    role         VARCHAR NOT NULL DEFAULT 'USER',
    created_at   TIMESTAMP NOT NULL
);

-- Recreate summaries with user_id column and per-user URL uniqueness.
-- (SQLite does not support ALTER TABLE DROP CONSTRAINT, so we recreate the table.)
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
);

INSERT INTO summaries_new
    SELECT id, NULL AS user_id, url, title, summary, model_used, created_at, is_read, saved_at, notes
    FROM summaries;

DROP TABLE summaries;
ALTER TABLE summaries_new RENAME TO summaries;
