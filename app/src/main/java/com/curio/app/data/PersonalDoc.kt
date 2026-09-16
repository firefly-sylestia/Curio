package com.curio.app.data

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser

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
 * A block's text may hold newlines: blocks are not lines. A block is only ever
 * split when a PHOTO is inserted at the caret (that is the one thing plain
 * text cannot express), so typing, undo and the IME behave like an ordinary
 * multi-line field.
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
    /** The paragraph is a pulled quote: a tinted, indented aside. */
    val quote: Boolean = false
)

/** Alignment of one block's paragraph. */
enum class PersonalAlign { START, CENTER }

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
    val align: PersonalAlign = PersonalAlign.START
) {
    val isPhoto: Boolean get() = photo != null
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
            b.addProperty("align", block.align.name)
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
                    quote = r.flag("q")
                ).takeIf { it.end > it.start }
            }.orEmpty()
            PersonalBlock(
                id = b.str("id").ifBlank { newBlockId() },
                text = text,
                runs = runs,
                photo = b.get("photo")?.takeIf { !it.isJsonNull }?.asString,
                caption = b.str("caption"),
                align = runCatching {
                    PersonalAlign.valueOf(b.str("align").ifBlank { PersonalAlign.START.name })
                }.getOrDefault(PersonalAlign.START)
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

/** Ids for blocks, notes and books. Prefixed so a stray id in a log or a
 *  backup is obvious about what it belongs to. */
fun newBlockId(): String = "pb-" + java.util.UUID.randomUUID().toString()
fun newNoteId(): String = "pn-" + java.util.UUID.randomUUID().toString()
fun newPersonalBookId(): String = "bk-" + java.util.UUID.randomUUID().toString()
