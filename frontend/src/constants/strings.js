/**
 * All user-facing strings centralized for future i18n support.
 * To add internationalization, replace this module with a library like
 * react-intl or i18next and swap these constants for translation keys.
 */
export const STRINGS = {
  APP_TITLE: 'Briefen',
  APP_SUBTITLE: 'Article summaries, instantly.',

  INPUT_PLACEHOLDER: 'Paste an article URL...',
  TEXT_PLACEHOLDER: 'Paste the article content here...',
  TITLE_PLACEHOLDER: 'Article title (optional)',
  SUMMARIZE_BUTTON: 'Summarize',
  PASTE_BUTTON: 'Paste',
  LOADING_TEXT: 'Reading and summarizing\u2026',

  TAB_URL: 'URL',
  TAB_TEXT: 'Paste Content',

  COPY_BUTTON: 'Copy summary',
  COPIED_TEXT: 'Copied!',
  GENERATED_IN: 'Generated in',

  SOURCE_LABEL: 'Source',

  RECENT_TITLE: 'Recent Summaries',
  RECENT_SHOW: 'Show recent summaries',
  RECENT_HIDE: 'Hide recent summaries',
  RECENT_EMPTY: 'No summaries yet. Paste a URL above to get started.',

  MAKE_SHORTER: 'Make Shorter',
  MAKE_LONGER: 'Make Longer',

  SETTINGS_TITLE: 'Settings',
  SETTINGS_BACK: 'Back',
  SETTINGS_LENGTH_HEADING: 'Summary Length',
  SETTINGS_LENGTH_SUBHEADING: 'Choose the default length for new summaries.',
  SETTINGS_LENGTH_SHORT: 'Short',
  SETTINGS_LENGTH_SHORT_DESC: '1-2 concise paragraphs with only the essential points.',
  SETTINGS_LENGTH_DEFAULT: 'Standard',
  SETTINGS_LENGTH_DEFAULT_DESC: '3-6 paragraphs covering key points and takeaways.',
  SETTINGS_LENGTH_LONG: 'Detailed',
  SETTINGS_LENGTH_LONG_DESC: '6-10 paragraphs with thorough coverage of all arguments and nuances.',

  SETTINGS_MODEL_HEADING: 'LLM Model',
  SETTINGS_MODEL_SUBHEADING: 'Choose which model generates your summaries.',

  SETTINGS_API_KEYS_HEADING: 'API Keys',
  SETTINGS_API_KEYS_SUBHEADING: 'Connect cloud providers to unlock additional models.',
  SETTINGS_OPENAI_KEY_LABEL: 'OpenAI API Key',
  SETTINGS_OPENAI_KEY_PLACEHOLDER: 'sk-...',
  SETTINGS_API_KEY_SAVE: 'Save',
  SETTINGS_API_KEY_SAVED: 'Saved',
  SETTINGS_API_KEY_REMOVE: 'Remove',
  SETTINGS_API_KEY_ADD: 'Add API key to enable',

  SETTINGS_PROVIDER_LOCAL: 'Local (Ollama)',
  SETTINGS_PROVIDER_OPENAI: 'OpenAI',

  SETTINGS_NOTIFICATIONS_HEADING: 'Notifications',
  SETTINGS_NOTIFICATIONS_SUBHEADING: 'Get a browser notification when a summary finishes generating.',
  SETTINGS_NOTIFICATIONS_ENABLE: 'Enable notifications',
  SETTINGS_NOTIFICATIONS_DENIED: 'Notifications blocked by browser. Allow them in your browser settings.',
  SETTINGS_NOTIFICATIONS_UNSUPPORTED: 'Your browser does not support notifications.',

  NOTIFICATION_DONE_TITLE: 'Summary ready',
  NOTIFICATION_DONE_BODY: 'Your article summary has been generated.',

  ERROR_INVALID_URL: 'Please enter a valid HTTP or HTTPS URL.',
  ERROR_EMPTY_TEXT: 'Please paste some article content to summarize.',
  ERROR_GENERIC: 'Something went wrong. Please try again.',
  ERROR_NETWORK: 'Could not reach the server. Is it running?',
  ERROR_TIMEOUT: 'The request timed out. The article may be too long.',
  ERROR_EXTRACTION: 'Could not extract article content from this URL.',
  ERROR_BOT_PROTECTION: 'This website uses bot protection and cannot be summarized. Try a different source for the same article.',
}

/**
 * Maximum number of times each length adjustment button can be clicked.
 * Set to a higher value to allow multiple adjustments.
 */
export const MAX_LENGTH_ADJUSTMENTS = 1
