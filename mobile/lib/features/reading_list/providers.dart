import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../summarize/domain/summary.dart';
import '../summarize/providers.dart';
import 'data/reading_list_cache.dart';
import 'data/reading_list_repository.dart';

// Filter state
enum ReadingListFilter { all, unread, read }

final readingListFilterProvider = StateProvider<ReadingListFilter>(
  (ref) => ReadingListFilter.unread,
);

final readingListSearchProvider = StateProvider<String>((ref) => '');
final readingListTagProvider = StateProvider<String?>((ref) => null);

// Reading list data — paginated, with offline cache fallback on page 0
class ReadingListNotifier extends AutoDisposeAsyncNotifier<PaginatedSummaries> {
  int _page = 0;
  List<Summary> _allItems = [];
  bool _hasMore = true;

  @override
  Future<PaginatedSummaries> build() async {
    _page = 0;
    _allItems = [];
    _hasMore = true;

    final filter = ref.watch(readingListFilterProvider);
    final search = ref.watch(readingListSearchProvider);
    final tag = ref.watch(readingListTagProvider);

    final repo = ref.read(readingListRepositoryProvider);
    final cache = ref.read(readingListCacheProvider);
    final cacheable = search.isEmpty && tag == null;

    try {
      final result = await repo.getSummaries(
        page: 0,
        size: 20,
        filter: filter.name,
        search: search.isNotEmpty ? search : null,
        tag: tag,
      );
      _allItems = List<Summary>.from(result.content);
      _hasMore = !result.last;
      if (cacheable) await cache.save(filter.name, result);
      return _merged(result);
    } catch (_) {
      if (cacheable) {
        final cached = await cache.load(filter.name);
        if (cached != null) {
          _allItems = List<Summary>.from(cached.content);
          _hasMore = false;
          return cached;
        }
      }
      rethrow;
    }
  }

  Future<void> loadMore() async {
    if (!_hasMore) return;
    final current = state.valueOrNull;
    if (current == null) return;

    _page++;
    final filter = ref.read(readingListFilterProvider);
    final search = ref.read(readingListSearchProvider);
    final tag = ref.read(readingListTagProvider);
    final repo = ref.read(readingListRepositoryProvider);

    try {
      final result = await repo.getSummaries(
        page: _page,
        size: 20,
        filter: filter.name,
        search: search.isNotEmpty ? search : null,
        tag: tag,
      );
      _allItems = [..._allItems, ...result.content];
      _hasMore = !result.last;
      state = AsyncData(_merged(result));
    } catch (e, st) {
      _page--;
      Error.throwWithStackTrace(e, st);
    }
  }

  PaginatedSummaries _merged(PaginatedSummaries page) => PaginatedSummaries(
    content: List.unmodifiable(_allItems),
    totalElements: page.totalElements,
    totalPages: page.totalPages,
    first: _page == 0,
    last: !_hasMore,
    isOffline: page.isOffline,
  );
}

final readingListProvider =
    AsyncNotifierProvider.autoDispose<ReadingListNotifier, PaginatedSummaries>(
      ReadingListNotifier.new,
    );

// Actions
class ReadingListActions {
  final Ref _ref;
  ReadingListActions(this._ref);

  Future<void> toggleRead(String id, bool isRead) async {
    final repo = _ref.read(readingListRepositoryProvider);
    await repo.toggleReadStatus(id, isRead);
    _ref.invalidate(readingListProvider);
    _ref.invalidate(unreadCountProvider);
  }

  Future<void> delete(String id) async {
    final repo = _ref.read(readingListRepositoryProvider);
    await repo.deleteSummary(id);
    _ref.invalidate(readingListProvider);
    _ref.invalidate(unreadCountProvider);
  }

  Future<void> updateNotes(String id, String notes) async {
    final repo = _ref.read(readingListRepositoryProvider);
    await repo.updateNotes(id, notes);
    _ref.invalidate(readingListProvider);
  }

  Future<void> updateTags(String id, List<String> tags) async {
    final repo = _ref.read(readingListRepositoryProvider);
    await repo.updateTags(id, tags);
    _ref.invalidate(readingListProvider);
  }

  Future<void> markAllRead() async {
    final repo = _ref.read(readingListRepositoryProvider);
    await repo.markAllRead();
    _ref.invalidate(readingListProvider);
    _ref.invalidate(unreadCountProvider);
  }

  Future<void> markAllUnread() async {
    final repo = _ref.read(readingListRepositoryProvider);
    await repo.markAllUnread();
    _ref.invalidate(readingListProvider);
    _ref.invalidate(unreadCountProvider);
  }

  Future<void> loadMore() async {
    await _ref.read(readingListProvider.notifier).loadMore();
  }
}

final readingListActionsProvider = Provider<ReadingListActions>((ref) {
  return ReadingListActions(ref);
});
