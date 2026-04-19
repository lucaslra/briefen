import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/auth/auth_provider.dart';
import '../../../l10n/generated/app_localizations.dart';
import '../data/users_repository.dart';
import '../domain/app_user.dart';
import '../providers.dart';

class UsersScreen extends ConsumerWidget {
  const UsersScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = AppLocalizations.of(context)!;
    final usersAsync = ref.watch(usersProvider);

    return Scaffold(
      appBar: AppBar(title: Text(l10n.manageUsers)),
      floatingActionButton: FloatingActionButton(
        onPressed: () => _showCreateUserDialog(context, ref, l10n),
        child: const Icon(Icons.person_add_outlined),
      ),
      body: usersAsync.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (_, __) => Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(l10n.unknownError),
              const SizedBox(height: 8),
              FilledButton.tonal(
                onPressed: () => ref.invalidate(usersProvider),
                child: Text(l10n.retry),
              ),
            ],
          ),
        ),
        data: (users) {
          if (users.isEmpty) {
            return Center(child: Text(l10n.noResults));
          }
          final currentUserId = ref
              .read(authProvider)
              .username; // used for self-check
          return ListView.builder(
            itemCount: users.length,
            itemBuilder: (context, index) {
              final user = users[index];
              final isSelf = user.username == currentUserId;
              return _UserTile(
                user: user,
                isSelf: isSelf,
                onDelete: (isSelf || user.mainAdmin)
                    ? null
                    : () => _confirmDelete(context, ref, l10n, user),
              );
            },
          );
        },
      ),
    );
  }

  Future<void> _showCreateUserDialog(
    BuildContext context,
    WidgetRef ref,
    AppLocalizations l10n,
  ) async {
    final usernameController = TextEditingController();
    final passwordController = TextEditingController();
    String selectedRole = 'USER';
    bool obscure = true;

    await showDialog<void>(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setStateDialog) => AlertDialog(
          title: Text(l10n.createUser),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(
                controller: usernameController,
                decoration: InputDecoration(
                  labelText: l10n.username,
                  border: const OutlineInputBorder(),
                ),
                textInputAction: TextInputAction.next,
                autofocus: true,
              ),
              const SizedBox(height: 12),
              TextField(
                controller: passwordController,
                decoration: InputDecoration(
                  labelText: l10n.password,
                  border: const OutlineInputBorder(),
                  suffixIcon: IconButton(
                    icon: Icon(
                      obscure ? Icons.visibility_off : Icons.visibility,
                    ),
                    onPressed: () => setStateDialog(() => obscure = !obscure),
                  ),
                ),
                obscureText: obscure,
                textInputAction: TextInputAction.done,
              ),
              const SizedBox(height: 12),
              DropdownButtonFormField<String>(
                initialValue: selectedRole,
                decoration: InputDecoration(
                  labelText: l10n.roleLabel,
                  border: const OutlineInputBorder(),
                ),
                items: [
                  DropdownMenuItem(value: 'USER', child: Text(l10n.userRole)),
                  DropdownMenuItem(value: 'ADMIN', child: Text(l10n.adminRole)),
                ],
                onChanged: (v) {
                  if (v != null) setStateDialog(() => selectedRole = v);
                },
              ),
            ],
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx),
              child: Text(l10n.cancel),
            ),
            FilledButton(
              onPressed: () async {
                final username = usernameController.text.trim();
                final password = passwordController.text;
                if (username.isEmpty || password.isEmpty) return;
                Navigator.pop(ctx);
                await _createUser(
                  context,
                  ref,
                  l10n,
                  username,
                  password,
                  selectedRole,
                );
              },
              child: Text(l10n.createUser),
            ),
          ],
        ),
      ),
    );

    Future.delayed(const Duration(milliseconds: 400), () {
      usernameController.dispose();
      passwordController.dispose();
    });
  }

  Future<void> _createUser(
    BuildContext context,
    WidgetRef ref,
    AppLocalizations l10n,
    String username,
    String password,
    String role,
  ) async {
    try {
      final repo = ref.read(usersRepositoryProvider);
      await repo.createUser(username: username, password: password, role: role);
      ref.invalidate(usersProvider);
      if (context.mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(l10n.userCreated)));
      }
    } catch (e) {
      if (context.mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(l10n.userCreateError)));
      }
    }
  }

  Future<void> _confirmDelete(
    BuildContext context,
    WidgetRef ref,
    AppLocalizations l10n,
    AppUser user,
  ) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(l10n.deleteUser),
        content: Text(l10n.deleteUserConfirm(user.username)),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: Text(l10n.cancel),
          ),
          TextButton(
            onPressed: () => Navigator.pop(ctx, true),
            child: Text(
              l10n.delete,
              style: TextStyle(color: Theme.of(context).colorScheme.error),
            ),
          ),
        ],
      ),
    );

    if (confirmed == true && context.mounted) {
      try {
        await ref.read(usersRepositoryProvider).deleteUser(user.id);
        ref.invalidate(usersProvider);
        if (context.mounted) {
          ScaffoldMessenger.of(
            context,
          ).showSnackBar(SnackBar(content: Text(l10n.userDeleted)));
        }
      } catch (e) {
        if (context.mounted) {
          ScaffoldMessenger.of(
            context,
          ).showSnackBar(SnackBar(content: Text(l10n.unknownError)));
        }
      }
    }
  }
}

class _UserTile extends StatelessWidget {
  final AppUser user;
  final bool isSelf;
  final VoidCallback? onDelete;

  const _UserTile({
    required this.user,
    required this.isSelf,
    required this.onDelete,
  });

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
    final l10n = AppLocalizations.of(context)!;

    return ListTile(
      leading: CircleAvatar(
        backgroundColor: user.isAdmin
            ? colorScheme.primaryContainer
            : colorScheme.surfaceContainerHighest,
        child: Text(
          user.username[0].toUpperCase(),
          style: TextStyle(
            color: user.isAdmin
                ? colorScheme.onPrimaryContainer
                : colorScheme.onSurfaceVariant,
            fontWeight: FontWeight.bold,
          ),
        ),
      ),
      title: Text(user.username),
      subtitle: Row(
        children: [
          Chip(
            label: Text(
              user.isAdmin ? l10n.adminRole : l10n.userRole,
              style: const TextStyle(fontSize: 11),
            ),
            padding: EdgeInsets.zero,
            visualDensity: VisualDensity.compact,
            side: BorderSide.none,
            backgroundColor: user.isAdmin
                ? colorScheme.primaryContainer
                : colorScheme.surfaceContainerHighest,
          ),
          if (isSelf) ...[
            const SizedBox(width: 6),
            Text(
              '(you)',
              style: TextStyle(
                fontSize: 12,
                color: colorScheme.onSurfaceVariant,
              ),
            ),
          ],
          if (user.mainAdmin) ...[
            const SizedBox(width: 6),
            Text(
              '(main)',
              style: TextStyle(
                fontSize: 12,
                color: colorScheme.onSurfaceVariant,
              ),
            ),
          ],
        ],
      ),
      trailing: onDelete != null
          ? IconButton(
              icon: Icon(Icons.delete_outline, color: colorScheme.error),
              onPressed: onDelete,
              tooltip: l10n.deleteUser,
            )
          : null,
    );
  }
}
