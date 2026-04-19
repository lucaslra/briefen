import 'dart:async';

import 'package:flutter/foundation.dart' hide Summary;
import 'package:intl/intl.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:briefen/l10n/generated/app_localizations.dart';
import 'package:flutter_markdown/flutter_markdown.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:share_plus/share_plus.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../summarize/data/summarize_repository.dart';
import '../../summarize/domain/summary.dart';
import '../data/reading_list_repository.dart';
import '../providers.dart';

/// Fetches a single summary.
/// 1. Synchronous cache hit (reading list already loaded).
/// 2. Direct GET /api/summaries/{id} — filter-independent.
final summaryDetailProvider = FutureProvider.autoDispose
    .family<Summary?, String>((ref, id) async {
      // 1. Synchronous cache hit
      final cached = ref
          .read(readingListProvider)
          .whenOrNull(
            data: (data) => data.content.where((s) => s.id == id).firstOrNull,
          );
      if (cached != null) return cached;

      // 2. Direct API lookup — filter-independent
      final repo = ref.read(readingListRepositoryProvider);
      return repo.getSummaryById(id);
    });

class SummaryDetailScreen extends ConsumerStatefulWidget {
  final String summaryId;

  const SummaryDetailScreen({super.key, required this.summaryId});

  @override
  ConsumerState<SummaryDetailScreen> createState() =>
      _SummaryDetailScreenState();
}

class _SummaryDetailScreenState extends ConsumerState<SummaryDetailScreen> {
  Timer? _markReadTimer;
  bool _isAdjusting = false;
  bool _disposed = false;

  @override
  void initState() {
    super.initState();
    // Auto-mark as read after 3 seconds
    _markReadTimer = Timer(const Duration(seconds: 3), () {
      if (_disposed) return;
      final summary = ref
          .read(summaryDetailProvider(widget.summaryId))
          .valueOrNull;
      if (summary != null && !summary.isRead) {
        ref.read(readingListActionsProvider).toggleRead(summary.id, true);
      }
    });
  }

  @override
  void dispose() {
    _disposed = true;
    _markReadTimer?.cancel();
    super.dispose();
  }

  Future<void> _adjustLength(String? lengthHint, Summary summary) async {
    if (summary.url == null) return;
    setState(() => _isAdjusting = true);
    try {
      final repo = ref.read(summarizeRepositoryProvider);
      await repo.summarize(
        url: summary.url,
        lengthHint: lengthHint,
        refresh: true,
      );
      ref.invalidate(summaryDetailProvider(widget.summaryId));
      ref.invalidate(readingListProvider);
    } catch (e) {
      if (mounted) {
        final l10n = AppLocalizations.of(context)!;
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(l10n.summarizeError)));
      }
    } finally {
      if (mounted) setState(() => _isAdjusting = false);
    }
  }

  Future<void> _editNotes(
    BuildContext context,
    AppLocalizations l10n,
    Summary summary,
  ) async {
    final controller = TextEditingController(text: summary.notes ?? '');
    final saved = await showDialog<String>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(l10n.notes),
        content: TextField(
          controller: controller,
          autofocus: true,
          maxLines: 6,
          decoration: InputDecoration(
            hintText: l10n.notesHint,
            border: const OutlineInputBorder(),
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: Text(l10n.cancel),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(ctx, controller.text),
            child: Text(l10n.saveNotes),
          ),
        ],
      ),
    );
    Future.delayed(const Duration(milliseconds: 400), controller.dispose);
    if (saved != null && context.mounted) {
      await ref.read(readingListActionsProvider).updateNotes(summary.id, saved);
      ref.invalidate(summaryDetailProvider(widget.summaryId));
      if (context.mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(l10n.notesUpdated)));
      }
    }
  }

  Future<void> _editTags(
    BuildContext context,
    AppLocalizations l10n,
    Summary summary,
  ) async {
    final tags = List<String>.from(summary.tags);
    final tagController = TextEditingController();

    await showDialog<void>(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setStateDialog) => AlertDialog(
          title: Text(l10n.tags),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Wrap(
                spacing: 6,
                runSpacing: 6,
                children: tags
                    .map(
                      (tag) => Chip(
                        label: Text(tag),
                        onDeleted: () => setStateDialog(() => tags.remove(tag)),
                      ),
                    )
                    .toList(),
              ),
              const SizedBox(height: 12),
              Row(
                children: [
                  Expanded(
                    child: TextField(
                      controller: tagController,
                      decoration: InputDecoration(
                        hintText: l10n.tagHint,
                        border: const OutlineInputBorder(),
                        isDense: true,
                      ),
                      textInputAction: TextInputAction.done,
                      onSubmitted: (v) {
                        final t = v.trim();
                        if (t.isNotEmpty && !tags.contains(t)) {
                          setStateDialog(() => tags.add(t));
                          tagController.clear();
                        }
                      },
                    ),
                  ),
                  IconButton(
                    icon: const Icon(Icons.add),
                    onPressed: () {
                      final t = tagController.text.trim();
                      if (t.isNotEmpty && !tags.contains(t)) {
                        setStateDialog(() => tags.add(t));
                        tagController.clear();
                      }
                    },
                  ),
                ],
              ),
            ],
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx),
              child: Text(l10n.cancel),
            ),
            FilledButton(
              onPressed: () => Navigator.pop(ctx),
              child: Text(l10n.saveNotes),
            ),
          ],
        ),
      ),
    );
    Future.delayed(const Duration(milliseconds: 400), tagController.dispose);

    if (!listEquals(tags, summary.tags) && context.mounted) {
      await ref.read(readingListActionsProvider).updateTags(summary.id, tags);
      ref.invalidate(summaryDetailProvider(widget.summaryId));
      if (context.mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(l10n.tagsUpdated)));
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final colorScheme = Theme.of(context).colorScheme;
    final textTheme = Theme.of(context).textTheme;

    final summaryAsync = ref.watch(summaryDetailProvider(widget.summaryId));

    return summaryAsync.when(
      loading: () => Scaffold(
        appBar: AppBar(),
        body: const Center(child: CircularProgressIndicator()),
      ),
      error: (_, _) => Scaffold(
        appBar: AppBar(),
        body: Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(l10n.unknownError),
              const SizedBox(height: 8),
              FilledButton.tonal(
                onPressed: () =>
                    ref.invalidate(summaryDetailProvider(widget.summaryId)),
                child: Text(l10n.retry),
              ),
            ],
          ),
        ),
      ),
      data: (summary) {
        if (summary == null) {
          return Scaffold(
            appBar: AppBar(),
            body: Center(child: Text(l10n.noResults)),
          );
        }

        return Scaffold(
          appBar: AppBar(
            leading: IconButton(
              icon: const Icon(Icons.arrow_back),
              onPressed: () => context.pop(),
            ),
            actions: [
              PopupMenuButton<String>(
                onSelected: (value) async {
                  switch (value) {
                    case 'toggle_read':
                      ref
                          .read(readingListActionsProvider)
                          .toggleRead(summary.id, !summary.isRead);
                    case 'edit_notes':
                      if (context.mounted) {
                        await _editNotes(context, l10n, summary);
                      }
                    case 'edit_tags':
                      if (context.mounted) {
                        await _editTags(context, l10n, summary);
                      }
                    case 'delete':
                      final confirmed = await showDialog<bool>(
                        context: context,
                        builder: (ctx) => AlertDialog(
                          title: Text(l10n.deleteConfirmTitle),
                          content: Text(l10n.deleteConfirmMessage),
                          actions: [
                            TextButton(
                              onPressed: () => Navigator.pop(ctx, false),
                              child: Text(l10n.cancel),
                            ),
                            TextButton(
                              onPressed: () => Navigator.pop(ctx, true),
                              child: Text(
                                l10n.delete,
                                style: TextStyle(color: colorScheme.error),
                              ),
                            ),
                          ],
                        ),
                      );
                      if (confirmed == true && context.mounted) {
                        ref.read(readingListActionsProvider).delete(summary.id);
                        context.pop();
                      }
                  }
                },
                itemBuilder: (context) => [
                  PopupMenuItem(
                    value: 'toggle_read',
                    child: Text(
                      summary.isRead ? l10n.markAsUnread : l10n.markAsRead,
                    ),
                  ),
                  PopupMenuItem(value: 'edit_notes', child: Text(l10n.notes)),
                  PopupMenuItem(value: 'edit_tags', child: Text(l10n.tags)),
                  PopupMenuItem(
                    value: 'delete',
                    child: Text(
                      l10n.delete,
                      style: TextStyle(color: colorScheme.error),
                    ),
                  ),
                ],
              ),
            ],
          ),
          body: SingleChildScrollView(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  summary.title,
                  style: textTheme.headlineSmall?.copyWith(
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const SizedBox(height: 8),
                Row(
                  children: [
                    if (summary.domain.isNotEmpty) ...[
                      Text(
                        summary.domain,
                        style: textTheme.bodyMedium?.copyWith(
                          color: colorScheme.primary,
                        ),
                      ),
                      Text(
                        ' · ',
                        style: TextStyle(color: colorScheme.onSurfaceVariant),
                      ),
                    ],
                    Text(
                      _formatFullDate(summary.savedAt),
                      style: textTheme.bodyMedium?.copyWith(
                        color: colorScheme.onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
                Row(
                  children: [
                    Icon(
                      Icons.smart_toy_outlined,
                      size: 14,
                      color: colorScheme.onSurfaceVariant,
                    ),
                    const SizedBox(width: 4),
                    Text(
                      summary.modelUsed,
                      style: textTheme.bodySmall?.copyWith(
                        color: colorScheme.onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
                const Divider(height: 32),
                MarkdownBody(
                  data: summary.summary,
                  selectable: true,
                  styleSheet: MarkdownStyleSheet.fromTheme(Theme.of(context)),
                  onTapLink: (_, href, __) {
                    if (href != null) {
                      launchUrl(
                        Uri.parse(href),
                        mode: LaunchMode.externalApplication,
                      );
                    }
                  },
                ),
                // Length adjustment (only for URL-based summaries)
                if (summary.url != null) ...[
                  const SizedBox(height: 16),
                  if (_isAdjusting)
                    const Center(child: CircularProgressIndicator())
                  else
                    Wrap(
                      spacing: 8,
                      children: [
                        OutlinedButton(
                          onPressed: () => _adjustLength('shorter', summary),
                          child: Text(l10n.makeShorter),
                        ),
                        OutlinedButton(
                          onPressed: () => _adjustLength('longer', summary),
                          child: Text(l10n.makeLonger),
                        ),
                        OutlinedButton(
                          onPressed: () => _adjustLength(null, summary),
                          child: Text(l10n.regenerate),
                        ),
                      ],
                    ),
                ],
                // Tags
                const SizedBox(height: 24),
                Row(
                  children: [
                    Text(
                      l10n.tags,
                      style: textTheme.titleSmall?.copyWith(
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const Spacer(),
                    IconButton(
                      icon: const Icon(Icons.edit_outlined, size: 18),
                      visualDensity: VisualDensity.compact,
                      onPressed: () => _editTags(context, l10n, summary),
                    ),
                  ],
                ),
                const SizedBox(height: 4),
                if (summary.tags.isNotEmpty)
                  Wrap(
                    spacing: 6,
                    runSpacing: 6,
                    children: summary.tags.map((tag) {
                      return ActionChip(
                        label: Text(tag),
                        onPressed: () {
                          ref.read(readingListTagProvider.notifier).state = tag;
                          context.go('/reading-list');
                        },
                      );
                    }).toList(),
                  )
                else
                  Text(
                    '—',
                    style: textTheme.bodySmall?.copyWith(
                      color: colorScheme.onSurfaceVariant,
                    ),
                  ),
                // Notes
                const SizedBox(height: 24),
                Row(
                  children: [
                    Text(
                      l10n.notes,
                      style: textTheme.titleSmall?.copyWith(
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const Spacer(),
                    IconButton(
                      icon: const Icon(Icons.edit_outlined, size: 18),
                      visualDensity: VisualDensity.compact,
                      onPressed: () => _editNotes(context, l10n, summary),
                    ),
                  ],
                ),
                const SizedBox(height: 4),
                if (summary.notes != null && summary.notes!.isNotEmpty)
                  Card(
                    child: Padding(
                      padding: const EdgeInsets.all(12),
                      child: Text(summary.notes!),
                    ),
                  )
                else
                  GestureDetector(
                    onTap: () => _editNotes(context, l10n, summary),
                    child: Text(
                      l10n.notesHint,
                      style: textTheme.bodySmall?.copyWith(
                        color: colorScheme.onSurfaceVariant,
                      ),
                    ),
                  ),
                const SizedBox(height: 24),
                Row(
                  children: [
                    if (summary.url != null)
                      Expanded(
                        child: OutlinedButton.icon(
                          onPressed: () async {
                            final uri = Uri.tryParse(summary.url!);
                            if (uri != null) await launchUrl(uri);
                          },
                          icon: const Icon(Icons.open_in_browser),
                          label: Text(l10n.openArticle),
                        ),
                      ),
                    if (summary.url != null) const SizedBox(width: 8),
                    Expanded(
                      child: FilledButton.tonalIcon(
                        onPressed: () {
                          final text =
                              '## ${summary.title}\n*${l10n.source}: ${summary.url ?? ''} *\n\n${summary.summary}';
                          SharePlus.instance.share(ShareParams(text: text));
                        },
                        icon: const Icon(Icons.share),
                        label: Text(l10n.share),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                SizedBox(
                  width: double.infinity,
                  child: TextButton.icon(
                    onPressed: () {
                      Clipboard.setData(ClipboardData(text: summary.summary));
                      ScaffoldMessenger.of(
                        context,
                      ).showSnackBar(SnackBar(content: Text(l10n.copied)));
                    },
                    icon: const Icon(Icons.copy),
                    label: Text(l10n.copyToClipboard),
                  ),
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  String _formatFullDate(DateTime date) {
    return DateFormat('MMM d, y').format(date);
  }
}
