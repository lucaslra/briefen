import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:briefen/l10n/generated/app_localizations.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:share_plus/share_plus.dart';

import '../data/reading_list_repository.dart';
import '../providers.dart';
import 'widgets/filter_bar.dart';
import 'widgets/skeleton_card.dart';
import 'widgets/summary_card.dart';

class ReadingListScreen extends ConsumerStatefulWidget {
  const ReadingListScreen({super.key});

  @override
  ConsumerState<ReadingListScreen> createState() => _ReadingListScreenState();
}

class _ReadingListScreenState extends ConsumerState<ReadingListScreen> {
  final _searchController = TextEditingController();
  bool _showSearch = false;

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  Future<void> _export(AppLocalizations l10n) async {
    try {
      final repo = ref.read(readingListRepositoryProvider);
      final filter = ref.read(readingListFilterProvider).name;
      final markdown = await repo.exportMarkdown(filter: filter);
      await SharePlus.instance.share(
        ShareParams(text: markdown, subject: 'briefen-export.md'),
      );
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(l10n.unknownError)));
      }
    }
  }

  Future<void> _bulkAction(
    AppLocalizations l10n,
    Future<void> Function() action,
  ) async {
    HapticFeedback.mediumImpact();
    await action();
    if (mounted) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text(l10n.bulkUpdated)));
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final readingList = ref.watch(readingListProvider);
    final colorScheme = Theme.of(context).colorScheme;

    return Scaffold(
      appBar: AppBar(
        title: _showSearch
            ? TextField(
                controller: _searchController,
                autofocus: true,
                decoration: InputDecoration(
                  hintText: l10n.search,
                  border: InputBorder.none,
                ),
                onChanged: (value) {
                  ref.read(readingListSearchProvider.notifier).state = value;
                },
              )
            : Text(l10n.readingList),
        actions: [
          IconButton(
            icon: Icon(_showSearch ? Icons.close : Icons.search),
            onPressed: () {
              setState(() {
                _showSearch = !_showSearch;
                if (!_showSearch) {
                  _searchController.clear();
                  ref.read(readingListSearchProvider.notifier).state = '';
                }
              });
            },
          ),
          PopupMenuButton<String>(
            onSelected: (value) async {
              switch (value) {
                case 'mark_all_read':
                  await _bulkAction(
                    l10n,
                    () => ref.read(readingListActionsProvider).markAllRead(),
                  );
                case 'mark_all_unread':
                  await _bulkAction(
                    l10n,
                    () => ref.read(readingListActionsProvider).markAllUnread(),
                  );
                case 'export':
                  await _export(l10n);
              }
            },
            itemBuilder: (context) => [
              PopupMenuItem(
                value: 'mark_all_read',
                child: Text(l10n.markAllRead),
              ),
              PopupMenuItem(
                value: 'mark_all_unread',
                child: Text(l10n.markAllUnread),
              ),
              const PopupMenuDivider(),
              PopupMenuItem(
                value: 'export',
                child: Text(l10n.exportReadingList),
              ),
            ],
          ),
        ],
      ),
      body: Column(
        children: [
          const FilterBar(),
          Expanded(
            child: readingList.when(
              data: (data) {
                if (data.content.isEmpty) {
                  return Center(
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Icon(
                          Icons.menu_book_outlined,
                          size: 64,
                          color: colorScheme.onSurfaceVariant.withValues(
                            alpha: 0.5,
                          ),
                        ),
                        const SizedBox(height: 16),
                        Text(
                          ref.watch(readingListSearchProvider).isNotEmpty
                              ? l10n.noResults
                              : l10n.noSummaries,
                          style: Theme.of(context).textTheme.bodyLarge
                              ?.copyWith(color: colorScheme.onSurfaceVariant),
                        ),
                      ],
                    ),
                  );
                }

                return Column(
                  children: [
                    // Offline banner
                    if (data.isOffline)
                      Material(
                        color: colorScheme.tertiaryContainer,
                        child: Padding(
                          padding: const EdgeInsets.symmetric(
                            horizontal: 16,
                            vertical: 6,
                          ),
                          child: Row(
                            children: [
                              Icon(
                                Icons.wifi_off,
                                size: 16,
                                color: colorScheme.onTertiaryContainer,
                              ),
                              const SizedBox(width: 8),
                              Text(
                                l10n.offlineCached,
                                style: TextStyle(
                                  color: colorScheme.onTertiaryContainer,
                                  fontSize: 13,
                                ),
                              ),
                            ],
                          ),
                        ),
                      ),
                    Expanded(
                      child: RefreshIndicator(
                        onRefresh: () async {
                          ref.invalidate(readingListProvider);
                        },
                        child: ListView.builder(
                          padding: const EdgeInsets.symmetric(horizontal: 8),
                          itemCount: data.content.length,
                          itemBuilder: (context, index) {
                            final summary = data.content[index];
                            return _AnimatedItem(
                              index: index,
                              child: SummaryCard(
                                summary: summary,
                                onTap: () {
                                  context.push('/reading-list/${summary.id}');
                                },
                                onToggleRead: () {
                                  ref
                                      .read(readingListActionsProvider)
                                      .toggleRead(summary.id, !summary.isRead);
                                },
                                onDelete: () async {
                                  HapticFeedback.heavyImpact();
                                  final confirmed = await showDialog<bool>(
                                    context: context,
                                    builder: (context) => AlertDialog(
                                      title: Text(l10n.deleteConfirmTitle),
                                      content: Text(l10n.deleteConfirmMessage),
                                      actions: [
                                        TextButton(
                                          onPressed: () =>
                                              Navigator.pop(context, false),
                                          child: Text(l10n.cancel),
                                        ),
                                        TextButton(
                                          onPressed: () =>
                                              Navigator.pop(context, true),
                                          child: Text(
                                            l10n.delete,
                                            style: TextStyle(
                                              color: colorScheme.error,
                                            ),
                                          ),
                                        ),
                                      ],
                                    ),
                                  );
                                  if (confirmed == true && mounted) {
                                    await ref
                                        .read(readingListActionsProvider)
                                        .delete(summary.id);
                                  }
                                },
                              ),
                            );
                          },
                        ),
                      ),
                    ),
                  ],
                );
              },
              loading: () => const SkeletonListView(),
              error: (error, _) => Center(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(l10n.unknownError),
                    const SizedBox(height: 8),
                    FilledButton.tonal(
                      onPressed: () => ref.invalidate(readingListProvider),
                      child: Text(l10n.retry),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

/// Fades and slides each list item in with a small staggered delay.
class _AnimatedItem extends StatelessWidget {
  final int index;
  final Widget child;

  const _AnimatedItem({required this.index, required this.child});

  @override
  Widget build(BuildContext context) {
    // Cap the delay so items deep in the list don't wait too long.
    final delay = Duration(milliseconds: (index * 40).clamp(0, 200));
    final totalDuration = const Duration(milliseconds: 300) + delay;

    return TweenAnimationBuilder<double>(
      tween: Tween(begin: 0, end: 1),
      duration: totalDuration,
      curve: Interval(
        delay.inMilliseconds / totalDuration.inMilliseconds,
        1.0,
        curve: Curves.easeOut,
      ),
      builder: (_, value, child) => Opacity(
        opacity: value,
        child: Transform.translate(
          offset: Offset(0, 16 * (1 - value)),
          child: child,
        ),
      ),
      child: child,
    );
  }
}
