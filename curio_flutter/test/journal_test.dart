import 'dart:io';

import 'package:curio/data/journal.dart';
import 'package:curio/store/app_model.dart';
import 'package:curio/store/journal_controller.dart';
import 'package:flutter_test/flutter_test.dart';

/// The journal page's tools, tested where they live: in the controller.
///
/// Every case here is a behaviour the phone's dock has and this one must match —
/// a marker that goes on and comes off, a numbered run that stays numbered, a
/// format flag that toggles over the selection, pens that move with the words,
/// and an undo that puts the page back. These need no rendering, so they run in
/// the CI's plain `flutter test` on a machine with no window.
JournalEntry page(
  String text, {
  List<InlineMark> marks = const <InlineMark>[],
  DateTime? createdAt,
}) {
  // A far-past created stamp by default, so a test that cares about DAYS (the
  // streak) is not handed one by the fixture.
  final born = createdAt ?? DateTime(2020, 1, 1);
  return JournalEntry(
    id: 'p1',
    title: '',
    text: text,
    blocks: <JournalBlock>[
      for (var i = 0; i < text.split('\n').length; i++) const JournalBlock(),
    ],
    marks: marks,
    mood: 0,
    tags: const <String>[],
    createdAt: born,
    updatedAt: born,
  );
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  test('the bullet door puts a marker on, takes it off, and renumbers a run',
      () {
    final controller = JournalController(page('one\ntwo'));
    controller.selection = const TextSelection.collapsed(offset: 0);

    controller.applyMarker(MarkerKind.dot);
    expect(controller.text.split('\n').first, '• one');
    expect(controller.caretMarker, MarkerKind.dot);

    // The same marker pressed again takes the list off the line.
    controller.applyMarker(MarkerKind.dot);
    expect(controller.text.split('\n').first, 'one');
    expect(controller.caretMarker, isNull);

    // A numbered list counts from the run above it …
    controller.text = 'first\nsecond\nthird';
    controller.selection = const TextSelection.collapsed(offset: 0);
    controller.applyMarker(MarkerKind.numbered);
    controller.selection = TextSelection.collapsed(offset: controller.text.indexOf('second'));
    controller.applyMarker(MarkerKind.numbered);
    controller.selection = TextSelection.collapsed(offset: controller.text.indexOf('third'));
    controller.applyMarker(MarkerKind.numbered);
    expect(
      controller.text.split('\n').map((line) => line.substring(0, 3)).toList(),
      <String>['1. ', '2. ', '3. '],
    );

    // … and the door on a line that already wears it takes it off again.
    controller.selection = const TextSelection.collapsed(offset: 0);
    controller.applyMarker(MarkerKind.numbered);
    expect(controller.text.split('\n').first, 'first');
  });

  test('the todo door ticks and unticks the caret line', () {
    final controller = JournalController(page('buy bread'));
    controller.selection = const TextSelection.collapsed(offset: 0);
    controller.toggleTodo();
    expect(controller.text, '☐ buy bread');
    controller.toggleTodo();
    expect(controller.text, '☑ buy bread');
    controller.toggleTodo();
    expect(controller.text, '☐ buy bread');
  });

  test('format toggles over the selection and back off', () {
    final controller = JournalController(page('one good thing'));
    controller.selection = const TextSelection(baseOffset: 4, extentOffset: 8);
    controller.toggleInline(bold: true);
    expect(controller.caretBold, isTrue);

    controller.toggleInline(italic: true);
    expect(controller.caretBold, isTrue);
    expect(controller.caretItalic, isTrue);

    controller.toggleInline(bold: true);
    expect(controller.caretBold, isFalse);
    // Turning the bold off must not take the italic with it.
    expect(controller.caretItalic, isTrue);
  });

  test('a marker pen goes on, moves with an edit, and comes off', () {
    final controller = JournalController(page('alpha beta'));
    controller.selection = const TextSelection(baseOffset: 6, extentOffset: 10);
    controller.applyHighlight('sage');
    expect(controller.caretHighlight, 'sage');

    // Typing BEFORE the pen shifts it, so it still covers the same words.
    controller.text = 'new alpha beta';
    controller.applyEdit('alpha beta');
    controller.selection = const TextSelection(baseOffset: 10, extentOffset: 14);
    expect(controller.caretHighlight, 'sage');

    controller.applyHighlight(null);
    expect(controller.caretHighlight, isNull);
  });

  test('undo puts the page back, redo puts the change back', () {
    final controller = JournalController(page('one'));
    controller.selection = const TextSelection.collapsed(offset: 0);
    controller.toggleTodo();
    expect(controller.text, '☐ one');
    expect(controller.canUndo, isTrue);

    controller.undo();
    expect(controller.text, 'one');
    expect(controller.canRedo, isTrue);

    controller.redo();
    expect(controller.text, '☐ one');
  });

  test('the export keeps the styling it was written with', () {
    final controller = JournalController(page('Intro\nbody words\nquote line'));
    controller.selection = const TextSelection.collapsed(offset: 0);
    controller.setKind(BlockKind.h1);
    controller.selection = TextSelection.collapsed(offset: controller.text.indexOf('body'));
    controller.toggleKind(BlockKind.body);
    controller.selection = const TextSelection(baseOffset: 6, extentOffset: 10);
    controller.toggleInline(bold: true);

    final markdown = controller.export(title: 'A page');
    expect(markdown, contains('# A page'));
    expect(markdown, contains('# Intro'));
    expect(markdown, contains('**body**'));

    final plain = controller.export(title: 'A page', markdown: false);
    expect(plain, isNot(contains('# ')));
    expect(plain, contains('body words'));
  });

  test('a page keeps its pens and marks through a JSON round trip', () {
    final original = page(
      'hello',
      marks: const <InlineMark>[
        InlineMark(start: 0, end: 5, bold: true, underline: true),
      ],
    ).copyWith(highlights: const <TextHighlight>[
      TextHighlight(start: 0, end: 3, pen: 'sky'),
    ]);
    final restored = JournalEntry.fromJson(original.toJson());
    expect(restored.marks.single.bold, isTrue);
    expect(restored.marks.single.underline, isTrue);
    expect(restored.highlights.single.pen, 'sky');
  });

  test('the streak counts the days something was done, and breaks on a gap', () {
    final today = DateTime.now();
    final model = AppModel(dataDir: Directory.systemTemp.createTempSync('curio'));
    model.journals.addAll(<JournalEntry>[
      page('today').copyWith(updatedAt: today),
      page('yesterday').copyWith(updatedAt: today.subtract(const Duration(days: 1))),
      page('two days ago')
          .copyWith(updatedAt: today.subtract(const Duration(days: 2))),
      page('last week').copyWith(updatedAt: today.subtract(const Duration(days: 7))),
    ]);
    expect(model.streakDays, 3);
  });
}
