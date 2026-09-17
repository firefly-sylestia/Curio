package com.curio.app.data

/**
 * v389 — THE ONE TOPIC-INDEX SEARCH.
 *
 * Three surfaces ask the merged index the same question — "which topics answer
 * these words?" — the community composer's chooser, the Share Hub's topic
 * picker, and the note-on-a-topic page. The ranking was written for the
 * composer (v385) and lived there as a private helper, so a second surface had
 * to copy it or rank differently: a topic that is easy to find in one place
 * would be invisible in another. It lives here now, and every chooser asks it.
 *
 * The tiers, best first: a NAME PREFIX, then a WORD of the name, then a name
 * substring, then the byline/subtype, then a tag, then the teaser. Within a
 * band the shortest name wins (a query that fits a short name exactly is that
 * topic, not the one whose name merely starts the same way), then the name
 * itself, so the order is stable.
 */
internal fun searchTopicIndex(
    index: List<TopicIndexEntry>,
    query: String,
    limit: Int
): List<TopicIndexEntry> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return index.take(limit)
    val hits = ArrayList<Pair<TopicIndexEntry, Int>>()
    index.forEach { entry ->
        val rank = topicMatchRank(entry, q)
        if (rank >= 0) hits += entry to rank
    }
    return hits
        .sortedWith(
            compareBy<Pair<TopicIndexEntry, Int>> { it.second }
                .thenBy { it.first.name.length }
                .thenBy { it.first.nameKey }
        )
        .take(limit)
        .map { it.first }
}

/**
 * Where [entry] answers [q], lower is better, -1 = no match. 0 name prefix,
 * 1 a word of the name starts with it, 2 the name contains it, 3 byline /
 * subtype, 4 a tag, 5 the teaser.
 */
internal fun topicMatchRank(entry: TopicIndexEntry, q: String): Int {
    val name = entry.nameKey
    if (name.startsWith(q)) return 0
    if (name.contains(" $q")) return 1
    if (name.contains(q)) return 2
    if (entry.bylineKey.contains(q) || entry.subtypeKey.contains(q)) return 3
    if (entry.tagKeys.any { tag -> tag.contains(q) }) return 4
    if (entry.teaserKey.contains(q)) return 5
    return -1
}
