import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_foreground_task/flutter_foreground_task.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'app.dart';
import 'core/notifications/notification_service.dart';
import 'features/summarize/providers.dart';

const _shareChannel = MethodChannel('dev.azurecoder.briefen/share');

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  final notificationService = NotificationService();
  await notificationService.init();
  await notificationService.requestPermission();

  // Request foreground service permission for background summarization
  await FlutterForegroundTask.requestNotificationPermission();

  // Use an explicit container so the share channel can update providers
  // before the widget tree is built (cold start) and while it's running.
  final container = ProviderContainer();

  // Handle URLs shared while the app is already running (warm start).
  _shareChannel.setMethodCallHandler((call) async {
    if (call.method == 'sharedUrl') {
      final url = call.arguments as String?;
      if (url != null && url.isNotEmpty) {
        container.read(sharedUrlProvider.notifier).state = url;
      }
    }
  });

  // Handle URL shared on cold start.
  try {
    final url = await _shareChannel.invokeMethod<String?>('getInitialUrl');
    if (url != null && url.isNotEmpty) {
      container.read(sharedUrlProvider.notifier).state = url;
    }
  } catch (_) {}

  runApp(
    UncontrolledProviderScope(container: container, child: const BriefenApp()),
  );
}
