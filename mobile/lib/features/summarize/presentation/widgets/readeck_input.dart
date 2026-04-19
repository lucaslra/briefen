import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../l10n/generated/app_localizations.dart';
import '../../data/readeck_repository.dart';
import '../../domain/readeck_bookmark.dart';
import '../../providers.dart';

class ReadeckInput extends ConsumerStatefulWidget {
  const ReadeckInput({super.key});

  @override
  ConsumerState<ReadeckInput> createState() => _ReadeckInputState();
}

class _ReadeckInputState extends ConsumerState<ReadeckInput> {
  bool? _configured; // null = loading, true/false = known
  List<ReadeckBookmark> _bookmarks = [];
  bool _loadingBookmarks = false;
  bool _hasMore = false;
  int _page = 1;
  String _search = '';
  String? _error;
  String? _summarizingId; // ID of bookmark currently being fetched/summarized

  final _searchController = TextEditingController();

  @override
  void initState() {
    super.initState();
    _checkStatus();
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  Future<void> _checkStatus() async {
    try {
      final repo = ref.read(readeckRepositoryProvider);
      final configured = await repo.isConfigured();
      if (mounted) {
        setState(() => _configured = configured);
        if (configured) _fetchBookmarks(reset: true);
      }
    } catch (_) {
      if (mounted) setState(() => _configured = false);
    }
  }

  Future<void> _fetchBookmarks({bool reset = false}) async {
    if (_loadingBookmarks) return;
    final page = reset ? 1 : _page + 1;
    setState(() {
      _loadingBookmarks = true;
      _error = null;
      if (reset) {
        _bookmarks = [];
        _page = 1;
      }
    });

    try {
      final repo = ref.read(readeckRepositoryProvider);
      final items = await repo.getBookmarks(
        page: page,
        search: _search.isEmpty ? null : _search,
      );
      if (mounted) {
        setState(() {
          _bookmarks = reset ? items : [..._bookmarks, ...items];
          _hasMore = items.length >= 20;
          _page = page;
          _loadingBookmarks = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _error = e.toString();
          _loadingBookmarks = false;
        });
      }
    }
  }

  Future<void> _summarize(ReadeckBookmark bookmark) async {
    setState(() => _summarizingId = bookmark.id);

    try {
      final repo = ref.read(readeckRepositoryProvider);
      final article = await repo.getArticle(bookmark.id);

      if (!mounted) return;

      if (article.hasError && article.text.isEmpty) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(article.error!)));
        setState(() => _summarizingId = null);
        return;
      }

      await ref
          .read(summarizeActionProvider.notifier)
          .summarizeText(article.text, article.title);
    } catch (e) {
      if (mounted) {
        final l10n = AppLocalizations.of(context)!;
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(l10n.summarizeError)));
      }
    } finally {
      if (mounted) setState(() => _summarizingId = null);
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final colorScheme = Theme.of(context).colorScheme;
    final summarizeState = ref.watch(summarizeActionProvider);
    final isSummarizing =
        summarizeState.status == SummarizeStatus.loading ||
        _summarizingId != null;

    // Navigate to summary detail on success
    ref.listen(summarizeActionProvider, (prev, next) {
      if (next.status == SummarizeStatus.success &&
          next.summary != null &&
          prev?.status == SummarizeStatus.loading &&
          mounted) {
        context.push('/reading-list/${next.summary!.id}');
      }
    });

    // ── Not yet checked ─────────────────────────────────────────────────
    if (_configured == null) {
      return const Center(child: CircularProgressIndicator());
    }

    // ── Not configured ──────────────────────────────────────────────────
    if (!_configured!) {
      return Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.link_off, size: 48, color: colorScheme.onSurfaceVariant),
            const SizedBox(height: 16),
            Text(
              l10n.readeckNotConfigured,
              textAlign: TextAlign.center,
              style: Theme.of(context).textTheme.bodyLarge,
            ),
            const SizedBox(height: 12),
            FilledButton.tonal(
              onPressed: () => context.push('/settings'),
              child: Text(l10n.settings),
            ),
          ],
        ),
      );
    }

    // ── Configured ──────────────────────────────────────────────────────
    return Column(
      children: [
        // Search bar
        TextField(
          controller: _searchController,
          decoration: InputDecoration(
            hintText: l10n.search,
            prefixIcon: const Icon(Icons.search),
            suffixIcon: _search.isNotEmpty
                ? IconButton(
                    icon: const Icon(Icons.clear),
                    onPressed: () {
                      _searchController.clear();
                      setState(() => _search = '');
                      _fetchBookmarks(reset: true);
                    },
                  )
                : null,
          ),
          textInputAction: TextInputAction.search,
          onSubmitted: (v) {
            setState(() => _search = v.trim());
            _fetchBookmarks(reset: true);
          },
          onChanged: (v) => setState(() => _search = v),
          enabled: !isSummarizing,
        ),
        const SizedBox(height: 12),

        // Bookmark list
        if (_loadingBookmarks && _bookmarks.isEmpty)
          const Padding(
            padding: EdgeInsets.symmetric(vertical: 32),
            child: Center(child: CircularProgressIndicator()),
          )
        else if (_error != null)
          Padding(
            padding: const EdgeInsets.symmetric(vertical: 16),
            child: Column(
              children: [
                Text(
                  _error!,
                  style: TextStyle(color: colorScheme.error),
                  textAlign: TextAlign.center,
                ),
                const SizedBox(height: 8),
                TextButton(
                  onPressed: () => _fetchBookmarks(reset: true),
                  child: Text(l10n.retry),
                ),
              ],
            ),
          )
        else if (_bookmarks.isEmpty)
          Padding(
            padding: const EdgeInsets.symmetric(vertical: 32),
            child: Text(
              l10n.noResults,
              style: TextStyle(color: colorScheme.onSurfaceVariant),
            ),
          )
        else
          ListView.builder(
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            itemCount: _bookmarks.length + (_hasMore ? 1 : 0),
            itemBuilder: (context, index) {
              if (index == _bookmarks.length) {
                // Load more button
                return Padding(
                  padding: const EdgeInsets.symmetric(vertical: 8),
                  child: Center(
                    child: _loadingBookmarks
                        ? const CircularProgressIndicator()
                        : TextButton(
                            onPressed: () => _fetchBookmarks(),
                            child: Text(l10n.loadMore),
                          ),
                  ),
                );
              }
              final bookmark = _bookmarks[index];
              final isThisOne = _summarizingId == bookmark.id;
              return _BookmarkTile(
                bookmark: bookmark,
                loading: isThisOne,
                disabled: isSummarizing && !isThisOne,
                onTap: () => _summarize(bookmark),
              );
            },
          ),
      ],
    );
  }
}

class _BookmarkTile extends StatelessWidget {
  final ReadeckBookmark bookmark;
  final bool loading;
  final bool disabled;
  final VoidCallback onTap;

  const _BookmarkTile({
    required this.bookmark,
    required this.loading,
    required this.disabled,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;

    return Card(
      margin: const EdgeInsets.only(bottom: 8),
      child: ListTile(
        title: Text(
          bookmark.title,
          maxLines: 2,
          overflow: TextOverflow.ellipsis,
        ),
        subtitle: bookmark.url != null
            ? Text(
                Uri.tryParse(bookmark.url!)?.host ?? bookmark.url!,
                style: TextStyle(color: colorScheme.primary, fontSize: 12),
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
              )
            : null,
        trailing: loading
            ? const SizedBox(
                width: 20,
                height: 20,
                child: CircularProgressIndicator(strokeWidth: 2),
              )
            : Icon(
                Icons.summarize_outlined,
                color: disabled
                    ? colorScheme.onSurface.withValues(alpha: 0.38)
                    : colorScheme.primary,
              ),
        enabled: !disabled && !loading,
        onTap: disabled || loading ? null : onTap,
      ),
    );
  }
}
