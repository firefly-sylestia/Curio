package com.curio.app.features.community

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.curio.app.data.AppPreferences
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
import com.curio.app.data.supabase.CurioDirectMessage
import com.curio.app.data.supabase.OnlineAccount
import com.curio.app.data.supabase.SocialApi
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
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.curioDialogActionButtonColors
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.launch

/**
 * A DIRECT CONVERSATION — one thread with one friend.
 *
 * Private by construction: `dm_messages` gives its two participants the only
 * read policy, and the INSERT policy additionally requires an ACCEPTED friend
 * request, so a stranger cannot be messaged even with the right id. Text only —
 * there is no media column to fill.
 *
 * There is no realtime channel yet: the thread loads on entry and after every
 * send, which is honest about what it is — a message arrives when you come back
 * to the conversation.
 */
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
    var messages by remember { mutableStateOf<List<CurioDirectMessage>>(emptyList()) }
    var draft by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { OnlineAccount.restore(context) }

    val token = account.session?.accessToken
    val myUserId = account.session?.userId
    val eligible = account.signedIn && onlineMode && token != null && myUserId != null

    suspend fun load(active: String, me: String) {
        loading = true
        SocialApi.messages(active, otherUserId, me).fold(
            onSuccess = {
                messages = it
                error = null
            },
            onFailure = { error = it.message }
        )
        loading = false
    }

    suspend fun send(active: String, me: String) {
        val text = draft.trim()
        if (text.isEmpty() || sending) return
        sending = true
        SocialApi.send(active, otherUserId, text, me).fold(
            onSuccess = {
                draft = ""
                error = null
                load(active, me)
            },
            onFailure = { error = it.message }
        )
        sending = false
    }

    LaunchedEffect(eligible, token, myUserId) {
        if (!eligible || token == null || myUserId == null) {
            messages = emptyList()
            return@LaunchedEffect
        }
        load(token, myUserId)
        // A receipt is a courtesy, not a requirement: a failure here must never
        // blank a thread that loaded fine.
        SocialApi.markRead(token, otherUserId, myUserId)
    }

    // Keep the newest line in view — on open and after every sent message.
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    val title = handle.ifBlank { "Message" }

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

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(
                        start = wideContentEdgePadding(),
                        end = wideContentEdgePadding(),
                            top = if (wide) 0.dp else 142.dp,
                        bottom = 12.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (wide) {
                        item(key = "hero", contentType = "hero") {
                    SocialPixelHeader(
                                title = title,
                                subtitle = "Private messages",
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }

                    error?.let { message -> item { SocialNote(message, true) } }

                    if (messages.isEmpty() && !loading) {
                        item {
                            SettingsOptionCard {
                                SettingsOptionInfoRow(
                                    CurioIcons.Notes,
                                    "No messages yet",
                                    "Say hello — this conversation is just the two of you."
                                )
                            }
                        }
                    }

                    items(messages, key = { it.id }) { message ->
                        MessageBubble(message)
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .padding(
                            start = wideContentEdgePadding(),
                            end = wideContentEdgePadding(),
                            bottom = 10.dp +
                                WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                        )
                ) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { if (it.length <= SocialApi.MAX_MESSAGE_CHARS) draft = it },
                        minLines = 1,
                        maxLines = 4,
                        enabled = !sending,
                        placeholder = { Text("Message $title") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        shape = RoundedCornerShape(22.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (sending) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            text = "${draft.length}/${SocialApi.MAX_MESSAGE_CHARS}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.weight(1f))
                        Button(
                            onClick = { scope.launch { send(activeToken, activeUserId) } },
                            enabled = draft.isNotBlank() && !sending,
                            shape = RoundedCornerShape(50),
                            colors = curioDialogActionButtonColors()
                        ) {
                            Text(
                                text = "Send",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = wideContentEdgePadding(),
                        end = wideContentEdgePadding(),
                        top = if (wide) 0.dp else 142.dp,
                        bottom = 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (wide) {
                        item(key = "hero", contentType = "hero") {
                            SocialPixelHeader(
                                title = title,
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

        if (!wide) {
            SocialPixelHeader(
                title = title,
                subtitle = "Private messages",
                onBack = { navController.popBackStack() }
            )
        }
    }
}

/**
 * One message. Yours sits on the right in the accent container, theirs on the
 * left on the elevated surface — the reading direction of every messenger, so
 * who said what needs no label.
 */
@Composable
private fun MessageBubble(message: CurioDirectMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.mine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (message.mine) {
            Text(
                text = socialStamp(message.createdAtMillis),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 6.dp, bottom = 4.dp)
            )
        }
        Box(
            modifier = Modifier
                .weight(1f, fill = false)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (message.mine) 18.dp else 4.dp,
                        bottomEnd = if (message.mine) 4.dp else 18.dp
                    )
                )
                .background(
                    if (message.mine) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerHigh
                )
                .padding(horizontal = 14.dp, vertical = 9.dp)
        ) {
            Text(
                text = message.body,
                style = MaterialTheme.typography.bodyMedium,
                color = if (message.mine) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface
            )
        }
        if (!message.mine) {
            Text(
                text = socialStamp(message.createdAtMillis),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 6.dp, bottom = 4.dp)
            )
        }
    }
}
