import 'package:flutter/material.dart';

import '../data/lanes.dart';
import '../store/app_model.dart';
import '../theme/curio_theme.dart';
import '../widgets/curio_widgets.dart';

/// The Home room: the brand, what is inside the catalog, and the one door out.
///
/// The phone's Home is a hero, a streak and a lane rail; a desktop window has no
/// streak to protect and no thumb to reach with, so this is the calm version:
/// the rose hero says what Curio is, the counts say what is here, and the lanes
/// below are the same chips the phone uses (tap one to choose what to explore).
class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key, required this.model, required this.onSpin});

  final AppModel model;
  final VoidCallback onSpin;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return PageBody(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          PageHeader(
            title: 'Curio',
            subtitle: 'One good thing to explore, and somewhere to keep it.',
          ),
          const SizedBox(height: CurioSpace.gap + 4),
          if (model.catalogEmpty) ...[
            const CatalogMissing(),
            const SizedBox(height: CurioSpace.gap),
          ],
          _Hero(onSpin: onSpin),
          const SizedBox(height: CurioSpace.gap + 4),
          _Counts(model: model),
          const SizedBox(height: CurioSpace.gap + 12),
          Text(
            'PICK A LANE',
            style: TextStyle(
              fontSize: 11,
              fontWeight: FontWeight.w700,
              letterSpacing: 1.1,
              color: scheme.onSurfaceVariant,
            ),
          ),
          const SizedBox(height: 10),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              for (final lane in kLanes)
                LaneChip(
                  label: lane.label,
                  accent: lane.accent,
                  selected: lane.id == model.laneId,
                  onTap: () => model.selectLane(lane.id),
                ),
            ],
          ),
        ],
      ),
    );
  }
}

/// The rose hero. One sentence, one button — and a quiet drop shadow so the
/// slab reads as paper laid on the page, not as a painted rectangle.
class _Hero extends StatelessWidget {
  const _Hero({required this.onSpin});

  final VoidCallback onSpin;

  @override
  Widget build(BuildContext context) {
    final dark = Theme.of(context).brightness == Brightness.dark;
    final top = dark ? CurioPalette.roseNight : CurioPalette.rose;
    final bottom = Color.lerp(top, CurioPalette.plum, dark ? 0.45 : 0.22)!;
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.fromLTRB(30, 30, 30, 26),
      decoration: BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [top, bottom],
        ),
        borderRadius: BorderRadius.circular(CurioSpace.pageRadius),
        boxShadow: [
          BoxShadow(
            color: bottom.withValues(alpha: 0.28),
            blurRadius: 26,
            offset: const Offset(0, 12),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Curiosity, one card at a time.',
            style: TextStyle(
              fontSize: 30,
              height: 1.15,
              fontWeight: FontWeight.w800,
              letterSpacing: -0.6,
              color: Colors.white.withValues(alpha: 0.96),
            ),
          ),
          const SizedBox(height: 8),
          Text(
            'Draw something worth knowing. Read it, hear it, watch it — then keep '
            'it in your Cabinet.',
            style: TextStyle(
              fontSize: 13.5,
              height: 1.5,
              color: Colors.white.withValues(alpha: 0.86),
            ),
          ),
          const SizedBox(height: 22),
          FilledButton.icon(
            onPressed: onSpin,
            style: FilledButton.styleFrom(
              backgroundColor: Colors.white.withValues(alpha: 0.96),
              foregroundColor: bottom,
            ),
            icon: const Icon(Icons.casino_rounded, size: 18),
            label: const Text('Draw a card'),
          ),
        ],
      ),
    );
  }
}

class _Counts extends StatelessWidget {
  const _Counts({required this.model});

  final AppModel model;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 22, vertical: 18),
      decoration: BoxDecoration(
        color: scheme.surfaceContainerHighest.withValues(alpha: 0.55),
        borderRadius: BorderRadius.circular(CurioSpace.cardRadius),
      ),
      child: Row(
        children: [
          Expanded(
            child: StatBlock(
              value: '${model.totalTopics}',
              label: 'topics in the catalog',
            ),
          ),
          Expanded(
            child: StatBlock(value: '${kLanes.length}', label: 'lanes'),
          ),
          Expanded(
            child: StatBlock(
              value: '${model.saved.length}',
              label: 'kept in your Cabinet',
            ),
          ),
        ],
      ),
    );
  }
}
