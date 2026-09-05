# Request Log — fetch is provider-exclusive + Clear-covers memory-cache fix

## Status: implementation complete — committing & pushing (CI will validate)

## The request (user)
"the clear all covers doesnt work. and also i feel like the fecthing of
covers is still uses the old api, heres what i want from other auto
loading they are fine but when i select the provider and tap fetch then
it should only feth from that for the fetch button not for the fallbacks"

## Diagnosis
Two root causes, both in `BookCoverFetch`:
1. **"Clear doesn't work":** `clearAllCovers` only cleared the Coil DISK
   cache. Decoded covers kept rendering instantly from the MEMORY cache,
   so the hub's strip (and the reveal poster) looked unchanged after a
   clear — it seemed broken.
2. **"Still uses the old API":** `resolveVerifiedCoverUrl` (the bulk-fetch
   path) built candidates as stored-URL → authored imageUrl → chosen
   provider → cascade of ALL providers. Books with real authored covers
   kept their identical image no matter which provider was selected
   (authored always won first), so clearing + re-fetching with a new
   provider showed the same covers — the fetch button was never a pure
   provider test.

## What was done (BookCoverFetch.kt)
- `resolveVerifiedCoverUrl`: now resolves ONLY the chosen provider's
  URL(s) — no stored URL, no authored imageUrl, no fallback cascade.
  A book the chosen provider can't serve is marked failed (lands in the
  failed list / retry set). Verification (`loadsRealImage`, ≥40px short
  edge) still applies. The reveal poster / share card / hub tiles keep
  their own authored-first fallback via `coverCandidates` — the "auto
  loading" the user said is fine — untouched.
- `clearAllCovers`: now clears BOTH Coil caches — `memoryCache?.clear()`
  in addition to `diskCache?.clear()`.

## Docs
- app/AGENTS.md: **v363** entry (provider-exclusive fetch + memory-cache
  clear).
- fastlane changelog 20260921.txt: FIX bullet at the top.
- Prompt.md: this log.

## Verification
- Braces/parens balanced (111/111, 320/320) on BookCoverFetch.kt.
- `resolveVerifiedCoverUrl` has exactly one caller (`fetchAll`) — no other
  path depended on the old cascade. CI will validate.