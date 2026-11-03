# Curio for Windows (and iOS next)

A second Curio client, written fresh in **Dart / Flutter** — not a port of the
Android app's Kotlin. It reads the same content (the repo's `data/topics/*.json`)
and the same lane declarations (generated from the Kotlin catalog), and keeps
its own cabinet in its own folder.

Windows first, by decision. iOS follows in the same codebase, with the Android
app's layout and **native iOS 26 Liquid Glass** in place of this window shell.

## Why Flutter

One codebase for a Windows `.exe` and an iOS app; Windows packaging is a
first-class target; iOS can host a real native glass view through a platform
view, so "native" is not sacrificed for the shared UI.

## First run

There is **no generated `windows/` or `ios/` folder in git** — this project was
created in an environment without the Flutter SDK, so the platform runners are
made on a machine that has it:

```bash
cd curio_flutter
flutter create --org com.curio --platforms=windows .   # repeat with ios later
./tool/sync_assets.sh                                  # bundle data/topics/*.json
flutter pub get
flutter run -d windows
```

The `flutter create` step is idempotent: it adds the missing platform folder and
never touches `lib/`, `assets/` or `pubspec.yaml`. Once a runner exists it should
be **committed** (a Windows app needs its icon, manifest and CMake project) and
the `windows/` / `ios/` lines removed from `.gitignore`.

## Build

```bash
./tool/sync_assets.sh
flutter test          # the lane table + the catalog parser (pure Dart)
flutter build windows --release
# → build/windows/x64/runner/Release/
```

CI (`.github/workflows/curio-flutter.yml`) runs exactly these steps on
`windows-latest` and uploads the built folder as `curio-windows-<run number>`.

**On a `v*` tag it does one more thing:** it zips the Release folder into
`Curio-<tag>-windows.zip` and attaches it to the GitHub release the Android
workflow creates for the same tag. The upload is guarded — if the release does
not exist yet (the Android job may still be running) it says so and the run stays
green, with the build still available as a run artifact. It never creates a
release of its own, so it can never race Android's release notes.

## Layout of the code

| Path | What it is |
|---|---|
| `lib/main.dart` | Entry point; boots the model and hands it to the shell. |
| `lib/shell/app_shell.dart` | **The desktop frame**: rail + context column + page. |
| `lib/screens/` | The four rooms: Home, Spin, Cabinet, Settings. |
| `lib/store/app_model.dart` | Catalog, chosen lane, deck, cabinet, prefs; `ChangeNotifier`. |
| `lib/data/topic_catalog.dart` | Loads the bundled JSON from assets. |
| `lib/data/lanes.dart` | **Generated** lane table (slug, label, accent). |
| `lib/theme/curio_theme.dart` | Palette, spacing, light/dark themes. |
| `lib/widgets/curio_widgets.dart` | Page header, chips, stats, empty states. |
| `tool/sync_assets.sh` | Copies `data/topics/*.json` into `assets/topics/`. |
| `tool/sync_lanes.py` | Regenerates `lanes.dart` from the Kotlin catalog. |

## The one rule about data

The catalog is **never** copied into git here. `data/topics/` at the repo root is
the single copy; the Android build and this app both bundle from it. A content
edit therefore reaches both apps, and a lane rename or recolour is picked up by
running `python3 tool/sync_lanes.py`.

## Where the cabinet lives

| OS | Path |
|---|---|
| Windows | `%APPDATA%\Curio\cabinet.json` and `prefs.json` |
| macOS | `~/Library/Application Support/Curio/` |
| Linux | `~/.curio/` |

Two plain JSON files, written best-effort. No database, no account, nothing
leaves the machine.
