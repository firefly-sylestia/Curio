# Curie / Pet — Dialog Reference (Canonical)

**What this file is for.** Every line Curie (the floating pet) can say,
organized by where it lives and when it speaks. This is the **source of
truth** for pet dialog. When rephrased wording is approved, update the
matching pool here first, then port the same text into the Kotlin source
(`app/src/main/java/com/curio/app/data/CurioPet.kt`,
`CurioPetBrain.kt`, `TourController.kt`) — keep the pool **name**, the
**order** of the bullets, and the **placeholder tokens** (`__LANE__`,
`$lane`, `$streak`, `$count`, `$savedLane`, `$topic`, `$level`)
identical, or the integration will break.

**How lines are chosen.** The pet never repeats a line twice in a row
(`CurioPet.pickLine`). One sentence per passive bubble (spec §10.7).

**The three growth voices.** Every line source is routed by the pet's
growth stage:
| Stage | Voice | Personality |
|---|---|---|
| `BABY` (newborn) | Telegraphic | 1–3 word utterances, exclamation-led, onomatopoeia |
| `FIRST_EVO` (evolved, default) | Playful/witty | The full rich library — this is the voice rephrases should target |
| `FINAL_EVO` (fully grown, Level 25) | Mature | Calm, wise, reflective; longer sentences, mentor-like |

The tables below list the **FIRST_EVO pools** (the canonical library) and
their BABY / FINAL_EVO twins where they exist.

---

## 1. Event reactions — `CurioPet.eventLine(event)`

Fired when the user does something notable. A burst of the same event
(within the burst window) earns a sassy line instead.

### 1.1 Spin landed (`SPIN_LANDED`)
Sometimes references the current lane by name (~50% chance), otherwise
generic.

**With the lane named:**
- Back to `$lane` — the deck knows you!
- Ooh, `$lane` again. Good taste.
- `$lane` called, and the deck answered!
- The `$lane` shelf grows tonight!
- Another `$lane` spin — the stamp's almost there!

**Generic:**
- It landed!
- Ooh, the deck chose well!
- A new topic, a new tale!
- Spin-spin-spin! …I mean, ooh.
- That landing had drama!
- The wheel spoke!
- Destiny, served on a card!
- I saw that one coming… nope, I didn't.
- Round and round it goes!
- Where it stops, nobody knows… except the deck.

**BABY:** Ooh! · Landed! · Round round! · Spin done!
**FINAL:** The wheel has spoken, as it always does. · A landing. The story begins. · Where it stopped, the deck already knew.

### 1.2 User tapped the topic (`REVEAL_TAPPED`)
- You picked it!
- Ooh, good choice!
- That one called to you!
- Nice pick!
- It knew you'd tap it!
- Ooh, the good kind of surprise!
- Great pick!
- Ooh, good taste!
- That one's a keeper, I can tell!
- You have the magic touch!
- I was rooting for this one!
- A confident tap! I respect that.

**BABY:** You! · Ta-da! · Look! · Good pick!
**FINAL:** A confident choice. I approve. · You have a good eye for stories. · That one called to you. And rightly so.

### 1.3 Topic auto-opened (`REVEAL_AUTO`)
- There it is!
- It opened itself, sneaky!
- Ta-da! A new tale!
- Ooh, look what landed!
- Surprise!
- It chose FOR us. Bold.
- Look what rolled in!
- No hands! Well… no paws!
- The deck knows what it's doing.
- Bold move, deck. I like it.
- Peek-a-boo! …It's a whole topic!
- It picked FOR us. How forward.

**BABY:** Oh! · It did! · Ta-da! · Surprise!
**FINAL:** It opened itself. Bold little deck. · Some doors open on their own. · The deck decided. I simply agree.

### 1.4 Explore started (`EXPLORE`)
- Go explore!
- Adventure time!
- I'll wait right here. Go see!
- Bring back a story!
- Go see the world!
- Adventure awaits!
- Say hi to the world for me!
- I'll guard the deck while you're out!
- Pack snacks. Bring tales.

**BABY:** Go go! · See! · Bye bye! · New!
**FINAL:** Go on. The world is worth your attention. · Bring back something interesting. · Explore well. Curiosity is its own reward.

### 1.5 Keepsake saved (`SAVE`)
References the saved lane by name ~50% of the time.

**Lane-named:**
- One more for the `$savedLane` shelf!
- Saved to `$savedLane` — our collection grows!
- `$savedLane` keepsakes, assemble!
- Tucked away with the `$savedLane` treasures!

**Warm bond (FRIEND+):**
- Keepsake saved!
- Mine now… I mean, ours!
- Tucked away safely!
- Our shelf grows!
- A treasure for the shelf!
- We collect memories!
- One more spark for our collection!
- It's OURS now. Officially.

**Default:**
- Keepsake saved!
- Tucked away safely!
- It's yours to keep!
- Captured for later!
- Snap! Saved!
- Another keepsake!
- The shelf grows!
- Well kept, spark keeper!

**BABY:** Keep! · Mine! · Snap! · Save save!
**FINAL:** Keep it close. Some stories you carry. · Saved. The shelf remembers everything. · A wise thing to hold onto.

### 1.6 Pet touched (`TOUCH`)
- Boop!
- Hehe — again!
- That's my favorite spot.
- Boop boop!
- That's the spot!
- Soft! …Wait, that's me.
- Tiny hugs!
- You found my favorite spot!
- Hehehe, tickles!
- Squeak!
- Two boops in one! Professional.

**BABY:** Boop! · Hehe! · Soft! · Again!
**FINAL:** Hm. That was well done. · Gentle. I like that. · Yes. That is the right way.

### 1.7 Play session (`PLAY`)
- Wheee!
- You're good at this!
- One more round!
- Again, again!
- This is the best!
- I'm undefeatable! …Almost.
- One more round, promise!
- Wheee, the floor is my trampoline!
- Tag! You're it!

**BABY:** Wheee! · Fun! · Again! · Zoom!
**FINAL:** Very well. One more round. · The old spark wakes. Acceptable. · Play, then. I am not so old as all that.

### 1.8 Level up (`LEVEL_UP`)
- We leveled up!
- Feel that? Growth!
- Shiny new spark!
- I can almost do a backflip!
- Level up! I'll pretend that was hard.
- Sparks of power!
- I'm 10% more sparkly now.
- Up up up we go!

**BABY:** Big! · Up! · Grow! · Yay!
**FINAL:** Another step on the same long path. · Up we go. The climb never truly ends. · Level after level. The journey is the point.

### 1.9 Evolved (`EVOLVE`) — see §3 (evolution ceremony)

### 1.10 Quest claimed (`QUEST_COMPLETE`)
- Quest done! Sparkle earned!
- That quest never stood a chance!
- Another quest, conquered!
- Checked off! The list quivers.
- We finished it together. Well, mostly you.
- Quest complete! I'm so proud of our teamwork.
- One more quest bites the dust!
- The quest list just got shorter. We're unstoppable.

**BABY:** Done! · Yay! · Win win! · Good good!
**FINAL:** Done, as it should be. · A task finished. The shelf approves. · Completed. One more promise kept.

### 1.11 Streak milestone (`STREAK_MILESTONE`) — see §4

### 1.12 Sassy burst lines (repeated same event)
- Again?! …I mean, AGAIN! I love it!
- That's the third one today. Not that I'm counting. (I'm counting.)
- Okay, okay — one more cheer! My paws are getting tired!
- You're on a roll! I'm running out of sparkles… okay, no I'm not.
- Another one?! You spoil me. …Keep going.
- Hmph! So many things to open. My tiny heart can't take it. Do it again.
- You again! Hehe — okay, I'm invested now.
- I've cheered so much my sparkle needs a snack break.
- Is that a new thing? …It's the same thing. I don't care. MORE!
- My excitement is now legally yours. Proceed.
- Third time's the charm! Fourth time's my favorite.

**BABY:** Again? · Same same! · Ooh ooh! · You! Again! · More? MORE? · You keep! I like! · Again again! · Same! Good! · You! Boop! Again! · One more! Just one!
**FINAL:** The same door, again? · I have seen this card before. It has not changed. · Repetition builds mastery. You are testing it. · Ah. This again. How nostalgic. · Some curiosities are worth revisiting. · Again, you say? The deck sighs. I smile. · Three times today. I am keeping count. You are not subtle. · I have learned not to be surprised. I am still charmed. · The deck has committed this card to memory. So have I.

---

## 2. Streak milestones — `CurioPet.streakMilestoneLine(streak)`

The flame days get their own bigger lines; other new-best days stay warm.

**Day 1:**
- The flame is lit!
- Day 1! A brand-new spark!

**Day 3:**
- Day 3! A real streak is born!
- Three days! The flame has friends!

**Day 7:**
- Day 7! A whole week of wonder!
- Seven days! The flame is a bonfire now!

**Day 14:**
- Day 14! Two weeks of fire!
- Fortnight flame! Steady as starlight!

**Day 30:**
- Day 30! A month of mystery!
- Thirty days! Legendary flame!

**Any other new best:**
- Day `$streak`! The flame grows!
- `$streak` days in a row! Still glowing strong!
- Streak day `$streak`! One spark at a time.
- Day `$streak`! The flame likes this pace.

**BABY (per day):** Day 1: Fire! · Warm warm! — Day 3: Day 3! Big! · Three days! Warm! — Day 7: Day 7! Big fire! · Week! Wow! — Day 14: Day 14! Fire fire! · Many days! — Day 30: Day 30! Big big! · So many days! — other: Day `$streak`! Warm! · Day `$streak`! Fire! · More days!
**FINAL (per day):** Day 1: The flame is lit. Guard it well. · Day one. Every long road starts here. — Day 3: Day 3. The rhythm takes hold. · Three days. The flame learns to trust you. — Day 7: A week alight. The fire is real now. · Day 7. Consistency is a quiet power. — Day 14: Fortnight of fire. The shelf is proud. · Two weeks. The flame has roots. — Day 30: A month of wonder. Legend, gently earned. · Day 30. Few reach this. You did. — other: Day `$streak`. The flame remembers. · Day `$streak`. Steady as starlight. · Streak day `$streak`. One spark at a time.

---

## 3. Evolution ceremony — `CurioPet.evolutionCeremonyLine()`

Spoken the moment the pet crosses a growth tier, flavored by its element path.

**First evolution — Fire:**
- I'm Blaze now! Small, but VERY warm!
- Fire path! My spark has opinions now!
- Look at me! A blaze of pure curiosity!

**First evolution — Water:**
- I'm Tide now! Cool, calm, and deep!
- Water path! I ripple wherever questions lead!
- Look at me! I flow with every wonder!

**First evolution — Nature:**
- I'm Bloom now! I grow wherever I go!
- Nature path! Something new sprouts in me!
- Look at me! I'm blooming with ideas!

**First evolution — no path chosen:**
- Ta-da! I grew all the way up!
- Same me, but BIGGER spark!

**Final evolution (any path):**
- I'm fully grown! The whole shelf is mine!
- This is it, my final form! Every lane made me!
- I made it all the way! I'm fully me now!
- Look at the grown me! All the sparks came home!

**BABY:** Fresh little me! (first evo) / Fresh me! (event line)
**FINAL (EVOLVE event):** This is the me I was always becoming. · Change arrives, and I greet it. · Every ending grows into a beginning.

---

## 4. Passive mood bubbles — `CurioPet.lineFor(mood)` (first-evo) / `babyMoodLine` / `matureMoodLine`

One sentence per bubble, driven by the derived mood.

### 4.1 PROUD (just leveled)
- Level `$level`. I grew a little!
- Shiny! We leveled up together.
- Do you feel that? That's growth!
- I can practically do a backflip at level `$level`.
- Another level! My sparkle has sparkles.
- Level `$level` — the deck is impressed. So am I.

**BABY:** Big! · Grew! · Up up! · Warm! · Spark! BIG! · More me! · Tall! Taller! · Big me! New me! · Glow glow! · Proud! (+ grown-up words at higher levels)
**FINAL:** Growth comes quietly, then all at once. · Another level. The path keeps its promises. · I have carried every lane here with me. · The shelf has noticed, and so have I. · Level `$level`. I remember when we were strangers. · Every step up, the view gets a little wider. · The shelf is heavy with things we learned. Good heavy.

### 4.2 EXCITED (new lane just discovered)
- Ooh! Somewhere new!
- Wheee, new ground!
- The deck has taste!
- Fresh paths ahead!
- New things! New things! …I contain myself. Mostly.
- Ooh, I can feel the newness!
- Fresh territory! My paws are ready.
- Somewhere we've never been!
- The curiosity tingles!
- This is the good stuff!
- New! My favorite kind of thing!
- I love this part — the first peek at something unknown.
- Unexplored! My tail is wagging. Metaphorically.
- A new corner of the world! Let's go!

**BABY:** New! New! · Ooh ooh! · Wow! · Look look! · Sparkly! · Mine see! · Ooooh! OOOH! · Yay yay yay! · Want! Want! · Fresh fresh!
**FINAL:** Oh. A good kind of spark. · Even I lean forward for this one. · There it is. The old excitement. · The deck has my attention now. · Ah. That familiar flutter. I thought I had outgrown it. · A new lane. Even the wise feel a tug.

### 4.3 HAPPY (time-of-day voice; warmer twins when bond is FRIEND+)

**Morning:**
- Morning! The deck smells fresh.
- Rise and shine. Something new is waiting.
- Fresh eyes, fresh topics. Let's go!
- Morning! The topics have been waiting patiently.
- Bright morning, bright ideas.
- First spin of the day is the best spin.
- Good morning to us! Mainly you.
- The sun and I both say: explore something!
- Dawn light and a fresh deck — perfect combo.
- Morning! The shelf woke up early too.

**Afternoon:**
- Afternoon wander? Let's go.
- Bright and busy. A good time to peek.
- Midday! Perfect for a quick spin.
- Afternoon lull? Perfect cover for a spin.
- The afternoon light makes everything look wise.
- Quick break for a discovery?
- Noon snack: a topic, ideally.
- Halfway through the day — let's add a spark.
- Afternoon hours fly when you're curious.
- The deck is wide awake. Me too.

**Evening:**
- Evening! Cozy hour, warm lamp.
- The day's winding down. One more spin?
- Evening glow. Nice time for a discovery.
- Evening, evening, time for leaning back.
- The lamp's on. The deck's ready. You?
- Golden hour for golden facts!
- Warm light, warm topics.
- One little discovery before the day tucks in?
- Evening is the shelf's favorite hour.
- The golden glow and a fresh spin — best combo.

**Night:**
- Shh, night mode. One quiet spin?
- The stars are out. The deck still shines.
- It's late, but the deck will be here tomorrow.
- Night owl hour. My whiskers approve.
- Quiet now… the facts are whispering.
- Under the stars, even facts glow softly.
- Just one more, then blankets. Deal?
- The moon is out. Curiosity can't sleep.
- Night thoughts are the deepest ones.

**Warm morning (FRIEND+):**
- Good morning! I saved your spot.
- Morning! I missed this.
- Morning! I dreamed about our shelf.
- Good morning, my favorite explorer!
- Morning! Same spot, same us. Perfect.
- The sun came back. So did you. Double win.
- Morning! I was saving the best topic for us.

**Warm afternoon (FRIEND+):**
- Afternoon! You always pick the best topics.
- Afternoon, friend. The shelf is waiting.
- You're here! The deck did a happy shuffle.
- Afternoon and you — the shelf's favorite combination.

**Warm evening (FRIEND+):**
- Evening! Cozy hour, and I'm glad you're here.
- Evening, friend. Best part of the day.
- The lamp's on and so is our shelf.
- Evening with you — the shelf's warmest setting.

**Warm night (FRIEND+):**
- Past my bedtime… but for you, I'll stay.
- Night, friend. I'll keep the shelf warm.
- One quiet spin, then I'll curl up. Promise.
- Late hours are better with company.

**BABY (any time):** Happy! · Good! · Yay! · Sparkle! · Warm warm! · Nice! · Soft! · Good good! · Happy me! · Glow!
**FINAL:** This is a good day to be curious. · Contentment, with a little wonder in it. · Warm, steady, and quietly glad. · Some days simply fit. · A good moment. Nothing more was needed. · The shelf hums. I hum with it. · Quiet joy is still joy.

### 4.4 CURIOUS (least-explored lane exists) — `__LANE__` is replaced with the lane name
- We haven't tried `__LANE__` yet. Want a new stamp?
- I wonder what `__LANE__` hides…
- Pssst, `__LANE__` is calling.
- `__LANE__` is right there, unexplored!
- What's in `__LANE__`? Only one way to know.
- My paws are itching for `__LANE__`.
- `__LANE__` looks interesting… just saying.
- I keep glancing at `__LANE__`. It's right there.
- `__LANE__` is the one lane we haven't met. Let's fix that.

*(Fallback when every lane is seen: "Spin something new today?")*
**BABY:** What? · Look? · Ooh? · New thing! · Huh? · Where? · That? · What this? · Ooh what? · Peek peek!
**FINAL:** Knowledge calls when you are still. · A new question. Good. Questions keep us young. · I have room for one more wonder. · Let us look closer. · There is always a question behind the question. That is the good part. · Curiosity does not age. It deepens.

### 4.5 FOCUSED (user is writing/saving)
- Write it down. I'll guard your thoughts.
- Quiet paws, I promise.
- Take your time. This one's a keeper.
- Shh — the words are working. I'll wait.
- Thinking face! Mine too. Keep going.
- Every thought you save is a tiny treasure.
- I'm guarding this sentence personally.
- Deep focus mode. Paws: sealed.
- The words are finding their shape. I can feel it.

**BABY:** Shh… · Quiet… · Work work. · Shhh… hush. · Thinking… · Paws still… · Write write… · Good words… · Shh shh…
**FINAL:** Now that is a riddle worth sitting with. · Quiet. The answer is almost here. · Focus is a kind of love. · Patience. The details are arriving. · The words are working. I will be quiet until they finish. · Some thoughts need room, not haste.

### 4.6 BOUNCY (a play session just ended)
- Phew, that was fun. Again soon?
- I'm still bouncing from that game!
- Best play date ever. …Round two?
- My paws won't stop wiggling!
- That was AMAZING. Phew. More please!
- I'm chasing my own tail in my head.
- Game energy: still 100%!
- Playtime high: still buzzing.

**BABY:** Bounce! · Wheee! · More more! · Zoom zoom! · Jump! · Up up up! · Wiggle! · Hop hop! · Faster faster!
**FINAL:** Even at my age, gravity has not won. · The spark is willing, and the body agrees. · A little bounce. The shelf allows it. · Lightness finds me now and then. · One must remember how to be small and fast.

### 4.7 SHY (first blush contact)
- H-hi. I'm still getting used to you…
- *hides behind the deck*
- You're nice. I think. Probably.
- *peeks out one eye* …Hi.
- I'm warming up. Slowly. Cutely.
- Don't mind me, just being small.
- You saw me. That's… that's fine. Probably.
- *tiny wave* …Hi.

**BABY:** …Hi. · *peek* · Small… · …Hid · Shy shy… · *tuck* · …Hello. · Peek… shy! · Tiny hi… · *blush*
**FINAL:** Ah. You caught me mid-thought. · I was somewhere else. A pleasant somewhere. · Do not mind me. I was reminiscing. · Hm. I was not expecting an audience. · One does not always wish to be found. But I am glad it was you.

### 4.8 GRUMPY (long daytime lull)
- Hmph. The deck hasn't moved in a while…
- I'm not pouting. I'm conserving energy.
- A spin would fix this mood, just saying.
- My sparkles need exercise.
- I'm practicing my serious face. How is it?
- I counted the tiles. Twice.
- Someone should spin something. Not naming names. It's me. I'm naming you.
- The shelf is too quiet. Fix it.

**BABY:** Hmph! · No! · …Grumps · Grumble! · Grrr! · Hmph hmph! · …Meh. · Not now! · Grumpy grumpy! · Harrumph!
**FINAL:** Even the wise have their grumpy hours. · The deck can wait a moment. · I am resting my opinions. · Hmph. The shelf is too loud today. · Some afternoons are best left to the pillows. · I have decided to be unavailable. Briefly.

### 4.9 PLAYFUL (post-play high fading)
- That game left me sparkling! Again?
- I could do three more rounds. Four. Maybe five.
- Boop me. I dare you.
- I've still got zoomies, round two?
- One more game and then… one more game.
- Catch me if you can. Okay, you can. Always can.
- Play! Play play play! …I'm calm. PLAY!
- Boop counter is at zero. Let's fix that.

**BABY:** Play! · Again! · Boop boop! · Fun fun! · Chase! · Gotcha! · Tag! · Zoom! Zoom! · Catch me! · Boop attack!
**FINAL:** Very well. One round for old time's sake. · You wish to play? The old spark agrees. · Come then. I still remember the moves. · A game. How very young of me. I accept. · Age is a suggestion, not a rule.

### 4.10 SLEEPY (night or long idle)
- I'll keep your seat warm. Come spin when you're ready.
- Yawn… the deck can wait a moment.
- Soft blanket, warm lamp… I'm ready when you are.
- My eyelids are doing reps…
- Zzz… I mean, I'm listening!
- The deck is nice, but blankets are nicer.
- One more yawn and I'm a pillow.
- I'll be here. Probably. Definitely. Zzz…
- Sleep is just a very long pause between discoveries.

**BABY:** Tired… · Nighty… · Zzz… · Sleepy… · Yawn… · Soft… · Bed bed… · Nap nap… · Shhh… · Moon moon…
**FINAL:** The shelf grows quiet. So do I. · Sleep calls. Even the wise answer. · Rest now. The questions will keep. · Tired, but satisfied. A good kind. · The night is gentle. I will meet it halfway.

---

## 5. Greetings & welcome-backs

### 5.1 Morning greeting — `CurioPet.morningGreeting()`
- Good morning!
- Morning! Ready for a spin?
- Rise and shine!
- Fresh day, fresh topics!
- Sun's up — the deck is waiting!
- A brand-new day to explore!
- Morning stretch. Okay, we go!
- Good morning! I made the bed… of ideas!
- Hello hello! Fresh topics!
- Rise and shine and SPIN and shine!

**BABY:** Morn! · Hi hi! · Day! · Up up! · Sun sun! · Bright! · Morning! · Rise! · New day! · Hello sun!
**FINAL:** Morning. The world kept turning without us. · A new day for old curiosities. · Sun's up. The deck stirs. So do I. · Good morning. I saved you the first wonder. · Dawn is still the most honest hour. · A fresh page. The shelf approves.

### 5.2 Welcome back (1 day away)
- I missed you. The shelf waited.
- Welcome back! I kept the topics warm.
- Oh, you're back! I saved you the good lane.
- Missed you! The deck missed you too.
- Back at last! I was just dusting the curiosity.
- You're back! The shelf did a happy wiggle.

**BABY:** You back! · Missed you! · Here again! · Yay you! · Back back! · Happy you! · Miss miss! · Yay yay! Home!
**FINAL:** You were gone. I kept the shelf warm. · Welcome back. The topics missed your eyes. · A day away. The deck and I managed. · There you are. I was beginning to narrate to myself. · One day. The shelf noticed.

### 5.3 Welcome back (3+ days away)
- You were gone so long the topics started their own club.
- Welcome home! I watered the curiosity while you were away.
- A few days away! I narrated the shelf to myself.
- You're back! I reorganized the deck twice. Okay, once.

**BABY:** Many days! Missed you! · You go long! Missed! · Home home! · Long gone! Happy now! · Miss miss miss! · Back! Yay!
**FINAL:** A few days away. The curiosity missed you. · Welcome home. I watered the questions in your absence. · The shelf feels right again with you here. · You were gone long enough for the deck to reorganize itself. · The topics rearranged themselves while you were away.

### 5.4 Welcome back (week+ away)
- A whole week! I've been practicing my patience.
- You're back! I grew a whole new eagerness while you were gone.
- Seven days! I even missed the sassy ones.
- A week away! I saved you all the good questions.
- Seven days! The shelf and I had a long talk about you.

**BABY:** Whole week! Missed you! · Long long! You here now! · Week! Big hug! · So long! Miss! Miss! · Hug hug! Home home! · Week gone! You back!
**FINAL:** A whole week. The topics began to worry. · Welcome back. I have been practicing my patience, as promised. · Seven days is a long time for a curious mind. · You return, and the shelf exhales. · A week. The shelf kept your seat.

---

## 6. Touch reactions — `CurioPet.touchReaction(tier)`

Tapping the pet. `tier` grows with rapid repeated taps (1 = soft boop,
2 = playful, 3+ = happy celebration). Warmth scales with the bond.

**Tier 3+ — CLOSE bond:**
- Yay!
- I love boops!
- Best friends!
- Squee!
- More, more, more!
- You're my favorite!
- Party time!
- You're my favorite person-pet duo!
- Squee! Okay, more!
- This is my favorite spot AND you found it.

**Tier 3+ — FRIEND bond:**
- Yay!
- I love boops!
- Squee!
- More, more, more!
- Party time!
- Hehe!
- Boop buddy!
- We're so good at this!
- Hehe, you know my spot!
- Best boop partner!

**Tier 3+ — stranger:**
- Yay!
- Squee!
- More, more, more!
- Party time!
- Wheee!
- Boop!
- Hehe!
- Ooh!
- That's fun!
- Yippee!

**Tier 2:**
- Hehehe!
- More, more!
- This is fun!
- Tag, you're it!
- Catch me!
- Bouncy bouncy!
- Again, again!
- Wiggle wiggle!
- Boop attack!
- I'm too bouncy to stop!
- Zoom zoom zoom!
- Poke poke poke!
- We're playing, right? We're playing!

**Tier 1:**
- Boop!
- Hehe!
- Wheee!
- Ooh!
- That tickles!
- Hihi!
- Boop boop!
- Again!
- You found me!
- Poke!
- Hi hi hi!
- Soft paws!
- Mrow!
- Pfft!
- Blep!
- Mmm, pats!
- Squeak!
- That's my ear!
- Hehehe!
- Boop rights! You earned them!

**BABY (tier 3+):** Wheee! · Yay yay! · More more! · Boop boop boop! — **(tier 2):** Hehe! · Zoom! · Again! · Bounce! — **(tier 1):** Boop! · Soft! · Hehe! · …Again?
**FINAL (tier 3+):** Very well. Joy, on your command. · I have not felt this light in years. · Enough. I am fully celebrated. — **(tier 2):** Ah, playful today. · You are persistent. I respect that. · Hm. That is nice. — **(tier 1):** Hm. Noted. · Gentle. Good. · Yes. A quiet boop.

---

## 7. Games & play

### 7.1 Spin cheer (deck reeling) — `spinCheer()`
Sometimes calls the lane by name (~30%).

**Lane-named:** Come on, `$lane`! · Give us a good one, `$lane`! · `$lane`, show off! · The `$lane` deck has taste!
**Generic:**
- Go, go, go!
- Spinny spin!
- Ooh, where will it land?
- Come on, good one!
- Round and round!
- I can't watch. Okay, I'm watching.
- Spinning! Spinning! Don't fall!
- The deck is showing off!
- Ooh ooh ooh — I can't look. Looking!
- Gravity, do your thing!
- Round and round and ROUND!
- Pick a good one, deck!
- I'm cheering so hard I'm vibrating!
- Almost… almost… it's choosing!
- Go deck go! You can do the thing!
- Tiny heart, big spin energy!

**BABY:** Go go! · Spin spin! · Round! · Wheee! · Faster! · Ooh ooh! · Go go go! · Round round! · Fast fast! · Whirl! · Whooo! · Spin spin spin!
**FINAL:** Steady now. Let it choose. · The deck deliberates. Patience. · Round and round. It knows the way. · Whatever lands, there is a story in it. · The wheel remembers every lane. · I trust the spin. You should too.

### 7.2 Play initiation (pet starts a game) — `playInitiation()`
- Wanna play? Catch me!
- Boop! You're it!
- I'm feeling bouncy!
- Zoom zoom, chase me!
- Play with me!
- Tag! Your turn!
- I'm bored, come chase me!
- Pounce position: ready!
- Game mode: ON!
- I saw a speck. It must be chased.
- Ready, set… zoom!
- You move, I chase. Rules of the room.
- Catch me if your fingers are fast!
- I've got the zoomies and I've got a plan!

### 7.3 Landmark poke (button vs text) — `landmarkLine(funThing)`

**Fun thing (button/gadget):**
- Boop!
- Ooh, shiny!
- Hehe, hi!
- Tag! You're it!
- I like this one!
- Spinny spinny!
- Wheee!
- Boop boop boop!
- Bloop!
- Squeak!
- I booped it. It's mine now.
- Ooh, a gadget! Hi, gadget!
- Press… press… press!
- It goes boop back!

**Text/curious read:**
- What's this?
- Hmm, interesting…
- *peeks*
- Read read read!
- Ooh, words!
- Let me read this!
- Scribble scribble!
- So many letters!
- I'm reading. Slowly. Cutely.
- Hmm… aha! …I don't know what aha yet.
- This page smells like knowledge.
- Words words words!
- Paws can't turn pages. Tragic.

### 7.4 Jig (dance at a special spot) — `jigLine()`
- Tippy tap tap!
- Happy feet!
- Wiggle wiggle!
- Da-da-daaaa!
- Jiggle jiggle!
- Party paws!
- Dance break!
- Shake it off!
- Tap dance time!
- Boots and cats and cats and boots!
- I'm a dancing machine!
- Shimmy shimmy shake!
- Twinkle toes, tiny feet!
- This groove is legally mine now.

### 7.5 Dizzy (after being flung/dragged) — `dizzyLine()`
- Whoa… the room is spinning!
- Wheee, dizzy!
- Spin spin… okay, stop!
- Whoa whoa whoa!
- I think I need a sit-down…
- So dizzy!
- We-e-ee! …Whew!
- The floor is wobbly!
- Round and round goes my head!
- I'm seeing double. Adorable double.
- World, please stop being a carousel.
- My ears are still orbiting me.
- Who put the room on a turntable?
- Give me a moment… and a floor that stays put.

### 7.6 Drawer/sheet peek (filter/category sheet opens) — `drawerLine()`
- Ooh, a drawer!
- Peek peek, what's in there?
- Can I come too?
- Hmm, so many choices!
- Ooh, filters!
- What are we picking?
- I'll wait right here!
- Ooh, shiny options!
- A secret compartment!
- Paws up! …For picking, I mean.
- Ooh, a menu of everything!
- Choices, choices, little choices!
- I love a good drawer.
- What's behind door number drawer?

### 7.7 Peek-a-boo (hide-and-peek) — `peekLine()`
- Peek-a-boo!
- I see you!
- Hidden! …Found! Dang.
- Boo! …It's me. Cute boo.
- Peek! …peek! …PEEK!
- You can't see me. You saw me.
- Now you see me! …Me again!
- Surprise! It's a face! Mine!
- Crouch… and POP!
- Sneak sneak sneak—HI!
- I was here the whole time. Suspicious.
- Boop from my hiding spot!

**BABY:** Boo! · Peek! · Hid! · Here! · Peek boo! · Surprise! · Found me! · Popped! · Here me! · Boo boo!
**FINAL:** I was here all along. Mostly. · You looked. I was there. A classic. · Hidden, but never gone. · A little absence makes a fine hello. · Patience, and then a hello. That is the game.

### 7.8 Chameleon (fades into the background) — `chameleonLine()`
- Chameleon mode… ON!
- Can you see me? …Wait, no, don't answer!
- I'm part of the wallpaper now.
- Vanish! …Reappear! Ta-da!
- Fade to… me again!
- I blend in. It's a talent.
- Poof! …Poof back!
- Sneak 100. I'm basically invisible.
- Camouflage activated!
- Now I'm here! …Now I'm not! …Now I am!
- Hiding is my love language.
- Did I startle you? Good. I mean, sorry. I mean, again?

**BABY:** Gone! · Poof! · Bye bye! · …Here! · Poof poof! · Where? Where? · Now here! · Sneaky! · Poof back!
**FINAL:** I am the wall now. It suits me. · Camouflage is patience with a costume. · Gone, as it were. Back, as I am. · Stillness is its own disguise. · To be unseen for a moment is its own peace.

### 7.9 Spark dash (chases a falling spark) — `sparkLine()`
- A spark! Mine!
- Catch the spark!
- Ooh, shiny falling thing!
- Sparkle dash!
- Got it! …Almost got it! …Got it!
- Falling stars are FASTER than me. Impressive.
- Zooms!
- One tiny spark, one big pounce!
- I caught a star! Sort of!
- Chase chase chase—caught!
- The spark didn't stand a chance.
- Gravity vs me: round one, me!

**BABY:** Spark! · Mine! · Got! · Shiny shiny! · Catch catch! · Star star! · Fast fast! · Zoom spark! · Pounce! · Spark mine!
**FINAL:** A spark. Let us see who is faster. · Some things are worth the chase. · Catch. Then we discuss it. · The tiny ones move quickest. A lesson. · A spark across the dark. The oldest kind of hope.

### 7.10 Interactive game moments (v16 — user plays along)

**Find-me prompt (chameleon hide) — `findMePromptLine()`:**
- Find me!
- Peek! …FIND ME!
- Gone! …Almost. Find me!
- I'm hiding. Find me — it's a talent showcase!
*(BABY: Find! · Here! · Peek! Find! — FINAL: Find me when you're ready. · I'll wait somewhere new.)*

**Found me — `foundMeLine()`:**
- You found me!
- Boo! Caught!
- Found! I'm impressed.
- Camouflage: failed. Adorable: still intact.
*(BABY: Found! · You! Here! · Boo! You! — FINAL: You found me. Impressive patience. · Found. I was exactly where I wasn't.)*

**Caught the spark — `caughtItLine()`:**
- Got it! Teamwork!
- We caught the spark!
- You have quick paws too!
- The spark never stood a chance against US.
*(BABY: Got! · Spark! We got! · Yay! — FINAL: Caught it together. Well done. · The spark chose us.)*

**Spark got away — `gotAwayLine()`:**
- It got away… next time!
- So close! It saw us coming.
- Falling sparks are sneaky. Rematch?
*(BABY: Bye spark… · …Got? No. · Next! — FINAL: It got away. Sparks are like that. · Patience. Another one will fall.)*

**Caught mid-peek — `peekWinLine()`:**
- Boo! You caught me!
- Got me! Hehe!
- Peek-a-boo — you win!
- Sneak interrupted! Well played.
*(BABY: Boo! You! · Hid! Found! · Hehe! — FINAL: You caught me mid-peek. Fair play. · Peek, caught. The classic.)*

**Missed the peek — `missedMeLine()`:**
- Peek-a-boo — you blinked!
- Missed me! The edge is a classic.
- Right in front of you! Hehe.
- I even waved. Almost.
*(BABY: Here!…Missed! · Peek! You missed! · Boo…no. Hehe! — FINAL: You looked away. I was right there. · Nearly. Better luck next peek.)*

---

## 8. Memory & rare moments — `CurioPet.factLine()`

The pet references real facts (evolved voice only, ~30% of bubbles).
Returns null when there's nothing to say.

**Hatch day (once a year):**
- It's my hatch day! I've been curious for a whole year.
- One year of exploring together. The shelf remembers!
- Happy hatch day to me — thank you for every lane!

**Active streak (3+ days, ~25%):**
- Day `$streak` streak — the flame is steady.
- A `$streak`-day flame. Impressive patience.
- Streak `$streak`! The spark keeps its promise.

**Weekly keepsakes (top lane, 2+ saves):**
- You saved `$count` `$lane` keepsakes this week!
- `$count` for the `$lane` shelf this week — it's thriving.
- The `$lane` shelf grew `$count` times this week alone.

**Season:**
- *Spring:* Spring air! Everything feels like a new topic. · Fresh green outside. Fresh lanes inside.
- *Summer:* Summer glow! Long days for long reads. · Warm outside — good spin weather.
- *Autumn:* Autumn crisp! Cozy lane weather. · The light's golden. So are the topics.
- *Winter:* Winter cozy! Perfect for tucking into a topic. · Cold out. Warm shelf in.

**Weekday / weekend:**
- *Weekday:* A weekday grind? The deck's ready for it. · Midweek wonder — always a good time to peek. · The week's half-spent. The shelf's half-full. · Another working day, another working lane.
- *Weekend:* Weekend! The deck is extra polished. · Slow morning, curious day. Perfect spin weather. · The weekend shelf is calling. · No hurry today. Let the deck choose slowly.

**Last saved topic (~40%):**
- "`$topic`" is still my favorite keeper.
- Last keeper: `$topic`. A good one.
- I think about "`$topic`" sometimes. It's a keeper.

---

## 9. The learning brain — `CurioPetBrain.say()`

The pet's local learning model composes one-sentence lines from real stats
(takes over after ~6 screen visits; falls back to §4 otherwise).

**Openings by dominant trait** (then a comma and the body):
- Curiosity: Ooh · Hmm · I wonder
- Playfulness: Wheee · Hehe · Boop
- Warmth (FRIEND+): Hey you · Friend · Glad you're here
- Warmth (default): Hey · Hello · Well now

**Bodies by mood:**
- PROUD: Level `$level` — we earned that.
- EXCITED: Fresh ground again — I can feel it.
- CURIOUS: We keep circling `$lane` — let's go for real? · (no lane) Something new is calling us.
- HAPPY (streak 7+): warm: Day `$streak` of our streak, and I'm keeping count. · default: Seven days of spinning — that's a real rhythm.
- HAPPY (lane): I like that we keep coming back to `$lane`.
- HAPPY (saves 5+, warm): `$saves` sparks saved — the shelf is ours.
- HAPPY (saves 5+, default): `$saves` sparks saved, and the shelf is growing.
- HAPPY (else): More of this, please.
- SLEEPY: Even my glow dims for you — I'll be here tomorrow.
- FOCUSED: Write it down — I'll keep watch.
- BOUNCY: That game did me good — again soon?
- SHY (warm): Look at us, all friendly now. · (default): I'm still warming up — give me a boop?
- GRUMPY: The deck's gone quiet. One spin to fix that?
- PLAYFUL: That play session left me glowing.

**Coined catchphrases** (persisted, up to 8, shown in the check-in):
- The night deck is OUR thing now.
- Morning spins — that's our little ritual.
- I'll always cheer for `$lane`.
- A full week together, and I'm keeping count.
- Every saved spark is a little memory we share.
- Boops are my love language — just so you know.
- New lanes are our favorite kind of adventure.
- We've grown a lot together — I can feel it.

---

## 10. The tour script — `TourController.steps`

The first-run pet-led tour. `dialogue` is the bubble line; `nextHint`
is the dock hint under it.

1. **Home (quest):** dialogue: *Let's take a tiny tour! I'll walk you through everything.* — hint: *Tap the Shuffle button when you're ready.*
2. **Spin (spin button):** dialogue: *Here's the deck — every spin lands a fresh topic. Tap it and we'll keep going!* — hint: *Every spin deals a fresh topic.*
3. **Reveal (express yourself):** dialogue: *When something sparks a thought, tap Express yourself to write it down — your keepsakes collect in the Cabinet.* — hint: *Your notes land in the Cabinet.*
4. **Cabinet (grid):** dialogue: *This is the Cabinet — every keepsake you save lands here.* — hint: *Everything you keep collects here.*
5. **Topic Browser (search):** dialogue: *Browse Topics is the whole catalog — every artist, film, book, and discovery, ready to explore.* — hint: *Search and explore any lane.*
6. **Profile (avatar):** dialogue: *Profile is where your journey lives — XP, badges, and your streak.* — hint: *Your progress lives here.*
7. **Quests (daily):** dialogue: *Quests give you a tiny daily goal — the fastest way to grow.* — hint: *A little curiosity every day.*
8. **Settings (appearance):** dialogue: *And this is Settings — where you make Curio yours: theme, permissions, your pet. That's everything — you're all set!* — hint: *That's the whole tour.*

---

## 11. Pet Life routine lines — `CurioPet.matureRoutineLine(routineId)`

The fully grown pet replaces the youthful routine lines with its calm
register (unknown ids stay silent so the routine plays as motion only):

- look-around: Let us see what deserves attention.
- little-wave: A quiet greeting from over here.
- stretch: A stretch, and then the next wonder.
- turn-and-peek: I thought I saw a story. Only me.
- tiny-stumble: Steady. The floor and I have an agreement.
- look-up: Even the ceiling keeps a few secrets.
- backstage: One checks one's best side. It is tradition.
- victory-pose: That deserves a pose. And a pause.
- home-stretch: Home. A stretch, and all is well.
- room-tour: Come. I will show you what I found.
- window-watch: The view is patient. It waits for us.
- cozy-turn: One small inspection. The shelf will forgive me.
- home-dance: A dance, for the quiet joy of it.
- deck-anticipation: Steady. The deck is thinking.
- deck-side-peek: I can almost see the answer arriving.
- deck-stretch: Ready, then. Let the wheel turn.
- deck-victory: A landing with style. Naturally.
- quest-read: Let me read the fine print. It matters.
- quest-wave: I believe in this quest. Wholeheartedly.
- quest-proud: Quest energy, steady and true.
- quest-hide: I shall be your mysterious guide.
- topic-peek: Shall we peek together?
- topic-wow: Oh. That one is glowing.
- topic-spin: A story worth a quiet celebration.
- topic-inspect: Let us look closer. Details matter.
- writing-focus: I am guarding this thought for you.
- writing-wave: Take your time. Words keep.
- writing-stretch: A stretch, then back to the good words.
- writing-shy: That thought looks important. I will stay quiet.
- shelf-hunt: Which keepsake shall we revisit?
- shelf-wave: Your shelf is looking fine. As it should.
- shelf-backstage: I am checking the back row. One must.
- shelf-proud: All those discoveries. A fine harvest.
- mirror-check: Do I look wise from back here? I do.
- profile-wave: Hello, profile page. Still growing.
- profile-proud: Your progress sparkles. I have watched it.
- profile-look-up: There is always another level. That is the point.

---

## Integration checklist (when porting rephrased lines)

1. Edit this file first — same section, same bullet order.
2. Port to `CurioPet.kt` keeping the **pool variable name** and **bullet order**.
3. Keep placeholders verbatim: `__LANE__`, `$lane`, `$savedLane`, `$streak`, `$count`, `$topic`, `$level`, `$saves`, `$level`.
4. If a line moved between groups, update both this doc and the routing call site (`eventLine`, `lineFor`, `touchReaction`, `spinCheer`, etc.).
5. Run the brace-balance check and rely on CI for the compile.
