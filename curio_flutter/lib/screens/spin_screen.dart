import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../data/lanes.dart';
import '../data/topic.dart';
import '../store/app_model.dart';
import '../theme/curio_theme.dart';
import '../widgets/curio_glyphs.dart';
import '../widgets/curio_widgets.dart';

/// The Spin room.
///
/// The phone's Spin is a hand of three cards on a narrow screen; on a desktop
/// the same hand is laid out WIDE — the lane strip above, the front card face up
/// with its two peeks fanned behind it, and the phone's own **orbit-ringed die**
/// under the deck instead of in a corner. The deal is the phone's too: the old
/// front card lifts and fades away as the new one rises into place, and the
/// ring turns while it happens.
class SpinScreen extends StatefulWidget {
  const SpinScreen({super.key, required this.model});

  final AppModel model;

  @override
  State<SpinScreen> createState() => _SpinScreenState();
}

class _SpinScreenState extends State<SpinScreen>
    with SingleTickerProviderStateMixin {
  late final AnimationController _turn;

  @override
  void initState() {
    super.initState();
    // Built here, not in a field initializer: a controller needs `this` as its
    // vsync, and `this` is not available while fields are still being set up.
    _turn = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 620),
    );
  }

  @override
  void dispose() {
    _turn.dispose();
    super.dispose();
  }

  /// One deal: the deck moves, and the ring turns with it. The ring's turn is
  /// the ONLY feedback a second tap needs — the card itself is already changing.
  void _spin() {
    if (widget.model.current == null) return;
    widget.model.shuffle();
    _turn.forward(from: 0);
  }

  @override
  Widget build(BuildContext context) {
    final model = widget.model;
    final topic = model.current;
    return PageBody(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          PageHeader(
            title: model.lane.label,
            subtitle: model.laneCount == 1
                ? '1 topic in this lane'
                : '${model.laneCount} topics in this lane',
          ),
          const SizedBox(height: 14),
          _LaneStrip(model: model),
          const SizedBox(height: CurioSpace.gap + 6),
          if (model.catalogEmpty)
            const CatalogMissing()
          else if (topic == null)
            _EmptyLane(lane: model.lane.label)
          else ...[
            _Deck(model: model, topic: topic),
            const SizedBox(height: 6),
            _SpinButton(
              turn: _turn,
              enabled: model.laneCount > 1,
              onTap: _spin,
            ),
            const SizedBox(height: 6),
            Center(
              child: Text(
                model.peeks.isEmpty
                    ? 'the last card in this lane'
                    : '${model.peeks.length} more behind this one',
                style: TextStyle(
                  fontSize: 11.5,
                  color: Theme.of(context).colorScheme.onSurfaceVariant,
                ),
              ),
            ),
            const SizedBox(height: CurioSpace.gap + 8),
            _Actions(model: model, topic: topic),
            const SizedBox(height: CurioSpace.gap + 10),
            _Detail(topic: topic, accent: model.lane.accent),
            const SizedBox(height: 12),
            Text(
              'Spun ${model.spinCount} ${model.spinCount == 1 ? 'time' : 'times'} this session',
              style: TextStyle(
                fontSize: 11,
                color: Theme.of(context).colorScheme.onSurfaceVariant,
              ),
            ),
          ],
        ],
      ),
    );
  }
}

/// The lane strip: the same chips the phone's Spin carries, so a desktop hand
/// can be dealt from any lane without leaving the room.
class _LaneStrip extends StatelessWidget {
  const _LaneStrip({required this.model});

  final AppModel model;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 34,
      child: ListView.separated(
        scrollDirection: Axis.horizontal,
        itemCount: kLanes.length,
        separatorBuilder: (context, index) => const SizedBox(width: 8),
        itemBuilder: (context, index) {
          final lane = kLanes[index];
          return LaneChip(
            label: lane.label,
            accent: lane.accent,
            selected: lane.id == model.laneId,
            onTap: () => model.selectLane(lane.id),
          );
        },
      ),
    );
  }
}

/// The hand: the front card face up, its two peeks fanned behind it — each peek
/// wearing the NAME of the card behind it at a whisper, so the deck says how
/// much is left instead of being two blank slabs.
class _Deck extends StatelessWidget {
  const _Deck({required this.model, required this.topic});

  final AppModel model;
  final Topic topic;

  @override
  Widget build(BuildContext context) {
    final accent = model.lane.accent;
    final peeks = model.peeks;
    const cardWidth = 560.0;
    const cardHeight = 292.0;

    return SizedBox(
      height: 340,
      child: Center(
        child: SizedBox(
          width: cardWidth + 60,
          child: Stack(
            alignment: Alignment.topCenter,
            children: [
              // Peeks, deepest first, so the nearer card draws over the far one.
              for (var index = peeks.length; index >= 1; index--)
                Positioned(
                  top: 34.0 - 16 * index,
                  child: Transform.rotate(
                    angle: (index.isEven ? 1 : -1) * 0.028,
                    child: _CardFrame(
                      accent: accent,
                      width: cardWidth - 34.0 * index,
                      height: cardHeight - 12.0 * index,
                      faded: true,
                      child: Align(
                        alignment: Alignment.bottomLeft,
                        child: Padding(
                          padding: const EdgeInsets.all(20),
                          child: Text(
                            peeks[index - 1].name,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: TextStyle(
                              fontSize: 15,
                              fontWeight: FontWeight.w700,
                              color: inkOn(Color(accent)).withValues(alpha: 0.4),
                            ),
                          ),
                        ),
                      ),
                    ),
                  ),
                ),
              // The front card. The outgoing one lifts and fades while the new
              // one rises into its place — a deal, not a swap.
              AnimatedSwitcher(
                duration: const Duration(milliseconds: 420),
                switchInCurve: Curves.easeOutCubic,
                switchOutCurve: Curves.easeInCubic,
                layoutBuilder: (current, previous) => Stack(
                  alignment: Alignment.topCenter,
                  children: <Widget>[...previous, if (current != null) current],
                ),
                transitionBuilder: (child, animation) {
                  final curve = CurvedAnimation(
                    parent: animation,
                    curve: Curves.easeOutCubic,
                  );
                  final incoming = animation.status != AnimationStatus.reverse;
                  return FadeTransition(
                    opacity: curve,
                    child: SlideTransition(
                      position: Tween<Offset>(
                        begin: Offset(0, incoming ? 0.06 : -0.04),
                        end: Offset.zero,
                      ).animate(curve),
                      child: ScaleTransition(
                        scale: Tween<double>(begin: 0.97, end: 1).animate(curve),
                        child: child,
                      ),
                    ),
                  );
                },
                child: KeyedSubtree(
                  key: ValueKey<String>(topic.id),
                  child: _CardFrame(
                    accent: accent,
                    width: cardWidth,
                    height: cardHeight,
                    child: _CardFace(topic: topic, accent: accent),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _CardFrame extends StatelessWidget {
  const _CardFrame({
    required this.accent,
    required this.width,
    required this.height,
    required this.child,
    this.faded = false,
  });

  final int accent;
  final double width;
  final double height;
  final Widget child;
  final bool faded;

  @override
  Widget build(BuildContext context) {
    final dark = Theme.of(context).brightness == Brightness.dark;
    return Container(
      width: width,
      height: height,
      decoration: BoxDecoration(
        gradient: laneGradient(accent, dark: dark),
        borderRadius: BorderRadius.circular(CurioSpace.pageRadius),
        boxShadow: faded
            ? null
            : [
                // The phone's tinted shadow: the lane's own colour, deepened,
                // rather than a grey halo.
                BoxShadow(
                  color: Color(accent).withValues(alpha: dark ? 0.28 : 0.32),
                  blurRadius: 30,
                  offset: const Offset(0, 16),
                ),
              ],
      ),
      clipBehavior: Clip.antiAlias,
      child: child,
    );
  }
}

class _CardFace extends StatelessWidget {
  const _CardFace({required this.topic, required this.accent});

  final Topic topic;
  final int accent;

  @override
  Widget build(BuildContext context) {
    final ink = inkOn(Color(accent));
    return Stack(
      fit: StackFit.expand,
      children: [
        if (topic.imageUrl.isNotEmpty)
          Image.network(
            topic.imageUrl,
            fit: BoxFit.cover,
            errorBuilder: (context, error, stack) => const SizedBox.shrink(),
          ),
        // A wash over the artwork so the name always reads, whatever the cover
        // behind it happens to be.
        DecoratedBox(
          decoration: BoxDecoration(
            gradient: LinearGradient(
              begin: Alignment.topCenter,
              end: Alignment.bottomCenter,
              colors: [
                Color(accent).withValues(alpha: 0.22),
                Color(accent).withValues(alpha: 0.82),
                Color(accent).withValues(alpha: 0.96),
              ],
              stops: const [0, 0.55, 1],
            ),
          ),
        ),
        Padding(
          padding: const EdgeInsets.all(28),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisAlignment: MainAxisAlignment.end,
            children: [
              Row(
                children: [
                  if (topic.subtype.isNotEmpty)
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 4),
                      decoration: BoxDecoration(
                        color: ink.withValues(alpha: 0.16),
                        borderRadius: BorderRadius.circular(999),
                      ),
                      child: Text(
                        topic.subtype.toUpperCase(),
                        style: TextStyle(
                          fontSize: 10,
                          fontWeight: FontWeight.w700,
                          letterSpacing: 1,
                          color: ink,
                        ),
                      ),
                    ),
                  const Spacer(),
                  if (topic.minutes > 0)
                    Text(
                      '${topic.minutes} min',
                      style: TextStyle(
                        fontSize: 11.5,
                        fontWeight: FontWeight.w600,
                        color: ink.withValues(alpha: 0.8),
                      ),
                    ),
                ],
              ),
              const SizedBox(height: 10),
              Text(
                topic.name,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
                style: TextStyle(
                  fontSize: 34,
                  height: 1.08,
                  fontWeight: FontWeight.w800,
                  letterSpacing: -0.8,
                  color: ink,
                ),
              ),
              if (topic.credit.isNotEmpty) ...[
                const SizedBox(height: 6),
                Text(
                  topic.credit,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: TextStyle(
                    fontSize: 13.5,
                    color: ink.withValues(alpha: 0.85),
                  ),
                ),
              ],
              if (topic.teaser.isNotEmpty) ...[
                const SizedBox(height: 10),
                Text(
                  topic.teaser,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  style: TextStyle(
                    fontSize: 12.5,
                    height: 1.45,
                    color: ink.withValues(alpha: 0.9),
                  ),
                ),
              ],
            ],
          ),
        ),
      ],
    );
  }
}

/// The phone's spin control (`SpinButton` + `OrbitRing`): a raised accent disc
/// with the die on it, inside a ring that turns while the deck is dealt.
class _SpinButton extends StatelessWidget {
  const _SpinButton({
    required this.turn,
    required this.enabled,
    required this.onTap,
  });

  final AnimationController turn;
  final bool enabled;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Center(
      child: SizedBox(
        width: 168,
        height: 168,
        child: Stack(
          alignment: Alignment.center,
          children: [
            Positioned.fill(
              child: AnimatedBuilder(
                animation: turn,
                builder: (context, _) => RotationTransition(
                  turns: turn,
                  child: CustomPaint(
                    painter: _OrbitPainter(
                      colour: scheme.primary
                          .withValues(alpha: enabled ? 0.5 : 0.2),
                      active: turn.isAnimating,
                    ),
                  ),
                ),
              ),
            ),
            _SpinDisc(enabled: enabled, onTap: onTap),
          ],
        ),
      ),
    );
  }
}

class _SpinDisc extends StatefulWidget {
  const _SpinDisc({required this.enabled, required this.onTap});

  final bool enabled;
  final VoidCallback onTap;

  @override
  State<_SpinDisc> createState() => _SpinDiscState();
}

class _SpinDiscState extends State<_SpinDisc> {
  bool _pressed = false;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Tooltip(
      message: 'Deal the next card',
      child: GestureDetector(
        onTapDown: widget.enabled ? (_) => setState(() => _pressed = true) : null,
        onTapCancel: () => setState(() => _pressed = false),
        onTapUp: (_) {
          setState(() => _pressed = false);
          if (widget.enabled) widget.onTap();
        },
        child: AnimatedScale(
          scale: _pressed ? 0.94 : 1,
          duration: const Duration(milliseconds: 110),
          curve: Curves.easeOutCubic,
          child: Container(
            width: 104,
            height: 104,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              gradient: widget.enabled
                  // The phone's 3D button: a light crown at the top-left and a
                  // deepened foot, so the disc reads as raised.
                  ? RadialGradient(
                      center: const Alignment(-0.28, -0.4),
                      radius: 1.05,
                      colors: [
                        Color.lerp(scheme.primary, Colors.white, 0.22)!,
                        scheme.primary,
                        Color.lerp(scheme.primary, Colors.black, 0.08)!,
                      ],
                    )
                  : null,
              color: widget.enabled ? null : scheme.surfaceContainerHighest,
              boxShadow: widget.enabled
                  ? [
                      BoxShadow(
                        color: scheme.primary.withValues(alpha: 0.35),
                        blurRadius: 18,
                        offset: const Offset(0, 8),
                      ),
                    ]
                  : null,
            ),
            alignment: Alignment.center,
            child: CurioGlyphIcon(
              // The app's own die, drawn at the phone's stroke — the stock
              // casino glyph is one of the most recognisably foreign icons in
              // the whole rail.
              CurioGlyph.spin,
              size: 40,
              color: widget.enabled
                  ? inkOn(scheme.primary)
                  : scheme.onSurfaceVariant.withValues(alpha: 0.6),
            ),
          ),
        ),
      ),
    );
  }
}

/// The orbit: a dashed ring (the phone draws it as a sweeping arc). Idle it is
/// still; while a deal runs the whole ring turns, which is what says the press
/// landed.
class _OrbitPainter extends CustomPainter {
  const _OrbitPainter({required this.colour, required this.active});

  final Color colour;
  final bool active;

  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = colour
      ..style = PaintingStyle.stroke
      ..strokeWidth = active ? 2.2 : 1.4
      ..strokeCap = StrokeCap.round;
    final rect = Rect.fromCircle(
      center: Offset(size.width / 2, size.height / 2),
      radius: size.width / 2 - 3,
    );
    const dashes = 14;
    for (var index = 0; index < dashes; index++) {
      final start = (index / dashes) * 2 * math.pi;
      canvas.drawArc(rect, start, 0.24, false, paint);
    }
  }

  @override
  bool shouldRepaint(_OrbitPainter oldDelegate) =>
      oldDelegate.colour != colour || oldDelegate.active != active;
}

class _Actions extends StatelessWidget {
  const _Actions({required this.model, required this.topic});

  final AppModel model;
  final Topic topic;

  @override
  Widget build(BuildContext context) {
    final saved = model.isSaved(topic.id);
    return Row(
      children: [
        FilledButton.tonalIcon(
          onPressed: saved ? null : model.saveCurrent,
          icon: Icon(
            saved ? Icons.check_rounded : Icons.bookmark_add_outlined,
            size: 18,
          ),
          label: Text(saved ? 'In the Cabinet' : 'Keep it'),
        ),
        const SizedBox(width: 10),
        OutlinedButton.icon(
          onPressed: model.shuffle,
          icon: const Icon(Icons.shuffle_rounded, size: 17),
          label: const Text('Deal again'),
        ),
        const Spacer(),
        Text(
          'Lane: ${model.lane.label}',
          style: TextStyle(
            fontSize: 11.5,
            color: Theme.of(context).colorScheme.onSurfaceVariant,
          ),
        ),
      ],
    );
  }
}

class _Detail extends StatelessWidget {
  const _Detail({required this.topic, required this.accent});

  final Topic topic;
  final int accent;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final dark = Theme.of(context).brightness == Brightness.dark;
    final heading = topic.verb.isNotEmpty ? topic.verb : 'Explore';
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(22),
      decoration: BoxDecoration(
        color: scheme.surfaceContainerHighest.withValues(alpha: 0.5),
        borderRadius: BorderRadius.circular(CurioSpace.cardRadius),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Text(
                heading.toUpperCase(),
                style: TextStyle(
                  fontSize: 11,
                  fontWeight: FontWeight.w800,
                  letterSpacing: 1.2,
                  color: Color(accent),
                ),
              ),
              if (topic.minutes > 0) ...[
                const SizedBox(width: 8),
                Text(
                  '· ${topic.minutes} min',
                  style: TextStyle(fontSize: 11.5, color: scheme.onSurfaceVariant),
                ),
              ],
            ],
          ),
          if (topic.target.isNotEmpty) ...[
            const SizedBox(height: 10),
            Text(topic.target, style: Theme.of(context).textTheme.headlineSmall),
          ],
          if (topic.instruction.isNotEmpty) ...[
            const SizedBox(height: 10),
            Text(
              topic.instruction,
              style: TextStyle(
                fontSize: 13.5,
                height: 1.55,
                color: scheme.onSurface.withValues(alpha: 0.86),
              ),
            ),
          ],
          if (topic.tags.isNotEmpty) ...[
            const SizedBox(height: 16),
            Wrap(
              spacing: 6,
              runSpacing: 6,
              children: [
                for (final tag in topic.tags)
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 4),
                    decoration: BoxDecoration(
                      color: dark
                          ? Colors.white.withValues(alpha: 0.06)
                          : scheme.surface,
                      borderRadius: BorderRadius.circular(999),
                    ),
                    child: Text(
                      tag,
                      style: TextStyle(fontSize: 11, color: scheme.onSurfaceVariant),
                    ),
                  ),
              ],
            ),
          ],
        ],
      ),
    );
  }
}

class _EmptyLane extends StatelessWidget {
  const _EmptyLane({required this.lane});

  final String lane;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(22),
      decoration: BoxDecoration(
        color: scheme.surfaceContainerHighest.withValues(alpha: 0.5),
        borderRadius: BorderRadius.circular(CurioSpace.cardRadius),
      ),
      child: Text(
        'No topics in $lane yet — tap another lane above.',
        style: TextStyle(fontSize: 13, color: scheme.onSurfaceVariant),
      ),
    );
  }
}
