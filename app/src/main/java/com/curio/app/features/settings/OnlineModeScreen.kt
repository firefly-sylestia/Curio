package com.curio.app.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.curio.app.data.supabase.OnlineAccount
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
 * ONLINE MODE — the account page for Curio's online layer.
 *
 * Signed out it is the email/password form (sign in + create account);
 * signed in it shows the account and the Online Mode switch. The switch is
 * the contract the rest of the online work hangs off: with it off, Curio
 * behaves exactly like the local-only app, and Room stays the source of
 * truth for everything.
 *
 * Text only by design — captures' media never leaves the device, which is
 * what the switch's own copy states.
 */
@Composable
fun OnlineModeScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val account = OnlineAccount.state
    val wide = windowWidthSizeClass().isWide
    val listState = rememberLazyListState()
    val glassBackdrop = rememberLayerBackdrop()
    // Signing out drops the session and takes Online mode with it, so it asks
    // first instead of firing on the tap.
    var confirmingSignOut by remember { mutableStateOf(false) }
    var signingOut by remember { mutableStateOf(false) }
    // The switch reads the OBSERVABLE mirror (seeded in AppPreferences
    // .initThemeMode), so signing in / out — which flips the same pref through
    // OnlineAccount — moves it here too. Before it was a local copy synced off
    // the session, which could drift from what the nav chrome was reading.
    val onlineMode = AppPreferences.onlineModeEnabledState

    // A returning user is already signed in; a stored session is the only
    // thing that makes Online Mode available.
    LaunchedEffect(Unit) { OnlineAccount.restore(context) }

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
                        title = "Online mode",
                        subtitle = "Account and sync",
                        onBack = { navController.popBackStack() }
                    )
                }
            }
            item(key = "settings-nav", contentType = "settings-nav") {
                SettingsNavRail(
                    active = "online",
                    onSelect = { navigateToSettingsSection(navController, it) },
                    navController = navController
                )
            }

            item { SettingsSectionHeading("Account") }
            item {
                SettingsOptionCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (account.signedIn) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                CurioAccountIdentityCard(email = account.email)
                            }
                            SettingsOptionDivider()
                            SettingsOptionRow(
                                icon = CurioIcons.Close,
                                title = "Sign out",
                                subtitle = "Your captures stay on this device",
                                onClick = { confirmingSignOut = true }
                            )
                        } else {
                            Column(modifier = Modifier.padding(14.dp)) {
                                CurioAuthCard()
                            }
                        }
                    }
                }
            }

            item { SettingsSectionHeading("Social") }
            item {
                SettingsOptionCard {
                    // The tab is strictly opt-in and is the only Social control
                    // kept in Settings; the full Social and Friends experiences
                    // are available from the app navigation.
                    SettingsOptionSwitchRow(
                        icon = CurioIcons.Public,
                        title = "Social tab",
                        subtitle = if (onlineMode && account.signedIn) {
                            "Show Social in the app navigation."
                        } else {
                            "Turn on Online mode and sign in to show Social."
                        },
                        checked = AppPreferences.communityTabVisible,
                        enabled = onlineMode && account.signedIn,
                        onCheckedChange = { wanted ->
                            AppPreferences.setCommunityTabEnabled(context, wanted)
                        }
                    )
                }
            }

            item { SettingsSectionHeading("Sync") }
            item {
                SettingsOptionCard {
                    SettingsOptionSwitchRow(
                        icon = CurioIcons.Refresh,
                        title = "Online mode",
                        subtitle = if (account.signedIn) {
                            "Keeps your account and liked topics in sync. Text only — photos, audio and screenshots never leave this device."
                        } else {
                            "Sign in to turn sync on. Text only — photos, audio and screenshots never leave this device."
                        },
                        checked = onlineMode && account.signedIn,
                        enabled = account.signedIn && !account.busy,
                        onCheckedChange = { wanted ->
                            // No local copy to move: setOnlineMode writes the
                            // pref, which updates the observable state this
                            // switch reads (so the row, the tab and the wall
                            // can never disagree).
                            scope.launch { OnlineAccount.setOnlineMode(context, wanted) }
                        }
                    )
                    if (!OnlineAccount.configured) {
                        SettingsOptionDivider()
                        SettingsOptionInfoRow(
                            CurioIcons.Warning,
                            "Not set up in this build",
                            "No Curio online project is configured here, so sync stays off."
                        )
                    }
                }
            }
        }

        // The phone hero rides on TOP of the scroll content (rows slide under
        // the tear); wide windows scroll it as the list's first item instead.
        if (!wide) {
            SettingsHeroHeader(
                title = "Online mode",
                subtitle = "Account and sync",
                onBack = { navController.popBackStack() },
                glassBackdrop = glassBackdrop
            )
        }
    }

    if (confirmingSignOut) {
        SocialConfirmDialog(
            title = "Sign out of Curio?",
            body = "Online mode turns off and nothing online loads until you sign in again. " +
                "Your captures, recordings and conversations stay on this device.",
            confirmLabel = "Sign out",
            busy = signingOut,
            onDismiss = { if (!signingOut) confirmingSignOut = false },
            onConfirm = {
                signingOut = true
                scope.launch {
                    OnlineAccount.signOut(context)
                    signingOut = false
                    confirmingSignOut = false
                }
            }
        )
    }
}
