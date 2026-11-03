import 'dart:convert';

import 'package:flutter/services.dart' show rootBundle;

import 'lanes.dart';
import 'topic.dart';

/// The whole topic catalog, one list per lane, loaded from bundled assets.
///
/// The JSON is bundled into `assets/topics/` by `tool/sync_assets.sh` from the
/// repo's canonical `data/topics/` — the same files the Android build copies
/// into its own assets. Nothing here is parsed twice: [load] reads each lane
/// once at boot and the shell works from the lists afterwards.
class TopicCatalog {
  TopicCatalog._(this._byLane);

  final Map<String, List<Topic>> _byLane;

  static Future<TopicCatalog> load() async {
    final byLane = <String, List<Topic>>{};
    for (final lane in kLanes) {
      byLane[lane.id] = await _loadLane(lane.id);
    }
    return TopicCatalog._(byLane);
  }

  /// One lane's topics. A missing or malformed file yields an empty list rather
  /// than an exception: a build that skipped the asset sync still opens (and
  /// says the catalog is empty), which is a better failure than a white window.
  static Future<List<Topic>> _loadLane(String id) async {
    try {
      final raw = await rootBundle.loadString('assets/topics/$id.json');
      final decoded = jsonDecode(raw);
      if (decoded is! List) return const <Topic>[];
      return decoded
          .whereType<Map<String, dynamic>>()
          .map(Topic.fromJson)
          .toList(growable: false);
    } catch (_) {
      return const <Topic>[];
    }
  }

  List<Topic> lane(String id) => _byLane[id] ?? const <Topic>[];

  int get total =>
      _byLane.values.fold<int>(0, (sum, topics) => sum + topics.length);

  bool get isEmpty => total == 0;
}
