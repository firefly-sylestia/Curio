# Request — Remove the Pet Studio detail editor; keep Accessories visibility toggles

- User asked to remove the detail editor and keep only the ability to turn generated details on or off.
- Removed the dormant detail drawing card from `PetDesignerScreen.kt`, including detail-layer painting, blueprint, placement, canvas zoom, and clear-layer controls.
- Removed the Details picker category/cards and `PetEditorTarget.DetailLayer`, plus detail-editor-only state, reset plumbing, and tool branches.
- Kept Settings → Accessories as the only detail-facing UI. Its switches still call `PetDesign.withProceduralEnabled(...)` and remain connected to the runtime sprite.
- Preserved `PetDesign` detail layers, procedural visibility data, serialization, transforms, `CurioPetSprite` detail rendering, and Accessories thumbnails for runtime/data compatibility.
- Static validation pending: `node scripts/check_braces.js`, `git diff --check`, and stale-reference audit. Gradle compile/build/lint/test commands are forbidden in this environment; CI remains the compile gate.
- After validation/review: commit and push the follow-up change.
