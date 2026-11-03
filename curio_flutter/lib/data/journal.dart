import 'dart:math';

/// A journal page's shape, in the terms the phone's editor uses.
///
/// The Android editor is a block-and-run model (`PersonalDoc`), and the tools
/// act on the LINE the caret is on: a heading, a quote, small text, a marker.
/// This is the same idea at the size a desktop editor needs — one block per
/// line, and inline bold/italic as offset ranges over the page's text.
enum BlockKind { body, h1, h2, quote, small }

/// The markers the bullet door offers. A marker is REAL text at the start of
/// the line (the phone renders a gutter; a plain TextField cannot, and a marker
/// that is not in the text would move the caret by a character it cannot see).
enum MarkerKind { dot, dash, numbered }

class JournalBlock {
  const JournalBlock({this.kind = BlockKind.body});

  final BlockKind kind;

  JournalBlock copyWith({BlockKind? kind}) =>
      JournalBlock(kind: kind ?? this.kind);

  Map<String, dynamic> toJson() => <String, dynamic>{'kind': kind.name};

  factory JournalBlock.fromJson(Map<String, dynamic> json) => JournalBlock(
        kind: BlockKind.values.firstWhere(
          (value) => value.name == json['kind'],
          orElse: () => BlockKind.body,
        ),
      );
}

/// A stretch of a line that is set differently — any combination of the four
/// format flags the phone's FORMAT door offers (bold, italic, underline,
/// strikethrough). Offsets are into the page's whole text, so a mark moves with
/// the words when text before it is edited (see `JournalController.applyEdit`).
class InlineMark {
  const InlineMark({
    required this.start,
    required this.end,
    this.bold = false,
    this.italic = false,
    this.underline = false,
    this.strike = false,
  });

  final int start;
  final int end;
  final bool bold;
  final bool italic;
  final bool underline;
  final bool strike;

  bool get isEmpty => end <= start;

  /// True when this mark says nothing anywhere — dropped rather than stored.
  bool get isPlain => !bold && !italic && !underline && !strike;

  InlineMark copyWith({
    int? start,
    int? end,
    bool? bold,
    bool? italic,
    bool? underline,
    bool? strike,
  }) =>
      InlineMark(
        start: start ?? this.start,
        end: end ?? this.end,
        bold: bold ?? this.bold,
        italic: italic ?? this.italic,
        underline: underline ?? this.underline,
        strike: strike ?? this.strike,
      );

  Map<String, dynamic> toJson() => <String, dynamic>{
        'start': start,
        'end': end,
        if (bold) 'bold': true,
        if (italic) 'italic': true,
        if (underline) 'underline': true,
        if (strike) 'strike': true,
      };

  factory InlineMark.fromJson(Map<String, dynamic> json) => InlineMark(
        start: json['start'] is int ? json['start'] as int : 0,
        end: json['end'] is int ? json['end'] as int : 0,
        bold: json['bold'] == true,
        italic: json['italic'] == true,
        underline: json['underline'] == true,
        strike: json['strike'] == true,
      );
}

/// One of the marker pens the phone's dock offers (its `PERSONAL_HIGHLIGHT_KEYS`).
/// A pen is stored by KEY on the page, and its colour is looked up here, so the
/// stored page never carries a colour that a theme change could make wrong.
class MarkPen {
  const MarkPen(this.key, this.label, this.color);

  final String key;
  final String label;
  final int color;
}

const List<MarkPen> kMarkPens = <MarkPen>[
  MarkPen('amber', 'Amber', 0xFFE0A33C),
  MarkPen('rose', 'Rose', 0xFFD98A8A),
  MarkPen('sage', 'Sage', 0xFF8FB08A),
  MarkPen('sky', 'Sky', 0xFF7FA8C9),
];

MarkPen? markPen(String key) {
  for (final pen in kMarkPens) {
    if (pen.key == key) return pen;
  }
  return null;
}

/// A stretch a marker pen has been drawn over — the phone's highlighter. Like
/// [InlineMark] it is a range over the page's whole text, and it moves with the
/// words as they are edited.
class TextHighlight {
  const TextHighlight({
    required this.start,
    required this.end,
    required this.pen,
  });

  final int start;
  final int end;
  final String pen;

  bool get isEmpty => end <= start;

  TextHighlight copyWith({int? start, int? end, String? pen}) => TextHighlight(
        start: start ?? this.start,
        end: end ?? this.end,
        pen: pen ?? this.pen,
      );

  Map<String, dynamic> toJson() => <String, dynamic>{
        'start': start,
        'end': end,
        'pen': pen,
      };

  factory TextHighlight.fromJson(Map<String, dynamic> json) => TextHighlight(
        start: json['start'] is int ? json['start'] as int : 0,
        end: json['end'] is int ? json['end'] as int : 0,
        pen: json['pen'] is String ? json['pen'] as String : 'amber',
      );
}

/// One journal page.
class JournalEntry {
  const JournalEntry({
    required this.id,
    required this.title,
    required this.text,
    required this.blocks,
    required this.marks,
    this.highlights = const <TextHighlight>[],
    required this.mood,
    required this.tags,
    required this.createdAt,
    required this.updatedAt,
  });

  final String id;
  final String title;
  final String text;
  final List<JournalBlock> blocks;
  final List<InlineMark> marks;

  /// The stretches a marker pen has been drawn over.
  final List<TextHighlight> highlights;

  /// 0…4, the same five moods the phone's editor offers (0 = not chosen).
  final int mood;
  final List<String> tags;
  final DateTime createdAt;
  final DateTime updatedAt;

  static const moodLabels = <String>['No mood', 'Light', 'Good', 'Even', 'Heavy'];
  static const moodGlyphs = <String>['·', '☀', '◇', '≈', '☂'];

  int get words {
    final trimmed = text.trim();
    if (trimmed.isEmpty) return 0;
    return trimmed.split(RegExp(r'\s+')).length;
  }

  String get displayTitle {
    if (title.trim().isNotEmpty) return title.trim();
    final first = text
        .split('\n')
        .map((line) => line.trim())
        .firstWhere((line) => line.isNotEmpty, orElse: () => '');
    if (first.isEmpty) return 'Untitled page';
    return first.length > 60 ? '${first.substring(0, 60)}…' : first;
  }

  String get preview {
    final body = text
        .split('\n')
        .map((line) => line.trim())
        .where((line) => line.isNotEmpty)
        .skip(title.trim().isEmpty ? 0 : 1)
        .join(' ');
    if (body.isEmpty) return '';
    return body.length > 120 ? '${body.substring(0, 120)}…' : body;
  }

  /// The page's headings, in order, numbered the way the phone's contents list
  /// numbers them: I, II for the page's own headings and I.1, I.2 for sections.
  List<JournalHeading> get headings {
    final out = <JournalHeading>[];
    var chapter = 0;
    var section = 0;
    final lines = text.split('\n');
    for (var index = 0; index < lines.length; index++) {
      final block = blockAt(index);
      final label = lines[index].trim();
      if (label.isEmpty) continue;
      if (block.kind == BlockKind.h1) {
        chapter++;
        section = 0;
        out.add(JournalHeading(
          line: index,
          label: label,
          number: _roman(chapter),
          level: 1,
        ));
      } else if (block.kind == BlockKind.h2) {
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
    if (blocks.isEmpty) return const JournalBlock();
    if (line < blocks.length) return blocks[line];
    return blocks.last;
  }

  JournalEntry copyWith({
    String? title,
    String? text,
    List<JournalBlock>? blocks,
    List<InlineMark>? marks,
    List<TextHighlight>? highlights,
    int? mood,
    List<String>? tags,
    DateTime? updatedAt,
  }) =>
      JournalEntry(
        id: id,
        title: title ?? this.title,
        text: text ?? this.text,
        blocks: blocks ?? this.blocks,
        marks: marks ?? this.marks,
        highlights: highlights ?? this.highlights,
        mood: mood ?? this.mood,
        tags: tags ?? this.tags,
        createdAt: createdAt,
        updatedAt: updatedAt ?? this.updatedAt,
      );

  Map<String, dynamic> toJson() => <String, dynamic>{
        'id': id,
        'title': title,
        'text': text,
        'blocks': blocks.map((block) => block.toJson()).toList(growable: false),
        'marks': marks.where((mark) => !mark.isEmpty).map((mark) => mark.toJson()).toList(growable: false),
        'highlights': highlights
            .where((highlight) => !highlight.isEmpty)
            .map((highlight) => highlight.toJson())
            .toList(growable: false),
        'mood': mood,
        'tags': tags,
        'createdAt': createdAt.toIso8601String(),
        'updatedAt': updatedAt.toIso8601String(),
      };

  factory JournalEntry.fromJson(Map<String, dynamic> json) {
    final rawBlocks = json['blocks'];
    final rawMarks = json['marks'];
    final rawHighlights = json['highlights'];
    final rawTags = json['tags'];
    return JournalEntry(
      id: json['id'] is String && (json['id'] as String).isNotEmpty
          ? json['id'] as String
          : newJournalId(),
      title: json['title'] is String ? json['title'] as String : '',
      text: json['text'] is String ? json['text'] as String : '',
      blocks: rawBlocks is List
          ? rawBlocks
              .whereType<Map<String, dynamic>>()
              .map(JournalBlock.fromJson)
              .toList(growable: false)
          : const <JournalBlock>[],
      marks: rawMarks is List
          ? rawMarks
              .whereType<Map<String, dynamic>>()
              .map(InlineMark.fromJson)
              .toList(growable: false)
          : const <InlineMark>[],
      highlights: rawHighlights is List
          ? rawHighlights
              .whereType<Map<String, dynamic>>()
              .map(TextHighlight.fromJson)
              .toList(growable: false)
          : const <TextHighlight>[],
      mood: json['mood'] is num ? (json['mood'] as num).toInt().clamp(0, 4).toInt() : 0,
      tags: rawTags is List
          ? rawTags.whereType<String>().toList(growable: false)
          : const <String>[],
      createdAt: _date(json['createdAt']),
      updatedAt: _date(json['updatedAt']),
    );
  }

  static DateTime _date(Object? value) =>
      value is String ? (DateTime.tryParse(value) ?? DateTime.now()) : DateTime.now();
}

class JournalHeading {
  const JournalHeading({
    required this.line,
    required this.label,
    required this.number,
    required this.level,
  });

  final int line;
  final String label;
  final String number;
  final int level;
}

String newJournalId() =>
    DateTime.now().microsecondsSinceEpoch.toRadixString(36) +
    Random().nextInt(1 << 16).toRadixString(36);

String _roman(int value) {
  const numerals = <String>[
    '', 'I', 'II', 'III', 'IV', 'V', 'VI', 'VII', 'VIII', 'IX', 'X',
    'XI', 'XII', 'XIII', 'XIV', 'XV', 'XVI', 'XVII', 'XVIII', 'XIX', 'XX',
  ];
  if (value <= 0) return 'I';
  if (value < numerals.length) return numerals[value];
  return '$value';
}
