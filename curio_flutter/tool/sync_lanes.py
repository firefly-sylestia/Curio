#!/usr/bin/env python3
"""Regenerate lib/data/lanes.dart from the Android catalog.

A lane is declared ONCE, in Kotlin:

  * its slug (which is also its topic file name) in
    `app/src/main/java/com/curio/app/data/Category.kt` (`routeSlug`);
  * its label in `CurioCategories.all` (same file);
  * its accent in `app/src/main/java/com/curio/app/ui/theme/CurioColors.kt`.

This script reads those three and writes the Dart table, so the phone and the
new Windows/iOS shell can never disagree about a lane's name, order or colour.
Run it after adding or recolouring a lane:

    python3 tool/sync_lanes.py
"""
from __future__ import annotations

import pathlib
import re
import sys

HERE = pathlib.Path(__file__).resolve().parent
PROJECT = HERE.parent
REPO = PROJECT.parent
CATEGORY_KT = REPO / "app/src/main/java/com/curio/app/data/Category.kt"
COLORS_KT = REPO / "app/src/main/java/com/curio/app/ui/theme/CurioColors.kt"
OUT = PROJECT / "lib/data/lanes.dart"

HEADER = """// GENERATED from the Kotlin catalog — do not hand-edit the table.
//
// A lane's SLUG (which is also its topic file name) is declared once, in
// `app/src/main/java/com/curio/app/data/Category.kt` (`routeSlug`), and its
// accent in `app/src/main/java/com/curio/app/ui/theme/CurioColors.kt`. The
// Android app stays the one place a lane is declared; `tool/sync_lanes.py`
// regenerates this file from it, so the shells can never drift on a name or a
// colour.

/// One Curio lane: its route slug (== `assets/topics/<slug>.json`), its label
/// and its accent (0xAARRGGBB).
class Lane {
  const Lane(this.id, this.label, this.accent);

  final String id;
  final String label;
  final int accent;

  @override
  String toString() => 'Lane($id)';
}

/// The catalog's lanes, in the order the Android catalog declares them.
const List<Lane> kLanes = <Lane>[
"""

FOOTER = """];

/// The lane for [id], or the wildcard lane when a stored preference names a lane
/// that no longer exists (a stale `prefs.json` must never crash the shell).
Lane laneById(String id) {
  for (final lane in kLanes) {
    if (lane.id == id) return lane;
  }
  return kLanes.last;
}
"""


def main() -> int:
    category = CATEGORY_KT.read_text(encoding="utf-8")
    colors_kt = COLORS_KT.read_text(encoding="utf-8")

    slugs = dict(re.findall(r"(\w+)\s*->\s*\"([a-z\-]+)\"", category))
    hexes = dict(
        re.findall(
            r"val\s+(\w+)\s*=\s*Color\(0xFF([0-9A-Fa-f]{6})\)\s*(?://.*)?$",
            colors_kt,
            re.M,
        )
    )
    aliases = dict(
        re.findall(r"val\s+(\w+)\s*=\s*(\w+)\s*(?://.*)?$", colors_kt, re.M)
    )

    def resolve(name: str, depth: int = 0) -> str | None:
        if name in hexes:
            return hexes[name]
        if name in aliases and depth < 5:
            return resolve(aliases[name], depth + 1)
        return None

    blocks = re.findall(
        r"CurioCategory\(\s*id\s*=\s*CategoryId\.(\w+),\s*"
        r"displayName\s*=\s*\"([^\"]+)\",\s*"
        r"accent\s*=\s*CurioColors\.(\w+)",
        category,
    )
    if not blocks:
        print("sync_lanes: no CurioCategory blocks found", file=sys.stderr)
        return 1
    if len(blocks) != len(slugs):
        print(
            f"sync_lanes: {len(blocks)} categories but {len(slugs)} route slugs",
            file=sys.stderr,
        )
        return 1

    rows = []
    for cid, label, accent in blocks:
        hue = resolve(accent)
        if hue is None:
            print(f"sync_lanes: cannot resolve {accent}", file=sys.stderr)
            return 1
        escaped = label.replace("'", "\\'")
        rows.append(f"  Lane('{slugs[cid]}', '{escaped}', 0xFF{hue.upper()}),")

    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(HEADER + "\n".join(rows) + "\n" + FOOTER, encoding="utf-8")
    print(f"sync_lanes: wrote {len(rows)} lanes to {OUT.relative_to(REPO)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
