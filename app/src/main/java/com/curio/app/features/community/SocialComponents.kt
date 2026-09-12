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
import com.curio.app.data.supabase.CurioDirectMessage
import com.curio.app.data.supabase.CurioDmThread
import com.curio.app.data.supabase.CurioPerson
import com.curio.app.ui.theme.CurioDialogShape
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
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
                    SocialPill(
                        label = "Add friend",
                        icon = CurioIcons.Add,
                        tone = SocialPillTone.ACCENT,
                        onClick = onAdd
                    )
                }
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
 * Each entry is a NAME from the app's own icon set — reactions are never
 * uploaded images, so the server stores a short word and the renderer is a
 * glyph the app already ships.
 */
internal object SocialReactions {
    /** (icon name, what it means) — the order is also the picker's order. */
    val PALETTE: List<Pair<String, String>> = listOf(
        CurioIcons.ThumbUp to "Love it",
        CurioIcons.LocalFire to "On fire",
        CurioIcons.MoodHappy to "Haha",
        CurioIcons.AutoAwesome to "Sparkle",
        CurioIcons.Star to "Starred",
        CurioIcons.Lightbulb to "Insightful"
    )

    /** The glyph for a stored reaction name — a calm fallback if unknown. */
    fun iconFor(kind: String): String =
        PALETTE.firstOrNull { it.first == kind }?.first ?: CurioIcons.AutoAwesome

    /** What a reaction means, for a content description. */
    fun labelFor(kind: String): String =
        PALETTE.firstOrNull { it.first == kind }?.second ?: "Reaction"
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
    private const val NAME = "curio_social_cache"
    private const val CAP = 200
    private const val VERSION_KEY = "cache_version"

    /** Bump when the stored shape changes, so stale blobs are dropped once. */
    private const val VERSION = 2

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)

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
