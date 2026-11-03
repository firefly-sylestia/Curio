import 'dart:convert';

import 'package:curio/data/lanes.dart';
import 'package:curio/data/topic.dart';
import 'package:flutter_test/flutter_test.dart';

/// The parts of the shell that can be tested without a window: the lane table
/// (generated from the Kotlin catalog, so a drift shows up here) and the catalog
/// parser (a phone topic has to read the same on the desktop).
void main() {
  test('every lane has a unique, non-empty slug and label', () {
    final seen = <String>{};
    for (final lane in kLanes) {
      expect(lane.id, isNotEmpty, reason: 'a lane has no slug');
      expect(lane.label, isNotEmpty, reason: '${lane.id} has no label');
      expect(seen.add(lane.id), isTrue, reason: 'duplicate slug ${lane.id}');
    }
    expect(kLanes.length, 38, reason: 'the Android catalog declares 38 lanes');
  });

  test('laneById falls back to the wildcard for a stale preference', () {
    expect(laneById('books').id, 'books');
    expect(laneById('nonsense-that-was-removed').id, kLanes.last.id);
  });

  test('a phone topic parses into the desktop model', () {
    final raw = jsonDecode('''
    [{
      "id": "book-iliad",
      "categoryId": "BOOKS",
      "name": "The Iliad",
      "teaser": "A long poem about 51 days of the Trojan War.",
      "imageUrl": "https://covers.openlibrary.org/b/isbn/9780140283334-L.jpg",
      "byline": "Homer",
      "subtype": "Epic",
      "tags": ["Epic", "Greek"],
      "exploreAction": {
        "verb": "Read",
        "targetName": "Book I",
        "durationMinutes": 90,
        "instruction": "Read the opening aloud."
      }
    }]''') as List<dynamic>;

    final topic = Topic.fromJson(raw.first as Map<String, dynamic>);
    expect(topic.id, 'book-iliad');
    expect(topic.name, 'The Iliad');
    expect(topic.credit, 'Homer');
    expect(topic.verb, 'Read');
    expect(topic.target, 'Book I');
    expect(topic.minutes, 90);
    expect(topic.tags, ['Epic', 'Greek']);
  });

  test('a topic missing its action still reads, with Explore as the verb', () {
    final topic = Topic.fromJson(<String, dynamic>{
      'id': 'x',
      'categoryId': 'BOOKS',
      'name': 'Untitled',
    });
    expect(topic.name, 'Untitled');
    expect(topic.verb, 'Explore');
    expect(topic.minutes, 0);
    expect(topic.tags, isEmpty);
    expect(topic.credit, isEmpty);
  });
}
