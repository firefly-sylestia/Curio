package com.curio.app.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.curio.app.data.AppPreferences
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
import com.curio.app.data.supabase.OnlineAccount
import com.curio.app.navigation.CurioRoutes
import com.curio.app.ui.adaptive.isWide
import com.curio.app.ui.adaptive.wideContentEdgePadding
import com.curio.app.ui.adaptive.windowWidthSizeClass
import com.curio.app.ui.components.CurioWatermarkBackdrop
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.curioDialogActionButtonColors
import com.curio.app.ui.theme.curioDialogActionColor
import com.curio.app.ui.theme.isCurioDarkTheme
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

    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var revealPassword by rememberSaveable { mutableStateOf(false) }
    var onlineMode by remember { mutableStateOf(AppPreferences.isOnlineModeEnabled(context)) }

    // A returning user is already signed in; a stored session is the only
    // thing that makes Online Mode available.
    LaunchedEffect(Unit) { OnlineAccount.restore(context) }
    // Sign in turns the pref on and sign out turns it off, so the switch
    // follows the account instead of drifting from it.
    LaunchedEffect(account.session) { onlineMode = AppPreferences.isOnlineModeEnabled(context) }

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
                            SettingsOptionInfoRow(
                                CurioIcons.Person,
                                account.email ?: "Signed in",
                                "Curio account"
                            )
                            SettingsOptionDivider()
                            SettingsOptionRow(
                                icon = CurioIcons.Close,
                                title = "Sign out",
                                subtitle = "Your captures stay on this device",
                                onClick = { scope.launch { OnlineAccount.signOut(context) } }
                            )
                        } else {
                            OnlineAuthField(
                                placeholder = "Email",
                                value = email,
                                enabled = !account.busy,
                                isPassword = false,
                                revealed = false,
                                onValueChange = { email = it }
                            )
                            SettingsOptionDivider()
                            OnlineAuthField(
                                placeholder = "Password",
                                value = password,
                                enabled = !account.busy,
                                isPassword = true,
                                revealed = revealPassword,
                                revealLabel = if (revealPassword) "Hide password" else "Show password",
                                onToggleReveal = { revealPassword = !revealPassword },
                                onValueChange = { password = it }
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp, bottom = 10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            if (OnlineAccount.signIn(context, email, password)) {
                                                password = ""
                                            }
                                        }
                                    },
                                    enabled = !account.busy,
                                    shape = RoundedCornerShape(50),
                                    colors = curioDialogActionButtonColors(),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = if (account.busy) "Signing in…" else "Sign in",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        scope.launch { OnlineAccount.signUp(context, email, password) }
                                    },
                                    enabled = !account.busy,
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = curioDialogActionColor()
                                    )
                                ) {
                                    Text(
                                        text = "Create account",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }
                            }
                            account.error?.let { message ->
                                Text(
                                    text = message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(bottom = 10.dp)
                                )
                            }
                            account.notice?.let { message ->
                                Text(
                                    text = message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 10.dp)
                                )
                            }
                        }
                    }
                }
            }

            item { SettingsSectionHeading("Community") }
            item {
                SettingsOptionCard {
                    SettingsOptionRow(
                        icon = CurioIcons.Share,
                        title = "Community",
                        subtitle = "Text cards from the last 24 hours",
                        onClick = { navController.navigate(CurioRoutes.COMMUNITY) }
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
                            onlineMode = wanted
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
}

/**
 * One frosted account field — the settings family's search-field surface
 * (frosted fill, hairline, 17dp rounding), with an optional reveal toggle
 * for the password. The placeholder is the field's only label: the form is
 * two fields, so a caption would be filler.
 */
@Composable
private fun OnlineAuthField(
    placeholder: String,
    value: String,
    enabled: Boolean,
    isPassword: Boolean,
    revealed: Boolean,
    revealLabel: String? = null,
    onToggleReveal: (() -> Unit)? = null,
    onValueChange: (String) -> Unit
) {
    val dark = isCurioDarkTheme()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .height(50.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(
                if (dark) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f)
                else Color.White.copy(alpha = 0.70f)
            )
            .border(
                1.dp,
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f),
                RoundedCornerShape(17.dp)
            )
            .padding(horizontal = 14.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            visualTransformation = if (isPassword && !revealed) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier.weight(1f)
        ) { inner ->
            Box {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                inner()
            }
        }
        if (onToggleReveal != null) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onToggleReveal),
                contentAlignment = Alignment.Center
            ) {
                CurioIcon(
                    name = CurioIcons.VisibilityOff,
                    contentDescription = revealLabel,
                    tint = if (revealed) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    size = 18.dp
                )
            }
        }
    }
}
