package com.curio.app.data

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlin.math.roundToInt

/**
 * v387 — THE JOURNAL/B OOK WRITING DOCUMENT (`features/personal/`).
 *
 * Journals, chapter reviews and a book's own notes are Curio's PERSONAL
 * writing — the member's own words, kept in their own store (`personal_notes`
 * / `personal_books`), completely separate from a capture in the Cabinet. A
 * capture is a *reaction to a topic*; this is writing with no topic at all, so
 * it must not borrow the capture schema, the capture deep-link routes nor the
 * saved-entry detail view. What they DO share is the writing experience.
 *
 * The body is a list of BLOCKS rather than one string: a block is either a run
 * of text or an attached photo, which is what lets a photo sit INSIDE the
 * writing ("add photos in the writing canvas … in that same canvas", user
 * decision: inline blocks in the page) instead of only at the top or the foot
 * of the note. [PersonalRun] carries the character styling of one range, and
 * [PersonalBlock.align] carries the paragraph's alignment, so every tool the
 * journal offers round-trips exactly.
 *
 * A block's text may hold newlines: blocks are not lines. v389e — a PROSE block
 * is a whole paragraph (Enter writes a newline into it), which is what keeps an
 * entry ONE text field: the platform's own Select all, its drag handles, its
 * cut and its undo then behave like ordinary writing instead of stopping at the
 * edge of a field that held a single line.
 *
 * A block is split only where a LINE really is the page's unit: a photo or a
 * voice note dropped at the caret (the one thing plain text cannot express), a
 * checklist or bullet row (whose dot or box is drawn once, at the row's own
 * height, and whose Enter means "the next item"), and a heading, which goes on
 * a line of its own so every view reads it as one.
 *
 * NOTE ON STORAGE SHAPE: [PersonalRun] is deliberately NOT the capture
 * editor's `TextSpan` (ui/components/RichTextEditor.kt). They look alike, they
 * have different jobs — `TextSpan` is the capture/share-card model with a
 * highlighter and per-letter sizes and a JSON contract of its own — and the
 * personal store must be free to grow fields (strike, quote, alignment)
 * without moving that contract.
 */
data class PersonalRun(
    val start: Int,
    val end: Int,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strike: Boolean = false,
    /** A pulled quote: a smaller aside with the coffee rule down its side. */
    val quote: Boolean = false,
    /** A TITLE: the line is set bigger (and in the display serif), so a page
     *  of writing can carry its own headings. */
    val title: Boolean = false,
    /** A footnote-sized line. */
    val small: Boolean = false,
    /** A bulleted line (the dot is drawn, never typed). */
    val bullet: Boolean = false,
    /** A checklist line (the box is drawn, never typed). */
    val checkbox: Boolean = false,
    /**
     * v389 — THE MARKER PEN this stretch of words was written with: one of
     * [PERSONAL_HIGHLIGHT_KEYS], or "" for none.
     *
     * A run's own KEY rather than a colour, exactly like [PersonalMarker]: the
     * page paints it (see personalHighlightInk) so the palette can move without
     * rewriting anybody's notes, and an unseen key degrades to no marker rather
     * than to a wrong colour.
     */
    val highlight: String = "",
    /**
     * v389 — THE FACE this stretch of words is set in: one of
     * [PERSONAL_FONT_KEYS], or "" for the page's own writing face.
     *
     * A key again, and for the same reason as [highlight]: the family is chosen
     * where it is painted, so a saved note names a CHOICE ("mono") rather than
     * a file — and a build that does not know the key sets the page's own face,
     * which is a note that looks plain, never a note that looks broken.
     */
    val font: String = "",
)

/** Alignment of one block's paragraph. */
/**
 * v389 — HOW A LINE SITS IN THE PAGE.
 *
 * [START] and [CENTER] were the only two for as long as the dock had only two
 * buttons for them. The universal dock carries the other two (user request: "the
 * full scren text editor formats of left right centered and justofied … add in
 * the universal tool bar"), and they are the same shape of property — the
 * block's, not a character's, because an alignment is a property of a line.
 *
 * Adding values is safe on disk: an old note never wrote one, and a note that
 * writes one still opens in a build that does not know it (see the tolerant
 * enum reading in the PersonalDoc codec).
 */
enum class PersonalAlign { START, CENTER, END, JUSTIFY }

/** One block of the writing canvas: text, or an attached photo. */
data class PersonalBlock(
    val id: String,
    val text: String = "",
    val runs: List<PersonalRun> = emptyList(),
    /** Content URI of an attached photo (persisted read permission), or null
     *  for a text block. A photo block has no text of its own. */
    val photo: String? = null,
    /** Optional one-line caption shown under a photo. */
    val caption: String = "",
    /** Stable key for the photo's print size; empty means page-wide. */
    val photoSize: String = "",
    val align: PersonalAlign = PersonalAlign.START,
    /**
     * A VOICE NOTE's audio file (v389) — an absolute path under
     * `filesDir/audio/`, or null for a text / photo block. A voice block has no
     * text of its own: it is the recording, drawn as a waveform the member can
     * scrub, and the page carries on underneath it (a note can be written above
     * or below, which is the whole point of a journal you talk into).
     */
    val audio: String? = null,
    /** How long the recording runs, in whole seconds (the time on the block). */
    val audioSeconds: Int = 0,
    /**
     * The waveform, already extracted and downsampled when the recording was
     * kept — see [PersonalAudioBars]. Stored WITH the block so a page draws its
     * waveforms instantly: extracting them means decoding the whole file
     * through MediaCodec, which is far too expensive to do while a page is
     * scrolling.
     */
    val audioBars: String = "",
    /**
     * A checklist line's TICK (v389). Stored WITH THE BLOCK, which is the whole
     * point of a to-do page: the tick used to live in the editor's own widget
     * state, so it vanished the moment the page reloaded and the read-only view
     * (journals, previews, book reviews) could not draw it at all — a checklist
     * that forgets what you finished is not a checklist.
     */
    val checked: Boolean = false,
    /**
     * The BULLET MARKER this line wears (v389): [PersonalMarker.key], "" for
     * the default dot. Per LINE rather than per page, so one page can run a
     * star list and a crystal list, and a re-opened note keeps what it wore.
     * Only meaningful on a line that carries the bullet flag.
     */
    val marker: String = ""
) {
    val isPhoto: Boolean get() = photo != null

    /** True for a voice-note block (a recording, no text of its own). */
    val isAudio: Boolean get() = audio != null

    /** The line's marker, or the default dot when it never picked one. */
    val markerStyle: PersonalMarker get() = PersonalMarker.fromKey(marker) ?: PersonalMarker.DOT
}

/** The whole body of one note. */
data class PersonalDoc(
    val blocks: List<PersonalBlock> = emptyList()
) {
    /** True when there is nothing worth saving (empty text, no photos). */
    val isEmpty: Boolean
        get() = blocks.none { it.isPhoto || it.text.isNotBlank() }

    /** Plain text of the canvas — used for previews, search and word counts. */
    val plainText: String
        get() = blocks.filterNot { it.isPhoto }
            .joinToString("\n\n") { it.text }
            .trim()

    /** Walking distance for the shelf/detail: everything the member typed or
     *  about to type. */
    val photoCount: Int get() = blocks.count { it.isPhoto }
}

/** How many words [plainText] holds (previews and journal stats). */
fun PersonalDoc.wordCount(): Int =
    plainText.split(' ', '\n', '\t').count { it.isNotBlank() }

/**
 * v389 — THE BULLET MARKER.
 *
 * A list line used to wear one hard-coded dot. The dock's bullet tool now
 * opens a menu of markers, and the pick is STORED ([PersonalBlock.marker]) so a
 * page keeps its look across a reload, a device and the read-only views.
 *
 * Keys are stable strings in the stored document — they are what a saved note
 * says, so they must never be renamed; a marker added later is a new value and
 * an unknown key reads as the default dot on an older build.
 */
enum class PersonalMarker(val key: String, val label: String) {
    DOT("dot", "Dot"),
    RING("ring", "Ring"),
    DASH("dash", "Dash"),
    STAR("star", "Star"),
    SPARK("spark", "Spark"),
    CRYSTAL("crystal", "Crystal"),
    ARROW("arrow", "Arrow"),
    LEAF("leaf", "Leaf"),
    HEART("heart", "Heart"),
    BOLT("bolt", "Bolt");

    companion object {
        fun fromKey(key: String?): PersonalMarker? =
            entries.firstOrNull { it.key == key }
    }
}

/**
 * v389 — HOW A VOICE NOTE'S WAVEFORM IS STORED.
 *
 * A page keeps its recordings' waveforms instead of decoding the audio every
 * time the block is drawn (MediaCodec on a scrolling page is a stutter), and it
 * keeps them as TEXT: one hex byte per bar, `00`–`ff` for 0.00–1.00. A
 * three-minute note over 72 bars is 144 characters in the stored document —
 * cheaper than the float array it came from, and it survives every JSON
 * round-trip without a float-precision surprise.
 *
 * [decode] always answers [BAR_COUNT] bars (padding with silence, resampling a
 * waveform written at another resolution), so a drawing loop never has to think
 * about the string's length.
 */
object PersonalAudioBars {

    /** How many bars a voice block draws — read at the block's own width. */
    const val BAR_COUNT = 72

    private const val HEX = "0123456789abcdef"

    fun encode(samples: FloatArray): String {
        val out = StringBuilder(samples.size * 2)
        samples.forEach { value ->
            val level = (value.coerceIn(0f, 1f) * 255f).roundToInt()
            out.append(HEX[level shr 4]).append(HEX[level and 0x0f])
        }
        return out.toString()
    }

    fun decode(encoded: String, barCount: Int = BAR_COUNT): FloatArray {
        val bars = FloatArray(barCount)
        if (encoded.length < 2) return bars
        val stored = FloatArray(encoded.length / 2)
        stored.indices.forEach { i ->
            val high = HEX.indexOf(encoded[i * 2].lowercaseChar())
            val low = HEX.indexOf(encoded[i * 2 + 1].lowercaseChar())
            if (high < 0 || low < 0) return bars
            stored[i] = ((high shl 4) or low) / 255f
        }
        if (stored.size == barCount) return stored
        // A waveform written at another resolution still fits: nearest bar.
        return FloatArray(barCount) { i ->
            stored[((i.toLong() * stored.size) / barCount).toInt().coerceIn(0, stored.size - 1)]
        }
    }
}

/** The mood of a journal day. Keys are stable strings in the DB. */
enum class PersonalMood(val key: String, val label: String) {
    CALM("calm", "Calm"),
    HAPPY("happy", "Happy"),
    CURIOUS("curious", "Curious"),
    INSPIRED("inspired", "Inspired"),
    TIRED("tired", "Tired"),
    HEAVY("heavy", "Heavy");

    companion object {
        fun fromKey(key: String?): PersonalMood? =
            entries.firstOrNull { it.key == key }
    }
}

/**
 * The document's JSON contract (`personal_notes.bodyJson`).
 *
 * Encoded by hand from nullable mirrors: Gson bypasses Kotlin constructor
 * defaults (it allocates rather than constructs), so a field added in a later
 * version would arrive as `null` inside a non-null Kotlin type and the note
 * would die on read. The mirrors make every field optional in BOTH
 * directions, so an older note always opens and a newer one never crashes an
 * older build.
 */
object PersonalDocCodec {

    private val gson = Gson()

    fun encode(doc: PersonalDoc): String {
        val root = JsonObject()
        val blocks = JsonArray()
        doc.blocks.forEach { block ->
            val b = JsonObject()
            b.addProperty("id", block.id)
            b.addProperty("text", block.text)
            b.addProperty("photo", block.photo)
            b.addProperty("caption", block.caption)
            if (block.photoSize.isNotEmpty()) b.addProperty("ps", block.photoSize)
            b.addProperty("align", block.align.name)
            // v389 — a ticked checklist line and a line's bullet marker. Both
            // are omitted at their defaults, so every page written before this
            // version encodes byte-for-byte as it did.
            if (block.checked) b.addProperty("ck", true)
            if (block.marker.isNotBlank()) b.addProperty("mk", block.marker)
            // v389 — a voice note (its file, its length, its waveform). None of
            // the three is written for any other kind of block, so a text or
            // photo page encodes exactly as it did before.
            block.audio?.let { audio ->
                b.addProperty("aud", audio)
                b.addProperty("aus", block.audioSeconds)
                if (block.audioBars.isNotBlank()) b.addProperty("aub", block.audioBars)
            }
            val runs = JsonArray()
            block.runs.forEach { run ->
                val r = JsonObject()
                r.addProperty("s", run.start)
                r.addProperty("e", run.end)
                if (run.bold) r.addProperty("b", true)
                if (run.italic) r.addProperty("i", true)
                if (run.underline) r.addProperty("u", true)
                if (run.strike) r.addProperty("k", true)
                if (run.quote) r.addProperty("q", true)
                if (run.title) r.addProperty("h", true)
                if (run.small) r.addProperty("m", true)
                if (run.bullet) r.addProperty("l", true)
                if (run.checkbox) r.addProperty("c", true)
                // v389 — the marker pen, written only when a pen is down, so an
                // unmarked note is byte-for-byte what it was before.
                if (run.highlight.isNotEmpty()) r.addProperty("g", run.highlight)
                // v389 — the face, written only when one was chosen.
                if (run.font.isNotEmpty()) r.addProperty("f", run.font)
                runs.add(r)
            }
            b.add("runs", runs)
            blocks.add(b)
        }
        root.add("blocks", blocks)
        return gson.toJson(root)
    }

    /** Never throws: an unreadable body reads as one empty block. */
    fun decode(json: String?): PersonalDoc {
        if (json.isNullOrBlank()) return PersonalDoc(listOf(newBlock()))
        val root = runCatching { JsonParser.parseString(json).asJsonObject }.getOrNull()
            ?: return PersonalDoc(listOf(newBlock()))
        val array = runCatching { root.getAsJsonArray("blocks") }.getOrNull()
            ?: return PersonalDoc(listOf(newBlock()))
        val blocks = array.mapNotNull { element ->
            val b = element as? JsonObject ?: return@mapNotNull null
            val text = b.str("text")
            val runs = b.getAsJsonArray("runs")?.mapNotNull { runElement ->
                val r = runElement as? JsonObject ?: return@mapNotNull null
                PersonalRun(
                    start = r.int("s").coerceAtLeast(0),
                    end = r.int("e").coerceAtLeast(0),
                    bold = r.flag("b"),
                    italic = r.flag("i"),
                    underline = r.flag("u"),
                    strike = r.flag("k"),
                    quote = r.flag("q"),
                    title = r.flag("h"),
                    small = r.flag("m"),
                    bullet = r.flag("l"),
                    checkbox = r.flag("c"),
                    // v389 — an older note has no "g" key: it decodes as
                    // unmarked. An UNKNOWN key (a pen a future build adds)
                    // decodes as unmarked too rather than throwing, because
                    // [personalHighlightInk] answers "" with the default pen and
                    // a wrong colour is worse than no colour.
                    highlight = r.str("g"),
                    // v389 — absent on an older note (the page's own face), and
                    // an unknown key falls back to the same place rather than
                    // throwing away the whole run.
                    font = r.str("f")
                ).takeIf { it.end > it.start }
            }.orEmpty()
            PersonalBlock(
                id = b.str("id").ifBlank { newBlockId() },
                text = text,
                runs = runs,
                photo = b.get("photo")?.takeIf { !it.isJsonNull }?.asString,
                caption = b.str("caption"),
                // v389 — absent on an older note, which reads as a page-wide
                // print: exactly how that note already looked.
                photoSize = b.str("ps"),
                align = runCatching {
                    PersonalAlign.valueOf(b.str("align").ifBlank { PersonalAlign.START.name })
                }.getOrDefault(PersonalAlign.START),
                // v389 — the tick and the bullet marker. An older note has
                // neither key: it decodes as unticked, with the default dot.
                checked = b.flag("ck"),
                marker = b.str("mk"),
                // v389 — a voice note. An older note has no "aud" key at all,
                // so it decodes as an ordinary text block, exactly as before.
                audio = b.str("aud").ifBlank { null },
                audioSeconds = b.int("aus"),
                audioBars = b.str("aub")
            )
        }
        // A note whose body decoded to nothing still needs ONE writable block,
        // or the editor would open with nowhere to put the caret.
        val safe = if (blocks.isEmpty()) listOf(newBlock()) else blocks
        return PersonalDoc(safe)
    }

    private fun JsonObject.str(key: String): String {
        val value = get(key) ?: return ""
        return if (value.isJsonNull) "" else runCatching { value.asString }.getOrDefault("")
    }

    private fun JsonObject.flag(key: String): Boolean =
        runCatching { get(key)?.asBoolean == true }.getOrDefault(false)

    private fun JsonObject.int(key: String): Int =
        runCatching { get(key)?.asInt ?: 0 }.getOrDefault(0)

    /** A fresh empty text block with a stable id (focus keys ride these). */
    fun newBlock(): PersonalBlock = PersonalBlock(id = newBlockId())
}

/**
 * ONE CHAPTER OF A BOOK THE APP'S OWN CATALOG DOES NOT HAVE.
 *
 * Curio's ~800 curated books carry real chapter lists (name, pages, a
 * one-line summary) in the topic JSON — see `BookCatalog`. A book added from
 * Open Library, or typed in by hand, used to have nothing: every chapter row
 * said "Chapter 7" and the member had to know how long the book was. Open
 * Library's editions DO carry a table of contents, so this is what the shelf
 * stores once it has read one — the same three facts the catalog gives, so
 * the chapter page reads the same whichever door the book came in by.
 */
data class PersonalChapter(
    val number: Int,
    val title: String = "",
    /** 1-based page range from the edition; 0 when it did not say. */
    val pageStart: Int = 0,
    val pageEnd: Int = 0,
    val summary: String = ""
)

/**
 * `personal_books.chaptersJson`'s contract.
 *
 * Hand-written from nullable mirrors for the same reason [PersonalDocCodec]
 * is: Gson bypasses Kotlin constructor defaults, so a field added later would
 * arrive as `null` inside a non-null type. An older book always decodes and a
 * newer one never crashes an older build.
 */
object PersonalChapterCodec {

    private val gson = Gson()

    fun encode(chapters: List<PersonalChapter>): String {
        val out = JsonArray()
        chapters.forEach { chapter ->
            val obj = JsonObject()
            obj.addProperty("n", chapter.number)
            obj.addProperty("t", chapter.title)
            if (chapter.pageStart > 0) obj.addProperty("s", chapter.pageStart)
            if (chapter.pageEnd > 0) obj.addProperty("e", chapter.pageEnd)
            if (chapter.summary.isNotBlank()) obj.addProperty("m", chapter.summary)
            out.add(obj)
        }
        return gson.toJson(out)
    }

    /** Never throws: an unreadable list reads as no chapters at all. */
    fun decode(json: String?): List<PersonalChapter> {
        if (json.isNullOrBlank()) return emptyList()
        val array = runCatching { JsonParser.parseString(json).asJsonArray }.getOrNull()
            ?: return emptyList()
        return array.mapIndexedNotNull { index, element ->
            val obj = element as? JsonObject ?: return@mapIndexedNotNull null
            val title = obj.str("t")
            val number = obj.int("n").takeIf { it > 0 } ?: (index + 1)
            PersonalChapter(
                number = number,
                title = title,
                pageStart = obj.int("s"),
                pageEnd = obj.int("e"),
                summary = obj.str("m")
            )
        }
    }

    private fun JsonObject.str(key: String): String {
        val value = get(key) ?: return ""
        return if (value.isJsonNull) "" else runCatching { value.asString }.getOrDefault("")
    }

    private fun JsonObject.int(key: String): Int =
        runCatching { get(key)?.asInt ?: 0 }.getOrDefault(0)
}

/** Ids for blocks, notes and books. Prefixed so a stray id in a log or a
 *  backup is obvious about what it belongs to. */
fun newBlockId(): String = "pb-" + java.util.UUID.randomUUID().toString()
fun newNoteId(): String = "pn-" + java.util.UUID.randomUUID().toString()
fun newPersonalBookId(): String = "bk-" + java.util.UUID.randomUUID().toString()

/** A reader mark's id (see [ReaderMarkEntity]) — its own prefix, so a mark can
 *  never be mistaken for a note or a book when one is logged out of context. */
fun newReaderMarkId(): String = "rm-" + java.util.UUID.randomUUID().toString()
