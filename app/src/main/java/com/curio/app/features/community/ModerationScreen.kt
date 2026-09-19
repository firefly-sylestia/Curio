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
import androidx.compose.runtime.mutableIntStateOf
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
import com.curio.app.data.supabase.BAN_CONTENT
import com.curio.app.navigation.CurioRoutes
import com.curio.app.data.supabase.CommunityAdminRow
import com.curio.app.data.supabase.CommunityApi
import com.curio.app.data.supabase.CommunityBan
import com.curio.app.data.supabase.CommunityCard
import com.curio.app.data.supabase.CommunityComment
import com.curio.app.data.supabase.CommunityReport
import com.curio.app.data.supabase.CurioPerson
import com.curio.app.data.supabase.ModerationReasons
import com.curio.app.data.supabase.banTierBlurb
import com.curio.app.data.supabase.banTierLabel
import com.curio.app.data.supabase.communityMessage
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

    // Three faces now: the queue, the team (managers only) and the BAN LIST —
    // the ladder's own page, so a ban made from a profile or from the queue can
    // be found again, read, softened or lifted from one place.
    var tab by remember { mutableIntStateOf(0) }
    var bans by remember { mutableStateOf<List<CommunityBan>>(emptyList()) }
    var bansLoading by remember { mutableStateOf(false) }
    var bansLoaded by remember { mutableStateOf(false) }
    // The member whose ban sheet is open: id, the name to show, and the tier
    // already in force (blank when there is none).
    var banTarget by remember { mutableStateOf<Triple<String, String, String>?>(null) }
    var lifting by remember { mutableStateOf<CommunityBan?>(null) }
    // Open reports lead; "everything" is one tap away.
    var openOnly by remember { mutableStateOf(true) }
    // The report + action a reason sheet is open for.
    var acting by remember { mutableStateOf<Pair<CommunityReport, String>?>(null) }
    // The admin whose permissions are being edited (null row = a new admin).
    var permissionTarget by remember { mutableStateOf<Pair<CurioPerson, CommunityAdminRow?>?>(null) }

    LaunchedEffect(Unit) { OnlineAccount.restore(context) }
    var loaded by remember { mutableStateOf(false) }

    suspend fun load(active: String, me: String) {
        if (loaded) return@load
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
        loaded = true
    }

    LaunchedEffect(token, myUserId) {
        val active = token
        val me = myUserId
        if (active != null && me != null) load(active, me) else loading = false
    }

    /**
     * The ban list, read when the tab is first opened and after every change.
     * It is a separate read from the queue on purpose: a moderator looking at
     * reports does not need the whole ban list in memory, and a ban list read
     * must never slow the queue down.
     */
    fun loadBans() {
        val active = token ?: return
        bansLoading = true
        scope.launch {
            CommunityApi.bans(active).fold(
                onSuccess = { rows ->
                    bans = rows
                    bansLoaded = true
                    error = null
                },
                onFailure = { failure -> error = communityMessage(failure) }
            )
            bansLoading = false
        }
    }

    LaunchedEffect(tab, token) {
        if (tab == 2 && token != null && !bansLoaded) loadBans()
    }

    /** Bans at a tier (or re-bans at a new one), then refreshes the list. */
    fun applyBan(userId: String, kind: String, reason: String, hours: Int?) {
        val active = token ?: return
        busy = true
        scope.launch {
            CommunityApi.banMember(active, userId, kind, reason, hours).fold(
                onSuccess = {
                    notice = "Banned: ${banTierLabel(kind)}" +
                        if (hours == null) " until it is lifted." else " for a while."
                    banTarget = null
                    loadBans()
                    load(active, myUserId.orEmpty())
                },
                onFailure = { failure -> error = communityMessage(failure) }
            )
            busy = false
        }
    }

    /** Lifts whatever tier is in force. */
    fun applyLift(userId: String, note: String?) {
        val active = token ?: return
        busy = true
        scope.launch {
            CommunityApi.liftBan(active, userId, note).fold(
                onSuccess = {
                    notice = "Ban lifted."
                    lifting = null
                    banTarget = null
                    loadBans()
                    load(active, myUserId.orEmpty())
                },
                onFailure = { failure -> error = communityMessage(failure) }
            )
            busy = false
        }
    }

    val canPosts = myRow?.allows("posts") == true
    val canReplies = myRow?.allows("replies") == true
    val canBans = myRow?.allows("bans") == true
    val canAdmins = myRow?.allows("admins") == true
    val canForms = myRow?.allows("forms") == true

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
                            selected = tab == 0,
                            onClick = { tab = 0 }
                        )
                        if (canAdmins) {
                            ModerationTabPill(
                                label = "Team",
                                selected = tab == 1,
                                onClick = { tab = 1 }
                            )
                        }
                        if (canBans) {
                            val live = bans.count { it.active }
                            ModerationTabPill(
                                label = if (live > 0) "Bans · $live" else "Bans",
                                selected = tab == 2,
                                onClick = { tab = 2 }
                            )
                        }
                        if (canForms) {
                            ModerationTabPill(
                                label = "Forms",
                                selected = tab == 3,
                                onClick = { tab = 3 }
                            )
                        }
                        if (tab == 0) {
                            ModerationTabPill(
                                label = if (openOnly) "Open only" else "All reports",
                                selected = false,
                                onClick = { openOnly = !openOnly }
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        if (loading || bansLoading) {
                            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.width(16.dp))
                        }
                    }
                }

                if (tab == 0) {
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
                } else if (tab == 1) {
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
                    }                } else if (tab == 3) {
                    // ── The forms (v403) ───────────────────────────────────
                    // The team's own questionnaire: write it, test it as often
                    // as they like, publish it, then read the answers counted.
                    item(key = "forms") {
                        val active = token
                        if (active != null) ModerationFormsTab(accessToken = active)
                    }
                } else {

                    // ── The ban list ────────────────────────────────────────
                    // Every member carrying a ban stamp: the live ones first,
                    // then the bans that lapsed or were lifted, because "who is
                    // banned" and "who WAS banned" are the same question asked
                    // at two different times.
                    item(key = "bans-heading") { SettingsSectionHeading("Bans") }
                    notice?.let { line ->
                        item(key = "notice") { SocialNote(line, false) }
                    }
                    error?.let { line ->
                        item(key = "error") { SocialNote(line, true) }
                    }

                    if (bans.isEmpty() && !bansLoading) {
                        item(key = "bans-empty") {
                            SettingsOptionCard {
                                SettingsOptionInfoRow(
                                    CurioIcons.TaskAlt,
                                    "Nobody is banned",
                                    "A ban set from a member's profile lands here, with its tier, " +
                                        "its clock and the reason it was set for."
                                )
                            }
                        }
                    }

                    items(bans, key = { it.userId }) { ban ->
                        ModerationBanRow(
                            ban = ban,
                            busy = busy,
                            onOpen = {
                                navController.navigate(CurioRoutes.socialProfile(ban.userId)) {
                                    launchSingleTop = true
                                }
                            },
                            onChange = { banTarget = Triple(ban.userId, ban.label, ban.kind) },
                            onLift = { lifting = ban }
                        )
                    }
                }
            }
        }

        SettingsHeroHeader(
            title = "Moderation",
            subtitle = "Reports, bans and the team",
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
            // The queue's member action opens the LADDER, not a yes/no hide:
            // the content tier is the default it always was, and the three
            // harder tiers are one tap away instead of a trip to a profile.
            "hide_author" -> ModerationBanDialog(
                memberName = author ?: "this member",
                busy = busy,
                onDismiss = { if (!busy) acting = null },
                onConfirm = { kind, reason, hours ->
                    val active = token ?: return@ModerationBanDialog
                    val target = report.targetUserId
                        ?: report.cardId?.let { cardsById[it]?.authorId }
                        ?: report.commentId?.let { repliesById[it]?.authorId }
                    if (target.isNullOrBlank()) {
                        error = "That member is no longer readable."
                        acting = null
                        return@ModerationBanDialog
                    }
                    busy = true
                    scope.launch {
                        if (kind == BAN_CONTENT) {
                            // The queue's own action, unchanged: the content
                            // tier and the report close together, so the queue
                            // still records WHICH report the ban came from.
                            CommunityApi.handleReport(
                                active, report.id, "hide_author", reason, banNote(kind, hours)
                            ).fold(
                                onSuccess = {
                                    notice = "Banned: ${banTierLabel(kind)}."
                                    acting = null
                                    loadBans()
                                    load(active, myUserId.orEmpty())
                                },
                                onFailure = { error = communityMessage(it) }
                            )
                        } else {
                            CommunityApi.banMember(active, target, kind, reason, hours).fold(
                                onSuccess = {
                                    // The report is then closed behind the ban,
                                    // with the tier written into the note so the
                                    // queue does not look like it shrugged.
                                    CommunityApi.handleReport(
                                        active, report.id, "dismiss", reason, banNote(kind, hours)
                                    )
                                    notice = "Banned: ${banTierLabel(kind)}."
                                    acting = null
                                    loadBans()
                                    load(active, myUserId.orEmpty())
                                },
                                onFailure = { error = communityMessage(it) }
                            )
                        }
                        busy = false
                    }
                }
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

    // ── The ban sheet, and the way back out ──────────────────────────────
    banTarget?.let { (userId, name, currentKind) ->
        ModerationBanDialog(
            memberName = name,
            currentKind = currentKind,
            busy = busy,
            onDismiss = { if (!busy) banTarget = null },
            onConfirm = { kind, reason, hours -> applyBan(userId, kind, reason, hours) },
            // Only while a tier is actually in force: “lift the ban instead”
            // makes no sense on a member who is not banned.
            onLift = if (currentKind.isNotBlank()) {
                { lifting = bans.firstOrNull { it.userId == userId } }
            } else null
        )
    }

    lifting?.let { ban ->
        ModerationReasonDialog(
            title = "Lift the ban on ${ban.label}?",
            subtitle = "Nothing stays paused for them. The record of the ban remains in their " +
                "moderation history, which is what stops a lift from erasing what happened.",
            reasons = ModerationReasons.LIFT,
            confirmLabel = "Lift ban",
            destructive = false,
            busy = busy,
            onDismiss = { if (!busy) lifting = null },
            onConfirm = { reason, _ -> applyLift(ban.userId, reason) }
        )
    }

    // ── Permissions ──────────────────────────────────────────────────────
    permissionTarget?.let { (person, existing) ->
        ModerationPermissionsDialog(
            person = person,
            existing = existing,
            busy = busy,
            onDismiss = { if (!busy) permissionTarget = null },
            onSave = { posts, replies, reports_, admins, bans, forms ->
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
                        canBanMembers = bans,
                        canManageForms = forms
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

/** The line a ban's record keeps: which tier, and for how long. Written once
 *  here so the queue's own action and the ladder describe a ban identically. */
private fun banNote(kind: String, hours: Int?): String =
    "${banTierLabel(kind)} — " + when {
        hours == null -> "until it is lifted"
        hours == 24 -> "24 hours"
        else -> "${hours / 24} days"
    }

/**
 * One row of the BAN LIST: who, at which tier, why, by whom — and the two
 * moves a moderator can make from here (change the tier, lift it).
 *
 * A lapsed or lifted ban keeps its row, drawn quiet and without actions: the
 * list is a record as much as a control panel, and a member who was banned for
 * a week should still be findable the day after it expired.
 */
@Composable
private fun ModerationBanRow(
    ban: CommunityBan,
    busy: Boolean,
    onOpen: () -> Unit,
    onChange: () -> Unit,
    onLift: () -> Unit
) {
    val accent = if (ban.active) settingsRoseAccent()
                 else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                SocialAvatar(
                    style = ban.avatarStyle,
                    avatarSize = 40.dp,
                    onClick = onOpen
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = ban.label.ifBlank { "A member" },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = "@${ban.username.ifBlank { "—" }}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                // The tier chip: the tier's own name, so the list answers
                // "how far does this reach" without a second tap.
                Surface(
                    shape = RoundedCornerShape(50),
                    color = accent.copy(alpha = if (ban.active) 0.16f else 0.10f)
                ) {
                    Text(
                        text = if (ban.active) banTierLabel(ban.kind) else "Lifted",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = accent,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            if (ban.active) {
                Text(
                    text = banTierBlurb(ban.kind),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = banClockLine(ban),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ban.reason?.let { why ->
                Text(
                    text = "“$why”",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (ban.active) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ModerationRowAction(
                        label = "Change tier",
                        primary = false,
                        enabled = !busy,
                        onClick = onChange
                    )
                    ModerationRowAction(
                        label = "Lift ban",
                        primary = true,
                        enabled = !busy,
                        onClick = onLift
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "Open profile",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = curioDialogActionColor(),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onOpen)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

/** "Set 3 days ago by Jugnu · lifts in 4 days" — the clock in one line. */
private fun banClockLine(ban: CommunityBan): String {
    val set = ban.bannedAtMillis.takeIf { it > 0L }?.let { "Set ${relativeStamp(it)}" }
    val by = ban.bannedByName.trim().takeIf { it.isNotEmpty() }
    val head = listOfNotNull(set, by?.let { "by $it" }).joinToString(" ")
    if (!ban.active) return if (head.isBlank()) "Lifted" else "$head · lifted"
    val tail = ban.untilMillis?.let { until ->
        val left = until - System.currentTimeMillis()
        if (left <= 0L) "expiring now" else "lifts ${relativeStamp(until, future = true)}"
    } ?: "until lifted"
    return if (head.isBlank()) tail else "$head · $tail"
}

/** A compact "3 days ago" / "in 4 days" for the ban list. */
private fun relativeStamp(millis: Long, future: Boolean = false): String {
    val delta = if (future) millis - System.currentTimeMillis()
                else System.currentTimeMillis() - millis
    val minutes = (delta / 60_000L).coerceAtLeast(0L)
    val text = when {
        minutes < 2L -> "just now"
        minutes < 60L -> "$minutes min"
        minutes < 60L * 24L -> "${minutes / 60L} h"
        else -> "${minutes / (60L * 24L)} days"
    }
    return when {
        text == "just now" -> text
        future -> "in $text"
        else -> "$text ago"
    }
}

/** One small action in a ban row: filled = the main move, outlined = the other. */
@Composable
private fun ModerationRowAction(
    label: String,
    primary: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val accent = curioDialogActionColor()
    val shape = RoundedCornerShape(50)
    Surface(
        shape = shape,
        color = if (primary) accent else MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .clip(shape)
            .border(
                width = 1.dp,
                color = if (primary) accent else MaterialTheme.colorScheme.outlineVariant,
                shape = shape
            )
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = if (primary) androidx.compose.ui.graphics.Color.White
                    else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp)
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

/** The six switches, and what each one means. */
@Composable
private fun ModerationPermissionsDialog(
    person: CurioPerson,
    existing: CommunityAdminRow?,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        posts: Boolean,
        replies: Boolean,
        reports: Boolean,
        admins: Boolean,
        bans: Boolean,
        forms: Boolean
    ) -> Unit
) {
    var posts by remember { mutableStateOf(existing?.canDeletePosts ?: true) }
    var replies by remember { mutableStateOf(existing?.canDeleteReplies ?: true) }
    var reports_ by remember { mutableStateOf(existing?.canHandleReports ?: true) }
    var admins by remember { mutableStateOf(existing?.canManageAdmins ?: false) }
    var bans by remember { mutableStateOf(existing?.canBanMembers ?: false) }
    var forms by remember { mutableStateOf(existing?.canManageForms ?: false) }

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
                SettingsOptionSwitchRow(
                    icon = CurioIcons.AutoAwesome,
                    title = "Feedback forms",
                    subtitle = "Write, test and publish a form, and read its answers",
                    checked = forms,
                    enabled = !busy,
                    onCheckedChange = { forms = it }
                )
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = { onSave(posts, replies, reports_, admins, bans, forms) },
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
