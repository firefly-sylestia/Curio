package com.curio.app.ui.components.liquidglass

import android.content.Context
import android.os.Build
import android.util.Log
import android.view.View
import android.view.ViewParent
import android.view.Window
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import com.curio.app.ui.components.isLiquidGlassRequested

/**
 * v484 — REAL BLUR BEHIND A SHEET OR A DIALOG.
 *
 * A sheet and a dialog are not drawn on the page: each gets its OWN Android
 * window, and the page behind it belongs to a DIFFERENT window. That is why no
 * modifier can make a sheet's panel refract like a pill does — the pills sample
 * a [com.kyant.backdrop.backdrops.LayerBackdrop] captured inside their own
 * window, and there is no capture of another window's content to sample.
 *
 * What the platform CAN do is blur the screen behind a window. Since Android 12
 * a window can carry `WindowManager.LayoutParams.FLAG_BLUR_BEHIND` plus a
 * `blurBehindRadius` (the same mechanism the system's own dialogs use), and
 * everything behind it is then really blurred by RenderEngine — cross-window,
 * off the app's draw path, no capture and no cyclic-render-node risk. That is
 * the only honest route to "the sheet is glass", and this file is it.
 *
 * The exact two lines are not invented: they are what `androidx.compose.ui`'s
 * own `DialogProperties.blurBehindRadius` does on the platform side
 * (`DialogApi31Impl.setBlurBehindRadius`) — add the flag, write the radius into
 * `window.attributes`, clear the flag when it is over. The flag form is used
 * rather than `Window.setBackgroundBlurRadius` because that one blurs only
 * WITHIN the window's bounds, where blur-behind blurs the whole page.
 *
 * ## The one rule at the call site
 *
 * [CurioGlassWindowBlur] must be called **inside the sheet's or the dialog's own
 * content**. A dialog's `containerColor` — and any other parameter — is evaluated
 * at the CALL SITE, i.e. in the APP's window composition, before the dialog's
 * window exists, so `LocalView.current` there is the page and not the dialog. The
 * content lambda, on the other hand, is composed inside the window whose
 * `DialogWindowProvider` this reads:
 *
 *  - `androidx.compose.ui.window.Dialog`  → its `DialogLayout` IS the provider;
 *  - Material3's `ModalBottomSheet`       → its `ModalBottomSheetDialogLayout` IS
 *    the provider (`AbstractComposeView`, own transparent window).
 *
 * Call it once per window: it sets the radius on show and clears it on dispose.
 *
 * ## What it never does
 *
 * - Below Android 12 there is no window blur, and [windowBlurAvailable] says so,
 *   so the panel's opaque recipe stays in charge.
 * - Lite mode / glass off → no window blur at all: the same
 *   [isLiquidGlassRequested] gate every other glass surface asks.
 * - Devices where the platform has cross-window blur switched off
 *   (`WindowManager.isCrossWindowBlurEnabled`) are left alone. Setting a radius
 *   there is a no-op, but the panel would have gone translucent for a blur that
 *   never arrives — so we ask first and the panel stays opaque.
 */

/**
 * The radius the sheet/dialog blur is drawn with, in dp. The platform quantises
 * window blur to its own levels, so an exact value is not meaningful; the system
 * recommendation for a depth-of-field layer is ~10dp (≈20px) and this sits a
 * little above it — dense enough to read as glass, light enough not to smear a
 * page of text into soup. Radius only applies when a blur is drawn at all.
 */
val CurioWindowBlurRadius: Dp = 14.dp

/**
 * Whether the platform will really blur behind a window right now.
 *
 * It is asked of the CONTEXT the panel is built with — the app's, never a
 * window's — which is what lets a caller pose the question before the window
 * exists (see [CurioGlassWindowBlur]). Everything that could make the answer
 * "no" is folded in here: glass off / Lite mode ([isLiquidGlassRequested]),
 * Android below 12, and cross-window blur disabled by the system.
 */
fun windowBlurAvailable(context: Context): Boolean {
    if (!isLiquidGlassRequested()) return false
    if (Build.VERSION.SDK_INT < 31) return false
    return try {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        wm?.isCrossWindowBlurEnabled ?: false
    } catch (t: Throwable) {
        // A window service we cannot ask is not a window service we should
        // assume blur from — the panels stay opaque.
        Log.w("CurioGlass", "window blur availability unknown", t)
        false
    }
}

/**
 * Blurs the screen behind the window [view] lives in (see the file header for
 * why this must be called from INSIDE a sheet's or a dialog's content).
 *
 * Composed only when [enabled]; the caller's `containerColor` has usually
 * already asked the same question, so the two agree. Every failure is swallowed:
 * a window that cannot be found, or a platform that refuses the radius, leaves
 * the surface exactly as it was — an opaque panel nobody notices.
 */
@Composable
fun CurioGlassWindowBlur(
    radius: Dp = CurioWindowBlurRadius,
    enabled: Boolean = windowBlurAvailable(LocalContext.current)
) {
    if (!enabled || Build.VERSION.SDK_INT < 31) return
    val view = LocalView.current
    val px = with(LocalDensity.current) { radius.roundToPx() }
    // Keyed on the view and the radius: a recomposition with the same window
    // must not re-set the radius (it is a window-level call), and a new window
    // (a re-shown sheet) gets its own effect.
    DisposableEffect(view, px) {
        val window = view.curioHostWindow()
        if (window == null) {
            // No window behind this view: nothing to blur, and the panel's
            // colour was decided by windowBlurAvailable() — so a flat glass panel
            // is the only risk here, never a crash.
            Log.w("CurioGlass", "no host window for ${view.javaClass.simpleName}; glass stays flat")
        } else {
            try {
                window.blurBehind(px)
            } catch (t: Throwable) {
                Log.w("CurioGlass", "blur behind $px px refused", t)
            }
        }
        onDispose {
            if (window != null) {
                try {
                    window.blurBehind(0)
                } catch (_: Throwable) {
                    // Dismissal is not a moment to complain: the window goes away.
                }
            }
        }
    }
}

/**
 * The [Window] this view is drawn in, or null when it is not in a dialog/sheet
 * window of its own.
 *
 * Both window-hosting layouts Compose can give us — `DialogLayout` and
 * Material3's `ModalBottomSheetDialogLayout` — implement [DialogWindowProvider]
 * directly and ARE the local view, so the first test catches them; the walk up
 * the parent chain is for a view nested inside one (or inside a `ComposeView`
 * that some other wrapper put there).
 */
private fun View.curioHostWindow(): Window? {
    (this as? DialogWindowProvider)?.let { return it.window }
    var parent: ViewParent? = this.parent
    while (parent != null) {
        (parent as? DialogWindowProvider)?.let { return it.window }
        parent = (parent as? View)?.parent
    }
    return null
}

/**
 * Blur behind a window, exactly as AndroidX's own dialog implementation does it
 * (`DialogApi31Impl.setBlurBehindRadius`, Compose UI): the FLAG is what asks the
 * window manager for a behind-blur at all, and the radius rides in the window's
 * own attributes. Passing `0` clears the flag, which is how a window is handed
 * back clean when the sheet or dialog goes away.
 *
 * The `SDK_INT` guard lives in HERE rather than only at the call site: lint
 * (`NewApi`, an error in this project's lint run) reads a guard in the same
 * scope as the API-31 symbols, and an early return from the enclosing
 * composable is not seen inside the `DisposableEffect` lambda it precedes.
 */
private fun Window.blurBehind(radiusPx: Int) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    if (radiusPx > 0) {
        addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
        val updated = attributes
        updated.blurBehindRadius = radiusPx
        attributes = updated
    } else {
        clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
    }
}
