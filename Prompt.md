# Request — Reimagine evolved pets, move evolved art to 64×64, redesign home, remove home editor

## User request
- Keep the baby pet version as-is.
- Redesign the evolved forms because the current evolved forms are not good.
- Upgrade evolved pet art/editor resolution to 64×64 for more detail.
- Give each evolved path its own accessories.
- Redesign the pet's home because the current home does not look good.
- Remove the home editor.

## Completed
- Preserved the baby form and its original 16×16 body/curl art.
- Evolved default forms now use a detailed 64×64 body/curl canvas, while older saved 16×16, 24×24, and 32×32 designs remain readable and are not silently resized.
- Fire, Water, and Nature evolved paths now receive distinct accessory pixel layers; final evolution adds its crown treatment. The existing Accessories visibility control hides authored and generated accessory art consistently.
- Replaced the editable flower-bed presentation with a fixed layered house/home sprite scene used from Home and the companion area.
- Removed the Home section, bed editor entry, dialog, and helper UI from Pet Studio. Existing persisted bed rows remain dormant compatibility data rather than being deleted.
- Kept animation/detail/action models and runtime behavior available for future UI re-entry while the related editors remain hidden.

## Validation
- `node scripts/check_braces.js` passed: 125 files checked.
- `git diff --check` passed.
- Static audits found no stale home-editor symbols or removed home-editor imports in app source.
- Local Gradle compile/build/lint/test commands were not run per repository rules; CI remains the Kotlin/Android compile gate.
- Code review found no remaining blocker.
