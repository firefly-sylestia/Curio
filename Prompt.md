# Prompt — Current Request

## Request
Topics expansion, Android app ONLY (`app/src/main/assets/topics/`). Target the thinnest pools, hand-written real quick facts, batches of 20–40 topics per pass (per SCHEMA.md quality bar #5), long-term goal **1000+ topics per category**. Also refresh `SCHEMA.md` (it documented 21 files but 38 exist).

## Status: COMPLETE (this batch — more batches to follow)

## Completed this session

### Batches written (125 new topics)
- **oceans.json**: 48 → **83** (+35) — creatures, ecosystems, phenomena, expeditions, features
- **medicine.json**: 58 → **92** (+34) — pioneers, discoveries, anatomy, conditions, treatments, systems
- **psychology.json**: 61 → **89** (+28) — biases, effects, concepts, phenomena, theories
- **mathematics.json**: 62 → **90** (+28) — theorems, concepts, branches, problems, numbers

### Schema doc refresh
- **SCHEMA.md** rewritten to document all 38 actual files (was 21), with a topic-count table

### Validation
- All 4 files parse cleanly, all IDs unique across all 38 files, all names unique within file, all teasers ≤ 450 chars, all instructions ≤ 600 chars, all verbs valid exploration-only

## Remaining work (future batches toward 1000+/category)

Thinnest pools still needing expansion (in order):

| Category | Current | Gap to 1000 | Batch priority |
|---|---|---|---|
| mythology | 60 | ~940 | NEXT — media format, byline+Title-case tags |
| series | 60 | ~940 | NEXT — media format |
| anime | 61 | ~939 | NEXT — media format + episodeCount |
| internet | 61 | ~939 | NEXT — media format |
| psychology | 89 | ~911 | Batch 2 of science family |
| mathematics | 90 | ~910 | Batch 2 of science family |
| medicine | 92 | ~908 | Batch 2 of science family |
| oceans | 83 | ~917 | Batch 2 of science family |
| manhwa | 64 | ~936 | media format |
| games | 65 | ~935 | media format |
| language | 71 | ~929 | science family |
| engineering | 75 | ~925 | science family |
| sports | 76 | ~924 | media format |
| food | 77 | ~923 | media format |
| economics | 95 | ~905 | science family |

At 30 topics per batch per category (~35 batches × 15 categories), reaching 1000+ each will require approximately **525 more batches**. Each session can realistically deliver 4–6 batches (~120–180 topics).

## Format families (for consistent future batches)
- **Science family** (oceans, medicine, psychology, mathematics, language, engineering, economics, biology, chemistry, astronomy, geology): verb `Explore`, lowercase tags, no byline, key order id/categoryId/subtype/name/teaser/imageUrl/tier/tags/exploreAction
- **Media family** (mythology, series, anime, internet, manhwa, games, food, sports): byline present, Title-case tags, verbs Read/Watch, names include "(Year)", tier last
