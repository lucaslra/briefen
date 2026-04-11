-- Add custom_prompt column to settings for per-user prompt customization.
-- When non-null, this prompt replaces the built-in summarization system prompt.
ALTER TABLE settings ADD COLUMN custom_prompt TEXT;
