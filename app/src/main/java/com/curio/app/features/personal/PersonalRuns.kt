package com.curio.app.features.personal

import com.curio.app.data.PersonalRun

/**
 * v387 — THE JOURNAL'S CHARACTER-STYLE ENGINE (pure, no Compose, no I/O).
 *
 * The writing canvas edits text in a plain field, so the styling has to be
 * derived from the text itself: every tool (bold / italic / underline /
 * strike / quote) is a per-character flag, kept as a bitmask the same length
 * as the block's text.
 *
 * A mask rather than a run list is a deliberate choice: an edit shifts every
 * range after the caret, and range arithmetic (split here, clamp there, drop
 * whatever fell inside the replaced region, then re-merge) is exactly where
 * rich-text editors grow bugs that only show up after a certain combination
 * of typing and formatting. With a mask, an edit is a copy: the untouched
 * head, the freshly typed characters stamped with the style that was at the
 * caret, and the untouched tail. There is no arithmetic to get wrong, and a
 * block is a few hundred characters — the array costs nothing.
 *
 * [PersonalRun] remains the STORED shape (compact JSON, merged ranges); the
 * mask is the EDITING shape. [maskToRuns] / [runsToMask] are the only bridge,
 * so a saved note can never disagree with what the editor painted.
 */

/** Style bits. The values are stable: they are what [PersonalRun]'s booleans
 *  map onto and what a future flag has to avoid colliding with. */
internal const val FLAG_BOLD = 1
internal const val FLAG_ITALIC = 2
internal const val FLAG_UNDERLINE = 4
internal const val FLAG_STRIKE = 8
internal const val FLAG_QUOTE = 16
internal const val FLAG_TITLE = 32
internal const val FLAG_SMALL = 64
internal const val FLAG_BULLET = 128
internal const val FLAG_CHECKBOX = 256

/** Every style bit, in toolbar order. */
internal val ALL_FLAGS = intArrayOf(
    FLAG_BOLD, FLAG_ITALIC, FLAG_UNDERLINE, FLAG_STRIKE, FLAG_QUOTE,
    FLAG_TITLE, FLAG_SMALL, FLAG_BULLET, FLAG_CHECKBOX
)

/**
 * Every style bit in ONE mask — the value a line starts from when the canvas
 * asks what a whole line is wearing (see `PersonalEditorState.lineFlags`,
 * which ANDs each character's own bits into it). [ALL_FLAGS] is the toolbar's
 * ORDER, an array to iterate; this is the same bits as a mask, which is a
 * different thing and the reason the two are not interchangeable.
 */
internal val ALL_FLAGS_MASK = ALL_FLAGS.fold(0) { mask, flag -> mask or flag }

// ── v389 — THE MARKER PEN ─────────────────────────────────────────────
//
// A highlighter is a COLOUR, not a yes/no, and the mask is one int per
// character — so the colour lives in the three bits directly above the flags
// (the flags stop at 256) and a character's whole appearance stays ONE int.
//
// That is not a trick for its own sake. [maskToRuns] merges neighbouring
// characters by comparing their ints, so two words written with different pens
// are already two runs and two words written with the same pen are already one —
// the marker needs no merging logic of its own, and an edit that shifts text
// around carries it for free, because every copy of the mask is a copy of the
// colour with it.

/** The pens, in the order the menu lists them. */
internal val PERSONAL_HIGHLIGHT_KEYS = listOf("amber", "rose", "sage", "sky")

internal const val HIGHLIGHT_SHIFT = 9
internal const val HIGHLIGHT_BITS = 0x0E00

// ── v389 — THE FACE ───────────────────────────────────────────────────
//
// Two bits above the marker, giving FOUR faces: the page's own writing serif
// (0 — so an old note is byte-for-byte the note it was), a sans, a mono and a
// display serif. Same reasoning as [HIGHLIGHT_BITS]: the mask is one int per
// character, the merge already splits runs by value, and a face is copied
// around by every edit that copies the mask.

/** The faces, in the order the menu lists them. Index 0 is the page's own. */
internal val PERSONAL_FONT_KEYS = listOf("", "sans", "mono", "display")

internal const val FONT_SHIFT = 12
internal const val FONT_BITS = 0x3000

/** The face a mask was set in — "" for the page's own. */
internal fun fontKeyOf(mask: Int): String =
    PERSONAL_FONT_KEYS.getOrNull((mask and FONT_BITS) shr FONT_SHIFT).orEmpty()

/** The bits a face key means; 0 for the page's own, which is the default. */
internal fun fontMaskFor(key: String?): Int {
    val index = PERSONAL_FONT_KEYS.indexOf(key.orEmpty())
    return (if (index < 0) 0 else index) shl FONT_SHIFT
}

/** A copy of [mask] with every character from [start] to [end] set in the face
 *  [key] names. */
internal fun maskApplyFont(mask: IntArray, start: Int, end: Int, key: String): IntArray {
    if (end <= start) return mask
    val bits = fontMaskFor(key)
    val out = mask.copyOf()
    for (i in start until end.coerceAtMost(out.size)) {
        out[i] = (out[i] and FONT_BITS.inv()) or bits
    }
    return out
}

/** The pen a mask was written with: "" for none. */
internal fun highlightKeyOf(mask: Int): String =
    PERSONAL_HIGHLIGHT_KEYS.getOrNull(((mask and HIGHLIGHT_BITS) shr HIGHLIGHT_SHIFT) - 1).orEmpty()

/** The bits a pen key means, or 0 for none. */
internal fun highlightMaskFor(key: String?): Int {
    val index = PERSONAL_HIGHLIGHT_KEYS.indexOf(key.orEmpty())
    return if (index < 0) 0 else (index + 1) shl HIGHLIGHT_SHIFT
}

/** A copy of [mask] with [key]'s pen over [start] until [end], or with the
 *  marker taken off entirely when [key] is null. */
internal fun maskApplyHighlight(
    mask: IntArray,
    start: Int,
    end: Int,
    key: String?
): IntArray {
    if (end <= start) return mask
    val bits = highlightMaskFor(key)
    val out = mask.copyOf()
    for (i in start until end.coerceAtMost(out.size)) {
        out[i] = (out[i] and HIGHLIGHT_BITS.inv()) or bits
    }
    return out
}

/** An empty mask of [length] characters. */
internal fun emptyMask(length: Int): IntArray = IntArray(length)

/** The stored runs of a block expanded into one bitmask per character. */
internal fun runsToMask(textLength: Int, runs: List<PersonalRun>): IntArray {
    val mask = IntArray(textLength)
    runs.forEach { run ->
        val start = run.start.coerceIn(0, textLength)
        val end = run.end.coerceIn(start, textLength)
        // ── THE PEN AND THE FACE COME FIRST (v389d) ────────────────────
        //
        // They travel in the same int as the flags, but they are NOT flags: a
        // stretch of words can be marked up with a highlighter and nothing else,
        // and this used to skip exactly that case — the pen and the face were
        // only OR-ed in once some OTHER flag was already set, and a run with
        // neither then bailed out entirely (`if (flags == 0) return`). So a
        // pen-only run was silently dropped every time runs were turned back
        // into a mask, which is every page load, every save's read-back, and
        // every Enter (splitting a line REBUILDS its runs) — the marker the
        // member had just laid down vanished the moment they pressed Enter, and
        // a page read back showed no highlights at all (user reports: "when i do
        // enter then the highlighter of the previous texts disappear also in eye
        // view the highlighter doest show").
        var flags = highlightMaskFor(run.highlight) or fontMaskFor(run.font)
        if (run.bold) flags = flags or FLAG_BOLD
        if (run.italic) flags = flags or FLAG_ITALIC
        if (run.underline) flags = flags or FLAG_UNDERLINE
        if (run.strike) flags = flags or FLAG_STRIKE
        if (run.quote) flags = flags or FLAG_QUOTE
        if (run.title) flags = flags or FLAG_TITLE
        if (run.small) flags = flags or FLAG_SMALL
        if (run.bullet) flags = flags or FLAG_BULLET
        if (run.checkbox) flags = flags or FLAG_CHECKBOX
        if (flags == 0) return@forEach
        for (i in start until end) mask[i] = mask[i] or flags
    }
    return mask
}

/** The mask collapsed back into merged runs (only non-zero stretches are
 *  stored, and neighbouring identical stretches are one run). */
internal fun maskToRuns(mask: IntArray): List<PersonalRun> {
    val runs = ArrayList<PersonalRun>()
    var i = 0
    while (i < mask.size) {
        val flags = mask[i]
        if (flags == 0) {
            i++
            continue
        }
        var j = i + 1
        while (j < mask.size && mask[j] == flags) j++
        runs.add(
            PersonalRun(
                start = i,
                end = j,
                bold = flags and FLAG_BOLD != 0,
                italic = flags and FLAG_ITALIC != 0,
                underline = flags and FLAG_UNDERLINE != 0,
                strike = flags and FLAG_STRIKE != 0,
                quote = flags and FLAG_QUOTE != 0,
                title = flags and FLAG_TITLE != 0,
                small = flags and FLAG_SMALL != 0,
                bullet = flags and FLAG_BULLET != 0,
                checkbox = flags and FLAG_CHECKBOX != 0,
                highlight = highlightKeyOf(flags),
                font = fontKeyOf(flags)
            )
        )
        i = j
    }
    return runs
}

/**
 * The mask of a block after its text changed from [oldText] to [newText].
 *
 * The head before the first difference and the tail after the last one are
 * carried over untouched; everything the writer actually typed is stamped
 * with [armed] when they have a tool switched on with nothing selected, and
 * with the style that sat at the caret otherwise (typing inside a bold phrase
 * stays bold — that is the behaviour every writing app has, and losing it
 * after one edit is what makes a marker-based editor feel broken).
 *
 * v389 — [cleared] is the other half of that promise: the tools switched OFF
 * for what comes next. With no selection the dock is an INPUT STYLE (see
 * `PersonalEditorState.toggle`), so turning bold off inside a bold phrase has
 * to reach the very next keystroke — and the keystrokes after it, which keep
 * inheriting from the now-unbold character the caret leaves behind.
 */
internal fun maskAfterEdit(
    oldText: String,
    newText: String,
    mask: IntArray,
    armed: Int,
    cleared: Int = 0,
    /**
     * v389 — the MARKER the typed words wear: null keeps whatever pen the caret
     * already sat in (the same rule the flags follow), and `0` is the pen being
     * taken off, which a nullable Int says and a plain Int cannot.
     */
    highlightOverride: Int? = null,
    /** v389 — the FACE the typed words are set in, on the same terms. */
    fontOverride: Int? = null
): IntArray {
    val oldLength = oldText.length
    val newLength = newText.length
    var prefix = 0
    while (prefix < oldLength && prefix < newLength && oldText[prefix] == newText[prefix]) prefix++
    var suffix = 0
    while (
        suffix < oldLength - prefix &&
        suffix < newLength - prefix &&
        oldText[oldLength - 1 - suffix] == newText[newLength - 1 - suffix]
    ) suffix++
    val oldRegionEnd = oldLength - suffix
    val newRegionEnd = newLength - suffix

    // What the caret already sat in — typing at the start of a styled run
    // continues that run.
    val inherited = when {
        prefix - 1 in 0 until oldLength && mask.getOrZero(prefix - 1) != 0 -> mask[prefix - 1]
        mask.getOrZero(prefix) != 0 -> mask[prefix]
        mask.getOrZero(oldRegionEnd) != 0 -> mask[oldRegionEnd]
        else -> 0
    }
    // The INPUT STYLE: the tools switched on (which replace the inherited style
    // outright), then the tools switched off taken back out of it. The marker is
    // its own axis — a pen is not a bold.
    val typedFlags = ((if (armed != 0) 0 else inherited and ALL_FLAGS_MASK) and cleared.inv()) or armed
    val typed = typedFlags or
        (highlightOverride ?: (inherited and HIGHLIGHT_BITS)) or
        (fontOverride ?: (inherited and FONT_BITS))

    val out = IntArray(newLength)
    for (i in 0 until prefix.coerceAtMost(newLength)) out[i] = mask.getOrZero(i)
    for (i in prefix until newRegionEnd) out[i] = typed
    var i = newRegionEnd
    var j = oldRegionEnd
    while (i < newLength && j < oldLength) {
        out[i] = mask[j]
        i++
        j++
    }
    return out
}

private fun IntArray.getOrZero(index: Int): Int =
    if (index in indices) this[index] else 0

/** True when EVERY character of [start] until [end] carries [flag]. */
internal fun maskCovers(mask: IntArray, start: Int, end: Int, flag: Int): Boolean {
    if (end <= start) return false
    for (i in start until end.coerceAtMost(mask.size)) {
        if (mask[i] and flag == 0) return false
    }
    return true
}

/** True when every character of the range carries ALL of [flags]. */
internal fun maskCoversAll(mask: IntArray, start: Int, end: Int, flags: Int): Boolean {
    if (end <= start || flags == 0) return false
    for (i in start until end.coerceAtMost(mask.size)) {
        if (mask[i] and flags != flags) return false
    }
    return true
}

/** A copy of [mask] with [flag] switched [on] over [start] until [end]. */
internal fun maskApply(
    mask: IntArray,
    start: Int,
    end: Int,
    flag: Int,
    on: Boolean
): IntArray {
    if (end <= start) return mask
    val out = mask.copyOf()
    for (i in start until end.coerceAtMost(out.size)) {
        out[i] = if (on) out[i] or flag else out[i] and flag.inv()
    }
    return out
}

/** A copy of [mask] with EVERY bit of [flags] set over the range (the armed
 *  tools applied to a freshly typed stretch). */
internal fun maskApplyAll(mask: IntArray, start: Int, end: Int, flags: Int): IntArray {
    if (end <= start || flags == 0) return mask
    val out = mask.copyOf()
    for (i in start until end.coerceAtMost(out.size)) out[i] = out[i] or flags
    return out
}
