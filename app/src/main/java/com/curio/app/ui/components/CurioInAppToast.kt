package com.curio.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
private data class CurioToastMessage(
    val id: Long,
    val text: String,
    val glyph: String? = null
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
    var current by mutableStateOf<CurioToastMessage?>(null)
        private set
    private var nextId = 0L

    /** Shows [text] as an in-app toast (main thread). Re-shows replace. */
    fun show(text: String, glyph: String? = null) {
        current = CurioToastMessage(nextId++, text, glyph)
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
 * with a soft slide + fade.
 */
@Composable
fun CurioInAppToastHost(modifier: Modifier = Modifier) {
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
            Surface(
                shape = RoundedCornerShape(50),
                color = curioDialogContainerColor(),
                shadowElevation = 6.dp,
                modifier = Modifier.padding(horizontal = 24.dp)
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
                }
            }
        }
    }
}
