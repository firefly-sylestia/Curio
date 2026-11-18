package com.curio.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import kotlin.math.max

/**
 * A PICTURE THAT TRAVELS AS TEXT (v513).
 *
 * Curio's online layer is text only on purpose: no storage bucket, no media
 * columns, no signed URLs. A member asked for pictures anyway — and the shape
 * that fits that contract is an **SVG document stored as a string**, so a card
 * crosses devices through the same table its words do.
 *
 * Two encodings are produced, and [encode] picks between them by SIZE:
 *
 * 1. **Traced (vector).** The image is reduced to a small palette and each
 *    colour is emitted as merged `<rect>` runs. This is what makes a logo, an
 *    icon or an illustration come out as real vector art: a handful of colours
 *    and a few thousand rectangles.
 * 2. **Embedded (still SVG text).** A photograph cannot be vectorised without
 *    turning into path soup — every pixel becomes a shape and the file grows
 *    far past the original. When the traced form is too big, the picture is
 *    stored as a downscaled JPEG inside an SVG `<image>` element instead: the
 *    text stays bounded, the photograph stays recognisable, and it is still one
 *    string in one text column.
 *
 * [embeddedBitmap] reads form 2 back without any SVG renderer (it pulls out the
 * base64 payload), while form 1 is drawn by Coil's `SvgDecoder` — the app
 * already registers it in `MainActivity`.
 *
 * Everything here is deterministic and synchronous: call it from IO, never from
 * a composable's composition.
 */
object SvgPicture {

    /** The database's own ceiling (mirrors `community_cards_svg_len`). */
    const val MAX_CHARS = 1_200_000

    /**
     * Longest edge the TRACER works at — larger buys nothing but characters.
     *
     * Tracing costs O(pixels × palette), so the vector path works on a
     * thumbnail of the picture; the photograph path does not (see [PHOTO_EDGE]).
     */
    const val MAX_EDGE = 320

    /** Colours in a traced picture. Sixteen reads as art, not as a broken GIF. */
    private const val TRACE_PALETTE = 16

    /**
     * Longest edge the embedded (photograph) form is decoded and kept at.
     *
     * v517 — THIS IS THE PHOTOGRAPH'S OWN SIZE, and it used to be dead code:
     * [fromUri] decoded the picture at [MAX_EDGE] and handed THAT to the
     * encoder, so the fallback could never be larger than 320px however it was
     * configured, and every photograph on the wall was a 320px thumbnail. The
     * big decode now happens on the embedded path alone — see [fromUri].
     */
    private const val PHOTO_EDGE = 1080

    /** JPEG quality for the embedded form — small enough to stay under the cap. */
    private const val PHOTO_QUALITY = 90

    /** Where the embedded form stops stepping down: a photograph of noise fits here. */
    private const val MIN_EDGE = 128

    /** Floor for the embedded form's quality while it steps down. */
    private const val MIN_QUALITY = 60

    /** Tallest a framed picture is DRAWN — a portrait phone photo (9:16). */
    const val MIN_FRAME_RATIO = 0.55f

    /** Widest a framed picture is DRAWN — a wide panorama (12:5). */
    const val MAX_FRAME_RATIO = 2.4f

    /** The dimensions every document this object writes states in its root tag. */
    private val VIEW_BOX = Regex("viewBox\\s*=\\s*\"0 0 ([0-9.]+) ([0-9.]+)\"")

    /**
     * Reads a picked image and answers the SVG document to store.
     *
     * Null when the URI cannot be decoded (a deleted file, a permission that
     * was never granted) — the caller reports that rather than posting a card
     * with an empty picture.
     */
    fun fromUri(context: Context, uri: Uri): String? = runCatching {
        val bounds = boundsOf(context, uri) ?: return null

        // 1. THE VECTOR ATTEMPT, from a thumbnail. A few thousand merged
        //    rectangles are what a logo, an icon or an illustration comes out
        //    as, and 320px is enough of a copy to decide that with.
        val tracedSource = decodeAt(context, uri, bounds, MAX_EDGE) ?: return null
        val traced = trace(tracedSource)
        tracedSource.recycle()
        if (traced.length in 1..MAX_CHARS) return traced

        // 2. THE PHOTOGRAPH, from a real decode. Tracing failed because the
        //    palette form grew past the budget, which means this is a
        //    photograph (or close to one) — and a photograph read at 320px is a
        //    smudge. It is decoded AGAIN at [PHOTO_EDGE] and compressed to fit
        //    the ceiling, which is the picture the wall actually deserves.
        val photoSource = decodeAt(context, uri, bounds, PHOTO_EDGE) ?: return null
        val document = embedded(photoSource)
        photoSource.recycle()
        document
    }.getOrNull()

    /** The picture's own pixel size, read without allocating the bitmap. */
    private fun boundsOf(context: Context, uri: Uri): BitmapFactory.Options? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        return bounds.takeIf { it.outWidth > 0 && it.outHeight > 0 }
    }

    /**
     * [uri] decoded so that its LONGEST edge is [maxEdge] — and its ratio left
     * exactly as the picture has it: only a scale factor is applied, never a
     * crop and never a box.
     */
    private fun decodeAt(
        context: Context,
        uri: Uri,
        bounds: BitmapFactory.Options,
        maxEdge: Int
    ): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxEdge)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return null
        val scaled = scaleDown(decoded, maxEdge)
        if (scaled !== decoded) decoded.recycle()
        return scaled
    }

    /**
     * Turns a bitmap into an SVG document, preferring the traced form and
     * falling back to the embedded one when tracing would blow the budget.
     */
    fun encode(source: Bitmap): String {
        if (source.width <= 0 || source.height <= 0) return ""
        val traced = trace(source)
        if (traced.length in 1..MAX_CHARS) return traced
        return embedded(source)
    }

    /** True when [svg] looks like a document we wrote (a card may carry none). */
    fun isPicture(svg: String): Boolean =
        svg.isNotBlank() && svg.trimStart().startsWith("<svg", ignoreCase = true)

    /**
     * The document's own width ÷ height, read from the `viewBox` it states.
     *
     * A picture is not one shape: a screenshot is tall, a camera photograph is
     * usually 4:3 or 3:2, a phone held upright is 9:16, a panorama runs wide.
     * Every document this object writes carries its real dimensions, so a
     * surface can draw the picture in the shape it actually has instead of
     * squeezing it into one band. [fallback] answers for a document that says
     * nothing about its size.
     */
    fun ratioOf(svg: String, fallback: Float = 1f): Float {
        val match = VIEW_BOX.find(svg) ?: return fallback
        val width = match.groupValues[1].toFloatOrNull() ?: return fallback
        val height = match.groupValues[2].toFloatOrNull() ?: return fallback
        if (width <= 0f || height <= 0f) return fallback
        return width / height
    }

    /**
     * The ratio a framed picture is DRAWN at — [ratioOf] held inside
     * [MIN_FRAME_RATIO]..[MAX_FRAME_RATIO].
     *
     * Every ordinary picture lands exactly on its own shape: 3:2 is 1.5, an
     * upright phone photo is 0.56, a square is 1, a 4:5 portrait is 0.8. The
     * band only catches shapes that would otherwise take over the page (a 9:1
     * strip of sky, a 1:6 screenshot of a chat), and the excess is CROPPED by
     * the frame rather than stretched, so no picture is ever squashed.
     */
    fun frameRatio(svg: String): Float =
        ratioOf(svg).coerceIn(MIN_FRAME_RATIO, MAX_FRAME_RATIO)

    /**
     * The bitmap inside an EMBEDDED document, or null for a traced one (that
     * one is rendered by Coil's SVG decoder, and decoding it by hand here would
     * be a second renderer to keep honest).
     */
    fun embeddedBitmap(svg: String): Bitmap? {
        val marker = "base64,"
        val at = svg.indexOf(marker)
        if (at < 0) return null
        val start = at + marker.length
        val quote = svg.indexOf('"', start)
        val chevron = svg.indexOf('<', start)
        val end = when {
            quote > start -> quote
            chevron > start -> chevron
            else -> svg.length
        }
        if (end <= start) return null
        return runCatching {
            val bytes = Base64.decode(svg.substring(start, end), Base64.DEFAULT)
            if (bytes.isEmpty()) null else BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }

    // ── the tracer ──────────────────────────────────────────────────────────

    /**
     * Palette + merged rectangles.
     *
     * Runs are merged DOWNWARDS as well as across, which is the difference
     * between a few thousand rectangles and one per pixel run: a flat area
     * becomes a single tall `<rect>` instead of a stack of one-row strips.
     */
    private fun trace(source: Bitmap): String {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        val palette = paletteOf(pixels, TRACE_PALETTE)
        if (palette.isEmpty()) return ""
        val index = IntArray(pixels.size) { nearestIndex(palette, pixels[it]) }

        val body = StringBuilder()
        for (colour in palette.indices) {
            val open = HashMap<Long, Int>()
            for (y in 0..height) {
                val runs = ArrayList<Long>()
                if (y < height) {
                    var x = 0
                    while (x < width) {
                        if (index[y * width + x] == colour) {
                            val start = x
                            while (x < width && index[y * width + x] == colour) x++
                            runs.add((start.toLong() shl 16) or (x - start).toLong())
                        } else {
                            x++
                        }
                    }
                }

                // Close the runs that stopped on this row.
                val iterator = open.entries.iterator()
                while (iterator.hasNext()) {
                    val entry = iterator.next()
                    if (runs.binarySearch(entry.key) < 0) {
                        appendRect(body, entry.key, entry.value, y, palette[colour])
                        iterator.remove()
                    }
                }
                // Open the runs that started on this row.
                for (run in runs) {
                    if (!open.containsKey(run)) open[run] = y
                }
            }
        }

        return buildString {
            append("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 ")
            append(width).append(' ').append(height)
            append("\" width=\"").append(width).append("\" height=\"").append(height)
            append("\" shape-rendering=\"crispEdges\">")
            append(body)
            append("</svg>")
        }
    }

    private fun appendRect(out: StringBuilder, run: Long, top: Int, bottom: Int, colour: Int) {
        val x = (run shr 16).toInt()
        val width = (run and 0xFFFF).toInt()
        val height = bottom - top
        if (width <= 0 || height <= 0) return
        out.append("<rect x=\"").append(x)
            .append("\" y=\"").append(top)
            .append("\" width=\"").append(width)
            .append("\" height=\"").append(height)
            .append("\" fill=\"#").append(hex(colour)).append("\"/>")
    }

    /** The most common colours, coarsely binned — deterministic, no dithering. */
    private fun paletteOf(pixels: IntArray, limit: Int): IntArray {
        val counts = IntArray(32 * 32 * 32)
        for (pixel in pixels) {
            if (Color.alpha(pixel) < 24) continue
            counts[bin(pixel)]++
        }
        val keys = counts.indices
            .filter { counts[it] > 0 }
            .sortedByDescending { counts[it] }
            .take(limit)
        if (keys.isEmpty()) return IntArray(0)
        return keys.map { key ->
            val red = ((key shr 10) and 31) * 255 / 31
            val green = ((key shr 5) and 31) * 255 / 31
            val blue = (key and 31) * 255 / 31
            Color.rgb(red, green, blue)
        }.toIntArray()
    }

    private fun bin(pixel: Int): Int =
        ((Color.red(pixel) shr 3) shl 10) or
            ((Color.green(pixel) shr 3) shl 5) or
            (Color.blue(pixel) shr 3)

    private fun nearestIndex(palette: IntArray, pixel: Int): Int {
        val red = Color.red(pixel)
        val green = Color.green(pixel)
        val blue = Color.blue(pixel)
        var best = 0
        var bestDistance = Int.MAX_VALUE
        for (index in palette.indices) {
            val candidate = palette[index]
            val dr = red - Color.red(candidate)
            val dg = green - Color.green(candidate)
            val db = blue - Color.blue(candidate)
            val distance = dr * dr + dg * dg + db * db
            if (distance < bestDistance) {
                bestDistance = distance
                best = index
            }
        }
        return best
    }

    private fun hex(colour: Int): String = String.format(
        "%02X%02X%02X",
        Color.red(colour),
        Color.green(colour),
        Color.blue(colour)
    )

    // ── the embedded fallback ───────────────────────────────────────────────

    /**
     * A photograph as SVG text: one `<image>` whose `href` is a data URI,
     * stepped down until the document fits [MAX_CHARS].
     *
     * This is the honest form of "PNG/JPG converted to SVG" for a picture that
     * is not flat art — vectorising a photograph produces a bigger, mangled
     * file, while this keeps the same visual result in bounded text.
     *
     * THE CEILING IS ENFORCED HERE TOO, and that is not belt-and-braces: the
     * traced document is measured in [encode], but this one is produced AFTER
     * that measurement, so it was the one path that could leave the encoder over
     * the database's `community_cards_svg_len` check — a detailed photograph
     * base64s to several times the original bytes, and the post then failed at
     * the last step with `new row … violates check constraint
     * community_cards_svg_len` after the words were written and the picture was
     * already showing in the preview. So the resolution and the quality step
     * down until the document fits; at [MIN_EDGE] even noise is tens of
     * thousands of characters, so this always ends.
     */
    private fun embedded(source: Bitmap): String {
        var edge = PHOTO_EDGE
        var quality = PHOTO_QUALITY
        while (true) {
            val document = embeddedAt(source, edge, quality)
            if (document.length <= MAX_CHARS || edge <= MIN_EDGE) return document
            edge = max(MIN_EDGE, edge / 2)
            quality = max(MIN_QUALITY, quality - 15)
        }
    }

    /** One attempt at the embedded form: [source] as a JPEG of [edge], [quality]. */
    private fun embeddedAt(source: Bitmap, edge: Int, quality: Int): String {
        val scaled = scaleDown(source, edge)
        // The document states the JPEG's OWN dimensions, which keep the
        // picture's ratio exactly (a scale is all [scaleDown] does) — that is
        // what every surface reads back to draw it in its own shape.
        val width = scaled.width
        val height = scaled.height
        val stream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        if (scaled !== source) scaled.recycle()
        val payload = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
        return buildString {
            append("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 ")
            append(width).append(' ').append(height)
            append("\" width=\"").append(width).append("\" height=\"").append(height)
            append("\"><image width=\"").append(width).append("\" height=\"").append(height)
            append("\" preserveAspectRatio=\"xMidYMid slice\" href=\"data:image/jpeg;base64,")
            append(payload)
            append("\"/></svg>")
        }
    }

    // ── plumbing ────────────────────────────────────────────────────────────

    private fun sampleSize(width: Int, height: Int, maxEdge: Int): Int {
        var sample = 1
        var wide = width
        var tall = height
        while (max(wide, tall) / 2 >= maxEdge) {
            sample *= 2
            wide /= 2
            tall /= 2
        }
        return sample
    }

    private fun scaleDown(bitmap: Bitmap, maxEdge: Int): Bitmap {
        val edge = max(bitmap.width, bitmap.height)
        if (edge <= maxEdge) return bitmap
        val ratio = maxEdge.toFloat() / edge
        val width = max(1, (bitmap.width * ratio).toInt())
        val height = max(1, (bitmap.height * ratio).toInt())
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }
}
