#!/usr/bin/env python3
"""
Enrich the science lanes with MISSING tags.

The Topic Reveal bottom strip shows a topic's tags; topics without a
`tags` field render an empty strip. This fills the gap for the two lanes
that shipped with untagged topics:

  - scientists.json  → 370 untagged (of 501)
  - discoveries.json → 221 untagged (of 506)

Tags are DERIVED deterministically from data already in the topic, in the
SAME [Field, Origin, Era] style the tagged entries use:

  - Field      ← subtype for scientists (Physicist → Physics, …); keyword
                 scan of name + teaser for discoveries (antibiotic →
                 Antibiotics / Medicine, x-ray → Physics / Medicine, …).
  - Subfield   ← optional keyword (discoveries: antibiotics, electronics,
                 computing, nuclear, genetics, microscopy, anesthesia,
                 surgery …).
  - Origin     ← nationality adjective in the teaser (British, American,
                 German, Greek, …) or the discoverer byline; a curated
                 override table covers figures whose teaser names no
                 nationality.
  - Era        ← year/decade/century in the name or teaser (discoveries
                 are DATED events, so the name year wins). Century/CE/BCE
                 phrasing is parsed ("around 500 CE", "in 1020", "3rd
                 century BCE"); the override table pins eras for figures
                 with no year signal and corrects misleading year mentions
                 (e.g. a Babbage teaser that references a 1990s rebuild).

Idempotent: topics that already have a non-empty tags list are untouched.
Key order is preserved — `tags` is inserted right before `tier` (the
position the hand-curated entries use), and output is written with literal
UTF-8 + indent=2 matching the checked-in JSON formatting.
"""

import json
import os
import re

TOPICS_DIR = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets", "topics")


def load(name):
    with open(os.path.join(TOPICS_DIR, name), encoding="utf-8") as f:
        return json.load(f)


def save(name, data):
    with open(os.path.join(TOPICS_DIR, name), "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")


# ── Field from subtype (scientists) ────────────────────────────────────────
SUBTYPE_FIELD = {
    "Mathematician": "Mathematics",
    "Physicist": "Physics",
    "Chemist": "Chemistry",
    "Astronomer": "Astronomy",
    "Astrophysicist": "Astronomy",
    "Biologist": "Biology",
    "Botanist": "Biology",
    "Zoologist": "Biology",
    "Geneticist": "Genetics",
    "Biochemist": "Biochemistry",
    "Microbiologist": "Microbiology",
    "Bacteriologist": "Microbiology",
    "Virologist": "Medicine",
    "Biophysicist": "Biophysics",
    "Physician": "Medicine",
    "Surgeon": "Medicine",
    "Anatomist": "Medicine",
    "Psychologist": "Psychology",
    "Psychiatrist": "Psychology",
    "Anthropologist": "Anthropology",
    "Paleontologist": "Paleontology",
    "Paleoanthropologist": "Paleontology",
    "Primatologist": "Biology",
    "Engineer": "Engineering",
    "Computer Scientist": "Computer Science",
    "Cryptographer": "Computer Science",
    "Statistician": "Mathematics",
    "Geologist": "Geology",
    "Geophysicist": "Earth Science",
    "Seismologist": "Earth Science",
    "Geographer": "Geography",
    "Meteorologist": "Earth Science",
    "Oceanographer": "Earth Science",
    "Naturalist": "Biology",
    "Ecologist": "Ecology",
    "Earth Scientist": "Earth Science",
    "Philosopher": "Philosophy",
    "Inventor": "Engineering",
    "Scientist": "Science",
}

# ── Origin keyword → tag ────────────────────────────────────────────────────
# Ordered so compound nationalities ("Russian-American") match before their
# parts, and so specific adjectives ("Hindu" → Indian) match before generic
# ones that appear as substrings of unrelated teaser words.
ORIGIN_RULES = [
    # Compound nationalities FIRST so the more specific label wins.
    ("German-British", "German-British"),
    ("German-American", "German-American"),
    ("Russian-American", "Russian-American"),
    ("Russian-British", "Russian-British"),
    ("Dutch-Russian", "Dutch-Russian"),
    ("Lithuanian-Canadian", "Lithuanian-Canadian"),
    ("Hellenistic-Egyptian", "Hellenistic-Egyptian"),
    ("Egyptian-Greek", "Egyptian-Greek"),
    ("Roman-Egyptian", "Roman-Egyptian"),
    ("Scottish-British", "Scottish-British"),
    ("Welsh-British", "Welsh-British"),
    # Ancient / medieval labels before modern ones.
    ("Sumerian", "Sumerian"),
    ("Babylonian", "Babylonian"),
    ("Assyrian", "Assyrian"),
    ("Macedonian", "Macedonian"),
    ("Hellenistic", "Hellenistic"),
    ("Byzantine", "Byzantine"),
    ("Persian", "Persian"),
    ("Basran", "Arab"),
    ("Egyptian", "Egyptian"),
    ("Arab", "Arab"),
    ("Greek", "Greek"),
    ("Roman", "Roman"),
    # Modern nationalities.
    ("Hindu", "Indian"),
    ("Indian", "Indian"),
    ("Chinese", "Chinese"),
    ("Japanese", "Japanese"),
    ("Iranian", "Iranian"),
    ("Korean", "Korean"),
    ("English", "English"),
    ("British", "British"),
    ("Scottish", "Scottish"),
    ("Irish", "Irish"),
    ("Welsh", "Welsh"),
    ("American", "American"),
    ("Canadian", "Canadian"),
    ("Australian", "Australian"),
    ("New Zealander", "New Zealander"),
    ("Russian", "Russian"),
    ("Soviet", "Russian"),
    ("Ukrainian", "Ukrainian"),
    ("German", "German"),
    ("French", "French"),
    ("Italian", "Italian"),
    ("Spanish", "Spanish"),
    ("Portuguese", "Portuguese"),
    ("Swiss", "Swiss"),
    ("Swedish", "Swedish"),
    ("Norwegian", "Norwegian"),
    ("Danish", "Danish"),
    ("Dutch", "Dutch"),
    ("Belgian", "Belgian"),
    ("Austrian", "Austrian"),
    ("Hungarian", "Hungarian"),
    ("Polish", "Polish"),
    ("Czech", "Czech"),
    ("Serbian", "Serbian"),
    ("Kenyan", "Kenyan"),
    ("Transylvanian", "German"),
    ("Central Asian", "Central Asian"),
    ("Icelandic", "Icelandic"),
    ("Prussian", "German"),
]

# Curated (origin, era) for figures whose teaser names no nationality, has
# no usable year, or carries a misleading year mention. These are the only
# places the script hardcodes external knowledge.
ORIGIN_OVERRIDES = {
    # Origin pinned (no nationality in teaser).
    "Henry Cavendish": ("English", "18th Century"),
    "Caroline Herschel": ("German", "18th-19th Century"),
    "Henrietta Swan Leavitt": ("American", "20th Century"),
    "Clyde Tombaugh": ("American", "20th Century"),
    "Zhang Heng": ("Chinese", "2nd Century"),
    "Shen Kuo": ("Chinese", "11th Century"),
    "Milutin Milanković": ("Serbian", "20th Century"),
    "Edward Lorenz": ("American", "20th Century"),
    "Philo Farnsworth": ("American", "20th Century"),
    "Hedy Lamarr": ("Austrian", "20th Century"),
    "Hermann Oberth": ("German", "20th Century"),
    "Al-Khwarizmi": ("Persian", "Medieval"),
    "Aryabhata": ("Indian", "Ancient"),
    "Ernest Rutherford": ("New Zealander", "20th Century"),
    # Era pinned — nationality fallback was wrong or the teaser year is a
    # misleading reference (e.g. Babbage's engine rebuilt in the 1990s).
    "Ignaz Semmelweis": ("Hungarian", "19th Century"),
    "John Snow": ("English", "19th Century"),
    "James Prescott Joule": ("English", "19th Century"),
    "Edsger Dijkstra": ("Dutch", "20th Century"),
    "John Dalton": ("English", "19th Century"),
    "Charles Babbage": ("British", "19th Century"),
    "John Muir": ("Scottish", "19th Century"),
    "Alexander von Humboldt": ("German", "19th Century"),
    "Alfred Wegener": ("German", "20th Century"),
    "Henry Moseley": ("English", "20th Century"),
    "Max Born": ("German-British", "20th Century"),
    "Hans Christian Ørsted": ("Danish", "19th Century"),
    "Blaise Pascal": ("French", "17th Century"),
    "Henri Poincaré": ("French", "19th-20th Century"),
    "Claude Bernard": ("French", "19th Century"),
    "Williamina Fleming": ("Scottish", "19th-20th Century"),
    "Fred Hoyle": ("British", "20th Century"),
    "Aryabhata": ("Indian", "Ancient"),
    "Andre Geim": ("Dutch-Russian", "2000s"),
    "Konstantin Novoselov": ("Russian-British", "2000s"),
    "Robert Burns Woodward": ("American", "20th Century"),
    "Lars Onsager": ("Norwegian", "20th Century"),
    "Eugene Wigner": ("Hungarian", "20th Century"),
    "George de Hevesy": ("Hungarian", "20th Century"),
    "Lev Landau": ("Russian", "20th Century"),
    "Andrei Sakharov": ("Russian", "20th Century"),
    "Ivan Pavlov": ("Russian", "19th-20th Century"),
    "Konstantin Tsiolkovsky": ("Russian", "19th-20th Century"),
    "Nikolai Vavilov": ("Russian", "19th-20th Century"),
    "Rudolf Virchow": ("German", "19th Century"),
    "Emil von Behring": ("German", "19th-20th Century"),
    "Ernst Mach": ("Austrian", "19th-20th Century"),
    "Ludwig Boltzmann": ("Austrian", "19th-20th Century"),
    "Konrad Lorenz": ("Austrian", "20th Century"),
    "Karl von Frisch": ("Austrian", "20th Century"),
    "Ernst Chain": ("German-British", "20th Century"),
    "James Watt": ("Scottish", "18th Century"),
    "George Stephenson": ("English", "19th Century"),
    "Isambard Kingdom Brunel": ("British", "19th Century"),
    "Nikola Tesla": ("Serbian-American", "19th-20th Century"),
    "Alexander Graham Bell": ("Scottish", "19th Century"),
    "Samuel Morse": ("American", "19th Century"),
    "Thomas Edison": ("American", "19th-20th Century"),
    "Joseph Henry": ("American", "19th Century"),
    "Lee de Forest": ("American", "20th Century"),
    "Edwin Armstrong": ("American", "20th Century"),
    "Percy Spencer": ("American", "20th Century"),
    "John Logie Baird": ("Scottish", "20th Century"),
    "Dennis Gabor": ("Hungarian", "20th Century"),
    "Rudolf Diesel": ("German", "19th-20th Century"),
    "Guglielmo Marconi": ("Italian", "19th-20th Century"),
    "Walter Reed": ("American", "19th-20th Century"),
    "Ronald Ross": ("British", "19th-20th Century"),
    "Alexander Yersin": ("French", "19th-20th Century"),
    "Charles Nicolle": ("French", "20th Century"),
    "Selman Waksman": ("American", "20th Century"),
    "Howard Florey": ("Australian", "20th Century"),
    "Virginia Apgar": ("American", "20th Century"),
    "Charles Drew": ("American", "20th Century"),
    "Glenn Seaborg": ("American", "20th Century"),
    "Melvin Calvin": ("American", "20th Century"),
    "Erwin Chargaff": ("Austrian", "20th Century"),
    "Maurice Wilkins": ("British", "20th Century"),
    "Carl Woese": ("American", "20th Century"),
    "Sydney Brenner": ("South African", "20th Century"),
    "Max Delbrück": ("German-American", "20th Century"),
    "Abdus Salam": ("Pakistani", "20th Century"),
    "Steven Weinberg": ("American", "20th Century"),
    "Alan Guth": ("American", "20th Century"),
    "Paul Ehrlich": ("German", "19th-20th Century"),
    "James Hutton": ("Scottish", "18th Century"),
    "Charles Lyell": ("Scottish", "19th Century"),
    "Marie Tharp": ("American", "20th Century"),
    "Inge Lehmann": ("Danish", "20th Century"),
    "Harry Hess": ("American", "20th Century"),
    "John Tuzo Wilson": ("Canadian", "20th Century"),
    "Wilhelm Wundt": ("German", "19th Century"),
    "William James": ("American", "19th-20th Century"),
    "Elizabeth Loftus": ("American", "20th Century"),
    "Abraham Maslow": ("American", "20th Century"),
    "Konrad Zuse": ("German", "20th Century"),
    "John McCarthy": ("American", "20th Century"),
    "Barbara Liskov": ("American", "20th Century"),
    "George Washington Carver": ("American", "19th-20th Century"),
    "Luther Burbank": ("American", "19th-20th Century"),
    "Eugene Odum": ("American", "20th Century"),
    "Arthur Tansley": ("British", "20th Century"),
    "John Tyndall": ("Irish", "19th Century"),
    "Satyendra Nath Bose": ("Indian", "20th Century"),
    "C. V. Raman": ("Indian", "20th Century"),
    "Meghnad Saha": ("Indian", "20th Century"),
    "Emilio Segrè": ("Italian", "20th Century"),
    "Luis Alvarez": ("American", "20th Century"),
    "Ernest Lawrence": ("American", "20th Century"),
    "Leo Szilard": ("Hungarian", "20th Century"),
    "John Wheeler": ("American", "20th Century"),
    "David Bohm": ("American", "20th Century"),
    "Alice Ball": ("American", "20th Century"),
    "Esther Lederberg": ("American", "20th Century"),
    "Gladys West": ("American", "20th Century"),
    "Yvonne Brill": ("American", "20th Century"),
    "Ruby Payne-Scott": ("Australian", "20th Century"),
    "Katharine Burr Blodgett": ("American", "20th Century"),
    "Edith Clarke": ("American", "20th Century"),
    "François Jacob": ("French", "20th Century"),
    "Howard Temin": ("American", "20th Century"),
    "Craig Venter": ("American", "20th Century"),
    "Francis Collins": ("American", "20th Century"),
    "Paul Berg": ("American", "20th Century"),
    "Walter Gilbert": ("American", "20th Century"),
    "Walter Cannon": ("American", "20th Century"),
    "William Osler": ("Canadian", "19th-20th Century"),
    "James Chadwick": ("English", "20th Century"),
    "Norman Borlaug": ("American", "20th Century"),
    "Sylvia Earle": ("American", "20th Century"),
    "Ernst Mayr": ("German-American", "20th Century"),
    "George Gaylord Simpson": ("American", "20th Century"),
    "Annie Jump Cannon": ("American", "19th-20th Century"),
    "Robert Goddard": ("American", "20th Century"),
    "Ronald Fisher": ("British", "20th Century"),
    "J. B. S. Haldane": ("British", "20th Century"),
    "Theodosius Dobzhansky": ("Ukrainian", "20th Century"),
    "Ernst Haeckel": ("German", "19th Century"),
    "Niels Henrik Abel": ("Norwegian", "19th Century"),
    "Georg Cantor": ("German", "19th-20th Century"),
    "David Hilbert": ("German", "19th-20th Century"),
    "Maryam Mirzakhani": ("Iranian", "20th Century"),
    "Birutė Galdikas": ("Lithuanian-Canadian", "20th Century"),
    "Wangari Maathai": ("Kenyan", "20th Century"),
    "Charles Elton": ("British", "20th Century"),
    "Aldo Leopold": ("American", "20th Century"),
    "John Atanasoff": ("American", "20th Century"),
    "J. Presper Eckert": ("American", "20th Century"),
    "John Mauchly": ("American", "20th Century"),
    "Seymour Cray": ("American", "20th Century"),
    "Ken Thompson": ("American", "20th Century"),
    "Niklaus Wirth": ("Swiss", "20th Century"),
    "Louis Leakey": ("British", "20th Century"),
    "Donald Johanson": ("American", "20th Century"),
    "Robert Broom": ("Scottish", "20th Century"),
    "Raymond Dart": ("Australian", "20th Century"),
    "Edwin Copeland": ("American", "20th Century"),
    "Robert Whittaker": ("American", "20th Century"),
    "James Lovelock": ("British", "20th Century"),
    "Matthias Schleiden": ("German", "19th Century"),
    "Robert Brown": ("Scottish", "19th Century"),
    "Eugène Dubois": ("Dutch", "19th-20th Century"),
    "Alessandro Volta": ("Italian", "18th-19th Century"),
    "André-Marie Ampère": ("French", "18th-19th Century"),
    "Georg Ohm": ("German", "19th Century"),
    "Amedeo Avogadro": ("Italian", "19th Century"),
    "Jöns Jacob Berzelius": ("Swedish", "19th Century"),
    "Justus von Liebig": ("German", "19th Century"),
    "August Kekulé": ("German", "19th Century"),
    "Svante Arrhenius": ("Swedish", "19th-20th Century"),
    "Frederick Soddy": ("British", "20th Century"),
    "Henri Becquerel": ("French", "19th-20th Century"),
    "J. J. Thomson": ("British", "19th-20th Century"),
    "Tycho Brahe": ("Danish", "16th-17th Century"),
    "William Herschel": ("German", "18th Century"),
    "Josiah Willard Gibbs": ("American", "19th Century"),
    "Rudolf Clausius": ("German", "19th Century"),
    "Sadi Carnot": ("French", "19th Century"),
    "Julius Robert von Mayer": ("German", "19th Century"),
    "Hermann von Helmholtz": ("German", "19th Century"),
    "Jacobus van 't Hoff": ("Dutch", "19th-20th Century"),
    "Henri Le Chatelier": ("French", "19th-20th Century"),
    "Fritz Haber": ("German", "19th-20th Century"),
    "Carl Bosch": ("German", "20th Century"),
    "Wilhelm Ostwald": ("German", "19th-20th Century"),
    "Adolf von Baeyer": ("German", "19th-20th Century"),
    "Emil Fischer": ("German", "19th-20th Century"),
    "Hermann Staudinger": ("German", "20th Century"),
    "Wallace Carothers": ("American", "20th Century"),
    "Stephanie Kwolek": ("American", "20th Century"),
    "Charles Goodyear": ("American", "19th Century"),
    "Alfred Nobel": ("Swedish", "19th Century"),
    "Elias James Corey": ("American", "20th Century"),
    "Carl Djerassi": ("Austrian", "20th Century"),
    "Percy Julian": ("American", "20th Century"),
    "K. Barry Sharpless": ("American", "20th Century"),
    "Arnold Sommerfeld": ("German", "19th-20th Century"),
    "Felix Bloch": ("Swiss", "20th Century"),
    "Norman Ramsey": ("American", "20th Century"),
    "Theodore Maiman": ("American", "20th Century"),
    "Hans Bethe": ("German-American", "20th Century"),
    "George Gamow": ("Russian-American", "20th Century"),
    "Ralph Alpher": ("American", "20th Century"),
    "Arthur Schawlow": ("American", "20th Century"),
    "Charles Townes": ("American", "20th Century"),
    "Harry Kroto": ("British", "20th Century"),
    "Lars Onsager": ("Norwegian", "20th Century"),
    "Peter Debye": ("Dutch", "20th Century"),
    "George Uhlenbeck": ("Dutch", "20th Century"),
    "James Rainwater": ("American", "20th Century"),
    "Frank Wilczek": ("American", "20th Century"),
    "David Gross": ("American", "20th Century"),
    "H. David Politzer": ("American", "20th Century"),
    "Robert Boyle": ("Irish", "17th Century"),
    "William Harvey": ("English", "17th Century"),
    "Andreas Vesalius": ("Belgian", "16th Century"),
    "Christiaan Huygens": ("Dutch", "17th Century"),
    "Edmond Halley": ("English", "17th-18th Century"),
    "Hero of Alexandria": ("Greek", "1st Century"),
    "Eratosthenes": ("Greek", "3rd Century BCE"),
    "Democritus": ("Greek", "Ancient"),
    "Pythagoras": ("Greek", "Ancient"),
    "Avicenna (Ibn Sina)": ("Persian", "Medieval"),
    "Al-Biruni": ("Persian", "Medieval"),
    "Ibn al-Haytham (Alhazen)": ("Arab", "Medieval"),
    "Ibn al-Haytham": ("Arab", "Medieval"),
    "Lars Onsager": ("Norwegian", "20th Century"),
}


# ── Era hints ──────────────────────────────────────────────────────────────
_CENTURY_RE = re.compile(r"(\d{1,2})(?:st|nd|rd|th)\s+century(?:\s*(bce|ce|bc|ad))?", re.I)
_DECADE_RE = re.compile(r"\b((?:1[5-9]|20)\d0)s\b")
_YEAR_RE = re.compile(r"\b(1[0-9]{3}|20[0-9]{2})\b")
# "around 500 CE", "in 1020", "in 2004" — year + era suffix or a bare year.
_CE_YEAR_RE = re.compile(
    r"\b(?:around|circa|about|c\.?|in|by)\s+(1[0-9]{2,3}|[2-9][0-9]{0,3})\s*(bce|ce|bc|ad)\b", re.I
)


def ordinal(n):
    n = int(n)
    if 10 <= n % 100 <= 20:
        suffix = "th"
    else:
        suffix = {1: "st", 2: "nd", 3: "rd"}.get(n % 10, "th")
    return f"{n}{suffix}"


def century_tag(year, bce=False):
    c = -(-int(year) // 100)  # ceiling
    return f"{ordinal(c)} Century" + (" BCE" if bce else "")


def era_from_year(year):
    """Decades for 1900+, plain centuries before that — matches the tagged style."""
    year = int(year)
    if year >= 1900:
        return f"{year // 10}0s"
    return century_tag(year)


def parse_era(text):
    """Returns an era tag from century/decade/year mentions in text, or None."""
    if not text:
        return None
    m = _CENTURY_RE.search(text)
    if m:
        num = int(m.group(1))
        suf = (m.group(2) or "").upper()
        bce = suf in ("BCE", "BC")
        return f"{ordinal(num)} Century" + (" BCE" if bce else "")
    m = _DECADE_RE.search(text)
    if m:
        return f"{m.group(1)}s"
    # Explicit "around/in/by YEAR CE|BCE|AD" forms.
    m = _CE_YEAR_RE.search(text)
    if m:
        y = int(m.group(1))
        suf = (m.group(2) or "").upper()
        if suf in ("BCE", "BC"):
            return century_tag(y, bce=True)
        return era_from_year(y)
    for mm in _YEAR_RE.finditer(text):
        y = int(mm.group())
        if 900 <= y <= 2100:
            return era_from_year(y)
    return None


# ── Field/subfield keywords for discoveries ────────────────────────────────
# Matched with WORD BOUNDARIES so "gene" never matches inside "iodine" and
# "moon" never matches inside "bromine". Two layers:
#
#   NAME rules   — checked against the discovery NAME first. These cover
#                  objects/events whose teaser text is full of unrelated
#                  keywords (Pluto's teaser mentions microscopes; the
#                  Sodium–Potassium pump's mentions sodium atoms).
#   TEASER rules — high-confidence *subject* keywords only, scanned when
#                  the name gave no answer (antibiotic, vaccine, x-ray,
#                  DNA, hormone, …).
#
# Simple elements are matched only inside the NAME (and only for
# "X Isolated/Discovered" events), so "Hydrogen Isolated (1766)" →
# Chemistry while "The 21-cm Hydrogen Line" stays Astronomy.
def _rx(*words):
    """Compile a word-boundary pattern covering several spellings."""
    return re.compile(r"\b(?:%s)\b" % "|".join(re.escape(w) for w in words), re.I)


NAME_FIELD_RULES = [
    (_rx("penicillin"), "Antibiotics", "Medicine"),
    (_rx("antibiotic", "antibiotics"), "Antibiotics", "Medicine"),
    (_rx("vaccine", "vaccines", "vaccination"), "Vaccines", "Medicine"),
    (_rx("x-ray", "x-rays"), "Radiology", "Physics"),
    (_rx("mri", "magnetic resonance"), "Radiology", "Medicine"),
    (_rx("dna", "rna"), "Genetics", "Biology"),
    (_rx("genome", "genomes"), "Genetics", "Biology"),
    (_rx("gene", "genes", "genetic", "genetics"), "Genetics", "Biology"),
    (_rx("chromosome", "chromosomes"), "Genetics", "Biology"),
    (_rx("enzyme", "enzymes"), "Biochemistry", "Biology"),
    (_rx("vitamin", "vitamins"), "Nutrition", "Medicine"),
    (_rx("hormone", "hormones"), "Endocrinology", "Medicine"),
    (_rx("insulin"), "Endocrinology", "Medicine"),
    (_rx("anesthesia", "anaesthesia", "anesthetic"), "Anesthesia", "Medicine"),
    (_rx("surgery", "surgical", "transplant", "bypass", "catheterization"), "Surgery", "Medicine"),
    (_rx("blood", "transfusion"), "Hematology", "Medicine"),
    (_rx("neuron", "neurotransmitter"), "Neuroscience", "Medicine"),
    (_rx("virus", "viruses", "viral"), "Virology", "Medicine"),
    (_rx("cell", "cells"), "Cell Biology", "Biology"),
    (_rx("pump"), "Cell Biology", "Biology"),
    (_rx("cycle"), "Biochemistry", "Biology"),
    (_rx("bacteria", "bacterial"), "Microbiology", "Biology"),
    (_rx("microscope", "microscopy"), "Microscopy", "Biology"),
    (_rx("fossil", "fossils", "skull", "skulls", "skeleton", "hominin", "dinosaur"), "Paleontology", "Biology"),
    (_rx("planet", "planets", "exoplanet", "moon", "moons", "venus", "mars", "jupiter",
        "saturn", "neptune", "uranus", "mercury", "pluto", "charon", "ceres", "titan",
        "quasar", "nebula", "nebulae", "comet", "asteroid", "supernova", "galax",
        "black hole", "telescope", "telescopes", "cosmic", "cosmolog"), "Astronomy", "Astronomy"),
    (_rx("hydrogen line"), "Astronomy", "Astronomy"),
    (_rx("quark", "quarks", "gluon", "neutrino", "neutrinos", "positron", "muon", "lepton",
        "leptons"), "Particle Physics", "Physics"),
    (_rx("relativity"), "Relativity", "Physics"),
    (_rx("quantum"), "Quantum", "Physics"),
    (_rx("radioactivity", "radioactive"), "Nuclear", "Physics"),
    (_rx("nuclear"), "Nuclear", "Physics"),
    (_rx("gravity", "gravitational"), "Physics", "Physics"),
    (_rx("electromagnet", "magnetism", "magnetic", "electricity", "electric"), "Electromagnetism", "Physics"),
    (_rx("transistor"), "Electronics", "Electronics"),
    (_rx("semiconductor", "integrated circuit", "vacuum tube"), "Electronics", "Electronics"),
    (_rx("computer", "computing", "program", "software", "algorithm", "cryptograph", "cipher",
        "internet"), "Computing", "Computing"),
    (_rx("laser", "optic", "optics", "optical", "fiber", "fibre"), "Optics", "Physics"),
    (_rx("telephone", "radio", "television", "tv"), "Communications", "Engineering"),
    (_rx("phonograph", "recording", "sound"), "Sound", "Engineering"),
    (_rx("photograph", "photography"), "Photography", "Engineering"),
    (_rx("automobile", "airplane", "aircraft", "flight", "aviation"), "Transport", "Engineering"),
    (_rx("rocket", "rockets", "satellite", "satellites", "space"), "Space", "Engineering"),
    (_rx("plastics", "plastic", "polymer", "alloy", "steel"), "Materials", "Chemistry"),
    (_rx("element", "elements", "periodic", "chemical", "chemistry", "molecule", "molecular",
        "atomic", "isotope", "isotopes"), "Chemistry", "Chemistry"),
    (_rx("equation", "number theory", "prime", "primes", "geometry", "calculus", "statistic",
        "probability"), "Mathematics", "Mathematics"),
    (_rx("climate", "weather", "atmosphere", "atmospheric", "geology", "geolog", "earthquake",
        "seism", "volcano", "volcanic", "continent", "continental", "ocean", "ozone"),
        "Earth Science", "Earth Science"),
    (_rx("archaeolog"), "Archaeology", "Archaeology"),
    (_rx("anthropolog"), "Anthropology", "Anthropology"),
    (_rx("psycholog"), "Psychology", "Psychology"),
]

TEASER_FIELD_RULES = [
    (_rx("penicillin"), "Antibiotics", "Medicine"),
    (_rx("antibiotic", "antibiotics"), "Antibiotics", "Medicine"),
    (_rx("vaccine", "vaccines", "vaccination"), "Vaccines", "Medicine"),
    (_rx("x-ray", "x-rays"), "Radiology", "Physics"),
    (_rx("dna", "rna", "genome", "chromosome", "chromosomes", "genetic", "genetics", "gene"),
        "Genetics", "Biology"),
    (_rx("enzyme", "enzymes"), "Biochemistry", "Biology"),
    (_rx("vitamin", "vitamins"), "Nutrition", "Medicine"),
    (_rx("hormone", "hormones", "insulin"), "Endocrinology", "Medicine"),
    (_rx("anesthesia", "anaesthesia", "anesthetic"), "Anesthesia", "Medicine"),
    (_rx("surgery", "surgical", "transplant", "bypass", "catheterization"), "Surgery", "Medicine"),
    (_rx("blood", "transfusion"), "Hematology", "Medicine"),
    (_rx("virus", "viruses", "viral"), "Virology", "Medicine"),
    (_rx("bacteria", "bacterial"), "Microbiology", "Biology"),
    (_rx("microscope", "microscopy"), "Microscopy", "Biology"),
    (_rx("fossil", "fossils", "skull", "skeleton", "hominin", "dinosaur"), "Paleontology", "Biology"),
    (_rx("planet", "planets", "moon", "moons", "quasar", "comet", "asteroid", "supernova",
        "galax", "black hole", "telescope", "cosmic", "cosmolog", "exoplanet"), "Astronomy", "Astronomy"),
    (_rx("relativity"), "Relativity", "Physics"),
    (_rx("quantum"), "Quantum", "Physics"),
    (_rx("radioactivity", "radioactive", "nuclear"), "Nuclear", "Physics"),
    (_rx("quark", "gluon", "neutrino", "positron", "muon", "lepton"), "Particle Physics", "Physics"),
    (_rx("electromagnet", "magnetism", "magnetic", "electricity", "electric"), "Electromagnetism", "Physics"),
    (_rx("transistor", "semiconductor", "integrated circuit"), "Electronics", "Electronics"),
    (_rx("computer", "computing", "software", "algorithm", "cryptograph", "cipher", "internet"),
        "Computing", "Computing"),
    (_rx("laser", "optic", "optical", "fiber", "fibre"), "Optics", "Physics"),
    (_rx("telephone", "radio", "television"), "Communications", "Engineering"),
    (_rx("photograph", "photography"), "Photography", "Engineering"),
    (_rx("rocket", "satellite", "space"), "Space", "Engineering"),
    (_rx("plastics", "plastic", "polymer", "alloy", "steel"), "Materials", "Chemistry"),
    (_rx("chemical", "chemistry", "molecule", "molecular", "atomic", "isotope"), "Chemistry", "Chemistry"),
    (_rx("equation", "geometry", "calculus", "statistic", "probability", "prime"), "Mathematics", "Mathematics"),
    (_rx("climate", "weather", "atmosphere", "geology", "geolog", "earthquake", "seism",
        "volcano", "continent", "ocean", "ozone"), "Earth Science", "Earth Science"),
    (_rx("archaeolog"), "Archaeology", "Archaeology"),
    (_rx("anthropolog"), "Anthropology", "Anthropology"),
    (_rx("psycholog"), "Psychology", "Psychology"),
]

# Elements map to Chemistry when the discovery NAME is an isolation/discovery
# event ("Hydrogen Isolated (1766)"); incidental element mentions in teasers
# ("the sodium–potassium pump", "the 21-cm hydrogen line") must NOT trigger it.
_ELEMENTS = re.compile(
    r"\b(hydrogen|helium|chlorine|nitrogen|iodine|bromine|lithium|selenium|cadmium|aluminium|"
    r"aluminum|silicon|beryllium|titanium|chromium|manganese|molybdenum|tungsten|uranium|"
    r"zirconium|vanadium|tantalum|niobium|palladium|rhodium|osmium|iridium|sodium|potassium|"
    r"fluorine|argon|neon|krypton|xenon|technetium|francium|plutonium|polonium|radium|radon|"
    r"oxygen|carbon|iron|gold|silver|copper|zinc|lead|tin|mercury|magnesium|calcium|barium|"
    r"strontium|rubidium|cesium|caesium|thallium|bismuth|antimony|tellurium|ruthenium|"
    r"hafnium|rhenium|osmium|iridium)\b",
    re.I,
)
_ISOLATION_RE = re.compile(r"\b(?:isolated|discovered|identified|recognized|found)\b", re.I)


def field_for_discovery(topic):
    name = topic.get("name", "")
    teaser = topic.get("teaser", "") or ""
    # 1. Element-isolation events (chemistry) — NAME only.
    if _ELEMENTS.search(name) and _ISOLATION_RE.search(name):
        return "Chemistry", None
    # 2. Name carries a clear subject — the teaser is too noisy to trust.
    for rx, sub, field in NAME_FIELD_RULES:
        if rx.search(name):
            return field, sub
    # 3. High-confidence subject keywords anywhere in name + teaser.
    for rx, sub, field in TEASER_FIELD_RULES:
        if rx.search(name + " " + teaser):
            return field, sub
    return "Science", None


def derive_tags(topic):
    """Deterministic [Field, (Subfield), Origin, Era] tags for one topic."""
    name = topic.get("name", "")
    teaser = topic.get("teaser", "") or ""
    subtype = topic.get("subtype", "")
    byline = topic.get("byline", "") or ""
    hay = name + " " + teaser + " " + byline

    tags = []

    # Field
    if topic.get("categoryId") == "SCIENTISTS":
        field = SUBTYPE_FIELD.get(subtype, "Science")
    else:
        field, sub = field_for_discovery(topic)
        if sub and sub not in tags:
            tags.append(sub)
    if field and field not in tags:
        tags.append(field)

    # Origin — curated override first, then byline/teaser adjective scan.
    origin = None
    ov = ORIGIN_OVERRIDES.get(name)
    if ov:
        origin = ov[0]
    if not origin:
        for kw, tag in ORIGIN_RULES:
            if re.search(r"\b" + re.escape(kw).replace(r"\ ", r"[ -]") + r"\b", hay, re.I):
                origin = tag
                break
    if origin and origin not in tags:
        tags.append(origin)

    # Era — curated override wins; otherwise year/century in name/teaser.
    era = None
    if ov and ov[1]:
        era = ov[1]
    if not era:
        for text in ([name] if topic.get("categoryId") == "DISCOVERIES" else []) + [name, teaser, byline]:
            if text:
                era = parse_era(text) or era
                if era:
                    break
    if era and era not in tags:
        tags.append(era)

    return tags[:4]


def insert_tags_before_tier(topic, tags):
    rebuilt = {}
    for k, v in topic.items():
        if k == "tier" and "tags" not in rebuilt:
            rebuilt["tags"] = tags
        rebuilt[k] = v
    if "tags" not in rebuilt:
        rebuilt["tags"] = tags
    topic.clear()
    topic.update(rebuilt)


def main():
    added = 0
    skipped = 0
    for name in ("scientists.json", "discoveries.json"):
        data = load(name)
        changed = 0
        for topic in data:
            if topic.get("tags"):
                skipped += 1
                continue
            tags = derive_tags(topic)
            insert_tags_before_tier(topic, tags)
            changed += 1
            added += 1
        if changed:
            save(name, data)
            print(f"{name}: tagged {changed} previously-tagged-absent topics")
        else:
            print(f"{name}: no change")
    print(f"TOTAL added={added} (untouched={skipped})")


if __name__ == "__main__":
    main()
