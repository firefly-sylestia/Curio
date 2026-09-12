package com.curio.app.features.community

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.curio.app.data.supabase.CommunityCard
import com.curio.app.data.supabase.CurioDirectMessage
import com.curio.app.data.supabase.KIND_QUOTE
import com.curio.app.data.supabase.CurioDmThread
import com.curio.app.data.supabase.CurioPerson
import com.curio.app.data.supabase.KIND_CARD
import com.curio.app.data.supabase.SOCIAL_CACHE_PREFS
import com.curio.app.ui.theme.CurioDialogShape
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.components.curioPressClickable
import com.curio.app.ui.theme.CurioMotion
import com.curio.app.ui.theme.curioDialogActionButtonColors
import com.curio.app.ui.theme.curioDialogActionColor
import com.curio.app.ui.theme.curioDialogContainerColor
import com.curio.app.ui.theme.isCurioDarkTheme
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * CURIO'S SOCIAL LANGUAGE — the shared kit the wall, Friends, a profile and a
 * conversation are all built from.
 *
 * The rules it encodes, so every social surface agrees without repeating
 * itself:
 *
 *  - **Box cards, not bare rows.** A person, a conversation or an empty state
 *    is a rounded 22dp card with a title line and its own actions, the way the
 *    rest of Curio groups things. Nothing floats untethered on the backdrop.
 *  - **Pills, not text buttons.** An action is a pill with a glyph and a
 *    label (`SocialPill`), so what a tap will do is legible before it is
 *    tapped and the hit target is a real one.
 *  - **Accent means "this is the one".** Exactly one accent-filled pill per
 *    card — the primary move. Everything else is calm surface, and anything
 *    destructive wears the error container rather than the accent.
 */

/** How a [SocialPill] is painted — the only three tones a social action has. */
enum class SocialPillTone { ACCENT, NEUTRAL, DESTRUCTIVE }

/**
 * One pill action: optional glyph + label, filled by [tone].
 *
 * A disabled pill stays in place and dims rather than vanishing, so a row's
 * shape never jumps between states (the old `TextButton`s appeared and
 * disappeared, which is what made "Add" feel like it did nothing).
 */
@Composable
internal fun SocialPill(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: String? = null,
    tone: SocialPillTone = SocialPillTone.NEUTRAL,
    enabled: Boolean = true
) {
    val dark = isCurioDarkTheme()
    val base = when (tone) {
        SocialPillTone.ACCENT -> curioDialogActionColor()
        SocialPillTone.DESTRUCTIVE -> MaterialTheme.colorScheme.errorContainer
        SocialPillTone.NEUTRAL -> if (dark) {
            MaterialTheme.colorScheme.surfaceContainerHighest
        } else {
            Color.White.copy(alpha = 0.86f)
        }
    }
    val ink = when (tone) {
        SocialPillTone.ACCENT -> Color.White
        SocialPillTone.DESTRUCTIVE -> MaterialTheme.colorScheme.error
        SocialPillTone.NEUTRAL -> MaterialTheme.colorScheme.onSurface
    }
    val fill by animateColorAsState(
        targetValue = if (enabled) base else base.copy(alpha = 0.45f),
        animationSpec = tween(CurioMotion.Durations.Quick),
        label = "socialPillFill"
    )
    Surface(
        onClick = { if (enabled) onClick() },
        shape = RoundedCornerShape(50),
        color = fill,
        contentColor = ink,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
        ) {
            if (icon != null) {
                CurioIcon(
                    name = icon,
                    contentDescription = null,
                    tint = ink,
                    size = 15.dp
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = ink,
                maxLines = 1
            )
        }
    }
}

/**
 * The social card frame — Curio's raised box: 22dp corners, a frosted fill, a
 * hairline edge and a soft lift in dark mode so the box reads as a surface on
 * the pitch-black theme. Every social surface is one of these.
 */
@Composable
internal fun SocialCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val dark = isCurioDarkTheme()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                if (dark) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f)
                else Color.White.copy(alpha = 0.72f)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.10f),
                shape = RoundedCornerShape(22.dp)
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}

/** A card's own heading: glyph plate + title + one honest line under it. */
@Composable
internal fun SocialCardHeading(
    icon: String,
    title: String,
    subtitle: String? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            CurioIcon(
                name = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                size = 17.dp
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * What one account is to the reader right now.
 *
 * This is the fix for "adding someone I am already friends with": the pill a
 * row offers is decided by the RELATIONSHIP, not by which list happened to
 * render it, so "Add friend" can never be offered for someone who is already
 * a friend, already asked, or is you.
 */
internal enum class SocialRelation { NONE, OUTGOING, INCOMING, FRIEND, SELF }

/** A round, icon-only action — for the one secondary move in a compact row. */
@Composable
internal fun SocialIconPill(
    icon: String,
    contentDescription: String,
    onClick: () -> Unit,
    tone: SocialPillTone = SocialPillTone.NEUTRAL
) {
    val dark = isCurioDarkTheme()
    val fill = when (tone) {
        SocialPillTone.ACCENT -> curioDialogActionColor()
        SocialPillTone.DESTRUCTIVE -> MaterialTheme.colorScheme.errorContainer
        SocialPillTone.NEUTRAL -> if (dark) {
            MaterialTheme.colorScheme.surfaceContainerHighest
        } else {
            Color.White.copy(alpha = 0.86f)
        }
    }
    val ink = when (tone) {
        SocialPillTone.ACCENT -> Color.White
        SocialPillTone.DESTRUCTIVE -> MaterialTheme.colorScheme.error
        SocialPillTone.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(onClick = onClick, shape = CircleShape, color = fill, contentColor = ink) {
        Box(modifier = Modifier.size(38.dp), contentAlignment = Alignment.Center) {
            CurioIcon(
                name = icon,
                contentDescription = contentDescription,
                tint = ink,
                size = 17.dp
            )
        }
    }
}

/**
 * ONE PERSON, as a box card: portrait, name, live @username and the pills that
 * make sense for where the relationship actually stands.
 *
 * Every social list renders people through this, which is why a name, a
 * portrait and a set of actions look and behave the same on the wall, in
 * Friends and in search results.
 */
@Composable
internal fun SocialPersonCard(
    person: CurioPerson,
    relation: SocialRelation,
    onOpenProfile: () -> Unit,
    onAdd: () -> Unit,
    onMessage: () -> Unit,
    onRemove: () -> Unit,
    onAccept: () -> Unit = {},
    onDecline: () -> Unit = {}
) {
    SocialCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenProfile)
        ) {
            SocialAvatar(style = person.avatarStyle, avatarSize = 46.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = person.label,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = "@${person.handle}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            CurioIcon(
                name = CurioIcons.ChevronRight,
                contentDescription = "Open ${person.label}'s profile",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                size = 18.dp
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            when (relation) {
                SocialRelation.FRIEND -> {
                    SocialPill(
                        label = "Message",
                        icon = CurioIcons.Notes,
                        tone = SocialPillTone.ACCENT,
                        onClick = onMessage
                    )
                    Spacer(Modifier.weight(1f))
                    SocialIconPill(
                        icon = CurioIcons.Delete,
                        contentDescription = "Remove ${person.label}",
                        tone = SocialPillTone.DESTRUCTIVE,
                        onClick = onRemove
                    )
                }
                SocialRelation.INCOMING -> {
                    SocialPill(
                        label = "Accept",
                        icon = CurioIcons.Check,
                        tone = SocialPillTone.ACCENT,
                        onClick = onAccept
                    )
                    SocialPill(
                        label = "Decline",
                        icon = CurioIcons.Close,
                        onClick = onDecline
                    )
                }
                SocialRelation.OUTGOING -> {
                    SocialPill(
                        label = "Requested",
                        icon = CurioIcons.Check,
                        tone = SocialPillTone.NEUTRAL,
                        onClick = onAdd,
                        enabled = false
                    )
                    Spacer(Modifier.weight(1f))
                    SocialIconPill(
                        icon = CurioIcons.Close,
                        contentDescription = "Cancel the request to ${person.label}",
                        onClick = onRemove
                    )
                }
                SocialRelation.SELF -> {
                    SocialPill(
                        label = "This is you",
                        icon = CurioIcons.Person,
                        onClick = onAdd,
                        enabled = false
                    )
                }
                SocialRelation.NONE -> {
                    // ICON, not a word: adding someone is the one move this row
                    // exists for, and the accent disc says it louder than a
                    // label ever did (the content description keeps it
                    // readable to a screen reader).
                    SocialIconPill(
                        icon = CurioIcons.Add,
                        contentDescription = "Add ${person.label} as a friend",
                        tone = SocialPillTone.ACCENT,
                        onClick = onAdd
                    )
                }
            }
        }
    }
}

/**
 * A TEXT POST — the body of a NOTE or a QUOTE.
 *
 * A topic card is art plus words; a note is WORDS, and a quote is words plus
 * who said them. Drawing those through the share-card renderer would put a
 * topic they are not about behind them, so they get this instead: the app's
 * own text surface, sized by what was written, with the credit on its own
 * line for a quote.
 */
@Composable
internal fun SocialTextPost(
    card: CommunityCard,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
        modifier = modifier
            .fillMaxWidth()
            .curioPressClickable(pressedScale = 0.99f, onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = card.factText,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (card.kind == KIND_QUOTE && card.byline.isNotBlank()) {
                Text(
                    text = "— ${card.byline}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * ONE CONVERSATION, as a box card: who, the last line, when, and a count when
 * something is unread. The card is the tap target, so the whole surface opens
 * the thread rather than a word in the corner.
 */
@Composable
internal fun SocialThreadCard(
    thread: CurioDmThread,
    onOpen: () -> Unit
) {
    val unread = thread.unread > 0
    SocialCard(modifier = Modifier.clickable(onClick = onOpen)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            SocialAvatar(style = thread.person.avatarStyle, avatarSize = 48.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = thread.person.label,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = if (unread) FontWeight.Bold else FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(8.dp))
                    if (thread.lastAtMillis > 0L) {
                        Text(
                            text = socialStamp(thread.lastAtMillis),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = thread.preview,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (unread) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            if (unread) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(curioDialogActionColor()),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (thread.unread > 9) "9+" else thread.unread.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * THE SOCIAL CONFIRMATION — one dialog for every irreversible action in the
 * social layer (removing a friend, taking down a card, deleting a reply,
 * signing out).
 *
 * It exists because the destructive action used to be a single tap on a pill:
 * a remove button that fires on touch is a remove button that eventually fires
 * by accident. The dialog names the actual person or thing in [body] so nobody
 * confirms the wrong thing.
 */
@Composable
internal fun SocialConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    destructive: Boolean = true,
    busy: Boolean = false
) {
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        containerColor = curioDialogContainerColor(),
        shape = CurioDialogShape,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (!busy) onConfirm() },
                colors = if (destructive) {
                    androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                } else {
                    curioDialogActionButtonColors()
                }
            ) {
                Text(
                    text = if (busy) "Working…" else confirmLabel,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, colors = curioDialogActionButtonColors()) {
                Text("Cancel", style = MaterialTheme.typography.labelLarge)
            }
        }
    )
}

/** An empty surface that says what the screen is FOR, not just that it is empty. */
@Composable
internal fun SocialEmptyCard(
    icon: String,
    title: String,
    body: String,
    action: (@Composable () -> Unit)? = null
) {
    SocialCard {
        SocialCardHeading(icon = icon, title = title, subtitle = body)
        action?.invoke()
    }
}

/**
 * The conversation's date rule — "Today", "Yesterday", or a written date.
 * A chat needs the seam between days or a long thread reads as one sitting.
 */
@Composable
internal fun SocialDayDivider(label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f))
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f))
        )
    }
}

/**
 * `14:32` for today, `Aug 12` for anything older — one short line that never
 * wraps and never pushes the content it sits beside.
 */
internal fun socialStamp(millis: Long): String {
    if (millis <= 0L) return ""
    val zone = ZoneId.systemDefault()
    val time = Instant.ofEpochMilli(millis).atZone(zone)
    return if (time.toLocalDate() == LocalDate.now(zone)) {
        time.format(DateTimeFormatter.ofPattern("HH:mm"))
    } else {
        time.format(DateTimeFormatter.ofPattern("MMM d"))
    }
}

/** One line of feedback under a section — [isError] picks the ink. */
@Composable
internal fun SocialNote(message: String, isError: Boolean) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = if (isError) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/**
 * The social search field — the same frosted pill as the message composer, so
 * finding a person looks like writing to one. It reports its own busy state in
 * place of a hint line, which keeps the field one line tall.
 */
@Composable
internal fun SocialSearchField(
    value: String,
    placeholder: String,
    busy: Boolean,
    onValueChange: (String) -> Unit
) {
    val dark = isCurioDarkTheme()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(
                if (dark) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.75f)
                else Color.White.copy(alpha = 0.85f)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f),
                shape = RoundedCornerShape(26.dp)
            )
            .padding(horizontal = 16.dp)
    ) {
        CurioIcon(
            name = CurioIcons.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            size = 18.dp
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            cursorBrush = SolidColor(curioDialogActionColor()),
            modifier = Modifier.weight(1f)
        ) { inner ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                inner()
            }
        }
        if (busy) {
            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
        } else if (value.isNotEmpty()) {
            CurioIcon(
                name = CurioIcons.Close,
                contentDescription = "Clear the search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                size = 17.dp,
                modifier = Modifier.clickable { onValueChange("") }
            )
        }
    }
}

/** `Today` / `Yesterday` / `Aug 12` — the day a message belongs to. */
internal fun socialDayLabel(millis: Long): String {
    if (millis <= 0L) return ""
    val zone = ZoneId.systemDefault()
    val day = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
    val today = LocalDate.now(zone)
    return when (day) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> day.format(DateTimeFormatter.ofPattern("MMMM d"))
    }
}

/**
 * Curio's reaction palette.
 *
 * A reaction IS an emoji — the character is what the server stores, so a
 * reaction costs a few bytes, renders the same on every device and needs no
 * image of its own. [LEGACY] maps the icon NAMES the first release wrote, so a
 * conversation that already carries reactions keeps showing them instead of
 * going blank after this change.
 */
internal object SocialReactions {
    /** (emoji, what it means) — the order is also the picker's order. */
    val PALETTE: List<Pair<String, String>> = listOf(
        "❤️" to "Love",
        "😂" to "Haha",
        "😮" to "Wow",
        "😢" to "Sad",
        "👍" to "Like",
        "🙏" to "Thanks"
    )

    /** Icon names written by the previous build → the emoji they mean now. */
    private val LEGACY = mapOf(
        "ThumbUp" to "👍",
        "LocalFire" to "🔥",
        "MoodHappy" to "😂",
        "AutoAwesome" to "✨",
        "Star" to "⭐",
        "Lightbulb" to "💡"
    )

    /** What a legacy icon name meant, for its content description. */
    private val LEGACY_LABELS = mapOf(
        "ThumbUp" to "Like",
        "LocalFire" to "On fire",
        "MoodHappy" to "Haha",
        "AutoAwesome" to "Sparkle",
        "Star" to "Starred",
        "Lightbulb" to "Insightful"
    )

    /** The emoji for a stored reaction — a calm fallback when it is empty. */
    fun emojiFor(kind: String): String {
        val trimmed = kind.trim()
        if (trimmed.isEmpty()) return PALETTE.first().first
        if (PALETTE.any { it.first == trimmed }) return trimmed
        return LEGACY[trimmed] ?: trimmed
    }

    /** What a reaction means, for a content description. */
    fun labelFor(kind: String): String {
        val trimmed = kind.trim()
        return PALETTE.firstOrNull { it.first == trimmed }?.second
            ?: LEGACY_LABELS[trimmed]
            ?: "Reaction"
    }
}

/**
 * THE ON-DEVICE MESSAGE CACHE.
 *
 * Why it exists: a thread used to be empty until the network answered, so
 * opening a conversation you had just had showed a blank screen, and losing
 * signal lost the conversation. This keeps the last [CAP] messages of each
 * conversation in the app's own storage, so a thread renders instantly from
 * the device and the network only confirms or extends it.
 *
 * It is a CACHE, not a store of record: the server owns the messages, a clear
 * forgets everything, and writing the same conversation again replaces its
 * copy (no growth, no duplicates).
 *
 * Only text is ever kept — the same rule as the online layer itself.
 */
internal object SocialMessageCache {
    private const val CAP = 200
    private const val VERSION_KEY = "cache_version"

    /** Bump when the stored shape changes, so stale blobs are dropped once. */
    private const val VERSION = 2

    private fun prefs(context: Context) = socialCachePrefs(context)

    private fun key(otherUserId: String) = "thread_$otherUserId"

    /**
     * The cached conversation with [otherUserId], oldest first. [myUserId] is
     * only used to recompute who said what — never stored, so switching
     * accounts can never mislabel a bubble as "mine".
     */
    fun read(
        context: Context,
        otherUserId: String,
        myUserId: String
    ): List<CurioDirectMessage> {
        if (otherUserId.isBlank()) return emptyList()
        val cooked = runCatching { decode(context, otherUserId, myUserId) }
        return cooked.getOrDefault(emptyList())
    }

    private fun decode(
        context: Context,
        otherUserId: String,
        myUserId: String
    ): List<CurioDirectMessage> {
        val raw = prefs(context).getString(key(otherUserId), null) ?: return emptyList()
        val array = JSONArray(raw)
        val out = ArrayList<CurioDirectMessage>(array.length())
        for (index in 0 until array.length()) {
            val row = array.optJSONObject(index) ?: continue
            val body = row.optString("b")
            if (body.isBlank()) continue
            val senderId = row.optString("s")
            out += CurioDirectMessage(
                id = row.optString("i"),
                senderId = senderId,
                body = body,
                createdAtMillis = row.optLong("t"),
                readAtMillis = row.optLong("r").takeIf { it > 0L },
                mine = senderId == myUserId
            )
        }
        return out
    }

    /** Replaces the stored copy of one conversation (best-effort, silent). */
    fun write(
        context: Context,
        otherUserId: String,
        messages: List<CurioDirectMessage>
    ) {
        if (otherUserId.isBlank()) return
        runCatching {
            val kept = messages.takeLast(CAP)
            val array = JSONArray()
            kept.forEach { message ->
                array.put(
                    JSONObject()
                        .put("i", message.id)
                        .put("s", message.senderId)
                        .put("b", message.body)
                        .put("t", message.createdAtMillis)
                        .put("r", message.readAtMillis ?: 0L)
                )
            }
            prefs(context)
                .edit()
                .putString(key(otherUserId), array.toString())
                .putInt(VERSION_KEY, VERSION)
                .apply()
        }
    }

    /**
     * Drops everything when the stored shape is from an older build. Called
     * once per screen entry — it is a single integer read.
     */
    fun migrateIfNeeded(context: Context) {
        val store = prefs(context)
        if (store.getInt(VERSION_KEY, VERSION) == VERSION) return
        runCatching { store.edit().clear().putInt(VERSION_KEY, VERSION).apply() }
    }

    /** Forgets every cached conversation (used when signing out). */
    fun clear(context: Context) {
        runCatching { prefs(context).edit().clear().apply() }
    }
}

/**
 * THE ON-DEVICE PEOPLE CACHE.
 *
 * Why it exists: a name is the FIRST thing a social surface draws, and every
 * surface used to ask the server for it after composing — so a friend's name
 * landed a beat late and the header wore a placeholder in the meantime. This
 * remembers the last resolved identity of each account (display name,
 * @username, portrait) so a row, a conversation header or a profile can draw
 * the real person on the first frame, and the network only ever REFINES it.
 *
 * It holds identity and nothing else — no message, no card, no email — and it
 * is cleared with the message cache when the account signs out.
 */
/**
 * The ONE prefs file the social caches share — messages and remembered
 * people. Signing out clears the file, so both are forgotten together.
 */
private fun socialCachePrefs(context: Context) =
    context.applicationContext
        .getSharedPreferences(SOCIAL_CACHE_PREFS, Context.MODE_PRIVATE)

internal object SocialPeopleCache {
    private const val KEY_PREFIX = "person_"
    private const val ORDER_KEY = "person_order"

    /** How many accounts are remembered; beyond this the oldest are dropped. */
    private const val CAP = 300

    fun read(context: Context, userId: String): CurioPerson? {
        if (userId.isBlank()) return null
        return runCatching {
            val raw = socialCachePrefs(context).getString(KEY_PREFIX + userId, null) ?: return null
            val row = JSONObject(raw)
            CurioPerson(
                userId = userId,
                displayName = row.optString("n"),
                username = row.optString("u"),
                avatarStyle = row.optInt("a", 0).coerceIn(0, 15)
            )
        }.getOrNull()
    }

    /** Remembers one identity (best-effort — a cache write never fails a screen). */
    fun remember(context: Context, person: CurioPerson) {
        if (person.userId.isBlank()) return
        runCatching {
            val store = socialCachePrefs(context)
            val row = JSONObject()
                .put("n", person.displayName)
                .put("u", person.username)
                .put("a", person.avatarStyle)
            val order = order(store).filterNot { it == person.userId } + person.userId
            val kept = order.takeLast(CAP)
            val editor = store.edit().putString(KEY_PREFIX + person.userId, row.toString())
            (order - kept.toSet()).forEach { editor.remove(KEY_PREFIX + it) }
            editor.putString(ORDER_KEY, JSONArray(kept).toString())
            editor.apply()
        }
    }

    /** Remembers a whole page of identities in one write. */
    fun remember(context: Context, people: Collection<CurioPerson>) {
        people.forEach { remember(context, it) }
    }

    private fun order(store: android.content.SharedPreferences): List<String> {
        // `return@runCatching` (not `return`): a non-local return out of an
        // expression body is not legal Kotlin.
        return runCatching {
            val raw = store.getString(ORDER_KEY, null) ?: return@runCatching emptyList()
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                array.optString(index).takeIf { it.isNotBlank() }
            }
        }.getOrDefault(emptyList())
    }

    /** Forgets every remembered identity (used when signing out). */
    fun clear(context: Context) {
        runCatching {
            val store = socialCachePrefs(context)
            val editor = store.edit()
            order(store).forEach { editor.remove(KEY_PREFIX + it) }
            editor.remove(ORDER_KEY).apply()
        }
    }
}

/**
 * THE ON-DEVICE WALL CACHE.
 *
 * Why it exists: the community page was BLANK whenever the first request
 * failed — open the wall on a train and it said nothing at all. This keeps the
 * last page of cards the device actually saw, so opening Community offline
 * shows the 24-hour wall it last loaded (with the age of that copy stated)
 * instead of an empty screen, and the network replaces it the moment it
 * answers.
 *
 * Text only, like everything else in the social layer, and it is a cache: the
 * server owns the cards, `expires_at` still decides what is alive, and an
 * expired copy is dropped on read rather than shown past its 24 hours.
 */
internal object SocialFeedCache {
    private const val KEY = "feed_cards"
    private const val KEY_AT = "feed_at"
    private const val CAP = 40

    /** The last page of cards, minus anything that has already expired. */
    fun read(context: Context): List<CommunityCard> {
        return runCatching {
            val raw = socialCachePrefs(context).getString(KEY, null)
                ?: return@runCatching emptyList()
            val array = JSONArray(raw)
            val now = System.currentTimeMillis()
            buildList(array.length()) {
                for (index in 0 until array.length()) {
                    val row = array.optJSONObject(index) ?: continue
                    val card = fromJson(row)
                    // A cached card is only shown while the server would still
                    // serve it: the 24-hour promise holds offline too.
                    if (card.expiresAtMillis > now) add(card)
                }
            }
        }.getOrDefault(emptyList())
    }

    /** When the cached page was written (0 when there is nothing cached). */
    fun cachedAt(context: Context): Long =
        runCatching { socialCachePrefs(context).getLong(KEY_AT, 0L) }.getOrDefault(0L)

    fun write(context: Context, cards: List<CommunityCard>) {
        runCatching {
            val array = JSONArray()
            cards.take(CAP).forEach { array.put(toJson(it)) }
            socialCachePrefs(context)
                .edit()
                .putString(KEY, array.toString())
                .putLong(KEY_AT, System.currentTimeMillis())
                .apply()
        }
    }

    private fun toJson(card: CommunityCard): JSONObject = JSONObject()
        .put("id", card.id)
        .put("author", card.authorId)
        .put("handle", card.authorHandle)
        .put("name", card.authorName)
        .put("avatar", card.authorAvatar)
        .put("kind", card.kind)
        .put("topic", card.topicName)
        .put("cat", card.categoryName)
        .put("glyph", card.categoryGlyph)
        .put("accent", card.accentHex)
        .put("fact", card.factText)
        .put("caption", card.caption)
        .put("style", card.style)
        .put("aspect", card.aspect)
        .put("scale", card.bodyScale.toDouble())
        .put("byline", card.byline)
        .put("created", card.createdAtMillis)
        .put("expires", card.expiresAtMillis)
        .put("likes", card.likeCount)
        .put("liked", card.likedByMe)
        .put("comments", card.commentCount)
        .put("mine", card.mine)

    private fun fromJson(row: JSONObject): CommunityCard = CommunityCard(
        id = row.optString("id"),
        authorId = row.optString("author"),
        authorHandle = row.optString("handle"),
        authorName = row.optString("name"),
        authorAvatar = row.optInt("avatar", 0),
        kind = row.optString("kind").ifBlank { KIND_CARD },
        topicName = row.optString("topic"),
        categoryName = row.optString("cat"),
        categoryGlyph = row.optString("glyph"),
        accentHex = row.optString("accent"),
        factText = row.optString("fact"),
        caption = row.optString("caption"),
        style = row.optString("style"),
        aspect = row.optString("aspect"),
        bodyScale = row.optDouble("scale", 1.0).toFloat(),
        byline = row.optString("byline"),
        createdAtMillis = row.optLong("created"),
        expiresAtMillis = row.optLong("expires"),
        likeCount = row.optInt("likes"),
        likedByMe = row.optBoolean("liked"),
        commentCount = row.optInt("comments"),
        mine = row.optBoolean("mine")
    )
}
