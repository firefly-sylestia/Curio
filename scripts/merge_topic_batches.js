#!/usr/bin/env node
/**
 * Merges hand-written topic batches (scripts/batches/<category>_<n>.json)
 * into the main topic files under app/src/main/assets/topics/.
 *
 * Rules:
 *  - A batch file name must be `<category>_<anything>.json`, where
 *    `<category>` matches a topic filename (e.g. `directors_b1.json`).
 *  - Every batch is a bare JSON array of topic objects.
 *  - Ids AND normalized names are deduped within and across batches; an id
 *    or name that already exists in the main file is SKIPPED (never
 *    overwrite existing entries, never create duplicate names).
 *  - The merged file is written back with 2-space indent + trailing newline.
 *
 * Usage: node scripts/merge_topic_batches.js [category...]
 *        (no args = merge every batch file found)
 */
const fs = require('fs');
const path = require('path');

const TOPICS_DIR = 'app/src/main/assets/topics';
const BATCHES_DIR = 'scripts/batches';
const CATEGORIES = [
  'artists', 'albums', 'directors', 'films', 'authors', 'books',
  'painters', 'artworks', 'scientists', 'discoveries', 'wildcard'
];

function readJson(p) {
  return JSON.parse(fs.readFileSync(p, 'utf8'));
}

function writeJson(p, data) {
  fs.writeFileSync(p, JSON.stringify(data, null, 2) + '\n', 'utf8');
}

function normName(n) {
  return String(n || '')
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^a-z0-9]+/g, ' ')
    .trim();
}

const only = process.argv.slice(2).filter(c => CATEGORIES.includes(c));
const batchFiles = fs.existsSync(BATCHES_DIR)
  ? fs.readdirSync(BATCHES_DIR).filter(f => f.endsWith('.json'))
  : [];

const skipped = [];
let merged = 0;

for (const category of CATEGORIES) {
  if (only.length && !only.includes(category)) continue;
  const mainPath = path.join(TOPICS_DIR, category + '.json');
  if (!fs.existsSync(mainPath)) continue;
  const main = readJson(mainPath);
  const ids = new Set(main.map(t => t.id));
  const names = new Set(main.map(t => normName(t.name)));
  const relevant = batchFiles.filter(f => f.startsWith(category + '_'));
  for (const bf of relevant.sort()) {
    const batch = readJson(path.join(BATCHES_DIR, bf));
    if (!Array.isArray(batch)) {
      console.error(`SKIP ${bf}: not a JSON array`);
      continue;
    }
    for (const topic of batch) {
      if (!topic || typeof topic !== 'object' || !topic.id) {
        console.error(`SKIP ${bf}: topic without id`);
        continue;
      }
      const key = topic.id;
      const nk = normName(topic.name);
      if (ids.has(key) || names.has(nk)) {
        skipped.push(`${bf}:${topic.id}${names.has(nk) ? ' (name dup)' : ''}`);
        continue;
      }
      ids.add(key);
      names.add(nk);
      main.push(topic);
      merged++;
    }
  }
  if (relevant.length) writeJson(mainPath, main);
}

console.log(`Merged ${merged} topic(s).`);
if (skipped.length) console.log(`Skipped ${skipped.length} duplicate(s): ${skipped.slice(0, 10).join(', ')}${skipped.length > 10 ? '…' : ''}`);
