# Request — Fix CurioFloatingPet CI compile failure

## Reported CI failure
`CurioFloatingPet.kt:1221` reported unresolved `lifeFrame`, syntax error, condition mismatch, and unsafe nullable access in both debug and release compilation.

## Cause
The `activeView` `when` branch had been accidentally collapsed into one malformed line:
`caFrame != null -> caFrame.view lifeFrame != null ...`

## Fix
Restored the intended multiline Kotlin `when` expression:
- custom action frame view when present;
- non-front Pet Life frame view when present;
- otherwise the routine view.

## Validation
- `node scripts/check_braces.js` passed.
- `git diff --check` passed.
- Focused review found no remaining immediate blocker in the active-frame block.
- Gradle was not run locally per root DOX; CI remains the source of truth.

## Next work
Resume the project-local PyTorch smoke test and toggleable ONNX Runtime Mobile integration after this CI fix is pushed.
