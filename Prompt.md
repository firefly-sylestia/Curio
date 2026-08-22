# Prompt.md — current request log

## Request (complete): Live notification fix + permission checker (v229)

User: "the live notification style isn't working. add its permission checker
dialog. and proper logic how it forces other apps to show… properly analyse"
(referencing https://github.com/appsfolder/livebridge).

### Analysis

Fetched LiveBridge's `LiveUpdateNotifier.kt` (~5000 lines) and compared with
Curio's `ExploreSessionService.liveNotification()`. Curio already posted
`NotificationCompat.ProgressStyle`, but three things LiveBridge always does
were missing:

1. `builder.setRequestPromotedOngoing(true)` — the actual request for the
   system to promote the ongoing notification to a status-bar chip /
   lock-screen live activity. Without it, no promotion ever happens.
2. `.setStyledByProgress(true)` on the ProgressStyle.
3. `builder.setShortCriticalText(...)` — the readout shown on the chip.

Verified all three exist in androidx core 1.18.0 (`/tmp/core-src`).

Separately, the Reveal explore flow requested POST_NOTIFICATIONS at session
start, but after a permanent denial ("Don't ask again") the runtime prompt is
a silent no-op → the session ran with NO visible timer anywhere.

### Changes

- `ExploreSessionService.kt`: in the running + API 36+ branch, add
  setRequestPromotedOngoing(true), setShortCriticalText("${progressMins}m"),
  and .setStyledByProgress(true). Paused / pre-16 unchanged.
- `TopicRevealScreen.kt`: permission checker — when the grant result is denied
  AND no rationale AND notifications off, show an app-styled dialog
  ("Notifications are off"): Open settings (ACTION_APP_NOTIFICATION_SETTINGS,
  ON_RESUME continues the same pending session) or Start anyway. First-time
  denials keep the old quiet behavior.

Verification: delimiter balance OK on both files; CI on push compiles.
Docs: changelog FIX bullet + app/AGENTS.md v229 entry.
