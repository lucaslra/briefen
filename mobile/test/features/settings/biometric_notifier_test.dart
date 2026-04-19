import 'package:briefen/core/auth/biometric_provider.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

void main() {
  setUp(() {
    SharedPreferences.setMockInitialValues({});
  });

  ProviderContainer _container() {
    final c = ProviderContainer();
    addTearDown(c.dispose);
    return c;
  }

  group('BiometricEnabledNotifier', () {
    test('starts as false when no saved preference', () async {
      final container = _container();
      await pumpEventQueue();
      expect(container.read(biometricEnabledProvider), isFalse);
    });

    test(
      'setEnabled(true) updates state and persists to SharedPreferences',
      () async {
        final container = _container();
        await pumpEventQueue();

        await container
            .read(biometricEnabledProvider.notifier)
            .setEnabled(true);

        expect(container.read(biometricEnabledProvider), isTrue);
        final prefs = await SharedPreferences.getInstance();
        expect(prefs.getBool('biometric_enabled'), isTrue);
      },
    );

    test(
      'setEnabled(false) updates state and persists to SharedPreferences',
      () async {
        final container = _container();
        await pumpEventQueue();
        await container
            .read(biometricEnabledProvider.notifier)
            .setEnabled(true);

        await container
            .read(biometricEnabledProvider.notifier)
            .setEnabled(false);

        expect(container.read(biometricEnabledProvider), isFalse);
        final prefs = await SharedPreferences.getInstance();
        expect(prefs.getBool('biometric_enabled'), isFalse);
      },
    );

    test(
      'new notifier restores value previously persisted by setEnabled',
      () async {
        // Write the value via a first container
        final c1 = ProviderContainer();
        await pumpEventQueue();
        await c1.read(biometricEnabledProvider.notifier).setEnabled(true);
        c1.dispose();

        // A fresh container should load the persisted value.
        // Read first to trigger lazy creation, then drain so _load() completes.
        final c2 = ProviderContainer();
        addTearDown(c2.dispose);
        c2.read(biometricEnabledProvider);
        await pumpEventQueue();
        expect(c2.read(biometricEnabledProvider), isTrue);
      },
    );

    test('toggling back to false is also restored by a new notifier', () async {
      final c1 = ProviderContainer();
      await pumpEventQueue();
      await c1.read(biometricEnabledProvider.notifier).setEnabled(true);
      await c1.read(biometricEnabledProvider.notifier).setEnabled(false);
      c1.dispose();

      final c2 = ProviderContainer();
      addTearDown(c2.dispose);
      c2.read(biometricEnabledProvider);
      await pumpEventQueue();
      expect(c2.read(biometricEnabledProvider), isFalse);
    });
  });
}
