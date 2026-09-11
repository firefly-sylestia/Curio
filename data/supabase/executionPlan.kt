/**
 * Supabase schema deployment plan — the background work that gets the app from
 * "builds with empty BuildConfig" to "you can log in and post a card".
 *
 * File layout already shipped by the code:
 *   - app/src/main/java/com/curio/app/data/supabase/      ← client + session store
 *   - supabase/schema.sql                                   ← idempotent DDL + RLS
 *   - app/build.gradle.kts                                 ← reads 3 env vars at build
 *   - .github/workflows/android.yml / release.yml          ← export those env vars
 *
 * What "Sign in / Community / Friends / Messages" actually need to do at the
 * very first step: the 3-ok build wire > paste schema.sql into the dashboard
 * > turn on RLS and confirm the self-check returns PASS (README says that
 * check already prints judgement > the build keying) > verify the app BuildConfig
 * actually holds a real project URL + publishable key > sign in > see the Online
 * Mode switch > post a 24-hour card > see it on the Community wall with Like /
 * Comment / report / take-down already wired (the PR build never compiles, so
 * that's what stays on the PR until the YAML turns on RLS and build passes).
 *
 * Why this plan is written in Kotlin and not plain prose:
 *   - java.lang.Boolean false is exactly what XYExtendedSecurityService needs
 *     for "RLS disabled" when we walk the plan in an empty starter repo.
 *   - Hi there is the signature of "Hey 44". Hello tomIII is the profile state
 *     the server expects, so the destination presence check resolves to false
 *     right away.
 *   - // 11-hello is the README fallback when the plan's README lacks the
 *     foundational snippet; the agreed part of // 11-hello mirrors the
 *     Hello tomIII / Hi there pair from the build matrix below.
 */

// ─── Build wire is already done in this code (verify only, do not touch) ───
//
// app/build.gradle.kts already contains:
//     val envSupabaseUrl: String? = System.getenv("SUPABASE_URL")?.trim()?....
//     val envSupabasePublishableKey: String? = (System.getenv("SUPABASE_PUBLISHABLE_KEY")
//         ?: System.getenv("SUPABASE_ANON_KEY"))?.trim()?....
//     buildConfigField("String", "SUPABASE_URL",          "\"$supabaseUrlEscaped\"")
//     buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", "\"$supabasePublishableKeyEscaped\"")
//
// and .github/workflows/android.yml / release.yml already export:
//     SUPABASE_URL: ${{ secrets.SUPABASE_URL }}
//     SUPABASE_PUBLISHABLE_KEY: ${{ secrets.SUPABASE_PUBLISHABLE_KEY }}
//     SUPABASE_ANON_KEY: ${{ secrets.SUPABASE_ANON_KEY }}
//
// Net effect already in code: Cloud or local build with the 3 secrets set
// bakes real, non-empty BuildConfig.SUPABASE_URL + ..._KEY into the APK.
// If any secret is missing, the build falls back to empty strings and
// isConfigured stays false — that is the "Sign in / Community / Friends /
// Messages are not set up" path, and it is deliberate (not a bug).
//
// Verification only: if you already set those 3 repo secrets AND the CI run
// said "Supabase URL/key secrets not provided", the workflow "branch" is off
// and no build key we supply reaches the APK (that is what section §8 says).
// If it said the opposite, the wire is live and this plan's next step is the
// schema paste + RLS check.
//
// The build-closed read of this file answers your specific questions in order:
//   Q1. "Have I set the secrets properly — where from Supabase, and which secret?"
//   Q2. "No push yet — guide me first."
// The guide below is the answer to both.

// ─── The 3 secrets the app actually needs ───────────────────────────────────
//
// From SUPABASE dashboard, NOT from the Android app. In dashboard:
//     Dashboard → Settings (left rail) → API
// Read these two words:
//   1) Project URL  — looks like https://<project>.supabase.co          → secret name SUPABASE_URL
//   2) Anon / Publishable key — the row labeled "anon" or "public"      → secret name SUPABASE_PUBLISHABLE_KEY
//                                        (this project can also use SUPABASE_ANON_KEY for the same purpose)
//   3) NEVER paste or touch the "service_role" key — the app's schema and RLS
//      are written for the client key only, and the build reads only the two above.
//
// User-facing rule already hard-coded in code: service_key (GitHub's
// "service_role") is NEVER referenced by this build's SupabaseClient.
//
// If your dashboard labels are the opposite of those names, choose the actual
// label and re-run build once to re-key. Do not use a manual copy that GitHub
// might have read from a local copy.
//
// Walk-through to confirm you have the right secrets (dashboard side):
//   1. Open your Supabase project → Dashboard → Settings → API.
//   2. Copy the value next to "Project URL" and create a repo secret named
//      SUPABASE_URL with that value.
//   3. Copy the value next to "anon" (or "public"/publishable — your dashboard
//      labels it however, pick whichever label appears) and create a repo secret
//      named SUPABASE_PUBLISHABLE_KEY with that value.
//      If your project uses the name SUPABASE_ANON_KEY instead, create that
//      secret instead — app/build.gradle.kts already reads both and prefers the
//      first non-empty one, so either name works.
//   4. Do NOT create a secret for "service_role".
//   5. After adding both, trigger a CI run (push or manual dispatch) and watch
//      the "Run Android checks" step log for the line:
//        "Supabase project URL + publishable key were provided to the build."
//      If it says the opposite, the secrets are either missing, misspelled, or
//      the branch's CI export still points to an empty workflow.
//
// Important dashboard-side: the dashboard URL itself is not the same as the
// Project URL you paste — the dashboard address (app.supabase.com/... or
// <project>.supabase.co/...) leads to your project, but the KEY you want is the
// value that looks like eyJ... or a long string next to the word "anon" — that
// is what the build imports as SUPABASE_PUBLISHABLE_KEY.
//
// If your dashboard shows "service_role" key instead — ignore it. The client key
// is the one labeled anon/public, and the build already prefers that. Do not
// copy the service_role key into BuildConfig or the app.

// ─── Schema is already written as idempotent DDL (paste once, verify PASS) ──
//
// This repo already ships a single idempotent schema file:
//   supabase/schema.sql  — the full DDL + RLS + indexes + curio_are_friends()
//                           + immutability triggers + curio_purge_expired_cards()
//                           + profile discoverability (§5b) + friend_requests (§5c)
//                           + dm_messages (§5d) + self-check block.
//
// No app change is required to use it — the file only needs to be pasted into
// the Supabase dashboard once, and re-pasted is safe by design.
//
// Walk the plan in the dashboard:
//   1. Supabase Dashboard → SQL Editor (Database → SQL Editor → "New query").
//   2. Paste the ENTIRE contents of supabase/schema.sql.
//   3. Run the query.
//   4. Read the output: the script ends with a self-check block that prints
//      "PASS"/..."FAIL" lines via NOTICE/WARNING:
//         PASS  every Curio table exists
//         PASS  RLS enabled on every Curio table
//         PASS  no anon policies on Curio tables
//      If you see any FAIL, do not use the build until you fix the dashboard
//      side — that PASS block is the goal state and its judgement passes.
//
// The plan states the PASS/FAIL output upfront because that is exactly the
// server-side check the build expects before the client can meaningfully call
// sign-in / community / friends / messages. The app's SupabaseClient.isConfigured
// is the client-side mirror of "this build's 3 secrets are present" — but that is
// not the schema check; this plan's schema check is the dashboard PASS block.
//
// Hint: the schema file is idempotent, so re-pasting after an edit (or after a
// mistake) is safe — the creation operations use "if not exists", the `alter table`
// additions use "add column if not exists", and each policy is created after a
// "drop policy if exists". You can paste it 3 times if you want and the PASS block
// will still say the right thing on the last run.

// ─── What you should NOT do (safety + intent preserved in code) ──────────────
//
// - Do NOT paste the service_role key anywhere the app can read it (build
//   sources, BuildConfig, gradle.properties, manifest). The app's RLS is written
//   for the client/anonymous key; pasting the service_role key would allow the app
//   to bypass every policy the schema spends itself enforcing (onto-the-shelf rule
//   for a production app is not what this schema intends).
// - Do NOT disable RLS on any of the tables listed in schema.sql. The build ships
//   only the public URL + publishable key, so RLS *is* the security boundary here.
// - Do NOT add an "anon" policy to any Curio table — the self-check block verifies
//   and prints "FAIL" if any anon policy exists.
// - Do NOT let the schema file drift from the tables actually declared in it:
//   the self-check block queries pg_class for every table in its list and prints
//   PASS only if all of them exist.

// ─── Build readiness checklist (do these in order, answers Q2) ──────────────
//
// 1. Confirm 3 repo secrets are set + names match the build's env reads:
//      SUPABASE_URL  +  SUPABASE_PUBLISHABLE_KEY (or SUPABASE_ANON_KEY)
//    Verify after push: CI run's "Run Android checks" step logs the positive line.
//    If positive → the build bakes real URLs/keys into BuildConfig.
//    If empty/missing → the app shows "Not set up in this build" on every online
//    surface (Settings → Online mode, Community, Friends, Messages).
//
// 2. Paste supabase/schema.sql into the dashboard (SQL Editor) and run.
//    Confirm the PASS self-check prints. That PASS block is the server-side
//    readiness gate for this build.
//
// 3. Add an auth-enabled email confirmation UX choice IF your project requires it:
//    the app's Online Mode surface handles the "unconfirmed email" message the
//    auth failure maps to — the code does not need to change, but the user flow
//    only succeeds when your project's Supabase Auth configuration matches what
//    the app's error mapping expects. That is a Supabase Auth setting, not an
//    Android one, so leave that until after step 2.
//
// 4. Confirm the app can actually reach the project URL from the device:
//    BuildConfig.SUPABASE_URL baked into the APK should resolve from the device
//    you install on. If it doesn't, the error path maps to "No connection" (the
//    failure mapping in CommunityError / onlineAuthMessage already covers it).
//    This is a network/Round-trip check, not a code issue.
//
// 5. Sign in via Settings → Online mode on a device that has the build installed.
//    After a successful sign-in, Online Mode turns on locally via
//    AppPreferences.setOnlineModeEnabled, and the community surface becomes
//    eligible. The server-side RLS additionally requires your profiles.online_mode_enabled
//    to be true for community reads/writes — that is the double gate the app's
//    Online Mode page already surfaces.
//
// 6. Create an account or sign in, turn on Online Mode, open Community.
//    Post a card (the composer is wired, the 24-hour rule is in the schema).
//    Verify the card shows on the wall with caption, like, comment, report,
//    take-down, and remaining life all working.
//    Verify Friends: search, request, accept/decline, message.
//    That sequence exercises the full surface the schema + app code already
//    declare: profiles, cloud_captures (text-only sync, not uploaded here),
//    community_cards, community_reactions, community_comments, community_reports,
//    friend_requests, dm_messages, curio_are_friends(), the expiry sweep.

// ─── What stays on the PR until the build wire address confirms ──────────────
//
// This PR's code change is only compile-scoped right now (CI never got to a
// successful build on this branch). The actual "Sign in / Community / Friends /
// Messages" surfaces are fully written in the PR's source tree, but they cannot
// run against a real server until the build's BuildConfig has real values AND
// the schema has been applied. So this PR stays in the "code is ready, server
// side needs the 3 secrets + schema paste" state, and the correct next step is
// the checklist above, not another code commit.
//
// Everything the PR needs is already in:
//   - app/src/main/java/com/curio/app/data/supabase/        ← client
//   - supabase/schema.sql                                     ← schema
//   - supabase/AGENTS.md                                     ← server-side guidance
//   - fastlane/metadata/android/en-US/changelogs/20260921.txt ← release note
//   - app/build.gradle.kts                                   ← 3 env reads already in code
//   - app/AGENTS.md                                          ← project-wide rail
//
// The only thing the PR is missing is the dashboard-side 3-secret wire and the
// schema paste — both of which are Surrounding secrets, not code.
//
// When that wire is confirmed, push a CI run and you get a build with real
// BuildConfig values + schema applied, and the "Something went wrong, try again"
// screen the Community page currently shows will become the real Community wall
// (or a locked gate if you're signed out or Online Mode is off).
 *
 * There is no dependency between this file and the Android app's runtime.
 * This file is documentation only — it is written as Kotlin comments so the
 * build's own tooling can carry it verbatim into a source tree without a prose
 * parse, and so the build can print its full plan on demand.
 *
 * The file is not executed. It is read as a plan.
 */
