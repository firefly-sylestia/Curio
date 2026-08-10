#!/usr/bin/env python3
"""Validate app/src/main/assets/topics/*.json: parse + required fields + counts."""
import glob
import json
import sys

OK = True
REQUIRED = ("id", "categoryId", "name", "teaser", "exploreAction")
EA_REQUIRED = ("verb", "targetName", "instruction")

for f in sorted(glob.glob("app/src/main/assets/topics/*.json")):
    try:
        with open(f, encoding="utf-8") as fh:
            data = json.load(fh)
    except Exception as e:
        print(f"FAIL JSON PARSE: {f}: {e}")
        OK = False
        continue
    if not isinstance(data, list):
        print(f"FAIL NOT A LIST: {f}")
        OK = False
        continue
    bad = []
    for i, t in enumerate(data):
        if not isinstance(t, dict):
            bad.append((i, "not an object"))
            continue
        for req in REQUIRED:
            if req not in t:
                bad.append((i, f"missing '{req}'"))
        ea = t.get("exploreAction")
        if isinstance(ea, dict):
            for er in EA_REQUIRED:
                if er not in ea:
                    bad.append((i, f"exploreAction missing '{er}'"))
        elif ea is not None:
            bad.append((i, "exploreAction not an object"))
    if bad:
        OK = False
        print(f"FAIL {f}: {len(bad)} bad topic(s): {bad[:5]}...")
    else:
        print(f"OK   {f}: {len(data)} topics")

print("\nRESULT:", "ALL FILES VALID" if OK else "ERRORS FOUND")
sys.exit(0 if OK else 1)
