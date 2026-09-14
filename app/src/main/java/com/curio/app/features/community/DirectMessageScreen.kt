package com.curio.app.features.community

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.animation.core.snap
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.foundation.LocalIndication
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.navigation.NavController
import com.curio.app.data.AppPreferences
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
import com.curio.app.data.supabase.CurioDirectMessage
import com.curio.app.data.supabase.CurioDmIdentity
import com.curio.app.data.supabase.CurioDmReaction
import com.curio.app.data.supabase.CurioDmCrypto
import com.curio.app.data.supabase.DmCryptoDiagnostics
import com.curio.app.data.supabase.CurioPerson
import com.curio.app.data.supabase.dmConversationId
import com.curio.app.data.supabase.OnlineAccount
import com.curio.app.data.supabase.RealtimeWatch
import com.curio.app.data.supabase.SocialApi
import com.curio.app.data.supabase.SupabaseRealtime
import com.curio.app.ui.components.rememberCurioPressSource
import com.curio.app.features.settings.SettingsHeroHeader
import com.curio.app.features.settings.SettingsHeroTotalHeight
import com.curio.app.features.settings.SettingsOptionCard
import com.curio.app.features.settings.SettingsOptionInfoRow
import com.curio.app.features.settings.SettingsOptionRow
import com.curio.app.features.settings.SettingsSectionHeading
import com.curio.app.features.settings.heroPageBackground
import com.curio.app.features.settings.settingsRoseAccent
import com.curio.app.navigation.CurioRoutes
import com.curio.app.ui.adaptive.isWide
import com.curio.app.ui.adaptive.wideContentEdgePadding
import com.curio.app.ui.adaptive.windowWidthSizeClass
import com.curio.app.ui.components.CurioWatermarkBackdrop
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.CurioMotion
import com.curio.app.ui.theme.curioDialogActionColor
import com.curio.app.ui.theme.curioDialogContainerColor
import com.curio.app.ui.theme.isCurioDarkTheme
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.async
import kotlin.math.roundToInt
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A DIRECT CONVERSATION — one thread with one friend.
 *
 * Private by construction: `dm_messages` gives its two participants the only
 * read policy, and the INSERT policy additionally requires an ACCEPTED friend
 * request, so a stranger cannot be messaged even with the right id. Text only —
 * there is no media column to fill.
 *
 * What the surface does now, top to bottom:
 *
 *  - **It opens instantly and survives losing signal.** The thread renders the
 *    last messages kept on the device ([SocialMessageCache]) before the network
 *    is asked anything, then the server's copy replaces it. A message you
 *    already saw is never a blank screen again.
 *  - **A header you can act on** — portrait, live username, and one honest
 *    line about what "private" means here — plus a tap through to the profile.
 *  - **A read conversation**: day rules, grouped runs from one person, a single
 *    timestamp per run, and a read receipt on your last line.
 *  - **Reactions**: tap a bubble and a palette slides in under it. The emoji
 *    itself is what the server stores, so nothing is uploaded.
 *  - **"is typing…"**: a real, server-backed row that expires on its own, shown
 *    as a live line in the header and as a breathing bubble in the thread.
 *  - **A composer that sends the moment you tap**: the message appears
 *    immediately on a spring, the field clears, and the network catches up.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DirectMessageScreen(
    navController: NavController,
    otherUserId: String,
    handle: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val account = OnlineAccount.state
    val wide = windowWidthSizeClass().isWide
    val listState = rememberLazyListState()
    val glassBackdrop = rememberLayerBackdrop()

    val onlineMode = AppPreferences.onlineModeEnabledState
    val token = account.session?.accessToken
    val myUserId = account.session?.userId
    val eligible = account.signedIn && onlineMode && token != null && myUserId != null

    // Filled from the device's own copy the instant the account resolves, so
    // a conversation you have already had is never a blank screen.
    var messages by remember { mutableStateOf<List<CurioDirectMessage>>(emptyList()) }
    // Sent-but-not-yet-confirmed messages, drawn exactly like real ones.
    var pending by remember { mutableStateOf<List<CurioDirectMessage>>(emptyList()) }
    // Sent bubbles waiting for their server row: rendered exactly like a
    // delivered message (same text, same clock), dropped the moment the real
    // row arrives. This is what keeps a send from vanishing and returning.
    var sentShadow by remember { mutableStateOf<List<CurioDirectMessage>>(emptyList()) }
    var person by remember {
        // On screen from the FIRST frame: the device remembers every identity
        // it has resolved, and the route carries the handle the caller already
        // had (Friends, a thread row, a profile). The network only refines
        // both — a conversation never opens on a placeholder.
        mutableStateOf(
            SocialPeopleCache.read(context, otherUserId)
                ?: handle.trim()
                    .takeIf { it.isNotBlank() }
                    ?.let { CurioPerson(userId = otherUserId, displayName = it) }
        )
    }
    var reactions by remember { mutableStateOf<Map<String, List<CurioDmReaction>>>(emptyMap()) }
    var draft by remember { mutableStateOf("") }
    var peerTyping by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }
    // This is read from the server-owned conversation row. It is never a
    // device preference: both participants see and use the same mode. It
    // starts OFF, which is what a brand-new conversation is (see the schema),
    // and a row that says otherwise replaces it as soon as it loads.
    var encryptionEnabled by remember(otherUserId) { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var loadedOnce by remember { mutableStateOf(false) }
    // A server push bumps this, which re-runs the delta fetch below. It is a
    // COUNTER rather than a flag so two pushes in a row are two fetches, and it
    // is keyed on the thread so opening another conversation starts fresh.
    var pushed by remember(otherUserId) { mutableStateOf(0) }
    // Set by a realtime UPDATE/DELETE push: the next delta pull re-reads the
    // whole page once, so a peer's edit or recall is reflected even though it
    // happened to a row OLDER than the newest one on screen.
    var pendingRealtimeRevisions by remember(otherUserId) { mutableStateOf(false) }

    // An ENCRYPTED send that failed raises this instead of a dead error line:
    // the dialog states the reason and offers the one-tap way out (turn the
    // shared mode off for both, then send the same text). Keyed on the thread
    // so another conversation never inherits a stale dialog.
    var encryptionIssue by remember(otherUserId) { mutableStateOf<String?>(null) }

    /**
     * Reacting to one message, from the floating action sheet (the palette is
     * the sheet's first row): optimistic glyph first, server confirm after,
     * revert on failure. Guarded so a call without a live session is simply a
     * no-op.
     */
    fun pickReactionScreen(messageId: String, kind: String, activeToken: String?, activeUserId: String?) {
        if (activeToken == null || activeUserId == null) return
        scope.launch {
            val mine = reactions[messageId]?.firstOrNull { it.userId == activeUserId }
            val optimistic = if (mine?.kind == kind) {
                reactions[messageId].orEmpty().filterNot { it.userId == activeUserId }
            } else {
                reactions[messageId].orEmpty()
                    .filterNot { it.userId == activeUserId } +
                    CurioDmReaction(messageId, activeUserId, kind)
            }
            reactions = reactions + (messageId to optimistic)
            val result = if (mine?.kind == kind) {
                SocialApi.clearReaction(activeToken, messageId)
            } else {
                SocialApi.react(activeToken, messageId, kind)
            }
            result.fold(
                onSuccess = {
                    SocialApi.reactions(activeToken, listOf(messageId))
                        .onSuccess { fresh ->
                            reactions = reactions + (messageId to fresh.getOrElse(messageId) { emptyList<CurioDmReaction>() })
                        }
                },
                onFailure = {
                    reactions = reactions + (messageId to (mine?.let { listOf(it) } ?: emptyList()))
                    error = it.message
                }
            )
        }
    }

    LaunchedEffect(Unit) { OnlineAccount.restore(context) }

    suspend fun load(active: String, me: String) {
        loading = true
        SocialApi.messages(active, otherUserId, me).fold(
            onSuccess = { raw ->
                val conversationId = dmConversationId(me, otherUserId)
                encryptionEnabled = SocialApi.dmConversation(active, conversationId)
                    .getOrDefault(com.curio.app.data.supabase.CurioDmConversation()).encryptionEnabled
                val identity = runCatching { CurioDmCrypto.identity(context) }.getOrElse { failure ->
                    error = "This device's encrypted-message identity is unavailable."
                    DmCryptoDiagnostics.failure("identity_load", null, null, null, failure)
                    return@fold
                }
                var identityUsable = true
                SocialApi.publishDmIdentity(active, identity, me).getOrElse { failure ->
                    // Registration keeps the ENCRYPTED features honest, but it
                    // must never take the conversation down with it: without
                    // this row the device cannot receive wrapped keys, so
                    // ciphertext stays sealed — and the error text says that
                    // plainly instead of pretending the page is broken.
                    identityUsable = false
                    DmCryptoDiagnostics.failure("identity_publish", null, null, null, failure)
                }
                // The same self-heal the send path does: a stale identity of
                // MINE (a previous install) is retired so the server counts
                // exactly one active device per side. Best-effort — the send
                // path retires again before wrapping.
                if (identityUsable) {
                    SocialApi.dmIdentities(active, listOf(me)).getOrDefault(emptyList())
                        .filter { it.deviceId != identity.deviceId }
                        .forEach { stale -> SocialApi.retireDmDevice(active, stale.deviceId) }
                }
                // Get every envelope a page needs in ONE request. The former
                // per-message sequential requests delayed arrivals and could
                // leave a realtime row rendered before its key was available.
                val requiredVersions = raw.mapNotNull { message ->
                    CurioDmCrypto.messageKeyVersion(message.encryptionVersion.orEmpty())
                }.toSet()
                val envelopeResult = SocialApi.dmEnvelopes(
                    active, conversationId, identity.deviceId, requiredVersions
                )
                val envelopeFailure = envelopeResult.exceptionOrNull()
                val envelopes = envelopeResult.getOrDefault(emptyMap())
                val installedVersions = mutableSetOf<Int>()
                requiredVersions.forEach { version ->
                    val envelope = envelopes[version]
                    if (envelope == null) {
                        DmCryptoDiagnostics.event(
                            stage = "envelope_missing", conversationId = conversationId,
                            keyVersion = version, deviceId = identity.deviceId,
                            detail = "found=false installed=false"
                        )
                    } else {
                        runCatching { CurioDmCrypto.installEnvelope(context, conversationId, envelope) }
                            .onSuccess { key ->
                                installedVersions += version
                                DmCryptoDiagnostics.event(
                                    stage = "envelope_install", conversationId = conversationId,
                                    keyVersion = version, deviceId = identity.deviceId,
                                    detail = "found=true installed=true keyLength=${key.size} rsaUnwrap=success"
                                )
                            }
                            .onFailure { failure ->
                                DmCryptoDiagnostics.failure(
                                    "envelope_install", conversationId, null, version, failure,
                                    "found=true installed=false rsaUnwrap=failure"
                                )
                            }
                    }
                }
                val fresh = raw.map { message ->
                    if (message.migrationState == "legacy") message.copy(body = "Legacy message — re-encryption required")
                    else if (message.migrationState == "plaintext") message
                    else {
                        val encrypted = com.curio.app.data.supabase.CurioEncryptedMessage(
                            message.ciphertext.orEmpty(),
                            message.nonce.orEmpty(),
                            message.encryptionVersion.orEmpty()
                        )
                        // A thread can hold messages from multiple envelope
                        // generations. The page's exact-version envelopes were
                        // fetched and installed above before any AES-GCM read.
                        val version = CurioDmCrypto.messageKeyVersion(encrypted.version)
                        when {
                            version == null -> message.copy(body = "Unsupported encrypted message format")
                            envelopeFailure != null -> {
                                DmCryptoDiagnostics.failure(
                                    "envelope_fetch", conversationId, message.id, version, envelopeFailure,
                                    "envelopeFound=unknown keyInstalled=false"
                                )
                                message.copy(body = "Message key could not be retrieved")
                            }
                            envelopes[version] == null -> {
                                DmCryptoDiagnostics.event(
                                    stage = "message_key_missing", conversationId = conversationId,
                                    messageId = message.id, keyVersion = version,
                                    deviceId = identity.deviceId,
                                    detail = "envelopeFound=false keyInstalled=false"
                                )
                                message.copy(body = "Message key is unavailable on this device")
                            }
                            version !in installedVersions -> message.copy(body = "Message key envelope is invalid")
                            else -> runCatching {
                                CurioDmCrypto.decrypt(context, conversationId, encrypted)
                            }.onSuccess { plaintext ->
                                DmCryptoDiagnostics.event(
                                    stage = "aes_decrypt", conversationId = conversationId,
                                    messageId = message.id, keyVersion = version,
                                    deviceId = identity.deviceId,
                                    detail = "keyInstalled=true keyLength=32 nonceLength=${android.util.Base64.decode(encrypted.nonce, android.util.Base64.NO_WRAP).size} ciphertextLength=${android.util.Base64.decode(encrypted.ciphertext, android.util.Base64.NO_WRAP).size} aesDecrypt=success"
                                )
                            }.onFailure { failure ->
                                DmCryptoDiagnostics.failure(
                                    "aes_decrypt", conversationId, message.id, version, failure,
                                    "keyInstalled=true aesDecrypt=failure"
                                )
                            }.fold(
                                onSuccess = { plaintext -> message.copy(body = plaintext) },
                                onFailure = { failure ->
                                    val reason = if (failure is javax.crypto.AEADBadTagException) {
                                        "Message authentication failed"
                                    } else "Message key or encrypted data is invalid"
                                    message.copy(body = reason)
                                }
                            )
                        }
                    }
                }
                // v3xx53 — the SERVER keeps 24 hours; the DEVICE keeps what it
                // received. Merging (rather than replacing) is what makes "gone
                // from the server" and "gone from Curio" two different things:
                // opening a conversation can never lose a message this phone
                // already had. Newest wins per id, so a cached row still gets
                // its fresh read receipt.
                val hidden = SocialMessageCache.hiddenIds(context, otherUserId)
                val known = SocialMessageCache.read(context, otherUserId, me).filterNot { it.id in hidden }
                val merged = (known + fresh.filterNot { it.id in hidden })
                    .distinctBy { it.id }
                    .sortedBy { it.createdAtMillis }
                messages = merged
                SocialMessageCache.write(context, otherUserId, merged)
                // Shadows whose words are now covered by a real server row
                // (same text, mine, within ten seconds) retire here — the
                // swap is invisible because both render the same bubble.
                sentShadow = sentShadow.filterNot { shadow ->
                    merged.any { real ->
                        real.mine && real.body == shadow.body &&
                            kotlin.math.abs(real.createdAtMillis - shadow.createdAtMillis) < 10_000L
                    }
                }
                error = null
                loadedOnce = true
            },
            onFailure = { failure ->
                // The cache already put something on screen; only speak up
                // when there was nothing to fall back on.
                if (messages.isEmpty()) error = failure.message
            }
        )
        loading = false
        // Reactions ride the same refresh, best-effort: a failure hides a
        // glyph, it never blanks the conversation.
        val ids = messages.map { it.id }
        if (ids.isNotEmpty()) {
            SocialApi.reactions(active, ids).onSuccess { reactions = it }
        }
    }

    // Who this conversation is with — resolved here so the header shows a
    // portrait and the LIVE username rather than the name the route carried.
    suspend fun loadPerson(active: String) {
        // `profile` rather than `people`: the peer card also draws the presence
        // line, which only the wider privacy read carries — and it falls back
        // to the base identity read on a project that has not been re-pasted.
        SocialApi.profile(active, otherUserId).onSuccess { fresh ->
            if (fresh != null) {
                person = fresh
                // Remembered so the NEXT open draws the real name instantly.
                SocialPeopleCache.remember(context, fresh)
            }
        }
    }

    // The text an ENCRYPTED send was carrying when it failed, so the dialog's
    // one-tap fallback can send the very words the member already typed.
    var failedDraft by remember(otherUserId) { mutableStateOf("") }

    // The message whose floating action sheet is up (Instagram style): the
    // palette, Copy, Edit and Remove ride ON the thread — no dialog, no scrim.
    // A second hold on the same bubble drops it.
    var actionTarget by remember(otherUserId) { mutableStateOf<CurioDirectMessage?>(null) }
    // The message being ANSWERED: the swipe raised it. Its words ride above
    // the composer until the answer is sent or the banner is dismissed.
    var quoteTarget by remember(otherUserId) { mutableStateOf<CurioDirectMessage?>(null) }
    // Quotes for replies whose parent fell out of the loaded window, fetched
    // OUTSIDE composition (a suspend read must never run inside a composable).
    var replyQuotes by remember(otherUserId) { mutableStateOf<Map<String, String>>(emptyMap()) }
    val haptics = LocalHapticFeedback.current
    // The message being EDITED: its words load into the composer and the
    // send button becomes Save until the edit is done or dropped.
    var editing by remember(otherUserId) { mutableStateOf<CurioDirectMessage?>(null) }

    /** Applies an edit from the composer; plaintext rows only. */
    suspend fun commitEdit(active: String, message: CurioDirectMessage) {
        val text = draft.trim()
        if (text.isEmpty() || sending) return
        sending = true
        SocialApi.editMessage(active, message.id, text).fold(
            onSuccess = {
                editing = null
                draft = ""
                load(active, message.senderId)
            },
            onFailure = { failure -> error = failure.message ?: "Couldn't edit that message." }
        )
        sending = false
    }

    // Tracks outgoing messages so the auto-scroll LaunchedEffect can
    // scroll to bottom after every send (even when the user has scrolled
    // up to read history).
    var pendingSends by remember(otherUserId) { mutableStateOf(0) }

    suspend fun send(active: String, me: String, forcePlaintext: Boolean = false) {
        // An edit in flight takes the composer over: Send IS Save until the
        // edit is committed or dropped.
        editing?.let { target ->
            commitEdit(active, target)
            return
        }
        val text = draft.trim()
        if (text.isEmpty() || sending) return
        sending = true
        error = null
        encryptionIssue = null
        // A reply binds to the message raised above the composer; the banner
        // drops the moment the answer is on its way.
        val replyTo = quoteTarget?.id
        quoteTarget = null

        // Optimistic: the bubble is on screen before the request leaves, and
        // its id is local-only so a refresh can never show it twice.
        val optimistic = CurioDirectMessage(
            id = "local-${System.currentTimeMillis()}",
            senderId = me,
            body = text,
            createdAtMillis = System.currentTimeMillis(),
            readAtMillis = null,
            mine = true,
            replyTo = replyTo
        )
        pending = pending + optimistic
        draft = ""
        pendingSends++

        val conversationId = dmConversationId(me, otherUserId)
        if (!encryptionEnabled || forcePlaintext) {
            // The dialog's promise, kept here: turning the shared mode off is
            // part of the fallback send, for BOTH people — if the server
            // refuses the change, the plain error line says why and nothing is
            // sent in the wrong mode.
            if (forcePlaintext && encryptionEnabled) {
                SocialApi.setDmEncryption(active, conversationId, me, otherUserId, false).fold(
                    onSuccess = { encryptionEnabled = false },
                    onFailure = { failure ->
                        pending = pending.filterNot { it.id == optimistic.id }
                        draft = text
                        error = failure.message ?: "Couldn't change message encryption."
                        sending = false
                        return
                    }
                )
            }
            SocialApi.sendPlaintext(active, otherUserId, text, me, replyTo).fold(
                onSuccess = {
                    SocialApi.setTyping(active, otherUserId, false)
                    // The bubble does NOT vanish while the thread re-reads: it
                    // moves from [pending] to [sentShadow], a stand-in that
                    // renders until the server's own copy of the same words
                    // lands in [messages] — then it is dropped silently. The
                    // old remove-then-load sequence was the flicker: gone for
                    // a beat, then back.
                    pending = pending.filterNot { it.id == optimistic.id }
                    sentShadow = sentShadow + optimistic
                    load(active, me)
                },
                onFailure = { failure ->
                    pending = pending.filterNot { it.id == optimistic.id }
                    draft = text
                    error = failure.message ?: "That message didn't send."
                }
            )
            sending = false
            return
        }
        val encrypted = runCatching {
            val mine = CurioDmCrypto.identity(context)
            // A reinstall leaves the PREVIOUS device row active on the server
            // forever, and the envelope completeness check then demands a key
            // be wrapped for hardware this account no longer owns — the exact
            // "missing a device envelope for its key version" failure. Retire
            // every other active identity of MINE first so only this device
            // counts, then publish this one. The friend's rows are theirs to
            // manage, and old envelopes stay historically valid for their
            // devices.
            SocialApi.dmIdentities(active, listOf(me)).getOrDefault(emptyList())
                .filter { it.deviceId != mine.deviceId }
                .forEach { stale -> SocialApi.retireDmDevice(active, stale.deviceId) }
            SocialApi.publishDmIdentity(active, mine, me).getOrThrow()
            val ownEnvelope = SocialApi.dmEnvelope(active, conversationId, mine.deviceId)
                .getOrNull()
            // A conversation from a previous install can carry an envelope
            // that this device's replacement keypair cannot open. Keep that
            // historical envelope for older messages and rotate a new version
            // for the next send instead of failing the entire conversation.
            val restoredKey = ownEnvelope?.let { envelope ->
                runCatching { CurioDmCrypto.installEnvelope(context, conversationId, envelope) }
                    .getOrNull()
            }
            val keyVersion = when {
                restoredKey != null -> ownEnvelope!!.keyVersion
                else -> SocialApi.dmHighestKeyVersion(active, conversationId).getOrThrow()
                    .coerceAtMost(Int.MAX_VALUE - 1) + 1
            }
            val key = restoredKey
                ?: CurioDmCrypto.existingKey(context, conversationId, keyVersion)
                ?: CurioDmCrypto.newKey(context, conversationId, keyVersion)
            // The server is the bookkeeper: it names the devices that still
            // lack an envelope for this version (its own trigger re-checks the
            // same list on insert). The client could not compute this list —
            // row-level security hides the friend's device rows from the
            // sender on purpose, and an empty read once made every send look
            // like a friend who never opened the app.
            val missing = SocialApi.dmMissingEnvelopes(active, conversationId, keyVersion)
                .getOrNull()
            val peers: List<CurioDmIdentity> = when {
                missing != null -> {
                    // The authoritative path: every row the server named comes
                    // WITH its public key, so the wrap list is built directly
                    // from it — no second read that row-level security could
                    // empty. Rows without a usable key are skipped; if that
                    // leaves the friend uncovered, the send fails with the
                    // clear message below rather than a rejected insert.
                    missing.mapNotNull { it.toIdentity() }.ifEmpty {
                        // The server named devices but none carried a usable
                        // key: fall back to the identity read for whatever it
                        // can still see.
                        SocialApi.dmIdentities(active, listOf(otherUserId, me))
                            .getOrDefault(emptyList())
                            .filter(CurioDmCrypto::canWrapFor)
                    }
                }
                else -> {
                    // The RPC is not installed on this project yet: wrap for
                    // every device the (possibly empty) identity read sees.
                    // A stale row that should have been retired is healed by
                    // the publish above; the rest is the server's grace.
                    SocialApi.dmIdentities(active, listOf(otherUserId, me)).getOrDefault(emptyList())
                        .filter(CurioDmCrypto::canWrapFor)
                }
            }
            check(peers.any { it.userId == otherUserId }) {
                "This friend needs to open Curio once before encrypted messages can reach them."
            }
            // Envelope writes are independent. Send them together instead of
            // making the composer wait one network round trip per device.
            coroutineScope {
                peers.map { peer ->
                    async {
                        SocialApi.saveDmEnvelope(
                            active, conversationId, peer.userId,
                            CurioDmCrypto.wrapConversationKey(key, peer, keyVersion)
                        ).getOrThrow()
                    }
                }.awaitAll()
            }
            CurioDmCrypto.encrypt(context, conversationId, key, keyVersion, text)
        }.getOrElse { failure ->
            pending = pending.filterNot { it.id == optimistic.id }
            draft = text
            // This preparation also contacts the server to exchange public
            // keys. Do not misreport a friend/RLS/network failure as a broken
            // keystore: that sent people looking for a device fix when the
            // actionable problem was the server response.
            val reason = failure.message
                ?.takeIf { it.isNotBlank() }
                ?: "Couldn't prepare this encrypted message. Please try again."
            if (encryptionEnabled) {
                error = null
                failedDraft = text
                encryptionIssue = reason
            } else {
                error = reason
            }
            sending = false
            return
        }
        SocialApi.sendEncrypted(
            active,
            otherUserId,
            encrypted.ciphertext,
            encrypted.nonce,
            encrypted.version,
            me,
            replyTo
        ).fold(
            onSuccess = {
                SocialApi.setTyping(active, otherUserId, false)
                // Same shadow swap as the plaintext path: never remove the
                // bubble before its replacement exists.
                pending = pending.filterNot { it.id == optimistic.id }
                sentShadow = sentShadow + optimistic
                load(active, me)
            },
            onFailure = { failure ->
                pending = pending.filterNot { it.id == optimistic.id }
                draft = text
                val reason = failure.message ?: "That message didn't send."
                if (encryptionEnabled) {
                    error = null
                    failedDraft = text
                    encryptionIssue = reason
                } else {
                    error = reason
                }
            }
        )
        sending = false
    }

    LaunchedEffect(eligible, token, myUserId) {
        if (!eligible || token == null || myUserId == null) {
            messages = emptyList()
            pending = emptyList()
            sentShadow = emptyList()
            reactions = emptyMap()
            peerTyping = false
            return@LaunchedEffect
        }
        // The device's copy first — the account id is known here, so "mine"
        // is labelled correctly — then the server's own record.
        SocialMessageCache.migrateIfNeeded(context)
        val cached = SocialMessageCache.read(context, otherUserId, myUserId)
        if (cached.isNotEmpty()) messages = cached
        load(token, myUserId)
        loadPerson(token)
        // Existing incoming messages are marked only after the conversation
        // has rendered and the user has entered this screen; new arrivals use
        // the same rule in pullDelta below. Never mark a sender's messages
        // read merely because the sender refreshed their own thread.
    }

    // "is typing…" — pushed by a `dm_typing` frame, and re-read on a timer as
    // the safety net (the push shows it the instant the other side starts; the
    // timer is what CLEARS a row whose writer stopped refreshing it). Fast only
    // while the push channel is down.
    LaunchedEffect(eligible, token, myUserId) {
        if (!eligible || token == null || myUserId == null) return@LaunchedEffect
        while (true) {
            peerTyping = SocialApi.isTyping(token, myUserId, otherUserId)
            delay(if (SupabaseRealtime.isLinked) TYPING_SAFETY_TICK_MS else TYPING_TICK_MS)
        }
    }

    // THE THREAD'S DELTA — one small pull, shared by the push and the timer.
    //
    // It asks only for what is NEWER than the newest message already on screen
    // (a strict `created_at >` window, so a pull is a few hundred bytes),
    // merges it in, keeps the device's copy current and stamps the receipt.
    suspend fun pullDelta(active: String, me: String) {
        // Only CONFIRMED messages anchor the window: an optimistic bubble
        // carries the phone's own clock, and a fast phone would otherwise push
        // the window past the very messages this pull exists to find.
        val anchor = messages
            .filterNot { it.id.startsWith(LOCAL_ID_PREFIX) }
            .maxOfOrNull { it.createdAtMillis }
            ?: return
        val fresh = SocialApi.messagesSince(active, otherUserId, me, anchor)
            .getOrNull()
            .orEmpty()
            .filter { row -> (messages + pending).none { it.id == row.id } }
        // A realtime push can also be an EDIT or a DELETE on an OLDER row,
        // which the created_at window above never sees. Edits are re-read in
        // full at most once per push (cheap: one page read); deletes of rows
        // we still hold are pruned locally.
        if (pendingRealtimeRevisions) {
            pendingRealtimeRevisions = false
            SocialApi.messages(active, otherUserId, me).getOrNull()?.let { server ->
                val hidden = SocialMessageCache.hiddenIds(context, otherUserId)
                messages = messages.mapNotNull { held ->
                    when {
                        held.id in hidden -> null
                        // Gone from the server: they recalled it.
                        server.none { it.id == held.id } && !held.id.startsWith(LOCAL_ID_PREFIX) -> null
                        // Changed on the server: their edit wins.
                        else -> server.firstOrNull { it.id == held.id } ?: held
                    }
                }
                SocialMessageCache.write(context, otherUserId, messages)
            }
        }
        // The receipts ride the same pull, whether or not anything new arrived:
        // they change when the other side READS, which is a different moment
        // from when they write.
        SocialApi.readStamps(active, otherUserId, me).onSuccess { stamps ->
            if (stamps.isNotEmpty()) {
                messages = messages.map { message ->
                    val at = stamps[message.id] ?: return@map message
                    if (message.readAtMillis == null || message.readAtMillis < at) {
                        message.copy(readAtMillis = at)
                    } else {
                        message
                    }
                }
            }
        }
        if (fresh.isEmpty()) return
        // `messagesSince` returns transport rows. Route a real arrival through
        // the same batched-envelope decrypt path as initial load; otherwise a
        // realtime message can briefly keep its null body or stale ciphertext.
        load(active, me)
        // An arrival means the other side stopped writing.
        peerTyping = false
        if (fresh.any { !it.mine }) {
            SocialApi.markRead(active, otherUserId, me)
        }
    }

    /**
     * The live bits that are NOT part of the message delta: the other side's
     * "is typing…" row and their reactions on the messages already on screen.
     *
     * Both are tiny RLS-protected reads, driven by a realtime hint instead of a
     * tick. Reactions are re-read whole (rather than merged) because a REMOVED
     * reaction has no row to merge from — the other person taking their glyph
     * back is exactly as live as them leaving one.
     */
    suspend fun refreshLiveBits(active: String, me: String) {
        peerTyping = SocialApi.isTyping(active, me, otherUserId)
        val ids = messages.map { it.id }.filterNot { it.startsWith(LOCAL_ID_PREFIX) }
        if (ids.isEmpty()) return
        SocialApi.reactions(active, ids).onSuccess { fresh -> reactions = fresh }
    }

    // REALTIME — the server tells this screen when the thread moved, instead of
    // a timer asking. Four bindings, all SERVER-filtered: their new messages
    // (INSERT), my own message being read (UPDATE on a row I sent them), their
    // "is typing…" row (INSERT/UPDATE), and a reaction from them (the row's
    // primary key carries the reactor, so INSERT/UPDATE/DELETE all match the
    // filter). The subscription is released the moment the screen goes away.
    DisposableEffect(eligible, token, myUserId, otherUserId) {
        val active = token
        val me = myUserId
        val owner = "dm:$otherUserId"
        if (eligible && active != null && me != null) {
            SupabaseRealtime.watch(
                owner = owner,
                accessToken = active,
                watches = listOf(
                    RealtimeWatch(
                        table = "dm_messages",
                        // INSERT is their new message; UPDATE catches their
                        // edit; DELETE their recall. The delta fetch then
                        // re-reads the touched rows.
                        filter = "sender=eq.$otherUserId",
                        events = listOf("INSERT", "UPDATE", "DELETE")
                    ),
                    RealtimeWatch(
                        table = "dm_messages",
                        filter = "recipient=eq.$otherUserId",
                        events = listOf("UPDATE")
                    ),
                    RealtimeWatch(
                        table = "dm_typing",
                        filter = "sender=eq.$otherUserId",
                        events = listOf("INSERT", "UPDATE")
                    ),
                    RealtimeWatch(
                        table = "dm_reactions",
                        filter = "user_id=eq.$otherUserId",
                        events = listOf("INSERT", "UPDATE", "DELETE")
                    )
                )
            ) {
                // The push may be an INSERT, or an UPDATE/DELETE on an older
                // row (their edit, their recall). The next delta pull asks for
                // the whole page once when any revision flag is set, so both
                // shapes land. Compose state is written on the composition's
                // own scope, never from the socket thread.
                scope.launch { pendingRealtimeRevisions = true }
                scope.launch { pushed++ }
                // The live bits that are not part of the message delta — the
                // typing row and their reactions — are re-read right away.
                scope.launch { refreshLiveBits(active, me) }
            }
        }
        onDispose { SupabaseRealtime.unwatch(owner) }
    }

    // A push means "fetch now". The very first run is skipped (pushed == 0):
    // entry already loaded the thread, and this effect exists for arrivals.
    LaunchedEffect(pushed, eligible, token, myUserId, otherUserId) {
        if (!eligible || token == null || myUserId == null || pushed == 0) return@LaunchedEffect
        pullDelta(token, myUserId)
    }

    // THE FALLBACK TIMER. With realtime linked this is only a safety net — a
    // missed frame, a socket that dropped silently — so it stays deliberately
    // slow. Without realtime it is the whole mechanism, at the original
    // cadence, which is why a blocked WebSocket never freezes the thread.
    LaunchedEffect(eligible, token, myUserId, otherUserId) {
        if (!eligible || token == null || myUserId == null) return@LaunchedEffect
        while (true) {
            delay(if (SupabaseRealtime.isLinked) SAFETY_TICK_MS else LIVE_TICK_MS)
            pullDelta(token, myUserId)
        }
    }

    // Tell the other side when this side is writing: a short debounce stops a
    // request per keystroke, and the row clears itself after a pause.
    LaunchedEffect(draft, eligible, token) {
        if (!eligible || token == null) return@LaunchedEffect
        if (draft.isBlank()) {
            SocialApi.setTyping(token, otherUserId, false)
            return@LaunchedEffect
        }
        delay(700)
        SocialApi.setTyping(token, otherUserId, true)
        delay(6_000)
        SocialApi.setTyping(token, otherUserId, false)
    }

    val thread = remember(messages, pending, sentShadow) {
        // Shadows sit BETWEEN the confirmed rows and the still-sending tail:
        // they are delivered as far as anyone can see, and they sort by their
        // own (phone) clock like the optimistic rows do.
        messages + (sentShadow + pending).sortedBy { it.createdAtMillis }
    }        // The rows that sit ABOVE the messages in the same LazyColumn. Scrolling
    // needs the message's real index, not its index within the conversation.
    val headerRows = (if (wide) 1 else 0) +
        1 + // the peer card
        (if (error != null) 1 else 0) +
        (if (thread.isEmpty() && !loading && loadedOnce) 1 else 0)

    // Keep the newest line in view — on open, after every send, on an edit
    // or a reaction (both change a row's height), and when the other side
    // starts typing under us. The jump is suppressed while the member has
    // scrolled UP to read history; a message arriving mid-scroll must never
    // yank the list out from under their finger.
    var hasPresentedThread by remember(otherUserId) { mutableStateOf(false) }
    val pinnedToNewest = !listState.canScrollForward || !hasPresentedThread
    LaunchedEffect(thread.size, peerTyping, headerRows) {
        if (thread.isEmpty()) return@LaunchedEffect
        val newest = headerRows + thread.lastIndex + if (peerTyping) 1 else 0
        if (!hasPresentedThread) {
            listState.scrollToItem(newest)
            hasPresentedThread = true
        } else if (pinnedToNewest) {
            listState.animateScrollToItem(newest)
        }
    }
    // v-fix — SENDING always scrolls to bottom, even when the member has
    // scrolled up to read history. The main LaunchedEffect above skips the
    // scroll when pinnedToNewest is false (reading history), so a send
    // while scrolled up used to drop the optimistic bubble off-screen.
    LaunchedEffect(pendingSends) {
        if (pendingSends == 0) return@LaunchedEffect
        // +1 for the optimistic message that is about to enter [thread]
        listState.animateScrollToItem(headerRows + thread.lastIndex + 1)
    }

    // The DISPLAY name wins over the name the route carried, so a rename shows
    // up in the conversation too. A locally remembered or route-carried person
    // has no @username yet — the peer card then shows their name alone rather
    // than a made-up handle.
    val fallback = person?.label?.takeIf { it.isNotBlank() }
        ?: handle.ifBlank { "Message" }
    // The hero (and the collapsed bar) carry the display name; the @username
    // and the presence line sit on the peer card beneath it.
    val title = fallback

    // Read receipts are per-row now: a bubble shows ticks only when THAT row
    // carries the other side's read stamp — no newest-row inference that can
    // claim a reading that never happened.

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                heroPageBackground(
                    lerp(MaterialTheme.colorScheme.background, settingsRoseAccent(), 0.10f)
                )
            )
    ) {
        if (!wide) {
            CurioWatermarkBackdrop(
                activeCat = CurioCategories.byId(CategoryId.WILDCARD),
                alphaScale = 0.45f
            )
        }

        Column(
            modifier = Modifier
                .layerBackdrop(glassBackdrop)
                .fillMaxSize()

        ) {
            if (eligible && token != null && myUserId != null) {
                val activeToken = token
                val activeUserId = myUserId

                fun pickReaction(messageId: String, kind: String) {
                    pickReactionScreen(messageId, kind, activeToken, activeUserId)
                }

                // A double-tap IS a heart (Instagram's gesture): the first
                // palette emoji, picked on the spot — and cleared again by the
                // same double-tap when it is already the active reaction.
                fun doubleTapReaction(messageId: String) {
                    pickReaction(messageId, SocialReactions.PALETTE.first().first)
                }

                fun beginReply(message: CurioDirectMessage) {
                    editing = null
                    quoteTarget = message
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }

                // A reply whose parent is no longer in the window still shows
                // its words: fetch them once, outside composition, into the
                // small quote map. RLS limits the read to this conversation.
                val unresolvedQuoteIds = thread
                    .mapNotNull { it.replyTo }
                    .filter { id -> thread.none { it.id == id } && replyQuotes[id] == null }
                    .distinct()
                LaunchedEffect(unresolvedQuoteIds) {
                    if (unresolvedQuoteIds.isEmpty()) return@LaunchedEffect
                    val active = activeToken
                    val me = activeUserId
                    if (active == null || me == null) return@LaunchedEffect
                    unresolvedQuoteIds.forEach { id ->
                        SocialApi.replyPreview(active, id, me).getOrNull()?.let { row ->
                            if (row.body.isNotBlank()) {
                                replyQuotes = replyQuotes + (id to row.body)
                            }
                        }
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(
                        start = wideContentEdgePadding(),
                        end = wideContentEdgePadding(),
                        top = if (wide) 0.dp else SettingsHeroTotalHeight,
                        bottom = 12.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (wide) {
                        item(key = "hero", contentType = "hero") {
                            SettingsHeroHeader(
                                title = title,
                                subtitle = if (peerTyping) "Typing…" else person?.handleLabel.orEmpty(),
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }

                    item(key = "peer") {
                        MessagePeerHeader(
                            person = person,
                            fallback = fallback,
                            typing = peerTyping,
                            activityAtMillis = thread.lastOrNull { !it.mine }?.createdAtMillis,
                            onOpenProfile = {
                                navController.navigate(CurioRoutes.socialProfile(otherUserId)) {
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                    // The per-chat encryption toggle is EXPERIMENTAL and
                    // opt-in at the device level: it only exists when the
                    // member asked for it in Settings → Online mode. A
                    // conversation whose shared mode is already ON keeps its
                    // pill (and its encrypted behaviour) either way — the gate
                    // hides the door, it never slams one that is open.
                    val encryptionExposed = AppPreferences.dmEncryptionEnabledState || encryptionEnabled
                    if (encryptionExposed) {
                        item(key = "delivery-mode") {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                SocialPill(
                                    label = if (encryptionEnabled) "Encrypted" else "Encryption off",
                                    icon = if (encryptionEnabled) CurioIcons.Lock else CurioIcons.Warning,
                                    tone = if (encryptionEnabled) SocialPillTone.ACCENT else SocialPillTone.NEUTRAL,
                                    enabled = !sending,
                                    onClick = {
                                        scope.launch {
                                            val conversationId = dmConversationId(activeUserId, otherUserId)
                                            SocialApi.setDmEncryption(
                                                activeToken, conversationId, activeUserId, otherUserId,
                                                !encryptionEnabled
                                            ).fold(
                                                onSuccess = { encryptionEnabled = it.encryptionEnabled },
                                                onFailure = { error = it.message ?: "Couldn't change message encryption." }
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }

                    error?.let { message -> item(key = "error") { SocialNote(message, true) } }

                    if (thread.isEmpty() && !loading && loadedOnce) {
                        item(key = "empty") {
                            SocialEmptyCard(
                                icon = CurioIcons.Notes,
                                title = "Nothing said yet",
                                body = "This thread is only the two of you — start it with a hello."
                            )
                        }
                    }

                    itemsIndexedWithDays(thread) { index, message, dayLabel, firstOfRun, lastOfRun ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            if (dayLabel != null) SocialDayDivider(dayLabel)
                            MessageEntry(
                                message = message,
                                firstOfRun = firstOfRun,
                                lastOfRun = lastOfRun,
                                // Per-row truth: a tick pair only when THIS
                                // row carries the other side's read stamp.
                                receipt = message.readAtMillis,
                                actionSheet = actionTarget?.id == message.id,
                                reactions = reactions[message.id].orEmpty(),
                                myUserId = activeUserId,
                                onDoubleTap = { doubleTapReaction(message.id) },
                                // A second hold on the same bubble drops the sheet.
                                onHold = {
                                    actionTarget = if (actionTarget?.id == message.id) null else message
                                },
                                onSwipeReply = { beginReply(message) },
                                onReply = { beginReply(message) },
                                onPick = { kind -> pickReaction(message.id, kind) },
                                onCopy = {
                                    val clip = context.getSystemService(Context.CLIPBOARD_SERVICE)
                                        as? android.content.ClipboardManager
                                    clip?.setPrimaryClip(
                                        android.content.ClipData.newPlainText("message", message.body)
                                    )
                                    actionTarget = null
                                },
                                onEdit = {
                                    editing = message
                                    draft = message.body
                                    actionTarget = null
                                },
                                onRemove = if (message.mine && !message.id.startsWith(LOCAL_ID_PREFIX)) {
                                    {
                                        actionTarget = null
                                        scope.launch {
                                            SocialApi.deleteMessage(activeToken, message.id).fold(
                                                onSuccess = {
                                                    messages = messages.filterNot { it.id == message.id }
                                                    SocialMessageCache.write(context, otherUserId, messages)
                                                },
                                                onFailure = { error = it.message ?: "Couldn't remove that message." }
                                            )
                                        }
                                    }
                                } else null,
                                quoteOf = message.replyTo?.let { target ->
                                    (thread + pending + sentShadow).firstOrNull { it.id == target }?.body
                                        ?: replyQuotes[target]
                                },
                                animateIn = message.id.startsWith(LOCAL_ID_PREFIX)
                            )
                        }
                    }

                    if (peerTyping && thread.isNotEmpty()) {
                        item(key = "typing") { TypingBubble(person) }
                    }
                }

                MessageComposer(
                    modifier = Modifier,
                    draft = draft,
                    title = fallback,
                    sending = sending,
                    editTarget = editing,
                    quoteTarget = quoteTarget,
                    onDropEdit = {
                        editing = null
                        draft = ""
                    },
                    onDropQuote = { quoteTarget = null },
                    onDraftChange = { if (it.length <= SocialApi.MAX_MESSAGE_CHARS) draft = it },
                    onSend = { scope.launch { send(activeToken, activeUserId) } }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = wideContentEdgePadding(),
                        end = wideContentEdgePadding(),
                        top = if (wide) 0.dp else SettingsHeroTotalHeight,
                        bottom = 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (wide) {
                        item(key = "hero", contentType = "hero") {
                            SettingsHeroHeader(
                                title = fallback,
                                subtitle = "Private messages",
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                    item { SettingsSectionHeading("Before you start") }
                    item {
                        SettingsOptionCard {
                            when {
                                !OnlineAccount.configured -> SettingsOptionInfoRow(
                                    CurioIcons.Warning,
                                    "Not set up in this build",
                                    "This build has no Curio online project, so messages can't load."
                                )
                                !account.signedIn -> {
                                    SettingsOptionInfoRow(
                                        CurioIcons.Info,
                                        "Sign in to message",
                                        "Messages are tied to a Curio account."
                                    )
                                    SettingsOptionRow(
                                        icon = CurioIcons.Person,
                                        title = "Sign in or create an account",
                                        subtitle = "Settings → Online mode",
                                        onClick = { navController.navigate(CurioRoutes.SETTINGS_ONLINE) }
                                    )
                                }
                                else -> {
                                    SettingsOptionInfoRow(
                                        CurioIcons.Info,
                                        "Online mode is off",
                                        "Nothing loads while Online mode is off."
                                    )
                                    SettingsOptionRow(
                                        icon = CurioIcons.Refresh,
                                        title = "Turn Online mode on",
                                        subtitle = "Settings → Online mode",
                                        onClick = { navController.navigate(CurioRoutes.SETTINGS_ONLINE) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // An encrypted send that could not be delivered raises THIS instead of
        // a dead error line: the reason is stated, the one-tap way out (turn
        // the shared mode off for both, send as normal text) is offered, and
        // the honest status of the feature is named once, in full.
        encryptionIssue?.let { reason ->
            SocialConfirmDialog(
                title = "Encrypted message didn't send",
                body = "$reason\n\nEncryption only works while you are both on a version that " +
                    "supports it. You can send this message with encryption turned off for " +
                    "this chat instead — either of you can switch it back on later. " +
                    "Encrypted messages are experimental and may be changed or withdrawn.",
                confirmLabel = "Send without encryption",
                destructive = false,
                busy = sending,
                onDismiss = { if (!sending) encryptionIssue = null },
                onConfirm = {
                    // The dialog lives OUTSIDE the eligible branch, so the
                    // session values are read from the screen's own state
                    // here; without a session there is nothing to send.
                    val active = token
                    val me = myUserId
                    if (active == null || me == null) {
                        encryptionIssue = null
                        return@SocialConfirmDialog
                    }
                    scope.launch {
                        pending = pending.filterNot { it.id.startsWith(LOCAL_ID_PREFIX) }
                        draft = failedDraft
                        failedDraft = ""
                        encryptionIssue = null
                        send(active, me, forcePlaintext = true)
                    }
                }
            )
        }

        if (!wide) {
            // The conversation's hero IS the peer: their portrait rides in the
            // title slot and the @username (or a live Typing… line) rides as
            // the subtitle, so the person you are writing to is named at the
            // top — not stated as a standing label with the person listed
            // below the fold.
            SettingsHeroHeader(
                title = person?.label ?: fallback,
                subtitle = if (peerTyping) "Typing…" else person?.handleLabel.orEmpty(),
                onBack = { navController.popBackStack() },
                glassBackdrop = glassBackdrop,
                // The person LEADS the header — avatar first, then the name,
                // exactly like a messenger. The old titleTrailing slot put
                // them on the right edge, past the (empty) title.
                titleLeading = { ink ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable {
                                navController.navigate(CurioRoutes.socialProfile(otherUserId)) {
                                    launchSingleTop = true
                                }
                            }
                            .padding(end = 2.dp)
                    ) {
                        SocialAvatar(
                            style = person?.avatarStyle ?: 0,
                            avatarSize = 38.dp,
                            online = person?.isActiveNow == true
                        )
                    }
                }
            )
        }
    }
}

/**
 * Walks a conversation once and hands each row its framing: the day rule it
 * opens, and whether it starts or ends a run from one person.
 *
 * Grouping is what makes a long thread readable — without it every line wears
 * its own timestamp and portrait and the conversation reads as a log rather
 * than as people talking. A run breaks on a change of sender, a gap of more
 * than five minutes, or a new day.
 */
private fun androidx.compose.foundation.lazy.LazyListScope.itemsIndexedWithDays(
    thread: List<CurioDirectMessage>,
    row: @Composable (index: Int, message: CurioDirectMessage, dayLabel: String?, first: Boolean, last: Boolean) -> Unit
) {
    items(thread.size, key = { thread[it].id }) { index ->
        val message = thread[index]
        val previous = thread.getOrNull(index - 1)
        val next = thread.getOrNull(index + 1)
        val newDay = previous == null ||
            socialDayLabel(previous.createdAtMillis) != socialDayLabel(message.createdAtMillis)
        val sameAsPrevious = !newDay && previous != null &&
            previous.mine == message.mine &&
            message.createdAtMillis - previous.createdAtMillis <= 5 * 60 * 1000
        val sameAsNext = next != null &&
            socialDayLabel(next.createdAtMillis) == socialDayLabel(message.createdAtMillis) &&
            next.mine == message.mine &&
            next.createdAtMillis - message.createdAtMillis <= 5 * 60 * 1000
        row(
            index,
            message,
            if (newDay) socialDayLabel(message.createdAtMillis) else null,
            !sameAsPrevious,
            !sameAsNext
        )
    }
}

/**
 * Who you are talking to: the portrait, the live username and a tap that opens
 * their profile. When they are writing the @username gives way to a live
 * "Typing…" line, so the header answers the question the thread is about to
 * ask without any standing paragraph about it.
 */
@Composable
private fun MessagePeerHeader(
    person: CurioPerson?,
    fallback: String,
    typing: Boolean,
    activityAtMillis: Long?,
    onOpenProfile: () -> Unit
) {
    val dark = isCurioDarkTheme()
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = if (dark) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f)
        else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.72f),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.10f),
                shape = RoundedCornerShape(22.dp)
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenProfile)
                .padding(14.dp)
        ) {
            SocialAvatar(
                style = person?.avatarStyle ?: 0,
                avatarSize = 46.dp,
                // The dot and the presence line under the name are the SAME
                // fact, so they can never disagree.
                online = person?.isActiveNow == true
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = person?.label ?: fallback,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                if (typing) {
                    Text(
                        text = "Typing…",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = curioDialogActionColor()
                    )
                } else {
                    // The @username, and — only when the other member left
                    // activity visible — a quiet presence line beside it.
                    Text(
text = listOfNotNull(
                            person?.handleLabel,
                            activityAtMillis?.let { sentAt ->
                                val ageMinutes = ((System.currentTimeMillis() - sentAt).coerceAtLeast(0L) / 60_000L)
                                when {
                                    ageMinutes < 5L -> "Active now"
                                    ageMinutes < 60L -> "Active ${ageMinutes}m ago"
                                    ageMinutes < 24L * 60L -> "Active ${ageMinutes / 60L}h ago"
                                    else -> null
                                }
                            }
                        ).joinToString(" · ").ifBlank { "Open profile" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            CurioIcon(
                name = CurioIcons.ChevronRight,
                contentDescription = "Open profile",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                size = 18.dp
            )
        }
    }
}

/**
 * One message, its framing and — when tapped — the reaction palette under it.
 *
 * [animateIn] is set only for messages this device just sent, so a bubble
 * a user wrote arrives on a spring while history arrives still.
 */
@Composable
private fun MessageEntry(
    message: CurioDirectMessage,
    firstOfRun: Boolean,
    lastOfRun: Boolean,
    receipt: Long?,
    actionSheet: Boolean,
    reactions: List<CurioDmReaction>,
    myUserId: String,
    onDoubleTap: () -> Unit,
    onHold: () -> Unit,
    onSwipeReply: () -> Unit,
    onReply: () -> Unit,
    onPick: (String) -> Unit,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onRemove: (() -> Unit)?,
    quoteOf: String?,
    animateIn: Boolean
) {
    // `initial = !animateIn` is what makes this safe to use for EVERY row: a
    // historic message starts already visible (no animation at all), while a
    // message this device just sent starts hidden and springs in. A row never
    // animates OUT — the optimistic bubble is replaced by the server's copy of
    // the same text in the same frame, and an exit animation there would read
    // as the message being taken away.
    val visible = remember {
        MutableTransitionState(!animateIn).apply { if (animateIn) targetState = true }
    }
    AnimatedVisibility(
        visibleState = visible,
        enter = slideInVertically(initialOffsetY = { it / 2 }) +
            fadeIn(animationSpec = tween(CurioMotion.Durations.Quick)),
        exit = ExitTransition.None
    ) {
        // The action sheet floats ABOVE the bubble (Instagram-style).
        // A full-width invisible tap target sits above it so tapping
        // anywhere outside the pills dismisses the sheet.
        Box(modifier = Modifier.fillMaxWidth()) {
            if (actionSheet) {
                Popup(
                    alignment = if (message.mine) Alignment.TopEnd else Alignment.TopStart,
                    offset = IntOffset(0, with(LocalDensity.current) { (-96).dp.roundToPx() }),
                    properties = PopupProperties(focusable = true, dismissOnClickOutside = true),
                    onDismissRequest = onHold
                ) {
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(animationSpec = tween(CurioMotion.Durations.Quick)) +
                            slideInVertically { -it / 3 }
                    ) {
                        MessageActionSheet(
                            mine = message.mine,
                            current = reactions.firstOrNull { it.userId == myUserId }?.kind,
                            canEdit = message.mine && message.migrationState == "plaintext" &&
                                !message.id.startsWith(LOCAL_ID_PREFIX),
                            canRemove = onRemove != null,
                            onPick = onPick,
                            onCopy = onCopy,
                            onEdit = onEdit,
                            onRemove = onRemove,
                            onReply = onReply
                        )
                    }
                }
            }
            MessageBubble(
                message = message,
                firstOfRun = firstOfRun,
                lastOfRun = lastOfRun,
                receipt = receipt,
                reactions = reactions,
                myUserId = myUserId,
                onDoubleTap = onDoubleTap,
                onHold = onHold,
                onSwipeReply = onSwipeReply,
                quoteOf = quoteOf
            )
        }
    }
}

/**
 * One message. Yours sits on the right in the brand rose, theirs on the left
 * on the raised surface — the reading direction of every messenger, so who
 * said what needs no label. Corners open up on the first line of a run and
 * only the last line of a run gets the full rounding and the timestamp.
 * Both fills are OPAQUE: a translucent bubble let the background bleed
 * through and read as unfinished.
 *
 * Gestures: a DOUBLE-TAP is the heart (Instagram's), a HOLD raises the
 * floating action sheet (reactions, copy, edit, remove), and a horizontal
 * SWIPE answers the message — the bubble leans with the finger and snaps
 * back when the reply is armed.
 */
@Composable
private fun MessageBubble(
    message: CurioDirectMessage,
    firstOfRun: Boolean,
    lastOfRun: Boolean,
    receipt: Long?,
    reactions: List<CurioDmReaction>,
    myUserId: String,
    onDoubleTap: () -> Unit,
    onHold: () -> Unit,
    onSwipeReply: () -> Unit,
    quoteOf: String?
) {
    val mine = message.mine
    val dark = isCurioDarkTheme()
    val haptics = LocalHapticFeedback.current
    val mineGlyph = reactions.firstOrNull { it.userId == myUserId }?.kind
    val others = reactions.filterNot { it.userId == myUserId }
    val shape = if (mine) {
        RoundedCornerShape(
            topStart = 20.dp,
            topEnd = if (firstOfRun) 20.dp else 7.dp,
            bottomStart = 7.dp,
            bottomEnd = if (lastOfRun) 20.dp else 7.dp
        )
    } else {
        RoundedCornerShape(
            topStart = if (firstOfRun) 20.dp else 7.dp,
            topEnd = 20.dp,
            bottomStart = if (lastOfRun) 20.dp else 7.dp,
            bottomEnd = 7.dp
        )
    }

    // The swipe-to-answer gesture: the bubble follows the finger up to a
    // short travel, then the reply banner raises. The lean tracks the finger
    // exactly while dragging (snap) and springs back to rest on release.
    var replyDrag by remember { mutableStateOf(0f) }
    val dragLimit = with(LocalDensity.current) { 44.dp.toPx() }
    val settle = animateFloatAsState(
        targetValue = replyDrag,
        animationSpec = if (replyDrag == 0f) spring(dampingRatio = 0.6f, stiffness = 500f) else snap(),
        label = "replySettle"
    )
    // The bubble follows the finger in the same direction for both sides;
    // only the allowed drag direction differs between sent and received rows.
    val leanX = settle.value

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!mine) {
            Spacer(modifier = Modifier.weight(1f, fill = true))
        }

        Column(
            horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
            modifier = Modifier.weight(0f, fill = false)
        ) {
            val press = rememberCurioPressSource(pressedScale = 0.96f)
            Box(modifier = Modifier.offset { IntOffset(leanX.roundToInt(), 0) }) {
                Surface(
                    shape = shape,
                    color = when {
                        // Mine: the brand rose, both themes — no more dark
                        // bubble that read as the other person's.
                        mine -> curioDialogActionColor()
                        // Theirs: a raised neutral, clearly not the accent.
                        dark -> MaterialTheme.colorScheme.surfaceContainerHighest
                        else -> MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .then(press.modifier)
                        .pointerInput(message.id, mine) {
                            detectDragGestures(
                                onDragStart = { replyDrag = 0f },
                                onDragEnd = {
                                    if (kotlin.math.abs(replyDrag) >= dragLimit * 0.85f) {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onSwipeReply()
                                    }
                                    replyDrag = 0f
                                },
                                onDragCancel = { replyDrag = 0f }
                            ) { change, amount ->
                                change.consume()
                                val raw = replyDrag + amount.x * 0.45f
                                replyDrag = if (mine) raw.coerceIn(-dragLimit, 0f)
                                else raw.coerceIn(0f, dragLimit)
                            }
                        }
                ) {
                    Column(
                        modifier = Modifier
                            .combinedClickable(
                                interactionSource = press.interactionSource,
                                indication = LocalIndication.current,
                                onClickLabel = "Message options",
                                onLongClickLabel = "Message options",
                                // The HOLD is the message's decision point: it
                                // raises the floating sheet (reactions, copy,
                                // edit, remove). A second hold drops it.
                                onLongClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onHold()
                                },
                                onDoubleClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onDoubleTap()
                                },
                                onClick = {}                            )
                            .padding(horizontal = 14.dp, vertical = 9.dp)) {
                        if (quoteOf != null) {
                            ReplyQuoteRow(quoteOf)
                        }
                        Text(
                            text = message.body,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (mine) Color.White
                            else MaterialTheme.colorScheme.onSurface
                        )
                        if (message.editedAtMillis != null) {
                            Text(
                                text = "edited",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (mine) Color.White.copy(alpha = 0.7f)
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                        if (mine && lastOfRun) {
                            Text(
                                text = if (receipt != null) "\u2713\u2713" else "\u2713",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 8.sp,
                                    lineHeight = 8.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (receipt != null) Color.White.copy(alpha = 0.86f)
                                else Color.White.copy(alpha = 0.62f),
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }
                }
            }

            // The reaction chips sit OUTSIDE the bubble, on its tail corner, so
            // the bubble keeps its own shape and a reaction never reflows the
            // text it is attached to.
            if (mineGlyph != null || others.isNotEmpty()) {
                ReactionChips(mineGlyph = mineGlyph, others = others)
            }
        }

        if (mine && lastOfRun) {
            Text(
                text = socialStamp(message.createdAtMillis),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 6.dp, bottom = 2.dp)
            )
        }

        if (!mine && lastOfRun) {
            Text(
                text = socialStamp(message.createdAtMillis),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 6.dp, bottom = 2.dp)
            )
        }
    }
}

/** The reaction chips already on a message — mine first, then everyone else's. */
@Composable
private fun ReactionChips(mineGlyph: String?, others: List<CurioDmReaction>) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 3.dp)
    ) {
        if (mineGlyph != null) {
            ReactionChip(kind = mineGlyph, count = 1, mine = true)
        }
        others.groupBy { it.kind }.forEach { (kind, group) ->
            ReactionChip(kind = kind, count = group.size, mine = false)
        }
    }
}

@Composable
private fun ReactionChip(kind: String, count: Int, mine: Boolean) {
    val ink = if (mine) curioDialogActionColor() else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        shape = RoundedCornerShape(50),
        color = if (mine) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = SocialReactions.emojiFor(kind),
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
                color = ink
            )
            if (count > 1) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = ink
                )
            }
        }
    }
}

/**
 * The quick quote above an answer: the parent's words, one soft bar. Instagram
 * renders this INSIDE the bubble and Curio does too — the bar is deliberately
 * quiet so the answer stays the loudest line.
 */
@Composable
private fun ReplyQuoteRow(quoted: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(34.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.55f))
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = quoted,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.85f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * The Instagram-style action bar: floating pills attached to the held bubble —
 * the emoji palette first, then Copy, Edit (mine, plaintext), Remove (mine)
 * and Reply. No dialog and no scrim: the thread stays readable, and a second
 * hold anywhere drops the bar.
 */
@Composable
private fun MessageActionSheet(
    mine: Boolean,
    current: String?,
    canEdit: Boolean,
    canRemove: Boolean,
    onPick: (String) -> Unit,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onRemove: (() -> Unit)?,
    onReply: () -> Unit
) {
    val visible = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.9f),
        exit = ExitTransition.None
    ) {
        Column(
            horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
            modifier = Modifier.padding(vertical = 2.dp)
        ) {
            // Row 1: emoji reaction palette — a frosted pill with reaction
            // emojis. Tapping the active one clears it (toggle).
            Surface(
                shape = RoundedCornerShape(50),
                color = curioDialogContainerColor(),
                shadowElevation = 4.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(1.dp),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    SocialReactions.PALETTE.forEach { (emoji, meaning) ->
                        val chosen = current != null && SocialReactions.emojiFor(current) == emoji
                        Surface(
                            onClick = { onPick(emoji) },
                            shape = CircleShape,
                            color = if (chosen) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else Color.Transparent,
                            modifier = Modifier
                                .size(36.dp)
                                .semantics { contentDescription = meaning }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = emoji,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            // Row 2: text action chips — Reply, Copy, Edit, Remove. Each is
            // its own frosted pill so the destructive Remove stands out.
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                ActionChip("Reply", onReply)
                ActionChip("Copy", onCopy)
                if (canEdit) ActionChip("Edit", onEdit)
                if (canRemove) ActionChip("Remove", { onRemove?.invoke() }, destructive = true)
            }
        }
    }
}

@Composable
private fun ActionChip(label: String, onClick: () -> Unit, destructive: Boolean = false) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (destructive)
            MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
        else
            curioDialogContainerColor(),
        shadowElevation = 3.dp
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (destructive) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}

/** Three breathing dots — the other person is writing, shown in the thread. */
@Composable
private fun TypingBubble(person: CurioPerson?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SocialAvatar(style = person?.avatarStyle ?: 0, avatarSize = 26.dp)
        Spacer(Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(topStart = 7.dp, topEnd = 18.dp, bottomStart = 7.dp, bottomEnd = 18.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                (0..2).forEach { dot -> TypingDot(delayMillis = dot * 160L) }
            }
        }
    }
}

/** One dot of [TypingBubble], pulsing on its own offset so the row breathes. */
@Composable
private fun TypingDot(delayMillis: Long) {
    var up by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMillis)
        while (true) {
            up = true
            delay(420)
            up = false
            delay(420)
        }
    }
    val lift by animateFloatAsState(
        targetValue = if (up) 1f else 0.55f,
        animationSpec = tween(400),
        label = "typingDot"
    )
    Box(
        modifier = Modifier
            .size(6.dp)
            .scale(lift)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f + 0.5f * lift))
    )
}

/**
 * The composer: a frosted pill you write in and one circular accent button
 * that sends. The send control does not appear — it grows the moment there is
 * something to send, which is the feedback that says "this is ready".
 */
@Composable
private fun MessageComposer(
  modifier: Modifier = Modifier,
  draft: String,
    title: String,
    sending: Boolean,
    /** When set, the composer is EDITING this message: Save replaces Send. */
    editTarget: CurioDirectMessage?,
    /** When set, the next send ANSWERS this message: the quote banner rides. */
    quoteTarget: CurioDirectMessage?,
    onDropEdit: () -> Unit,
    onDropQuote: () -> Unit,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit
) {
    val dark = isCurioDarkTheme()
    // A direct message is deliberately NOT run through CurioContentFilter —
    // this is a private conversation between two friends, and the filter
    // guards the public surfaces instead. See SocialApi.send.
    val armed = draft.isNotBlank() && !sending
    val sendScale by animateFloatAsState(
        targetValue = if (armed) 1f else 0.86f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 900f),
        label = "sendScale"
    )
    val nearLimit = draft.length > SocialApi.MAX_MESSAGE_CHARS * 8 / 10

  Column(
  modifier = modifier
  .fillMaxWidth()
  // The keyboard sits ON TOP of the composer otherwise: the app is
  // edge-to-edge (`setDecorFitsSystemWindows(false)`) and the NavHost only
  // delivers the navigation-bar inset, never the IME one. This lifts the pill
  // clear of the keyboard the moment it opens and returns it to the gesture
  // bar when it closes.
  .imePadding()
  .padding(
                start = wideContentEdgePadding(),
                end = wideContentEdgePadding(),
                bottom = 10.dp
            ),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // The REPLY banner: the message being answered, with a focused tint
        // so it reads as "on the record" rather than as decoration.
        androidx.compose.animation.AnimatedVisibility(visible = quoteTarget != null) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(26.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(curioDialogActionColor())
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Replying to ${quoteTarget?.let { if (it.mine) "yourself" else title }}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = curioDialogActionColor(),
                            maxLines = 1
                        )
                        Text(
                            text = quoteTarget?.body.orEmpty(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.labelMedium,
                        color = curioDialogActionColor(),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onDropQuote)
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
        }
        // The edit banner: what is being changed, and the way out. It sits
        // above the field so the composer's own height never jumps.
        androidx.compose.animation.AnimatedVisibility(visible = editTarget != null) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    CurioIcon(
                        name = CurioIcons.Edit,
                        contentDescription = null,
                        tint = curioDialogActionColor(),
                        size = 14.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Editing message",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.labelMedium,
                        color = curioDialogActionColor(),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onDropEdit)
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
        }
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        if (dark) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.75f)
                        else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f)
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(26.dp)
                    )
                    .padding(horizontal = 16.dp)
            ) {
                BasicTextField(
                    value = draft,
                    onValueChange = onDraftChange,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
cursorBrush = SolidColor(curioDialogActionColor()),
                singleLine = true,
                // A message is a sentence, not a word: capitals and
                    // sentence punctuation are the default here (the field is
                    // unlabelled, so this is the only cue it needs).
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Send
                    ),
                    modifier = Modifier.weight(1f)
                ) { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (draft.isEmpty()) {
                            Text(
                                text = "Message $title",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                        inner()
                    }
                }
            }
            Surface(
                onClick = { if (armed) onSend() },
                shape = CircleShape,
                color = if (armed) curioDialogActionColor()
                else MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .size(52.dp)
                    .scale(sendScale)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    CurioIcon(
                        // An edit in flight turns the arrow into a check: the
                        // same button, a different verb.
                        name = if (editTarget != null) CurioIcons.Check else CurioIcons.ArrowForward,
                        contentDescription = if (editTarget != null) "Save edit" else "Send",
                        tint = if (armed) androidx.compose.ui.graphics.Color.White
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        size = 20.dp
                    )
                }
            }
        }
        if (nearLimit) {
            Text(
                text = "${draft.length}/${SocialApi.MAX_MESSAGE_CHARS}",
                style = MaterialTheme.typography.labelSmall,
                color = if (draft.length >= SocialApi.MAX_MESSAGE_CHARS) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.padding(start = 6.dp)
            )
        }
    }
}

/**
 * How often an OPEN conversation asks whether anything new arrived.
 *
 * 1.2s is deliberately tighter than a "poll every few seconds" cadence: each
 * tick asks only for messages newer than the newest one on screen, so it is a
 * few hundred bytes, and the point of the live thread is that a reply lands
 * while you are looking at it rather than a beat later. The very first tick
 * after an arrival is also what turns the typing row off and stamps the read
 * receipt, so the whole exchange moves at this cadence.
 */
private const val LIVE_TICK_MS = 1_200L

/**
 * The safety-net cadence used once realtime is linked: a missed frame or a
 * socket that died without saying so is still picked up, but the timer is no
 * longer the thing that makes the thread feel live.
 */
private const val SAFETY_TICK_MS = 20_000L

/**
 * How often the "is typing…" row is re-read when realtime is NOT linked.
 *
 * 1.5s is the old whole mechanism: the row is one tiny read of a single
 * server-stamped timestamp, and an indicator that arrives after the message it
 * was announcing is worse than none. Once the push channel is linked a
 * `dm_typing` frame shows it immediately, so this becomes a slow safety tick —
 * it also has to clear a writer that vanished without deleting its row.
 */
private const val TYPING_TICK_MS = 1_500L

/** The typing row's safety-net cadence once realtime is linked. */
private const val TYPING_SAFETY_TICK_MS = 5_000L

/** The id prefix of a bubble that is sent but not yet confirmed. */
private const val LOCAL_ID_PREFIX = "local-"
