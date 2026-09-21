package com.curio.app.features.personal

import com.curio.app.data.PersonalKinds
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * v426 — THE COMICS SOURCES.
 *
 * A manga, a manhwa, a manhua or a light novel is NOT in a books catalogue.
 * Open Library has never heard of them — a search there for a volume of a
 * long-running series answers with a study guide about it, or with nothing at
 * all — which is exactly why the kinds the member added needed their own sources
 * rather than a new label on the old ones (member: "keep them as manga or
 * whatever they are called, but add them to be able to add in my shelf").
 *
 * ── KEYLESS FIRST, WHICH IS THE WHOLE DESIGN ────────────────────────────
 *
 * Every source here answers WITHOUT a key or an account, so the shelf works on a
 * fresh install with nothing to configure (the member's own answer on sources:
 * MangaDex, AniList, Jikan and Kitsu are keyless). They are asked IN ORDER and
 * the first one that answers with anything wins, because each is a different
 * database rather than a different page of the same one:
 *
 *  1. **AniList** (`graphql.anilist.co`) — the widest cover set, and the only one
 *     that knows a light novel is a NOVEL rather than a comic.
 *  2. **MangaDex** (`api.mangadex.org`) — where a scanlation lives, so it answers
 *     for series the others have never catalogued.
 *  3. **Jikan** (`api.jikan.moe`) — MyAnimeList's own database, which is gently
 *     rate-limited (about three requests a second), which is why it is not first.
 *  4. **Kitsu** (`kitsu.io/api/edge`) — a last, generous fallback.
 *
 * ── AND THE KEYED DOOR, FOR THE KIND THE KEYLESS FOUR CANNOT ANSWER ─────
 *
 * v426b — a WESTERN COMIC ([PersonalKinds.COMIC]). The four catalogues above
 * are Japanese and Korean
 * databases: a volume of *Watchmen*, *Saga* or *The Sandman* comes back empty
 * from every one of them. So this cascade gained [ComicVineFetch] — the
 * member's own "comic vine (needs a free key)" — and it is asked **FIRST** for
 * a Comic and **LAST** for a manga (a manga's own databases know it better than
 * a Western comics wiki does, while a comic's own wiki is the only thing that
 * has ever heard of it).
 *
 * Marvel's and DC's own APIs are NOT here on purpose: Marvel's was discontinued
 * (its keys answer nothing), DC never published one, and Marvel's scheme wanted
 * a private key shipped in the APK — which is not a private key. Comic Vine
 * carries both publishers, named on every volume.
 *
 * Nothing is ever trusted blindly: a hit is shown in the sheet with what was
 * found and the member taps it, exactly like the books side.
 */
internal object MangaFetch {

    /** One found series: what a shelf row needs, and which source answered. */
    internal data class Hit(
        val title: String,
        val author: String,
        /** A cover URL, empty when the source had none (a generated cover then). */
        val coverUrl: String,
        /** How many chapters the source knows about (0 = unknown). */
        val chapters: Int,
        /** How many volumes (0 = unknown) — a manga's own unit of length. */
        val volumes: Int,
        /** The source's own words, already stripped of markup. */
        val description: String,
        /** The source's name, for the sheet's quiet credit line. */
        val source: String
    )

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    /**
     * v429 — a SHORT budget, because this client is the one an anime's doors
     * share and Jikan can answer `504` from its own gateway: a cascade of four
     * sources at eight seconds each is thirty-two seconds for one cover, which is
     * what a member experiences as "it never loads". Ten seconds for the whole
     * call, a source at a time.
     */
    private val http: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .callTimeout(7, TimeUnit.SECONDS)
            .build()
    }

    /**
     * The series matching [query] for [kind] (a [PersonalKinds] id).
     *
     * Answers the first source's list that is not empty, and NULL only when every
     * source was unreachable — the same contract the books search keeps, so the
     * sheet can tell "nothing found" from "offline" and say the right thing.
     */
    internal fun search(query: String, kind: String): List<Hit>? {
        val wanted = kind.trim().lowercase()
        // v426b — THE KEYED DOOR GOES WHERE IT BELONGS, NOT AT THE END.
        // A Western comic asks Comic Vine first: it is the only source of the
        // five that holds one, and asking a Japanese manga catalogue first
        // spends four requests to be told nothing. A manga asks it last, because
        // its own databases know it better (and its cover set is wider).
        val comicVine: (String) -> List<Hit>? = { text ->
            ComicVineFetch.search(text)?.map { volume -> volume.asHit() }
        }
        val keyless = listOf<(String) -> List<Hit>?>(
            { text -> aniList(text, wanted) },
            ::mangaDex,
            ::jikan,
            ::kitsu
        )
        val comicVineFirst = wanted == PersonalKinds.COMIC
        val sources = if (comicVineFirst) listOf(comicVine) + keyless else keyless + comicVine
        var reached = false
        for (source in sources) {
            val found = runCatching { source(query) }.getOrNull() ?: continue
            reached = true
            if (found.isNotEmpty()) return found
        }
        return if (reached) emptyList() else null
    }

    // ── AniList (GraphQL, keyless) ─────────────────────────────────────────

    private fun aniList(query: String, kind: String): List<Hit>? {
        // A light novel is a MANGA-typed media whose FORMAT is NOVEL; everything
        // else in this family is a comic of some kind. Said in AniList's own
        // terms, rather than asking for both and guessing which is which.
        val format = if (kind == PersonalKinds.LIGHT_NOVEL) ", format_in: [NOVEL]" else ""
        val graph = "query (\$q: String) { Page(perPage: 8) { media(search: \$q, type: MANGA, " +
            "sort: SEARCH_MATCH$format) { title { romaji english } coverImage { large } " +
            "description(asHtml: false) volumes chapters " +
            "staff(perPage: 1) { nodes { name { full } } } } } }"
        val body = JsonObject().apply {
            addProperty("query", graph)
            add("variables", JsonObject().apply { addProperty("q", query) })
        }.toString()
        val request = Request.Builder()
            .url("https://graphql.anilist.co")
            .post(body.toRequestBody(jsonMedia))
            .build()
        val payload = get(request) ?: return null
        val list = payload.asJsonObject
            .getAsJsonObject("data")
            ?.getAsJsonObject("Page")
            ?.getAsJsonArray("media")
            ?: return emptyList()
        return list.mapNotNull { element ->
            val obj = element.asJsonObject
            val title = obj.getAsJsonObject("title")?.let { titles ->
                titles.string("english")?.takeIf { it.isNotBlank() }
                    ?: titles.string("romaji").orEmpty()
            }.orEmpty()
            if (title.isBlank()) return@mapNotNull null
            Hit(
                title = title,
                author = obj.getAsJsonObject("staff")
                    ?.getAsJsonArray("nodes")
                    ?.firstOrNull()
                    ?.asJsonObject
                    ?.getAsJsonObject("name")
                    ?.string("full")
                    .orEmpty(),
                coverUrl = obj.getAsJsonObject("coverImage")?.string("large").orEmpty(),
                chapters = obj.int("chapters"),
                volumes = obj.int("volumes"),
                description = plainText(obj.string("description")),
                source = "AniList"
            )
        }
    }

    // ── MangaDex (keyless) ─────────────────────────────────────────────────

    private fun mangaDex(query: String): List<Hit>? {
        val url = "https://api.mangadex.org/manga?limit=8&order[relevance]=desc" +
            "&includes[]=cover_art&includes[]=author&title=" + encode(query)
        val payload = get(Request.Builder().url(url).get().build()) ?: return null
        val list = payload.asJsonObject.getAsJsonArray("data") ?: return emptyList()
        return list.mapNotNull { element ->
            val obj = element.asJsonObject
            val id = obj.string("id").orEmpty()
            val attributes = obj.getAsJsonObject("attributes") ?: return@mapNotNull null
            // A MangaDex title is a MAP of languages: the English one when there
            // is one, else the first name it holds, else nothing.
            val titles = attributes.getAsJsonObject("title")
            val title = (titles?.string("en")?.takeIf { it.isNotBlank() }
                ?: titles?.entrySet()?.firstOrNull()?.value?.asString).orEmpty()
            if (title.isBlank()) return@mapNotNull null
            val relationships = obj.getAsJsonArray("relationships")
            val cover = relationships?.firstOrNull { relationship ->
                relationship.asJsonObject.string("type") == "cover_art"
            }?.asJsonObject?.getAsJsonObject("attributes")?.string("fileName").orEmpty()
            val author = relationships?.firstOrNull { relationship ->
                relationship.asJsonObject.string("type") == "author"
            }?.asJsonObject?.getAsJsonObject("attributes")?.string("name").orEmpty()
            Hit(
                title = title,
                author = author,
                coverUrl = if (id.isBlank() || cover.isBlank()) ""
                else "https://uploads.mangadex.org/covers/$id/$cover.256.jpg",
                chapters = attributes.text("lastChapter").digits(),
                volumes = attributes.text("lastVolume").digits(),
                description = plainText(
                    attributes.getAsJsonObject("description")?.string("en")
                ),
                source = "MangaDex"
            )
        }
    }

    // ── Jikan — MyAnimeList (keyless, gently paced) ────────────────────────

    private fun jikan(query: String): List<Hit>? {
        val url = "https://api.jikan.moe/v4/manga?limit=8&q=" + encode(query)
        val payload = get(Request.Builder().url(url).get().build()) ?: return null
        val list = payload.asJsonObject.getAsJsonArray("data") ?: return emptyList()
        return list.mapNotNull { element ->
            val obj = element.asJsonObject
            val title = obj.string("title").orEmpty()
            if (title.isBlank()) return@mapNotNull null
            val images = obj.getAsJsonObject("images")?.getAsJsonObject("jpg")
            Hit(
                title = title,
                author = obj.getAsJsonArray("authors")
                    ?.firstOrNull()
                    ?.asJsonObject
                    ?.string("name")
                    .orEmpty(),
                coverUrl = images?.string("large_image_url")?.takeIf { it.isNotBlank() }
                    ?: images?.string("image_url").orEmpty(),
                chapters = obj.int("chapters"),
                volumes = obj.int("volumes"),
                description = plainText(obj.string("synopsis")),
                source = "MyAnimeList"
            )
        }
    }

    // ── Kitsu (keyless) ────────────────────────────────────────────────────

    private fun kitsu(query: String): List<Hit>? {
        val url = "https://kitsu.io/api/edge/manga?page[limit]=8&filter[text]=" + encode(query)
        val payload = get(Request.Builder().url(url).get().build()) ?: return null
        val list = payload.asJsonObject.getAsJsonArray("data") ?: return emptyList()
        return list.mapNotNull { element ->
            val attributes = element.asJsonObject.getAsJsonObject("attributes")
                ?: return@mapNotNull null
            val title = attributes.string("canonicalTitle").orEmpty()
            if (title.isBlank()) return@mapNotNull null
            Hit(
                title = title,
                author = "",
                coverUrl = attributes.getAsJsonObject("posterImage")?.string("large").orEmpty(),
                chapters = attributes.int("chapterCount"),
                volumes = attributes.int("volumeCount"),
                description = plainText(attributes.string("synopsis")),
                source = "Kitsu"
            )
        }
    }

    // ── The shared plumbing ────────────────────────────────────────────────

    /** One GET, parsed — null for an HTTP failure, an empty body or a dead line. */
    private fun get(request: Request): JsonElement? = runCatching {
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string().orEmpty()
            if (body.isBlank()) null else JsonParser.parseString(body)
        }
    }.getOrNull()

    /**
     * A description with its markup and its length taken off: AniList answers
     * with `<br>` and HTML entities, Jikan with a synopsis that can run to a
     * page, and none of that belongs in a sheet's few lines.
     */
    private fun plainText(value: String?): String {
        val text = value?.takeIf { it.isNotBlank() } ?: return ""
        return text.replace(Regex("<[^>]*>"), " ")
            .replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&#039;", "'")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(420)
    }

    private fun encode(value: String): String = URLEncoder.encode(value.trim(), "UTF-8")

    private fun JsonObject.string(key: String): String? =
        get(key)?.takeIf { !it.isJsonNull }?.asString

    private fun JsonObject.int(key: String): Int =
        get(key)?.takeIf { !it.isJsonNull }?.asInt ?: 0

    private fun JsonObject.text(key: String): String = string(key).orEmpty()

    /** A count the source wrote as TEXT ("116", "", "—" or "Unknown") as a number. */
    private fun String.digits(): Int = filter { it.isDigit() }.take(5).toIntOrNull() ?: 0
}
