import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:briefen/core/api/api_client.dart';
import 'package:briefen/features/settings/data/settings_repository.dart';
import 'package:briefen/features/settings/domain/llm_models.dart';
import 'package:briefen/features/settings/domain/user_settings.dart';

final settingsProvider = AsyncNotifierProvider<SettingsNotifier, UserSettings>(
  SettingsNotifier.new,
);

class SettingsNotifier extends AsyncNotifier<UserSettings> {
  @override
  Future<UserSettings> build() async {
    final repo = ref.read(settingsRepositoryProvider);
    final data = await repo.getSettings();
    return UserSettings.fromJson(data);
  }

  Future<void> save(Map<String, dynamic> patch) async {
    final repo = ref.read(settingsRepositoryProvider);
    final data = await repo.updateSettings(patch);
    state = AsyncData(UserSettings.fromJson(data));
  }
}

final modelsProvider = FutureProvider.autoDispose<List<LlmProvider>>((
  ref,
) async {
  final client = ref.read(apiClientProvider);
  final response = await client.get('/api/models');
  final data = response.data as Map<String, dynamic>;
  final providers = data['providers'] as List<dynamic>;
  return providers
      .map((p) => LlmProvider.fromJson(p as Map<String, dynamic>))
      .toList();
});
