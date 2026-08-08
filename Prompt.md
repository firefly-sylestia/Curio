# Request — fix detail-view text and entry overlap

## Completed

- Reworked the detail category header so the category name and saved-entry title share one constrained vertical column instead of competing weighted text children in the same row.
- Allowed the saved-entry title to use up to two lines with ellipsis, while keeping the optional Legacy badge stable.
- Increased the shared detail hero height from 360dp to 400dp so the title and metadata strip remain inside the morph target instead of colliding with the content below.
- Made FieldMind metadata and structured detail values explicitly wrap inside their weighted value columns, preventing long descriptions from painting over adjacent content.

## Validation

- EntryDetailScreen delimiter balance passed.
- `git diff --check` passed.
- Static review found no concrete Kotlin/Compose blocker.
- Gradle builds are forbidden locally by the Curio DOX rules; CI remains the compile gate.
