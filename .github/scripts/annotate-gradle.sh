#!/usr/bin/env bash
#
# ── SURFACE 2: THE CHECKS TAB ────────────────────────────────────────────────
#
# Kotlin writes its diagnostics as `e: file:///abs/path.kt:LINE:COL message` on
# stdout, where nobody reads them: on this repo the release build alone emits
# ~260 of them, so the one line that matters is 12,000 lines into a log. This
# script re-emits every one of them as a GitHub workflow command, which turns
# each into a real ANNOTATION on the pull request's file view — a clickable row
# naming its file, line and column — and counts them so the run summary and the
# report job can say "262 warnings, 0 errors" without re-reading the log.
#
# Contract:
#   annotate-gradle.sh [log-path]      (default: gradle-build.log)
#   → annotations on stdout (workflow commands)
#   → `warnings` and `errors` on $GITHUB_OUTPUT when it is set
#   → a "diagnostics" block on $GITHUB_STEP_SUMMARY when it is set
#
# It is a READER of the log, never a check: a missing log or an empty run exits
# 0, so a step that failed before Gradle ever ran still reports cleanly.
#
# Limits: a run with hundreds of warnings would flood the Checks tab, so only the
# first CURIO_ANNOTATION_LIMIT (default 400) are annotated — the counts and the
# per-file table are always complete, which is what the summary needs.
set -uo pipefail

log="${1:-gradle-build.log}"
limit="${CURIO_ANNOTATION_LIMIT:-400}"

warnings=0
errors=0
emitted=0
files="$(mktemp)"
trap 'rm -f "$files"' EXIT

# The workflow-command escaping rules for a message body.
escape() {
  local value="$1"
  value="${value//%/%25}"
  value="${value//$'\r'/%0D}"
  value="${value//$'\n'/%0A}"
  printf '%s' "$value"
}

if [ -f "$log" ]; then
  while IFS= read -r line; do
    # ── ⚠️ v465e — THE PREFIX IS STRIPPED BY NAME, NEVER WITH `#*: ` ────────
    #
    # This read `rest="${line#*: }"`, which is GREEDY: it removes the longest
    # prefix ending in a colon-space, and a diagnostic like
    # `e: file:///…/File.kt:902:36 Unresolved reference 'Context'.` has TWO
    # (`e: ` at the front, `:36 ` in the middle). So `rest` came out as
    # `Unresolved reference 'Context'.`, `path` became `Unresolved`, `lineno`
    # became `reference` — non-numeric, so the guard below `continue`d, and the
    # split returned nothing. **SURFACE 2 HAS THEREFORE BEEN SILENTLY
    # ANNOTATING NOTHING**, which is why a 3-minute red run showed an empty
    # Checks tab and every compiler error had to be dug out of the uploaded log
    # by hand (most recently the v465e `Unresolved reference 'Context'`, two
    # lines, both editions red).
    #
    # The prefix is a known, FIXED-LENGTH literal, so it is dropped by LENGTH.
    # That is deliberate rather than `#"e: file://"`: a quoted pattern inside a
    # parameter expansion is easy to over-escape (it landed as `#\"e: …\"`, whose
    # pattern contains literal backslashes and matches nothing — the same class
    # of silent no-op this whole fix is about), and `"e: file://"` and
    # `"w: file://"` are both exactly 10 characters, so one number serves both.
    case "$line" in
      "e: file://"*) severity=error; rest="${line:10}" ;;
      "w: file://"*) severity=warning; rest="${line:10}" ;;
      *) continue ;;
    esac

    # rest: /home/runner/work/Curio/Curio/app/src/…/File.kt:275:58 'msg'
    path="${rest%%:*}"
    after="${rest#*:}"           # LINE:COL message
    lineno="${after%%:*}"
    after="${after#*:}"          # COL message
    col="${after%% *}"
    message="${after#* }"

    # A diagnostic always names a real line and column; anything else is a
    # differently-shaped line and not worth an annotation.
    case "$lineno" in
      ''|*[!0-9]*) continue ;;
    esac
    case "$col" in
      ''|*[!0-9]*) col=1 ;;
    esac

    # GitHub wants a repository-relative path for the annotation to land on the
    # file rather than nowhere.
    rel="${path#"${GITHUB_WORKSPACE:-}"/}"

    if [ "$severity" = error ]; then
      errors=$((errors + 1))
    else
      warnings=$((warnings + 1))
      printf '%s\n' "$rel" >> "$files"
    fi

    if [ "$emitted" -lt "$limit" ]; then
      printf '::%s file=%s,line=%s,col=%s::%s\n' \
        "$severity" "$rel" "$lineno" "$col" "$(escape "$message")"
      emitted=$((emitted + 1))
    fi
  done < "$log"
fi

if [ "$emitted" -ge "$limit" ]; then
  echo "::notice::Showing the first ${limit} diagnostics; the full set is in the uploaded Gradle log."
fi

if [ -n "${GITHUB_OUTPUT:-}" ]; then
  {
    echo "warnings=${warnings}"
    echo "errors=${errors}"
  } >> "$GITHUB_OUTPUT"
fi

if [ -n "${GITHUB_STEP_SUMMARY:-}" ]; then
  {
    echo ""
    echo "### Compiler diagnostics"
    echo ""
    echo "**${errors}** error(s) · **${warnings}** warning(s) — every one is an"
    echo "annotation in this run's **Checks** tab."
    if [ "$warnings" -gt 0 ]; then
      echo ""
      echo "| Noisiest files | Warnings |"
      echo "| --- | --- |"
      sort "$files" | uniq -c | sort -rn | head -10 |
        while read -r count file; do
          echo "| \`${file}\` | ${count} |"
        done
    fi
  } >> "$GITHUB_STEP_SUMMARY"
fi

exit 0
