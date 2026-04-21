import 'package:briefen/core/foreground_task/foreground_task_service.dart';
import 'package:briefen/core/notifications/notification_service.dart';
import 'package:briefen/features/summarize/data/summarize_repository.dart';
import 'package:briefen/features/summarize/domain/summary.dart';
import 'package:briefen/features/summarize/providers.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';

import '../../helpers/fixtures.dart';

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

Summary get _testSummary => Summary.fromJson(summaryFixture);

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

  group('SummarizeActionNotifier — initial state', () {
    test('should start in idle state', () {
      final container = _container(repo);
      addTearDown(container.dispose);

      expect(
        container.read(summarizeActionProvider).status,
        SummarizeStatus.idle,
      );
    });
  });

  group('SummarizeActionNotifier — summarize(url)', () {
    test('should transition idle → loading → success', () async {
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
      ).thenAnswer((_) async => _testSummary);

      final container = _container(repo);
      addTearDown(container.dispose);

      final states = <SummarizeStatus>[];
      container.listen(
        summarizeActionProvider.select((s) => s.status),
        (_, next) => states.add(next),
      );

      await container
          .read(summarizeActionProvider.notifier)
          .summarize('https://example.com');

      // loading was observed, then success
      expect(states, contains(SummarizeStatus.loading));
      expect(states.last, SummarizeStatus.success);
    });

    test('should store summary in state on success', () async {
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
      ).thenAnswer((_) async => _testSummary);

      final container = _container(repo);
      addTearDown(container.dispose);

      await container
          .read(summarizeActionProvider.notifier)
          .summarize('https://example.com');

      final state = container.read(summarizeActionProvider);
      expect(state.status, SummarizeStatus.success);
      expect(state.summary?.id, _testSummary.id);
      expect(state.summary?.title, _testSummary.title);
      expect(state.error, isNull);
    });

    test('should set error state when repo throws', () async {
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
      ).thenThrow(Exception('Network failure'));

      final container = _container(repo);
      addTearDown(container.dispose);

      await container
          .read(summarizeActionProvider.notifier)
          .summarize('https://example.com');

      final state = container.read(summarizeActionProvider);
      expect(state.status, SummarizeStatus.error);
      expect(state.error, contains('Network failure'));
      expect(state.summary, isNull);
    });

    test('should call repo with the correct url', () async {
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
      ).thenAnswer((_) async => _testSummary);

      final container = _container(repo);
      addTearDown(container.dispose);

      await container
          .read(summarizeActionProvider.notifier)
          .summarize('https://example.com/article');

      verify(
        () => repo.summarize(
          url: 'https://example.com/article',
          text: any(named: 'text'),
          title: any(named: 'title'),
          lengthHint: any(named: 'lengthHint'),
          model: any(named: 'model'),
          refresh: any(named: 'refresh'),
          timeout: any(named: 'timeout'),
        ),
      ).called(1);
    });
  });

  group('SummarizeActionNotifier — summarizeText', () {
    test('should transition to success state with summary', () async {
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
      ).thenAnswer((_) async => _testSummary);

      final container = _container(repo);
      addTearDown(container.dispose);

      await container
          .read(summarizeActionProvider.notifier)
          .summarizeText('Paste text here', 'My Title');

      expect(
        container.read(summarizeActionProvider).status,
        SummarizeStatus.success,
      );
    });

    test('should call repo with text and title', () async {
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
      ).thenAnswer((_) async => _testSummary);

      final container = _container(repo);
      addTearDown(container.dispose);

      await container
          .read(summarizeActionProvider.notifier)
          .summarizeText('Article body', 'Custom Title');

      verify(
        () => repo.summarize(
          url: any(named: 'url'),
          text: 'Article body',
          title: 'Custom Title',
          lengthHint: any(named: 'lengthHint'),
          model: any(named: 'model'),
          refresh: any(named: 'refresh'),
          timeout: any(named: 'timeout'),
        ),
      ).called(1);
    });

    test('should set error state when text summarize fails', () async {
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
      ).thenThrow(Exception('timeout'));

      final container = _container(repo);
      addTearDown(container.dispose);

      await container
          .read(summarizeActionProvider.notifier)
          .summarizeText('text', null);

      expect(
        container.read(summarizeActionProvider).status,
        SummarizeStatus.error,
      );
    });
  });

  group('SummarizeActionNotifier — reset', () {
    test('should return to idle state after success', () async {
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
      ).thenAnswer((_) async => _testSummary);

      final container = _container(repo);
      addTearDown(container.dispose);

      await container
          .read(summarizeActionProvider.notifier)
          .summarize('https://example.com');
      expect(
        container.read(summarizeActionProvider).status,
        SummarizeStatus.success,
      );

      container.read(summarizeActionProvider.notifier).reset();

      final state = container.read(summarizeActionProvider);
      expect(state.status, SummarizeStatus.idle);
      expect(state.summary, isNull);
      expect(state.error, isNull);
    });

    test('should return to idle state after error', () async {
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
      ).thenThrow(Exception('boom'));

      final container = _container(repo);
      addTearDown(container.dispose);

      await container
          .read(summarizeActionProvider.notifier)
          .summarize('https://example.com');
      expect(
        container.read(summarizeActionProvider).status,
        SummarizeStatus.error,
      );

      container.read(summarizeActionProvider.notifier).reset();

      expect(
        container.read(summarizeActionProvider).status,
        SummarizeStatus.idle,
      );
    });
  });
}
