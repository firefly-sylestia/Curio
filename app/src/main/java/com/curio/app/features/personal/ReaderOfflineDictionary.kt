package com.curio.app.features.personal

import android.content.Context
import android.util.JsonReader
import android.util.JsonToken
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.Reader
import java.io.Writer
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * v444 — A DICTIONARY THAT LIVES ON THE PHONE.
 * v446 — THREE OF THEM, EACH FROM ITS OWN RELEASE FILE.
 *
 * The member, after living with the two online doors: *"the online dictionary is
 * bad, add a downloadable dictionary inside the app in the dictionary bottom
 * sheet"*. Two keyless APIs answer for most words and neither answers for the
 * word you actually stopped at — a name, an older spelling, a word the free
 * dictionary has never carried — and both need a connection, which is precisely
 * what a reader on a train does not have.
 *
 * **Webster's Unabridged (1913) was the first door**, and it is the right one for
 * a reading app: PUBLIC DOMAIN (so it can be downloaded, kept and searched with
 * no licence to honour per lookup), a real dictionary rather than a word list, and
 * ~9MB / ~86,000 headwords — small enough to fetch once over a phone connection
 * and keep for good.
 *
 * **The member then asked for the other two**: *"wordnet and fuller please"* —
 * so the sheet carries three offline volumes, side by side with the two online
 * doors, and each is parsed from the file its own project actually publishes:
 *
 *  1. [ReaderOfflineVolume.WEBSTER] — `matthewreagan/WebstersEnglishDictionary`'s
 *     single 9MB JSON object (`headword → definition`), searched IN PLACE: it is
 *     already the shape a lookup wants, so nothing is copied and nothing is
 *     duplicated on disk (see [defineFromJsonFile]).
 *  2. [ReaderOfflineVolume.MODERN] — **WordNet 3.1** in the JSON conversion
 *     `fluhus/wordnet-to-json` publishes as a release asset
 *     (`wordnet.json.gz`, ~11.4MB). This is the door that carries MODERN
 *     vocabulary — the 1913 editions predate "internet" and "software" — and it
 *     is under the WordNet licence (free, attribution), which the row names.
 *  3. [ReaderOfflineVolume.FULL] — the **full OPTED 1913** as `CloudBytes
 *     -Academy/English-Dictionary-Open-Source` publishes it
 *     (`csv/dictionary.csv`, ~14MB, 176,023 definitions, three fields:
 *     word · word-type · definition). Same public-domain Webster's text as door
 *     1, but the COMPLETE article set rather than the abridged conversion.
 *
 * **Why two of them are INDEXED rather than kept as they arrive.** Doors 2 and 3
 * publish data built for a database, not for a phone: WordNet's JSON is keyed by
 * synset, so the words live in a separate `lemma` map and a definition needs a
 * join, and it arrives gzipped; the OPTED CSV is one 14MB table. Streaming either
 * one per lookup would mean decompressing and scanning tens of megabytes for one
 * word — several seconds, on every word, forever. So they are **translated once,
 * at download time, into an index this app can actually search**, and the
 * translation is the interesting part:
 *
 *  - **One file per first letter** (`a`…`z`, and `_` for everything else). A line
 *    is `word \t label \t definition`, and a lookup opens exactly ONE bucket —
 *    about half a megabyte — instead of the whole dictionary. Alphabetical order
 *    is not needed to make that work, which is why the index can be written in
 *    one streaming pass with no sorting and no dictionary-size buffer in memory.
 *  - **Nothing half-built is ever searchable**: the index is written into a
 *    `*.part` directory and renamed into place only when the last byte has been
 *    read, so an interrupted download cannot answer anything (the same rule the
 *    single-file door already followed).
 *  - **The download IS the index** for doors 2 and 3 — the compressed/original
 *    bytes are consumed as they arrive and never kept, so the phone pays for the
 *    index and not for both.
 *
 * Nothing is fetched until the member taps Download in the sheet, each volume is
 * removed from the same row it was downloaded from, and **a missing volume answers
 * `null`, not `emptyList()`** — "there is no dictionary" and "there is no such
 * word" are still different answers, and the sheet says them differently.
 */
internal object ReaderOfflineDictionary {

    /**
     * The compressed WordNet 3.1 dump — the project's own release asset (see the
     * class note). The size is the asset's, so the row can promise it honestly.
     */
    private const val WORDNET_URL =
        "https://github.com/fluhus/wordnet-to-json/releases/download/v1.0/wordnet.json.gz"

    /** The single JSON object the first door downloads (public domain). */
    private const val WEBSTER_URL =
        "https://raw.githubusercontent.com/matthewreagan/WebstersEnglishDictionary/master/dictionary.json"

    /** The full OPTED 1913 table, three fields per line (public domain). */
    private const val OPTED_URL =
        "https://raw.githubusercontent.com/CloudBytes-Academy/English-Dictionary-Open-Source" +
            "/main/csv/dictionary.csv"

    private const val PART = ".part"

    /** Below this a volume cannot be the dictionary, whatever its name says. */
    private const val MIN_BYTES = 1_000_000L

    /** How much of ONE definition is kept: four lines, nothing longer. */
    private const val MAX_LINES = 4
    private const val MAX_CHARS = 420

    /** At most this many sense groups per part of speech, and groups per word. */
    private const val MAX_SENSES = 4
    private const val MAX_GROUPS = 4

    private const val TAB = '\t'

    /**
     * ONE OF THE DICTIONARIES THAT CAN LIVE ON THE PHONE.
     *
     * Every field on it is a promise the sheet keeps: [label] is the badge, [size]
     * is what the row says before the tap (the file's own size), and [blurb] names
     * the source and its licence — a file this app will keep for good is worth
     * naming where the tap is.
     */
    internal enum class Volume(
        val label: String,
        val source: String,
        val blurb: String,
        val size: String,
        val url: String,
        val format: Format
    ) {
        WEBSTER(
            label = "Offline",
            source = "Webster's 1913",
            blurb = "public domain \u00b7 86,000 headwords",
            size = "9 MB",
            url = WEBSTER_URL,
            format = Format.JSON_MAP
        ),
        MODERN(
            label = "Modern",
            source = "WordNet 3.1",
            blurb = "WordNet licence \u00b7 155,000 words, modern senses",
            size = "11 MB",
            url = WORDNET_URL,
            format = Format.BUCKETS_WORDNET
        ),
        FULL(
            label = "Full 1913",
            source = "Webster's 1913 (full)",
            blurb = "public domain \u00b7 176,000 definitions",
            size = "14 MB",
            url = OPTED_URL,
            format = Format.BUCKETS_CSV
        );

        /**
         * How the bytes this door publishes become something searchable.
         *
         * [JSON_MAP] is the one format that is ALREADY an index (a map of
         * headword to definition), so it is searched in place. The other two are
         * translated into per-letter buckets as they arrive.
         */
        enum class Format { JSON_MAP, BUCKETS_WORDNET, BUCKETS_CSV }

        /** The folder (or file) this volume occupies once it is there. */
        fun home(context: Context): File = File(File(context.filesDir, FOLDER), folderName)

        /** The `*.part` sibling a half-written volume waits in. */
        fun partial(context: Context): File = File(File(context.filesDir, FOLDER), folderName + PART)

        private val folderName: String
            get() = when (this) {
                WEBSTER -> "webster1913.json"
                MODERN -> "wordnet31"
                FULL -> "opted1913"
            }
    }

    /** There, AND whole — a partial volume is never searchable (see the class note). */
    fun isReady(context: Context, volume: Volume): Boolean =
        when (volume.format) {
            Volume.Format.JSON_MAP -> {
                val saved = volume.home(context)
                saved.isFile && saved.length() > MIN_BYTES
            }
            else -> {
                val built = volume.home(context)
                built.isDirectory && (built.listFiles()?.sumOf { it.length() } ?: 0L) > MIN_BYTES
            }
        }

    /** How much of the phone this volume is using (the sheet's Remove row). */
    fun bytes(context: Context, volume: Volume): Long {
        val home = volume.home(context)
        return runCatching {
            if (home.isDirectory) home.listFiles()?.sumOf { it.length() } ?: 0L else home.length()
        }.getOrDefault(0L)
    }

    fun remove(context: Context, volume: Volume) {
        runCatching { deleteTree(volume.home(context)) }
        runCatching { deleteTree(volume.partial(context)) }
    }

    /**
     * Fetches [volume] and makes it searchable, reporting 0..1 while the bytes
     * arrive (the callback runs on the IO dispatcher — the sheet writes it into
     * Compose state, which is safe from any thread).
     *
     * For the two indexed volumes the progress is the DOWNLOAD's own fraction: the
     * work after the last byte (writing the last bucket, renaming the folder) is
     * instant by comparison, and a bar that jumps backwards when a second phase
     * starts would be lying about what is being waited on.
     */
    suspend fun download(
        context: Context,
        volume: Volume,
        onProgress: (Float) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val finished = when (volume.format) {
            Volume.Format.JSON_MAP -> fetchJsonMap(context, volume, onProgress)
            Volume.Format.BUCKETS_WORDNET -> buildBuckets(context, volume, onProgress) { reader, sink ->
                indexWordNet(reader, sink)
            }
            Volume.Format.BUCKETS_CSV -> buildBuckets(context, volume, onProgress) { reader, sink ->
                indexCsv(reader, sink)
            }
        }
        onProgress(if (finished) 1f else 0f)
        finished
    }

    /**
     * What [term] means in [volume], offline.
     *
     * `null` is the volume NOT BEING THERE (the sheet says so and offers the
     * download); an empty list is the volume having no such headword — the same
     * two answers the online doors give, for the same reason.
     */
    suspend fun define(
        context: Context,
        volume: Volume,
        term: String
    ): List<ReaderDictionarySense>? = withContext(Dispatchers.IO) {
        val wanted = ReaderDictionary.headword(term).lowercase()
        if (wanted.isEmpty() || wanted.length > 48) return@withContext emptyList()
        if (!wanted.all { it.isLetter() || it == '-' || it == '\'' }) {
            return@withContext emptyList()
        }
        if (!isReady(context, volume)) return@withContext null
        when (volume.format) {
            Volume.Format.JSON_MAP -> defineFromJsonFile(context, volume, wanted)
            else -> defineFromBuckets(context, volume, wanted)
        }
    }

    /**
     * v453 — THE WORDS THIS DICTIONARY HOLDS UNDER [letter].
     *
     * The member: *"the dictionary from the home screen shows the full words it
     * have, like a physical dictionary and user can search words and it shows it"*.
     * A dictionary page that can only answer what you already spelled is a
     * lookup box; a dictionary you can BROWSE is the thing a shelf is for, and
     * browsing needs the words themselves.
     *
     * The index makes this cheap, and that is the whole point of the per-letter
     * buckets (see the class note): **one letter is one file** (~0.5MB), so the
     * page reads a single bucket, takes the first field of each line and returns
     * them sorted — no whole-dictionary scan, no second copy of anything, and only
     * ever while the member is looking at that letter. The single-file door has no
     * buckets, so it is streamed once for the letter asked for (its object is
     * already `headword → definition`, so only the NAMES are read and every value
     * is skipped without being built).
     *
     * The sort is `CASE_INSENSITIVE_ORDER` — a physical dictionary files "Apple"
     * and "apple" together, and a list where "Zebra" sorts before "apple" (which
     * is what plain string order does to mixed case) reads as broken.
     */
    suspend fun headwords(context: Context, volume: Volume, letter: Char): List<String> =
        withContext(Dispatchers.IO) {
            if (!isReady(context, volume)) return@withContext emptyList()
            val wanted = letter.lowercaseChar()
            val found = java.util.TreeSet(String.CASE_INSENSITIVE_ORDER)
            runCatching {
                when (volume.format) {
                    Volume.Format.JSON_MAP -> {
                        FileInputStream(volume.home(context)).use { stream ->
                            JsonReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                                reader.beginObject()
                                while (reader.hasNext()) {
                                    val name = reader.nextName()
                                    // The value is never read: only the headwords are
                                    // wanted, so every definition is skipped as tokens.
                                    reader.skipValue()
                                    if (name.firstOrNull()?.lowercaseChar() == wanted) found.add(name)
                                }
                                reader.endObject()
                            }
                        }
                    }
                    else -> {
                        val bucket = File(volume.home(context), bucketName(wanted.toString()))
                        if (bucket.isFile && bucket.length() > 0L) {
                            FileInputStream(bucket).use { stream ->
                                BufferedReader(
                                    InputStreamReader(stream, Charsets.UTF_8),
                                    64 * 1024
                                ).use { lines ->
                                    while (true) {
                                        val line = lines.readLine() ?: break
                                        val tab = line.indexOf(TAB)
                                        if (tab <= 0) continue
                                        val head = line.substring(0, tab)
                                        if (head.firstOrNull()?.lowercaseChar() == wanted) found.add(head)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            found.toList()
        }

    // ── Door 1: the file that is already an index ────────────────────────────
    //
    // Webster's conversion is one JSON object of `headword → definition`, which is
    // exactly the shape a lookup wants, so it is read IN PLACE and the index step
    // the other two volumes need is skipped entirely — no second copy on disk.
    //
    // The read is a STREAM, not a parse: the file is ~86,000 entries, and reading
    // it into memory would cost tens of megabytes of heap for one word. The data is
    // alphabetical, so a miss stops as soon as the headwords have passed the word.

    private fun defineFromJsonFile(
        context: Context,
        volume: Volume,
        wanted: String
    ): List<ReaderDictionarySense>? = runCatching {
        FileInputStream(volume.home(context)).use { stream ->
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
                    if (key == wanted) return@runCatching entry(volume, value)
                    if (key > wanted) return@runCatching emptyList()
                }
                emptyList()
            }
        }
    }.getOrNull()

    // ── Doors 2 and 3: the volumes that are translated on arrival ────────────

    /**
     * Downloads [volume], turns it into per-letter buckets while it arrives, and
     * renames the whole thing into place only once the source has ended.
     *
     * One streaming pass, a handful of open writers, and no dictionary-size buffer:
     * the source's own order is irrelevant because the BUCKET is what a lookup
     * narrows by, not the position in the file.
     */
    private fun buildBuckets(
        context: Context,
        volume: Volume,
        onProgress: (Float) -> Unit,
        index: (Reader, BucketSink) -> Unit
    ): Boolean {
        val partial = volume.partial(context)
        runCatching { deleteTree(partial) }
        if (!partial.mkdirs()) return false
        val sink = BucketSink(partial) { bucketName(it) }
        val done = runCatching {
            val conn = open(volume.url)
            try {
                if (conn.responseCode != 200) return@runCatching false
                val total = conn.contentLengthLong
                val counting = CountingStream(conn.inputStream) { read ->
                    onProgress(
                        if (total > 0L) {
                            (read.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                        } else {
                            0f
                        }
                    )
                }
                // Gzipped doors are decompressed as they are read; the counting
                // stream sits UNDER the decompressor, so the fraction it reports is
                // the fraction of the FILE that has arrived (which is what the row
                // promises the member).
                val text = if (volume.format == Volume.Format.BUCKETS_WORDNET) {
                    InputStreamReader(GZIPInputStream(counting), Charsets.UTF_8)
                } else {
                    InputStreamReader(counting, Charsets.UTF_8)
                }
                BufferedReader(text, 64 * 1024).use { reader -> index(reader, sink) }
                true
            } finally {
                conn.disconnect()
            }
        }.getOrElse { false }
        sink.close()
        if (!done) {
            runCatching { deleteTree(partial) }
            return false
        }
        val home = volume.home(context)
        runCatching { deleteTree(home) }
        val moved = runCatching { partial.renameTo(home) }.getOrDefault(false)
        if (!moved) runCatching { deleteTree(partial) }
        return moved && isReady(context, volume)
    }

    /**
     * WordNet 3.1, from the release's own `wordnet.json.gz`.
     *
     * The root object is keyed by DATA TYPE, not by word: `synset` is a map of
     * synset id → { pos, word[], gloss, … }, and the words' own index (`lemma`) is
     * a map that points back INTO it. Reading the file as published would mean
     * joining two maps across ~50MB of JSON for every lookup, so the index is built
     * from `synset` alone: each synset already carries the words it holds, so one
     * pass over it produces every `word → definition` line the buckets need, with
     * no join at all. Everything else in the root (`lemma`, `lemmaRanked`,
     * `exception`, `example`) is skipped without being read — `skipValue` walks the
     * tokens without building any of them.
     *
     * Usage examples are deliberately NOT carried: synset examples are references
     * (`wordNumber` + `templateNumber`) into the root's `example` template map, and
     * that map may sit either side of `synset` in the file — a second pass over the
     * download for a sentence the glosses already often carry in parentheses.
     */
    private fun indexWordNet(reader: Reader, sink: BucketSink) {
        JsonReader(reader).use { json ->
            json.beginObject()
            while (json.hasNext()) {
                if (json.nextName() != "synset") {
                    json.skipValue()
                    continue
                }
                json.beginObject()
                while (json.hasNext()) {
                    // The synset's own id: the words themselves carry the index.
                    json.nextName()
                    json.beginObject()
                    var pos = ""
                    var gloss = ""
                    val words = ArrayList<String>(2)
                    while (json.hasNext()) {
                        when (json.nextName()) {
                            "pos" -> if (json.peek() == JsonToken.STRING) {
                                pos = json.nextString()
                            } else {
                                json.skipValue()
                            }
                            "gloss" -> if (json.peek() == JsonToken.STRING) {
                                gloss = json.nextString()
                            } else {
                                json.skipValue()
                            }
                            "word" -> {
                                if (json.peek() != JsonToken.BEGIN_ARRAY) {
                                    json.skipValue()
                                    continue
                                }
                                json.beginArray()
                                while (json.hasNext()) {
                                    if (json.peek() == JsonToken.STRING) {
                                        words.add(json.nextString())
                                    } else {
                                        json.skipValue()
                                    }
                                }
                                json.endArray()
                            }
                            else -> json.skipValue()
                        }
                    }
                    json.endObject()
                    if (gloss.isNotBlank()) {
                        val definition = clean(gloss)
                        val label = wordNetLabel(pos)
                        for (word in words) sink.write(word, label, definition)
                    }
                }
                json.endObject()
            }
            json.endObject()
        }
    }

    /**
     * The full OPTED 1913, from the project's own `dictionary.csv`.
     *
     * Three fields — word, word-type, definition — and the definition is prose that
     * freely contains commas, quotes and (in the longer articles) newlines, so this
     * is read by a real CSV reader rather than by splitting on commas
     * (see [CsvReader]).
     */
    private fun indexCsv(reader: Reader, sink: BucketSink) {
        val csv = CsvReader(reader)
        while (true) {
            val row = csv.next() ?: break
            if (row.size < 3) continue
            val word = row[0].trim()
            val type = row[1].trim()
            val definition = row[2].trim()
            if (word.isEmpty() || definition.isEmpty()) continue
            // A header row, if this release ever writes one.
            if (word.equals("word", true) && type.equals("wordtype", true)) continue
            sink.write(word, typeLabel(type), clean(definition))
        }
    }

    /**
     * One bucket's worth of a lookup.
     *
     * The index is one file per first letter, so this opens exactly one of them
     * (~0.5MB) and reads it line by line. A line is `word \t label \t definition`;
     * every line whose word is the one asked for contributes to a group under its
     * own label, so a word that is a noun in one sense and a verb in another reads
     * as two groups rather than as one run-on.
     */
    private fun defineFromBuckets(
        context: Context,
        volume: Volume,
        wanted: String
    ): List<ReaderDictionarySense>? = runCatching {
        val bucket = File(volume.home(context), bucketName(wanted))
        if (!bucket.isFile || bucket.length() == 0L) return@runCatching emptyList()
        val groups = LinkedHashMap<String, MutableList<String>>()
        FileInputStream(bucket).use { stream ->
            BufferedReader(InputStreamReader(stream, Charsets.UTF_8), 64 * 1024).use { lines ->
                while (true) {
                    val line = lines.readLine() ?: break
                    val first = line.indexOf(TAB)
                    if (first <= 0) continue
                    if (!line.regionMatches(0, wanted, 0, wanted.length, ignoreCase = true)) continue
                    if (line.length <= first + 1) continue
                    // Exact headword, not a prefix of a longer one.
                    if (!line.substring(0, first).equals(wanted, ignoreCase = true)) continue
                    val second = line.indexOf(TAB, first + 1)
                    if (second <= first) continue
                    val label = line.substring(first + 1, second)
                    val definition = line.substring(second + 1)
                    if (definition.isBlank()) continue
                    val group = groups.getOrPut(label) { ArrayList(2) }
                    if (group.size >= MAX_SENSES) continue
                    if (group.any { it.equals(definition, ignoreCase = true) }) continue
                    group.add(definition)
                }
            }
        }
        val out = ArrayList<ReaderDictionarySense>()
        for ((label, definitions) in groups) {
            if (out.size >= MAX_GROUPS) break
            out.add(ReaderDictionarySense(label, definitions.toList()))
        }
        out
    }.getOrNull()

    // ── Plumbing ─────────────────────────────────────────────────────────────

    /**
     * One Writer per bucket, opened the first time a letter is needed.
     *
     * Doors 2 and 3 hand their words over in the order their own data happens to
     * hold (WordNet by synset, the CSV by article), which is no order at all by
     * first letter — so the index is written by BUCKET rather than by position,
     * and keeping ~27 small writers open costs a buffer each instead of the
     * megabytes a sorted index would need held in memory.
     */
    private class BucketSink(
        private val dir: File,
        /** Which file a word belongs in — handed in so the sink owns no rules. */
        private val bucketOf: (String) -> String
    ) {

        private val open = HashMap<String, Writer>()

        fun write(word: String, label: String, definition: String) {
            val key = bucketOf(word)
            val writer = open[key] ?: run {
                val file = File(dir, key)
                BufferedWriter(OutputStreamWriter(FileOutputStream(file), Charsets.UTF_8), 64 * 1024)
                    .also { open[key] = it }
            }
            runCatching {
                writer.write(word.lowercase())
                writer.write('\t'.code)
                writer.write(label)
                writer.write('\t'.code)
                writer.write(definition)
                writer.write("\n")
            }
        }

        fun close() {
            for (writer in open.values) runCatching { writer.close() }
            open.clear()
        }
    }

    /**
     * A CSV reader that keeps up with a real table.
     *
     * The OPTED definitions are prose: they contain commas, they contain quotes
     * (escaped by doubling), and the long articles contain the line breaks the
     * source wrapped them at. Splitting on commas would cut those definitions in
     * half, so this walks the characters once and honours quoting — which is also
     * what lets a whole 14MB table be read as a STREAM, line by line, with no
     * table-size buffer anywhere.
     */
    private class CsvReader(private val reader: Reader) {

        private var pushed: Int = -2

        /** The next row's fields, or null at the end of the file. */
        fun next(): List<String>? {
            val fields = ArrayList<String>(3)
            val field = StringBuilder()
            var quoted = false
            var started = false
            while (true) {
                val c = read()
                if (c == -1) {
                    if (!started && fields.isEmpty() && field.isEmpty()) return null
                    fields.add(field.toString())
                    return fields
                }
                val ch = c.toChar()
                if (quoted) {
                    when (ch) {
                        '"' -> {
                            val following = read()
                            if (following == '"'.code) {
                                field.append('"')
                            } else {
                                quoted = false
                                pushed = following
                            }
                        }
                        else -> field.append(ch)
                    }
                    continue
                }
                when (ch) {
                    '"' -> {
                        quoted = true
                        started = true
                    }
                    ',' -> {
                        fields.add(field.toString())
                        field.setLength(0)
                        started = true
                    }
                    '\n' -> {
                        fields.add(field.toString())
                        return fields
                    }
                    '\r' -> Unit
                    else -> {
                        field.append(ch)
                        started = true
                    }
                }
            }
        }

        private fun read(): Int {
            if (pushed != -2) {
                val c = pushed
                pushed = -2
                return c
            }
            return reader.read()
        }
    }

    /**
     * A stream that reports how much of it has been read.
     *
     * The row's bar is honest about the FILE it is fetching, so the count is taken
     * under the decompressor (see [buildBuckets]): what the member is waiting on is
     * the download, not the app's own indexing.
     */
    private class CountingStream(
        private val wrapped: java.io.InputStream,
        private val onRead: (Long) -> Unit
    ) : java.io.InputStream() {

        private var total = 0L

        override fun read(): Int {
            val c = wrapped.read()
            if (c >= 0) {
                total += 1
                onRead(total)
            }
            return c
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            val count = wrapped.read(b, off, len)
            if (count > 0) {
                total += count
                onRead(total)
            }
            return count
        }

        override fun close() = wrapped.close()
        override fun available(): Int = wrapped.available()
    }

    private fun open(url: String): HttpURLConnection {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 30_000
        conn.instanceFollowRedirects = true
        conn.setRequestProperty("Accept", "*/*")
        conn.setRequestProperty("User-Agent", "Curio/1.1 (Android offline dictionary)")
        return conn
    }

    /** Door 1's download: the JSON object, byte for byte, into its own file. */
    private fun fetchJsonMap(context: Context, volume: Volume, onProgress: (Float) -> Unit): Boolean {
        val home = volume.home(context)
        val partial = volume.partial(context)
        val finished = runCatching {
            partial.parentFile?.mkdirs()
            val conn = open(volume.url)
            try {
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
                    home.delete()
                    partial.renameTo(home)
                } else {
                    false
                }
            } finally {
                conn.disconnect()
            }
        }.getOrElse { false }
        if (!finished) runCatching { partial.delete() }
        return finished
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
    private fun entry(volume: Volume, raw: String): List<ReaderDictionarySense> {
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
        return listOf(ReaderDictionarySense(volume.source, lines))
    }

    /**
     * A line's own text: one line, no tabs, no runs of air.
     *
     * The index is a line per definition, so a definition that carried a newline or
     * a tab would break the format it is stored in — and a 20,000-character article
     * would cost the phone for a definition nobody can read in a sheet. Runs of
     * whitespace collapse to one space, which is also what the source's wrapping
     * deserves.
     */
    private fun clean(raw: String): String {
        val text = raw.replace(Regex("\\s+"), " ").trim()
        return if (text.length > MAX_CHARS) text.take(MAX_CHARS).trimEnd() + "\u2026" else text
    }

    /** Which bucket [word] belongs to: its first letter, or `_` for anything else. */
    private fun bucketName(word: String): String {
        val first = word.firstOrNull()?.lowercaseChar() ?: return "_"
        return if (first in 'a'..'z') first.toString() else "_"
    }

    /** WordNet's one-letter part of speech, as the sheet's own group name. */
    private fun wordNetLabel(pos: String): String = when (pos) {
        "n" -> "Noun"
        "v" -> "Verb"
        "a", "s" -> "Adjective"
        "r" -> "Adverb"
        else -> "Definition"
    }

    /**
     * The 1913 edition's abbreviations, as the sheet's own group names.
     *
     * Webster's types a word as `n.`, `v. t.`, `p. p.`, `interj.` — a reader wants
     * the word, not the abbreviation, so the common ones are spelled out and
     * anything unrecognised is shown as it was written (never invented).
     */
    private fun typeLabel(type: String): String {
        val key = type.trim().trimEnd('.').replace(Regex("\\s+"), " ").lowercase()
        return when (key) {
            "" -> "Definition"
            "n", "n pl", "pl", "plural" -> "Noun"
            "v", "v t", "v i", "vt", "vi" -> "Verb"
            "a", "adj", "adj pl" -> "Adjective"
            "adv" -> "Adverb"
            "prep" -> "Preposition"
            "conj" -> "Conjunction"
            "pron" -> "Pronoun"
            "interj" -> "Interjection"
            "p p", "pp", "p a", "pa", "part", "participle" -> "Participle"
            else -> type.trim().trimEnd('.').replaceFirstChar { it.uppercase() }
        }
    }

    private fun deleteTree(root: File) {
        if (root.isDirectory) root.listFiles()?.forEach { deleteTree(it) }
        root.delete()
    }

    private const val FOLDER = "dictionary"
}
