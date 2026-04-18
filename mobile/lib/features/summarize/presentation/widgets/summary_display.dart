import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:briefen/l10n/generated/app_localizations.dart';
import 'package:flutter_markdown/flutter_markdown.dart';
import 'package:share_plus/share_plus.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../domain/summary.dart';

class SummaryDisplay extends StatelessWidget {
  final Summary summary;
  final VoidCallback? onTap;

  const SummaryDisplay({super.key, required this.summary, this.onTap});

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final colorScheme = Theme.of(context).colorScheme;
    final textTheme = Theme.of(context).textTheme;

    return Card(
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              summary.title,
              style: textTheme.titleMedium?.copyWith(
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 4),
            if (summary.domain.isNotEmpty)
              Text(
                summary.domain,
                style: textTheme.bodySmall?.copyWith(
                  color: colorScheme.primary,
                ),
              ),
            Row(
              children: [
                Icon(Icons.smart_toy_outlined,
                    size: 14, color: colorScheme.onSurfaceVariant),
                const SizedBox(width: 4),
                Text(
                  summary.modelUsed,
                  style: textTheme.bodySmall?.copyWith(
                    color: colorScheme.onSurfaceVariant,
                  ),
                ),
              ],
            ),
            const Divider(height: 24),
            MarkdownBody(
              data: summary.summary,
              selectable: true,
              styleSheet: MarkdownStyleSheet.fromTheme(Theme.of(context)),
            ),
            if (summary.tags.isNotEmpty) ...[
              const SizedBox(height: 16),
              Wrap(
                spacing: 6,
                runSpacing: 6,
                children: summary.tags.map((tag) {
                  return Chip(
                    label: Text(tag),
                    visualDensity: VisualDensity.compact,
                    materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
                  );
                }).toList(),
              ),
            ],
            const SizedBox(height: 16),
            Row(
              children: [
                if (summary.url != null)
                  TextButton.icon(
                    onPressed: () async {
                      final uri = Uri.tryParse(summary.url!);
                      if (uri != null) await launchUrl(uri);
                    },
                    icon: const Icon(Icons.open_in_browser, size: 18),
                    label: Text(l10n.openArticle),
                  ),
                const Spacer(),
                IconButton(
                  icon: const Icon(Icons.copy, size: 20),
                  tooltip: l10n.copyToClipboard,
                  onPressed: () {
                    Clipboard.setData(ClipboardData(text: summary.summary));
                    ScaffoldMessenger.of(context).showSnackBar(
                      SnackBar(content: Text(l10n.copied)),
                    );
                  },
                ),
                IconButton(
                  icon: const Icon(Icons.share, size: 20),
                  tooltip: l10n.share,
                  onPressed: () {
                    final text = '${summary.title}\n\n${summary.summary}';
                    SharePlus.instance.share(ShareParams(text: text));
                  },
                ),
              ],
            ),
          ],
        ),
      ),
      ),
    );
  }
}
