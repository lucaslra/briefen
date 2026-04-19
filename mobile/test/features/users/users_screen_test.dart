import 'package:briefen/core/auth/auth_provider.dart';
import 'package:briefen/features/users/domain/app_user.dart';
import 'package:briefen/features/users/presentation/users_screen.dart';
import 'package:briefen/features/users/providers.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../../helpers/test_app.dart';

// ── Helpers ───────────────────────────────────────────────────────────────────

AppUser _user({
  required String username,
  String role = 'USER',
  bool mainAdmin = false,
}) => AppUser(
  id: username,
  username: username,
  role: role,
  createdAt: DateTime(2024),
  mainAdmin: mainAdmin,
);

AuthState _authAs(String username, {String role = 'ADMIN'}) => AuthState(
  status: AuthStatus.authenticated,
  username: username,
  role: role,
  serverUrl: 'http://localhost:8080',
);

List<Override> _overrides({
  required List<AppUser> users,
  required AuthState auth,
}) => [
  usersProvider.overrideWith((_) async => users),
  authProvider.overrideWith(() => _FakeAuthNotifier(auth)),
];

// ── Tests ────────────────────────────────────────────────────────────────────

void main() {
  group('UsersScreen', () {
    testWidgets('shows a row for each user', (tester) async {
      final users = [
        _user(username: 'alice', role: 'ADMIN', mainAdmin: true),
        _user(username: 'bob'),
        _user(username: 'carol'),
      ];

      await tester.pumpWidget(
        buildTestApp(
          const UsersScreen(),
          overrides: _overrides(users: users, auth: _authAs('alice')),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('alice'), findsOneWidget);
      expect(find.text('bob'), findsOneWidget);
      expect(find.text('carol'), findsOneWidget);
    });

    testWidgets('shows empty-state message when user list is empty', (
      tester,
    ) async {
      await tester.pumpWidget(
        buildTestApp(
          const UsersScreen(),
          overrides: _overrides(users: [], auth: _authAs('alice')),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('No results found'), findsOneWidget);
    });

    testWidgets('shows FAB for creating a new user', (tester) async {
      await tester.pumpWidget(
        buildTestApp(
          const UsersScreen(),
          overrides: _overrides(
            users: [_user(username: 'alice', mainAdmin: true)],
            auth: _authAs('alice'),
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.byIcon(Icons.person_add_outlined), findsOneWidget);
    });

    testWidgets('shows retry button on error', (tester) async {
      await tester.pumpWidget(
        buildTestApp(
          const UsersScreen(),
          overrides: [
            usersProvider.overrideWith((_) async => throw Exception('fail')),
            authProvider.overrideWith(
              () => _FakeAuthNotifier(_authAs('alice')),
            ),
          ],
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('Retry'), findsOneWidget);
    });

    testWidgets('renders manage-users title in app bar', (tester) async {
      await tester.pumpWidget(
        buildTestApp(
          const UsersScreen(),
          overrides: _overrides(
            users: [_user(username: 'alice', mainAdmin: true)],
            auth: _authAs('alice'),
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('Manage Users'), findsOneWidget);
    });
  });
}

// ── Fake auth notifier ────────────────────────────────────────────────────────

class _FakeAuthNotifier extends AuthNotifier {
  final AuthState _fixed;
  _FakeAuthNotifier(this._fixed);

  @override
  AuthState build() {
    // Skip the real credential check; return fixed state immediately.
    Future.microtask(() => state = _fixed);
    return const AuthState();
  }
}
