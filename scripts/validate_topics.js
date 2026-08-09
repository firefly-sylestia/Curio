#!/usr/bin/env node
/**
 * Node mirror of `scripts/validate_topics.py` (which mirrors the Gradle
 * `validateTopics` task). python3 is not available in this environment, so
 * this port runs the SAME checks, matching the Python behavior exactly:
 *   - root is a BARE JSON array of topic objects
 *   - every topic has a non-blank id, unique across all files
 *   - categoryId matches the filename (uppercased)
 *   - every REQUIRED_TOPIC_FIELDS key is PRESENT
 *   - every exploreAction REQUIRED_ACTION_FIELDS key is PRESENT
 *   - instruction, when a string, is <= 600 chars
 *   - tier, when present, is an int in 1..3
 * Exit 0 = all OK; exit 1 = failures (first 15 printed).
 */
const fs = require('fs');
const path = require('path');

const TOPICS_DIR = 'app/src/main/assets/topics';
const EXPECTED_CATEGORIES = [
  'artists', 'albums', 'directors', 'films', 'authors', 'books',
  'painters', 'artworks', 'scientists', 'discoveries', 'wildcard'
];
const REQUIRED_TOPIC_FIELDS = ['subtype', 'name', 'teaser', 'imageUrl', 'exploreAction'];
const REQUIRED_ACTION_FIELDS = ['verb', 'targetName', 'durationMinutes', 'instruction'];
const MAX_INSTRUCTION_LEN = 600;

const seenIds = new Map();
const errors = [];
let totalTopics = 0;

for (const file of EXPECTED_CATEGORIES) {
  const p = path.join(TOPICS_DIR, file + '.json');
  if (!fs.existsSync(p)) {
    errors.push(`${file}.json: MISSING file`);
    continue;
  }
  let data;
  try {
    data = JSON.parse(fs.readFileSync(p, 'utf8'));
  } catch (e) {
    errors.push(`${file}.json: invalid JSON: ${e.message}`);
    continue;
  }
  if (!Array.isArray(data)) {
    errors.push(`${file}.json: root must be a BARE JSON array (got ${typeof data})`);
    continue;
  }
  const expectedCat = file.toUpperCase();
  data.forEach((topic, idx) => {
    totalTopics++;
    const tid = topic && topic.id;
    if (!topic || typeof topic !== 'object') {
      errors.push(`${file}.json: topic #${idx} is not an object`);
      return;
    }
    if (typeof tid !== 'string' || !tid.trim()) {
      errors.push(`${file}.json: topic #${idx} missing or blank id`);
      return;
    }
    if (seenIds.has(tid)) {
      errors.push(`duplicate topic id '${tid}' across files: first in ${seenIds.get(tid)}, also in ${file}.json`);
    }
    seenIds.set(tid, file + '.json');

    const cat = topic.categoryId;
    if (typeof cat !== 'string' || !cat.trim()) {
      errors.push(`${file}.json: topic '${tid}' missing or non-string categoryId`);
    } else if (cat !== expectedCat) {
      errors.push(`${file}.json: topic '${tid}' categoryId '${cat}' does not match filename '${expectedCat}'`);
    }

    for (const f of REQUIRED_TOPIC_FIELDS) {
      if (!(f in topic)) {
        errors.push(`${file}.json: topic '${tid}' missing required field '${f}'`);
      }
    }

    const action = topic.exploreAction;
    if (action && typeof action === 'object') {
      for (const f of REQUIRED_ACTION_FIELDS) {
        if (!(f in action)) {
          errors.push(`${file}.json: topic '${tid}' exploreAction missing required field '${f}'`);
        }
      }
      const instr = action.instruction;
      if (instr != null && typeof instr === 'string' && instr.length > MAX_INSTRUCTION_LEN) {
        errors.push(`${file}.json: topic '${tid}' instruction is ${instr.length} chars (max ${MAX_INSTRUCTION_LEN})`);
      }
    } else if ('exploreAction' in topic) {
      errors.push(`${file}.json: topic '${tid}' exploreAction is not an object`);
    }

    if ('tier' in topic) {
      const tier = topic.tier;
      if (typeof tier !== 'number' || ![1, 2, 3].includes(tier)) {
        errors.push(`${file}.json: topic '${tid}' tier must be 1, 2, or 3 (got ${JSON.stringify(tier)})`);
      }
    }
  });
}

console.log(`Validated ${totalTopics} topics across ${EXPECTED_CATEGORIES.length} files.`);
if (errors.length) {
  console.log(`${errors.length} error(s). First 15:`);
  errors.slice(0, 15).forEach(e => console.log('  - ' + e));
  process.exit(1);
}
console.log('All topic files OK.');
