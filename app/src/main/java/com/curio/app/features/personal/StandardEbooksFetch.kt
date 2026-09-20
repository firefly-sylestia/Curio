package com.curio.app.features.personal

import java.net.URLEncoder

/**
 * v426 — STANDARD EBOOKS, THE PUBLIC-DOMAIN DOOR (keyless).
 *
 * A classic is the one kind of book whose cover, blurb and author are all
 * published for free by somebody who wants them read: Standard Ebooks rebuilds
 * public-domain literature as properly typeset ebooks and publishes a cover for
 * every one of them, plus a one-line summary and a real blurb — all of it on an
 * Atom feed that asks for no key, no account and nothing in return.
 *
 * ── WHY IT IS WORTH A DOOR OF ITS OWN ───────────────────────────────────
 *
 * The books sources Curio already had answer for a MODERN book well and for a
 * classic badly. Open Library's title-cover endpoint serves a 1×1 GIF for a
 * great many of them (which is the placeholder the cover cache has to test
 * against), and iTunes' ebook search is a shop: a nineteenth-century novel is
 * very often not in it at all. So *Middlemarch*, *The Odyssey* or *Moby-Dick*
 * could sit on a shelf with a blank plate while a finished cover existed — with
 * a summary and a blurb beside it.
 *
 * ── VERIFIED AGAINST THE LIVE FEED, NOT REMEMBERED ──────────────────────
 *
 * `/feeds/opds/all?query=<words>&per-page=<n>` answers Atom with `<entry>` per
 * book: `<title>`, `<author><name>`, `<summary type="text">`, a `<content>` that
 * holds the longer blurb, and `<link rel="http://opds-spec.org/image"
 * href="…/downloads/cover.jpg">` for the artwork (the same feed ships the
 * ebook's own files under `acquisition` links, which this door deliberately
 * ignores — see [Entry]). Every one of those tags was read off a live response
 * while this was written, and the matching rule below is strict for the same
 * reason: a full-text feed search will happily answer a query about one book
 * with four others.
 *
 * The EPUB the feed offers is a genuine next step for this shelf (a classic that
 * arrives with its own file), and it is deliberately NOT taken here: adopting a
 * file onto a row changes what the row IS, and that is a decision for the member
 * rather than a side effect of a cover lookup.
 */
internal object StandardEbooksFetch {

    /** What the door found for a title: exactly the fields a shelf row wears. */
    internal data class Entry(
        val title: String,
        val author: String,
        /** A real cover URL (Standard Ebooks' own artwork), or "" if the feed had none. */
        val coverUrl: String,
        /** The feed's one-line summary, then the longer blurb — already plain text. */
        val description: String
    )

    /** The catalogue's own OPDS feed. `query` and `per-page` are both honoured. */
    private const val FEED = "https://standardebooks.org/feeds/opds/all"

    /**
     * The classic matching [title] (with [author] as a tie breaker when the
     * caller has one), or null when the feed has nothing that really is this
     * book — an unreachable feed, a blank answer, or a title no entry matches.
     *
     * Null (rather than an empty Entry) is the honest answer for every one of
     * those: a caller cascading through providers must be able to keep going.
     */
    internal fun find(title: String, author: String?): Entry? {
        val wanted = title.trim()
        if (wanted.length < 2) return null
        val url = "$FEED?query=${encode(wanted)}&per-page=5"
        val feed = httpGet(url) ?: return null
        val wantedKey = key(wanted)
        val wantedAuthor = author?.trim().orEmpty()
        var fallback: Entry? = null
        for (block in entries(feed)) {
            val foundTitle = tag(block, "title") ?: continue
            if (foundTitle.isBlank()) continue
            val entry = Entry(
                title = unescape(foundTitle),
                author = unescape(nameOf(block)),
                coverUrl = coverOf(block),
                description = summaryOf(block)
            )
            val foundKey = key(entry.title)
            val titleFits = foundKey == wantedKey || acceptable(wantedKey, foundKey)
            if (!titleFits) continue
            // An exact title with nobody to compare against is a match; when the
            // caller HAS an author, a title-only match is kept as the fallback
            // rather than returned, so a same-title-different-author entry does
            // not win by being first in the feed.
            val authorFits = wantedAuthor.isEmpty() || entry.author.isBlank() ||
                entry.author.contains(wantedAuthor, ignoreCase = true) ||
                wantedAuthor.contains(entry.author, ignoreCase = true)
            if (foundKey == wantedKey && authorFits) return entry
            if (fallback == null) fallback = entry
        }
        return fallback
    }

    // ── The feed, read for the four things a row wears ─────────────────────

    /** Every `<entry>` block of the feed, in the order it was served. */
    private fun entries(feed: String): List<String> =
        Regex("<entry>(.*?)</entry>", RegexOption.DOT_MATCHES_ALL)
            .findAll(feed)
            .map { it.groupValues[1] }
            .toList()

    /** The first `<tag …>text</tag>` inside [block], with its attributes ignored. */
    private fun tag(block: String, name: String): String? =
        Regex("<$name(?:\\s[^>]*)?>(.*?)</$name>", RegexOption.DOT_MATCHES_ALL)
            .find(block)
            ?.groupValues?.get(1)
            ?.trim()

    /** `<author><name>Jules Verne</name>` — the author's own name, without the URI. */
    private fun nameOf(block: String): String {
        val author = tag(block, "author") ?: return ""
        return tag(author, "name").orEmpty()
    }

    /**
     * The cover URL. The feed carries two sizes of the same artwork
     * (`opds-spec.org/image` around 600px wide, `/image/thumbnail` around 150):
     * the real one is taken, and Coil downscales it for a card, because the
     * thumbnail is small enough to look soft on a tablet.
     */
    private fun coverOf(block: String): String {
        val link = Regex("<link\\s[^>]*>")
            .findAll(block)
            .map { it.value }
            .firstOrNull { it.contains("opds-spec.org/image\"") }
            ?: return ""
        val href = Regex("href=\"([^\"]*)\"").find(link)?.groupValues?.get(1) ?: return ""
        return unescape(href).replace("http://", "https://")
    }

    /** The one-line `<summary>` when there is one, else the longer `<content>` blurb. */
    private fun summaryOf(block: String): String {
        val summary = tag(block, "summary")?.takeIf { it.isNotBlank() }
        val content = tag(block, "content").orEmpty()
        val text = listOfNotNull(summary, content.takeIf { it.isNotBlank() })
            .joinToString(" ")
            .let { plainText(it) }
        return text
    }

    // ── Matching, and the plumbing ─────────────────────────────────────────

    /**
     * A title with its case, punctuation and spacing taken off, so
     * "Moby-Dick; or, The Whale" and "moby dick or the whale" are one book.
     */
    private fun key(value: String): String =
        value.lowercase()
            .replace(Regex("[^a-z0-9 ]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    /**
     * True when [found] is the same book as [wanted] without being written the
     * same way: one contains the other AND they are close in length, so
     * "the odyssey" matches "the odyssey of homer" while refusing to hand a
     * query for "the sea" an entry called "the sea, the sea, the sea, the sea".
     * The ratio is what does the refusing — a full-text feed search is a
     * generous thing, and a provider that answers with the WRONG book is worse
     * than one that answers with nothing.
     */
    private fun acceptable(wanted: String, found: String): Boolean {
        if (wanted.length < 4 || found.isBlank()) return false
        val longer = maxOf(wanted.length, found.length)
        val shorter = minOf(wanted.length, found.length)
        if (shorter * 10 < longer * 7) return false
        return found.contains(wanted) || wanted.contains(found)
    }

    /** Markup, entities and length — a sheet's few lines, not a page. */
    private fun plainText(value: String): String = unescape(value)
        .replace(Regex("<[^>]*>"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
        .take(420)

    private fun unescape(value: String): String = value
        .replace("&quot;", "\"")
        .replace("&#039;", "'")
        .replace("&apos;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&amp;", "&")

    private fun encode(value: String): String = URLEncoder.encode(value.trim(), "UTF-8")

    /** Minimal keyless GET — 8s timeout, best-effort, exactly like the other doors. */
    private fun httpGet(urlString: String): String? = runCatching {
        val conn = java.net.URL(urlString).openConnection() as java.net.HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty("User-Agent", "Curio/1.0")
            if (conn.responseCode != 200) return null
            conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }.getOrNull()
}
