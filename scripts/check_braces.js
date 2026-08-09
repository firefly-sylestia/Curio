#!/usr/bin/env node
/**
 * check_braces.js — Kotlin/KTS delimiter-balance checker.
 *
 * The Curio project has no local Android toolchain (CI owns Gradle builds),
 * so pre-commit static validation relies on cheap checks like this one. It
 * verifies that every `{`, `[`, `(` in a Kotlin source file is matched by
 * its closing delimiter.
 *
 * This is the repo-homed replacement for the ad-hoc delimiter checks that
 * used to be written to /tmp during a session (see app/AGENTS.md "Static
 * validation when Gradle is unavailable").
 *
 * The scanner walks the file character-by-character with a small state
 * machine, so it correctly ignores:
 *   - line comments (two slashes to end of line)
 *   - block comments — Kotlin block comments NEST, so a comment inside a
 *     comment is tracked with a depth counter
 *   - double-quoted strings incl. escapes and dollar-template bodies
 *   - triple-quoted (raw) strings
 *   - char literals incl. escapes
 * This processes source in order — a slash-slash inside a string or a
 * quote inside a comment is always handled correctly (regex-based
 * string/comment passes cannot guarantee that, which is why this script
 * exists).
 *
 * Usage:
 *   node scripts/check_braces.js               # check every .kt/.kts under the repo
 *   node scripts/check_braces.js <file> [...]  # check specific files
 *
 * Exit code 0 = balanced, 1 = at least one file failed, 2 = bad arguments.
 */
const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');
const EXTENSIONS = new Set(['.kt', '.kts']);
// Never descend into generated/cached dirs.
const SKIP_DIRS = new Set(['.git', '.gradle', 'build', 'node_modules']);

const PAIRS = { '{': '}', '[': ']', '(': ')' };

/** Returns an error message if delimiters are unbalanced, else null. */
function check(text) {
  const stack = [];
  let i = 0;
  const n = text.length;
  while (i < n) {
    const ch = text[i];
    const next = text[i + 1];

    // ── Line comment: `//` to end of line ───────────────────────────────
    if (ch === '/' && next === '/') {
      while (i < n && text[i] !== '\n') i++;
      continue;
    }

    // ── Block comment: `/* ... */` (Kotlin nests them) ──────────────────
    if (ch === '/' && next === '*') {
      let depth = 1;
      i += 2;
      while (i < n && depth > 0) {
        if (text[i] === '/' && text[i + 1] === '*') {
          depth++;
          i += 2;
        } else if (text[i] === '*' && text[i + 1] === '/') {
          depth--;
          i += 2;
        } else {
          i++;
        }
      }
      continue;
    }

    // ── Triple-quoted (raw) string: `""" ... """` ───────────────────────
    if (ch === '"' && next === '"' && text[i + 2] === '"') {
      i += 3;
      while (i < n) {
        if (text[i] === '"' && text[i + 1] === '"' && text[i + 2] === '"') {
          i += 3;
          break;
        }
        i++;
      }
      continue;
    }

    // ── Double-quoted string (escapes + $templates are inert here) ──────
    if (ch === '"') {
      i++;
      while (i < n) {
        if (text[i] === '\\') {
          i += 2;
          continue;
        }
        if (text[i] === '"') {
          i++;
          break;
        }
        i++;
      }
      continue;
    }

    // ── Char literal (escapes) ───────────────────────────────────────────
    if (ch === "'") {
      i++;
      while (i < n) {
        if (text[i] === '\\') {
          i += 2;
          continue;
        }
        if (text[i] === "'") {
          i++;
          break;
        }
        i++;
      }
      continue;
    }

    // ── Delimiters ───────────────────────────────────────────────────────
    if (PAIRS[ch]) {
      stack.push(ch);
      i++;
      continue;
    }
    if (ch === '}' || ch === ']' || ch === ')') {
      const top = stack.pop();
      if (!top || PAIRS[top] !== ch) {
        const line = text.slice(0, i).split('\n').length;
        return 'line ' + line + ': stray closing "' + ch + '" (expected "' +
          (top ? PAIRS[top] : 'nothing') + '")';
      }
      i++;
      continue;
    }

    i++;
  }

  if (stack.length) {
    const line = text.split('\n').length;
    return 'line ' + line + ': unclosed ' +
      stack.map((c) => PAIRS[c]).join('') + ' at end of file';
  }
  return null;
}

function collect(dir, out) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    if (SKIP_DIRS.has(entry.name)) continue;
    const p = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      collect(p, out);
    } else if (EXTENSIONS.has(path.extname(entry.name))) {
      out.push(p);
    }
  }
}

const args = process.argv.slice(2);
let files;
if (args.length > 0) {
  files = args.map((a) => path.resolve(process.cwd(), a));
  for (const f of files) {
    if (!fs.existsSync(f)) {
      console.error('No such file: ' + f);
      process.exit(2);
    }
  }
} else {
  files = [];
  collect(ROOT, files);
}

let failed = 0;
for (const file of files) {
  const err = check(fs.readFileSync(file, 'utf8'));
  if (err) {
    failed++;
    console.log(path.relative(ROOT, file) + ': ' + err);
  }
}
if (failed > 0) {
  console.log(failed + ' of ' + files.length + ' file(s) have unbalanced delimiters.');
  process.exit(1);
}
console.log('Brace balance OK (' + files.length + ' file(s) checked).');
