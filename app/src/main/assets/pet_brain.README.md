# Optional neural pet brain assets

The Android app has an opt-in inference boundary for a verified recurrent
model through ONNX Runtime Mobile. It becomes available only when
`pet_brain.onnx` is placed directly in this `assets/` directory:

- `pet_brain.onnx` — graph and metadata
- `pet_brain.onnx.data` — optional external initializer payload when produced
  by the Python exporter

If the sidecar exists, both files must come from the same export. The app
copies the graph and optional sidecar to internal storage before creating the
ONNX Runtime session so external initializers can be resolved safely.

The files are intentionally not committed yet. The current checkpoint is a
10-episode smoke-training artifact, useful for proving the pipeline but not a
production-trained pet. A real model must also ship `pet_brain.manifest.json`
with `verified_for_android: true`, `training_status: "evaluated"`, and the
matching input/hidden/action sizes. Until those reviewed assets are added, the
neural adapter is unavailable and the existing Kotlin `CurioPetBrain` remains
the active behavior/dialogue brain.

The model contract is:

- observations: `[1, 1, 128]`
- hidden_in / hidden_out: `[1, 1, 1536]`
- action logits: `[1, 1, 64]`
- emotion: `[1, 1, 24]`
- needs: `[1, 1, 16]`
- memory: `[1, 1, 256]`

The Android adapter currently exposes the model outputs to a constrained
speech preview only; it does not pretend to execute physical actions that the
Android app does not yet model. The neural setting is separate from the
existing local learning-brain setting and defaults OFF.
