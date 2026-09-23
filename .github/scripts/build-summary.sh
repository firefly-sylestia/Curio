#!/usr/bin/env bash
#
# ── SURFACE 1: THE RUN SUMMARY ───────────────────────────────────────────────
#
# The one page that answers "what did this build actually do?" without reading
# a log. It derives everything from the files the build left behind, so it stays
# correct when a step is re-ordered, and it never fails the run: it is a reader.
#
# Contract:
#   build-summary.sh [signing-story] [build-outcome] [edition] [phase]
#   → markdown on $GITHUB_STEP_SUMMARY
#   → a one-line JSON row at ci-report/curio-<edition>-<phase>.json
#
# v465e — THE EDITION ARGUMENT, AND WHY THE SUMMARY GREW A MACHINE-READABLE
# HALF. Two editions now build in PARALLEL on separate runners, so each one
# reads its own APK directory and writes its own page — and there is no longer
# any single job whose step outputs the standalone report could quote. The
# report job therefore reads these JSON rows instead of `needs.*.outputs`, which
# is the only aggregation that is actually well-defined for a matrix job.
#
# v466 — THE PHASE ARGUMENT. `lint` became its own parallel job per edition
# because it is 40% of the run and accounted for NONE of the critical path: it
# ran last, after the APK was already built and signed. So there are now four
# runners, and this script is invoked by two different kinds of them. `phase`
# (build | lint) says which, and it is not decoration — the build runner has no
# lint report to quote and the lint runner has no APK, so a summary that printed
# both rows unconditionally would show "Lint: no report" on a build that simply
# never lints, which reads as a finding rather than as a shape.
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
edition="${3:-}"
phase="${4:-build}"
summary="${GITHUB_STEP_SUMMARY:-/dev/stdout}"

# ── WHICH LOG THE POSTMORTEM READS ─────────────────────────────────────────
# v466 — the two phases tee DIFFERENT filenames (`gradle-build.log` and
# `gradle-lint.log`) so that the uploaded artifacts name themselves honestly.
# The postmortem below therefore takes the name from the caller instead of
# assuming the build runner's — assuming it would leave every red lint run with
# an empty "Failed tasks" section, which is the one page a red run is read for.
log="${GRADLE_LOG:-gradle-build.log}"

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

# ── the APKs this run produced ──────────────────────────────────────────────
#
# v465 — ONE DIRECTORY PER EDITION. The `edition` flavor dimension moved these
# from `apk/release/` to `apk/<flavor>/release/`, so the glob carries the extra
# level. The table names EVERY APK (both editions build on a push) and reports
# the size and hash of the first — the core edition's universal, since `core`
# sorts before `full` — with the row saying so, because one number standing in
# for two different files is how a summary starts lying.
apk_name="—"
apk_size="—"
apk_sha="—"
# v465e — THIS RUNNER BUILT ONE EDITION, so it reads ONE directory. The
# wildcard stays as the fallback so the script still works when it is invoked
# without an edition (a local run, or any caller that has not been updated).
if [ -n "$edition" ]; then
  apk_glob="app/build/outputs/apk/${edition}/release/*.apk"
else
  apk_glob="app/build/outputs/apk/*/release/*.apk"
fi
shopt -s nullglob
apks=($apk_glob)
shopt -u nullglob
if [ "${#apks[@]}" -gt 0 ]; then
  apk="${apks[0]}"
  names=""
  for one in "${apks[@]}"; do
    if [ -n "$names" ]; then names="$names, "; fi
    names="$names$(basename "$one")"
  done
  apk_name="$names"
  # The ABI-split set and the universal APK both land here, so the size line
  # says which file it is describing rather than implying it covers the set.
  apk_size="$(du -h "$apk" | cut -f1)"
  [ "${#apks[@]}" -gt 1 ] && apk_size="${apk_size} (first of ${#apks[@]})"
  apk_sha="$(sha256sum "$apk" | cut -d' ' -f1)"
fi

# ── the lint totals ─────────────────────────────────────────────────────────
# v465e — the report name carries the VARIANT now that one runner lints one
# edition (`lint-results-coreRelease.txt`), so the glob keeps the old bare name
# as a fallback rather than dropping to "no report" on every run.
#
# v466 — ONLY THE LINT RUNNER REPORTS A LINT COUNT. On the build runner
# `lintAnalyze*` is not in the task list at all, so an empty glob here would
# print "no report" — a red-looking finding invented by the split.
lint_line="— (lint runs in its own job)"
if [ "$phase" = "lint" ]; then
  lint_line="no report"
  report=$(ls app/build/reports/lint-results*.txt 2>/dev/null | head -1 || true)
  if [ -n "$report" ]; then
    lint_line=$(grep -oE '[0-9]+ (errors?|warnings?)' "$report" | tail -2 | tr '\n' ' ' | sed 's/ $//')
    [ -n "$lint_line" ] || lint_line="clean"
  fi
fi

# ── which optional providers this build carried ─────────────────────────────
# A provider may accept MORE THAN ONE credential: TMDB takes a v3 key or an API
# Read Access Token (see `TmdbFetch`), so its row lists both vars and the label is
# printed once, whichever one this build was given.
providers=""
add_provider() {
  label="$1"; shift
  for var in "$@"; do
    if [ -n "${!var:-}" ]; then
      providers="${providers}${label}, "
      return
    fi
  done
}
add_provider "Google Books" GOOGLE_BOOKS_API_KEY
add_provider "LibraryThing" LIBRARY_THING_API_KEY
add_provider "Spotify" SPOTIFY_CLIENT_ID
add_provider "TMDB" TMDB_API_KEY TMDB_READ_TOKEN
add_provider "OMDb" OMDB_API_KEY
add_provider "Comic Vine" COMIC_VINE_API_KEY
add_provider "Supabase" SUPABASE_URL
add_provider "Account site" CURIO_AUTH_SITE_URL
[ -n "$providers" ] || providers="none (keyless build)"

{
  echo ""
  if [ -n "$edition" ]; then
    echo "## Curio Android CI — ${phase} · ${edition}"
  else
    echo "## Curio Android CI — build summary"
  fi
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
  if [ -n "$edition" ]; then
    echo "| Edition | \`${edition}\` |"
  fi
  # ⚠️ THE CATALOG COUNT IS A PROPERTY OF THE APK, NOT OF THE CHECKOUT.
  # `app/src/main/assets/topics` holds only SCHEMA.md in git: the JSONs are
  # COPIED IN by the build job's "Bundle the topic catalogs" step, which is why
  # that step also `rm -rf`s the directory first. A lint runner does not run it,
  # so counting here would report `0 file(s) · 0 topics` — which is not a
  # cosmetic zero but the EXACT failure the bundling step was written to catch
  # (v412: an APK shipped with an empty catalog). Inventing that finding on the
  # runner that never packages anything is the one lie this script must not
  # tell, so a non-build phase says where the number comes from instead.
  if [ "$phase" = "build" ]; then
    echo "| Topic catalogs | ${catalogs} file(s) · ${topics} topics |"
  else
    echo "| Topic catalogs | — (bundled by the build runners) |"
  fi
  # `Lint` is printed for BOTH phases — it is the one row that means something
  # on either kind of runner, and on a build runner it now says outright that
  # lint happens elsewhere (rather than the "no report" an empty glob implies).
  echo "| Lint | ${lint_line} |"
  if [ "$phase" = "build" ]; then
    echo "| Variant | release · universal APK (ABI splits off) |"
    echo "| Signing | ${signing} |"
    echo "| APK | ${apk_name} · ${apk_size} |"
    echo "| SHA-256 | \`${apk_sha}\` |"
    # Only the runners that actually BUILD carried the keys, so only they can
    # answer this. A lint runner has no provider secrets in its environment at
    # all, and printing "none (keyless build)" there would describe a keyless
    # APK that this runner never produced.
    echo "| Keyed providers | ${providers%, } |"
  fi
  echo ""
  if [ -n "$edition" ]; then
    echo "The per-file Kotlin diagnostics are annotations on this job's own"
    echo "**Checks** entry — the \`${edition}\` · ${phase} job — and its artifacts"
    echo "carry the Gradle log."
  else
    echo "The per-file Kotlin diagnostics, the full Gradle log and the lint reports"
    echo "are on the building job — the diagnostics are annotations, not log lines."
  fi
} >> "$summary"

# ── v465e — THE MACHINE-READABLE ROW, for the report job ────────────────────
#
# The report used to be a table of `needs.verify.outputs.*`. A MATRIX JOB HAS NO
# SUCH THING: `needs.<matrixed job>.outputs` is an aggregation whose winner is
# not specified, so a roll-up built on it would silently report one edition and
# look like it had reported both. Each edition therefore leaves its own row on
# disk, uploads it as an artifact, and the report job merges them.
#
# Written even when the job failed, because "which edition broke" is the first
# question a red run has to answer. Best-effort: a writer that throws must never
# be what fails the build.
if [ -n "$edition" ]; then
  mkdir -p ci-report 2>/dev/null || true
  json_escape() { printf '%s' "$1" | sed 's/\\/\\\\/g; s/"/\\"/g' | tr -d '\n'; }
  # A lint runner assembles nothing, so these three are BLANKED rather than
  # whatever the globs happened to find — an APK sitting in this working tree
  # would otherwise be reported as this job's output, and "Signing: not
  # applicable (lint runner)" next to a real SHA-256 is a row that contradicts
  # itself. (run-report.py blanks them again for display; this is so the FILE is
  # honest too, since it is the artifact of record.)
  if [ "$phase" = "build" ]; then
    row_signing="$signing"
    row_apk="${apk_name} · ${apk_size}"
    row_sha="$apk_sha"
    row_providers="${providers%, }"
    row_catalogs="${catalogs} file(s) · ${topics} topics"
  else
    row_signing="not applicable (no APK on this runner)"
    row_apk="—"
    row_sha="—"
    row_providers="— (keys are read by the build runners)"
    # Same reason as the table row above: this runner never copied the catalogs
    # into `assets/`, so its count is 0 by construction and not a finding.
    row_catalogs="— (bundled by the build runners)"
  fi
  cat > "ci-report/curio-${edition}-${phase}.json" <<EOF
{
  "edition": "$(json_escape "$edition")",
  "phase": "$(json_escape "$phase")",
  "outcome": "$(json_escape "$outcome")",
  "version": "$(json_escape "${version_name:-unknown} (${version_code:-unknown})")",
  "catalogs": "$(json_escape "$row_catalogs")",
  "signing": "$(json_escape "$row_signing")",
  "apk": "$(json_escape "$row_apk")",
  "sha": "$(json_escape "$row_sha")",
  "lint": "$(json_escape "$lint_line")",
  "providers": "$(json_escape "$row_providers")"
}
EOF
fi

# ── the postmortem, only when the build failed ───────────────────────────────
# The Gradle log names the failing tasks (`> Task :x:y FAILED`) and the
# compiler names its errors (`e: file://…`) — both buried in thousands of
# lines. Lift the failed tasks and the first distinct errors onto the summary
# so a red run is readable without opening anything.
if [ "$outcome" = "failure" ] && [ -f "$log" ]; then
  failed_tasks=$(grep -oE '^> Task [^ ]+ FAILED' "$log" | sort -u | sed 's/^> Task //; s/ FAILED$//')
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
  errors_block=$(grep '^e: file://' "$log" | sed -E 's#^e: file://.*/app/src/#app/src/#' | sort -u | head -15)
  error_count=$(grep -c '^e: file://' "$log" || true)
  if [ -n "$errors_block" ]; then
    {
      echo ""
      echo "### First compiler errors ($error_count total — every one is an annotation in the Checks tab)"
      echo ""
      # ⚠️ SINGLE QUOTES, AND THEY ARE NOT A STYLE CHOICE. Inside DOUBLE quotes
      # bash reads a backtick as COMMAND SUBSTITUTION, so `echo "```"` is not a
      # markdown fence: the first two backticks are an empty substitution, the
      # third OPENS one, and the closing fence further down closes it — meaning
      # every line in between was EXECUTED. In this block that meant bash trying
      # to run `printf '%s\n' "$errors_block"` as a command named after the
      # first compiler error, which is why the section printed its heading and
      # then nothing: the errors never made it onto the page. (It also meant a
      # `$(…)` or `$VAR` inside a compiler message was evaluated.)
      echo '```'
      printf '%s\n' "$errors_block"
      echo '```'
    } >> "$summary"
  fi
fi

exit 0
