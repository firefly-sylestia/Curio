# Request — v8.18: pet landmarks on more screens

Follow-up to v8.17 (PLAY landmark kind — the pet's jig at its flower bed,
`6bb136d`). Shipped as a LOCAL commit only — the user asked to hold the push.

## What the user asked

Add pet landmarks to more screens — the Save button on the capture screen,
the Cabinet's grid, and the Quests cards — so the pet interacts with more of
the app.

## Changes (4 files)

| File | Change |
| --- | --- |
| `ui/components/CurioSettingsCard.kt` | Gains a defaulted `modifier: Modifier = Modifier` param (applied as `modifier.fillMaxWidth()`) — backward compatible, all existing callers unchanged. Needed so quest cards can take the landmark modifier. |
| `features/quests/QuestsScreen.kt` | `CurrentQuestCard` wrapped in `PetLandmark("quest", FUN, "quests")` (the pet dashes over and boops the active quest) and `DailyCard` in `PetLandmark("daily", CURIOUS, "quests")` (tiptoes over and reads today's quests). Both cards thread a defaulted `modifier` param into `CurioSettingsCard`. |
| `features/capture/SaveCaptureScreen.kt` | The sticky Save CTA wrapped in `PetLandmark("save", FUN, "capture")` — the pet boops it while you write (route prefix matches the pet overlay's `capture` screen). |
| `features/cabinet/CabinetScreen.kt` | The `LazyVerticalGrid` wrapped in `PetLandmark("grid", CURIOUS, "cabinet")` — the pet tiptoes over and peeks at your keepsakes; the whole shelf springs a beat (draw-only `graphicsLayer` pulse, sticky chips/hero stay put). |

Landmark coverage now: home (greeting CURIOUS, flower bed PLAY), spin (Shuffle
FUN), profile (avatar FUN), capture (Save FUN), cabinet (grid CURIOUS),
quests (current quest FUN, today's quests CURIOUS).

## Validation

- Brace balance ALL OK (4 files), `git diff --check` clean.
- Reviewer (code-reviewer-deepseek-flash) passed.

## Completion summary

v8.18 shipped: four new landmarks across the capture screen (Save button),
Cabinet (grid) and Quests (current + daily cards). **Committed locally,
NOT pushed** — per the user's request; push is pending.
