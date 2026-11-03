// GENERATED from the Kotlin catalog — do not hand-edit the table.
//
// A lane's SLUG (which is also its topic file name) is declared once, in
// `app/src/main/java/com/curio/app/data/Category.kt` (`routeSlug`), and its
// accent in `app/src/main/java/com/curio/app/ui/theme/CurioColors.kt`. The
// Android app stays the one place a lane is declared; `tool/sync_lanes.py`
// regenerates this file from it, so the shells can never drift on a name or a
// colour.

/// One Curio lane: its route slug (== `assets/topics/<slug>.json`), its label
/// and its accent (0xAARRGGBB).
class Lane {
  const Lane(this.id, this.label, this.accent);

  final String id;
  final String label;
  final int accent;

  @override
  String toString() => 'Lane($id)';
}

/// The catalog's lanes, in the order the Android catalog declares them.
const List<Lane> kLanes = <Lane>[
  Lane('artists', 'Artists', 0xFF4338CA),
  Lane('albums', 'Albums', 0xFF5F4DCB),
  Lane('directors', 'Directors', 0xFFBE123C),
  Lane('films', 'Films', 0xFFBE123C),
  Lane('animated-movies', 'Animated Films', 0xFF701A75),
  Lane('authors', 'Authors', 0xFFB45309),
  Lane('books', 'Books', 0xFFB45309),
  Lane('painters', 'Painters', 0xFF0F766E),
  Lane('artworks', 'Artworks', 0xFF0F766E),
  Lane('scientists', 'Scientists', 0xFF0369A1),
  Lane('discoveries', 'Discoveries', 0xFF0369A1),
  Lane('songs', 'Songs', 0xFF0E7490),
  Lane('series', 'Series', 0xFFBE185D),
  Lane('anime', 'Anime', 0xFF7E22CE),
  Lane('manga', 'Manga', 0xFF5B21B6),
  Lane('manhwa', 'Manhwa', 0xFF9333EA),
  Lane('games', 'Games', 0xFFA21CAF),
  Lane('mythology', 'Mythology', 0xFFC2410C),
  Lane('sports', 'Sports', 0xFF047857),
  Lane('food', 'Food', 0xFFB91C1C),
  Lane('internet', 'Internet', 0xFF1D4ED8),
  Lane('biology', 'Biology', 0xFF15803D),
  Lane('chemistry', 'Chemistry', 0xFF4D7C0F),
  Lane('animals', 'Animals', 0xFF78350F),
  Lane('plants', 'Plants', 0xFF065F46),
  Lane('technologies', 'Technologies', 0xFF334155),
  Lane('astronomy', 'Astronomy', 0xFF1E3A8A),
  Lane('history', 'History', 0xFF854D0E),
  Lane('geology', 'Geology', 0xFF57534E),
  Lane('medicine', 'Medicine', 0xFF9F1239),
  Lane('psychology', 'Psychology', 0xFF6D28D9),
  Lane('mathematics', 'Mathematics', 0xFF1E40AF),
  Lane('economics', 'Economics', 0xFFA16207),
  Lane('language', 'Language', 0xFF0D9488),
  Lane('engineering', 'Engineering', 0xFF3F3F46),
  Lane('oceans', 'Oceans', 0xFF0C4A6E),
  Lane('quotes', 'Quotes', 0xFF7C3AED),
  Lane('wildcard', 'Wildcard', 0xFFFF8FA3),
];

/// The lane for [id], or the wildcard lane when a stored preference names a lane
/// that no longer exists (a stale `prefs.json` must never crash the shell).
Lane laneById(String id) {
  for (final lane in kLanes) {
    if (lane.id == id) return lane;
  }
  return kLanes.last;
}
