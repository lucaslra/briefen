import 'package:flutter/material.dart';
import 'package:briefen/l10n/generated/app_localizations.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../providers.dart';

class BatchInput extends ConsumerStatefulWidget {
  const BatchInput({super.key});

  @override
  ConsumerState<BatchInput> createState() => _BatchInputState();
}

class _BatchInputState extends ConsumerState<BatchInput> {
  final _controller = TextEditingController();

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  List<String> _parseUrls() {
    return _controller.text
        .split('\n')
        .map((l) => l.trim())
        .where((l) => l.isNotEmpty && l.startsWith('http'))
        .toList();
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final state = ref.watch(batchSummarizeProvider);
    final colorScheme = Theme.of(context).colorScheme;
    final textTheme = Theme.of(context).textTheme;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        TextField(
          controller: _controller,
          decoration: InputDecoration(
            hintText: l10n.batchInputHint,
            border: const OutlineInputBorder(),
            alignLabelWithHint: true,
          ),
          maxLines: 6,
          minLines: 4,
          enabled: !state.isRunning,
        ),
        const SizedBox(height: 12),
        if (state.isRunning) ...[
          LinearProgressIndicator(
            value: state.total > 0 ? state.currentIndex / state.total : null,
          ),
          const SizedBox(height: 8),
          Text(
            l10n.batchProgress(state.currentIndex + 1, state.total),
            style: textTheme.bodySmall?.copyWith(
              color: colorScheme.onSurfaceVariant,
            ),
            textAlign: TextAlign.center,
          ),
        ] else ...[
          Row(
            children: [
              Expanded(
                child: FilledButton(
                  onPressed: () {
                    final urls = _parseUrls();
                    if (urls.isNotEmpty) {
                      ref.read(batchSummarizeProvider.notifier).run(urls);
                    }
                  },
                  child: Text(l10n.batchSummarize),
                ),
              ),
              if (state.status == BatchStatus.done) ...[
                const SizedBox(width: 8),
                TextButton(
                  onPressed: () {
                    _controller.clear();
                    ref.read(batchSummarizeProvider.notifier).reset();
                  },
                  child: Text(l10n.cancel),
                ),
              ],
            ],
          ),
        ],
        if (state.results.isNotEmpty) ...[
          const SizedBox(height: 16),
          if (state.status == BatchStatus.done)
            Padding(
              padding: const EdgeInsets.only(bottom: 8),
              child: Text(
                l10n.batchComplete(
                  state.results.where((r) => r.succeeded).length,
                ),
                style: textTheme.titleSmall?.copyWith(
                  fontWeight: FontWeight.bold,
                ),
              ),
            ),
          ...state.results.map((r) => _BatchResultTile(result: r)),
        ],
      ],
    );
  }
}

class _BatchResultTile extends StatelessWidget {
  final BatchResult result;
  const _BatchResultTile({required this.result});

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
    final textTheme = Theme.of(context).textTheme;
    final summary = result.summary;

    if (summary != null) {
      return ListTile(
        dense: true,
        contentPadding: EdgeInsets.zero,
        leading: Icon(
          Icons.check_circle_outline,
          size: 18,
          color: colorScheme.primary,
        ),
        title: Text(
          summary.title.isNotEmpty ? summary.title : result.url,
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
          style: textTheme.bodyMedium,
        ),
        subtitle: summary.domain.isNotEmpty
            ? Text(summary.domain, style: textTheme.bodySmall)
            : null,
        onTap: () => context.push('/reading-list/${summary.id}'),
      );
    }

    return ListTile(
      dense: true,
      contentPadding: EdgeInsets.zero,
      leading: Icon(Icons.error_outline, size: 18, color: colorScheme.error),
      title: Text(
        result.url,
        maxLines: 1,
        overflow: TextOverflow.ellipsis,
        style: textTheme.bodyMedium,
      ),
      subtitle: Text(
        result.error ?? '',
        maxLines: 1,
        overflow: TextOverflow.ellipsis,
        style: textTheme.bodySmall?.copyWith(color: colorScheme.error),
      ),
    );
  }
}
