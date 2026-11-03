import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../data/journal.dart';
import '../data/lanes.dart';
import '../store/app_model.dart';
import '../theme/curio_theme.dart';
import '../widgets/curio_glyphs.dart';
import '../widgets/curio_widgets.dart';

/// The Home room.
///
/// The phone's Home is a rose hero carrying the member's counts (Streak ·
/// Cabinet · Topics), the newest card, the writing desk and the lane rail. This
/// is that page at window size: the same hero and the same three things under
/// it — **today's card**, **the page you were writing** and **the pages you
/// wrote recently** — so a member opening Curio on a desktop sees exactly what
/// the phone would have told them.
class HomeScreen extends StatelessWidget {
  const HomeScreen({
    super.key,
    required this.model,
    required this.onSpin,
    required this.onWrite,
  });

  final AppModel model;
  final VoidCallback onSpin;
  final VoidCallback onWrite;

  @override
  Widget build(BuildContext context) {
    final resume = model.resumeJournal;
    return PageBody(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          PageHeader(title: 'Curio', subtitle: _today()),
          const SizedBox(height: CurioSpace.gap + 4),
          if (model.catalogEmpty) ...[
            const CatalogMissing(),
            const SizedBox(height: CurioSpace.gap),
          ],
          _Hero(
            model: model,
            onSpin: onSpin,
            onWrite: () async {
              await model.createJournal();
              onWrite();
            },
          ),
          const SizedBox(height: CurioSpace.gap + 4),
          _TodayCard(model: model, onSpin: onSpin),
          const SizedBox(height: CurioSpace.gap + 4),
          _Desk(
            entry: resume,
            onContinue: resume == null
                ? null
                : () {
                    model.openJournalById(resume.id);
                    onWrite();
                  },
            onNew: () async {
              await model.createJournal();
              onWrite();
            },
          ),
          if (model.journals.isNotEmpty) ...[
            const SizedBox(height: CurioSpace.gap + 10),
            _Recent(
              entries: model.recentJournals,
              onOpen: (entry) {
                model.openJournalById(entry.id);
                onWrite();
              },
            ),
          ],
          const SizedBox(height: CurioSpace.gap + 10),
          const _SectionLabel(text: 'Pick a lane'),
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

  static String _today() {
    const weekdays = <String>[
      'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday',
    ];
    const months = <String>[
      'January', 'February', 'March', 'April', 'May', 'June',
      'July', 'August', 'September', 'October', 'November', 'December',
    ];
    final now = DateTime.now();
    return '${weekdays[now.weekday - 1]}, ${now.day} ${months[now.month - 1]}';
  }
}

// ═══════════════════════════════════════════════════════════════════════
// The hero
// ═══════════════════════════════════════════════════════════════════════

/// The rose slab: one sentence, two doors, the member's own four counts and a
/// quiet shadow so it reads as paper laid on the page.
///
/// The counts live IN the hero because that is where the phone puts them (its
/// `HeroStatSegment` row under the banner's sentence) — a separate card of
/// numbers would say the same thing from a place the eye has already left.
class _Hero extends StatelessWidget {
  const _Hero({
    required this.model,
    required this.onSpin,
    required this.onWrite,
  });

  final AppModel model;
  final VoidCallback onSpin;
  final VoidCallback onWrite;

  @override
  Widget build(BuildContext context) {
    final dark = Theme.of(context).brightness == Brightness.dark;
    final top = dark ? CurioPalette.roseNight : CurioPalette.rose;
    final bottom = Color.lerp(top, CurioPalette.plum, dark ? 0.45 : 0.22)!;
    final ink = Colors.white.withValues(alpha: 0.96);

    return Container(
      width: double.infinity,
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
      clipBehavior: Clip.antiAlias,
      child: Stack(
        children: [
          // The phone's mirrored watermark collage (`HomeHeroSymbol`): a few of
          // the app's own glyphs, rotated and held at a whisper, so the slab is
          // a place rather than a rectangle of colour.
          const _Watermark(
            glyph: CurioGlyph.book,
            alignment: Alignment.topRight,
            size: 132,
            turn: 0.18,
          ),
          const _Watermark(
            glyph: CurioGlyph.compass,
            alignment: Alignment.bottomRight,
            size: 104,
            turn: -0.22,
          ),
          const _Watermark(
            glyph: CurioGlyph.spark,
            alignment: Alignment.bottomLeft,
            size: 78,
            turn: 0.12,
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(30, 26, 30, 24),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  _overline(),
                  style: TextStyle(
                    fontSize: 10.5,
                    fontWeight: FontWeight.w800,
                    letterSpacing: 1.5,
                    color: ink.withValues(alpha: 0.72),
                  ),
                ),
                const SizedBox(height: 10),
                Text(
                  'Curiosity, one card at a time.',
                  style: TextStyle(
                    fontSize: 30,
                    height: 1.15,
                    fontWeight: FontWeight.w800,
                    letterSpacing: -0.6,
                    color: ink,
                  ),
                ),
                const SizedBox(height: 8),
                Text(
                  'Draw something worth knowing. Read it, hear it, watch it — '
                  'then keep it in your Cabinet, or write it out in a page of '
                  'your own.',
                  style: TextStyle(
                    fontSize: 13.5,
                    height: 1.5,
                    color: ink.withValues(alpha: 0.86),
                  ),
                ),
                const SizedBox(height: 20),
                Row(
                  children: [
                    FilledButton.icon(
                      onPressed: onSpin,
                      style: FilledButton.styleFrom(
                        backgroundColor: Colors.white.withValues(alpha: 0.96),
                        foregroundColor: bottom,
                      ),
                      icon: const Icon(Icons.casino_rounded, size: 18),
                      label: const Text('Draw a card'),
                    ),
                    const SizedBox(width: 10),
                    TextButton.icon(
                      onPressed: onWrite,
                      style: TextButton.styleFrom(
                        foregroundColor: ink.withValues(alpha: 0.92),
                      ),
                      icon: const Icon(Icons.edit_note_rounded, size: 20),
                      label: const Text('Write a page'),
                    ),
                  ],
                ),
                const SizedBox(height: 20),
                Container(
                  height: 1,
                  color: ink.withValues(alpha: 0.18),
                ),
                const SizedBox(height: 14),
                Row(
                  children: [
                    _HeroStat(
                      glyph: CurioGlyph.spark,
                      value: '${model.streakDays}',
                      label: 'day streak',
                      ink: ink,
                    ),
                    _HeroStat(
                      glyph: CurioGlyph.cabinet,
                      value: '${model.saved.length}',
                      label: 'in the Cabinet',
                      ink: ink,
                    ),
                    _HeroStat(
                      glyph: CurioGlyph.compass,
                      value: '${model.totalTopics}',
                      label: 'topics to meet',
                      ink: ink,
                    ),
                    _HeroStat(
                      glyph: CurioGlyph.feather,
                      value: '${model.wordsToday}',
                      label: 'words today',
                      ink: ink,
                    ),
                  ],
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  static String _overline() {
    const months = <String>[
      'JANUARY', 'FEBRUARY', 'MARCH', 'APRIL', 'MAY', 'JUNE',
      'JULY', 'AUGUST', 'SEPTEMBER', 'OCTOBER', 'NOVEMBER', 'DECEMBER',
    ];
    final now = DateTime.now();
    return '${now.day} ${months[now.month - 1]}';
  }
}

/// One count in the hero — the phone's `HeroStatSegment`: an icon, an extra-bold
/// value, and its name at a low alpha.
class _HeroStat extends StatelessWidget {
  const _HeroStat({
    required this.glyph,
    required this.value,
    required this.label,
    required this.ink,
  });

  final CurioGlyph glyph;
  final String value;
  final String label;
  final Color ink;

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          CurioGlyphIcon(glyph, size: 17, color: ink.withValues(alpha: 0.8)),
          const SizedBox(height: 5),
          Text(
            value,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: TextStyle(
              fontSize: 18,
              height: 1.1,
              fontWeight: FontWeight.w800,
              color: ink,
            ),
          ),
          const SizedBox(height: 2),
          Text(
            label,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: TextStyle(
              fontSize: 11,
              color: ink.withValues(alpha: 0.78),
            ),
          ),
        ],
      ),
    );
  }
}

class _Watermark extends StatelessWidget {
  const _Watermark({
    required this.glyph,
    required this.alignment,
    required this.size,
    required this.turn,
  });

  final CurioGlyph glyph;
  final Alignment alignment;
  final double size;
  final double turn;

  @override
  Widget build(BuildContext context) {
    return Positioned(
      left: alignment.x < 0 ? 6 : null,
      right: alignment.x > 0 ? 6 : null,
      top: alignment.y < 0 ? 4 : null,
      bottom: alignment.y > 0 ? 4 : null,
      child: IgnorePointer(
        child: Transform.rotate(
          angle: turn * math.pi,
          child: CurioGlyphIcon(
            glyph,
            size: size,
            color: Colors.white.withValues(alpha: 0.07),
            stroke: 1.2,
          ),
        ),
      ),
    );
  }
}

// ═══════════════════════════════════════════════════════════════════════
// Today's card — what the deck would deal from
// ═══════════════════════════════════════════════════════════════════════

/// The phone's Home carries the card the member is exploring (its
/// `CurrentlyExploringCard`). A window that showed only counts would make them
/// walk to Spin to remember what they were reading, so the front of the deck is
/// here, in its lane's own wash.
class _TodayCard extends StatelessWidget {
  const _TodayCard({required this.model, required this.onSpin});

  final AppModel model;
  final VoidCallback onSpin;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final dark = Theme.of(context).brightness == Brightness.dark;
    final topic = model.current;
    final lane = model.lane;
    if (topic == null) {
      return Container(
        width: double.infinity,
        padding: const EdgeInsets.all(22),
        decoration: BoxDecoration(
          color: scheme.surfaceContainerHighest.withValues(alpha: 0.5),
          borderRadius: BorderRadius.circular(CurioSpace.cardRadius),
        ),
        child: Row(
          children: [
            Expanded(
              child: Text(
                model.catalogEmpty
                    ? 'The catalog is empty, so there is nothing to deal.'
                    : 'Nothing dealt in ${lane.label} yet.',
                style: TextStyle(fontSize: 13, color: scheme.onSurfaceVariant),
              ),
            ),
            FilledButton.tonalIcon(
              onPressed: onSpin,
              icon: const Icon(Icons.casino_rounded, size: 18),
              label: const Text('Spin'),
            ),
          ],
        ),
      );
    }
    final accent = Color(lane.accent);
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.fromLTRB(22, 18, 18, 18),
      decoration: BoxDecoration(
        gradient: laneGradient(lane.accent, dark: dark),
        borderRadius: BorderRadius.circular(CurioSpace.cardRadius),
      ),
      child: Row(
        children: [
          Container(
            width: 4,
            height: 52,
            decoration: BoxDecoration(
              color: inkOn(accent).withValues(alpha: 0.5),
              borderRadius: BorderRadius.circular(2),
            ),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'TODAY\'S CARD · ${lane.label.toUpperCase()}',
                  style: TextStyle(
                    fontSize: 10.5,
                    fontWeight: FontWeight.w800,
                    letterSpacing: 1.2,
                    color: inkOn(accent).withValues(alpha: 0.75),
                  ),
                ),
                const SizedBox(height: 5),
                Text(
                  topic.name,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: TextStyle(
                    fontSize: 19,
                    fontWeight: FontWeight.w800,
                    color: inkOn(accent),
                  ),
                ),
                if (topic.verb.isNotEmpty || topic.teaser.isNotEmpty) ...[
                  const SizedBox(height: 3),
                  Text(
                    topic.verb.isNotEmpty ? topic.verb : topic.teaser,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: TextStyle(
                      fontSize: 12.5,
                      color: inkOn(accent).withValues(alpha: 0.82),
                    ),
                  ),
                ],
              ],
            ),
          ),
          const SizedBox(width: 12),
          FilledButton.tonal(
            onPressed: onSpin,
            child: const Text('Open'),
          ),
        ],
      ),
    );
  }
}

// ═══════════════════════════════════════════════════════════════════════
// The desk and the recents
// ═══════════════════════════════════════════════════════════════════════

/// The writing desk: resume the last page, or start one.
class _Desk extends StatelessWidget {
  const _Desk({required this.entry, required this.onContinue, required this.onNew});

  final JournalEntry? entry;
  final VoidCallback? onContinue;
  final Future<void> Function() onNew;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final page = entry;
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(22),
      decoration: BoxDecoration(
        color: scheme.surfaceContainerHighest.withValues(alpha: 0.5),
        borderRadius: BorderRadius.circular(CurioSpace.pageRadius),
      ),
      child: Row(
        children: [
          Container(
            width: 46,
            height: 46,
            decoration: BoxDecoration(
              color: scheme.primary.withValues(alpha: 0.14),
              borderRadius: BorderRadius.circular(14),
            ),
            alignment: Alignment.center,
            child: CurioGlyphIcon(CurioGlyph.feather, color: scheme.primary, size: 24),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: page == null
                ? Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Your writing desk',
                        style: Theme.of(context).textTheme.titleMedium,
                      ),
                      const SizedBox(height: 3),
                      Text(
                        'A page is yours alone — headings, quotes, checklists, '
                        'a mood and tags, kept on this device.',
                        style: TextStyle(
                          fontSize: 12.5,
                          height: 1.45,
                          color: scheme.onSurfaceVariant,
                        ),
                      ),
                    ],
                  )
                : Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        page.displayTitle,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: Theme.of(context).textTheme.titleMedium,
                      ),
                      const SizedBox(height: 3),
                      Text(
                        page.preview.isEmpty
                            ? '${page.words} ${page.words == 1 ? 'word' : 'words'} so far'
                            : page.preview,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: TextStyle(
                          fontSize: 12.5,
                          color: scheme.onSurfaceVariant,
                        ),
                      ),
                    ],
                  ),
          ),
          const SizedBox(width: 12),
          if (onContinue != null)
            FilledButton.tonalIcon(
              onPressed: onContinue,
              icon: const Icon(Icons.arrow_forward_rounded, size: 18),
              label: const Text('Continue'),
            )
          else
            FilledButton.tonalIcon(
              onPressed: () => onNew(),
              icon: const Icon(Icons.add_rounded, size: 18),
              label: const Text('New page'),
            ),
        ],
      ),
    );
  }
}

/// The phone's Home feed, at window size: the pages written lately, each one a
/// tap from being open.
class _Recent extends StatelessWidget {
  const _Recent({required this.entries, required this.onOpen});

  final List<JournalEntry> entries;
  final ValueChanged<JournalEntry> onOpen;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const _SectionLabel(text: 'Recently written'),
        const SizedBox(height: 6),
        for (final entry in entries)
          InkWell(
            onTap: () => onOpen(entry),
            borderRadius: BorderRadius.circular(12),
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 9),
              child: Row(
                children: [
                  CurioGlyphIcon(
                    CurioGlyph.book,
                    size: 17,
                    color: scheme.onSurfaceVariant,
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          entry.displayTitle,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: const TextStyle(
                            fontSize: 13.5,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                        if (entry.preview.isNotEmpty)
                          Text(
                            entry.preview,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: TextStyle(
                              fontSize: 11.5,
                              color: scheme.onSurfaceVariant,
                            ),
                          ),
                      ],
                    ),
                  ),
                  const SizedBox(width: 12),
                  Text(
                    _ago(entry.updatedAt),
                    style: TextStyle(
                      fontSize: 11,
                      color: scheme.onSurfaceVariant.withValues(alpha: 0.8),
                    ),
                  ),
                ],
              ),
            ),
          ),
      ],
    );
  }

  /// "just now" / "12m" / "3h" / "2d" — the phone's `prettyTime`, short.
  static String _ago(DateTime moment) {
    final delta = DateTime.now().difference(moment);
    if (delta.inMinutes < 1) return 'just now';
    if (delta.inMinutes < 60) return '${delta.inMinutes}m';
    if (delta.inHours < 24) return '${delta.inHours}h';
    if (delta.inDays < 7) return '${delta.inDays}d';
    return '${(delta.inDays / 7).floor()}w';
  }
}

class _SectionLabel extends StatelessWidget {
  const _SectionLabel({required this.text});

  final String text;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Text(
      text.toUpperCase(),
      style: TextStyle(
        fontSize: 11,
        fontWeight: FontWeight.w700,
        letterSpacing: 1.1,
        color: scheme.onSurfaceVariant,
      ),
    );
  }
}
