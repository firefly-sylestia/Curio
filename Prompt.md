# Prompt.md — Request Log

## Current Request (IN PROGRESS): Web app creation — full feature parity

**Date:** 2026-08-11

**What was asked:** Create a web version of Curio that mirrors the Android app's UI design and database. The web app should:
- Live in the `web/` directory
- Not be included in the Android build
- Use React + TypeScript with Vite and Tailwind CSS
- Use IndexedDB for local storage (mirroring Room database)
- No authentication required (local only)
- Full feature parity with Android app

**Changes made (so far):**

1. **Project setup:**
   - Created `web/` directory with React + TypeScript project via Vite
   - Installed Tailwind CSS v3, React Router, and IDB (IndexedDB wrapper)
   - Configured Tailwind with Curio's color palette

2. **Type definitions (`web/src/types/index.ts`):**
   - Defined TypeScript types mirroring Android data models
   - CategoryId, CurioCategory, CaptureFormat, CurioTopic, ExploreAction
   - CaptureEntity, CurioEntry, CaptureData variants
   - PetDefinition, Quest, ThemeSettings, UserPreferences

3. **Category data (`web/src/data/categories.ts`):**
   - All 21 categories with colors, icons, families
   - Category color system (accent, ink, tint)
   - Helper functions for gradients and lookups

4. **IndexedDB setup (`web/src/db/database.ts`):**
   - Database schema matching Room's captures table
   - CRUD operations for captures
   - Indexes on category, date, and topic

5. **Theme system (`web/src/theme/ThemeContext.tsx`):**
   - React context for theme state
   - Supports Curio/AMOLED/Material styles
   - Dark/light mode, pastel colors, tint wash, hero gradient toggles
   - Theme-aware color helper functions

6. **Components:**
   - `CurioCard` - Base card component with variants (default, hero, compact)
   - `CategoryCard` - Grid display for categories
   - `HeroCard` - Main spin screen card
   - `BottomNav` - Bottom navigation bar

7. **Screens:**
   - `HomeScreen` - Hero, streak, category chips, recently explored
   - `SpinScreen` - Category selector, spin dial, animation
   - `CabinetScreen` - Saved entries grid with filtering
   - `ProfileScreen` - Stats, achievements, pet display
   - `SettingsScreen` - Theme toggles, data management
   - `TopicRevealScreen` - Topic details, explore action, save flow

8. **Routing (`web/src/App.tsx`):**
   - React Router setup with all routes
   - Placeholder screens for unimplemented features

9. **Configuration:**
   - Updated `.gitignore` to exclude web build artifacts
   - Updated `AGENTS.md` to document web directory

**What's done:**
- Core project structure ✓
- Type system ✓
- Category data ✓
- IndexedDB storage ✓
- Theme system ✓
- Basic components ✓
- 6 main screens ✓
- Routing ✓

**What's remaining:**
- Topic data loading from JSON assets
- Pet system (designer, floating pet, dialogues)
- Quest system
- Explore sessions with timers
- More components (CurioButton, CurioIcon, etc.)
- Responsive design polish
- Additional screens (Onboarding, Pet Designer, Quests, etc.)

**Validation:**
- Project structure created
- TypeScript types defined
- Components and screens created
- No build validation yet (npm run build)

## Previous Requests

[See previous request logs in git history]
