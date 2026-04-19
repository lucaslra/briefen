import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'auth/auth_provider.dart';
import '../features/summarize/providers.dart';
import '../features/setup/presentation/login_screen.dart';
import '../features/setup/presentation/setup_screen.dart';
import '../features/summarize/presentation/summarize_screen.dart';
import '../features/reading_list/presentation/reading_list_screen.dart';
import '../features/reading_list/presentation/summary_detail_screen.dart';
import '../features/settings/presentation/settings_screen.dart';
import '../features/users/presentation/users_screen.dart';

final _rootNavigatorKey = GlobalKey<NavigatorState>();

/// Listenable that bridges Riverpod auth state to GoRouter refreshes.
class _AuthNotifierListenable extends ChangeNotifier {
  _AuthNotifierListenable(Ref ref) {
    ref.listen(authProvider, (_, _) => notifyListeners());
  }
}

final routerProvider = Provider<GoRouter>((ref) {
  final refreshListenable = _AuthNotifierListenable(ref);

  return GoRouter(
    navigatorKey: _rootNavigatorKey,
    initialLocation: '/summarize',
    refreshListenable: refreshListenable,
    redirect: (context, state) {
      final authState = ref.read(authProvider);
      final path = state.matchedLocation;
      final isAuthRoute = path == '/login' || path == '/setup';

      if (authState.status == AuthStatus.unknown) return null;

      if (authState.status == AuthStatus.unauthenticated) {
        return isAuthRoute ? null : '/login';
      }

      if (authState.status == AuthStatus.needsSetup) {
        return path == '/setup' ? null : '/setup';
      }

      if (authState.status == AuthStatus.authenticated && isAuthRoute) {
        return '/summarize';
      }

      return null;
    },
    routes: [
      GoRoute(path: '/login', builder: (context, state) => const LoginScreen()),
      GoRoute(path: '/setup', builder: (context, state) => const SetupScreen()),
      StatefulShellRoute.indexedStack(
        builder: (context, state, navigationShell) {
          return ScaffoldWithNavBar(navigationShell: navigationShell);
        },
        branches: [
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: '/summarize',
                builder: (context, state) => const SummarizeScreen(),
              ),
            ],
          ),
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: '/reading-list',
                builder: (context, state) => const ReadingListScreen(),
                routes: [
                  GoRoute(
                    path: ':id',
                    parentNavigatorKey: _rootNavigatorKey,
                    builder: (context, state) {
                      final id = state.pathParameters['id']!;
                      return SummaryDetailScreen(summaryId: id);
                    },
                  ),
                ],
              ),
            ],
          ),
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: '/settings',
                builder: (context, state) => const SettingsScreen(),
                routes: [
                  GoRoute(
                    path: 'users',
                    parentNavigatorKey: _rootNavigatorKey,
                    builder: (context, state) => const UsersScreen(),
                  ),
                ],
              ),
            ],
          ),
        ],
      ),
    ],
  );
});

class ScaffoldWithNavBar extends ConsumerWidget {
  final StatefulNavigationShell navigationShell;

  const ScaffoldWithNavBar({super.key, required this.navigationShell});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final unreadCount = ref.watch(unreadCountProvider).valueOrNull ?? 0;

    return Scaffold(
      body: navigationShell,
      bottomNavigationBar: NavigationBar(
        selectedIndex: navigationShell.currentIndex,
        onDestinationSelected: (index) {
          navigationShell.goBranch(
            index,
            initialLocation: index == navigationShell.currentIndex,
          );
        },
        destinations: [
          const NavigationDestination(
            icon: Icon(Icons.summarize_outlined),
            selectedIcon: Icon(Icons.summarize),
            label: 'Summarize',
          ),
          NavigationDestination(
            icon: Badge(
              isLabelVisible: unreadCount > 0,
              label: Text(unreadCount > 99 ? '99+' : '$unreadCount'),
              child: const Icon(Icons.menu_book_outlined),
            ),
            selectedIcon: Badge(
              isLabelVisible: unreadCount > 0,
              label: Text(unreadCount > 99 ? '99+' : '$unreadCount'),
              child: const Icon(Icons.menu_book),
            ),
            label: 'Reading List',
          ),
          const NavigationDestination(
            icon: Icon(Icons.settings_outlined),
            selectedIcon: Icon(Icons.settings),
            label: 'Settings',
          ),
        ],
      ),
    );
  }
}
