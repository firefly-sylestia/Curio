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

/** Every style bit, in toolbar order. */
internal val ALL_FLAGS = intArrayOf(
    FLAG_BOLD, FLAG_ITALIC, FLAG_UNDERLINE, FLAG_STRIKE, FLAG_QUOTE,
    FLAG_TITLE, FLAG_SMALL, FLAG_BULLET
)

/** An empty mask of [length] characters. */
internal fun emptyMask(length: Int): IntArray = IntArray(length)

/** The stored runs of a block expanded into one bitmask per character. */
internal fun runsToMask(textLength: Int, runs: List<PersonalRun>): IntArray {
    val mask = IntArray(textLength)
    runs.forEach { run ->
        val start = run.start.coerceIn(0, textLength)
        val end = run.end.coerceIn(start, textLength)
        var flags = 0
        if (run.bold) flags = flags or FLAG_BOLD
        if (run.italic) flags = flags or FLAG_ITALIC
        if (run.underline) flags = flags or FLAG_UNDERLINE
        if (run.strike) flags = flags or FLAG_STRIKE
        if (run.quote) flags = flags or FLAG_QUOTE
        if (run.title) flags = flags or FLAG_TITLE
        if (run.small) flags = flags or FLAG_SMALL
        if (run.bullet) flags = flags or FLAG_BULLET
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
                bullet = flags and FLAG_BULLET != 0
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
 */
internal fun maskAfterEdit(
    oldText: String,
    newText: String,
    mask: IntArray,
    armed: Int
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

    val typed = when {
        armed != 0 -> armed
        // Nothing armed: inherit from the character just left of the caret,
        // else the one just right of it (typing at the start of a styled run
        // continues that run).
        prefix - 1 in 0 until oldLength && mask.getOrZero(prefix - 1) != 0 -> mask[prefix - 1]
        mask.getOrZero(prefix) != 0 -> mask[prefix]
        mask.getOrZero(oldRegionEnd) != 0 -> mask[oldRegionEnd]
        else -> 0
    }

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
