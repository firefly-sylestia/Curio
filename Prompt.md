# Prompt.md — current request log

## Request (complete): auto-backup frequency + cabinet→detail entrance fix

1. **Auto backup frequency (added + made reliable):**
   - `AppPreferences`: `autoBackupFrequencyDaysOptions = [1,3,7]`, `get/setAutoBackupFrequencyDays`
     (KEY `auto_backup_frequency_days`), reactive `autoBackupFrequencyDaysState`, seeded on startup.
   - `BackupToolsScreen` (Settings → Backup & restore → Auto backup): a "Backup frequency" chip row
     (Daily / Every 3 days / Weekly, solid primary fill = selected) shows when auto backup is enabled;
     the toggle subtitle now reflects the chosen cadence.
   - `MainActivity`: new `runAutoBackupIfDue()` replaces the hardcoded-24h inline block — interval =
     chosen days × 24h; called from onCreate AND onResume so warm processes still fire on schedule
     ("it should work too" — the old onCreate-only hook could go days without firing). Due-date gate
     keeps repeat calls no-ops.

2. **Cabinet→detail entrance:** `DetailContentEntrance` faded the body in with `tween(400,
   delayMillis=200)` — a delay paced to the OLD shared morph that v8.38 replaced with a center pop-up.
   Result: quick fact + entry sat invisible ~200ms after opening. Now `tween(260)`, no delay.

Verification: delimiter balance OK on all touched files; imports verified (mutableIntStateOf,
Surface/RoundedCornerShape/Spacer/height in BackupToolsScreen, lifecycleScope/Uri in MainActivity).
CI validates compile on push.
