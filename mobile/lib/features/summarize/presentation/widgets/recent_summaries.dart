import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../providers.dart';

class RecentSummaries extends ConsumerStatefulWidget {
  const RecentSummaries({super.key});

  @override
  ConsumerState<RecentSummaries> createState() => _RecentSummariesState();
}

class _RecentSummariesState extends ConsumerState<RecentSummaries> {
  bool _expanded = false;

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
    final textTheme = Theme.of(context).textTheme;
    final recentAsync = ref.watch(recentSummariesProvider);

    return recentAsync.when(
      loading: () => const SizedBox.shrink(),
      error: (_, _) => const SizedBox.shrink(),
      data: (data) {
        if (data.content.isEmpty) return const SizedBox.shrink();

        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            InkWell(
              onTap: () => setState(() => _expanded = !_expanded),
              borderRadius: BorderRadius.circular(8),
              child: Padding(
                padding: const EdgeInsets.symmetric(vertical: 8),
                child: Row(
                  children: [
                    Text(
                      'Recent',
                      style: textTheme.titleSmall?.copyWith(
                        fontWeight: FontWeight.bold,
                        color: colorScheme.onSurfaceVariant,
                      ),
                    ),
                    const SizedBox(width: 4),
                    Icon(
                      _expanded
                          ? Icons.expand_less
                          : Icons.expand_more,
                      size: 20,
                      color: colorScheme.onSurfaceVariant,
                    ),
                  ],
                ),
              ),
            ),
            if (_expanded)
              ...data.content.map((summary) {
                return ListTile(
                  dense: true,
                  contentPadding: EdgeInsets.zero,
                  leading: Icon(
                    summary.isRead
                        ? Icons.check_circle_outline
                        : Icons.circle,
                    size: 12,
                    color: summary.isRead
                        ? colorScheme.outline
                        : colorScheme.primary,
                  ),
                  title: Text(
                    summary.title.isNotEmpty
                        ? summary.title
                        : summary.summary.substring(
                            0,
                            summary.summary.length > 60
                                ? 60
                                : summary.summary.length,
                          ),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: textTheme.bodyMedium,
                  ),
                  subtitle: summary.domain.isNotEmpty
                      ? Text(
                          summary.domain,
                          style: textTheme.bodySmall?.copyWith(
                            color: colorScheme.onSurfaceVariant,
                          ),
                        )
                      : null,
                  onTap: () {
                    context.push('/reading-list/${summary.id}');
                  },
                );
              }),
          ],
        );
      },
    );
  }
}
