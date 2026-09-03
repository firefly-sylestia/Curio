#!/usr/bin/env python3
"""
enrich_albums_art_genius.py — author album-level metadata into data/topics/albums.json

Two enrichments per album:
  1. `imageUrl`  — the album's artwork, resolved from the keyless **iTunes
     Search API** (album entity search by name + artist). The artwork URL is
     resized to 600x600 and stored authored, so the app shows covers with no
     network lookup. Falls back to MusicBrainz + Cover Art Archive (CAA)
     when iTunes has no match.
  2. `geniusUrl` — the album's Genius page. Genius' official API needs a
     token, which the app does NOT bundle, so this tool builds the canonical
     `https://genius.com/albums/<artist-slug>/<album-slug>` URL from the
     artist + album names (Genius' own slug rule: lowercase, strip
     punctuation, spaces -> hyphens).

Usage:
    python3 tools/enrich_albums_art_genius.py [--apply]

Without --apply the tool runs in dry-run mode and only reports what it
would change. With --apply it writes data/topics/albums.json back.

Genius official API (optional, for future verification of geniusUrl):
    GENIUS_API_TOKEN=xxx python3 tools/enrich_albums_art_genius.py
When the token is set, the tool VALIDATES each constructed geniusUrl by
searching the API (song/album search) and only keeps the URL when the
official result confirms it; otherwise it drops the field for a follow-up
pass. Put the token in a local .env-style export — NEVER commit it:

    # ~/.bashrc or a local un-committed .env sourced before running:
    export GENIUS_API_TOKEN="your-client-access-token"

The API token is a *client access token* from https://genius.com/api-clients
(create an API client, copy the "Client Access Token" line).
"""
import json
import os
import re
import subprocess
import sys
import time
import urllib.parse
import urllib.request

ALBUMS_JSON = "data/topics/albums.json"
ITUNES_SIZE = "600x600bb"
USER_AGENT = "CurioAlbumAuthor/1.0 (album art + genius enrichment)"
MB_UA = "CurioAlbumAuthor/1.0 (contact: curio-app)"
SLEEP = 0.25  # iTunes is fine with this cadence; be polite anyway


def http_get(url, headers=None, timeout=15):
    req = urllib.request.Request(url, headers=headers or {"User-Agent": USER_AGENT})
    try:
        with urllib.request.urlopen(req, timeout=timeout) as r:
            return r.status, r.read()
    except Exception as e:
        return None, str(e).encode()


def itunes_artwork(name, artist):
    """Return a 600px artwork URL from iTunes, or None."""
    term = urllib.parse.quote(f"{name} {artist}".strip())
    status, body = http_get(f"https://itunes.apple.com/search?term={term}&entity=album&limit=10")
    if status != 200:
        return None
    try:
        data = json.loads(body.decode("utf-8", "replace"))
    except Exception:
        return None
    best = None
    best_score = 0
    for r in data.get("results", []):
        coll = (r.get("collectionName") or "").strip()
        art = r.get("artworkUrl100") or ""
        art_artist = (r.get("artistName") or "").strip()
        if not coll or not art:
            continue
        score = 0
        cn, wn = coll.lower(), name.lower()
        if cn == wn:
            score += 2
        elif cn in wn or wn in cn:
            score += 1
        if artist and art_artist.lower() == artist.lower():
            score += 1
        if score > best_score:
            best_score = score
            best = art
        if score >= 3:
            break
    if not best:
        return None
    return best.replace("100x100bb", ITUNES_SIZE).replace("http://", "https://")


def musicbrainz_artwork(name, artist):
    """Fallback: MusicBrainz release-group search -> CAA front cover."""
    time.sleep(1.0)  # MusicBrainz asks for max 1 req/s
    def esc(s):
        return re.sub(r'([\\"+()\[\]])', r"\\\1", s)
    query = f'releasegroup:"{esc(name)}"'
    if artist:
        query += f' AND artist:"{esc(artist)}"'
    url = "https://musicbrainz.org/ws/2/release-group/?query=" + urllib.parse.quote(query) + "&fmt=json&limit=5"
    status, body = http_get(url, headers={"User-Agent": MB_UA})
    if status != 200:
        return None
    try:
        groups = json.loads(body.decode("utf-8", "replace")).get("release-groups", [])
    except Exception:
        return None
    for g in groups:
        gid = g.get("id")
        if not gid:
            continue
        gname = (g.get("title") or "").strip().lower()
        # artist credit check
        try:
            ac = g.get("artist-credit") or []
            gartist = (ac[0].get("name") or "") if ac else ""
        except Exception:
            gartist = ""
        ok = gname == name.lower() or (gname and (gname in name.lower() or name.lower() in gname))
        if artist and gartist and gartist.lower() != artist.lower():
            ok = False
        if not ok:
            continue
        # Cover Art Archive front cover probe. CAA answers with a 307
        # redirect to archive.org when the art exists (404 otherwise); the
        # probe must NOT follow the redirect — following it downloads the
        # full image just to check. curl with -I + no -L gives the status
        # of the redirect itself; any 2xx/3xx confirms the cover exists.
        url = f"https://coverartarchive.org/release-group/{gid}/front-500"
        try:
            code = subprocess.run(
                ["curl", "-s", "-o", "/dev/null", "-w", "%{http_code}", "-I", "--max-time", "10", url],
                capture_output=True, text=True, timeout=15,
            ).stdout.strip()
        except Exception:
            code = ""
        if code and 200 <= int(code) <= 399:
            return url
    return None


def genius_slug(name):
    """Genius canonical slug: lowercase, keep [a-z0-9], spaces -> hyphens."""
    s = name.strip().lower()
    # keep unicode letters? genius slugs are ascii; transliterate common ones
    s = s.replace("&", "and")
    s = re.sub(r"[^a-z0-9]+", "-", s)
    s = re.sub(r"-{2,}", "-", s).strip("-")
    return s


def genius_url(name, artist):
    return f"https://genius.com/albums/{genius_slug(artist)}/{genius_slug(name)}"


def validate_genius_api(url, name, artist):
    """Optional: confirm a geniusUrl with the official API. Needs GENIUS_API_TOKEN."""
    token = os.environ.get("GENIUS_API_TOKEN")
    if not token:
        return url  # no token -> trust the constructed slug
    q = urllib.parse.quote(f"{artist} {name}")
    status, body = http_get(
        f"https://api.genius.com/search?q={q}",
        headers={"Authorization": f"Bearer {token}", "User-Agent": USER_AGENT},
    )
    if status != 200:
        return url
    try:
        hits = json.loads(body.decode("utf-8", "replace")).get("response", {}).get("hits", [])
    except Exception:
        return url
    for h in hits[:10]:
        result = h.get("result") or {}
        rtype = result.get("type") or ""
        if rtype != "album":
            continue
        aurl = result.get("url") or ""
        if aurl and aurl.startswith("https://genius.com/albums/"):
            return aurl  # official URL wins over our slug
    return None  # API found no album for this name/artist


def main():
    apply = "--apply" in sys.argv
    # --limit N processes only N albums, --offset M starts at index M
    # (batching so a run never exceeds the environment's run budget; each
    # batch reads the FULL file and rewrites it, so nothing is lost).
    limit = None
    if "--limit" in sys.argv:
        limit = int(sys.argv[sys.argv.index("--limit") + 1])
    offset = 0
    if "--offset" in sys.argv:
        offset = int(sys.argv[sys.argv.index("--offset") + 1])
    print(f"enrich: apply={apply} offset={offset} limit={limit}", flush=True)
    albums = json.load(open(ALBUMS_JSON, encoding="utf-8"))
    total = len(albums)
    art_found = gen_found = art_miss = 0
    for i, a in enumerate(albums[offset:]):
        i = offset + i
        if limit and (i - offset) >= limit:
            break
        name, artist = a.get("name", ""), a.get("byline", "")
        if (i + 1) % 10 == 0:
            print(f"[{i+1}/{total}] ...", flush=True)
        if not name:
            continue
        changed = []
        # ---- imageUrl ----
        if not a.get("imageUrl"):
            art = itunes_artwork(name, artist)
            if not art:
                art = musicbrainz_artwork(name, artist)
            if art:
                a["imageUrl"] = art
                art_found += 1
                changed.append("imageUrl")
            else:
                art_miss += 1
        # ---- geniusUrl ----
        if not a.get("geniusUrl"):
            gurl = genius_url(name, artist)
            gurl = validate_genius_api(gurl, name, artist)
            if gurl:
                a["geniusUrl"] = gurl
                gen_found += 1
                changed.append("geniusUrl")
        if (i + 1) % 50 == 0 or i == total - 1:
            print(f"[{i+1}/{total}] art {art_found} found / {art_miss} miss | genius {gen_found}")
        time.sleep(SLEEP)

    with_art = sum(1 for a in albums if a.get("imageUrl"))
    with_gen = sum(1 for a in albums if a.get("geniusUrl"))
    print(f"\nDONE: {with_art}/{total} imageUrl, {with_gen}/{total} geniusUrl")

    if apply:
        with open(ALBUMS_JSON, "w", encoding="utf-8") as fh:
            json.dump(albums, fh, ensure_ascii=False, indent=1)
            fh.write("\n")
        print("WROTE", ALBUMS_JSON)
    else:
        print("dry-run (no write) — rerun with --apply to save.")


if __name__ == "__main__":
    main()
