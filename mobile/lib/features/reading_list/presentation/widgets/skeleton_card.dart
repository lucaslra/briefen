import 'package:flutter/material.dart';

/// A pulsing placeholder card that mimics the shape of a SummaryCard.
/// Uses a single AnimationController shared via InheritedWidget so all
/// cards in a list pulse in sync.
class SkeletonListView extends StatefulWidget {
  final int count;

  const SkeletonListView({super.key, this.count = 6});

  @override
  State<SkeletonListView> createState() => _SkeletonListViewState();
}

class _SkeletonListViewState extends State<SkeletonListView>
    with SingleTickerProviderStateMixin {
  late final AnimationController _controller;
  late final Animation<double> _opacity;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 900),
    )..repeat(reverse: true);
    _opacity = Tween<double>(
      begin: 0.35,
      end: 0.75,
    ).animate(CurvedAnimation(parent: _controller, curve: Curves.easeInOut));
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return _SkeletonAnimation(
      opacity: _opacity,
      child: ListView.builder(
        padding: const EdgeInsets.symmetric(horizontal: 8),
        itemCount: widget.count,
        itemBuilder: (_, __) => const _SkeletonCard(),
      ),
    );
  }
}

class _SkeletonAnimation extends InheritedWidget {
  final Animation<double> opacity;

  const _SkeletonAnimation({required this.opacity, required super.child});

  static Animation<double> of(BuildContext context) {
    return context
        .dependOnInheritedWidgetOfExactType<_SkeletonAnimation>()!
        .opacity;
  }

  @override
  bool updateShouldNotify(_SkeletonAnimation old) => false;
}

class _SkeletonCard extends StatelessWidget {
  const _SkeletonCard();

  @override
  Widget build(BuildContext context) {
    final opacity = _SkeletonAnimation.of(context);
    final color = Theme.of(context).colorScheme.onSurface;

    return AnimatedBuilder(
      animation: opacity,
      builder: (_, __) => Card(
        margin: const EdgeInsets.symmetric(vertical: 4),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Unread dot
              Container(
                width: 10,
                height: 10,
                margin: const EdgeInsets.only(top: 6, right: 12),
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: color.withValues(alpha: opacity.value * 0.4),
                ),
              ),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    // Title line
                    _Box(
                      width: double.infinity,
                      height: 14,
                      color: color,
                      opacity: opacity.value,
                    ),
                    const SizedBox(height: 6),
                    // Second title line (shorter)
                    _Box(
                      width: 200,
                      height: 14,
                      color: color,
                      opacity: opacity.value,
                    ),
                    const SizedBox(height: 8),
                    // Meta line (domain + date)
                    Row(
                      children: [
                        _Box(
                          width: 80,
                          height: 11,
                          color: color,
                          opacity: opacity.value * 0.6,
                        ),
                        const SizedBox(width: 6),
                        _Box(
                          width: 60,
                          height: 11,
                          color: color,
                          opacity: opacity.value * 0.6,
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _Box extends StatelessWidget {
  final double width;
  final double height;
  final Color color;
  final double opacity;

  const _Box({
    required this.width,
    required this.height,
    required this.color,
    required this.opacity,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      width: width,
      height: height,
      decoration: BoxDecoration(
        color: color.withValues(alpha: opacity * 0.15),
        borderRadius: BorderRadius.circular(4),
      ),
    );
  }
}
