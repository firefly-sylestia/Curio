# 🌍 Curio

> **Explore something. Notice more. Keep the discovery.**

A curated discovery app for curious people. Spin a roulette deck of 16,000+ hand-written topics across 38 categories, then capture what you notice in a beautiful, personal offline library. Share your discoveries as stunning, topic-specific cards. No accounts. No feed. No ads. Just you, a topic, and the world.

---

## About

We scroll past interesting things every day and forget them. Curio turns passive scrolling into active, real-world exploration — and gives you a warm, tactile place to keep what you found. Six rich capture formats (voice notes, reviews, journals, mood boards, field notes, and more) live on a paper-textured editor with ruled lines, torn edges, and coffee stains. Everything stays on your device. Your journal is yours.

Built with **Kotlin + Jetpack Compose** on Android 8.0+.

---

## ✨ Features

### 🎡 The Spin — Your Discovery Deck
Pick from 38 curated categories (Music, Films, Books, Art, Science, Games, Anime, Food, History, Sports, Internet, Mythology, and more) — or blend into a mixed deck. Curio shuffles a roulette of topics, each with a teaser, imagery, and a structured "go do this" explore action with a suggested time. The deck *feels* like a deck: stacked peek cards, category-tinted edges, shadows, and a satisfying shuffle. Anti-repeat history means you'll never see the same topic back-to-back.

### 📝 Six Capture Formats on Paper-Textured Notes
Whatever you discover, there's a format waiting: **SoundBite** (voice notes) · **ReelNotes** (reviews) · **Marginalia** (journals) · **Gallery Wall** (mood boards) · **Field Notes** (observations) · **Open Notebook** (wildcard). Every note lives on a rich, paper-styled editor with ruled lines, torn edges, coffee stains, and watermark paper. Full rich text, quotes, images, audio, and tags — all autosaving.

### ⏱️ Explore Sessions with Floating Timer Bubble
Start a timed session and a persistent floating bubble follows you over other apps (like Messenger) — so you can open the browser, dig in, and always see how much time you planned. Sessions survive reboots and queue for later.

### 🗄️ The Cabinet — Your Personal Museum
Browse by category, search your collection, zoom images in a lightbox, and explore rich entry pages. Streaks, levels, and lane stats make the habit rewarding.

### 🎨 Design Identity
A **Material-inspired custom design language** — not a hand-invented look, but one assembled from the established design language of the modern web. Material 3's open design system is the foundation; on top sits a warm-cream paper world drawn from the editorial, tactile aesthetic of today's best apps: torn-paper heroes, category colors, custom typography, and motion throughout. Three theme styles (Curio, AMOLED, Material You).

### 🔒 Privacy First
Everything lives on your device. No accounts. No analytics. No tracking. Open source.

---

## 🚀 Quick Start

### Install from Release APK

1. Download the latest `release` APK from the [Releases page](https://github.com/firefly-sylestia/Curio/releases)
2. Install it on any Android 8.0+ device
3. Android will ask you to allow "install from unknown sources" for your browser — that's normal for sideloaded apps

> **Google Play support is not planned.** v1.0 ships via GitHub Releases.

### Build from Source

```bash
git clone https://github.com/firefly-sylestia/Curio.git
cd Curio
./gradlew build
```

**Requirements:**
- Android Studio (latest stable)
- Kotlin 2.3+
- Gradle 9.4+
- Android SDK 26+ (API level)

For a signed release build:
```bash
./gradlew bundleRelease
```

---

## 📖 Usage

1. **Discover** — Open the Spin on Home, pick a lane or create a mixed deck, and hit shuffle
2. **Explore** — Read the explore action and step-by-step guide, then go find the topic in the real world
3. **Capture** — When done exploring, tap the floating timer bubble (or open Curio) and pick a format to capture what you noticed
4. **Keep** — Your entry lands in the Cabinet, tagged and searchable, ready to revisit anytime

Start with **"Today's Quest"** (Home → Today's Quest) for a one-tap random discovery whenever the mood strikes.

---

## 📋 System Requirements

| | |
|---|---|
| **Platform** | Android (v1.0) |
| **Minimum OS** | Android 8.0 (API 26) and up |
| **Target OS** | Android 17 (API 37) |
| **Permissions** | Microphone (voice notes + dictation) · Notifications (reminders) · "Display over other apps" (optional floating explore bubble) · Background service (explore timer) |
| **Language** | English |
| **Accounts** | None — Curio works completely offline |
| **Network** | Optional. All your data is fully local; images load from the web when available |

---

## 📚 Complete Feature List

### Discovery — The Spin
- **38 curated categories**: Music · Films · Books · Art · Science · Games · Anime · Food · History · Sports · Internet · Mythology · and more
- **16,000+ topics** across 38 categories (500+ in every lane) — each with teaser, imagery, and structured explore action (verb, target, suggested duration, step-by-step guide)
- Roulette deck with stacked cards, category-tinted edges, shadows, and satisfying shuffle
- Single-lane or mixed decks (2–4 categories blended into one)
- Anti-repeat history — never see the same topic twice in a row
- Topic reveal pages with full explore action
- **Browse Topics** — the whole catalog in one place: search, filter by lane, and sort A–Z or newest/oldest by year
- "Today's Quest" — one-tap random shuffle on Home
- Manage Categories — show, hide, or reorder lanes

### Capturing
- **Six rich formats** — SoundBite, ReelNotes, Marginalia, Gallery Wall, Field Notes, Open Notebook
- **Multi-section entries** — mix formats within one entry
- **Paper-styled editor** — ruled lines, torn edges, coffee stains, folded corners, red margins, watermark paper, six colors
- **Rich text** — formatting, quotes, images, audio, tags (custom, searchable)
- **Voice recording** — trim with waveform, playback, voice-to-text transcription
- **Mood boards** — draggable quote boxes on category-tinted backgrounds; export as PNG
- Autosaving drafts

### Exploring
- **Timed explore sessions** with floating timer bubble over other apps
- Persistent notification + "are you done?" reminder
- Sessions survive reboots; pause, queue, and resume
- Daily shuffle reminders (scheduled notifications)
- Recently explored/unexplored topic tracking on Home

### Share Cards
- **8 share card styles** — Paper, Vinyl, Collage, Clean, Editorial, Minimal, Signature, and Custom
- **Custom style** — 50+ unique topic-specific designs (Pokemon pokeball, Frozen snowflakes, Star Wars starfield, Marvel halftone, etc.) that only appear when a matching design exists
- **Signature style** — 13 category-specific designs (music vinyl grooves, film grain, book spine, anime speed lines, etc.)
- **Carousel picker** — swipe through full-size card previews to pick a style
- 9:12 and 3:4 aspect ratios
- Quick fact, custom fact, and no-fact options
- Save as image or share directly

### The Cabinet (Library)
- Browse all captures; filter by category or view All
- **Full-text search** across your library
- Rich entry detail pages — hero cards, metadata, image lightbox with pinch-to-zoom
- Edit entries after saving
- Saved shelf — bookmarked quotes, pinned topics
- Legacy section for imported FieldMind records
- Recents feed on Home

### Quests & the Curio Pet
- **Daily quests** — a fresh warm-up / discovery / creation trio every day, with bonus quests that unlock when the core trio is claimed
- **Weekly quests** — three rotating goals refreshed every Monday
- **Category passport** — stamps for every lane you explore; quests nudge you toward underexplored categories
- **Journey chains** — first-run tutorial quests that walk you through the whole Discover → Explore → Capture loop
- **XP + 50 levels** — quest completion, spins, explores, and saves earn XP; levels unlock titles and grow the pet
- **Curio pet companion** — a pixel pet that lives on your screen, reacts to exploration, and grows with XP across six growth stages
- **Pet brain** — the pet learns from your habits on-device (favorite lanes, time-of-day rhythms, streaks) and develops its own catchphrases over time
- **Pet designer** — a full pixel editor: draw the body, faces, and accessories; build frame-by-frame animations; author custom reaction actions with triggers (tap, reveal, save, level-up, app open, time of day, idle, and more); then share or import designs as text

### Profile & Stats
- Display name + curiosity tagline
- **50-level quest system** — XP ranks with titles, quest chains, and progress tracking
- Streak, Saved, and Lanes stats
- Your most-explored categories at a glance

### Settings & Customization
- **Appearance** — Curio, AMOLED, or Material You theme; Light/Dark/System; pastel color mode
- **Notifications** — daily shuffle reminders, reminder time, explore controls
- **Recording** — voice quality, dictation settings
- **Experiments** — toggle 30+ optional UI tweaks (deck styling, layouts, smart density, voice-to-text)
- Manage Categories — show, hide, reorder
- Topic History — revisit everything explored
- **Backup & Restore** — export and import full backups; legacy FieldMind archive import
- Replay onboarding anytime

### Reliability & Support
- Onboarding flow (replayable)
- Crash reporter with saved history
- In-app bug report screen
- Daily reminders and explore sessions rebuild after reboot
- CI-validated release builds

### Design & Privacy
- Material-inspired custom design language — Material 3 foundation extended with a web-drawn editorial aesthetic
- Torn-paper heroes, watermark backdrops, custom typography
- Custom Material Symbols and motion
- **100% local data** — Room database, offline-first
- No accounts, no analytics, no tracking, no ads
- Open source on GitHub

---

## ⚙️ Configuration & Customization

### Themes
- **Curio** — the warm-cream material-inspired look (default)
- **AMOLED** — pure black for dark screens
- **Material You** — your device's Material palette
- **Light / Dark / System** mode selection
- **Pastel color mode** for softer aesthetics

### Settings
- **Notifications** — daily shuffle reminders, reminder time, explore controls
- **Recording** — voice quality, dictation (voice-to-text)
- **Manage Categories** — show, hide, or reorder lanes
- **Backup & Restore** — export and import full backups; legacy FieldMind archive import
- **Experiments** — 30+ optional UI tweaks (deck styling, smart density, etc.)

See Settings in-app for the full list.

---

## 🤝 Contributing

We'd love contributions! Here's how:

### Report Bugs
1. Check [existing issues](https://github.com/firefly-sylestia/Curio/issues) first
2. Use the **Bug Report** template with steps, screenshots, and version info
3. Or use the in-app bug report (Profile → Support & diagnostics → **Report a bug**), which opens a pre-filled GitHub issue

### Request Features or Topics
- Open an issue with the **Feature Request** template
- For new topics, name the category and the specific topic you'd like to see
- Topic requests help grow the catalog over time

### Code Contributions
- Fork the repo and create a feature branch
- Follow the existing code style (Kotlin + Jetpack Compose conventions)
- Submit a pull request with a clear description
- Link to any related issues

### Topic Contributions
The 16,000+ curated topics are hand-picked. If you have topics to suggest for any lane (Artists, Albums, Directors, Films, Authors, Books, Painters, Artworks, Scientists, Discoveries), open a **Feature Request** with specifics.

---

## 📜 License

Curio is **free and open source** under the [MIT License](LICENSE). See LICENSE for details.

---

## 🤔 Known Limitations & Roadmap

### Not in v1.0
- **iOS** — Android only for now
- **Cloud sync** — all data is local. Use Backup & Restore (Settings) to export and keep your library safe
- **Localization** — English only in v1.0
- **Google Play** — distributed via GitHub Releases; Play Store support coming next
- **Content depth grows** — every lane ships 500+ topics today, and the catalog keeps growing
- **Images need network** — topic and entry images load from the web (your data is always offline)

### Planned
- **More topics and categories** — every lane already has 500+; the catalog keeps growing and suggestions are welcome
- **Google Play listing** — once polished
- **Accessibility & localization** — after launch
- **iOS version** — long-term goal

---

## 📬 Support & Feedback

Found a bug? Have an idea? Want to request a topic?

- **GitHub Issues** — [Report bugs or request features](https://github.com/firefly-sylestia/Curio/issues)
- **In-app** — Profile → Support & diagnostics → **Report a bug** (opens a pre-filled GitHub issue)
- **Discussions** — Coming soon for general questions and ideas

---

## 👋 Credits

**Made by Firefly** — design, code, and curated topic catalog.

> **Note:** Approximately 70% of this project's codebase was written and iterated on with AI assistance (Codebuff). Human direction, design decisions, product judgment, and curation drove every feature — AI handled the heavy lifting of implementation, debugging, and polish across thousands of commits.

Special thanks to the early testers and people who shared topics and feedback.

### Open source

- **[vFlow](https://github.com/ChaoMixian/vFlow)** (GPL-2.0-or-later) — the liquid-glass tab bar (draggable active blob, backdrop refraction) is adapted from vFlow's `LiquidGlassBottomBar`. Thanks to [@ChaoMixian](https://github.com/ChaoMixian) for the original implementation.

---

*Explore something. Notice more. Keep the discovery.*
