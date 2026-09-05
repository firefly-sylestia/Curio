#!/usr/bin/env python3
"""v340 — repair data/topics/albums.json track data.

Fixes applied (each web-verified):
  1. SPLIT entries that the CD/streaming indexes as SEPARATE tracks but the
     data merged into one (so the album shows them as one):
       - Daydream Nation (Sonic Youth): "Trilogy" -> 12. "Trilogy: a) The
         Wonder" (4:15) / 13. "Trilogy: b) Hyperstation" (7:12) /
         14. "Trilogy: z) Eliminator Jr." (2:37). Sonic Youth really label
         the third part "z)".
       - A Love Supreme (Coltrane): the merged Part 3/Part 4 entry splits
         into "Part 3: Pursuance" (10:42) and "Part 4: Psalm" (7:05).
       - Everything Will Be Alright in the End (Weezer): "The Futurescope
         Trilogy" splits into I. The Wasteland (1:56) / II. Anonymous (3:19)
         / III. Return to Ithaka (2:17).
  2. Moanin' (Art Blakey): track 4 title cleaned to "The Drum Thunder Suite"
     (one suite with three internal themes); track 8 was a DUPLICATE of that
     suite and is the RVG-edition bonus "Moanin' (Alternate Take)" instead.
  3. Clean the `" * ` / `" / ` quoting artifacts that jammed multi-part
     titles into one string, keeping single-indexed tracks as ONE track with
     their proper official title (medleys/suites stay single — only the
     quoting was broken):
       Grievous Angel "Medley: Live from Northern Quebec / Cash on the
         Barrelhead / Hickory Wind", Liege & Lief "Medley: The Lark in the
         Morning / Rakish Paddy / Foxhunter's Jig / Toss the Feathers",
       Beach Boys' Party! "Medley: I Get Around / Little Deuce Coupe",
       Sam Cooke "Medley: It's All Right / For Sentimental Reasons",
       In a Silent Way "Shhh / Peaceful" and "In a Silent Way / It's About
         That Time", Run the Jewels 3 "A Report to the Shareholders / Kill
         Your Masters", The Score "Manifest / Outro", Red Headed Stranger
         "Blue Rock Montana / Red Headed Stranger", Dopethrone "Weird Tales /
         Electric Frost / Golgotha / Altar of Melektaus", Bloody Kisses
         "Christian Woman", Deja Vu "Country Girl", The Yes Album
         "Starship Trooper", The Black Saint "Mode D: ... / Mode E: ... /
         Mode F: ...", Magnification "In the Presence Of", Minstrel "Baker
         St. Muse", Journey "A Seed's a Star / Tree Medley".
  4. Woodstock soundtrack: every title carried "Artist - Title (composer) -
     duration (extra)" garbage; cleaned to "Artist - Title" keeping the
     track's own duration field.
  5. Generic unquoting pass for any remaining stray quote artifacts.
"""

import json
import re

path = "data/topics/albums.json"
d = json.load(open(path, encoding="utf-8"))
by_name = {t.get("name"): t for t in d}


def tr(num, title, duration=""):
    return {"number": num, "title": title, "duration": duration}


def set_tracks(name, tracks):
    for i, t in enumerate(tracks, 1):
        t["number"] = i
    by_name[name]["tracks"] = tracks


# ── 1) Splits ──────────────────────────────────────────────────────────────
dn = by_name["Daydream Nation"]
dn_tr = [x for x in dn["tracks"] if x["number"] != 12]
dn_tr += [tr(12, "Trilogy: a) The Wonder", "4:15"),
          tr(13, "Trilogy: b) Hyperstation", "7:12"),
          tr(14, "Trilogy: z) Eliminator Jr.", "2:37")]
set_tracks("Daydream Nation", dn_tr)

als = by_name["A Love Supreme"]
als_tr = [x for x in als["tracks"] if x["number"] != 3]
als_tr += [tr(3, "Part 3: Pursuance", "10:42"), tr(4, "Part 4: Psalm", "7:05")]
set_tracks("A Love Supreme", als_tr)

ew = by_name["Everything Will Be Alright in the End"]
ew_tr = [x for x in ew["tracks"] if x["number"] != 11]
ew_tr += [tr(11, "The Futurescope Trilogy: I. The Wasteland", "1:56"),
          tr(12, "The Futurescope Trilogy: II. Anonymous", "3:19"),
          tr(13, "The Futurescope Trilogy: III. Return to Ithaka", "2:17")]
set_tracks("Everything Will Be Alright in the End", ew_tr)

# ── 2) Moanin' — clean t4, fix duplicate t8 ────────────────────────────────
mo = by_name["Moanin'"]
for t in mo["tracks"]:
    if t["number"] == 4:
        t["title"] = "The Drum Thunder Suite"
    elif t["number"] == 8:
        t["title"] = "Moanin' (Alternate Take)"
        t["duration"] = ""

# ── 3) Curated single-track title cleans ───────────────────────────────────
def retitle(name, num, title):
    for t in by_name[name]["tracks"]:
        if t["number"] == num:
            t["title"] = title
            return
    raise KeyError(f"{name} #{num}")

retitle("Grievous Angel", 6, "Medley: Live from Northern Quebec / Cash on the Barrelhead / Hickory Wind")
retitle("Liege & Lief", 6, "Medley: The Lark in the Morning / Rakish Paddy / Foxhunter's Jig / Toss the Feathers")
retitle("Beach Boys' Party!", 10, "Medley: I Get Around / Little Deuce Coupe")
retitle("Live at the Harlem Square Club, 1963", 4, "Medley: It's All Right / For Sentimental Reasons")
retitle("In a Silent Way", 1, "Shhh / Peaceful")
retitle("In a Silent Way", 2, "In a Silent Way / It's About That Time")
retitle("Run the Jewels 3", 13, "A Report to the Shareholders / Kill Your Masters")
retitle("The Score", 13, "Manifest / Outro")
retitle("Red Headed Stranger", 4, "Blue Rock Montana / Red Headed Stranger")
retitle("Dopethrone", 3, "Weird Tales / Electric Frost / Golgotha / Altar of Melektaus")
retitle("Bloody Kisses", 2, "Christian Woman")
retitle("Déjà Vu", 9, "Country Girl")
retitle("The Yes Album", 3, "Starship Trooper")
retitle("The Black Saint and the Sinner Lady", 4,
         "Mode D: Trio and Group Dancers / Mode E: Single Solos and Group Dance / Mode F: Group and Solo Dance")
retitle("Magnification", 9, "In the Presence Of")
retitle("Minstrel in the Gallery", 6, "Baker St. Muse")
retitle("Journey Through the Secret Life of Plants", 17, "A Seed's a Star / Tree Medley")

# Hymns to the Silence: drop " - (composer) - mm:ss" credit garbage
hym = by_name["Hymns to the Silence"]
for t in hym["tracks"]:
    m = re.match(r"^(.*?)\s*–\s*\([^)]*\)\s*–\s*\d+:\d+(.*)$", t["title"])
    if m:
        t["title"] = (m.group(1) + " " + m.group(2)).strip()

# ── 4) Woodstock soundtrack — "Artist – Title (composers) – dur" → clean ───
ws = by_name["Woodstock: Music from the Original Soundtrack"]
special = {
    3: "Richie Havens – Freedom (Motherless Child)",
    6: "Country Joe McDonald – The Fish Cheer / I-Feel-Like-I'm-Fixin'-to-Die Rag",
    9: "Crosby, Stills & Nash – Suite: Judy Blue Eyes",
    17: "Sly and the Family Stone – Medley: Dance to the Music / Music Lover / I Want to Take You Higher",
}
for t in ws["tracks"]:
    if t["number"] in special:
        t["title"] = special[t["number"]]
        continue
    title = t["title"]
    # cut everything from the LAST " – m:ss" (the sleeve duration) onward
    m = list(re.finditer(r"\s–\s\d+:\d+", title))
    if m:
        title = title[: m[-1].start()]
    # strip one trailing composer "(...)" group (not part of the title)
    m = re.search(r"\s*\([^()]*\)\s*$", title)
    if m and not title[: m.start()].rstrip().endswith(("-", "–")):
        title = title[: m.start()]
    title = re.sub(r"\s*\((\d+:\d+)\)\s*$", "", title)
    t["title"] = title.strip()

# ── 5) Generic unquote pass for any remaining literal quotes ───────────────
for alb in d:
    for t in alb.get("tracks") or []:
        title = t.get("title") or ""
        if '"' in title:
            title = re.sub(r'"\s*\*\s*"?', " / ", title)  # "A" * "B" -> A / B
            title = title.replace('"', "")
            title = re.sub(r"\s+", " ", title).strip()
            title = re.sub(r"\s*/\s*", " / ", title)
            t["title"] = title

json.dump(d, open(path, "w", encoding="utf-8"), ensure_ascii=False, indent=2)

# ── Verification ───────────────────────────────────────────────────────────
d2 = json.load(open(path, encoding="utf-8"))
rem = []
for alb in d2:
    for i, t in enumerate((alb.get("tracks") or []), 1):
        if t["number"] != i:
            rem.append((alb["name"], "num"))
        if '"' in (t.get("title") or ""):
            rem.append((alb["name"], t["title"]))
print("albums:", len(d2))
print("remaining quote artifacts:", len(rem))
for r in rem[:10]:
    print("  ", r)
print("Daydream Nation:", [ (t["number"], t["title"], t["duration"]) for t in by_name["Daydream Nation"]["tracks"]][-3:])
print("A Love Supreme:", [(t["number"], t["title"], t["duration"]) for t in by_name["A Love Supreme"]["tracks"]])
print("Moanin' t4/t8:", [(t["number"], t["title"]) for t in by_name["Moanin'"]["tracks"] if t["number"] in (4, 8)])
print("Woodstock t1/t3/t9/t17:", [(t["number"], t["title"]) for t in by_name["Woodstock: Music from the Original Soundtrack"]["tracks"] if t["number"] in (1, 3, 9, 17)])