# Curio Topic Schema — Quick Reference

This file is the in-folder quick reference for the canonical topic data format
shipped under `app/src/main/assets/topics/{categoryId}.json`. The schema is
enforced by `TopicJsonLoader` (`app/src/main/java/com/curio/app/data/TopicJsonLoader.kt`)
and the `CurioTopic` data class (`CurioTopic.kt`).

---

## Files in this directory

| File | CategoryId | Subtype | Verb |
|---|---|---|---|
| `artists.json` | `ARTISTS` | Artist | Listen |
| `albums.json` | `ALBUMS` | Album / Track / EP | Listen |
| `directors.json` | `DIRECTORS` | Director | Watch |
| `films.json` | `FILMS` | Film | Watch |
| `authors.json` | `AUTHORS` | Author | Read |
| `books.json` | `BOOKS` | Book | Read |
| `painters.json` | `PAINTERS` | Painter / Sculptor / Photographer | Look at |
| `artworks.json` | `ARTWORKS` | Painting / Sculpture / Photograph / Installation | Look at |
| `scientists.json` | `SCIENTISTS` | Scientist / Mathematician / Inventor / Philosopher | Read |
| `discoveries.json` | `DISCOVERIES` | Discovery / Theory / Invention / Phenomenon | Explore |
| `songs.json` | `SONGS` | Song | Listen |
| `series.json` | `SERIES` | Series | Watch |
| `anime.json` | `ANIME` | Anime | Watch |
| `manga.json` | `MANGA` | Manga | Read |
| `manhwa.json` | `MANHWA` | Manhwa | Read |
| `games.json` | `GAMES` | Game | Watch |
| `mythology.json` | `MYTHOLOGY` | Myth / Legend / Tale / Tall Tale | Read |
| `sports.json` | `SPORTS` | Sport / Legend | Watch |
| `food.json` | `FOOD` | Dish / Recipe | Read |
| `internet.json` | `INTERNET` | Meme / Viral / Slang / Creepypasta | Watch |
| `wildcard.json` | `WILDCARD` | Curiosity / Mystery / Phenomenon / Ritual / etc. | varies |

The filename (minus `.json`) MUST equal `CategoryId.routeSlug` from
`Category.kt` so `TopicJsonLoader` can find the file. 21 files total —
one per `CategoryId` enum value.

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
| `id` | string | ✅ | Unique **across all 11 files**. Kebab-case. Convention: `{subtype-prefix}-{slug}` (`album-bjork-vespertine`, `film-godfather-1972`, `discovery-penicillin-1928`). Never recycle — Room will FK on this. |
| `categoryId` | string | ✅ | Must be one of the 21 `CategoryId` enum values: `ARTISTS`, `ALBUMS`, `SONGS`, `DIRECTORS`, `FILMS`, `SERIES`, `AUTHORS`, `BOOKS`, `PAINTERS`, `ARTWORKS`, `SCIENTISTS`, `DISCOVERIES`, `ANIME`, `MANGA`, `MANHWA`, `GAMES`, `MYTHOLOGY`, `SPORTS`, `FOOD`, `INTERNET`, `WILDCARD`. Must match the filename's category. |
| `subtype` | string | ✅ | Category-specific vocabulary. Music: `Album` \| `Track` \| `EP` \| `Artist`. Films: `Film` \| `Documentary` \| `Short`. Books: `Book` \| `Collection` \| `Essay`. Painters: `Painter` \| `Sculptor` \| `Photographer`. See table above for defaults. |
| `name` | string | ✅ | Display title. ≤ 80 chars. For works, format as `Title (Year) — Author` or `Title (Year)` — whichever reads best. |
| `teaser` | string | ✅ | 1–2 sentences, ≤ 450 chars. The "one quirky fact" surfaced on Topic Reveal (CURIO_SPEC §6). NOT a Wikipedia bio — find a surprising angle. |
| `imageUrl` | string | ✅ | Empty string `""` for now (image strategy deferred to a later phase). |
| `byline` | string | ❌ | Creator tag shown as a pill on the Topic Reveal hero card (`Artist · The Beatles`, `Author · George Orwell`, `Discovered by · Alexander Fleming`). Albums → artist, Books → author, Films → director, Artworks → painter, Discoveries → discoverer. Optional, default `""`. Populated by `scripts/enrich_topics.py` + `scripts/enrich_discoveries_bylines.py`. |
| `exploreAction.verb` | string | ✅ | Exploration verbs only — `Listen` \| `Watch` \| `Read` \| `Look at` \| `Explore` \| `Visit` \| `Learn` \| `Discover`. **Never a making/doing verb** (`Cook`, `Play`, `Make`, `Try`, `Build`, `Write`, `Fold`) — every instruction must be about exploring (reading/watching/learning), not doing. Drives the icon glyph on the action card. See table above for per-category defaults. |
| `exploreAction.targetName` | string | ✅ | The exact artifact to consume. `Vespertine (2001) end-to-end`, not `an album by Björk`. |
| `exploreAction.durationMinutes` | int | ✅ | Realistic human time-to-engage. ≤ 60 unless the artifact genuinely demands more. |
| `exploreAction.instruction` | string | ✅ | ≤ 600 chars (matches the `validateTopics` Gradle task cap). Must pass the **quality bar** below. |
| `tags` | string[] | ❌ | Free-form tags for the Spin screen's dynamic filter chip row. Default `[]`. Tags are category-specific: Artists might use `["Rock", "1970s"]`, Films might use `["Drama", "1990s"]`, Painters might use `["Impressionism", "Oil"]`. Films + Directors use the industry-region tags `Hollywood` (US studio system, replaces the plain `American` origin tag on those two categories) and `Bollywood` (Hindi cinema) — `SpinScreen` buckets both into the filter sheet's Origin group. Franchise tags (`MCU`, `Star Wars`, `DC`, `Harry Potter`, `Lord of the Rings`, `Pixar`, `Studio Ghibli`, `Disney`) are bucketed into their own **Franchise** filter row — see `FranchiseTags` in `SpinScreen.kt` (`scripts/add_franchise_tags.py` maintains them on `films.json`/`anime.json`). |
| `tier` | int 1–3 | ❌ | Quality tier. 1 = human-curated marquee (highest quality, surfaces most often). 2 = AI-curated long-tail (still good). 3 = draft / placeholder. Default 1 if omitted. |

---

## The `instruction` quality bar

Every `instruction` field must pass all four checks:

1. **Actionable** — the user can act without further research. Names the specific artifact.
2. **Specific** — names the actual album / film / book / painting / paper. Not "explore music from 1995."
3. **Time-bounded** — ≤ 60 minutes unless the topic genuinely demands more. `"Read chapter 1"`, not `"read the whole thing."`
4. **Curiously-framed** — invites the user to notice something they wouldn't notice casually. Not "listen to Vespertine" but "Notice how the beats hit your chest vs your head — that's intentional."

---

## Validation

The `CurioTopic` constructor (`CurioTopic.kt:init`) validates every loaded topic at runtime:

- `id` not blank
- `name` not blank
- `teaser` not blank
- `tier` in 1..3

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
6. **For LLM-drafted topics**, set `tier: 2`. For human-curated marquee content, set `tier: 1`.

For the full LLM authoring prompt template, see `master.md`'s Phase 4 plan.

---

## End of SCHEMA.md
