import 'package:flutter_foreground_task/flutter_foreground_task.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final foregroundTaskServiceProvider = Provider<ForegroundTaskService>((ref) {
  return ForegroundTaskService();
});

/// Wraps the static [FlutterForegroundTask] API so callers can be tested
/// without a real platform channel (replace via [foregroundTaskServiceProvider]
/// override in tests).
class ForegroundTaskService {
  Future<void> start(String notificationText) async {
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
      notificationText: notificationText,
    );
  }

  Future<void> stop() async {
    await FlutterForegroundTask.stopService();
  }
}
