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

// Reading list data — with offline cache fallback
final readingListProvider = FutureProvider.autoDispose<PaginatedSummaries>((
  ref,
) async {
  final repo = ref.read(readingListRepositoryProvider);
  final cache = ref.read(readingListCacheProvider);
  final filter = ref.watch(readingListFilterProvider);
  final search = ref.watch(readingListSearchProvider);
  final tag = ref.watch(readingListTagProvider);

  // Only cache unfiltered, unsearched results (keeps cache keys bounded).
  final cacheable = search.isEmpty && tag == null;

  try {
    final result = await repo.getSummaries(
      page: 0,
      size: 20,
      filter: filter.name,
      search: search.isNotEmpty ? search : null,
      tag: tag,
    );
    if (cacheable) await cache.save(filter.name, result);
    return result;
  } catch (_) {
    if (cacheable) {
      final cached = await cache.load(filter.name);
      if (cached != null) return cached;
    }
    rethrow;
  }
});

// Actions
class ReadingListActions {
  final Ref _ref;
  ReadingListActions(this._ref);

  Future<void> toggleRead(String id, bool isRead) async {
    try {
      final repo = _ref.read(readingListRepositoryProvider);
      await repo.toggleReadStatus(id, isRead);
      _ref.invalidate(readingListProvider);
      _ref.invalidate(unreadCountProvider);
    } catch (e) {
      rethrow;
    }
  }

  Future<void> delete(String id) async {
    try {
      final repo = _ref.read(readingListRepositoryProvider);
      await repo.deleteSummary(id);
      _ref.invalidate(readingListProvider);
      _ref.invalidate(unreadCountProvider);
    } catch (e) {
      rethrow;
    }
  }

  Future<void> updateNotes(String id, String notes) async {
    try {
      final repo = _ref.read(readingListRepositoryProvider);
      await repo.updateNotes(id, notes);
      _ref.invalidate(readingListProvider);
    } catch (e) {
      rethrow;
    }
  }

  Future<void> updateTags(String id, List<String> tags) async {
    try {
      final repo = _ref.read(readingListRepositoryProvider);
      await repo.updateTags(id, tags);
      _ref.invalidate(readingListProvider);
    } catch (e) {
      rethrow;
    }
  }

  Future<void> markAllRead() async {
    try {
      final repo = _ref.read(readingListRepositoryProvider);
      await repo.markAllRead();
      _ref.invalidate(readingListProvider);
      _ref.invalidate(unreadCountProvider);
    } catch (e) {
      rethrow;
    }
  }

  Future<void> markAllUnread() async {
    try {
      final repo = _ref.read(readingListRepositoryProvider);
      await repo.markAllUnread();
      _ref.invalidate(readingListProvider);
      _ref.invalidate(unreadCountProvider);
    } catch (e) {
      rethrow;
    }
  }
}

final readingListActionsProvider = Provider<ReadingListActions>((ref) {
  return ReadingListActions(ref);
});
