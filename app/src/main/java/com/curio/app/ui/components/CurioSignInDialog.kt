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
 * SIGNING IN, WITHOUT LEAVING THE PAGE (v516).
 *
 * The member reported the sign-in flow reached from Edit profile as bad: the row
 * navigated away to the whole account page, so signing in meant leaving the
 * profile being edited. The form now opens as a dialog on the page the member is
 * already on.
 *
 * It offers the two doors that belong in a dialog — sign in, and create an
 * account (the member's choice) — and leaves the long tail (reset a password,
 * magic link, delete the account) on the account page, which is still one door
 * further in. Everything comes from [OnlineAccount], so the dialog and that page
 * cannot disagree about whether someone is signed in or what went wrong: the
 * failure text is the already-safe [OnlineAccount.State.error], never a raw
 * response body.
 */
@Composable
fun CurioSignInDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var creating by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var working by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf<String?>(null) }

    val configured = OnlineAccount.configured
    val canSubmit = configured && !working && email.isNotBlank() && password.length >= 6

    AlertDialog(
        onDismissRequest = { if (!working) onDismiss() },
        containerColor = curioDialogContainerColor(),
        shape = CurioDialogShape,
        title = { Text(if (creating) "Create your account" else "Sign in to Curio") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (creating) {
                        "An account keeps your name, your handle and your photo across devices. " +
                            "What you save offline stays on this device."
                    } else {
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
                        creating = !creating
                        failure = null
                        OnlineAccount.clearMessage()
                    },
                    enabled = !working
                ) {
                    Text(
                        if (creating) "I already have an account"
                        else "New here? Create an account"
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSubmit,
                colors = curioDialogActionButtonColors(),
                onClick = {
                    scope.launch {
                        working = true
                        failure = null
                        OnlineAccount.clearMessage()
                        val accepted = if (creating) {
                            OnlineAccount.signUp(context, email.trim(), password)
                        } else {
                            OnlineAccount.signIn(context, email.trim(), password)
                        }
                        working = false
                        if (accepted) {
                            onDismiss()
                        } else {
                            failure = OnlineAccount.state.error ?: if (creating) {
                                "The account could not be created."
                            } else {
                                "That email and password did not match."
                            }
                        }
                    }
                }
            ) {
                Text(
                    when {
                        working -> "Working…"
                        creating -> "Create account"
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
