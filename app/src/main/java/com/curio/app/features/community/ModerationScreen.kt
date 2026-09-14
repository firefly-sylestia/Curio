package com.curio.app.features.community

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.curio.app.data.supabase.CommunityAdminRow
import com.curio.app.data.supabase.CommunityApi
import com.curio.app.data.supabase.CommunityCard
import com.curio.app.data.supabase.CommunityComment
import com.curio.app.data.supabase.CommunityReport
import com.curio.app.data.supabase.CurioPerson
import com.curio.app.data.supabase.ModerationReasons
import com.curio.app.data.supabase.OnlineAccount
import com.curio.app.data.supabase.SocialApi
import com.curio.app.features.settings.SettingsHeroHeader
import com.curio.app.features.settings.SettingsHeroTotalHeight
import com.curio.app.features.settings.SettingsOptionCard
import com.curio.app.features.settings.SettingsOptionInfoRow
import com.curio.app.features.settings.SettingsOptionSwitchRow
import com.curio.app.features.settings.SettingsSectionHeading
import com.curio.app.features.settings.heroPageBackground
import com.curio.app.features.settings.settingsRoseAccent
import com.curio.app.ui.adaptive.isWide
import com.curio.app.ui.adaptive.wideContentEdgePadding
import com.curio.app.ui.adaptive.windowWidthSizeClass
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.curioDialogActionColor
import kotlinx.coroutines.launch

/**
 * MODERATION — the community's control room.
 *
 * Two halves, because they are the two jobs the team actually does:
 *
 *  - **The queue** — every report, whatever it names (a post, a reply or a
 *    member). Each row shows the reported thing itself, beside the reason the
 *    reporter gave, and offers exactly the actions the reader is allowed to
 *    take: remove the content, hide its author, or dismiss the report. Every
 *    decision records a reason.
 *  - **The team** — who is on it, what each of them may do (five independent
 *    permissions), and the owner, who cannot be removed or demoted by anyone.
 *
 * No action here is a bare tap: the destructive ones open a reason sheet, and
 * the DATABASE checks the permission again for every call — a hidden control is
 * never what keeps the community safe.
 */
@Composable
fun ModerationScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val account = OnlineAccount.state
    val wide = windowWidthSizeClass().isWide
    val token = account.session?.accessToken
    val myUserId = account.session?.userId

    var myRow by remember { mutableStateOf<CommunityAdminRow?>(null) }
    var reports by remember { mutableStateOf<List<CommunityReport>>(emptyList()) }
    var cardsById by remember { mutableStateOf<Map<String, CommunityCard>>(emptyMap()) }
    var repliesById by remember { mutableStateOf<Map<String, CommunityComment>>(emptyMap()) }
    var peopleById by remember { mutableStateOf<Map<String, CurioPerson>>(emptyMap()) }
    var team by remember { mutableStateOf<List<CommunityAdminRow>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    // Queue is the default face; the team half only exists for a manager.
    var showTeam by remember { mutableStateOf(false) }
    // Open reports lead; "everything" is one tap away.
    var openOnly by remember { mutableStateOf(true) }
    // The report + action a reason sheet is open for.
    var acting by remember { mutableStateOf<Pair<CommunityReport, String>?>(null) }
    // The admin whose permissions are being edited (null row = a new admin).
    var permissionTarget by remember { mutableStateOf<Pair<CurioPerson, CommunityAdminRow?>?>(null) }

    LaunchedEffect(Unit) { OnlineAccount.restore(context) }

    suspend fun load(active: String, me: String) {
        loading = true
        CommunityApi.myAdminRow(active, me).fold(
            onSuccess = { row ->
                myRow = row
                if (row == null) {
                    error = "This is the moderation team's screen."
                    loading = false
                    return@fold
                }
                CommunityApi.reported(active).fold(
                    onSuccess = { rows ->
                        reports = rows
                        error = null
                        val cardIds = rows.mapNotNull { it.cardId }
                        val commentIds = rows.mapNotNull { it.commentId }
                        val peopleIds = (rows.mapNotNull { it.targetUserId } + rows.map { it.reporterId })
                            .filter { it.isNotBlank() }
                            .distinct()
                        if (cardIds.isNotEmpty()) {
                            CommunityApi.cardsByIds(active, cardIds, me)
                                .onSuccess { list -> cardsById = list.associateBy { it.id } }
                        }
                        if (commentIds.isNotEmpty()) {
                            CommunityApi.commentsByIds(active, commentIds, me)
                                .onSuccess { list -> repliesById = list.associateBy { it.id } }
                        }
                        if (peopleIds.isNotEmpty()) {
                            SocialApi.people(active, peopleIds)
                                .onSuccess { map -> peopleById = peopleById + map }
                        }
                    },
                    onFailure = { error = it.message }
                )
                if (row.allows("admins")) {
                    CommunityApi.admins(active).fold(
                        onSuccess = { rows ->
                            team = rows
                            val ids = rows.map { it.userId }
                            if (ids.isNotEmpty()) {
                                SocialApi.people(active, ids).onSuccess { map ->
                                    peopleById = peopleById + map
                                }
                            }
                        },
                        onFailure = { /* the queue still works without the roster */ }
                    )
                }
            },
            onFailure = { error = it.message ?: "Couldn't verify moderation access." }
        )
        loading = false
    }

    LaunchedEffect(token, myUserId) {
        val active = token
        val me = myUserId
        if (active != null && me != null) load(active, me) else loading = false
    }

    val canPosts = myRow?.allows("posts") == true
    val canReplies = myRow?.allows("replies") == true
    val canBans = myRow?.allows("bans") == true
    val canAdmins = myRow?.allows("admins") == true

    fun applyReportAction(action: String, report: CommunityReport, reason: String?, note: String?) {
        val active = token ?: return
        busy = true
        scope.launch {
            CommunityApi.handleReport(active, report.id, action, reason, note).fold(
                onSuccess = {
                    notice = when (action) {
                        "remove_content" -> "Content removed."
                        "hide_author" -> "Member hidden."
                        "dismiss" -> "Report dismissed."
                        else -> "Report reopened."
                    }
                    acting = null
                    load(active, myUserId.orEmpty())
                },
                onFailure = { error = it.message }
            )
            busy = false
        }
    }

    val visible = reports.filter { if (openOnly) it.open else true }
    val openCount = reports.count { it.open }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                heroPageBackground(
                    lerp(MaterialTheme.colorScheme.background, settingsRoseAccent(), 0.10f)
                )
            )
    ) {
        LazyColumn(
            contentPadding = PaddingValues(
                start = wideContentEdgePadding(),
                end = wideContentEdgePadding(),
                top = SettingsHeroTotalHeight,
                bottom = 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (!account.signedIn || token == null) {
                item(key = "signed-out") {
                    SettingsOptionCard {
                        SettingsOptionInfoRow(
                            CurioIcons.Info,
                            "Sign in to moderate",
                            "The moderation queue is only readable by signed-in members of the team."
                        )
                    }
                }
            } else if (myRow == null && !loading) {
                item(key = "not-team") {
                    SettingsOptionCard {
                        SettingsOptionInfoRow(
                            CurioIcons.Lock,
                            "Not a moderator",
                            error ?: "This screen is for the community's moderation team."
                        )
                    }
                }
            } else {
                // ── The two halves ───────────────────────────────────────
                item(key = "tabs") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ModerationTabPill(
                            label = if (openCount > 0) "Queue · $openCount" else "Queue",
                            selected = !showTeam,
                            onClick = { showTeam = false }
                        )
                        if (canAdmins) {
                            ModerationTabPill(
                                label = "Team",
                                selected = showTeam,
                                onClick = { showTeam = true }
                            )
                        }
                        if (!showTeam) {
                            ModerationTabPill(
                                label = if (openOnly) "Open only" else "All reports",
                                selected = false,
                                onClick = { openOnly = !openOnly }
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        if (loading) {
                            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.width(16.dp))
                        }
                    }
                }

                if (!showTeam) {
                    item(key = "queue-heading") { SettingsSectionHeading("Reports") }

                    notice?.let { line ->
                        item(key = "notice") { SocialNote(line, false) }
                    }
                    error?.let { line ->
                        item(key = "error") { SocialNote(line, true) }
                    }

                    if (visible.isEmpty() && !loading) {
                        item(key = "queue-empty") {
                            SettingsOptionCard {
                                SettingsOptionInfoRow(
                                    CurioIcons.TaskAlt,
                                    if (openOnly) "Nothing waiting" else "No reports yet",
                                    if (openOnly) {
                                        "Every report has been handled. Switch to All to look back."
                                    } else {
                                        "Reports members file on posts, replies and members land here."
                                    }
                                )
                            }
                        }
                    }

                    items(visible, key = { it.id }) { report ->
                        ModerationReportRow(
                            report = report,
                            card = report.cardId?.let { cardsById[it] },
                            reply = report.commentId?.let { repliesById[it] },
                            person = report.targetUserId?.let { peopleById[it] },
                            reporter = peopleById[report.reporterId],
                            canRemove = when (report.targetKind) {
                                "card" -> canPosts
                                "comment" -> canReplies
                                else -> false
                            },
                            canHide = canBans,
                            busy = busy,
                            onRemove = { acting = report to "remove_content" },
                            onHide = { acting = report to "hide_author" },
                            onDismiss = { acting = report to "dismiss" },
                            onReopen = { applyReportAction("reopen", report, null, null) }
                        )
                    }
                } else {
                    item(key = "team-heading") { SettingsSectionHeading("The team") }
                    notice?.let { line ->
                        item(key = "notice") { SocialNote(line, false) }
                    }
                    error?.let { line ->
                        item(key = "error") { SocialNote(line, true) }
                    }

                    if (canAdmins) {
                        item(key = "team-add") {
                            ModerationAddAdminCard(
                                accessToken = token!!,
                                onFound = { person -> permissionTarget = person to null }
                            )
                        }
                    }

                    items(team, key = { it.userId }) { row ->
                        ModerationTeamRow(
                            row = row,
                            person = peopleById[row.userId],
                            canManage = canAdmins,
                            busy = busy,
                            onEdit = {
                                permissionTarget = (peopleById[row.userId] ?: CurioPerson(row.userId, "")) to row
                            },
                            onRemove = {
                                val active = token
                                if (active == null) return@ModerationTeamRow
                                busy = true
                                scope.launch {
                                    CommunityApi.removeAdmin(active, row.userId).fold(
                                        onSuccess = {
                                            notice = "Removed from the team."
                                            load(active, myUserId.orEmpty())
                                        },
                                        onFailure = { error = it.message }
                                    )
                                    busy = false
                                }
                            }
                        )
                    }
                }
            }
        }

        SettingsHeroHeader(
            title = "Moderation",
            subtitle = "Reports and the team",
            onBack = { navController.popBackStack() }
        )
    }

    // ── The reason sheet every decision goes through ─────────────────────
    acting?.let { (report, action) ->
        val author = when (report.targetKind) {
            "card" -> report.cardId?.let { cardsById[it]?.authorLabel }
            "comment" -> report.commentId?.let { repliesById[it]?.authorLabel }
            else -> report.targetUserId?.let { peopleById[it]?.label }
        }
        when (action) {
            "remove_content" -> ModerationReasonDialog(
                title = if (report.targetKind == "card") "Remove this post" else "Remove this reply",
                subtitle = "Removing it takes it off the wall for everyone. The author is not told why.",
                reasons = ModerationReasons.REMOVAL,
                confirmLabel = "Remove",
                busy = busy,
                onDismiss = { if (!busy) acting = null },
                onConfirm = { reason, note -> applyReportAction(action, report, reason, note) }
            )
            "hide_author" -> ModerationReasonDialog(
                title = "Hide ${author ?: "this member"}?",
                subtitle = "Their content disappears and they cannot post. Their account keeps working, " +
                    "and you can lift this at any time.",
                reasons = ModerationReasons.HIDE,
                confirmLabel = "Hide",
                busy = busy,
                onDismiss = { if (!busy) acting = null },
                onConfirm = { reason, note -> applyReportAction(action, report, reason, note) }
            )
            else -> ModerationReasonDialog(
                title = "Dismiss this report?",
                subtitle = "Nothing changes on the wall. A line is kept for the record.",
                reasons = listOf(
                    "No action needed",
                    "Not enough to act on",
                    "Already handled",
                    "Duplicate report",
                    "Something else"
                ),
                confirmLabel = "Dismiss",
                destructive = false,
                reasonRequired = false,
                busy = busy,
                onDismiss = { if (!busy) acting = null },
                onConfirm = { reason, note -> applyReportAction("dismiss", report, reason, note) }
            )
        }
    }

    // ── Permissions ──────────────────────────────────────────────────────
    permissionTarget?.let { (person, existing) ->
        ModerationPermissionsDialog(
            person = person,
            existing = existing,
            busy = busy,
            onDismiss = { if (!busy) permissionTarget = null },
            onSave = { posts, replies, reports_, admins, bans ->
                val active = token ?: return@ModerationPermissionsDialog
                busy = true
                scope.launch {
                    CommunityApi.setAdmin(
                        accessToken = active,
                        userId = person.userId,
                        role = existing?.role ?: "admin",
                        canDeletePosts = posts,
                        canDeleteReplies = replies,
                        canHandleReports = reports_,
                        canManageAdmins = admins,
                        canBanMembers = bans
                    ).fold(
                        onSuccess = {
                            notice = "${person.label} ${if (existing == null) "joined the team" else "was updated"}."
                            permissionTarget = null
                            load(active, myUserId.orEmpty())
                        },
                        onFailure = { error = it.message }
                    )
                    busy = false
                }
            }
        )
    }
}

/** One of the screen's two halves, or the queue's Open/All switch. */
@Composable
private fun ModerationTabPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val accent = curioDialogActionColor()
    val shape = RoundedCornerShape(50)
    Surface(
        shape = shape,
        color = if (selected) accent else MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.clip(shape).clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = if (selected) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

/**
 * One queue row: what was reported, why, by whom, and the three decisions.
 */
@Composable
private fun ModerationReportRow(
    report: CommunityReport,
    card: CommunityCard?,
    reply: CommunityComment?,
    person: CurioPerson?,
    reporter: CurioPerson?,
    canRemove: Boolean,
    canHide: Boolean,
    busy: Boolean,
    onRemove: () -> Unit,
    onHide: () -> Unit,
    onDismiss: () -> Unit,
    onReopen: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CurioIcon(
                    name = CurioIcons.Flag,
                    contentDescription = null,
                    tint = curioDialogActionColor(),
                    size = 15.dp
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = when (report.targetKind) {
                        "card" -> "Post reported"
                        "comment" -> "Reply reported"
                        else -> "Member reported"
                    },
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.weight(1f))
                if (!report.open) {
                    Text(
                        text = report.status.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            ModerationTargetPreview(card = card, reply = reply, person = person)

            Text(
                text = "Reason: ${report.reason}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            report.note?.let {
                Text(
                    text = "“$it”",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "by ${reporter?.label ?: "a member"} · ${agoLabel(report.createdAtMillis)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            report.resolution?.takeIf { !report.open }?.let {
                Text(
                    text = "Outcome: $it",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (report.open) {
                    if (canRemove) {
                        SocialPill(
                            label = "Remove",
                            icon = CurioIcons.Delete,
                            tone = SocialPillTone.DESTRUCTIVE,
                            enabled = !busy,
                            onClick = onRemove
                        )
                    }
                    if (canHide) {
                        SocialPill(
                            label = "Hide author",
                            icon = CurioIcons.VisibilityOff,
                            tone = SocialPillTone.DESTRUCTIVE,
                            enabled = !busy,
                            onClick = onHide
                        )
                    }
                    SocialPill(
                        label = "Dismiss",
                        icon = CurioIcons.Check,
                        enabled = !busy,
                        onClick = onDismiss
                    )
                } else {
                    SocialPill(
                        label = "Reopen",
                        icon = CurioIcons.Undo,
                        enabled = !busy,
                        onClick = onReopen
                    )
                }
            }
        }
    }
}

/** The reported thing itself — the words, never a placeholder. */
@Composable
private fun ModerationTargetPreview(
    card: CommunityCard?,
    reply: CommunityComment?,
    person: CurioPerson?
) {
    val shape = RoundedCornerShape(14.dp)
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
    ) {
        Column(
            modifier = Modifier.padding(11.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            when {
                card != null -> {
                    Text(
                        text = "Post · ${card.kind.lowercase().replaceFirstChar { it.uppercase() }}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (card.topicName.isNotBlank()) {
                        Text(
                            text = card.topicName,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    val words = card.factText.ifBlank { card.caption }
                    if (words.isNotBlank()) {
                        Text(
                            text = words,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "by ${card.authorLabel} ${card.authorHandleLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                reply != null -> {
                    Text(
                        text = "Reply",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = reply.body,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "by ${reply.authorLabel} · ${agoLabel(reply.createdAtMillis)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                person != null -> {
                    Text(
                        text = "Member",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = person.identityLabel,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                else -> Text(
                    text = "This content is no longer readable — it may already be gone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** One moderator, with the switches they hold. */
@Composable
private fun ModerationTeamRow(
    row: CommunityAdminRow,
    person: CurioPerson?,
    canManage: Boolean,
    busy: Boolean,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    val permissions = if (row.owner) {
        listOf("Everything")
    } else {
        buildList {
            if (row.canDeletePosts) add("Posts")
            if (row.canDeleteReplies) add("Replies")
            if (row.canHandleReports) add("Reports")
            if (row.canManageAdmins) add("Team")
            if (row.canBanMembers) add("Bans")
        }.ifEmpty { listOf("Nothing yet") }
    }
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CurioIcon(
                    name = if (row.owner) CurioIcons.Star else CurioIcons.Person,
                    contentDescription = null,
                    tint = if (row.owner) curioDialogActionColor() else MaterialTheme.colorScheme.onSurfaceVariant,
                    size = 16.dp
                )
                Spacer(Modifier.width(7.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = person?.label ?: "A member",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = person?.handleLabel ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = if (row.owner) "Owner" else "Moderator",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (row.owner) curioDialogActionColor() else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = permissions.joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (canManage && !row.owner) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SocialPill(
                        label = "Permissions",
                        icon = CurioIcons.Tune,
                        enabled = !busy,
                        onClick = onEdit
                    )
                    SocialPill(
                        label = "Remove",
                        icon = CurioIcons.Close,
                        tone = SocialPillTone.DESTRUCTIVE,
                        enabled = !busy,
                        onClick = onRemove
                    )
                }
            }
        }
    }
}

/** Find a member by @username, then hand them the switches. */
@Composable
private fun ModerationAddAdminCard(
    accessToken: String,
    onFound: (CurioPerson) -> Unit
) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var miss by remember { mutableStateOf<String?>(null) }

    SocialCard {
        SocialCardHeading(
            icon = CurioIcons.Add,
            title = "Add a moderator",
            subtitle = "Find them by @username, then choose what they may do."
        )
        SocialSearchField(
            value = query,
            placeholder = "@username",
            busy = searching,
            onValueChange = { query = it }
        )
        SocialPill(
            label = if (searching) "Looking…" else "Find member",
            icon = CurioIcons.Search,
            tone = SocialPillTone.ACCENT,
            enabled = query.isNotBlank() && !searching,
            onClick = {
                searching = true
                miss = null
                scope.launch {
                    SocialApi.findByUsername(accessToken, query).fold(
                        onSuccess = { found ->
                            if (found == null) miss = "No member with that @username." else onFound(found)
                        },
                        onFailure = { miss = it.message }
                    )
                    searching = false
                }
            }
        )
        miss?.let { SocialNote(it, true) }
    }
}

/** The five switches, and what each one means. */
@Composable
private fun ModerationPermissionsDialog(
    person: CurioPerson,
    existing: CommunityAdminRow?,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSave: (posts: Boolean, replies: Boolean, reports: Boolean, admins: Boolean, bans: Boolean) -> Unit
) {
    var posts by remember { mutableStateOf(existing?.canDeletePosts ?: true) }
    var replies by remember { mutableStateOf(existing?.canDeleteReplies ?: true) }
    var reports_ by remember { mutableStateOf(existing?.canHandleReports ?: true) }
    var admins by remember { mutableStateOf(existing?.canManageAdmins ?: false) }
    var bans by remember { mutableStateOf(existing?.canBanMembers ?: false) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        containerColor = com.curio.app.ui.theme.curioDialogContainerColor(),
        shape = com.curio.app.ui.theme.CurioDialogShape,
        title = {
            Text(
                text = if (existing == null) "Add ${person.label}?" else "Permissions for ${person.label}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SettingsOptionSwitchRow(
                    icon = CurioIcons.Delete,
                    title = "Remove posts",
                    subtitle = "Take a post off the wall",
                    checked = posts,
                    enabled = !busy,
                    onCheckedChange = { posts = it }
                )
                SettingsOptionSwitchRow(
                    icon = CurioIcons.Notes,
                    title = "Remove replies",
                    subtitle = "Take a reply off a post",
                    checked = replies,
                    enabled = !busy,
                    onCheckedChange = { replies = it }
                )
                SettingsOptionSwitchRow(
                    icon = CurioIcons.Flag,
                    title = "Handle reports",
                    subtitle = "Work the moderation queue",
                    checked = reports_,
                    enabled = !busy,
                    onCheckedChange = { reports_ = it }
                )
                SettingsOptionSwitchRow(
                    icon = CurioIcons.Tune,
                    title = "Manage the team",
                    subtitle = "Add, edit and remove moderators",
                    checked = admins,
                    enabled = !busy,
                    onCheckedChange = { admins = it }
                )
                SettingsOptionSwitchRow(
                    icon = CurioIcons.VisibilityOff,
                    title = "Hide members",
                    subtitle = "Hide a member's content and posting",
                    checked = bans,
                    enabled = !busy,
                    onCheckedChange = { bans = it }
                )
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = { onSave(posts, replies, reports_, admins, bans) },
                enabled = !busy,
                colors = com.curio.app.ui.theme.curioDialogActionButtonColors()
            ) {
                Text(
                    text = if (busy) "Saving…" else if (existing == null) "Add" else "Save",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(
                onClick = onDismiss,
                colors = com.curio.app.ui.theme.curioDialogActionButtonColors()
            ) {
                Text("Cancel", style = MaterialTheme.typography.labelLarge)
            }
        }
    )
}
