# Request — Pet Studio Faces-only editor + hide Details and Actions UI (DONE)

- User confirmed: replace the Faces editor's whole-pet presentation with face-only previews and mood face choices; remove the face zoom slider; add a Painting on/off toggle in the Paint this face area; make the face pixel board creamy.
- User also confirmed: remove the Details drawing editor, keep only detail visibility toggles, and remove the entire Actions area. Preserve detail/reaction/custom-action data and runtime behavior for future re-entry.
- Implemented in `PetDesignerScreen.kt`:
  - Added face-only `FaceOnlyPreview` and `FaceMoodPickerCard` components for mood choices and the selected-face preview; whole-pet previews are no longer used in the Faces picker/editor.
  - Added a `Painting` toggle. Turning it off clears the active paint tool and disables face-grid gestures; the existing blueprint toggle remains available.
  - Removed the Faces zoom slider and fixed the face board to drawing-size fit behavior.
  - Applied `CurioColors.SoftCream` to the face pixel board.
  - Removed Details and Actions categories from the picker; detail drawing, reactions, and custom-action editor surfaces are dormant and unreachable. Settings → Accessories keeps only generated-part visibility switches, with no Draw it shortcut.
  - Preserved `PetDesign` detail/reaction/custom-action models, serialization, and runtime playback logic.
- Documentation/release updates: app contract notes and version metadata now use versionCode `20260918`; added `fastlane/metadata/android/en-US/changelogs/20260918.txt`.
- Verification: `node scripts/check_braces.js` passed for 125 files; `git diff --check` passed; stale Face zoom/Details/Actions picker/Draw it references are absent; code review found no blockers. Gradle compile/build/lint/test commands are forbidden here; CI remains the compile gate.
- Next: commit and push the completed change.
