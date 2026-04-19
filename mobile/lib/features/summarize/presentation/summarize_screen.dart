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

class SummarizeScreen extends ConsumerStatefulWidget {
  const SummarizeScreen({super.key});

  @override
  ConsumerState<SummarizeScreen> createState() => _SummarizeScreenState();
}

class _SummarizeScreenState extends ConsumerState<SummarizeScreen>
    with SingleTickerProviderStateMixin {
  late final TabController _tabController;
  String? _sharedUrl;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 3, vsync: this);

    // Consume any URL that was shared into the app (cold start or warm start).
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final url = ref.read(sharedUrlProvider);
      if (url != null) {
        _applySharedUrl(url);
      }
    });
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  void _applySharedUrl(String url) {
    setState(() => _sharedUrl = url);
    _tabController.animateTo(0);
    // Clear the provider so it doesn't re-trigger on rebuild.
    ref.read(sharedUrlProvider.notifier).state = null;
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final summarizeState = ref.watch(summarizeActionProvider);
    final isLoading = summarizeState.status == SummarizeStatus.loading;

    // React to URLs shared while the screen is mounted (warm start / onNewIntent).
    ref.listen(sharedUrlProvider, (_, next) {
      if (next != null) _applySharedUrl(next);
    });

    return Scaffold(
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
          controller: _tabController,
          tabs: [
            Tab(text: l10n.urlTab),
            Tab(text: l10n.textTab),
            Tab(text: l10n.batchTab),
          ],
        ),
      ),
      body: TabBarView(
        controller: _tabController,
        children: [
          _SummarizeTabContent(
            isLoading: isLoading,
            summarizeState: summarizeState,
            input: UrlInput(
              key: ValueKey(_sharedUrl),
              initialUrl: _sharedUrl,
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
