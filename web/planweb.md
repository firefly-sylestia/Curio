# Curio Web App — Plan & Gap Analysis

Last updated: 2026-08-11

## Android Screens vs Web Screens

| # | Android Screen | Web Screen | Status |
|---|---------------|-----------|--------|
| 1 | **SplashScreen** | ❌ Missing | Not yet created |
| 2 | **OnboardingScreen** | ✅ Exists | Has torn hero, tour steps missing |
| 3 | **HomeScreen** | ✅ Exists | Torn hero ✓, menu drawer ✓, recents ✓, quest block missing Shuffle button in hero |
| 4 | **SpinScreen** | ✅ Exists | Fan deck ✓, Categories/Filter ✓, swipe-to-cycle missing, mixed deck missing |
| 5 | **CabinetScreen** | ✅ Exists | Torn hero ✓, search ✓, filter chips ✓, sort options missing |
| 6 | **ProfileScreen** | ✅ Exists | Torn hero ✓, stats ✓, achievements ✓, settings gear missing |
| 7 | **QuestsScreen** | ✅ Exists | Torn hero ✓, tabs ✓, level card ✓ |
| 8 | **PetDesignerScreen** | ✅ Exists | Pet picker with 6 presets, no editor |
| 9 | **TopicRevealScreen** | ✅ Exists | Hero card ✓, "Express yourself" ✓, quick-fact missing |
| 10 | **SaveCaptureScreen** | ✅ Exists | Voice recording ✓, mood chips ✓, paper cards ✓ |
| 11 | **EntryDetailScreen** | ✅ Exists | Torn hero ✓, audio player ✓, paper cards ✓, share button missing |
| 12 | **TopicBrowserScreen** | ✅ Exists | Torn hero ✓, search ✓, filter ✓, sort ✓ |
| 13 | **SettingsScreen** | ✅ Exists | Torn hero ✓, toggles ✓, sub-sections missing (backup, experiments) |
| 14 | **RecentScreen** | ❌ Missing | Android has dedicated /recents route |
| 15 | **TopicHistoryScreen** | ❌ Missing | Android has /topic-history route |
| 16 | **ManageCategoriesScreen** | ❌ Missing | Android has /manage-categories route |
| 17 | **LightboxScreen** | ❌ Missing | Android image lightbox |
| 18 | **SettingsSectionScreen** | ❌ Missing | Android sub-settings (appearance, data, about) |
| 19 | **ExperimentsScreen** | ❌ Missing | Android experiment toggles |
| 20 | **BackupToolsScreen** | ❌ Missing | Android backup/restore |
| 21 | **BugReportScreen** | ❌ Missing | Low priority |
| 22 | **SupportScreen** | ❌ Missing | Low priority |
| 23 | **CategoryPickerScreen** | ❌ Missing | Covered by category picker sheet in SpinScreen |
| 24 | **FieldMindObservationScreen** | ❌ Missing | Low priority |
| 25 | **PromoModeScreen** | ❌ Missing | Low priority |

---

## Existing Screen: UI Mismatches & Missing Functions

### 1. HomeScreen (`web/src/screens/HomeScreen.tsx`)

| Feature | Android | Web | Fix? |
|---------|---------|-----|------|
| Torn hero banner | ✅ Rose-wood, seeded tear | ✅ | OK |
| Menu drawer | ✅ Slide-in drawer | ✅ | OK (fixed — was navigating to /browse) |
| Profile pill | ✅ Top-right | ✅ Top-right | OK |
| "Surprise me" shuffle button | ✅ In hero banner | ❌ Missing | **Add** shuffle CTA inside hero |
| Quest block | ✅ "Today's Quest" with icon | ✅ Basic | OK |
| Recent entries | ✅ From DB + explore session | ✅ From IndexedDB | OK |
| Pinned topics/quotes | ✅ Section below recents | ❌ Missing | **Add** pinned section |
| Explore session resume | ✅ "Continue exploring" card | ❌ Missing | **Add** explore session resume |
| Category chip row | ✅ Below hero (older versions) | ❌ Missing | Low priority |

### 2. SpinScreen (`web/src/screens/SpinScreen.tsx`)

| Feature | Android | Web | Fix? |
|---------|---------|-----|------|
| Fan deck (hero + 2 peeks) | ✅ 444dp fan container | ✅ | OK (recently fixed) |
| Peek card dimensions | ✅ 360×116dp near, 328×96 far | ✅ 360×116px near only | Far peeks missing — low priority |
| Hero card gradient | ✅ Category-aware blend | ✅ | OK |
| Orbit ring on spin button | ✅ Animated dots ring | ✅ | OK |
| **Swipe to cycle cards** | ✅ Horizontal swipe gesture | ❌ Missing | **Add** swipe gesture on deck |
| Dice tumble animation | ✅ 3D rotateX/Y/Z | ✅ | OK |
| Confetti on landing | ✅ Particle burst | ✅ | OK |
| Auto-open after landing | ✅ 1.2s → Topic Reveal | ✅ | OK |
| Category picker sheet | ✅ Bottom sheet grid | ✅ | OK |
| Filter subtype sheet | ✅ Bottom sheet list | ✅ | OK |
| Mixed deck support | ✅ Multi-category blend | ❌ Missing | Future |
| Deck loading state | ✅ Skeleton/spinner | ❌ Missing | **Add** loading indicator |

### 3. TopicRevealScreen (`web/src/screens/TopicRevealScreen.tsx`)

| Feature | Android | Web | Fix? |
|---------|---------|-----|------|
| Hero card with gradient | ✅ Category tint | ✅ | OK |
| Subtype badge | ✅ Pill on card | ✅ | OK |
| Watermark glyph | ✅ Category icon | ✅ | OK |
| "Express yourself" button | ✅ Category-tinted | ✅ | OK |
| "Start exploring" button | ✅ Accent-filled | ✅ | OK |
| Quick fact section | ✅ Below hero | ❌ Missing | **Add** teaser/quick-fact |
| Related topics | ✅ "You might also like" | ❌ Missing | **Add** related topics |
| Difficulty indicator | ✅ 1-5 dots | ❌ Missing | **Add** difficulty dots |
| Action prompt | ✅ "Explore by..." | ✅ | OK |
| Browse button | ✅ Opens TopicDatabase | ❌ Missing | **Add** browse topics link |
| Pet reaction | ✅ Spins/celebrates | ❌ Missing | Future |

### 4. EntryDetailScreen (`web/src/screens/EntryDetailScreen.tsx`)

| Feature | Android | Web | Fix? |
|---------|---------|-----|------|
| Torn hero banner | ✅ Seeded tear | ✅ | OK |
| Frosted date bar | ✅ Date + type | ✅ | OK |
| Back + action pills | ✅ Back/edit/delete | ✅ Back/edit/delete | OK |
| Quick fact | ✅ Teaser display | ✅ | OK |
| Paper card rendering | ✅ Patrick Hand + ruled | ✅ | OK |
| Audio player (SoundBite) | ✅ Waveform + seek | ✅ | OK |
| Mood badge | ✅ Pill with icon | ✅ | OK |
| Quote cards | ✅ Accent left border | ✅ | OK |
| **Share button** | ✅ Share intent | ❌ Missing | **Add** share button |
| Edit entry route | ✅ /edit-entry/{id} | ❌ Missing | **Add** edit support |
| Lightbox for images | ✅ Full-screen viewer | ❌ Missing | Future |
| Tags display | ✅ Chips | ✅ | OK |

### 5. SaveCaptureScreen (`web/src/screens/SaveCaptureScreen.tsx`)

| Feature | Android | Web | Fix? |
|---------|---------|-----|------|
| Topic reminder strip | ✅ Category-tinted | ✅ | OK |
| Mood chips row | ✅ 6 moods with icons | ✅ | OK |
| Format selector chips | ✅ 5 formats | ✅ | OK |
| Multi-section take tabs | ✅ "Add take" | ❌ Missing | Future |
| SoundBite voice recording | ✅ MediaRecorder | ✅ | OK |
| Live waveform | ✅ Animated bars | ✅ | OK |
| Timer display | ✅ mm:ss | ✅ | OK |
| Paper card editors | ✅ Patrick Hand + ruled | ✅ | OK |
| Tag editor | ✅ Chips + input | ✅ | OK |
| Save CTA | ✅ Category-tinted | ✅ | OK |
| Draft autosave | ✅ Debounced store | ❌ Missing | Future |
| Discard confirmation | ✅ Dialog | ❌ Missing | **Add** back-navigation guard |

### 6. SettingsScreen (`web/src/screens/SettingsScreen.tsx`)

| Feature | Android | Web | Fix? |
|---------|---------|-----|------|
| Torn hero banner | ✅ Compact 180dp | ✅ | OK |
| Back pill | ✅ Top-left | ✅ | OK |
| Theme picker | ✅ 3 styles | ✅ | OK |
| Dark mode toggle | ✅ | ✅ | OK |
| Pastel colors toggle | ✅ | ✅ | OK |
| Hero gradient toggle | ✅ | ✅ | OK |
| **Pet chatter setting** | ✅ Quiet/Cozy/Talkative | ❌ Missing | **Add** |
| **Pet game frequency** | ✅ Relaxed/Normal/Eager | ❌ Missing | **Add** |
| **Voice-to-text toggle** | ✅ Settings section | ❌ Missing | **Add** |
| **Entry date & mood toggle** | ✅ Meta card toggle | ❌ Missing | **Add** |
| Sub-section navigation | ✅ Appearance/Data/About | ❌ Missing | **Add** section pages |
| Backup/Restore | ✅ Import/Export tools | ❌ Missing | **Add** backup tools |
| Experiments screen | ✅ Feature flags | ❌ Missing | **Add** experiments page |

### 7. CabinetScreen (`web/src/screens/CabinetScreen.tsx`)

| Feature | Android | Web | Fix? |
|---------|---------|-----|------|
| Torn hero banner | ✅ Category-matched | ✅ | OK |
| Search field | ✅ | ✅ | OK |
| Category filter chips | ✅ Horizontal scroll | ✅ | OK |
| Grouped by category | ✅ Section headers | ✅ | OK |
| Entry cards | ✅ Accent bar + icon | ✅ | OK |
| **Format filter** | ✅ Filter by format | ❌ Missing | **Add** format filter |
| **Sort options** | ✅ Newest/Oldest/A-Z | ❌ Missing | **Add** sort picker |
| Legacy entries view | ✅ Separate filter | ❌ Missing | Future |
| Empty state | ✅ Illustration + CTA | ✅ | OK |

### 8. ProfileScreen (`web/src/screens/ProfileScreen.tsx`)

| Feature | Android | Web | Fix? |
|---------|---------|-----|------|
| Torn hero banner | ✅ Rose-wood | ✅ | OK |
| Avatar + name + level | ✅ Inside hero | ✅ | OK |
| Stats grid | ✅ 4 cards | ✅ | OK |
| Pet card | ✅ Level + XP bar | ✅ | OK |
| Achievements | ✅ List with icons | ✅ | OK |
| Level progress | ✅ XP bar | ✅ | OK |
| **Settings gear icon** | ✅ Top-right of hero | ❌ Missing | **Add** settings nav button |
| **Daily challenge status** | ✅ Progress ring | ❌ Missing | **Add** daily quest preview |
| **Lanes/Passports** | ✅ Category stamps | ❌ Missing | Future |

### 9. QuestsScreen (`web/src/screens/QuestsScreen.tsx`)

| Feature | Android | Web | Fix? |
|---------|---------|-----|------|
| Torn hero banner | ✅ | ✅ | OK |
| Level card in hero | ✅ Glass panel | ✅ | OK |
| Daily quests tab | ✅ | ✅ | OK |
| Weekly quests tab | ✅ | ✅ | OK |
| Quest chains tab | ✅ | ✅ | OK |
| Badge display | ✅ Bonus pill | ✅ | OK |
| **Pet celebration on claim** | ✅ Quest complete reaction | ❌ Missing | Future |
| Progress animation | ✅ Animated bar fill | ❌ Missing | Low priority |

### 10. BottomNav (`web/src/components/BottomNav.tsx`)

| Feature | Android | Web | Fix? |
|---------|---------|-----|------|
| Home tab | ✅ | ✅ | OK |
| Spin tab | ✅ | ✅ | OK |
| Cabinet tab | ✅ | ✅ | OK |
| Active tab indicator | ✅ Accent pill | ✅ | OK |
| Tab labels | ✅ Text below icon | ✅ | OK |

### 11. FloatingPet (`web/src/components/FloatingPet.tsx`)

| Feature | Android | Web | Fix? |
|---------|---------|-----|------|
| Pixel art sprite | ✅ Canvas-drawn | ✅ | OK |
| Drag to move | ✅ Pointer events | ✅ | OK |
| Wander AI | ✅ Random walk | ✅ | OK |
| Dialogue bubbles | ✅ | ✅ | OK |
| Tap interaction | ✅ Squish + hop | ✅ | OK |
| Mood-based expressions | ✅ Eyes + cheeks | ✅ | OK |
| **Chatter frequency control** | ✅ Settings-driven | ❌ Missing | Wire to settings |
| **Game interactions** | ✅ Hide-and-seek etc | ❌ Missing | Future |
| Stage-based size | ✅ Baby/Evolved/Mature | ✅ | OK |

### 12. MenuDrawer (`web/src/components/MenuDrawer.tsx`)

| Feature | Android | Web | Fix? |
|---------|---------|-----|------|
| Slide-in animation | ✅ | ✅ | OK |
| Rose hero banner | ✅ | ✅ | OK |
| Browse Topics | ✅ | ✅ | OK |
| Quests & Levels | ✅ | ✅ | OK |
| Topic History | ✅ | ✅ (→ Cabinet) | OK |
| Settings | ✅ | ✅ | OK |
| Profile | ✅ | ✅ | OK |
| Dark mode indicator | ✅ Theme-aware | ✅ | OK |

---

## Missing Screens — Implementation Priority

### High Priority (user-visible gaps)

1. **SpinScreen swipe-to-cycle** — Add horizontal swipe gesture on fan deck to cycle through cards like Android (30 lines)
2. **Settings sub-pages** — Pet chatter, game frequency, voice-to-text toggles (50 lines)
3. **HomeScreen quest shuffle CTA** — "Surprise me" button inside hero banner (15 lines)
4. **TopicReveal quick-fact** — Teaser + difficulty dots below hero card (20 lines)
5. **EntryDetail share button** — Web Share API for sharing entries (15 lines)
6. **SaveCapture back-navigation guard** — Discard confirmation dialog (20 lines)

### Medium Priority

7. **Cabinet sort + format filter** — Sort picker (Newest/Oldest/A-Z) + format chip filter (40 lines)
8. **Settings Experiments page** — New screen with feature flags (requires new route, ~80 lines)
9. **Settings BackupTools page** — Export/Import JSON for entries (requires new route, ~80 lines)
10. **RecentScreen** — Dedicated all-recents page with search (new screen, ~100 lines)

### Low Priority

11. **TopicHistoryScreen** — Track which topics have been revealed (new screen, ~100 lines)
12. **ManageCategoriesScreen** — Reorder/hide categories (new screen, ~150 lines)
13. **SplashScreen** — Brief logo splash before onboarding (new screen, ~40 lines)
14. **LightboxScreen** — Full-screen image viewer (new screen, ~60 lines)
15. Pet game interactions (hide-and-seek, spark catch)
16. Mixed deck support in SpinScreen
17. Multi-section take tabs in SaveCapture

---

## Button / Function Parity Checklist

| Button/Function | Screen | Android | Web | Fix |
|-----------------|--------|---------|-----|-----|
| "Surprise me" shuffle | Home | ✅ | ❌ | Add to hero banner |
| Menu (☰) opens drawer | Home | ✅ | ✅ | Fixed |
| Profile pill → /profile | Home | ✅ | ✅ | OK |
| Swipe to cycle cards | Spin | ✅ | ❌ | Add gesture |
| Categories → picker sheet | Spin | ✅ | ✅ | OK |
| Filter → subtype sheet | Spin | ✅ | ✅ | OK |
| "Express yourself" → /capture | Reveal | ✅ | ✅ | OK |
| "Start exploring" → browse | Reveal | ✅ | ✅ | OK |
| Quick fact display | Reveal | ✅ | ❌ | Add |
| Back ← | Detail | ✅ | ✅ | OK |
| Edit ✏️ | Detail | ✅ | ✅ | OK |
| Delete 🗑 | Detail | ✅ | ✅ | OK |
| Share ↗ | Detail | ✅ | ❌ | Add |
| Back-navigation guard | Capture | ✅ | ❌ | Add |
| Sort picker | Cabinet | ✅ | ❌ | Add |
| Format filter | Cabinet | ✅ | ❌ | Add |
| Settings gear | Profile | ✅ | ❌ | Add |
| Pet chatter setting | Settings | ✅ | ❌ | Add |
| Pet games setting | Settings | ✅ | ❌ | Add |
| Voice-to-text toggle | Settings | ✅ | ❌ | Add |
| Back button | Settings | ✅ | ✅ | OK |
| Dark mode toggle | Settings | ✅ | ✅ | OK |
| Pastel colors toggle | Settings | ✅ | ✅ | OK |
| Save companion | PetDesigner | ✅ | ✅ | OK |
| Claim reward | Quests | ✅ | ✅ | OK |

---

## Summary

**DONE (12 screens):** Home, Spin, Cabinet, Profile, Quests, PetDesigner, TopicReveal, SaveCapture, EntryDetail, TopicBrowser, Settings, Onboarding — all have torn hero banners, matching layouts, and core functionality.

**REMAINING — Quick wins (~10 changes, ~200 lines total):**
1. HomeScreen: Add "Surprise me" shuffle button in hero
2. SpinScreen: Add swipe gesture to cycle fan deck
3. TopicReveal: Add quick-fact + difficulty dots
4. EntryDetail: Add share button (Web Share API)
5. SaveCapture: Add discard confirmation dialog
6. Cabinet: Add sort picker + format filter
7. Profile: Add settings gear navigation button
8. Settings: Add pet chatter, game frequency, voice-to-text toggles
9. Settings: Add Experiments sub-page route
10. Settings: Add BackupTools sub-page route

**REMAINING — New screens (~4 screens, ~350 lines total):**
1. RecentScreen (all recents page)
2. TopicHistoryScreen
3. ExperimentsScreen
4. BackupToolsScreen
