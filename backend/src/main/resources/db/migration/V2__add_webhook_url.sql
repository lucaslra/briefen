-- Add webhook_url column to settings (nullable, no default)
ALTER TABLE settings ADD COLUMN webhook_url VARCHAR;
