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
import com.google.android.play.core.appupdate.AppUpdateType
import com.google.android.play.core.tasks.Task
import com.google.android.play.core.install.InstallStateUpdatedListener
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
 * Play) are handled gracefully: Play reports UPDATE_NOT_AVAILABLE, so
 * [available] returns null and the page falls back to the GitHub release
 * check (notes + release page) instead.
 */
object CurioInAppUpdate {

    /**
     * Suspends until Play answers whether a FLEXIBLE update is available.
     *
     * Returns the [AppUpdateInfo] when an update is available AND the
     * flexible flow is allowed; null otherwise (up to date, non-Play
     * install, or the query failed).
     */
    suspend fun available(context: Context): AppUpdateInfo? = suspendCancellableCoroutine { cont ->
        // Defensive: on sideloaded builds / Play-less devices a factory
        // quirk must never crash the check — it just reports "no update".
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
     * Completes a downloaded flexible update. The chained [Task] call keeps
     * the invocation unambiguous even if a deprecated void overload of
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
    // Defensive: if Play Core can't initialize (sideload / no Play client),
    // this host simply does nothing — it must never crash the app.
    val manager = remember { runCatching { AppUpdateManagerFactory.create(context) }.getOrNull() }
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
