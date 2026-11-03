import 'dart:convert';
import 'dart:io';
import 'dart:math';

import 'package:flutter/material.dart';

import '../data/journal.dart';
import '../data/lanes.dart';
import '../data/topic.dart';
import '../data/topic_catalog.dart';

/// One saved discovery. The TOPIC is stored inside the entry (not just its id):
/// the cabinet has to open and read long after a catalog edit, and an entry the
/// catalog no longer carries must still show what was saved.
class SavedEntry {
  const SavedEntry({required this.topic, required this.savedAt});

  final Topic topic;
  final DateTime savedAt;

  factory SavedEntry.fromJson(Map<String, dynamic> json) {
    final rawTopic = json['topic'];
    final saved = json['savedAt'];
    return SavedEntry(
      topic: Topic.fromJson(
        rawTopic is Map ? Map<String, dynamic>.from(rawTopic) : const <String, dynamic>{},
      ),
      savedAt: saved is String ? (DateTime.tryParse(saved) ?? DateTime.now()) : DateTime.now(),
    );
  }

  Map<String, dynamic> toJson() => <String, dynamic>{
        'topic': topic.toJson(),
        'savedAt': savedAt.toIso8601String(),
      };
}

/// The whole state of the desktop/iOS shell: what is loaded, which lane is
/// chosen, the three cards on the deck, and what has been saved.
///
/// Two JSON files, both plain and both owned by this app:
///   * `prefs.json` — the chosen lane and the theme, so a window opens the way
///     it was left;
///   * `cabinet.json` — the saved discoveries.
///
/// They live under `%APPDATA%\Curio` on Windows and `~/.curio` elsewhere — the
/// same idea as the phone's Room rows, kept as files because a desktop app has
/// no process to hold a database and a single-user cabinet is small.
class AppModel extends ChangeNotifier {
  AppModel({Directory? dataDir}) : _dataDirOverride = dataDir;

  final Directory? _dataDirOverride;
  Directory? _dataDir;

  bool loading = true;
  String? loadError;

  TopicCatalog? _catalog;

  ThemeMode themeMode = ThemeMode.system;
  String laneId = kLanes.first.id;
  int spinCount = 0;

  final List<SavedEntry> saved = <SavedEntry>[];
  final List<Topic> deck = <Topic>[];

  /// The journals, newest first. Kept in the same folder as the cabinet, in
  /// their own file, because a page of writing is the member's own and must not
  /// be lost to a bad cabinet write (or the other way round).
  final List<JournalEntry> journals = <JournalEntry>[];

  /// The journal whose page is open in the Cabinet room, if any.
  String? openJournalId;

  Directory get dataDir => _dataDir ??= _resolveDataDir();

  String get dataPath => dataDir.path;

  /// The lane the deck is drawing from.
  Lane get lane => laneById(laneId);

  /// The card at the front of the deck; the two behind it are the peeks.
  Topic? get current => deck.isEmpty ? null : deck.first;

  List<Topic> get peeks => deck.length <= 1 ? const <Topic>[] : deck.sublist(1);

  int get laneCount => _catalog?.lane(laneId).length ?? 0;

  int get totalTopics => _catalog?.total ?? 0;

  bool get catalogEmpty => _catalog?.isEmpty ?? true;

  bool isSaved(String topicId) => saved.any((entry) => entry.topic.id == topicId);

  /// The journal the editor is showing.
  JournalEntry? get openJournal {
    final id = openJournalId;
    if (id == null) return null;
    for (final entry in journals) {
      if (entry.id == id) return entry;
    }
    return null;
  }

  /// Words written, across every page — the number Home shows.
  int get journalWords =>
      journals.fold<int>(0, (total, entry) => total + entry.words);

  JournalEntry? get latestJournal => journals.isEmpty ? null : journals.first;

  /// The newest few pages, for Home's recents list.
  List<JournalEntry> get recentJournals =>
      journals.length <= 4 ? journals : journals.sublist(0, 4);

  /// The page that is open, or the newest one — what "Continue" means.
  JournalEntry? get resumeJournal => openJournal ?? latestJournal;

  /// ── THE STREAK ───────────────────────────────────────────────────
  ///
  /// Consecutive days on which something was done: a page was written, or a
  /// card was kept. The phone's `StreakTracker` counts the same activity, so the
  /// two apps agree about what a streak IS — and nothing new is stored for it:
  /// every timestamp is one the cabinet and the journals already carry. The run
  /// may end today (still going) or yesterday (not broken yet at breakfast),
  /// which is the phone's own rule.
  int get streakDays {
    final days = <DateTime>{
      for (final entry in journals) ...<DateTime>{
        _dayOf(entry.updatedAt),
        _dayOf(entry.createdAt),
      },
      for (final entry in saved) _dayOf(entry.savedAt),
    };
    if (days.isEmpty) return 0;
    var cursor = _dayOf(DateTime.now());
    if (!days.contains(cursor)) {
      cursor = cursor.subtract(const Duration(days: 1));
      if (!days.contains(cursor)) return 0;
    }
    var streak = 0;
    while (days.contains(cursor)) {
      streak++;
      cursor = cursor.subtract(const Duration(days: 1));
    }
    return streak;
  }

  /// Words written today, across every page touched today.
  int get wordsToday {
    final today = _dayOf(DateTime.now());
    return journals
        .where((entry) => _dayOf(entry.updatedAt) == today)
        .fold<int>(0, (total, entry) => total + entry.words);
  }

  static DateTime _dayOf(DateTime moment) =>
      DateTime(moment.year, moment.month, moment.day);

  /// Reads the catalog and both stores, then deals the first hand.
  ///
  /// A failure is REPORTED (`loadError`), never swallowed into a blank window:
  /// a shell that opens with an empty cabinet and says why is debuggable; one
  /// that opens silently empty is not.
  Future<void> boot() async {
    try {
      _catalog = await TopicCatalog.load();
      await _readPrefs();
      await _readCabinet();
      await _readJournals();
      _deal();
    } catch (error) {
      loadError = '$error';
    } finally {
      loading = false;
      notifyListeners();
    }
  }

  /// Choose a lane and deal a fresh hand from it.
  Future<void> selectLane(String id) async {
    laneId = laneById(id).id;
    _deal();
    notifyListeners();
    await _writePrefs();
  }

  /// Turn over a new front card, keeping the two that were behind it in play.
  void shuffle() {
    spinCount++;
    _deal();
    notifyListeners();
  }

  Future<void> setTheme(ThemeMode mode) async {
    themeMode = mode;
    notifyListeners();
    await _writePrefs();
  }

  /// Saves the front card. Saving twice is a no-op (the same topic is one row).
  Future<void> saveCurrent() async {
    final topic = current;
    if (topic == null || isSaved(topic.id)) return;
    saved.insert(0, SavedEntry(topic: topic, savedAt: DateTime.now()));
    notifyListeners();
    await _writeCabinet();
  }

  // ── Journals ───────────────────────────────────────────────────────────

  /// Opens a blank page at the top of the list. The page is written to disk at
  /// once, so the Cabinet never shows a journal that a crash could take away.
  Future<JournalEntry> createJournal() async {
    final now = DateTime.now();
    final entry = JournalEntry(
      id: newJournalId(),
      title: '',
      text: '',
      blocks: const <JournalBlock>[JournalBlock()],
      marks: const <InlineMark>[],
      mood: 0,
      tags: const <String>[],
      createdAt: now,
      updatedAt: now,
    );
    journals.insert(0, entry);
    openJournalId = entry.id;
    notifyListeners();
    await _writeJournals();
    return entry;
  }

  void openJournalById(String id) {
    openJournalId = id;
    notifyListeners();
  }

  void closeJournal() {
    openJournalId = null;
    notifyListeners();
  }

  /// Saves an edited page in place, keeping the list ordered by the last edit.
  Future<void> saveJournal(JournalEntry entry) async {
    final index = journals.indexWhere((item) => item.id == entry.id);
    if (index < 0) {
      journals.insert(0, entry);
    } else {
      journals[index] = entry;
    }
    journals.sort((a, b) => b.updatedAt.compareTo(a.updatedAt));
    notifyListeners();
    await _writeJournals();
  }

  Future<void> deleteJournal(String id) async {
    journals.removeWhere((entry) => entry.id == id);
    if (openJournalId == id) openJournalId = null;
    notifyListeners();
    await _writeJournals();
  }

  Future<void> remove(String topicId) async {
    saved.removeWhere((entry) => entry.topic.id == topicId);
    notifyListeners();
    await _writeCabinet();
  }

  Future<void> clearCabinet() async {
    if (saved.isEmpty) return;
    saved.clear();
    notifyListeners();
    await _writeCabinet();
  }

  // ── The deck ───────────────────────────────────────────────────────────

  void _deal() {
    deck.clear();
    final pool = _catalog?.lane(laneId) ?? const <Topic>[];
    if (pool.isEmpty) return;
    final random = Random();
    final seen = <String>{};
    final wanted = min(3, pool.length);
    var guard = 0;
    while (deck.length < wanted && guard < 40) {
      guard++;
      final candidate = pool[random.nextInt(pool.length)];
      if (seen.add(candidate.id)) deck.add(candidate);
    }
  }

  // ── The stores ─────────────────────────────────────────────────────────

  File get _prefsFile => File('${dataDir.path}${Platform.pathSeparator}prefs.json');

  File get _cabinetFile =>
      File('${dataDir.path}${Platform.pathSeparator}cabinet.json');

  File get _journalsFile =>
      File('${dataDir.path}${Platform.pathSeparator}journals.json');

  Future<void> _readPrefs() async {
    try {
      if (!await _prefsFile.exists()) return;
      final decoded = jsonDecode(await _prefsFile.readAsString());
      if (decoded is! Map<String, dynamic>) return;
      final mode = decoded['theme'];
      if (mode is int && mode >= 0 && mode < ThemeMode.values.length) {
        themeMode = ThemeMode.values[mode];
      }
      final lane = decoded['lane'];
      if (lane is String && lane.isNotEmpty) laneId = laneById(lane).id;
    } catch (_) {
      // A corrupt prefs file must never cost the app its cabinet: the defaults
      // stand and the file is rewritten on the next change.
    }
  }

  Future<void> _writePrefs() async {
    try {
      await dataDir.create(recursive: true);
      await _prefsFile.writeAsString(
        jsonEncode(<String, dynamic>{
          'lane': laneId,
          'theme': themeMode.index,
        }),
      );
    } catch (_) {
      // Preferences are a convenience; a read-only folder is not an error worth
      // interrupting the member for.
    }
  }

  Future<void> _readCabinet() async {
    try {
      if (!await _cabinetFile.exists()) return;
      final decoded = jsonDecode(await _cabinetFile.readAsString());
      if (decoded is! List) return;
      saved
        ..clear()
        ..addAll(
          decoded
              .whereType<Map<String, dynamic>>()
              .map(SavedEntry.fromJson)
              .where((entry) => entry.topic.name.isNotEmpty),
        );
    } catch (_) {
      // Same rule as the prefs: an unreadable cabinet is left alone rather than
      // overwritten, so a hand-fixed file still has its chance.
    }
  }

  Future<void> _readJournals() async {
    try {
      if (!await _journalsFile.exists()) return;
      final decoded = jsonDecode(await _journalsFile.readAsString());
      if (decoded is! List) return;
      // Every page is kept, including a brand-new blank one: "New page" writes
      // its entry at once, and a restart must not quietly delete the sheet the
      // member had only just opened.
      journals
        ..clear()
        ..addAll(decoded.whereType<Map<String, dynamic>>().map(JournalEntry.fromJson));
      journals.sort((a, b) => b.updatedAt.compareTo(a.updatedAt));
    } catch (_) {
      // A corrupt journals file is left alone rather than overwritten: a page
      // of writing is not something to replace with an empty list.
    }
  }

  Future<void> _writeJournals() async {
    try {
      await dataDir.create(recursive: true);
      await _journalsFile.writeAsString(
        jsonEncode(journals.map((entry) => entry.toJson()).toList(growable: false)),
      );
    } catch (error) {
      loadError = 'Could not write the journals: $error';
      notifyListeners();
    }
  }

  Future<void> _writeCabinet() async {
    try {
      await dataDir.create(recursive: true);
      await _cabinetFile.writeAsString(
        jsonEncode(saved.map((entry) => entry.toJson()).toList(growable: false)),
      );
    } catch (error) {
      loadError = 'Could not write the cabinet: $error';
      notifyListeners();
    }
  }

  Directory _resolveDataDir() {
    if (_dataDirOverride != null) return _dataDirOverride;
    final env = Platform.environment;
    final separator = Platform.pathSeparator;
    if (Platform.isWindows) {
      final base = env['APPDATA'] ?? env['USERPROFILE'] ?? Directory.current.path;
      return Directory('$base${separator}Curio');
    }
    final home = env['HOME'] ?? Directory.current.path;
    if (Platform.isMacOS) {
      return Directory('$home/Library/Application Support/Curio');
    }
    return Directory('$home$separator.curio');
  }
}
