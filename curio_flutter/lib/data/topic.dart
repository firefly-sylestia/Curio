/// One entry from the shared Curio catalog (`data/topics/<lane>.json`).
///
/// The JSON is the phone's own format — a bare array, no wrapper object, one
/// file per lane (`app/src/main/assets/topics/SCHEMA.md` is the contract). This
/// class reads the fields the desktop and iOS shells need and ignores the rest
/// (capture formats, tiers, alternates), so a catalog addition never breaks the
/// shell: an unknown key is simply not read.
class Topic {
  const Topic({
    required this.id,
    required this.categoryId,
    required this.name,
    required this.teaser,
    required this.imageUrl,
    required this.byline,
    required this.subtype,
    required this.verb,
    required this.target,
    required this.minutes,
    required this.instruction,
    required this.tags,
  });

  final String id;
  final String categoryId;
  final String name;

  /// The one-line hook shown on the card.
  final String teaser;

  /// A cover/image URL — the phone's `imageUrl`, reused as-is.
  final String imageUrl;

  /// Author, artist, director: whoever the entry belongs to.
  final String byline;

  final String subtype;

  /// The action's verb (`Read`, `Listen`, `Watch`, `Explore`), its target,
  /// how long it takes and what to actually do.
  final String verb;
  final String target;
  final int minutes;
  final String instruction;

  final List<String> tags;

  factory Topic.fromJson(Map<String, dynamic> json) {
    final action = json['exploreAction'];
    final explore = action is Map ? action : const <String, dynamic>{};
    return Topic(
      id: _string(json['id']),
      categoryId: _string(json['categoryId']),
      name: _string(json['name']),
      teaser: _string(json['teaser']),
      imageUrl: _string(json['imageUrl']),
      byline: _string(json['byline']),
      subtype: _string(json['subtype']),
      verb: _string(explore['verb']).isEmpty ? 'Explore' : _string(explore['verb']),
      target: _string(explore['targetName']),
      minutes: _int(explore['durationMinutes']),
      instruction: _string(explore['instruction']),
      tags: _tags(json['tags']),
    );
  }

  /// The line under the name on a card: the byline when there is one, the
  /// subtype otherwise, and never an empty row.
  String get credit {
    if (byline.isNotEmpty) return byline;
    if (subtype.isNotEmpty) return subtype;
    return '';
  }

  Map<String, dynamic> toJson() => <String, dynamic>{
        'id': id,
        'categoryId': categoryId,
        'name': name,
        'teaser': teaser,
        'imageUrl': imageUrl,
        'byline': byline,
        'subtype': subtype,
        'exploreAction': <String, dynamic>{
          'verb': verb,
          'targetName': target,
          'durationMinutes': minutes,
          'instruction': instruction,
        },
        'tags': tags,
      };

  static String _string(Object? value) => value is String ? value : '';

  static int _int(Object? value) => value is num ? value.toInt() : 0;

  static List<String> _tags(Object? value) {
    if (value is! List) return const <String>[];
    return value.whereType<String>().toList(growable: false);
  }
}
