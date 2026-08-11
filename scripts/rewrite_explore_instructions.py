#!/usr/bin/env python3
"""
Rewrite explore instructions so they are about EXPLORING, not MAKING.

User direction:
- Food: read/watch about the dish's history and where it's from (origin linked).
- Games: a fun fact + read about it + watch it on YouTube; "if it looks fun,
  you can try a similar game."
- Scope: ALL categories — every instruction that told the user to
  cook/play/make/build/write/fold/craft something becomes read/watch/learn.

The transform preserves the hand-crafted "genius" clause (the quirky fact)
and the "notice X" observation where one exists, reframing the leading
action from "make it / play it" to "read / watch it".

Run: python3 scripts/rewrite_explore_instructions.py
"""
import json
import os
import re

ROOT = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets", "topics")

FOOD_GENIUS = re.compile(r" The (?:dish|pastry|bread|recipe|food)'s genius is that (.*?)\.\s*$")
GAME_GENIUS = re.compile(r" The genius is that (.*?)\.\s*$")


def strip_year(name: str) -> str:
    """'Pizza Margherita (1889)' -> 'Pizza Margherita'."""
    return re.sub(r"\s*\(\d{4}\)\s*$", "", name)


def rewrite_food(t: dict) -> None:
    a = t["exploreAction"]
    if a["verb"] != "Cook":
        return  # idempotent: only transform untouched topics
    name = strip_year(t["name"])
    byline = t.get("byline", "").strip()
    inst = a["instruction"]
    m = FOOD_GENIUS.search(inst)
    genius = m.group(1).strip() if m else ""
    head = inst[: m.start()] if m else inst
    origin = f" ({byline})" if byline else ""
    if "notice " in head:
        obs = head.split("notice ", 1)[1].strip().rstrip(".")
        new = (
            f"Read how {name} got its name and where it's from — "
            f"{genius}{origin}. Then watch how it's made and notice {obs}."
        )
    else:
        obs = head.split(" — ", 1)[1].strip().rstrip(".") if " — " in head else head.strip().rstrip(".")
        new = (
            f"Read how {name} got its name and where it's from — "
            f"{genius}{origin}. Then watch how it's made — {obs}."
        )
    a["verb"] = "Read"
    a["instruction"] = new


def rewrite_game(t: dict) -> None:
    a = t["exploreAction"]
    if a["verb"] != "Play":
        return  # idempotent: only transform untouched topics
    name = strip_year(t["name"])
    inst = a["instruction"]
    m = GAME_GENIUS.search(inst)
    genius = m.group(1).strip() if m else ""
    head = inst[: m.start()] if m else inst
    for marker in ("notice ", "learn "):
        if marker in head:
            rest = head.split(marker, 1)[1].strip().rstrip(".")
            new = (
                f"Fun fact: {genius}. Read about {name}, then watch it on "
                f"YouTube and {marker.strip()} {rest}. If it looks fun, "
                f"you can try a similar game."
            )
            a["verb"] = "Watch"
            a["instruction"] = new
            return
    obs = head.split(" — ", 1)[1].strip().rstrip(".") if " — " in head else head.strip().rstrip(".")
    new = (
        f"Fun fact: {genius}. Read about {name}, then watch it on YouTube — "
        f"{obs}. If it looks fun, you can try a similar game."
    )
    a["verb"] = "Watch"
    a["instruction"] = new


# Hand-written rewrites for the strays: {file: {name: (verb, instruction)}}
STRAYS = {
    "painters.json": {
        "Yoko Ono": (
            "Read",
            "Read Ono's instruction pieces — 'Imagine the clouds dripping', "
            "'Listen to the sound of the Earth turning'. Her instructions ARE "
            "the artwork; the canvas is the audience's act.",
        ),
    },
    "scientists.json": {
        "Ronald Fisher": (
            "Read",
            "Read about Fisher's principles of randomization and replication "
            "from his famous tea-tasting experiment. Write why controlled "
            "experiments need randomization and how his methods underpin "
            "modern clinical trials.",
        ),
        "Ernst Haeckel": (
            "Look at",
            "Look through Haeckel's Art Forms in Nature illustrations and pick "
            "one plate, like a radiolarian or medusa. Write what the creature "
            "is and why his scientific art helped spread evolutionary ideas.",
        ),
        "Karl Schwarzschild": (
            "Read",
            "Read how Schwarzschild derived the first exact solution to "
            "Einstein's equations and its event horizon. Write what the "
            "Schwarzschild radius is and how his wartime paper predicted "
            "black holes.",
        ),
        "Maryam Mirzakhani": (
            "Read",
            "Learn what hyperbolic geometry is and how Mirzakhani counted "
            "the ways to slice a surface. Write why her work on 'the moduli "
            "space of Riemann surfaces' earned the Fields Medal in 2014.",
        ),
        "Samuel Morse": (
            "Read",
            "Learn Morse code and why frequent letters like E and T got "
            "shorter codes. Try decoding your own name in Morse and explain "
            "how the telegraph relay extended signals across continents.",
        ),
    },
    "wildcard.json": {
        "Mole Negro": (
            "Read",
            "Read about mole negro — how Oaxaca layers smoky, herbal, and "
            "citrus-sweet flavors, and why the green and yellow moles are "
            "served alongside it.",
        ),
        "Capoeira": (
            "Watch",
            "Watch how the roda works — it's a music event first, not a "
            "fight. Notice the ginga stance and how players move "
            "partner-to-partner.",
        ),
        "Parkour / Freerunning": (
            "Watch",
            "Watch how traceurs train — the 'precision' is a standing jump "
            "from one ledge to another of the same height. Notice the habit "
            "of measuring before you jump.",
        ),
        "Origami": (
            "Watch",
            "Read the story of origami, then watch a crane tutorial — the 14 "
            "steps map to 14 creases. Notice the paper choice: washi folds, "
            "printer paper tears.",
        ),
        "Senbazuru (1000 Cranes)": (
            "Read",
            "Read about senbazuru — the 1000-crane tradition and Sadako's "
            "story — and what the discipline of folding by hand means to "
            "those who complete it.",
        ),
        "Japanese Tea Ceremony": (
            "Watch",
            "Watch a tea ceremony and read about the ritual — the hostess "
            "turns the bowl 90 degrees from its front design for the guest, "
            "and it's drunk in two sips.",
        ),
        "Ashtanga Yoga": (
            "Read",
            "Read about Ashtanga's primary series — how the teacher gauges "
            "a first-timer, and why the breathing is the practice while the "
            "postures are the consequence.",
        ),
        "Vipassana 10-day Retreat": (
            "Read",
            "Read how a 10-day Vipassana retreat unfolds — days 1-3 are "
            "silence and breath, day 4 introduces body-scanning, days 5-9 "
            "are full scans. It's one of the most demanding introspective "
            "courses in the world.",
        ),
        "Icelandic Þorrablót": (
            "Read",
            "Read about Þorrablót — Iceland's midwinter feast of preserved "
            "foods — and what svið (singed sheep head) and slátur (blood "
            "sausage) say about survival cooking.",
        ),
        "Ultramarathon": (
            "Read",
            "Read how ultramarathoners prepare — heart-rate training, "
            "calorie intake strategy, hydration planning. The 50K is the "
            "standard entry length.",
        ),
        "Cosplay": (
            "Read",
            "Learn how cosplayers build costumes — sewing, foam armor, "
            "prosthetics, and wigs — and how conventions celebrate the "
            "craft. Write what draws people to transform into characters "
            "and how the community works.",
        ),
        "How Paper Changed the World": (
            "Read",
            "Learn how paper was made from plant fibers and how the "
            "technology spread from China through Samarkand and Spain. Write "
            "why paper was cheaper than parchment and how it enabled mass "
            "literacy and print.",
        ),
        "The History of Chess": (
            "Read",
            "Learn how chess evolved from chaturanga, how the pieces changed "
            "(the queen's rise, the bishop's reach), and how the rules were "
            "standardized. Write what the game's spread tells us about "
            "cultural exchange.",
        ),
        "Paella": (
            "Read",
            "Learn the traditional Valencian paella method — toast the rice, "
            "add broth, and let it cook without stirring until a crust "
            "forms. Write what socarrat is, why the pan must be wide, and "
            "how seafood paella differs from the original.",
        ),
        "Día de los Muertos Altars": (
            "Read",
            "Learn the elements of a traditional ofrenda — the photo, "
            "marigolds, candles, water, food — and what each layer and "
            "object means. Write why the holiday blends Indigenous and "
            "Catholic traditions and how it celebrates memory.",
        ),
        "The IKEA Effect": (
            "Read",
            "Learn how researchers showed that people overvalue "
            "self-assembled products. Write why effort creates attachment, "
            "how it shapes everything from DIY to customization, and when "
            "the effect backfires.",
        ),
        "The Veil of Ignorance": (
            "Read",
            "Learn how Rawls argued that fairness comes from ignorance of "
            "your own position. Write the thought experiment, its two "
            "principles of justice, and how it is used to evaluate policies "
            "today.",
        ),
        "Great Zimbabwe": (
            "Read",
            "Learn how the Shona people stacked granite blocks to build "
            "Great Zimbabwe and how the city grew wealthy on gold and ivory "
            "trade. Write why early Europeans doubted its origin and how the "
            "site shaped Zimbabwe's identity.",
        ),
        "The Art of the Handwritten Letter": (
            "Read",
            "Read why a handwritten letter does what a text cannot — "
            "slowness, care, permanence. Notice how composing by hand "
            "changes the experience of writing.",
        ),
        "One Thousand Paper Cranes": (
            "Watch",
            "Learn the story of Sadako Sasaki and the Children's Peace "
            "Monument — why the crane became a symbol of peace. Watch a "
            "folding video to see the craft.",
        ),
        "Kanzashi": (
            "Read",
            "Learn how tsumami kanzashi are folded from silk and how the "
            "flowers symbolize seasons. Write what each flower means and "
            "how they are worn with kimono.",
        ),
    },
    "internet.json": {
        "Sus (2018)": (
            "Watch",
            "Read how Among Us turned 'sus' into a global word, then watch a "
            "round — notice the accusation pattern that makes it funny.",
        ),
    },
}


def main() -> None:
    changes = 0
    # food + games: scripted transforms
    food = json.load(open(os.path.join(ROOT, "food.json")))
    for t in food:
        rewrite_food(t)
        changes += 1
    games = json.load(open(os.path.join(ROOT, "games.json")))
    for t in games:
        rewrite_game(t)
        changes += 1
    json.dump(food, open(os.path.join(ROOT, "food.json"), "w"), ensure_ascii=False, indent=2)
    json.dump(games, open(os.path.join(ROOT, "games.json"), "w"), ensure_ascii=False, indent=2)

    # strays
    for fname, mapping in STRAYS.items():
        path = os.path.join(ROOT, fname)
        data = json.load(open(path))
        for t in data:
            if t["name"] in mapping:
                verb, inst = mapping[t["name"]]
                t["exploreAction"]["verb"] = verb
                t["exploreAction"]["instruction"] = inst
                changes += 1
        json.dump(data, open(path, "w"), ensure_ascii=False, indent=2)

    print(f"rewrote {changes} explore instructions")
    # sanity: no make-verbs left in the verb field
    bad = ("cook", "play", "make", "try", "build", "write", "craft", "fold", "dot", "design", "draw", "solve")
    leftovers = []
    for fname in sorted(os.listdir(ROOT)):
        if not fname.endswith(".json"):
            continue
        for t in json.load(open(os.path.join(ROOT, fname))):
            v = (t.get("exploreAction", {}).get("verb") or "").lower()
            if v in bad:
                leftovers.append(f"{fname}: {t['name']} -> {v}")
    if leftovers:
        print("REMAINING MAKE-VERBS:")
        for l in leftovers:
            print("  ", l)
    else:
        print("no make-verbs remain in any verb field")


if __name__ == "__main__":
    main()
