import 'package:flutter/material.dart';

import '../data/lanes.dart';
import '../screens/cabinet_screen.dart';
import '../screens/home_screen.dart';
import '../screens/settings_screen.dart';
import '../screens/spin_screen.dart';
import '../store/app_model.dart';
import '../theme/curio_paper.dart';
import '../theme/curio_theme.dart';
import '../widgets/curio_glyphs.dart';

/// The desktop shell: a rail of four rooms, a context column, and the page.
///
/// **This is the "totally new layout" the member asked for** — a Windows window
/// is wide and has no thumb, so the phone's bottom bar and its one-column page
/// would be the wrong shape. The rail names the rooms, the context column holds
/// whatever the room is *about* (lanes on Home and Spin, saved discoveries in
/// the Cabinet, the data folders in Settings), and the page does the work. The
/// iOS shell keeps this state and swaps this frame for a tab bar + one column.
class CurioShell extends StatefulWidget {
  const CurioShell({super.key, required this.model});

  final AppModel model;

  @override
  State<CurioShell> createState() => _CurioShellState();
}

class _CurioShellState extends State<CurioShell> {
  int _room = 0;

  static const _rooms = <_Room>[
    _Room('Home', CurioGlyph.home),
    _Room('Spin', CurioGlyph.spin),
    _Room('Cabinet', CurioGlyph.cabinet),
    _Room('Settings', CurioGlyph.settings),
  ];

  @override
  Widget build(BuildContext context) {
    final model = widget.model;
    return AnimatedBuilder(
      animation: model,
      builder: (context, _) {
        final scheme = Theme.of(context).colorScheme;
        return Scaffold(
          body: Stack(
            children: [
              // ── THE PAPER UNDER EVERY ROOM ───────────────────────────
              //
              // The phone's look is a sheet on a table, and the sheet's own
              // texture is what carries it — not its torn edge (see
              // [PaperBackdrop]). One field for the whole window, seeded, so a
              // room change never re-scatters the grain.
              const Positioned.fill(
                child: PaperBackdrop(seed: 11, strength: 0.9, clouds: false),
              ),
              Row(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  _Rail(
                    rooms: _rooms,
                    selected: _room,
                    onSelect: (index) => setState(() => _room = index),
                  ),
                  VerticalDivider(width: 1, color: scheme.outlineVariant),
                  SizedBox(
                    width: CurioSpace.paneWidth,
                    child: _ContextPane(
                      model: model,
                      room: _room,
                      onOpenRoom: (index) => setState(() => _room = index),
                    ),
                  ),
                  VerticalDivider(width: 1, color: scheme.outlineVariant),
                  Expanded(child: _page(model)),
                ],
              ),
            ],
          ),
        );
      },
    );
  }

  Widget _page(AppModel model) {
    if (model.loading) {
      return const Center(child: CircularProgressIndicator());
    }
    return switch (_room) {
      0 => HomeScreen(
          model: model,
          onSpin: () => setState(() => _room = 1),
          onWrite: () => setState(() => _room = 2),
        ),
      1 => SpinScreen(model: model),
      2 => CabinetScreen(model: model),
      _ => SettingsScreen(model: model),
    };
  }
}

class _Room {
  const _Room(this.label, this.glyph);

  final String label;

  /// The room's mark, from Curio's own drawn set — a rail of stock Material
  /// glyphs is the one place the borrowed family shows the most.
  final CurioGlyph glyph;
}

/// The left rail. A destination is one pill: its glyph, and its name under it
/// once it is chosen (a label on all four would be a nav bar stood on its side).
class _Rail extends StatelessWidget {
  const _Rail({required this.rooms, required this.selected, required this.onSelect});

  final List<_Room> rooms;
  final int selected;
  final ValueChanged<int> onSelect;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Container(
      width: CurioSpace.railWidth,
      color: scheme.surface,
      child: Column(
        children: [
          const SizedBox(height: 18),
          // The wordmark: the rose disc is the app's mark, and it is the one
          // thing on the rail that is not a destination.
          Container(
            width: 38,
            height: 38,
            decoration: const BoxDecoration(
              color: CurioPalette.rose,
              shape: BoxShape.circle,
            ),
            alignment: Alignment.center,
            child: const CurioGlyphIcon(
              CurioGlyph.spark,
              size: 20,
              color: Colors.white,
            ),
          ),
          const SizedBox(height: 22),
          for (var index = 0; index < rooms.length; index++)
            Padding(
              padding: const EdgeInsets.only(bottom: 6),
              child: _RailButton(
                room: rooms[index],
                active: index == selected,
                onTap: () => onSelect(index),
              ),
            ),
        ],
      ),
    );
  }
}

class _RailButton extends StatelessWidget {
  const _RailButton({
    required this.room,
    required this.active,
    required this.onTap,
  });

  final _Room room;
  final bool active;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final ink = active ? scheme.onPrimary : scheme.onSurfaceVariant;
    return Tooltip(
      message: room.label,
      child: Semantics(
        selected: active,
        button: true,
        label: room.label,
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(16),
          child: AnimatedContainer(
            duration: const Duration(milliseconds: 180),
            width: 56,
            padding: const EdgeInsets.symmetric(vertical: 9),
            decoration: BoxDecoration(
              color: active ? scheme.primary : Colors.transparent,
              borderRadius: BorderRadius.circular(16),
            ),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                CurioGlyphIcon(room.glyph, size: 21, color: ink),
                const SizedBox(height: 3),
                Text(
                  room.label,
                  style: TextStyle(
                    fontSize: 9.5,
                    fontWeight: FontWeight.w600,
                    color: ink,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

/// The middle column: what the current room is about.
class _ContextPane extends StatelessWidget {
  const _ContextPane({
    required this.model,
    required this.room,
    required this.onOpenRoom,
  });

  final AppModel model;
  final int room;
  final ValueChanged<int> onOpenRoom;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final heading = switch (room) {
      0 || 1 => 'Lanes',
      2 => 'Saved',
      _ => 'This device',
    };
    return Container(
      color: scheme.surfaceContainerHighest.withValues(alpha: 0.32),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(18, 20, 18, 10),
            child: Text(
              heading.toUpperCase(),
              style: TextStyle(
                fontSize: 11,
                fontWeight: FontWeight.w700,
                letterSpacing: 1.1,
                color: scheme.onSurfaceVariant,
              ),
            ),
          ),
          Expanded(
            child: switch (room) {
              0 || 1 => _LaneList(model: model),
              2 => _SavedList(model: model),
              _ => _DevicePane(model: model),
            },
          ),
          if (room == 0)
            Padding(
              padding: const EdgeInsets.all(14),
              child: FilledButton.icon(
                onPressed: () => onOpenRoom(1),
                icon: const Icon(Icons.casino_rounded, size: 18),
                label: const Text('Spin'),
              ),
            ),
        ],
      ),
    );
  }
}

class _LaneList extends StatelessWidget {
  const _LaneList({required this.model});

  final AppModel model;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return ListView.builder(
      padding: const EdgeInsets.fromLTRB(10, 0, 10, 12),
      itemCount: kLanes.length,
      itemBuilder: (context, index) {
        final lane = kLanes[index];
        final active = lane.id == model.laneId;
        return Padding(
          padding: const EdgeInsets.symmetric(vertical: 1),
          child: InkWell(
            onTap: () => model.selectLane(lane.id),
            borderRadius: BorderRadius.circular(10),
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
              decoration: BoxDecoration(
                color: active
                    ? Color(lane.accent).withValues(alpha: 0.16)
                    : Colors.transparent,
                borderRadius: BorderRadius.circular(10),
              ),
              child: Row(
                children: [
                  Container(
                    width: 8,
                    height: 8,
                    decoration: BoxDecoration(
                      color: Color(lane.accent),
                      shape: BoxShape.circle,
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Text(
                      lane.label,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(
                        fontSize: 13,
                        fontWeight: active ? FontWeight.w700 : FontWeight.w500,
                        color: active ? scheme.onSurface : scheme.onSurfaceVariant,
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        );
      },
    );
  }
}

class _SavedList extends StatelessWidget {
  const _SavedList({required this.model});

  final AppModel model;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    if (model.saved.isEmpty) {
      return Padding(
        padding: const EdgeInsets.symmetric(horizontal: 18),
        child: Text(
          'Nothing saved yet. Spin a card and keep it.',
          style: TextStyle(fontSize: 12.5, color: scheme.onSurfaceVariant, height: 1.4),
        ),
      );
    }
    return ListView.builder(
      padding: const EdgeInsets.fromLTRB(10, 0, 10, 12),
      itemCount: model.saved.length,
      itemBuilder: (context, index) {
        final entry = model.saved[index];
        return Padding(
          padding: const EdgeInsets.symmetric(vertical: 1),
          child: InkWell(
            borderRadius: BorderRadius.circular(10),
            onTap: () {},
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 9),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    entry.topic.name,
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w600),
                  ),
                  if (entry.topic.credit.isNotEmpty)
                    Text(
                      entry.topic.credit,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(fontSize: 11.5, color: scheme.onSurfaceVariant),
                    ),
                ],
              ),
            ),
          ),
        );
      },
    );
  }
}

class _DevicePane extends StatelessWidget {
  const _DevicePane({required this.model});

  final AppModel model;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final style = TextStyle(fontSize: 11.5, color: scheme.onSurfaceVariant, height: 1.5);
    return Padding(
      padding: const EdgeInsets.fromLTRB(18, 0, 18, 18),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('Data folder', style: style.copyWith(fontWeight: FontWeight.w700)),
          const SizedBox(height: 4),
          SelectableText(model.dataPath, style: style),
          const SizedBox(height: 14),
          Text('Catalog', style: style.copyWith(fontWeight: FontWeight.w700)),
          const SizedBox(height: 4),
          Text('${model.totalTopics} topics across ${kLanes.length} lanes', style: style),
        ],
      ),
    );
  }
}
