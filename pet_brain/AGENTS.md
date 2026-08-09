# Standalone Pet Brain — AGENTS.md

## Purpose

`pet_brain/` owns the offline PyTorch training, simulation, evaluation, speech
translation, checkpointing, and export scaffolding for Curio's future learned
pet. It is deliberately separate from the Android `app/` module.

## Ownership

- `model/` owns the stable 128-feature input schema, 64-action vocabulary,
  recreated Emotional v3 GRU, actor-critic heads, and checkpoint format.
- `environment/` owns physical room simulation, needs, consequences, curiosity,
  and bounded persistent memory. It must not encode a scripted policy.
- `training/` owns recurrent PPO, curriculum stages, and command-line training.
- `inference/` owns checkpoint execution and grounded short speech only.
- `export/` owns explicit recurrent-state export boundaries; no Android adapter
  belongs here until numerical parity is verified.

## Local Contracts

- The supplied raw `.bin` is not a checkpoint until a tensor manifest proves
  shape/order compatibility. Never silently reshape or convert it.
- `FEATURE_NAMES` ordering is an API contract. Any schema change requires a
  corresponding Android encoder contract and documentation update.
- Emotion and need channels are learned representations, not pretrained
  semantic truth. Reports must distinguish architecture from learned behavior.
- Speech may describe observed model outputs and executed actions but never
  select actions or replace the policy.
- Generated checkpoints, ONNX files, and caches remain ignored.

## Verification

- `python -m pet_brain.tests_schema` requires no third-party dependencies.
- After installing a compatible PyTorch build, run
  `python -m pet_brain.tests_smoke` and then
  `python -m training.train --episodes 10`.
- Run `python3 -m compileall -q pet_brain` and `git diff --check` before commit.
- Android Gradle commands are forbidden in this environment; Android
  integration remains a later, CI-verified task.

## Child DOX Index

None.
