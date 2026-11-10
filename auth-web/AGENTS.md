# Curio Web + Account Site (`auth-web/`) — AGENTS.md

## Purpose

The single, installable Curio website. This directory is both the Vercel project root and the source root for the React discovery app, account pages, Supabase functions and PWA.

## Ownership

- `src/` — React + TypeScript discovery experience: Home, Explore, Shuffle, Cabinet, Profile, Settings and supporting routes.
- `index.html`, `vite.config.ts`, `tailwind.config.js`, `postcss.config.js`, `tsconfig*.json` — Vite application/build configuration.
- `public/` — static account routes, PWA manifest, service worker, icons and account assets. Vite copies this directory to the build output.
- `api/config.js` — public Supabase configuration only.
- `api/delete-account.js` — guarded account deletion endpoint. The service-role key stays server-side.
- `assets/curio.js`, `assets/theme.css` — retained account source assets. The deployed account pages use the matching copies under `public/assets/`.
- `vercel.json` — Vercel build, output, security headers and SPA deep-link rewrites.
- `package.json` and `package-lock.json` — one npm/Vite toolchain for the entire site.

## Local Contracts

- **One deployment root:** Vercel Root Directory remains `auth-web`. Do not rename the project, change the root directory, or create a second public website deployment as part of ordinary web work.
- **One public Curio site:** `/` is discovery. Account routes stay on the same origin at `/signin`, `/signup`, `/confirm`, `/link`, `/reset`, `/account`, `/support`, `/privacy` and `/terms`.
- **Navigation:** Home, Explore, Shuffle and Cabinet are the four primary destinations in the floating glass navigation pill. Keep the icon, label, active state and moving indicator aligned to all four equal slots.
- **Local-first discovery:** topic history, saved entries and cabinet data remain local in the browser. Signing into the online account does not imply that local discoveries are backed up or synced.
- **Account security:** `SUPABASE_SERVICE_ROLE_KEY` is used only by the account deletion serverless function. Never expose it in browser code, Vite variables, HTML, or logs. The deletion function verifies the caller's bearer token with GoTrue and deletes only the verified user id.
- **PWA cache:** the service worker may cache same-origin app shell/static GET assets. It must bypass API routes, Supabase traffic and authentication callback URLs containing tokens or auth errors.
- **CSP:** Vite/React currently uses inline style attributes for dynamic UI styling, so `style-src 'unsafe-inline'` is required. Keep inline scripts prohibited.
- **Account routes:** account HTML pages are under `public/<route>/index.html` so Vite emits them into `dist`; their source assets are under `public/assets/`. Preserve their paths for Supabase email templates.
- **No topic catalog rewrite:** do not alter discovery topic content as a side effect of UI, PWA or account work.

## Work Guidance

- Install with `npm ci`; run `npm run dev`, `npm run lint`, `npm run check` and `npm run build` as appropriate.
- Vercel builds from this directory with `npm ci` and `npm run build`, serving `dist/`. Keep the Vercel project's existing name and Root Directory unchanged.
- Keep `SITE_URL`, Supabase Authentication Site URL/Redirect URLs and Android `CURIO_AUTH_SITE_URL` aligned to the existing production Curio domain. Do not switch production email redirects until preview sign-in, sign-up, confirmation, magic-link, password reset and deletion flows pass.
- Keep the existing environment variable names and values. No environment variable migration is needed solely for this folder merge.

## Verification

- `npm run check` validates account JavaScript.
- `npm run lint` and `npm run build` validate the React/Vite app.
- Preview-check the root discovery experience, all SPA deep links, account static routes, API config, email callbacks, installability and offline behavior.
- Never claim that offline discovery is complete unless topic data and relevant assets have been cached and tested.

## Child DOX Index

No child AGENTS.md files defined yet.
