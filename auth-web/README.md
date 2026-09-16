# Curio Account Web (`auth-web/`)

The web side of Curio's online accounts: the pages Supabase email links land on,
plus a small account desk. It is a **static site with two tiny serverless
functions** (no build step, no framework, no npm dependencies).

## Why it exists

The Android app calls `POST /auth/v1/signup` with no redirect, so Supabase sends
confirmation and recovery emails to the project's **Site URL**, which defaults to
`http://localhost:3000`. That is the "the link opens localhost" bug. This site is
the destination those emails should have, and the only place a password can be
set or an account deleted without touching the app.

## Pages

| URL | What it does |
| --- | --- |
| `/` | Landing page: every flow, one tap away. |
| `/confirm` | Where a **signup confirmation** email lands. Verifies the token, says so, offers a new link when it expired. |
| `/link` | Where a **sign-in link** (magic link) lands. Consumes the token and hands you to `/account`. |
| `/reset` | Two modes: request a reset email, and set the new password from that email's link. |
| `/signin` | Email sign-in link (optionally creating the account), or password sign-in. |
| `/account` | Your profile, your counts, sign out, and **delete my account**. |
| `/support` | Link problems, password help, data questions, where to report things. |
| `/privacy`, `/terms` | The online notice and terms from `docs/ONLINE_PRIVACY.md` and `docs/ONLINE_TERMS.md`. |

## Environment variables (set these in Vercel)

The static pages read nothing from the environment themselves: they call
`/api/config`, which reads these server-side. So the anon key is never baked into
a committed file.

| Variable | Required | Purpose |
| --- | --- | --- |
| `SUPABASE_URL` | yes | The project URL, e.g. `https://abcd.supabase.co` (same value the Android build uses). |
| `SUPABASE_ANON_KEY` | yes | The publishable/anon key. `SUPABASE_PUBLISHABLE_KEY` is accepted as an alias. |
| `SUPABASE_SERVICE_ROLE_KEY` | yes | **Server-only.** Used by `/api/delete-account` to remove the account. Never sent to the browser. |
| `SUPPORT_EMAIL` | recommended | Published on `/support` and `/privacy` as the contact address. Without it those pages point at the in-app route instead. |
| `APP_DOWNLOAD_URL` | optional | The "Get Curio" link. Defaults to the latest GitHub release. |
| `SITE_NAME` | optional | Defaults to `Curio`. |

Vercel: **New Project → import the repo → Root Directory `auth-web`** → add the
variables above (Production and Preview) → Deploy. There is no build command and
no output directory to configure; Vercel serves the folder and picks up `api/`.

## Supabase dashboard checklist

1. **Authentication → URL Configuration**
   - **Site URL**: `https://<your-domain>` (this is the fallback for every email).
   - **Redirect URLs**: add `https://<your-domain>/**` (and
     `https://*-<your-team>.vercel.app/**` if you want preview deployments to
     work). A `redirect_to` that is not on this list is silently ignored, and the
     email link goes to the Site URL instead.
2. **Authentication → Email Templates**: this site works with both the classic
   `{{ .ConfirmationURL }}` links and the newer `{{ .RedirectTo }}` templates.
   The modern form is:
   - Confirm signup: `{{ .RedirectTo }}/confirm?token_hash={{ .TokenHash }}&type=email`
   - Reset password: `{{ .RedirectTo }}/reset?token_hash={{ .TokenHash }}&type=recovery`
   - Magic link: `{{ .RedirectTo }}/link?token_hash={{ .TokenHash }}&type=magiclink`
   With `{{ .RedirectTo }}`, the destination comes from the request (`redirect_to`),
   which the Android app now sends. The classic `{{ .ConfirmationURL }}` keeps
   working too: it goes through GoTrue's `/verify` and redirects here with the
   session in the URL fragment, and this site reads both shapes.
3. **Authentication → Providers → Email**: keep "Confirm email" on if you want
   new accounts to prove the address before signing in.

## Local preview

```bash
npx vercel dev --cwd auth-web     # or: cd auth-web && npx vercel dev
```

A plain static server (`npx serve auth-web`) serves the pages but **not**
`/api/config`, so the pages would report that the site is not configured yet.
That is the designed behaviour when the variables are missing: every page keeps
working and explains itself instead of failing.

## Notes

- Sessions live in `localStorage` (`curio.web.session`), which is what a static
  site can do. Token material never touches the URL of any page after the
  landing one, and `Referrer-Policy: no-referrer` plus a strict CSP (see
  `vercel.json`) keep it that way.
- `/api/delete-account` never trusts a user id from the request: it calls GoTrue
  with the caller's own token, and deletes the id that comes back.
- The privacy notice and terms published here are the text of
  `docs/ONLINE_PRIVACY.md` and `docs/ONLINE_TERMS.md`, without their internal
  launch-review banner. If those documents change, re-sync these two pages.
