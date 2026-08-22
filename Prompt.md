# Prompt.md — current request log

## Request (complete): Explore-session round-trip + Sans Flex voice (v226)

User asked (across messages): rename "Done and write about it" → **Express yourself** and restyle
the Keep-exploring pill on Home + dialog; stop showing the done dialog every app open; auto-pause a
session when the user closes the explore/app; fix the notification's inner timer text that lagged
behind the chronometer; add Cancel to the floating explore bubble; make cancelled sessions
recoverable; and apply the Sans Flex font changes. User decisions: recovery = resume card on Home;
notification = drop the inner timer text; skip re-checking quest/level backup restore.

### Done (commit pending)
- `MainActivity.onDestroy` auto-pauses the active session on real close (`!isChangingConfigurations`),
  re-arms the service so the shade flips to Paused.
- Recovery: `stashCancelledSession` / `resumeCancelledSession` in ExploreSessionStore +
  `CancelledExploreRow` on Home (gated on no active session). Dialog confirm-cancel, Home stop, and
  the bubble's new Cancel button all stash first.
- Bubble: `onCancel` param + Delete-glyph button in the expanded control row; service wires it to the
  notification-Cancel teardown.
- Notification staleness: live notif drops collapsed content text while running (shade chronometer is
  the timer); bubble-only quiet notif drops its elapsed line; paused keeps frozen readout.
- Dialog once per session (`dialogDismissedFor`, keyed by startMillis). Home CTA "Express yourself",
  Keep-exploring pill restyled on Home + dialog.
- Fonts: `SansFlexFontFamily` (Roboto Flex variable TTF, real wght instances 400/600/700/800) on nav
  tab labels, rail labels, Spin Categories/Filter pills, Home subtitle rows.

Verification: delimiter balance OK on all touched files; single-call-site checks for new params;
formatElapsed still referenced. Gradle forbidden here — CI validates compile on push.
Docs: store changelog 20260920.txt bullets, app/AGENTS.md v226 entry.

## NEXT REQUEST (queued): live-notification logic from appsfolder/livebridge + liquid-glass nav bar
User wants:
1. Fix/rebuild the live notification using the logic of https://github.com/appsfolder/livebridge
   (verify the actual repo — name may differ).
2. Copy the liquid-glass navigation bar style/effect from https://github.com/ChaoMixian/vFlow as an
   EXTRA Appearance option (user already said "extra option" — toggleable, per DOX rules ship as a
   settings option). Respect licenses — reimplement the visual effect in Compose, don't vendor code
   wholesale without checking the license.
