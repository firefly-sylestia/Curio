#!/usr/bin/env bash
#
# ── SURFACE 1: THE RUN SUMMARY ───────────────────────────────────────────────
#
# The one page that answers "what did this build actually do?" without reading
# a log. It derives everything from the files the build left behind, so it stays
# correct when a step is re-ordered, and it never fails the run: it is a reader.
#
# Contract:
#   build-summary.sh [signing-story]
#   → markdown on $GITHUB_STEP_SUMMARY
#
# What it reports, and why each line earns its place:
#   Version      — the numbers the APK will actually carry (single source of
#                  truth: app/build.gradle.kts's defaultConfig).
#   Variant      — that this is a throwaway universal release build and not the
#                  per-ABI set the tag workflow publishes.
#   Catalogs     — how many topic JSON files were bundled and how many topics
#                  they carry. An empty catalog is the one failure that has
#                  already shipped an APK with no data, and it is invisible in a
#                  log line count.
#   Signing      — release keystore, or the debug-key fallback (a fork PR).
#   APK          — name, size and SHA-256, so a tester can check the file they
#                  downloaded is the file this run made.
#   Lint         — the totals from the lint report.
#   Providers    — which optional keyed providers were configured, since a
#                  keyless build behaves differently from a keyed one and the
#                  only other place that is visible is a `--info` log.
set -uo pipefail

signing="${1:-unknown}"
summary="${GITHUB_STEP_SUMMARY:-/dev/stdout}"

# ── the version the APK carries ─────────────────────────────────────────────
version_code=$(grep -oE 'versionCode[[:space:]]*=[[:space:]]*[0-9]+' app/build.gradle.kts 2>/dev/null |
  grep -oE '[0-9]+' | head -1)
version_name=$(grep -oE 'envReleaseVersion \?: "[0-9.]+"' app/build.gradle.kts 2>/dev/null |
  grep -oE '[0-9]+\.[0-9]+\.[0-9]+' | head -1)

# ── the catalogs that were bundled ──────────────────────────────────────────
catalogs=0
topics=0
if [ -d app/src/main/assets/topics ]; then
  catalogs=$(find app/src/main/assets/topics -maxdepth 1 -name '*.json' | wc -l)
  topics=$(python3 - <<'PY' 2>/dev/null || echo 0
import glob, json
total = 0
for path in glob.glob("app/src/main/assets/topics/*.json"):
    with open(path, encoding="utf-8") as handle:
        data = json.load(handle)
    total += len(data if isinstance(data, list) else data.get("topics", []))
print(total)
PY
  )
fi

# ── the APK this run produced ───────────────────────────────────────────────
apk_name="—"
apk_size="—"
apk_sha="—"
shopt -s nullglob
apks=(app/build/outputs/apk/release/*.apk)
shopt -u nullglob
if [ "${#apks[@]}" -gt 0 ]; then
  apk="${apks[0]}"
  apk_name="$(basename "$apk")"
  apk_size="$(du -h "$apk" | cut -f1)"
  apk_sha="$(sha256sum "$apk" | cut -d' ' -f1)"
fi

# ── the lint totals ─────────────────────────────────────────────────────────
lint_line="no report"
report=$(ls app/build/reports/lint-results*.txt 2>/dev/null | head -1 || true)
if [ -n "$report" ]; then
  lint_line=$(grep -oE '[0-9]+ (errors?|warnings?)' "$report" | tail -2 | tr '\n' ' ' | sed 's/ $//')
  [ -n "$lint_line" ] || lint_line="clean"
fi

# ── which optional providers this build carried ─────────────────────────────
providers=""
for entry in \
  "Google Books:GOOGLE_BOOKS_API_KEY" \
  "LibraryThing:LIBRARY_THING_API_KEY" \
  "Spotify:SPOTIFY_CLIENT_ID" \
  "TMDB:TMDB_API_KEY" \
  "Supabase:SUPABASE_URL" \
  "Account site:CURIO_AUTH_SITE_URL"; do
  label="${entry%%:*}"
  var="${entry##*:}"
  if [ -n "${!var:-}" ]; then
    providers="${providers}${label}, "
  fi
done
[ -n "$providers" ] || providers="none (keyless build)"

{
  echo ""
  echo "## Curio Android CI — build summary"
  echo ""
  echo "| | |"
  echo "| --- | --- |"
  echo "| Version | ${version_name:-1.1.1} (${version_code:-unknown}) |"
  echo "| Variant | release · universal APK (ABI splits off) |"
  echo "| Topic catalogs | ${catalogs} file(s) · ${topics} topics |"
  echo "| Signing | ${signing} |"
  echo "| APK | ${apk_name} · ${apk_size} |"
  echo "| SHA-256 | \`${apk_sha}\` |"
  echo "| Lint | ${lint_line} |"
  echo "| Keyed providers | ${providers%, } |"
  echo ""
  echo "The per-file Kotlin diagnostics, the full Gradle log and the lint reports"
  echo "are on the **verify** job — the diagnostics are annotations, not log lines."
} >> "$summary"

exit 0
