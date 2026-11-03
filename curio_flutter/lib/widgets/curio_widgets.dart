import 'package:flutter/material.dart';

import '../theme/curio_theme.dart';

/// A room's head: the name, one line under it, and whatever action belongs to
/// the page on the right. Every room uses this, so the four pages never drift.
class PageHeader extends StatelessWidget {
  const PageHeader({
    super.key,
    required this.title,
    required this.subtitle,
    this.trailing,
  });

  final String title;
  final String subtitle;
  final Widget? trailing;

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    final scheme = Theme.of(context).colorScheme;
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(title, style: text.displaySmall),
              const SizedBox(height: 4),
              Text(
                subtitle,
                style: text.bodyMedium?.copyWith(color: scheme.onSurfaceVariant),
              ),
            ],
          ),
        ),
        if (trailing != null) trailing!,
      ],
    );
  }
}

/// A page's scrollable body. A Windows window can be made very short, so every
/// room scrolls rather than clipping.
class PageBody extends StatelessWidget {
  const PageBody({super.key, required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      padding: const EdgeInsets.fromLTRB(
        CurioSpace.page,
        CurioSpace.page,
        CurioSpace.page,
        CurioSpace.page + 12,
      ),
      child: child,
    );
  }
}

/// Shown when the bundled catalog is empty — almost always a build that skipped
/// `tool/sync_assets.sh`. It names the command instead of saying "no data".
class CatalogMissing extends StatelessWidget {
  const CatalogMissing({super.key});

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Container(
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: scheme.errorContainer.withValues(alpha: 0.5),
        borderRadius: BorderRadius.circular(CurioSpace.cardRadius),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'The catalog is not bundled yet',
            style: TextStyle(
              fontWeight: FontWeight.w700,
              color: scheme.onErrorContainer,
            ),
          ),
          const SizedBox(height: 6),
          Text(
            'Run ./tool/sync_assets.sh in curio_flutter/, then rebuild. '
            'It copies data/topics/*.json — the same catalog the phone ships — '
            'into assets/topics/.',
            style: TextStyle(
              fontSize: 12.5,
              height: 1.45,
              color: scheme.onErrorContainer,
            ),
          ),
        ],
      ),
    );
  }
}

/// A quiet stat: a big number over its name.
class StatBlock extends StatelessWidget {
  const StatBlock({
    super.key,
    required this.value,
    required this.label,
    this.ink,
  });

  final String value;
  final String label;
  final Color? ink;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final color = ink ?? scheme.onSurface;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          value,
          style: TextStyle(
            fontSize: 24,
            fontWeight: FontWeight.w800,
            color: color,
            height: 1.1,
          ),
        ),
        const SizedBox(height: 2),
        Text(
          label,
          style: TextStyle(fontSize: 11.5, color: color.withValues(alpha: 0.78)),
        ),
      ],
    );
  }
}

/// A rounded tag on a lane's own colour — the phone's chip language, at desktop
/// size.
class LaneChip extends StatelessWidget {
  const LaneChip({
    super.key,
    required this.label,
    required this.accent,
    this.selected = false,
    this.onTap,
  });

  final String label;
  final int accent;
  final bool selected;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final base = Color(accent);
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(999),
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 160),
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 7),
        decoration: BoxDecoration(
          color: selected ? base : base.withValues(alpha: 0.12),
          borderRadius: BorderRadius.circular(999),
          border: Border.all(color: base.withValues(alpha: selected ? 0 : 0.35)),
        ),
        child: Text(
          label,
          style: TextStyle(
            fontSize: 12.5,
            fontWeight: FontWeight.w600,
            color: selected ? inkOn(base) : base,
          ),
        ),
      ),
    );
  }
}
