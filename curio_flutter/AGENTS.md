# curio_flutter — the Windows/iOS client (Flutter/Dart)

This is the NEW Curio app for **Windows** (and, next, **iOS**). It is **not** a port
of the Android app: different language, different layout, different stores. It
shares exactly two things with `app/` — the topic catalog and the lane table.

Read `master.md` + the root `AGENTS.md` first. This file is the owning contract
for everything under `curio_flutter/`.

## The member's decisions (asked and answered, 2026-11-03)

- **Stack: Flutter (Dart).** One codebase for a Windows `.exe` and an iOS app.
- **The old `desktop/` module is gone** — this replaces it.
- **Windows first.** iOS follows in the same project.
- **iOS 26+ only**, with **native Liquid Glass** (`UIGlassEffect`). No simulated
  fallback below 26 is wanted; do not add one without asking.

## What this app is, in code

| Path | Owns |
|---|---|
| `lib/main.dart` | Entry point; boots `AppModel`, applies the theme. |
| `lib/shell/app_shell.dart` | **The Windows frame** — rail + context column + page. |
| `lib/screens/*` | The four rooms: Home, Spin, Cabinet, Settings. |
| `lib/store/app_model.dart` | Catalog, lane, deck, cabinet, prefs; `ChangeNotifier`. |
| `lib/data/topic_catalog.dart` | Loads bundled `assets/topics/<slug>.json`. |
| `lib/data/topic.dart` | The catalog model (reads the phone's own JSON shape). |
| `lib/data/lanes.dart` | **GENERATED** — slug, label, accent for all 38 lanes. |
| `lib/theme/curio_theme.dart` | Palette (mirrors `CurioColors`), spacing, themes. |
| `lib/widgets/curio_widgets.dart` | Header, chips, stats, empty states. |
| `tool/sync_assets.sh` | Bundles `data/topics/*.json` into `assets/topics/`. |
| `tool/sync_lanes.py` | Regenerates `lanes.dart` from the Kotlin catalog. |

## Hard rules

1. **A lane is declared ONCE, in Kotlin.** `lanes.dart` is generated; never
   hand-edit it. Adding a lane means editing `Category.kt` (and
   `CurioColors.kt` if it has a new accent), then running
   `python3 tool/sync_lanes.py`. A PR that adds a lane to the Dart table alone
   will be overwritten by the next generation.
2. **The catalog is never copied into git here.** `assets/topics/*.json` is
   `.gitignore`d and produced by `tool/sync_assets.sh` from `data/topics/`.
   A build without that step is a build with an empty catalog — the app says so
   (`CatalogMissing`) rather than failing silently.
3. **No Flutter SDK in the development environment.** Do not claim a Dart change
   compiles because it "looks right": `.github/workflows/curio-flutter.yml` is
   the only compile. Say so in the summary.
4. **`windows/` and `ios/` are generated, not hand-written.** They are
   `.gitignore`d because no SDK existed when this project was created; the CI job
   runs `flutter create --org com.curio --platforms=windows .` before building.
   Once a runner has been generated on a machine with the SDK, COMMIT it and
   delete the ignore lines.
5. **This app does not import anything from `app/`.** Shared state is the JSON
   catalog and the generated lane table — nothing else. Do not "sync" Android
   behaviour by porting widgets; the window wants its own layout.
6. **Two files, not a database.** `prefs.json` and `cabinet.json` under
   `%APPDATA%\Curio` (Windows), `~/Library/Application Support/Curio` (macOS),
   `~/.curio` (Linux). A corrupt file must never be overwritten blindly — the
   read paths keep the defaults and leave the file alone.

## The Windows frame (why it is not the phone's layout)

A Windows window is wide and has no thumb. So: a 76dp rail of rooms, a 300dp
context column that holds what the room is about (lanes / saved / device), and
the page. The deck is a stack of three with the front card face up. Cards are
tinted by their lane's own accent, with `inkOn()` choosing readable ink — the
same rule the phone uses, and for the same reason (a pale fill cannot carry
white text).

## iOS, when it starts

Same project, same `lib/`. `flutter create --platforms=ios .` adds the runner;
the shell swaps the rail/context column for a tab bar and one column, matching
Android's shape. The **only** Swift that should exist is a platform view wrapping
iOS 26 `UIGlassEffect` for the glass surfaces — native glass cannot be faked in
Dart, and the member chose the native look over a wider OS range.
