package com.curio.app.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.curio.app.data.AppPreferences
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
import com.curio.app.data.supabase.CurioPerson
import com.curio.app.data.supabase.OnlineAccount
import com.curio.app.data.supabase.PROFILE_VISIBILITY_FRIENDS
import com.curio.app.data.supabase.PROFILE_VISIBILITY_PUBLIC
import com.curio.app.data.supabase.SocialApi
import com.curio.app.data.supabase.SocialPresence
import com.curio.app.features.community.SocialConfirmDialog
import com.curio.app.ui.adaptive.isWide
import com.curio.app.ui.adaptive.wideContentEdgePadding
import com.curio.app.ui.adaptive.windowWidthSizeClass
import com.curio.app.ui.components.CurioWatermarkBackdrop
import com.curio.app.ui.theme.CurioIcons
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.launch

/**
 * PRIVACY — the member's own rules about who can see what.
 *
 * Three decisions, and each one is enforced where it actually matters:
 *
 *  - **Who sees your profile.** A friends-only profile is readable by accepted
 *    friends alone; the discoverable SELECT policy in `supabase/schema.sql`
 *    §5f is what refuses everyone else.
 *  - **Whether anything is drawn about your activity.** Hiding it clears the
 *    stamp on the server in the same write, so there is nothing to read — the
 *    promise is kept by absence, not by a flag somebody might forget to check.
 *  - **Who is blocked.** A block removes the pair from every reachable
 *    surface: cards, replies, likes, requests, messages and each other's
 *    profiles, in both directions.
 *
 * Every switch writes its LOCAL preference first, so the choice holds on a
 * device with no signal, and then mirrors it onto the profile row. The screen
 * says that plainly rather than pretending the network is always there.
 */
@Composable
fun PrivacyScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val account = OnlineAccount.state
    val wide = windowWidthSizeClass().isWide
    val listState = rememberLazyListState()
    val glassBackdrop = rememberLayerBackdrop()

    // The observable mirrors, so the switches move the instant they are tapped
    // and every other reader (the profile page, the presence tick) agrees.
    val visibility = AppPreferences.profileVisibilityState
    val hideActivity = AppPreferences.hideActivityState
    val token = account.session?.accessToken

    var blocked by remember { mutableStateOf<List<CurioPerson>>(emptyList()) }
    var unblockTarget by remember { mutableStateOf<CurioPerson?>(null) }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { OnlineAccount.restore(context) }

    suspend fun loadBlocked(active: String) {
        // Best-effort: a project that has not been re-pasted since §5f has no
        // `member_blocks`, and that is an empty list, not a broken page.
        SocialApi.blocks(active).onSuccess { blocked = it }
    }

    LaunchedEffect(token) { token?.let { loadBlocked(it) } }

    // The local preference is authoritative and lands immediately; the server
    // copy follows, and a failure is reported instead of silently dropped.
    fun applyPrivacy(nextVisibility: String, nextHide: Boolean) {
        AppPreferences.setProfileVisibility(context, nextVisibility)
        AppPreferences.setActivityHidden(context, nextHide)
        scope.launch {
            val active = token
            if (active == null) {
                notice = "Saved on this device. Sign in to apply it to your account."
                return@launch
            }
            SocialApi.updatePrivacy(active, nextVisibility, nextHide).fold(
                onSuccess = { notice = "Saved." },
                onFailure = {
                    notice = "Saved on this device — it will sync when you're back online."
                }
            )
            // Hiding activity takes the stamp away NOW; showing it again
            // publishes a fresh one so the line comes back straight away.
            if (nextHide) SocialPresence.withdraw(context) else SocialPresence.publish(context, force = true)
        }
    }

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

        LazyColumn(
            state = listState,
            modifier = Modifier
                .layerBackdrop(glassBackdrop)
                .fillMaxSize(),
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
                        title = "Privacy",
                        subtitle = "Who can see what",
                        onBack = { navController.popBackStack() }
                    )
                }
            }
            item(key = "settings-nav", contentType = "settings-nav") {
                SettingsNavRail(
                    active = "privacy",
                    onSelect = { navigateToSettingsSection(navController, it) },
                    navController = navController
                )
            }

            item { SettingsSectionHeading("Your profile") }
            item {
                SettingsOptionCard {
                    SettingsOptionSwitchRow(
                        icon = CurioIcons.Person,
                        title = "Friends-only profile",
                        subtitle = if (visibility == PROFILE_VISIBILITY_FRIENDS) {
                            "Only your friends can open your profile or find you by name."
                        } else {
                            "Any signed-in member with Online mode on can open your profile."
                        },
                        checked = visibility == PROFILE_VISIBILITY_FRIENDS,
                        onCheckedChange = { wanted ->
                            applyPrivacy(
                                if (wanted) PROFILE_VISIBILITY_FRIENDS else PROFILE_VISIBILITY_PUBLIC,
                                hideActivity
                            )
                        }
                    )
                    SettingsOptionDivider()
                    SettingsOptionInfoRow(
                        CurioIcons.Info,
                        "Your profile holds only your name and portrait",
                        "Cards, replies and messages are separate things that happen to " +
                            "carry your name — nothing else about you is part of it."
                    )
                }
            }

            item { SettingsSectionHeading("Activity") }
            item {
                SettingsOptionCard {
                    SettingsOptionSwitchRow(
                        icon = CurioIcons.VisibilityOff,
                        title = "Hide my activity",
                        subtitle = if (hideActivity) {
                            "Nothing is published about when you were last around."
                        } else {
                            "Your profile shows a quiet \"Active now\" line to people who open it."
                        },
                        checked = hideActivity,
                        onCheckedChange = { wanted ->
                            applyPrivacy(visibility, wanted)
                        }
                    )
                }
            }

            item { SettingsSectionHeading("Blocked people") }
            item {
                SettingsOptionCard {
                    if (blocked.isEmpty()) {
                        SettingsOptionInfoRow(
                            CurioIcons.Info,
                            "Nobody is blocked",
                            "Block someone from their profile. A block stops messages, " +
                                "requests and each other's cards, both ways."
                        )
                    } else {
                        blocked.forEachIndexed { index, person ->
                            if (index > 0) SettingsOptionDivider()
                            SettingsOptionRow(
                                icon = CurioIcons.Delete,
                                title = person.label,
                                subtitle = "Blocked · tap to unblock",
                                onClick = { unblockTarget = person }
                            )
                        }
                    }
                }
            }

            if (!account.signedIn) {
                item {
                    SettingsOptionCard {
                        SettingsOptionInfoRow(
                            CurioIcons.Warning,
                            "Sign in to apply these to your account",
                            "They are already saved on this device. Signing in mirrors them, " +
                                "and they are what other members' servers enforce."
                        )
                    }
                }
            }

            notice?.let { message ->
                item {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                }
            }

            item { SettingsSectionHeading("Always true") }
            item {
                SettingsOptionCard {
                    SettingsOptionInfoRow(
                        CurioIcons.Image,
                        "Photos, audio and recordings never leave this device",
                        "The online layer is text only — there is no field anywhere in the " +
                            "schema that could carry a capture."
                    )
                    SettingsOptionDivider()
                    SettingsOptionInfoRow(
                        CurioIcons.Notes,
                        "Messages are between the two people in the thread",
                        "They are stored so they can be delivered, and a block ends the " +
                            "conversation in both directions."
                    )
                }
            }
        }

        if (!wide) {
            SettingsHeroHeader(
                title = "Privacy",
                subtitle = "Who can see what",
                onBack = { navController.popBackStack() },
                glassBackdrop = glassBackdrop
            )
        }
    }

    unblockTarget?.let { person ->
        SocialConfirmDialog(
            title = "Unblock ${person.label}?",
            body = "They can message you and send a friend request again. Your conversation " +
                "stays on this device either way.",
            confirmLabel = "Unblock",
            busy = busy,
            onDismiss = { if (!busy) unblockTarget = null },
            onConfirm = {
                val active = token
                if (active == null) {
                    unblockTarget = null
                    return@SocialConfirmDialog
                }
                busy = true
                scope.launch {
                    SocialApi.unblock(active, person.userId).fold(
                        onSuccess = {
                            blocked = blocked.filterNot { it.userId == person.userId }
                            notice = "Unblocked ${person.label}."
                        },
                        onFailure = { notice = it.message ?: "Couldn't lift that block." }
                    )
                    busy = false
                    unblockTarget = null
                }
            }
        )
    }
}
