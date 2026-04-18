import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:briefen/core/auth/auth_provider.dart';
import 'package:briefen/core/locale/locale_provider.dart';
import 'package:briefen/core/theme/theme_provider.dart';
import 'package:briefen/features/settings/data/settings_repository.dart';
import 'package:briefen/features/settings/domain/llm_models.dart';
import 'package:briefen/features/settings/domain/user_settings.dart';
import 'package:briefen/features/settings/providers.dart';
import 'package:briefen/l10n/generated/app_localizations.dart';

final _versionProvider = FutureProvider.autoDispose<String>((ref) async {
  final repo = ref.read(settingsRepositoryProvider);
  try {
    final data = await repo.getVersion();
    return data['version'] as String? ?? 'unknown';
  } catch (_) {
    return 'unknown';
  }
});

class SettingsScreen extends ConsumerWidget {
  const SettingsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = AppLocalizations.of(context)!;
    final authState = ref.watch(authProvider);
    final themeMode = ref.watch(themeModeProvider);
    final locale = ref.watch(localeProvider);
    final version = ref.watch(_versionProvider);
    final settingsAsync = ref.watch(settingsProvider);
    final colorScheme = Theme.of(context).colorScheme;
    final isDark =
        themeMode == ThemeMode.dark ||
        (themeMode == ThemeMode.system &&
            MediaQuery.platformBrightnessOf(context) == Brightness.dark);

    return Scaffold(
      appBar: AppBar(title: Text(l10n.settings)),
      body: settingsAsync.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (_, __) => Center(child: Text(l10n.unknownError)),
        data: (settings) => ListView(
          children: [
            // ── Account ──────────────────────────────────────────────
            _SectionHeader(title: l10n.account),
            ListTile(
              leading: const Icon(Icons.person),
              title: Text(authState.username ?? ''),
              subtitle: Text(authState.role ?? ''),
            ),
            ListTile(
              leading: const Icon(Icons.dns_outlined),
              title: Text(l10n.server),
              subtitle: Text(authState.serverUrl ?? ''),
            ),

            const Divider(),

            // ── Summarization ─────────────────────────────────────────
            _SectionHeader(title: l10n.summarizationSettings),
            _DefaultLengthTile(settings: settings),
            _ModelSelectorTile(settings: settings),
            _CustomPromptTile(settings: settings),

            const Divider(),

            // ── Integrations ──────────────────────────────────────────
            _SectionHeader(title: l10n.integrations),
            _ApiKeyTile(
              label: l10n.openaiApiKey,
              icon: Icons.key,
              maskedValue: settings.openaiApiKey,
              obscure: true,
              onSave: (value) =>
                  _updateSetting(context, ref, l10n, {'openaiApiKey': value}),
            ),
            _ApiKeyTile(
              label: l10n.anthropicApiKey,
              icon: Icons.key,
              maskedValue: settings.anthropicApiKey,
              obscure: true,
              onSave: (value) => _updateSetting(context, ref, l10n, {
                'anthropicApiKey': value,
              }),
            ),
            _ApiKeyTile(
              label: l10n.readeckUrl,
              icon: Icons.link,
              maskedValue: settings.readeckUrl,
              obscure: false,
              onSave: (value) =>
                  _updateSetting(context, ref, l10n, {'readeckUrl': value}),
            ),
            _ApiKeyTile(
              label: l10n.readeckApiKey,
              icon: Icons.key,
              maskedValue: settings.readeckApiKey,
              obscure: true,
              onSave: (value) =>
                  _updateSetting(context, ref, l10n, {'readeckApiKey': value}),
            ),
            _ApiKeyTile(
              label: l10n.webhookUrl,
              icon: Icons.webhook,
              maskedValue: settings.webhookUrl,
              obscure: false,
              onSave: (value) =>
                  _updateSetting(context, ref, l10n, {'webhookUrl': value}),
            ),

            const Divider(),

            // ── Appearance ────────────────────────────────────────────
            _SectionHeader(title: l10n.appearance),
            SwitchListTile(
              secondary: Icon(isDark ? Icons.dark_mode : Icons.light_mode),
              title: Text(isDark ? l10n.darkMode : l10n.lightMode),
              value: isDark,
              onChanged: (_) => ref.read(themeModeProvider.notifier).toggle(),
            ),
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 4, 16, 8),
              child: Row(
                children: [
                  const Icon(Icons.language, size: 20),
                  const SizedBox(width: 16),
                  Text(
                    l10n.language,
                    style: Theme.of(context).textTheme.bodyLarge,
                  ),
                  const Spacer(),
                  SegmentedButton<String>(
                    segments: const [
                      ButtonSegment(value: 'en', label: Text('EN')),
                      ButtonSegment(value: 'pt', label: Text('PT')),
                    ],
                    selected: {locale.languageCode},
                    onSelectionChanged: (selected) {
                      ref
                          .read(localeProvider.notifier)
                          .setLocale(Locale(selected.first));
                    },
                  ),
                ],
              ),
            ),

            const Divider(),

            // ── About ─────────────────────────────────────────────────
            _SectionHeader(title: l10n.about),
            ListTile(
              leading: const Icon(Icons.info_outline),
              title: Text(l10n.appName),
              subtitle: version.when(
                data: (v) => Text('v$v'),
                loading: () => const Text('...'),
                error: (_, __) => const Text('unknown'),
              ),
            ),

            const Divider(),

            // ── Logout ────────────────────────────────────────────────
            Padding(
              padding: const EdgeInsets.all(16),
              child: OutlinedButton.icon(
                onPressed: () => ref.read(authProvider.notifier).logout(),
                icon: Icon(Icons.logout, color: colorScheme.error),
                label: Text(
                  l10n.logout,
                  style: TextStyle(color: colorScheme.error),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _updateSetting(
    BuildContext context,
    WidgetRef ref,
    AppLocalizations l10n,
    Map<String, dynamic> patch,
  ) async {
    try {
      await ref.read(settingsProvider.notifier).save(patch);
      if (context.mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(l10n.settingsSaved)));
      }
    } catch (_) {
      if (context.mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(l10n.unknownError)));
      }
    }
  }
}

// ── Default length ──────────────────────────────────────────────────────────

class _DefaultLengthTile extends ConsumerWidget {
  final UserSettings settings;
  const _DefaultLengthTile({required this.settings});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = AppLocalizations.of(context)!;
    final current = settings.defaultLength;

    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 4, 16, 8),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            l10n.defaultLength,
            style: Theme.of(context).textTheme.bodyLarge,
          ),
          const SizedBox(height: 8),
          SegmentedButton<String?>(
            segments: [
              ButtonSegment<String?>(
                value: null,
                label: Text(l10n.lengthDefault),
              ),
              ButtonSegment<String?>(
                value: 'short',
                label: Text(l10n.lengthShort),
              ),
              ButtonSegment<String?>(
                value: 'medium',
                label: Text(l10n.lengthMedium),
              ),
              ButtonSegment<String?>(
                value: 'long',
                label: Text(l10n.lengthLong),
              ),
            ],
            selected: {current},
            onSelectionChanged: (selected) async {
              try {
                await ref.read(settingsProvider.notifier).save({
                  'defaultLength': selected.first,
                });
                if (context.mounted) {
                  ScaffoldMessenger.of(
                    context,
                  ).showSnackBar(SnackBar(content: Text(l10n.settingsSaved)));
                }
              } catch (_) {
                if (context.mounted) {
                  ScaffoldMessenger.of(
                    context,
                  ).showSnackBar(SnackBar(content: Text(l10n.unknownError)));
                }
              }
            },
          ),
        ],
      ),
    );
  }
}

// ── Model selector ──────────────────────────────────────────────────────────

class _ModelSelectorTile extends ConsumerWidget {
  final UserSettings settings;
  const _ModelSelectorTile({required this.settings});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = AppLocalizations.of(context)!;

    return ListTile(
      leading: const Icon(Icons.psychology_outlined),
      title: Text(l10n.model),
      subtitle: Text(settings.model ?? l10n.lengthDefault),
      trailing: const Icon(Icons.chevron_right),
      onTap: () => _showModelSheet(context, ref, l10n, settings.model),
    );
  }

  void _showModelSheet(
    BuildContext context,
    WidgetRef ref,
    AppLocalizations l10n,
    String? currentModel,
  ) {
    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      builder: (ctx) => _ModelBottomSheet(
        currentModel: currentModel,
        onSelect: (modelId) async {
          Navigator.of(ctx).pop();
          try {
            await ref.read(settingsProvider.notifier).save({'model': modelId});
            if (context.mounted) {
              ScaffoldMessenger.of(
                context,
              ).showSnackBar(SnackBar(content: Text(l10n.settingsSaved)));
            }
          } catch (_) {
            if (context.mounted) {
              ScaffoldMessenger.of(
                context,
              ).showSnackBar(SnackBar(content: Text(l10n.unknownError)));
            }
          }
        },
      ),
    );
  }
}

class _ModelBottomSheet extends ConsumerWidget {
  final String? currentModel;
  final void Function(String modelId) onSelect;

  const _ModelBottomSheet({required this.currentModel, required this.onSelect});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = AppLocalizations.of(context)!;
    final modelsAsync = ref.watch(modelsProvider);

    return DraggableScrollableSheet(
      initialChildSize: 0.6,
      maxChildSize: 0.9,
      minChildSize: 0.4,
      expand: false,
      builder: (_, scrollController) {
        return Column(
          children: [
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 16, 16, 8),
              child: Text(
                l10n.selectModel,
                style: Theme.of(context).textTheme.titleMedium,
              ),
            ),
            const Divider(),
            Expanded(
              child: modelsAsync.when(
                loading: () => const Center(child: CircularProgressIndicator()),
                error: (_, __) => Center(
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text(l10n.unknownError),
                      const SizedBox(height: 8),
                      TextButton(
                        onPressed: () => ref.invalidate(modelsProvider),
                        child: Text(l10n.retry),
                      ),
                    ],
                  ),
                ),
                data: (providers) {
                  final configured = providers
                      .where((p) => p.configured)
                      .toList();
                  if (configured.isEmpty) {
                    return Center(child: Text(l10n.noResults));
                  }
                  return ListView.builder(
                    controller: scrollController,
                    itemCount: configured.fold<int>(
                      0,
                      (sum, p) => sum + 1 + p.models.length,
                    ),
                    itemBuilder: (_, index) {
                      var i = 0;
                      for (final provider in configured) {
                        if (index == i) {
                          return _ProviderHeader(provider: provider);
                        }
                        i++;
                        for (final model in provider.models) {
                          if (index == i) {
                            return ListTile(
                              title: Text(model.name),
                              subtitle: model.description.isNotEmpty
                                  ? Text(model.description)
                                  : null,
                              trailing: currentModel == model.id
                                  ? const Icon(Icons.check)
                                  : null,
                              onTap: () => onSelect(model.id),
                            );
                          }
                          i++;
                        }
                      }
                      return const SizedBox.shrink();
                    },
                  );
                },
              ),
            ),
          ],
        );
      },
    );
  }
}

class _ProviderHeader extends StatelessWidget {
  final LlmProvider provider;
  const _ProviderHeader({required this.provider});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 4),
      child: Text(
        provider.name,
        style: Theme.of(context).textTheme.labelLarge?.copyWith(
          color: Theme.of(context).colorScheme.primary,
          fontWeight: FontWeight.bold,
        ),
      ),
    );
  }
}

// ── Custom prompt ───────────────────────────────────────────────────────────

class _CustomPromptTile extends ConsumerWidget {
  final UserSettings settings;
  const _CustomPromptTile({required this.settings});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = AppLocalizations.of(context)!;

    return ListTile(
      leading: const Icon(Icons.edit_note),
      title: Text(l10n.customPrompt),
      subtitle: Text(
        settings.customPrompt?.isNotEmpty == true
            ? settings.customPrompt!
            : l10n.keyNotSet,
        maxLines: 1,
        overflow: TextOverflow.ellipsis,
      ),
      trailing: const Icon(Icons.chevron_right),
      onTap: () => _showPromptDialog(context, ref, l10n),
    );
  }

  Future<void> _showPromptDialog(
    BuildContext context,
    WidgetRef ref,
    AppLocalizations l10n,
  ) async {
    final controller = TextEditingController(text: settings.customPrompt ?? '');

    await showDialog<void>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(l10n.customPrompt),
        content: TextField(
          controller: controller,
          maxLines: 6,
          decoration: InputDecoration(
            hintText: l10n.customPromptHint,
            border: const OutlineInputBorder(),
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(),
            child: Text(l10n.cancel),
          ),
          FilledButton(
            onPressed: () async {
              Navigator.of(ctx).pop();
              try {
                await ref.read(settingsProvider.notifier).save({
                  'customPrompt': controller.text.trim().isEmpty
                      ? null
                      : controller.text.trim(),
                });
                if (context.mounted) {
                  ScaffoldMessenger.of(
                    context,
                  ).showSnackBar(SnackBar(content: Text(l10n.settingsSaved)));
                }
              } catch (_) {
                if (context.mounted) {
                  ScaffoldMessenger.of(
                    context,
                  ).showSnackBar(SnackBar(content: Text(l10n.unknownError)));
                }
              }
            },
            child: Text(l10n.saveNotes),
          ),
        ],
      ),
    );
    Future.delayed(const Duration(milliseconds: 400), controller.dispose);
  }
}

// ── API key / URL tile ──────────────────────────────────────────────────────

class _ApiKeyTile extends StatelessWidget {
  final String label;
  final IconData icon;
  final String? maskedValue;
  final bool obscure;
  final Future<void> Function(String value) onSave;

  const _ApiKeyTile({
    required this.label,
    required this.icon,
    required this.maskedValue,
    required this.obscure,
    required this.onSave,
  });

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final subtitle = maskedValue?.isNotEmpty == true
        ? maskedValue!
        : l10n.keyNotSet;

    return ListTile(
      leading: Icon(icon),
      title: Text(label),
      subtitle: Text(subtitle),
      trailing: IconButton(
        icon: const Icon(Icons.edit_outlined),
        onPressed: () => _showEditDialog(context, l10n),
      ),
      onTap: () => _showEditDialog(context, l10n),
    );
  }

  Future<void> _showEditDialog(
    BuildContext context,
    AppLocalizations l10n,
  ) async {
    final controller = TextEditingController();
    bool isObscured = obscure;

    await showDialog<void>(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setState) => AlertDialog(
          title: Text(label),
          content: TextField(
            controller: controller,
            obscureText: isObscured,
            decoration: InputDecoration(
              hintText: label,
              border: const OutlineInputBorder(),
              suffixIcon: obscure
                  ? IconButton(
                      icon: Icon(
                        isObscured ? Icons.visibility : Icons.visibility_off,
                      ),
                      onPressed: () => setState(() => isObscured = !isObscured),
                    )
                  : null,
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(ctx).pop(),
              child: Text(l10n.cancel),
            ),
            FilledButton(
              onPressed: () async {
                final value = controller.text.trim();
                Navigator.of(ctx).pop();
                await onSave(value);
              },
              child: Text(l10n.saveNotes),
            ),
          ],
        ),
      ),
    );
    Future.delayed(const Duration(milliseconds: 400), controller.dispose);
  }
}

// ── Section header ──────────────────────────────────────────────────────────

class _SectionHeader extends StatelessWidget {
  final String title;
  const _SectionHeader({required this.title});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 4),
      child: Text(
        title,
        style: Theme.of(context).textTheme.titleSmall?.copyWith(
          color: Theme.of(context).colorScheme.primary,
          fontWeight: FontWeight.bold,
        ),
      ),
    );
  }
}
