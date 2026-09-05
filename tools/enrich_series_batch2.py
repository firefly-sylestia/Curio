#!/usr/bin/env python3
"""Enrich series.json batch 2 (web-verified, small batches per project workflow).

Adds `synopsis` + `episodes` (season/episode/title/summary) to five more
tier-1 series: Sherlock, Squid Game, The Last of Us, Severance, Wednesday.
Rerunnable: entries already carrying `episodes` are skipped unless FORCE=1.
Authored text avoids em/en dashes on purpose.
"""
import json
import os

PATH = "data/topics/series.json"
FORCE = os.environ.get("FORCE") == "1"

CONTENT = {
    "series-sherlock": {
        "synopsis": (
            "Steven Moffat and Mark Gatiss's modern-day retelling of Arthur Conan "
            "Doyle moves Sherlock Holmes to present-day London, where the brilliant, "
            "antisocial detective solves crimes with the help of John Watson, a "
            "veteran army doctor who blogs about their cases. Benedict Cumberbatch "
            "plays Holmes, Andrew Scott his arch-enemy Moriarty, and the four series "
            "of ninety-minute episodes trade the original stories' gaslight for "
            "text messages, mind palaces and a jump off the roof of St Bart's. The "
            "show won a string of BAFTAs and Emmys and turned the pair into global "
            "stars."
        ),
        "episodes": [
            {"season": 1, "number": 1, "title": "A Study in Pink",
             "summary": "John Watson returns from Afghanistan and meets Sherlock Holmes, who needs a flatmate and a case. Four suicides turn out to be one serial killer, a cab driver who forces his victims to pick a pill, and Holmes corners him only to learn Moriarty is pulling his strings."},
            {"season": 1, "number": 2, "title": "The Blind Banker",
             "summary": "A banker is found dead in a locked room covered in graffiti, and the trail leads to a smuggling ring that communicates in coded symbols. Holmes breaks a cipher tied to a Chinese circus while Watson chases a date, and the pair stop an assassin before she escapes with the ledger."},
            {"season": 1, "number": 3, "title": "The Great Game",
             "summary": "A bomber forces Holmes to solve a string of cases in five ticks, from a pool of acid to a priceless painting. The final puzzle leads him to the pool where Moriarty reveals himself, holding Watson hostage in a bomb vest, and the season ends on the pair at gunpoint."},
            {"season": 2, "number": 1, "title": "A Scandal in Belgravia",
             "summary": "Holmes is hired by the royals to retrieve compromising photos from Irene Adler, the dominatrix and master of disguise who texts him to prove her superiority. He fakes her death to save her from Moriarty, and she returns the favour by saving his life with a hidden phone."},
            {"season": 2, "number": 2, "title": "The Hounds of Baskerville",
             "summary": "A traumatised man begs Holmes to investigate the giant hound that killed his father, and the pair drive to Dartmoor, where Watson faces his own war demons. The beast is a hallucination caused by a gas released at a secret research site."},
            {"season": 2, "number": 3, "title": "The Reichenbach Fall",
             "summary": "Moriarty breaks out of the Tower of London and frames Holmes as a fraud, stripping him of his reputation. With the assassin threatening Watson, Lestrade and Mrs Hudson, Holmes stages his own suicide at St Bart's, watched by a devastated Watson."},
            {"season": 3, "number": 1, "title": "The Empty Hearse",
             "summary": "Two years after the fall, Watson has moved on and is about to marry Mary, when Holmes returns from hiding and immediately suspects a bomb plot on the underground. The pair reunite as the bomb is defused, and a man with a tattooed face watches them."},
            {"season": 3, "number": 2, "title": "The Sign of Three",
             "summary": "At Watson and Mary's wedding Holmes gives a best-man speech that doubles as a murder case, deducing which guest planned to kill the groomsman. He relives the night of the stag night to crack a locked-room killing in the guards' barracks."},
            {"season": 3, "number": 3, "title": "His Last Vow",
             "summary": "Holmes infiltrates the den of Charles Augustus Magnussen, a blackmailer who keeps secrets in a mind palace. To force a confession without evidence, Holmes shoots him, and Mycroft has him sent on a mission abroad as punishment."},
            {"season": 3, "number": 4, "title": "The Abominable Bride",
             "summary": "A Victorian special: Holmes and Watson investigate a bride who apparently returned from the dead to murder her husband. Inside Holmes's mind palace the case unravels into a confrontation with a female Moriarty, and he wakes on a plane still in danger."},
            {"season": 4, "number": 1, "title": "The Six Thatchers",
             "summary": "Mary's past catches up with her when a series of busts of Thatcher are smashed and a mission from her spy days resurfaces. Holmes saves Watson from a sniper but Mary takes the bullet, dying in her husband's arms and leaving Holmes shattered."},
            {"season": 4, "number": 2, "title": "The Lying Detective",
             "summary": "Holmes returns to drugs as he hunts Culverton Smith, a celebrity philanthropist and serial killer who has confessed on tape. Smith drugs and frames him, but Watson, Eurus and the sister's hidden influence pull him through the case."},
            {"season": 4, "number": 3, "title": "The Final Problem",
             "summary": "Eurus, the third Holmes sibling locked away on an island, escapes and forces the brothers through a series of psychological trials, including a plane full of people and a coffin at Sherrinford. The family survives, Eurus is recaptured, and Holmes tells Watson he is a man who cares."},
        ],
    },
    "series-squid-game": {
        "synopsis": (
            "Hwang Dong-hyuk's survival drama follows Seong Gi-hun, a debt-ridden "
            "gambler who is invited to a mysterious competition where 456 players "
            "play childhood games for a prize of 45.6 billion won, with elimination "
            "meaning death. The games, from Red Light Green Light to the marble "
            "round, expose the desperation of people who chose to risk their lives "
            "for money, while the masked Front Man and the VIPs watch from above. "
            "The show became Netflix's most-watched series, and its imagery, the "
            "green tracksuits, the giant doll, the honeycomb shapes, became global "
            "iconography."
        ),
        "episodes": [
            {"season": 1, "number": 1, "title": "Red Light, Green Light",
             "summary": "Gi-hun, drowning in gambling debt, accepts a card and wakes in a dormitory with 455 others. The first game is Red Light Green Light, and when a man flinches the players learn the cost: the doll's sensors shoot the eliminated where they stand."},
            {"season": 1, "number": 2, "title": "Hell",
             "summary": "The survivors vote to leave and return to their real lives, but within days most are back, broke and desperate. The games restart, and Gi-hun joins forces with Cho Sang-woo, his old schoolmate, and Oh Il-nam, the old man who loves to play."},
            {"season": 1, "number": 3, "title": "The Man with the Umbrella",
             "summary": "Players choose shapes for the second game, honeycomb carving, and Gi-hun's umbrella shape proves the hardest. He licks the honeycomb to free his shape while others are shot for breaking theirs, and learns the guards are all players who survived."},
            {"season": 1, "number": 4, "title": "Stick to the Team",
             "summary": "Players form teams of ten for tug of war, and Gi-hun's team is the oldest and weakest. Il-nam reveals he was a factory foreman, and the team uses the old man's trick of forward steps to drag the stronger opposing team over the edge."},
            {"season": 1, "number": 5, "title": "A Fair World",
             "summary": "After a night fight leaves players dead, the survivors vote again and the prize is split, with the remaining players given a chance to walk away. Most stay, and a doctor is discovered to have been trading organs with the guards in return for information."},
            {"season": 1, "number": 6, "title": "Gganbu",
             "summary": "The marble game pairs players with their closest allies. Gi-hun and Il-nam play marbles together, and when Il-nam lets himself be eliminated, he gives Gi-hun his marbles, revealing the old man's surprising calm about dying."},
            {"season": 1, "number": 7, "title": "VIPs",
             "summary": "The masked VIPs arrive to watch the fifth game, a bridge made of glass panels that shatter under the players' weight. A glassmaker survives by tapping panels, but is pushed off by a desperate player, and three of the four survivors cross the bridge."},
            {"season": 1, "number": 8, "title": "Front Man",
             "summary": "The final three players are left to fight in the sixth game, but the last one, Jang Deok-su, is killed by the guards when he refuses. Gi-hun and Sang-woo are told they can both leave with the prize, until the Front Man reveals only one can."},
            {"season": 1, "number": 9, "title": "One Lucky Day",
             "summary": "Sang-woo stabs himself so Gi-hun can take the prize and use it for his family. A year later Gi-hun finds Il-nam alive, learns the old man created the games, and watches him die of a brain tumour. At the airport Gi-hun turns back to expose the games."},
        ],
    },
    "series-the-last-of-us": {
        "synopsis": (
            "Craig Mazin and Neil Druckmann's adaptation of the 2013 video game "
            "follows Joel, a hardened smuggler, as he escorts fourteen-year-old "
            "Ellie across a post-apocalyptic United States, twenty years after a "
            "cordyceps fungus turned most of humanity into infected. The journey "
            "from Boston to a Firefly hospital in Utah tests Joel's ability to care "
            "again, and Ellie's immunity to the infection makes her the key to a "
            "cure. Pedro Pascal plays Joel and Bella Ramsey plays Ellie, and the "
            "series follows the game's story beat for beat while expanding it with "
            "episodes devoted to the people they meet."
        ),
        "episodes": [
            {"season": 1, "number": 1, "title": "When You're Lost in the Darkness",
             "summary": "The outbreak begins in 2003 when Joel's daughter Sarah is shot during the evacuation, and twenty years later he runs smuggling jobs in the quarantine zone. A friend asks him to take a teenage girl named Ellie out of the city."},
            {"season": 1, "number": 2, "title": "Infected",
             "summary": "Joel, Ellie and Tess cross the city to the statehouse, fighting infected in a museum. Tess is bitten and reveals she knows Ellie is immune, holding off the infected so the pair can escape with the mission intact."},
            {"season": 1, "number": 3, "title": "Long, Long Time",
             "summary": "Bill's story: a survivalist who fortifies a town alone until he meets Frank, and the two live together for years. Frank, dying, asks Bill to end it with him, and Bill leaves everything, including his truck, for Joel and Ellie to use."},
            {"season": 1, "number": 4, "title": "Please Hold to My Hand",
             "summary": "Joel and Ellie make their way through Kansas City, dodging a group led by Kathleen who is hunting a man named Henry. The pair team up with Henry and his deaf brother Sam, and the four plan an escape through the city."},
            {"season": 1, "number": 5, "title": "Endure and Survive",
             "summary": "Kathleen corners the group in a suburb as an army of infected, drawn by noise, overruns the city. Henry kills Kathleen and the group escapes, but Sam is bitten and turns; Henry shoots Sam and then himself rather than live without him."},
            {"season": 1, "number": 6, "title": "Kin",
             "summary": "Joel brings Ellie to Wyoming to find his brother Tommy, who has built a community at a dam. The pair learn the Fireflies may be at a hospital in Colorado, and Joel, weakened, collapses on the journey as Ellie cares for him."},
            {"season": 1, "number": 7, "title": "Left Behind",
             "summary": "Ellie's backstory: the night she was bitten she and her best friend Riley explored a mall, and Riley revealed she had been recruited by the Fireflies. Both were bitten, Riley turned, and Ellie survived, alone."},
            {"season": 1, "number": 8, "title": "When We Are in Need",
             "summary": "Joel, badly hurt, is nursed by a couple, while Ellie hunts for supplies and is captured by a preacher, David, who wants to eat her. Ellie escapes and kills David, and Joel, recovered, finds her shaken but alive."},
            {"season": 1, "number": 9, "title": "Look for the Light",
             "summary": "The Fireflies plan to extract the fungus from Ellie's brain to make a cure, a surgery that will kill her. Joel kills the surgeon and the soldiers to take her back, and when she wakes he lies, telling her they found others like her who were immune."},
        ],
    },
    "series-severance": {
        "synopsis": (
            "Dan Erickson's workplace thriller imagines Lumon Industries, where "
            "employees undergo a surgical procedure that severs their memories, so "
            "the work self, their innie, exists only inside the office and knows "
            "nothing of the life outside. Adam Scott plays Mark, who leads a team "
            "of refiners sorting numbers on screens, until a new hire and a "
            "mysterious colleague begin to suspect the company's true purpose. The "
            "show's sterile corridors, cult-like rituals and the question of what "
            "the numbers mean made it a slow-burn hit that won the Emmy for best "
            "drama."
        ),
        "episodes": [
            {"season": 1, "number": 1, "title": "Good News About Hell",
             "summary": "Mark, recently severed, starts work at Lumon's macrodata refinement floor, guided by manager Harmony Cobel. A former colleague, Petey, appears outside the building and tells Mark he has reintegrated, before collapsing and being taken away."},
            {"season": 1, "number": 2, "title": "Half Loop",
             "summary": "Mark's outie mourns his wife, while his innie investigates Petey's disappearance. The new hire Helly tries repeatedly to resign, and Mark finds Petey hiding in his basement, where Petey explains he can feel both lives at once."},
            {"season": 1, "number": 3, "title": "In Perpetuity",
             "summary": "Petey dies of reintegration sickness, and Mark's innie digs through his old friend's notes. Helly attempts a violent escape that lands her in the hospital, and the team learns the break room is where insubordination is punished."},
            {"season": 1, "number": 4, "title": "The You You Are",
             "summary": "The team finds a book in the break room written by a mysterious CEO, and Helly meets her outie's fiancé, who is actually a Lumon employee sent to keep her in line. Mark discovers his outie lives next door to his boss, Cobel."},
            {"season": 1, "number": 5, "title": "The Grim Barbarity of Optics and Design",
             "summary": "The refiners meet the staff of another severed department, Optics and Design, and learn the two floors have been kept apart. Mark finds a hidden area with a goat, and the team grows closer as they plan their escape."},
            {"season": 1, "number": 6, "title": "Hide and Seek",
             "summary": "Cobel keeps an eye on Mark's outie after a visit to his wife's grave, while the refiners devise the overtime contingency, a way to wake their innies outside work. Irving's outie paints the black hallway over and over."},
            {"season": 1, "number": 7, "title": "Defiant Jazz",
             "summary": "The team plans to use the overtime contingency to expose Lumon from the outside. Helly records a message to her outie, Irving begins to distrust Burt, and the refiners agree to flip the switch despite the risk."},
            {"season": 1, "number": 8, "title": "What's for Dinner?",
             "summary": "The overtime contingency fires: Mark's innie wakes at a party, Irving's innie follows the painting to Burt's house and to the mysterious hallway, and Helly's innie wakes at her own outie's event and delivers a devastating speech."},
            {"season": 1, "number": 9, "title": "The We We Are",
             "summary": "The innies use their stolen minutes to spread the truth: Helly exposes the severed floor, Irving finds the exports hall, and Mark follows the sound of a baby and his wife's voice to find her alive in Lumon's basement, just as the switch is cut."},
        ],
    },
    "series-wednesday": {
        "synopsis": (
            "Tim Burton's comedy-horror reimagines Wednesday Addams as a "
            "teenager at Nevermore Academy, a school for outcasts where werewolves, "
            "vampires and sirens study alongside normies. Jenna Ortega plays the "
            "deadpan heroine who investigates a monster killing the town's "
            "students, uncovering a conspiracy tied to her own family and her "
            "parents' past. The show's goth humour, Ortega's deadpan delivery and "
            "the viral dance scene made it Netflix's most-watched series at "
            "launch, and it was renewed for a second season."
        ),
        "episodes": [
            {"season": 1, "number": 1, "title": "Wednesday's Child Is Full of Woe",
             "summary": "Wednesday is expelled from her high school after releasing piranhas into the pool, and her parents send her to Nevermore Academy. A monster kills a student in the woods, and Wednesday's visions show her the attack in detail."},
            {"season": 1, "number": 2, "title": "Woe Is the Loneliest Number",
             "summary": "Wednesday joins the school's outcast clubs and meets her roommate Enid, the stalker Xavier and the scheming Bianca. Another student is killed, and Wednesday uses her psychic visions to hunt the monster, called the Hyde."},
            {"season": 1, "number": 3, "title": "Friend or Woe",
             "summary": "Wednesday investigates the town's Pilgrim Day festival and the history of the outcasts' persecution. The monster attacks during the festival, and Wednesday's visions link it to her ancestor Goody Addams and the original feud."},
            {"season": 1, "number": 4, "title": "Woe What a Night",
             "summary": "Wednesday attends the school's Raven dance and performs the now-famous dance, while the monster attacks the mayor's fundraiser. Tyler, the town barista, reveals a secret connection to the sheriff's investigation."},
            {"season": 1, "number": 5, "title": "You Reap What You Woe",
             "summary": "Wednesday breaks into the sheriff's files and learns her father was a suspect in an old murder case. She uses her visions to reopen the investigation, and her uncle Fester arrives for a visit with advice about her powers."},
            {"season": 1, "number": 6, "title": "Quid Pro Woe",
             "summary": "The Hyde's victims pile up, and Wednesday's visions show her that the monster is controlled by someone at the school. She suspects the principal, then the therapist, while her romantic tension with Xavier and Tyler grows."},
            {"season": 1, "number": 7, "title": "If You Don't Woe Me by Now",
             "summary": "Wednesday uncovers the conspiracy of Laurel Gates, who has been using Tyler as her Hyde. Her visions reveal the truth about her father's case, and she races to stop the monster before it kills again."},
            {"season": 1, "number": 8, "title": "A Murder of Woes",
             "summary": "Wednesday confronts Laurel, who is resurrecting the original monster to destroy Nevermore. With the school under siege, Wednesday uses her ancestor's powers to defeat the Hyde, and a dramatic twist reveals a family secret about her mother."},
        ],
    },
}


def main():
    with open(PATH, encoding="utf-8") as f:
        data = json.load(f)
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