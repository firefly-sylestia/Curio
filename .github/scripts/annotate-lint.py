#!/usr/bin/env python3
"""SURFACE 2, FOR LINT: turn the lint XML into check annotations.

────────────────────────────────────────────────────────────────────────────────
WHY THIS IS A SECOND ANNOTATOR AND NOT A SECOND CALL TO annotate-gradle.sh.

`annotate-gradle.sh` reads the Kotlin compiler's `e:`/`w:` lines out of the
Gradle log. Before v466 that was enough, because lint and the compile shared one
job and one log. They no longer do: lint runs on its own runner now, and its
findings are not compiler diagnostics — they are `lint-results-*.xml`, written
by AGP, one `<issue>` per finding with its own severity and location. Feeding
that log to the Kotlin annotator would annotate the *compile* twice and the
*lint findings* never, which would make the new job's Checks tab look empty on
exactly the red run it exists to explain.

So: lint reads lint's own report.

Contract:
    annotate-lint.py [glob]   → ::error/::warning/::notice on stdout
    never exits non-zero — it is a reader, not a check.

────────────────────────────────────────────────────────────────────────────────
SEVERITY MAPPING, and why `Information` is a notice and not a warning.

AGP's severities are Fatal / Error / Warning / Information / Ignore. Only Fatal
and Error can fail the build under this project's configuration, so those two
become `::error` and only those two read as red in the Checks tab; Warning stays
`::warning` (visible, non-blocking) and Information becomes `::notice`, so the
tab does not turn yellow with stylistic suggestions that were never failures.

The cap exists because AGP happily emits thousands of findings for a big
resource change, and GitHub renders at most 10 per type per step anyway: an
uncapped loop would spend its whole budget on the first twenty and push the
server round-trips for nothing. The cap is per severity, and the number dropped
is reported so a truncated tab is never mistaken for the whole story.
"""

from __future__ import annotations

import glob
import sys
import xml.etree.ElementTree as ElementTree

DEFAULT_GLOB = "app/build/reports/lint-results*.xml"

# GitHub shows 10 annotations per type per step; ask for a little more than that
# so the interesting tail is not cut off at exactly the UI limit.
MAX_PER_SEVERITY = 25

COMMANDS = {"Fatal": "error", "Error": "error", "Warning": "warning"}


def escape_data(text: str) -> str:
    """Escape for an annotation's MESSAGE body (the part after `::`)."""
    return text.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")


def escape_property(text: str) -> str:
    """Escape for a `key=value` pair in the annotation's header."""
    return escape_data(text).replace(":", "%3A").replace(",", "%2C")


def report_path(file_attribute: str) -> str:
    """AGP writes paths relative to the MODULE dir; the workspace root is above it.

    Without this the annotation lands on `src/main/...`, which GitHub cannot
    resolve to a file in the checkout and therefore silently drops.
    """
    path = file_attribute.strip()
    if not path:
        return path
    if not path.startswith(("app/", "/")):
        path = "app/" + path
    return path


def collect(paths: list[str]) -> list[dict[str, str]]:
    findings: list[dict[str, str]] = []
    for path in paths:
        try:
            tree = ElementTree.parse(path)
        except Exception as error:  # noqa: BLE001 — a reader must never fail the run
            print(f"::warning::could not read lint report {path}: {error}")
            continue
        for issue in tree.getroot().iter("issue"):
            location = issue.find("location")
            if location is None:
                # A project-level finding (a missing baseline, a configuration
                # complaint) has no file to annotate; it still belongs in the
                # log, so it is printed rather than dropped.
                print(
                    "Lint (no file): "
                    f"{issue.get('id', 'lint')} — {issue.get('message', '')}"
                )
                continue
            severity = issue.get("severity", "Warning")
            findings.append(
                {
                    "command": COMMANDS.get(severity, "notice"),
                    "severity": severity,
                    "file": report_path(location.get("file", "")),
                    "line": location.get("line", "") or "1",
                    "column": location.get("column", ""),
                    "title": issue.get("id", "lint"),
                    "message": issue.get("message", ""),
                }
            )
    return findings


def emit(findings: list[dict[str, str]]) -> None:
    shown: dict[str, int] = {}
    dropped: dict[str, int] = {}
    for finding in findings:
        severity = finding["severity"]
        if shown.get(severity, 0) >= MAX_PER_SEVERITY:
            dropped[severity] = dropped.get(severity, 0) + 1
            continue
        shown[severity] = shown.get(severity, 0) + 1
        properties = [f"file={escape_property(finding['file'])}"]
        if finding["line"]:
            properties.append(f"line={escape_property(finding['line'])}")
        if finding["column"]:
            properties.append(f"col={escape_property(finding['column'])}")
        properties.append(f"title={escape_property(finding['title'])}")
        message = f"{finding['title']}: {finding['message']}"
        print(
            f"::{finding['command']} {','.join(properties)}"
            f"::{escape_data(message)}"
        )
    for severity, count in sorted(dropped.items()):
        print(
            f"::notice::{count} further {severity.lower()}-severity lint finding(s) "
            f"were not annotated (the cap is {MAX_PER_SEVERITY} per severity) — "
            "the full report is uploaded as an artifact."
        )


def main() -> int:
    patterns = sys.argv[1:] or [DEFAULT_GLOB]
    paths: list[str] = []
    for pattern in patterns:
        paths.extend(sorted(glob.glob(pattern)))
    if not paths:
        # Not an error: the lint runner that FAILED before lint ran has no
        # report, and there is nothing to annotate — the Gradle log's own
        # failed-task line is the finding in that case.
        print(f"No lint XML found ({', '.join(patterns)}) — nothing to annotate.")
        return 0
    findings = collect(paths)
    if not findings:
        print(f"Lint is clean in {len(paths)} report(s) — nothing to annotate.")
        return 0
    emit(findings)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
