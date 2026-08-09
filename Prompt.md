# Request — Continue the optional neural pet-brain integration

## User decision
- First Android integration is toggleable, not always-on.
- Target runtime is ONNX Runtime Mobile.

## Completed
- Installed project-local CPU PyTorch in `.venv` (not global): `torch 2.13.0+cpu`.
- Schema test passed: exact 128 unique features and 64 action channels.
- Neural smoke test passed: model construction, recurrent hidden-state changes, gradients, checkpoint save/reload, and a reloaded episode.
- Ten-episode PPO smoke training passed and wrote checkpoints outside the repository. Reward movement during this short run is only a pipeline signal, not evidence of intelligence.
- ONNX export was structurally checked and numerically compared against PyTorch; the graph exposes explicit `observations`, `hidden_in`, and `hidden_out` state.
- Fixed the Python schema/environment mismatch by removing the simulator's undeclared `pet_direction_y` feature so the contract remains exactly 128 channels.
- Added an optional Android ONNX Runtime dependency and a separate neural opt-in setting, default OFF. The existing local `CurioPetBrain` setting remains independent.
- Added `NeuralPetBrain` with exact named ONNX outputs, recurrent hidden-state persistence, native resource cleanup on reset, optional external-data copying, and safe fallback on any unavailable/invalid model.
- Added `NeuralPetObservation` with conservative real-app signals and explicit zeroes for physical channels the Android app does not yet collect.
- Kept neural output as a constrained speech preview only. It does not claim to execute physical actions that the Android app does not model yet.
- Added asset documentation and a manifest gate. No ONNX model is committed: the ten-episode smoke artifact is not a production-trained pet.

## Validation
- `python -m pet_brain.tests_schema` passed.
- `python -m pet_brain.tests_smoke` passed.
- `python -m compileall -q pet_brain training inference` passed.
- `node scripts/check_braces.js` passed.
- `git diff --check` passed.
- Gradle was not run locally because the repository explicitly forbids Android Gradle builds here; CI remains the Android compilation source of truth.

## Remaining limitation
A longer-trained, numerically verified, behaviorally evaluated checkpoint plus a reviewed manifest must be produced before adding `pet_brain.onnx` to Android assets or presenting the neural setting as learned intelligence. The app currently keeps the Kotlin learning brain as the active fallback and the neural toggle OFF by default.
