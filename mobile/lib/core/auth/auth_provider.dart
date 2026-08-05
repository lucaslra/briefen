import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_web_auth_2/flutter_web_auth_2.dart';

import '../api/api_client.dart';
import '../api/api_exceptions.dart';
import 'auth_storage.dart';

enum AuthStatus { unknown, unauthenticated, needsSetup, authenticated }

class AuthState {
  final AuthStatus status;
  final String? serverUrl;
  final String? username;
  final String? role;
  final String? error;

  const AuthState({
    this.status = AuthStatus.unknown,
    this.serverUrl,
    this.username,
    this.role,
    this.error,
  });

  AuthState copyWith({
    AuthStatus? status,
    String? serverUrl,
    String? username,
    String? role,
    String? error,
  }) {
    return AuthState(
      status: status ?? this.status,
      serverUrl: serverUrl ?? this.serverUrl,
      username: username ?? this.username,
      role: role ?? this.role,
      error: error,
    );
  }

  bool get isAdmin => role == 'ADMIN';
}

final authProvider = NotifierProvider<AuthNotifier, AuthState>(
  AuthNotifier.new,
);

class AuthNotifier extends Notifier<AuthState> {
  @override
  AuthState build() {
    _checkExistingCredentials();
    return const AuthState();
  }

  Future<void> _checkExistingCredentials() async {
    final storage = ref.read(authStorageProvider);
    final creds = await storage.readCredentials();

    if (creds == null) {
      state = const AuthState(status: AuthStatus.unauthenticated);
      return;
    }

    try {
      final setupRequired = await _checkSetupStatus(creds.serverUrl);
      if (setupRequired) {
        state = AuthState(
          status: AuthStatus.needsSetup,
          serverUrl: creds.serverUrl,
        );
        return;
      }

      // Validate credentials by fetching settings
      final apiClient = ref.read(apiClientProvider);
      await apiClient.get('/api/settings');

      // Get user info
      final userResponse = await apiClient.get('/api/users/me');
      final userData = userResponse.data as Map<String, dynamic>;

      state = AuthState(
        status: AuthStatus.authenticated,
        serverUrl: creds.serverUrl,
        username: creds.username,
        role: userData['role'] as String?,
      );
    } on AuthException {
      await storage.clearCredentials();
      ref.read(apiClientProvider).reset();
      state = const AuthState(status: AuthStatus.unauthenticated);
    } catch (_) {
      // Network/server error at startup — keep credentials, stay authenticated
      state = AuthState(
        status: AuthStatus.authenticated,
        serverUrl: creds.serverUrl,
        username: creds.username,
      );
    }
  }

  Future<void> login(String serverUrl, String username, String password) async {
    // Clear previous error but keep unauthenticated status to avoid remounting the login screen
    state = state.copyWith(status: AuthStatus.unauthenticated, error: null);

    final normalizedUrl = serverUrl.endsWith('/')
        ? serverUrl.substring(0, serverUrl.length - 1)
        : serverUrl;

    try {
      final setupRequired = await _checkSetupStatus(normalizedUrl);
      if (setupRequired) {
        final storage = ref.read(authStorageProvider);
        await storage.saveCredentials(
          Credentials(
            serverUrl: normalizedUrl,
            username: username,
            password: password,
          ),
        );
        state = AuthState(
          status: AuthStatus.needsSetup,
          serverUrl: normalizedUrl,
        );
        return;
      }

      // Validate credentials
      final apiClient = ref.read(apiClientProvider);
      final tempDio = apiClient.createTempDio(
        normalizedUrl,
        username,
        password,
      );
      final encoded = base64Encode(utf8.encode('$username:$password'));

      await tempDio.get(
        '/api/settings',
        options: Options(headers: {'Authorization': 'Basic $encoded'}),
      );

      final userResponse = await tempDio.get(
        '/api/users/me',
        options: Options(headers: {'Authorization': 'Basic $encoded'}),
      );
      final userData = userResponse.data as Map<String, dynamic>;

      // Store credentials
      final storage = ref.read(authStorageProvider);
      await storage.saveCredentials(
        Credentials(
          serverUrl: normalizedUrl,
          username: username,
          password: password,
        ),
      );
      apiClient.reset();

      state = AuthState(
        status: AuthStatus.authenticated,
        serverUrl: normalizedUrl,
        username: username,
        role: userData['role'] as String?,
      );
    } on DioException catch (e) {
      final status = e.response?.statusCode;
      state = state.copyWith(
        status: AuthStatus.unauthenticated,
        error: status == 401
            ? 'Invalid credentials'
            : 'Could not connect to server',
      );
    } catch (e) {
      state = state.copyWith(
        status: AuthStatus.unauthenticated,
        error: 'Could not connect to server',
      );
    }
  }

  /// Runs the OIDC authorization-code flow in an external browser tab and, on
  /// success, stores the returned bearer session and marks the user authenticated.
  ///
  /// The backend deep-links the token back via the custom scheme
  /// `briefen://auth?sso_token=…` (triggered by the `?client=mobile` marker).
  Future<void> loginWithSso(String serverUrl) async {
    state = state.copyWith(status: AuthStatus.unauthenticated, error: null);

    final normalizedUrl = serverUrl.endsWith('/')
        ? serverUrl.substring(0, serverUrl.length - 1)
        : serverUrl;

    try {
      final result = await FlutterWebAuth2.authenticate(
        url: '$normalizedUrl/oauth2/authorization/briefen?client=mobile',
        callbackUrlScheme: 'briefen',
      );

      final params = Uri.parse(result).queryParameters;
      final token = params['sso_token'];
      if (token == null || token.isEmpty) {
        state = state.copyWith(
          status: AuthStatus.unauthenticated,
          error: 'Single sign-on failed',
        );
        return;
      }

      final username = params['username'];
      final role = params['role'];

      final storage = ref.read(authStorageProvider);
      await storage.saveCredentials(
        Credentials(
          serverUrl: normalizedUrl,
          username: username,
          token: token,
        ),
      );
      ref.read(apiClientProvider).reset();

      state = AuthState(
        status: AuthStatus.authenticated,
        serverUrl: normalizedUrl,
        username: username,
        role: role,
      );
    } on PlatformException {
      // User cancelled the browser tab (or scheme mismatch) — stay on the login screen.
      state = state.copyWith(status: AuthStatus.unauthenticated, error: null);
    } catch (_) {
      state = state.copyWith(
        status: AuthStatus.unauthenticated,
        error: 'Single sign-on failed',
      );
    }
  }

  Future<void> logout() async {
    // Best-effort server-side revocation of the bearer session (no-op for Basic auth).
    try {
      await ref.read(apiClientProvider).post('/api/auth/logout');
    } catch (_) {
      // Ignore — logout must always succeed locally.
    }
    final storage = ref.read(authStorageProvider);
    await storage.clearCredentials();
    ref.read(apiClientProvider).reset();
    state = const AuthState(status: AuthStatus.unauthenticated);
  }

  Future<bool> _checkSetupStatus(String serverUrl) async {
    try {
      final dio = Dio(
        BaseOptions(
          baseUrl: serverUrl,
          connectTimeout: const Duration(seconds: 10),
          receiveTimeout: const Duration(seconds: 10),
        ),
      );
      final response = await dio.get('/api/setup/status');
      final data = response.data as Map<String, dynamic>;
      return data['setupRequired'] == true;
    } catch (_) {
      return false;
    }
  }

  void markSetupComplete() {
    _checkExistingCredentials();
  }
}
