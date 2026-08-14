package com.curio.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.curioDialogContainerColor
import kotlinx.coroutines.delay

/** One in-app toast message currently showing. */
internal data class CurioToastMessage(
    val id: Long,
    val text: String,
    val glyph: String? = null,
    /** Short tap-affordance label ("Open") shown at the pill's end. */
    val actionLabel: String? = null,
    /** Opaque action key passed to the host's [onAction] when tapped. */
    val actionId: String? = null
)

/**
 * v63 — the app's IN-APP toast bus, replacing `android.widget.Toast` for
 * background announcements (currently the update-available notice). Any
 * layer — including data-layer coroutines — can call [show] on the main
 * thread; the message is rendered by [CurioInAppToastHost] inside the
 * NavHost as a themed pill that slides up, stays ~3.5s and fades away.
 *
 * Global snapshot state, so a message shown before (or while) the UI
 * composes is picked up the moment the host enters composition — no
 * Android system toast, no permission, works over every screen.
 */
object CurioToast {

    /** The message currently shown; null = nothing. */
    internal var current by mutableStateOf<CurioToastMessage?>(null)
        private set
    private var nextId = 0L

    /**
     * Shows [text] as an in-app toast (main thread). Re-shows replace.
     * When [actionLabel] + [actionId] are given, the pill becomes tappable
     * and the host's `onAction` fires with [actionId] (the NavHost maps it
     * to navigation, e.g. "support" → Support & diagnostics).
     */
    fun show(
        text: String,
        glyph: String? = null,
        actionLabel: String? = null,
        actionId: String? = null
    ) {
        current = CurioToastMessage(nextId++, text, glyph, actionLabel, actionId)
    }

    /** Dismisses the toast — only when [id] is still the one showing. */
    fun dismiss(id: Long) {
        if (current?.id == id) current = null
    }
}

/**
 * The in-app toast overlay — place at the ROOT of the NavHost so it floats
 * above every screen (bottom-nav tabs and pushed pages alike). Observes
 * [CurioToast.current], auto-dismisses after ~3.5s, and animates in/out
 * with a soft slide + fade. Toasts that carry an action are tappable:
 * tapping dismisses them and forwards the action key to [onAction].
 */
@Composable
fun CurioInAppToastHost(
    modifier: Modifier = Modifier,
    // v63b — tap-to-action: the NavHost wires navigation here ("support" →
    // Support & diagnostics) so the update toast opens the update page.
    onAction: ((actionId: String) -> Unit)? = null
) {
    val message = CurioToast.current
    // Auto-dismiss: a fresh message id restarts the timer from zero.
    LaunchedEffect(message?.id) {
        val m = message ?: return@LaunchedEffect
        delay(3500)
        CurioToast.dismiss(m.id)
    }
    AnimatedVisibility(
        visible = message != null,
        modifier = modifier,
        enter = slideInVertically(
            animationSpec = tween(260, easing = FastOutSlowInEasing)
        ) { height -> height / 3 } + fadeIn(animationSpec = tween(220)),
        exit = fadeOut(animationSpec = tween(180))
    ) {
        message?.let { m ->
            val action = m.actionId
            Surface(
                shape = RoundedCornerShape(50),
                color = curioDialogContainerColor(),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .then(
                        if (action != null) Modifier.clickable {
                            CurioToast.dismiss(m.id)
                            onAction?.invoke(action)
                        } else Modifier
                    )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (m.glyph != null) {
                        CurioIcon(
                            name = m.glyph,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            size = 20.dp
                        )
                    }
                    Text(
                        text = m.text,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    if (m.actionLabel != null) {
                        VerticalDivider(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                            modifier = Modifier
                                .fillMaxHeight(0.55f)
                                .width(1.dp)
                        )
                        Text(
                            text = m.actionLabel,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
