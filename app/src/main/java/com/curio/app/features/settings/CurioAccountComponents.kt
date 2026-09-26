package com.curio.app.features.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Checkbox
import androidx.compose.material3.TextButton
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
import com.curio.app.BuildConfig
import com.curio.app.data.AppPreferences
import com.curio.app.data.CurioContentFilter
import com.curio.app.data.supabase.OnlineAccount
import com.curio.app.data.supabase.SocialApi
import com.curio.app.ui.components.liquidglass.CurioGlassWindowBlur
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
    var termsAccepted by remember { mutableStateOf(AppPreferences.hasAcceptedCurrentTerms(context)) }
    var showTerms by remember { mutableStateOf(false) }

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
    val canSubmit = !account.busy && address.isNotEmpty() && password.isNotEmpty() && termsAccepted

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

        TermsAcceptanceRow(
            accepted = termsAccepted,
            onRequest = { showTerms = true }
        )
        if (showTerms) {
            CurioTermsDialog(
                onDismiss = { showTerms = false },
                onAccept = {
                    termsAccepted = true
                    AppPreferences.acceptCurrentTerms(context)
                    showTerms = false
                }
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
                        AppPreferences.acceptCurrentTerms(context)
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
                    color = settingsReadableInk(settingsRoseAccent()),
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

        // ── Password recovery lives on the account site ───────────────────
        // A reset link has to open somewhere a password field can live, and the
        // app's sign-in form is not a URL. So the site owns that page (it can
        // also set a new password without the app), and this row is the door to
        // it. Hidden entirely while the build carries no site URL: a build from
        // before the site exists must not offer a door that goes nowhere.
        if (mode == AuthMode.SIGN_IN && BuildConfig.CURIO_AUTH_SITE_URL.isNotBlank()) {
            TextButton(
                onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(
                                    BuildConfig.CURIO_AUTH_SITE_URL.trimEnd('/') + "/reset"
                                )
                            )
                        )
                    }
                },
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = "Forgot your password?",
                    style = MaterialTheme.typography.labelLarge,
                    color = curioDialogActionColor()
                )
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
 * the server's answer shown) and the Online Mode switch.
 *
 * v435 — THE PORTRAIT PICKER IS GONE from here and from Edit profile (member:
 * "Remove the picker entirely"). A member's face is derived from their username
 * rather than chosen, so there is nothing to pick and no `avatarStyle` state to
 * carry; `includeAvatarPicker` existed only to keep the row from appearing on
 * two pages, and it went with the row.
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
    var savingName by remember { mutableStateOf(false) }
    var nameAnswer by remember { mutableStateOf<String?>(null) }
    var nameFailed by remember { mutableStateOf(false) }
    var termsAccepted by remember { mutableStateOf(AppPreferences.hasAcceptedCurrentTerms(context)) }
    var showTerms by remember { mutableStateOf(false) }

    // What the field would accept right now — the SAME rule the server
    // enforces, stated before the user presses anything.
    val clean = username.trim().removePrefix("@").lowercase()
    val nameProblem = when {
        clean.isEmpty() -> null
        !clean.matches(Regex("[a-z0-9_]{3,24}")) ->
            "Usernames use 3 to 24 letters, numbers or underscores."
        // v3xx53 — the same text filter every post and message goes through,
        // stated BEFORE the request instead of only as the server's answer.
        CurioContentFilter.problem(clean) != null -> CurioContentFilter.problem(clean)
        else -> null
    }
    // v3xx60 — a name that is both well-formed AND clears the safety filter
    // needs no coaching: the field speaks for itself. The rules line and the
    // policy warning below only appear while the field still has something to
    // say (empty, broken, or carrying a banned term) — a valid name shows the
    // field and nothing else.
    val nameValid = clean.isNotEmpty() && nameProblem == null

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

        // Stated ONCE, and only when it has something to say: an account
        // with no handle yet gets the invite to claim one, an account that
        // has one gets its handle echoed back (the field starts as that
        // handle, so "what am I called?" is answered by the page itself).
        // v3xx60 — and never once the typed name is valid: the field already
        // carries the handle, so the echo is noise.
        if (!nameValid) {
            Text(
                text = if (savedName.isBlank()) {
                    "Choose a unique username so friends can find and mention you."
                } else {
                    "You are @${savedName.trim().removePrefix("@")}. Friends find and mention you with it."
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

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

        // ── ALWAYS an answer. ───────────────────────────────────────────��
        // The old field could sit there with a dead Save button and no reason
        // given, which is exactly how "saving does nothing" feels. There is
        // now one line under the field in every state: the rule being broken,
        // the server's verdict, or — when the button is off — why it is off.
        val status = when {
            nameProblem != null -> nameProblem to true
            nameAnswer != null -> nameAnswer!! to nameFailed
            token == null -> "Sign in to claim a username." to false
            clean.isEmpty() && savedName.isBlank() ->
                "Choose a username: 3 to 24 letters, numbers or underscores." to false
            // v3xx60 — a valid name gets no "free to claim" filler; the only
            // lines left are the rule being broken, the server's verdict, and
            // why a disabled button is disabled.
            else -> "" to false
        }
        if (status.first.isNotEmpty()) {
            AccountMessage(text = status.first, isError = status.second)
        }

        // v3xx53 — the ACCOUNT POLICY, stated where the name is chosen. The
        // filter above is the enforcement; this is the warning.
        // v3xx60 — it now appears only when it APPLIES: the moment the typed
        // name carries a banned term, alongside the refusal. A clean name
        // never has the ban notice parked under the field.
        if (CurioContentFilter.carriesBadWord(clean)) {
            Text(
                text = CurioContentFilter.NAME_WARNING,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
            )
        }

        TermsAcceptanceRow(
            accepted = termsAccepted,
            onRequest = { showTerms = true }
        )
        if (showTerms) {
            CurioTermsDialog(
                onDismiss = { showTerms = false },
                onAccept = {
                    termsAccepted = true
                    AppPreferences.acceptCurrentTerms(context)
                    showTerms = false
                }
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = {
                    if (!termsAccepted) {
                        nameAnswer = "Please accept Curio's Terms and data disclosures first."
                        nameFailed = true
                        return@Button
                    }
                    AppPreferences.acceptCurrentTerms(context)

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
                        color = settingsReadableInk(settingsRoseAccent()),
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
        }

    }
}

@Composable
private fun TermsAcceptanceRow(
    accepted: Boolean,
    onRequest: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onRequest),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!accepted) {
            Checkbox(
                checked = false,
                onCheckedChange = { if (it) onRequest() }
            )
        }
        Text(
            text = if (accepted) "Terms and data disclosures accepted · View" else "Review Curio's Terms and data disclosures",
            style = MaterialTheme.typography.labelMedium,
            color = settingsAccentInk()
        )
    }
}

@Composable
internal fun CurioTermsDialog(
    onDismiss: () -> Unit,
    onAccept: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Curio terms and disclosures") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Curio stores your account details, profile information, posts, comments and reactions to provide the service.")
                Text("Direct messages are sent as plain text over the protected Online Mode connection and are stored so they can reach the other person. They are not end-to-end encrypted, so do not send anything you could not afford to disclose.")
                Text("You can review these disclosures again from this screen at any time.")
            }
        },
        confirmButton = {
            CurioGlassWindowBlur()
            TextButton(onClick = onAccept) { Text("Accept") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Not now") }
        }
    )
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
            color = if (selected) settingsReadableInk(settingsRoseAccent())
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
                if (dark) MaterialTheme.colorScheme.surfaceContainerHigh
                else MaterialTheme.colorScheme.surfaceContainerLow
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
            cursorBrush = SolidColor(settingsRoseAccent()),
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
                    tint = if (revealed) settingsRoseAccent()
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
    else settingsRoseAccent()
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
