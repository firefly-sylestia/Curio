package com.curio.app.features.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.curio.app.data.AppPreferences
import com.curio.app.data.supabase.OnlineAccount
import com.curio.app.data.supabase.SOCIAL_AVATAR_STYLE_COUNT
import com.curio.app.data.supabase.SocialApi
import com.curio.app.features.community.AvatarPickerIcon
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.CurioMotion
import com.curio.app.ui.theme.curioDialogActionButtonColors
import com.curio.app.ui.theme.curioDialogActionColor
import com.curio.app.ui.theme.isCurioDarkTheme
import kotlinx.coroutines.launch

/**
 * The account surfaces, shared by Online mode (Settings) and Edit profile.
 *
 * They live here rather than in `OnlineModeScreen` because the account is now
 * reachable from two places, and the two must never disagree about what
 * "create account" means, what a username may contain, or what a taken
 * username says. One implementation, two doors.
 *
 * What is deliberately different from the old page:
 *  - Sign in and Create account are TWO MODES with one primary action, not one
 *    row of fields with a stray "create account" text button next to a
 *    "sign in" button. The mode decides the labels, the fields (create asks
 *    for the password twice) and the copy.
 *  - Every rule the user can break is stated BEFORE the network is called, and
 *    a server answer is always shown: a taken username says it is taken, a
 *    bad address says it is bad. The old flow swallowed username failures
 *    entirely, which is why saving appeared to do nothing.
 */
private enum class AuthMode { SIGN_IN, CREATE }

/**
 * The signed-out account form: mode switch, fields, one primary action and a
 * visible answer for both local validation and the server.
 */
@Composable
internal fun CurioAuthCard(
    /** Called after a successful sign-in or account creation. */
    onAuthenticated: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val account = OnlineAccount.state

    var mode by remember { mutableStateOf(AuthMode.SIGN_IN) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var reveal by remember { mutableStateOf(false) }
    // A local problem (bad address, short password, no match) OR the server's
    // own answer. Shown in one place so the user always sees the outcome.
    var localProblem by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }

    // A mode switch is a fresh start: clear the local problem and the second
    // password so a stale complaint never sits under a different form.
    fun switchTo(next: AuthMode) {
        mode = next
        localProblem = null
        notice = null
        confirm = ""
        OnlineAccount.clearMessage()
    }

    val address = email.trim()
    val canSubmit = !account.busy && address.isNotEmpty() && password.isNotEmpty()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── Mode switch ──────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AuthModePill(
                label = "Sign in",
                selected = mode == AuthMode.SIGN_IN,
                modifier = Modifier.weight(1f)
            ) { if (mode != AuthMode.SIGN_IN) switchTo(AuthMode.SIGN_IN) }
            AuthModePill(
                label = "Create account",
                selected = mode == AuthMode.CREATE,
                modifier = Modifier.weight(1f)
            ) { if (mode != AuthMode.CREATE) switchTo(AuthMode.CREATE) }
        }

        Text(
            text = if (mode == AuthMode.SIGN_IN) {
                "Welcome back. Your account carries your username, your portrait and the cards " +
                    "you post."
            } else {
                "Choose an email and a password of at least 6 characters. We send a link to " +
                    "confirm the address before the account is yours."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        AccountField(
            placeholder = "Email",
            value = email,
            enabled = !account.busy,
            keyboardType = KeyboardType.Email,
            onValueChange = { email = it; localProblem = null }
        )
        AccountField(
            placeholder = if (mode == AuthMode.CREATE) "Password (6 characters or more)" else "Password",
            value = password,
            enabled = !account.busy,
            isPassword = true,
            revealed = reveal,
            revealLabel = if (reveal) "Hide password" else "Show password",
            onToggleReveal = { reveal = !reveal },
            imeAction = if (mode == AuthMode.CREATE) ImeAction.Next else ImeAction.Done,
            onValueChange = { password = it; localProblem = null }
        )
        if (mode == AuthMode.CREATE) {
            AccountField(
                placeholder = "Password again",
                value = confirm,
                enabled = !account.busy,
                isPassword = true,
                revealed = reveal,
                imeAction = ImeAction.Done,
                onValueChange = { confirm = it; localProblem = null }
            )
        }

        // ── One primary action, labelled by the mode ─────────────────────
        Button(
            onClick = {
                val problem = when {
                    address.isEmpty() || password.isEmpty() ->
                        "Enter your email and your password."
                    !looksLikeEmail(address) -> "That doesn't look like a valid email address."
                    password.length < 6 -> "Passwords need at least 6 characters."
                    mode == AuthMode.CREATE && confirm != password ->
                        "Those two passwords don't match."
                    else -> null
                }
                if (problem != null) {
                    localProblem = problem
                    return@Button
                }
                localProblem = null
                scope.launch {
                    val ok = if (mode == AuthMode.SIGN_IN) {
                        OnlineAccount.signIn(context, address, password)
                    } else {
                        OnlineAccount.signUp(context, address, password)
                    }
                    if (ok) {
                        password = ""
                        confirm = ""
                        notice = "Signed in. Your Curio account is connected."
                        onAuthenticated()
                    }
                }
            },
            enabled = canSubmit,
            shape = RoundedCornerShape(50),
            colors = curioDialogActionButtonColors(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 15.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (account.busy) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(10.dp))
            }
            Text(
                text = when {
                    account.busy && mode == AuthMode.SIGN_IN -> "Signing in…"
                    account.busy -> "Creating your account…"
                    mode == AuthMode.SIGN_IN -> "Sign in"
                    else -> "Create account"
                },
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        }

        (localProblem ?: account.error)?.let { message ->
            AccountMessage(text = message, isError = true)
        }
        account.notice?.let { message -> AccountMessage(text = message, isError = false) }
        notice?.let { message ->
            if (account.notice == null && localProblem == null && account.error == null) {
                AccountMessage(text = message, isError = false)
            }
        }

        Text(
            text = "Only text is kept online: your username, your portrait and what you choose to " +
                "post. Photos, audio, recordings and screenshots never leave this device.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * The signed-in identity card: the username (with its rules stated up front and
 * the server's answer shown), the portrait picker and the Online Mode switch.
 */
@Composable
internal fun CurioAccountIdentityCard(
    /** Shown above the fields, e.g. the signed-in email. */
    email: String? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val account = OnlineAccount.state
    val token = account.session?.accessToken

    // The claimed name as the SERVER last confirmed it, so "is this a change?"
    // is answered against what is actually stored rather than against whatever
    // happens to be in the field.
    var savedName by remember { mutableStateOf(AppPreferences.getUsername(context)) }
    var username by remember { mutableStateOf(savedName) }
    var avatarStyle by remember { mutableStateOf(AppPreferences.getSocialAvatarStyle(context)) }
    var savingName by remember { mutableStateOf(false) }
    var nameAnswer by remember { mutableStateOf<String?>(null) }
    var nameFailed by remember { mutableStateOf(false) }

    // What the field would accept right now — the SAME rule the server
    // enforces, stated before the user presses anything.
    val clean = username.trim().removePrefix("@").lowercase()
    val nameProblem = when {
        clean.isEmpty() -> null
        !clean.matches(Regex("[a-z0-9_]{3,24}")) ->
            "Usernames use 3 to 24 letters, numbers or underscores."
        else -> null
    }
    val changed = clean.isNotEmpty() && clean != savedName.trim().removePrefix("@").lowercase()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (email != null) {
            Text(
                text = email,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // A NAME and a HANDLE are different things, and this is the one place
        // the handle is chosen — so it is said here, once, instead of leaving
        // the two fields to look like duplicates of each other.
        Text(
            text = "Your name (above) is what people read first. This handle is how they find " +
                "and mention you — it must be unique.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        AccountField(
            placeholder = "Username",
            value = username,
            enabled = !savingName,
            imeAction = ImeAction.Done,
            onValueChange = {
                username = it.removePrefix("@")
                nameAnswer = null
                nameFailed = false
            }
        )

        // ── ALWAYS an answer. ────────────────────────────────────────────
        // The old field could sit there with a dead Save button and no reason
        // given, which is exactly how "saving does nothing" feels. There is
        // now one line under the field in every state: the rule being broken,
        // the server's verdict, or — when the button is off — why it is off.
        val status = when {
            nameProblem != null -> nameProblem to true
            nameAnswer != null -> nameAnswer!! to nameFailed
            token == null -> "Sign in to claim a username." to false
            clean.isEmpty() -> "Choose a username: 3 to 24 letters, numbers or underscores." to false
            !changed -> "That is already your username." to false
            else -> "Free to claim — save it and it is yours." to false
        }
        AccountMessage(text = status.first, isError = status.second)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = {
                    // A click always answers: an invalid name is reported here
                    // too, not only as a rule line under the field.
                    if (!clean.matches(Regex("[a-z0-9_]{3,24}"))) {
                        nameAnswer = if (clean.isEmpty()) {
                            "Choose a username first."
                        } else {
                            "Usernames use 3 to 24 letters, numbers or underscores."
                        }
                        nameFailed = true
                        return@Button
                    }
                    val active = token
                    if (active == null) {
                        nameAnswer = "Sign in to claim a username."
                        nameFailed = true
                        return@Button
                    }
                    savingName = true
                    nameAnswer = null
                    nameFailed = false
                    scope.launch {
                        SocialApi.updateUsername(active, clean).fold(
                            onSuccess = {
                                AppPreferences.setUsername(context, clean)
                                savedName = clean
                                nameAnswer = "Saved. Your friends will see @$clean."
                                nameFailed = false
                            },
                            onFailure = { failure ->
                                nameAnswer = failure.message
                                    ?: "Couldn't save that username. Try again."
                                nameFailed = true
                            }
                        )
                        savingName = false
                    }
                },
                enabled = !savingName && token != null,
                shape = RoundedCornerShape(50),
                colors = curioDialogActionButtonColors()
            ) {
                if (savingName) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = if (savingName) "Saving…" else "Save username",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            }
            Text(
                text = "Friends find you by this name.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = "Profile icon",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items((0 until SOCIAL_AVATAR_STYLE_COUNT).toList()) { style ->
                AvatarPickerIcon(style, style == avatarStyle) {
                    avatarStyle = style
                    AppPreferences.setSocialAvatarStyle(context, style)
                    token?.let { active ->
                        scope.launch {
                            SocialApi.updateAvatarStyle(active, style).onFailure { failure ->
                                nameAnswer = failure.message ?: "Couldn't save that icon."
                                nameFailed = true
                            }
                        }
                    }
                }
            }
        }
        Text(
            text = "Twenty-eight hand-drawn icons. Nothing is uploaded: the icon is a single " +
                "number on your profile.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** One of the two auth modes — a filled pill for the active one. */
@Composable
private fun AuthModePill(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val fill by animateColorAsState(
        targetValue = if (selected) curioDialogActionColor()
        else MaterialTheme.colorScheme.surfaceContainerHighest,
        animationSpec = tween(CurioMotion.Durations.Quick),
        label = "authModeFill"
    )
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = fill,
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 11.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

/**
 * One frosted account field — the settings family's search-field surface, with
 * an optional reveal toggle. The placeholder is the field's only label.
 */
@Composable
private fun AccountField(
    placeholder: String,
    value: String,
    enabled: Boolean,
    isPassword: Boolean = false,
    revealed: Boolean = false,
    revealLabel: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onToggleReveal: (() -> Unit)? = null,
    onValueChange: (String) -> Unit
) {
    val dark = isCurioDarkTheme()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
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
                keyboardType = if (isPassword) KeyboardType.Password else keyboardType,
                imeAction = imeAction
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
                    tint = if (revealed) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    size = 18.dp
                )
            }
        }
    }
}

/**
 * A validation or server answer, rendered as a soft box rather than loose red
 * text — errors and confirmations never get lost in the form.
 */
@Composable
private fun AccountMessage(text: String, isError: Boolean) {
    val tint = if (isError) MaterialTheme.colorScheme.error
    else MaterialTheme.colorScheme.primary
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isError) {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            CurioIcon(
                name = if (isError) CurioIcons.ErrorOutline else CurioIcons.Check,
                contentDescription = null,
                tint = tint,
                size = 16.dp
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/** A deliberately forgiving address check: something@something.something. */
private fun looksLikeEmail(value: String): Boolean {
    val at = value.indexOf('@')
    val dot = value.lastIndexOf('.')
    return at in 1 until value.length - 1 && dot > at + 1 && dot < value.length - 1
}

