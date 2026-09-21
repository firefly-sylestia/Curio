package com.curio.app.features.incursion

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curio.app.data.IncursionCatalog
import com.curio.app.data.IncursionEntry
import com.curio.app.data.IncursionStore
import com.curio.app.data.IncursionStudio
import com.curio.app.features.settings.settingsRoseAccent
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.curioCardShadow
import com.curio.app.ui.theme.curioTintOn
import com.curio.app.ui.theme.isCurioDarkTheme

/**
 * v427 — THE PERSONAL TAB: where the member is.
 *
 * The member: *"instead of essential tab add personal tab where [the] status
 * shows"* — and, asked what that tab should hold, *"all 3"*: the status desk, the
 * six-state tally, AND the derived figures. So this is the whole of what the page
 * knows about THEM, on one card at a time, in the app's own language.
 *
 * It is deliberately a READ-ONLY page. Everything here is a fact about the
 * statuses the rows already carry — nothing on this tab invents a new state, and
 * the only thing it can do is take you to the title it is talking about (the next
 * row that needs you, one tap into its sheet). That is what keeps a stats page
 * from becoming a second control surface for the same list.
 *
 * Order, top to bottom — the order the questions are actually asked:
 *
 *  1. **How far in am I?** — the whole order, watched of total.
 *  2. **Where?** — one bar per line (Marvel · Sony · X-Men).
 *  3. **What is next?** — the first row each line still wants from you.
 *  4. **What have I decided?** — the six states and how many rows wear each.
 *  5. **The figures** — hours in, hours left, essentials, and the longest run of
 *     consecutive titles in the order, which is the one number here that is not
 *     just a count of rows.
 */
@Composable
internal fun IncursionPersonalDesk(
    studios: List<IncursionStudio>,
    statuses: Map<String, Int>,
    onOpen: (IncursionEntry) -> Unit
) {
    val accent = settingsRoseAccent()
    val dark = isCurioDarkTheme()
    val shape = RoundedCornerShape(24.dp)
    val entries = remember(studios) { studios.flatMap { it.entries } }

    // The status of a row, read the SAME way the list reads it: the stored ordinal
    // when there is one, and NOT WATCHED otherwise.
    fun statusOf(entry: IncursionEntry): IncursionStore.Status =
        statuses[entry.storageKey]
            ?.let { IncursionStore.Status.entries.getOrNull(it) }
            ?: IncursionStore.Status.UNWATCHED

    val watched = entries.count { statusOf(it) == IncursionStore.Status.WATCHED }
    val total = entries.size
    val minutes = entries.sumOf { minutesOf(it) }
    val minutesIn = entries.filter { statusOf(it) == IncursionStore.Status.WATCHED }
        .sumOf { minutesOf(it) }
    val essentials = remember(studios) { IncursionCatalog.essentials(studios) }
    val essentialsDone = essentials.count { statusOf(it.second) == IncursionStore.Status.WATCHED }
    val states = IncursionStore.Status.entries.map { state ->
        state to entries.count { statusOf(it) == state }
    }
    // The first row each line still wants: not watched, and not one the member has
    // deliberately set aside. A DROPPED row is a decision, so "next up" stepping
    // over it is the only reading that respects it.
    val nextUp = remember(studios, statuses) {
        studios.mapNotNull { studio ->
            studio.entries
                .sortedBy { it.order }
                .firstOrNull { entry ->
                    val state = statusOf(entry)
                    state == IncursionStore.Status.UNWATCHED ||
                        state == IncursionStore.Status.PLANNED
                }
                ?.let { studio to it }
        }
    }
    // The longest unbroken run of WATCHED rows, in each line's own order — the one
    // figure that says "you went straight through" rather than "you counted".
    val bestRun = remember(studios, statuses) {
        studios
            .map { studio ->
                // A lambda rather than a callable reference: `::localFunction`
                // is not something to rely on compiling, and a lambda that just
                // forwards is the same one call.
                studio to longestRun(studio.entries.sortedBy { it.order }) { statusOf(it) }
            }
            .maxByOrNull { it.second }
            ?.takeIf { it.second > 0 }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── 1 · How far in ──────────────────────────────────────────────────
        item(key = "personal-progress") {
            Card(shape, accent, dark) {
                Text(
                    "THE WHOLE ORDER",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.3.sp,
                    color = accent
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "$watched",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        " of $total watched",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(8.dp))
                Bar(
                    fraction = if (total == 0) 0f else watched.toFloat() / total,
                    accent = accent
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    if (watched == 0) {
                        "Nothing marked yet — open a line and start where the order starts."
                    } else if (watched == total && total > 0) {
                        "Every title in the order is watched."
                    } else {
                        "${percent(watched, total)}% of the order is behind you."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ── 2 · Where ───────────────────────────────────────────────────────
        item(key = "personal-lines") {
            Card(shape, accent, dark) {
                Heading("BY LINE", accent)
                Spacer(Modifier.height(10.dp))
                studios.forEachIndexed { index, studio ->
                    if (index > 0) Spacer(Modifier.height(12.dp))
                    val lineTotal = studio.entries.size
                    val lineWatched = studio.entries.count {
                        statusOf(it) == IncursionStore.Status.WATCHED
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CurioIcon(
                            name = IncursionDestination.of(studio).glyph,
                            contentDescription = null,
                            tint = accent,
                            size = 15.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            studio.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "$lineWatched/$lineTotal",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = accent
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Bar(
                        fraction = if (lineTotal == 0) 0f else lineWatched.toFloat() / lineTotal,
                        accent = accent
                    )
                }
            }
        }

        // ── 3 · What is next ────────────────────────────────────────────────
        item(key = "personal-next") {
            Card(shape, accent, dark) {
                Heading("NEXT UP", accent)
                Spacer(Modifier.height(4.dp))
                if (nextUp.isEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Nothing left in any line. The order is finished.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    nextUp.forEach { (studio, entry) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onOpen(entry) }
                                .padding(vertical = 7.dp)
                        ) {
                            IncursionPosterPlate(
                                entry = entry,
                                modifier = Modifier.width(38.dp).height(54.dp),
                                shape = RoundedCornerShape(7.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    entry.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "${studio.name} · #${entry.orderLabel}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (entry.essential) {
                                Spacer(Modifier.width(6.dp))
                                CurioIcon(
                                    name = CurioIcons.Star,
                                    contentDescription = "Essential",
                                    tint = accent,
                                    size = 13.dp
                                )
                            }
                            Spacer(Modifier.width(4.dp))
                            CurioIcon(
                                name = CurioIcons.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                size = 16.dp
                            )
                        }
                    }
                }
            }
        }

        // ── 4 · What have I decided ─────────────────────────────────────────
        item(key = "personal-states") {
            Card(shape, accent, dark) {
                Heading("WHERE THINGS STAND", accent)
                Spacer(Modifier.height(10.dp))
                states.chunked(2).forEachIndexed { lineIndex, line ->
                    if (lineIndex > 0) Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        line.forEach { (state, count) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .padding(horizontal = 9.dp, vertical = 7.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .height(9.dp)
                                        .width(9.dp)
                                        .clip(CircleShape)
                                        .background(incursionStatusInk(state))
                                )
                                Spacer(Modifier.width(7.dp))
                                Text(
                                    state.shortLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    "$count",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        if (line.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        // ── 5 · The figures ────────────────────────────────────────────────
        item(key = "personal-figures") {
            Card(shape, accent, dark) {
                Heading("THE FIGURES", accent)
                Spacer(Modifier.height(10.dp))
                Figure("Hours watched", hoursText(minutesIn), accent)
                Figure("Hours left", hoursText((minutes - minutesIn).coerceAtLeast(0)), accent)
                Figure(
                    "Essentials",
                    "$essentialsDone of ${essentials.size}",
                    accent
                )
                Figure(
                    "Longest run",
                    bestRun?.let { (studio, run) -> "$run in a row · ${studio.name}" }
                        ?: "—",
                    accent,
                    last = true
                )
            }
        }

        item(key = "personal-note") {
            Text(
                // A quiet line under the desk, because a page this full of figures
                // should say what they are figures OF.
                "Every figure here is a count of the rows you have marked, in the order " +
                    "upstream keeps. Nothing on this page changes a status.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp)
            )
        }
    }
}

// ── The desk's own furniture ─────────────────────────────────────────────────

/** One card of the desk — the app's own plate: fill, radius, soft shadow, no edge. */
@Composable
private fun Card(shape: RoundedCornerShape, accent: Color, dark: Boolean, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .curioCardShadow(shape)
            .clip(shape)
            .background(
                curioTintOn(
                    MaterialTheme.colorScheme.surfaceContainerLow,
                    accent,
                    if (dark) 0.10f else 0.06f
                )
            )
            .padding(horizontal = 14.dp, vertical = 13.dp)
    ) {
        content()
    }
}

@Composable
private fun Heading(text: String, accent: Color) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.3.sp,
        color = accent
    )
}

@Composable
private fun Bar(fraction: Float, accent: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(5.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(accent)
        )
    }
}

@Composable
private fun Figure(label: String, value: String, accent: Color, last: Boolean = false) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.ExtraBold,
            color = accent,
            textAlign = TextAlign.End
        )
    }
    if (!last) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
        )
    }
}

// ── The figures, worked out ──────────────────────────────────────────────────

/**
 * How long a row IS, in minutes — the count the hours figures are built from.
 *
 * A film is its own runtime. A series row is its runtime PER EPISODE times how
 * many episodes the row covers: the stated count where upstream gives one, and
 * otherwise the range it names ("S1 Eps 8–12" is five), because a season is not
 * an episode and a row that says nothing about its length must not be counted as
 * an hour. A row with no runtime at all counts for nothing rather than for a
 * guess.
 */
private fun minutesOf(entry: IncursionEntry): Int {
    val per = entry.runtime ?: 0
    if (per <= 0) return 0
    val episodes = if (entry.type.lowercase() == "series") episodesOf(entry) else 1
    return per * episodes
}

private fun episodesOf(entry: IncursionEntry): Int {
    val stated = entry.episodes ?: 0
    if (stated > 0) return stated
    val start = entry.epStart ?: 0
    val end = entry.epEnd ?: 0
    if (start > 0 && end >= start) return end - start + 1
    return 1
}

/** The longest stretch of consecutive WATCHED rows in one line's own order. */
private fun longestRun(
    ordered: List<IncursionEntry>,
    statusOf: (IncursionEntry) -> IncursionStore.Status
): Int {
    var best = 0
    var run = 0
    ordered.forEach { entry ->
        if (statusOf(entry) == IncursionStore.Status.WATCHED) {
            run += 1
            if (run > best) best = run
        } else {
            run = 0
        }
    }
    return best
}

/** "12h 40m" / "48m" / "0m" — a figure, not a paragraph. */
private fun hoursText(minutes: Int): String {
    val safe = minutes.coerceAtLeast(0)
    val hours = safe / 60
    val rest = safe % 60
    return if (hours > 0) "${hours}h ${rest}m" else "${rest}m"
}

private fun percent(part: Int, whole: Int): Int =
    if (whole <= 0) 0 else (part * 100f / whole).toInt()
