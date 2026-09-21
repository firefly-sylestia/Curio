package com.curio.app.features.community

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import java.text.Normalizer
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * v435 — THE BLOBATAR, PORTED INTO CURIO.
 *
 * A member's portrait used to be one of 28 hand-drawn characters indexed by an
 * `avatar_style` integer (`PORTRAITS` + `ICONS`, ~1,300 lines of Canvas art).
 * That set had two problems the member named in one line — "instead of those bad
 * drawing" — and both are structural rather than cosmetic: with 28 rows, every
 * member on a page is one of 28 pictures, and a member can only ever be handed a
 * look somebody else already has.
 *
 * So the cast is gone and the portrait is now DERIVED. This file is a faithful
 * Kotlin port of **[blobatar](https://github.com/Alain00/blobatar)**'s gen-2 core
 * (`blobatar` v2: `hash.ts` + `traits.ts` + `color.ts` + `shape.ts` +
 * `styles/blob.ts` + `styles/compose.ts` + `styles/shapes.ts`), which turns ANY
 * string into a deterministic geometric face: ten weighted silhouettes, two
 * capsule eyes, and a tone drawn from six authored swatches.
 *
 * ## Why the port and not the endpoint
 *
 * blobatar also ships `https://blobatar.dev/avatar/<name>`, which would be ~40
 * lines and a Coil call. It was refused for two reasons that are contract, not
 * taste: a social avatar must render **offline and instantly** in a list that
 * scrolls, and — the one that settles it — a notification's large icon is an
 * `android.graphics.Bitmap` painted from a receiver with no composition and no
 * network. A remote avatar cannot be drawn there, and a notification-only
 * lookalike would drift from the faces the app shows. Deriving it locally is the
 * only shape that serves both.
 *
 * ## The three guarantees the port must keep (they are the reason it is a port)
 *
 * 1. **Avalanche.** "alain" and "alaim" must produce unrelated faces. Plain
 *    FNV-1a does not give that; the murmur3 finalizer does. Do not "simplify"
 *    [finalize] away.
 * 2. **Per-trait independence.** The seed is hashed ONCE and every trait key
 *    continues from that same state, so trait keys are an append-only namespace:
 *    adding a trait in a later version cannot disturb any existing face. Which
 *    is also why the order the traits are READ in never matters — that is a
 *    property worth keeping, not an accident.
 * 3. **Contrast.** Eyes clear 4.5:1 against the head and the head clears its
 *    backdrop, enforced in OKLCh against real sRGB luminance (see [ensureContrast]
 *    and [FLOORS]). This is what makes a face legible at 26dp in a comment row.
 *
 * ## What "faithful" means here
 *
 * Every constant, range, band edge, tone threshold and Bézier control offset is
 * carried over unchanged, and the arithmetic runs in [Double] exactly as
 * JavaScript numbers do — only the final path coordinates narrow to [Float],
 * which is what the canvas takes. So a seed renders the same face here as it does
 * in blobatar's own renderer (modulo the 1/100-unit path rounding blobatar does
 * for its SVG strings and we do not need). Re-read the upstream module named in
 * each section's header before changing a number in it.
 */

// ────────────────────────────────────────────────────────────────────────────
// The seed
// ────────────────────────────────────────────────────────────────────────────

/**
 * What a member's face is derived from: their **username if they have one, else
 * their account id** (member's answer).
 *
 * The handle is what a member chose and what other members see printed beside
 * the face, so a rename is expected to change the face — that is the point of
 * deriving it from a name rather than storing a number. The account id is the
 * fallback for a profile that has not claimed a name yet, and it is stable, so
 * the face does not shuffle while they type one in.
 *
 * The `@` is stripped because it is punctuation the app adds for display, and a
 * blank pair falls back to a constant rather than to an empty string — a seed of
 * `""` is a perfectly valid face, but "nobody" reading as one specific face is
 * worse than a shared neutral one.
 */
internal fun blobatarSeed(userId: String?, username: String? = null): String {
    val handle = username.orEmpty().trim().removePrefix("@")
    return handle.ifBlank { userId.orEmpty().trim() }.ifBlank { "curio" }
}

// ────────────────────────────────────────────────────────────────────────────
// hash.ts — seed hashing
// ────────────────────────────────────────────────────────────────────────────

private const val SEP = 0xFF

/** Mixes bytes into a 32-bit state. Kotlin's `Int` overflow IS `Math.imul`. */
private fun feed(h: Int, bytes: ByteArray): Int {
    var state = h
    for (byte in bytes) {
        state = (state xor (byte.toInt() and 0xFF)) * 0xCC9E2D51.toInt()
        state = (state shl 13) or (state ushr 19)
    }
    return state
}

/**
 * murmur3 `fmix32` — a bijection on uint32 with full avalanche.
 *
 * Returns the UNSIGNED value as a [Double] in [0, 4294967296), which is what
 * `>>> 0` means in the original. The whole avalanche guarantee lives in these
 * three multiplies; shortening this to one is the one change that silently
 * ruins the library.
 */
private fun finalize(h: Int): Double {
    var x = h
    x = (x xor (x ushr 16)) * 0x85EBCA6B.toInt()
    x = (x xor (x ushr 13)) * 0xC2B2AE35.toInt()
    return (x xor (x ushr 16)).toUInt().toDouble()
}

/**
 * Normalizes a seed so that inputs a human considers equal hash equally.
 *
 * NFC first, so precomposed "é" and decomposed "é" agree; then trim, then
 * lowercase. Without this, `Alain@x.com` and `alain@x.com` produce different
 * faces for the same person — which gets reported as a bug, every time.
 */
private fun normalizeSeed(seed: String): String =
    Normalizer.normalize(seed, Normalizer.Form.NFC).trim().lowercase()

/**
 * Hashes the seed once into a reusable state.
 *
 * Note the deliberate asymmetry carried over from upstream: the INITIAL state
 * mixes `s.length` — UTF-16 code units, which is what `String.length` means in
 * both JavaScript and Kotlin — while the body is fed UTF-8 BYTES. Both halves
 * matter for byte-identical output and neither is interchangeable.
 */
private fun seedState(seed: String, normalize: Boolean = true): Int {
    val s = if (normalize) normalizeSeed(seed) else seed
    return feed(1779033703 xor s.length, s.toByteArray(Charsets.UTF_8))
}

/** Derives one uniform float in [0, 1) for [key], independent of every other key. */
private fun stream(state: Int, key: String): Double =
    finalize(
        feed(
            feed(state, byteArrayOf(SEP.toByte())),
            key.toByteArray(Charsets.UTF_8)
        )
    ) / 4294967296.0

// ────────────────────────────────────────────────────────────────────────────
// traits.ts — the trait reader
// ────────────────────────────────────────────────────────────────────────────

/**
 * Every value is addressed by a string key rather than drawn from a sequential
 * stream, so definition order is not part of the seed→look mapping. Read through
 * [num] / [int] / [jitter] rather than [get] wherever the layout names a range —
 * the ranges are what makes a retune a visible decision.
 *
 * Values are memoized not as an optimization but as a *stability* statement:
 * `t.num("eye.n", …)` is read once per eye upstream, and one key must never read
 * as two values.
 */
private class BlobatarTraits(seed: String) {

    private val state = seedState(seed)
    private val memo = HashMap<String, Double>(64)

    /** Uniform float in [0, 1). */
    operator fun get(key: String): Double = memo.getOrPut(key) { stream(state, key) }

    /** Uniform float in [min, max). */
    fun num(key: String, min: Double, max: Double): Double = min + get(key) * (max - min)

    /** Uniform integer in [min, max]. */
    fun int(key: String, min: Int, max: Int): Int =
        min + floor(get(key) * (max - min + 1)).toInt()

    /** Symmetric jitter in [-amount, amount). */
    fun jitter(key: String, amount: Double): Double = (get(key) * 2 - 1) * amount
}

// ────────────────────────────────────────────────────────────────────────────
// color.ts — palette construction
// ────────────────────────────────────────────────────────────────────────────

/** OKLCh, as `color.ts` speaks it. */
private class Oklch(val l: Double, val c: Double, val h: Double)

/** Every color slot a blobatar has. There is no fourth. */
internal class BlobatarColors(val bg: Color, val head: Color, val eye: Color)

/** OKLCh → linear-light sRGB. Components may fall outside [0,1] (out of gamut). */
private fun toLinear(color: Oklch): DoubleArray {
    val r = color.h * PI / 180.0
    val a = color.c * cos(r)
    val b = color.c * sin(r)

    val l_ = color.l + 0.3963377774 * a + 0.2158037573 * b
    val m_ = color.l - 0.1055613458 * a - 0.0638541728 * b
    val s_ = color.l - 0.0894841775 * a - 1.291485548 * b

    val l3 = l_ * l_ * l_
    val m3 = m_ * m_ * m_
    val s3 = s_ * s_ * s_

    return doubleArrayOf(
        4.0767416621 * l3 - 3.3077115913 * m3 + 0.2309699292 * s3,
        -1.2684380046 * l3 + 2.6097574011 * m3 - 0.3413193965 * s3,
        -0.0041960863 * l3 - 0.7034186147 * m3 + 1.707614701 * s3
    )
}

private fun inGamut(rgb: DoubleArray): Boolean =
    rgb.all { it >= -1e-4 && it <= 1 + 1e-4 }

/**
 * Resolves to in-gamut linear sRGB, reducing chroma if needed.
 *
 * Chroma is the right axis to give up: lowering it desaturates, while clipping
 * channels shifts hue — a clipped vivid blue turns purple.
 */
private fun resolve(color: Oklch): DoubleArray {
    var rgb = toLinear(color)
    if (!inGamut(rgb)) {
        var lo = 0.0
        var hi = color.c
        repeat(12) {
            val mid = (lo + hi) / 2
            if (inGamut(toLinear(Oklch(color.l, mid, color.h)))) lo = mid else hi = mid
        }
        rgb = toLinear(Oklch(color.l, lo, color.h))
    }
    return doubleArrayOf(
        min(1.0, max(0.0, rgb[0])),
        min(1.0, max(0.0, rgb[1])),
        min(1.0, max(0.0, rgb[2]))
    )
}

/**
 * WCAG relative luminance. What [resolve] returns is already linear-light sRGB,
 * which is exactly what WCAG's piecewise transfer function produces — so this
 * needs no further linearization, and the contrast ratio is honest.
 */
private fun luminance(color: Oklch): Double {
    val rgb = resolve(color)
    return 0.2126 * rgb[0] + 0.7152 * rgb[1] + 0.0722 * rgb[2]
}

private fun contrast(a: Oklch, b: Oklch): Double {
    val x = luminance(a)
    val y = luminance(b)
    return (max(x, y) + 0.05) / (min(x, y) + 0.05)
}

/**
 * Pushes [fg]'s lightness away from [bg] until the pair clears [target].
 *
 * Walks in the direction it is already leaning first, so a dark ink on a light
 * head gets darker rather than flipping to light. If that direction runs out of
 * range, it tries the other way before giving up at pure black or white.
 *
 * The ratio parameter is named `target` rather than `min` on purpose: it is the
 * one name in the file that would shadow [kotlin.math.min] inside a function
 * that calls it.
 */
private fun ensureContrast(fg: Oklch, bg: Oklch, target: Double): Oklch {
    if (contrast(fg, bg) >= target) return fg

    val lean = if (fg.l >= bg.l) 1.0 else -1.0
    for (dir in doubleArrayOf(lean, -lean)) {
        var l = fg.l
        var step = 0
        while (step < 60) {
            l = min(1.0, max(0.0, l + dir * 0.02))
            if (contrast(Oklch(l, fg.c, fg.h), bg) >= target) return Oklch(l, fg.c, fg.h)
            if (l == 0.0 || l == 1.0) break
            step++
        }
    }

    // Unreachable for the authored ramps, but a palette override could get here.
    val black = Oklch(0.0, 0.0, fg.h)
    val white = Oklch(1.0, 0.0, fg.h)
    return if (contrast(black, bg) >= contrast(white, bg)) black else white
}

private fun toHex(color: Oklch): String {
    val rgb = resolve(color)
    var out = "#"
    for (v in rgb) {
        val s = if (v <= 0.0031308) 12.92 * v else 1.055 * v.pow(1.0 / 2.4) - 0.055
        val byte = Math.round(s * 255).toInt().coerceIn(0, 255)
        out += byte.toString(16).padStart(2, '0')
    }
    return out
}

/** `#rrggbb` → an opaque [Color]. The palette is always opaque by construction. */
private fun hexColor(hex: String): Color {
    val v = hex.removePrefix("#").toLong(16)
    return Color((0xFF000000L or v).toInt())
}

/**
 * The tone set.
 *
 * This is the one place the seed is allowed to move lightness and chroma, not
 * just hue — a body vocabulary this varied looks monotonous in a single tone.
 * Letting the seed roam freely over L and C is what makes generated palettes
 * look generated, so instead it picks from six authored swatches: the same
 * discipline as a designer handing you a set, rather than a slider.
 *
 * Thresholds are cumulative, so pale and mid tones dominate and the near-black
 * body stays a rare find.
 */
private class Tone(val l: Double, val c: Double)

private val TONES: List<Pair<Double, Tone>> = listOf(
    0.2 to Tone(0.86, 0.085),   // pastel
    0.36 to Tone(0.9, 0.028),   // pale neutral
    0.62 to Tone(0.73, 0.135),  // mid
    0.8 to Tone(0.62, 0.165),   // deep
    0.93 to Tone(0.87, 0.16),   // bright
    // Dark, but not darker than a dark host surface. At l 0.17 this swatch scored
    // 1.03:1 against a near-black page and the body simply vanished, leaving two
    // floating eyes. l 0.34 still reads as the ink tone and clears both ends.
    1.0 to Tone(0.34, 0.035)    // ink
)

private fun toneAt(v: Double): Tone =
    TONES.firstOrNull { v < it.first }?.second ?: TONES[0].second

/**
 * The darkest host surface a backdrop-less blob is expected to land on, and the
 * ratio it must clear against it. Curio always draws a backdrop disc, but the
 * floor is kept: it is what stops an ink-tone head from rendering as a
 * silhouette lost against a dark page, and we draw the disc in [BlobatarColors.bg]
 * whose own floor is separate.
 */
private val DARK_SURFACE = Oklch(0.145, 0.0, 0.0) // ≈ #0a0a0b
private const val SURFACE_FLOOR = 1.5

/** The authored ramp: the seed picks a hue and a tone, and everything follows. */
private fun ramp(h: Double, tone: Double): MutableMap<String, Oklch> {
    val t = toneAt(tone)
    val head = ensureContrast(Oklch(t.l, t.c, h), DARK_SURFACE, SURFACE_FLOOR)
    return mutableMapOf(
        "bg" to Oklch(0.965, 0.01, h),
        "head" to head,
        // Polarity follows the body: dark eyes on a light body, light eyes on a
        // dark one. Without this the ink tone would render an invisible face.
        "eye" to if (head.l >= 0.5) Oklch(0.17, 0.02, h) else Oklch(0.97, 0.012, h)
    )
}

/**
 * Minimum contrast ratios as (foreground, background, ratio), applied in order.
 * Later pairs resolve against already-final earlier colors, so the chain
 * converges. `4.5` on the eyes is the WCAG text floor: they are small marks that
 * have to read at 24px.
 *
 * The body/backdrop floor is deliberately weak. The pale swatches are meant to
 * sit quietly on a light surface — forcing 1.6:1 there would darken exactly the
 * tones the style exists for.
 */
private val FLOORS: List<Triple<String, String, Double>> = listOf(
    Triple("head", "bg", 1.25),
    Triple("eye", "head", 4.5)
)

private fun blobatarColors(hue: Double, tone: Double): BlobatarColors {
    val r = ramp(hue, tone)
    for ((fg, bg, ratio) in FLOORS) {
        r[fg] = ensureContrast(r.getValue(fg), r.getValue(bg), ratio)
    }
    return BlobatarColors(
        bg = hexColor(toHex(r.getValue("bg"))),
        head = hexColor(toHex(r.getValue("head"))),
        eye = hexColor(toHex(r.getValue("eye")))
    )
}

// ────────────────────────────────────────────────────────────────────────────
// styles/shapes.ts — the silhouette vocabulary
// ────────────────────────────────────────────────────────────────────────────

/** Which primitive traces a silhouette's core. Omitted upstream, it is the superellipse. */
private enum class BlobStroke { SUPERELLIPSE, SPLINE, POLY, BOX }

private class BlobBody(
    var cx: Double,
    var cy: Double,
    var rx: Double,
    var ry: Double,
    var n: Double,
    var rot: Double,
    val radii: DoubleArray,
    /** Polygon-only, set by the shapes that draw one. */
    var sides: Int = 0,
    var round: Double = 0.0
)

private class BlobEllipse(val cx: Double, val cy: Double, val rx: Double, val ry: Double)

private class BlobPetal(val cx: Double, val cy: Double, val r: Double)

private class BlobTaper(val cx: Double, val cy: Double, val rx: Double, val ry: Double, val tip: Double)

private class BlobDeco {
    val petals = ArrayList<BlobPetal>(9)

    /** The droplet's taper, kept as geometry rather than as a traced path. */
    var taper: BlobTaper? = null
}

/**
 * A silhouette: how much of the frame its core body takes, how it patches that
 * body, what room it leaves the eyes, what it decorates with, and which path
 * primitive traces it. What it deliberately does NOT carry is its threshold —
 * how often it comes up is a property of the band table in [BANDS].
 *
 * Shapes that are parameterizations of another share its implementation rather
 * than restating it: [BOXY] is [ROUND] with a squarer `n` and a tilt, [HEXAGON]
 * is [TRIANGLE] with six sides, [CLOUD] is [ORGANIC] with lobes.
 */
private class BlobSilhouette(
    val name: String,
    /** How much of the frame the core body takes. */
    val core: Double,
    val stroke: BlobStroke = BlobStroke.SUPERELLIPSE,
    /** Patches the body before the face is measured. */
    val body: ((BlobatarTraits, BlobBody) -> Unit)? = null,
    /**
     * The region the eyes must fit inside. Absent, it is the body itself — which
     * is what every silhouette convex around its own centre wants, and half the
     * roster is.
     */
    val face: ((BlobBody) -> BlobEllipse)? = null,
    val decorate: ((BlobatarTraits, BlobBody, BlobDeco) -> Unit)? = null
)

private fun shrunk(k: Double): (BlobBody) -> BlobEllipse = { b ->
    BlobEllipse(b.cx, b.cy, b.rx * k, b.ry * k)
}

/** The spline body's own radii, pulled in a touch — the face a pebble leaves. */
private val SPLINE_FACE: (BlobBody) -> BlobEllipse = { b ->
    val smallest = b.radii.minOrNull() ?: 1.0
    shrunk(smallest * 0.95)(b)
}

private val POLY_FACE: (BlobBody) -> BlobEllipse = shrunk(0.84)

private val ROUND = BlobSilhouette(name = "round", core = 1.0)

private val ORGANIC = BlobSilhouette(
    name = "organic",
    core = 0.98,
    stroke = BlobStroke.SPLINE,
    face = SPLINE_FACE
)

/** [ROUND], squared off and tilted. Same path, different parameters. */
private val BOXY = BlobSilhouette(
    name = "boxy",
    core = 0.86,
    body = { t, b ->
        b.n = t.num("body.n", 3.4, 6.0)
        b.rot = t.num("body.rot", -20.0, 20.0)
    }
)

private val CAPSULE = BlobSilhouette(
    name = "capsule",
    core = 1.02,
    stroke = BlobStroke.BOX,
    body = { t, b -> b.ry *= t.num("capsule.squat", 0.55, 0.68) },
    face = shrunk(0.94),
    decorate = { _, b, out ->
        for (s in intArrayOf(-1, 1)) {
            out.petals.add(BlobPetal(b.cx + s * (b.rx - b.ry), b.cy, b.ry))
        }
    }
)

private val NUB = BlobSilhouette(
    name = "nub",
    core = 0.88,
    decorate = { t, b, out ->
        val count = t.int("nub.n", 1, 2)
        for (i in 0 until count) {
            val a = t.num("nub.a$i", 0.0, 2 * PI)
            out.petals.add(
                BlobPetal(
                    cx = b.cx + cos(a) * b.rx * 0.88,
                    cy = b.cy + sin(a) * b.rx * 0.88,
                    r = b.rx * t.num("nub.r$i", 0.24, 0.4)
                )
            )
        }
    }
)

/** [ORGANIC], with lobes on the upper half. */
private val CLOUD = BlobSilhouette(
    name = "cloud",
    core = 0.78,
    stroke = BlobStroke.SPLINE,
    face = SPLINE_FACE,
    decorate = { t, b, out ->
        val count = t.int("cloud.n", 4, 6)
        for (i in 0 until count) {
            val a = PI + (PI * (i + 0.5)) / count
            out.petals.add(
                BlobPetal(
                    cx = b.cx + cos(a) * b.rx * 0.8,
                    cy = b.cy + sin(a) * b.rx * 0.5,
                    r = b.rx * t.num("cloud.r$i", 0.44, 0.62)
                )
            )
        }
    }
)

private val DROPLET = BlobSilhouette(
    name = "droplet",
    core = 0.78,
    // Shifted down by what the taper adds above, so the whole silhouette — head
    // and point together — sits centred in the frame rather than the head alone.
    // `n` is pinned to a true ellipse, which is the curve the taper is tangent to.
    body = { _, b ->
        b.cy += 0.22 * b.ry
        b.n = 2.0
    },
    face = { b -> BlobEllipse(b.cx, b.cy + b.ry * 0.05, b.rx * 0.88, b.ry * 0.88) },
    decorate = { t, b, out ->
        out.taper = BlobTaper(b.cx, b.cy, b.rx, b.ry, t.num("droplet.tip", 1.4, 1.65))
    }
)

private val HEXAGON = BlobSilhouette(
    name = "hexagon",
    core = 1.05,
    stroke = BlobStroke.POLY,
    body = { t, b ->
        b.sides = 6
        b.rot = t.num("body.rot", -12.0, 12.0)
        b.round = t.num("poly.round", 0.24, 0.5)
    },
    face = POLY_FACE
)

private val SUN = BlobSilhouette(
    name = "sun",
    core = 0.7,
    decorate = { t, b, out ->
        val count = t.int("sun.n", 6, 9)
        val dist = b.rx * t.num("sun.dist", 1.0, 1.08)
        val pr = b.rx * t.num("sun.r", 0.2, 0.26)
        val off = t.num("sun.rot", 0.0, 2 * PI)
        for (i in 0 until count) {
            val a = off + (2 * PI * i) / count
            out.petals.add(BlobPetal(b.cx + cos(a) * dist, b.cy + sin(a) * dist, pr))
        }
    }
)

/** [HEXAGON] with three sides, and a tighter tilt so it rests on its base. */
private val TRIANGLE = BlobSilhouette(
    name = "triangle",
    core = 1.15,
    stroke = BlobStroke.POLY,
    body = { t, b ->
        b.sides = 3
        b.rot = t.num("body.rot", -5.0, 5.0)
        b.round = t.num("poly.round", 0.24, 0.5)
    },
    face = { b -> BlobEllipse(b.cx, b.cy + b.ry * 0.1, b.rx * 0.54, b.ry * 0.36) }
)

/**
 * The ten-shape vocabulary, **weighted rather than uniform**: round and organic
 * are the everyday shapes, while the louder silhouettes stay finds.
 *
 * These bands, the layout ranges in [blobatarLayout] and the tone set together
 * form the frozen seed→look mapping. Changing an edge does not "tune" the set —
 * it re-rolls every face in the app, so treat an edit here as a new cast.
 */
private val BANDS: List<Pair<BlobSilhouette, Double>> = listOf(
    ROUND to 0.22, ORGANIC to 0.48, BOXY to 0.6, CAPSULE to 0.7, NUB to 0.79,
    CLOUD to 0.86, DROPLET to 0.915, HEXAGON to 0.95, SUN to 0.98, TRIANGLE to 1.0
)

// ────────────────────────────────────────────────────────────────────────────
// styles/compose.ts — the shared body, eyes and fit
// ────────────────────────────────────────────────────────────────────────────

private class BlobEye(
    val cx: Double,
    val cy: Double,
    val rx: Double,
    val ry: Double,
    val n: Double,
    val rot: Double
)

private class BlobLayout(
    val silhouette: BlobSilhouette,
    val body: BlobBody,
    val petals: List<BlobPetal>,
    val taper: BlobTaper?,
    val eyes: List<BlobEye>
)

/**
 * Fits the eye cluster against the silhouette's face region on both axes.
 *
 * The containment guarantee lives here: `need` is the eye cluster's reach as a
 * fraction of the face's own axes, and anything over 0.9 is scaled back down by
 * `fit`. An extreme trait combination therefore lands SHORT of where it asked
 * rather than outside the body — which is why nothing here may be "simplified"
 * into a plain clamp on each radius.
 */
private fun faceFit(t: BlobatarTraits, b: BlobBody, face: BlobEllipse): List<BlobEye> {
    val rx = b.rx
    val er0 = t.num("eye.rx", 0.075, 0.105) * rx
    val ratio = t.num("eye.ratio", 1.9, 3.2)
    val scale = t.num("eye.scale", 0.78, 1.24)
    val stretch = t.num("eye.stretch", 0.85, 1.18)
    val clearance = t.num("eye.gap", 0.1, 0.24) * rx
    val wide = er0 * max(1.0, scale)
    val tall = er0 * ratio * max(1.0, scale * stretch)
    val gap0 = wide + rx * 0.03 + clearance

    val gx = t.jitter("gaze.x", 0.09) * face.rx
    val gy = t.num("gaze.y", -0.2, 0.08) * face.ry
    val dy = t.jitter("eye.dy", 0.04) * face.ry
    val reach = hypot(wide, tall)
    val need = hypot(
        (abs(gx) + gap0 + reach) / face.rx,
        (abs(gy) + abs(dy) + reach) / face.ry
    )
    val fit = if (need > 0.9) 0.9 / need else 1.0

    val er = er0 * fit
    val eyeRy = er * ratio
    val gap = gap0 * fit
    val room = max(0.0, min(1.0, clearance / tall))
    val bound = min(12.0, (asin(room) * 180) / PI)
    val lean = t.num("eye.lean", -1.0, 1.0) * bound
    val lean2 = max(-12.0, min(12.0, lean + t.jitter("eye.lean2", 3.5)))

    val cx = face.cx + gx * fit
    val cy = face.cy + gy * fit
    val eyeN = t.num("eye.n", 3.5, 6.0)
    return listOf(
        BlobEye(cx = cx - gap, cy = cy, rx = er, ry = eyeRy, n = eyeN, rot = lean),
        BlobEye(
            cx = cx + gap,
            cy = cy + dy * fit,
            rx = er * scale,
            ry = eyeRy * scale * stretch,
            n = eyeN,
            rot = lean2
        )
    )
}

private fun blobatarLayout(t: BlobatarTraits): BlobLayout {
    val shape = BANDS.firstOrNull { t["shape"] < it.second }?.first ?: BANDS.last().first
    val r = t.num("body.r", 31.0, 38.0) * shape.core
    val body = BlobBody(
        cx = 50 + t.jitter("body.x", 1.5),
        cy = 50 + t.jitter("body.y", 1.5),
        rx = r,
        ry = r * t.num("body.ratio", 0.92, 1.08),
        n = t.num("body.n", 1.9, 2.5),
        rot = 0.0,
        radii = DoubleArray(t.int("body.pts", 6, 8)) { i -> 1 + t.jitter("body.r$i", 0.16) }
    )
    shape.body?.invoke(t, body)

    // The body itself when the shape names no face, which is what a silhouette
    // convex around its own centre wants — and it already carries the four
    // fields a face is.
    val face = shape.face?.invoke(body)
        ?: BlobEllipse(body.cx, body.cy, body.rx, body.ry)
    val deco = BlobDeco()
    shape.decorate?.invoke(t, body, deco)

    return BlobLayout(
        silhouette = shape,
        body = body,
        petals = deco.petals,
        taper = deco.taper,
        eyes = faceFit(t, body, face)
    )
}

// ────────────────────────────────────────────────────────────────────────────
// shape.ts — the path primitives
// ────────────────────────────────────────────────────────────────────────────

/**
 * The single primitive.
 *
 * |x/a|^n + |y/b|^n = 1 covers the whole part vocabulary: n=2 is an ellipse
 * (eyes, pupils), n≈4 a squircle (head, background), n→large a rectangle. One
 * shape function, one continuous knob, so "head shape" is a numeric trait rather
 * than a set of hand-drawn alternatives.
 *
 * Each quadrant is one cubic Bézier whose control offset is chosen so the curve
 * passes exactly through the superellipse's 45° point: at n=2 this yields the
 * standard circle constant, which is a good sign the derivation is right. Four
 * segments instead of a sampled polyline keeps each shape at ~130 bytes upstream.
 */
private fun superellipseInto(
    out: Path,
    cx: Double,
    cy: Double,
    rx: Double,
    ry: Double,
    n: Double,
    rot: Double
) {
    // Above n≈5.55 the control offset exceeds the radius, and the curve bulges
    // outside the bounding box instead of squaring off — an inflated-looking
    // corner rather than a sharper one. Clamping k trades exactness at the 45°
    // point for a shape that always stays within its stated bounds.
    val k = min(1.0, (8.0 * 2.0.pow(-1.0 / n) - 4.0) / 3.0)
    val a = rx
    val b = ry
    val ak = a * k
    val bk = b * k

    val pts = doubleArrayOf(
        a, 0.0,
        a, bk, ak, b, 0.0, b,
        -ak, b, -a, bk, -a, 0.0,
        -a, -bk, -ak, -b, 0.0, -b,
        ak, -b, a, -bk, a, 0.0
    )

    val t = rot * PI / 180.0
    val cosT = cos(t)
    val sinT = sin(t)
    fun px(i: Int) = cx + pts[i * 2] * cosT - pts[i * 2 + 1] * sinT
    fun py(i: Int) = cy + pts[i * 2] * sinT + pts[i * 2 + 1] * cosT

    out.moveTo(px(0).toFloat(), py(0).toFloat())
    var i = 1
    while (i < 13) {
        out.cubicTo(
            px(i).toFloat(), py(i).toFloat(),
            px(i + 1).toFloat(), py(i + 1).toFloat(),
            px(i + 2).toFloat(), py(i + 2).toFloat()
        )
        i += 3
    }
    out.close()
}

/**
 * An organic closed curve: radii sampled around a circle, joined by a closed
 * Catmull-Rom spline converted to cubic Béziers.
 *
 * The superellipse handles everything symmetric; this handles everything that
 * needs to look hand-drawn. [radii] are multipliers of the base radius, one per
 * vertex, so a seed perturbing them by ±15% produces the lopsided pebble shapes
 * without any noise function.
 *
 * Catmull-Rom rather than a Bézier fit because it interpolates its points
 * exactly, so the radii mean what they say and containment stays predictable.
 */
private fun blobPathInto(
    out: Path,
    cx: Double,
    cy: Double,
    rx: Double,
    ry: Double,
    radii: DoubleArray,
    rot: Double
) {
    val n = radii.size
    val t0 = rot * PI / 180.0
    val xs = DoubleArray(n)
    val ys = DoubleArray(n)
    for (i in 0 until n) {
        val a = t0 + (2 * PI * i) / n
        xs[i] = cx + rx * radii[i] * cos(a)
        ys[i] = cy + ry * radii[i] * sin(a)
    }

    fun idx(i: Int) = ((i % n) + n) % n

    out.moveTo(xs[idx(0)].toFloat(), ys[idx(0)].toFloat())
    for (i in 0 until n) {
        val i0 = idx(i - 1)
        val i1 = idx(i)
        val i2 = idx(i + 1)
        val i3 = idx(i + 2)
        out.cubicTo(
            (xs[i1] + (xs[i2] - xs[i0]) / 6).toFloat(),
            (ys[i1] + (ys[i2] - ys[i0]) / 6).toFloat(),
            (xs[i2] - (xs[i3] - xs[i1]) / 6).toFloat(),
            (ys[i2] - (ys[i3] - ys[i1]) / 6).toFloat(),
            xs[i2].toFloat(),
            ys[i2].toFloat()
        )
    }
    out.close()
}

/**
 * A regular polygon with rounded corners.
 *
 * The third primitive, and it is here because neither of the other two reaches a
 * flat-sided shape: `superellipse` has no odd-sided member at all — its n knob
 * cannot produce a triangle — and `blobPath` rounds a corner away rather than
 * turning it.
 *
 * Corners are cut back along both adjoining edges by `round` and joined with a
 * quadratic through the vertex itself, which puts the whole outline inside the
 * polygon's convex hull for free: a quadratic never leaves the triangle of its
 * three points.
 */
private fun polygonInto(
    out: Path,
    cx: Double,
    cy: Double,
    rx: Double,
    ry: Double,
    sides: Int,
    round: Double,
    rot: Double
) {
    // Halved because the cut is taken from both ends of every edge: at round = 1
    // each end reaches the midpoint and they meet exactly.
    val k = if (round > 0) (if (round < 1) round / 2 else 0.5) else 0.0
    // −90° so a vertex sits at the top: a triangle points up and rests on a flat
    // edge, which is the orientation anybody who asks for a triangle means.
    val t0 = rot * PI / 180.0 - PI / 2
    val xs = DoubleArray(sides)
    val ys = DoubleArray(sides)
    for (i in 0 until sides) {
        val a = t0 + (2 * PI * i) / sides
        xs[i] = cx + rx * cos(a)
        ys[i] = cy + ry * sin(a)
    }

    fun idx(i: Int) = ((i % sides) + sides) % sides
    fun cutX(i: Int, j: Int) = xs[idx(i)] + (xs[idx(j)] - xs[idx(i)]) * k
    fun cutY(i: Int, j: Int) = ys[idx(i)] + (ys[idx(j)] - ys[idx(i)]) * k

    out.moveTo(cutX(0, -1).toFloat(), cutY(0, -1).toFloat())
    for (i in 0 until sides) {
        out.quadraticBezierTo(
            xs[idx(i)].toFloat(),
            ys[idx(i)].toFloat(),
            cutX(i, i + 1).toFloat(),
            cutY(i, i + 1).toFloat()
        )
        // The straight run to the next corner's cut. Omitted when the cuts meet,
        // so a fully rounded polygon does not emit zero-length lines.
        if (k < 0.5) out.lineTo(cutX(i + 1, i).toFloat(), cutY(i + 1, i).toFloat())
    }
    out.close()
}

/**
 * The straight run of a capsule, as a plain box.
 *
 * Drawn with the two cap circles the capsule already decorates with, the union
 * is an exact stadium: the box reaches full height everywhere, so each cap meets
 * it along its own diameter and there is no crease.
 */
private fun boxInto(out: Path, cx: Double, cy: Double, rx: Double, ry: Double) {
    val l = cx - rx
    val r = cx + rx
    out.moveTo(l.toFloat(), (cy - ry).toFloat())
    out.lineTo(r.toFloat(), (cy - ry).toFloat())
    out.lineTo(r.toFloat(), (cy + ry).toFloat())
    out.lineTo(l.toFloat(), (cy + ry).toFloat())
    out.close()
}

/**
 * The taper of a droplet: the two tangents from an apex to the body ellipse.
 *
 * Drawn with that ellipse, the union is a teardrop. A tangent meets the curve
 * without a corner, so the taper grows out of the head at every `tip` rather
 * than being stuck on. The point is eased with a quadratic through the apex, so
 * the drawn tip stops just short of it — no needle at small sizes.
 */
private fun taperInto(out: Path, taper: BlobTaper) {
    val t = max(1.05, taper.tip)
    // In the circle the ellipse is an affine image of, the tangent points sit at
    // angle acos(1/tip) from the apex direction. Affine maps preserve tangency,
    // so scaling those two points by rx and ry is exact, not an approximation.
    val tx = taper.rx * sqrt(1 - 1 / (t * t))
    val ty = taper.cy - taper.ry / t
    val apex = taper.cy - t * taper.ry
    val px = tx * 0.14
    val py = ty + 0.86 * (apex - ty)
    out.moveTo((taper.cx - tx).toFloat(), ty.toFloat())
    out.lineTo((taper.cx - px).toFloat(), py.toFloat())
    out.quadraticBezierTo(
        taper.cx.toFloat(),
        apex.toFloat(),
        (taper.cx + px).toFloat(),
        py.toFloat()
    )
    out.lineTo((taper.cx + tx).toFloat(), ty.toFloat())
    out.close()
}

/** The core path of [body], traced by the silhouette's own primitive. */
private fun traceBody(out: Path, stroke: BlobStroke, b: BlobBody) {
    when (stroke) {
        BlobStroke.SUPERELLIPSE ->
            superellipseInto(out, b.cx, b.cy, b.rx, b.ry, b.n, b.rot)
        BlobStroke.SPLINE ->
            blobPathInto(out, b.cx, b.cy, b.rx, b.ry, b.radii, b.rot)
        BlobStroke.POLY ->
            polygonInto(out, b.cx, b.cy, b.rx, b.ry, b.sides, b.round, b.rot)
        BlobStroke.BOX ->
            boxInto(out, b.cx, b.cy, b.rx - b.ry, b.ry)
    }
}

// ────────────────────────────────────────────────────────────────────────────
// The art: one seed, resolved once, ready to paint
// ────────────────────────────────────────────────────────────────────────────

/**
 * One member's face, resolved and traced.
 *
 * Everything expensive happens in the constructor — the hash, the palette, the
 * forty trait reads, the Bézier control points — and what is left is a handful of
 * [Path]s and three [Color]s. So a `SocialAvatar` builds one per seed inside a
 * `remember` and a redraw is pure `drawPath`.
 *
 * Crucially this is ALSO what the notification's off-screen bitmap paints, which
 * is the whole reason the face is derived locally rather than fetched: the same
 * object serves a scrolling list and a receiver with no composition.
 *
 * Paths are built in the upstream **viewBox of 0..100 × 0..100**; the caller
 * scales. That box IS the disc — the background circle is radius 50 in these
 * coordinates — and the figure fits it, which is what lets the avatar be drawn
 * with NO clipping (the old hand-drawn cast had to be clipped to its disc; a
 * derived face does not). That was MEASURED rather than assumed: over 20,000
 * seeds the furthest any part of any face reaches from the centre is 48.30 of
 * the disc's 50, with the worst case an `organic` body. Re-check it if a band
 * edge, a `sun.dist`/`capsule.squat`-style range or a `body.r` range moves.
 */
internal class BlobatarArt(seed: String) {

    val colors: BlobatarColors

    private val petals: List<BlobPetal>
    private val taperPath: Path?
    private val bodyPath: Path
    private val eyePaths: List<Path>

    init {
        val traits = BlobatarTraits(seed)
        colors = blobatarColors(
            hue = traits["hue"] * 360,
            tone = traits["tone"]
        )

        val layout = blobatarLayout(traits)
        petals = layout.petals
        val taper = layout.taper
        taperPath = taper?.let { t -> Path().also { path -> taperInto(path, t) } }
        bodyPath = Path().also { path ->
            traceBody(path, layout.silhouette.stroke, layout.body)
        }
        eyePaths = layout.eyes.map { eye ->
            Path().also { path ->
                superellipseInto(path, eye.cx, eye.cy, eye.rx, eye.ry, eye.n, eye.rot)
            }
        }
    }

    /**
     * Paints the face into the current [DrawScope], which must be SQUARE.
     *
     * The disc is the palette's own `bg` — the hue-tinted near-white the ramp
     * authors for exactly this — lit from the top-left and deepened at the lower
     * edge, so the face sits in its own light rather than reading as a sticker cut
     * out of the page. It is drawn in device pixels because a gradient is not art
     * and must not scale with the viewBox; the figure is drawn under a single
     * viewBox→canvas scale so every coordinate above stays comparable with the
     * upstream source.
     */
    fun draw(scope: DrawScope, ring: Boolean = true) {
        val radius = scope.size.minDimension / 2f
        val u = scope.size.minDimension / 100f

        scope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    lerp(colors.bg, Color.White, 0.22f),
                    colors.bg,
                    lerp(colors.bg, Color.Black, 0.16f)
                ),
                center = Offset(scope.size.width * 0.34f, scope.size.height * 0.30f),
                radius = radius * 1.5f
            ),
            radius = radius
        )

        scope.withTransform({ scale(u, u, pivot = Offset.Zero) }) {
            for (petal in petals) {
                drawCircle(
                    color = colors.head,
                    radius = petal.r.toFloat(),
                    center = Offset(petal.cx.toFloat(), petal.cy.toFloat())
                )
            }
            taperPath?.let { drawPath(it, colors.head) }
            drawPath(bodyPath, colors.head)
            for (eye in eyePaths) drawPath(eye, colors.eye)
        }

        if (ring) {
            scope.drawCircle(
                color = Color.White.copy(alpha = 0.22f),
                radius = radius - 1.5f * u,
                style = Stroke(width = 2f * u)
            )
        }
    }
}
