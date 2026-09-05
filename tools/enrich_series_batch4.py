#!/usr/bin/env python3
"""Enrich series.json batch 4 (web-verified, small batches per project workflow).

Adds `synopsis` + `episodes` (season/episode/title/summary) to five more
tier-1 series: The Good Place, Ted Lasso, The Mandalorian, Succession,
True Detective (first seasons, titles verified against episode guides).
Rerunnable: entries already carrying `episodes` are skipped unless FORCE=1.
Authored text avoids em/en dashes on purpose.
"""
import json
import os

PATH = "data/topics/series.json"
FORCE = os.environ.get("FORCE") == "1"

CONTENT = {
    "series-the-good-place": {
        "synopsis": (
            "Michael Schur's comedy about the afterlife is one of the "
            "smartest sitcoms ever made. Eleanor Shellstrop, a selfish "
            "Arizona woman who sold placebos and never did anything for "
            "anyone, dies and wakes up in the Good Place, the "
            "neighborhood of eternal reward designed by the architect "
            "Michael, where she is introduced as a beloved human rights "
            "lawyer and philanthropist. She knows she was sent there by "
            "mistake, and to stay she must become a genuinely good "
            "person, which is hard when everyone around her is "
            "perfect. Kristen Bell plays Eleanor, William Jackson "
            "Harper plays Chidi, the indecisive ethics professor "
            "who teaches her, Jameela Jamil plays Tahani, whose "
            "own goodness is a form of vanity, and Manny Jacinto "
            "plays Jason, who is not the monk he pretends to be. "
            "The show is a philosophical romp through ethics and "
            "the meaning of a good life, and its first season "
            "ends with one of the great twists in television "
            "history."
        ),
        "episodes": [
            {"season": 1, "number": 1, "title": "Everything Is Fine",
             "summary": "Eleanor Shellstrop dies and wakes in the Good Place, greeted by the architect Michael, who mistakes her for a philanthropist. She quickly realizes she was sent by mistake, and she is a terrible person who belongs nowhere near paradise."},
            {"season": 1, "number": 2, "title": "Flying",
             "summary": "Eleanor tries to be good by attaching herself to Chidi, the ethics professor assigned as her soulmate, while Tahani and Jason, a silent monk, appear as perfect as the neighborhood demands. She decides to make Chidi teach her to be a better person."},
            {"season": 1, "number": 3, "title": "Tahani Al-Jamil",
             "summary": "Tahani's backstory reveals a lifelong rivalry with her famous sister, and Eleanor, terrified of exposure, spins an elaborate lie about her own charitable past. Jason lets slip that he is not what he seems."},
            {"season": 1, "number": 4, "title": "Jason Mendoza",
             "summary": "Jason's real story comes out: he is a small-time crook from Jacksonville who died hiding in a safe. Eleanor's cover is blown with Chidi and Tahani, and the four of them begin to realize how badly they fit the neighborhood."},
            {"season": 1, "number": 5, "title": "Category 55 Emergency Doomsday Crisis",
             "summary": "Michael tests the residents with a staged emergency, and Eleanor, Jason and Chidi fail spectacularly. Chidi grows suspicious of the neighborhood's cheerful perfection, and Eleanor is told to fix things before she gets sent away."},
            {"season": 1, "number": 6, "title": "What We Owe to Each Other",
             "summary": "Chidi's ethics lessons push Eleanor toward real change while Tahani and Jason try a date that goes nowhere. Michael watches from his control room as his experiment strains at the seams."},
            {"season": 1, "number": 7, "title": "The Eternal Shriek",
             "summary": "A terrible shriek echoes through the neighborhood and Eleanor and Jason investigate where it comes from, while Michael is forced to admit that even paradise has a secret room and a secret purpose."},
            {"season": 1, "number": 8, "title": "Most Improved Player",
             "summary": "Michael announces a self-improvement competition and the four residents work on their flaws: Eleanor on selfishness, Chidi on indecision, Tahani on vanity, Jason on everything. Michael watches them with a little too much interest."},
            {"season": 1, "number": 9, "title": "...Someone Like Me as a Member",
             "summary": "Eleanor panics when someone who knew her real life on Earth arrives in the neighborhood and could expose her lie, and she scrambles to control a situation that is slipping away."},
            {"season": 1, "number": 10, "title": "Chidi's Choice",
             "summary": "Chidi is asked to choose between Eleanor and Tahani, and his inability to decide becomes the test of the neighborhood itself. Eleanor begins to realize what Chidi means to her."},
            {"season": 1, "number": 11, "title": "What's My Motivation",
             "summary": "The group splits apart and Eleanor and Chidi go looking for the truth about the neighborhood, finding rooms and clues that the Good Place was never supposed to have."},
            {"season": 1, "number": 12, "title": "Mindy St. Claire",
             "summary": "The Medium Place is revealed: Mindy St. Claire, who died before her case could be decided, lives in eternal mediocrity between the two places. Eleanor gets the proof she needs that the system is not what it seems."},
            {"season": 1, "number": 13, "title": "Michael's Gambit",
             "summary": "The great reveal: the neighborhood is the Bad Place, Michael is a demon, and Eleanor, Chidi, Tahani and Jason have been torturing one another through centuries of reboots. Eleanor's hidden message to Chidi survives, and Michael is exposed before his bosses."},
        ],
    },
    "series-ted-lasso": {
        "synopsis": (
            "Jason Sudeikis plays Ted Lasso, an American college football "
            "coach hired to manage AFC Richmond, a struggling English "
            "Premier League club, despite knowing nothing about soccer. "
            "What he does not know is that the club's new owner, "
            "Rebecca Welton, hired him specifically to destroy the team "
            "and humiliate her ex-husband, its former owner. Ted's "
            "unshakable optimism, his folksy aphorisms and his belief "
            "sign slowly win over the skeptical players, the cynical "
            "press and eventually the audience, but the show never lets "
            "the sweetness go unearned: Ted is coping with a failing "
            "marriage and panic attacks, and his kindness is a "
            "discipline, not a lack of perception. With Hannah "
            "Waddingham's Rebecca, Brett Goldstein's Roy Kent and "
            "Phil Dunster's Jamie Tartt, the show became a global "
            "phenomenon, a comedy about empathy that made "
            "everyone feel like they were on the team."
        ),
        "episodes": [
            {"season": 1, "number": 1, "title": "Pilot",
             "summary": "Ted Lasso, an American football coach, is hired to manage AFC Richmond by new owner Rebecca Welton, who secretly wants the club to fail. Ted's folksy optimism confuses the players, the press and the fans alike."},
            {"season": 1, "number": 2, "title": "Biscuits",
             "summary": "Ted brings Rebecca biscuits every morning, makes his first tactical errors, and begins to win over the locker room, though the veteran Roy Kent and the young star Jamie Tartt remain unimpressed."},
            {"season": 1, "number": 3, "title": "Trent Crimm: The Independent",
             "summary": "A journalist shadows the club and expects to find a laughingstock; Ted's genuine curiosity and warmth instead leave him with nothing bad to write. The team plays its first match of the season."},
            {"season": 1, "number": 4, "title": "For the Children",
             "summary": "A charity gala forces the players to auction themselves off, and Ted's speech, honest about failure, upstages the performance. Rebecca's sabotage of the club begins to take real shape."},
            {"season": 1, "number": 5, "title": "Tan Lines",
             "summary": "Jamie Tartt's ego grows with a reality show, and the team loses again. Ted's wife Michelle is distant on the phone, and the first cracks in his cheerful armor show."},
            {"season": 1, "number": 6, "title": "Two Aces",
             "summary": "Ted suffers a panic attack and begins to hide it, while the club's finances and Rebecca's vendetta tighten around them. The team finds a small victory on the pitch."},
            {"season": 1, "number": 7, "title": "Make Rebecca Great Again",
             "summary": "Rebecca's ex-husband Rupert arrives with his young girlfriend, and Ted beats him at darts in the pub with a lesson about curiosity. Rebecca starts to soften toward the club, and toward Ted."},
            {"season": 1, "number": 8, "title": "The Diamond Dogs",
             "summary": "The coaching staff forms the Diamond Dogs, a huddle for personal problems, and the players begin to trust each other. The club's relegation fight comes into view."},
            {"season": 1, "number": 9, "title": "All Apologies",
             "summary": "The season's pressure peaks: Roy Kent faces his own decline, Jamie's ego costs the team, and an apology tour forces everyone to look honestly at themselves."},
            {"season": 1, "number": 10, "title": "The Hope That Kills You",
             "summary": "Richmond's fate is decided on the final day of the season. Rebecca confesses her plot to Ted and he forgives her; the club is relegated but leaves the pitch full of hope, and the Believe sign goes up again."},
        ],
    },
    "series-the-mandalorian": {
        "synopsis": (
            "Jon Favreau's series was the first live-action Star Wars "
            "television show, and it became a cultural event. Pedro "
            "Pascal plays the Mandalorian, a lone bounty hunter in the "
            "years after the fall of the Empire, a helmeted gunslinger "
            "who takes a job that changes his life: the target is a "
            "fifty-year-old green child of the same species as Yoda, "
            "wanted by Imperial remnants. Instead of delivering the "
            "child, he protects him, and the two travel the galaxy as "
            "the Empire closes in. The show is a western in space, "
            "built from a series of adventures across desert planets, "
            "harbor towns and Imperial wrecks, and its refusal to "
            "explain too much, the Child's powers, the Mandalorian's "
            "code, the mysterious remnant led by Moff Gideon, "
            "became part of its charm. The season builds to a "
            "foundling story about honor and found family, and "
            "it made Baby Yoda, as everyone called him, the "
            "most beloved character in the galaxy."
        ),
        "episodes": [
            {"season": 1, "number": 1, "title": "Chapter 1: The Mandalorian",
             "summary": "A lone Mandalorian bounty hunter takes a job from a mysterious client: find a target on a desert planet and bring it in. He tracks the mark to a remote camp, and the target turns out to be a small green child."},
            {"season": 1, "number": 2, "title": "Chapter 2: The Child",
             "summary": "Jawas strip the Mandalorian's ship for parts and he must bargain with them to get them back. The Child saves his life by using the Force to stop a mudhorn, and the hunter begins to understand what he is carrying."},
            {"season": 1, "number": 3, "title": "Chapter 3: The Sin",
             "summary": "The Mandalorian delivers the Child and is paid in beskar steel, the metal of his people. He has a change of heart, breaks back into the compound, and fights his way out with the Child, helped by his fellow Mandalorians."},
            {"season": 1, "number": 4, "title": "Chapter 4: Sanctuary",
             "summary": "Hiding from the hunters, the Mandalorian and the Child land on a quiet farming world, where he helps a village defend itself from raiders with the help of Cara Dune, a former Rebel shock trooper, in a battle against an Imperial war machine."},
            {"season": 1, "number": 5, "title": "Chapter 5: The Gunslinger",
             "summary": "The pair stop on Tatooine, where a young bounty hunter named Toro Calican wants to capture the legendary Fennec Shand. The deal goes wrong and the Mandalorian is left with a mess and a new enemy."},
            {"season": 1, "number": 6, "title": "Chapter 6: The Prisoner",
             "summary": "The Mandalorian joins a crew of mercenaries for a prison break job that goes exactly as badly as expected. He outwits them, locks them in the cells, and learns that the Client is still hunting the Child."},
            {"season": 1, "number": 7, "title": "Chapter 7: The Reckoning",
             "summary": "The Mandalorian teams up with Cara Dune and Greef Karga to lure out the Client, with the Child as bait. The trap is sprung by Moff Gideon, and the group barely escapes with the Child taken."},
            {"season": 1, "number": 8, "title": "Chapter 8: Redemption",
             "summary": "Moff Gideon closes in with an army of stormtroopers. The droid IG-11 sacrifices himself to save the group, the Mandalorian's helmet comes off, and he vows to return the Child to his own kind, a foundling's promise that sets up the war to come."},
        ],
    },
    "series-succession": {
        "synopsis": (
            "Jesse Armstrong's drama about the Roy family, owners of the "
            "global media empire Waystar Royco, is a tragedy wearing a "
            "comedy's clothes. Logan Roy, the aging patriarch, built "
            "the company from nothing, and his children, Kendall, "
            "Roman, Shiv and Connor, circle him like planets, "
            "desperate for his approval and for the crown they "
            "believe is theirs. Brian Cox plays Logan, Jeremy "
            "Strong the tortured Kendall, Kieran Culkin the "
            "cruel Roman, Sarah Snook the calculating Shiv, "
            "and Matthew Macfadyen the hapless Tom, Shiv's "
            "fiancé. The show follows the family through "
            "takeovers, weddings, funerals and betrayals, "
            "its dialogue crackling with the special "
            "cruelty of people raised on money and "
            "neglect. It won a shelf of Emmys and is "
            "widely regarded as one of the greatest "
            "dramas of its era, a portrait of "
            "power as inheritance, and of a "
            "family that cannot stop "
            "destroying itself."
        ),
        "episodes": [
            {"season": 1, "number": 1, "title": "Celebration",
             "summary": "Logan Roy's eightieth birthday: the family gathers at the estate, and Logan announces he has no intention of stepping down as CEO. Kendall, the presumed heir, is blindsided, and the night ends with Logan suffering a stroke."},
            {"season": 1, "number": 2, "title": "Shit Show at the Fuck Factory",
             "summary": "Logan lies in the hospital and his children begin to maneuver. Kendall tries to act like a leader, Shiv balances her political career, and Roman toys with his father's empire from the sidelines."},
            {"season": 1, "number": 3, "title": "Lifeboats",
             "summary": "Kendall moves for a vote of no confidence and fails to rally the board, learning how little power he actually has. The siblings realize the succession will be a war, not an inheritance."},
            {"season": 1, "number": 4, "title": "Sad Sack Wasp Trap",
             "summary": "Logan recovers at home, surrounded by handlers, and the children compete for his attention while the company drifts. Shiv's relationship with Tom is put under the family microscope."},
            {"season": 1, "number": 5, "title": "I Went to Market",
             "summary": "Kendall begins a risky deal with the financier Stewy to fund a takeover, and the family learns that an outside force is circling the company. Logan begins to suspect his own children."},
            {"season": 1, "number": 6, "title": "Which Side Are You On?",
             "summary": "The bear hug, the takeover play, comes into the open and each Roy must choose a side. Kendall presses his advantage while Logan fights from weakness for the first time."},
            {"season": 1, "number": 7, "title": "Austerlitz",
             "summary": "The family convenes for a therapy weekend that goes badly for everyone: Logan's health, Kendall's confidence and Roman's loyalties all surface under the therapist's questions."},
            {"season": 1, "number": 8, "title": "Prague",
             "summary": "Tom's bachelor party takes the group to Prague while Shiv and Tom's wedding looms, and the dealmaking behind the family's backs threatens to break the couple apart before the vows."},
            {"season": 1, "number": 9, "title": "DC",
             "summary": "The Roys travel to Washington, where Logan is honored and the succession maneuvering continues in public, with each child testing how much of the empire they can claim."},
            {"season": 1, "number": 10, "title": "Nobody Is Ever Missing",
             "summary": "Shiv and Tom marry on a luxury ocean liner while Kendall's night unravels: after a car accident that kills a young man, Logan covers it up, and Kendall, broken, signs away his rebellion, ending the season as his father's prisoner."},
        ],
    },
    "series-true-detective": {
        "synopsis": (
            "Nic Pizzolatto's anthology series reinvented the detective "
            "show, and its first season became a phenomenon. Matthew "
            "McConaughey plays Rust Cohle, a philosophical, "
            "world-weary detective who sees through everything, "
            "and Woody Harrelson plays Marty Hart, the married, "
            "conventional partner who cannot understand him. "
            "In 1995 Louisiana, the two investigate the murder "
            "of a woman posed with antlers in a cane field, "
            "a case that pulls them into a web of secret "
            "churches, corrupt families and a decades-old "
            "cult, and the show cuts between the original "
            "investigation and 2012, when a new murder "
            "forces the estranged partners back together. "
            "With its swampy atmosphere, its spiraling "
            "obsession with the occult, and Cohle's "
            "bleak monologues, the season felt like "
            "a novel, not a procedural, and its "
            "ending, in which the horror finally "
            "has a face, remains one of the "
            "most debated finales in "
            "television."
        ),
        "episodes": [
            {"season": 1, "number": 1, "title": "The Long Bright Dark",
             "summary": "In 2012, detectives Rust Cohle and Marty Hart are interviewed about a case from 1995: the murder of a woman posed with antlers in a Louisiana cane field. The two old partners are barely speaking, and the story they tell is only the beginning."},
            {"season": 1, "number": 2, "title": "Seeing Things",
             "summary": "The 1995 investigation digs into the dead woman's church and the strange spiral painted on her back. Cohle's past as an undercover narcotics agent comes out, and the case begins to look less like a murder and more like a ritual."},
            {"season": 1, "number": 3, "title": "The Locked Room",
             "summary": "The detectives trace the victim's letters and the church's pastor, and the clues keep circling the same families. Hart's marriage strains while Cohle pushes the case past the point his superiors want."},
            {"season": 1, "number": 4, "title": "Who Goes There",
             "summary": "The killer seems to be watching, and the case explodes: a raid on a meth lab leads to a shootout and the arrest of Reggie Ledoux, the man the evidence points to, and the spiral seems to end."},
            {"season": 1, "number": 5, "title": "The Secret Fate of All Life",
             "summary": "The raid goes wrong and Cohle and Hart kill Ledoux's cousin in the woods. The case is closed with Ledoux behind bars, and the two men think it is over, though the spiral keeps turning."},
            {"season": 1, "number": 6, "title": "Haunted Houses",
             "summary": "Years later a new body with the same markings is found, and the case is alive again. Hart's affair has destroyed his marriage, Cohle is alone with his obsession, and the two partners are drifting apart."},
            {"season": 1, "number": 7, "title": "After You've Gone",
             "summary": "In 2012 the two men are dragged back together to reopen the case, and the name of the Yellow King surfaces. They follow the spiral through the Tuttle family's history toward the truth they missed."},
            {"season": 1, "number": 8, "title": "Form and Void",
             "summary": "The truth is Errol Childress, the killer they overlooked for years. Cohle and Hart corner him in the ruins of Carcosa and barely survive, and in the hospital, looking at the stars, Cohle admits the light is winning after all."},
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