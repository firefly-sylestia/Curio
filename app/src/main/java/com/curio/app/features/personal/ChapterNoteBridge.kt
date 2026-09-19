package com.curio.app.features.personal

import com.curio.app.data.PERSONAL_INLINE_MARK
import com.curio.app.data.PersonalBlock
import com.curio.app.data.PersonalDoc
import com.curio.app.data.TextSpan
import com.curio.app.data.newBlockId

/**
 * THE BRIDGE between a CHAPTER NOTE and a CHAPTER REVIEW.
 *
 * A book that came from Curio's own catalog is ONE book in two places: the
 * topic page's book sheet (where a chapter note is written as text + runs)
 * and the personal shelf (where a chapter review is written on the block
 * canvas). They share a store — [com.curio.app.data.PersonalEntityKt]'s
 * `personal_notes` rows — so the conversion has to exist exactly once, here,
 * and it has to be lossless in both directions.
 *
 * Flag mapping (the two models are not identical, so this is the contract):
 *
 *  · bold / italic / underline — the same flag on both sides.
 *  · the note's HIGHLIGHT — carried as the canvas' QUOTE flag, because the
 *    canvas has no wash: a highlighted stretch becomes the blockquote the
 *    writing surfaces can actually paint (smaller text + the rule beside it).
 *  · per-letter font size — a note-only idea with no canvas equivalent, so it
 *    travels as the note's plain text and is not kept.
 *
 * Text is the join of the document's text blocks with newlines, and a newline
 * in the text is a block on the way back — so a note written in the sheet
 * arrives on the shelf as paragraphs, and a review written on the canvas
 * arrives in the sheet as lines.
 *
 * v398 — AND AN ATTACHMENT INSIDE A PARAGRAPH IS NOT A LINE OF THE NOTE. A note
 * holds text, so a picture that sits between the words cannot be carried: its
 * mark is taken out of the text it stands in (or the note would read with a
 * stray character in the middle of a sentence), and the block that carries the
 * picture is left out of the join entirely (or the note would gain a blank line
 * for a picture that is not on one).
 */

/** A document's blocks as a chapter note sees them: text only, no marks. */
private fun PersonalDoc.noteLines(): List<Pair<String, IntArray>> {
    val held = blocks.flatMap { it.inlineRefs }.toSet()
    return blocks
        .filterNot { it.isPhoto || it.id in held }
        .map { block ->
            val text = block.text
            val mask = runsToMask(text.length, block.runs)
            val marks = text.indices.filter { text[it] == PERSONAL_INLINE_MARK }
            if (marks.isEmpty()) {
                text to mask
            } else {
                val out = StringBuilder(text.length - marks.size)
                val outMask = ArrayList<Int>(text.length - marks.size)
                var cursor = 0
                marks.forEach { at ->
                    while (cursor < at) {
                        out.append(text[cursor])
                        outMask.add(mask.getOrElse(cursor) { 0 })
                        cursor++
                    }
                    cursor = at + 1
                }
                while (cursor < text.length) {
                    out.append(text[cursor])
                    outMask.add(mask.getOrElse(cursor) { 0 })
                    cursor++
                }
                out.toString() to outMask.toIntArray()
            }
        }
}

/** The note's plain text for a stored chapter review. */
internal fun chapterNoteText(doc: PersonalDoc): String =
    doc.noteLines().joinToString("\n") { it.first }

/** The note's rich runs for a stored chapter review. */
internal fun chapterNoteSpans(doc: PersonalDoc): List<TextSpan> {
    val spans = ArrayList<TextSpan>()
    var offset = 0
    var first = true
    doc.noteLines().forEach { (text, mask) ->
        if (!first) offset += 1 // the newline that joined the blocks
        first = false
        var i = 0
        while (i < text.length) {
            val flags = mask[i]
            var j = i + 1
            while (j < text.length && mask[j] == flags) j++
            if (flags != 0) {
                spans.add(
                    TextSpan(
                        start = offset + i,
                        end = offset + j,
                        bold = flags and FLAG_BOLD != 0,
                        italic = flags and FLAG_ITALIC != 0,
                        highlight = flags and FLAG_QUOTE != 0,
                        underline = flags and FLAG_UNDERLINE != 0
                    )
                )
            }
            i = j
        }
        offset += text.length
    }
    return spans
}

/** The block document for a chapter note: one block per line, styled by the
 *  note's runs. */
internal fun chapterNoteDoc(text: String, spans: List<TextSpan>): PersonalDoc {
    if (text.isBlank()) return PersonalDoc(emptyList())
    var cursor = 0
    val blocks = text.split('\n').map { line ->
        val start = cursor
        cursor += line.length + 1
        val mask = IntArray(line.length)
        spans.forEach { span ->
            val from = (span.start - start).coerceIn(0, line.length)
            val to = (span.end - start).coerceIn(0, line.length)
            if (to > from) {
                var flags = 0
                if (span.bold) flags = flags or FLAG_BOLD
                if (span.italic) flags = flags or FLAG_ITALIC
                if (span.underline) flags = flags or FLAG_UNDERLINE
                if (span.highlight) flags = flags or FLAG_QUOTE
                for (i in from until to) mask[i] = mask[i] or flags
            }
        }
        PersonalBlock(id = newBlockId(), text = line, runs = maskToRuns(mask))
    }
    return PersonalDoc(blocks)
}
