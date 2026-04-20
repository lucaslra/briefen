import 'package:briefen/features/reading_list/data/reading_list_cache.dart';
import 'package:briefen/features/reading_list/data/reading_list_repository.dart';
import 'package:briefen/features/reading_list/providers.dart';
import 'package:briefen/features/summarize/domain/summary.dart';
import 'package:briefen/features/summarize/providers.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';

// ── Mocks / Fakes ─────────────────────────────────────────────────────────────

class MockReadingListRepository extends Mock implements ReadingListRepository {}

class FakeReadingListCache extends Fake implements ReadingListCache {
  @override
  Future<void> save(String filter, PaginatedSummaries data) async {}
  @override
  Future<PaginatedSummaries?> load(String filter) async => null;
}

// ── Helpers ────────────────────────────────────────────────────────────────────

Summary _summary(String id) => Summary(
  id: id,
  title: 'Title $id',
  summary: 'Body $id',
  modelUsed: 'test',
  createdAt: DateTime(2026),
  isRead: false,
  savedAt: DateTime(2026),
);

ProviderContainer _container(MockReadingListRepository repo) {
  return ProviderContainer(
    overrides: [
      readingListRepositoryProvider.overrideWithValue(repo),
      readingListCacheProvider.overrideWith((_) => FakeReadingListCache()),
      // Prevent unreadCountProvider from triggering real auth/storage
      unreadCountProvider.overrideWith((_) async => 0),
    ],
  );
}

// ── Tests ─────────────────────────────────────────────────────────────────────

void main() {
  late MockReadingListRepository repo;

  setUp(() {
    repo = MockReadingListRepository();
    registerFallbackValue(<String>[]);
  });

  group('ReadingListActions — toggleRead', () {
    test('should call repo.toggleReadStatus with correct args', () async {
      when(
        () => repo.toggleReadStatus(any(), any()),
      ).thenAnswer((_) async => _summary('1').copyWith(isRead: true));

      final container = _container(repo);
      addTearDown(container.dispose);

      await container
          .read(readingListActionsProvider)
          .toggleRead('sum-1', true);

      verify(() => repo.toggleReadStatus('sum-1', true)).called(1);
    });

    test('should mark as unread when called with false', () async {
      when(
        () => repo.toggleReadStatus(any(), any()),
      ).thenAnswer((_) async => _summary('1').copyWith(isRead: false));

      final container = _container(repo);
      addTearDown(container.dispose);

      await container
          .read(readingListActionsProvider)
          .toggleRead('sum-1', false);

      verify(() => repo.toggleReadStatus('sum-1', false)).called(1);
    });

    test('should propagate exception from repo', () async {
      when(
        () => repo.toggleReadStatus(any(), any()),
      ).thenThrow(Exception('network error'));

      final container = _container(repo);
      addTearDown(container.dispose);

      expect(
        () => container
            .read(readingListActionsProvider)
            .toggleRead('sum-1', true),
        throwsException,
      );
    });
  });

  group('ReadingListActions — delete', () {
    test('should call repo.deleteSummary with correct id', () async {
      when(() => repo.deleteSummary(any())).thenAnswer((_) async {});

      final container = _container(repo);
      addTearDown(container.dispose);

      await container.read(readingListActionsProvider).delete('sum-42');

      verify(() => repo.deleteSummary('sum-42')).called(1);
    });

    test('should propagate exception from repo', () async {
      when(() => repo.deleteSummary(any())).thenThrow(Exception('not found'));

      final container = _container(repo);
      addTearDown(container.dispose);

      expect(
        () => container.read(readingListActionsProvider).delete('sum-42'),
        throwsException,
      );
    });
  });

  group('ReadingListActions — updateNotes', () {
    test('should call repo.updateNotes with id and notes text', () async {
      when(
        () => repo.updateNotes(any(), any()),
      ).thenAnswer((_) async => _summary('1'));

      final container = _container(repo);
      addTearDown(container.dispose);

      await container
          .read(readingListActionsProvider)
          .updateNotes('sum-1', 'My note text');

      verify(() => repo.updateNotes('sum-1', 'My note text')).called(1);
    });

    test('should allow empty string to clear notes', () async {
      when(
        () => repo.updateNotes(any(), any()),
      ).thenAnswer((_) async => _summary('1'));

      final container = _container(repo);
      addTearDown(container.dispose);

      await container.read(readingListActionsProvider).updateNotes('sum-1', '');

      verify(() => repo.updateNotes('sum-1', '')).called(1);
    });
  });

  group('ReadingListActions — updateTags', () {
    test('should call repo.updateTags with id and tags', () async {
      when(
        () => repo.updateTags(any(), any()),
      ).thenAnswer((_) async => _summary('1'));

      final container = _container(repo);
      addTearDown(container.dispose);

      await container.read(readingListActionsProvider).updateTags('sum-1', [
        'flutter',
        'dart',
      ]);

      verify(() => repo.updateTags('sum-1', ['flutter', 'dart'])).called(1);
    });

    test('should allow empty list to clear all tags', () async {
      when(
        () => repo.updateTags(any(), any()),
      ).thenAnswer((_) async => _summary('1'));

      final container = _container(repo);
      addTearDown(container.dispose);

      await container.read(readingListActionsProvider).updateTags('sum-1', []);

      verify(() => repo.updateTags('sum-1', <String>[])).called(1);
    });
  });

  group('ReadingListActions — markAllRead', () {
    test('should call repo.markAllRead', () async {
      when(() => repo.markAllRead()).thenAnswer((_) async {});

      final container = _container(repo);
      addTearDown(container.dispose);

      await container.read(readingListActionsProvider).markAllRead();

      verify(() => repo.markAllRead()).called(1);
    });

    test('should propagate exception from repo', () async {
      when(() => repo.markAllRead()).thenThrow(Exception('server error'));

      final container = _container(repo);
      addTearDown(container.dispose);

      expect(
        () => container.read(readingListActionsProvider).markAllRead(),
        throwsException,
      );
    });
  });

  group('ReadingListActions — markAllUnread', () {
    test('should call repo.markAllUnread', () async {
      when(() => repo.markAllUnread()).thenAnswer((_) async {});

      final container = _container(repo);
      addTearDown(container.dispose);

      await container.read(readingListActionsProvider).markAllUnread();

      verify(() => repo.markAllUnread()).called(1);
    });

    test('should propagate exception from repo', () async {
      when(() => repo.markAllUnread()).thenThrow(Exception('server error'));

      final container = _container(repo);
      addTearDown(container.dispose);

      expect(
        () => container.read(readingListActionsProvider).markAllUnread(),
        throwsException,
      );
    });
  });

  group('ReadingListActions — loadMore', () {
    test('should delegate to readingListProvider.notifier.loadMore', () async {
      // Seed the provider so loadMore has a state to work with
      when(
        () => repo.getSummaries(
          page: any(named: 'page'),
          size: any(named: 'size'),
          filter: any(named: 'filter'),
          search: any(named: 'search'),
          tag: any(named: 'tag'),
        ),
      ).thenAnswer(
        (_) async => PaginatedSummaries(
          content: [_summary('1')],
          totalElements: 2,
          totalPages: 2,
          first: true,
          last: false,
        ),
      );

      final container = _container(repo);
      addTearDown(container.dispose);

      // Seed initial state
      await container.read(readingListProvider.future);

      // Wire second page
      when(
        () => repo.getSummaries(
          page: any(named: 'page'),
          size: any(named: 'size'),
          filter: any(named: 'filter'),
          search: any(named: 'search'),
          tag: any(named: 'tag'),
        ),
      ).thenAnswer(
        (_) async => PaginatedSummaries(
          content: [_summary('2')],
          totalElements: 2,
          totalPages: 2,
          first: false,
          last: true,
        ),
      );

      await container.read(readingListActionsProvider).loadMore();

      final result = container.read(readingListProvider).value!;
      expect(result.content.map((s) => s.id), containsAll(['1', '2']));
    });
  });
}
