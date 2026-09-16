# Curio Account Web (`auth-web/`) — AGENTS.md

## DOX Framework

This file is a child of the DOX hierarchy defined in `master.md`. Its parent DOX
rail is the root `AGENTS.md`.

**DOX chain:** `master.md` ← `AGENTS.md` (root) ← `auth-web/AGENTS.md` (this file)

Read `master.md` and the root `AGENTS.md` first, then this file.

## Purpose

The web side of Curio's online accounts: the pages Supabase email links land on
(confirmation, magic link, password recovery) plus a small account desk that can
sign in, review the account and delete it. Deliberately a **static site with two
serverless functions**: no build step, no framework, no npm dependencies, so it
can never break the Android CI and can be deployed straight from the repo.

## Ownership

- `index.html`, `confirm/`, `link/`, `reset/`, `signup/`, `signin/`, `account/`,
  `support/`, `privacy/`, `terms/` — the pages. Each one is a complete HTML
  document that links the two shared assets and declares which flow it is with
  `<body data-page="...">`.
- `assets/theme.css` — the whole design system (tokens, glass surfaces, buttons,
  fields, notices, the aurora backdrop, motion, and a light-mode override).
- `assets/curio.js` — the only script: config fetch, the GoTrue client, session
  storage, URL token handling, copy, and the per-page wiring keyed on
  `document.body.dataset.page`. No inline scripts anywhere (the CSP forbids
  them), so every behaviour a page needs must be expressible as a `data-` hook
  this file already knows.
- `api/config.js` — serves the public Supabase URL + anon key (and the optional
  support address and download URL) from Vercel environment variables.
- `api/delete-account.js` — the ONE privileged endpoint. It re-derives the
  caller's identity from their own access token before deleting anything with
  the service-role key.
- `vercel.json` — security headers (strict CSP, no-referrer, frame denial) and
  asset caching. There is no build configuration on purpose.
- `README.md` — the operator guide: environment variables, the Supabase
  dashboard checklist, local preview.

## Local Contracts

- **The service-role key never reaches the browser.** It is read in
  `api/delete-account.js` only. `/api/config` returns the anon key alone. Never
  add an endpoint that echoes a server-only variable, and never inline a secret
  into a page.
- **An account's first password is set on `/signup`; a link never creates
  one.** `signin/`'s "this address is new" checkbox carries the address to
  `/signup` (sessionStorage, not the URL) and the link form sends
  `create_user: false`, so a magic link can only sign an existing account in.
  `/signup` requires the email, the password twice, and the terms acceptance
  the app's create mode requires, then sends the account to `/confirm` for the
  address to be proved.
- **`/api/delete-account` trusts nothing from the request body.** The caller's
  `Authorization: Bearer <access token>` is verified against GoTrue
  (`GET /auth/v1/user`), and the id that call returns is the one deleted. A
  client-supplied `userId` must never be honoured, and the typed `confirm:
  "DELETE"` is re-checked server-side because the dialog is only a UI affordance.
- **Two token shapes are supported on purpose.** Supabase can deliver a session
  as a URL fragment (`#access_token=...`, the classic path through GoTrue's
  `/verify`), as `?token_hash=&type=` (the modern template form, exchanged at
  `POST /auth/v1/verify`), or as an error fragment (`error_code`,
  `error_description`). `assets/curio.js` normalises all three; a new page must
  use that normaliser rather than reading `location` itself.
- **Requests carry `redirect_to` as a QUERY parameter**, which is what GoTrue
  reads for `/signup`, `/recover`, `/otp` and `/resend`. It is always built from
  `location.origin`, so the site works on any domain, dashboard domain and
  preview URL without a hardcoded hostname. **Every path that sends an email
  must pass it** — a missing `redirect_to` silently falls back to the project's
  Site URL, which is the `http://localhost:3000` bug this site exists to fix
  (this is exactly what happened to `/resend`).
- **A missing configuration is a state, not a crash.** With no environment
  variables, `/api/config` answers `{ configured: false }` and every page renders
  its own "not configured yet" notice. Keep that path working: it is what a fresh
  clone and a half-finished deploy look like.
- **No inline scripts or styles** (the CSP has neither `unsafe-inline` for
  scripts nor for styles). Element styling goes through classes in
  `assets/theme.css`; dynamic values are set through CSSOM
  (`element.style.setProperty`) or `classList`, never as a `style="..."` string
  in markup.
- **Copy rules:** no em dashes, no jargon from the server (`JWT`, `PGRST`,
  status codes), and every failure state names the ONE next action. The same rule
  the Android surfaces follow.
- **The legal pages are a mirror.** `privacy/` and `terms/` carry the text of
  `docs/ONLINE_PRIVACY.md` and `docs/ONLINE_TERMS.md` **without** the internal
  "Launch requirement" banner that those files open with (it is an operator note,
  not part of the published notice). If the docs change, re-sync the pages in the
  same edit.
- **No analytics, no third-party scripts, no cookies.** The only external
  requests are Google Fonts and Supabase itself.

## Work Guidance

- New page: copy the shell of the closest existing page, set `data-page`, add its
  ids to the wiring switch in `assets/curio.js`, and add the route to `README.md`.
- The site has no test runner and no linter: validate with
  `node --check assets/curio.js api/*.js` and `python3` tag-balance checks, then
  deploy a preview.
- Deployment is a Vercel project whose Root Directory is `auth-web/` (see
  `README.md`). Nothing in this folder participates in the Android or desktop
  builds.

## Verification

- `node --check` on every JavaScript file (the functions are Node ESM).
- Tag balance on every HTML file, and a grep that no page hardcodes a Supabase
  URL, a key, or an inline `style=`/`<script>` block.
- CI does not build this folder; a Vercel preview deploy is the real check.

## Child DOX Index

No child AGENTS.md files defined yet.
