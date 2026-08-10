#!/usr/bin/env python3
"""
Mirror of `app/build.gradle.kts:validateTopics`.

Runs the SAME field-by-field checks the Gradle task does, against every
JSON file in `app/src/main/assets/topics/`. Lets us catch schema drift
locally — `./gradlew validateTopics` is forbidden in this environment
(no Android SDK) per root `AGENTS.md`.

Asserts (per the fixed Gradle task):
  - root is a non-empty BARE JSON array of topic objects
  - every topic has id (unique cross-file) + categoryId (matches filename)
  - every topic has subtype/name/teaser/imageUrl/exploreAction
  - every exploreAction has verb/targetName/durationMinutes/instruction
  - every instruction <= 600 chars
  - tier, if present, is in 1..3

Exit 0 = all OK; exit 1 = at least one failure (print first 10).
"""

import json
import sys
from pathlib import Path

TOPICS_DIR = Path("app/src/main/assets/topics")
EXPECTED_CATEGORIES = {
    "artists", "albums", "songs", "directors", "films", "series", "authors",
    "books", "painters", "artworks", "scientists", "discoveries", "anime",
    "manga", "manhwa", "games", "mythology", "sports", "food", "internet",
    "wildcard",
}
REQUIRED_TOPIC_FIELDS = ("subtype", "name", "teaser", "imageUrl", "exploreAction")
REQUIRED_ACTION_FIELDS = ("verb", "targetName", "durationMinutes", "instruction")
MAX_INSTRUCTION_LEN = 600  # matches the Gradle validateTopics task (app/build.gradle.kts)


def validate_one(path: Path, seen_ids: dict[str, str]) -> tuple[int, int, list[str]]:
    """Returns (num_topics, num_errors, error_messages)."""
    try:
        raw = path.read_text(encoding="utf-8")
        data = json.loads(raw)
    except json.JSONDecodeError as e:
        return 0, 1, [f"{path.name}: invalid JSON at line {e.lineno} col {e.colno}: {e.msg}"]
    if not isinstance(data, list):
        return 0, 1, [
            f"{path.name}: root must be a BARE JSON array of topic objects "
            f"(got {type(data).__name__})"
        ]

    expected_cat = path.name.removesuffix(".json").upper()
    errors: list[str] = []
    for idx, topic in enumerate(data):
        if not isinstance(topic, dict):
            errors.append(f"{path.name}: topic #{idx} is not an object (got {type(topic).__name__})")
            continue

        tid = topic.get("id")
        if not isinstance(tid, str) or not tid.strip():
            errors.append(f"{path.name}: topic #{idx} missing or blank `id`")
            continue

        prior = seen_ids.get(tid)
        if prior:
            errors.append(f"duplicate topic id '{tid}' across files: first in {prior}, also in {path.name}")
        seen_ids[tid] = path.name

        cat = topic.get("categoryId")
        if not isinstance(cat, str) or not cat.strip():
            errors.append(f"{path.name}: topic '{tid}' missing or non-string `categoryId`")
        elif cat != expected_cat:
            errors.append(
                f"{path.name}: topic '{tid}' categoryId '{cat}' "
                f"does not match filename '{expected_cat}'"
            )

        for f in REQUIRED_TOPIC_FIELDS:
            if f not in topic:
                errors.append(f"{path.name}: topic '{tid}' missing required field `{f}`")

        action = topic.get("exploreAction")
        if isinstance(action, dict):
            for f in REQUIRED_ACTION_FIELDS:
                if f not in action:
                    errors.append(f"{path.name}: topic '{tid}' exploreAction missing required field `{f}`")
            instr = action.get("instruction")
            if instr is not None and isinstance(instr, str):
                if len(instr) > MAX_INSTRUCTION_LEN:
                    errors.append(
                        f"{path.name}: topic '{tid}' instruction is {len(instr)} chars "
                        f"(max {MAX_INSTRUCTION_LEN})"
                    )
        elif "exploreAction" in topic:
            errors.append(f"{path.name}: topic '{tid}' exploreAction is not an object")

        if "tier" in topic:
            tier = topic["tier"]
            if not isinstance(tier, int) or tier not in (1, 2, 3):
                errors.append(f"{path.name}: topic '{tid}' tier must be 1, 2, or 3 (got {tier!r})")

    return len(data), len(errors), errors


def main() -> int:
    if not TOPICS_DIR.exists():
        print(f"❌ {TOPICS_DIR} missing", file=sys.stderr)
        return 1

    files = sorted(TOPICS_DIR.glob("*.json"))
    if len(files) != len(EXPECTED_CATEGORIES):
        present = {p.name.removesuffix(".json") for p in files}
        missing = EXPECTED_CATEGORIES - present
        extra = present - EXPECTED_CATEGORIES
        if missing:
            print(f"⚠️  Missing JSON files for: {sorted(missing)}")
        if extra:
            print(f"⚠️  Unexpected JSON files: {sorted(extra)}")

    seen_ids: dict[str, str] = {}
    total_topics = 0
    total_errors = 0
    all_errors: list[str] = []

    print(f"── Validating {len(files)} files ──")
    for path in files:
        n_topics, n_errors, errs = validate_one(path, seen_ids)
        total_topics += n_topics
        total_errors += n_errors
        if errs:
            all_errors.extend(errs)
            print(f"✗ {path.name:18} {n_topics:4} topics, {n_errors} errors")
        else:
            print(f"✓ {path.name:18} {n_topics:4} topics validated")

    print(f"\n── Summary ──")
    print(f"  files:     {len(files)}")
    print(f"  topics:    {total_topics}")
    print(f"  unique ids:{len(seen_ids)}")
    print(f"  errors:    {total_errors}")

    if all_errors:
        print(f"\n── First {min(10, len(all_errors))} errors ──")
        for e in all_errors[:10]:
            print(f"  • {e}")
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
