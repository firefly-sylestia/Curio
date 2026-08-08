package com.curio.app.data

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * The Curie pet's custom look — a pixel design (16, 24 or 32 grid) with its
 * own palette, face expressions and reaction rules, serialized as a
 * plain-text file the Pet designer playground can import and export.
 *
 * v8.35 — grid sizes: the designer ships a 24×24 and a 32×32 canvas and can
 * convert a design between them ([withSize], dominant-key resample). The
 * default look is 24×24 (upscaled from the original 16×16 art, so it keeps
 * its proportions). Designs saved at any size keep rendering — the sprite
 * adapts to the grid.
 *
 * ## Import format (plain text, hex colors)
 *
 * A design is a tiny text file: optional `# size=N` header, a palette line
 * per grid key (`key=RRGGBB`), then the BODY grid, then the CURLED (asleep)
 * grid, then optional `face=` and `react=` config lines. Lines starting
 * with `#` are comments. Example:
 *
 * ```
 * # Curie pet design
 * # size=24
 * b=FFF3DC
 * o=4A3426
 * s=FF6F61
 * face=EXCITED;eyes=STAR;mouth=WIDE;blush=1;sparkles=1
 * react=TOUCH;enabled=1;anim=HOP;eyes=STAR;mouth=WIDE;blush=1;sparkles=1
 * ```
 *
 * Grid keys: `b` body, `B` body shade, `o` ink outline, `s` scarf accent,
 * `S` scarf shade, `G` gold, `g` gold deep, `c`/`C`/`d`/`D` custom paint
 * slots, `r` blush, `y` eye/star color. Any other char renders empty.
 * Missing palette keys fall back to the default look, so a design only has
 * to list the colors it actually changes.
 */
data class PetDesign(
    val palette: Map<Char, String>,
    val bodyRows: List<String>,
    val curledRows: List<String>,
    /** The canvas size: 16, 24 or 32. */
    val gridSize: Int = DEFAULT_GRID_SIZE,
    /**
     * Per-mood face customization (key = mood name, e.g. "HAPPY"). An
     * absent mood wears the pet's built-in face for that mood.
     */
    val faces: Map<String, PetFace> = emptyMap(),
    /**
     * Per-event reaction rules (key = event name, e.g. "TOUCH"). An absent
     * event uses the built-in reaction for that event.
     */
    val reactions: Map<String, PetReaction> = emptyMap(),
    /** Optional transparent drawn layers for tail, accessories, effects, and antenna art. */
    val details: Map<String, List<String>> = emptyMap(),
    /** Explicit per-element visibility overrides; absent keys stay procedurally enabled. */
    val procedural: Map<String, Boolean> = emptyMap(),
    /**
     * v8.48 — user-custom animation frames (key = animation id, e.g. "happy").
     * An absent key plays the built-in animation; old designs without this
     * field keep working unchanged.
     */
    val animations: Map<String, PetAnimation> = emptyMap(),
    /**
     * v8.51 — the species this design belongs to (multi-pet foundations,
     * Phase 6). Old designs without this field resolve to Curie via
     * [PetRegistry.resolve] — always readable, never crashes.
     */
    val petSpeciesId: String = PET_CURIE_ID
) {
    /** The palette keys a design may recolor. */
    companion object {
        val KEYS = listOf('b', 'B', 'o', 's', 'S', 'G', 'g', 'c', 'C', 'd', 'D', 'r', 'y')

        const val DEFAULT_GRID_SIZE = 24
        const val MIN_GRID_SIZE = 16
        val SUPPORTED_SIZES = listOf(24, 32)

        /** Drawable detail layers exposed by the compact Details editor. */
        val DETAIL_KEYS = listOf("tail", "accessories", "effects", "antenna")

        /** Procedural art elements that can be independently hidden. */
        val PROCEDURAL_KEYS = listOf("tail", "belly", "accessories", "effects", "antenna")

        val DEFAULT_PALETTE: Map<Char, String> = mapOf(
            'b' to "FFF3DC", // body — warm cream
            'B' to "F0DDBB", // body shade — deeper cream
            'o' to "4A3426", // ink outline / face — warm brown
            's' to "FF6F61", // scarf accent — coral
            'S' to "D95A50", // scarf shade — deeper coral
            'G' to "FFD97D", // gold — antenna star / sparkles
            'g' to "E0B050", // gold deep — halo detail
            'c' to "FF9ECB", // custom 1 — pastel pink
            'C' to "9ECBFF", // custom 2 — pastel blue
            'd' to "CBF0A8", // custom 3 — pastel green
            'D' to "F0E6A8", // custom 4 — pastel yellow
            'r' to "F7AFAF", // blush — cheek color
            'y' to "7A4E2E"  // eye / star color — warm brown
        )

        /** The original 16×16 default body — the source art for larger sizes. */
        val DEFAULT_BODY_16: List<String> = listOf(
            ".......GG.......",
            "...o...GG...o...",
            "..ob........bo..",
            "..ob....o....bo.",
            "...oooooooooo...",
            ".obbbbbbbbbbbbo.",
            "obbbbbbbbbbbbbbo",
            "obbbbbbbbbbbbbbo",
            "obbbbbbbbbbbBBbo",
            "obbbbbbbbbbbBBbo",
            "obbbbbbbbbbbBBbo",
            "obbbbbbbbbbbbBBo",
            ".osssssssssssso.",
            "..oSSssssssSSo..",
            "..oo........oo..",
            "..oo........oo.."
        )

        /** The original 16×16 default curled (asleep) grid. */
        val DEFAULT_CURLED_16: List<String> = listOf(
            ".......GG.......",
            "...o...GG...o...",
            "..ob........bo..",
            "..obbbbbbbbbbo..",
            "...obbbbbbbbo...",
            "..obbbbbbbbbbbo.",
            ".obbbbbbbbbbbbo.",
            "obbbbbbbbbbbbbbo",
            "obssssssssssssbo",
            ".obbbbbbbbbbbbo.",
            "..obbbbbbbbbbbo.",
            "...obbbbbbbbo...",
            ".....oBBbbbbBBo.",
            "......oooooo....",
            "................",
            "................"
        )

        /** The default 24×24 body — a clean 1.5× upscale of the 16 art. */
        val DEFAULT_BODY: List<String> = resizeGrid(DEFAULT_BODY_16, 16, 24)

        /** The default 24×24 curled pose. */
        val DEFAULT_CURLED: List<String> = resizeGrid(DEFAULT_CURLED_16, 16, 24)

        // ── Default faces per mood (v8.35) — mirrors the sprite's built-in
        //    faces so an untouched design looks exactly as before.
        val DEFAULT_FACES: Map<String, PetFace> = mapOf(
            "HAPPY" to PetFace(eyes = EyeStyle.OPEN, mouth = MouthStyle.SMILE, blush = false, sparkles = false),
            "EXCITED" to PetFace(eyes = EyeStyle.STAR, mouth = MouthStyle.WIDE, blush = true, sparkles = true),
            "SLEEPY" to PetFace(eyes = EyeStyle.CLOSED, mouth = MouthStyle.NONE, blush = false, sparkles = false),
            "CURIOUS" to PetFace(eyes = EyeStyle.OPEN, mouth = MouthStyle.SMILE, blush = false, sparkles = false),
            "PROUD" to PetFace(eyes = EyeStyle.HAPPY, mouth = MouthStyle.WIDE, blush = true, sparkles = false),
            "BOUNCY" to PetFace(eyes = EyeStyle.HAPPY, mouth = MouthStyle.WIDE, blush = true, sparkles = false),
            "FOCUSED" to PetFace(eyes = EyeStyle.OPEN, mouth = MouthStyle.SMILE, blush = false, sparkles = false)
        )

        // ── Default reaction rules per event.
        val DEFAULT_REACTIONS: Map<String, PetReaction> = mapOf(
            "TOUCH" to PetReaction(anim = ReactionAnim.HOP, face = PetFace(eyes = EyeStyle.OPEN, mouth = MouthStyle.SMILE)),
            "SPIN_LANDED" to PetReaction(anim = ReactionAnim.HOP, face = PetFace(eyes = EyeStyle.STAR, mouth = MouthStyle.WIDE, blush = true, sparkles = true)),
            "REVEAL" to PetReaction(anim = ReactionAnim.HOP, face = PetFace(eyes = EyeStyle.STAR, mouth = MouthStyle.WIDE, blush = true, sparkles = true)),
            "EXPLORE" to PetReaction(anim = ReactionAnim.HOP, face = PetFace(eyes = EyeStyle.OPEN, mouth = MouthStyle.SMILE)),
            "SAVE" to PetReaction(anim = ReactionAnim.HOP, face = PetFace(eyes = EyeStyle.HAPPY, mouth = MouthStyle.SMILE, blush = true)),
            "PLAY" to PetReaction(anim = ReactionAnim.BOUNCE, face = PetFace(eyes = EyeStyle.STAR, mouth = MouthStyle.WIDE, blush = true, sparkles = true)),
            "LEVEL_UP" to PetReaction(anim = ReactionAnim.SPIN, face = PetFace(eyes = EyeStyle.STAR, mouth = MouthStyle.WIDE, blush = true, sparkles = true))
        )

        val DEFAULT = PetDesign(DEFAULT_PALETTE, DEFAULT_BODY, DEFAULT_CURLED, DEFAULT_GRID_SIZE, DEFAULT_FACES, DEFAULT_REACTIONS)

        /**
         * The same design [curled] shows while asleep. When the user paints
         * the BODY grid they usually want the sleep pose to follow, so the
         * playground offers "copy body to curled" — this is that copy.
         */
        fun bodyAsCurled(body: List<String>): List<String> = body

        /**
         * Resamples a grid from one size to another by dominant-key box
         * averaging: every target cell is filled with the source key that
         * covers the largest fractional area of its footprint. Preserves
         * palette keys exactly (no color interpolation), so converting a
         * design keeps every pixel on the palette.
         */
        fun resizeGrid(rows: List<String>, from: Int, to: Int): List<String> {
            if (from == to) return rows
            val result = Array(to) { CharArray(to) { '.' } }
            for (ty in 0 until to) {
                for (tx in 0 until to) {
                    val x0 = tx * from.toDouble() / to
                    val x1 = (tx + 1) * from.toDouble() / to
                    val y0 = ty * from.toDouble() / to
                    val y1 = (ty + 1) * from.toDouble() / to
                    val weights = HashMap<Char, Double>()
                    for (sy in floor(y0).toInt() until ceil(y1).toInt()) {
                        for (sx in floor(x0).toInt() until ceil(x1).toInt()) {
                            val ox = (min(x1, sx + 1.0) - max(x0, sx.toDouble())).coerceAtLeast(0.0)
                            val oy = (min(y1, sy + 1.0) - max(y0, sy.toDouble())).coerceAtLeast(0.0)
                            if (ox <= 0.0 || oy <= 0.0) continue
                            val ch = rows.getOrNull(sy)?.getOrNull(sx) ?: '.'
                            weights[ch] = (weights[ch] ?: 0.0) + ox * oy
                        }
                    }
                    result[ty][tx] = weights.maxByOrNull { it.value }?.key ?: '.'
                }
            }
            return result.map { String(it) }
        }
    }

    /** Resolves a grid key to its hex color, falling back to the default. */
    fun colorOf(key: Char): String = palette[key] ?: DEFAULT_PALETTE[key] ?: "000000"

    /** The hex color to DRAW for a grid cell char, or null when empty/unknown. */
    fun colorFor(ch: Char): String? =
        if (ch == '.') null else palette[ch] ?: DEFAULT_PALETTE[ch]

    /** True when this design differs from the default in any way. */
    val isCustom: Boolean get() = this != DEFAULT

    /** The face to wear for [mood] — user override or the built-in face. */
    /** The face for [mood], including its optional hand-drawn overlay grid. */
    fun faceFor(moodName: String): PetFace = normalizeFace(
        faces[moodName] ?: DEFAULT_FACES[moodName] ?: PetFace()
    )

    /** The reaction rule for [event] — user override or the built-in rule. */
    fun reactionFor(eventName: String): PetReaction {
        val reaction = reactions[eventName] ?: DEFAULT_REACTIONS[eventName] ?: PetReaction()
        return reaction.copy(face = normalizeFace(reaction.face))
    }

    private fun normalizeFace(face: PetFace): PetFace = if (face.gridRows.isEmpty()) face else face.copy(
        gridRows = face.gridRows.map { (it + ".".repeat(gridSize)).take(gridSize) }
            .take(gridSize)
            .let { rows ->
                if (rows.size == gridSize) rows else rows + List(gridSize - rows.size) { ".".repeat(gridSize) }
            }
    )

    /** The full design as importable text (the format shown in the header). */
    fun toText(): String = buildString {
        appendLine("# Curie pet design")
        appendLine("# pet=$petSpeciesId")
        appendLine("# size=$gridSize")
        appendLine("# Palette: one hex color per grid key")
        KEYS.forEach { key ->
            appendLine("$key=${colorOf(key)}")
        }
        appendLine("# Body grid — $gridSize rows of exactly $gridSize chars (. empty, keys from palette)")
        bodyRows.forEach { appendLine(it) }
        appendLine("# Curled (asleep) grid — $gridSize rows of exactly $gridSize chars")
        curledRows.forEach { appendLine(it) }
        details.forEach { (layer, rows) ->
            val encoded = runCatching {
                java.net.URLEncoder.encode(rows.joinToString("\n"), "UTF-8")
            }.getOrDefault("")
            appendLine("detail=$layer;grid=$encoded")
        }
        procedural.forEach { (element, enabled) ->
            appendLine("procedural=$element;enabled=${if (enabled) 1 else 0}")
        }
        DEFAULT_FACES.keys.forEach { mood ->
            appendLine("face=$mood;${faces[mood]?.toConfig() ?: DEFAULT_FACES[mood]?.toConfig() ?: PetFace().toConfig()}")
        }
        DEFAULT_REACTIONS.keys.forEach { event ->
            appendLine("react=$event;${reactions[event]?.toConfig() ?: DEFAULT_REACTIONS[event]?.toConfig() ?: PetReaction().toConfig()}")
        }
    }

    /**
     * Parses [text] back into a design. Tolerant: blank lines and `#`
     * comments are skipped, `key=HEX` lines fill the palette, `face=` /
     * `react=` lines carry expression/reaction config, `size=N` sets the
     * canvas, and the next non-comment lines become the body grid with the
     * following `size` the curled grid. Short rows are padded, long rows
     * truncated. Never throws — on a completely unparseable input it
     * returns [fallback].
     */
    fun toParsedOr(text: String, fallback: PetDesign): PetDesign {
        val palette = mutableMapOf<Char, String>()
        val rows = mutableListOf<String>()
        val faces = mutableMapOf<String, PetFace>()
        val reactions = mutableMapOf<String, PetReaction>()
        val details = mutableMapOf<String, List<String>>()
        val procedural = mutableMapOf<String, Boolean>()
        var declaredSize: Int? = null
        // v8.51 — the design's species (multi-pet). Absent in old designs
        // and in hand-written text → falls back below.
        var petId: String? = null
        text.lineSequence().forEach { raw ->
            val line = raw.trim()
            if (line.isEmpty()) return@forEach
            if (line.startsWith("#")) {
                // `# pet=...` is real metadata (written with the `#` so
                // older parsers skip it as a comment); every other comment
                // line is decorative.
                if (line.startsWith("# pet=")) {
                    petId = line.substring("# pet=".length).trim().lowercase()
                        .takeIf { it.isNotBlank() }
                }
                return@forEach
            }
            val eq = line.indexOf('=')
            when {
                line.startsWith("size=") && eq > 0 -> {
                    declaredSize = line.substring(5).trim().toIntOrNull()
                }
                line.startsWith("detail=") && eq == 6 -> {
                    val separator = line.indexOf(';', startIndex = 7)
                    val layerEnd = if (separator >= 0) separator else line.length
                    val layer = line.substring(7, layerEnd).trim().lowercase()
                    val config = if (separator >= 0) line.substring(separator + 1) else ""
                    val value = config.split(';')
                        .firstOrNull { it.startsWith("grid=") }
                        ?.substringAfter('=')
                        .orEmpty()
                    if (layer in DETAIL_KEYS && value.isNotBlank()) {
                        runCatching {
                            details[layer] = java.net.URLDecoder.decode(value, "UTF-8").split("\n")
                        }
                    }
                }
                line.startsWith("procedural=") && eq == 10 -> {
                    val separator = line.indexOf(';', startIndex = 11)
                    val elementEnd = if (separator >= 0) separator else line.length
                    val element = line.substring(11, elementEnd).trim().lowercase()
                    val enabled = if (separator >= 0) {
                        line.substring(separator + 1).split(';')
                            .firstOrNull { it.startsWith("enabled=") }
                            ?.substringAfter('=')
                            ?.let { it == "1" || it.equals("true", ignoreCase = true) }
                            ?: true
                    } else true
                    if (element in PROCEDURAL_KEYS) procedural[element] = enabled
                }
                line.startsWith("face=") && eq == 4 -> {
                    // The mood name is between `face=` and the first `;`.
                    // Do not use [eq] as the end index: it points to the
                    // equals sign at index 4 and caused substring(5, 4)
                    // crashes when a saved custom design was read.
                    val separator = line.indexOf(';', startIndex = 5)
                    val moodEnd = if (separator >= 0) separator else line.length
                    val mood = line.substring(5, moodEnd).trim()
                    val config = if (separator >= 0) line.substring(separator + 1) else ""
                    if (mood.isNotEmpty()) {
                        PetFace.parse(config)?.let { faces[mood] = it }
                    }
                }
                line.startsWith("react=") && eq == 5 -> {
                    // Same delimiter rule for `react=EVENT;...` lines.
                    val separator = line.indexOf(';', startIndex = 6)
                    val eventEnd = if (separator >= 0) separator else line.length
                    val event = line.substring(6, eventEnd).trim()
                    val config = if (separator >= 0) line.substring(separator + 1) else ""
                    if (event.isNotEmpty()) {
                        PetReaction.parse(config)?.let { reactions[event] = it }
                    }
                }
                eq == 1 && line.length > eq + 1 -> {
                    val key = line[0]
                    val hex = line.substring(eq + 1).trim()
                    if (hex.length == 6 && hex.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }) {
                        palette[key] = hex.uppercase()
                    }
                }
                else -> rows.add(line)
            }
        }
        if (rows.size < MIN_GRID_SIZE) return fallback
        val size = declaredSize?.takeIf { it == 16 || it == 24 || it == 32 }
            ?: when {
                rows.size >= 64 -> 32
                rows.size >= 48 -> 24
                else -> 16
            }
        // A well-formed design carries BOTH grids (2 × size rows); a
        // declared size with a truncated body/curled section is malformed.
        if (rows.size < size * 2) return fallback
        val body = rows.take(size).map { (it + ".".repeat(size)).take(size) }
        val curled = rows.drop(size).take(size).map { (it + ".".repeat(size)).take(size) }
        val faceMap = if (faces.isEmpty()) fallback.faces else faces
        val reactionMap = if (reactions.isEmpty()) fallback.reactions else reactions
        fun normalize(face: PetFace): PetFace = if (face.gridRows.isEmpty()) face else face.copy(
            gridRows = face.gridRows.map { (it + ".".repeat(size)).take(size) }
                .take(size)
                .let { parsedRows ->
                    if (parsedRows.size == size) parsedRows
                    else parsedRows + List(size - parsedRows.size) { ".".repeat(size) }
                }
        )
        return PetDesign(
            palette = if (palette.isEmpty()) fallback.palette else palette,
            bodyRows = body,
            curledRows = curled,
            gridSize = size,
            faces = faceMap.mapValues { (_, face) -> normalize(face) },
            reactions = reactionMap.mapValues { (_, reaction) -> reaction.copy(face = normalize(reaction.face)) },
            details = details.mapValues { (_, layer) ->
                layer.map { (it + ".".repeat(size)).take(size) }
                    .take(size)
                    .let { parsedRows ->
                        if (parsedRows.size == size) parsedRows
                        else parsedRows + List(size - parsedRows.size) { ".".repeat(size) }
                    }
            },
            procedural = procedural,
            petSpeciesId = petId ?: fallback.petSpeciesId
        )
    }

    /**
     * A fun random look: random palette hues for every key while keeping
     * the default grid shapes. Palette hues stay in a warm, pleasant band
     * so random designs still look like the Curie spirit.
     */
    fun randomize(seed: Int = System.nanoTime().toInt()): PetDesign {
        val r = kotlin.random.Random(seed)
        fun hsl(h: Float, s: Float, l: Float): String {
            val c = (1f - kotlin.math.abs(2f * l - 1f)) * s
            val hp = (h % 360f + 360f) % 360f / 60f
            val x = c * (1f - kotlin.math.abs(hp % 2f - 1f))
            val (r1, g1, b1) = when {
                hp < 1f -> Triple(c, x, 0f)
                hp < 2f -> Triple(x, c, 0f)
                hp < 3f -> Triple(0f, c, x)
                hp < 4f -> Triple(0f, x, c)
                hp < 5f -> Triple(x, 0f, c)
                else -> Triple(c, 0f, x)
            }
            val m = l - c / 2f
            fun toByte(v: Float): Int = ((v + m) * 255f).toInt().coerceIn(0, 255)
            return "%02X%02X%02X".format(toByte(r1), toByte(g1), toByte(b1))
        }
        val hue = r.nextFloat() * 360f
        val hue2 = (hue + r.nextFloat() * 60f - 30f + 360f) % 360f
        val bodyHue = hue
        val inkHue = (hue + 25f) % 360f
        val scarfHue = hue2
        val goldHue = (hue + 40f) % 360f
        val blushHue = (hue + 200f) % 360f
        val eyeHue = (hue + 12f) % 360f
        val newPalette = mapOf(
            'b' to hsl(bodyHue, r.nextFloat() * 0.25f + 0.30f, 0.86f),
            'B' to hsl(bodyHue, r.nextFloat() * 0.25f + 0.30f, 0.74f),
            'o' to hsl(inkHue, 0.45f, 0.26f),
            's' to hsl(scarfHue, 0.72f, 0.62f),
            'S' to hsl(scarfHue, 0.72f, 0.48f),
            'G' to hsl(goldHue, 0.85f, 0.72f),
            'g' to hsl(goldHue, 0.85f, 0.55f),
            'c' to hsl((scarfHue + 60f) % 360f, 0.55f, 0.80f),
            'C' to hsl((scarfHue + 140f) % 360f, 0.55f, 0.80f),
            'd' to hsl((scarfHue + 220f) % 360f, 0.50f, 0.78f),
            'D' to hsl((scarfHue + 300f) % 360f, 0.50f, 0.80f),
            'r' to hsl(blushHue, 0.55f, 0.78f),
            'y' to hsl(eyeHue, 0.45f, 0.30f)
        )
        return copy(palette = newPalette)
    }

    /** Paints one grid cell: [key] in [grid] ("body"/"curled") at [row]/[col]. */
    fun withPixel(grid: String, row: Int, col: Int, key: Char): PetDesign {
        val rows = (if (grid == "curled") curledRows else bodyRows).toMutableList()
        val line = rows.getOrNull(row) ?: return this
        if (col !in line.indices) return this
        val chars = line.toCharArray()
        chars[col] = key
        rows[row] = String(chars)
        return if (grid == "curled") copy(curledRows = rows) else copy(bodyRows = rows)
    }

    /**
     * Flood-fills the connected region of the same key at [row]/[col] with
     * [key] — the "fill bucket" tool.
     */
    fun withFloodFill(grid: String, row: Int, col: Int, key: Char): PetDesign {
        val src = if (grid == "curled") curledRows else bodyRows
        if (row !in src.indices) return this
        if (col !in src[row].indices) return this
        val target = src[row][col]
        if (target == key) return this
        val work = src.map { it.toCharArray() }
        val stack = ArrayDeque<Pair<Int, Int>>()
        stack.add(row to col)
        val size = gridSize
        while (stack.isNotEmpty()) {
            val (r, c) = stack.removeLast()
            if (r !in 0 until size || c !in 0 until size) continue
            if (work[r][c] != target) continue
            work[r][c] = key
            stack.add(r - 1 to c)
            stack.add(r + 1 to c)
            stack.add(r to c - 1)
            stack.add(r to c + 1)
        }
        val rows = work.map { String(it) }
        return if (grid == "curled") copy(curledRows = rows) else copy(bodyRows = rows)
    }

    /** Sets one palette color. */
    fun withPaletteColor(key: Char, hex: String): PetDesign {
        val clean = hex.uppercase().filter { it.isDigit() || it in 'A'..'F' }.take(6).padEnd(6, '0')
        return copy(palette = palette + (key to clean))
    }

    /** Sets a whole grid from pasted rows (padded/truncated to [gridSize]). */
    fun withGrid(grid: String, rows: List<String>): PetDesign {
        val cleaned = rows.map { (it + ".".repeat(gridSize)).take(gridSize) }.take(gridSize)
        return if (grid == "curled") copy(curledRows = cleaned) else copy(bodyRows = cleaned)
    }

    /**
     * Converts the design to another canvas size (24 ↔ 32, or back to 16)
     * by dominant-key resample — every pixel stays on the palette.
     */
    fun withSize(newSize: Int): PetDesign {
        if (newSize == gridSize) return this
        fun resizeFace(face: PetFace): PetFace = face.copy(
            gridRows = if (face.gridRows.isEmpty()) emptyList() else resizeGrid(face.gridRows, gridSize, newSize)
        )
        return copy(
            gridSize = newSize,
            bodyRows = resizeGrid(bodyRows, gridSize, newSize),
            curledRows = resizeGrid(curledRows, gridSize, newSize),
            faces = faces.mapValues { (_, face) -> resizeFace(face) },
            reactions = reactions.mapValues { (_, reaction) -> reaction.copy(face = resizeFace(reaction.face)) },
            details = details.mapValues { (_, rows) -> resizeGrid(rows, gridSize, newSize) }
        )
    }

    /** Overrides one mood's face. */
    fun withFace(moodName: String, face: PetFace): PetDesign =
        copy(faces = faces + (moodName to face))

    /** Paints one cell in a mood's hand-drawn face overlay. */
    fun withFacePixel(moodName: String, row: Int, col: Int, key: Char): PetDesign =
        withFace(moodName, faceFor(moodName).withPixel(row, col, key, gridSize))

    /** Paints one cell in an event reaction's hand-drawn face overlay. */
    fun withReactionFacePixel(eventName: String, row: Int, col: Int, key: Char): PetDesign =
        withReaction(eventName, reactionFor(eventName).copy(
            face = reactionFor(eventName).face.withPixel(row, col, key, gridSize)
        ))

    /** Overrides one event's reaction rule. */
    fun withReaction(eventName: String, reaction: PetReaction): PetDesign =
        copy(reactions = reactions + (eventName to reaction))

    /** Returns a normalized transparent detail layer, or a blank canvas. */
    fun detailFor(layer: String): List<String> {
        val rows = details[layer] ?: return List(gridSize) { ".".repeat(gridSize) }
        return rows.map { (it + ".".repeat(gridSize)).take(gridSize) }
            .take(gridSize)
            .let { normalized ->
                if (normalized.size == gridSize) normalized
                else normalized + List(gridSize - normalized.size) { ".".repeat(gridSize) }
            }
    }

    /** Paints one cell in a transparent detail layer. */
    fun withDetailPixel(layer: String, row: Int, col: Int, key: Char): PetDesign {
        if (layer !in DETAIL_KEYS || row !in 0 until gridSize || col !in 0 until gridSize) return this
        val rows = detailFor(layer).toMutableList()
        val chars = rows[row].toCharArray()
        chars[col] = key
        rows[row] = String(chars)
        return copy(details = details + (layer to rows))
    }

    /** Flood-fills one connected region in a transparent detail layer. */
    fun withDetailFloodFill(layer: String, row: Int, col: Int, key: Char): PetDesign {
        if (layer !in DETAIL_KEYS || row !in 0 until gridSize || col !in 0 until gridSize) return this
        val work = detailFor(layer).map { it.toCharArray() }
        val target = work[row][col]
        if (target == key) return this
        val stack = ArrayDeque<Pair<Int, Int>>()
        stack.add(row to col)
        while (stack.isNotEmpty()) {
            val (r, c) = stack.removeLast()
            if (r !in 0 until gridSize || c !in 0 until gridSize || work[r][c] != target) continue
            work[r][c] = key
            stack.add(r - 1 to c); stack.add(r + 1 to c)
            stack.add(r to c - 1); stack.add(r to c + 1)
        }
        return copy(details = details + (layer to work.map { String(it) }))
    }

    /** Replaces a complete detail layer. */
    fun withDetailGrid(layer: String, rows: List<String>): PetDesign {
        if (layer !in DETAIL_KEYS) return this
        val cleaned = rows.map { (it + ".".repeat(gridSize)).take(gridSize) }
            .take(gridSize)
        return copy(details = details + (layer to cleaned))
    }

    /** Sets whether one procedural element remains visible. */
    fun withProceduralEnabled(element: String, enabled: Boolean): PetDesign =
        if (element in PROCEDURAL_KEYS) copy(procedural = procedural + (element to enabled)) else this

    /** Missing flags intentionally mean enabled for old designs. */
    fun isProceduralEnabled(element: String): Boolean = procedural[element] ?: true
}

/**
 * A face the pet wears — eyes, mouth, blush and sparkles. The Face editor
 * lets the user customize these per mood, and the reaction editor attaches
 * a face to each event.
 */
data class PetFace(
    val eyes: EyeStyle = EyeStyle.OPEN,
    val mouth: MouthStyle = MouthStyle.SMILE,
    val blush: Boolean = true,
    val sparkles: Boolean = false,
    /** Transparent pixel overlay; empty means use the procedural fallback. */
    val gridRows: List<String> = emptyList()
) {
    fun toConfig(): String {
        val grid = if (gridRows.isEmpty()) "" else runCatching {
            java.net.URLEncoder.encode(gridRows.joinToString("\n"), "UTF-8")
        }.getOrDefault("")
        return "eyes=${eyes.name};mouth=${mouth.name};blush=${if (blush) 1 else 0};sparkles=${if (sparkles) 1 else 0};grid=$grid"
    }

    /** Paints one cell in the transparent overlay, creating a blank grid when needed. */
    fun withPixel(row: Int, col: Int, key: Char, gridSize: Int): PetFace {
        val rows = if (gridRows.size == gridSize) gridRows
        else List(gridSize) { ".".repeat(gridSize) }
        if (row !in rows.indices || col !in 0 until gridSize) return this
        val chars = rows[row].toCharArray()
        chars[col] = key
        return copy(gridRows = rows.toMutableList().also { it[row] = String(chars) })
    }

    /** Flood-fills one connected transparent face region. */
    fun withFloodFill(row: Int, col: Int, key: Char, gridSize: Int): PetFace {
        val rows = if (gridRows.size == gridSize) gridRows
        else List(gridSize) { ".".repeat(gridSize) }
        if (row !in 0 until gridSize || col !in 0 until gridSize) return this
        val work = rows.map { it.toCharArray() }
        val target = work[row][col]
        if (target == key) return this
        val stack = ArrayDeque<Pair<Int, Int>>()
        stack.add(row to col)
        while (stack.isNotEmpty()) {
            val (r, c) = stack.removeLast()
            if (r !in 0 until gridSize || c !in 0 until gridSize || work[r][c] != target) continue
            work[r][c] = key
            stack.add(r - 1 to c)
            stack.add(r + 1 to c)
            stack.add(r to c - 1)
            stack.add(r to c + 1)
        }
        return copy(gridRows = work.map { String(it) })
    }

    companion object {
        /** Tolerant parse of "eyes=STAR;mouth=WIDE;blush=1;sparkles=1". */
        fun parse(config: String): PetFace? {
            var eyes = EyeStyle.OPEN
            var mouth = MouthStyle.SMILE
            var blush = true
            var sparkles = false
            var gridRows = emptyList<String>()
            config.split(';').forEach { part ->
                val eq = part.indexOf('=')
                if (eq <= 0) return@forEach
                val key = part.substring(0, eq).trim().lowercase()
                val value = part.substring(eq + 1).trim()
                when (key) {
                    "eyes" -> EyeStyle.entries.firstOrNull { it.name.equals(value, ignoreCase = true) }?.let { eyes = it }
                    "mouth" -> MouthStyle.entries.firstOrNull { it.name.equals(value, ignoreCase = true) }?.let { mouth = it }
                    "blush" -> blush = value == "1" || value.equals("true", ignoreCase = true)
                    "sparkles" -> sparkles = value == "1" || value.equals("true", ignoreCase = true)
                    "grid" -> if (value.isNotBlank()) {
                        gridRows = runCatching {
                            java.net.URLDecoder.decode(value, "UTF-8").split("\n")
                        }.getOrDefault(emptyList())
                    }
                }
            }
            return PetFace(eyes, mouth, blush, sparkles, gridRows)
        }
    }
}

/**
 * A reaction rule for one event: whether the pet reacts at all, which
 * animation plays, and which face it wears while reacting.
 */
data class PetReaction(
    val enabled: Boolean = true,
    val anim: ReactionAnim = ReactionAnim.HOP,
    val face: PetFace = PetFace(eyes = EyeStyle.STAR, mouth = MouthStyle.WIDE, blush = true, sparkles = true),
    /** Custom speech lines for this event; one is chosen at random when enabled. */
    val lines: List<String> = emptyList()
) {
    fun toConfig(): String {
        val cleanLines = lines.map { it.trim().take(MAX_LINE_LENGTH) }
            .filter { it.isNotBlank() }
            .take(MAX_LINES)
        val encodedLines = runCatching {
            java.net.URLEncoder.encode(cleanLines.joinToString("\n"), "UTF-8")
        }.getOrDefault("")
        return "enabled=${if (enabled) 1 else 0};anim=${anim.name};lines=$encodedLines;${face.toConfig()}"
    }

    companion object {
        private const val MAX_LINES = 8
        private const val MAX_LINE_LENGTH = 120

        /** Limits the live editor draft without moving the cursor for ordinary typing. */
        fun limitDraft(text: String): String = text
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .split('\n', limit = MAX_LINES + 1)
            .take(MAX_LINES)
            .joinToString("\n") { it.take(MAX_LINE_LENGTH) }

        /** Keeps user-entered lines bounded and removes blank lines for storage. */
        fun normalizeLines(text: String): List<String> = limitDraft(text)
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .take(MAX_LINES)
            .toList()

        /** Tolerant parse of "enabled=1;anim=HOP;lines=...;eyes=STAR;...". */
        fun parse(config: String): PetReaction? {
            var enabled = true
            var anim = ReactionAnim.HOP
            var face = PetFace(eyes = EyeStyle.STAR, mouth = MouthStyle.WIDE, blush = true, sparkles = true)
            var lines = emptyList<String>()
            config.split(';').forEach { part ->
                val eq = part.indexOf('=')
                if (eq <= 0) return@forEach
                val key = part.substring(0, eq).trim().lowercase()
                val value = part.substring(eq + 1).trim()
                when (key) {
                    "enabled" -> enabled = value == "1" || value.equals("true", ignoreCase = true)
                    "anim" -> ReactionAnim.entries.firstOrNull { it.name.equals(value, ignoreCase = true) }?.let { anim = it }
                    "lines" -> {
                        lines = runCatching {
                            normalizeLines(java.net.URLDecoder.decode(value, "UTF-8"))
                        }.getOrDefault(emptyList())
                    }
                    // Accept the singular field for forward/backward-tolerant imports.
                    "line" -> {
                        val decoded = runCatching { java.net.URLDecoder.decode(value, "UTF-8") }.getOrNull()
                        lines = decoded?.let(::normalizeLines).orEmpty()
                    }
                    "eyes" -> EyeStyle.entries.firstOrNull { it.name.equals(value, ignoreCase = true) }?.let { face = face.copy(eyes = it) }
                    "mouth" -> MouthStyle.entries.firstOrNull { it.name.equals(value, ignoreCase = true) }?.let { face = face.copy(mouth = it) }
                    "blush" -> face = face.copy(blush = value == "1" || value.equals("true", ignoreCase = true))
                    "sparkles" -> face = face.copy(sparkles = value == "1" || value.equals("true", ignoreCase = true))
                    "grid" -> if (value.isNotBlank()) {
                        face = face.copy(gridRows = runCatching {
                            java.net.URLDecoder.decode(value, "UTF-8").split("\n")
                        }.getOrDefault(emptyList()))
                    }
                }
            }
            return PetReaction(enabled, anim, face, lines)
        }
    }
}

/** The eye styles the pet can wear (customizable in the Face editor). */
enum class EyeStyle { OPEN, BLINK, CLOSED, WIDE, STAR, DIZZY, HAPPY }

/** The mouth styles the pet can wear (customizable in the Face editor). */
enum class MouthStyle { SMILE, WIDE, O, NONE }

/** How the pet moves when a configured reaction fires. */
enum class ReactionAnim { HOP, SPIN, SQUISH, BOUNCE, NONE }

/** The events the pet can react to (reaction editor keys). */
object PetReactionEvents {
    const val TOUCH = "TOUCH"
    const val SPIN_LANDED = "SPIN_LANDED"
    const val REVEAL = "REVEAL"
    const val EXPLORE = "EXPLORE"
    const val SAVE = "SAVE"
    const val PLAY = "PLAY"
    const val LEVEL_UP = "LEVEL_UP"

    val ALL = listOf(TOUCH, SPIN_LANDED, REVEAL, EXPLORE, SAVE, PLAY, LEVEL_UP)

    fun label(event: String): String = when (event) {
        TOUCH -> "Petting / boops"
        SPIN_LANDED -> "Spin lands"
        REVEAL -> "Topic revealed"
        EXPLORE -> "Going exploring"
        SAVE -> "Keepsake saved"
        PLAY -> "Playtime"
        LEVEL_UP -> "Level up"
        else -> event
    }

    /** v8.50 — short "when does this fire" summary shown on the Actions
     *  landing cards in the Pet Designer (Phase 5). */
    fun trigger(event: String): String = when (event) {
        TOUCH -> "When you tap or boop Curie"
        SPIN_LANDED -> "When the spin lands a topic"
        REVEAL -> "When a topic opens"
        EXPLORE -> "When you start exploring"
        SAVE -> "When you save a keepsake"
        PLAY -> "During playtime"
        LEVEL_UP -> "When you level up"
        else -> event
    }

    /** v8.50 — preview-only speech line for the action preview. Shown in
     *  the Pet Designer's live bubble when the event has no custom lines;
     *  NEVER saved into the design (defaults live only in this object). */
    fun defaultLine(event: String): String = when (event) {
        TOUCH -> "Boop!"
        SPIN_LANDED -> "Ooh, what did we get?"
        REVEAL -> "Let's look inside!"
        EXPLORE -> "Adventure time!"
        SAVE -> "Kept safe forever!"
        PLAY -> "Wheee!"
        LEVEL_UP -> "We leveled up!"
        else -> "Yay!"
    }
}

/** The moods the Face editor exposes (a face per mood). */
object PetFaceMoods {
    const val HAPPY = "HAPPY"
    const val EXCITED = "EXCITED"
    const val SLEEPY = "SLEEPY"
    const val CURIOUS = "CURIOUS"
    const val PROUD = "PROUD"
    const val BOUNCY = "BOUNCY"
    const val FOCUSED = "FOCUSED"

    val ALL = listOf(HAPPY, EXCITED, SLEEPY, CURIOUS, PROUD, BOUNCY, FOCUSED)

    fun label(mood: String): String = when (mood) {
        HAPPY -> "Happy"
        EXCITED -> "Excited"
        SLEEPY -> "Sleepy"
        CURIOUS -> "Curious"
        PROUD -> "Proud"
        BOUNCY -> "Bouncy"
        FOCUSED -> "Focused"
        else -> mood
    }
}

/**
 * v8.38 — one-tap personality presets for the Face & reactions editor.
 * Each preset paints ALL mood faces AND ALL reaction rules at once, so a
 * single tap restyles Curie's whole personality (Shy, Party, Sleepyhead).
 */
object PetFacePresets {

    /** A ready-made personality: a face per mood + a rule per event. */
    data class Preset(
        val name: String,
        val tagline: String,
        val faces: Map<String, PetFace>,
        val reactions: Map<String, PetReaction>
    ) {
        /** Paints every mood face and every reaction rule onto [design]. */
        fun applyTo(design: PetDesign): PetDesign {
            var out = design
            faces.forEach { (mood, face) -> out = out.withFace(mood, face) }
            reactions.forEach { (event, reaction) -> out = out.withReaction(event, reaction) }
            return out
        }
    }

    /** Shy — bashful, wide-eyed, blushes at everything. */
    val SHY = Preset(
        name = "Shy",
        tagline = "Bashful — blushes at everything",
        faces = mapOf(
            "HAPPY" to PetFace(eyes = EyeStyle.WIDE, mouth = MouthStyle.SMILE, blush = true),
            "EXCITED" to PetFace(eyes = EyeStyle.HAPPY, mouth = MouthStyle.SMILE, blush = true),
            "SLEEPY" to PetFace(eyes = EyeStyle.CLOSED, mouth = MouthStyle.NONE, blush = false),
            "CURIOUS" to PetFace(eyes = EyeStyle.OPEN, mouth = MouthStyle.O, blush = true),
            "PROUD" to PetFace(eyes = EyeStyle.HAPPY, mouth = MouthStyle.SMILE, blush = true),
            "BOUNCY" to PetFace(eyes = EyeStyle.HAPPY, mouth = MouthStyle.SMILE, blush = true),
            "FOCUSED" to PetFace(eyes = EyeStyle.OPEN, mouth = MouthStyle.NONE, blush = true)
        ),
        reactions = mapOf(
            "TOUCH" to PetReaction(anim = ReactionAnim.HOP, face = PetFace(eyes = EyeStyle.WIDE, mouth = MouthStyle.O, blush = true)),
            "SPIN_LANDED" to PetReaction(anim = ReactionAnim.SQUISH, face = PetFace(eyes = EyeStyle.HAPPY, mouth = MouthStyle.SMILE, blush = true)),
            "REVEAL" to PetReaction(anim = ReactionAnim.HOP, face = PetFace(eyes = EyeStyle.WIDE, mouth = MouthStyle.O, blush = true)),
            "EXPLORE" to PetReaction(anim = ReactionAnim.HOP, face = PetFace(eyes = EyeStyle.OPEN, mouth = MouthStyle.SMILE, blush = true)),
            "SAVE" to PetReaction(anim = ReactionAnim.SQUISH, face = PetFace(eyes = EyeStyle.HAPPY, mouth = MouthStyle.SMILE, blush = true)),
            "PLAY" to PetReaction(anim = ReactionAnim.BOUNCE, face = PetFace(eyes = EyeStyle.HAPPY, mouth = MouthStyle.SMILE, blush = true)),
            "LEVEL_UP" to PetReaction(anim = ReactionAnim.SPIN, face = PetFace(eyes = EyeStyle.STAR, mouth = MouthStyle.WIDE, blush = true))
        )
    )

    /** Party — starry eyes and sparkles everywhere. */
    val PARTY = Preset(
        name = "Party",
        tagline = "Sparkly & bursting with excitement",
        faces = mapOf(
            "HAPPY" to PetFace(eyes = EyeStyle.STAR, mouth = MouthStyle.WIDE, blush = true, sparkles = true),
            "EXCITED" to PetFace(eyes = EyeStyle.STAR, mouth = MouthStyle.WIDE, blush = true, sparkles = true),
            "SLEEPY" to PetFace(eyes = EyeStyle.CLOSED, mouth = MouthStyle.NONE, blush = false, sparkles = false),
            "CURIOUS" to PetFace(eyes = EyeStyle.STAR, mouth = MouthStyle.SMILE, blush = true, sparkles = true),
            "PROUD" to PetFace(eyes = EyeStyle.STAR, mouth = MouthStyle.WIDE, blush = true, sparkles = true),
            "BOUNCY" to PetFace(eyes = EyeStyle.STAR, mouth = MouthStyle.WIDE, blush = true, sparkles = true),
            "FOCUSED" to PetFace(eyes = EyeStyle.OPEN, mouth = MouthStyle.SMILE, blush = false, sparkles = false)
        ),
        reactions = mapOf(
            "TOUCH" to PetReaction(anim = ReactionAnim.BOUNCE, face = PetFace(eyes = EyeStyle.STAR, mouth = MouthStyle.WIDE, blush = true, sparkles = true)),
            "SPIN_LANDED" to PetReaction(anim = ReactionAnim.SPIN, face = PetFace(eyes = EyeStyle.STAR, mouth = MouthStyle.WIDE, blush = true, sparkles = true)),
            "REVEAL" to PetReaction(anim = ReactionAnim.SPIN, face = PetFace(eyes = EyeStyle.STAR, mouth = MouthStyle.WIDE, blush = true, sparkles = true)),
            "EXPLORE" to PetReaction(anim = ReactionAnim.BOUNCE, face = PetFace(eyes = EyeStyle.STAR, mouth = MouthStyle.WIDE, sparkles = true)),
            "SAVE" to PetReaction(anim = ReactionAnim.BOUNCE, face = PetFace(eyes = EyeStyle.STAR, mouth = MouthStyle.WIDE, blush = true, sparkles = true)),
            "PLAY" to PetReaction(anim = ReactionAnim.SPIN, face = PetFace(eyes = EyeStyle.STAR, mouth = MouthStyle.WIDE, blush = true, sparkles = true)),
            "LEVEL_UP" to PetReaction(anim = ReactionAnim.SPIN, face = PetFace(eyes = EyeStyle.STAR, mouth = MouthStyle.WIDE, blush = true, sparkles = true))
        )
    )

    /** Sleepyhead — dozy, calm, barely reacts. */
    val SLEEPYHEAD = Preset(
        name = "Sleepyhead",
        tagline = "Dozy & calm — big yawns, tiny reactions",
        faces = mapOf(
            "HAPPY" to PetFace(eyes = EyeStyle.CLOSED, mouth = MouthStyle.SMILE, blush = false),
            "EXCITED" to PetFace(eyes = EyeStyle.CLOSED, mouth = MouthStyle.SMILE, blush = false),
            "SLEEPY" to PetFace(eyes = EyeStyle.CLOSED, mouth = MouthStyle.NONE, blush = false),
            "CURIOUS" to PetFace(eyes = EyeStyle.BLINK, mouth = MouthStyle.SMILE, blush = false),
            "PROUD" to PetFace(eyes = EyeStyle.HAPPY, mouth = MouthStyle.SMILE, blush = false),
            "BOUNCY" to PetFace(eyes = EyeStyle.CLOSED, mouth = MouthStyle.SMILE, blush = false),
            "FOCUSED" to PetFace(eyes = EyeStyle.CLOSED, mouth = MouthStyle.SMILE, blush = false)
        ),
        reactions = mapOf(
            "TOUCH" to PetReaction(anim = ReactionAnim.SQUISH, face = PetFace(eyes = EyeStyle.CLOSED, mouth = MouthStyle.SMILE)),
            "SPIN_LANDED" to PetReaction(anim = ReactionAnim.NONE, face = PetFace(eyes = EyeStyle.CLOSED, mouth = MouthStyle.NONE)),
            "REVEAL" to PetReaction(anim = ReactionAnim.NONE, face = PetFace(eyes = EyeStyle.CLOSED, mouth = MouthStyle.SMILE)),
            "EXPLORE" to PetReaction(anim = ReactionAnim.HOP, face = PetFace(eyes = EyeStyle.CLOSED, mouth = MouthStyle.SMILE)),
            "SAVE" to PetReaction(anim = ReactionAnim.SQUISH, face = PetFace(eyes = EyeStyle.CLOSED, mouth = MouthStyle.SMILE)),
            "PLAY" to PetReaction(anim = ReactionAnim.HOP, face = PetFace(eyes = EyeStyle.CLOSED, mouth = MouthStyle.SMILE)),
            "LEVEL_UP" to PetReaction(anim = ReactionAnim.NONE, face = PetFace(eyes = EyeStyle.HAPPY, mouth = MouthStyle.SMILE))
        )
    )

    val ALL = listOf(SHY, PARTY, SLEEPYHEAD)
}

/**
 * v8.48 — a keyframe of a [PetAnimation]: a small transform applied to the
 * base design. Kept procedural (no per-frame pixel layers yet) so every
 * animation plays on any custom design; future phases add pixel overrides.
 */
data class PetAnimationFrame(
    val durationMs: Int = 180,
    val offsetY: Float = 0f,
    val scale: Float = 1f,
    val rotationDegrees: Float = 0f
)

/**
 * v8.48 — a named, looping animation: [frames] each transform the pet, and
 * [mood] sets the face worn while it plays (a mood name string so the model
 * stays independent of the sprite layer).
 */
data class PetAnimation(
    val id: String,
    val name: String,
    val mood: String,
    val frames: List<PetAnimationFrame>,
    val loop: Boolean = true
)

/** v8.48 — built-in animation presets for the designer gallery + timeline. */
val BUILTIN_ANIMATIONS: List<PetAnimation> = listOf(
    PetAnimation("idle", "Idle", "HAPPY", listOf(
        PetAnimationFrame(520, 0f, 1f, 0f),
        PetAnimationFrame(520, -1.5f, 1.015f, 0f)
    )),
    PetAnimation("happy", "Happy", "HAPPY", listOf(
        PetAnimationFrame(140, 0f, 1f, 0f),
        PetAnimationFrame(160, -7f, 1.05f, 0f),
        PetAnimationFrame(160, 0f, 0.96f, 0f),
        PetAnimationFrame(160, -3f, 1.03f, 0f)
    )),
    PetAnimation("excited", "Excited", "EXCITED", listOf(
        PetAnimationFrame(110, -4f, 1.06f, -3f),
        PetAnimationFrame(110, 0f, 0.98f, 3f),
        PetAnimationFrame(110, -6f, 1.08f, -2f),
        PetAnimationFrame(110, 0f, 0.98f, 2f)
    )),
    PetAnimation("sleepy", "Sleepy", "SLEEPY", listOf(
        PetAnimationFrame(700, 2f, 1f, 0f),
        PetAnimationFrame(700, 1f, 0.99f, 0f)
    )),
    PetAnimation("curious", "Curious", "CURIOUS", listOf(
        PetAnimationFrame(260, 0f, 1f, -4f),
        PetAnimationFrame(260, -1f, 1f, 4f),
        PetAnimationFrame(260, 0f, 1f, 0f)
    )),
    PetAnimation("proud", "Proud", "PROUD", listOf(
        PetAnimationFrame(200, -6f, 1.07f, 0f),
        PetAnimationFrame(400, -2f, 1.02f, 0f)
    )),
    PetAnimation("bouncy", "Bouncy", "BOUNCY", listOf(
        PetAnimationFrame(120, 0f, 0.94f, 0f),
        PetAnimationFrame(120, -8f, 1.06f, 0f),
        PetAnimationFrame(120, -3f, 0.97f, 0f),
        PetAnimationFrame(120, 0f, 1f, 0f)
    )),
    PetAnimation("focused", "Focused", "FOCUSED", listOf(
        PetAnimationFrame(500, 0f, 1f, 0f),
        PetAnimationFrame(500, 0f, 1.012f, 0f)
    )),
    PetAnimation("touch", "Touch", "HAPPY", listOf(
        PetAnimationFrame(90, 0f, 0.86f, 0f),
        PetAnimationFrame(260, 0f, 1f, 0f)
    )),
    PetAnimation("spin", "Spin landed", "EXCITED", listOf(
        PetAnimationFrame(130, 0f, 1f, 0f),
        PetAnimationFrame(130, 0f, 1f, 90f),
        PetAnimationFrame(130, 0f, 1f, 180f),
        PetAnimationFrame(130, 0f, 1f, 270f),
        PetAnimationFrame(180, 0f, 1.05f, 360f)
    )),
    PetAnimation("reveal", "Reveal", "HAPPY", listOf(
        PetAnimationFrame(120, 0f, 0.7f, 0f),
        PetAnimationFrame(120, -2f, 1.1f, 0f),
        PetAnimationFrame(180, 0f, 1f, 0f)
    )),
    PetAnimation("explore", "Explore", "CURIOUS", listOf(
        PetAnimationFrame(240, -2f, 1f, 3f),
        PetAnimationFrame(240, 0f, 1f, -2f),
        PetAnimationFrame(240, 0f, 1f, 0f)
    )),
    PetAnimation("save", "Save", "HAPPY", listOf(
        PetAnimationFrame(130, 0f, 0.95f, 0f),
        PetAnimationFrame(130, -9f, 1.1f, 0f),
        PetAnimationFrame(160, -2f, 1.02f, 0f),
        PetAnimationFrame(160, 0f, 1f, 0f)
    )),
    PetAnimation("play", "Play", "BOUNCY", listOf(
        PetAnimationFrame(110, -5f, 1.05f, -5f),
        PetAnimationFrame(110, 0f, 0.95f, 5f),
        PetAnimationFrame(110, -5f, 1.05f, 5f),
        PetAnimationFrame(110, 0f, 0.95f, -5f)
    )),
    PetAnimation("levelup", "Level up", "PROUD", listOf(
        PetAnimationFrame(140, -8f, 1.15f, -4f),
        PetAnimationFrame(140, -4f, 1.05f, 4f),
        PetAnimationFrame(140, -8f, 1.12f, -2f),
        PetAnimationFrame(220, 0f, 1f, 0f)
    ))
)

/** Looks up a built-in animation by id. */
fun animationById(id: String): PetAnimation? =
    BUILTIN_ANIMATIONS.firstOrNull { it.id == id }

/** Display name for an animation id (built-in name or a readable fallback). */
fun petAnimationName(id: String): String =
    BUILTIN_ANIMATIONS.firstOrNull { it.id == id }?.name ?: id.replaceFirstChar { it.uppercase() }
