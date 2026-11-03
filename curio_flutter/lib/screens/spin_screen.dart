import 'package:flutter/material.dart';

import '../data/topic.dart';
import '../store/app_model.dart';
import '../theme/curio_theme.dart';
import '../widgets/curio_widgets.dart';

/// The Spin room: three cards, the front one face up.
///
/// The deck is a STACK, not a carousel: a desktop window shows the card you are
/// on and the two behind it, so a spin reads as a hand being dealt rather than
/// as a page changing. Everything the phone puts on its reveal (the verb, what
/// to do, how long it takes) is on the page under the deck, in one column.
class SpinScreen extends StatelessWidget {
  const SpinScreen({super.key, required this.model});

  final AppModel model;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
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
            trailing: FilledButton.icon(
              onPressed: topic == null ? null : model.shuffle,
              icon: const Icon(Icons.casino_rounded, size: 18),
              label: const Text('Spin'),
            ),
          ),
          const SizedBox(height: CurioSpace.gap + 6),
          if (model.catalogEmpty)
            const CatalogMissing()
          else if (topic == null)
            _EmptyLane(lane: model.lane.label)
          else ...[
            _Deck(model: model, topic: topic),
            const SizedBox(height: CurioSpace.gap + 8),
            _Actions(model: model, topic: topic),
            const SizedBox(height: CurioSpace.gap + 10),
            _Detail(topic: topic, accent: model.lane.accent),
          ],
          const SizedBox(height: 8),
          Text(
            'Spun ${model.spinCount} ${model.spinCount == 1 ? 'time' : 'times'} this session',
            style: TextStyle(fontSize: 11.5, color: scheme.onSurfaceVariant),
          ),
        ],
      ),
    );
  }
}

class _Deck extends StatelessWidget {
  const _Deck({required this.model, required this.topic});

  final AppModel model;
  final Topic topic;

  @override
  Widget build(BuildContext context) {
    final accent = model.lane.accent;
    final peeks = model.peeks;
    return SizedBox(
      height: 268,
      child: Stack(
        alignment: Alignment.center,
        children: [
          // The two cards behind: scaled and lifted, so the deck has depth
          // without a shadow (an animating card with a shadow reads as a box —
          // the phone learned that the hard way).
          for (var index = peeks.length; index >= 1; index--)
            Positioned(
              top: 6.0 * index,
              child: Transform.scale(
                scale: 1 - 0.035 * index,
                child: _CardFrame(
                  accent: accent,
                  width: 480,
                  height: 240,
                  child: const SizedBox.shrink(),
                ),
              ),
            ),
          _CardFrame(
            accent: accent,
            width: 520,
            height: 252,
            child: _CardFace(topic: topic, accent: accent),
          ),
        ],
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
  });

  final int accent;
  final double width;
  final double height;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    final dark = Theme.of(context).brightness == Brightness.dark;
    return Container(
      width: width,
      height: height,
      decoration: BoxDecoration(
        gradient: laneGradient(accent, dark: dark),
        borderRadius: BorderRadius.circular(CurioSpace.pageRadius),
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
          padding: const EdgeInsets.all(26),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisAlignment: MainAxisAlignment.end,
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
              const SizedBox(height: 10),
              Text(
                topic.name,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
                style: TextStyle(
                  fontSize: 32,
                  height: 1.1,
                  fontWeight: FontWeight.w800,
                  letterSpacing: -0.7,
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
                  maxLines: 3,
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

class _Actions extends StatelessWidget {
  const _Actions({required this.model, required this.topic});

  final AppModel model;
  final Topic topic;

  @override
  Widget build(BuildContext context) {
    final saved = model.isSaved(topic.id);
    return Row(
      children: [
        FilledButton.icon(
          onPressed: model.shuffle,
          icon: const Icon(Icons.casino_rounded, size: 18),
          label: const Text('Spin again'),
        ),
        const SizedBox(width: 10),
        FilledButton.tonalIcon(
          onPressed: saved ? null : model.saveCurrent,
          icon: Icon(saved ? Icons.check_rounded : Icons.bookmark_add_outlined, size: 18),
          label: Text(saved ? 'In the Cabinet' : 'Keep it'),
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
                  style: TextStyle(
                    fontSize: 11.5,
                    color: scheme.onSurfaceVariant,
                  ),
                ),
              ],
            ],
          ),
          if (topic.target.isNotEmpty) ...[
            const SizedBox(height: 10),
            Text(
              topic.target,
              style: Theme.of(context).textTheme.headlineSmall,
            ),
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
                      style: TextStyle(
                        fontSize: 11,
                        color: scheme.onSurfaceVariant,
                      ),
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
        'No topics in $lane yet — choose another lane in the column on the left.',
        style: TextStyle(fontSize: 13, color: scheme.onSurfaceVariant),
      ),
    );
  }
}
