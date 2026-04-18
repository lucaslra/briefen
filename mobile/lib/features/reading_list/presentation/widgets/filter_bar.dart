import 'package:flutter/material.dart';
import 'package:briefen/l10n/generated/app_localizations.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../providers.dart';

class FilterBar extends ConsumerWidget {
  const FilterBar({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = AppLocalizations.of(context)!;
    final currentFilter = ref.watch(readingListFilterProvider);

    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      child: Row(
        children: [
          _FilterChip(
            label: l10n.all,
            selected: currentFilter == ReadingListFilter.all,
            onSelected: () => ref.read(readingListFilterProvider.notifier).state =
                ReadingListFilter.all,
          ),
          const SizedBox(width: 8),
          _FilterChip(
            label: l10n.unread,
            selected: currentFilter == ReadingListFilter.unread,
            onSelected: () => ref.read(readingListFilterProvider.notifier).state =
                ReadingListFilter.unread,
          ),
          const SizedBox(width: 8),
          _FilterChip(
            label: l10n.read,
            selected: currentFilter == ReadingListFilter.read,
            onSelected: () => ref.read(readingListFilterProvider.notifier).state =
                ReadingListFilter.read,
          ),
        ],
      ),
    );
  }
}

class _FilterChip extends StatelessWidget {
  final String label;
  final bool selected;
  final VoidCallback onSelected;

  const _FilterChip({
    required this.label,
    required this.selected,
    required this.onSelected,
  });

  @override
  Widget build(BuildContext context) {
    return FilterChip(
      label: Text(label),
      selected: selected,
      onSelected: (_) => onSelected(),
      showCheckmark: false,
    );
  }
}
