package com.curio.app.data

/**
 * The Curio pet's custom look — a full 16×16 pixel design with its own
 * palette, serialized as a plain-text file the Pet designer playground can
 * import and export (v8.34). When a design is saved (always-on — see
 * [AppPreferences]), the pet sprite renders this design everywhere instead
 * of the default.
 *
 * ## Import format (plain text, hex colors)
 *
 * A design is a tiny text file: a palette line per grid key (key=RRGGBB),
 * then the BODY 16×16 grid, then the CURLED (asleep) 16×16 grid. Lines
 * starting with `#` are comments. Example:
 *
 * ```
 * # Curio pet design
 * # Palette: one hex color per grid key
 * b=FFF3DC
 * B=F0DDBB
 * o=4A3426
 * s=FF6F61
 * S=D95A50
 * G=FFD97D
 * g=E0B050
 * # Body grid — 16 rows of exactly 16 chars (. empty, keys from palette)
 * .......GG.......
 * ...o...GG...o...
 * ..ob........bo..
 * ..ob....o....bo.
 * ...oooooooooo...
 * .obbbbbbbbbbbbo.
 * obbbbbbbbbbbbbbo
 * obbbbbbbbbbbbbbo
 * obbbbbbbbbbbBBbo
 * obbbbbbbbbbbBBbo
 * obbbbbbbbbbbBBbo
 * obbbbbbbbbbbbBBo
 * .osssssssssssso.
 * ..oSSssssssSSo..
 * ..oo........oo..
 * ..oo........oo..
 * # Curled (asleep) grid — 16 rows of exactly 16 chars
 * .......GG.......
 * ...o...GG...o...
 * ..ob........bo..
 * ..obbbbbbbbbbo..
 * ...obbbbbbbbo...
 * ..obbbbbbbbbbbo.
 * .obbbbbbbbbbbbo.
 * obbbbbbbbbbbbbbo
 * obssssssssssssbo
 * .obbbbbbbbbbbbo.
 * ..obbbbbbbbbbbo.
 * ...obbbbbbbbo...
 * .....oBBbbbbBBo.
 * ......oooooo....
 * ................
 * ................
 * ```
 *
 * Grid keys: `b` body, `B` body shade, `o` ink outline, `s` scarf accent,
 * `S` scarf shade, `G` gold, `g` gold deep. Any other char renders empty.
 * Missing palette keys fall back to the default look, so a design only has
 * to list the colors it actually changes.
 */
data class PetDesign(
    val palette: Map<Char, String>,
    val bodyRows: List<String>,
    val curledRows: List<String>
) {
    /** The default palette keys a design may recolor. */
    companion object {
        val KEYS = listOf('b', 'B', 'o', 's', 'S', 'G', 'g')

        val DEFAULT_PALETTE: Map<Char, String> = mapOf(
            'b' to "FFF3DC", // body — warm cream
            'B' to "F0DDBB", // body shade — deeper cream
            'o' to "4A3426", // ink outline / face — warm brown
            's' to "FF6F61", // scarf accent — coral
            'S' to "D95A50", // scarf shade — deeper coral
            'G' to "FFD97D", // gold — antenna star / sparkles
            'g' to "E0B050"  // gold deep — halo detail
        )

        val DEFAULT_BODY: List<String> = listOf(
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

        val DEFAULT_CURLED: List<String> = listOf(
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

        val DEFAULT = PetDesign(DEFAULT_PALETTE, DEFAULT_BODY, DEFAULT_CURLED)

        /**
         * The same design [curled] shows while asleep. When the user paints
         * the BODY grid they usually want the sleep pose to follow, so the
         * playground offers \"copy body to curled\" — this is that copy.
         */
        fun bodyAsCurled(body: List<String>): List<String> = body
    }

    /** Resolves a grid key to its hex color, falling back to the default. */
    fun colorOf(key: Char): String = palette[key] ?: DEFAULT_PALETTE[key] ?: "000000"

    /** True when this design differs from the default in any way. */
    val isCustom: Boolean get() = palette != DEFAULT_PALETTE || bodyRows != DEFAULT_BODY || curledRows != DEFAULT_CURLED

    /** The full design as importable text (the format shown in the header). */
    fun toText(): String = buildString {
        appendLine("# Curio pet design")
        appendLine("# Palette: one hex color per grid key")
        KEYS.forEach { key ->
            appendLine("$key=${colorOf(key)}")
        }
        appendLine("# Body grid — 16 rows of exactly 16 chars (. empty, keys from palette)")
        bodyRows.forEach { appendLine(it) }
        appendLine("# Curled (asleep) grid — 16 rows of exactly 16 chars")
        curledRows.forEach { appendLine(it) }
    }

    /**
     * Parses [text] back into a design. Tolerant: blank lines and `#`
     * comments are skipped, `key=HEX` lines fill the palette, and the next
     * 16 non-comment lines become the body grid with the following 16 the
     * curled grid. Short rows are padded, long rows truncated to 16. Never
     * throws — on a completely unparseable input it returns [DEFAULT].
     */
    fun toParsedOr(text: String, fallback: PetDesign): PetDesign {
        val palette = mutableMapOf<Char, String>()
        val rows = mutableListOf<String>()
        text.lineSequence().forEach { raw ->
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) return@forEach
            val eq = line.indexOf('=')
            if (eq == 1 && line.length > eq + 1) {
                val key = line[0]
                val hex = line.substring(eq + 1).trim()
                if (hex.length == 6 && hex.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }) {
                    palette[key] = hex.uppercase()
                }
                return@forEach
            }
            if (rows.size < 32) {
                val padded = (line + ".".repeat(16)).take(16)
                rows.add(padded)
            }
        }
        if (rows.size < 32) return fallback
        val body = rows.take(16)
        val curled = rows.drop(16).take(16)
        return PetDesign(
            palette = if (palette.isEmpty()) fallback.palette else palette,
            bodyRows = body,
            curledRows = curled
        )
    }

    /**
     * A fun random look: random palette hues for body/ink/scarf/gold while
     * keeping the default grid shapes. Palette hues stay in a warm, pleasant
     * band so random designs still look like the Curio spirit.
     */
    fun randomize(seed: Int = System.nanoTime().toInt()): PetDesign {
        val r = kotlin.random.Random(seed)
        // HSL→RGB inline (no theme dependency): h in degrees, s/l in 0..1.
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
        // Pick a base hue; keep body cream-ish but tinted, ink stays warm-dark.
        val hue = r.nextFloat() * 360f
        val hue2 = (hue + r.nextFloat() * 60f - 30f + 360f) % 360f
        val bodyHue = hue
        val inkHue = (hue + 25f) % 360f
        val scarfHue = hue2
        val goldHue = (hue + 40f) % 360f
        val newPalette = mapOf(
            'b' to hsl(bodyHue, r.nextFloat() * 0.25f + 0.30f, 0.86f),
            'B' to hsl(bodyHue, r.nextFloat() * 0.25f + 0.30f, 0.74f),
            'o' to hsl(inkHue, 0.45f, 0.26f),
            's' to hsl(scarfHue, 0.72f, 0.62f),
            'S' to hsl(scarfHue, 0.72f, 0.48f),
            'G' to hsl(goldHue, 0.85f, 0.72f),
            'g' to hsl(goldHue, 0.85f, 0.55f)
        )
        return copy(palette = newPalette)
    }

    /** Paints one grid cell: [key] in [grid] (\"body\"/\"curled\") at [row]/[col]. */
    fun withPixel(grid: String, row: Int, col: Int, key: Char): PetDesign {
        val rows = if (grid == "curled") curledRows.toMutableList() else bodyRows.toMutableList()
        val line = rows.getOrNull(row) ?: return this
        if (col !in line.indices) return this
        val chars = line.toCharArray()
        chars[col] = key
        rows[row] = String(chars)
        return if (grid == "curled") copy(curledRows = rows) else copy(bodyRows = rows)
    }

    /** Sets one palette color. */
    fun withPaletteColor(key: Char, hex: String): PetDesign {
        val clean = hex.uppercase().filter { it.isDigit() || it in 'A'..'F' }.take(6).padEnd(6, '0')
        return copy(palette = palette + (key to clean))
    }

    /** Sets a whole grid from pasted rows (padded/truncated to 16×16). */
    fun withGrid(grid: String, rows: List<String>): PetDesign {
        val cleaned = rows.map { (it + ".".repeat(16)).take(16) }
        return if (grid == "curled") copy(curledRows = cleaned) else copy(bodyRows = cleaned)
    }
}
