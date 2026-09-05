# Curio Topic Schema — Quick Reference

This file is the in-folder quick reference for the canonical topic data format
shipped under `app/src/main/assets/topics/{categoryId}.json`. The schema is
enforced by `TopicJsonLoader` (`TopicJsonLoader.kt`)
and the `CurioTopic` data class (`CurioTopic.kt`).

---

## Files in this directory

| File | CategoryId | Subtypes | Default Verb |
|---|---|---|---|
| `artists.json` | `ARTISTS` | Artist | Listen |
| `albums.json` | `ALBUMS` | Album / EP | Listen |
| `songs.json` | `SONGS` | Song | Listen |
| `directors.json` | `DIRECTORS` | Director | Watch |
| `films.json` | `FILMS` | Film / Documentary | Watch |
| `animated-movies.json` | `ANIMATED_MOVIES` | Animated Movie | Watch |
| `series.json` | `SERIES` | Series | Watch |
| `authors.json` | `AUTHORS` | Author | Read |
| `books.json` | `BOOKS` | Book | Read |
| `manga.json` | `MANGA` | Manga | Read |
| `manhwa.json` | `MANHWA` | Manhwa | Read |
| `painters.json` | `PAINTERS` | Painter / Sculptor / Photographer | Look at |
| `artworks.json` | `ARTWORKS` | Painting / Sculpture / Photograph / Installation | Look at |
| `scientists.json` | `SCIENTISTS` | Scientist / Mathematician / Inventor | Read |
| `discoveries.json` | `DISCOVERIES` | Discovery / Theory / Invention / Phenomenon | Explore |
| `anime.json` | `ANIME` | Anime | Watch |
| `games.json` | `GAMES` | Game | Watch |
| `mythology.json` | `MYTHOLOGY` | Myth / Legend / Tale / Tall Tale | Read |
| `sports.json` | `SPORTS` | Sport / Legend | Watch |
| `food.json` | `FOOD` | Dish / Recipe | Read |
| `wildcard.json` | `WILDCARD` | Curiosity / Mystery / Phenomenon / Ritual / etc. | varies |
| `oceans.json` | `OCEANS` | Creature / Ecosystem / Expedition / Feature / Ocean / Phenomenon | Explore |
| `medicine.json` | `MEDICINE` | Anatomy / Concept / Condition / Discovery / History / Pioneer / System / Treatment | Explore |
| `psychology.json` | `PSYCHOLOGY` | Bias / Concept / Effect / Phenomenon / Theory / Condition | Explore |
| `mathematics.json` | `MATHEMATICS` | Branch / Concept / Number / Problem / Theorem | Explore |
| `biology.json` | `BIOLOGY` | varies | Explore |
| `chemistry.json` | `CHEMISTRY` | varies | Explore |
| `astronomy.json` | `ASTRONOMY` | varies | Explore |
| `geology.json` | `GEOLOGY` | varies | Explore |
| `history.json` | `HISTORY` | varies | Read |
| `plants.json` | `PLANTS` | varies | Explore |
| `animals.json` | `ANIMALS` | varies | Explore |
| `quotes.json` | `QUOTES` | varies | Explore |
| `technologies.json` | `TECHNOLOGIES` | varies | Explore |
| `language.json` | `LANGUAGE` | Concept / Family / Linguist / Phenomenon / Script / Word | Explore |
| `engineering.json` | `ENGINEERING` | Concept / Discipline / History / Inventor / Material / Structure / System | Explore |
| `economics.json` | `ECONOMICS` | Concept / Economist / History / Institution / Market / Policy / Theory | Explore |
| `internet.json` | `INTERNET` | Meme / Viral / Slang / Creepypasta | Watch |

**38 files total** — one per `CategoryId` enum value.  
The filename (minus `.json`) MUST equal `CategoryId.routeSlug` from
`Category.kt` so `TopicJsonLoader` can find the file.

There is **no root wrapper object**. The file is a bare JSON array. The
`categoryId` on every topic must match the filename's enum value.

---

## JSON shape (root = bare array)

```json
[
  {
    "id": "album-bjork-vespertine",
    "categoryId": "ALBUMS",
    "subtype": "Album",
    "name": "Vespertine",
    "teaser": "Björk's 2001 chamber-electronic album, mostly recorded alone in her Reykjavík home. The beats sit closer than they should.",
    "imageUrl": "",
    "byline": "Björk",
    "exploreAction": {
      "verb": "Listen",
      "targetName": "Vespertine (2001) end-to-end",
      "durationMinutes": 55,
      "instruction": "Notice how the beats hit your chest vs your head — that's intentional."
    },
    "tags": ["Electronic", "Art Pop", "2000s"],
    "tier": 1
  }
]
```

---

## Per-topic fields

| Field | Type | Required | Notes |
|---|---|:---:|---|
| `id` | string | ✅ | Unique **across all files**. Kebab-case. Convention: `{subtype-prefix}-{slug}` (`album-bjork-vespertine`, `film-godfather-1972`, `discovery-penicillin-1928`). Never recycle — Room will FK on this. |
| `categoryId` | string | ✅ | Must be one of the 38 `CategoryId` enum values (see table above). Must match the filename's category. |
| `subtype` | string | ✅ | Category-specific vocabulary. See per-file table above for defaults. |
| `name` | string | ✅ | Display title. ≤ 80 chars. For works, format as `Title (Year) — Author` or `Title (Year)` — whichever reads best. |
| `teaser` | string | ✅ | 1–2 sentences, **≤ 450 chars**. The "one quirky fact" surfaced on Topic Reveal (CURIO_SPEC §6). NOT a Wikipedia bio — find a surprising angle. |
| `imageUrl` | string | ✅ | Empty string `""` for now (image strategy deferred to a later phase). |
| `byline` | string | ❌ | Creator tag shown as a pill on the Topic Reveal hero card (`Artist · The Beatles`, `Author · George Orwell`, `Discovered by · Alexander Fleming`). Albums → artist, Books → author, Films → director, Artworks → painter, Discoveries → discoverer. Optional, default `""`. Populated by `scripts/enrich_topics.py` + `scripts/enrich_discoveries_bylines.py`. |
| `exploreAction.verb` | string | ✅ | Exploration verbs only — `Listen` \| `Watch` \| `Read` \| `Look at` \| `Explore` \| `Visit` \| `Learn` \| `Discover`. **Never a making/doing verb** (`Cook`, `Play`, `Make`, `Try`, `Build`, `Write`, `Fold`) — every instruction must be about exploring (reading/watching/learning), not doing. Drives the icon glyph on the action card. See table above for per-category defaults. |
| `exploreAction.targetName` | string | ✅ | The exact artifact to consume. `Vespertine (2001) end-to-end`, not `an album by Björk`. |
| `exploreAction.durationMinutes` | int | ✅ | Realistic human time-to-engage. ≤ 60 unless the artifact genuinely demands more. |
| `exploreAction.instruction` | string | ✅ | **≤ 600 chars** (matches the `validateTopics` Gradle task cap). Must pass the **quality bar** below. |
| `tags` | string[] | ❌ | Free-form tags for the Spin screen's dynamic filter chip row. Default `[]`. **Science/education categories use lowercase tags** (e.g. `["oceans", "geology", "coral"]`). **Media/entertainment categories use Title-case tags** (e.g. `["Comedy", "Sitcom", "American"]`). Films + Directors use the industry-region tags `Hollywood` (US studio system, replaces the plain `American` origin tag on those two categories) and `Bollywood` (Hindi cinema) — `SpinScreen` buckets both into the filter sheet's Origin group. Franchise tags (`MCU`, `Star Wars`, `DC`, `Harry Potter`, `Lord of the Rings`, `Pixar`, `Studio Ghibli`, `Disney`) are bucketed into their own **Franchise** filter row — see `FranchiseTags` in `SpinScreen.kt`. |
| `tier` | int 1 | ❌ | Quality tier. 1 = human-curated marquee (highest quality, surfaces most often). 2 = AI-curated long-tail (still good). 3 = draft / placeholder. Default 1 omitted. |
| `altPageCount` | int | ❌ | **Books only** (v126). A second common edition's page count when editions differ hugely (translations / annotated editions / print size). Powers the alternate-edition pill beside the progress pill — tapping it pre-fills the editor with that count. Default omitted. |
| `altPageLabel` | string | ❌ | **Books only** (v126). Short label for `altPageCount` — the edition name (`"Lombardo"`, `"Wordsworth"`, `"Modern Library"`, `"Penguin Classics"`, `"Corrected text"`). Default `""`. |
| `episodeCount` | int | ❌ | **Anime only** (v29). Total episodes for watching progress tracking. Anime films deliberately carry no `episodeCount`. Default omitted. |
| `geniusUrl` | string | ❌ | **Albums only** (v333). The album's Genius page (`https://genius.com/albums/<artist>/<album>`), authored by `tools/enrich_albums_art_genius.py` (slug-constructed; the official Genius API, when a `GENIUS_API_TOKEN` is supplied, validates each URL). Surfaced as a "GENIUS" pill in the track-list sheet header. Default omitted. |
| `synopsis` | string | ❌ | Narrative description. **Books** (v11): a detailed synopsis powering the Synopsis overlay + chapter reader on Topic Reveal. **Albums** (v334): a description-style synopsis (context, how it was made, sound, legacy) authored in batches of ~35 by the data authoring flow. Written in human prose, no em dashes. Default omitted. |
| `tracks` | array | ❌ | **Albums only** (v332). The album's track list — `[{ "number": 1, "title": "…", "duration": "3:28" }, …]` — powering the TRACKLIST card + track-list sheet on Topic Reveal. `duration` is an `m:ss` (or `h:mm:ss`) string, empty when the source omits per-track lengths. Default omitted. |

---

## The `instruction` quality bar

Every `instruction` field must pass all four checks:

1. **Actionable** — the user can act without further research. Names the specific artifact.
2. **Specific** — names the actual album / film / book / painting / paper. Not "explore music from 1995."
3. **Time-bounded** — ≤ 60 minutes unless the topic genuinely demands more. `"Read chapter 1"`, not `"read the whole thing."`
4. **Curiously-framed** — invites the user to notice something they wouldn't notice casually. Not "listen to Vespertine" but "Notice how the beats hit your chest vs your head — that's intentional."
5. **Try in small batches of 20 to 40**

---

## Validation

The `CurioTopic` constructor (`CurioTopic.kt:init`) validates every loaded topic at runtime:

- `id` not blank
- `name` not blank
- `teaser` not blank
- `tier` in 1

A failure throws `IllegalArgumentException` and aborts the parse, surfacing
as a `TopicLoadException` from `TopicJsonLoader`.

A separate `validateTopics` Gradle task (registered in `app/build.gradle.kts`)
runs the full schema check on every JSON file in this directory, including
cross-file ID uniqueness and the per-topic fields above. The task is wired
into `preBuild` automatically when JSON files exist.

---

## Authoring a new topic (quick recipe)

1. **Pick a real thing** — verify against the relevant Wikipedia article that the artifact exists, has the claimed author/date.
2. **Draft the `instruction` first.** If you can't write a 1–2 sentence prompt that passes the quality bar, the topic isn't ready — try a different one.
3. **Fill in the rest** (`teaser`, `exploreAction.verb`, `targetName`, `durationMinutes`, `tags`).
4. **Set `imageUrl` to `""`** (image strategy deferred).
5. **Pick an ID** using the `{subtype-prefix}-{slug}` convention. If the ID already exists in another category file, change the slug.
6.  For human-curated marquee content, set `tier: 1`.
7.  **Keep teasers ≤ 450 chars and instructions ≤ 600 chars** — the `validateTopics` task will catch violations.
8.  **Batch 20–40 topics at a time**, deduping against existing names and IDs in every file.

For the full LLM authoring prompt template, see `CURIO_DATA_PLAN.md` §6.

---

## Topic counts (as of this update)

| File | Count | File | Count |
|---|---|---|---|
| biology.json | 1202 | quotes.json | 1094 |
| plants.json | 1030 | artworks.json | 1023 |
| chemistry.json | 1010 | animals.json | 1006 |
| artists.json | 1004 | astronomy.json | 1002 |
| technologies.json | 1000 | songs.json | 1000 |
| history.json | 1000 | geology.json | 1000 |
| albums.json | 1000 | films.json | 948 |
| authors.json | 688 | animated-movies.json | 591 |
| directors.json | 508 | painters.json | 506 |
| discoveries.json | 506 | wildcard.json | 503 |
| scientists.json | 501 | books.json | 444 |
| economics.json | 95 | food.json | 77 |
| sports.json | 76 | manga.json | 75 |
| engineering.json | 75 | language.json | 71 |
| games.json | 65 | manhwa.json | 64 |
| mathematics.json | 90 | psychology.json | 89 |
| ocean.json | 83 | internet.json | 61 |
| anime.json | 61 | series.json | 60 |
| mythology.json | 60 | medicine.json | 92 |

---

## End of SCHEMA.md
