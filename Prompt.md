# Prompt.md — Request log

## Current request — COMPLETED: offline model picker quality-tier badges (v140)

The user picked the "Tier badges" suggestion: add a Small / Large / Full quality-tier
badge with accuracy hints to each row of the offline model picker.

### Changes
- **`VoskModels.Tier` enum** (OfflineTranscriber.kt) — `SMALL("Small", "fast & light")`,
  `LARGE("Large", "more accurate")`, `FULL("Full", "most accurate")`; added as a
  required `tier` field on `Info` and set per catalog entry (3 smalls → SMALL,
  en-us-0.22-lgraph → LARGE, the three Full models → FULL). No other Info construction
  sites; Info isn't serialized so Gson/backup are unaffected.
- **Picker rows** (SettingsSharedComponents `OfflineModelDialog`) — between the model
  name and the langLabel·sizeLabel subtitle, each row now shows a compact tinted badge:
  "Small · fast & light" / "Large · more accurate" / "Full · most accurate", colored with
  the existing theme-aware inks — `curioSageInk()` (green), `curioGoldInk()` (amber),
  `curioRoseInk()` (rose) — fill = ink at 16% alpha; selected rows flip to white on the
  solid amber fill. Name column got `verticalArrangement = spacedBy(3.dp)`.

### Verification
No Gradle build in this environment (project rule — CI validates on push). On-device:
open Settings → Recording → Offline model — each row should show its tinted tier badge,
readable in light and dark, and white-on-amber on the selected row.
