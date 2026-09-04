#!/usr/bin/env python3
"""Enrich series.json batch 1 (web-verified, small batches per project workflow).

Adds `synopsis` + `episodes` (season/episode/title/summary) to five finite,
web-researched series. Rerunnable: entries already carrying `episodes` are
skipped unless FORCE=1. Authored text avoids em/en dashes on purpose.
"""
import json
import os
import sys

PATH = "data/topics/series.json"
FORCE = os.environ.get("FORCE") == "1"

CONTENT = {
    "series-chernobyl": {
        "synopsis": (
            "Craig Mazin's five-part HBO drama reconstructs the April 1986 Chernobyl "
            "nuclear disaster and the Soviet cover-up that followed. It follows physicist "
            "Valery Legasov and deputy chairman Boris Shcherbina as they are sent to contain "
            "a reactor whose core has been exposed, while the people of Pripyat and the first "
            "responders pay the price for a system that punished bad news. The story moves "
            "from the night of the explosion to the liquidation, the trial, and the design "
            "flaws that made the accident possible."
        ),
        "episodes": [
            {"season": 1, "number": 1, "title": "1:23:45",
             "summary": "Two years after the disaster Legasov records his testimony and hangs himself. On 26 April 1986 reactor No. 4 explodes; Dyatlov denies the core is exposed and orders water added and fires fought, exposing everyone to lethal radiation while Legasov is summoned to the Kremlin."},
            {"season": 1, "number": 2, "title": "Please Remain Calm",
             "summary": "Physicist Ulana Khomyuk detects a radiation spike in Minsk and pushes for a real investigation. Legasov and Shcherbina arrive, prove the core is exposed, arrest the plant managers, and order the evacuation of Pripyat only after realizing they have absorbed a lethal dose themselves."},
            {"season": 1, "number": 3, "title": "Open Wide, O Earth",
             "summary": "Workers drain the water reservoirs under the reactor by hand while miners dig a heat exchanger beneath the core. Khomyuk interviews the dying Akimov and Toptunov and learns that the AZ-5 shutdown button may have caused the explosion."},
            {"season": 1, "number": 4, "title": "The Happiness of All Mankind",
             "summary": "The exclusion zone expands and liquidators clear the radioactive roof in ninety-second shifts while the dead are buried in sealed graves. Khomyuk uncovers the reactor's fatal design flaw and urges Legasov to expose it instead of accepting a secret fix."},
            {"season": 1, "number": 5, "title": "Vichnaya Pamyat",
             "summary": "The night before the explosion the plant's managers ran a doomed safety test for the sake of promotion. Legasov testifies at the trial that the AZ-5 button was the fatal flaw, is stripped of his honours, and his posthumous memoirs force the state to admit the truth."},
        ],
    },
    "series-band-of-brothers": {
        "synopsis": (
            "Steven Spielberg and Tom Hanks produced this ten-part HBO account of Easy "
            "Company, the 506th Parachute Infantry Regiment of the 101st Airborne, from "
            "paratrooper training at Camp Toccoa through D-Day, Operation Market Garden, the "
            "siege of Bastogne, and the capture of Hitler's Eagle's Nest. Each episode "
            "follows the company through one campaign, often from a single soldier's point "
            "of view, and the surviving veterans introduce their own story in interviews "
            "that bookend the series. Based on Stephen Ambrose's book of the same name."
        ),
        "episodes": [
            {"season": 1, "number": 1, "title": "Currahee",
             "summary": "Easy Company trains under the harsh Captain Sobel at Camp Toccoa and ships to England in 1943. When Sobel invents a grievance against Winters, the non-commissioned officers threaten mass resignation and Sobel is reassigned before D-Day."},
            {"season": 1, "number": 2, "title": "Day of Days",
             "summary": "Easy drops into Normandy scattered and short its commander. Winters takes charge and destroys the German artillery firing on Utah Beach at Brecourt Manor, winning the Distinguished Service Cross."},
            {"season": 1, "number": 3, "title": "Carentan",
             "summary": "Easy fights through the Battle of Carentan and loses several men while rumours spread that Speirs killed prisoners. A shell-shocked Private Blithe is rallied by Winters and is later shot by a sniper on patrol."},
            {"season": 1, "number": 4, "title": "Replacements",
             "summary": "Green replacements struggle to be accepted by the Normandy veterans. Easy parachutes into Holland for Market Garden, liberates Eindhoven, and is forced to retreat from Nuenen, where a wounded Bull Randleman hides in a barn from a German patrol."},
            {"season": 1, "number": 5, "title": "Crossroads",
             "summary": "Winters is haunted by shooting a teenage SS soldier and is promoted away from the company. Easy helps rescue the British 1st Airborne at Pegasus Bridge, loses commander Heyliger to a friendly-fire accident, and is rushed to Bastogne."},
            {"season": 1, "number": 6, "title": "Bastogne",
             "summary": "Easy holds the frozen perimeter around Bastogne through the winter with almost no supplies. Medic Doc Roe keeps the men alive, befriends a Belgian nurse who is later killed in a bombing raid, and General McAuliffe answers the German surrender demand with a single word."},
            {"season": 1, "number": 7, "title": "The Breaking Point",
             "summary": "The fight for Foy grinds Easy down: Hoobler dies shooting himself with a captured pistol, Guarnere and Toye each lose a leg in the same shelling, and Dike freezes during the assault until Speirs relieves him. Lipton earns a field commission and Easy takes the town."},
            {"season": 1, "number": 8, "title": "The Last Patrol",
             "summary": "Easy rests in Haguenau and gives a cold welcome to Webster, who stayed in hospital instead of returning to the line. A night raid across the river to capture prisoners wins Jones and Webster some respect, and Winters is promoted to major."},
            {"season": 1, "number": 9, "title": "Why We Fight",
             "summary": "Easy crosses into Germany and stumbles on the Kaufering concentration camp. The men feed the survivors while the regiment forces local civilians to bury the dead, and Nixon learns that Hitler has killed himself."},
            {"season": 1, "number": 10, "title": "Points",
             "summary": "Easy takes the Eagle's Nest without a fight and the war in Europe ends. With the division due for the Pacific, soldiers with enough points go home, and Winters narrates the fates of the men as Japan surrenders."},
        ],
    },
    "series-fleabag": {
        "synopsis": (
            "Phoebe Waller-Bridge's two-series comedy about a grieving, angry young woman in "
            "London who keeps breaking the fourth wall to confide in the audience. Adapted "
            "from her one-woman show, it follows Fleabag through the fallout of her best "
            "friend Boo's death, her failing cafe, her difficult sister Claire, and in the "
            "second series her charged and impossible attraction to a priest. The show is "
            "famous for its direct address, its excruciating humour, and a finale that "
            "politely asks the audience to leave."
        ),
        "episodes": [
            {"season": 1, "number": 1, "title": "Episode 1",
             "summary": "Fleabag sleeps with Arsehole Guy, picks up Bus Rodent, and fails to get a loan for her cafe. She argues with her sister Claire, steals a sculpture from her godmother's studio, and confesses to the audience how her best friend Boo died."},
            {"season": 1, "number": 2, "title": "Episode 2",
             "summary": "Fleabag tries to sell the stolen statue to Claire's husband Martin. Awkward sex with Arsehole Guy sends her back to Harry, who finally dumps her when he discovers she lied about giving up her old habits."},
            {"season": 1, "number": 3, "title": "Episode 3",
             "summary": "Fleabag helps Martin shop for Claire's surprise party and brings Bus Rodent along. Martin gives Claire the stolen sculpture, then tries to kiss Fleabag and is rebuffed."},
            {"season": 1, "number": 4, "title": "Episode 4",
             "summary": "The sisters attend a silent retreat, where Fleabag admits she stole the sculpture and bonds with the disgraced Bank Manager. Claire reveals a lucrative job offer in Finland that she may turn down for her family."},
            {"season": 1, "number": 5, "title": "Episode 5",
             "summary": "On the anniversary of their mother's death the family gathers for the annual lunch. Fleabag returns the sculpture to the godmother's studio, Claire steals it back for her, and Fleabag rekindles things with Arsehole Guy."},
            {"season": 1, "number": 6, "title": "Episode 6",
             "summary": "Fleabag is humiliated at the godmother's exhibition, gets dumped, and learns Harry has moved on. Claire stays with Martin, and a confrontation reveals that Fleabag was the woman Boo's boyfriend cheated with; a guilt-ridden Fleabag is stopped from harming herself by the Bank Manager."},
            {"season": 2, "number": 1, "title": "Episode 1",
             "summary": "A year later Fleabag rejoins her family for her father's engagement dinner and is drawn to the priest who will officiate the wedding. Claire has a miscarriage she refuses to admit, Fleabag covers for her, and the sisters end up in hospital."},
            {"season": 2, "number": 2, "title": "Episode 2",
             "summary": "Fleabag's cafe is thriving but the family still treats her dismissively. She attends a counselling session, helps the priest at a garden party, and pursues him despite his vows."},
            {"season": 2, "number": 3, "title": "Episode 3",
             "summary": "Fleabag rescues Claire's awards event and learns about Claire's crush on her Finnish colleague Klare. She flirts with the priest, who refuses to break his celibacy, and he starts to notice her breaking the fourth wall."},
            {"season": 2, "number": 4, "title": "Episode 4",
             "summary": "A pleasant day with the priest ends badly when Fleabag refuses his attempt to help her. She prays at his church, confesses, and they nearly sleep together before he pulls back."},
            {"season": 2, "number": 5, "title": "Episode 5",
             "summary": "The priest withdraws from officiating the wedding, then admits his feelings are more than physical and the two sleep together. In the moment, Fleabag pushes the audience away."},
            {"season": 2, "number": 6, "title": "Episode 6",
             "summary": "At the wedding Fleabag returns the sculpture and learns it was modelled on her mother. Claire leaves Martin for Klare, and at the bus stop the priest admits he loves Fleabag but has chosen God; Fleabag tells the audience to go home."},
        ],
    },
    "series-the-queen-s-gambit": {
        "synopsis": (
            "Scott Frank's seven-part adaptation of Walter Tevis's novel follows Beth Harmon "
            "from an orphanage in 1950s Kentucky, where the janitor teaches her chess and the "
            "state hands out tranquillizers, to the top of the world game in the 1960s. Her "
            "rise through the American circuit and into the Soviet-dominated international "
            "scene is shadowed by an addiction to the pills that first let her see the board "
            "clearly. Anya Taylor-Joy stars as Beth, with Bill Camp as her first teacher and "
            "Marcin Dorocinski as the world champion she must finally beat."
        ),
        "episodes": [
            {"season": 1, "number": 1, "title": "Openings",
             "summary": "Nine-year-old Beth, orphaned after her mother's crash, learns chess from the janitor Mr Shaibel and develops her gift while addicted to the orphanage's tranquillizers. Caught stealing the pills, she collapses, but her talent is already clear."},
            {"season": 1, "number": 2, "title": "Exchanges",
             "summary": "Beth is adopted by Alma and the absent Allston Wheatley. She enters the Kentucky State Championship on money sent by Shaibel, beats state champion Harry Beltik, and Alma begins to manage her career."},
            {"season": 1, "number": 3, "title": "Doubled Pawns",
             "summary": "Beth wins in Cincinnati and gains national notice. At the Las Vegas Open she reunites with the journalist Townes, loses for the first time to Benny Watts, and the two finish as co-champions."},
            {"season": 1, "number": 4, "title": "Middle Game",
             "summary": "Beth plays her first international tournament in Mexico City, beats Soviet prodigy Georgi Girev, and loses to world champion Borgov. She returns to the hotel to find Alma has died, leaving her alone in the house."},
            {"season": 1, "number": 5, "title": "Fork",
             "summary": "Beltik moves in to train with Beth until he accepts she will never choose him over chess. At the U.S. Championship she beats Benny to become national champion and agrees to train with him in New York."},
            {"season": 1, "number": 6, "title": "Adjournment",
             "summary": "Benny trains Beth for the Paris Invitational with his grandmaster friends. A night out with Cleo before the final leaves her hungover, and she loses to Borgov again, then spirals into a bender before Jolene turns up at her door."},
            {"season": 1, "number": 7, "title": "End Game",
             "summary": "Jolene takes Beth to Shaibel's funeral, and the two travel to Moscow for the invitational. Beth beats Borgov in the final, and after a walk through the park she turns down an invitation to stay, choosing to go home."},
        ],
    },
    "series-watchmen": {
        "synopsis": (
            "Damon Lindelof's nine-episode sequel to the graphic novel is set in Tulsa in "
            "2019, where the police wear masks against the white supremacist Seventh Kavalry. "
            "Detective Angela Abar, known as Sister Night, investigates the murder of police "
            "chief Judd Crawford and uncovers a conspiracy that reaches back to the 1921 "
            "Tulsa race massacre and forward to Doctor Manhattan, who is hiding in Oklahoma "
            "as her husband. Jeremy Irons plays Adrian Veidt, imprisoned in a manor on Europa "
            "and trying to get out. The series won eleven Emmys."
        ),
        "episodes": [
            {"season": 1, "number": 1, "title": "It's Summer and We're Running Out of Ice",
             "summary": "The 1921 Tulsa massacre opens the story, and in 2019 Sister Night hunts down the Kavalry shooter who wounded a masked officer. Police chief Judd Crawford is found hanged beneath a tree, with an old man in a wheelchair below him."},
            {"season": 1, "number": 2, "title": "Martial Feats of Comanche Horsemanship",
             "summary": "Angela shelters Will, who claims to be her grandfather, and finds a Ku Klux Klan robe hidden in Judd's closet. A flying craft abducts Will before she can question him, while the imprisoned Veidt stages a play retelling Manhattan's origin."},
            {"season": 1, "number": 3, "title": "She Was Killed by Space Junk",
             "summary": "FBI agent Laurie Blake arrives to investigate Judd's murder and warns Angela not to protect him. Angela's empty car drops out of the sky in front of her, and Veidt tests the limits of his prison in an Ozymandias costume."},
            {"season": 1, "number": 4, "title": "If You Don't Like My Story, Write Your Own",
             "summary": "Lady Trieu takes over the land where an object from space crashes, and Angela leaves Judd's Klan robe and Will's pills with Wade. Laurie ties the evidence to Trieu's Millennium Clock while Veidt grows new servants to probe his prison."},
            {"season": 1, "number": 5, "title": "Little Fear of Lightning",
             "summary": "Looking Glass, a survivor of the 1985 squid attack, is coerced into spying on Angela by the Kavalry, who are building a teleportation device. Angela is arrested for hiding her grandfather, and Veidt spells out a plea with frozen servants on Europa."},
            {"season": 1, "number": 6, "title": "This Extraordinary Being",
             "summary": "The Nostalgia pills flood Angela with Will's memories: his life as a black NYPD officer in 1938, the lynching that made him Hooded Justice, and his one-man war on a Klan mind-control plot. Will used the same technology to make Judd hang himself."},
            {"season": 1, "number": 7, "title": "An Almost Religious Awe",
             "summary": "Angela learns the phone booths to Mars actually reach Trieu, and that Doctor Manhattan is her husband Cal, disguised as a human. She smashes his head to remove the device hiding his powers as the Kavalry move in."},
            {"season": 1, "number": 8, "title": "A God Walks into Abar",
             "summary": "In 2009 Manhattan courts Angela, explaining that he took the identity of Cal and gave himself amnesia until his capture. As the Kavalry attack in 2019 he tells her this is the moment he fell in love with her, and Veidt finds a way to start digging out."},
            {"season": 1, "number": 9, "title": "See How They Fly",
             "summary": "Trieu activates the Millennium Clock to steal Manhattan's power, but he teleports Veidt, Laurie and Wade to Karnak, where the frozen squid rain destroys her device. Told that Manhattan could pass his powers through an egg, Angela eats one and steps onto the pool."},
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
