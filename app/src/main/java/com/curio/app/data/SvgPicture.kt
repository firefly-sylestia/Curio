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
    const val MAX_CHARS = 180_000

    /** Longest edge the tracer works at — larger buys nothing but characters. */
    const val MAX_EDGE = 192

    /** Colours in a traced picture. Sixteen reads as art, not as a broken GIF. */
    private const val TRACE_PALETTE = 16

    /** Longest edge the embedded fallback is resampled to. */
    private const val PHOTO_EDGE = 320

    /** JPEG quality for the embedded form — small enough to stay under the cap. */
    private const val PHOTO_QUALITY = 68

    /**
     * Reads a picked image and answers the SVG document to store.
     *
     * Null when the URI cannot be decoded (a deleted file, a permission that
     * was never granted) — the caller reports that rather than posting a card
     * with an empty picture.
     */
    fun fromUri(context: Context, uri: Uri): String? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, MAX_EDGE)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return null

        val scaled = scaleDown(decoded, MAX_EDGE)
        val svg = encode(scaled)
        if (scaled !== decoded) decoded.recycle()
        svg
    }.getOrNull()

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
     * A photograph as SVG text: one `<image>` whose `href` is a data URI.
     *
     * This is the honest form of "PNG/JPG converted to SVG" for a picture that
     * is not flat art — vectorising a photograph produces a bigger, mangled
     * file, while this keeps the same visual result in bounded text.
     */
    private fun embedded(source: Bitmap): String {
        val width = source.width
        val height = source.height
        val scaled = scaleDown(source, PHOTO_EDGE)
        val stream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, PHOTO_QUALITY, stream)
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
