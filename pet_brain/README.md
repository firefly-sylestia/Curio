# Curio Pet Brain

A separate PyTorch training project for Curio's artificial pet. It is not part
of the Android Gradle build and does not alter the existing on-device
`CurioPetBrain.kt` personality layer.

## Important model provenance

`pet_brain_emotional_v3.zip` is a supplied, randomly initialized source
artifact. Its raw binary has 8,225,640 float32 values but no tensor manifest.
The supplied reference `nn.GRU` implementation requires 8,230,248 parameters,
so this project does **not** reshape or load the binary. See
`model/ARCHITECTURE.md`. The PyTorch model is recreated with fresh explicit
initialization and carries `randomly_initialized: true` in checkpoints.

## Install

Use a virtual environment and install PyTorch for the target CPU/GPU from the
official PyTorch selector. Optional export/evaluation packages can be added
later; no dependency is installed by this repository. The project-local smoke
workflow uses CPU PyTorch and keeps all generated checkpoints outside source
control.

```bash
python -m venv .venv
. .venv/bin/activate
# Install a compatible torch build from https://pytorch.org/get-started/locally/
```

## Train

From the repository root:

```bash
python -m pet_brain.tests_schema
python -m pet_brain.tests_smoke
python -m training.train --episodes 10
python -m training.train --episodes 10000
python -m training.train --steps 1000000
python -m training.train --resume pet_brain/checkpoints/latest.pt
```

The smoke run saves `pet_brain/checkpoints/latest.pt` and an episode checkpoint
every ten episodes. Logs include reward, PPO losses, entropy, and intrinsic
curiosity reward. The rollout uses recurrent hidden state and the update uses
truncated sequence backpropagation per episode.

## Evaluate and run inference

```bash
python -m training.evaluate pet_brain/checkpoints/latest.pt
python -m inference.run_pet pet_brain/checkpoints/latest.pt
```

Evaluation runs randomized seeds and reports measured reward/events. It does
not claim the model learned a behavior unless a comparison experiment shows it.
The environment is intentionally small and includes separate future probes for
novel objects, changed rewards, owner patterns, routines, and unseen rooms.

## Structure

- `model/schema.py`: exact 128-feature order, 24 emotion channels, 16 need
  channels, and 64 output channels.
- `model/pet_brain.py`: 1,536-unit GRU actor-critic with memory, emotion,
  needs, scalar value, and action-conditioned next-state head.
- `environment/`: room physics, needs, objects, owner, consequences, curiosity,
  and bounded decaying persistent memory.
- `training/`: recurrent PPO, trajectory storage, curriculum stages, and CLI.
- `inference/`: checkpoint runner and constrained speech translation. Speech
  reads actual outputs/actions and never selects behavior.
- `export/`: explicit hidden-state ONNX export; TFLite remains disabled until
  an ONNX graph is numerically verified.

The simulator defines physical affordances and safety only. It does not contain
an authored policy such as “see ball → fetch”; action selection comes from the
trainable policy distribution.

## Android boundary

The Android module now contains an opt-in ONNX Runtime Mobile adapter, but it
is deliberately inert until a verified `pet_brain.onnx` asset is added to
`app/src/main/assets/`. The adapter accepts the exact 128-feature order,
passes explicit `[1, 1, 1536]` recurrent state, and persists `hidden_out`
separately from model weights. The existing Kotlin brain remains the fallback.
Do not ship the 10-episode smoke checkpoint as production intelligence: it
only proves loading, gradients, checkpointing, and the export boundary.
Online learning should remain optional and should collect experience rather
than mutate global weights after every interaction.
