# Prompt.md — Current Request Log

## Request (2026-08-07): Draft quest redesign and Curio pet system plan

**User request:** Create a `.md` file only, with no app code changes, that fully designs a redesigned Quests experience, improved first tutorial/navigation flow, category-discovery quest logic, satisfying/addictive feedback, and a cute pixelated AI Curio pet that lives over the app and grows from quest XP.

### Analysis
- This is a documentation/planning task, not implementation.
- Root and app DOX were read before editing.
- Relevant current implementation context read:
  - `app/src/main/java/com/curio/app/features/quests/QuestsScreen.kt`
  - `app/src/main/java/com/curio/app/data/CurioQuests.kt`
  - `app/src/main/java/com/curio/app/data/QuestGuide.kt`
- No Gradle/build commands should be run per root AGENTS.md.
- The requested redesign adds future features; because the user explicitly asked for design instructions only and no code implementation, no toggleability decision is needed in this turn. The design doc should still instruct future implementers to ask/gate experiments before implementation.

### Plan
1. Add a dedicated Markdown design spec under `app/` so it sits with the app-module planning docs.
2. Include UI hierarchy, interaction logic, quest taxonomy, onboarding tour requirements, Curio pet growth/AI behavior, data model notes, implementation phases, QA acceptance criteria, and explicit non-goals.
3. Run text/static checks only (`git diff --check`, review file contents); do not run Gradle.
4. Commit, push, and open a PR.

### Completion summary
- Added `app/QUEST_AND_PET_REDESIGN_SPEC.md` with the complete quest redesign, tutorial flow, category discovery logic, feedback system, and Curio pet plan.
- No app source code changed.
