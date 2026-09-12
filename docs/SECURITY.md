# Security Policy

## Supported versions

Curio is distributed as a signed APK from [GitHub Releases](https://github.com/firefly-sylestia/Curio/releases). Only the **latest published release** receives security fixes — always update to the newest version.

## Reporting a vulnerability

Please **do not** open a public issue for a security vulnerability until it has been addressed.

- **Preferred:** use GitHub's **private vulnerability reporting** (repo **Security → Report a vulnerability**), if it is enabled on this repository.
- **Alternative:** open a [GitHub Issue](https://github.com/firefly-sylestia/Curio/issues) with **`[security]`** in the title, and let the maintainer triage privately from there.

Please include:

- The app version (Profile → Support & diagnostics → Version) and Android version.
- A clear description of the issue and, if possible, steps to reproduce.
- Whether the issue affects **on-device data** (your captures, recordings, photos and the cached social data — all in app-private storage and in backups) or the **online layer** (an account, a profile, a card, a reply, a conversation, a friend request).

## Scope

Curio is **offline-first**: your library — captures, recordings, screenshots, drafts, collections — lives on the device and is never uploaded. That half of the app has no server to attack.

Curio also has an **optional online layer** (Community, Friends, Messages), which is off until you sign in and switch Online mode on. When it is on, this is what leaves the device:

- Your Curio account's display name, `@username`, portrait number and bio.
- Cards you post (a topic, its words, a caption), your replies, and your reactions/likes.
- Direct messages to your friends, and friend requests.
- A last-active timestamp, **only** if you have not hidden your activity.

Your captures, audio, images and screenshots are never part of it, and no card can carry media.

**Security boundary.** Access is enforced in the database by **row-level security** on every table (`supabase/schema.sql` is the source of truth) — only the two people in a conversation can read it, only the friends a card is visible to can read it, blocks are honoured in both directions, and a card's replies disappear with it after its 24-hour life. The session token on the device is sealed with the **Android Keystore** (AES-GCM) rather than stored in readable preferences; signing out clears the token and the cached social data.

**Not end-to-end encrypted.** Messages, replies and cards are stored on Curio's server in plain text so they can be delivered; the server (and whoever operates the Supabase project) can read them. They are private *by policy*, not by cryptography. Do not put content in a message that must stay unreadable to the service.

**In scope here:** row-level-security gaps, a way to read another member's data, auth/session handling on the client, the token store, or anything that leaks a capture/recording off the device.

**Out of scope:** mistakes in the bundled topic content (factual errors in descriptions belong in the issues tracker), and the fact that a server can read what a server stores (see above — that is a documented, deliberate limitation, not a vulnerability).
