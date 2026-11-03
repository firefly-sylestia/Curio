import 'dart:math';

import 'package:flutter/material.dart';

import '../data/journal.dart';

/// The colours a page renders in. The controller cannot read the theme (it is
/// not a widget), so the editor hands it these once per build.
class JournalPalette {
  const JournalPalette({
    this.ink = const Color(0xFF241C1B),
    this.muted = const Color(0xFF7A6A66),
    this.accent = const Color(0xFFDC7482),
    this.paper = const Color(0xFFFFFBF5),
    this.dark = false,
  });

  final Color ink;
  final Color muted;
  final Color accent;
  final Color paper;
  final bool dark;
}

/// One point in a page's history: everything an undo has to put back.
class JournalSnapshot {
  const JournalSnapshot({
    required this.text,
    required this.blocks,
    required this.marks,
    required this.highlights,
    required this.selection,
  });

  final String text;
  final List<JournalBlock> blocks;
  final List<InlineMark> marks;
  final List<TextHighlight> highlights;
  final TextSelection selection;
}

/// The journal page's text controller: one TextField, but each LINE has its own
/// block style and each inline stretch its own bold/italic/underline/strike or
/// marker pen — the phone's editor model, at the size a desktop page needs.
///
/// Why one field instead of a field per block: a desktop page is typed in, not
/// tapped, so the caret has to walk from the last word of one line to the first
/// of the next with the arrow keys, and one field is the only way Backspace at a
/// line start behaves like a text editor rather than like a stack of boxes.
///
/// The cost of one field is that a marker cannot be drawn in a gutter: a `•`
/// painted outside the text would sit at an offset the caret does not know. So
/// markers are REAL characters at the line start (`applyMarker`), which keeps
/// every offset honest — the same trick a plain-text editor uses.
///
/// **The one thing the phone's editor has that this cannot wear is per-line
/// ALIGNMENT.** Compose lays out one paragraph per block, so a block can be
/// centred on its own; a `TextField` publishes a single `textAlign` for the
/// whole page, and a line-align button here would either do nothing or re-align
/// every line at once. It is therefore not in the dock at all — the repo's rule
/// is that a row of buttons that do nothing is worse than no row (see the
/// Android dock's own note on captions). Everything else the dock offers — the
/// heading door, format, marker pens, todo, small text, the bullet door, quote,
/// history, contents and export — is here and acts on the caret's line or the
/// selection, exactly as it does on the phone.
class JournalController extends TextEditingController {
  JournalController(JournalEntry entry)
      : _blocks = List<JournalBlock>.from(entry.blocks),
        _marks = List<InlineMark>.from(entry.marks),
        _highlights = List<TextHighlight>.from(entry.highlights),
        super(text: entry.text) {
    _syncBlockCount();
    _shakeBaseline = DateTime.now();
  }

  List<JournalBlock> _blocks;
  List<InlineMark> _marks;
  List<TextHighlight> _highlights;
  JournalPalette palette = const JournalPalette();

  /// Called after THIS controller changed the text itself (a tool, an undo, a
  /// marker). A keystroke arrives through the field's `onChanged`; anything the
  /// controller writes would otherwise leave the editor's idea of "the text as
  /// it was" stale, and the next keystroke would shift every mark by the wrong
  /// amount. The editor sets this and re-syncs.
  VoidCallback? onMutated;

  // ── History ────────────────────────────────────────────────────────────

  final List<JournalSnapshot> _undo = <JournalSnapshot>[];
  final List<JournalSnapshot> _redo = <JournalSnapshot>[];
  DateTime _shakeBaseline = DateTime.now();

  static const int _historyLimit = 80;

  /// A typing BURST is one undo step: a snapshot is only taken when the last
  /// one is more than this old, so a sentence costs one entry rather than one
  /// per character.
  static const Duration _burst = Duration(milliseconds: 900);

  bool get canUndo => _undo.isNotEmpty;

  bool get canRedo => _redo.isNotEmpty;

  /// The page as it is now, ready to be pushed.
  JournalSnapshot _snapshot({String? text, TextSelection? selection}) =>
      JournalSnapshot(
        text: text ?? this.text,
        blocks: List<JournalBlock>.from(_blocks),
        marks: List<InlineMark>.from(_marks),
        highlights: List<TextHighlight>.from(_highlights),
        selection: selection ?? this.selection,
      );

  void _push(JournalSnapshot snapshot) {
    _undo.add(snapshot);
    if (_undo.length > _historyLimit) _undo.removeAt(0);
    _redo.clear();
  }

  void _restore(JournalSnapshot snapshot) {
    _blocks = List<JournalBlock>.from(snapshot.blocks);
    _marks = List<InlineMark>.from(snapshot.marks);
    _highlights = List<TextHighlight>.from(snapshot.highlights);
    value = TextEditingValue(
      text: snapshot.text,
      selection: snapshot.selection.copyWith(
        baseOffset:
            snapshot.selection.baseOffset.clamp(0, snapshot.text.length).toInt(),
        extentOffset:
            snapshot.selection.extentOffset.clamp(0, snapshot.text.length).toInt(),
      ),
    );
    // The snapshot's own marks and pens are already positioned for its text, so
    // nothing is shifted here — `applyEdit` would move them a second time.
    _syncBlockCount();
    _shakeBaseline = DateTime.now();
    notifyListeners();
    onMutated?.call();
  }

  void undo() {
    if (_undo.isEmpty) return;
    final snapshot = _undo.removeLast();
    _redo.add(_snapshot());
    _restore(snapshot);
  }

  void redo() {
    if (_redo.isEmpty) return;
    final snapshot = _redo.removeLast();
    _undo.add(_snapshot());
    _restore(snapshot);
  }

  List<JournalBlock> get blocks => List<JournalBlock>.unmodifiable(_blocks);

  List<JournalHeading> get headings {
    final lines = text.split('\n');
    final out = <JournalHeading>[];
    var chapter = 0;
    var section = 0;
    for (var index = 0; index < lines.length; index++) {
      final label = lines[index].trim();
      if (label.isEmpty) continue;
      final kind = blockAt(index).kind;
      if (kind == BlockKind.h1) {
        chapter++;
        section = 0;
        out.add(JournalHeading(
          line: index,
          label: label,
          number: _roman(max(1, chapter)),
          level: 1,
        ));
      } else if (kind == BlockKind.h2) {
        section++;
        out.add(JournalHeading(
          line: index,
          label: label,
          number: '${_roman(max(1, chapter))}.$section',
          level: 2,
        ));
      }
    }
    return out;
  }

  JournalBlock blockAt(int line) {
    if (_blocks.isEmpty) return const JournalBlock();
    if (line >= 0 && line < _blocks.length) return _blocks[line];
    return _blocks.last;
  }

  int get caretLine {
    final offset = selection.baseOffset.clamp(0, text.length).toInt();
    return _lineOf(offset);
  }

  /// The kind of the line the caret is on — what the heading door's badge wears.
  BlockKind get caretKind => blockAt(caretLine).kind;

  /// The marker the caret's line wears, if it is a list.
  MarkerKind? get caretMarker {
    final line = _lines()[caretLine];
    if (line.startsWith('• ')) return MarkerKind.dot;
    if (line.startsWith('– ')) return MarkerKind.dash;
    if (_numberedPattern.hasMatch(line)) return MarkerKind.numbered;
    return null;
  }

  bool get caretBold => _rangeHas(_activeRange(), bold: true);

  bool get caretItalic => _rangeHas(_activeRange(), italic: true);

  bool get caretUnderline => _rangeHas(_activeRange(), underline: true);

  bool get caretStrike => _rangeHas(_activeRange(), strike: true);

  /// The pen the caret's run is under, or null when it is plain paper.
  String? get caretHighlight {
    final range = _activeRange();
    if (range.end <= range.start) return null;
    String? pen;
    for (final highlight in _highlights) {
      final overlap = min(highlight.end, range.end) - max(highlight.start, range.start);
      if (overlap <= 0) continue;
      if (pen != null && pen != highlight.pen) return null;
      pen = highlight.pen;
    }
    return pen;
  }

  bool get caretTodo {
    final line = _lines()[caretLine];
    return line.startsWith('☐ ') || line.startsWith('☑ ');
  }

  /// True when the caret's line carries any part of the FORMAT door's subject,
  /// which is what lights its own tile.
  bool get caretFormatted =>
      caretBold || caretItalic || caretUnderline || caretStrike;

  // ── Editing ────────────────────────────────────────────────────────────

  /// Called from `onChanged` with the text BEFORE the keystroke. Marks, pens and
  /// the block list are kept in step with the edit, so a page typed into for an
  /// hour still has its headings and its bold words where they belong.
  ///
  /// [record] is false only when the controller itself is restoring a snapshot
  /// (an undo must not push another undo).
  void applyEdit(String oldText, {bool record = true}) {
    final newText = text;
    if (oldText == newText) {
      _syncBlockCount();
      return;
    }
    // Typing is batched into one undo step; the snapshot taken here is the page
    // AS IT WAS, because the marks below are still at their pre-edit offsets.
    if (record && DateTime.now().difference(_shakeBaseline) > _burst) {
      _push(_snapshot(text: oldText));
      _shakeBaseline = DateTime.now();
    } else if (record) {
      _shakeBaseline = DateTime.now();
    }
    if (oldText.isEmpty) {
      _syncBlockCount();
      return;
    }
    final minLength = min(oldText.length, newText.length);
    var prefix = 0;
    while (prefix < minLength && oldText[prefix] == newText[prefix]) {
      prefix++;
    }
    var suffix = 0;
    while (suffix < minLength - prefix &&
        oldText[oldText.length - 1 - suffix] == newText[newText.length - 1 - suffix]) {
      suffix++;
    }
    final removedStart = prefix;
    final removedEnd = oldText.length - suffix;
    final delta = (newText.length - suffix) - removedStart;

    _marks = _shift(_marks, removedStart, removedEnd, delta);
    _highlights = _shiftHighlights(_highlights, removedStart, removedEnd, delta);
    _syncBlockCount();
  }

  /// Moves every mark around an edit, keeping what is left of a mark the edit
  /// ran through rather than dropping the whole word.
  List<InlineMark> _shift(
    List<InlineMark> marks,
    int removedStart,
    int removedEnd,
    int delta,
  ) {
    final next = <InlineMark>[];
    for (final mark in marks) {
      if (mark.end <= removedStart) {
        next.add(mark);
      } else if (mark.start >= removedEnd) {
        next.add(mark.copyWith(start: mark.start + delta, end: mark.end + delta));
      } else {
        if (mark.start < removedStart) {
          next.add(mark.copyWith(end: removedStart));
        }
        if (mark.end > removedEnd) {
          next.add(mark.copyWith(start: removedStart + delta, end: mark.end + delta));
        }
      }
    }
    return next.where((mark) => !mark.isEmpty && !mark.isPlain).toList(growable: true);
  }

  List<TextHighlight> _shiftHighlights(
    List<TextHighlight> highlights,
    int removedStart,
    int removedEnd,
    int delta,
  ) {
    final next = <TextHighlight>[];
    for (final highlight in highlights) {
      if (highlight.end <= removedStart) {
        next.add(highlight);
      } else if (highlight.start >= removedEnd) {
        next.add(highlight.copyWith(
          start: highlight.start + delta,
          end: highlight.end + delta,
        ));
      } else {
        if (highlight.start < removedStart) {
          next.add(highlight.copyWith(end: removedStart));
        }
        if (highlight.end > removedEnd) {
          next.add(highlight.copyWith(
            start: removedStart + delta,
            end: highlight.end + delta,
          ));
        }
      }
    }
    return next.where((highlight) => !highlight.isEmpty).toList(growable: true);
  }

  /// The heading door, the todo door, the quote and small text: set the kind of
  /// every selected line, or take it off again when they all wear it already.
  void toggleKind(BlockKind kind) {
    final lines = _selectedLines();
    final already = lines.every((line) => blockAt(line).kind == kind);
    _push(_snapshot());
    for (final line in lines) {
      _ensureBlocks(line);
      _blocks[line] = _blocks[line].copyWith(
        kind: already ? BlockKind.body : kind,
      );
    }
    notifyListeners();
  }

  /// Set one line's kind outright (the heading door's panel does not toggle —
  /// it says which of the three the line should be).
  void setKind(BlockKind kind) {
    final lines = _selectedLines();
    _push(_snapshot());
    for (final line in lines) {
      _ensureBlocks(line);
      _blocks[line] = _blocks[line].copyWith(kind: kind);
    }
    notifyListeners();
  }

  /// The format door: bold, italic, underline or strike over the selection — or
  /// over the caret's line when nothing is selected, which is what a toolbar
  /// press means when the caret is idle.
  void toggleInline({
    bool bold = false,
    bool italic = false,
    bool underline = false,
    bool strike = false,
  }) {
    final range = _activeRange();
    if (range.end <= range.start) return;
    final wanted = !_rangeHas(
      range,
      bold: bold,
      italic: italic,
      underline: underline,
      strike: strike,
    );
    _push(_snapshot());

    bool wants(InlineMark mark) =>
        (bold && mark.bold) ||
        (italic && mark.italic) ||
        (underline && mark.underline) ||
        (strike && mark.strike);

    final next = <InlineMark>[];
    for (final mark in _marks) {
      if (!wants(mark)) {
        next.add(mark);
        continue;
      }
      final overlapStart = max(mark.start, range.start);
      final overlapEnd = min(mark.end, range.end);
      if (overlapEnd <= overlapStart) {
        next.add(mark);
        continue;
      }
      final keepBefore = mark.start < overlapStart;
      final keepAfter = mark.end > overlapEnd;
      if (keepBefore) next.add(mark.copyWith(end: overlapStart));
      if (keepAfter) next.add(mark.copyWith(start: overlapEnd));
      // A mark that carries other flags keeps them on the stretch this press
      // covers — otherwise bolding part of an italic word would silently
      // un-italicise it, and taking the bold off would take the italic too.
      final crossing = mark.copyWith(
        start: overlapStart,
        end: overlapEnd,
        bold: bold ? false : mark.bold,
        italic: italic ? false : mark.italic,
        underline: underline ? false : mark.underline,
        strike: strike ? false : mark.strike,
      );
      if (!crossing.isPlain) next.add(crossing);
    }
    if (wanted) {
      next.add(InlineMark(
        start: range.start,
        end: range.end,
        bold: bold,
        italic: italic,
        underline: underline,
        strike: strike,
      ));
    }
    _marks = _normalize(next);
    notifyListeners();
  }

  /// The marker door: draw a pen over the selection (or take it off, or take it
  /// off the whole line when the caret is idle). One pen per stretch — a second
  /// colour replaces the first, the way a highlighter would.
  void applyHighlight(String? pen) {
    final range = _activeRange();
    _push(_snapshot());
    if (range.end <= range.start) {
      notifyListeners();
      return;
    }
    final next = <TextHighlight>[];
    for (final highlight in _highlights) {
      final overlapStart = max(highlight.start, range.start);
      final overlapEnd = min(highlight.end, range.end);
      if (overlapEnd <= overlapStart) {
        next.add(highlight);
        continue;
      }
      if (highlight.start < overlapStart) {
        next.add(highlight.copyWith(end: overlapStart));
      }
      if (highlight.end > overlapEnd) {
        next.add(highlight.copyWith(start: overlapEnd));
      }
    }
    if (pen != null) {
      next.add(TextHighlight(start: range.start, end: range.end, pen: pen));
    }
    _highlights = _normalizeHighlights(next);
    notifyListeners();
  }

  /// The bullet door: dot, dash or numbered. Pressing the marker a line already
  /// wears takes it off; any other marker replaces it. A numbered run below the
  /// press is renumbered, so a list never reads 1, 2, 2.
  void applyMarker(MarkerKind marker) {
    final lines = _lines();
    final index = caretLine;
    final line = lines[index];
    final prefix = _markerPrefix(line);
    final wanted = _prefixFor(marker, index);
    String replacement;
    if (prefix == wanted) {
      replacement = line.substring(prefix.length);
    } else {
      final body = line.substring(prefix.length);
      replacement = '$wanted$body';
    }
    final startOffset = _lineStart(index);
    final caret = (selection.baseOffset - startOffset - prefix.length)
        .clamp(0, max(0, replacement.length - wanted.length))
        .toInt();
    lines[index] = replacement;

    // Renumber the contiguous numbered run that starts at this line.
    if (replacement.startsWith(_numberedPattern)) {
      var number = _numberOf(replacement);
      for (var next = index + 1;
          next < lines.length && lines[next].startsWith(_numberedPattern);
          next++) {
        number++;
        lines[next] = lines[next].replaceFirstMapped(
          _numberedPattern,
          (match) => '${number.toString().padLeft(match.group(0)!.length)}. ',
        );
      }
    }

    _write(lines, startOffset + wanted.length + caret);
  }

  /// Takes the marker (and a todo box) off the caret's line or every selected
  /// line — the bullet panel's first cross, as on the phone.
  void removeMarker() {
    final lines = _lines();
    _push(_snapshot());
    for (final index in _selectedLines()) {
      lines[index] = lines[index].substring(_markerPrefix(lines[index]).length);
    }
    _write(lines, _lineStart(caretLine), record: false);
  }

  /// The todo door: the focused line gets `☐`, and pressing it again ticks it to
  /// `☑`. A line that is neither gets a box.
  void toggleTodo() {
    final lines = _lines();
    final index = caretLine;
    final line = lines[index];
    final String replacement;
    if (line.startsWith('☐ ')) {
      replacement = '☑ ${line.substring(2)}';
    } else if (line.startsWith('☑ ')) {
      replacement = '☐ ${line.substring(2)}';
    } else {
      replacement = '☐ $line';
    }
    lines[index] = replacement;
    _write(lines, _lineStart(index) + 2);
  }

  /// Puts the caret at the start of [line] — the contents door's jump.
  void jumpToLine(int line) {
    final offset = _lineStart(line.clamp(0, text.split('\n').length - 1).toInt());
    selection = TextSelection.collapsed(offset: offset);
    notifyListeners();
  }

  // ── Rendering ──────────────────────────────────────────────────────────

  TextStyle styleFor(BlockKind kind) {
    final base = TextStyle(
      color: palette.ink,
      fontSize: 15.5,
      height: 1.62,
      letterSpacing: 0.1,
    );
    return switch (kind) {
      BlockKind.h1 => base.copyWith(
          fontSize: 25,
          height: 1.28,
          fontWeight: FontWeight.w800,
          letterSpacing: -0.5,
        ),
      BlockKind.h2 => base.copyWith(
          fontSize: 19,
          height: 1.35,
          fontWeight: FontWeight.w700,
          letterSpacing: -0.2,
        ),
      BlockKind.quote => base.copyWith(
          color: palette.accent,
          fontStyle: FontStyle.italic,
          height: 1.55,
        ),
      BlockKind.small => base.copyWith(
          fontSize: 12.5,
          height: 1.6,
          color: palette.muted,
        ),
      BlockKind.body => base,
    };
  }

  @override
  TextSpan buildTextSpan({
    required BuildContext context,
    TextStyle? style,
    required bool withComposing,
  }) {
    _syncBlockCount();
    final lines = text.split('\n');
    final children = <InlineSpan>[];
    var offset = 0;
    for (var index = 0; index < lines.length; index++) {
      final line = lines[index];
      final block = blockAt(index);
      final lineStyle = styleFor(block.kind);
      children.addAll(_spansForLine(line, offset, lineStyle));
      offset += line.length;
      if (index < lines.length - 1) {
        children.add(TextSpan(text: '\n', style: lineStyle));
        offset += 1;
      }
    }
    return TextSpan(style: style, children: children);
  }

  List<InlineSpan> _spansForLine(String line, int start, TextStyle style) {
    if (line.isEmpty) return <InlineSpan>[TextSpan(text: '', style: style)];
    final segments = _segments(line, start);
    final spans = <InlineSpan>[];
    for (final segment in segments) {
      final colour = segment.pen == null ? null : markPen(segment.pen!)?.color;
      spans.add(TextSpan(
        text: segment.text,
        style: style.copyWith(
          fontWeight: segment.bold ? FontWeight.w700 : null,
          fontStyle: segment.italic ? FontStyle.italic : null,
          decoration: (segment.underline || segment.strike)
              ? TextDecoration.combine(<TextDecoration>[
                  if (segment.underline) TextDecoration.underline,
                  if (segment.strike) TextDecoration.lineThrough,
                ])
              : null,
          decorationColor: style.color,
          backgroundColor: colour == null
              ? null
              : Color(colour).withValues(alpha: palette.dark ? 0.26 : 0.34),
        ),
      ));
    }
    return spans;
  }

  /// One line cut into runs that share a look — the single source for both the
  /// painted page and the exported file, so what is written out is exactly what
  /// was seen.
  List<JournalRun> _segments(String line, int start) {
    if (line.isEmpty) return <JournalRun>[const JournalRun(text: '')];
    final end = start + line.length;
    final marks = <InlineMark>[
      for (final mark in _marks)
        if (mark.end > start && mark.start < end) mark,
    ];
    final pens = <TextHighlight>[
      for (final highlight in _highlights)
        if (highlight.end > start && highlight.start < end) highlight,
    ];
    if (marks.isEmpty && pens.isEmpty) {
      return <JournalRun>[JournalRun(text: line)];
    }
    final cuts = <int>{0, line.length};
    for (final mark in marks) {
      cuts.add((mark.start - start).clamp(0, line.length).toInt());
      cuts.add((mark.end - start).clamp(0, line.length).toInt());
    }
    for (final pen in pens) {
      cuts.add((pen.start - start).clamp(0, line.length).toInt());
      cuts.add((pen.end - start).clamp(0, line.length).toInt());
    }
    final points = cuts.toList()..sort();
    final runs = <JournalRun>[];
    for (var i = 0; i < points.length - 1; i++) {
      final from = points[i];
      final to = points[i + 1];
      if (to <= from) continue;
      var bold = false;
      var italic = false;
      var underline = false;
      var strike = false;
      String? pen;
      for (final mark in marks) {
        if (mark.start <= start + from && mark.end >= start + to) {
          bold = bold || mark.bold;
          italic = italic || mark.italic;
          underline = underline || mark.underline;
          strike = strike || mark.strike;
        }
      }
      for (final highlight in pens) {
        if (highlight.start <= start + from && highlight.end >= start + to) {
          pen = highlight.pen;
        }
      }
      runs.add(JournalRun(
        text: line.substring(from, to),
        bold: bold,
        italic: italic,
        underline: underline,
        strike: strike,
        pen: pen,
      ));
    }
    return runs;
  }

  /// The page as a file's worth of text: Markdown keeps the styling the phone
  /// offers (headings, quotes, bold, italic, strike, highlight), plain text is
  /// the words and nothing else.
  String export({required String title, bool markdown = true}) {
    final book = StringBuffer();
    final clean = title.trim();
    if (clean.isNotEmpty) {
      book.writeln(markdown ? '# $clean\n' : '$clean\n');
    }
    final lines = text.split('\n');
    var offset = 0;
    for (var index = 0; index < lines.length; index++) {
      final line = lines[index];
      final kind = blockAt(index).kind;
      if (!markdown) {
        book.writeln(line);
      } else {
        final prefix = switch (kind) {
          BlockKind.h1 => '# ',
          BlockKind.h2 => '## ',
          BlockKind.quote => '> ',
          _ => '',
        };
        final body = _segments(line, offset).map((run) {
          if (run.text.trim().isEmpty) return run.text;
          var text = run.text;
          if (run.bold) text = '**$text**';
          if (run.italic) text = '*$text*';
          if (run.strike) text = '~~$text~~';
          if (run.underline) text = '<u>$text</u>';
          if (run.pen != null) text = '==$text==';
          return text;
        }).join();
        book.writeln('$prefix$body');
      }
      offset += line.length + 1;
    }
    return book.toString();
  }

  // ── Snapshot ───────────────────────────────────────────────────────────

  JournalEntry toEntry({
    required JournalEntry base,
    required String title,
    required int mood,
    required List<String> tags,
  }) =>
      base.copyWith(
        title: title,
        text: text,
        blocks: List<JournalBlock>.from(_blocks),
        marks: _marks.where((mark) => !mark.isEmpty).toList(growable: false),
        highlights: _highlights
            .where((highlight) => !highlight.isEmpty)
            .toList(growable: false),
        mood: mood,
        tags: tags,
        updatedAt: DateTime.now(),
      );

  // ── Internals ──────────────────────────────────────────────────────────

  List<String> _lines() => text.split('\n');

  void _syncBlockCount() {
    final count = _lines().length;
    while (_blocks.length < count) {
      _blocks.add(const JournalBlock());
    }
    if (_blocks.length > count) {
      _blocks = _blocks.sublist(0, count);
    }
  }

  void _ensureBlocks(int line) {
    while (_blocks.length <= line) {
      _blocks.add(const JournalBlock());
    }
  }

  int _lineOf(int offset) {
    final lines = _lines();
    var running = 0;
    for (var index = 0; index < lines.length; index++) {
      running += lines[index].length + 1;
      if (offset < running) return index;
    }
    return max(0, lines.length - 1);
  }

  int _lineStart(int line) {
    final lines = _lines();
    var start = 0;
    for (var index = 0; index < line && index < lines.length; index++) {
      start += lines[index].length + 1;
    }
    return start;
  }

  List<int> _selectedLines() {
    final from = _lineOf(selection.start.clamp(0, text.length).toInt());
    final to = _lineOf(selection.end.clamp(0, text.length).toInt());
    return [for (var line = min(from, to); line <= max(from, to); line++) line];
  }

  /// The selected range, or the caret's whole line when nothing is selected.
  ({int start, int end}) _activeRange() {
    if (selection.isValid && !selection.isCollapsed) {
      return (
        start: min(selection.start, selection.end).clamp(0, text.length).toInt(),
        end: max(selection.start, selection.end).clamp(0, text.length).toInt(),
      );
    }
    final line = caretLine;
    final start = _lineStart(line);
    final end = (start + _lines()[line].length).clamp(0, text.length).toInt();
    return (start: start, end: end);
  }

  bool _rangeHas(
    ({int start, int end}) range, {
    bool bold = false,
    bool italic = false,
    bool underline = false,
    bool strike = false,
  }) {
    if (range.end <= range.start) return false;
    if (!bold && !italic && !underline && !strike) return false;
    // `num` on purpose: `dart:math`'s `min`/`max` are generic over `num`, and a
    // caller that hands them two ints still gets a `num` back on some SDKs.
    num covered = 0;
    for (final mark in _marks) {
      if (bold && !mark.bold) continue;
      if (italic && !mark.italic) continue;
      if (underline && !mark.underline) continue;
      if (strike && !mark.strike) continue;
      final overlap =
          min<int>(mark.end, range.end) - max<int>(mark.start, range.start);
      if (overlap > 0) covered += overlap;
    }
    return covered >= range.end - range.start;
  }

  static final RegExp _numberedPattern = RegExp(r'^\d+\. ');
  static final RegExp _anyMarker = RegExp(r'^(• |– |\d+\. |☐ |☑ )');

  static String _markerPrefix(String line) {
    final match = _anyMarker.firstMatch(line);
    return match?.group(0) ?? '';
  }

  String _prefixFor(MarkerKind marker, int index) {
    switch (marker) {
      case MarkerKind.dot:
        return '• ';
      case MarkerKind.dash:
        return '– ';
      case MarkerKind.numbered:
        var number = 1;
        final lines = _lines();
        for (var line = index - 1; line >= 0; line--) {
          if (_numberedPattern.hasMatch(lines[line])) {
            number++;
          } else {
            break;
          }
        }
        return '$number. ';
    }
  }

  int _numberOf(String line) {
    final match = _numberedPattern.firstMatch(line);
    if (match == null) return 0;
    return int.tryParse(match.group(0)!.split('.').first) ?? 0;
  }

  void _write(List<String> lines, int caret, {bool record = true}) {
    final next = lines.join('\n');
    final old = text;
    if (record) _push(_snapshot());
    value = TextEditingValue(
      text: next,
      selection: TextSelection.collapsed(offset: caret.clamp(0, next.length).toInt()),
    );
    if (old != next) applyEdit(old, record: false);
    _shakeBaseline = DateTime.now();
    notifyListeners();
    onMutated?.call();
  }

  /// Merges touching marks so one word is ONE span, and drops plain ones.
  List<InlineMark> _normalize(List<InlineMark> marks) {
    final out = <InlineMark>[];
    for (final mark in marks) {
      if (mark.isEmpty || mark.isPlain) continue;
      final index = out.indexWhere((other) =>
          other.bold == mark.bold &&
          other.italic == mark.italic &&
          other.underline == mark.underline &&
          other.strike == mark.strike &&
          mark.start <= other.end &&
          other.start <= mark.end);
      if (index >= 0) {
        final other = out[index];
        out[index] = other.copyWith(
          start: min(other.start, mark.start),
          end: max(other.end, mark.end),
        );
      } else {
        out.add(mark);
      }
    }
    return out;
  }

  List<TextHighlight> _normalizeHighlights(List<TextHighlight> highlights) {
    final out = <TextHighlight>[];
    for (final highlight in highlights) {
      if (highlight.isEmpty) continue;
      final index = out.indexWhere((other) =>
          other.pen == highlight.pen &&
          highlight.start <= other.end &&
          other.start <= highlight.end);
      if (index >= 0) {
        final other = out[index];
        out[index] = other.copyWith(
          start: min(other.start, highlight.start),
          end: max(other.end, highlight.end),
        );
      } else {
        out.add(highlight);
      }
    }
    return out;
  }
}

/// One stretch of a line with everything it wears — what both the painted page
/// and the exported file read from.
class JournalRun {
  const JournalRun({
    required this.text,
    this.bold = false,
    this.italic = false,
    this.underline = false,
    this.strike = false,
    this.pen,
  });

  final String text;
  final bool bold;
  final bool italic;
  final bool underline;
  final bool strike;
  final String? pen;
}

String _roman(int value) {
  const numerals = <String>[
    '', 'I', 'II', 'III', 'IV', 'V', 'VI', 'VII', 'VIII', 'IX', 'X',
    'XI', 'XII', 'XIII', 'XIV', 'XV', 'XVI', 'XVII', 'XVIII', 'XIX', 'XX',
  ];
  if (value <= 0) return 'I';
  if (value < numerals.length) return numerals[value];
  return '$value';
}
