#!/usr/bin/env python3
"""Enrich series.json batch 3 (web-verified, small batches per project workflow).

Adds `synopsis` + `episodes` (season/episode/title/summary) to five more
tier-1 series: Breaking Bad, Stranger Things, Game of Thrones, The Wire,
The Sopranos (first seasons, titles verified against episode guides).
Rerunnable: entries already carrying `episodes` are skipped unless FORCE=1.
Authored text avoids em/en dashes on purpose.
"""
import json
import os

PATH = "data/topics/series.json"
FORCE = os.environ.get("FORCE") == "1"

CONTENT = {
    "series-breaking-bad": {
        "synopsis": (
            "Vince Gilligan's crime drama follows Walter White, a high school "
            "chemistry teacher in Albuquerque who is diagnosed with inoperable "
            "lung cancer and turns to cooking methamphetamine to secure his "
            "family's future. Bryan Cranston plays Walt, whose quiet desperation "
            "curdles into a ruthless alter ego, Heisenberg, and Aaron Paul plays "
            "Jesse Pinkman, his former student and reluctant partner. The show "
            "tracks the slow corruption of a good man across five seasons, from "
            "a single batch in an RV to a drug empire, a family torn apart and "
            "a body count that follows him home. Widely regarded as one of the "
            "greatest television dramas ever made, it won sixteen Emmy Awards "
            "and a place in the culture that its imitators have never matched."
        ),
        "episodes": [
            {"season": 1, "number": 1, "title": "Pilot",
             "summary": "Walter White, a fifty-year-old high school chemistry teacher, learns he has inoperable lung cancer. On a DEA ride-along with his brother-in-law Hank he spots Jesse Pinkman, a former student, and decides to cook meth to secure his family's future before he dies."},
            {"season": 1, "number": 2, "title": "Cat's in the Bag...",
             "summary": "Walt and Jesse kill Emilio and take the surviving witness, Krazy-8, prisoner in Jesse's basement. They try to dissolve Emilio's body in a bathtub with hydrofluoric acid, which eats straight through the floor, and the mess is only the beginning of their problems."},
            {"season": 1, "number": 3, "title": "...And the Bag's in the River",
             "summary": "Walt prepares to free Krazy-8 but catches him with a shard of broken plate, a weapon meant for Walt's throat. He strangles the man in the basement and returns home to lie to Skyler about where the gambling money came from."},
            {"season": 1, "number": 4, "title": "Cancer Man",
             "summary": "Walt tells the family about his illness and begins chemotherapy, while his pride keeps him from accepting help. Jesse is thrown out by his parents, and Walt lashes out at a group of loud young men, using his car to make a point."},
            {"season": 1, "number": 5, "title": "Gray Matter",
             "summary": "Flashbacks show Walt walking away from Gray Matter Technologies, the company he co-founded and sold his share of for five thousand dollars. Gretchen and Elliot Schwartz offer to pay for his treatment, and after a painful visit Walt finally agrees."},
            {"season": 1, "number": 6, "title": "Crazy Handful of Nothin'",
             "summary": "Jesse tries to sell the meth alone and is beaten and cheated by the dealer Tuco. Walt confronts Tuco in his office with a bag of fulminated mercury, blowing the place up to prove a point and walking out with his money."},
            {"season": 1, "number": 7, "title": "A No-Rough-Stuff-Type Deal",
             "summary": "Tuco takes Walt and Jesse to the desert for a deal and kills his own man in front of them. Walt prepares ricin but cannot use it, and the episode ends with the unstable Tuco forcing the pair into his trunk, their freedom gone."},
        ],
    },
    "series-stranger-things": {
        "synopsis": (
            "The Duffer Brothers' homage to 1980s horror and Spielbergian "
            "adventure is set in Hawkins, Indiana, where the disappearance of "
            "twelve-year-old Will Byers uncovers a secret government lab, a "
            "telekinetic girl with a shaved head and a number tattoo, and a "
            "parallel dimension called the Upside Down. Millie Bobby Brown "
            "plays Eleven, the girl who escaped the lab and holds the key to "
            "finding Will, while the boys' Dungeons and Dragons campaign turns "
            "out to have predicted the monster they are really facing. The "
            "show's first season became a phenomenon on its 2016 debut, "
            "blending small-town nostalgia, body horror and found-family "
            "warmth into a story that keeps expanding, and its kids grew up "
            "on screen across five seasons."
        ),
        "episodes": [
            {"season": 1, "number": 1, "title": "Chapter One: The Vanishing of Will Byers",
             "summary": "November 6, 1983: after a night of Dungeons and Dragons, Will Byers vanishes on his way home. Mike, Dustin and Lucas find a strange girl in the woods, while a scientist flees the Hawkins National Laboratory in terror."},
            {"season": 1, "number": 2, "title": "The Weirdo on Maple Street",
             "summary": "Mike hides the girl, who answers only to Eleven, in his basement, where she proves she can find people. Joyce wires her house with Christmas lights and hears Will breathing through the wall."},
            {"season": 1, "number": 3, "title": "Holly, Jolly",
             "summary": "Joyce builds a wall of lights that Will speaks through, spelling out words one letter at a time. Eleven uses the board to contact Will from the other side, while Barb goes missing from Steve's party."},
            {"season": 1, "number": 4, "title": "The Body",
             "summary": "A body is pulled from the quarry and the town mourns Will, but the corpse is a fake staged by the lab. Hopper traces its origins, and Eleven flips a van to save the boys from a gang of bullies."},
            {"season": 1, "number": 5, "title": "The Flea and the Acrobat",
             "summary": "The boys explain the Upside Down through the flea and the acrobat: there may be another way in. Hopper breaks into the lab and sees the gate, while Joyce and Hopper cut through the wall of her house."},
            {"season": 1, "number": 6, "title": "The Monster",
             "summary": "The creature attacks the Byers house and drags Joyce and Hopper through the gate into the Upside Down. Nancy and Jonathan hunt the monster in the woods, and Eleven's flashes of the lab reveal the truth about her past."},
            {"season": 1, "number": 7, "title": "The Bathtub",
             "summary": "The boys build a saltwater tank so Eleven can find Will, and she locates him hiding in the Upside Down. The lab's agents close in as Eleven pushes her powers to the limit."},
            {"season": 1, "number": 8, "title": "The Upside Down",
             "summary": "The boys and Joyce pull Will back through the gate, barely alive. Nancy and Jonathan hold off the monster at the Byers house, and Eleven confronts it in the school gym, destroying it and vanishing in the blast. A year later the gang plays D and D again, and a glimpse of the Upside Down promises more."},
        ],
    },
    "series-game-of-thrones": {
        "synopsis": (
            "HBO's adaptation of George R. R. Martin's A Song of Ice and Fire "
            "turned the fantasy epic into the biggest television series of its "
            "era. In the Seven Kingdoms of Westeros, noble houses scheme for "
            "the Iron Throne while winter and something far older gather beyond "
            "the Wall in the north, and across the Narrow Sea a deposed king's "
            "daughter raises dragons from stone eggs. The show's first season "
            "established its signature ruthlessness, killing off beloved "
            "characters without warning and making every alliance a knife's "
            "edge away from betrayal. Its eight seasons redefined event "
            "television, broke viewing records around the world, and ended in "
            "a finale so divisive it is still argued about today."
        ),
        "episodes": [
            {"season": 1, "number": 1, "title": "Winter Is Coming",
             "summary": "Ned Stark executes a deserter from the Wall and returns to Winterfell with news that could change everything. Bran climbs the tower and catches Queen Cersei with her brother Jaime, who pushes him from the window to keep the secret."},
            {"season": 1, "number": 2, "title": "The Kingsroad",
             "summary": "Ned leaves for King's Landing to serve as the King's Hand, Jon Snow heads north to the Wall, and an assassin sent to finish Bran is stopped by Catelyn. Tyrion travels with Jon and is taken hostage by Catelyn."},
            {"season": 1, "number": 3, "title": "Lord Snow",
             "summary": "Ned takes his seat on the small council and learns how much Robert's realm is in debt. Bran wakes from his coma, Jon begins his training at Castle Black, and Arya starts sword lessons with the dancing master."},
            {"season": 1, "number": 4, "title": "Cripples, Bastards, and Broken Things",
             "summary": "Ned investigates the death of the previous Hand, Jon Arryn, following his trail through the book of noble lineages. Tyrion visits the Wall, and the new recruit Samwell Tarly arrives, mocked by everyone."},
            {"season": 1, "number": 5, "title": "The Wolf and the Lion",
             "summary": "Ned confronts Cersei with the truth about her children and Jaime attacks him in the street, killing his men and leaving Ned wounded. Catelyn seizes Tyrion at an inn and carries him to the Eyrie for trial."},
            {"season": 1, "number": 6, "title": "A Golden Crown",
             "summary": "At the Eyrie, Tyrion demands a trial by combat and the sellsword Bronn fights for him and wins. Across the sea, Viserys threatens Daenerys one time too many and Khal Drogo pours a golden crown over his head."},
            {"season": 1, "number": 7, "title": "You Win or You Die",
             "summary": "Ned tells Cersei he knows her secret and gives her a chance to flee, a mercy that costs him. Robert is mortally gored by a boar, names Ned regent, and Littlefinger's knife finds Ned's back the moment he needs a friend."},
            {"season": 1, "number": 8, "title": "The Pointy End",
             "summary": "Ned is arrested for treason and the Lannisters take control of the capital. Robb calls the banners of the north and marches south to rescue his father, while Arya escapes the castle with the recruiter Yoren."},
            {"season": 1, "number": 9, "title": "Baelor",
             "summary": "Robb wins a great victory at the Whispering Wood and takes Jaime prisoner. To protect Sansa, Ned confesses to treason, but Joffrey orders his head taken anyway, and the sword falls before anyone can stop it."},
            {"season": 1, "number": 10, "title": "Fire and Blood",
             "summary": "The North, in grief and fury, proclaims Robb the King in the North. Daenerys walks into Khal Drogo's funeral pyre and emerges unburnt, cradling three newborn dragons, as winter and war settle over Westeros."},
        ],
    },
    "series-the-wire": {
        "synopsis": (
            "David Simon's masterpiece, drawn from his years as a Baltimore "
            "crime reporter, is a novel for television: a season-long story "
            "about one institution and the people caught inside it. Season one "
            "follows a police detail assembled to take down the Barksdale drug "
            "organization, and refuses to treat cops, dealers or addicts as "
            "caricatures, showing the game from every side of the wire. The "
            "show's cast of then-unknowns, Dominic West, Idris Elba, Michael "
            "K. Williams and Andre Royo among them, became stars on its "
            "strength, and its five seasons, each devoted to a different "
            "institution, the drug trade, the docks, city politics, schools "
            "and the press, are widely regarded as the most honest portrait "
            "of American urban life ever put on television."
        ),
        "episodes": [
            {"season": 1, "number": 1, "title": "The Target",
             "summary": "At the murder trial of D'Angelo Barksdale, the key witness recants on the stand, and homicide detective Jimmy McNulty points the judge at the Barksdale organization. The judge pressures the department to open a case the brass would rather ignore."},
            {"season": 1, "number": 2, "title": "The Detail",
             "summary": "The detail assembles in a dusty basement with a stolen budget and no real support. McNulty meets the team, and the young dealers Wallace and Bodie run the towers while the Barksdales figure out who is coming at them."},
            {"season": 1, "number": 3, "title": "The Buys",
             "summary": "The detail starts making undercover buys with the addict Bubbles, building a case brick by brick. Stringer Bell runs the business side of the organization while Avon Barksdale keeps the street, and a witness is found dead."},
            {"season": 1, "number": 4, "title": "Old Cases",
             "summary": "The detail reopens the murder of the witness who recanted at trial, and D'Angelo teaches Wallace and Bodie the game of chess, where the king stays the king. McNulty and Kima pore over the old file for a connection."},
            {"season": 1, "number": 5, "title": "The Pager",
             "summary": "The detail gets court approval to monitor the pagers and begins to map the organization's calls. A shooting at the towers puts the Barksdales on edge, and the wiretap case starts to take real shape."},
            {"season": 1, "number": 6, "title": "The Wire",
             "summary": "The wiretap comes online and the detail hears the organization's voice for the first time. D'Angelo is locked up again after the towers shooting, and McNulty's ex-wife complicates his life outside the case."},
            {"season": 1, "number": 7, "title": "One Arrest",
             "summary": "To keep the wiretap alive the detail must show a result, so they arrest a low-level dealer and charge D'Angelo, who refuses to give up his crew. The Barksdales bring in lawyers and begin to smell the investigation."},
            {"season": 1, "number": 8, "title": "Lessons",
             "summary": "The detail learns how the organization is really run while the Barksdales adapt to the pressure. McNulty pushes for more resources, and the case begins to cost people on both sides of the wire."},
            {"season": 1, "number": 9, "title": "Game Day",
             "summary": "The detail watches a football game that settles a bet between rival dealers, and the wiretap catches something that could break the case. McNulty learns how little his bosses want the investigation to succeed."},
            {"season": 1, "number": 10, "title": "The Cost",
             "summary": "The case eats money and lives: a police shooting and a dealer's death raise the stakes. D'Angelo is sent to prison, where the organization's code puts him in a cell with his own family's protection."},
            {"season": 1, "number": 11, "title": "The Hunt",
             "summary": "Avon and Stringer retaliate against their rivals and the detail races to keep up. Omar Little, the man who robs dealers, makes his presence felt, and the body count pulls the case toward its end."},
            {"season": 1, "number": 12, "title": "Cleaning Up",
             "summary": "The Barksdales clean house to protect themselves: Wallace, the young dealer who broke under the weight of what he had done, is killed by his own friends, and Wee-Bey prepares to take the fall for the organization."},
            {"season": 1, "number": 13, "title": "Sentencing",
             "summary": "The case collapses when the wiretap evidence is thrown out, and the Barksdales walk largely untouched. D'Angelo takes twenty years rather than break the code, McNulty is exiled to a harbor boat, and the game goes on."},
        ],
    },
    "series-the-sopranos": {
        "synopsis": (
            "David Chase's drama about a New Jersey mob boss in therapy "
            "changed television. James Gandolfini plays Tony Soprano, "
            "struggling to hold together his two families, the one at home "
            "with his wife Carmela and their children, and the one in the "
            "business, his uncle Junior, his crew and the FBI circling "
            "outside. Tony's sessions with Dr. Jennifer Melfi ground the "
            "show's violence in psychology, the panic attacks, the dreams, "
            "the mother he cannot escape, and the show balanced brutality "
            "with black comedy and genuine tenderness. Its six seasons "
            "redefined what a drama could do, and its final scene, a cut "
            "to black that still divides viewers, became the most "
            "discussed ending in television history."
        ),
        "episodes": [
            {"season": 1, "number": 1, "title": "The Sopranos",
             "summary": "Tony Soprano collapses during a panic attack after a family of ducks leaves his pool, and his doctor sends him to a psychiatrist. In therapy with Dr. Melfi he begins to untangle his mother Livia, his uncle Junior and the weight of running the family business."},
            {"season": 1, "number": 2, "title": "46 Long",
             "summary": "Tony struggles with his mother's demands and his uncle's resentment, while the crew chases a thief who has been ripping off their trucks. Melfi presses Tony on why he came to therapy at all."},
            {"season": 1, "number": 3, "title": "Denial, Anger, Acceptance",
             "summary": "Junior angers Tony by using one of Livia's friends as a messenger, and the two circle each other without quite going to war. Tony lies to Melfi about his business, and the panic attacks keep coming."},
            {"season": 1, "number": 4, "title": "Meadowlands",
             "summary": "Meadow's troubles at school and AJ's wildness strain Tony's home life. The FBI steps up its surveillance of the Sopranos, and Tony's version of his own life starts to crack under Melfi's questions."},
            {"season": 1, "number": 5, "title": "College",
             "summary": "Tony takes Meadow to visit colleges in Maine and spots a man from the old neighborhood who testified against his crew and entered witness protection. He kills the man with his bare hands, while Carmela wrestles with what she knows."},
            {"season": 1, "number": 6, "title": "Pax Soprana",
             "summary": "Tony brokers peace with Junior, making his uncle the official boss while keeping the real power for himself. After Melfi is raped, Tony finds the attacker and beats him nearly to death."},
            {"season": 1, "number": 7, "title": "Down Neck",
             "summary": "AJ is diagnosed with attention deficit disorder and Tony's own childhood comes flooding back, his father's stories and his mother's coldness. Melfi begins to see the shape of the man under the boss."},
            {"season": 1, "number": 8, "title": "The Legend of Tennessee Moltisanti",
             "summary": "Christopher broods about being overlooked and starts writing a screenplay, while the crew buzzes about who is and is not a made man. A hit goes wrong and the heat comes down on the family."},
            {"season": 1, "number": 9, "title": "Boca",
             "summary": "Junior's secret romance in Florida becomes a joke after the FBI gets video of him, and Tony lets the news slip to Carmela, humiliating his uncle in front of the whole family. Melfi's attacker turns up dead."},
            {"season": 1, "number": 10, "title": "A Hit Is a Hit",
             "summary": "Christopher and Adriana try to break into the music business with a rapper, and the money gets laundered in plain sight. Tony and Carmela clash over her plans for a business of her own."},
            {"season": 1, "number": 11, "title": "Nobody Knows Anything",
             "summary": "An FBI raid rattles the crew and Tony begins to suspect that Pussy, who has gone missing, may have been turned. Junior quietly moves against Tony as the family's war edges closer to home."},
            {"season": 1, "number": 12, "title": "Isabella",
             "summary": "Sedated and depressed after a car accident, Tony hallucinates a beautiful neighbor named Isabella while the FBI builds its case. Junior's crew prepares a move against Tony that will change everything."},
            {"season": 1, "number": 13, "title": "I Dream of Jeannie Cusamano",
             "summary": "Tony survives an assassination attempt ordered by his uncle and takes his revenge, having the shooters killed. The FBI arrests Junior in a raid, and the season ends with Tony, wounded but alive, dreaming of the normal life across the street."},
        ],
    },
}


def main():
    with open(PATH, encoding="utf-8") as f:
        data = json.load(f)
    by_id = {t.get("id"): t for t in data}
    missing = [i for i in CONTENT if i not in by_id]
    if missing:
        raise SystemExit(f"ids not found in {PATH}: {missing}")
    changed = 0
    for topic in data:
        enrich = CONTENT.get(topic.get("id"))
        if not enrich:
            continue
        if not FORCE and topic.get("episodes"):
            continue
        topic["synopsis"] = enrich["synopsis"]
        topic["episodes"] = enrich["episodes"]
        changed += 1
    with open(PATH, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=1)
        f.write("\n")
    print(f"enriched {changed} series (total topics {len(data)})")


if __name__ == "__main__":
    main()