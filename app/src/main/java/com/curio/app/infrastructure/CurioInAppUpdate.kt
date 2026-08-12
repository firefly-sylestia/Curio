package com.curio.app.infrastructure

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * v24 — Google Play in-app updates (Play Core `app-update`).
 *
 * The "Check for updates" flow on the Support & diagnostics page asks Play
 * whether a newer version is available and, when it is, offers the FLEXIBLE
 * in-app update (download in the background, install when it's ready).
 *
 * Sideloaded installs (direct APK / debug builds — not acquired via Google
 * Play) are handled gracefully: the Play path is gated on the actual
 * INSTALLER ([isInstalledFromPlay]), so [available] returns null and the
 * page falls back to the GitHub release check (notes + release page)
 * instead. The installer gate matters because Play's availability answer
 * alone is NOT a reliable Play-vs-sideload test.
 */
object CurioInAppUpdate {

    /**
     * True when this app was installed from the Google Play Store.
     *
     * GitHub / ADB / file-manager installs return null (or their own
     * installer package), so they never touch Play Core. [getInstallerPackageName]
     * can be absent on some restores, which is fine — the GitHub fallback
     * always works.
     */
    fun isInstalledFromPlay(context: Context): Boolean =
        runCatching {
            context.packageManager.getInstallerPackageName(context.packageName) == "com.android.vending"
        }.getOrDefault(false)

    /**
     * Suspends until Play answers whether a FLEXIBLE update is available.
     *
     * Returns the [AppUpdateInfo] when an update is available AND the
     * flexible flow is allowed; null otherwise (not a Play install, up to
     * date, or the query failed).
     */
    suspend fun available(context: Context): AppUpdateInfo? = suspendCancellableCoroutine { cont ->
        // v25 — gate on the INSTALLER, not on Play's availability answer:
        // sideloaded builds can still report UPDATE_AVAILABLE, which surfaced
        // a bogus "Update available on Google Play" card on GitHub installs.
        if (!isInstalledFromPlay(context)) {
            cont.resume(null)
            return@suspendCancellableCoroutine
        }
        // Defensive: a Play Core factory quirk must never crash the check —
        // it just reports "no update".
        val manager = runCatching { AppUpdateManagerFactory.create(context) }.getOrNull()
        if (manager == null) {
            cont.resume(null)
            return@suspendCancellableCoroutine
        }
        val task = manager.appUpdateInfo
        task.addOnSuccessListener { info ->
            val allowed = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            cont.resume(if (allowed) info else null)
        }
        task.addOnFailureListener { cont.resume(null) }
    }

    /**
     * Completes a downloaded flexible update. The chained addOnSuccessListener
     * keeps the invocation unambiguous even if a deprecated void overload of
     * [AppUpdateManager.completeUpdate] is present.
     */
    fun finishInstall(manager: AppUpdateManager) {
        runCatching { manager.completeUpdate().addOnSuccessListener { /* install completed */ } }
    }
}

/**
 * The root-level in-app-update host (rendered once from MainActivity, before
 * the NavHost) so the flexible-flow listener survives navigation.
 *
 * - Registers an [InstallStateUpdatedListener]: when a flexible update the
 *   user approved finishes downloading, completes the install immediately
 *   (the user already tapped "Update now", so no extra prompt is needed).
 * - On resume, finishes any flexible download that completed while the app
 *   was backgrounded or after a process restart.
 *
 * Both paths are guarded with runCatching — on sideloaded installs the Play
 * Core calls are inert and this host does nothing.
 */
@Composable
fun CurioInAppUpdateHost() {
    val context = LocalContext.current.applicationContext
    // v25 — only installs that came from Google Play can use in-app updates;
    // sideloaded GitHub builds never touch Play Core. Defensive on top: if
    // Play Core can't initialize the host simply does nothing.
    val manager = remember {
        if (!isInstalledFromPlay(context)) null
        else runCatching { AppUpdateManagerFactory.create(context) }.getOrNull()
    }
    if (manager == null) return
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(manager) {
        val listener = InstallStateUpdatedListener { state ->
            if (state.installStatus() == InstallStatus.DOWNLOADED) {
                finishInstall(manager)
            }
        }
        manager.registerListener(listener)
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                manager.appUpdateInfo.addOnSuccessListener { info ->
                    if (info.installStatus() == InstallStatus.DOWNLOADED) {
                        finishInstall(manager)
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            manager.unregisterListener(listener)
        }
    }
}
