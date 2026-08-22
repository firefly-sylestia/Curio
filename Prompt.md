# Prompt.md — current request log

## Request (complete): revert Sans Flex fonts + fix the in-app update toast

1. **Font revert (surgical, not a git revert):** commits `a80ee1d`/`fcf862f` bundled the Sans Flex voice
   WITH the explore-session round-trip; user wanted only the fonts gone. Reverted by hand:
   - CurioTypography.kt: `SansFlexFontFamily` block deleted (+ its ExperimentalTextApi/FontVariation imports).
   - CurioBottomNav + SpinScreen: Changa One / FontWeight.Normal restored on tab labels and the
     Categories/Filter nav-style buttons.
   - HomeScreen: all 5 Sans Flex sites back to plain theme styles.
   - Deleted `res/font/roboto_flex.ttf` + `app/third_party/roboto_flex_OFL.txt`.
   - Changelog Fonts bullet removed (never shipped in an APK release → no REMOVE note).
   - AGENTS.md v226 entry: font bullet removed. All explore work untouched.

2. **Update toast fix** (`CurioInAppToast.kt`): the corner pill had NO width cap — long single-line text
   stretched it across the screen. Now capped at 300dp (`widthIn(max)`), text start-aligned with
   `weight(1f, fill=false)` so ellipsis engages inside the cap, symmetric slide-up+fade exit, TextAlign
   import dropped.

Verification: delimiter balance OK on all touched files; no SansFlex/roboto references remain;
Changa One imports restored (grep-verified). CI validates compile on push.
