package com.curio.app.features.personal

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.curio.app.ui.theme.BebasNeueFontFamily
import com.curio.app.ui.theme.LoraFontFamily
import com.curio.app.ui.theme.PlayfairDisplayFontFamily
import com.curio.app.ui.theme.PatrickHandFontFamily
import com.curio.app.ui.theme.SpaceGroteskFontFamily
import com.curio.app.ui.theme.SpaceMonoFontFamily
import com.curio.app.ui.theme.WritingFontFamily

/**
 * v401 — WHAT A PRINT'S CAPTION CAN WEAR.
 *
 * A caption is a print's own little label — the strip of paper under the
 * picture where a hand writes what the picture is. The label was set in one
 * face at one size, whatever the print, which is wrong in the way a handwritten
 * album is never wrong: a polaroid's strip is written on in whatever pen came
 * out of the drawer that day (user request: "give its own font choices in tools
 * when im editing caption", and — asked and answered — "a prints own typography,
 * new ones complimenting the polaroid style").
 *
 * These are the app's OWN bundled faces (see `CurioTypography`) — no download,
 * no network, and every one of them is a face a paper label could plausibly
 * have been written or stamped in. The keys are what the saved JSON carries, and
 * `""` is deliberately the first entry: it means THE PRINT'S OWN, so a caption
 * written before this version keeps reading exactly as it read (and a caption
 * that never chooses costs nothing in the file).
 */
internal enum class PersonalCaptionFace(
    val key: String,
    val label: String
) {
    /** The print's own — the writing face, what every caption looked like before this. */
    PRINT("", "Print's own"),
    HAND("hand", "Handwriting"),
    SERIF("serif", "Serif"),
    DISPLAY("display", "Display"),
    MONO("mono", "Mono"),
    POSTER("poster", "Poster"),
    MODERN("modern", "Modern");

    /** The family this face is drawn in. */
    val family: FontFamily
        get() = when (this) {
            PRINT -> WritingFontFamily
            HAND -> PatrickHandFontFamily
            SERIF -> LoraFontFamily
            DISPLAY -> PlayfairDisplayFontFamily
            MONO -> SpaceMonoFontFamily
            POSTER -> BebasNeueFontFamily
            MODERN -> SpaceGroteskFontFamily
        }
}

/** A saved face key back to its face; an unknown key reads as the print's own. */
internal fun personalCaptionFace(key: String): PersonalCaptionFace =
    PersonalCaptionFace.entries.firstOrNull { it.key == key } ?: PersonalCaptionFace.PRINT

/**
 * HOW BIG THE LABEL IS WRITTEN, relative to the print's own frame.
 *
 * A caption's size was fixed by which print it sat under (a page-sized print
 * got 13sp, a small one 10sp) — so the only way to get bigger writing on a small
 * print was to make the whole photograph bigger, which is not what "I want to
 * read this bit" means. This is its OWN setting, a multiplier on whatever the
 * frame gave it, so the label stays in proportion if the print is resized.
 */
internal enum class PersonalCaptionLabelSize(
    val key: String,
    val label: String,
    val factor: Float
) {
    SMALL("small", "Small", 0.84f),
    STANDARD("", "Standard", 1f),
    LARGE("large", "Large", 1.24f)
}

internal fun personalCaptionLabelSize(key: String): PersonalCaptionLabelSize =
    PersonalCaptionLabelSize.entries.firstOrNull { it.key == key }
        ?: PersonalCaptionLabelSize.STANDARD

/** The label's type size: the print's own, scaled by the label's own setting. */
internal fun personalCaptionSizeSp(base: TextUnit, key: String): TextUnit =
    (base.value * personalCaptionLabelSize(key).factor).sp

/**
 * THE ORDER A DATE IS WRITTEN IN.
 *
 * The member asked for a date on the label "in dd:mm:yyyy or user can switch" —
 * and, asked and answered, the order lives BOTH ways: an app-wide preference
 * (what most labels should do) and a per-caption override (what this one print
 * does), with the override winning where it is set. That is why a date is stored
 * as a DATE and not as text: the same 14 March 2026 comes out "14/03/2026",
 * "03/14/2026" or "2026/03/14" depending on who is reading, and a typed-in
 * string can never be re-written that way.
 */
internal enum class PersonalCaptionDateOrder(
    val key: String,
    val label: String,
    /** An example of the order, for the menu — a date of its OWN alphabet. */
    val hint: String
) {
    DAY_FIRST("dmy", "Day first", "14/03/2026"),
    MONTH_FIRST("mdy", "Month first", "03/14/2026"),
    YEAR_FIRST("ymd", "Year first", "2026/03/14")
}

/**
 * The date order — the app-wide preference, and the one place a member can set
 * it (the caption's own date menu).
 *
 * Read from composition (a print's label asks for its order while it draws) and
 * written from the tools, so this holds one snapshot value: re-ordering the app
 * re-writes every label that has not overridden it, live. The preference file is
 * small and its own, like the pet overlay's — a settings key does not belong in
 * a note's row.
 */
internal object PersonalCaptionDates {

    private const val PREFS = "curio_personal_writing"
    private const val KEY = "caption_date_order"

    /** Null until the file has been read once (see [order]). */
    private val appOrder = mutableStateOf<PersonalCaptionDateOrder?>(null)

    /** The app-wide order; absent (or unreadable) is Day first, which is how
     *  every date this app has ever shown reads. */
    fun order(context: Context, override: String = ""): PersonalCaptionDateOrder {
        PersonalCaptionDateOrder.entries.firstOrNull { it.key == override }?.let { return it }
        appOrder.value?.let { return it }
        val saved = runCatching {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
        }.getOrNull()
        val resolved = PersonalCaptionDateOrder.entries.firstOrNull { it.key == saved }
            ?: PersonalCaptionDateOrder.DAY_FIRST
        if (appOrder.value != resolved) appOrder.value = resolved
        return resolved
    }

    /** Sets the app-wide order and persists it (best effort — a failed write
     *  still re-orders this session). */
    fun setOrder(context: Context, order: PersonalCaptionDateOrder) {
        appOrder.value = order
        runCatching {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY, order.key).apply()
        }
    }
}

/**
 * A stored date as it is written on the label.
 *
 * The millis are local midnight of the day the caption stands for (see
 * `PersonalBlock.captionDateMillis`), which is why this only ever asks for the
 * DATE: a label says "14/03/2026", never a clock time. Formatting by hand rather
 * than through a pattern keeps it identical in every locale — a date stamp on a
 * photograph does not translate itself.
 */
internal fun personalCaptionDateText(millis: Long, order: PersonalCaptionDateOrder): String {
    if (millis <= 0L) return ""
    val date = runCatching {
        java.time.Instant.ofEpochMilli(millis)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
    }.getOrNull() ?: return ""
    val day = date.dayOfMonth
    val month = date.monthValue
    val year = date.year
    return when (order) {
        PersonalCaptionDateOrder.DAY_FIRST ->
            "%02d/%02d/%04d".format(day, month, year)
        PersonalCaptionDateOrder.MONTH_FIRST ->
            "%02d/%02d/%04d".format(month, day, year)
        PersonalCaptionDateOrder.YEAR_FIRST ->
            "%04d/%02d/%02d".format(year, month, day)
    }
}

/** Today, as local midnight — what the label's date button stamps. */
internal fun personalCaptionToday(): Long = runCatching {
    java.time.LocalDate.now()
        .atStartOfDay(java.time.ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
}.getOrDefault(System.currentTimeMillis())

/** A day's date a given number of days back (the menu's "Yesterday"). */
internal fun personalCaptionDaysAgo(days: Long): Long = runCatching {
    java.time.LocalDate.now()
        .minusDays(days)
        .atStartOfDay(java.time.ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
}.getOrDefault(System.currentTimeMillis())
