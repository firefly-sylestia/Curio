#!/usr/bin/env python3
"""gen_topics_to200.py — climb every category JSON up to TARGET topics,
lowest-count categories first, using REAL curated topics (schema-compliant).

Batch runner: pass category keys as args, e.g.
    python3 tools/gen_topics_to200.py mythology series anime

Data lives in DATA[key] below: one pipe-delimited line per topic —
    name|year|byline|fact|Tag,Tag,Tag|minutes[|episodes]
Mythology omits year. Dedupes against every topic file (id + normalized name).
"""
import json, glob, os, re, sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
TOPICS = os.path.join(ROOT, "app/src/main/assets/topics")
TARGET = 200

def slug(s):
    s = s.lower()
    s = re.sub(r"[^a-z0-9]+", "-", s).strip("-")
    return s[:70]

def load_all_names():
    """normalized-name set across EVERY topic file (dedupe requirement)."""
    names = set()
    for f in glob.glob(os.path.join(TOPICS, "*.json")):
        try:
            for t in json.load(open(f)):
                names.add(re.sub(r"\s*\(\d{4}\)\s*$", "", t["name"]).strip().lower())
        except Exception:
            pass
    return names

def make_topic(cat_id, prefix, subtype, verb, line, taken_names):
    parts = [p.strip() for p in line.split("|")]
    if len(parts) < 6:
        raise ValueError(f"bad line (need ≥6 fields): {line[:60]}")
    name, year, byline, fact, tagstr = parts[0], parts[1], parts[2], parts[3], parts[4]
    minutes = int(parts[5])
    episodes = int(parts[6]) if len(parts) > 6 and parts[6] else None

    display = f"{name} ({year})" if year else name
    key = name.lower()
    if key in taken_names:
        return None
    taken_names.add(key)

    tid = f"{prefix}-{slug(name)}"
    fact = fact.strip()
    if not fact.endswith((".", "!", "?")):
        fact += "."
    teaser = fact[0].upper() + fact[1:]
    if cat_id == "MYTHOLOGY":
        instruction = (
            f"Read one complete retelling of {name} in a single sitting, then try "
            f"explaining its strangest scene to someone else in under a minute — myths "
            f"this durable survive because they still work on people who know nothing "
            f"about the culture that told them."
        )
        target = name
    elif cat_id == "ANIME":
        eps = episodes or 12
        instruction = (
            f"Watch the first three episodes of {display} back to back — pay attention "
            f"to how the opening frames the show's central promise, then check whether "
            f"episode three starts paying it off or quietly renegotiating it."
        )
        target = f"{display} episodes 1–3"
        minutes = min(minutes, 72)
    else:  # SERIES
        instruction = (
            f"Watch the pilot and one late-season episode back to back — note what "
            f"{name} thinks it is about at the start versus what it has become by the "
            f"end, and which characters changed the most in between."
        )
        target = f"{display} pilot + a late-season episode"
        minutes = min(minutes, 90)

    decade = ""
    if year and year.isdigit():
        decade = f"{(int(year) // 10) * 10}s"
    tags = [t.strip() for t in tagstr.split(",") if t.strip()]
    if decade and decade.lower() not in [t.lower() for t in tags]:
        tags.append(decade)

    topic = {
        "id": tid,
        "categoryId": cat_id,
        "subtype": subtype,
        "name": display,
        "teaser": teaser[:450],
        "imageUrl": "",
        "byline": byline,
        "exploreAction": {
            "verb": verb,
            "targetName": target,
            "durationMinutes": minutes,
            "instruction": instruction[:600],
        },
        "tags": tags,
        "tier": 2,
    }
    if episodes:
        topic["episodeCount"] = episodes
    return topic

# ══════════════════════════ DATA ══════════════════════════
# name|year|byline|fact|tags|minutes[|episodes]

DATA = {}
DATA["mythology"] = dict(
    cat="MYTHOLOGY", prefix="myth", subtype="Myth", verb="Read",
    items=[
"Zeus||Greece|king of the Olympians who won the sky by lottery after freeing his swallowed siblings — his father Cronus ate every child at birth until Zeus arrived hidden in a swaddle of stones.|Gods,Greek,Ancient|30",
"Hera||Greece|queen of the gods whose jealousy shaped more Greek stories than any monster — Zeus once hung her from the sky with anvils tied to her ankles just to end an argument.|Gods,Greek,Ancient|30",
"Poseidon||Greece|god of the sea who lost the sky draw to Zeus and never quite got over it — he sent a flood to settle a land dispute with Athena and built Troy's walls for a wage he was cheated out of.|Gods,Greek,Ancient|30",
"Hades||Greece|the most stable marriage-free ruler in Greek myth — almost nobody worshipped him directly because saying his name was considered unlucky, so Greeks called him Plouton, 'the wealthy one'.|Gods,Greek,Underworld|30",
"Athena||Greece|goddess of wisdom born fully armored from Zeus's headache — she judged a weaving contest by turning the winner into a spider, and her olive tree beat Poseidon's salt spring to found Athens.|Gods,Greek,Wisdom|30",
"Apollo||Greece|god of prophecy who killed the Python at Delphi and took over its oracle — his love affairs ended so badly that even his laurel tree used to be a fleeing woman named Daphne.|Gods,Greek,Prophecy|30",
"Artemis||Greece|hunter goddess who turned a peeping hunter into a stag to be killed by his own dogs — her temple at Ephesus was one of the Seven Wonders, twice.|Gods,Greek,Hunt|30",
"Ares||Greece|the god of war whom even his own father called the most hateful of the Olympians — in one story he was trapped in a bronze jar for thirteen months by giants, and nobody came looking.|Gods,Greek,War|30",
"Aphrodite||Greece|born from sea foam where Uranus's severed body fell — judges voted her the owner of the golden apple that started the entire Trojan War.|Gods,Greek,Love|30",
"Hermes||Greece|messenger god and patron of thieves who invented the lyre the same day he stole his brother's cattle — while still a newborn.|Gods,Greek,Trickster|30",
"Hephaestus||Greece|the smith god thrown off Olympus twice — once by each parent — who forged automatons of gold and built traps to catch his wife in bed with Ares, inviting the other gods to watch.|Gods,Greek,Forge|30",
"Dionysus||Greece|the twice-born god of wine whose mother died asking Zeus for proof of his identity — pirates who kidnapped him once watched their ship sprout vines while their oars turned into snakes.|Gods,Greek,Wine|30",
"Demeter||Greece|harvest goddess whose grief for her missing daughter froze every crop on Earth — she drank an entire barrel of a joke-tender named Baubo to laugh and remember to let spring return.|Harvest,Greek,Ancient|30",
"Persephone||Greece|the queen of the underworld bound by a pomegranate seed rule — eat six seeds in the land of the dead, spend six months there every year. The original seasonal contract.|Underworld,Greek,Seasons|30",
"Prometheus||Greece|the fire-thief chained to a rock with an eagle eating his regenerating liver daily — his punishment lasted centuries until Hercules shot the bird.|Fire,Titan,Greek|30",
"Pandora||Greece|the first woman, crafted by the gods as beautiful bait — her jar released every evil into the world, and only Hope stayed inside when the lid slammed shut.|Origin,Greek,Ancient|30",
"Icarus||Greece|the boy who flew on wax-and-feather wings and ignored every altitude warning — the sea that drowned him still bears his name: the Icarian Sea.|Flight,Greek,Hubris|30",
"Midas||Greece|the king whose golden touch starved him before it enriched him — he washed it away in the Pactolus river, which really did contain gold, and ancient Greeks mined it for centuries.|Gold,Greek,Hubris|30",
"Narcissus||Greece|the youth who fell in love with his own reflection and wasted away at the water's edge — the flower that sprouted there took his name.|Vanity,Greek,Love|30",
"Echo||Greece|the nymph cursed to only repeat others' words — she loved Narcissus but could never speak first, and faded until only her voice remained.|Nymph,Greek,Love|30",
"Orpheus||Greece|the musician who charmed Hades into releasing his dead wife on one condition — he looked back too soon, and Argus-tier grief made him play so sadly that wild animals wept.|Music,Love,Greek|30",
"Theseus||Greece|the labyrinth-solver who forgot to swap his black sails for white ones — his father, watching the horizon, read the black cloth as death and leapt into the Aegean.|Hero,Labyrinth,Greek|30",
"Minotaur||Greece|half-bull half-man locked in Daedalus's labyrinth and fed Athenian youths yearly — the maze was built so well its architect barely escaped it himself.|Monster,Labyrinth,Greek|30",
"Oedipus||Greece|the man who fled a prophecy straight into it — he solved the Sphinx's riddle, became king, and discovered his queen was his mother, blinding himself when the truth surfaced.|Tragedy,Greek,Fate|30",
"Achilles||Greece|the near-invincible warrior dipped in immortality by his heel — he refused to fight over an insult, sulked while friends died, then avenged them knowing it meant his own death.|Hero,Trojan War,Greek|30",
"Hector||Greece|Troy's greatest defender who faced Achilles knowing he would die — three times they ran the city walls before the gods tricked him into stopping.|Trojan War,Greek,Hero|30",
"Odysseus||Greece|the trickster king whose ten-year voyage home included a one-eyed giant, singing monsters, and a witch who turned men into pigs — his dog recognized him first, then died.|Odyssey,Greek,Trickster|30",
"Circe||Greece|the sorceress who turned sailors into swine on her island — Odysseus resisted her potion thanks to a god-given herb called moly, which Homer describes so vaguely botanists still argue about it.|Sorcery,Greek,Odyssey|30",
"Medusa||Greece|the snake-haired Gorgon whose gaze turned men to stone — she was pregnant with Poseidon's child when beheaded, and her blood produced both healing springs and deadly serpents.|Monster,Greek,Gorgon|30",
"Perseus||Greece|the slayer who used a polished shield as a mirror to behead Medusa safely — he later accidentally fulfilled a prophecy by killing his grandfather with a stray discus throw.|Hero,Greek,Gorgon|30",
"Andromeda||Greece|the princess chained to a rock as dragon food to appease a sea god — rescued mid-wedding by Perseus waving Medusa's head, turning the wedding guests to stone.|Rescue,Greek,Constellation|30",
"Heracles||Greece|the strongest mortal alive driven temporarily insane by Hera into killing his family — his twelve labors were penance, starting with a lion whose hide no blade could cut.|Labors,Greek,Hero|30",
"Atlas||Greece|the Titan condemned to hold up the sky forever — 'Atlantis' and the Atlantic are both named after him, and the tallest mountain range in northwest Africa is the Atlas Mountains.|Titan,Greek,Sky|30",
"Cronus||Greece|the Titan who castrated his own father with a flint sickle, then ate his children fearing the same fate — his harvest festival, Kronia, was the Greek ancestor of Saturnalia.|Titan,Greek,Time|30",
"Sisyphus||Greece|the trickster who cheated death twice by chaining up Thanatos himself — his boulder punishment became philosophy's favorite metaphor for endless human effort.|Underworld,Greek,Punishment|30",
"Tantalus||Greece|the king who served his own son at a banquet for the gods — his eternal punishment gave English the word 'tantalize': food and water eternally retreating.|Underworld,Greek,Punishment|30",
"Arachne||Greece|the mortal weaver who challenged Athena to a tie-dye of tapestries — hers depicted the gods' worst crimes, and the furious goddess shredded it and turned her into the first spider.|Weaving,Greek,Hubris|30",
"Bellerophon||Greece|the rider of Pegasus who defeated the Chimera by flying above its flames — he later tried to fly to Olympus itself, and Zeus's gadfly bucked him back to Earth.|Pegasus,Greek,Hero|30",
"Pegasus||Greece|the winged horse born from Medusa's neck-blood — he carried thunderbolts for Zeus and stamped out the fountain Hippocrene, beloved of poets, on Mount Helicon.|Winged horse,Greek,Mythic beast|30",
"Chimera||Greece|the lion-goat-serpent hybrid that breathed fire until Bellerophon flew above its blast — 'chimera' now means any impossible hybrid, including lab-made organisms.|Monster,Greek,Hybrid|30",
"Typhon||Greece|the hundred-headed storm giant who ripped Zeus's tendons out and hid them in a cave — even the other gods fled to Egypt disguised as animals rather than face him.|Monster,Storm,Greek|30",
"Eros and Psyche||Greece|the love god who visited his bride only in darkness — her curiosity with an oil lamp cost her three impossible trials, including sorting a mountain of mixed seeds overnight.|Love,Greek,Trial|30",
"Atalanta||Greece|the fastest huntress alive who agreed to marry only a man who could outrun her — suitors lost and died until one dropped golden apples mid-race.|Race,Greek,Huntress|30",
"Jason and the Argonauts||Greece|the quest crew who sailed for the Golden Fleece with the era's entire hero roster aboard — their ship, the Argo, spoke to them through a talking prow-beam.|Quest,Greek,Golden Fleece|30",
"Medea||Greece|the sorceress who betrayed her homeland for love of Jason — years later she took revenge on his infidelity with gifts that literally ignited the new bride.|Sorcery,Greek,Revenge|30",
"Romulus and Remus||Rome|the twins raised by a wolf who founded Rome on the Palatine Hill — Remus mocked his brother's city walls by jumping over them, and paid with his life.|Founding,Roman,Ancient|30",
"Aeneas||Rome|the Trojan survivor who carried his father out of burning Troy on his back — Virgil made his journey the founding prophecy of the Roman Empire.|Troy,Roman,Epic|30",
"Odin||Norse|the one-eyed Allfather who traded an eye for a drink from the well of wisdom — he hanged himself on the world-tree for nine nights to steal the runes, dying and returning.|Gods,Norse,Wisdom|30",
"Thor||Norse|the hammer-wielding thunder god who once dressed as a bride to infiltrate a giant wedding and retrieve Mjölnir — Loki played the bridesmaid, and nearly ruined the veil act.|Thunder,Norse,Gods|30",
"Loki||Norse|the shapeshifting trickster who cut Sif's hair on a dare, caused Baldr's death with mistletoe, and fathered the wolf destined to eat Odin.|Trickster,Norse,Mischief|30",
"Freyja||Norse|the love goddess who cries tears of gold and rides a chariot of cats — half of all battle-dead belonged to her, not Odin, and she got first pick.|Love,Norse,Valkyrie|30",
"Fenrir||Norse|the wolf so large the gods had to chain him with a magical ribbon — he bit off the hand of the god who bound him, and will swallow Odin at Ragnarök.|Wolf,Norse,Doom|30",
"Jörmungandr||Norse|the world-serpent so vast it encircles the ocean biting its own tail — Thor once tried to fish it up with an ox-head lure and nearly sank the boat.|Serpent,Norse,Ocean|30",
"Yggdrasil||Norse|the world-tree whose roots reach into wisdom wells and dragon lairs — an eagle and a hawk argue at its crown through a squirrel messenger named Ratatoskr.|Cosmos,Norse,Tree|30",
"Ragnarök||Norse|the prophesied end of the world where the sun is eaten, gods die, and the earth rises again green — the Norse believed their own gods were doomed, and wrote it down anyway.|Doomsday,Norse,Prophecy|30",
"Valhalla||Norse|the afterlife hall where worthy slain warriors fight all day and feast all night — the roof is made of golden shields and the goat grazing on it refills the mead barrels endlessly.|Afterlife,Norse,Warriors|30",
"Baldur||Norse|the invulnerable god of light killed by a mistletoe dart blindfolded into a game — his mother extracted oaths from every object on Earth but skipped one shrub.|Light,Norse,Death|30",
"Heimdall||Norse|the watchman god who can see a hundred leagues and hear grass grow — he owns a horn loud enough to wake the dead and will kill Loki at Ragnarök, mutually.|Watchman,Norse,Bifrost|30",
"Wayland the Smith||Norse|the master smith hamstrung by a king who stole his sword and murdered his sons — he escaped on wings he forged himself after getting revenge via the king's children's deaths.|Smith,Norse,Revenge|30",
"Beowulf||Norse|the Geatish hero who tore off a monster's arm bare-handed and fought a dragon at seventy — the poem describing him survived in exactly one manuscript, half-burned.|Epic,Norse,Dragon|30",
"Ra||Egypt|the sun god who sails a boat through the underworld nightly, fighting the chaos serpent Apophis — some pharaohs claimed to be his son so loudly they built pyramids as landing pads.|Sun,Egyptian,Gods|30",
"Osiris||Egypt|the murdered god resurrected as ruler of the dead — his jealous brother sealed him in a chest and scattered the pieces, and Isis collected fourteen of them to revive him.|Death,Egyptian,Resurrection|30",
"Isis||Egypt|the magic-using widow who pieced her husband back together and hid their son in the Nile marshes — Egyptians credited her with teaching medicine and weaving.|Magic,Egyptian,Mother|30",
"Horus||Egypt|the falcon-headed heir who fought Set for decades over his father's throne — he lost an eye in battle, and its restoration became Egypt's most protective symbol.|Sky,Egyptian,Falcon|30",
"Set||Egypt|the desert god of chaos who murdered his brother and lost his testicles in the rematch — Egyptians saw him as necessary violence, defending Ra's sun-barque nightly.|Chaos,Egyptian,Desert|30",
"Anubis||Egypt|the jackal-headed embalmer who weighs hearts against a feather — hearts heavy with sin fed the crocodile-lion-hippo devourer Ammit, ending existence entirely.|Death,Egyptian,Judgment|30",
"Thoth||Egypt|the ibis-headed scribe god who invented writing and referees divine disputes — he won five extra calendar days from the moon in a gambling match, letting gods be born at all.|Writing,Egyptian,Wisdom|30",
"Sekhmet||Egypt|the lioness goddess sent to punish humanity who enjoyed slaughter so much she nearly wiped everyone out — she stopped only when the earth was flooded with beer dyed red to look like blood.|Lioness,Egyptian,War|30",
"Bastet||Egypt|the cat goddess whose cult center banned lion burials — her festival drew over 700,000 visitors a year, making it ancient Egypt's rowdiest party.|Cat,Egyptian,Home|30",
"Apophis||Egypt|the mile-long serpent of chaos who attacks the sun-boat every midnight — Egyptians held nightly rituals of spitting, cursing, and stepping on wax effigies to help Ra win.|Serpent,Egyptian,Chaos|30",
"Weighing of the Heart||Egypt|the afterlife entrance exam where your heart balances against Maat's feather — confess forty-two sins you did NOT commit while forty-two gods watch silently.|Afterlife,Egyptian,Judgment|30",
"Marduk||Babylonian|the storm god who split the sea-dragon Tiamat in half and built heaven and earth from her carcass — Babylon's New Year festival re-enacted it annually, king included.|Creation,Babylonian,Storm|30",
"Tiamat||Babylonian|the primordial salt-sea dragon who birthed the gods then declared war on them — her eleven monster-children were the first recorded monster army.|Dragon,Babylonian,Ocean|30",
"Descent of Ishtar||Babylonian|the love goddess who stormed the underworld gate by gate, shedding one power at each — while she sat imprisoned, all sex and birth on Earth simply stopped.|Underworld,Babylonian,Love|30",
"Indra||Hindu|the thunder god who rides a white elephant and drinks so much soma he needs the priests' help — he slew the drought-serpent Vritra with a bolt forged from sage bones.|Thunder,Hindu,King|30",
"Ganesha||Hindu|the elephant-headed remover of obstacles who writes epics with a broken tusk — his head came about when Shiva beheaded him and swore to replace it with the first animal found: an elephant.|Elephant,Hindu,Wisdom|30",
"Hanuman||Hindu|the monkey god who once tried to eat the sun thinking it was fruit — as an adult he leapt the ocean to Lanka, burned a city with his tail, and carried a whole mountain of herbs.|Monkey,Hindu,Devotion|30",
"Narasimha||Hindu|the man-lion avatar engineered as a loophole — appearing at twilight, on a threshold, to kill a demon granted immunity from man, beast, day, night, indoors, and outdoors.|Avatar,Hindu,Lion|30",
"Kali||Hindu|the blue-black goddess of time who danced so wildly on a battlefield she nearly destroyed it — her husband Shiva lay down under her feet to stop the dance, which is why she is shown standing on him.|Time,Hindu,Dance|30",
"Samudra Manthan||Hindu|the churning of the cosmic ocean by gods and demons coiling a serpent around a mountain — poison emerged first, and Shiva drank it, holding it in his throat forever.|Ocean,Hindu,Creation|30"
,
"Amaterasu||Japanese|the sun goddess who hid in a cave after her brother's tantrum, plunging the world dark — gods lured her out with a mirror and a strip-tease dance, laughing so hard she peeked.|Sun,Japanese,Cave|30",
"Susanoo||Japanese|the storm god exiled from heaven who celebrated by flaying a pony — he redeemed himself by slaying the eight-headed serpent Yamata no Orochi and finding a sword in its tail.|Storm,Japanese,Serpent|30",
"Izanagi and Izanami||Japanese|the creation couple who stirred the ocean with a jeweled spear — drops falling from its tip became Japan's first island, and their divorce created the border between life and death.|Creation,Japanese,Islands|30",
"Kitsune||Japanese|fox spirits whose tails multiply with age and wisdom — nine-tailed kitsune can possess people, and statues at Inari shrines often wear tiny red bibs donated by devotees.|Fox,Yokai,Japanese|30",
"Tanuki||Japanese|raccoon-dog tricksters whose illusions famously fail at the tail — their statues outside bars and restaurants invite prosperity, and their scrotum-based transformations are a folk-art classic.|Trickster,Yokai,Japanese|30",
"Momotaro||Japanese|the peach-born hero who recruited a talking dog, monkey, and pheasant with millet dumplings and raided Oni Island — every Japanese child learns the song before school age.|Hero,Folklore,Japanese|30",
"Urashima Taro||Japanese|the fisherman who rescued a turtle and spent days in the Dragon Palace that were centuries ashore — opening the forbidden box aged him instantly to dust.|Rip van Winkle,Folklore,Japanese|30",
"Yuki-onna||Japanese|the snow woman who freezes travelers with her breath — she spared a handsome young man because of his eyes, then married him, and melted when he told the story.|Snow,Yokai,Japanese|30",
"Kappa||Japanese|water imps with bowls of life-water in their heads — bowing to one drains the bowl and defeats it, which is why cucumber offerings and deep bows feature in kappa etiquette.|Water,Yokai,Japanese|30",
"Sun Wukong||Chinese|the Monkey King who ate immortal peaches, erased his name from the ledger of the dead, and survived 49 days in a furnace — gaining fire-eyes that see through any disguise.|Monkey,Chinese,Journey West|30",
"Nüwa||Chinese|the serpent-bodied creator who molded humans from yellow clay by hand — tired halfway, she dragged a rope through mud to mass-produce the rest, explaining nobles and peasants alike.|Creation,Chinese,Goddess|30",
"Pangu||Chinese|the primordial giant who cracked open the cosmic egg and separated sky from earth with his body — his breath became wind, his eyes the sun and moon, and his lice became humans.|Creation,Chinese,Giant|30",
"Chang'e and Hou Yi||Chinese|the archer who shot down nine of ten suns and won immortality pills — his wife swallowed them all and floated to the moon, where she lives with a jade rabbit.|Moon,Chinese,Legend|30",
"Nezha||Chinese|the boy god born from a ball of flesh who fought a dragon prince, then returned his own body to his parents to spare them — rebuilt from lotus flowers by his teacher.|Boy god,Chinese,Rebellion|30",
"The White Snake||Chinese|the snake spirit who married a human pharmacist — a monk jailed her husband and flooded a temple to expose her, and Chinese opera still stages the flood scene.|Snake spirit,Chinese,Romance|30",
"Cowherd and Weaver Girl||Chinese|star-crossed lovers separated by the Milky Way — magpies form a bridge once a year on the seventh day of the seventh moon, China's original Valentine's Day.|Stars,Chinese,Romance|30"
,
"Cú Chulainn||Celtic|the Irish war-boy who transformed into a battle-monster at rage peak — his enemies feared his ríastrad warp-spasm more than any army, and he tied himself to a standing post to die upright.|Warrior,Celtic,Ireland|30",
"Morrígan||Celtic|the phantom queen who appears as a crow on battlefields offering victory — she once tested hero Cú Chulainn as an old woman and cursed him when he spurned her.|Crow,Celtic,War|30",
"Dagda||Celtic|the good god whose club kills with one end and revives with the other — he owns a cauldron no company leaves unfed and a harp that plays the seasons themselves by mood.|Father,Celtic,Ireland|30",
"Brigid||Celtic|the fire goddess of poetry and healing whom Christians canonized as Saint Brigid — her eternal flame at Kildare was tended by nuns for a thousand years.|Fire,Celtic,Poetry|30",
"Fionn mac Cumhaill||Celtic|the Fenian leader who gained all knowledge by tasting the Salmon of Wisdom and burned his thumb — he built the Giant's Causeway as steps to Scotland.|Hunter,Celtic,Ireland|30",
"Selkies||Scottish|seal-folk who shed their skins to dance on shore — hide a selkie's skin and she stays married ashore, but find it, and the sea calls her home regardless of the children.|Seal,Scottish,Sea|30",
"Banshee||Irish|the keening woman whose wail foretells a death in certain families — she combs her hair with a silver comb, which is why picking up strange combs is Irish-bad luck.|Omen,Irish,Death|30",
"Baba Yaga||Slavic|the witch who lives in a hut on chicken legs and flies a mortar with a pestle — her fence is made of skulls with glowing eye sockets, and she sometimes helps the brave.|Witch,Slavic,Hut|30",
"Koschei the Deathless||Slavic|the villain who cannot die because his soul hides in a needle inside an egg inside a duck inside a hare inside a chest buried on an island.|Immortal,Slavic,Villain|30",
"Firebird||Slavic|the glowing bird whose single feather lights a room like a thousand candles — stealing one launches Russia's most famous quest tales.|Bird,Slavic,Glowing|30",
"Domovoi||Slavic|the household spirit living behind the stove — feed him and the milk stays sweet; anger him by sweeping toward the hearth at night and hair turns to rope.|Household spirit,Slavic,Home|30",
"Rusalka||Slavic|the water maiden drowned by betrayal who tickles travelers into rivers — during Rusalka week, swimming was forbidden and garlands floated instead.|Water spirit,Slavic,River|30",
"Perun and Veles||Slavic|the thunder god and serpent-cattle god feud that explains every storm — Veles steals Perun's cattle, Perun hurls lightning, and rain ends their chase.|Thunder,Slavic,Storm|30",
"Vasilisa the Beautiful||Slavic|the girl sent to Baba Yaga's hut for fire who survives by feeding the witch's talking skull-horses and completing impossible sorting tasks with doll magic.|Fairy tale,Slavic,Doll|30",
"Anansi||West African|the spider trickster who bought all the world's stories from the sky god — price: a live leopard, hornets, and a fairy nobody can see, all caught by wit alone.|Spider,Trickster,African|30",
"Maui||Polynesian|the demigod who fished islands out of the sea with a hook made of his grandmother's jawbone — his attempt at immortality for mankind failed when a bird laughed mid-spell.|Demigod,Polynesian,Trickster|30"
,
"Rainbow Serpent||Aboriginal Australian|the creator being whose movements carved rivers and gorges — she sleeps in deep waterholes and sends floods when disturbed, so many billabongs remain unswum.|Creation,Australian,Serpent|30",
"Quetzalcoatl||Aztec|the feathered serpent god who invented the calendar and books — he sailed east on a raft of serpents, promising to return, and the timing haunted Montezuma's welcome of Cortés.|Feathered serpent,Aztec,Wind|30",
"Tezcatlipoca||Aztec|the smoking-mirror god who ruled by night and quarreled with Quetzalcoatl across five world-ages — each world ended in jaguars, hurricanes, fire-rain, and flood.|Jaguar,Aztec,Night|30",
"Popol Vuh Twins||Maya|the hero twins Hunahpu and Xbalanque who out-cheated the lords of death at a ballgame — after losing the first round fatally, they resurrected each other and sacrificed the gods instead.|Twins,Maya,Ballgame|30",
"Thunderbird||Native American|the storm bird whose wingbeats are thunder and whose glance is lightning — petroglyphs of it guard cliff faces across the Pacific Northwest.|Thunder,Native American,Bird|30",
"Wendigo||Algonquian|the gaunt ice-spirit born when a human eats human flesh during famine — it grows taller with every meal, always starving, always hunting.|Ice,Algonquian,Cannibal|30",
"John Henry||American|the steel-driving man who raced a steam drill through a mountain and won — then died with hammer in hand, becoming labor's first folk martyr.|Folk hero,American,Railroad|30",
"Paul Bunyan||American|the lumberjack so large his footprints filled with water and became Minnesota's lakes — his blue ox Babe dug the Grand Canyon dragging an axe.|Lumberjack,American,Tall tale|30",
"Robin Hood||English|the outlaw who robbed coaches in Sherwood Forest — the earliest ballads mention no Maid Marian, and his 'rob the rich' motto only hardened centuries later.|Outlaw,English,Folklore|30",
"Faust||German|the scholar who sold his soul for twenty-four years of knowledge and pleasure — based on a real alchemist, Johann Georg Faust, whose contemporaries left genuinely horrified reviews.|Deal,German,Scholar|30",
"Pied Piper of Hamelin||German|the rat-catcher whose flute lured 130 children away on June 26, 1284 — the town of Hamelin documented the date for centuries without ever explaining what happened.|Piper,German,Mystery|30",
"William Tell||Swiss|the marksman forced to shoot an apple off his son's head — his second crossbow bolt, meant for the tyrant Gessler, started Switzerland's founding rebellion.|Marksman,Swiss,Apple|30",
"Prester John||Medieval|the legendary Christian king of the East whose letter to Europe promised a kingdom of wonders — explorers searched Africa and Asia for him for four centuries.|Legend,Medieval,King|30",
"El Dorado||South American|the 'golden man' who dusted himself in gold powder and dove into Lake Guatavita as an offering — conquistadors heard 'city of gold' and drained Colombian mountainsides.|Gold,South American,Legend|30",
"Kraken||Norwegian|the sea monster so vast sailors mistook it for an island — anchoring on its back was fatal, and it dragged ships down with whirlpool-sucking arms.|Sea monster,Norwegian,Kraken|30",
"Kalevala||Finnish|the national epic compiled from singing shamans — its wizard-hero Väinämöinen builds a boat by singing, and Tolkien borrowed its meter for his own legendarium.|Epic,Finnish,Singing magic|30",
"Rostam and Sohrab||Persian|Iran's greatest hero unknowingly kills his own son in single combat — the tragedy anchors the Shahnameh, a poem longer than the Iliad and Odyssey combined.|Epic,Persian,Tragedy|30",
"Simurgh||Persian|the benevolent thirty-bird giant of Persian myth whose feathers summon help — in Sufi poetry, thirty birds seeking the Simurgh discover they themselves are it ('si morgh').|Bird,Persian,Wisdom|30",
"Sundiata||West African|the crippled prince who rose to found the Mali Empire — griots still recite his epic with instruments, and his lineage ruled the richest gold empire of medieval Africa.|Epic,Malian,Founder|30",
"Aswang||Filipino|the shapeshifting ghoul that appears as a neighbor by day and feeds on the sick at night — Philippine villages historically posted garlic and salt at doors.|Shapeshifter,Filipino,Ghoul|30",
"Bakunawa||Filipino|the moon-eating serpent who swallows eclipses — Filipinos banged pots and pans during eclipses to make it spit the moon back out.|Moon-eater,Filipino,Serpent|30",
"Tangun||Korean|the bear-turned-woman whose son founded Korea's first kingdom in 2333 BC — Koreans still count their calendar from his reign.|Founder,Korean,Bear|30",
"Dokkaebi||Korean|night-spirit tricksters who challenge travelers to wrestling matches — carry a straw rope or answer riddles correctly and they gift you magic items instead of bruises.|Spirit,Korean,Trickster|30",
],
)
DATA["series"] = dict(
    cat="SERIES", prefix="series", subtype="Series", verb="Watch",
    items=[
"The Wire|2002|David Simon|a Baltimore drug drama cast with real teachers, cops, and ex-offenders — season 4's school storyline is used in actual sociology courses.|Crime,Drama,American|60",
"The Sopranos|1999|David Chase|the mob boss in therapy that legitimized prestige TV — HBO nearly passed, and the final cut-to-black is debated louder today than in 2007.|Mafia,Drama,American|60",
"Mad Men|2007|Matthew Weiner|advertising's golden age through Don Draper's smoke — the pitch meeting that sold the Kodak Carousel made grown ad executives cry on set.|Advertising,Drama,American|50",
"Breaking Bad|2008|Vince Gilligan|chemistry teacher to meth kingpin — AMC worried Walt was unsympathetic, so Bryan Cranston argued the plan was 'Mr. Chips becomes Scarface' and got the green light.|Crime,Drama,American|47",
"Better Call Saul|2015|Peter Gould|the lawyer prequel that many critics eventually rated above Breaking Bad — Bob Odenkirk suffered a heart attack filming take twelve of a single scene.|Legal,Drama,American|47",
"The Leftovers|2014|Damon Lindelof|2% of humanity vanishes and nobody explains why — the show refuses answers on purpose, and its season 2 pivot is considered one of TV's great turnarounds.|Mystery,Drama,American|55",
"Chernobyl|2019|Craig Mazin|the five-episode retelling so accurate that tourists flooded the exclusion zone — the 'three divers' survived into the 2010s, a fact the show deliberately corrects on screen.|Historical,Drama,British|60",
"Fleabag|2016|Phoebe Waller-Bridge|a fourth-wall-breaking comedy that started as a 15-minute Edinburgh fringe slot — the Hot Priest season rewrote British TV romance in six episodes.|Comedy,Drama,British|27",
"Succession|2018|Jesse Armstrong|the media dynasty written partly by a real ex-tabloid journalist — its dialogue improvisation rate was so high editors kept accidental gold takes.|Family,Drama,American|60",
"Severance|2022|Dan Erickson|work-life balance taken literally via brain surgery — Erickson wrote the pilot while working at a door factory, inspired by dreading Mondays.|Sci-fi,Thriller,American|50",
"Ted Lasso|2020|Bill Lawrence|an American football coach hired to run English football as a sabotage plot — Apple renewed it partly off the strength of its mental-health storylines.|Sports,Comedy,British|30",
"The Bear|2022|Christopher Storer|a fine-dining chef inherits his late brother's chaotic sandwich shop — every kitchen panic attack is choreographed like an action sequence.|Food,Drama,American|30",
"Fargo|2014|Noah Hawley|each season is a new crime tale 'inspired by true events' — a title card the Coen brothers' original film invented, and the series keeps the joke going.|Crime,Anthology,American|50",
"True Detective|2014|Nic Pizzolatto|Louisiana occult noir built around two detectives who hate each other for a decade — Matthew McConaughey's Rust Cohle monologues spawned a philosophy meme industry.|Crime,Noir,American|55",
"Mr. Robot|2015|Sam Esmail|a cybersecurity engineer vigilante with unreliable memory — Esmail insisted on single-take camera moves and real hacking scripts reviewed by actual security researchers.|Hacking,Thriller,American|45",
"Atlanta|2016|Donald Glover|a hip-hop manager comedy that abandons genre every few episodes — the 'Teddy Perkins' episode required Glover in prosthetics for six hours uncredited.|Comedy,Drama,American|30",
"Barry|2018|Bill Hader|a hitman discovers community theater — Hader directed episodes using horror grammar for comic scenes, winning back-to-back Emmys.|Hitman,Dark comedy,American|30",
"What We Do in the Shadows|2019|Jemaine Clement|vampire flatmates on Staten Island — the energy-vampire Colin Robinson is based on office coworkers Clement refused to identify.|Vampires,Comedy,American|22",
"Schitt's Creek|2015|Eugene Levy|a billionaire family loses everything to a crooked accountant and lands in a town they bought as a joke — father and son Levy wrote it together.|Sitcom,Comedy,Canadian|22",
"Brooklyn Nine-Nine|2013|Dan Goor|a precinct sitcom whose cold opens are studied in writers' rooms — the cast's real Halloween Heist competitions got competitive enough to require arbitration.|Police,Comedy,American|22",
"Parks and Recreation|2009|Greg Daniels|a small-town parks department mockumentary — Ron Swanson's libertarianism is so beloved that real Libertarian parties ran 'Ron Swanson' write-in campaigns.|Government,Comedy,American|22"
,
"It's Always Sunny in Philadelphia|2005|Rob McElhenney|five terrible people ruin Philadelphia weekly — the show runs on no writers' room hierarchy, and McElhenney gained 50 pounds on purpose for comedic vanity arcs.|Sitcom,Comedy,American|22",
"Curb Your Enthusiasm|2000|Larry David|a semi-fictional Larry David weaponizes social etiquette — most plots start as real annoyances the writers experienced that week.|Improvisation,Comedy,American|30",
"Arrested Development|2003|Mitchell Hurwitz|the Bluth family's frozen-banana empire — its dense foreshadowing jokes only pay off on rewatch number three, creating the modern rewatch culture.|Sitcom,Comedy,American|22",
"Community|2009|Dan Harmon|a study group at a community college that weaponizes genre parody — NBC canceled it twice and fans campaigned it back with paintball metaphors.|Ensemble,Comedy,American|22",
"30 Rock|2006|Tina Fey|behind-the-scenes sketch comedy chaos — Fey based Liz Lemon on her SNL tenure and smuggled in jokes NBC lawyers flagged then approved anyway.|Workplace,Comedy,American|22",
"Veep|2012|Armando Iannucci|political profanity engineered like jazz — Iannucci's writers tracked real congressional scandals and often couldn't outdo reality.|Politics,Comedy,American|28",
"Silicon Valley|2014|Mike Judge|startup satire where the compression algorithm is real — Judge consulted Stanford engineers who confirmed the middle-out idea is mathematically sound.|Tech,Comedy,American|28",
"Rick and Morty|2013|Justin Roiland|dimension-hopping nihilism born from a Back to the Future parody — its Pickle Rick episode became a case study in fandom excess.|Animation,Sci-fi,American|22",
"BoJack Horseman|2014|Raphael Bob-Waksberg|a talking-horse sitcom that quietly became TV's sharpest depression study — animators hid background puns so densely fans still catalog new ones.|Animation,Drama,American|25",
"Arcane|2021|Christian Linke|a League of Legends adaptation that took six years of hand-painted frames — Riot expected fan service and got an Emmy-winning class-war opera.|Animation,Fantasy,French|42",
"The Boys|2019|Eric Kripke|superheroes as corporate products — Karl Urban never breaks character on set and stays in Butcher accent between takes.|Superhero,Satire,American|60",
"Invincible|2021|Robert Kirkman|a teen superhero whose dad is the genre's worst nightmare — the season 1 finale's train scene triggered discourse about animated violence limits.|Superhero,Animation,American|45",
"Watchmen|2019|Damon Lindelof|a sequel series about reparations and masked policing — Lindelof wrote a letter to fans declaring it a remix, not an adaptation.|Superhero,Drama,American|55",
"Dark|2017|Baran bo Odaru|German time-travel knot so tight the creators published a family tree — Netflix added a second-screen companion app just to follow it.|Time travel,Mystery,German|55",
"Black Mirror|2011|Charlie Brooker|tech dystopia anthology named after the phone screen glow — 'San Junipero' flipped the show's doom formula and won Emmys for it.|Anthology,Sci-fi,British|60",
"Stranger Things|2016|The Duffer Brothers|kids vs government monsters in 1980s Indiana — Netflix originally wanted an anthology; the Duffers pitched a movie-trailer style sizzle reel instead.|Horror,Sci-fi,American|50",
"The Crown|2016|Peter Morgan|the British monarchy across decades — each two-season cast recoup costs more than most shows' total budgets.|Historical,Drama,British|55",
"The Queen's Gambit|2020|Scott Frank|an orphaned chess prodigy miniseries that made chess sets sell out worldwide — every tournament board position was designed by grandmaster Garry Kasparov's consultant team.|Chess,Drama,American|55",
"Narcos|2015|Chris Brancato|the DEA vs Pablo Escobar saga — Wagner Moura learned Spanish for the role despite being Brazilian, earning praise from actual Colombian journalists.|Cartel,Crime,American|55",
"Peaky Blinders|2013|Steven Knight|Birmingham gangsters with razor caps — the flat cap trend it revived doubled UK newsboy-cap sales within two seasons.|Gangster,Drama,British|55"
,
"Taboo|2017|Steven Knight|Tom Hardy's 1814 London conspiracy thriller — Hardy co-wrote it with his father and much dialogue is whispered by design.|Period,Thriller,British|55",
"Vikings|2013|Michael Hirst|Ragnar Lothbrok's raids dramatized — Hirst writes every episode solo, a rarity credited with keeping the sagas consistent.|Historical,Drama,Irish|45",
"The Last Kingdom|2015|Stephen Butchard|Saxon England through a Danish-raised warrior — Bernard Cornwell wrote Uhtred after discovering no Anglo-Saxon chronicle tells the Danish side.|Historical,Drama,British|55",
"Rome|2005|Bruno Heller|Caesar's fall on an HBO budget so lavish it contributed to a studio merger — its sets were reused for Gladiator-adjacent productions for a decade.|Historical,Drama,British|55",
"Band of Brothers|2001|Tom Hanks|Easy Company's WWII campaign with real veteran interviews as bookends — every soldier interviewed who appears on screen is introduced by name.|WWII,Miniseries,American|60",
"The Pacific|2010|Steven Spielberg|the Pacific Theater companion piece — it follows three real Marines whose memoirs anchor each arc, with combat filmed in Australia's quarries at scale.|WWII,Miniseries,American|55",
"Deadwood|2004|David Milch|a gold-rush town speaking Shakespearean profanity — Milch wrote scenes aloud, acting every part in the writers' room.|Western,Drama,American|55",
"Justified|2010|Graham Yost|a US Marshal enforcing Kentucky justice — Elmore Leonard approved the adaptation shortly before his death and called Timothy Olyphant 'the real Raylan'.|Western,Crime,American|45",
"Sons of Anarchy|2008|Kurt Sutter|a motorcycle club's Hamlet arc — Sutter cast real club members as extras and required Harley authenticity checks on set.|Bikers,Drama,American|45",
"The Expanse|2015|Mark Fergus|hard sci-fi politics with real physics consultants — the show's zero-G vomit sequences use rotating sets NASA astronauts have praised.|Space,Sci-fi,American|45",
"Battlestar Galactica|2004|Ronald D. Moore|humanity's remnant fleet flees genocidal robots — Moore's podcast commentary on each episode became a masterclass in serialized TV decisions.|Space,Sci-fi,American|45",
"Firefly|2002|Joss Whedon|space western canceled after eleven aired episodes — its fan-driven film Serenity remains the textbook case of cancellation protest.|Space,Western,American|45",
"Lost|2004|J.J. Abrams|plane-crash survivors on a polar-bear island — the smoke monster was realized by accident when a producer's fog machine jammed.|Mystery,Sci-fi,American|45",
"Fringe|2008|J.J. Abrams|parallel-universe FBI cases anchored by a mad scientist — John Noble's dual-universe performance earned a cult following and a science lecture circuit.|Sci-fi,Mystery,American|45",
"Person of Interest|2011|Jonathan Nolan|an AI predicting crimes before they happen — the show pivoted from procedural to AI ethics and predicted modern surveillance debates years early.|AI,Thriller,American|45",
"Westworld|2016|Lisa Joy|robot theme park hosts gaining memory — Anthony Hopkins accepted the role specifically for the 'these violent delights' monologue.|Sci-fi,Thriller,American|60",
"Fallout|2024|Jonathan Nolan|video game vaults adapted with real lore discipline — Bethesda signed off after verifying every prop matched game assets frame-for-frame.|Post-apocalyptic,Sci-fi,American|55",
"The Last of Us|2023|Craig Mazin|fungal apocalypse road trip — the Bill and Frank episode deviated from the game and made creator Neil Druckmann cry at the table read.|Post-apocalyptic,Drama,American|55",
"House of the Dragon|2022|Ryan Condal|Targaryen civil war prequel — the showrunners mapped dragon lineage spreadsheets to keep fire-breathing genealogy consistent.|Fantasy,Drama,American|60",
"Game of Thrones|2011|David Benioff|the fantasy epic that globalized binge TV — its Red Wedding reaction compilation remains YouTube's most-edited audience-shock montage.|Fantasy,Drama,American|55",
"Sherlock|2010|Steven Moffat|Victorian detective in smartphone London — Benedict Cumberbatch's rapid-fire deduction monologues required teleprompter training.|Detective,Mystery,British|88",
"Doctor Who|2005|Russell T Davies|the regenerating time traveler rebooted into a global brand — Davies returned as showrunner twenty years later, an unprecedented comeback.|Sci-fi,British,Adventure|50",
"Luther|2010|Neil Cross|Idris Elba's detective walking the psychopath line — Cross wrote the role imagining what Coleridge's 'Ancient Mariner' would look like in a police coat.|Detective,Crime,British|55"
,
"Line of Duty|2012|Jed Mercurio|anti-corruption police interrogations famous for 'AC-12' jargon — Mercurio writes under pseudonyms to keep real officers advising anonymously.|Police,Crime,British|58",
"Broadchurch|2013|Chris Chibnall|a coastal town murder where every resident is a suspect — David Tennant's DI Hardy was written with his Doctor Who energy inverted.|Crime,Mystery,British|50",
"Happy Valley|2014|Sarah Lancashire|a Yorkshire sergeant confronting her grandson's criminal father — Lancashire insisted on doing the kidnapping struggle take herself, fracturing a knuckle.|Crime,Drama,British|58",
"Peep Show|2003|Andrew O'Connor|point-of-view sitcom narrated by two terrible flatmates — its internal monologue format was pitched as 'what if we hear what British men actually think'.|Comedy,British,Cringe|25",
"The Inbetweeners|2008|Damon Beesley|four suburban sixth-formers failing adolescence — the 'friendship' bus scene was improvised and became a national catchphrase.|Comedy,British,Teen|25",
"Black Books|2000|Dylan Moran|a misanthropic bookshop and its two enablers — Moran and Bill Bailey improvised so much the scripts were rewritten around takes.|Comedy,British,Sitcom|25",
"Spaced|1999|Edgar Wright|slacker flatmates framed like action cinema — Wright's whip-pans and crash zooms here directly seeded Shaun of the Dead's style.|Comedy,British,Pop culture|25",
"Misfits|2009|Howard Overman|young offenders gain useless superpowers on community service — Nathan's immortality reveal is saved for the finale as a running gag payoff.|Sci-fi,Comedy,British|45",
"Utopia|2013|Dennis Kelly|conspiracy thriller about population control with a yellow palette so distinct it influenced music videos — its violence censors demanded cuts that became iconic shots.|Thriller,Conspiracy,British|50",
"The End of the F***ing World|2017|Jonathan Entwistle|a self-declared psychopath teen and a runaway — the comic's author said the show improved his own ending, which he'd rushed.|Teen,Drama,British|20",
"Sex Education|2019|Laurie Nunn|a teen therapist operating out of a school bathroom — filmed in Wales but styled as an ambiguous Americana to dodge both UK and US clichés.|Teen,Comedy,British|50",
"Derry Girls|2018|Lisa McGee|Troubles-era Northern Ireland through teenage chaos — the finale intercut the Good Friday Agreement vote with a school talent show.|Comedy,British,Coming of age|30",
"This Is England|2010|Shane Meadows|skinhead culture examined across three miniseries sequels — Meadows cast untrained locals from council estates and let them rewrite lines.|Drama,British,Coming of age|48",
"Top Boy|2011|Ronan Bennett|East London drug-market drama revived by Drake — Ashley Walters and Kano were already musicians before critics called it 'the British Wire'.|Crime,Drama,British|55",
"Money Heist|2017|Álex Pina|the 'Bella Ciao' bank robbery that went global — Netflix acquired it after Spanish ratings collapsed, and it became the platform's most-watched non-English launch.|Heist,Thriller,Spanish|55",
"Squid Game|2021|Hwang Dong-hyuk|debt-ridden contestants play childhood games for survival — rejected by studios for a decade, it became Netflix's biggest launch ever.|Survival,Thriller,Korean|55",
"Kingdom|2019|Kim Seong-hun|Joseon-era zombie political thriller — its period zombies sprint, breaking both historical drama and zombie-genre rules simultaneously.|Zombie,Historical,Korean|50",
"Signal|2016|Kim Won-seok|a walkie-talkie connecting detectives sixteen years apart — based loosely on Korea's real unsolved Hwaseong serial murders, later solved by DNA.|Crime,Time travel,Korean|60",
"Reply 1988|2015|Shin Won-ho|five families in one Seoul alley in 1988 — its neighborhood dinners were staged so warmly that Korean viewers petitioned to preserve similar alleys from redevelopment.|Family,Nostalgia,Korean|80",
"Crash Landing on You|2019|Lee Jeong-hyo|a paraglider heiress crash-lands in North Korea — the writing team interviewed defectors for village details, making the romance weirdly documentary-adjacent.|Romance,Drama,Korean|70"
,
"Goblin|2016|Kim Eun-sook|an immortal general seeking a bride to end his curse — the sword-in-chest visual effect required a prosthetic rig actor Gong Yoo wore between takes.|Fantasy,Romance,Korean|70",
"Hospital Playlist|2020|Lee Woo-jung|five doctor friends who band together since med school — the actors rehearsed as an actual band for months before filming the cover songs.|Medical,Drama,Korean|65",
"My Mister|2018|Kim Won-seok|an engineer and a wiretapping temp worker saving each other quietly — IU's casting was controversial until critics called it the performance of the year.|Drama,Korean,Healing|70",
"Ozark|2017|Bill Dubuque|a financial planner laundering money in Missouri lakes — Julia Garner's Ruth dialect coaching involved real Ozark recordings archived by linguists.|Crime,Thriller,American|55",
"Hannibal|2013|Bryan Fuller|a cannibal psychiatrist plated like fine dining — food stylist Janice Poon made the 'human' dishes from edible art so realistic crew members refused lunch.|Thriller,Psychological,American|45",
"Dexter|2006|James Manos Jr.|a blood-spatter analyst with a code for serial killing — Michael C. Hall auditioned against type after Six Feet Under and changed cable antiheroes forever.|Serial killer,Crime,American|50",
"Six Feet Under|2001|Alan Ball|a funeral-home family processing death weekly — the series finale is still rated the best-ending episode ever by multiple critic polls.|Family,Drama,American|55",
"The Shield|2002|Shawn Ryan|a corrupt strike-team cop unit in Farmington — its final episode is taught in screenwriting courses as the model antihero conclusion.|Police,Crime,American|50",
"Boardwalk Empire|2010|Terence Winter|Prohibition-era Atlantic City — the boardwalk set cost more per episode than most shows' full budgets and was later donated to a museum.|Gangster,Period,American|55",
"Treme|2010|David Simon|post-Katrina New Orleans musicians rebuilding — every gig featured was performed live on set by actual local bands.|Music,Drama,American|55",
"Bosch|2014|Eric Overmyer|Harry Bosch solving LA cold cases — Titus Welliver read all 20+ Connelly novels to prepare, and the author cameos as a bartender.|Detective,Crime,American|50",
"Yellowstone|2018|Taylor Sheridan|Montana's largest ranch vs everyone — Sheridan wrote the pilot in one weekend and cast Costner after seeing his Western gravitas in Open Range.|Western,Drama,American|55",
"Twin Peaks|1990|David Lynch|an FBI agent investigates a homecoming queen's murder — Lynch's cherry-stem scene made Sherilyn Fenn a legend, and the 'damn fine coffee' line predates specialty café culture.|Mystery,Surreal,American|45",
"The X-Files|1993|Chris Carter|FBI agents chasing aliens and cryptids — Carter pitched it as 'Moonlighting meets Kolchak', and the smoking man was cast from a non-actor grip recommendation.|Sci-fi,Mystery,American|45",
"Buffy the Vampire Slayer|1997|Joss Whedon|a cheerleader slays demons as high-school metaphor — 'Hush' uses 29 minutes of silence, and 'Once More, With Feeling' pioneered the TV musical episode.|Supernatural,Drama,American|45",
"Orphan Black|2013|Graeme Manson|clone conspiracies carried by Tatiana Maslany playing a dozen roles — she filmed opposite tennis balls and body doubles, winning an Emmy for it.|Sci-fi,Thriller,Canadian|45",
"Stargate SG-1|1997|Brad Wright|the military team exploring gate planets — the Air Force consulted on set so consistently they requested script edits on rank protocol.|Sci-fi,Military,American|42",
"Star Trek: The Next Generation|1987|Gene Roddenberry|the diplomacy-first Enterprise — Patrick Stewart hid his baldness concerns until Roddenberry replied 'in the 24th century, they don't care'.|Space,Sci-fi,American|45",
"Star Trek: Deep Space Nine|1993|Michael Piller|a stationary station exploring war religion and morality — its serial arc anticipated prestige TV by a decade.|Space,Sci-fi,American|45",
"The Twilight Zone|1959|Rod Roddenberry|anthology sci-fi moral puzzles — Rod Serling personally wrote 92 of 156 episodes, often rewriting censored scripts overnight.|Anthology,Sci-fi,American|25"
,
"Columbo|1971|Richard Levinson|the inverted whodunit showing the murder first — Peter Falk's raincoat was his own, and NBC kept trying to cancel it for a decade while audiences kept reviving it.|Detective,Mystery,American|75",
"Monk|2002|Andy Breckman|an OCD detective solving crimes despite disorder — Tony Shalhoub's hand-sanitizing tics were choreographed with OCD consultants to avoid mockery.|Detective,Comedy,American|45",
"Psych|2006|Steve Franks|a hyper-observant fake psychic and his reluctant partner — every episode hides a pineapple, a scavenger hunt fans still catalog obsessively.|Detective,Comedy,American|43",
"House M.D.|2004|David Shore|a diagnostician who solves patients like puzzles — the show's medical mysteries are peer-reviewed enough that doctors publish corrections in journals.|Medical,Drama,American|45",
"Scrubs|2001|Bill Lawrence|hospital interns narrated like fantasy daydreams — real doctors praised its accuracy on residency burnout, and med schools use clips for empathy training.|Medical,Comedy,American|22",
"M*A*S*H|1972|Larry Gelbart|Korean War surgeons balancing farce and mortality — its finale drew 106 million viewers, a record that stood for decades until the Super Bowl.|War,Comedy,American|25",
"Cheers|1982|Glen Charles|the bar where everybody knows your name — NBC launched it last place and it ended its run as America's #1 show.|Bar,Comedy,American|25",
"Frasier|1993|David Angell|a radio psychiatrist navigating Seattle snobbery — Kelsey Grammer played the character for twenty consecutive years across two series.|Radio,Comedy,American|22",
"How I Met Your Mother|2005|Craig Thomas|a nine-year flashback framed as one dad's bedtime story — the yellow umbrella became such a fan symbol CBS hid it in other shows.|Sitcom,Romance,American|22",
"The Big Bang Theory|2007|Chuck Lorre|physicists vs practical life — David Saltzberg, a real physicist, vetted every whiteboard equation and added corrections weekly.|Geek,Comedy,American|20",
"Modern Family|2009|Christopher Lloyd|three branches of one family mockumentary — the show's writers room mapped birthdays and grudges on a wall-sized family tree for continuity.|Family,Comedy,American|22",
"The Fresh Prince of Bel-Air|1990|Andy Borowitz|Will Smith playing himself into Bel-Air royalty — Smith memorized the entire cast's lines out of nervousness during season one, earning early directing trust.|Sitcom,Comedy,American|22",
"Malcolm in the Middle|2000|Linwood Boomer|a gifted kid in a chaotic working-class family — the single-camera format with no laugh track helped normalize prestige comedy grammar.|Family,Comedy,American|22",
"Freaks and Geeks|1999|Paul Feig|1980 Michigan teens navigating cliques — canceled after 12 episodes, it launched Fey, Franco, Carell, and Rogen simultaneously.|Teen,Drama,American|44",
"Gilmore Girls|2000|Amy Sherman-Palladino|mother-daughter banter at 200 words per minute — the show holds a Guinness record for most words per TV script page.|Family,Drama,American|42",
"Veronica Mars|2004|Rob Thomas|a teen PI solving her best friend's murder — its Kickstarter film revival raised $5.7 million in hours, pioneering fan-funded revivals.|Mystery,Teen,American|42",
"Friday Night Lights|2006|Peter Berg|Texas football as civic religion — 'Clear eyes, full hearts' entered actual political speeches, to the showrunner's public annoyance.|Sports,Drama,American|45",
"Homeland|2011|Howard Gordon|a CIA officer convinced a returned POW is compromised — the show's bipolar depiction earned awards and criticism from veterans' groups alike, both cited in essays.|Spy,Thriller,American|55",
"24|2001|Joel Surnow|one day in real-time counterterrorism — the ticking-clock format required writers to map every minute on a wall-sized grid.|Counterterror,Thriller,American|45",
"Prison Break|2005|Paul Scheuring|an engineer tattoos an escape blueprint onto his body — the tattoo design was so intricate a real graphic artist needed months to finalize it.|Escape,Thriller,American|45"
,
],
)
DATA["anime"] = dict(
    cat="ANIME", prefix="anime", subtype="Anime", verb="Watch",
    items=[
"Death Note|2006|Madhouse|a notebook that kills anyone whose name is written in it falls to a bored genius — the potato-chip eating scene turned homework into a psychological thriller setpiece.|Thriller,Psychological,2000s|37",
"Fullmetal Alchemist: Brotherhood|2009|Bones|two brothers lose bodies seeking alchemy's taboo — the manga-following remake is so tightly plotted that fans rank it the #1 anime of all time routinely.|Shonen,Fantasy,2010s|24|64",
"Attack on Titan|2013|Wit Studio|humanity's last walled cities vs man-eating giants — the gear-based aerial combat was storyboarded by animators studying parkour footage frame by frame.|Action,Dark Fantasy,2010s|24|87",
"Steins;Gate|2011|White Fox|a microwave that texts the past unravels causality — its wordcount-heavy visual novel source forced the anime to invent visual storytelling for pure exposition.|Sci-fi,Thriller,2010s|24|24",
"Code Geass|2006|Sunrise|an exiled prince gains mind-control via eye contact — every chess-like battle was plotted on physical boards by writers who admitted losing track mid-season.|Mech,Strategy,2000s|24|50",
"Monster|2004|Madhouse|a surgeon saves a boy who becomes a serial killer — Naoki Urasawa researched German hospitals for two years and the anime adapts all 74 manga chapters faithfully.|Thriller,Psychological,2000s|24|74",
"Hunter x Hunter|2011|Madhouse|a boy hunts his absent father via a lethal licensing exam — Togashi's hiatuses became so legendary the anime ended on an unfinished arc that fans still await.|Shonen,Adventure,2010s|23|148",
"One Piece|1999|Toei Animation|a rubber pirate king aspirant sails the Grand Line — Eiichiro Oda plans endings years ahead, telling only his editor in sealed envelopes.|Shonen,Adventure,2000s|24|1100",
"Naruto|2002|Studio Pierrot|a ninja orphan dreaming of leadership carries a fox demon — Kishimoto originally designed Naruto as a fox itself until an editor intervened.|Shonen,Ninja,2000s|23|220",
"Bleach|2004|Studio Pierrot|a teen substitutes as a soul reaper — Kubik drew character designs so fast that early chapters shipped double-length monthly.|Shonen,Supernatural,2000s|24|366",
"Dragon Ball Z|1989|Toei Animation|alien martial arts escalating to planet-shaking stakes — the 'over 9000' meme originated from a dub mistranslation of the original Japanese number.|Shonen,Martial arts,1990s|24|291",
"Cowboy Bebop|1998|Sunrise|bounty hunters drifting through a jazz-scored solar system — composer Yoko Kanno recorded the Seatbelts live, and the session tapes became collector items.|Space,Neo-noir,1990s|24|26",
"Samurai Champloo|2004|Manglobe|hip-hop beats meet Edo-period swordplay — the breakdance-inspired swordfighting was choreographed by an animator who studied street battles footage.|Samurai,Hip-hop,2000s|24|26",
"Trigun|1998|Madhouse|a pacifist gunslinger with a $$60 billion bounty — the 'love and peace' persona hides a backstory the anime only hints at until the final act.|Western,Sci-fi,1990s|24|26",
"Ghost in the Shell: Stand Alone Complex|2002|Production I.G|cyber-brain detectives chasing a ghost hacker — its 'Stand Alone Complex' term entered academic papers on memetics and copycat crime.|Cyberpunk,Sci-fi,2000s|25|52",
"Psycho-Pass|2012|Production I.G|a system scores your criminal intent pre-crime — Gen Urobuchi wrote the villain first, designing a world where his logic could genuinely tempt society.|Cyberpunk,Thriller,2010s|23|22",
"Ergo Proxy|2006|Manglobe|dome-city androids gaining self-awareness in a wasteland — its Latin-heavy episode titles were chosen to mirror the philosophical texts each episode riffs on.|Cyberpunk,Philosophical,2000s|23|23",
"Serial Experiments Lain|1998|Triangle Staff|a quiet girl merges with the Wired internet — its 'present day, present time' opening predicted social-media identity dissolution decades early.|Cyberpunk,Psychological,1990s|24|13",
"Made in Abyss|2017|Kinema Citrus|children descend a bottomless pit of wonders and horrors — the cute art style vs body-horror contrast is deliberate, and the composer scored it like a nature documentary.|Fantasy,Adventure,2010s|25|13",
"Vinland Saga|2019|Wit Studio|Viking revenge deconstructed into pacifism — the farm arc was considered unfilmable by studios until director Shūhei Yabuta proved slow-burn pacing could hold viewers.|Historical,Viking,2010s|24|24",
"Dr. Stone|2019|TMS Entertainment|science rebuilds civilization from stone-age zero — every invention shown was vetted by a scientific advisory board and replicated practically where possible.|Sci-fi,Adventure,2010s|24|24",
"Re:Zero|2016|White Fox|a boy loops death in a fantasy world without explanation — its Return by Death mechanic forces viewers to dread each save point like the protagonist does.|Isekai,Thriller,2010s|24|25",
"KonoSuba|2016|Studio Deen|isekai parody where the party is functionally useless — the writer framed it as punishment for choosing a goddess who mocks him constantly.|Isekai,Comedy,2010s|24|10"
,
"Overlord|2015|Madhouse|a gamer stuck in his MMO villain avatar commits to evil convincingly — the show asks what happens when role-play stops feeling like pretend.|Isekai,Dark Fantasy,2010s|24|13",
"That Time I Got Reincarnated as a Slime|2018|8bit|a salaryman respawns as the weakest monster and builds a nation — its nation-building arcs inspired real economics essays about governance systems in fiction.|Isekai,Fantasy,2010s|24|24",
"Sword Art Online|2012|A-1 Pictures|ten thousand players trapped in a death-game MMO — the 'SAO incident' became shorthand in tech journalism for VR safety debates.|Isekai,VR,2010s|24|25",
"Log Horizon|2013|Satelight|trapped gamers build law, economy, and cuisine — its food-cooking scenes used real chef consultation, spawning cookable recipe guides.|Isekai,Strategy,2010s|24|25",
"No Game No Life|2014|Madhouse|step-siblings conquer a world where everything resolves via games — its rainbow palette is a deliberate rejection of standard isekai color grading.|Isekai,Games,2010s|24|12",
"Your Lie in April|2014|A-1 Pictures|a pianist who can't hear his own playing meets a free-spirited violinist — the performances were rotoscoped from real concert pianists' hands.|Music,Romance,2010s|22|22",
"Clannad: After Story|2008|Kyoto Animation|high-school romance continues into marriage and parenthood — its Ushio arc is frequently cited as the saddest television sequence in anime history.|Drama,Romance,2000s|24|24",
"Angel Beats!|2010|P.A. Works|dead teens wage war against God in a purgatory high school — the girl-band insert songs charted commercially in Japan.|Supernatural,Drama,2010s|24|13",
"Anohana|2011|A-1 Pictures|childhood friends reunite to grant a dead girl's wish — the ghost's visible-only-to-one conceit doubles as a grief metaphor the show never explains.|Drama,Supernatural,2010s|22|11",
"Violet Evergarden|2018|Kyoto Animation|a child soldier learns emotions by writing letters — Kyoto Animation's backgrounds reference real European locations frame-by-frame.|Drama,Fantasy,2010s|24|13",
"K-On!|2009|Kyoto Animation|a light-music club mostly drinks tea — its instrument accuracy made Yamaha release official character-model guitars.|Slice of life,Music,2000s|24|39",
"The Melancholy of Haruhi Suzumiya|2006|Kyoto Animation|a godlike girl unknowingly reshapes reality from boredom — its Endless Eight arc repeated the same episode eight times, testing viewer loyalty as art.|Sci-fi,Slice of life,2000s|24|28",
"Toradora!|2008|J.C. Staff|a gentle delinquent and a tiny tsundere ally to woo each other's best friends — the Christmas chapter is a perennial Japanese rewatch tradition.|Romance,Comedy,2000s|24|25",
"Kaguya-sama: Love Is War|2019|A-1 Pictures|student council elites treat confession as strategic defeat — its narrator announces psychological warfare rules like a sports broadcast.|Romance,Comedy,2010s|24|12",
"Haikyu!!|2014|Production I.G|a short volleyball player refusing to stay benched — the animation team filmed real national-level matches and traced momentum arcs for rallies.|Sports,Shonen,2010s|24|85",
"Slam Dunk|1993|Toei Animation|a delinquent joins basketball for a girl and stays for glory — Takehiko Inoue's court anatomy drawings are referenced in real coaching clinics.|Sports,Shonen,1990s|22|101",
"Yuri!!! on Ice|2016|MAPPA|figure skaters, anxiety, and a coaching bond that went global — its routines were choreographed by real Olympic program designer Kenji Miyamoto.|Sports,Romance,2010s|24|12",
"Ping Pong the Animation|2014|Yuasa Masaaki|table-tennis prodigies drawn in raw sketch style — Masaaki Yuasa deliberately ignored 'clean' linework, and the result won animation awards worldwide.|Sports,Art house,2010s|22|11",
"Megalo Box|2018|TMS Entertainment|boxing in a retro-future slum — produced as a 50th-anniversary homage to Ashita no Joe with intentionally gritty digital grain.|Boxing,Sci-fi,2010s|23|13"
,
"Mob Psycho 100|2016|Bones|a psychic middle-schooler just wants normalcy — ONE's webcomic scribbles were elevated by animation that treats every psychic burst as a music video.|Supernatural,Comedy,2010s|24|37",
"One Punch Man|2015|Madhouse|a hero defeats everything in one punch and is bored stiff — the fight animation budget spikes only when Saitama isn't fighting, a deliberate subversion.|Superhero,Comedy,2010s|24|24",
"Fire Force|2019|David Production|firefighters battle spontaneous human combustion — the flame effects were hand-keyframed to mimic real combustion patterns studied from footage.|Shonen,Supernatural,2010s|24|48",
"Soul Eater|2008|Bones|weapons are people partnered with wielders at a academy — its gothic Bauhaus backgrounds were painted to look like a Tim Burton musical stage.|Shonen,Fantasy,2000s|24|51",
"Blue Exorcist|2011|A-1 Pictures|Satan's twin sons attend an exorcism academy — Kazue Katō designed Rin's blue flame to look warm rather than menacing, on purpose.|Supernatural,Shonen,2010s|24|25",
"Bungo Stray Dogs|2016|Bones|literary legends reborn as superpowered detectives — every ability references its namesake author's actual bibliography.|Supernatural,Mystery,2010s|24|61",
"Jujutsu Kaisen|2020|MAPPA|cursed energy born from negative emotion — Gege Akutami storyboarded fights like kung-fu choreography, and MAPPA's sakuga went viral frame by frame.|Shonen,Supernatural,2020s|23|47",
"Demon Slayer|2019|ufotable|Taisho-era swordsman vs demons with breathing techniques — the Mugen Train film became Japan's highest-grossing movie of all time.|Shonen,Supernatural,2010s|23|55",
"Chainsaw Man|2022|MAPPA|a debt-slave boy fused with a chainsaw devil hunting devils — Fujimoto's cinematic paneling pushed MAPPA toward film-style framing throughout.|Dark fantasy,Shonen,2020s|24|12",
"Dorohedoro|2020|MAPPA|a man with a caiman head searches a sorcerer slum for his face — Q Hayashida's grime aesthetic survived adaptation intact via CG-assisted textures.|Dark fantasy,Action,2020s|24|12",
"Banana Fish|2018|MAPPA|a NYC gang leader and a Japanese photographer unravel a CIA experiment — the Vietnam-era conspiracy manga was updated carefully to modern Manhattan.|Crime,Drama,2010s|24|24",
"Grand Blue|2018|Zero-G|college diving club fueled by drinking games — its drunk-face animation style shifts deliberately into photorealistic comedy close-ups.|Comedy,College,2010s|24|12",
"Gintama|2006|Sunrise|a samurai freelancer in alien-occupied Edo breaks every fourth wall — its parodies were so bold publishers asked permission retroactively.|Comedy,Samurai,2000s|24|367",
"Nichijou|2011|Kyoto Animation|absurd everyday life escalated to operatic scale — its 'daiku' wooden-sandal kick gag took months of storyboarding for three seconds.|Comedy,Surreal,2010s|24|26",
"Great Teacher Onizuka|1999|Studio Pierrot|an ex-biker becomes a homeroom teacher by chaos — the manga sold 50 million copies and changed how Japanese TV portrayed teachers.|School,Comedy,1990s|24|43",
"Assassination Classroom|2015|Lerche|students must assassinate their tentacle teacher who is also the best educator alive — the premise doubles as a satire of exam culture.|School,Action,2010s|23|22",
"My Hero Academia|2016|Bones|a quirkless boy inherits the top hero's power — Horikoshi modeled All Might on classic American comics and draws him in western panel layouts.|Superhero,Shonen,2010s|24|138",
"Black Clover|2017|Pierrot|a magic-less boy aims for wizard king through grit alone — its opening arc was widely criticized, then the show earned a reputation as a slow-burn comeback.|Shonen,Fantasy,2010s|23|170",
"Seven Deadly Sins|2014|A-1 Pictures|knights branded traitors reunite as bar owners — Suzuki Nakaba's giant-vs-human scale battles forced the animators to rethink perspective shots.|Shonen,Fantasy,2010s|24|100",
"Magi|2012|A-1 Pictures|Alibaba and Aladdin adventure through dungeon economies — Ohtaka's worldbuilding mixes One Thousand and One Nights geography with trade-route politics.|Fantasy,Adventure,2010s|24|50"
,
"Future Diary|2011|Asread|twelve diary holders battle to become god — Yuno Gasai's yandere portrait defined an archetype so hard it's now a psychology-paper citation.|Thriller,Psychological,2010s|23|26",
"Another|2012|P.A. Works|a cursed classroom where classmates die mysteriously — the umbrella staircase scene is studied as a masterclass in dread pacing.|Horror,Mystery,2010s|24|12",
"Higurashi: When They Cry|2006|Studio Deen|a village festival repeats a massacre across timelines — the sound of cicadas was engineered to induce unease, becoming the show's signature.|Horror,Mystery,2000s|24|26",
"Elfen Lied|2004|ARMS|a mutant girl escapes a lab wearing only a helmet — its extreme violence vs innocence contrast divided critics and defined 2000s edgy anime.|Horror,Supernatural,2000s|24|13",
"Parasyte: The Maxim|2014|Madhouse|aliens possess human hands and heads — the manga's ecological warning was updated with modern antibiotic-resistance parallels.|Sci-fi,Horror,2010s|23|24",
"Paranoia Agent|2004|Madhouse|an urban legend on rollerblades attacks Tokyo's liars — Satoshi Kon stitched four abandoned project ideas into one anthology-thriller.|Psychological,Surreal,2000s|23|13",
"Baccano!|2007|Brain's Base|immortal mafia aboard a 1930s transcontinental train — its non-linear timeline jumps across decades, resolved like a magic trick in episode 14.|Mafia,Supernatural,2000s|24|16",
"Durarara!!|2010|Brain's Base|Ikebukuro's gangs narrated by an anonymous chatroom — the headless rider Celty delivers packages, and her helmet became a cosplay icon.|Urban fantasy,Supernatural,2010s|24|36",
"Fate/Zero|2011|ufotable|seven mage families summon historical heroes to war — Gen Urobuchi wrote it as a tragedy where every faction is right and doomed.|Action,Fantasy,2010s|24|25",
"Spice and Wolf|2008|Imagin|a traveling merchant partners with a wheat goddess — its medieval economics were accurate enough to be cited in currency-history blogs.|Fantasy,Romance,2000s|24|24",
"Cardcaptor Sakura|1998|Madhouse|a girl captures sentient Clow cards — CLAMP designed outfits for every capture, and the costume changes became the series' signature.|Magical girl,Fantasy,1990s|24|70",
"Sailor Moon|1992|Toei Animation|middle-school girls transform into planetary guardians — Takeuchi's sailor uniforms were inspired by her school's own uniform regulations.|Magical girl,Romance,1990s|24|200",
"Yu Yu Hakusho|1992|Studio Pierrot|a delinquent dies saving a kid and becomes spirit detective — Togashi wrote the Dark Tournament arc while hospitalized, dictating notes from bed.|Shonen,Supernatural,1990s|24|112",
"Inuyasha|2000|Sunrise|a schoolgirl falls through a well into warring-states Japan — Rumiko Takahashi mixed genuine Sengoku history with yokai folklore.|Fantasy,Romance,2000s|24|167",
"Ranma ½|1989|Studio Deen|a martial artist cursed to switch gender with cold water — Takahashi's gender-flip comedy broke ground for mainstream trans themes on TV.|Comedy,Martial arts,1980s|23|161",
"Detective Conan|1996|TMS Animation|a high-school detective poisoned into a child's body solves murders — Gosho Aoyama maintains a spreadsheet tracking every case's legal aftermath.|Detective,Mystery,1990s|24|1100",
"Cells at Work!|2018|David Production|your body as an office of anthropomorphized cells — medical consultants verified each pathogen raid, and real immunology classes adopted clips.|Educational,Comedy,2010s|23|13",
"Silver Spoon|2013|A-1 Pictures|a city genius enrolls in agricultural high school — Hiromu Arakawa drew from her own dairy-farm childhood, including calf-birth details.|Slice of life,Comedy,2010s|23|22",
"Barakamon|2014|Kinema Citrus|a calligrapher exiled to an island learns looseness from village kids — Satsuki Yoshino based Handa's breakdown on her own critique trauma.|Slice of life,Comedy,2010s|23|12",
"Flying Witch|2016|J.C. Staff|a witch moves in with relatives and does nothing dramatic — its 'iyashikei' pacing treats grocery shopping as gently as spellcasting.|Slice of life,Supernatural,2010s|24|12"
,
"Mushishi|2005|Artland|a wanderer treats ailments caused by primitive lifeforms — each episode is a standalone folk tale, and the green palette was tuned to feel like humidity.|Supernatural,Folklore,2000s|24|46",
"Natsume's Book of Friends|2008|Brain's Base|a boy inherits a book of yokai contracts and returns names — its melancholy warmth made it a comfort-watch staple across three decades of fans.|Supernatural,Slice of life,2000s|24|74",
"Kino's Journey|2003|A.C.G.T|a traveler spends three days per country, no more — each nation is a thought experiment about laws, freedom, or technology.|Travel,Philosophical,2000s|24|13",
"Girls' Last Tour|2017|White Fox|two girls drift through a dead megastructure city — the manga artist wrote it after a depression diagnosis, and the show preserves that quietness.|Post-apocalyptic,Slice of life,2010s|23|12",
"A Place Further Than the Universe|2018|Madhouse|high-school girls join a civilian Antarctic expedition — the production consulted Japan's actual Antarctic program for ship and gear accuracy.|Adventure,Drama,2010s|23|13",
"Non Non Biyori|2013|Silver Link|five students share one rural schoolhouse — its cicada-and-grass ambience was recorded in real Japanese countryside for authentic silence.|Slice of life,Comedy,2010s|24|36",
"Hinamatsuri|2018|feel.|a yakuza adopts a psychic girl who crash-lands in his apartment — its homelessness subplot is unexpectedly one of anime's most empathetic portrayals.|Comedy,Sci-fi,2010s|23|12",
"March Comes in Like a Lion|2016|Shaft|a pro shogi player battles depression as much as opponents — Shaft's watercolor interludes visualize loneliness better than dialogue manages.|Drama,Sports,2010s|23|62",
"Chihayafuru|2011|Madhouse|karuta card-snatching as a competitive sport — the poetry memorization sparked real-world karuta club surges in Japanese high schools.|Sports,Romance,2010s|23|75",
"Run with the Wind|2018|Production I.G|ten university students train for the Hakone Ekiden relay — the real marathon's course maps appear on screen accurately, kilometer by kilometer.|Sports,Drama,2010s|23|23",
"Golden Kamuy|2018|Geno Studio|post-Russo-Japanese-war treasure hunt through Hokkaido — every Ainu recipe shown is documented, and fans cook them from screenshots.|Historical,Adventure,2010s|23|49",
"Mob Psycho 100 II|2019|Bones|the sequel that turned sakuga into mainstream vocabulary — its 'confession' episode is analyzed in animation-storyboard courses.|Supernatural,Drama,2010s|24|13",
"Erased|2016|A-1 Pictures|a mangaker travels back to childhood to stop kidnappings — its Satoru Kayo arc sparked debates on time-travel rules that still fill forums.|Mystery,Thriller,2010s|23|12",
"The Promised Neverland|2019|CloverWorks|orphanage escape planned like a heist — Kaiu Shirai's tension math gives every episode a countdown, even in flashbacks.|Thriller,Sci-fi,2010s|23|23",
"Beastars|2019|Orange|carnivore-herbivore tensions at a boarding school — Orange blended CG fur physics with 2D faces, a technique the studio refined specifically here.|Drama,Fantasy,2010s|23|24",
"Demon Slayer: Mugen Train Arc|2021|ufotable|the film re-cut as seven TV episodes with a new first episode — it holds records as both a movie and a series launch.|Shonen,Supernatural,2020s|23|7",
"Odd Taxi|2021|OLM|a walrus taxi driver connects missing-person threads in urban Japan — its hip-hop outro won awards, and the mystery's logic puzzle rewards rewatching.|Mystery,Drama,2020s|23|13",
"Ranking of Kings|2021|Wit Studio|a deaf prince and a shadow assassin befriend across kingdoms — the fairytale art style hides one of anime's sharpest betrayal-redemption plots.|Fantasy,Drama,2020s|23|23",
"Spy x Family|2022|Wit Studio/CloverWorks|a spy, assassin, and telepath fake a family for missions — Anya's peanut reactions became global meme currency overnight.|Comedy,Action,2020s|24|37",
"Bocchi the Rock!|2022|CloverWorks|a socially anxious guitarist joins a band — her meltdown animations riff on experimental film techniques rarely seen in comedy.|Music,Comedy,2020s|23|12"
,
],
)
# ══════════════════════════ RUNNER ══════════════════════════
def main():
    keys = sys.argv[1:] or list(DATA.keys())
    taken = load_all_names()
    report = []
    for key in keys:
        cfg = DATA[key]
        path = os.path.join(TOPICS, f"{key}.json")
        current = json.load(open(path))
        before = len(current)
        need = TARGET - before
        if need <= 0:
            report.append(f"{key}: already {before} ≥ {TARGET}, skipped")
            continue
        added = 0
        for line in cfg["items"]:
            if added >= need:
                break
            t = make_topic(cfg["cat"], cfg["prefix"], cfg["subtype"], cfg["verb"], line, taken)
            if t is None:
                continue
            if any(x["id"] == t["id"] for x in current):
                continue
            current.append(t)
            added += 1
        json.dump(current, open(path, "w"), indent=2, ensure_ascii=False)
        report.append(f"{key}: {before} → {before + added} (+{added})")
    print("\n".join(report))

if __name__ == "__main__":
    main()
