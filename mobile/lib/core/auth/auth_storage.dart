import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

final authStorageProvider = Provider<AuthStorage>((ref) {
  return AuthStorage();
});

class Credentials {
  final String serverUrl;
  final String username;
  final String password;

  const Credentials({
    required this.serverUrl,
    required this.username,
    required this.password,
  });
}

class AuthStorage {
  static const _storage = FlutterSecureStorage();
  static const _keyServerUrl = 'briefen_server_url';
  static const _keyUsername = 'briefen_username';
  static const _keyPassword = 'briefen_password';

  Future<Credentials?> readCredentials() async {
    final serverUrl = await _storage.read(key: _keyServerUrl);
    final username = await _storage.read(key: _keyUsername);
    final password = await _storage.read(key: _keyPassword);

    if (serverUrl == null || username == null || password == null) return null;
    return Credentials(
      serverUrl: serverUrl,
      username: username,
      password: password,
    );
  }

  Future<void> saveCredentials(Credentials credentials) async {
    await Future.wait([
      _storage.write(key: _keyServerUrl, value: credentials.serverUrl),
      _storage.write(key: _keyUsername, value: credentials.username),
      _storage.write(key: _keyPassword, value: credentials.password),
    ]);
  }

  Future<void> clearCredentials() async {
    await Future.wait([
      _storage.delete(key: _keyServerUrl),
      _storage.delete(key: _keyUsername),
      _storage.delete(key: _keyPassword),
    ]);
  }
}
