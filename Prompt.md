# Request — prevent detail-view text overlap and cramped sections

## Completed

- Kept the detail page's category label, quick fact, tags, and format body in one sequential reading column with explicit spacing.
- Removed the nested tag `MorphEntrance`, which could scale/paint tags through neighboring long text during entry.
- Replaced the detail-body `AnimatedVisibility` entrance with a measured `Box` whose content is fully laid out immediately and only fades via `graphicsLayer`; long descriptions and note cards no longer move through one another while the Cabinet → Detail morph settles.
- Increased spacing between metadata, “My thoughts,” and the Field Notes sections (“Observed,” “Surprised me,” and “Want to learn next”) so similar long-text blocks have the same breathing room.
- Preserved dynamic note-paper sizing and wrapping; no fixed-height content was introduced.

## Validation

- EntryDetailScreen delimiter balance passed.
- `git diff --check` passed.
- Animation import/use audit passed after removing obsolete `AnimatedVisibility`, `MutableTransitionState`, `fadeIn`, and `MorphEntrance` usage from this reading flow.
- Code review found no concrete Kotlin/Compose or remaining-overlap blocker.
- Gradle builds are forbidden locally by the Curio DOX rules; CI remains the compile gate.
