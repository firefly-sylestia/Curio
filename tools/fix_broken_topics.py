#!/usr/bin/env python3
"""fix_broken_topics.py — repair topics where exploreAction is a bare string
instead of a Map, and tier is "standard" instead of an int.

Affected files: food.json, games.json, mathematics.json (208 topics total).
"""
import json, os, sys

TOPICS = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..",
                       "data", "topics")

# Per-category defaults from SCHEMA.md
CATEGORY_DEFAULTS = {
    "FOOD":          {"verb": "Read",    "minutes": 30},
    "GAMES":         {"verb": "Watch",   "minutes": 30},
    "MATHEMATICS":   {"verb": "Explore", "minutes": 30},
    "ECONOMICS":     {"verb": "Explore", "minutes": 30},
    "LANGUAGE":      {"verb": "Explore", "minutes": 30},
    "MEDICINE":      {"verb": "Explore", "minutes": 30},
    "PSYCHOLOGY":    {"verb": "Explore", "minutes": 30},
    "SPORTS":        {"verb": "Watch",   "minutes": 30},
    "WILDCARD":      {"verb": "Explore", "minutes": 30},
}


def generate_explore_action(topic: dict) -> dict:
    """Build a schema-compliant exploreAction for a topic with an empty one."""
    cat = topic.get("categoryId", "")
    defaults = CATEGORY_DEFAULTS.get(cat, {"verb": "Explore", "minutes": 30})
    name = topic.get("name", "this topic")

    # targetName pattern: "{name} end-to-end" (matches existing convention)
    target_name = f"{name} end-to-end"

    # instruction: actionable, specific, curiously-framed
    instruction = (
        f"Read one thorough overview of {name} — pay attention to what "
        f"makes it worth knowing, then try explaining its most surprising "
        f"detail to someone else in under a minute."
    )

    return {
        "verb": defaults["verb"],
        "targetName": target_name,
        "durationMinutes": defaults["minutes"],
        "instruction": instruction[:600],
    }


def fix_file(filepath: str) -> int:
    """Fix broken topics in a single JSON file. Returns count fixed."""
    with open(filepath) as f:
        data = json.load(f)

    fixed = 0
    for topic in data:
        changed = False

        # Fix exploreAction: must be a Map, not a string
        ea = topic.get("exploreAction")
        if isinstance(ea, str):
            topic["exploreAction"] = generate_explore_action(topic)
            changed = True

        # Fix tier: "standard" → 2
        tier = topic.get("tier")
        if tier == "standard":
            topic["tier"] = 2
            changed = True

        # Ensure byline exists
        if "byline" not in topic:
            topic["byline"] = ""
            changed = True

        if changed:
            fixed += 1

    if fixed:
        with open(filepath, "w") as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
            f.write("\n")

    return fixed


def main():
    files = ["food.json", "games.json", "mathematics.json",
             "economics.json", "language.json", "medicine.json",
             "psychology.json", "sports.json", "wildcard.json"]
    total = 0
    for fname in files:
        path = os.path.join(TOPICS, fname)
        if not os.path.exists(path):
            print(f"⚠️  {fname}: not found, skipping")
            continue
        n = fix_file(path)
        print(f"✓ {fname}: fixed {n} topics")
        total += n
    print(f"\n── Total: {total} topics fixed")
    return 0


if __name__ == "__main__":
    sys.exit(main())
