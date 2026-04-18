class UserSettings {
  final String? defaultLength;
  final String? model;
  final bool notificationsEnabled;
  final String? openaiApiKey;
  final String? anthropicApiKey;
  final String? readeckApiKey;
  final String? readeckUrl;
  final String? webhookUrl;
  final String? customPrompt;

  const UserSettings({
    this.defaultLength,
    this.model,
    this.notificationsEnabled = false,
    this.openaiApiKey,
    this.anthropicApiKey,
    this.readeckApiKey,
    this.readeckUrl,
    this.webhookUrl,
    this.customPrompt,
  });

  factory UserSettings.fromJson(Map<String, dynamic> json) {
    return UserSettings(
      defaultLength: json['defaultLength'] as String?,
      model: json['model'] as String?,
      notificationsEnabled: json['notificationsEnabled'] as bool? ?? false,
      openaiApiKey: json['openaiApiKey'] as String?,
      anthropicApiKey: json['anthropicApiKey'] as String?,
      readeckApiKey: json['readeckApiKey'] as String?,
      readeckUrl: json['readeckUrl'] as String?,
      webhookUrl: json['webhookUrl'] as String?,
      customPrompt: json['customPrompt'] as String?,
    );
  }

  UserSettings copyWith({
    Object? defaultLength = _sentinel,
    Object? model = _sentinel,
    bool? notificationsEnabled,
    Object? openaiApiKey = _sentinel,
    Object? anthropicApiKey = _sentinel,
    Object? readeckApiKey = _sentinel,
    Object? readeckUrl = _sentinel,
    Object? webhookUrl = _sentinel,
    Object? customPrompt = _sentinel,
  }) {
    return UserSettings(
      defaultLength: defaultLength == _sentinel
          ? this.defaultLength
          : defaultLength as String?,
      model: model == _sentinel ? this.model : model as String?,
      notificationsEnabled: notificationsEnabled ?? this.notificationsEnabled,
      openaiApiKey: openaiApiKey == _sentinel
          ? this.openaiApiKey
          : openaiApiKey as String?,
      anthropicApiKey: anthropicApiKey == _sentinel
          ? this.anthropicApiKey
          : anthropicApiKey as String?,
      readeckApiKey: readeckApiKey == _sentinel
          ? this.readeckApiKey
          : readeckApiKey as String?,
      readeckUrl: readeckUrl == _sentinel
          ? this.readeckUrl
          : readeckUrl as String?,
      webhookUrl: webhookUrl == _sentinel
          ? this.webhookUrl
          : webhookUrl as String?,
      customPrompt: customPrompt == _sentinel
          ? this.customPrompt
          : customPrompt as String?,
    );
  }
}

const _sentinel = Object();
