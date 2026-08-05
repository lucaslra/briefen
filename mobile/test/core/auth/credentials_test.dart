import 'package:briefen/core/auth/auth_storage.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  group('Credentials.isBearer', () {
    test('true when a non-empty token is present', () {
      const c = Credentials(serverUrl: 'https://x', username: 'a', token: 'bfn_1');
      expect(c.isBearer, isTrue);
    });

    test('false for password credentials', () {
      const c = Credentials(serverUrl: 'https://x', username: 'a', password: 'p');
      expect(c.isBearer, isFalse);
    });

    test('false when token is empty', () {
      const c = Credentials(serverUrl: 'https://x', token: '');
      expect(c.isBearer, isFalse);
    });
  });
}
