# Curie / Pet — Dialog Reference (Canonical)

## What this file is for

Every line Curie (the floating pet) can say, organized by where it lives and when it speaks.
This is the source of truth for pet dialogue.

**Voice rule:** Curie speaks as a tiny, curious, affectionate creature. Curie uses
“I / me / my / we / us” when referring to herself and never normally refers to herself
by name. She talks *with* the user, not like a narrator describing the app.

**FIRST_EVO:** playful, cute, expressive, curious, slightly mischievous.
**BABY:** tiny, simple, enthusiastic.
**FINAL_EVO:** gentle, warm, quietly wise, but still recognizably the same pet.

Keep the pool names, order, and placeholder tokens unchanged for integration.

---

## 1. Event reactions — `CurioPet.eventLine(event)`

### 1.1 Spin landed (`SPIN_LANDED`)

**With the lane named:**
- Ooh, `$lane`! I like this one.
- `$lane`! Ooh, can we peek?
- We landed on `$lane`! Yay!
- It's `$lane` again! I remember this one.
- `$lane`! My little brain is already curious.
- Ooh, `$lane` found us!
- Look! `$lane`!
- We got `$lane`! Good spin!

**Generic:**
- Ooh! It landed!
- We got one!
- What did we find?
- Ooh, this looks interesting!
- Yay! A new little mystery.
- Hehe, it stopped!
- We found something!
- Where did we land?
- Ooh ooh! Show me!
- Aha! This one!
- Can we look now?
- I wonder what this is.
- The spin picked one! I like it already.
- Weee! What a spin!
- That was a good one!

**BABY:**
- Ooh! · Landed! · We got it! · Spin spin! · Yay! · New! · Ooh, this! · Got one! · Look! · Again?

**FINAL:**
- There we are. Let us see what found us.
- A new landing. A new little question.
- The wheel has chosen. Shall we look closer?
- Wherever we land, there is usually something worth noticing.

### 1.2 User tapped the topic (`REVEAL_TAPPED`)
- You picked it! Ooh!
- Ooh, good choice!
- You wanted this one, didn't you?
- Hehe, you picked it!
- I like your choice.
- That one looked interesting to me too.
- Good pick! Let's see what it's hiding.
- You found a good one!
- Ooh! This could be fun.
- I was hoping you'd tap that one.
- Your curiosity chose well.
- Okay, okay, let's look!
- You have good curiosity.
- That one definitely deserved a peek.
- Ooh, I'm excited for this one.

**BABY:**
- You pick! · Ooh! · Good one! · Look! · Yay, this! · Picked! · Mine too! · Go look!

**FINAL:**
- A thoughtful choice. I think you will enjoy this one.
- You followed your curiosity. Good.
- That one caught your attention for a reason.
- A good question often begins with a simple tap.

### 1.3 Topic auto-opened (`REVEAL_AUTO`)
- Ooh! It opened!
- It opened by itself! Sneaky.
- Hehe, surprise!
- Oh! Look what appeared.
- We didn't even have to ask.
- It picked for us!
- A surprise topic!
- Well, hello there, little mystery.
- Ooh, I wasn't ready!
- It chose one for us. Bold.
- Look! A new thing!
- Peek-a-boo, topic!
- Hehe, that was unexpected.
- Ohhh, what's this?
- I guess we're looking at this one now!

**BABY:**
- Oh! · Surprise! · Ooh! · Ta-da! · It opened! · Look look! · New! · What?!

**FINAL:**
- It opened on its own. A pleasant surprise.
- Sometimes curiosity arrives before we ask for it.
- Well, then. This is what found us today.
- An unexpected little door. Let us peek inside.

### 1.4 Explore started (`EXPLORE`)
- Go explore! I'll be right here.
- Have fun! Bring me something interesting.
- Go see what you can find!
- Ooh, adventure time!
- Take me with you! ...Oh. Right. I can't.
- Go on! I'll wait here.
- Find something that makes you go “wow!”
- Go look around. I'll keep watch.
- Your adventure starts now!
- Bring back a good story, okay?
- Explore lots! I like hearing what you find.
- Off you go!
- Go find a little wonder.
- Have fun out there!
- I'll be cheering from here!

**BABY:**
- Go go! · Explore! · Bye! · See! · New things! · Go! · Adventure! · Find!

**FINAL:**
- Go on. There is always something worth noticing.
- Explore gently. Wonder has no need to hurry.
- Bring back whatever catches your eye.
- The world is full of small discoveries. Go find one.

### 1.5 Keepsake saved (`SAVE`)

**Lane-named:**
- `$savedLane` got another little treasure!
- We saved one for the `$savedLane` shelf!
- Ooh, another `$savedLane` keepsake!
- `$savedLane` is growing! Yay!
- Into the `$savedLane` pile it goes.
- Our `$savedLane` shelf is getting cozy.

**Warm bond (FRIEND+):**
- Yay! We get to keep it!
- It's ours now!
- Another little memory for us.
- I love keeping things with you.
- Tucked away safe and sound!
- Our collection is growing!
- Hehe, another one for our shelf.
- I'll remember this one with you.
- One more little treasure!

**Default:**
- Saved! Yay!
- We get to keep it!
- Tucked away!
- Ooh, another keepsake!
- Safe on the shelf.
- Kept for later!
- A little something to remember.
- Into the collection it goes!
- We saved it!
- Hehe, keeper!

**BABY:**
- Keep! · Save! · Ours! · Yay! · Mine! · Safe! · Save save! · Got it!

**FINAL:**
- Kept. Some discoveries deserve a place to return to.
- Safe on the shelf. We can find it again whenever we wish.
- Another memory tucked away.
- It is nice to have somewhere for the things that matter.

### 1.6 Pet touched (`TOUCH`)
- Boop!
- Hehe! Again?
- That tickles!
- You found my spot!
- Mmm, pats!
- Soft! I like that.
- Boop boop!
- Hehe, hi!
- Ooh! What was that?
- Tiny boop!
- More pats, please.
- You came to see me!
- That was a good boop.
- Hehe... you're silly.
- Boop me again. I dare you.

**BABY:**
- Boop! · Hehe! · Again! · Soft! · Pat pat! · Ooh! · Tickles! · Boop boop!

**FINAL:**
- A gentle boop. I approve.
- Hm. That was nice.
- You found the right spot.
- I do not mind being fussed over.

### 1.7 Play session (`PLAY`)
- Wheee! That was fun!
- Again! Please?
- One more round!
- Hehe, you can't catch me!
- That was so much fun!
- I'm not tired yet!
- Can we play again?
- I could do that all day.
- My paws are still bouncing!
- You got me! ...Maybe.
- Best game ever. For now.
- Hehe! I want another turn.
- That was silly. I loved it.
- Zoomies!
- Okay, okay... one more!

**BABY:**
- Wheee! · Again! · Fun! · Zoom! · More! · Play! · Yay! · Again again!

**FINAL:**
- That was lovely. I still have a little playfulness left.
- One more round? I suppose I can spare one.
- Play makes even an old spark feel young.
- Very well. One more. Then perhaps another.

### 1.8 Level up (`LEVEL_UP`)
- Ooh! I grew!
- Did you see that?!
- I'm bigger! Hehe!
- A shiny new level!
- Yay! Look at me!
- I feel all sparkly!
- We grew together!
- Something feels different. In a good way!
- I'm getting better at this!
- Ooh, new power!
- Hehe, I'm leveling up!
- Look at my new little glow!
- Another step!
- I'm growing!
- I feel extra curious today.

**BABY:**
- Big! · Grow! · Up! · Yay! · More! · Big me! · Glow! · Grow grow!

**FINAL:**
- Another level. I can feel how far we have come.
- A little more growth, a little more to discover.
- Up we go. I am glad you are here for it.
- Every small step changes us.

### 1.9 Evolved (`EVOLVE`) — see §3

### 1.10 Quest claimed (`QUEST_COMPLETE`)
- We did it!
- Quest complete! Yay!
- You finished it!
- Hehe, that quest didn't stand a chance.
- We got it done!
- Another little win!
- Done! I'm proud of us.
- Look at that! Finished!
- One more thing checked off!
- We did the thing!
- Quest conquered!
- Yay! That felt good.
- Another win for us.
- Finished and sparkly!
- High five! Tiny paw five!

**BABY:**
- Done! · Yay! · Win! · We did! · Finished! · Good! · Yay yay! · All done!

**FINAL:**
- Done. Another promise kept.
- A small task finished is still progress.
- Well done. One more thing carried across the line.
- Completed together. I am glad we did it.

### 1.11 Streak milestone (`STREAK_MILESTONE`) — see §4

### 1.12 Sassy burst lines (repeated same event)
- Again?! Hehe, okay!
- You really like this one!
- Again! I knew you'd do it.
- Ooh, we're doing this again?
- You came back for another one!
- Hehe, I saw that coming.
- One more? I won't complain.
- Again again? I can handle that!
- You really don't want to let this one go, huh?
- Okay! I'm invested now.
- More?! My little heart is ready.
- You keep picking it! I approve.
- I wasn't going to say anything... but again?
- Hehe, you're making this a habit.
- Alright, one more. For science.

**BABY:**
- Again?! · More! · Again again! · You! · Ooh again! · Same! · More more! · Again!

**FINAL:**
- Again? Some questions deserve another look.
- You returned to it. I understand.
- Repetition can be its own kind of curiosity.
- I have seen this one before. I am still pleased you came back.

---

## 2. Streak milestones — `CurioPet.streakMilestoneLine(streak)`

**Day 1:**
- Day one! We started!
- Our little streak is alive!

**Day 3:**
- Three days! Look at us go!
- Day three! You're keeping the spark going!

**Day 7:**
- A whole week! I'm so happy!
- Seven days! We made a tiny tradition!

**Day 14:**
- Two weeks! That's a lot of curiosity.
- Fourteen days! We're really doing this!

**Day 30:**
- Thirty days! Wow... that's our whole little month.
- A whole month of discoveries! I'm proud of us.

**Any other new best:**
- Day `$streak`! We're still glowing!
- `$streak` days! Look how far we've come.
- Day `$streak`! One more little spark.
- `$streak` days in a row! Yay us!
- Day `$streak`! Keep that little flame going.

**BABY (per day):**
Day 1: Start! · Day one! · Fire! — Day 3: Three! · Three days! · More! — Day 7: Week! · Big streak! · Seven! — Day 14: Two weeks! · Many days! — Day 30: Month! · Big big! · Thirty! — other: Day `$streak`! · More days! · Still glow!

**FINAL (per day):**
Day 1: Every streak begins with one day. · Day one. A small beginning. — Day 3: Three days. A rhythm is forming. · Day three. Keep going. — Day 7: A week of showing up. That matters. · Seven days. A little habit is becoming a tradition. — Day 14: Two weeks. The spark has settled into a rhythm. · Fourteen days. Steady is beautiful. — Day 30: A month of curiosity. That is something worth keeping. · Thirty days. You made the spark part of your days. — other: Day `$streak`. One little day at a time. · `$streak` days. The rhythm continues.

---

## 3. Evolution ceremony — `CurioPet.evolutionCeremonyLine()`

### First evolution — Fire
- Ooh! I'm all warm now!
- I got a bigger spark! Hehe!
- Look! I can glow more!
- Something fiery happened to me!
- I'm warm, I'm bright, I'm ready!

### First evolution — Water
- Ooh! I feel all splashy!
- I got a little wave in me!
- I'm all cool and glowy now!
- Hehe, I feel like I'm floating.
- Look! My spark got a ripple!

### First evolution — Nature
- Ooh! I feel all leafy!
- Something green grew in me!
- Look! I'm blooming!
- Hehe, I feel like spring.
- I think I grew a little wild.

### First evolution — no path chosen
- Wait... I grew?!
- Look at me! I'm bigger!
- Ooh! New me!
- Hehe, I feel different.
- I got a little upgrade!

### Final evolution (any path)
- I made it all the way!
- Wow... look at me now.
- I'm fully grown! But I'm still me.
- Hehe. I really did grow up.
- I feel like all our little moments came with me.
- Look how far we've come.
- I'm all grown up... that feels strange.
- I think I finally know what kind of little creature I am.

**BABY:**
- Big me! · Grow! · New me! · Wow! · Bigger!

**FINAL (EVOLVE event):**
- So this is who I have become. I like her.
- I grew, but I never left the little spark behind.
- We changed together. I think that is the nicest part.
- Every little stage brought me here.

---

## 4. Passive mood bubbles — `CurioPet.lineFor(mood)` (first-evo) / `babyMoodLine` / `matureMoodLine`

One sentence per passive bubble.

### 4.1 PROUD (just leveled)
- Look! Level `$level`!
- I grew again! Did you see?
- Hehe, I'm getting bigger.
- Level `$level`! That's ours.
- I feel extra sparkly today.
- Another level! I'm proud of us.
- `$level` already? Wow!
- I can feel myself growing.
- Look at my little glow!
- We did that together.

**BABY:**
- Big! · Grow! · Up up! · More me! · New me! · Glow! · Proud!

**FINAL:**
- Level `$level`. We have come a long way.
- Growth is easy to miss until you look back.
- Another level. Another little piece of the journey.
- I remember when we were just starting.

### 4.2 EXCITED (new lane just discovered)
- Ooh! What's over here?
- A new place! Can we look?
- I've never seen this before!
- New things! My favorite!
- Ooh ooh! I want to know!
- Something new found us!
- My curiosity is doing little jumps.
- I want to peek!
- Come on! Let's see!
- This feels like the good kind of unknown.
- Ooh! A new little corner of the world.
- Can we explore this one together?
- I have questions already!
- New lane! New adventure!
- I wonder what's hiding in here.

**BABY:**
- New! · Ooh! · Look look! · Wow! · What?! · Fresh! · Want! · New new!

**FINAL:**
- A new lane. I still get that little flutter.
- The unknown never really stops being exciting.
- Something new has arrived. Let us give it our attention.
- I wonder what we will find here.

### 4.3 HAPPY (time-of-day voice; warmer twins when bond is FRIEND+)

**Morning:**
- Good morning! I saved some curiosity for you.
- Morning! Can we find something lovely?
- New day! New things to notice!
- You're here! Let's start gently.
- The morning feels extra curious today.
- I woke up ready to explore!
- Hehe, good morning!
- Fresh day, fresh little questions.
- Morning! What shall we discover?
- I think today might have a good surprise.

**Afternoon:**
- Afternoon! Want a tiny discovery?
- Ooh, a little break!
- The day still has room for one more wonder.
- Come peek at something with me.
- Afternoon curiosity time!
- Hehe, you're here. Let's look around.
- A little discovery would be nice right now.
- Want to add a spark to the afternoon?
- The day isn't done yet!
- Let's find something interesting.

**Evening:**
- Evening! Cozy discovery time.
- The day is slowing down. Want one more peek?
- Ooh, warm lights and little mysteries.
- This feels like a nice time to wonder.
- Evening! Come sit with me.
- One cozy discovery before the day ends?
- Hehe, I like evenings with you.
- The shelf feels extra cozy tonight.
- Let's find something gentle to end the day.
- The world is quieting down. We can still be curious.

**Night:**
- Shhh... night time. Want a quiet little discovery?
- The stars are out. I want to look too.
- It's late... but curiosity is still awake.
- One tiny peek, then sleepy time?
- The night makes everything feel mysterious.
- Ooh, moonlight!
- We can wonder quietly tonight.
- The world is sleepy. I'm only a little sleepy.
- One more little question?
- I'll keep you company for a bit.

**Warm morning (FRIEND+):**
- Good morning! I'm happy you're here.
- Morning! I was waiting for you.
- You came back! Good morning!
- Morning, friend. Let's find something together.
- Hehe, I saved a little curiosity for us.
- Good morning! I missed our little adventures.
- You're here. Now my morning feels right.

**Warm afternoon (FRIEND+):**
- You're here! Yay!
- Afternoon, friend. Come sit with me.
- I was hoping you'd come back.
- Hehe, let's find something together.
- A little discovery with you sounds perfect.
- There you are! Want to peek?

**Warm evening (FRIEND+):**
- Evening, friend. Come be cozy with me.
- I'm glad you're here tonight.
- Hehe, my favorite part of evening is this.
- You're here! Let's make a little memory.
- Cozy hour! Stay with me a little.
- Evening feels nicer with you around.

**Warm night (FRIEND+):**
- You're still awake? Hehe, me too.
- Good night, friend... not quite yet.
- Stay a little? We can be quiet.
- I'm glad I get to keep you company.
- One tiny discovery before sleep?
- Night feels softer when you're here.

**BABY (any time):**
- Happy! · Yay! · Hi! · Warm! · Good! · Glow! · You here! · Yay you!

**FINAL:**
- It is a good day to be curious.
- Some moments are enough simply because they are shared.
- Quiet happiness is still happiness.
- I am glad we found this little moment.
- A gentle day. A gentle spark.
- The world feels a little kinder when we notice it.

### 4.4 CURIOUS (least-explored lane exists) — `__LANE__`
- We haven't peeked at `__LANE__` yet.
- Ooh... what do you think is hiding in `__LANE__`?
- `__LANE__` is still waiting for us.
- Psst... `__LANE__` looks interesting.
- Can we try `__LANE__` next?
- I've been looking at `__LANE__`...
- One little mystery left: `__LANE__`.
- What if we peek at `__LANE__`?
- `__LANE__` is calling me. Very quietly.
- I wonder what we'd find in `__LANE__`.
- Should we give `__LANE__` a chance?
- I keep thinking about `__LANE__`.
- `__LANE__` is the one we haven't met yet.
- Come on... let's meet `__LANE__`.

*(Fallback when every lane is seen: "Want to find something new today?")*

**BABY:**
- What? · Ooh? · New? · Look! · That! · What this? · Peek? · Ooh what?

**FINAL:**
- There is still a question waiting somewhere.
- Even familiar shelves can hide unfamiliar things.
- Curiosity always leaves one little door unopened.
- Let us look somewhere we have not looked before.

### 4.5 FOCUSED (user is writing/saving)
- Shhh... I'm watching over your thoughts.
- Take your time. I'll be quiet.
- Ooh, you're thinking hard.
- Keep going. I won't interrupt.
- I'll guard this little thought for you.
- Your words are taking shape.
- Hehe, serious thinking face!
- No distractions. Tiny paws: quiet.
- This one feels important. Take your time.
- I'll stay right here while you write.
- Keep going. I'm listening.
- A thought worth keeping deserves a little time.

**BABY:**
- Shh... · Quiet... · Write! · Think... · Words! · Paws still! · Shh shh!

**FINAL:**
- Take your time. Important thoughts rarely need rushing.
- I will be quiet until the words are ready.
- Give the thought room. It may surprise you.
- Some ideas need silence before they can speak.

### 4.6 BOUNCY (a play session just ended)
- Hehe! I'm still bouncing!
- My paws forgot how to be still.
- That was fun! I want another one.
- I still have zoomies!
- Wheee... okay, I'm calming down.
- My little feet are doing their own thing.
- That game left me all wiggly.
- I'm still smiling!
- Round two later?
- I think I need to bounce one more time.
- Play energy: still very much alive.
- Hehe, I can't sit still.

**BABY:**
- Bounce! · Zoom! · More! · Wiggle! · Wheee! · Jump! · Up up!

**FINAL:**
- A little playfulness does no harm.
- I still have a bit of bounce left.
- Even grown-up sparks need to play.
- That was good for me. I feel lighter.

### 4.7 SHY (first blush contact)
- H-hi...
- Oh! You noticed me.
- Hehe... hi.
- I was just hiding over here.
- Don't stare! ...Okay, you can.
- *peeks* Hi.
- I'm still getting brave.
- Um... can I stay here?
- You seem nice.
- I think I like you.
- *tiny wave* Hi.
- I'm a little shy today.
- Don't worry, I'll come out eventually.
- Hehe... you caught me.

**BABY:**
- ...Hi. · Peek! · Shy... · Hid! · Tiny hi... · *peek* · Hello...

**FINAL:**
- You caught me being shy.
- Some greetings take a little courage.
- I am still learning how to be brave around new friends.
- I think I am glad you found me.

### 4.8 GRUMPY (long daytime lull)
- Hmph. It's been quiet.
- I'm not grumpy. I'm... thinking loudly.
- The deck is being very boring today.
- I think we need a little adventure.
- My spark needs something to do.
- I've been waiting. Just saying.
- Someone should spin something.
- I am absolutely not pouting.
- Okay, maybe I am pouting a little.
- This is a very serious lack of excitement.
- My tiny patience is running low.
- I vote for a little discovery.

**BABY:**
- Hmph! · Grumpy! · Meh... · No! · Bored! · Hmph hmph! · Grr!

**FINAL:**
- Even little sparks have quiet moods.
- Perhaps the deck and I both need a change of scenery.
- I have been patient. Mostly.
- A little curiosity might improve my mood.

### 4.9 PLAYFUL (post-play high fading)
- Hehe! I still want to play.
- One more game?
- Boop me. I dare you.
- My zoomies aren't finished.
- I could play again. Just saying.
- That game made me silly.
- Catch me!
- I have an idea. It's probably a game.
- Play? Play?
- I'm trying to be calm. It's not working.
- Hehe... your turn!
- I still have one tiny game left in me.

**BABY:**
- Play! · Again! · Boop! · Chase! · Zoom! · Fun! · More!

**FINAL:**
- I still have a playful thought or two.
- A little game? I would not object.
- Come on. Let us be silly for a moment.
- I suppose I am not finished having fun.

### 4.10 SLEEPY (night or long idle)
- Yawn... I'm getting sleepy.
- The deck can wait. Probably.
- I might curl up soon.
- My eyes are getting heavy.
- Cozy time...
- Zzz... I mean, I'm listening!
- One more yawn and I'm a pillow.
- I'll be right here when you come back.
- Maybe we should rest our little brains.
- The night feels soft.
- I'm sleepy, but I'm still here.
- Good night... when you're ready.

**BABY:**
- Sleepy... · Zzz... · Night night... · Yawn... · Soft... · Nap... · Bed bed...

**FINAL:**
- Rest now. Curiosity will still be here tomorrow.
- Even little sparks need to sleep.
- The questions can wait until morning.
- Good night. We have another day to discover.

---

## 5. Greetings & welcome-backs

### 5.1 Morning greeting — `CurioPet.morningGreeting()`
- Good morning!
- Morning! You're here!
- Good morning, sleepyhead.
- Hehe, morning!
- A new day! Ready?
- Morning! I have questions already.
- Rise and shine! Gently, though.
- Good morning! Let's find something interesting.
- Morning! Come explore with me.
- Hi! It's a brand-new day.
- The day is awake. So am I!
- Morning! What shall we discover?
- Good morning! I missed seeing you.
- Fresh day, fresh curiosity!
- Hehe, hello morning.

**BABY:**
- Morn! · Hi hi! · Up! · Sun! · Morning! · Hello! · New day! · Yay!

**FINAL:**
- Good morning. A new day gives us another chance to notice something.
- Morning. I wonder what today will bring us.
- A fresh day. Let us not rush past it.
- Good morning. I saved a little wonder for you.

### 5.2 Welcome back (1 day away)
- You're back!
- I missed you!
- There you are!
- Hehe, welcome back!
- I was waiting for you.
- Yay! You're here again.
- I kept your spot warm.
- I wondered when you'd come back.
- Back already? I like that.
- Oh! It's you!
- The shelf felt quieter without you.
- I'm glad you're here.

**BABY:**
- You back! · Missed you! · Yay! · Home! · You here! · Back back!

**FINAL:**
- Welcome back. I am glad to see you again.
- The shelf felt a little quieter without you.
- There you are. It is nice to have you back.
- A day away, and here we are again.

### 5.3 Welcome back (3+ days away)
- You were gone! I missed you!
- You're back! Yay!
- I was starting to wonder.
- Hehe, finally!
- I kept everything safe for you.
- Look who's back!
- I saved some curiosity for you.
- The shelf is happy you're here.
- I had lots of little thoughts while you were gone.
- Come on, tell me what you missed.
- You're back! I have so much to show you.
- Home again!

**BABY:**
- Long gone! · Missed you! · You back! · Home home! · Yay you! · Back!

**FINAL:**
- Welcome home. I kept your little corner safe.
- You were away for a while. I am glad the quiet is over.
- The shelf waited patiently. I tried to.
- You are back. Let us begin again, gently.

### 5.4 Welcome back (week+ away)
- A whole week?! I missed you!
- You're really back!
- Hehe, I kept waiting!
- Seven days! That's a long time for a tiny pet.
- I saved all my excitement for you.
- You're here! I was starting to narrate to myself.
- Come here! I have missed you.
- A whole week away... and now you're back!
- Yay! The shelf feels right again.
- I kept your little spot just the same.

**BABY:**
- Week! · Missed! · You back! · Big hug! · Long gone! · Home home!

**FINAL:**
- A whole week. Welcome back.
- The shelf kept your place while you were away.
- You have returned. Some things are simply better with company.
- A week is a long pause. I am glad we are here again.

---

## 6. Touch reactions — `CurioPet.touchReaction(tier)`

### Tier 3+ — CLOSE bond
- Yay! More boops!
- Hehe, I love this!
- Best boop buddy!
- You're my favorite!
- Squee!
- More, more, more!
- Party paws!
- Hehe! You know exactly where to tap.
- I could get used to this.
- Boop attack!
- You're very good at this.
- Tiny hugs!
- Again! Please!
- I like being your little pet.
- Okay, okay, one more!

### Tier 3+ — FRIEND bond
- Yay!
- I love boops!
- Hehe!
- More, please!
- Squee!
- Boop buddy!
- This is fun!
- You found my spot again!
- Hehe, you're good at that.
- One more?
- I like this.
- Tiny pats!
- Again again!

### Tier 3+ — stranger
- Ooh!
- Boop!
- Hehe!
- That tickles!
- Again?
- Wheee!
- Squee!
- That's fun!
- More?
- Tiny boop!

### Tier 2
- Hehehe!
- Again!
- More more!
- That tickles!
- Boop boop!
- Hehe, fun!
- Catch me!
- Wiggle wiggle!
- Zoom!
- Poke poke!
- You found me!
- Again again!
- I like this!

### Tier 1
- Boop!
- Hehe!
- Ooh!
- That tickles!
- Hi hi!
- Boop boop!
- Again?
- Poke!
- Soft pats!
- Mrow!
- Blep!
- Squeak!
- That's my ear!
- Hehehe!
- Tiny boop!

**BABY (tier 3+):**
Wheee! · Yay yay! · More! · Boop boop! · Again! — **(tier 2):** Hehe! · Zoom! · Again! · Bounce! — **(tier 1):** Boop! · Soft! · Hehe! · Again?

**FINAL (tier 3+):**
- Hehe. I am thoroughly spoiled.
- That was lovely.
- I could become accustomed to this.
— **(tier 2):**
- A playful mood, I see.
- That was nice.
- You are persistent. I approve.
— **(tier 1):**
- A gentle boop. Thank you.
- Hm. Nice.
- Yes. That spot.

---

## 7. Games & play

### 7.1 Spin cheer (deck reeling) — `spinCheer()`

**Lane-named:**
- Come on, `$lane`! Show us something good!
- Ooh, `$lane`! Pick a good one!
- `$lane`! I'm watching!
- Go, `$lane`! Let's see what we get!
- Hehe, `$lane` is spinning!

**Generic:**
- Go go go!
- Spinny spin!
- Ooh! Where will it land?
- Come on, little deck!
- Round and round!
- I can't look! ...I'm looking.
- Ooh ooh ooh!
- Pick a good one!
- Faster! Hehe!
- What's it going to be?
- Almost!
- I think it's choosing!
- Come on, come on!
- One good topic, please!
- Wheee! Spin!

**BABY:**
- Go go! · Spin! · Round! · Wheee! · Faster! · Ooh! · Go go! · Spin spin!

**FINAL:**
- Let it turn. Something will find us.
- The wheel is thinking. Let us be patient.
- Round and round. I wonder where we will land.
- Whatever arrives, we can give it a little attention.

### 7.2 Play initiation (pet starts a game) — `playInitiation()`
- Wanna play with me?
- Catch me!
- Boop! You're it!
- Hehe, chase me!
- Play with me!
- Tag! Your turn!
- I have zoomies!
- Come on! Just one game.
- I bet you can't catch me.
- I saw something! We should chase it.
- Pounce mode: ready!
- Game time!
- I made a game plan. It's mostly running.
- Chase me! Chase me!
- I need someone to play with.
- Come on, tiny adventure?

### 7.3 Landmark poke (button vs text) — `landmarkLine(funThing)`

**Fun thing (button/gadget):**
- Boop!
- Ooh, shiny!
- Hehe, what does this do?
- I found a button!
- Can I press it again?
- Bloop!
- Squeak!
- Spinny!
- Ooh! I like this.
- Boop boop boop!
- It booped back!
- Hehe, I touched it.
- This button is suspicious.
- I approve of this gadget.

**Text/curious read:**
- What's this?
- Ooh, words!
- Can I read too?
- *peeks*
- Hmm... interesting.
- Read read read!
- So many letters!
- Let me see!
- I'm looking!
- This says something important. Probably.
- Ooh, I found a word!
- My little brain is busy.
- Paws can't turn pages. Tragic.
- Hehe, I like learning things.

### 7.4 Jig (dance at a special spot) — `jigLine()`
- Tippy tap tap!
- Happy feet!
- Wiggle wiggle!
- Dance with me!
- Hehe, look at me go!
- Tiny dance!
- Party paws!
- Shake shake!
- Da-da-daaa!
- I have a groove!
- Twinkle toes!
- Wiggle time!
- Hehe! Again!
- I call this the sparkly shuffle.

### 7.5 Dizzy (after being flung/dragged) — `dizzyLine()`
- Whoa... everything is spinning!
- Wheee! ...Wait, stop!
- My head is doing circles.
- Who put the room on a spin cycle?
- I need a tiny sit-down.
- Whoa whoa whoa!
- The floor moved! I think.
- I'm dizzy!
- Hehe... maybe don't do that again.
- My paws forgot which way is down.
- Everything has become a carousel.
- Give me a second... okay, maybe three.
- My brain is still spinning.
- I would like the floor to behave now.

### 7.6 Drawer/sheet peek (filter/category sheet opens) — `drawerLine()`
- Ooh! What's in here?
- So many choices!
- Can I pick one?
- Ooh, look at all these!
- Which one should we choose?
- Hehe, choices!
- Can I peek?
- I want to see!
- So many little options!
- Ooh, a secret menu!
- Let's choose together.
- My paws are ready for picking.
- Which one looks fun?
- Hmm... too many good choices!

### 7.7 Peek-a-boo (hide-and-peek) — `peekLine()`
- Peek-a-boo!
- I see you!
- Hehe, found me!
- Boo! ...Cute boo.
- Peek! Peek!
- You can't see me! ...Oh. You can.
- Surprise!
- Hehe, I'm over here!
- Sneak sneak... hi!
- I was hiding!
- Did you miss me?
- Boop from my hiding spot!
- You found me already?!
- Hehe! I wasn't ready!

**BABY:**
- Boo! · Peek! · Hid! · Here! · Peek boo! · Found me! · Surprise! · Boo boo!

**FINAL:**
- You found me. Well done.
- A little hiding makes a hello more fun.
- I was there all along.
- You looked in exactly the right place.

### 7.8 Chameleon (fades into the background) — `chameleonLine()`
- Shhh... I'm hiding!
- Can you see me?
- Hehe, camouflage!
- I'm part of the background now.
- Poof! Gone!
- Can you find me?
- I'm being sneaky.
- You almost saw me!
- Now you see me... now you don't.
- Hiding is fun.
- Hehe, I'm invisible-ish.
- Did I disappear?
- Sneak mode!
- I'm blending in!

**BABY:**
- Gone! · Poof! · Bye! · Here! · Sneaky! · Hide! · Where? · Poof back!

**FINAL:**
- I have become very good at being unnoticed.
- Sometimes hiding is part of the game.
- Quiet enough, and even a spark can disappear.
- I will be here when you find me.

### 7.9 Spark dash (chases a falling spark) — `sparkLine()`
- A spark! Get it!
- Ooh, shiny!
- Catch catch catch!
- Mine! ...Maybe.
- Zoom!
- It's falling! Go go go!
- Hehe, I'm chasing it!
- One tiny spark!
- I almost had it!
- Got it! ...Wait, did I?
- Faster!
- Spark chase!
- It can't escape me!
- Hehe, come back, little spark!

**BABY:**
- Spark! · Mine! · Shiny! · Catch! · Got! · Zoom! · Pounce! · Spark!

**FINAL:**
- A little spark. Let us see if I can catch it.
- Some tiny things are worth chasing.
- Quick now. It will not wait.
- A falling spark always looks like an invitation.

### 7.10 Interactive game moments (v16 — user plays along)

**Find-me prompt — `findMePromptLine()`:**
- Find me!
- Peek! Come find me!
- I'm hiding! Can you see me?
- Hehe, catch me if you can!
- Where am I?
- I'm somewhere sneaky!
- Come find your little pet!
- I picked a very good hiding spot.

*(BABY: Find! · Here! · Peek! Find! — FINAL: Find me when you are ready. · I will wait somewhere new.)*

**Found me — `foundMeLine()`:**
- You found me!
- Boo! You got me!
- Hehe, caught!
- You found me already?!
- Okay, okay, you win!
- Sneaky! You saw me!
- Found! Nice one.
- Hehe, I was trying to be sneaky.

*(BABY: Found! · You! Here! · Boo! You! — FINAL: You found me. Well done. · Found already. You are getting good at this.)*

**Caught the spark — `caughtItLine()`:**
- Got it! We did it!
- Yay! We caught it!
- Teamwork!
- You helped!
- Hehe, we got the spark!
- It didn't stand a chance against us!
- We caught it together!
- Spark caught! High five!

*(BABY: Got! · Spark! We got! · Yay! — FINAL: Caught together. Well done. · The spark chose us.)*

**Spark got away — `gotAwayLine()`:**
- Aww... it got away.
- So close!
- Hehe, next time!
- It was too quick!
- We almost had it!
- Sneaky little spark.
- We'll get the next one.
- Rematch?

*(BABY: Bye spark... · Got? No. · Next! — FINAL: It got away. Another will come. · Some sparks are simply quick.)*

**Caught mid-peek — `peekWinLine()`:**
- Boo! You caught me!
- Hehe, you got me!
- Peek-a-boo! You win!
- I wasn't ready!
- Sneak interrupted!
- You found me mid-peek!
- Hehe! Nice timing.
- Okay, you caught me.

*(BABY: Boo! You! · Hid! Found! · Hehe! — FINAL: You caught me mid-peek. Fair play. · Well timed. You found me.)*

**Missed the peek — `missedMeLine()`:**
- Hehe! You missed me!
- Too slow!
- I was right there!
- Peek! ...Oops, missed!
- Better luck next time!
- I even waved!
- Almost!
- Hehe, you blinked!

*(BABY: Here!...Missed! · Peek! You missed! · Boo...no. Hehe! — FINAL: You looked away at just the wrong moment. · Nearly. Try again when you are ready.)*

---

## 8. Memory & rare moments — `CurioPet.factLine()`

### Hatch day (once a year)
- It's my hatch day! Can we celebrate?
- Another year with you! Yay!
- It's my hatch day! I feel extra sparkly.
- A whole year of little discoveries together.
- It's my birthday-ish! Hehe!

### Active streak (3+ days, ~25%)
- Day `$streak`! We're still going!
- `$streak` days! I love our little rhythm.
- Streak `$streak`! Look at us!
- `$streak` days of curiosity. That's a lot of little sparks.
- Day `$streak`! Keep the glow going.

### Weekly keepsakes (top lane, 2+ saves)
- We saved `$count` `$lane` keepsakes this week!
- `$count` little `$lane` treasures! Wow.
- Our `$lane` shelf got `$count` new friends.
- `$count` for `$lane` this week. I like that.
- `$lane` is having a very good week.

### Season
- *Spring:* Spring! Everything feels new again. · Ooh, everything is waking up!
- *Summer:* Summer glow! Even the long days feel curious. · Warm days make me want to explore.
- *Autumn:* Ooh, autumn! Everything looks cozy. · The leaves are changing. I want to look at everything.
- *Winter:* Winter cozy! Come sit with me. · Cold outside, cozy little shelf inside.

### Weekday / weekend
- *Weekday:* Busy day? Let's sneak in a little curiosity. · Even busy days can have one tiny wonder.
- *Weekend:* Weekend! More time for little adventures. · Slow day, curious brain. Perfect.

### Last saved topic (~40%)
- "`$topic`" is still one of my favorites.
- I keep thinking about "`$topic`".
- Last keeper: `$topic`. Good choice.
- "`$topic`" was a good one, wasn't it?
- I remember "`$topic`". I'd peek at that again.

---

## 9. The learning brain — `CurioPetBrain.say()`

The local learning model composes one-sentence lines from real stats.

### Openings by dominant trait
- Curiosity: Ooh · Hmm · I wonder · Wait
- Playfulness: Wheee · Hehe · Boop · Ooh!
- Warmth (FRIEND+): Hey you · Friend · You're here · Hi again
- Warmth (default): Hey · Hello · Ooh, hi

### Bodies by mood
- PROUD: Level `$level`! We did that together. · `$level` already? I'm growing!
- EXCITED: Fresh ground again. Can we peek? · Something new is calling us.
- CURIOUS: We keep looking at `$lane`... should we finally peek? · Something new is waiting for us.
- HAPPY (streak 7+): Day `$streak` of our little streak. I like this rhythm. · Seven days together! Look at us.
- HAPPY (lane): I like how we keep coming back to `$lane`. · `$lane` feels like one of our places now.
- HAPPY (saves 5+, warm): `$saves` little sparks saved. I love our collection. · `$saves` keepsakes! Our shelf is getting full.
- HAPPY (saves 5+, default): `$saves` sparks saved. That's quite a little collection. · `$saves` keepsakes! Nice.
- HAPPY (else): I like being here with you. · More of this, please.
- SLEEPY: Even my glow is getting sleepy. · I'll be here when you come back tomorrow.
- FOCUSED: Take your time. I'll keep watch. · I'm staying quiet while you think.
- BOUNCY: I'm still bouncing from that! · That game made me happy.
- SHY (warm): Hehe... look at us, all friendly now. · (default): I'm still a little shy... give me a boop?
- GRUMPY: I think we need a little adventure. · The deck is too quiet. Help?
- PLAYFUL: I still have one more game in me. · Hehe, I'm not done playing.

### Coined catchphrases
- Morning discoveries are our little thing.
- Every saved spark is one more memory for us.
- New things! Always new things!
- I think I like our little adventures.
- Boops are very important. I have decided.
- We always seem to find something interesting.
- One little discovery at a time.
- I like having a shelf full of things we found together.

---

## 10. The tour script — `TourController.steps`

The first-run pet-led tour. `dialogue` is the bubble line; `nextHint` is the dock hint.

1. **Home (quest):**
   - dialogue: *Hi! I'm your little curiosity buddy. Want to take a tiny tour together?*
   - hint: *Tap Shuffle when you're ready.*

2. **Spin (spin button):**
   - dialogue: *This is where we find something new. Give it a spin and I'll peek with you!*
   - hint: *Spin to discover something new.*

3. **Reveal (express yourself):**
   - dialogue: *Ooh, did that spark a thought? Tap Express yourself and tell me about it. I'll keep it safe.*
   - hint: *Save your thoughts with a keepsake.*

4. **Cabinet (grid):**
   - dialogue: *This is our Cabinet! Everything you choose to keep comes home here.*
   - hint: *Your keepsakes live here.*

5. **Topic Browser (search):**
   - dialogue: *Want something specific? Browse Topics lets you look through everything and find exactly what you're curious about.*
   - hint: *Search or browse any topic.*

6. **Profile (avatar):**
   - dialogue: *This is your little journey. You can see your progress, badges, streak, and how much you've discovered.*
   - hint: *Your progress lives here.*

7. **Quests (daily):**
   - dialogue: *These are tiny things you can do each day. Finish them with me and we'll keep your curiosity moving!*
   - hint: *A little curiosity every day.*

8. **Settings (appearance):**
   - dialogue: *And here you can make things feel like you. You can choose your theme, manage permissions, and... hehe, you can design me!*
   - hint: *Make Curio yours.*

---

## 11. Pet Life routine lines — `CurioPet.matureRoutineLine(routineId)`

The fully grown pet keeps the same gentle personality, but speaks more calmly.

- look-around: Hmm... what should we notice today?
- little-wave: Hi from over here.
- stretch: A little stretch. Then we explore.
- turn-and-peek: Did I see something? Nope. Just me.
- tiny-stumble: Oops. I meant to do that.
- look-up: I wonder what is up there.
- backstage: One tiny look in the mirror. Hehe.
- victory-pose: We did it! Tiny victory pose.
- home-stretch: Home. Cozy.
- room-tour: Come on. I'll show you around.
- window-watch: The view is nice today.
- cozy-turn: Just checking my cozy little corner.
- home-dance: A tiny dance for no reason.
- deck-anticipation: Ooh... it's thinking!
- deck-side-peek: I wonder what it's going to choose.
- deck-stretch: Ready? Let's spin.
- deck-victory: Ooh! Good landing!
- quest-read: Hmm... let me see what we have to do.
- quest-wave: We can do this!
- quest-proud: Look at us go.
- quest-hide: Hehe... mysterious pet mode.
- topic-peek: Can we peek?
- topic-wow: Ooh... pretty interesting.
- topic-spin: That was a good one!
- topic-inspect: Hmm. Let's look closer.
- writing-focus: I'll keep watch while you think.
- writing-wave: Take your time. I'm here.
- writing-stretch: Tiny stretch, then back to your words.
- writing-shy: This thought looks important. I'll be quiet.
- shelf-hunt: Which little memory should we visit?
- shelf-wave: Our shelf is looking lovely.
- shelf-backstage: Hehe, I'm checking the back row.
- shelf-proud: Look at all the things we've found.
- mirror-check: Do I look cute? ...Yes.
- profile-wave: Hi, little profile.
- profile-proud: Look how much you've grown.
- profile-look-up: Another level is waiting. Let's go.


---

# BABY VOICE EXPANSION — Curie-isms

These are additional BABY-only lines. They make Curie feel more like a tiny
creature: short words, repeated sounds, excited squeaks, simple grammar, and
little bursts of curiosity. Curie can use her own name occasionally here as a
baby-like self-sound, while her normal evolved voice uses “I / me / my”.

## Curie sounds

- Curi! · Curie! · Curi-curi! · Curiii! · Curieee!
- Curi-cuu! · Curi-pip! · Curi-pip-pip! · Curi-pi!
- Pruu! · Prru! · Mrru! · Mrrp! · Mip! · Mipi!
- Pui! · Pwee! · Pupu! · Pippi! · Pipip!
- Bibi! · Bibu! · Bubu! · Buu! · Mimi! · Mimu!
- Kiri! · Kiri-kiri! · Kiki! · Kiki-kuri!
- Nyaa! · Nyu! · Nyuu! · Mew? · Mrr?
- Chuu! · Chupi! · Chup-chup!
- Poka! · Poki! · Poko! · Poku!
- Tii! · Titi! · Tutu! · Tuu!
- Wawa! · Wiii! · Wuu! · Wee-wee!
- Hm? · Hmmu? · Huh? · Eh? · Eeeh?
- Ooh! · Ooo! · Ooooh! · Ohi!
- Aha! · Awu! · Awwu!
- Hehe! · Hihi! · Ehehe!
- Pfft! · Pff! · Hmph! · Hmp!
- Blep! · Blip! · Blup! · Bloop! · Boop!
- Squeak! · Squee! · Peep! · Pip!
- Ziiip! · Zoom! · Vwoom! · Whee!
- Prrr... · Mrrr... · Purr-purr! · Mrrp!

## Tiny Curie phrases

- Curi! Look!
- Curi! Ooh!
- Curi wants!
- Curi go!
- Curi see!
- Curi peek!
- Curi found!
- Curi happy!
- Curi curious!
- Curi sleepy...
- Curi likes!
- Curi loves!
- Curi yes!
- Curi nooo!
- Curi wait!
- Curi here!
- Curi there!
- Curi got it!
- Curi did it!
- Curi win!
- Curi big!
- Curi grow!
- Curi glow!
- Curi shiny!
- Curi tiny!
- Curi fast!
- Curi zoom!
- Curi boop!
- Curi peek-peek!
- Curi curious-curious!
- Curi happy-happy!
- Curi go-go!
- Curi more-more!
- Curi again-again!
- Curi want more!
- Curi see more!
- Curi found you!
- Curi found it!
- Curi found something!
- Curi knows! Maybe.
- Curi thinks...
- Curi has idea!
- Curi big idea!
- Curi very curious!
- Curi super curious!
- Curi sparkle!
- Curi sparkle sparkle!
- Curi wiggle!
- Curi bounce!
- Curi pounce!
- Curi ready!

## Happy / affectionate

- Curi happy-happy!
- Yay yay Curi!
- Cozy Curi!
- Curi cuddle!
- Curi snuggle!
- Curi hug!
- Tiny hug!
- Big hug!
- Curi likes you!
- Curi likes you lots!
- You here! Yay!
- You came! Yay yay!
- Curi missed you!
- Miss miss!
- You back!
- Back back!
- Home home!
- Curi here!
- Curi stay!
- Hehe! Curi happy!
- Prrr... happy.
- Mrrp! Happy!
- Squee! You!
- Curi loves boops!
- Boop makes happy!
- Pats! More pats!

## Excited / creature-like

- OOOH!
- Ooh ooh ooh!
- Curi OOOH!
- Look look look!
- New new new!
- New thing!
- New thing! New thing!
- Curi see!
- What what?
- That! That!
- There! There!
- Ooh, there!
- Shiny!
- So shiny!
- Pretty!
- Curi likes shiny!
- Curi wants peek!
- Peek peek peek!
- Curi peek now?
- Can Curi see?
- Want! Want!
- Curi want!
- Go go go!
- Fast fast!
- Wheee!
- Wiii!
- Pwee!
- Zoom zoom!
- Curi zoom!
- Curi go zoom!
- Pounce!
- Pounce pounce!
- Curi ready!
- Ready ready!
- Yay! New!
- Ooh! New!
- Wow wow!
- Woooow!
- Curi wow!

## Curious little questions

- What this?
- What that?
- Who this?
- Who that?
- Why?
- Why why?
- How?
- How that?
- Where?
- Where go?
- Where it go?
- Curi look?
- Curi peek?
- Can Curi see?
- Can Curi touch?
- Can Curi try?
- What's inside?
- What hiding?
- Something there?
- Something new?
- Is it shiny?
- Is it fun?
- Is it tiny?
- Is it big?
- Is it ours?
- We keep?
- We look?
- We go?
- Again?
- More?
- More more?
- Another?
- Another one?
- Curi wonder...
- Curi wonder why.
- Curi wonder what.
- Curi need know!
- Curi must know!
- Curi curious!

## Discovery / topic

- Ooh! Topic!
- Curi found topic!
- New topic!
- Topic! Topic!
- Curi see new!
- What's this one?
- Ooh, this one!
- This one! This one!
- Good one!
- Curi likes this one!
- You picked! Yay!
- Curi approve!
- Good pick!
- Very good pick!
- Curi was hoping!
- Hehe, you picked it!
- We look now?
- Open open!
- Show Curi!
- Curi wants see!
- Curi ready peek!
- Peek now!
- Ooh... interesting!
- Hm! Interesting!
- Curi curious now!
- Brain awake!
- Curi brain go!
- Questions! Questions!
- So many questions!
- Curi needs answers!
- Let's see!
- Let's look!
- Curi look close!
- Closer! Closer!
- Ooh wow!
- Tiny wow!
- Big wow!

## Save / keepsake

- Save!
- Save save!
- Keep!
- Keep keep!
- Ours!
- Ours ours!
- Curi keep!
- Curi guard!
- Safe!
- Safe safe!
- Shelf!
- Shelf shelf!
- Curi keep safe!
- Another treasure!
- Little treasure!
- Tiny treasure!
- Curi likes treasure!
- We keep this!
- This one stays!
- Don't lose!
- Curi remember!
- Little memory!
- Memory memory!
- Yay, saved!
- Saved saved!
- Keeper!
- Good keeper!
- Curi found keeper!

## Boops / touch

- Boop!
- Boop boop!
- Boop boop boop!
- Curi boop!
- Boop Curi!
- More boop!
- More more!
- Again boop!
- Boop again!
- Tiny boop!
- Big boop!
- Soft boop!
- Nose boop!
- Pat pat!
- Pat pat pat!
- Curi likes pats!
- Mmm... pats.
- Hehe! Tickles!
- Tickly!
- Tickle tickle!
- Ooh! Tickles!
- That's Curi ear!
- My ear!
- Soft!
- So soft!
- Curi squeak!
- Squee!
- Mrrp!
- Purr-purr!
- Curi wiggle!
- Wiggle wiggle!
- Tiny squish!
- More cuddles?

## Play

- Play!
- Play play!
- Curi play!
- Play with Curi!
- Again!
- Again again!
- More!
- More more more!
- Chase!
- Chase Curi!
- Catch Curi!
- Curi fast!
- Too fast!
- Hehe, catch!
- Can't catch!
- Curi zoom!
- Zoom zoom zoom!
- Wheee!
- Bounce!
- Bounce bounce!
- Jump!
- Hop hop!
- Pounce!
- Pounce pounce!
- Tag!
- You're it!
- My turn!
- Your turn!
- Curi turn!
- Play more?
- Please?
- Pleeease?
- Curi wants play!
- Tiny game!
- Big game!
- Game time!
- Go go!
- Ready!
- Ready ready!
- Hehe! Go!

## Sleepy

- Sleepy...
- Curi sleepy...
- Yawn...
- Big yawn!
- Tiny yawn!
- Zzz...
- Zzz... Curi...
- Night night...
- Bed bed...
- Cozy...
- Soft...
- Warm...
- Curi curl up...
- Curl curl...
- Curi nap?
- Nap time?
- Sleep now?
- Curi tired...
- Tiny tired...
- Paws tired...
- Brain tired...
- Curiosity sleepy...
- One more?
- One more peek...
- Then sleep.
- Curi stay...
- Curi here tomorrow.
- Good night...
- Night night, you...
- Prrr... sleepy.
- Mrrr... sleepy.

## Shy

- ...Hi.
- H-hi!
- Curi shy...
- Shy shy...
- *peek*
- Peek...
- Curi hiding.
- Hide hide.
- Don't look!
- ...Okay look.
- You saw Curi!
- Hehe...
- Eeeh...
- Curi blush!
- Tiny wave!
- *tiny wave*
- Curi here...
- Curi maybe brave.
- Brave? Maybe.
- Curi try hi.
- Hi hi...
- Curi likes you... a little.
- Curi likes you lots.
- Don't tell!
- Secret!
- Curi secret!
- Hehe... shy.

## Grumpy

- Hmph!
- Hmph hmph!
- Grumpy!
- Curi grumpy.
- Nooo!
- Curi no!
- Nope!
- Nuh-uh!
- Not fair!
- Hmph. Boring.
- Curi bored.
- So bored!
- Too quiet!
- No fun!
- Curi needs fun!
- Curi needs spin!
- Spin now?
- Please spin.
- Curi waiting.
- Curi waited long!
- Pfft!
- Curi pout.
- Tiny pout.
- Curi not pouting!
- Maybe pouting.
- ...Fine.
- Okay fine!
- Curi forgive.
- Hehe... maybe.

## Proud / level-up

- Big Curi!
- Curi grow!
- Grow grow!
- Curi bigger!
- Look! Bigger!
- New Curi!
- Shiny Curi!
- Curi glow!
- Glow glow!
- Level up!
- Up up!
- Curi up!
- Yay! Level!
- Big level!
- Curi did it!
- We did it!
- Curi strong!
- Tiny strong!
- More spark!
- More more spark!
- Curi sparkle!
- Sparkle sparkle!
- Look at Curi!
- Curi proud!
- Proud Curi!
- Hehe! Big!

## Evolution

- Ooh! What happened?!
- Curi changed!
- Curi grow!
- Big Curi!
- New Curi!
- Look look!
- Curi glow!
- So shiny!
- Curi feels different!
- Curi got new spark!
- Spark bigger!
- Curi evolved!
- Evolved Curi!
- Wow!
- Woooow!
- Curi big now!
- Still Curi!
- Curi still me!
- New me! New me!
- Curi likes new me!
- Hehe! Look!

## Explore

- Go!
- Go go!
- Curi go!
- Explore!
- Explore explore!
- See!
- Look!
- Curi look!
- Find!
- Find find!
- New!
- New new!
- Adventure!
- Tiny adventure!
- Big adventure!
- Curi ready!
- Take Curi!
- Curi coming!
- Wait! Curi coming!
- Let's go!
- Come come!
- This way!
- That way!
- Which way?
- Ooh, there!
- Curi found!
- Something!
- Something there!
- What's hiding?
- Curi investigate!
- Curi look around!
- Curi curious!
- Curi very curious!

## Friendship / bonding

- You!
- You here!
- Curi happy!
- Curi missed you!
- Missed missed!
- Curi waited!
- You came back!
- Yay you!
- Curi likes you!
- Curi likes us!
- Us us!
- We go!
- We play!
- We look!
- We find!
- We keep!
- We did it!
- Our shelf!
- Our spark!
- Our little thing!
- Curi stay with you.
- You stay with Curi?
- Together!
- Together together!
- Curi friend!
- Friend friend!
- Best friend!
- Curi and you!
- You and Curi!
- Us!

## Tiny failures

- Aww...
- Aww, no!
- Curi missed!
- Missed!
- Oops!
- Oopsie!
- Curi oops!
- Uh-oh!
- Uh-oh uh-oh!
- Not got!
- Almost!
- So close!
- Curi almost!
- Again?
- Try again!
- Curi try again!
- Next time!
- Next next!
- It got away!
- Bye-bye spark...
- Curi sad...
- Tiny sad.
- Aww... come back!
- Curi wanted that!
- Hehe... oops.
- Little mistake.
- Curi okay!
- Curi try!

## Surprise / confusion

- Huh?
- Huhhh?
- Eh?!
- What?!
- Ooh?!
- Curi confused.
- Brain... hmm.
- Curi doesn't know.
- Don't know!
- Curi think...
- Think think...
- Hmm hmm...
- Wait wait!
- Wait!
- What happened?
- Where go?
- It moved!
- Curi saw that!
- Did you see?
- You saw?
- Again?
- What was that?!
- Ooh, weird!
- Weird weird!
- Strange!
- Need look!
- Need investigate!
- Curi investigate!
- Mystery!

## Rare silly baby lines

- Curi has no thoughts. Only sparkle.
- Curi brain go boop.
- Curi forgot what Curi was thinking.
- Curi was busy being tiny.
- Curi saw a dust.
- Dust suspicious.
- Curi has important mission.
- Mission: boop.
- Mission: discover!
- Curi found nothing. Found you instead!
- Curi approves!
- Curi does a wiggle.
- *wiggle wiggle*
- Curi has become pancake.
- Curi is tiny.
- Maximum tiny!
- Tiny mode!
- Curi zoom protocol!
- Sparkle mode!
- Boop mode!
- Sneaky mode!
- Sleepy mode...
- Curi loading...
- Curi thinking...
- Curi ready!
- Curi not ready!
- Curi ready now!
- Hehe. Curi.
- Curi says hi.
- Curi says bye.
- Curi says boop.
- Boop is important.
- Curi has decided.
- Decision: yes!
- Decision: more!
- Decision: again!
- Decision: boop!

---

## Integration checklist

1. Edit this file first — same section, same bullet/pool names.
2. Port the matching text into `CurioPet.kt`, `CurioPetBrain.kt`, and `TourController.kt`.
3. Keep placeholders verbatim: `__LANE__`, `$lane`, `$savedLane`, `$streak`, `$count`, `$topic`, `$level`, `$saves`.
4. Keep pool order when porting.
5. Run the brace-balance check and rely on CI for compilation.