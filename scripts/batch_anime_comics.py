#!/usr/bin/env python3
"""batch_anime_comics.py — first-drop content for the new Anime / Manga / Manhwa lanes.

Adds hand-curated topics (quirky teaser + personalized explore instruction per
the Curio quality bar) to anime.json / manga.json / manhwa.json. Idempotent:
skips ids that already exist, dedupes by name within each file.

Entry format (tuple):
  (name, year, creator, [tags...], duration_min, tier, teaser, instruction)

- anime:  byline = studio,   verb Watch, tags [genre, genre, "Japanese"]
- manga:  byline = author,   verb Read,  tags [genre, genre, "Japanese"]
- manhwa: byline = author,   verb Read,  tags [genre, genre, "Korean"]

Usage: python3 scripts/batch_anime_comics.py   (run from repo root)
Then:   python3 scripts/validate_topics.py
"""

import json
import re
import unicodedata
from pathlib import Path

ASSETS = Path("app/src/main/assets/topics")

ANIME = [
    # ── Studio Ghibli / film classics ──────────────────────────────────
    ("Spirited Away", 2001, "Studio Ghibli", ["Fantasy", "Adventure", "Studio Ghibli"], 125, 1,
     "Hayao Miyazaki made the film his studio almost died for — it beat Titanic at the Japanese box office and is still the only hand-drawn film to win the Best Animated Feature Oscar.",
     "Watch the bathhouse opening with the sound up — every hiss, bubble and creak is hand-recorded. Then track how Chihiro's name disappears before the money does."),
    ("Princess Mononoke", 1997, "Studio Ghibli", ["Fantasy", "Environmental", "Studio Ghibli"], 134, 1,
     "Miyazaki drew so many frames by hand the studio called it a curse — the forest spirit's shape-shifting alone took months, and there is no real villain, only wounded humans and wounded gods.",
     "Pay attention to the iron town vs. the forest — neither side is wrong, and the film never tells you who to root for. Notice how the deer-god's walk changes after he loses his head."),
    ("My Neighbor Totoro", 1988, "Studio Ghibli", ["Family", "Slice of Life", "Studio Ghibli"], 86, 1,
     "Totoro was nearly a plush toy before he was a character — Miyazaki drew him first as merchandise, then built the movie around him. The bus-stop scene has no score, just rain.",
     "Watch the bus-stop scene in the rain and count how many seconds pass with no music at all. Then rewatch the cat-bus flight and notice the trees bend toward it like they're bowing."),
    ("Grave of the Fireflies", 1988, "Studio Ghibli", ["War", "Drama", "Studio Ghibli"], 89, 1,
     "It opened as a double feature with Totoro — the most brutal double bill in animation history. Isao Takahata based the story on his own wartime childhood, and the tin of fruit drops is real.",
     "Watch the opening cold-open — you know the ending before the film starts, and that changes everything. Notice how the film never shows the bombing, only its afterimages."),
    ("Akira", 1988, "Toho", ["Cyberpunk", "Action", "Sci-Fi"], 124, 1,
     "It took 160,000 hand-painted cells and bankrupted its studio, but it made anime a global genre. The opening bike slide was drawn frame-by-frame with matching tread patterns.",
     "Watch the first five minutes and you'll see the entire plot seeded in the motorcycle geometry — red on black. Then rewind to the slide and count the light streaks."),
    ("Ghost in the Shell", 1995, "Production I.G", ["Cyberpunk", "Philosophical", "Sci-Fi"], 83, 1,
     "The Wachowskis screened it for the Matrix crew before writing their script — the opening title sequence alone was storyboarded to its exact rhythm.",
     "Watch the opening credits: the doll assembly, the eye, the rain — it's a whole film's philosophy in four minutes. The philosophy is the plot, not the action."),
    ("Perfect Blue", 1997, "Madhouse", ["Psychological", "Thriller", "Horror"], 81, 1,
     "Satoshi Kon's debut — the mirror trick in Black Swan is a direct homage. Its editing style of 'cut into the same space from impossible angles' was copied so much it has a name now.",
     "Watch for the hallway scene where Mima passes herself — pause it. Then count how many times the film cuts into a space that shouldn't exist from the angle you just saw."),
    ("Paprika", 2006, "Madhouse", ["Sci-Fi", "Surreal", "Psychological"], 90, 1,
     "Satoshi Kon's last film before his death at 46 — Inception's rotating-hallway fight was built from this movie's parade scene.",
     "Watch the parade sequence and notice the objects are all domestic — refrigerators, dolls, teapots — invading the street. The dream logic is the point; don't look for rules."),
    ("Your Name", 2016, "CoMix Wave Films", ["Romance", "Supernatural", "Drama"], 106, 1,
     "Makoto Shinkai's body-swap romance broke Spirited Away's box-office record, and the comet is drawn with real astronomy — the red thread metaphor is a Japanese wedding tradition.",
     "Watch the 'twilight' scene where the two finally meet and notice the light is real sunset physics. Then rewind to the first body-swap and count the details that prove the other one was there."),
    ("Weathering with You", 2019, "CoMix Wave Films", ["Romance", "Fantasy", "Drama"], 112, 1,
     "Shinkai's rain in this film is so detailed that animators photographed real Tokyo downpours — and the ending deliberately splits from Your Name's, setting it in a drowning world the characters choose anyway.",
     "Watch the last scene on the balcony and notice what the characters choose over the weather. The rain isn't an obstacle — it's the cost."),
    ("Suzume", 2022, "CoMix Wave Films", ["Adventure", "Supernatural", "Drama"], 122, 1,
     "Suzume's doors were drawn from real earthquake-ruined sites across Japan, and the film became a charity drive — screenings donated to 2023 quake relief.",
     "Watch the door-closing ritual and notice the repeated phrase — it's a real Shinto blessing. Each door the film opens is a real place you can still visit today."),
    ("A Silent Voice", 2016, "Kyoto Animation", ["Drama", "Slice of Life", "Romance"], 130, 1,
     "KyoAni's deaf protagonist is played by a hearing actress who learned sign language for a year, and the film's signature X-over-eyes effect started as a single doodle in the manga.",
     "Watch the first time Shoko's world goes silent — the sound design is the whole film. Then notice how the X marks come off faces one by one."),
    ("Violet Evergarden", 2018, "Kyoto Animation", ["Drama", "Romance", "Fantasy"], 90, 1,
     "KyoAni animators hand-painted each letter scene to make the typing feel physical — Violet's prosthetics move with real finger articulation, and the story was written as a light novel by a former scriptwriter for dramas.",
     "Watch the lake scene where Violet types underwater-adjacent letters and count the keystrokes — they're accurate. The film's question is whether a machine can feel what she writes."),
    ("Millennium Actress", 2001, "Madhouse", ["Drama", "Romance", "Historical"], 87, 1,
     "Satoshi Kon's love letter to cinema — the 'actress' role-jumps across eras of Japanese film in single cuts, and the ending reveals the whole chase was about one man she never caught.",
     "Watch the transition from the war film to the space film — it happens in one cut. The film is a history of Japanese cinema disguised as a biography."),
    ("The Tale of the Princess Kaguya", 2013, "Studio Ghibli", ["Fantasy", "Drama", "Studio Ghibli"], 137, 1,
     "Isao Takahata's last film, drawn with charcoal and watercolor in a style that looks unfinished on purpose — the 'sketchy' moments are where the character stops performing.",
     "Watch the running scene at the end — it's charcoal scribbles that somehow convey more emotion than any detailed animation. Notice when the film 'forgets' to finish drawing."),
    ("Nausicaä of the Valley of the Wind", 1984, "Studio Ghibli", ["Environmental", "Adventure", "Sci-Fi"], 117, 1,
     "Ghibli's founding film, made before the studio existed — Miyazaki adapted his own manga and the toxic jungle was his warning about real-world pollution, now read as eerily prescient.",
     "Watch the Ohmu stampede scene and notice the eyes are blue because they're healing, not angry. The film's environmentalism is practical, not preachy — the jungle is alive and so are its monsters."),
    ("Howl's Moving Castle", 2004, "Studio Ghibli", ["Fantasy", "Romance", "Studio Ghibli"], 119, 1,
     "The castle's four legs each animate independently — Miyazaki added the legs because a walking house that only bounced felt dead. Calcifer's fire is hand-drawn with real flame frames.",
     "Watch how the castle's legs each step at their own rhythm — it's four separate animations. Then notice Howl's hair color is tied to his vanity, not his magic."),
    ("Ponyo", 2008, "Studio Ghibli", ["Family", "Fantasy", "Studio Ghibli"], 100, 1,
     "Miyazaki hand-drew most of the tsunami waves himself, refusing digital effects — the waves are real water physics painted by a 68-year-old master frame by frame.",
     "Watch the tsunami at Ponyo's climax and notice every wave has its own personality — one is playful, one is angry. Hand-drawn water that behaves like a character."),
    ("The Wind Rises", 2013, "Studio Ghibli", ["Biography", "Drama", "Studio Ghibli"], 126, 1,
     "Miyazaki's final film (he came out of retirement twice since) about the man who designed the Zero fighter — the film refuses to condemn him, showing the beauty of flight and the horror it enabled.",
     "Watch the earthquake sequence — it's a masterpiece of sound design, everything shakes except the characters' imagination. The film's moral ambiguity is the point, not a flaw."),
    ("Wolf Children", 2012, "Studio Ponoc", ["Family", "Drama", "Supernatural"], 117, 1,
     "Mamoru Hosoda's mother-raising-werewolf-cubs film was inspired by his own decision to become a father — the wolf-father dies in the first act and the film becomes a mother's story.",
     "Watch the scene where Yuki and Ame choose their paths — the film never judges either choice. Notice how the animation style subtly shifts when the children are wolves."),
    ("The Girl Who Leapt Through Time", 2006, "Madhouse", ["Sci-Fi", "Romance", "Slice of Life"], 98, 1,
     "Hosoda's breakthrough — the time-leaps are triggered by a jump, not a device, and the film is a stealth adaptation of a 1967 novel with the same name.",
     "Watch the very first leap and notice she has to run and jump — no button, no countdown. The film's twist is in the last five minutes; resist skipping ahead."),
    ("Summer Wars", 2009, "Madhouse", ["Sci-Fi", "Family", "Adventure"], 114, 1,
     "The online world of OZ was designed to look like a Japanese festival — the film's villain is a rogue AI playing an ancient card game, and the family scenes are real Japanese summer traditions.",
     "Watch the hanafuda game — the card game is real, and the strategy is the plot. The film's real fight is between technology and tradition, and the family wins with food, not firewalls."),
]

MANGA = [
    ("One Piece", 1997, "Eiichiro Oda", ["Adventure", "Shonen", "Pirate"], 0, 1,
     "Oda planned the ending decades ago and the manga is so detail-dense that fans have found characters hidden in backgrounds years before they appear. Volume 1's cover has a message that only made sense 20 years later.",
     "Read the first chapter twice: once for the story, once hunting for Oda's hidden doodles. Then check the cover of volume 1 — there's a promise hidden in the drawing."),
    ("Naruto", 1999, "Masashi Kishimoto", ["Action", "Shonen", "Ninja"], 0, 1,
     "Kishimoto designed Naruto's spiky hair because he couldn't draw good hair — and the character's loneliness is autobiographical. The ramen he always eats is real: Ichiraku Ramen was modeled on a shop near the studio.",
     "Read the fight with Zabuza — it's where the series stops being a kids' cartoon. Pay attention to the hands: Kishimoto draws hands so expressive they carry whole conversations."),
    ("Bleach", 2001, "Tite Kubo", ["Action", "Supernatural", "Shonen"], 0, 1,
     "Kubo is left-handed and draws with a special mirror technique — his panel layouts are so stylish they're studied in art schools, and the series' fashion sense comes from his love of punk and designer clothes.",
     "Read the first chapter and notice the panel rhythm — Kubo uses whitespace like a composer. The sword designs (zanpakuto) each tell the owner's character; compare Ichigo's to Byakuya's."),
    ("Dragon Ball", 1984, "Akira Toriyama", ["Action", "Adventure", "Shonen"], 0, 1,
     "Toriyama invented the Super Saiyan because his editor said the manga was getting boring — and the power level scale started as a joke about the villain's scouters.",
     "Read the first volume and notice Goku is a martial-arts comedy, not an action epic. Then jump to the 23rd Budokai and watch how the fights turned from slapstick to physics."),
    ("Attack on Titan", 2009, "Hajime Isayama", ["Dark Fantasy", "Action", "Dystopian"], 0, 1,
     "Isayama's hometown inspired the walled city, and the reveal about the basement was planned from chapter 1 — the manga's final chapter drew record global attention and split the fandom.",
     "Read the first chapter and notice the foreshadowing in the wall's height and the Titans' smiles. The story's real subject is freedom and who gets to define it — the monsters are never just monsters."),
    ("Death Note", 2003, "Tsugumi Ohba", ["Thriller", "Psychological", "Mystery"], 0, 1,
     "The artist and writer never met in person — Ohba sent scripts and Obata drew, and the iconic rules of the Death Note were invented to make the mind-games fair.",
     "Read the first two chapters and track every rule the notebook introduces — each one exists to make the game solvable. Notice when Light stops being the hero: it's earlier than you think."),
    ("Fullmetal Alchemist", 2001, "Hiromu Arakawa", ["Adventure", "Dark Fantasy", "Steampunk"], 0, 1,
     "Arakawa grew up on a dairy farm and wrote the alchemy rules to mirror real chemistry — the manga's equivalent-exchange law is literally conservation of mass with fantasy dressing.",
     "Read the Nina chapter — it's the moral thesis of the whole series in one gut-punch. Then notice how often equivalent exchange is actually broken; the series is about what it costs."),
    ("Jujutsu Kaisen", 2018, "Gege Akutami", ["Action", "Supernatural", "Dark Fantasy"], 0, 1,
     "Akutami's power system (cursed energy) is explained in a classroom scene that fans still analyze — and the manga's author planned the ending from the first arc.",
     "Read the Shibuya Incident arc for the series' true identity — it's a horror manga wearing a shonen's clothes. Notice how many named characters die off-panel: the author refuses drama, which is the drama."),
    ("Demon Slayer", 2016, "Koyoharu Gotouge", ["Action", "Historical", "Dark Fantasy"], 0, 1,
     "Gotouge wrote the manga while working a part-time job and the anime's episode 19 became the most-discussed episode of the decade — the manga's gentle ending was planned from the start.",
     "Read the final battle and notice how the demons are mourned as humans — the series' kindness is its secret weapon. The swords' colors tell you the user's breathing style; spot the mistake in the anime version."),
    ("Chainsaw Man", 2018, "Tatsuki Fujimoto", ["Action", "Dark Comedy", "Horror"], 0, 1,
     "Fujimoto drew the manga while binge-watching Western films — the panels are framed like movie shots, and the series' most famous scene was inspired by a real dog video.",
     "Read the first arc and notice the film references — the pacing is edited like a Tarantino movie. The chainsaw sounds are drawn, not written: count how the panel borders change when Denji transforms."),
    ("My Hero Academia", 2014, "Kohei Horikoshi", ["Action", "Superhero", "Shonen"], 0, 1,
     "Horikoshi is a Western comic nerd — the hero rankings mirror American comics' power scaling, and the series' symbolism (All Might's smile, the embers) is drawn with a purpose.",
     "Read the Sports Festival arc — it's the series' thesis on what makes a hero when powers are unfair. Notice how often Deku wins by understanding others' powers better than they do."),
    ("Vinland Saga", 2005, "Makoto Yukimura", ["Historical", "Drama", "Viking"], 0, 1,
     "The author spent years researching Viking history and the manga's farm arc — where the protagonist becomes a pacifist — is based on the real Thorfinn Karlsefni's documented life.",
     "Read the farm arc slowly — it's the point of the whole series, not a filler. The violence in the early volumes exists to make the peace meaningful; notice how the art changes once the fighting stops."),
    ("Berserk", 1989, "Kentaro Miura", ["Dark Fantasy", "Horror", "Tragedy"], 0, 1,
     "Miura died at 54 with the manga unfinished — his friend Kouji Mori finished the story from their shared notes, and the Eclipse is widely called the darkest moment in manga history.",
     "Read the Golden Age arc and understand Guts' rage before the Eclipse — the horror is earned. The brand's meaning changes once you know who put it there."),
    ("Monster", 1994, "Naoki Urasawa", ["Thriller", "Psychological", "Mystery"], 0, 1,
     "Urasawa drew every character with real German street fashion and the manga's antagonist was inspired by a real German doctor — the series asks what a person becomes when they choose evil.",
     "Read the first volume and notice the kindness of the hero — it's the setup for the moral question. The villain's name is a diagnosis, not a title."),
    ("JoJo's Bizarre Adventure", 1987, "Hirohiko Araki", ["Action", "Adventure", "Supernatural"], 0, 1,
     "Araki's art is displayed in the Louvre, and each part of JoJo is a new genre — the series' power system (Stands) was invented because the author wanted fights that were more strategic than physical.",
     "Read Part 3's beginning and understand how Stands changed manga fights forever — the strategy is the fight. Araki's fashion is drawn from real magazines; spot the designer references."),
    ("Hunter x Hunter", 1998, "Yoshihiro Togashi", ["Adventure", "Strategy", "Shonen"], 0, 1,
     "Togashi's Chimera Ant arc is considered the greatest shonen arc ever written, and the author's health issues famously pause the manga mid-arc — the nen system is the most analyzed power system in manga.",
     "Read the Chimera Ant arc and notice how the villain's humanity is the point — the ants are more human than the hunters. The nen system rewards planning over power; the fights are chess matches."),
    ("Frieren: Beyond Journey's End", 2020, "Kanehito Yamada", ["Fantasy", "Drama", "Slice of Life"], 0, 1,
     "The manga starts after the hero's quest is already over — the elven mage Frieren outlives her party and the series is about memory and what people leave behind.",
     "Read the first chapter and notice the time-skip trick — decades pass in a page. The series' magic system runs on belief: magic becomes real when people believe it, which is the whole theme."),
    ("Oyasumi Punpun", 2007, "Inio Asano", ["Drama", "Psychological", "Coming of Age"], 0, 1,
     "The protagonist is drawn as a crudely doodled bird while everyone around him is detailed — the visual metaphor is the story, and the manga's ending is one of the most discussed in the medium.",
     "Read the first volume and notice Punpun's design — the crude bird is how he sees himself. The manga is a study of how childhood trauma quietly decides adult choices."),
    ("Gintama", 2003, "Hideaki Sorachi", ["Comedy", "Action", "Historical"], 0, 1,
     "A samurai comedy set in an Edo occupied by aliens — Sorachi's humor is so absurd that the series' serious arcs hit harder because of the joke structure.",
     "Read the first volume expecting comedy, then hit the Benizakura arc and watch the tone shift. The series' humor is the armor; the serious arcs are the wound."),
    ("Blue Lock", 2018, "Muneyuki Kaneshiro", ["Sports", "Psychological", "Soccer"], 0, 1,
     "A soccer manga where 300 strikers compete in a death-game to become the world's best egoist — the author studied real football tactics and the 'ego' philosophy is the actual plot.",
     "Read the first volume and notice the manga treats soccer like a battle — the psychological duels are the real game. The 'ego' concept is a real sports psychology idea."),
    ("Haikyuu!!", 2012, "Haruichi Furudate", ["Sports", "Coming of Age", "Volleyball"], 0, 1,
     "Furudate played volleyball and drew every rotation correctly — the manga's rallies are choreographed so you can follow the ball's physics, and the 'low height' protagonist is the point.",
     "Read the first match against the Seijoh team and track the ball's trajectory — every pass is drawn with correct physics. The series is about height as a metaphor for limits."),
    ("Tokyo Revengers", 2017, "Ken Wakui", ["Action", "Drama", "Gang"], 0, 1,
     "The time-travel gang manga was inspired by the author's own delinquent youth in Tokyo's '90s — the fashion is authentic to the era and the fights are drawn from real street-brawl choreography.",
     "Read the first volume and notice the time-travel rules — every trip costs something. The series' real subject is whether you can save people who don't want to be saved."),
    ("Vagabond", 1998, "Takehiko Inoue", ["Historical", "Martial Arts", "Drama"], 0, 1,
     "Inoue paused the manga to paint — his brushwork in the sword fights is considered the finest in the medium, and the series is a fictionalized biography of Miyamoto Musashi.",
     "Read the 'way of the sword' scenes and watch the art — the fights are drawn with ink strokes that move like the blade. The series' real battle is Musashi's inner one; the duels are externalized."),
    ("Kingdom", 2006, "Yasuhisa Hara", ["Historical", "War", "Strategy"], 0, 1,
     "A retelling of the unification of China through the eyes of a slave who becomes a general — the manga's battle formations are drawn from real Chinese war history and the author's research trips.",
     "Read the first siege arc and track the formations — the strategy is real. The series' thesis is that history is made by the people the history books forget."),
    ("The Promised Neverland", 2016, "Kaiu Shirai", ["Thriller", "Mystery", "Horror"], 0, 1,
     "The author planned the entire manga before writing a single chapter — the first arc's escape is a perfect puzzle that the reader can actually solve.",
     "Read the first arc and try to solve the escape yourself — every rule is seeded in the first chapters. The manga's twist is that the horror is the system, not the monsters."),
    ("Slam Dunk", 1990, "Takehiko Inoue", ["Sports", "Coming of Age", "Basketball"], 0, 1,
     "Inoue's basketball manga is credited with a real spike in Japanese basketball participation — the final match against Sannoh has no music in the anime because the manga's panels are that powerful.",
     "Read the final match against Sannoh and notice the silent panel — the manga's most famous moment has no dialogue. The series is about effort, not talent: watch Sakuragi's fundamentals grow."),
    ("Noragami", 2010, "Adachitoka", ["Supernatural", "Action", "Comedy"], 0, 1,
     "The gods in Noragami are based on real Shinto beliefs and the manga's 'gods die without worshippers' concept is a genuine theological idea drawn from Japanese folk religion.",
     "Read the first volume and notice the shrine — gods are sustained by belief, and the series treats faith as a real currency. The fights are theology made physical."),
    ("Fire Force", 2015, "Atsushi Ohkubo", ["Action", "Supernatural", "Sci-Fi"], 0, 1,
     "Ohkubo (of Soul Eater fame) draws fire as a living character — the 'infernals' are humans who burned alive, and the series' real mystery is the First Pillar.",
     "Read the first arc and notice the fire's personality — it's drawn as an emotion, not an element. The series' secret is that the 'devil' is the system itself."),
    ("Dorohedoro", 2000, "Q Hayashida", ["Dark Fantasy", "Gore", "Comedy"], 0, 1,
     "The author draws everything with ink and screen tone in a style so dense that fans call it 'the manga that looks like a fever dream' — the worldbuilding is anarchic and everything has a rule.",
     "Read the first volume and just surrender to the world — the rules reveal themselves. The series is about who gets to decide what's magic and what's crime."),
    ("Made in Abyss", 2012, "Akihito Tsukushi", ["Adventure", "Dark Fantasy", "Sci-Fi"], 0, 1,
     "Tsukushi self-published the first chapters at Comiket before getting a publisher — the Abyss's curse mechanic is the series' thesis: depth has a price, and curiosity pays it.",
     "Read the first volume and notice the curse system — every descent has a cost, and the series never forgets it. The cute art is the trap; the Abyss is the real character."),
    ("Kaguya-sama: Love Is War", 2015, "Aka Akasaka", ["Romance", "Comedy", "Psychological"], 0, 1,
     "The rom-com is structured as a mind-game — each chapter is a battle of psychological warfare between two people too proud to confess, and the author later wrote the darker Oshi no Ko.",
     "Read the first few chapters as a strategy game — the narrator's '4D chess' framing is the joke. The series' genius is that the mind-games are actually about vulnerability."),
    ("To Your Eternity", 2016, "Yoshitoki Oima", ["Fantasy", "Drama", "Philosophical"], 0, 1,
     "Oima (A Silent Voice's author) asks what it means to be human through an immortal shapeshifter — the manga's first chapter is a single, devastating relationship.",
     "Read the first chapter alone — it's a complete tragedy in one sitting. The series' thesis is that identity is formed by the people you lose."),
]

MANHWA = [
    ("Tower of God", 2010, "SIU", ["Fantasy", "Action", "Adventure"], 0, 1,
     "The manhwa that made webtoons global — SIU wrote it while working as a delivery driver, and the Tower's floors are so detailed that fans have maps.",
     "Read the first floor climb and notice the rules — the Tower is a system that rewards cleverness over power. The series' twist is that the 'god' of the title is the Tower itself."),
    ("Solo Leveling", 2018, "Chugong", ["Action", "Fantasy", "System"], 0, 1,
     "The 'system' manhwa that spawned a genre — the hunter E-rank who levels up alone became a template copied by hundreds of series. The art studio Redice drew every fight to look cinematic.",
     "Read the first chapters and notice the E-rank humiliation — the power fantasy only works because the powerless start is real. The system is the story; the stats are the plot."),
    ("The God of High School", 2011, "Yongje Park", ["Action", "Martial Arts", "Tournament"], 0, 1,
     "The tournament arc is the whole first season — and the manhwa's secret is that the 'god' powers are a bait-and-switch for a much bigger mythic war.",
     "Read the first tournament and track the martial arts styles — each fighter has a real discipline. The series' twist is that the tournament is a recruitment tool for gods."),
    ("Noblesse", 2007, "Jeho Son", ["Action", "Supernatural", "Vampire"], 0, 1,
     "A vampire nobleman wakes after 820 years and enrolls in high school — the manhwa's action scenes are famous for their white-space fights where the panel IS the impact.",
     "Read the first school arc and notice the power disparity — Rai's strength is the joke. The series' real theme is found family, and the fights exist to protect it."),
    ("The Breaker", 2007, "Geuk-Jin Jeon", ["Martial Arts", "Action", "Murim"], 0, 1,
     "The 'murim' (martial arts underworld) manhwa that western fans discovered through scanlations — the sequel, New Waves, is considered the best of the trilogy.",
     "Read the first volume and notice the master-student dynamic — the series is about inheritance of violence. The fights are choreographed like wuxia films; the bruises are real."),
    ("Sweet Home", 2017, "Kim Carnby", ["Horror", "Survival", "Drama"], 0, 1,
     "Kim Carnby's monster apocalypse where people turn into monsters that embody their desires — the Netflix adaptation kept the manhwa's core: the monster is the wish.",
     "Read the first chapters and match each monster to its human's desire — the wish is the clue. The series' horror is that the monsters are more honest than the humans."),
    ("Bastard", 2014, "Kim Carnby", ["Thriller", "Psychological", "Horror"], 0, 1,
     "A serial killer's son who discovers his father's secret — the manhwa's tension is built entirely on what the reader knows that the characters don't.",
     "Read the first volume and notice the dramatic irony — you know the truth before the protagonist does. The series is a study of complicity; the killer is the least of the horrors."),
    ("Lookism", 2014, "Park Tae-joon", ["Drama", "Action", "Social"], 0, 1,
     "A bullied overweight teen wakes in a handsome body — the manhwa's fantasy premise is a social commentary on Korean beauty standards, and the action arcs hide real psychological insight.",
     "Read the first volume and notice the body-switch rules — the 'ugly' body and 'handsome' body are the same person. The series asks what changes when the world treats you differently."),
    ("Unordinary", 2016, "uru-chan", ["Superhero", "Drama", "Action"], 0, 1,
     "A superhero-school webtoon where the protagonist is the one 'normal' kid in a world of powers — the series' commentary on power hierarchies is the actual plot.",
     "Read the first arc and notice how the world treats the powerless — the hierarchy is the villain. The series' twist is that John's 'normalcy' is a performance."),
    ("The Beginning After the End", 2018, "TurtleMe", ["Fantasy", "Action", "Isekai"], 0, 1,
     "An isekai where the protagonist reincarnates and learns magic — the manhwa's art and the novel's depth made it one of the most popular webtoons on the platform.",
     "Read the first arc and notice the 'second chance' framing — the protagonist's past life is the tragic part. The series' strength is the slow worldbuilding; don't skim the magic school."),
    ("Eleceed", 2018, "Jeho Son", ["Action", "Comedy", "Superhero"], 0, 1,
     "A cat-man superhero mentor comedy from the Noblesse artist — the 'awakened' power system and the cat mentor's comedy hide a real found-family story.",
     "Read the first volume for the cat jokes, then notice the mentor's past — the comedy is armor. The series' fights are drawn with the same white-space impact as Noblesse."),
    ("Omniscient Reader's Viewpoint", 2020, "sing N song", ["Fantasy", "Action", "System"], 0, 1,
     "A novel reader wakes inside the apocalypse novel he read — the manhwa's meta-commentary on stories and readers is the actual plot, and the 'sponsor' system is a brilliant conceit.",
     "Read the first chapters and notice the 'sponsors' — they're watching the reader watch the story. The series' thesis is that the reader is the most important character."),
    ("Wind Breaker", 2013, "Yongseok Jo", ["Sports", "Action", "Drama"], 0, 1,
     "A bicycle racing manhwa (not the high school one) with a gang twist — the cycling scenes are drawn with real physics and the author's passion for biking is in every panel.",
     "Read the first race and track the drafting — the bike tactics are real. The series' secret is that the gang violence and the racing are the same struggle."),
    ("The Boxer", 2019, "JH", ["Sports", "Psychological", "Drama"], 0, 1,
     "A boxing manhwa about talent vs. hard work — the 'genius' protagonist is a question the series keeps asking: what is a person who never struggles?",
     "Read the first fight and notice the talent gap — it's not a training montage series. The series' thesis is about what 'talent' really means, and the answer is darker than you'd expect."),
    ("Gosu", 2015, "Mun Jeong-ho", ["Martial Arts", "Action", "Murim"], 0, 1,
     "A martial arts revenge story where the protagonist is already the strongest — the series' comedy comes from the power gap, and the murim politics are the real plot.",
     "Read the first arc and notice the comedic power-gap — the series is a power fantasy with a brain. The real fights aren't the physical ones; they're the political ones."),
    ("Her Summon", 2017, "Mogoon", ["Romance", "Fantasy", "Drama"], 0, 1,
     "A painter who can summon his paintings into reality — the series' art is its selling point, and the painter's 'summons' mirror his emotional state.",
     "Read the first chapters and notice the summoning rules — the paintings are the painter's soul. The romance is secondary to the art; the real love story is the painting."),
    ("The Max Level Hero Has Returned!", 2021, "Ray Halim", ["Fantasy", "Action", "Revenge"], 0, 1,
     "A max-level hero returns to a powerless world and the 'revenge' is against nobles who mocked him — the manhwa's satisfaction comes from the level gap being unfair on purpose.",
     "Read the first arc and notice the power-system joke — the world can't comprehend his level. The series is a power fantasy with a political point about inherited power."),
    ("DICE: The Cube that Changes Everything", 2013, "Hyunseok Song", ["Fantasy", "Drama", "System"], 0, 1,
     "A webtoon where dice grant stats in real life — the manhwa's social commentary on Korean education and status anxiety is the real plot hidden under the game mechanics.",
     "Read the first volume and notice what the dice cost — stats come from other people's loss. The series' horror is that the game is a metaphor for real social systems."),
    ("The Remarried Empress", 2019, "Alphatart", ["Romance", "Drama", "Fantasy"], 0, 1,
     "The empress's husband divorces her for a mistress and she remarries — the manhwa's revenge is quiet and legal, and the 'remarried empress' became the face of the genre.",
     "Read the first chapters and notice the political chess — the 'revenge' is courtly, not bloody. The series' pleasure is watching a woman win by being more competent than everyone around her."),
    ("Solo Farming in the Tower", 2023, "World's End", ["Fantasy", "Action", "System"], 0, 1,
     "A failed hunter who becomes a 'farmer' inside the tower — the manhwa's farming mechanics are the actual plot, and the tower's ecology is built like a real ecosystem.",
     "Read the first chapters and notice the farming system — the 'weak' class is the clever one. The series' thesis is that the tower rewards patience, not power."),
    ("The Constellation That Returned From Hell", 2023, "Ggoba", ["Fantasy", "Action", "Revenge"], 0, 1,
     "A hunter who returns from hell with the power of constellations — the manhwa's art is its signature and the constellation system is the plot engine.",
     "Read the first arc and notice the constellation mechanic — each one has a personality. The series is a revenge fantasy with a cosmic bureaucracy."),
]


def _slug(text):
    text = unicodedata.normalize("NFKD", text).encode("ascii", "ignore").decode().lower()
    text = re.sub(r"[^a-z0-9]+", "-", text).strip("-")
    return text or "topic"


def _build(file_name, category_id, subtype, verb, rows):
    path = ASSETS / file_name
    existing = json.loads(path.read_text(encoding="utf-8")) if path.exists() else []
    existing_ids = {t["id"] for t in existing}
    seen_names = {t["name"] for t in existing}
    added = 0
    for (name, year, creator, tags, duration, tier, teaser, instruction) in rows:
        display = f"{name} ({year})" if year else name
        if display in seen_names:
            continue
        base = f"{subtype.lower()}-{_slug(name)}"
        tid = base
        n = 2
        while tid in existing_ids:
            tid = f"{base}-{n}"
            n += 1
        existing_ids.add(tid)
        seen_names.add(display)
        full_tags = list(tags)
        if year and not any(t.endswith("s") and t[:-1].isdigit() for t in full_tags):
            full_tags.append(f"{year // 10 * 10}s")
        existing.append({
            "id": tid,
            "categoryId": category_id,
            "subtype": subtype,
            "name": display,
            "teaser": teaser,
            "imageUrl": "",
            "byline": creator,
            "exploreAction": {
                "verb": verb,
                "targetName": f"{name} ({year}) end-to-end" if year else f"{name}",
                "durationMinutes": duration if duration else 0,
                "instruction": instruction,
            },
            "tags": full_tags,
            "tier": tier,
        })
        added += 1
    # ExploreAction durationMinutes must be > 0 for reading manga — clamp 0 to 45.
    for t in existing:
        if t["exploreAction"]["durationMinutes"] == 0:
            t["exploreAction"]["durationMinutes"] = 45
    path.write_text(json.dumps(existing, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"{file_name}: +{added} (total {len(existing)})")


def main():
    _build("anime.json", "ANIME", "Anime", "Watch", ANIME)
    _build("manga.json", "MANGA", "Manga", "Read", MANGA)
    _build("manhwa.json", "MANHWA", "Manhwa", "Read", MANHWA)


if __name__ == "__main__":
    main()
