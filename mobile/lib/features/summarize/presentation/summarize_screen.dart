import 'package:flutter/material.dart';
import 'package:briefen/l10n/generated/app_localizations.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/theme/theme_provider.dart';
import '../providers.dart';
import 'widgets/url_input.dart';
import 'widgets/text_input.dart';
import 'widgets/batch_input.dart';
import 'widgets/summary_display.dart';
import 'widgets/summary_loading.dart';
import 'widgets/recent_summaries.dart';

class SummarizeScreen extends ConsumerWidget {
  const SummarizeScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = AppLocalizations.of(context)!;
    final summarizeState = ref.watch(summarizeActionProvider);
    final isLoading = summarizeState.status == SummarizeStatus.loading;

    return DefaultTabController(
      length: 3,
      child: Scaffold(
        appBar: AppBar(
          title: Text(l10n.appName),
          actions: [
            IconButton(
              icon: Theme.of(context).brightness == Brightness.dark
                  ? const Icon(Icons.light_mode_outlined)
                  : const Icon(Icons.dark_mode_outlined),
              onPressed: () => ref.read(themeModeProvider.notifier).toggle(),
            ),
          ],
          bottom: TabBar(
            tabs: [
              Tab(text: l10n.urlTab),
              Tab(text: l10n.textTab),
              Tab(text: l10n.batchTab),
            ],
          ),
        ),
        body: TabBarView(
          children: [
            _SummarizeTabContent(
              isLoading: isLoading,
              summarizeState: summarizeState,
              input: UrlInput(
                onSubmit: (url) {
                  ref.read(summarizeActionProvider.notifier).summarize(url);
                },
                loading: isLoading,
              ),
            ),
            _SummarizeTabContent(
              isLoading: isLoading,
              summarizeState: summarizeState,
              input: TextInput(
                onSubmit: (text, title) {
                  ref
                      .read(summarizeActionProvider.notifier)
                      .summarizeText(text, title);
                },
                loading: isLoading,
              ),
            ),
            // Batch tab has its own self-contained UI
            SingleChildScrollView(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: const [BatchInput()],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _SummarizeTabContent extends ConsumerWidget {
  final bool isLoading;
  final SummarizeState summarizeState;
  final Widget input;

  const _SummarizeTabContent({
    required this.isLoading,
    required this.summarizeState,
    required this.input,
  });

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = AppLocalizations.of(context)!;

    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          input,
          const SizedBox(height: 24),
          if (isLoading) const SummaryLoading(),
          if (summarizeState.status == SummarizeStatus.success &&
              summarizeState.summary != null)
            SummaryDisplay(
              summary: summarizeState.summary!,
              onTap: () {
                context.push('/reading-list/${summarizeState.summary!.id}');
              },
            ),
          if (summarizeState.status == SummarizeStatus.error)
            Card(
              color: Theme.of(context).colorScheme.errorContainer,
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Text(
                  summarizeState.error ?? l10n.summarizeError,
                  style: TextStyle(
                    color: Theme.of(context).colorScheme.onErrorContainer,
                  ),
                ),
              ),
            ),
          if (!isLoading) ...[
            const SizedBox(height: 24),
            const RecentSummaries(),
          ],
        ],
      ),
    );
  }
}
