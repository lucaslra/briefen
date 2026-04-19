import 'package:briefen/features/reading_list/data/reading_list_cache.dart';
import 'package:briefen/features/reading_list/data/reading_list_repository.dart';
import 'package:briefen/features/reading_list/providers.dart';
import 'package:briefen/features/summarize/domain/summary.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';

// ── Fakes ────────────────────────────────────────────────────────────────────

class MockReadingListRepository extends Mock implements ReadingListRepository {}

class FakeReadingListCache extends Fake implements ReadingListCache {
  @override
  Future<void> save(String filter, PaginatedSummaries data) async {}

  @override
  Future<PaginatedSummaries?> load(String filter) async => null;
}

// ── Helpers ───────────────────────────────────────────────────────────────────

Summary _summary(String id) => Summary(
  id: id,
  title: 'Title $id',
  summary: 'Summary $id',
  modelUsed: 'test',
  createdAt: DateTime(2024),
  isRead: false,
  savedAt: DateTime(2024),
);

PaginatedSummaries _page(List<String> ids, {bool last = true, int total = 0}) =>
    PaginatedSummaries(
      content: ids.map(_summary).toList(),
      totalElements: total == 0 ? ids.length : total,
      totalPages: last ? 1 : 2,
      first: true,
      last: last,
    );

ProviderContainer _container(MockReadingListRepository repo) {
  return ProviderContainer(
    overrides: [
      readingListRepositoryProvider.overrideWithValue(repo),
      readingListCacheProvider.overrideWith((_) => FakeReadingListCache()),
    ],
  );
}

// ── Tests ────────────────────────────────────────────────────────────────────

void main() {
  late MockReadingListRepository repo;

  setUp(() {
    repo = MockReadingListRepository();
    registerFallbackValue('all');
  });

  group('ReadingListNotifier — initial load', () {
    test('loads first page and exposes items', () async {
      when(
        () => repo.getSummaries(
          page: any(named: 'page'),
          size: any(named: 'size'),
          filter: any(named: 'filter'),
          search: any(named: 'search'),
          tag: any(named: 'tag'),
        ),
      ).thenAnswer((_) async => _page(['a', 'b', 'c']));

      final container = _container(repo);
      addTearDown(container.dispose);

      final result = await container.read(readingListProvider.future);
      expect(result.content.map((s) => s.id), ['a', 'b', 'c']);
      expect(result.last, isTrue);
    });

    test('last=false when server signals more pages', () async {
      when(
        () => repo.getSummaries(
          page: any(named: 'page'),
          size: any(named: 'size'),
          filter: any(named: 'filter'),
          search: any(named: 'search'),
          tag: any(named: 'tag'),
        ),
      ).thenAnswer((_) async => _page(['a', 'b'], last: false, total: 4));

      final container = _container(repo);
      addTearDown(container.dispose);

      final result = await container.read(readingListProvider.future);
      expect(result.last, isFalse);
    });
  });

  group('ReadingListNotifier — loadMore', () {
    test('appends second page to first page items', () async {
      var callCount = 0;
      when(
        () => repo.getSummaries(
          page: any(named: 'page'),
          size: any(named: 'size'),
          filter: any(named: 'filter'),
          search: any(named: 'search'),
          tag: any(named: 'tag'),
        ),
      ).thenAnswer((_) async {
        callCount++;
        return callCount == 1
            ? _page(['a', 'b'], last: false, total: 4)
            : _page(['c', 'd'], last: true, total: 4);
      });

      final container = _container(repo);
      addTearDown(container.dispose);

      await container.read(readingListProvider.future);
      await container.read(readingListProvider.notifier).loadMore();

      final result = container.read(readingListProvider).value!;
      expect(result.content.map((s) => s.id), ['a', 'b', 'c', 'd']);
      expect(result.last, isTrue);
    });

    test('loadMore is a no-op when already on last page', () async {
      when(
        () => repo.getSummaries(
          page: any(named: 'page'),
          size: any(named: 'size'),
          filter: any(named: 'filter'),
          search: any(named: 'search'),
          tag: any(named: 'tag'),
        ),
      ).thenAnswer((_) async => _page(['a', 'b']));

      final container = _container(repo);
      addTearDown(container.dispose);

      await container.read(readingListProvider.future);
      await container.read(readingListProvider.notifier).loadMore();

      // getSummaries called exactly once (no second call)
      verify(
        () => repo.getSummaries(
          page: any(named: 'page'),
          size: any(named: 'size'),
          filter: any(named: 'filter'),
          search: any(named: 'search'),
          tag: any(named: 'tag'),
        ),
      ).called(1);
    });

    test('rolls back page counter when loadMore fails', () async {
      var callCount = 0;
      when(
        () => repo.getSummaries(
          page: any(named: 'page'),
          size: any(named: 'size'),
          filter: any(named: 'filter'),
          search: any(named: 'search'),
          tag: any(named: 'tag'),
        ),
      ).thenAnswer((_) async {
        callCount++;
        if (callCount == 1) return _page(['a'], last: false, total: 2);
        throw Exception('network error');
      });

      final container = _container(repo);
      addTearDown(container.dispose);

      await container.read(readingListProvider.future);

      await expectLater(
        container.read(readingListProvider.notifier).loadMore(),
        throwsException,
      );

      // Existing items preserved despite error
      final state = container.read(readingListProvider).value!;
      expect(state.content.map((s) => s.id), ['a']);
    });
  });

  group('ReadingListNotifier — filter change', () {
    test('resets accumulated items when filter changes', () async {
      var callCount = 0;
      when(
        () => repo.getSummaries(
          page: any(named: 'page'),
          size: any(named: 'size'),
          filter: any(named: 'filter'),
          search: any(named: 'search'),
          tag: any(named: 'tag'),
        ),
      ).thenAnswer((_) async {
        callCount++;
        return callCount == 1
            ? _page(['a', 'b'], last: false, total: 4)
            : _page(['x', 'y']);
      });

      final container = _container(repo);
      addTearDown(container.dispose);

      await container.read(readingListProvider.future);
      await container.read(readingListProvider.notifier).loadMore();

      // Change filter → should reset
      container.read(readingListFilterProvider.notifier).state =
          ReadingListFilter.read;
      final result = await container.read(readingListProvider.future);

      expect(result.content.map((s) => s.id), ['x', 'y']);
    });
  });

  group('ReadingListNotifier — offline cache', () {
    test('returns cached data when network fails on first load', () async {
      when(
        () => repo.getSummaries(
          page: any(named: 'page'),
          size: any(named: 'size'),
          filter: any(named: 'filter'),
          search: any(named: 'search'),
          tag: any(named: 'tag'),
        ),
      ).thenThrow(Exception('no network'));

      final cachedData = _page(['cached-1', 'cached-2']);

      final container = ProviderContainer(
        overrides: [
          readingListRepositoryProvider.overrideWithValue(repo),
          readingListCacheProvider.overrideWith(
            (_) => _CacheWithData(cachedData),
          ),
        ],
      );
      addTearDown(container.dispose);

      final result = await container.read(readingListProvider.future);
      expect(result.content.map((s) => s.id), ['cached-1', 'cached-2']);
      expect(result.isOffline, isTrue);
    });

    test('rethrows when network fails and no cache exists', () async {
      when(
        () => repo.getSummaries(
          page: any(named: 'page'),
          size: any(named: 'size'),
          filter: any(named: 'filter'),
          search: any(named: 'search'),
          tag: any(named: 'tag'),
        ),
      ).thenThrow(Exception('no network'));

      final container = _container(repo);
      addTearDown(container.dispose);

      await expectLater(
        container.read(readingListProvider.future),
        throwsException,
      );
    });
  });
}

class _CacheWithData extends Fake implements ReadingListCache {
  final PaginatedSummaries data;
  _CacheWithData(this.data);

  @override
  Future<void> save(String filter, PaginatedSummaries d) async {}

  @override
  Future<PaginatedSummaries?> load(String filter) async => PaginatedSummaries(
    content: data.content,
    totalElements: data.totalElements,
    totalPages: data.totalPages,
    first: data.first,
    last: data.last,
    isOffline: true,
  );
}
