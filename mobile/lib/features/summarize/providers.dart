import 'package:flutter/widgets.dart';
import 'package:flutter_foreground_task/flutter_foreground_task.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/auth/auth_provider.dart';
import '../../core/notifications/notification_service.dart';
import '../reading_list/providers.dart';
import 'data/summarize_repository.dart';
import 'domain/summary.dart';

/// Holds a URL shared into Briefen from another app (Android share intent).
/// Consumed by SummarizeScreen — cleared after use.
final sharedUrlProvider = StateProvider<String?>((ref) => null);

// Summarize action state
enum SummarizeStatus { idle, loading, success, error }

class SummarizeState {
  final SummarizeStatus status;
  final Summary? summary;
  final String? error;

  const SummarizeState({
    this.status = SummarizeStatus.idle,
    this.summary,
    this.error,
  });
}

final summarizeActionProvider =
    NotifierProvider<SummarizeActionNotifier, SummarizeState>(
      SummarizeActionNotifier.new,
    );

class SummarizeActionNotifier extends Notifier<SummarizeState> {
  @override
  SummarizeState build() => const SummarizeState();

  Future<void> summarize(String url) async {
    await _run(() async {
      final repo = ref.read(summarizeRepositoryProvider);
      return repo.summarize(url: url);
    });
  }

  Future<void> summarizeText(String text, String? title) async {
    await _run(() async {
      final repo = ref.read(summarizeRepositoryProvider);
      return repo.summarize(text: text, title: title);
    });
  }

  Future<void> _run(Future<Summary> Function() fetch) async {
    state = const SummarizeState(status: SummarizeStatus.loading);
    await _startForegroundTask();

    try {
      final notifications = ref.read(notificationServiceProvider);
      await notifications.init();

      final summary = await fetch();
      state = SummarizeState(status: SummarizeStatus.success, summary: summary);
      ref.invalidate(unreadCountProvider);
      ref.invalidate(readingListProvider);

      final appLifecycle = WidgetsBinding.instance.lifecycleState;
      if (appLifecycle != AppLifecycleState.resumed) {
        await notifications.showSummaryReady(summary.title);
      }
    } catch (e, stack) {
      debugPrint('Summarize error: $e\n$stack');
      state = SummarizeState(
        status: SummarizeStatus.error,
        error: e.toString(),
      );
    } finally {
      await _stopForegroundTask();
    }
  }

  Future<void> _startForegroundTask() async {
    FlutterForegroundTask.init(
      androidNotificationOptions: AndroidNotificationOptions(
        channelId: 'briefen_summarize',
        channelName: 'Summarizing',
        channelDescription: 'Shown while an article is being summarized',
        channelImportance: NotificationChannelImportance.DEFAULT,
        priority: NotificationPriority.DEFAULT,
      ),
      iosNotificationOptions: const IOSNotificationOptions(),
      foregroundTaskOptions: ForegroundTaskOptions(
        eventAction: ForegroundTaskEventAction.nothing(),
        autoRunOnBoot: false,
        autoRunOnMyPackageReplaced: false,
        allowWakeLock: true,
        allowWifiLock: true,
      ),
    );

    await FlutterForegroundTask.startService(
      notificationTitle: 'Briefen',
      notificationText: 'Summarizing article...',
    );
  }

  Future<void> _stopForegroundTask() async {
    await FlutterForegroundTask.stopService();
  }

  void reset() {
    state = const SummarizeState();
  }
}

// Batch summarize state
enum BatchStatus { idle, running, done }

class BatchResult {
  final String url;
  final Summary? summary;
  final String? error;
  const BatchResult({required this.url, this.summary, this.error});
  bool get succeeded => summary != null;
}

class BatchSummarizeState {
  final BatchStatus status;
  final List<String> urls;
  final int currentIndex;
  final List<BatchResult> results;

  const BatchSummarizeState({
    this.status = BatchStatus.idle,
    this.urls = const [],
    this.currentIndex = 0,
    this.results = const [],
  });

  int get total => urls.length;
  bool get isRunning => status == BatchStatus.running;
}

final batchSummarizeProvider =
    NotifierProvider<BatchSummarizeNotifier, BatchSummarizeState>(
      BatchSummarizeNotifier.new,
    );

class BatchSummarizeNotifier extends Notifier<BatchSummarizeState> {
  @override
  BatchSummarizeState build() => const BatchSummarizeState();

  Future<void> run(List<String> urls) async {
    if (urls.isEmpty) return;
    state = BatchSummarizeState(
      status: BatchStatus.running,
      urls: urls,
      currentIndex: 0,
      results: [],
    );

    await _startForegroundTask(urls.length);

    final repo = ref.read(summarizeRepositoryProvider);
    final results = <BatchResult>[];

    try {
      for (var i = 0; i < urls.length; i++) {
        state = BatchSummarizeState(
          status: BatchStatus.running,
          urls: urls,
          currentIndex: i,
          results: List.unmodifiable(results),
        );
        try {
          final summary = await repo.summarize(
            url: urls[i],
            timeout: const Duration(minutes: 10),
          );
          results.add(BatchResult(url: urls[i], summary: summary));
        } catch (e) {
          results.add(BatchResult(url: urls[i], error: e.toString()));
        }
      }
    } finally {
      await FlutterForegroundTask.stopService();
    }

    state = BatchSummarizeState(
      status: BatchStatus.done,
      urls: urls,
      currentIndex: urls.length,
      results: List.unmodifiable(results),
    );
    ref.invalidate(unreadCountProvider);
    ref.invalidate(recentSummariesProvider);
    ref.invalidate(readingListProvider);

    final appLifecycle = WidgetsBinding.instance.lifecycleState;
    if (appLifecycle != AppLifecycleState.resumed) {
      final succeeded = results.where((r) => r.succeeded).length;
      final notifications = ref.read(notificationServiceProvider);
      await notifications.init();
      await notifications.showSummaryReady(
        '$succeeded/${urls.length} articles ready',
      );
    }
  }

  Future<void> _startForegroundTask(int total) async {
    FlutterForegroundTask.init(
      androidNotificationOptions: AndroidNotificationOptions(
        channelId: 'briefen_summarize',
        channelName: 'Summarizing',
        channelDescription: 'Shown while articles are being summarized',
        channelImportance: NotificationChannelImportance.DEFAULT,
        priority: NotificationPriority.DEFAULT,
      ),
      iosNotificationOptions: const IOSNotificationOptions(),
      foregroundTaskOptions: ForegroundTaskOptions(
        eventAction: ForegroundTaskEventAction.nothing(),
        autoRunOnBoot: false,
        autoRunOnMyPackageReplaced: false,
        allowWakeLock: true,
        allowWifiLock: true,
      ),
    );

    await FlutterForegroundTask.startService(
      notificationTitle: 'Briefen',
      notificationText: 'Summarizing $total articles...',
    );
  }

  void reset() => state = const BatchSummarizeState();
}

// Recent summaries
final recentSummariesProvider = FutureProvider.autoDispose<PaginatedSummaries>((
  ref,
) async {
  final repo = ref.read(summarizeRepositoryProvider);
  return repo.getSummaries(page: 0, size: 5);
});

// Unread count — watches auth so it resets to 0 and refetches on user change.
final unreadCountProvider = FutureProvider<int>((ref) async {
  final username = ref.watch(authProvider).username;
  if (username == null) return 0;
  final repo = ref.read(summarizeRepositoryProvider);
  return repo.getUnreadCount();
});
