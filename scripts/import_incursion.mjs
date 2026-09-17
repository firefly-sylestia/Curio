// ── INCURSION — the dataset importer ─────────────────────────────────────────
//
// Curio's Incursion screen is powered by the viewing-order data of
// https://github.com/firefly-sylestia/mcu-viewing-order. That repo keeps its
// titles as plain ES modules under `src/data/`, one per studio; this script is
// the ONE transform between them and the app's assets, so the list can be
// re-imported whenever upstream moves instead of being hand-copied (a few
// hundred rows transcribed by eye is how a list ends up wrong).
//
//   node scripts/import_incursion.mjs
//
// It writes `app/src/main/assets/incursion/{marvel,sony,xmen}.json`. Nothing is
// invented: every field below is read straight out of the modules, and the few
// derived ones (a normalised group, a `label` for the era headings) are named
// as such. Trailers are deliberately not imported — Curio asked for viewing
// order, essentials and statuses, not promo material, so `trailerData.js` is
// never fetched.

import { readFileSync, mkdirSync, writeFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const HERE = dirname(fileURLToPath(import.meta.url))
const ROOT = join(HERE, '..')
const OUT = join(ROOT, 'app/src/main/assets/incursion')

const BASE = 'https://raw.githubusercontent.com/firefly-sylestia/mcu-viewing-order/main/src/data'

const SOURCES = {
  marvel: `${BASE}/mcuData.js`,
  sony: `${BASE}/sonyData.js`,
  xmen: `${BASE}/xmenData.js`,
}

/**
 * The modules are ordinary ES modules, so they are imported AS modules: only
 * the two constructor calls whose values the app will never need are
 * neutralised (`new Set(…)` is used upstream purely for membership, and
 * `Object.freeze` is a lookup map), leaving the file itself untouched and the
 * values read out by their own exported names rather than by parsing text —
 * which is what makes this a transform and not a transcription.
 */
async function load(url) {
  const res = await fetch(url)
  if (!res.ok) throw new Error(`${url} → HTTP ${res.status}`)
  const src = (await res.text())
    .replace(/new Set\(/g, '(')
    .replace(/Object\.freeze\(/g, '(')
  const href = 'data:text/javascript;base64,' + Buffer.from(src, 'utf8').toString('base64')
  return import(href)
}

/** Trim strings and drop the `null`s JSON has no use for. */
const clean = (v) => (typeof v === 'string' ? v.trim() : v)

function entry(e, groupKey) {
  const out = {
    id: e.id,
    order: e.order,
    group: e[groupKey] ?? null,
    type: clean(e.type) || 'film',
    title: clean(e.title),
    year: e.year ?? null,
    essential: e.essential === true,
    ageRating: clean(e.ageRating) || null,
    prereq: clean(e.prereq) || null,
    desc: clean(e.desc) || null,
    tmdbId: e.tmdbId ?? null,
  }
  // Series carry their shape; films leave it out entirely so the app's model can
  // tell "no season" from "season 0".
  if (e.type === 'series') {
    out.season = e.season ?? null
    out.episodes = e.episodes ?? null
    out.epStart = e.epStart ?? null
    out.epEnd = e.epEnd ?? null
  }
  if (e.seriesGroup) out.seriesGroup = e.seriesGroup
  if (e.runtime) out.runtime = e.runtime
  if (e.releaseDate) out.releaseDate = e.releaseDate
  if (e.releaseLabel) out.releaseLabel = e.releaseLabel
  if (e.releaseStatus) out.releaseStatus = e.releaseStatus
  return out
}

/**
 * Upstream stores one flat list whose `order` IS the viewing order, and a small
 * table of group headers (MCU phases, Sony eras, X-Men eras). The app wants the
 * same two things: entries, and the headings they fall under — so the headings
 * are emitted in the app's own shape (`{ id, name, tagline, summary }`) with the
 * studio's own words kept verbatim.
 */
function groups(phases) {
  return phases.map((p) => ({
    id: p.id,
    label: clean(p.label) || null,
    name: clean(p.name) || `Group ${p.id}`,
    tagline: clean(p.tagline) || null,
    summary: clean(p.summary) || null,
  }))
}

const byOrder = (a, b) => (a.order ?? 0) - (b.order ?? 0) || (a.id ?? 0) - (b.id ?? 0)

async function main() {
  mkdirSync(OUT, { recursive: true })

  const mcu = await load(SOURCES.marvel)
  const sony = await load(SOURCES.sony)
  const xmen = await load(SOURCES.xmen)

  const marvel = {
    id: 'marvel',
    name: 'Marvel',
    blurb: 'The Marvel Cinematic Universe in viewing order — every film, series and short.',
    groupLabel: 'Phase',
    groups: groups(mcu.PHASES),
    entries: [
      ...mcu.ESSENTIAL_LIST.map((e) => entry(e, 'phase')),
      ...mcu.ADDITIONAL_LIST.map((e) => entry(e, 'phase')),
      ...mcu.UPCOMING_PLACEHOLDERS.map((e) => entry(e, 'phase')),
    ].sort(byOrder),
  }

  const sonyData = {
    id: 'sony',
    name: 'Sony',
    blurb: "Spider-Man's own line — the Raimi and Webb films, the Spider-Verse, and the SSU.",
    groupLabel: 'Era',
    groups: groups(sony.SONY_PHASES),
    entries: sony.SONY_RAW.map((e) => entry(e, 'phase')).sort(byOrder),
  }

  const xmenData = {
    id: 'xmen',
    name: 'X-Men',
    blurb: 'The mutant saga, from the first X-Men through Logan and Deadpool.',
    groupLabel: 'Era',
    groups: groups(xmen.XMEN_PHASES),
    entries: xmen.XMEN_RAW.map((e) => entry(e, 'phase')).sort(byOrder),
  }

  const files = { marvel, sony: sonyData, xmen: xmenData }
  for (const [name, data] of Object.entries(files)) {
    const path = join(OUT, `${name}.json`)
    writeFileSync(path, JSON.stringify(data, null, 2) + '\n', 'utf8')
    console.log(
      `${name.padEnd(7)} ${String(data.entries.length).padStart(4)} entries · ` +
        `${data.groups.length} ${data.groupLabel.toLowerCase()}s → ${path}`
    )
  }
}

main().catch((err) => {
  console.error('import_incursion failed:', err.message)
  process.exit(1)
})
