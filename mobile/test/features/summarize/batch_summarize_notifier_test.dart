import 'package:briefen/core/foreground_task/foreground_task_service.dart';
import 'package:briefen/core/notifications/notification_service.dart';
import 'package:briefen/features/summarize/data/summarize_repository.dart';
import 'package:briefen/features/summarize/domain/summary.dart';
import 'package:briefen/features/summarize/providers.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';

// ── Mocks / Fakes ─────────────────────────────────────────────────────────────

class MockSummarizeRepository extends Mock implements SummarizeRepository {}

class FakeNotificationService extends Fake implements NotificationService {
  @override
  Future<void> init() async {}
  @override
  Future<void> showSummaryReady(String title) async {}
}

class FakeForegroundTaskService extends Fake implements ForegroundTaskService {
  @override
  Future<void> start(String notificationText) async {}
  @override
  Future<void> stop() async {}
}

// ── Helpers ────────────────────────────────────────────────────────────────────

Summary _summary(String id) => Summary(
  id: id,
  url: 'https://example.com/$id',
  title: 'Title $id',
  summary: 'Summary $id',
  modelUsed: 'gemma3:4b',
  createdAt: DateTime(2026),
  isRead: false,
  savedAt: DateTime(2026),
);

ProviderContainer _container(MockSummarizeRepository repo) {
  return ProviderContainer(
    overrides: [
      summarizeRepositoryProvider.overrideWithValue(repo),
      notificationServiceProvider.overrideWithValue(FakeNotificationService()),
      foregroundTaskServiceProvider.overrideWithValue(
        FakeForegroundTaskService(),
      ),
      // Prevent unreadCountProvider from triggering real auth/storage
      unreadCountProvider.overrideWith((_) async => 0),
    ],
  );
}

// ── Tests ─────────────────────────────────────────────────────────────────────

void main() {
  setUpAll(() {
    TestWidgetsFlutterBinding.ensureInitialized();
    registerFallbackValue(const Duration());
  });

  late MockSummarizeRepository repo;

  setUp(() {
    repo = MockSummarizeRepository();
  });

  group('BatchSummarizeNotifier — empty input', () {
    test('should remain idle when urls list is empty', () async {
      final container = _container(repo);
      addTearDown(container.dispose);

      await container.read(batchSummarizeProvider.notifier).run([]);

      expect(container.read(batchSummarizeProvider).status, BatchStatus.idle);
      verifyNever(
        () => repo.summarize(
          url: any(named: 'url'),
          text: any(named: 'text'),
          title: any(named: 'title'),
          lengthHint: any(named: 'lengthHint'),
          model: any(named: 'model'),
          refresh: any(named: 'refresh'),
          timeout: any(named: 'timeout'),
        ),
      );
    });
  });

  group('BatchSummarizeNotifier — successful run', () {
    test('should reach done status after processing all urls', () async {
      when(
        () => repo.summarize(
          url: any(named: 'url'),
          text: any(named: 'text'),
          title: any(named: 'title'),
          lengthHint: any(named: 'lengthHint'),
          model: any(named: 'model'),
          refresh: any(named: 'refresh'),
          timeout: any(named: 'timeout'),
        ),
      ).thenAnswer((invocation) async {
        final url = invocation.namedArguments[#url] as String?;
        final id = url?.split('/').last ?? 'x';
        return _summary(id);
      });

      final container = _container(repo);
      addTearDown(container.dispose);

      await container.read(batchSummarizeProvider.notifier).run([
        'https://example.com/a',
        'https://example.com/b',
        'https://example.com/c',
      ]);

      final state = container.read(batchSummarizeProvider);
      expect(state.status, BatchStatus.done);
      expect(state.results, hasLength(3));
      expect(state.currentIndex, 3);
    });

    test('should record success for each url', () async {
      when(
        () => repo.summarize(
          url: any(named: 'url'),
          text: any(named: 'text'),
          title: any(named: 'title'),
          lengthHint: any(named: 'lengthHint'),
          model: any(named: 'model'),
          refresh: any(named: 'refresh'),
          timeout: any(named: 'timeout'),
        ),
      ).thenAnswer((_) async => _summary('ok'));

      final container = _container(repo);
      addTearDown(container.dispose);

      await container.read(batchSummarizeProvider.notifier).run([
        'https://example.com/1',
        'https://example.com/2',
      ]);

      final results = container.read(batchSummarizeProvider).results;
      expect(results.every((r) => r.succeeded), isTrue);
      expect(results.every((r) => r.error == null), isTrue);
    });

    test('should call repo once per url', () async {
      when(
        () => repo.summarize(
          url: any(named: 'url'),
          text: any(named: 'text'),
          title: any(named: 'title'),
          lengthHint: any(named: 'lengthHint'),
          model: any(named: 'model'),
          refresh: any(named: 'refresh'),
          timeout: any(named: 'timeout'),
        ),
      ).thenAnswer((_) async => _summary('x'));

      final container = _container(repo);
      addTearDown(container.dispose);

      await container.read(batchSummarizeProvider.notifier).run([
        'https://a.com',
        'https://b.com',
        'https://c.com',
      ]);

      verify(
        () => repo.summarize(
          url: any(named: 'url'),
          text: any(named: 'text'),
          title: any(named: 'title'),
          lengthHint: any(named: 'lengthHint'),
          model: any(named: 'model'),
          refresh: any(named: 'refresh'),
          timeout: any(named: 'timeout'),
        ),
      ).called(3);
    });

    test('should store urls in state', () async {
      when(
        () => repo.summarize(
          url: any(named: 'url'),
          text: any(named: 'text'),
          title: any(named: 'title'),
          lengthHint: any(named: 'lengthHint'),
          model: any(named: 'model'),
          refresh: any(named: 'refresh'),
          timeout: any(named: 'timeout'),
        ),
      ).thenAnswer((_) async => _summary('x'));

      final container = _container(repo);
      addTearDown(container.dispose);

      final urls = ['https://a.com', 'https://b.com'];
      await container.read(batchSummarizeProvider.notifier).run(urls);

      expect(container.read(batchSummarizeProvider).urls, urls);
    });
  });

  group('BatchSummarizeNotifier — per-url errors', () {
    test('should continue processing after one url fails', () async {
      var callCount = 0;
      when(
        () => repo.summarize(
          url: any(named: 'url'),
          text: any(named: 'text'),
          title: any(named: 'title'),
          lengthHint: any(named: 'lengthHint'),
          model: any(named: 'model'),
          refresh: any(named: 'refresh'),
          timeout: any(named: 'timeout'),
        ),
      ).thenAnswer((_) async {
        callCount++;
        if (callCount == 2) throw Exception('bad article');
        return _summary('ok');
      });

      final container = _container(repo);
      addTearDown(container.dispose);

      await container.read(batchSummarizeProvider.notifier).run([
        'https://a.com',
        'https://b.com',
        'https://c.com',
      ]);

      final state = container.read(batchSummarizeProvider);
      expect(state.status, BatchStatus.done);
      expect(state.results, hasLength(3));
      expect(state.results[0].succeeded, isTrue);
      expect(state.results[1].succeeded, isFalse);
      expect(state.results[1].error, contains('bad article'));
      expect(state.results[2].succeeded, isTrue);
    });

    test('should reach done even when all urls fail', () async {
      when(
        () => repo.summarize(
          url: any(named: 'url'),
          text: any(named: 'text'),
          title: any(named: 'title'),
          lengthHint: any(named: 'lengthHint'),
          model: any(named: 'model'),
          refresh: any(named: 'refresh'),
          timeout: any(named: 'timeout'),
        ),
      ).thenThrow(Exception('all broken'));

      final container = _container(repo);
      addTearDown(container.dispose);

      await container.read(batchSummarizeProvider.notifier).run([
        'https://a.com',
        'https://b.com',
      ]);

      final state = container.read(batchSummarizeProvider);
      expect(state.status, BatchStatus.done);
      expect(state.results.every((r) => !r.succeeded), isTrue);
    });
  });

  group('BatchSummarizeNotifier — reset', () {
    test('should return to idle with empty results', () async {
      when(
        () => repo.summarize(
          url: any(named: 'url'),
          text: any(named: 'text'),
          title: any(named: 'title'),
          lengthHint: any(named: 'lengthHint'),
          model: any(named: 'model'),
          refresh: any(named: 'refresh'),
          timeout: any(named: 'timeout'),
        ),
      ).thenAnswer((_) async => _summary('x'));

      final container = _container(repo);
      addTearDown(container.dispose);

      await container.read(batchSummarizeProvider.notifier).run([
        'https://a.com',
      ]);
      expect(container.read(batchSummarizeProvider).status, BatchStatus.done);

      container.read(batchSummarizeProvider.notifier).reset();

      final state = container.read(batchSummarizeProvider);
      expect(state.status, BatchStatus.idle);
      expect(state.urls, isEmpty);
      expect(state.results, isEmpty);
    });
  });

  group('BatchSummarizeNotifier — state transitions', () {
    test('should set running state before processing', () async {
      // Completer lets us capture the running state before the run finishes
      final seenStatuses = <BatchStatus>[];

      when(
        () => repo.summarize(
          url: any(named: 'url'),
          text: any(named: 'text'),
          title: any(named: 'title'),
          lengthHint: any(named: 'lengthHint'),
          model: any(named: 'model'),
          refresh: any(named: 'refresh'),
          timeout: any(named: 'timeout'),
        ),
      ).thenAnswer((_) async => _summary('x'));

      final container = _container(repo);
      addTearDown(container.dispose);

      container.listen(
        batchSummarizeProvider.select((s) => s.status),
        (_, next) => seenStatuses.add(next),
      );

      await container.read(batchSummarizeProvider.notifier).run([
        'https://a.com',
      ]);

      expect(seenStatuses, contains(BatchStatus.running));
      expect(seenStatuses.last, BatchStatus.done);
    });

    test('total should equal number of input urls', () async {
      when(
        () => repo.summarize(
          url: any(named: 'url'),
          text: any(named: 'text'),
          title: any(named: 'title'),
          lengthHint: any(named: 'lengthHint'),
          model: any(named: 'model'),
          refresh: any(named: 'refresh'),
          timeout: any(named: 'timeout'),
        ),
      ).thenAnswer((_) async => _summary('x'));

      final container = _container(repo);
      addTearDown(container.dispose);

      await container.read(batchSummarizeProvider.notifier).run([
        'https://a.com',
        'https://b.com',
        'https://c.com',
      ]);

      expect(container.read(batchSummarizeProvider).total, 3);
    });
  });
}
