# Request — restore pre-v8.32 Spin card animations only

## Completed

- Compared commit `0e60e2a32a73cbb0a444732e170aeb2bb64f389d` with its parent `27f7596d5cc7765fbbea0cf50d41b80e50318331`.
- Restored the pre-commit main-card heartbeat: the original soft spring pulse instead of the newer press/lift/settle timing.
- Restored the pre-commit hero content reel timing: the original 300ms/260ms shuffle transition without the later delay.
- Restored the pre-commit peek-card shuffle wipes: simultaneous 320ms/300ms transitions without the later cascade delays.
- Preserved topic swiping and ordering behavior: `onDeckCycle`, `cycleIndex`, slot/topic resolution, gesture handling, z-index, card geometry, and design were not reverted.

## Validation

- SpinScreen delimiter balance passed.
- `git diff --check` passed.
- Static review confirmed the diff is limited to animation constants and transition specs; no swipe/topic-switching or z-index changes were included.
- Gradle builds are forbidden locally by the Curio DOX rules; CI remains the compile gate.
