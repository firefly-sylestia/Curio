package com.curio.app.features.personal

import com.curio.app.data.AppPreferences
import com.curio.app.data.PersonalBookEntity
import com.curio.app.data.PersonalChapter
import com.curio.app.data.PersonalChapterCodec
import com.curio.app.data.PersonalKinds
import com.curio.app.features.reveal.WikipediaSummary
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * THE SHELF'S AUTO-FETCH — what Curio knows about a book, filled in for the
 * member instead of asked of them.
 *
 * A book added from Curio's own lane arrives complete (chapter list, page
 * count, synopsis). One added from Open Library — or typed in by hand — used
 * to arrive as a title with nothing under it: every chapter row said "Chapter
 * 7" and the member had to say how long the book was themselves. This object
 * closes those gaps:
 *
 *  1. THE APP'S OWN CATALOG FIRST. ~800 curated books with real chapter
 *     lists, page counts and synopses, offline and instant. An exact,
 *     punctuation-insensitive title match binds the shelf row to the catalog
 *     book (its `catalogId`), which is also what lets the topic page's book
 *     sheet and the shelf share one set of chapter notes.
 *  2. OPEN LIBRARY, for a book the catalog does not have: a real TABLE OF
 *     CONTENTS (chapter names, and the page range each one spans when the
 *     edition gives it) and the median page count — but only while the
 *     member's book-fetch consent is on. Both places Open Library keeps one
 *     are read: the editions' own, richest table first, and the work's.
 *  3. GOOGLE BOOKS, then CROSSREF, when Open Library has no table at all.
 *     Google Books carries a volume's `tableOfContents` for a good many titles;
 *     Crossref registers every chapter of an academic or edited book as its own
 *     DOI with the pages it spans, which is the only one of the three that
 *     answers for a volume compiled from contributions. All three are keyless,
 *     and all three are held to the same floor: fewer than three titled rows is
 *     not a chapter list.
 *
 *  4. STANDARD EBOOKS (v426b), for the one kind of book every door above
 *     answers badly: a CLASSIC. iTunes' ebook search is a shop that often has no
 *     nineteenth-century novel at all, and Open Library's about-text for one is
 *     frequently absent — while Standard Ebooks publishes a summary and a blurb
 *     for every book it typesets, keyless ([StandardEbooksFetch]). Asked only
 *     when the about-text is still blank after everything above.
 *
 *  5. A COMICS ROW ASKS ITS OWN SOURCES (v426b). A manga, manhwa, manhua, light
 *     novel or comic is not in any of the four doors above — a search of a books
 *     catalogue for a volume of a long series answers with a study guide about
 *     it — so a row of a comics kind is handed back to [MangaFetch] instead
 *     (see [comicsPass]), which is what finally gives a manga or comic added BY
 *     HAND its cover, author, synopsis and length.
 *
 * Everything here is best-effort: a failure leaves the book exactly as it was,
 * and nothing is fetched at all without consent.
 */
internal object BookEnrichment {

    /**
     * What ONE pass over a book learned, so the book's page can say it out
     * loud. "Look it up" used to be a pill that visibly did nothing: the pass
     * ran, found nothing it could add, and reported nothing back. Now every
     * door it opened is named in [learned], and a pass that found nothing
     * while fetching is OFF says so ([needsConsent]) instead of sitting there.
     */
    internal data class EnrichReport(
        val book: PersonalBookEntity,
        /** "12 chapters", "416 pages", "the description" … in pass order. */
        val learned: List<String>,
        val needsConsent: Boolean
    ) {
        /** True when the row has to be written back. */
        val changed: Boolean get() = learned.isNotEmpty()
    }

    /**
     * Fills in what [book] is missing, trying EVERY source: the app's own
     * catalog first, then Open Library's table of contents, page count and
     * description. A catalog match no longer ends the pass — a book Curio
     * knows can still be missing its page count or an about-text.
     */
    suspend fun enrich(book: PersonalBookEntity): EnrichReport {
        val learned = mutableListOf<String>()
        var updated = book

        // v426 — A MANGA IS NOT ASKED OF A BOOKS CATALOGUE.
        //
        // Every door below belongs to the books world: Curio's own lane (~800
        // printed books), Open Library's table of contents, its page count and
        // its description. A manga, manhwa, manhua or light novel is in none of
        // them — a search there for a volume of a long series answers with a
        // study guide about it, or with nothing — so a comics row is left exactly
        // as its own source gave it (its cover, its synopsis and its chapter
        // count came from [MangaFetch] when it was added) instead of being
        // "enriched" with another book's facts.
        if (PersonalKinds.asksComicSources(book.kind)) {
            return comicsPass(book)
        }

        catalogMatch(updated)?.let { matched ->
            updated = matched
            learned += "the catalog's own record"
        }
        // ── WHAT IS ACTUALLY MISSING (v410) ──────────────────────────────
        // Each question is asked ONCE, and only when the book cannot already
        // answer it, so a pass over a complete book opens no socket at all.
        val catalogChapters = if (updated.catalogId.isNotBlank() && updated.chaptersJson.isBlank()) {
            withContext(Dispatchers.IO) {
                runCatching { BookCatalog.chapters(updated.catalogId) }.getOrNull()
            }.orEmpty()
        } else {
            emptyList()
        }
        // THE CATALOG'S OWN CHAPTER LIST IS A CHAPTER LIST (v410). It lives in
        // the topic JSON and is read back by catalogId (see
        // `rememberBookChapters`), so a catalog book has nothing to ask Open
        // Library for — yet this pass asked anyway, on every first open, and
        // spent three requests on a list the app was already holding.
        val wantChapters = updated.chaptersJson.isBlank() && catalogChapters.isEmpty()
        val wantPages = updated.pageCount <= 0
        // A catalog book's about-text is the catalog's own (read live by the
        // book's page), so only a book the catalog does not have asks Open
        // Library for a description.
        val wantDescription = updated.catalogId.isBlank() && updated.synopsis.isBlank()

        // ── ONE OPEN LIBRARY PASS (v410) ─────────────────────────────────
        // Open Library used to be asked THREE separate times for the same
        // title — one search for a table of contents, a second for the page
        // count, a third (plus a second work read) for the description — five
        // sequential requests before Crossref was even reached. It is ONE
        // title search and ONE work read now, and the table of contents, the
        // page count and the description all come out of that single visit
        // (member report: "the look up is slow … the look up should do look up
        // in open library first check all").
        if (wantChapters || wantPages || wantDescription) {
            openLibraryPass(updated.title, updated.author, wantChapters, wantPages, wantDescription)
                ?.let { pass ->
                    pass.chapters?.let { list ->
                        updated = updated.copy(
                            chaptersJson = PersonalChapterCodec.encode(list),
                            totalChapters = if (updated.totalChapters <= 0) list.size
                            else updated.totalChapters
                        )
                        learned += "${list.size} chapters"
                    }
                    pass.pages?.let { pages ->
                        updated = updated.copy(pageCount = pages)
                        learned += "$pages pages"
                    }
                    pass.description?.let { text ->
                        updated = updated.copy(synopsis = text)
                        learned += "the description"
                    }
                }
        }
        // v389e — FREE FIRST, KEYS LAST, and only when Open Library had no
        // table of contents at all. Crossref answers for nothing and covers an
        // academic book's chapters registered as DOIs; Google Books is the one
        // source that FAILS without a key — its anonymous endpoint answers 429
        // ("Quota exceeded … Queries per day", checked live from this repo) — so
        // it is asked LAST and only when a key is actually configured (see
        // googleBooksChapters).
        if (wantChapters && updated.chaptersJson.isBlank()) {
            val chapters = crossrefChapters(updated.title, updated.author)
                ?: googleBooksChapters(updated.title, updated.author)
            chapters?.let { list ->
                updated = updated.copy(
                    chaptersJson = PersonalChapterCodec.encode(list),
                    totalChapters = if (updated.totalChapters <= 0) list.size
                    else updated.totalChapters
                )
                learned += "${list.size} chapters"
            }
        }

        // v426b — THE PUBLIC-DOMAIN DOOR, for the one kind of book every
        // catalogue above answers badly: a classic. iTunes' ebook search is a
        // shop that very often has no nineteenth-century novel at all, and Open
        // Library's about-text for one is frequently missing entirely — while
        // Standard Ebooks publishes a real summary and blurb for every book it
        // typesets, keyless. Asked only when the about-text is STILL blank
        // after everything above, so a modern book never costs a request.
        if (wantDescription && updated.synopsis.isBlank()) {
            val entry = withContext(Dispatchers.IO) {
                runCatching {
                    StandardEbooksFetch.find(updated.title, updated.author)
                }.getOrNull()
            }
            entry?.description?.takeIf { it.length >= MIN_DESCRIPTION }?.let { text ->
                updated = updated.copy(synopsis = text)
                learned += "the description"
            }
        }

        // ── v429 — GOOGLE BOOKS, ASKED FOR EVERYTHING IT STATES ──────────
        //
        // The member: *"wire google book fetching for chapters etc. if nothing
        // returns use more fallbacks"*. Google Books was reached for ONE question
        // (a table of contents) and only through Crossref; its volume record
        // states the page count and the publisher's own blurb as well, which is
        // precisely what a book added by hand is missing. It is asked once,
        // inside this single visit, for whatever is STILL empty — and it stays
        // key-gated, because its anonymous endpoint is a shared daily quota that
        // answers `429 Quota exceeded` (checked live from this repo) and a
        // request answered with an error is worse than no request.
        if ((wantPages && updated.pageCount <= 0) ||
            (wantDescription && updated.synopsis.isBlank())
        ) {
            googleBooksVolume(updated.title, updated.author)?.let { volume ->
                if (wantPages && updated.pageCount <= 0 && volume.pages > 0) {
                    updated = updated.copy(pageCount = volume.pages)
                    learned += "${volume.pages} pages"
                }
                if (wantDescription && updated.synopsis.isBlank()) {
                    volume.description.takeIf { it.length >= MIN_DESCRIPTION }?.let { text ->
                        updated = updated.copy(synopsis = text)
                        learned += "the description"
                    }
                }
            }
        }

        // ── v429 — AND THE ENCYCLOPAEDIA LAST ────────────────────────────
        //
        // When every catalogue above has answered nothing, the book is usually one
        // they file under a DIFFERENT name than the shelf holds — which is exactly
        // the miss a search over an encyclopaedia fixes. Wikipedia is the widest
        // net of the set, keyless, and its article for a novel states the plot in
        // prose; `Kind.BOOK` is what keeps "Dune" from being answered with the
        // FILM's article.
        if (wantDescription && updated.synopsis.isBlank()) {
            val text = withContext(Dispatchers.IO) {
                runCatching {
                    WikipediaSummary.extract(updated.title, null, WikipediaSummary.Kind.BOOK)
                }.getOrNull()
            }
            text?.takeIf { it.length >= MIN_DESCRIPTION }?.let { found ->
                updated = updated.copy(synopsis = found)
                learned += "the description"
            }
        }

        return EnrichReport(
            book = updated,
            learned = learned,
            needsConsent = learned.isEmpty() &&
                updated.catalogId.isBlank() &&
                !AppPreferences.bookFetchEnabledState
        )
    }

    /**
     * v426b — A COMICS ROW ASKS ITS OWN SOURCES, and asks them for what the row
     * is MISSING.
     *
     * The books doors below are wrong for a manga or a comic (a search of Open
     * Library for a volume of a long series answers with a study guide about it),
     * which is why this used to return the row untouched. That was right about the
     * doors and wrong about the row: a manga or a comic added BY HAND — its own
     * title typed, or read out of a file name — never went through a search at
     * all, so it kept a blank plate and no about-text forever (the member's own
     * note: "a manga added BY HAND still cannot fetch its own cover").
     *
     * So the row is handed back to the sources it actually belongs to
     * ([MangaFetch] — its keyless four, and Comic Vine for a Western comic, asked
     * for its own kind), and the FIRST entry that really matches the row's title
     * fills in whatever the row does not have.
     *
     * ── NOTHING IS EVER OVERWRITTEN ────────────────────────────────────
     * Every field is filled only while it is EMPTY: the cover the member chose
     * (or the one a search already found), the synopsis they wrote into, the
     * chapter count of the edition they are reading — all of it stays exactly as
     * it is. A pass adds; it never replaces.
     */
    private suspend fun comicsPass(book: PersonalBookEntity): EnrichReport {
        if (book.title.isBlank()) {
            return EnrichReport(book = book, learned = emptyList(), needsConsent = false)
        }
        if (!AppPreferences.bookFetchEnabledState) {
            return EnrichReport(book = book, learned = emptyList(), needsConsent = true)
        }
        val wanted = normalise(book.title)
        val found = withContext(Dispatchers.IO) {
            runCatching { MangaFetch.search(book.title, book.kind) }.getOrNull()
        }?.let { hits ->
            hits.firstOrNull { normalise(it.title) == wanted } ?: hits.firstOrNull()
        } ?: return EnrichReport(book = book, learned = emptyList(), needsConsent = false)

        val learned = mutableListOf<String>()
        var updated = book
        if (updated.coverUrl.isBlank() && found.coverUrl.isNotBlank()) {
            updated = updated.copy(coverUrl = found.coverUrl)
            learned += "the cover"
        }
        if (updated.author.isBlank() && found.author.isNotBlank()) {
            updated = updated.copy(author = found.author)
            learned += "the author"
        }
        if (updated.synopsis.isBlank() && found.description.isNotBlank()) {
            updated = updated.copy(synopsis = found.description)
            learned += "the description"
        }
        // A comics row counts in CHAPTERS (a manga's own unit is its chapter),
        // and volume counts are only used when the source gave no chapter count.
        val length = if (found.chapters > 0) found.chapters else found.volumes
        if (updated.totalChapters <= 0 && length > 0) {
            updated = updated.copy(totalChapters = length)
            learned += if (found.chapters > 0) "$length chapters" else "$length volumes"
        }
        return EnrichReport(book = updated, learned = learned, needsConsent = false)
    }

    /** What ONE Open Library visit learned — any of the three may be absent. */
    private data class OpenLibraryPass(
        val chapters: List<PersonalChapter>?,
        val pages: Int?,
        val description: String?
    )

    /**
     * v410 — ONE VISIT TO OPEN LIBRARY, ANSWERING EVERYTHING.
     *
     * The three functions this replaces each went to the catalogue on their own:
     * `openLibraryChapters` did its own title search (plus an editions read and
     * sometimes a work read), `openLibraryPages` did a SECOND search, and
     * `openLibraryDescription` did a THIRD search and its own work read. Five
     * sequential round trips for facts that live in one search hit and one work
     * document — which is exactly the "the look up is slow" the member reported.
     *
     * So: the title is searched ONCE; the work the search matched is read ONCE,
     * and only when the description or a table of contents is actually wanted;
     * the editions are read at most once, and only for a table of contents. The
     * three questions are still Open Library's THREE answers — the description
     * (the work's own about-text, an object or a string, with the first sentence
     * standing in), the median page count the search reports, and the work's or
     * its editions' table of contents — they just arrive from one visit.
     *
     * Null when consent is off, when nothing matched the title, or when the
     * catalogue could not be reached.
     */
    private suspend fun openLibraryPass(
        title: String,
        author: String,
        wantChapters: Boolean,
        wantPages: Boolean,
        wantDescription: Boolean
    ): OpenLibraryPass? {
        if (title.isBlank()) return null
        if (!AppPreferences.bookFetchEnabledState) return null
        return withContext(Dispatchers.IO) {
            runCatching {
                val wanted = normalise(title)
                // ONE search, carrying every field the three questions need.
                val match = getJson(
                    searchUrl(title, author, 5, "key,title,number_of_pages_median")
                )?.let { runCatching { it.asJsonObject }.getOrNull() }
                    ?.array("docs")
                    .orEmpty()
                    .mapNotNull { it as? JsonObject }
                    .firstOrNull {
                        normalise(it.str("title")) == wanted && it.str("key").isNotBlank()
                    }
                    ?: return@runCatching null
                val workKey = match.str("key")
                val isWork = workKey.startsWith("/works/")
                // ONE work read, and only when something needs it: the
                // description lives on the work, and so does the table of
                // contents its editions may not carry.
                val work = if (isWork && (wantDescription || wantChapters)) {
                    getJson("https://openlibrary.org$workKey.json")
                        ?.let { runCatching { it.asJsonObject }.getOrNull() }
                } else {
                    null
                }
                val pages = if (wantPages) {
                    match.get("number_of_pages_median")
                        ?.takeIf { !it.isJsonNull }
                        ?.let { runCatching { it.asInt }.getOrNull() }
                        ?.takeIf { it > 0 }
                } else {
                    null
                }
                val description = if (wantDescription && work != null) {
                    val text = work.descriptionText().ifBlank { work.str("first_sentence") }
                    text.trim().takeIf { it.length >= MIN_DESCRIPTION }
                } else {
                    null
                }
                // v389d — THE RICHEST TABLE WINS, and the WORK is the second
                // door: an edition's table is often a bare "Contents" stub while
                // a sibling edition carries the book's real chapter list, and a
                // good many works keep the only table anyone entered on the WORK
                // itself. Fewer than [MIN_CHAPTERS] rows is not a chapter list.
                val chapters = if (wantChapters && isWork) {
                    val table = richestEditionTable(workKey)
                        ?: work?.tableOfContents()?.takeIf { it.size >= MIN_CHAPTERS }
                    table?.mapIndexed { index, chapter -> chapter.copy(number = index + 1) }
                } else {
                    null
                }
                OpenLibraryPass(chapters = chapters, pages = pages, description = description)
            }.getOrNull()
        }
    }

    /**
     * The fullest table of contents among a work's EDITIONS (v389d).
     *
     * Open Library keeps a table of contents per edition, most editions have
     * none, and an edition's table is often a bare "Contents" line — so every
     * candidate is kept and the fullest one wins, with a table that carries page
     * ranges preferred over one that has only names. Null when no edition has a
     * real one.
     */
    private fun richestEditionTable(workKey: String): List<PersonalChapter>? {
        val editions = getJson("https://openlibrary.org$workKey/editions.json?limit=50")
            ?.let { runCatching { it.asJsonObject }.getOrNull() }
            ?.array("entries")
            .orEmpty()
        return editions
            .mapNotNull { entry -> (entry as? JsonObject)?.tableOfContents() }
            .filter { it.size >= MIN_CHAPTERS }
            .maxByOrNull { table -> table.count { it.pageStart > 0 } * 100 + table.size }
    }

    /** `description`, which Open Library writes as a string OR an object. */
    private fun JsonObject.descriptionText(): String {
        val value = get("description") ?: return ""
        if (value.isJsonNull) return ""
        return runCatching {
            if (value.isJsonObject) {
                value.asJsonObject.get("value")?.takeIf { !it.isJsonNull }?.asString.orEmpty()
            } else {
                value.asString
            }
        }.getOrDefault("")
    }

    /**
     * Binds a hand-added book to the catalog's own record of it (its topic id,
     * chapters, pages, cover and author). Null when the catalog does not have
     * the book, or when the row is already bound.
     */
    suspend fun catalogMatch(book: PersonalBookEntity): PersonalBookEntity? {
        if (book.catalogId.isNotBlank()) return null
        val hit = BookCatalog.bestMatch(book.title, book.author) ?: return null
        return book.copy(
            catalogId = hit.topicId,
            totalChapters = if (book.totalChapters <= 0) hit.chapterCount else book.totalChapters,
            pageCount = if (book.pageCount <= 0) hit.pageCount else book.pageCount,
            coverUrl = book.coverUrl.ifBlank { hit.coverUrl },
            author = book.author.ifBlank { hit.author }
        )
    }

    /**
     * v389d — THE THIRD SOURCE: CROSSREF, for the books that keep their chapters
     * in an academic record rather than in a table of contents page.
     *
     * Crossref registers every chapter of an edited or academic book as its own
     * DOI, with the pages it spans, which is a chapter list nobody had to type
     * into a table of contents. Two keyless calls, both in Crossref's free public
     * pool:
     *
     *  1. the BOOK, so the chapter query can be scoped to it — a bare
     *     `type:book-chapter` search would mix every other book's chapters in;
     *  2. its chapters, as `type:book-chapter,container-title:<the book>`,
     *     ordered by the page each one starts on.
     *
     * Null unless a book matches by title AND returns at least [MIN_CHAPTERS]
     * chapters that actually have titles — the same floor the other two sources
     * are held to.
     */
    private suspend fun crossrefChapters(
        title: String,
        author: String
    ): List<PersonalChapter>? = withContext(Dispatchers.IO) {
        if (title.isBlank()) return@withContext null
        runCatching {
            val wanted = normalise(title)
            val bookQuery = buildString {
                append("https://api.crossref.org/works?rows=3&filter=type:book")
                append("&select=title,container-title")
                append("&query.bibliographic=")
                append(java.net.URLEncoder.encode("$title $author".trim(), "UTF-8"))
            }
            val bookJson = getJson(bookQuery)?.let { runCatching { it.asJsonObject }.getOrNull() }
                ?: return@runCatching null
            val items = bookJson.getAsJsonObject("message")?.array("items").orEmpty()
            // The container the chapters are registered under — the book's own
            // title AS CROSSREF HAS IT, because that exact string is what the
            // filter has to match. Only a title that normalises to the one being
            // looked up may stand in for it, or the chapters of a different book
            // would be read as this one's.
            val container = items
                .mapNotNull { it as? JsonObject }
                .mapNotNull { doc ->
                    doc.array("title").firstOrNull()
                        ?.let { value -> runCatching { value.asString }.getOrNull() }
                        ?.trim()
                        ?.takeIf { it.isNotBlank() }
                }
                .firstOrNull { normalise(it) == wanted }
                ?: return@runCatching null

            val chaptersUrl = buildString {
                append("https://api.crossref.org/works?rows=100&select=title,page")
                append("&filter=type:book-chapter,container-title:")
                append(java.net.URLEncoder.encode(container, "UTF-8"))
            }
            val chaptersJson = getJson(chaptersUrl)
                ?.let { runCatching { it.asJsonObject }.getOrNull() }
                ?: return@runCatching null
            val rows = chaptersJson.getAsJsonObject("message")?.array("items").orEmpty()
            rows.mapNotNull { row ->
                val entry = row as? JsonObject ?: return@mapNotNull null
                val name = entry.array("title").firstOrNull()
                    ?.let { runCatching { it.asString }.getOrNull() }
                    ?.trim()
                    .orEmpty()
                if (name.isBlank()) return@mapNotNull null
                val (start, end) = pagesOf(entry.str("page"))
                PersonalChapter(number = 0, title = name, pageStart = start, pageEnd = end)
            }
                .takeIf { it.size >= MIN_CHAPTERS }
                // Registration order is not reading order: the page each
                // chapter starts on is.
                ?.sortedBy { if (it.pageStart > 0) it.pageStart else Int.MAX_VALUE }
                ?.mapIndexed { index, chapter -> chapter.copy(number = index + 1) }
        }.getOrNull()
    }

    /** One edition's table of contents as chapters (numbers assigned later). */
    private fun JsonObject.tableOfContents(): List<PersonalChapter>? {
        val rows = array("table_of_contents")
        if (rows.isEmpty()) return null
        val chapters = rows.mapNotNull { row ->
            val entry = row as? JsonObject ?: return@mapNotNull null
            val name = entry.str("title")
            if (name.isBlank()) return@mapNotNull null
            val (start, end) = pagesOf(entry.str("pagination"))
            PersonalChapter(number = 0, title = name, pageStart = start, pageEnd = end)
        }
        return chapters.takeIf { it.isNotEmpty() }
    }

    /** \"121-154\" / \"121\" / \"\" → the pages a chapter spans. */
    private fun pagesOf(pagination: String): Pair<Int, Int> {
        val numbers = Regex("\\d+").findAll(pagination).map { it.value.toInt() }.toList()
        return when {
            numbers.isEmpty() -> 0 to 0
            numbers.size == 1 -> numbers[0] to numbers[0]
            else -> numbers.min() to numbers.max()
        }
    }

    /**
     * v392 — GOOGLE BOOKS CHAPTER FALLBACK.
     *
     * When Open Library has no table of contents for a book, Google Books
     * sometimes does (its `volumeInfo.tableOfContents`).
     *
     * v389e — AND IT NEEDS THE KEY NOW, on purpose. The anonymous endpoint is a
     * shared daily quota, it is very often spent (a live check from this repo
     * answered `429 Quota exceeded … Queries per day` for the keyless URL), and a
     * request that is answered with an error is worse than no request: it costs
     * the member a wait and looks like a broken feature. So this asks only when
     * [com.curio.app.BuildConfig.GOOGLE_BOOKS_API_KEY] is set, and the answer no
     * longer depends on a quota nobody controls.
     */
    private suspend fun googleBooksChapters(
        title: String, author: String
    ): List<PersonalChapter>? = googleBooksVolume(title, author)?.chapters

    /**
     * v429 — WHAT ONE GOOGLE BOOKS VOLUME STATES, for every question it can
     * answer: the table of contents, the printed page count and the publisher's
     * blurb. The record is read ONCE per book (see the pass above), so the three
     * questions cost one request rather than three.
     */
    private data class GoogleVolume(
        val chapters: List<PersonalChapter>?,
        val pages: Int,
        val description: String
    )

    /**
     * One Google Books search for a title, best-effort, key-gated.
     *
     * The KEY IS REQUIRED on purpose (see [googleBooksChapters]): the anonymous
     * endpoint answers `429 Quota exceeded … Queries per day`, so a keyless build
     * makes no request here and the failover chain above is what it always was.
     */
    private suspend fun googleBooksVolume(
        title: String, author: String
    ): GoogleVolume? {
        if (title.isBlank()) return null
        if (com.curio.app.BuildConfig.GOOGLE_BOOKS_API_KEY.isBlank()) return null
        if (!AppPreferences.bookFetchEnabledState) return null
        return withContext(Dispatchers.IO) {
            runCatching {
                val q = buildString {
                    append("intitle:")
                    append(java.net.URLEncoder.encode(title, "UTF-8"))
                    if (author.isNotBlank()) {
                        append("+inauthor:")
                        append(java.net.URLEncoder.encode(author, "UTF-8"))
                    }
                }
                val key = com.curio.app.BuildConfig.GOOGLE_BOOKS_API_KEY
                val url = "https://www.googleapis.com/books/v1/volumes?q=$q&maxResults=3" +
                    "&key=$key"
                val json = getJson(url) ?: return@runCatching null
                val wanted = normalise(title)
                val items = json.asJsonObject.array("items")
                    .mapNotNull { (it as? JsonObject)?.getAsJsonObject("volumeInfo") }
                // The closest title first: a search for a common word can lead
                // with a different book entirely, and its page count and blurb
                // would then be attached to this one.
                val ranked = items.sortedByDescending { vol ->
                    val name = normalise(vol.str("title"))
                    when {
                        name == wanted -> 2
                        name.contains(wanted) || wanted.contains(name) -> 1
                        else -> 0
                    }
                }
                val best = ranked.firstOrNull() ?: return@runCatching null
                val chapters = best.getAsJsonArray("tableOfContents")?.mapNotNull { entry ->
                    val name = runCatching { entry.asString }.getOrDefault("")
                    if (name.isBlank()) null
                    else PersonalChapter(number = 0, title = name, pageStart = 0, pageEnd = 0)
                }?.takeIf { it.size >= MIN_CHAPTERS }
                    ?.mapIndexed { i, ch -> ch.copy(number = i + 1) }
                GoogleVolume(
                    chapters = chapters,
                    pages = best.str("pageCount").toIntOrNull()?.takeIf { it > 0 } ?: 0,
                    description = best.str("description").trim()
                )
            }.getOrNull()
        }
    }

    /** One Open Library title search, encoded once for every caller. */
    private fun searchUrl(
        title: String,
        author: String,
        limit: Int,
        fields: String
    ): String = buildString {
        append("https://openlibrary.org/search.json?title=")
        append(java.net.URLEncoder.encode(title, "UTF-8"))
        if (author.isNotBlank()) {
            append("&author=")
            append(java.net.URLEncoder.encode(author, "UTF-8"))
        }
        append("&limit=$limit&fields=$fields")
    }

    private fun getJson(url: String): com.google.gson.JsonElement? = runCatching {
        val request = Request.Builder().url(url).get().build()
        val body = http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@use ""
            response.body?.string().orEmpty()
        }
        if (body.isBlank()) null else JsonParser.parseString(body)
    }.getOrNull()

    /** Letters and digits only, lowercased — the title match's own rule. */
    private fun normalise(value: String): String =
        value.lowercase().filter { it.isLetterOrDigit() }

    private fun JsonObject.str(key: String): String {
        val value = get(key) ?: return ""
        return if (value.isJsonNull) "" else runCatching { value.asString }.getOrDefault("")
    }

    /** A JSON array read as a list; an absent or odd value reads as empty. */
    private fun JsonObject.array(key: String): List<com.google.gson.JsonElement> =
        runCatching { getAsJsonArray(key)?.toList() }.getOrNull().orEmpty()

    /** Three rows is the floor for a real chapter list. */
    private const val MIN_CHAPTERS = 3

    /** Below this many characters it is a tagline, not a description. */
    private const val MIN_DESCRIPTION = 60

    private val http: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()
    }
}
