# CURIO DATA PLAN

**Version:** v1
**Date:** Current Curio data contract
**Purpose:** Define Curio's content layer — the categories, the topics, the per-topic "what to do" prompt, the schema, and the rollout cadence. This document is the source of truth for Curio topic data and authoring; the active Android code consumes the contract defined here.

**How this doc relates to the app:**
- The active Curio source owns the shipped UI, category presentation, and capture behavior.
- This document owns the **topic data schema**, the **authoring pipeline**, and the **per-category rollout cadence**.
- When new category topics are authored and shipped, this document remains the source of truth for their data contract.

---

## §1. Category Roadmap

### Currently shipped categories

| ID | Category          | Accent (CURIO_SPEC §0.2) | Glyph (CURIO_SPEC §0.6)       | §8 capture format | Status |
|----|-------------------|---------------------------|-------------------------------|-------------------|--------|
| MUSIC      | Music / Artists       | Electric Blue #3D8CFF | `album`       | Sound Bite (voice note)   | shipped (placeholder UI) |
| MOVIES     | Movies / Directors    | Cobalt        #5B5FEF | `movie`       | Reel Notes (review + collage) | shipped (placeholder UI) |
| BOOKS      | Books / Authors       | Mint          #16B89A | `menu_book`   | Marginalia (journal + quotes)  | shipped (placeholder UI) |
| VISUAL_ART | Visual Art / Painters | Orange        #E6652F | `palette`     | Gallery Wall (moodboard)  | shipped (placeholder UI) |
| SCIENCE    | Science & Nature      | Cyan          #079DB8 | `science`     | Field Notes (3-section report) | shipped (placeholder UI) |
| WILDCARD   | Wildcard              | Rainbow gradient   | `casino`      | Open Notebook (pick format) | shipped (placeholder UI) |

### Planned v1 additions (rollout in §5 below)

| ID                | Category                  | Accent (candidate) | Glyph (candidate)              | §8.x capture format (proposed) |
|-------------------|---------------------------|---------------------|--------------------------------|----------------------------------|
| PHILOSOPHY        | Philosophy & Ideas        | Amber       #FFB74D | `psychology`        | Mental Model Map — definition + 2 real-world applications |
| HISTORY           | History & Mythology       | Crimson     #E57373 | `history_edu`       | Timeline & Takeaway — 5 events + the running thread |
| ARCHITECTURE      | Architecture & Design     | Slate Blue  #7986CB | `architecture`      | Structure Breakdown — form vs function observations |
| FOOD              | Food & Culture            | Saffron     #F6C153 | `restaurant`        | Taste Deconstruction — 3 sensory notes |

**Why these four first (and not, say, Tech or Sports or Health):** they collectively cover the dominant "discussion-starter" categories a curious omnivore will hit (`Book club picks philosophy`, `Foodie explores cumin`, `Architect admires Gothic arches`, etc.). The remaining "softer" categories — Tech, Sports, Health, Math, Mythology-deep-dives — get queued for v2 once the system is proven.

**Wildcard refactor:** instead of being a category in the data layer, the Wildcard becomes a **meta-spin** that lands on any one of the other 9 categories per spin. The dial visually stays the same (rainbow gradient) but lands on a hidden category. This keeps the wheel to 6 visual wedges forever (geometry constraint from §5); the "landed on Wildcard" event triggers a derived `categoryId` from a uniform random of the ready categories. When only Music is authored, Wildcard always lands on Music. When more arrive, Wildcard samples uniformly.

### Category export marker

Each authored category ships with:
- `isReady: Boolean` on the `CurioCategory` data class — `false` until 100+ topics are authored + reviewed.
- Categories with `isReady = false` are filtered out of the Home chip row + the Category Picker by default. Users get the "Coming soon" treatment in the existing empty-state slots (§13.7).

---

## §2. Topic Data Schema

### Why a new data layer

Phase 2 ships a placeholder app with the 6 starter categories hardcoded in `CurioCategories.all` (`app/data/Category.kt`). No topics exist yet — the spec describes WHAT the user sees (a topic image, a teaser, an action prompt) but never defines the underlying data structure. We need that definition before any topic can be authored.

### CurioTopic data class

```kotlin
data class CurioTopic(
    val id: String,                  // stable kebab-case string
                                   //   e.g., "music-bjork-vespertine"
                                   //   e.g., "history-mythology-gilgamesh"
                                   //   generated once, never recycled.
    val categoryId: CategoryId,
    val subtype: String,             // category-specific vocabulary
                                   //   Music / "Album" | "Track" | "Era"
                                   //   Movies / "Director" | "Film" | "Decade"
                                   //   Books / "Title" | "Author" | "Movement"
                                   //   Philosophy / "Concept" | "Thinker" | "Paradox"
    val name: String,                // display title — "Vespertine", "Parasite",
                                   // "Gödel, Escher, Bach", "Meditations"
    val teaser: String,              // 1–2 sentences, the "one quirky fact"
                                   //   referenced by §6 Topic Reveal.
                                   //   ≤ 450 chars, NOT a full bio.
    val imageUrl: String,            // remote URL — Wikimedia Commons preferred
                                   //   (see §4.2 image strategy)
    val actionPrompt: ExploreAction, // see §2.1 — the new "what to do" requirement
    val aliases: List<String> = emptyList(),
                                   //   search keywords ("Bowie" ↔ "David Bowie")
    val relatedTopicIds: List<String> = emptyList(),
                                   //   cross-category links for serendipity
                                   //   e.g. "frida-kahlo" ↔ "diego-rivera"
    val difficulty: Int = 1,         // 1 (immediate) — 3 (deep dive)
                                   //   tunes serendipity (random-draw rate)
    val weight: Int = 100,           // pool-relative weight (sum for weighted draw)
    val curatedBy: String = "human", // for trust label: "human", "ai", "ai+human"
    val curatedDate: String = ""     // ISO-8601; surfaced in Topic Reveal tooltip?
)
```

> **v29 (active schema drift):** the shipped `CurioTopic` in `app/data/CurioTopic.kt`
> has evolved past this spec — it now carries `tags: List<String>` (dynamic Spin
> filter chips), `tier: Int` (1 = marquee / human-curated, 2+ = long tail),
> `byline: String`, plus two **progress-metadata fields** added in v29:
>
> ```kotlin
> val pageCount: Int? = null     // BOOKS — total pages (e.g. 704); powers reading progress
> val episodeCount: Int? = null  // ANIME — total episodes (e.g. 26); powers watching progress
> ```
>
> These are optional and absent from legacy JSON (null = no progress tracking).
> Progress is stored per-topic in `TopicProgressStore` (SharedPreferences), shared
> by the reveal hero card, the Cabinet cards and the entry detail hero — a single
> floating accent button (slider editor) that reads/writes the same map.
> Anime films deliberately carry no `episodeCount` (a film has no episodes to track).

### §2.1 ExploreAction — the new "what to do" requirement

This is the user's "proper explanation of what to do" requirement. Every topic gets ONE structured `ExploreAction`. The Renderer (§2.2) turns it into a card UI; the writer fills the fields.

```kotlin
data class ExploreAction(
    val verb: String,                // "Listen" | "Watch" | "Read" | "Look at"
                                   //   | "Try" | "Visit" | "Make" | "Explore"
                                   //   verb drives icon glyph in §2.2
    val targetName: String,          // exact artifact to consume
                                   //   "Vespertine" (the specific album)
                                   //   "Parasite" (the specific film)
                                   //   "the first chapter of Gödel, Escher, Bach"
                                   //   "a high-res photo of Frida Kahlo's The Two Fridas"
    val durationMinutes: Int,        // time-boxed; UI shows "⏱ 45m"
    val instruction: String,        // the actual prompt — 1 to 2 sentences.
                                   //   ≤ 450 chars. Imperative, specific,
                                   //   curiously-framed. Examples below.
)
```

**Quality bar for `instruction` field** (this is what "proper explanation of what to do, accurately" means):
1. **Actionable** — user can act without further research. Names the specific artifact.
2. **Specific** — names the actual album / film / book / painting / paper. Not "explore music from 1995."
3. **Time-bounded** — ≤ 60 minutes unless the topic demands more (e.g. a book gets 45m "read chapter 1", not "read the whole thing").
4. **Curiously-framed** — invites the user to notice something they wouldn't notice casually. Not "listen to Vespertine" but "Notice how the beats hit your chest vs your head — that's intentional."

**Example `ExploreAction` instances per category** (illustrative; will be authored with 100+ real ones per category):

| Topic               | `verb`     | `targetName`                                       | `duration` | `instruction` |
|---------------------|------------|----------------------------------------------------|------------|----------------|
| Björk – Vespertine  | "Listen"   | "Vespertine (2001) end-to-end"                     | 45         | "Notice how the beats hit your chest vs your head — that's intentional. The album mixes orchestral and beat programming in a way most artists avoid." |
| Parasite (2019)     | "Watch"    | "Parasite (2019, Bong Joon-ho) on any reliable service" | 130   | "Pay attention to the rainstorm scene — every shot is doing double duty as social commentary. The vertical framing is doing work too." |
| Gödel, Escher, Bach | "Read"    | "the first chapter of Gödel, Escher, Bach (Hofstadter)" | 45 | "Stop after the chapter. Write down one thing you didn't understand AND one thing that surprised you. Future-you picks up the thread." |
| The Two Fridas      | "Look at"  | "a high-res photo of Frida Kahlo's The Two Fridas (1939)" | 5 | "Trace the visual structure — what sits at center? Notice how the broken artery physically connects the two selves. That's autobiographical; she painted this after divorcing Diego." |
| Wired for Sound    | "Watch"    | "the Wired for Sound concert film"                  | 6          | "Watch the audience, not the band. Notice how 'I Want You' rewires them in real time." |

**Per-topic vs per-pool:** every topic owns its OWN `ExploreAction` (no shared templates). The verb + duration cap pattern establishes a uniform feel without requiring identical text.

---

## §3. JSON-on-disk representation

Topics ship inside the APK as JSON files under `assets/topics/{category}.json` (one file per ready category). On first launch, a Room DB seeds from these JSON files (Phase 4 work). The JSON is the canonical authoring format; the Room schema is the runtime-queryable form.

### `app/src/main/assets/topics/music.json` shape

```json
{
  "categoryId": "MUSIC",
  "version": 1,
  "curatedDate": "2026-01-15",
  "topics": [
    {
      "id": "music-bjork-vespertine",
      "subtype": "Album",
      "name": "Vespertine",
      "teaser": "Björk's 2001 chamber-electronic album recorded mostly in her Icelandic home. The beats feel closer than they should.",
      "imageUrl": "https://upload.wikimedia.org/wikipedia/commons/thumb/.../File:Bjork-vespertine.jpg/512px-Bjork-vespertine.jpg",
      "actionPrompt": {
        "verb": "Listen",
        "targetName": "Vespertine (2001) end-to-end",
        "durationMinutes": 45,
        "instruction": "Notice how the beats hit your chest vs your head — that's intentional. The album mixes orchestral and beat programming in a way most artists avoid."
      },
      "aliases": ["Björk Vespertine", "Vespertine Bjork"],
      "relatedTopicIds": ["philosophy-jungian-shadow", "music-bjork-biophilia"],
      "difficulty": 2,
      "weight": 100,
      "curatedBy": "human",
      "curatedDate": "2026-01-15"
    }
  ]
}
```

**Image-URL policy:** all `imageUrl` values point to public-domain or freely-licensed sources — primarily Wikimedia Commons. Backlinks to the original file page are maintained in the topic author's notes (not in the JSON, but in the GitHub PR comment thread).

---

## §4. Storage + Image Strategy

### §4.1 Hybrid local-seed + Room DB

**Why not pure-JSON assets:** even though the JSON ships in the APK, querying 100+ topics per spin (random draw + anti-repeat + alias match) needs indexed SQL. Room with a `topics` table + indices on `categoryId`, `id`, `weight`, and a `last_shown_at` row is the right runtime form.

**Seed-on-first-launch pattern:**
1. APK ships with `assets/topics/music.json` etc.
2. First app launch: detect `topics_meta` row count = 0 → copy JSON files into Room tables. Future launches: skip.
3. Runtime: queries go against Room. JSON is the canonical authoring artifact; Room is the queryable form.

**Why not server-fetched:** offline is a feature. The user said "we ill go each category one by one" — we own each release's content. A 12-week-old user getting a stale topic list because the server is unavailable is worse than a 12-week-old topic pool that's still on-device.

### §4.2 Image strategy — on-demand fetch with Coil

**Don't bundle images into the APK.** 100+ topics × 50–300 KB each = 5–30 MB APK bloat.

**Use a URL+Coil flow:**
- `imageUrl` field stores a remote URL (Wikimedia Commons primary; photographer attribution linked from the file page; we never host the bytes ourselves).
- At Topic Reveal time (§6), Coil downloads + caches to disk + renders.
- Disk cache survives app restarts.
- Image budget per topic reveal: ≤ 2 seconds target; fall back to the placeholder icon if slow/error.
- For users without network (rare but possible): the placeholder icon or the cached image is fine.

**Why Wikimedia Commons:**
- Wikipedia authors have already curated encyclopedia-quality images for ~all famous topics.
- Public-domain or CC-BY / CC-BY-SA — attribution-friendly.
- No hosting/CDN cost to us.
- Discovered via the topic's Wikipedia article (which the author reads while writing the prompt).

**Caveat we accept:** some topics genuinely lack an image (a recently released album, an obscure philosophy concept). For those, the JSON's `imageUrl` is empty, and the Topic Reveal §6 shows the §0.2 category accent + the topic name + the wildcard icon until Coil has nothing to load. UI treats "no image" gracefully.

### §4.3 Randomness / serendipity

**Anti-repeat default:**
1. Query Room: `WHERE last_shown_at IS NULL OR last_shown_at < now - 30 days` ORDER BY `weight DESC, RANDOM() LIMIT 1`.
2. If zero unpicked topics remain, reset `last_shown_at` to NULL across all rows (full reservoir refresh) OR seed an LRU eviction (whichever the data model supports).
3. Apply `weight DESC` to nudge toward higher-curation-confidence topics early on.

**Session spreading:** if the user has spun 3 times in a row for Music, the 4th spin shouldn't always be Music. A small "if last 3 spins are same category, draw 70/30 biased toward different" is a Phase 4 nicety; v1 ships uniform-with-anti-repeat.

**Surprise levels per topic:**
- `difficulty: 1` — immediate-entry topic (e.g. "listen to a 4-min track"). Surfaces more often.
- `difficulty: 3` — deep-dive topic ("read three chapters"). Surfaces less often.
- The Wildcard dial VISUALLY doesn't show these weights — the surface-level randomness is uniform enough for the brand experience.

---

## §5. Per-Category Rollout Cadence

### §5.1 Order

The user said "we ill go each category one by one." Concretely:

| Order | Category         | Why this slot |
|-------|------------------|---------------|
| 1     | **Music**        | Existing Curio team's native expertise (the legacy app was a music player). Lowest authoring risk; highest prompt-quality ceiling. |
| 2     | **Movies**       | Visual/film literacy transfers; rich Wikimedia Commons image pool; Reel Notes capture format is well-suited to "watch this film." |
| 3     | **Books**        | Books are the spine of "curiosity" — and the Marginalia capture format has the highest long-term re-read value. |
| 4     | **Visual Art**   | Gallery Wall moodboard capture format + strong Wikimedia coverage. |
| 5     | **Science**      | Field Notes' structured 3-section report is the most-clearly-defined capture format. |
| 6     | **Wildcard**     | Becomes the meta-spin then. Hand-curated "anything-goes" set if we still want it post-rollout (or fully derived from the other 5). |
| 7–10  | **Philosophy, History, Architecture, Food** | v1.1 — the 4 new additions, each with the schema in §2 + the capture format in §1's table. |

### §5.2 Authoring pipeline per category

Per category ship, the pipeline is:

1. **Seed (LLM-assisted, heavy human review)**
   - Use an LLM (Claude / sonnet-class) with a strict authoring prompt to draft 150–250 `CurioTopic` candidates per category.
   - The authoring prompt mandates: real not made-up names; real not made-up URLs (Wikimedia only); `instruction` field that passes the §2.1 quality bar.
   - Output: a draft `assets/topics/{category}.json` and a `WIKIMEDIA_SOURCES.md` document listing the Wikipedia article + Commons file pages used for each topic's source material.

2. **Human review pass**
   - A human reviewer goes through the draft JSON.
   - Reject made-up / out-of-place topics. Verify `imageUrl` is not 404. Verify `instruction` is time-bounded + specific.
   - Output: a JSON with a `reviewedBy: "human"` field added per topic + a final commit message that cites the LLM prompt + reviewer name.

3. **Lightweight smoke test**
   - The CI runs a `validatetopics` Gradle task (Phase 4 work — not yet built) that parses each JSON file and asserts:
     - All fields present, all IDs unique within file, all `actionPrompt` instructions ≤ 450 chars.
     - Every `imageUrl` HEADs with 200 (sanity check; not blocking).
     - `categoryId` matches the filename.
     - All `relatedTopicIds` resolve to actual `id`s (or the ID exists in another category file).
   - Smoke test runs in CI; doesn't block the merge but flags for review.

4. **Ship via PR**
   - One PR per category (not per topic). PR title: `[data] Add {N} {Category} topics`.
   - Reviewer on the PR = the human reviewer from step 2.
   - Manifest version bump + JSON asset commit + a small DoX note in `app/AGENTS.md` (one-liner pointing at the new JSON file).

5. **Toggle `isReady = true`**
   - In a tiny follow-up PR: bump `CurioCategories.all[MUSIC] = CurioCategory(... isReady = true)`.
   - The UI unblocks Music from the chip row + Picker.

### §5.3 v1 launch target counts

| Phase   | Categories ready | Topics per category | Total topics | APK ~delta |
|---------|------------------|---------------------|--------------|------------|
| v1.0    | Music            | 150                 | 150          | ~75 KB JSON |
| v1.1    | + Movies         | 150 each            | 300          | + ~75 KB    |
| v1.2    | + Books          | 150 each            | 450          | + ~75 KB    |
| v1.3    | + Visual Art     | 150 each            | 600          | + ~75 KB    |
| v1.4    | + Science        | 150 each            | 750          | + ~75 KB    |
| v2.0    | + the 4 new      | 100–150 each        | 1100–1350    | + ~400 KB   |
| v3.0    | + the remaining soft categories | 100 each | ~1700 | + ~500 KB |

The placeholder UI ship remains stable throughout; each new category is a discrete on/off via `isReady`.

---

## §6. Authoring Prompt Template (for the LLM)

A reusable prompt template the author (or the LLM the author delegates to) fills with the target category. Used by both the seeded 150/category draft AND future manual additions.

```
You are authoring {N} topics for the "{CATEGORY_NAME}" category of Curio, a
discovery app. Each topic becomes a roulette-dial landing that a curious user
sees in their pocket for 10 seconds before deciding whether to explore it.

For each topic, output a single JSON object matching the CurioTopic schema
in app/CURIO_DATA_PLAN.md §2. Then in batch, output the JSON array.

Hard requirements:
1. ONLY real, encyclopedically verifiable topics. NO made-up artists,
   albums, books, concepts. Verify against Wikipedia first.
2. `name` + `subtype` + `categoryId` + `teaser` + `actionPrompt.instruction`
   must each be a unique string per category — no near-duplicates across
   the {N} batch.
3. `teaser` field is the §6 "One quirky fact" — make it punchy, ≤ 450 chars.
4. `actionPrompt.instruction` must follow the §2.1 quality bar:
   ACTIONABLE, SPECIFIC, TIME-BOUNDED, CURIOUSLY FRAMED. Reference the
   actual artifact (album / film / book / painting / paper) by name.
   Avoid "explore the music of the 90s" — instead "Listen to Vespertine
   end-to-end; notice how the beats hit your chest vs your head."
5. `imageUrl` is a real Wikimedia Commons file URL, /thumb/-shaped to
   512px or 1024px wide. If no good image exists, leave `imageUrl: ""`.
6. `durationMinutes` reflects realistic human time-to-engage. "5 min"
   for looking at a painting; "45 min" for reading a chapter; never
   "120 min" unless the artifact genuinely demands it.
7. Spread topics across the category's SUBTYPES so we don't ship 150
   music-albums and zero music-decades. {SUBTYPE_DISTRIBUTION_TEMPLATE}
8. Spread difficulty across 1/2/3 evenly. Don't make every topic a 2.
9. Provide a "WIKIMEDIA_SOURCES" reference document at the end listing
   the {N} source URLs grouped by topic id.

Output ONLY valid JSON + the sources doc — no commentary.
```

This template is invoked from the Curio repo's `scripts/` directory (Phase 4 work). Each invocation targets one category with the appropriate `SUBTYPE_DISTRIBUTION_TEMPLATE` filled in.

---

## §7. Open Decisions

| # | Decision | Status |
|---|----------|--------|
| 14.7 | **Expand taxonomy from 6 to 10 categories.** Add Philosophy & Ideas, History & Mythology, Architecture & Design, Food & Culture over v1.1–v1.4 in addition to the 6 original spec categories. Wildcard becomes a meta-spin. ✅ Locked here. |
| 14.8 | **Schema decisions** — see §2. `CurioTopic` (10 fields) + `ExploreAction` (4 fields). JSON canonical, Room derived. Wikimedia Commons image source. Anti-repeat spinner. Music first. ✅ Locked here. |
| 14.9 | **Authoring pipeline** — LLM-assisted draft + human review + JSON smoke-test + per-category ship PR. ✅ Locked here. |
| 14.10 | **Image strategy** — URL + Coil, on-demand fetch + disk cache. No image bundling. Empty-string URL = fallback icon. ✅ Locked here. |
| 14.11 | **Per-topic `ExploreAction`** — one structured (verb, targetName, durationMinutes, instruction) per topic. Quality bar in §2.1. ✅ Locked here. |
| 14.12 | **Wildcard refactor** — meta-spin that lands on any ready category; not its own pool. ✅ Locked here. |

When each batch of new-category topics is ready to ship, update the category roadmap and rollout notes above.

---

## §8. End of Data Plan

The data plan lives until the schema changes; per-category rollout docs live in the PR threads (and may be summarized in `app/AGENTS.md` once the data layer is in active use).

### Next concrete deliverable

The first PR after this data plan lands: `feat(data): seed Music v1 (150 topics)`. That PR:
- Adds `app/src/main/assets/topics/music.json` (150 entries per §6 template).
- Adds `app/src/main/assets/topics/SOURCES_MUSIC.md` (the source URLs, grouped by topic id).
- Bumps Music's `CurioCategory` in `app/data/Category.kt` to `isReady = true`.
- Adds the `validatetopics` Gradle task (or its first-version equivalent).
- Updates `app/AGENTS.md` with one line: "Music topic data: `assets/topics/music.json` (150 topics, v1)".

When that PR lands, Curio is data-live for Music. Movies, Books, Art, Science + the 4 new additions follow one PR at a time per §5.1.
