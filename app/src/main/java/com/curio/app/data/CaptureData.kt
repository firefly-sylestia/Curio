@file:Suppress("UNNECESSARY_SAFE_CALL") // defensive null guards on non-null Gson-decoded fields

package com.curio.app.data

import com.curio.app.data.CaptureFormat

/**
 * The note-paper look a capture's text boxes wear. [RULED] is the classic
 * notebook page (cream paper + ruled lines); [TORN] is the torn-note style —
 * a properly ripped paper slip with jagged edges, no ruled lines, slight
 * rotation for a hand-placed feel; [TORN_RULED] is a torn slip that ALSO
 * carries ruled lines (the "rules on torn" toggle) — ripped edge, notebook
 * cadence inside. [COFFEE] is a ruled page with coffee-stain blotches along
 * the edges; [FOLDED] is a ruled page with a folded (dog-ear) top-right
 * corner; [RED_MARGIN] is the classic school-notebook ruled page with a red
 * vertical margin line and the text indented past it. [TORN_COFFEE] and
 * [TORN_FOLDED] are torn slips that ALSO carry the coffee stains / folded
 * dog-ear (the "coffee" and "folded" options on a torn page).
 *
 * Chosen PER TEXT BOX (each field's toolbar holds the style chips +
 * rules-on-torn toggle) and persisted per field on every [CaptureData]
 * variant, so each note keeps its own look across save → detail view.
 * Legacy entries omit the per-field fields (Gson → null) and resolve to
 * the take-level [CaptureData.paperStyle] or [RULED].
 */
enum class NotePaperStyle {
    RULED, TORN, TORN_RULED, COFFEE, FOLDED, RED_MARGIN, TORN_COFFEE, TORN_FOLDED,
    // v7.16 — universal decorations: coffee / red margin / folded can now
    // apply to EITHER base (torn or ruled paper), and the torn slip can
    // combine the ruled lines with any single decoration.
    TORN_RED_MARGIN, TORN_RULED_COFFEE, TORN_RULED_FOLDED, TORN_RULED_RED_MARGIN,
    // v7.18 — STACKED decorations: coffee / folded / red margin can now be
    // combined in any combination on either base (a page can be folded AND
    // coffee-stained AND red-margined at once). The flag views in PaperCard
    // decode purely from the enum NAME (contains/startsWith), so these
    // appended values need no per-value logic — every combo Just Works.
    COFFEE_FOLDED, COFFEE_RED_MARGIN, FOLDED_RED_MARGIN, COFFEE_FOLDED_RED_MARGIN,
    TORN_COFFEE_FOLDED, TORN_COFFEE_RED_MARGIN, TORN_FOLDED_RED_MARGIN,
    TORN_COFFEE_FOLDED_RED_MARGIN,
    TORN_RULED_COFFEE_FOLDED, TORN_RULED_COFFEE_RED_MARGIN,
    TORN_RULED_FOLDED_RED_MARGIN, TORN_RULED_COFFEE_FOLDED_RED_MARGIN,
    // v7.30 — retain the rounded-top choice while the user temporarily
    // switches to Torn; it is hidden by the torn renderer and returns when
    // Ruled is selected again.
    TORN_ROUNDED_TOP, TORN_COFFEE_ROUNDED_TOP, TORN_FOLDED_ROUNDED_TOP,
    TORN_RED_MARGIN_ROUNDED_TOP, TORN_COFFEE_FOLDED_ROUNDED_TOP,
    TORN_COFFEE_RED_MARGIN_ROUNDED_TOP, TORN_FOLDED_RED_MARGIN_ROUNDED_TOP,
    TORN_COFFEE_FOLDED_RED_MARGIN_ROUNDED_TOP,
    TORN_RULED_ROUNDED_TOP, TORN_RULED_COFFEE_ROUNDED_TOP,
    TORN_RULED_FOLDED_ROUNDED_TOP, TORN_RULED_RED_MARGIN_ROUNDED_TOP,
    TORN_RULED_COFFEE_FOLDED_ROUNDED_TOP,
    TORN_RULED_COFFEE_RED_MARGIN_ROUNDED_TOP,
    TORN_RULED_FOLDED_RED_MARGIN_ROUNDED_TOP,
    TORN_RULED_COFFEE_FOLDED_RED_MARGIN_ROUNDED_TOP,
    // v7.30 — normal ruled paper can keep the hero-style soft torn bottom
    // while opting into rounded top edges. These names are intentionally
    // appended for Gson/backward compatibility; PaperCard decodes the flag
    // from the enum name just like the other stacked decorations.
    ROUNDED_TOP, COFFEE_ROUNDED_TOP, FOLDED_ROUNDED_TOP, RED_MARGIN_ROUNDED_TOP,
    COFFEE_FOLDED_ROUNDED_TOP, COFFEE_RED_MARGIN_ROUNDED_TOP,
    FOLDED_RED_MARGIN_ROUNDED_TOP, COFFEE_FOLDED_RED_MARGIN_ROUNDED_TOP,
    // v7.33 — WATERMARK paper: the sheet's background wears a faint scatter
    // of category icons (the page-backdrop language printed on the paper).
    // Combines only with the base + rounded-top — NOT with coffee / folded /
    // red margin (see notePaperStyleOf), keeping the enum manageable.
    WATERMARK, WATERMARK_ROUNDED_TOP,
    TORN_WATERMARK, TORN_WATERMARK_ROUNDED_TOP,
    TORN_RULED_WATERMARK, TORN_RULED_WATERMARK_ROUNDED_TOP
}

/**
 * The note-paper COLOR a capture's text boxes wear — a small swatch picker
 * sits next to the Ruled/Torn toggle in each field's toolbar. [CREAM] is the
 * classic warm note (and the default / legacy fallback); the others are
 * pastel note colors that keep the dark warm ink readable. Chosen PER TEXT
 * BOX and persisted per field on every [CaptureData] variant (parallel to
 * the per-field [NotePaperStyle]), so each note keeps its own paper color
 * across save → detail view.
 */
enum class NotePaperColor { CREAM, BUTTER, PINK, MINT, SKY, LILAC }

/**
 * The mood a journal entry records — picked in the Marginalia editor's
 * "How did it make you feel?" row and shown (with its icon) in the saved
 * entry's meta card. Stored by name via Gson; legacy entries omit it
 * (→ null) and simply show no mood.
 */
enum class JournalMood {
    CALM, HAPPY, CURIOUS, INSPIRED, TIRED, OVERWHELMED;

    /** Display label — plain text, safe for the editor chips + meta card. */
    val label: String
        get() = when (this) {
            CALM -> "Calm"
            HAPPY -> "Happy"
            CURIOUS -> "Curious"
            INSPIRED -> "Inspired"
            TIRED -> "Tired"
            OVERWHELMED -> "Overwhelmed"
        }
}

/**
 * A styled run over a string of text — half-open [start, end) character
 * offsets, plus the rich-text flags that style that run. Used by the
 * formats' text fields (Marginalia journal + quotes, Reel Notes review,
 * Field Notes sections, Sound Bite note) so saved captures keep their
 * bold / italic / highlight formatting.
 *
 * Offsets are into the ORIGINAL plain text (the `*Text`/`*Spans` parallel
 * fields in [CaptureData]), so legacy entries without spans decode fine and
 * any consumer can build an AnnotatedString from the pair.
 */
data class TextSpan(
    val start: Int,
    val end: Int,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val highlight: Boolean = false,
    // Optional per-letter font size in sp — lets the A+/A− rich-text tools
    // enlarge/shrink INDIVIDUAL letters while the rest of the field keeps
    // its default size. null = inherit the field's bodyLarge size. Stored as
    // a plain Float (not TextUnit) so Gson serializes it cleanly in saved
    // entries; legacy entries omit it → null.
    val fontSizeSp: Float? = null
)

/** Structured FieldMind provenance preserved on a restored observation or note. */
data class FieldMindMetadata(
    val recordType: String = "observation",
    val category: String = "",
    val confidence: String = "",
    val date: String = "",
    val time: String = "",
    val location: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val weather: String = "",
    val weatherCondition: String = "",
    val weatherTemperature: Double? = null,
    val humidity: Int? = null,
    val windSpeed: Double? = null,
    val cloudCover: Int? = null,
    val pressure: Double? = null,
    val durationMs: Long? = null,
    val startedAt: Long? = null,
    val endedAt: Long? = null,
    // Additional FieldMind observation timing/provenance fields. These stay
    // optional so older Curio backups and native captures remain compatible.
    val changeObservedAt: Long? = null,
    val changeDurationMs: Long? = null,
    val weatherSnapshotAt: Long? = null,
    val parentObservationId: Long? = null,
    val followUpScheduledAt: Long? = null,
    val archivedAt: Long? = null,
    val deletedAt: Long? = null,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    val timeNote: String = "",
    val status: String = "",
    val projectId: Long? = null,
    val sourceId: Long? = null,
    val qualityScore: Int? = null,
    val tags: List<String> = emptyList(),
    val structuredDetailsJson: String = "",
    val species: FieldMindSpecies? = null
)

/** Taxonomy/species data associated with a restored FieldMind observation. */
data class FieldMindSpecies(
    val commonName: String = "",
    val scientificName: String = "",
    val kingdom: String = "",
    val phylum: String = "",
    val className: String = "",
    val order: String = "",
    val family: String = "",
    val genus: String = "",
    val species: String = "",
    val conservationStatus: String = "",
    val lifeStage: String = "",
    val sex: String = "",
    val observationCount: Int? = null,
    val notes: String = ""
)

/**
 * Sealed class hierarchy for structured capture data produced by each format.
 *
 * Each format produces its own typed data object. When saving, the data is
 * serialized to JSON and stored in the Room database as a JSON blob, then
 * deserialized back for rendering in EntryDetail.
 */
sealed class CaptureData {

    /** Sound Bite (§8.1): voice recording with optional title, audio file path, and metadata. */
    data class SoundBite(
        val durationSeconds: Int,
        val title: String,
        val note: String = "",
        val audioFilePath: String? = null,
        val fileSizeBytes: Long = 0,
        val encodingFormat: String = "AAC",
        // Optional rich-text formatting for the note field (bold/italic/
        // highlight). Legacy entries omit it → null, guard with orEmpty().
        val noteSpans: List<TextSpan> = emptyList(),
        // Note-paper style per text box — the title slip and the note wear
        // their OWN choice (ruled page / torn note / torn with rules).
        // Legacy entries omit them (Gson → null) and fall back to
        // [paperStyle] → [NotePaperStyle.RULED].
        val titleStyle: NotePaperStyle? = null,
        val noteStyle: NotePaperStyle? = null,
        // Note-paper COLOR per text box (parallel to the per-field styles).
        // Legacy entries omit them (Gson → null) and fall back to
        // [NotePaperColor.CREAM].
        val titleColor: NotePaperColor? = null,
        val noteColor: NotePaperColor? = null,
        // Optional quote cards — the same hand-placed paper slips as
        // Marginalia's "Favorite quotes". Legacy entries omit them (Gson →
        // empty) so saved voice notes keep their shape.
        val quotes: List<String> = emptyList(),
        val quoteSpans: List<List<TextSpan>> = emptyList(),
        val quoteTilts: List<Float> = emptyList(),
        val quoteStyles: List<NotePaperStyle> = emptyList(),
        val quoteColors: List<NotePaperColor> = emptyList(),
        // Take-level note-paper style — legacy fallback + the "primary"
        // field's style for old consumers. New entries set per-field styles
        // and mirror the note here so [notePaperStyle] stays meaningful.
        val paperStyle: NotePaperStyle? = null,
        // Mood — picked in the editor, shown in the saved entry's meta
        // card. Legacy entries omit it (Gson → null) → no mood.
        val mood: JournalMood? = null
    ) : CaptureData()

    /** Reel Notes (§8.2): review with rating, text, and attached images. */
    data class ReelNotes(
        val rating: Int,
        val reviewText: String,
        val imageCount: Int,
        // Real attached image URIs (poster / stills) so saved entries can show
        // the actual images — legacy entries only stored a count, so this
        // defaults to empty and stays backward-compatible.
        val imageUris: List<String> = emptyList(),
        // Optional rich-text formatting for the review field. Legacy entries
        // omit it → null, guard with orEmpty().
        val reviewSpans: List<TextSpan> = emptyList(),
        // Note-paper style for the review box (ruled / torn / torn with
        // rules). Legacy entries omit it (Gson → null) and fall back to
        // [paperStyle] → [NotePaperStyle.RULED].
        val reviewStyle: NotePaperStyle? = null,
        // Note-paper COLOR for the review box — legacy entries omit it
        // (Gson → null) and fall back to [NotePaperColor.CREAM].
        val reviewColor: NotePaperColor? = null,
        // Optional quote cards — the same hand-placed paper slips as
        // Marginalia's "Favorite quotes". Legacy entries omit them (Gson →
        // empty) so saved reviews keep their shape.
        val quotes: List<String> = emptyList(),
        val quoteSpans: List<List<TextSpan>> = emptyList(),
        val quoteTilts: List<Float> = emptyList(),
        val quoteStyles: List<NotePaperStyle> = emptyList(),
        val quoteColors: List<NotePaperColor> = emptyList(),
        // Take-level note-paper style — legacy fallback.
        val paperStyle: NotePaperStyle? = null,
        // Mood — picked in the editor, shown in the saved entry's meta
        // card. Legacy entries omit it (Gson → null) → no mood.
        val mood: JournalMood? = null
    ) : CaptureData()

    /** Marginalia (§8.3): journal entry with favorite quotes. */
    data class Marginalia(
        val journalText: String,
        val quotes: List<String>,
        // Rich-text formatting (bold/italic/highlight). journalSpans maps 1:1
        // to journalText; quoteSpans is parallel to quotes (one span list per
        // quote). Legacy entries omit both → null, guard with orEmpty().
        val journalSpans: List<TextSpan> = emptyList(),
        val quoteSpans: List<List<TextSpan>> = emptyList(),
        // Hand-placed tilt (degrees) per quote card — generated ONCE when a
        // card is created and saved with the entry, so the angle you saw while
        // adding persists into the saved view instead of re-rolling every
        // recomposition / section switch / revisit. Legacy entries omit it →
        // empty, callers fall back to a stable random tilt.
        val quoteTilts: List<Float> = emptyList(),
        // Note-paper style per text box — the journal page and EACH quote
        // card wear their own choice (ruled / torn / torn with rules).
        // quoteStyles is parallel to quotes (one style per card). Legacy
        // entries omit them (Gson → null, guard with orEmpty()) and fall
        // back to [paperStyle] → [NotePaperStyle.RULED].
        val journalStyle: NotePaperStyle? = null,
        val quoteStyles: List<NotePaperStyle> = emptyList(),
        // Note-paper COLOR per text box — the journal page and EACH quote
        // card wear their own color. quoteColors is parallel to quotes (one
        // color per card). Legacy entries omit them (Gson → null / empty)
        // and fall back to [NotePaperColor.CREAM].
        val journalColor: NotePaperColor? = null,
        val quoteColors: List<NotePaperColor> = emptyList(),
        // Take-level note-paper style — legacy fallback.
        val paperStyle: NotePaperStyle? = null,
        // Mood — picked in the journal editor, shown in the saved entry's
        // meta card. Legacy entries omit it (Gson → null) → no mood.
        val mood: JournalMood? = null,
        // Attached gallery images — legacy entries omit them (Gson → null,
        // guard with orEmpty() at the call sites).
        val imageUris: List<String> = emptyList(),
        // Optional voice-note attachment — the same AudioRecorder pipeline
        // as Sound Bite. Legacy entries omit them (Gson → null / 0).
        val audioFilePath: String? = null,
        val audioDurationSeconds: Int = 0,
        val audioFileSizeBytes: Long = 0,
        val audioEncodingFormat: String = "AAC",
        // Structured FieldMind provenance; absent for native Curio captures.
        val fieldMindMetadata: FieldMindMetadata? = null
    ) : CaptureData()

    /** A single tile's layout on the mood board canvas. */
    data class TileLayout(
        val uri: String,
        val offsetXPx: Float,
        val offsetYPx: Float,
        val rotationDeg: Float,
        val widthPx: Float,
        val heightPx: Float
    )

    /** One floating quote-box placement on the mood board — the card's
     *  top-left corner in the EDITOR board pixel space (same space as
     *  [TileLayout] offsets; the saved view scales it with the collage).
     *  (-1,-1) = never dragged — renderers fall back to the deterministic
     *  slot for that card index. v42 — [w] is the card's custom WIDTH in
     *  the same raw board pixels (-1 = default slot width); legacy entries
     *  and cards never resized keep the default via Gson's default value. */
    data class QuotePos(val x: Float, val y: Float, val w: Float = -1f)

    /** Gallery Wall (§8.4): moodboard collage with caption and tile positions. */
    data class GalleryWall(
        val imageCount: Int,
        val caption: String,
        val imageUris: List<String> = emptyList(),
        val tileLayouts: List<TileLayout> = emptyList(),
        // Note-paper style for the caption box (ruled / torn / torn with
        // rules). Legacy entries omit it (Gson → null) and fall back to
        // [paperStyle] → [NotePaperStyle.RULED].
        val captionStyle: NotePaperStyle? = null,
        // Note-paper COLOR for the caption box — legacy entries omit it
        // (Gson → null) and fall back to [NotePaperColor.CREAM].
        val captionColor: NotePaperColor? = null,
        // Optional quote cards — the same hand-placed paper slips as
        // Marginalia's "Favorite quotes", pinned under the collage. Legacy
        // entries omit them (Gson → empty) so saved boards keep their shape.
        val quotes: List<String> = emptyList(),
        val quoteSpans: List<List<TextSpan>> = emptyList(),
        val quoteTilts: List<Float> = emptyList(),
        val quoteStyles: List<NotePaperStyle> = emptyList(),
        val quoteColors: List<NotePaperColor> = emptyList(),
        // v7.20 — per-card placement for the floating quote boxes (editor
        // board pixels; (-1,-1) = never dragged → deterministic slot).
        // Legacy entries lack it (Gson → empty) → all cards use slots.
        val quotePositions: List<QuotePos> = emptyList(),
        // v7.22 — per-card placement flag: true = the card floats ON the
        // board (added via the board's Quote chip), false = it renders as a
        // separate quote box BELOW the board (added via the bottom Add-quote
        // button). Parallel to [quotes]. Legacy entries lack it (Gson →
        // empty) → every card renders on the board (the v7.19 look).
        val quoteOnBoard: List<Boolean> = emptyList(),
        // Take-level note-paper style — legacy fallback.
        val paperStyle: NotePaperStyle? = null,
        // Mood — picked in the editor's shared "How did it make you feel?"
        // row, shown in the saved entry's meta card. Legacy entries omit it
        // (Gson → null) → no mood.
        val mood: JournalMood? = null
    ) : CaptureData()

    /** Field Notes (§8.5): three-section observation journal. */
    data class FieldNotes(
        val observed: String,
        val surprised: String,
        val learnNext: String,
        val imageUris: List<String> = emptyList(),
        // Rich-text formatting for the three sections (parallel to the text
        // fields). Legacy entries omit them → null, guard with orEmpty().
        val observedSpans: List<TextSpan> = emptyList(),
        val surprisedSpans: List<TextSpan> = emptyList(),
        val learnNextSpans: List<TextSpan> = emptyList(),
        // Note-paper style per section — each of the three field-journal
        // sections wears its own choice (ruled / torn / torn with rules).
        // Legacy entries omit them (Gson → null) and fall back to
        // [paperStyle] → [NotePaperStyle.RULED].
        val observedStyle: NotePaperStyle? = null,
        val surprisedStyle: NotePaperStyle? = null,
        val learnNextStyle: NotePaperStyle? = null,
        // Note-paper COLOR per section (parallel to the per-field styles).
        // Legacy entries omit them (Gson → null) and fall back to
        // [NotePaperColor.CREAM].
        val observedColor: NotePaperColor? = null,
        val surprisedColor: NotePaperColor? = null,
        val learnNextColor: NotePaperColor? = null,
        // Take-level note-paper style — legacy fallback.
        val paperStyle: NotePaperStyle? = null,
        // Mood — picked in the editor, shown in the saved entry's meta
        // card. Legacy entries omit it (Gson → null) → no mood.
        val mood: JournalMood? = null,
        // Structured FieldMind provenance; absent for native Curio captures.
        val fieldMindMetadata: FieldMindMetadata? = null
    ) : CaptureData()

    /**
     * Open Notebook (§8.6): Wildcard pick-any-format.
     * [subFormat] captures which format the user chose,
     * [subData] contains that format's structured data.
     */
    data class OpenNotebook(
        val subFormat: CaptureFormat,
        val subData: CaptureData
    ) : CaptureData()

    /** One capture section inside a multi-section [Portfolio] entry. */
    data class CaptureSection(
        val format: CaptureFormat,
        val data: CaptureData,
        val title: String? = null
    )

    /**
     * Multi-section entry (universal capture): one saved entry that holds
     * several format bodies, each in its own [CaptureSection]. The detail
     * page shows a compact switcher to flip between sections. Single-section
     * entries are still stored bare (no Portfolio wrapper) so every
     * previously-saved entry keeps its shape.
     */
    data class Portfolio(
        val sections: List<CaptureSection>
    ) : CaptureData()

    /**
     * Returns a human-readable one-line preview for Cabinet cards.
     *
     * NULL-SAFE by design: legacy entries decode missing Kotlin-default
     * fields to NULL (Gson allocates via Unsafe, skipping constructor
     * defaults), so every String/List field is guarded with
     * `orEmpty()` / `isNullOrBlank()`. A legacy entry must NEVER take down
     * the Cabinet grid with an NPE — that exact crash (StringsKt.isBlank on
     * a null field, 2026-08-03) blanked the whole Cabinet, mood boards
     * included.
     */
    fun toPreview(): String = when (this) {
        is SoundBite -> "Voice note · ${durationSeconds}s" +
            if (!title.isNullOrBlank()) ", $title" else ""
        is ReelNotes -> buildString {
            if (rating > 0) append("★".repeat(rating) + " ")
            val text = reviewText.orEmpty()
            append(text.take(80))
            if (text.length > 80) append("…")
        }
        is Marginalia -> buildString {
            val journal = journalText.orEmpty()
            val source = if (journal.isNotBlank()) journal
                         else quotes.orEmpty().firstOrNull().orEmpty()
            append(source.take(80))
            if (source.length > 80) append("…")
        }
        is GalleryWall -> "Moodboard · $imageCount image${if (imageCount != 1) "s" else ""}" +
            if (!caption.isNullOrBlank()) ", ${caption.take(40)}" else ""
        is FieldNotes -> buildString {
            val parts = listOf(observed.orEmpty(), surprised.orEmpty(), learnNext.orEmpty())
                .filter { it.isNotBlank() }
            append(parts.firstOrNull()?.take(80) ?: "Empty field notes")
            if ((parts.firstOrNull()?.length ?: 0) > 80) append("…")
            if (parts.size > 1) append(" +${parts.size - 1} more")
        }
        is OpenNotebook -> buildString {
            // subFormat/subData are non-null by type, but a corrupt legacy
            // blob could decode either to null — degrade instead of crashing.
            append("Wildcard · ${subFormat?.name ?: "Wildcard"}")
            val inner = subData?.toPreview().orEmpty()
            if (inner.isNotBlank()) append(", ${inner.take(60)}")
        }
        is Portfolio -> buildString {
            val secs = sections.orEmpty()
            append("${secs.size} take${if (secs.size != 1) "s" else ""}")
            if (secs.isNotEmpty()) {
                append(" · ")
                append(secs.joinToString(" + ") { it.format.shortName })
            }
        }
    }

    /**
     * Returns full multi-line content for EntryDetail rendering.
     *
     * Same null-safety contract as [toPreview] — legacy Gson blobs decode
     * missing Kotlin-default fields to null, so every access is guarded.
     */
    fun toFullContent(): String = when (this) {
        is SoundBite -> buildString {
            appendLine("Voice note · ${durationSeconds}s")
            if (!title.isNullOrBlank()) appendLine("\"$title\"")
            if (!note.isNullOrBlank()) appendLine(note)
            quotes.orEmpty().filter { !it.isNullOrBlank() }.forEach { appendLine("\"$it\"") }
        }
        is ReelNotes -> buildString {
            if (rating > 0) appendLine("★".repeat(rating))
            val text = reviewText.orEmpty()
            if (text.isNotBlank()) {
                appendLine(text)
                if (quotes.orEmpty().any { !it.isNullOrBlank() }) appendLine()
            }
            quotes.orEmpty().filter { !it.isNullOrBlank() }.forEach { appendLine("\"$it\"") }
        }
        is Marginalia -> buildString {
            val journal = journalText.orEmpty()
            if (journal.isNotBlank()) {
                appendLine(journal)
                if (quotes.orEmpty().isNotEmpty()) appendLine()
            }
            quotes.orEmpty().forEachIndexed { i, q ->
                if (!q.isNullOrBlank()) appendLine("\"$q\"")
            }
        }
        is GalleryWall -> buildString {
            appendLine("Moodboard · $imageCount image${if (imageCount != 1) "s" else ""}")
            if (!caption.isNullOrBlank()) appendLine(caption)
            quotes.orEmpty().filter { !it.isNullOrBlank() }.forEach { appendLine("\"$it\"") }
            imageUris.orEmpty().forEach { appendLine(it) }
        }
        is FieldNotes -> buildString {
            val o = observed.orEmpty()
            val s = surprised.orEmpty()
            val l = learnNext.orEmpty()
            if (o.isNotBlank()) {
                appendLine("Observed:")
                appendLine(o)
            }
            if (s.isNotBlank()) {
                if (o.isNotBlank()) appendLine()
                appendLine("Surprised me:")
                appendLine(s)
            }
            if (l.isNotBlank()) {
                if (o.isNotBlank() || s.isNotBlank()) appendLine()
                appendLine("Want to learn next:")
                appendLine(l)
            }
            if (imageUris.orEmpty().isNotEmpty()) {
                if (o.isNotBlank() || s.isNotBlank() || l.isNotBlank()) appendLine()
                appendLine("Attached images:")
                imageUris.orEmpty().forEach { appendLine(it) }
            }
        }
        is OpenNotebook -> buildString {
            appendLine("Format: ${subFormat?.name ?: "Wildcard"}")
            append(subData?.toFullContent().orEmpty())
        }
        is Portfolio -> buildString {
            val secs = sections.orEmpty()
            secs.forEachIndexed { i, section ->
                appendLine("· ${section.format.shortName} ·")
                append(section.data.toFullContent())
                if (i != secs.lastIndex) appendLine()
            }
        }
    }

    /**
     * Every SoundBite audio file path nested inside this data — recurses
     * through OpenNotebook wrappers and Portfolio sections so delete / backup
     * flows can clean up all recordings, not just top-level ones.
     */
    fun audioFilePaths(): List<String> = when (this) {
        is SoundBite -> listOfNotNull(audioFilePath)
        is Marginalia -> listOfNotNull(audioFilePath)
        is OpenNotebook -> subData?.audioFilePaths().orEmpty()
        is Portfolio -> sections.orEmpty().flatMap { it.data.audioFilePaths() }
        else -> emptyList()
    }

    /**
     * Every attached image URI string nested inside this data — recurses
     * through OpenNotebook wrappers and Portfolio sections so backup / delete
     * flows cover all attachments, not just top-level ones.
     */
    fun imageUrisAll(): List<String> = when (this) {
        is SoundBite -> emptyList()
        is ReelNotes -> imageUris.orEmpty()
        is Marginalia -> imageUris.orEmpty()
        is GalleryWall -> imageUris.orEmpty()
        is FieldNotes -> imageUris.orEmpty()
        is OpenNotebook -> subData?.imageUrisAll().orEmpty()
        is Portfolio -> sections.orEmpty().flatMap { it.data.imageUrisAll() }
    }

    /**
     * Returns a copy of [this] data with every attached image URI rewritten
     * by [remap] — used by restore-from-backup to point attachments at the
     * image files re-homed into app storage. Recurses through OpenNotebook
     * wrappers and Portfolio sections. GalleryWall rewrites BOTH its flat
     * [CaptureData.GalleryWall.imageUris] and the per-tile layout URIs so the
     * mood board renders from its stored positions.
     */
    fun withImageUris(remap: (String) -> String): CaptureData = when (this) {
        is SoundBite -> this
        is ReelNotes -> copy(imageUris = imageUris.orEmpty().map(remap))
        is Marginalia -> copy(imageUris = imageUris.orEmpty().map(remap))
        is GalleryWall -> copy(
            imageUris = imageUris.orEmpty().map(remap),
            tileLayouts = tileLayouts.orEmpty().map { it.copy(uri = remap(it.uri)) }
        )
        is FieldNotes -> copy(imageUris = imageUris.orEmpty().map(remap))
        is OpenNotebook -> subData?.let { copy(subData = it.withImageUris(remap)) } ?: this
        is Portfolio -> copy(sections = sections.orEmpty().map { it.copy(data = it.data.withImageUris(remap)) })
    }

    /**
     * The note-paper style this capture wears — [NotePaperStyle.RULED] for
     * legacy entries that predate the style field (Gson → null). OpenNotebook
     * and Portfolio delegate to their nested data so the whole entry reads
     * one coherent style.
     */
    fun notePaperStyle(): NotePaperStyle = when (this) {
        is SoundBite -> paperStyle ?: NotePaperStyle.RULED
        is ReelNotes -> paperStyle ?: NotePaperStyle.RULED
        is Marginalia -> paperStyle ?: NotePaperStyle.RULED
        is GalleryWall -> paperStyle ?: NotePaperStyle.RULED
        is FieldNotes -> paperStyle ?: NotePaperStyle.RULED
        is OpenNotebook -> subData?.notePaperStyle() ?: NotePaperStyle.RULED
        is Portfolio -> sections.orEmpty().firstOrNull()?.data?.notePaperStyle() ?: NotePaperStyle.RULED
    }
}
