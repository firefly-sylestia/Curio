#!/usr/bin/env python3
"""add_franchise_tags.py — tag matching films with their universe franchise.

The Spin filter sheet buckets known blockbuster franchises (MCU, Star Wars,
DC, Harry Potter, Lord of the Rings, Pixar, Studio Ghibli, Disney — the
`FranchiseTags` set in SpinScreen.kt) into their own "Franchise" row.

Films without any franchise tag never get one (their own universe), and the
script is idempotent — running it again only fills in missing franchise tags
for names it recognizes.

Usage: python3 scripts/add_franchise_tags.py && python3 scripts/validate_topics.py
"""

import json
import os

TOPICS_DIR = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets", "topics")

# franchise -> list of name substrings (case-insensitive)
FRANCHISE_RULES = {
    "MCU": [
        "Iron Man", "The Incredible Hulk", "Thor", "Captain America", "Avengers",
        "Guardians of the Galaxy", "Ant-Man", "Doctor Strange", "Black Panther",
        "Spider-Man: Homecoming", "Spider-Man: Far From Home", "Spider-Man: No Way Home",
        "Captain Marvel", "Eternals", "Shang-Chi", "Black Widow", "WandaVision",
        "Loki", "Wakanda", "Deadpool", "Fantastic Four",
    ],
    "Star Wars": ["Star Wars", "The Mandalorian", "Ahsoka", "Obi-Wan Kenobi", "Andor", "The Book of Boba Fett"],
    "DC": [
        "Batman", "Superman", "Wonder Woman", "Justice League", "Aquaman",
        "The Flash", "Joker", "Shazam", "The Suicide Squad", "Suicide Squad",
        "Black Adam", "Blue Beetle", "Man of Steel", "Green Lantern", "The Dark Knight",
    ],
    "Harry Potter": ["Harry Potter", "Fantastic Beasts"],
    "Lord of the Rings": ["Lord of the Rings", "The Hobbit"],
    "Pixar": [
        "Toy Story", "Finding Nemo", "Finding Dory", "The Incredibles", "Up",
        "WALL-E", "Cars", "Monsters, Inc.", "Brave", "Inside Out", "Coco",
        "Soul", "Luca", "Turning Red", "Elemental", "Ratatouille", "A Bug's Life",
    ],
    "Disney": [
        "The Lion King", "Frozen", "Moana", "Beauty and the Beast", "Aladdin",
        "The Little Mermaid", "Mulan", "Tangled", "Encanto", "Zootopia",
        "Wreck-It Ralph", "Big Hero 6", "Hercules", "Tarzan", "The Jungle Book",
        "Pinocchio", "Snow White", "Cinderella", "Sleeping Beauty", "The Princess and the Frog",
        "Raya", "Wish", "Hocus Pocus",
    ],
}


def load(name):
    with open(os.path.join(TOPICS_DIR, name), encoding="utf-8") as f:
        return json.load(f)


def save(name, data):
    with open(os.path.join(TOPICS_DIR, name), "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")


def match_franchises(name):
    lower = name.lower()
    out = []
    for franchise, needles in FRANCHISE_RULES.items():
        if any(n.lower() in lower for n in needles):
            out.append(franchise)
    return out


def main():
    changed = 0
    for file_name, category in (("films.json", "FILMS"), ("anime.json", "ANIME")):
        data = load(file_name)
        for topic in data:
            if topic.get("categoryId") != category:
                continue
            tags = topic.get("tags") or []
            hits = match_franchises(topic.get("name", ""))
            missing = [f for f in hits if f not in tags]
            if missing:
                topic["tags"] = tags + missing
                changed += 1
        save(file_name, data)
    print(f"tagged {changed} topics with franchise tags (idempotent)")


if __name__ == "__main__":
    main()
