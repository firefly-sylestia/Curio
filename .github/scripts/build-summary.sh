#!/usr/bin/env bash
#
# ── SURFACE 1: THE RUN SUMMARY ───────────────────────────────────────────────
#
# The one page that answers "what did this build actually do?" without reading
# a log. It derives everything from the files the build left behind, so it stays
# correct when a step is re-ordered, and it never fails the run: it is a reader.
#
# Contract:
#   build-summary.sh [signing-story] [build-outcome]
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
#
# v412 — ON FAILURE the summary is the postmortem, not a shrug: the failed
# Gradle tasks and the first compiler errors are lifted out of the log into
# the table and a block below it, so a red run answers "what broke" on the
# page everyone already reads (the Checks tab still carries every error as
# an annotation; the log is still uploaded).
set -uo pipefail

signing="${1:-unknown}"
outcome="${2:-unknown}"
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
  if [ "$outcome" = "failure" ]; then
    echo "| Build | ❌ failed |"
  elif [ "$outcome" = "success" ]; then
    echo "| Build | ✅ passed |"
  else
    echo "| Build | ${outcome} |"
  fi
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

# ── the postmortem, only when the build failed ───────────────────────────────
# The Gradle log names the failing tasks (`> Task :x:y FAILED`) and the
# compiler names its errors (`e: file://…`) — both buried in thousands of
# lines. Lift the failed tasks and the first distinct errors onto the summary
# so a red run is readable without opening anything.
if [ "$outcome" = "failure" ] && [ -f gradle-build.log ]; then
  failed_tasks=$(grep -oE '^> Task [^ ]+ FAILED' gradle-build.log | sort -u | sed 's/^> Task //; s/ FAILED$//')
  if [ -n "$failed_tasks" ]; then
    {
      echo ""
      echo "### Failed tasks"
      echo ""
      while IFS= read -r task; do
        echo "- \`${task}\`"
      done <<< "$failed_tasks"
    } >> "$summary"
  fi
  errors_block=$(grep '^e: file://' gradle-build.log | sed -E 's#^e: file://.*/app/src/#app/src/#' | sort -u | head -15)
  error_count=$(grep -c '^e: file://' gradle-build.log || true)
  if [ -n "$errors_block" ]; then
    {
      echo ""
      echo "### First compiler errors ($error_count total — every one is an annotation in the Checks tab)"
      echo ""
      echo "```"
      printf '%s\n' "$errors_block"
      echo "```"
    } >> "$summary"
  fi
fi

exit 0
