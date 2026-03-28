/**
 * All user-facing strings centralized for future i18n support.
 * To add internationalization, replace this module with a library like
 * react-intl or i18next and swap these constants for translation keys.
 */
export const STRINGS = {
  APP_TITLE: 'Briefly',
  APP_SUBTITLE: 'Article summaries, instantly.',

  INPUT_PLACEHOLDER: 'Paste an article URL...',
  SUMMARIZE_BUTTON: 'Summarize',
  PASTE_BUTTON: 'Paste',
  LOADING_TEXT: 'Reading and summarizing\u2026',

  COPY_BUTTON: 'Copy summary',
  COPIED_TEXT: 'Copied!',

  SOURCE_LABEL: 'Source',

  RECENT_TITLE: 'Recent Summaries',
  RECENT_SHOW: 'Show recent summaries',
  RECENT_HIDE: 'Hide recent summaries',
  RECENT_EMPTY: 'No summaries yet. Paste a URL above to get started.',

  MAKE_SHORTER: 'Make Shorter',
  MAKE_LONGER: 'Make Longer',

  ERROR_INVALID_URL: 'Please enter a valid HTTP or HTTPS URL.',
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
