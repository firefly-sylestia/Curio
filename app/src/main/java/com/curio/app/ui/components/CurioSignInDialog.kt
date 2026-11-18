package com.curio.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.curio.app.data.supabase.OnlineAccount
import com.curio.app.ui.theme.CurioDialogShape
import com.curio.app.ui.theme.curioDialogActionButtonColors
import com.curio.app.ui.theme.curioDialogContainerColor
import kotlinx.coroutines.launch

/**
 * The three things a member can do with a password, in one place (v516).
 *
 * The member reported the sign-in flow reached from Edit profile as bad: the row
 * navigated away to the whole account page, so signing in meant leaving the
 * profile being edited. The form opens as a dialog on the page they are on.
 *
 * A forgotten password is the same door, not a fourth one: [AuthMode.RESET] sends
 * the reset email from the sign-in dialog itself, and says only that the link is
 * on its way — whether an address has an account is not this app's to reveal.
 * Every outcome comes from [OnlineAccount], so this dialog and the account page
 * cannot disagree about what happened.
 */
private enum class AuthMode { SIGN_IN, CREATE, RESET }

@Composable
fun CurioSignInDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var mode by remember { mutableStateOf(AuthMode.SIGN_IN) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var working by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf<String?>(null) }

    val configured = OnlineAccount.configured
    val canSubmit = configured && !working && when (mode) {
        AuthMode.RESET -> email.isNotBlank()
        else -> email.isNotBlank() && password.length >= 6
    }

    fun submit() {
        scope.launch {
            working = true
            failure = null
            OnlineAccount.clearMessage()
            val accepted = when (mode) {
                AuthMode.RESET -> OnlineAccount.requestPasswordReset(context, email.trim())
                AuthMode.CREATE -> OnlineAccount.signUp(context, email.trim(), password)
                AuthMode.SIGN_IN -> OnlineAccount.signIn(context, email.trim(), password)
            }
            working = false
            if (accepted) {
                // A reset is not a sign-in: the dialog stays up so the member can
                // read where the link went, and the notice does the talking.
                if (mode == AuthMode.RESET) {
                    mode = AuthMode.SIGN_IN
                } else {
                    onDismiss()
                }
            } else {
                failure = OnlineAccount.state.error ?: when (mode) {
                    AuthMode.RESET -> "The reset email could not be sent."
                    AuthMode.CREATE -> "The account could not be created."
                    AuthMode.SIGN_IN -> "That email and password did not match."
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!working) onDismiss() },
        containerColor = curioDialogContainerColor(),
        shape = CurioDialogShape,
        title = {
            Text(
                when (mode) {
                    AuthMode.RESET -> "Reset your password"
                    AuthMode.CREATE -> "Create your account"
                    AuthMode.SIGN_IN -> "Sign in to Curio"
                }
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = when (mode) {
                        AuthMode.RESET ->
                            "We'll email you a link to set a new password. " +
                                "Nothing changes until you open it."
                        AuthMode.CREATE ->
                            "An account keeps your name, your handle and your photo across devices. " +
                                "What you save offline stays on this device."
                        AuthMode.SIGN_IN ->
                            "Sign in to post, message and keep your profile in step across devices."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it.trim() },
                    singleLine = true,
                    enabled = !working,
                    label = { Text("Email") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
                if (mode != AuthMode.RESET) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        singleLine = true,
                        enabled = !working,
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (!configured) {
                    Text(
                        text = "Curio's online account is not set up in this build.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                val message = failure ?: OnlineAccount.state.error
                if (message != null) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                OnlineAccount.state.notice?.let { notice ->
                    Text(
                        text = notice,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(
                    onClick = {
                        mode = if (mode == AuthMode.RESET) AuthMode.SIGN_IN else AuthMode.RESET
                        failure = null
                        OnlineAccount.clearMessage()
                    },
                    enabled = !working
                ) {
                    Text(if (mode == AuthMode.RESET) "Back to sign in" else "Forgot your password?")
                }
                if (mode != AuthMode.RESET) {
                    TextButton(
                        onClick = {
                            mode = if (mode == AuthMode.CREATE) AuthMode.SIGN_IN else AuthMode.CREATE
                            failure = null
                            OnlineAccount.clearMessage()
                        },
                        enabled = !working
                    ) {
                        Text(
                            if (mode == AuthMode.CREATE) "I already have an account"
                            else "New here? Create an account"
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSubmit,
                colors = curioDialogActionButtonColors(),
                onClick = { submit() }
            ) {
                Text(
                    when {
                        working -> "Working…"
                        mode == AuthMode.RESET -> "Send reset link"
                        mode == AuthMode.CREATE -> "Create account"
                        else -> "Sign in"
                    }
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = { if (!working) onDismiss() },
                colors = curioDialogActionButtonColors()
            ) { Text("Cancel") }
        }
    )
}

/**
 * Changing a password you still remember (v516).
 *
 * [OnlineAccount.changePassword] proves the CURRENT password by signing in with
 * it before the new one is written, so this dialog asks for both and nothing
 * else: an account that cannot prove its old password has the reset email for
 * the case where it is forgotten.
 */
@Composable
fun CurioChangePasswordDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var current by remember { mutableStateOf("") }
    var replacement by remember { mutableStateOf("") }
    var working by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf<String?>(null) }

    val canSubmit = !working && current.isNotEmpty() && replacement.length >= 6

    fun submit() {
        scope.launch {
            working = true
            failure = null
            OnlineAccount.clearMessage()
            val changed = OnlineAccount.changePassword(context, current, replacement)
            working = false
            if (changed) onDismiss() else {
                failure = OnlineAccount.state.error ?: "The password could not be changed."
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!working) onDismiss() },
        containerColor = curioDialogContainerColor(),
        shape = CurioDialogShape,
        title = { Text("Change your password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Enter the password you use now, then the new one.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = current,
                    onValueChange = { current = it },
                    singleLine = true,
                    enabled = !working,
                    label = { Text("Current password") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = replacement,
                    onValueChange = { replacement = it },
                    singleLine = true,
                    enabled = !working,
                    label = { Text("New password") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                (failure ?: OnlineAccount.state.error)?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSubmit,
                colors = curioDialogActionButtonColors(),
                onClick = { submit() }
            ) { Text(if (working) "Working…" else "Change password") }
        },
        dismissButton = {
            TextButton(
                onClick = { if (!working) onDismiss() },
                colors = curioDialogActionButtonColors()
            ) { Text("Cancel") }
        }
    )
}

/**
 * The card Profile wears instead of its badge strip while signed out (v516).
 *
 * The strip's first tile was a LOCKED badge, so a signed-out member was greeted
 * by "Tuned Ear" — an aspiration for an account they do not have — before
 * anything that helped them. The card answers the question they actually have
 * and opens the same dialog the rest of the app uses.
 */
@Composable
fun CurioSignInCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "Sign in to Curio",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "Keep your name, handle and photo across devices, and post with the community.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Text(
                text = "Sign in",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
