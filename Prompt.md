# Request Log — album track data repair (grouped/mangled multi-part tracks)

## Status: implementation complete — committed & pushed (CI pending)

## The request (user, paraphrased)
- Sonic Youth's Daydream Nation album has a "trilogy" at the end that shows
  as ONE track instead of properly showing its parts — fix it and check for
  similar albums.

## Root cause
The albums catalog was generated from a source that joined multi-part /
multi-song titles into one track string with quoting artifacts
(`Trilogy" * a) "The Wonder" * b) "Hyperstation" * z) "Eliminator Jr.`,
`Medley" *I. "The Lark in the Morning" ...`, `The Fish Cheer" / "I-Feel-Like...`).
Scanning all 999 albums found 62 such tracks across ~25 albums. Most are
single-indexed medleys/suites (one track on the record — only the quoting
was broken), but a few merged tracks that the CD/streaming index SEPARATELY.

## What was done (data/topics/albums.json, web-verified)
### Split into the separately-indexed tracks (showed as one -> now separate):
- Daydream Nation (Sonic Youth): "Trilogy" 14:07 -> tracks 12-14
  "Trilogy: a) The Wonder" 4:15 / "Trilogy: b) Hyperstation" 7:12 /
  "Trilogy: z) Eliminator Jr." 2:37 (Sonic Youth deliberately used "z)").
- A Love Supreme (Coltrane): merged Part 3/Part 4 -> "Part 3: Pursuance"
  10:42 + "Part 4: Psalm" 7:05.
- Everything Will Be Alright in the End (Weezer): "The Futurescope Trilogy"
  -> I. The Wasteland 1:56 / II. Anonymous 3:19 / III. Return to Ithaka 2:17.
- Moanin' (Art Blakey): track 8 was a DUPLICATE of track 4's Drum Thunder
  Suite; it is the RVG-edition bonus "Moanin' (Alternate Take)".
### Single-indexed tracks kept as one, quoting artifacts cleaned to the
  official titles: Grievous Angel medley, Liege & Lief "Medley: The Lark in
  the Morning / Rakish Paddy / Foxhunter's Jig / Toss the Feathers", Beach
  Boys' Party! "Medley: I Get Around / Little Deuce Coupe", Sam Cooke
  medley, In a Silent Way sides ("Shhh / Peaceful", "In a Silent Way / It's
  About That Time"), Run the Jewels 3 closer, The Score "Manifest / Outro",
  Red Headed Stranger, Dopethrone "Weird Tales / Electric Frost / Golgotha /
  Altar of Melektaus", Bloody Kisses "Christian Woman", Deja Vu "Country
  Girl", The Yes Album "Starship Trooper", The Black Saint modes, Magnification
  "In the Presence Of", Minstrel "Baker St. Muse", Journey "A Seed's a Star /
  Tree Medley", Hymns to the Silence credit garbage.
### Woodstock soundtrack: all 19 titles cleaned from
  "Artist - Title (composer) - duration (extra)" to "Artist - Title"
  (the track's own duration field was kept).
### Generic pass: removed every remaining literal quote artifact
  (`"A" * "B"` -> `A / B`). 0 artifacts remain; structural validation clean
  (numbers sequential, titles non-empty).

## Notes / follow-ups
- tools/fix_album_tracks.py committed as the rerunnable script.
- Intentionally ambiguous one-index cases (Godspeed's long multi-movement
  titles, Amy Winehouse Frank hidden-track merges, Smile session comps) were
  only unquoted, NOT split — their structures genuinely vary by edition.
- Store changelog got one FIX bullet.