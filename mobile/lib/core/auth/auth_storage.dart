import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

final authStorageProvider = Provider<AuthStorage>((ref) {
  return AuthStorage();
});

/// Stored authentication material. Either a password (HTTP Basic) or an opaque
/// bearer [token] (minted by an SSO login) is present — never both.
class Credentials {
  final String serverUrl;
  final String? username;
  final String? password;
  final String? token;

  const Credentials({
    required this.serverUrl,
    this.username,
    this.password,
    this.token,
  });

  /// True when this is a bearer-token (SSO) session rather than password auth.
  bool get isBearer => token != null && token!.isNotEmpty;
}

class AuthStorage {
  static const _storage = FlutterSecureStorage();
  static const _keyServerUrl = 'briefen_server_url';
  static const _keyUsername = 'briefen_username';
  static const _keyPassword = 'briefen_password';
  static const _keyToken = 'briefen_token';

  Future<Credentials?> readCredentials() async {
    final serverUrl = await _storage.read(key: _keyServerUrl);
    if (serverUrl == null) return null;

    final username = await _storage.read(key: _keyUsername);
    final password = await _storage.read(key: _keyPassword);
    final token = await _storage.read(key: _keyToken);

    // Need at least one usable credential.
    if ((token == null || token.isEmpty) && (password == null || password.isEmpty)) {
      return null;
    }
    return Credentials(
      serverUrl: serverUrl,
      username: username,
      password: password,
      token: token,
    );
  }

  Future<void> saveCredentials(Credentials credentials) async {
    await Future.wait([
      _storage.write(key: _keyServerUrl, value: credentials.serverUrl),
      _writeOrDelete(_keyUsername, credentials.username),
      _writeOrDelete(_keyPassword, credentials.password),
      _writeOrDelete(_keyToken, credentials.token),
    ]);
  }

  Future<void> _writeOrDelete(String key, String? value) {
    return (value == null)
        ? _storage.delete(key: key)
        : _storage.write(key: key, value: value);
  }

  Future<void> clearCredentials() async {
    await Future.wait([
      _storage.delete(key: _keyServerUrl),
      _storage.delete(key: _keyUsername),
      _storage.delete(key: _keyPassword),
      _storage.delete(key: _keyToken),
    ]);
  }
}
