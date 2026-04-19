import 'package:briefen/core/api/api_client.dart';
import 'package:briefen/core/api/api_exceptions.dart';
import 'package:briefen/core/auth/auth_provider.dart';
import 'package:briefen/core/auth/auth_storage.dart';
import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';

// ── Fakes ─────────────────────────────────────────────────────────────────────

class FakeAuthStorage extends Fake implements AuthStorage {
  Credentials? _creds;
  bool cleared = false;

  FakeAuthStorage([this._creds]);

  @override
  Future<Credentials?> readCredentials() async => _creds;

  @override
  Future<void> saveCredentials(Credentials c) async => _creds = c;

  @override
  Future<void> clearCredentials() async {
    _creds = null;
    cleared = true;
  }
}

class MockApiClient extends Mock implements ApiClient {}

// ── Helper ────────────────────────────────────────────────────────────────────

ProviderContainer _container({
  required FakeAuthStorage storage,
  MockApiClient? apiClient,
}) {
  return ProviderContainer(
    overrides: [
      authStorageProvider.overrideWithValue(storage),
      if (apiClient != null) apiClientProvider.overrideWithValue(apiClient),
    ],
  );
}

Response<dynamic> _ok(String path, {Map<String, dynamic>? data}) => Response(
  requestOptions: RequestOptions(path: path),
  statusCode: 200,
  data: data ?? <String, dynamic>{},
);

// ── Tests ─────────────────────────────────────────────────────────────────────

void main() {
  group('AuthNotifier', () {
    test('no stored credentials → unauthenticated', () async {
      final c = _container(storage: FakeAuthStorage(null));
      addTearDown(c.dispose);

      c.read(authProvider); // trigger build
      await pumpEventQueue();

      expect(c.read(authProvider).status, AuthStatus.unauthenticated);
    });

    test('stored credentials + server reachable → authenticated', () async {
      // _checkSetupStatus creates its own Dio — an unreachable URL causes it
      // to catch and return false (setup not required), so we move on.
      final storage = FakeAuthStorage(
        Credentials(
          serverUrl: 'http://127.0.0.1:19999',
          username: 'alice',
          password: 'pass',
        ),
      );
      final api = MockApiClient();

      when(
        () => api.get('/api/settings'),
      ).thenAnswer((_) async => _ok('/api/settings'));
      when(
        () => api.get('/api/users/me'),
      ).thenAnswer((_) async => _ok('/api/users/me', data: {'role': 'USER'}));
      when(() => api.reset()).thenAnswer((_) {});

      final c = _container(storage: storage, apiClient: api);
      addTearDown(c.dispose);

      c.read(authProvider);
      await pumpEventQueue();

      final state = c.read(authProvider);
      expect(state.status, AuthStatus.authenticated);
      expect(state.username, 'alice');
      expect(state.role, 'USER');
    });

    test(
      'stored credentials + 401 → unauthenticated and credentials cleared',
      () async {
        final storage = FakeAuthStorage(
          Credentials(
            serverUrl: 'http://127.0.0.1:19999',
            username: 'alice',
            password: 'wrong',
          ),
        );
        final api = MockApiClient();

        when(() => api.get('/api/settings')).thenThrow(const AuthException());
        when(() => api.reset()).thenAnswer((_) {});

        final c = _container(storage: storage, apiClient: api);
        addTearDown(c.dispose);

        c.read(authProvider);
        await pumpEventQueue();

        expect(c.read(authProvider).status, AuthStatus.unauthenticated);
        expect(storage.cleared, isTrue);
      },
    );

    test(
      'stored credentials + network error → stays authenticated (graceful degradation)',
      () async {
        final storage = FakeAuthStorage(
          Credentials(
            serverUrl: 'http://127.0.0.1:19999',
            username: 'alice',
            password: 'pass',
          ),
        );
        final api = MockApiClient();

        when(
          () => api.get('/api/settings'),
        ).thenThrow(const NetworkException());
        when(() => api.reset()).thenAnswer((_) {});

        final c = _container(storage: storage, apiClient: api);
        addTearDown(c.dispose);

        c.read(authProvider);
        await pumpEventQueue();

        final state = c.read(authProvider);
        expect(state.status, AuthStatus.authenticated);
        expect(state.username, 'alice');
      },
    );

    test('logout() → unauthenticated and credentials cleared', () async {
      final storage = FakeAuthStorage(
        Credentials(
          serverUrl: 'http://127.0.0.1:19999',
          username: 'alice',
          password: 'pass',
        ),
      );
      final api = MockApiClient();

      when(
        () => api.get('/api/settings'),
      ).thenAnswer((_) async => _ok('/api/settings'));
      when(
        () => api.get('/api/users/me'),
      ).thenAnswer((_) async => _ok('/api/users/me', data: {'role': 'ADMIN'}));
      when(() => api.reset()).thenAnswer((_) {});

      final c = _container(storage: storage, apiClient: api);
      addTearDown(c.dispose);

      c.read(authProvider);
      await pumpEventQueue();
      expect(c.read(authProvider).status, AuthStatus.authenticated);

      await c.read(authProvider.notifier).logout();

      expect(c.read(authProvider).status, AuthStatus.unauthenticated);
      expect(storage.cleared, isTrue);
    });
  });
}
