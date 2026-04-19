// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for English (`en`).
class AppLocalizationsEn extends AppLocalizations {
  AppLocalizationsEn([String locale = 'en']) : super(locale);

  @override
  String get appName => 'Briefen';

  @override
  String get login => 'Sign In';

  @override
  String get logout => 'Logout';

  @override
  String get serverUrl => 'Server URL';

  @override
  String get serverUrlHint => 'https://briefen.example.com';

  @override
  String get username => 'Username';

  @override
  String get password => 'Password';

  @override
  String get loginError => 'Invalid credentials or server unreachable';

  @override
  String get setupTitle => 'Create Admin Account';

  @override
  String get setupSubtitle => 'Set up your Briefen instance';

  @override
  String get confirmPassword => 'Confirm Password';

  @override
  String get createAccount => 'Create Account';

  @override
  String get passwordsDoNotMatch => 'Passwords do not match';

  @override
  String get passwordTooShort => 'Password must be at least 8 characters';

  @override
  String get passwordRequirements =>
      'Must include uppercase, lowercase, digit, and special character';

  @override
  String get setupError => 'Failed to create account';

  @override
  String get summarize => 'Summarize';

  @override
  String get summarizeHint => 'Paste article URL...';

  @override
  String get summarizing => 'Summarizing...';

  @override
  String get summarizeError => 'Failed to summarize article';

  @override
  String get readingList => 'Reading List';

  @override
  String get settings => 'Settings';

  @override
  String get all => 'All';

  @override
  String get unread => 'Unread';

  @override
  String get read => 'Read';

  @override
  String get search => 'Search';

  @override
  String get noSummaries => 'No summaries yet';

  @override
  String get noResults => 'No results found';

  @override
  String get markAsRead => 'Mark as read';

  @override
  String get markAsUnread => 'Mark as unread';

  @override
  String get delete => 'Delete';

  @override
  String get deleteConfirmTitle => 'Delete Summary';

  @override
  String get deleteConfirmMessage =>
      'Are you sure you want to delete this summary?';

  @override
  String get cancel => 'Cancel';

  @override
  String get openArticle => 'Open Article';

  @override
  String get share => 'Share';

  @override
  String get copyToClipboard => 'Copy to clipboard';

  @override
  String get copied => 'Copied to clipboard';

  @override
  String get notes => 'Notes';

  @override
  String get notesHint => 'Add a note...';

  @override
  String get tags => 'Tags';

  @override
  String get addTag => 'Add tag...';

  @override
  String get model => 'Model';

  @override
  String get source => 'Source';

  @override
  String get savedAt => 'Saved';

  @override
  String get theme => 'Theme';

  @override
  String get darkMode => 'Dark Mode';

  @override
  String get lightMode => 'Light Mode';

  @override
  String get version => 'Version';

  @override
  String get server => 'Server';

  @override
  String get account => 'Account';

  @override
  String get retry => 'Retry';

  @override
  String get networkError => 'Network error. Check your connection.';

  @override
  String get timeoutError => 'Request timed out. The server may be busy.';

  @override
  String get unknownError => 'Something went wrong';

  @override
  String get urlTab => 'URL';

  @override
  String get textTab => 'Text';

  @override
  String get textInputHint => 'Paste or type article text...';

  @override
  String get titleHint => 'Title (optional)';

  @override
  String get summarizeText => 'Summarize Text';

  @override
  String get saveNotes => 'Save';

  @override
  String get notesUpdated => 'Notes saved';

  @override
  String get removeTag => 'Remove tag';

  @override
  String get tagHint => 'New tag';

  @override
  String get tagsUpdated => 'Tags updated';

  @override
  String get export => 'Export';

  @override
  String get exportReadingList => 'Export reading list';

  @override
  String get markAllRead => 'Mark all as read';

  @override
  String get markAllUnread => 'Mark all as unread';

  @override
  String get bulkUpdated => 'Reading list updated';

  @override
  String get batchTab => 'Batch';

  @override
  String get batchInputHint => 'Paste URLs, one per line...';

  @override
  String get batchSummarize => 'Summarize All';

  @override
  String batchProgress(int current, int total) {
    return 'Summarizing $current of $total…';
  }

  @override
  String batchComplete(int count) {
    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: '$count summaries ready',
      one: '$count summary ready',
    );
    return '$_temp0';
  }

  @override
  String get makeShorter => 'Make shorter';

  @override
  String get makeLonger => 'Make longer';

  @override
  String get regenerate => 'Regenerate';

  @override
  String get adjustingSummary => 'Adjusting…';

  @override
  String get filterByTag => 'Filter by tag';

  @override
  String get summarizationSettings => 'Summarization';

  @override
  String get defaultLength => 'Default Length';

  @override
  String get lengthDefault => 'Default';

  @override
  String get lengthShort => 'Short';

  @override
  String get lengthMedium => 'Medium';

  @override
  String get lengthLong => 'Long';

  @override
  String get customPrompt => 'Custom Prompt';

  @override
  String get customPromptHint => 'Override the default summarization prompt...';

  @override
  String get integrations => 'Integrations';

  @override
  String get openaiApiKey => 'OpenAI API Key';

  @override
  String get anthropicApiKey => 'Anthropic API Key';

  @override
  String get readeckApiKey => 'Readeck API Key';

  @override
  String get readeckUrl => 'Readeck URL';

  @override
  String get webhookUrl => 'Webhook URL';

  @override
  String get keyNotSet => 'Not set';

  @override
  String get settingsSaved => 'Settings saved';

  @override
  String get language => 'Language';

  @override
  String get selectModel => 'Select Model';

  @override
  String get appearance => 'Appearance';

  @override
  String get notificationsEnabled => 'Notifications';

  @override
  String get about => 'About';

  @override
  String get manageUsers => 'Manage Users';

  @override
  String get createUser => 'Create User';

  @override
  String get deleteUser => 'Delete User';

  @override
  String deleteUserConfirm(String username) {
    return 'Delete user \"$username\"? This cannot be undone.';
  }

  @override
  String get roleLabel => 'Role';

  @override
  String get adminRole => 'Admin';

  @override
  String get userRole => 'User';

  @override
  String get userCreated => 'User created';

  @override
  String get userDeleted => 'User deleted';

  @override
  String get userCreateError => 'Failed to create user';

  @override
  String get administration => 'Administration';

  @override
  String get offlineCached => 'You\'re offline — showing cached content';

  @override
  String get readeckTab => 'Readeck';

  @override
  String get readeckNotConfigured =>
      'Readeck is not configured.\nSet the URL and API key in Settings.';

  @override
  String get loadMore => 'Load more';

  @override
  String get biometricAuth => 'Biometric Authentication';

  @override
  String get biometricAuthSubtitle => 'Lock app when backgrounded';

  @override
  String get biometricUnlock => 'Unlock';

  @override
  String get biometricNotAvailable => 'Not available on this device';

  @override
  String get recentSummaries => 'Recent';

  @override
  String get summarizeTab => 'Summarize';

  @override
  String get readingListTab => 'Reading List';

  @override
  String get settingsTab => 'Settings';

  @override
  String get urlInvalid =>
      'Enter a valid URL starting with http:// or https://';

  @override
  String get languageEnglish => 'English';

  @override
  String get languagePortuguese => 'Português';
}
