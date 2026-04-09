-- Add updated_at timestamp and main_admin flag to users table.
ALTER TABLE users ADD COLUMN updated_at TIMESTAMP;
ALTER TABLE users ADD COLUMN main_admin BOOLEAN NOT NULL DEFAULT 0;
