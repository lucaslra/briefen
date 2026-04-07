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
  REGENERATE_BUTTON: 'Regenerate',
  GENERATED_IN: 'Generated in',
  CACHED: 'Cached',
  CANCEL: 'Cancel',
  GENERATED_WITH: 'Generated with',

  SOURCE_LABEL: 'Source',

  RECENT_TITLE: 'Recent Summaries',
  RECENT_SHOW: 'Show recent summaries',
  RECENT_HIDE: 'Hide recent summaries',
  RECENT_EMPTY: 'No summaries yet. Paste a URL above to get started.',

  MAKE_SHORTER: 'Make Shorter',
  MAKE_LONGER: 'Make Longer',

  SETTINGS_TITLE: 'Settings',
  SETTINGS_BACK: 'Back',
  SETTINGS_TAB_SUMMARIZATION: 'Summarization',
  SETTINGS_TAB_INTEGRATIONS: 'Integrations',
  SETTINGS_TAB_PREFERENCES: 'Preferences',
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
  SETTINGS_API_KEY_UPDATE: 'Update',
  SETTINGS_API_KEY_SAVED: 'Saved',
  SETTINGS_API_KEY_REMOVE: 'Remove',
  SETTINGS_API_KEY_ADD: 'Add API key to enable',

  SETTINGS_PROVIDER_LOCAL: 'Local (Ollama)',
  SETTINGS_PROVIDER_OPENAI: 'OpenAI',
  SETTINGS_PROVIDER_ANTHROPIC: 'Anthropic',

  SETTINGS_ANTHROPIC_KEY_LABEL: 'Anthropic API Key',
  SETTINGS_ANTHROPIC_KEY_PLACEHOLDER: 'sk-ant-...',

  SETTINGS_READECK_HEADING: 'Readeck',
  SETTINGS_READECK_SUBHEADING: 'Connect a Readeck instance to summarize your saved articles.',
  SETTINGS_READECK_URL_LABEL: 'Readeck URL',
  SETTINGS_READECK_URL_PLACEHOLDER: 'https://readeck.example.com',
  SETTINGS_READECK_KEY_LABEL: 'Readeck API Key',
  SETTINGS_READECK_KEY_PLACEHOLDER: 'Your Readeck API token',

  TAB_READECK: 'Readeck',

  READECK_SEARCH_PLACEHOLDER: 'Search articles...',
  READECK_LOADING: 'Loading articles…',
  READECK_EMPTY: 'No articles found.',
  READECK_NOT_CONFIGURED: 'Configure Readeck in Settings to browse your saved articles.',
  READECK_ERROR: 'Could not connect to Readeck.',
  READECK_RATE_LIMITED: 'Readeck is rate-limiting requests. Please wait a moment.',
  READECK_LOAD_MORE: 'Load more',

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
  ERROR_CLIPBOARD: 'Clipboard access denied. Please paste manually with Ctrl+V.',

  CONFIRM_REMOVE_OPENAI: 'Remove your OpenAI API key? OpenAI models will no longer be available.',
  CONFIRM_REMOVE_ANTHROPIC: 'Remove your Anthropic API key? Anthropic models will no longer be available.',
  CONFIRM_REMOVE_READECK: 'Remove your Readeck configuration? The Readeck tab will be hidden.',

  RECENT_LOAD_MORE: 'Load more',
  RECENT_LOADING: 'Loading\u2026',
  RECENT_EMPTY_HINT: 'Your summaries will appear here.',

  SETTINGS_MODELS_LOADING: 'Loading available models\u2026',

  READECK_SEARCH_BUTTON: 'Search',
  READECK_FETCHING: 'Loading\u2026',
  READECK_AUTH_ERROR: 'Readeck API key is invalid. Check your settings.',

  READING_LIST: 'Reading list',
  READING_LIST_FILTER_ALL: 'All',
  READING_LIST_FILTER_UNREAD: 'Unread',
  READING_LIST_FILTER_READ: 'Read',
  READING_LIST_EMPTY_ALL: 'No summaries yet. Paste an article URL to get started.',
  READING_LIST_EMPTY_UNREAD: 'You\u2019re all caught up.',
  READING_LIST_EMPTY_READ: 'Articles you\u2019ve finished will appear here.',
  READING_LIST_MARK_ALL_READ: 'Mark all as read',
  READING_LIST_ALL_MARKED: 'All marked as read.',
  READING_LIST_MARK_ALL_UNREAD: 'Mark all as unread',
  READING_LIST_ALL_MARKED_UNREAD: 'All marked as unread.',
  READING_LIST_MARK_READ: 'Mark as read',
  READING_LIST_MARK_UNREAD: 'Mark as unread',
  READING_LIST_OPEN_ARTICLE: 'Open article',
  READING_LIST_DELETE: 'Delete',
  READING_LIST_DELETE_CONFIRM: 'Delete this summary?',
  READING_LIST_UPDATE_FAILED: 'Couldn\u2019t update \u2014 try again.',
  READING_LIST_DELETE_FAILED: 'Couldn\u2019t delete \u2014 try again.',
  READING_LIST_LOAD_MORE: 'Load more',
  READING_LIST_SEARCH_PLACEHOLDER: 'Search summaries\u2026',
  READING_LIST_SEARCH_CLEAR: 'Clear search',
  READING_LIST_SEARCH_EMPTY: 'No summaries match your search.',
  READING_LIST_EXPORT: 'Export',
  READING_LIST_COPY_MD: 'Copy as Markdown',
  READING_LIST_COPY_MD_DONE: 'Copied!',

  NOTES_LABEL: 'Notes',
  NOTES_PLACEHOLDER: 'Add your thoughts...',
  NOTES_SAVED: 'Saved',

  BATCH_ADD_URL: '+ Add another URL',
  BATCH_COUNTER: '{n} of {max} URLs',
  BATCH_SUMMARIZE: 'Summarize {n} articles',
  BATCH_PROGRESS_TITLE: 'Summarizing {n} articles\u2026',
  BATCH_STATUS_QUEUED: 'Queued',
  BATCH_STATUS_PROCESSING: 'Summarizing\u2026',
  BATCH_STATUS_DONE: 'Done',
  BATCH_STATUS_ERROR: 'Failed',
  BATCH_COMPLETE: '{done} of {total} articles summarized.',
  BATCH_VIEW_READING_LIST: 'View in reading list',
  BATCH_DISMISS: 'Done',

  SETTINGS_VERSION_PREFIX: 'Briefen',
}

/**
 * Maximum number of times each length adjustment button can be clicked.
 * Set to a higher value to allow multiple adjustments.
 */
export const MAX_LENGTH_ADJUSTMENTS = 1
