import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:intl/intl.dart' as intl;

import 'app_localizations_en.dart';
import 'app_localizations_pt.dart';

// ignore_for_file: type=lint

/// Callers can lookup localized strings with an instance of AppLocalizations
/// returned by `AppLocalizations.of(context)`.
///
/// Applications need to include `AppLocalizations.delegate()` in their app's
/// `localizationDelegates` list, and the locales they support in the app's
/// `supportedLocales` list. For example:
///
/// ```dart
/// import 'generated/app_localizations.dart';
///
/// return MaterialApp(
///   localizationsDelegates: AppLocalizations.localizationsDelegates,
///   supportedLocales: AppLocalizations.supportedLocales,
///   home: MyApplicationHome(),
/// );
/// ```
///
/// ## Update pubspec.yaml
///
/// Please make sure to update your pubspec.yaml to include the following
/// packages:
///
/// ```yaml
/// dependencies:
///   # Internationalization support.
///   flutter_localizations:
///     sdk: flutter
///   intl: any # Use the pinned version from flutter_localizations
///
///   # Rest of dependencies
/// ```
///
/// ## iOS Applications
///
/// iOS applications define key application metadata, including supported
/// locales, in an Info.plist file that is built into the application bundle.
/// To configure the locales supported by your app, you’ll need to edit this
/// file.
///
/// First, open your project’s ios/Runner.xcworkspace Xcode workspace file.
/// Then, in the Project Navigator, open the Info.plist file under the Runner
/// project’s Runner folder.
///
/// Next, select the Information Property List item, select Add Item from the
/// Editor menu, then select Localizations from the pop-up menu.
///
/// Select and expand the newly-created Localizations item then, for each
/// locale your application supports, add a new item and select the locale
/// you wish to add from the pop-up menu in the Value field. This list should
/// be consistent with the languages listed in the AppLocalizations.supportedLocales
/// property.
abstract class AppLocalizations {
  AppLocalizations(String locale)
    : localeName = intl.Intl.canonicalizedLocale(locale.toString());

  final String localeName;

  static AppLocalizations? of(BuildContext context) {
    return Localizations.of<AppLocalizations>(context, AppLocalizations);
  }

  static const LocalizationsDelegate<AppLocalizations> delegate =
      _AppLocalizationsDelegate();

  /// A list of this localizations delegate along with the default localizations
  /// delegates.
  ///
  /// Returns a list of localizations delegates containing this delegate along with
  /// GlobalMaterialLocalizations.delegate, GlobalCupertinoLocalizations.delegate,
  /// and GlobalWidgetsLocalizations.delegate.
  ///
  /// Additional delegates can be added by appending to this list in
  /// MaterialApp. This list does not have to be used at all if a custom list
  /// of delegates is preferred or required.
  static const List<LocalizationsDelegate<dynamic>> localizationsDelegates =
      <LocalizationsDelegate<dynamic>>[
        delegate,
        GlobalMaterialLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
      ];

  /// A list of this localizations delegate's supported locales.
  static const List<Locale> supportedLocales = <Locale>[
    Locale('en'),
    Locale('pt'),
    Locale('pt', 'BR'),
  ];

  /// No description provided for @appName.
  ///
  /// In en, this message translates to:
  /// **'Briefen'**
  String get appName;

  /// No description provided for @login.
  ///
  /// In en, this message translates to:
  /// **'Sign In'**
  String get login;

  /// No description provided for @logout.
  ///
  /// In en, this message translates to:
  /// **'Logout'**
  String get logout;

  /// No description provided for @serverUrl.
  ///
  /// In en, this message translates to:
  /// **'Server URL'**
  String get serverUrl;

  /// No description provided for @serverUrlHint.
  ///
  /// In en, this message translates to:
  /// **'https://briefen.example.com'**
  String get serverUrlHint;

  /// No description provided for @username.
  ///
  /// In en, this message translates to:
  /// **'Username'**
  String get username;

  /// No description provided for @password.
  ///
  /// In en, this message translates to:
  /// **'Password'**
  String get password;

  /// No description provided for @loginError.
  ///
  /// In en, this message translates to:
  /// **'Invalid credentials or server unreachable'**
  String get loginError;

  /// No description provided for @setupTitle.
  ///
  /// In en, this message translates to:
  /// **'Create Admin Account'**
  String get setupTitle;

  /// No description provided for @setupSubtitle.
  ///
  /// In en, this message translates to:
  /// **'Set up your Briefen instance'**
  String get setupSubtitle;

  /// No description provided for @confirmPassword.
  ///
  /// In en, this message translates to:
  /// **'Confirm Password'**
  String get confirmPassword;

  /// No description provided for @createAccount.
  ///
  /// In en, this message translates to:
  /// **'Create Account'**
  String get createAccount;

  /// No description provided for @passwordsDoNotMatch.
  ///
  /// In en, this message translates to:
  /// **'Passwords do not match'**
  String get passwordsDoNotMatch;

  /// No description provided for @passwordTooShort.
  ///
  /// In en, this message translates to:
  /// **'Password must be at least 8 characters'**
  String get passwordTooShort;

  /// No description provided for @passwordRequirements.
  ///
  /// In en, this message translates to:
  /// **'Must include uppercase, lowercase, digit, and special character'**
  String get passwordRequirements;

  /// No description provided for @setupError.
  ///
  /// In en, this message translates to:
  /// **'Failed to create account'**
  String get setupError;

  /// No description provided for @summarize.
  ///
  /// In en, this message translates to:
  /// **'Summarize'**
  String get summarize;

  /// No description provided for @summarizeHint.
  ///
  /// In en, this message translates to:
  /// **'Paste article URL...'**
  String get summarizeHint;

  /// No description provided for @summarizing.
  ///
  /// In en, this message translates to:
  /// **'Summarizing...'**
  String get summarizing;

  /// No description provided for @summarizeError.
  ///
  /// In en, this message translates to:
  /// **'Failed to summarize article'**
  String get summarizeError;

  /// No description provided for @readingList.
  ///
  /// In en, this message translates to:
  /// **'Reading List'**
  String get readingList;

  /// No description provided for @settings.
  ///
  /// In en, this message translates to:
  /// **'Settings'**
  String get settings;

  /// No description provided for @all.
  ///
  /// In en, this message translates to:
  /// **'All'**
  String get all;

  /// No description provided for @unread.
  ///
  /// In en, this message translates to:
  /// **'Unread'**
  String get unread;

  /// No description provided for @read.
  ///
  /// In en, this message translates to:
  /// **'Read'**
  String get read;

  /// No description provided for @search.
  ///
  /// In en, this message translates to:
  /// **'Search'**
  String get search;

  /// No description provided for @noSummaries.
  ///
  /// In en, this message translates to:
  /// **'No summaries yet'**
  String get noSummaries;

  /// No description provided for @noResults.
  ///
  /// In en, this message translates to:
  /// **'No results found'**
  String get noResults;

  /// No description provided for @markAsRead.
  ///
  /// In en, this message translates to:
  /// **'Mark as read'**
  String get markAsRead;

  /// No description provided for @markAsUnread.
  ///
  /// In en, this message translates to:
  /// **'Mark as unread'**
  String get markAsUnread;

  /// No description provided for @delete.
  ///
  /// In en, this message translates to:
  /// **'Delete'**
  String get delete;

  /// No description provided for @deleteConfirmTitle.
  ///
  /// In en, this message translates to:
  /// **'Delete Summary'**
  String get deleteConfirmTitle;

  /// No description provided for @deleteConfirmMessage.
  ///
  /// In en, this message translates to:
  /// **'Are you sure you want to delete this summary?'**
  String get deleteConfirmMessage;

  /// No description provided for @cancel.
  ///
  /// In en, this message translates to:
  /// **'Cancel'**
  String get cancel;

  /// No description provided for @openArticle.
  ///
  /// In en, this message translates to:
  /// **'Open Article'**
  String get openArticle;

  /// No description provided for @share.
  ///
  /// In en, this message translates to:
  /// **'Share'**
  String get share;

  /// No description provided for @copyToClipboard.
  ///
  /// In en, this message translates to:
  /// **'Copy to clipboard'**
  String get copyToClipboard;

  /// No description provided for @copied.
  ///
  /// In en, this message translates to:
  /// **'Copied to clipboard'**
  String get copied;

  /// No description provided for @notes.
  ///
  /// In en, this message translates to:
  /// **'Notes'**
  String get notes;

  /// No description provided for @notesHint.
  ///
  /// In en, this message translates to:
  /// **'Add a note...'**
  String get notesHint;

  /// No description provided for @tags.
  ///
  /// In en, this message translates to:
  /// **'Tags'**
  String get tags;

  /// No description provided for @addTag.
  ///
  /// In en, this message translates to:
  /// **'Add tag...'**
  String get addTag;

  /// No description provided for @model.
  ///
  /// In en, this message translates to:
  /// **'Model'**
  String get model;

  /// No description provided for @source.
  ///
  /// In en, this message translates to:
  /// **'Source'**
  String get source;

  /// No description provided for @savedAt.
  ///
  /// In en, this message translates to:
  /// **'Saved'**
  String get savedAt;

  /// No description provided for @theme.
  ///
  /// In en, this message translates to:
  /// **'Theme'**
  String get theme;

  /// No description provided for @darkMode.
  ///
  /// In en, this message translates to:
  /// **'Dark Mode'**
  String get darkMode;

  /// No description provided for @lightMode.
  ///
  /// In en, this message translates to:
  /// **'Light Mode'**
  String get lightMode;

  /// No description provided for @version.
  ///
  /// In en, this message translates to:
  /// **'Version'**
  String get version;

  /// No description provided for @server.
  ///
  /// In en, this message translates to:
  /// **'Server'**
  String get server;

  /// No description provided for @account.
  ///
  /// In en, this message translates to:
  /// **'Account'**
  String get account;

  /// No description provided for @retry.
  ///
  /// In en, this message translates to:
  /// **'Retry'**
  String get retry;

  /// No description provided for @networkError.
  ///
  /// In en, this message translates to:
  /// **'Network error. Check your connection.'**
  String get networkError;

  /// No description provided for @timeoutError.
  ///
  /// In en, this message translates to:
  /// **'Request timed out. The server may be busy.'**
  String get timeoutError;

  /// No description provided for @unknownError.
  ///
  /// In en, this message translates to:
  /// **'Something went wrong'**
  String get unknownError;

  /// No description provided for @urlTab.
  ///
  /// In en, this message translates to:
  /// **'URL'**
  String get urlTab;

  /// No description provided for @textTab.
  ///
  /// In en, this message translates to:
  /// **'Text'**
  String get textTab;

  /// No description provided for @textInputHint.
  ///
  /// In en, this message translates to:
  /// **'Paste or type article text...'**
  String get textInputHint;

  /// No description provided for @titleHint.
  ///
  /// In en, this message translates to:
  /// **'Title (optional)'**
  String get titleHint;

  /// No description provided for @summarizeText.
  ///
  /// In en, this message translates to:
  /// **'Summarize Text'**
  String get summarizeText;

  /// No description provided for @saveNotes.
  ///
  /// In en, this message translates to:
  /// **'Save'**
  String get saveNotes;

  /// No description provided for @notesUpdated.
  ///
  /// In en, this message translates to:
  /// **'Notes saved'**
  String get notesUpdated;

  /// No description provided for @removeTag.
  ///
  /// In en, this message translates to:
  /// **'Remove tag'**
  String get removeTag;

  /// No description provided for @tagHint.
  ///
  /// In en, this message translates to:
  /// **'New tag'**
  String get tagHint;

  /// No description provided for @tagsUpdated.
  ///
  /// In en, this message translates to:
  /// **'Tags updated'**
  String get tagsUpdated;

  /// No description provided for @export.
  ///
  /// In en, this message translates to:
  /// **'Export'**
  String get export;

  /// No description provided for @exportReadingList.
  ///
  /// In en, this message translates to:
  /// **'Export reading list'**
  String get exportReadingList;

  /// No description provided for @markAllRead.
  ///
  /// In en, this message translates to:
  /// **'Mark all as read'**
  String get markAllRead;

  /// No description provided for @markAllUnread.
  ///
  /// In en, this message translates to:
  /// **'Mark all as unread'**
  String get markAllUnread;

  /// No description provided for @bulkUpdated.
  ///
  /// In en, this message translates to:
  /// **'Reading list updated'**
  String get bulkUpdated;

  /// No description provided for @batchTab.
  ///
  /// In en, this message translates to:
  /// **'Batch'**
  String get batchTab;

  /// No description provided for @batchInputHint.
  ///
  /// In en, this message translates to:
  /// **'Paste URLs, one per line...'**
  String get batchInputHint;

  /// No description provided for @batchSummarize.
  ///
  /// In en, this message translates to:
  /// **'Summarize All'**
  String get batchSummarize;

  /// No description provided for @batchProgress.
  ///
  /// In en, this message translates to:
  /// **'Summarizing {current} of {total}…'**
  String batchProgress(int current, int total);

  /// No description provided for @batchComplete.
  ///
  /// In en, this message translates to:
  /// **'{count, plural, one{{count} summary ready} other{{count} summaries ready}}'**
  String batchComplete(int count);

  /// No description provided for @makeShorter.
  ///
  /// In en, this message translates to:
  /// **'Make shorter'**
  String get makeShorter;

  /// No description provided for @makeLonger.
  ///
  /// In en, this message translates to:
  /// **'Make longer'**
  String get makeLonger;

  /// No description provided for @regenerate.
  ///
  /// In en, this message translates to:
  /// **'Regenerate'**
  String get regenerate;

  /// No description provided for @adjustingSummary.
  ///
  /// In en, this message translates to:
  /// **'Adjusting…'**
  String get adjustingSummary;

  /// No description provided for @filterByTag.
  ///
  /// In en, this message translates to:
  /// **'Filter by tag'**
  String get filterByTag;

  /// No description provided for @summarizationSettings.
  ///
  /// In en, this message translates to:
  /// **'Summarization'**
  String get summarizationSettings;

  /// No description provided for @defaultLength.
  ///
  /// In en, this message translates to:
  /// **'Default Length'**
  String get defaultLength;

  /// No description provided for @lengthDefault.
  ///
  /// In en, this message translates to:
  /// **'Default'**
  String get lengthDefault;

  /// No description provided for @lengthShort.
  ///
  /// In en, this message translates to:
  /// **'Short'**
  String get lengthShort;

  /// No description provided for @lengthMedium.
  ///
  /// In en, this message translates to:
  /// **'Medium'**
  String get lengthMedium;

  /// No description provided for @lengthLong.
  ///
  /// In en, this message translates to:
  /// **'Long'**
  String get lengthLong;

  /// No description provided for @customPrompt.
  ///
  /// In en, this message translates to:
  /// **'Custom Prompt'**
  String get customPrompt;

  /// No description provided for @customPromptHint.
  ///
  /// In en, this message translates to:
  /// **'Override the default summarization prompt...'**
  String get customPromptHint;

  /// No description provided for @integrations.
  ///
  /// In en, this message translates to:
  /// **'Integrations'**
  String get integrations;

  /// No description provided for @openaiApiKey.
  ///
  /// In en, this message translates to:
  /// **'OpenAI API Key'**
  String get openaiApiKey;

  /// No description provided for @anthropicApiKey.
  ///
  /// In en, this message translates to:
  /// **'Anthropic API Key'**
  String get anthropicApiKey;

  /// No description provided for @readeckApiKey.
  ///
  /// In en, this message translates to:
  /// **'Readeck API Key'**
  String get readeckApiKey;

  /// No description provided for @readeckUrl.
  ///
  /// In en, this message translates to:
  /// **'Readeck URL'**
  String get readeckUrl;

  /// No description provided for @webhookUrl.
  ///
  /// In en, this message translates to:
  /// **'Webhook URL'**
  String get webhookUrl;

  /// No description provided for @keyNotSet.
  ///
  /// In en, this message translates to:
  /// **'Not set'**
  String get keyNotSet;

  /// No description provided for @settingsSaved.
  ///
  /// In en, this message translates to:
  /// **'Settings saved'**
  String get settingsSaved;

  /// No description provided for @language.
  ///
  /// In en, this message translates to:
  /// **'Language'**
  String get language;

  /// No description provided for @selectModel.
  ///
  /// In en, this message translates to:
  /// **'Select Model'**
  String get selectModel;

  /// No description provided for @appearance.
  ///
  /// In en, this message translates to:
  /// **'Appearance'**
  String get appearance;

  /// No description provided for @notificationsEnabled.
  ///
  /// In en, this message translates to:
  /// **'Notifications'**
  String get notificationsEnabled;

  /// No description provided for @about.
  ///
  /// In en, this message translates to:
  /// **'About'**
  String get about;

  /// No description provided for @manageUsers.
  ///
  /// In en, this message translates to:
  /// **'Manage Users'**
  String get manageUsers;

  /// No description provided for @createUser.
  ///
  /// In en, this message translates to:
  /// **'Create User'**
  String get createUser;

  /// No description provided for @deleteUser.
  ///
  /// In en, this message translates to:
  /// **'Delete User'**
  String get deleteUser;

  /// No description provided for @deleteUserConfirm.
  ///
  /// In en, this message translates to:
  /// **'Delete user \"{username}\"? This cannot be undone.'**
  String deleteUserConfirm(String username);

  /// No description provided for @roleLabel.
  ///
  /// In en, this message translates to:
  /// **'Role'**
  String get roleLabel;

  /// No description provided for @adminRole.
  ///
  /// In en, this message translates to:
  /// **'Admin'**
  String get adminRole;

  /// No description provided for @userRole.
  ///
  /// In en, this message translates to:
  /// **'User'**
  String get userRole;

  /// No description provided for @userCreated.
  ///
  /// In en, this message translates to:
  /// **'User created'**
  String get userCreated;

  /// No description provided for @userDeleted.
  ///
  /// In en, this message translates to:
  /// **'User deleted'**
  String get userDeleted;

  /// No description provided for @userCreateError.
  ///
  /// In en, this message translates to:
  /// **'Failed to create user'**
  String get userCreateError;

  /// No description provided for @administration.
  ///
  /// In en, this message translates to:
  /// **'Administration'**
  String get administration;
}

class _AppLocalizationsDelegate
    extends LocalizationsDelegate<AppLocalizations> {
  const _AppLocalizationsDelegate();

  @override
  Future<AppLocalizations> load(Locale locale) {
    return SynchronousFuture<AppLocalizations>(lookupAppLocalizations(locale));
  }

  @override
  bool isSupported(Locale locale) =>
      <String>['en', 'pt'].contains(locale.languageCode);

  @override
  bool shouldReload(_AppLocalizationsDelegate old) => false;
}

AppLocalizations lookupAppLocalizations(Locale locale) {
  // Lookup logic when language+country codes are specified.
  switch (locale.languageCode) {
    case 'pt':
      {
        switch (locale.countryCode) {
          case 'BR':
            return AppLocalizationsPtBr();
        }
        break;
      }
  }

  // Lookup logic when only language code is specified.
  switch (locale.languageCode) {
    case 'en':
      return AppLocalizationsEn();
    case 'pt':
      return AppLocalizationsPt();
  }

  throw FlutterError(
    'AppLocalizations.delegate failed to load unsupported locale "$locale". This is likely '
    'an issue with the localizations generation tool. Please file an issue '
    'on GitHub with a reproducible sample app and the gen-l10n configuration '
    'that was used.',
  );
}
