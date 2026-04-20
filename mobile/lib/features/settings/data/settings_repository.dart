import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/api/api_client.dart';

final settingsRepositoryProvider = Provider<SettingsRepository>((ref) {
  return SettingsRepository(ref.read(apiClientProvider));
});

class SettingsRepository {
  final ApiClient _client;

  SettingsRepository(this._client);

  Future<Map<String, dynamic>> getSettings() async {
    final response = await _client.get('/api/settings');
    return response.data as Map<String, dynamic>;
  }

  Future<Map<String, dynamic>> updateSettings(
      Map<String, dynamic> settings) async {
    final response = await _client.put('/api/settings', data: settings);
    return response.data as Map<String, dynamic>;
  }

  Future<Map<String, dynamic>> getVersion() async {
    final response = await _client.get('/api/version');
    return response.data as Map<String, dynamic>;
  }
}
