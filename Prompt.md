# Request — Remove Faces editor and stabilize Topic Reveal morph

## User request
Remove the Faces editor UI and its option from Pet Designer. Keep the underlying face data/runtime available. Keep the normal bottom navigation visible on Topic Reveal so the shared Spin → Reveal morph does not change content height.

## Changes completed
- Removed the active reaction face-canvas editor from Pet Designer.
- Removed mood-face preview cards, face blueprint helpers, and the face picker path from the studio UI.
- Removed the personality-presets section that exposed bulk mood-face editing.
- Preserved `PetFace`, mood data, reaction data, serializers, presets, and sprite rendering for compatibility.
- Kept only the minimal eye-blueprint helper needed by the dormant animation timeline implementation.
- Added Topic Reveal to bottom-navigation route visibility so the Scaffold reserves the same bar height during the morph.
- Kept Shuffle selected while Reveal is open on both phone bottom navigation and wide-window navigation rail.
- Updated the app DOX contract and store changelog to remove stale Faces-editor wording and document Reveal navigation behavior.

## Validation
- `node scripts/check_braces.js` passed: 125 files checked.
- `git diff --check` passed.
- Focused symbol search found no remaining Pet Designer Faces-editor UI symbols or `PetEditorTarget.Face`.
- Focused review checked phone/rail route selection, morph height behavior, and runtime face-data preservation.
- Local Gradle compile/build/lint/test commands were not run because repository instructions forbid local Android builds; CI remains authoritative.

## Status
Implementation and static validation are complete. Per the user's standing preference, ask for confirmation before committing and pushing.
