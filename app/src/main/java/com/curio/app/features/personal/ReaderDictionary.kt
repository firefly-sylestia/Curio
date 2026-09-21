package com.curio.app.features.personal

import android.net.Uri
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * v431 — THE READER'S DICTIONARY: WIKTIONARY, IN THE APP.
 *
 * The member asked for a word's meaning without leaving the page ("In app
 * wikitionary"), and for a one-word selection to offer that and nothing else
 * ("when user hight one wor only show the dictionarcy icon"). Wiktionary's REST
 * definition endpoint is the right door for it: it is KEYLESS (so it works in
 * every build, with no key to configure and nothing to leak), it answers for the
 * word itself rather than for a list of pages that mention it, and it is the same
 * encyclopaedia family the app already reads through its own Wikipedia door.
 *
 * The rules are the project's own, and they are the reason this is a file rather
 * than a few lines in the reader:
 *
 *  - **One request, one word.** `GET /api/rest_v1/page/definition/<word>`.
 *  - **Nothing is asked twice** — answers AND misses are memoised, so looking the
 *    same word up again as the member scrolls is free.
 *  - **A failure is not an answer** — every call and every parse is wrapped, and
 *    `null` means "the dictionary could not be reached", which the sheet says
 *    differently from "there is no such word" (an empty list).
 *  - **Fail fast.** 4s to connect and 6s to read: a lookup that answers after the
 *    member has turned the page is not a lookup.
 *  - **Only the word-shaped is asked for.** Wiktionary would happily answer for
 *    punctuation and for a whole sentence; a term with anything but letters, a
 *    hyphen or an apostrophe in it is not a dictionary headword.
 */
internal data class ReaderDictionarySense(
    /** "Noun", "Verb", "Adjective" — Wiktionary's own label for the sense group. */
    val partOfSpeech: String,
    /** Up to a handful of senses, already stripped of Wiktionary's markup. */
    val definitions: List<String>
)

internal object ReaderDictionary {

    /** At most this many senses per part of speech, and this many groups. */
    private const val MAX_SENSES = 4
    private const val MAX_GROUPS = 4

    private val cache = ConcurrentHashMap<String, List<ReaderDictionarySense>>()

    /**
     * What [word] means, or null when the dictionary could not be reached.
     *
     * An EMPTY list is a real answer — the word is not in Wiktionary (or is not a
     * word at all) — and a null is not; the sheet draws them differently.
     */
    internal suspend fun define(word: String): List<ReaderDictionarySense>? =
        withContext(Dispatchers.IO) {
            val term = word.trim().lowercase()
            if (term.isBlank() || term.length > 48) return@withContext null
            if (!term.all { it.isLetter() || it == '-' || it == '\'' }) return@withContext null
            cache[term]?.let { return@withContext it }
            // Written as a branch rather than an early `return null` out of the
            // inline lambda: a non-local return makes the compiler emit its
            // `$$$$$NON_LOCAL_RETURN$$$$$` helper class, and R8 refuses to dex
            // that name (see the note in the app's other fetch doors).
            val body = getJson(term)
            if (body == null) {
                null
            } else {
                val senses = parse(body)
                cache[term] = senses
                senses
            }
        }

    /** One GET, best-effort, with a budget a lookup on a page can afford. */
    private fun getJson(term: String): String? = runCatching {
        val url = "https://en.wiktionary.org/api/rest_v1/page/definition/" + Uri.encode(term)
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 4_000
            conn.readTimeout = 6_000
            conn.setRequestProperty("Accept", "application/json")
            // Wiktionary asks for a real agent, and refusing one is its right.
            conn.setRequestProperty("User-Agent", "Curio/1.1 (Android reader dictionary)")
            if (conn.responseCode == 200) {
                conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else {
                null
            }
        } finally {
            conn.disconnect()
        }
    }.getOrNull()

    /**
     * Wiktionary's answer, as the sheet draws it.
     *
     * The payload is keyed by LANGUAGE first (`en` for the English section of the
     * page), then a list of parts of speech, then the senses. Only `en` is read:
     * a member reading an English book does not want the Finnish declension of the
     * same spelling.
     */
    private fun parse(body: String): List<ReaderDictionarySense> {
        val root = runCatching { JSONObject(body) }.getOrNull() ?: return emptyList()
        val english = root.optJSONArray("en") ?: return emptyList()
        val out = ArrayList<ReaderDictionarySense>()
        var i = 0
        while (i < english.length() && out.size < MAX_GROUPS) {
            val group = english.optJSONObject(i)
            i += 1
            if (group == null) continue
            val part = group.optString("partOfSpeech")
            val senses = group.optJSONArray("definitions") ?: continue
            val lines = ArrayList<String>()
            var j = 0
            while (j < senses.length() && lines.size < MAX_SENSES) {
                val entry = senses.optJSONObject(j)
                j += 1
                if (entry == null) continue
                val text = plain(entry.optString("definition"))
                if (text.isNotBlank()) lines.add(text)
            }
            if (lines.isNotEmpty()) out.add(ReaderDictionarySense(part, lines))
        }
        return out
    }

    /**
     * A sense, without the markup it arrives in.
     *
     * Wiktionary hands back HTML — links to the words inside the definition, an
     * italic gloss, a `&nbsp;` here and there — so the tags go, the common
     * entities are decoded, and the runs of whitespace a removed tag leaves behind
     * are collapsed to one space.
     */
    private fun plain(html: String): String = html
        .replace(Regex("<[^>]+>"), " ")
        .replace("&nbsp;", " ")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace(Regex("\\s+"), " ")
        .trim()
}
