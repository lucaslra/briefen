import 'package:briefen/features/settings/data/settings_repository.dart';
import 'package:briefen/features/settings/domain/user_settings.dart';
import 'package:briefen/features/settings/providers.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

// ── Fake ──────────────────────────────────────────────────────────────────────

class FakeSettingsRepository extends Fake implements SettingsRepository {
  Map<String, dynamic> _data;

  FakeSettingsRepository(this._data);

  @override
  Future<Map<String, dynamic>> getSettings() async => Map.of(_data);

  @override
  Future<Map<String, dynamic>> updateSettings(
    Map<String, dynamic> patch,
  ) async {
    _data = {..._data, ...patch};
    return Map.of(_data);
  }
}

// ── Tests ─────────────────────────────────────────────────────────────────────

void main() {
  group('SettingsNotifier', () {
    test('build() loads settings from repository', () async {
      final repo = FakeSettingsRepository({
        'defaultLength': 'SHORT',
        'model': 'gpt-4o-mini',
        'notificationsEnabled': true,
      });

      final container = ProviderContainer(
        overrides: [settingsRepositoryProvider.overrideWithValue(repo)],
      );
      addTearDown(container.dispose);

      final state = await container.read(settingsProvider.future);

      expect(state.defaultLength, 'SHORT');
      expect(state.model, 'gpt-4o-mini');
      expect(state.notificationsEnabled, isTrue);
    });

    test('save(patch) updates only the patched fields', () async {
      final repo = FakeSettingsRepository({
        'defaultLength': 'SHORT',
        'model': 'gpt-4o-mini',
        'openaiApiKey': 'sk-old',
      });

      final container = ProviderContainer(
        overrides: [settingsRepositoryProvider.overrideWithValue(repo)],
      );
      addTearDown(container.dispose);

      await container.read(settingsProvider.future);

      await container.read(settingsProvider.notifier).save({
        'openaiApiKey': 'sk-new',
      });

      final updated = container.read(settingsProvider).value!;
      expect(updated.openaiApiKey, 'sk-new');
      expect(updated.defaultLength, 'SHORT'); // unchanged
      expect(updated.model, 'gpt-4o-mini'); // unchanged
    });

    test('save(patch) exposes server response as new state', () async {
      final repo = FakeSettingsRepository({'model': 'gemma3:4b'});

      final container = ProviderContainer(
        overrides: [settingsRepositoryProvider.overrideWithValue(repo)],
      );
      addTearDown(container.dispose);

      await container.read(settingsProvider.future);
      await container.read(settingsProvider.notifier).save({'model': 'gpt-4o'});

      expect(
        container.read(settingsProvider).value,
        isA<UserSettings>().having((s) => s.model, 'model', 'gpt-4o'),
      );
    });
  });
}
