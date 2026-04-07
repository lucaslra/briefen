CREATE TABLE IF NOT EXISTS summaries (
    id          VARCHAR PRIMARY KEY,
    url         VARCHAR UNIQUE,
    title       VARCHAR,
    summary     TEXT,
    model_used  VARCHAR,
    created_at  TIMESTAMP,
    is_read     BOOLEAN NOT NULL DEFAULT 0,
    saved_at    TIMESTAMP,
    notes       TEXT
);

CREATE TABLE IF NOT EXISTS settings (
    id                   VARCHAR PRIMARY KEY,
    default_length       VARCHAR,
    model                VARCHAR,
    notifications_enabled BOOLEAN NOT NULL DEFAULT 0,
    openai_api_key       VARCHAR,
    anthropic_api_key    VARCHAR,
    readeck_api_key      VARCHAR,
    readeck_url          VARCHAR,
    updated_at           TIMESTAMP
);
