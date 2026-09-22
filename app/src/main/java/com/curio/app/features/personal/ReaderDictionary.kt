package com.curio.app.features.personal

import android.net.Uri
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
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

/**
 * v442 — A WORD A PASSAGE SUGGESTS, AND THE SENTENCE IT CAME FROM.
 *
 * A selection that is a whole passage has no single word to look up, so the sheet
 * OFFERS the words worth asking about instead of an empty field (the member:
 * *"improve the dictionary that it suggest word explanation from the selected
 * para"*). The sentence rides along because a meaning read beside the line it
 * appeared in is the difference between a dictionary and an answer (the member's
 * own pick: "chips + context line").
 */
internal data class ReaderDictionaryWord(
    /** The cleaned headword, with the passage's punctuation already off it. */
    val word: String,
    /** The one sentence the word stands in, for the sheet's context line. */
    val sentence: String
)

/**
 * v440 — WHICH DICTIONARY ANSWERS.
 *
 * Two online doors, chosen by the member in reading settings. It is a two-way
 * choice rather than a switch because both are real answers to the same question
 * — see [ReaderDictionary.define] for why a member would want either, and why a
 * failure in one is never quietly answered by the other.
 */
internal enum class ReaderDictionarySource(val key: String, val label: String) {
    WIKTIONARY("wiktionary", "Wiktionary"),
    FREE("free", "Free dictionary");

    companion object {
        /** The stored key back to the choice, with the default for anything unknown. */
        fun fromKey(key: String?): ReaderDictionarySource =
            entries.firstOrNull { it.key == key } ?: WIKTIONARY
    }
}

internal object ReaderDictionary {

    /** At most this many senses per part of speech, and this many groups. */
    private const val MAX_SENSES = 4
    private const val MAX_GROUPS = 4

    /** At most this many offered words from one passage, and this many spellings. */
    private const val MAX_SUGGESTIONS = 8
    private const val MAX_GUESSES = 4

    private val cache = ConcurrentHashMap<String, List<ReaderDictionarySense>>()

    /** Spelling suggestions, memoised like the senses are (see [suggest]). */
    private val guesses = ConcurrentHashMap<String, List<String>>()

    /**
     * v442 — THE HEADWORD OUT OF WHAT WAS ACTUALLY SELECTED.
     *
     * A sweep over a page does not stop politely at the end of a word: it carries
     * the comma, the full stop, the quote, the bracket that happens to sit beside
     * it, and sometimes the possessive ("Einstein's"). None of those are part of
     * the word, and asking Wiktionary for "Einstein," is asking for a headword
     * that does not exist — which is exactly the member's report (*"make the word
     * detection better it detects the word even theres a comma or something"*).
     *
     * So the edges are stripped — any run of punctuation at either end, the
     * typographic quotes and dashes included — while everything INSIDE the word is
     * left alone, because a hyphen and an apostrophe are real parts of real words
     * (`well-known`, `don't`). The possessive tail goes too: "Einstein's" is looked
     * up as "Einstein", which is the headword that has the answer.
     */
    internal fun headword(raw: String): String {
        var term = raw.trim()
        while (term.isNotEmpty() && term.first() in EDGE_PUNCTUATION) term = term.drop(1)
        while (term.isNotEmpty() && term.last() in EDGE_PUNCTUATION) term = term.dropLast(1)
        term = term.trim()
        // A possessive or a plural possessive: the apostrophe and what follows it
        // are the selection's grammar, not the word's spelling.
        val tail = term.indexOfLast { it == '\'' || it == '\u2019' }
        // A three-letter stem minimum: "Einstein's" is a headword's possessive,
        // while "it's" and "he's" are contractions whose own page has the answer.
        if (tail > 2 && term.substring(tail + 1).lowercase() in POSSESSIVE_TAILS) {
            term = term.substring(0, tail)
        }
        return term.trim()
    }

    /**
     * v442 — WHAT ELSE IT MIGHT HAVE BEEN.
     *
     * A lookup that finds nothing is usually a spelling, and a reader who mistyped
     * a word while sweeping a page should not have to guess which letter was wrong
     * (the member: *"or a mis type"*). Wiktionary's own search is the honest door
     * for that — it is the same keyless family the definitions come from, and it
     * answers with the page names nearest what was asked, which is a spell-check
     * built from the dictionary itself rather than a second service to trust.
     *
     * Only word-shaped answers are offered, the word asked for is never echoed
     * back as a "suggestion", and misses are memoised like the senses.
     */
    internal suspend fun suggest(term: String): List<String> =
        withContext(Dispatchers.IO) {
            val asked = headword(term).lowercase()
            if (asked.isEmpty() || asked.length > 48) return@withContext emptyList()
            guesses[asked]?.let { return@withContext it }
            val body = runCatching {
                val url = "https://en.wiktionary.org/w/api.php?action=opensearch&format=json" +
                    "&namespace=0&limit=" + MAX_GUESSES + "&search=" + Uri.encode(asked)
                val conn = URL(url).openConnection() as HttpURLConnection
                try {
                    conn.requestMethod = "GET"
                    conn.connectTimeout = 4_000
                    conn.readTimeout = 6_000
                    conn.setRequestProperty("Accept", "application/json")
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
            val parsed = if (body == null) {
                emptyList()
            } else {
                val out = ArrayList<String>()
                val names = runCatching { JSONArray(body).optJSONArray(1) }.getOrNull()
                var i = 0
                while (names != null && i < names.length() && out.size < MAX_GUESSES) {
                    val name = headword(names.optString(i))
                    i += 1
                    if (name.length < 2) continue
                    if (!name.all { it.isLetter() || it == '-' || it == '\'' }) continue
                    if (name.lowercase() == asked) continue
                    if (out.any { it.equals(name, ignoreCase = true) }) continue
                    out.add(name)
                }
                out
            }
            guesses[asked] = parsed
            parsed
        }

    /**
     * v442 — THE WORDS A PASSAGE IS WORTH ASKING ABOUT.
     *
     * A selected passage offers its NOTABLE words: long enough to be worth a
     * definition (five letters up), not one of the words that carry a sentence
     * rather than mean anything in it, and never the same word twice — so a
     * paragraph about one idea suggests the few words around it rather than eight
     * copies of the same one. The sheet draws them as chips (see
     * [ReaderDictionarySheet]) and each one carries the sentence it stood in.
     */
    internal fun wordsIn(passage: String): List<ReaderDictionaryWord> {
        if (passage.isBlank()) return emptyList()
        val out = LinkedHashMap<String, ReaderDictionaryWord>()
        for (match in SENTENCE.findAll(passage)) {
            val sentence = match.value.replace(WHITESPACE, " ").trim()
            if (sentence.isEmpty()) continue
            for (found in WORD.findAll(sentence)) {
                val word = headword(found.value)
                val key = word.lowercase()
                if (word.length < 5) continue
                if (key in STOP_WORDS) continue
                if (out.containsKey(key)) continue
                if (out.size >= MAX_SUGGESTIONS) break
                out[key] = ReaderDictionaryWord(word, sentence)
            }
            if (out.size >= MAX_SUGGESTIONS) break
        }
        return out.values.toList()
    }

    /**
     * What [word] means, from the source the member chose (see
     * [ReaderDictionarySource]), or null when that source could not be reached.
     *
     * An EMPTY list is a real answer — the word is not in that dictionary (or is
     * not a word at all) — and a null is not; the sheet draws them differently.
     */
    internal suspend fun define(word: String): List<ReaderDictionarySense>? =
        define(word, ReaderLook.dictionary)

    /**
     * v440 — THE SAME LOOKUP, AGAINST A NAMED SOURCE.
     *
     * The member, asked what "bundled vs online" should mean once they heard there
     * is no bundled dictionary to switch to: **two online sources to choose
     * between**. Two keyless doors rather than one, because a reader looking up a
     * word is already on a page they did not want to leave: Wiktionary answers for
     * the word itself and carries the best senses for older and literary words,
     * while the free dictionary API answers faster and covers modern usage better.
     * Neither needs a key, so neither can leak one or expire.
     *
     * **A failure is never quietly answered by the OTHER source.** The member chose
     * one; if it cannot be reached the sheet says so (see [ReaderDictionarySheet]),
     * which is the difference between a source that is down and a word that does not
     * exist.
     */
    internal suspend fun define(
        word: String,
        source: ReaderDictionarySource
    ): List<ReaderDictionarySense>? =
        withContext(Dispatchers.IO) {
            // v442 — the word as it WAS WRITTEN is not always the word to ask
            // for: the selection carries its punctuation (see [headword]).
            val term = headword(word).lowercase()
            if (term.isEmpty() || term.length > 48) return@withContext null
            if (!term.all { it.isLetter() || it == '-' || it == '\'' }) return@withContext null
            // The cache is keyed by SOURCE and term: the same word has a different
            // answer in each dictionary, and a shared key would hand one source's
            // senses to the other.
            val key = source.key + ":" + term
            cache[key]?.let { return@withContext it }
            // Written as a branch rather than an early `return null` out of the
            // inline lambda: a non-local return makes the compiler emit its
            // `$$$$$NON_LOCAL_RETURN$$$$$` helper class, and R8 refuses to dex
            // that name (see the note in the app's other fetch doors).
            val body = when (source) {
                ReaderDictionarySource.WIKTIONARY -> getJson(term)
                ReaderDictionarySource.FREE -> getFreeJson(term)
            }
            if (body == null) {
                null
            } else {
                val senses = when (source) {
                    ReaderDictionarySource.WIKTIONARY -> parse(body)
                    ReaderDictionarySource.FREE -> parseFree(body)
                }
                cache[key] = senses
                senses
            }
        }

    /**
     * The second door: dictionaryapi.dev, the free no-key English dictionary.
     *
     * Same budget and the same best-effort contract as [getJson] — a lookup that
     * answers after the member has turned the page is not a lookup.
     */
    private fun getFreeJson(term: String): String? = runCatching {
        val url = "https://api.dictionaryapi.dev/api/v2/entries/en/" + Uri.encode(term)
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 4_000
            conn.readTimeout = 6_000
            conn.setRequestProperty("Accept", "application/json")
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
     * dictionaryapi.dev's answer, as the sheet draws it.
     *
     * The payload is an ARRAY of entries for the one word, each with `meanings`
     * (one per part of speech) holding `definitions`. Definitions arrive as PLAIN
     * text here rather than as HTML, so they are trimmed and left alone.
     */
    private fun parseFree(body: String): List<ReaderDictionarySense> {
        val root = runCatching { JSONArray(body) }.getOrNull() ?: return emptyList()
        val out = ArrayList<ReaderDictionarySense>()
        var i = 0
        while (i < root.length() && out.size < MAX_GROUPS) {
            val entry = root.optJSONObject(i)
            i += 1
            val meanings = entry?.optJSONArray("meanings") ?: continue
            var m = 0
            while (m < meanings.length() && out.size < MAX_GROUPS) {
                val meaning = meanings.optJSONObject(m)
                m += 1
                if (meaning == null) continue
                val part = meaning.optString("partOfSpeech")
                val definitions = meaning.optJSONArray("definitions") ?: continue
                val lines = ArrayList<String>()
                var j = 0
                while (j < definitions.length() && lines.size < MAX_SENSES) {
                    val definition = definitions.optJSONObject(j)
                    j += 1
                    val text = definition?.optString("definition").orEmpty().trim()
                    if (text.isNotBlank()) lines.add(text)
                }
                if (lines.isNotEmpty()) out.add(ReaderDictionarySense(part, lines))
            }
        }
        return out
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

    /**
     * Everything that is not part of a word when it sits at one of its EDGES:
     * the sentence's punctuation, both quote families, both dash families and the
     * brackets. Deliberately not the hyphen or the apostrophe — those are letters'
     * business inside a word (see [headword]).
     */
    private const val EDGE_PUNCTUATION =
        "\"'\u2018\u2019\u201C\u201D.,;:!?()[]{}\u00AB\u00BB/\\*_~`|<>–—… "

    /** The tails that are a word's grammar rather than its spelling. */
    private val POSSESSIVE_TAILS = setOf("s", "es")

    /**
     * A word as the sweep may find it — `[A-Za-z]`, and an apostrophe or hyphen
     * INSIDE it. Four characters minimum, so the short words a sentence is made of
     * are not offered as lookups.
     */
    private val WORD = Regex("[A-Za-z][A-Za-z'\\-]{3,}")

    /** One sentence at a time, for the context line beside a meaning. */
    private val SENTENCE = Regex("[^.!?]+[.!?]*")

    private val WHITESPACE = Regex("\\s+")

    /**
     * The words that carry a sentence rather than mean anything in it: offering
     * "there" or "would" as a lookup would spend the chips on the page's grammar.
     * Kept to the commonest function words and the reading-verbs a passage of prose
     * is built from, so the list stays short enough to read and long enough to hide
     * the noise.
     */
    private val STOP_WORDS = setOf(
        "about", "after", "again", "against", "almost", "along", "already",
        "although", "always", "among", "another", "anything", "around",
        "because", "became", "become", "before", "behind", "being", "below",
        "beside", "better", "between", "beyond", "bring", "came", "cannot",
        "could", "course", "doing", "down", "during", "each", "either",
        "enough", "even", "ever", "every", "everything", "except", "father",
        "felt", "first", "found", "from", "going", "gone", "great", "having",
        "here", "herself", "himself", "house", "however", "inside", "instead",
        "into", "itself", "just", "knew", "know", "later", "least", "leave",
        "less", "life", "like", "little", "long", "looked", "made", "make",
        "many", "maybe", "might", "more", "most", "mother", "much", "must",
        "myself", "near", "never", "next", "nothing", "often", "once",
        "only", "other", "others", "ought", "over", "place", "please",
        "quite", "rather", "really", "right", "said", "same", "seem",
        "seemed", "shall", "should", "since", "some", "something",
        "sometimes", "soon", "still", "such", "sure", "take", "taken",
        "than", "that", "their", "them", "themselves", "then", "there",
        "these", "they", "thing", "things", "think", "this", "those",
        "though", "thought", "three", "through", "time", "together",
        "toward", "under", "until", "upon", "very", "want", "well", "went",
        "were", "what", "when", "where", "whether", "which", "while",
        "whole", "whose", "will", "with", "within", "without", "would",
        "your", "yourself"
    )
}
