#!/usr/bin/env python3
"""Extract all user-visible UI copy from the Curio Android app into a
reference markdown file. Excludes the pet dialogue files (Curie's lines).
"""
import os, re, sys

SRC = "app/src/main/java"
OUT = "app-texts.md"

# Files carrying Curie's dialogue lines — excluded per request ("no pet dialogs").
PET_FILES = {
    "app/src/main/java/com/curio/app/data/CurioPet.kt",
    "app/src/main/java/com/curio/app/ui/pet/CurioFloatingPet.kt",
    "app/src/main/java/com/curio/app/ui/pet/CurioPetSprite.kt",
    "app/src/main/java/com/curio/app/ui/pet/CurioFlowerBed.kt",
    "app/src/main/java/com/curio/app/ui/pet/CurioPetCompanion.kt",
}

# ── string literal scanner (handles "...", escapes, and """...""" blocks) ──
LIT = re.compile(r'("(?:[^"\\\n]|\\.)*"|"""(?:[^"]|"(?!""))*""")')

def unquote(s):
    if s.startswith('"""'):
        return s[3:-3]
    return s[1:-1]

def clean(s):
    s = s.replace('\\"', '"').replace("\\'", "'").replace("\\n", "\n")
    s = s.replace("\\t", "\t").replace("\\$", "$")
    # resolve unicode escapes (\\u201c → “ etc.)
    s = re.sub(r"\\u([0-9a-fA-F]{4})", lambda m: chr(int(m.group(1), 16)), s)
    # collapse whitespace runs so multiline blocks read as single lines
    return re.sub(r"\s+", " ", s).strip()

# ── keep/drop heuristics ──
LABEL_RE = re.compile(r"^[A-Z][a-zA-Zà-ÿ'’\u2019-]*( [a-zà-ÿA-Z0-9&'’\u2019.,!?\-]+)*$")
LOW_WORD_RE = re.compile(r"^[a-z][a-zA-Z0-9_]*$")            # "listen", "topicLoading", "sentimentScale"
KEBAB_RE = re.compile(r"^[a-zA-Z0-9]+(?:[-_][a-zA-Z0-9]+)+$") # "topic-loading", "SENTIMENT_LIKE", "express-yourself"
ALLCAPS_RE = re.compile(r"^[A-Z]{1,8}$")                     # "SPIN", "OK", "ADD"
DIGIT_TOKEN_RE = re.compile(r"^[0-9%$~.]+$")                  # "~2", "%1$s", "1200"

PUNCT = set(".!?,:;—…'\u2019()")

def keep(s):
    if not s or len(s) < 2:
        return False
    if len(s) > 400:
        return True  # long prose blocks are copy
    has_space = " " in s
    if s.startswith(("http://", "https://", "file://", "content://", "@", "R.", "#", "#FF")):
        return False
    if re.search(r"[{}]", s) and not has_space:
        return False
    if re.search(r"[\\=/]", s) and not has_space:
        return False
    if DIGIT_TOKEN_RE.match(s):
        return False
    if LOW_WORD_RE.match(s):
        return False            # single lowercase token = key/route/verb id
    if KEBAB_RE.match(s):
        return False            # kebab/snake token = key/id
    if re.match(r"^[A-Za-z0-9._-]+$", s) and "." in s:
        return False            # dotted token = package/action/date key
    if "," in s and not has_space:
        return False            # "artists,albums" route value, not copy
    if any(c in PUNCT for c in s):
        return True             # sentence punctuation → copy
    if has_space:
        return True             # multi-word → copy
    if LABEL_RE.match(s) and not any(c.isdigit() for c in s):
        return True             # "Home", "Skip", "Save entry"
    if ALLCAPS_RE.match(s):
        return True             # "SPIN", "OK"
    return False

# ── gather ──
sections = []          # (heading, [strings])
seen_global = set()

for root, dirs, files in os.walk(SRC):
    for fn in sorted(files):
        if not fn.endswith(".kt"):
            continue
        path = os.path.join(root, fn)
        if path in PET_FILES:
            continue
        src = open(path, encoding="utf-8").read()
        rel = path[len("app/src/main/java/"):]
        found = []
        for m in LIT.finditer(src):
            s = clean(unquote(m.group(1)))
            if keep(s):
                # dedupe within file (case-insensitive-ish, keep first)
                key = s.lower()
                if key in seen_global:
                    continue
                seen_global.add(key)
                found.append(s)
        if found:
            sections.append((rel, found))

# strings.xml additions
xml_path = "app/src/main/res/values/strings.xml"
if os.path.exists(xml_path):
    txt = open(xml_path, encoding="utf-8").read()
    found = []
    for m in re.finditer(r'<string name="([^"]+)">([^<]*)</string>', txt):
        s = m.group(2).strip()
        if s and (s.lower() not in seen_global):
            seen_global.add(s.lower())
            found.append(s)
    if found:
        sections.append(("res/values/strings.xml", found))

total = sum(len(s) for _, s in sections)
print(f"{len(sections)} files, {total} strings")

with open(OUT, "w", encoding="utf-8") as f:
    f.write("# Curio — App Texts\n\n")
    f.write("Every user-visible text in the Android app: headings, subtitles, hints, "
            "helper copy, buttons, dialogs, toasts, empty states and content descriptions. ")
    f.write("Curie's pet dialogue lines are excluded.\n\n")
    f.write(f"_{total} strings across {len(sections)} files — extracted from "
            "`app/src/main/java` + `app/src/main/res/values/strings.xml`._\n\n")
    f.write("---\n\n")
    for rel, strs in sections:
        f.write(f"## `{rel}`\n\n")
        for s in strs:
            # escape nothing; just bullet the text as-is
            f.write(f"- {s}\n")
        f.write("\n")
print(f"written to {OUT}")
