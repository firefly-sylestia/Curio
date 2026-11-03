import 'dart:async';
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../data/journal.dart';
import '../store/app_model.dart';
import '../store/journal_controller.dart';
import '../theme/curio_paper.dart';
import '../theme/curio_theme.dart';

/// A journal page, with the phone editor's tools on a desktop page.
///
/// The dock is the Android journal dock's, in its order and its manner: the
/// **format door** (Aa — bold, italic, underline, strike), the **marker door**
/// (the pens, which puts the first one down in one tap), the **heading door**
/// (one button wearing the line's level — I, II, or i for content), **todo**,
/// **small text**, the **bullet door** (dot · dash · numbered, with Remove
/// list), **quote**, **contents** and **export** — plus undo and redo, which the
/// phone keeps behind its history door and a desktop keeps in the row.
///
/// Every multi-choice tool opens IN the dock, under the row, exactly as the
/// phone's does (v433's collapse language): the choices stand where the tools
/// are, so the press and its answer are in the same place. A second press on a
/// door closes what it opened.
class JournalEditor extends StatefulWidget {
  const JournalEditor({
    super.key,
    required this.model,
    required this.entry,
  });

  final AppModel model;
  final JournalEntry entry;

  @override
  State<JournalEditor> createState() => _JournalEditorState();
}

/// The dock's expanding panels. `null` means the dock is closed.
enum _DockGroup { format, marker, heading, bullet, export }

class _JournalEditorState extends State<JournalEditor> {
  late JournalController _controller;
  late TextEditingController _title;
  late JournalEntry _base;
  late String _lastText;
  late int _mood;
  late List<String> _tags;

  final _focus = FocusNode();
  final _titleFocus = FocusNode();
  Timer? _saveTimer;
  bool _dirty = false;
  bool _contentsOpen = false;
  bool _toolsCollapsed = false;
  _DockGroup? _group;
  String _lastSelection = '';

  @override
  void initState() {
    super.initState();
    _attach(widget.entry);
    _controller.addListener(_onController);
  }

  void _attach(JournalEntry entry) {
    _base = entry;
    _controller = JournalController(entry);
    _controller.onMutated = _onMutated;
    _title = TextEditingController(text: entry.title);
    _lastText = entry.text;
    _mood = entry.mood;
    _tags = List<String>.from(entry.tags);
  }

  @override
  void didUpdateWidget(covariant JournalEditor oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.entry.id != _base.id) {
      _saveTimer?.cancel();
      _controller.removeListener(_onController);
      _controller.dispose();
      _title.dispose();
      _attach(widget.entry);
      _controller.addListener(_onController);
      _group = null;
    }
  }

  @override
  void dispose() {
    _saveTimer?.cancel();
    _flush(notify: false);
    _controller.removeListener(_onController);
    _controller.dispose();
    _title.dispose();
    _focus.dispose();
    _titleFocus.dispose();
    super.dispose();
  }

  void _onController() {
    // The dock's lit state follows the caret, so a SELECTION change repaints the
    // toolbar — a plain keystroke does not, hence the signature guard: without
    // it every character typed would rebuild the whole page and its dock.
    if (!mounted) return;
    final signature = '${_controller.selection.start}:${_controller.selection.end}';
    if (signature == _lastSelection) return;
    _lastSelection = signature;
    setState(() {});
  }

  /// Called by the controller after IT changed the text (a tool, an undo, a
  /// marker). The editor's idea of "the text as it was" has to move with it, or
  /// the next keystroke would shift every mark by the wrong amount.
  void _onMutated() {
    _lastText = _controller.text;
    _dirty = true;
    _saveTimer?.cancel();
    _saveTimer = Timer(const Duration(milliseconds: 700), _flush);
    if (mounted) setState(() {});
  }

  void _onTitleChanged() {
    _dirty = true;
    _saveTimer?.cancel();
    _saveTimer = Timer(const Duration(milliseconds: 700), _flush);
    setState(() {});
  }

  void _onChanged() {
    _controller.applyEdit(_lastText);
    _lastText = _controller.text;
    _dirty = true;
    _saveTimer?.cancel();
    _saveTimer = Timer(const Duration(milliseconds: 700), _flush);
    setState(() {});
  }

  void _flush({bool notify = true}) {
    if (!_dirty && _title.text == _base.title) return;
    final entry = _controller.toEntry(
      base: _base,
      title: _title.text,
      mood: _mood,
      tags: _tags,
    );
    _base = entry;
    _dirty = false;
    widget.model.saveJournal(entry);
    if (notify && mounted) setState(() {});
  }

  void _act(VoidCallback action) {
    action();
    setState(() {});
  }

  void _toggleGroup(_DockGroup group) {
    setState(() => _group = _group == group ? null : group);
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    final dark = theme.brightness == Brightness.dark;
    _controller.palette = JournalPalette(
      ink: scheme.onSurface,
      muted: scheme.onSurfaceVariant,
      accent: dark ? CurioPalette.coralBlush : CurioPalette.coralInk,
      paper: dark ? CurioPalette.nightRaised : CurioPalette.creamWhite,
      dark: dark,
    );

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        _Header(
          model: widget.model,
          title: _title,
          titleFocus: _titleFocus,
          mood: _mood,
          onMood: (value) {
            setState(() => _mood = value);
            _dirty = true;
            _flush();
          },
          words: _controller.text.trim().isEmpty
              ? 0
              : _controller.text.trim().split(RegExp(r'\s+')).length,
          saved: !_dirty,
          onChanged: _onTitleChanged,
          onDelete: () => _confirmDelete(context),
        ),
        Expanded(
          child: Padding(
            padding: const EdgeInsets.fromLTRB(
              CurioSpace.page,
              0,
              CurioSpace.page,
              CurioSpace.page,
            ),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Expanded(child: _page(context, dark)),
                AnimatedSize(
                  duration: const Duration(milliseconds: 180),
                  curve: Curves.easeOutCubic,
                  child: _contentsOpen
                      ? Padding(
                          padding: const EdgeInsets.only(left: 14),
                          child: SizedBox(
                            width: 250,
                            child: _ContentsPanel(
                              controller: _controller,
                              onJump: (line) {
                                _controller.jumpToLine(line);
                                _focus.requestFocus();
                                setState(() {});
                              },
                              onClose: () => setState(() => _contentsOpen = false),
                            ),
                          ),
                        )
                      : const SizedBox.shrink(),
                ),
              ],
            ),
          ),
        ),
        _dock(scheme),
      ],
    );
  }

  Widget _page(BuildContext context, bool dark) {
    final scheme = Theme.of(context).colorScheme;
    return Container(
      decoration: BoxDecoration(
        color: dark ? CurioPalette.nightRaised : CurioPalette.creamWhite,
        borderRadius: BorderRadius.circular(CurioSpace.pageRadius),
        boxShadow: dark
            ? null
            : [
                BoxShadow(
                  color: CurioPalette.plum.withValues(alpha: 0.07),
                  blurRadius: 30,
                  offset: const Offset(0, 14),
                ),
              ],
      ),
      clipBehavior: Clip.antiAlias,
      child: Stack(
        children: [
          // ── THE SHEET'S OWN TEXTURE ─────────────────────────────────
          //
          // The phone's pages are `PaperCard`s — a grain and a few tonal
          // clouds under the words (see [PaperBackdrop]). A flat cream fill
          // is the one thing that made this page read as a text box on a
          // panel rather than as a sheet of paper, so the sheet is textured
          // here too, under the text so it never fights the ink.
          const Positioned.fill(
            child: PaperBackdrop(seed: 3, strength: 0.75),
          ),
          Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              _TagsRow(
                key: ValueKey<String>(_base.id),
                tags: _tags,
                onChanged: (tags) {
                  setState(() => _tags = tags);
                  _dirty = true;
                  _saveTimer?.cancel();
                  _saveTimer = Timer(const Duration(milliseconds: 700), _flush);
                },
              ),
              Expanded(
                child: Padding(
                  padding: const EdgeInsets.fromLTRB(38, 8, 38, 24),
                  child: CallbackShortcuts(
                    bindings: _shortcuts(),
                    child: TextField(
                      controller: _controller,
                      focusNode: _focus,
                      onChanged: (_) => _onChanged(),
                      maxLines: null,
                      expands: true,
                      textAlignVertical: TextAlignVertical.top,
                      cursorColor: scheme.primary,
                      cursorWidth: 1.6,
                      style: _controller.styleFor(BlockKind.body),
                      decoration: const InputDecoration(
                        border: InputBorder.none,
                        isCollapsed: true,
                        hintText: 'Start writing…',
                      ),
                    ),
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  /// The phone has no keyboard, so its dock's history door is a sheet. A window
  /// has one, so the same actions are here WHILE TYPING — an editor without
  /// Ctrl+Z is not a text editor.
  Map<ShortcutActivator, VoidCallback> _shortcuts() {
    VoidCallback format({
      bool bold = false,
      bool italic = false,
      bool underline = false,
      bool strike = false,
    }) =>
        () => _act(() => _controller.toggleInline(
              bold: bold,
              italic: italic,
              underline: underline,
              strike: strike,
            ));

    final bindings = <ShortcutActivator, VoidCallback>{};
    for (final meta in <bool>[false, true]) {
      bindings[SingleActivator(LogicalKeyboardKey.keyB, control: !meta, meta: meta)] =
          format(bold: true);
      bindings[SingleActivator(LogicalKeyboardKey.keyI, control: !meta, meta: meta)] =
          format(italic: true);
      bindings[SingleActivator(LogicalKeyboardKey.keyU, control: !meta, meta: meta)] =
          format(underline: true);
      bindings[SingleActivator(LogicalKeyboardKey.keyX, control: !meta, meta: meta, shift: true)] =
          format(strike: true);
      bindings[SingleActivator(LogicalKeyboardKey.keyZ, control: !meta, meta: meta)] =
          () => _act(_controller.undo);
      bindings[SingleActivator(LogicalKeyboardKey.keyZ, control: !meta, meta: meta, shift: true)] =
          () => _act(_controller.redo);
      bindings[SingleActivator(LogicalKeyboardKey.keyY, control: !meta, meta: meta)] =
          () => _act(_controller.redo);
    }
    return bindings;
  }

  // ── The dock ───────────────────────────────────────────────────────────

  Widget _dock(ColorScheme scheme) {
    const padding = EdgeInsets.fromLTRB(
      CurioSpace.page,
      0,
      CurioSpace.page,
      16,
    );
    if (_toolsCollapsed) {
      // The phone's v502 rule: put away, the bar becomes ONE circle where the
      // arrow that put it away was.
      return Padding(
        padding: padding,
        child: Align(
          alignment: Alignment.centerLeft,
          child: _DockTool(
            tooltip: 'Show the writing tools',
            onTap: () => setState(() => _toolsCollapsed = false),
            child: const Icon(Icons.keyboard_arrow_up_rounded, size: 20),
          ),
        ),
      );
    }

    return Padding(
      padding: padding,
      child: Container(
        padding: const EdgeInsets.fromLTRB(6, 6, 8, 6),
        decoration: BoxDecoration(
          color: scheme.surfaceContainerHighest.withValues(alpha: 0.94),
          borderRadius: BorderRadius.circular(24),
          border: Border.all(color: scheme.outlineVariant.withValues(alpha: 0.6)),
          boxShadow: [
            BoxShadow(
              color: CurioPalette.plum.withValues(alpha: 0.08),
              blurRadius: 16,
              offset: const Offset(0, 8),
            ),
          ],
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Row(
              children: [
                _DockTool(
                  tooltip: 'Hide the writing tools',
                  onTap: () => setState(() => _toolsCollapsed = true),
                  child: const Icon(Icons.keyboard_arrow_down_rounded, size: 20),
                ),
                Expanded(
                  child: SingleChildScrollView(
                    scrollDirection: Axis.horizontal,
                    child: Row(children: _rowTools(scheme)),
                  ),
                ),
              ],
            ),
            AnimatedSize(
              duration: const Duration(milliseconds: 170),
              curve: Curves.easeOutCubic,
              child: _group == null
                  ? const SizedBox(width: double.infinity)
                  : _panel(context, scheme),
            ),
          ],
        ),
      ),
    );
  }

  List<Widget> _rowTools(ColorScheme scheme) {
    final c = _controller;
    final marker = c.caretMarker;
    return <Widget>[
      _DockTool(
        tooltip: 'Undo',
        onTap: c.canUndo ? () => _act(c.undo) : null,
        child: const Icon(Icons.undo_rounded, size: 18),
      ),
      _DockTool(
        tooltip: 'Redo',
        onTap: c.canRedo ? () => _act(c.redo) : null,
        child: const Icon(Icons.redo_rounded, size: 18),
      ),
      // ── FORMAT ────────────────────────────────────────────────────────
      _DockTool(
        tooltip: 'Format',
        active: c.caretFormatted || _group == _DockGroup.format,
        onTap: () => _toggleGroup(_DockGroup.format),
        child: const Text(
          'Aa',
          style: TextStyle(fontSize: 14, fontWeight: FontWeight.w700),
        ),
      ),
      // ── MARKER ────────────────────────────────────────────────────────
      // The bullet tool taught this dock a fast path: with no pen down the
      // first press puts the first pen down in ONE tap, and the press after
      // that opens the pens.
      _DockTool(
        tooltip: 'Marker',
        accent: _penColour(c.caretHighlight),
        active: c.caretHighlight != null || _group == _DockGroup.marker,
        onTap: () {
          if (c.caretHighlight != null) {
            _toggleGroup(_DockGroup.marker);
          } else {
            _act(() => c.applyHighlight(kMarkPens.first.key));
          }
        },
        child: Icon(
          Icons.brush_rounded,
          size: 17,
          color: c.caretHighlight == null ? null : _penInk(c.caretHighlight),
        ),
      ),
      // ── HEADING ───────────────────────────────────────────────────────
      _DockTool(
        tooltip: switch (c.caretKind) {
          BlockKind.h1 => 'Heading I',
          BlockKind.h2 => 'Heading II',
          _ => 'Heading or content',
        },
        active: c.caretKind == BlockKind.h1 ||
            c.caretKind == BlockKind.h2 ||
            _group == _DockGroup.heading,
        onTap: () => _toggleGroup(_DockGroup.heading),
        child: _HeadingBadge(
          level: switch (c.caretKind) {
            BlockKind.h1 => 1,
            BlockKind.h2 => 2,
            _ => 0,
          },
        ),
      ),
      // ── TODO ──────────────────────────────────────────────────────────
      _DockTool(
        tooltip: 'Todo checkbox',
        active: c.caretTodo,
        onTap: () => _act(c.toggleTodo),
        child: const Icon(Icons.check_box_outlined, size: 18),
      ),
      _DockTool(
        tooltip: 'Small text',
        active: c.caretKind == BlockKind.small,
        onTap: () => _act(() => c.toggleKind(BlockKind.small)),
        child: const Icon(Icons.text_decrease_rounded, size: 18),
      ),
      // ── BULLET ────────────────────────────────────────────────────────
      _DockTool(
        tooltip: 'Bullet style',
        active: marker != null || _group == _DockGroup.bullet,
        onTap: () {
          if (marker != null) {
            _toggleGroup(_DockGroup.bullet);
          } else {
            _act(() => c.applyMarker(MarkerKind.dot));
          }
        },
        child: _MarkerGlyph(kind: marker),
      ),
      _DockTool(
        tooltip: 'Quote',
        active: c.caretKind == BlockKind.quote,
        onTap: () => _act(() => c.toggleKind(BlockKind.quote)),
        child: const Icon(Icons.format_quote_rounded, size: 18),
      ),
      _Hairline(colour: scheme.outlineVariant),
      _DockTool(
        tooltip: 'Contents',
        active: _contentsOpen,
        onTap: () => setState(() => _contentsOpen = !_contentsOpen),
        child: const Icon(Icons.toc_rounded, size: 18),
      ),
      _DockTool(
        tooltip: 'Export this page',
        active: _group == _DockGroup.export,
        onTap: () => _toggleGroup(_DockGroup.export),
        child: const Icon(Icons.ios_share_rounded, size: 17),
      ),
    ];
  }

  Widget _panel(BuildContext context, ColorScheme scheme) {
    final c = _controller;
    return Column(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
          child: Container(height: 1, color: scheme.outlineVariant.withValues(alpha: 0.7)),
        ),
        Padding(
          padding: const EdgeInsets.fromLTRB(2, 6, 2, 2),
          child: SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            child: Row(children: _panelTools(context, c)),
          ),
        ),
      ],
    );
  }

  List<Widget> _panelTools(BuildContext context, JournalController c) {
    switch (_group) {
      case _DockGroup.format:
        return <Widget>[
          _DockTool(
            tooltip: 'Bold',
            active: c.caretBold,
            onTap: () => _act(() => c.toggleInline(bold: true)),
            child: const Icon(Icons.format_bold_rounded, size: 19),
          ),
          _DockTool(
            tooltip: 'Italic',
            active: c.caretItalic,
            onTap: () => _act(() => c.toggleInline(italic: true)),
            child: const Icon(Icons.format_italic_rounded, size: 19),
          ),
          _DockTool(
            tooltip: 'Underline',
            active: c.caretUnderline,
            onTap: () => _act(() => c.toggleInline(underline: true)),
            child: const Icon(Icons.format_underlined_rounded, size: 19),
          ),
          _DockTool(
            tooltip: 'Strikethrough',
            active: c.caretStrike,
            onTap: () => _act(() => c.toggleInline(strike: true)),
            child: const Icon(Icons.format_strikethrough_rounded, size: 19),
          ),
        ];
      case _DockGroup.marker:
        return <Widget>[
          // The phone's rule (v444): taking the pen off is the way out, so this
          // one control both clears the marker and closes the panel.
          _DockTool(
            tooltip: 'No marker',
            active: c.caretHighlight == null,
            onTap: () => _act(() {
              c.applyHighlight(null);
              setState(() => _group = null);
            }),
            child: const Icon(Icons.close_rounded, size: 18),
          ),
          for (final pen in kMarkPens)
            _DockTool(
              tooltip: 'Marker: ${pen.label}',
              accent: Color(pen.color),
              active: c.caretHighlight == pen.key,
              onTap: () => _act(() => c.applyHighlight(pen.key)),
              child: Container(
                width: 16,
                height: 16,
                decoration: BoxDecoration(
                  color: Color(pen.color),
                  shape: BoxShape.circle,
                ),
              ),
            ),
        ];
      case _DockGroup.heading:
        return <Widget>[
          for (final level in <int>[1, 2, 0])
            _DockTool(
              tooltip: switch (level) {
                1 => 'Heading I',
                2 => 'Heading II',
                _ => 'Content',
              },
              active: (level == 0
                      ? c.caretKind == BlockKind.body
                      : level == 1
                          ? c.caretKind == BlockKind.h1
                          : c.caretKind == BlockKind.h2),
              onTap: () => _act(() => c.setKind(switch (level) {
                    1 => BlockKind.h1,
                    2 => BlockKind.h2,
                    _ => BlockKind.body,
                  })),
              child: _HeadingBadge(level: level),
            ),
        ];
      case _DockGroup.bullet:
        return <Widget>[
          _DockTool(
            tooltip: 'Remove list',
            active: c.caretMarker == null,
            onTap: () => _act(() {
              c.removeMarker();
              setState(() => _group = null);
            }),
            child: const Icon(Icons.close_rounded, size: 18),
          ),
          for (final kind in MarkerKind.values)
            _DockTool(
              tooltip: switch (kind) {
                MarkerKind.dot => 'Dot',
                MarkerKind.dash => 'Dash',
                MarkerKind.numbered => 'Numbered',
              },
              active: c.caretMarker == kind,
              onTap: () => _act(() => c.applyMarker(kind)),
              child: _MarkerGlyph(kind: kind, plain: true),
            ),
        ];
      case _DockGroup.export:
        return <Widget>[
          _ExportChip(
            label: 'Markdown',
            onTap: () => _export(markdown: true),
          ),
          _ExportChip(
            label: 'Plain text',
            onTap: () => _export(markdown: false),
          ),
        ];
      case null:
        return const <Widget>[];
    }
  }

  Color _penColour(String? key) => key == null
      ? CurioPalette.rose
      : Color(markPen(key)?.color ?? kMarkPens.first.color);

  Color _penInk(String? key) => key == null ? CurioPalette.rose : Colors.white;

  /// Writes the page out beside its own data folder and says where — the honest
  /// desktop version of the phone's export sheet (there is no system file
  /// picker in a dependency-free build, and a file the member cannot find is
  /// not an export).
  Future<void> _export({required bool markdown}) async {
    final title = _title.text.trim().isEmpty ? 'Untitled page' : _title.text.trim();
    final slug = title
        .toLowerCase()
        .replaceAll(RegExp(r'[^a-z0-9]+'), '-')
        .replaceAll(RegExp(r'^-+|-+$'), '');
    final stamp = DateTime.now()
        .toIso8601String()
        .substring(0, 19)
        .replaceAll(RegExp(r'[^0-9]'), '');
    final folder = Directory(
      '${widget.model.dataPath}${Platform.pathSeparator}exports',
    );
    final file = File(
      '${folder.path}${Platform.pathSeparator}'
      '${slug.isEmpty ? 'page' : slug}-$stamp.${markdown ? 'md' : 'txt'}',
    );
    try {
      await folder.create(recursive: true);
      await file.writeAsString(_controller.export(title: title, markdown: markdown));
      if (!mounted) return;
      setState(() => _group = null);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          behavior: SnackBarBehavior.floating,
          content: Text('Exported to ${file.path}'),
        ),
      );
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          behavior: SnackBarBehavior.floating,
          content: Text('Could not export this page: $error'),
        ),
      );
    }
  }

  Future<void> _confirmDelete(BuildContext context) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Delete this page?'),
        content: const Text('The writing is removed from this device. This cannot be undone.'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('Keep it'),
          ),
          FilledButton(
            onPressed: () => Navigator.of(context).pop(true),
            child: const Text('Delete'),
          ),
        ],
      ),
    );
    if (confirmed ?? false) {
      _saveTimer?.cancel();
      _dirty = false;
      await widget.model.deleteJournal(_base.id);
    }
  }
}

// ═══════════════════════════════════════════════════════════════════════
// The page head
// ═══════════════════════════════════════════════════════════════════════

/// The page head: back, the title (typed here, not on the paper), the moods,
/// the word count and the save state.
class _Header extends StatelessWidget {
  const _Header({
    required this.model,
    required this.title,
    required this.titleFocus,
    required this.mood,
    required this.onMood,
    required this.words,
    required this.saved,
    required this.onChanged,
    required this.onDelete,
  });

  final AppModel model;
  final TextEditingController title;
  final FocusNode titleFocus;
  final int mood;
  final ValueChanged<int> onMood;
  final int words;
  final bool saved;
  final VoidCallback onChanged;
  final VoidCallback onDelete;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Padding(
      padding: const EdgeInsets.fromLTRB(CurioSpace.page, 20, CurioSpace.page, 16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              TextButton.icon(
                onPressed: model.closeJournal,
                icon: const Icon(Icons.arrow_back_rounded, size: 18),
                label: const Text('Journals'),
              ),
              const Spacer(),
              IconButton(
                tooltip: 'Delete this page',
                onPressed: onDelete,
                icon: const Icon(Icons.delete_outline_rounded, size: 18),
              ),
              const SizedBox(width: 4),
              Text(
                saved ? 'Saved' : 'Saving…',
                style: TextStyle(
                  fontSize: 11.5,
                  color: saved ? scheme.onSurfaceVariant : scheme.primary,
                ),
              ),
            ],
          ),
          const SizedBox(height: 2),
          TextField(
            controller: title,
            focusNode: titleFocus,
            onChanged: (_) => onChanged(),
            style: const TextStyle(
              fontSize: 30,
              fontWeight: FontWeight.w800,
              letterSpacing: -0.7,
            ),
            decoration: const InputDecoration(
              border: InputBorder.none,
              hintText: 'Untitled page',
              isCollapsed: true,
            ),
          ),
          const SizedBox(height: 10),
          Row(
            children: [
              for (var index = 0; index < JournalEntry.moodLabels.length; index++)
                Padding(
                  padding: const EdgeInsets.only(right: 6),
                  child: _MoodChip(
                    glyph: JournalEntry.moodGlyphs[index],
                    label: JournalEntry.moodLabels[index],
                    selected: mood == index,
                    onTap: () => onMood(index),
                  ),
                ),
              const Spacer(),
              Text(
                '$words ${words == 1 ? 'word' : 'words'}',
                style: TextStyle(fontSize: 11.5, color: scheme.onSurfaceVariant),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _MoodChip extends StatelessWidget {
  const _MoodChip({
    required this.glyph,
    required this.label,
    required this.selected,
    required this.onTap,
  });

  final String glyph;
  final String label;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Tooltip(
      message: label,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(999),
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 150),
          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
          decoration: BoxDecoration(
            color: selected
                ? scheme.primary.withValues(alpha: 0.14)
                : scheme.surfaceContainerHighest.withValues(alpha: 0.5),
            borderRadius: BorderRadius.circular(999),
          ),
          child: Text(
            glyph,
            style: TextStyle(
              fontSize: 13,
              color: selected ? scheme.primary : scheme.onSurfaceVariant,
              fontWeight: selected ? FontWeight.w700 : FontWeight.w400,
            ),
          ),
        ),
      ),
    );
  }
}

/// Tags as one comma-separated line — the phone keeps tags on a page, and a
/// desktop page has no room for a chip editor, so this is the honest small
/// version of the same field.
class _TagsRow extends StatefulWidget {
  const _TagsRow({super.key, required this.tags, required this.onChanged});

  final List<String> tags;
  final ValueChanged<List<String>> onChanged;

  @override
  State<_TagsRow> createState() => _TagsRowState();
}

class _TagsRowState extends State<_TagsRow> {
  late final TextEditingController _tags =
      TextEditingController(text: widget.tags.join(', '));

  @override
  void dispose() {
    _tags.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Padding(
      padding: const EdgeInsets.fromLTRB(28, 16, 28, 0),
      child: TextField(
        controller: _tags,
        style: TextStyle(fontSize: 11.5, color: scheme.onSurfaceVariant),
        decoration: InputDecoration(
          border: InputBorder.none,
          isCollapsed: true,
          hintText: 'tags, separated by commas',
          hintStyle: TextStyle(
            fontSize: 11.5,
            color: scheme.onSurfaceVariant.withValues(alpha: 0.7),
          ),
        ),
        onChanged: (value) => widget.onChanged(
          value
              .split(',')
              .map((tag) => tag.trim())
              .where((tag) => tag.isNotEmpty)
              .toList(growable: false),
        ),
      ),
    );
  }
}

// ═══════════════════════════════════════════════════════════════════════
// The dock's parts
// ═══════════════════════════════════════════════════════════════════════

/// One tool. The phone's v433 rule: a LIT tool is a solid disc of its accent
/// with the glyph knocked out, so a colour tool (the marker) can never draw in
/// the colour of the disc it sits on.
class _DockTool extends StatelessWidget {
  const _DockTool({
    required this.tooltip,
    this.onTap,
    this.active = false,
    this.accent,
    required this.child,
  });

  final String tooltip;

  /// Null disables the tool (undo with nothing to undo).
  final VoidCallback? onTap;
  final bool active;
  final Color? accent;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final fill = accent ?? scheme.primary;
    final enabled = onTap != null;
    final content = active
        ? inkOn(fill)
        : (enabled ? scheme.onSurfaceVariant : scheme.onSurfaceVariant.withValues(alpha: 0.35));
    return Tooltip(
      message: tooltip,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(12),
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 150),
          curve: Curves.easeOutCubic,
          width: 36,
          height: 36,
          margin: const EdgeInsets.symmetric(horizontal: 1),
          decoration: BoxDecoration(
            color: active ? fill : Colors.transparent,
            borderRadius: BorderRadius.circular(12),
          ),
          alignment: Alignment.center,
          child: IconTheme(
            data: IconThemeData(color: content),
            child: DefaultTextStyle(
              style: TextStyle(
                color: content,
                fontSize: 13,
                fontWeight: FontWeight.w700,
              ),
              child: child,
            ),
          ),
        ),
      ),
    );
  }
}

class _ExportChip extends StatelessWidget {
  const _ExportChip({required this.label, required this.onTap});

  final String label;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 3),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(999),
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
          decoration: BoxDecoration(
            color: scheme.surface,
            borderRadius: BorderRadius.circular(999),
            border: Border.all(color: scheme.outlineVariant.withValues(alpha: 0.8)),
          ),
          child: Text(
            label,
            style: TextStyle(
              fontSize: 12,
              fontWeight: FontWeight.w600,
              color: scheme.onSurface,
            ),
          ),
        ),
      ),
    );
  }
}

class _Hairline extends StatelessWidget {
  const _Hairline({required this.colour});

  final Color colour;

  @override
  Widget build(BuildContext context) => Container(
        width: 1,
        height: 20,
        margin: const EdgeInsets.symmetric(horizontal: 5),
        color: colour.withValues(alpha: 0.7),
      );
}

/// The heading door's face — the level the caret's line is at, in the same
/// circular badge the phone's dock wears (`i` for ordinary content).
class _HeadingBadge extends StatelessWidget {
  const _HeadingBadge({required this.level});

  final int level;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 20,
      height: 20,
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        border: Border.all(
          // The badge draws itself in the tool's own content colour, so it
          // reads on a lit disc and on plain paper alike.
          color: DefaultTextStyle.of(context).style.color ?? Colors.white,
          width: 1.4,
        ),
      ),
      alignment: Alignment.center,
      child: Text(
        switch (level) {
          1 => 'I',
          2 => 'II',
          _ => 'i',
        },
        style: TextStyle(
          fontSize: level == 2 ? 8.5 : 10.5,
          height: 1,
          fontWeight: FontWeight.w800,
          color: DefaultTextStyle.of(context).style.color,
        ),
      ),
    );
  }
}

/// The bullet door's face: the marker the line wears, or a plain list glyph when
/// it is not a list at all.
class _MarkerGlyph extends StatelessWidget {
  const _MarkerGlyph({required this.kind, this.plain = false});

  final MarkerKind? kind;

  /// In the panel the glyph is the CHOICE, so it always shows its own shape.
  final bool plain;

  @override
  Widget build(BuildContext context) {
    final colour = DefaultTextStyle.of(context).style.color;
    if (kind == null && !plain) {
      return Icon(Icons.format_list_bulleted_rounded, size: 18, color: colour);
    }
    return Text(
      switch (kind ?? MarkerKind.dot) {
        MarkerKind.dot => '•',
        MarkerKind.dash => '–',
        MarkerKind.numbered => '1.',
      },
      style: TextStyle(
        color: colour,
        fontSize: kind == MarkerKind.numbered || kind == null ? 13 : 17,
        height: 1,
        fontWeight: FontWeight.w700,
      ),
    );
  }
}

/// The page's headings, numbered I, II, I.1 …, as a jump list.
class _ContentsPanel extends StatelessWidget {
  const _ContentsPanel({
    required this.controller,
    required this.onJump,
    required this.onClose,
  });

  final JournalController controller;
  final ValueChanged<int> onJump;
  final VoidCallback onClose;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final headings = controller.headings;
    return Container(
      padding: const EdgeInsets.fromLTRB(16, 12, 12, 12),
      decoration: BoxDecoration(
        color: scheme.surfaceContainerHighest.withValues(alpha: 0.45),
        borderRadius: BorderRadius.circular(CurioSpace.cardRadius),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(
                  'CONTENTS',
                  style: TextStyle(
                    fontSize: 10.5,
                    fontWeight: FontWeight.w800,
                    letterSpacing: 1.1,
                    color: scheme.onSurfaceVariant,
                  ),
                ),
              ),
              IconButton(
                tooltip: 'Put the contents away',
                visualDensity: VisualDensity.compact,
                onPressed: onClose,
                icon: const Icon(Icons.close_rounded, size: 15),
              ),
            ],
          ),
          const SizedBox(height: 6),
          if (headings.isEmpty)
            Text(
              'Give a line Heading I or Heading II and it appears here.',
              style: TextStyle(
                fontSize: 12,
                height: 1.5,
                color: scheme.onSurfaceVariant,
              ),
            )
          else
            Expanded(
              child: ListView.builder(
                padding: EdgeInsets.zero,
                itemCount: headings.length,
                itemBuilder: (context, index) {
                  final heading = headings[index];
                  return InkWell(
                    onTap: () => onJump(heading.line),
                    borderRadius: BorderRadius.circular(8),
                    child: Padding(
                      padding: EdgeInsets.fromLTRB(
                        heading.level == 2 ? 14 : 2,
                        5,
                        2,
                        5,
                      ),
                      child: Row(
                        children: [
                          Text(
                            heading.number,
                            style: TextStyle(
                              fontSize: 10.5,
                              fontWeight: FontWeight.w700,
                              color: scheme.primary,
                            ),
                          ),
                          const SizedBox(width: 8),
                          Expanded(
                            child: Text(
                              heading.label,
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                              style: TextStyle(
                                fontSize: 12.5,
                                fontWeight: heading.level == 1
                                    ? FontWeight.w700
                                    : FontWeight.w500,
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
                  );
                },
              ),
            ),
        ],
      ),
    );
  }
}
