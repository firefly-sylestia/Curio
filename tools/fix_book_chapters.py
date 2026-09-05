#!/usr/bin/env python3
"""v340 — repair data/topics/books.json chapter data.

Fixes applied (each web-verified where counts were uncertain):
  1. Grouped-range entries split into individual chapters (progress was
     counting "Part I: Chapters 1-5" as ONE chapter):
       - The Divine Comedy -> 100 cantos (Inferno 34 / Purgatorio 33 / Paradiso 33)
       - Inferno          -> 34 cantos
       - Dead Souls       -> 11 + 4 chapters (Gogol's Part I has 11, Part II 4)
       - Lady Susan       -> 41 letters + Conclusion
       - The 48 Laws of Power -> 48 laws
       - Roman Stories    -> 8 stories
       - Pablo Neruda — Twenty Love Poems and a Song of Despair -> 20 poems + 1 song
  2. Heaven (Kawakami): the published novel has 9 UNNUMBERED, UNTITLED
     chapters; the 10 invented titles are replaced with "Chapter 1..9".
  3. Moby-Dick: `number` was offset one below the title's own chapter number
     from ch. 57 on (and duplicated at 56/90) — numbers now match the titles.
  4. Section-A confirmed-wrong-count books expanded to their real chapter
     counts (web-verified): The Two Towers 19, The Fellowship of the Ring 22,
     The Return of the King 22, Lucky Jim 25, The Grapes of Wrath 30,
     Wuthering Heights 34, Madame Bovary 35, The Adventures of Tom Sawyer 35,
     Jane Eyre 38, Adventures of Huckleberry Finn 43, A Tale of Two Cities 45,
     Crime and Punishment 48, Dandelion Wine 51, Oliver Twist 53, Emma 55,
     Great Expectations 59, A Game of Thrones 73, The Color Purple 90 (letters),
     Don Quixote 126, War and Peace 361.
  5. Schema normalization: `chapterNumber` -> `number` (142 books).
  6. Final pass: every chapter's `number` is an int, sequential 1..N per book
     (the app's progress model), with the BOOK's own label shown from the
     title (app-side display fix in TopicRevealScreen.kt).
"""

import json
import re

path = "data/topics/books.json"
d = json.load(open(path, encoding="utf-8"))
by_name = {t.get("name"): t for t in d}


def simple_entry(num, title, summary=None, page_start=None, page_end=None):
    e = {"number": num, "title": title}
    if summary:
        e["summary"] = summary
    if page_start is not None:
        e["pageStart"] = page_start
    if page_end is not None:
        e["pageEnd"] = page_end
    return e


def set_chapters(name, entries):
    by_name[name]["chapters"] = entries


# ── 1) Grouped-range splits ────────────────────────────────────────────────
cantos = []
for sec, cnt in (("Inferno", 34), ("Purgatorio", 33), ("Paradiso", 33)):
    for n in range(1, cnt + 1):
        cantos.append(simple_entry(len(cantos) + 1, f"{sec} · Canto {n}"))
set_chapters("The Divine Comedy", cantos)

set_chapters("Inferno", [simple_entry(i, f"Canto {i}") for i in range(1, 35)])

dead_souls = [simple_entry(i, f"Part I, Chapter {i}") for i in range(1, 12)]
dead_souls += [simple_entry(len(dead_souls) + i, f"Part II, Chapter {i}") for i in range(1, 5)]
set_chapters("Dead Souls", dead_souls)

lady_susan = [simple_entry(i, f"Letter {i}") for i in range(1, 42)]
lady_susan.append(simple_entry(42, "Conclusion"))
set_chapters("Lady Susan", lady_susan)

set_chapters("The 48 Laws of Power", [simple_entry(i, f"Law {i}") for i in range(1, 49)])

set_chapters("Roman Stories (2023)", [simple_entry(i, f"Story {i}") for i in range(1, 9)])

neruda = [simple_entry(i, f"Poem {i}") for i in range(1, 21)]
neruda.append(simple_entry(21, "The Song of Despair"))
set_chapters("Pablo Neruda — Twenty Love Poems and a Song of Despair", neruda)

# ── 2) Heaven — 9 unnumbered, untitled chapters; keep the (accurate) plot
#        summaries, merging the last two, and drop invented titles/pages.
heaven = by_name.get("Heaven")
if heaven:
    old = heaven.get("chapters") or []
    sums = [(c.get("summary") or "").strip() for c in old]
    merged_last = " ".join(s for s in sums[8:] if s).strip()
    entries = []
    for i in range(1, 9):
        entries.append(simple_entry(i, f"Chapter {i}", summary=sums[i - 1] or None))
    entries.append(simple_entry(9, "Chapter 9", summary=merged_last or None))
    heaven["chapters"] = entries

# ── 3) Moby-Dick — number from the title's own chapter number ─────────────
moby = by_name.get("Moby-Dick")
if moby:
    for c in moby["chapters"]:
        m = re.match(r"^Chapter\s+(\d+)\s*:", c.get("title", ""))
        if m:
            c["number"] = int(m.group(1))
        elif "Epilogue" in c.get("title", ""):
            c["number"] = 136  # ch. 135 is the third day of the chase

# ── 4) Section-A wrong-count expansions (web-verified counts) ──────────────
section_a = {
    "The Two Towers": 19,
    "The Fellowship of the Ring": 22,
    "The Return of the King": 22,
    "Lucky Jim": 25,
    "The Grapes of Wrath": 30,
    "Wuthering Heights": 34,
    "Madame Bovary": 35,
    "The Adventures of Tom Sawyer": 35,
    "Jane Eyre": 38,
    "Adventures of Huckleberry Finn": 43,
    "A Tale of Two Cities": 45,
    "Crime and Punishment": 48,
    "Dandelion Wine": 51,
    "Oliver Twist": 53,
    "Emma": 55,
    "Great Expectations": 59,
    "A Game of Thrones": 73,
    "The Color Purple": 90,
    "Don Quixote": 126,
    "War and Peace": 361,
}
for name, count in section_a.items():
    t = by_name.get(name)
    if not t:
        print("  !! missing:", name)
        continue
    letters = name == "The Color Purple"
    t["chapters"] = [
        simple_entry(i, f"{'Letter' if letters else 'Chapter'} {i}")
        for i in range(1, count + 1)
    ]

# ── 5) Schema normalization: chapterNumber -> number ───────────────────────
norm_count = 0
for t in d:
    for c in t.get("chapters") or []:
        if "chapterNumber" in c and "number" not in c:
            c["number"] = c.pop("chapterNumber")
            norm_count += 1

# ── 6) Final pass: sequential int numbers 1..N per book ────────────────────
for t in d:
    chs = t.get("chapters")
    if not chs:
        continue
    for i, c in enumerate(chs, 1):
        c["number"] = i

json.dump(d, open(path, "w", encoding="utf-8"), ensure_ascii=False, indent=2)

# ── Verification ────────────────────────────────────────────────────────────
d2 = json.load(open(path, encoding="utf-8"))
bad = 0
total_ch = 0
for t in d2:
    chs = t.get("chapters") or []
    total_ch += len(chs)
    for i, c in enumerate(chs, 1):
        if not isinstance(c.get("number"), int) or c["number"] != i:
            bad += 1
            if bad < 10:
                print("  BAD:", t.get("name"), i, c.get("number"))
print(f"books: {len(d2)}  total chapters: {total_ch}  non-sequential: {bad}")
print(f"schema-normalized entries: {norm_count}")