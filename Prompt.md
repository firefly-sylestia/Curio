# Prompt.md — the running log of the current request

This file is replaced for each new request (see `AGENTS.md`). It records what was asked,
what was found, what was decided, and what is still open, so the next session can start
from the state rather than from memory.

---

## 1. The request (this session)

> also is the form only visible to online users? also why it doesnt appear in home
> automatially it only appears after i open it in settings, also the theme of
> jadeorchid,ocean,sand,emeber,in dark mode the problem is their card colors in settings
> profile and many places, some are too bright in dark mode only even the hero color its
> too bright looking only in dark mode again. please fix it overally. only in dark mode,
> keep the colors muted fix the settings and profie buttom cards where settings otpions
> etc reside. and then update the incursion ui, it looking bad and doesnt matches the app
> style at all, and also add cover fetching to them series fetching, use the buttom sheet
> we use in topic reveal to show its details with proper infomation fetch and while
> keeping its old informations. also the activation overlay it shows upgrdae it, in books
> add manga support too manhwa etc, and then lets expand more providers more was to fetch
> scrape data in app. also in book detail screen if ive not added the pdf for page or
> suppose it using epub for pages in detail screen it doesnt show the pages count from
> it,so fix it, also for epub in the page swpie the nsid eby side pages they are kind of
> bad view and also lagging on pinc to zoom in epub fix it as well, and still the title
> detection for pubs are bad.

Confirmed with the member earlier via `ask_user`:

- **Sequence:** Colours → Incursion → Books.
- **Feedback form:** it must greet whoever opens the app the moment a form is published.
- **Incursion:** keep the TWO-COLUMN shape, fix the look so it matches the app; fix the
  unlock announcement's wording (it says "upgrade" — there is no upgrade in Incursion).
- **Books:** a bottom sheet like the topic reveal for a series' details (fetching proper
  information while keeping what is already stored); cover fetching for series; manga +
  manhwa support; more providers/scraping sources; full kind + link reading.
- **Dark colours:** "Mute cards + hero + accents" — dark mode only.

## 2. Findings

**The feedback form is online-only for two reasons, and both are load-bearing.**

- `FeedbackFormState.refresh` returns unless `AppPreferences.isOnlineModeEnabled` AND
  `OnlineAccount.state.session?.accessToken != null` — the form is published on the
  server and read with the member's own token. A member with Online mode off (or signed
  out) never sees a form at all. That is not a bug; it is the only way the form can reach
  the app without a bundled copy.
- **Why Home missed it:** Home's `LaunchedEffect(Unit) { FeedbackFormState.refresh(ctx) }`
  runs at first composition — usually BEFORE `OnlineAccount` has restored its session
  from storage. `refresh` bails on the null token, `fetched` stays false so it *could*
  retry, but nothing else asks until Support/Settings composes its own
  `LaunchedEffect(Unit)`. That is exactly "it only appears after I open it in Settings".

**Dark named themes are too bright because their fills carry daylight chroma.**

- `darkScheme()` built the page at HSL 0.36 chroma and the card ladder at 0.34 → 0.24,
  topping out at lightness 0.30 — while the app's OWN dark ladder is a neutral
  `#121212…#2C2C2C`. A named dark card therefore sits at more than twice the luminance of
  the scheme the app was tuned against, and a saturated one at that — "some are too bright
  in dark mode" on a page of settings cards.
- The dark hero was HSL 0.50 chroma at 0.33 — a lit, saturated slab.
- The dark accent was measured at 3.79–4.05:1 against the top card step — under the 4.5
  bar on the very surface it is usually drawn on.

## 3. What was built

*(in progress — see §5)*

## 4. Still open

*(nothing yet)*

## 5. Work log

- **Phase 1 — the form and the night.** (in progress)

## User prompts

*(Never cleared. A new prompt from the user goes here with its status; when it is done, its
status is updated and it is moved into the request log above. One empty slot for the next
prompt stays below it.)*

- (none)
