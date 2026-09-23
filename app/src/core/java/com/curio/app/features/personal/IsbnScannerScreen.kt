package com.curio.app.features.personal

import androidx.compose.runtime.Composable

/**
 * ── v465b — THE CORE EDITION'S ISBN SCANNER, WHICH IS DELIBERATELY NOTHING ──
 *
 * This is the twin of the full edition's real sheet at
 * `app/src/full/java/com/curio/app/features/personal/IsbnScannerScreen.kt`. Same
 * package, same name, same parameters, no body — and it exists so that the
 * add-a-book sheet in `main` can call [IsbnScannerSheet] at all.
 *
 * **Why a twin file rather than an `if` in `main`:** the scanner's real
 * implementation imports `androidx.camera.*` and `com.google.mlkit.*`, and
 * `main` is compiled into BOTH editions — so a camera or ML Kit type may never
 * appear in `main`, and neither may the scanner's five dependencies (they are
 * `fullImplementation` in app/build.gradle.kts). A flavor source set is the only
 * place a type can exist for one edition and not the other, so the seam is a
 * duplicated signature, exactly like `OfflineTranscriber` beside it.
 *
 * **How the caller decides:** `AddBookSheet` (in `main`) reads
 * `BuildConfig.EDITION_ISBN_SCANNER` — `false` here, `true` in the full edition
 * — and only ever renders the Scan door when it is true, in which case this
 * no-op can never be the composable being called. The flag is not decoration:
 * without it the core edition would draw a door that opens a black rectangle.
 *
 * **If a third edition is ever added,** this file and its twin are the two
 * halves of one contract — change the signature in one and the other stops
 * compiling the core edition, which is the intended failure (a silent no-op with
 * a stale signature would ship a dead door instead).
 */
@Composable
fun IsbnScannerSheet(onDismiss: () -> Unit, onAdded: (String) -> Unit) = Unit
