package com.curio.app.features.personal

import android.content.Context
import android.util.JsonReader
import android.util.JsonToken
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * v444 — A DICTIONARY THAT LIVES ON THE PHONE.
 *
 * The member, after living with the two online doors: *"the online dictionary is
 * bad, add a downloadable dictionary inside the app in the dictionary bottom
 * sheet"*. Two keyless APIs answer for most words and neither answers for the
 * word you actually stopped at — a name, an older spelling, a word the free
 * dictionary has never carried — and both need a connection, which is precisely
 * what a reader on a train does not have.
 *
 * **Webster's Unabridged (1913) is the source**, and it is the right one for a
 * reading app: it is PUBLIC DOMAIN (so it can be downloaded, kept and searched
 * with no licence to honour per lookup), it is a real dictionary rather than a
 * word list, and at ~9MB and ~86,000 headwords it is small enough to fetch once
 * over a phone connection and keep for good.
 *
 * The rules that make this a file rather than a few lines in the sheet:
 *
 *  - **The download is the member's** — nothing is fetched until they ask for it
 *    in the sheet, and it can be removed again from the same row.
 *  - **A half-written file is not a dictionary** — the bytes land in a `.part`
 *    beside the target and are only renamed into place when the whole file is
 *    there, so an interrupted download can never be searched.
 *  - **The lookup is a STREAM, not a parse.** The file is one JSON object of
 *    ~86,000 entries; reading it into memory would cost tens of megabytes of
 *    heap for one word, so the reader walks the tokens and stops at the entry it
 *    wants. The data is alphabetical, so a miss stops as soon as the headwords
 *    have passed the word.
 */
internal object ReaderOfflineDictionary {

    /**
     * Webster's Unabridged, 1913 — the JSON conversion the tool in
     * [matthewreagan/WebstersEnglishDictionary] produces (public domain).
     */
    const val DOWNLOAD_URL =
        "https://raw.githubusercontent.com/matthewreagan/WebstersEnglishDictionary/master/dictionary.json"

    /** What the row promises before the tap. The promise is the file's own size. */
    const val DOWNLOAD_SIZE = "9 MB"

    /** The label the sheet's answer wears when it came from here. */
    const val SOURCE = "Webster's 1913"

    private const val FOLDER = "dictionary"
    private const val FILE_NAME = "webster1913.json"

    /** Below this the file cannot be the dictionary, whatever its name says. */
    private const val MIN_BYTES = 1_000_000L

    /** How much of one entry the sheet is given: four lines, nothing longer. */
    private const val MAX_LINES = 4
    private const val MAX_CHARS = 420

    fun file(context: Context): File = File(File(context.filesDir, FOLDER), FILE_NAME)

    /** There, AND whole (see the note above — a partial file is not a dictionary). */
    fun isReady(context: Context): Boolean {
        val saved = file(context)
        return saved.isFile && saved.length() > MIN_BYTES
    }

    fun bytes(context: Context): Long = runCatching { file(context).length() }.getOrDefault(0L)

    fun remove(context: Context) {
        val saved = file(context)
        runCatching { saved.delete() }
        runCatching { File(saved.parentFile, FILE_NAME + PART).delete() }
    }

    /**
     * Fetches the dictionary, reporting 0..1 while it arrives (the callback runs
     * on the IO dispatcher — the sheet writes it into Compose state, which is
     * safe from any thread).
     */
    suspend fun download(context: Context, onProgress: (Float) -> Unit): Boolean =
        withContext(Dispatchers.IO) {
            val saved = file(context)
            val partial = File(saved.parentFile, FILE_NAME + PART)
            val finished = runCatching {
                saved.parentFile?.mkdirs()
                val conn = URL(DOWNLOAD_URL).openConnection() as HttpURLConnection
                try {
                    conn.connectTimeout = 10_000
                    conn.readTimeout = 30_000
                    conn.setRequestProperty("Accept", "application/json")
                    conn.setRequestProperty(
                        "User-Agent",
                        "Curio/1.1 (Android offline dictionary)"
                    )
                    if (conn.responseCode != 200) return@runCatching false
                    val total = conn.contentLengthLong
                    var read = 0L
                    conn.inputStream.use { input ->
                        partial.outputStream().use { output ->
                            val buffer = ByteArray(64 * 1024)
                            while (true) {
                                val count = input.read(buffer)
                                if (count <= 0) break
                                output.write(buffer, 0, count)
                                read += count
                                onProgress(
                                    if (total > 0L) {
                                        (read.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                                    } else {
                                        0f
                                    }
                                )
                            }
                        }
                    }
                    if (partial.length() > MIN_BYTES) {
                        saved.delete()
                        partial.renameTo(saved)
                        true
                    } else {
                        partial.delete()
                        false
                    }
                } finally {
                    conn.disconnect()
                }
            }.getOrElse { false }
            if (!finished) runCatching { partial.delete() }
            onProgress(if (finished) 1f else 0f)
            finished
        }

    /**
     * What [term] means, offline.
     *
     * `null` is the dictionary NOT BEING THERE (the sheet says so and offers the
     * download); an empty list is the dictionary having no such headword — the
     * same two answers the online doors give, for the same reason.
     */
    suspend fun define(context: Context, term: String): List<ReaderDictionarySense>? =
        withContext(Dispatchers.IO) {
            val wanted = ReaderDictionary.headword(term).lowercase()
            if (wanted.isEmpty() || wanted.length > 48) return@withContext emptyList()
            if (!wanted.all { it.isLetter() || it == '-' || it == '\'' }) {
                return@withContext emptyList()
            }
            if (!isReady(context)) return@withContext null
            runCatching {
                FileInputStream(file(context)).use { stream ->
                    JsonReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                        reader.beginObject()
                        while (reader.hasNext()) {
                            val name = reader.nextName()
                            if (reader.peek() != JsonToken.STRING) {
                                reader.skipValue()
                                continue
                            }
                            val value = reader.nextString()
                            val key = name.lowercase()
                            if (key == wanted) return@runCatching entry(value)
                            // Alphabetical data: once the headwords are past the
                            // word, it is not here (a file that is NOT sorted only
                            // costs the rest of the walk, never an answer).
                            if (key > wanted) return@runCatching emptyList()
                        }
                        emptyList()
                    }
                }
            }.getOrNull()
        }

    /**
     * One Webster's entry, as the sheet draws it.
     *
     * The 1913 entries are prose: one long run of senses, numbered when the word
     * has several and clean paragraphs when it has one. The numbered ones are
     * split so the sheet draws LINES rather than a wall, a very long run is cut at
     * a sane reading length, and the whole thing is offered under the source's own
     * name because Webster's labels its parts of speech in prose rather than in a
     * field (see [ReaderDictionarySense]).
     */
    private fun entry(raw: String): List<ReaderDictionarySense> {
        val text = raw.replace(Regex("\\s+"), " ").trim()
        if (text.isEmpty()) return emptyList()
        val numbered = Regex("(?<=[.;:])\\s+(?=\\d{1,2}[.)]\\s)").split(text)
        val parts = if (numbered.size > 1) numbered else listOf(text)
        val lines = ArrayList<String>()
        for (part in parts) {
            val clean = part.trim()
            if (clean.isEmpty()) continue
            lines.add(if (clean.length > MAX_CHARS) clean.take(MAX_CHARS).trimEnd() + "\u2026" else clean)
            if (lines.size >= MAX_LINES) break
        }
        if (lines.isEmpty()) return emptyList()
        return listOf(ReaderDictionarySense(SOURCE, lines))
    }

    private const val PART = ".part"
}
