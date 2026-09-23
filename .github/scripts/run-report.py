#!/usr/bin/env python3
"""SURFACE 3 + 4: THE RUN REPORT — one column per (edition · phase).

────────────────────────────────────────────────────────────────────────────────
v465e — WHY THIS EXISTS AS A SCRIPT AND NOT AS A TABLE OF `needs.*.outputs`.

The report used to quote `needs.verify.outputs.*` directly in the workflow. That
worked while `verify` was ONE job building both editions. It is now a MATRIX —
one parallel job per edition — and **`needs.<matrixed job>.outputs` is an
aggregation whose winner GitHub does not specify**. A roll-up built on it would
print one edition's numbers under a heading that implied both had been reported,
which is worse than printing nothing: the whole point of this check is to answer
"did BOTH editions build?".

So each edition writes its own row to `ci-report/curio-<edition>-<phase>.json`
(from `.github/scripts/build-summary.sh`), uploads it as an artifact, and this
script merges whatever it finds. **One row per job by construction, not by
luck** — and if a job's artifact is MISSING, that is reported as missing rather
than quietly dropped, because a run where a job never got as far as writing its
row is exactly the run that needs saying so.

────────────────────────────────────────────────────────────────────────────────
v466 — THE PHASE AXIS, AND WHY LINT GOT ITS OWN RUNNER.

Measured from a green run's log: the job wall was 1056s and 1007s of it was one
Gradle invocation, inside which `lintReportCoreRelease` was ~400s, R8 ~362s and
the Kotlin compile ~155s. Lint was **40% of the run and none of the critical
path** — it ran last, after the APK had already been built and signed, purely as
tail. So `lint` is now its own parallel job per edition and the critical path is
`max(build, lint)` instead of `build + lint`, which is roughly 10 minutes
against 17.6.

The consequence for THIS script is that there are four rows now, not two — and
the four are not interchangeable:

  * a **build** row has an APK, a signature and a topic count;
  * a **lint** row has neither, because that runner never assembles anything;
  * and the lint COUNT lives only on the lint row, so a reader who looked at
    the build column alone would see "— (lint runs in its own job)" where a
    number used to be.

That is why the columns carry the phase in their heading rather than the table
pretending to be two editions: showing a lint column under a heading that said
just "core" would invite exactly the misreading this split has to avoid.

Contract:
    run-report.py                → markdown on $GITHUB_STEP_SUMMARY
    run-report.py --pr-body      → markdown on stdout, for the PR comment
"""

from __future__ import annotations

import glob
import json
import os
import sys

# The order columns are reported in, and the order they are EXPECTED in. A
# missing one is named; an unexpected one is still shown (a third flavor must
# not be invisible just because this list has not learned about it yet).
EXPECTED_EDITIONS = ["core", "full"]
EXPECTED_PHASES = ["build", "lint"]

# Rows whose value only a build runner can have. A lint column shows "—" here,
# which is a fact about the shape of the pipeline and not a missing report.
BUILD_ONLY = ["signing", "apk", "sha"]

ROWS = ["outcome", "version", "catalogs", "lint", "signing", "apk", "sha"]

LABELS = {
    "outcome": "Result",
    "version": "Version",
    "catalogs": "Topic catalogs",
    "lint": "Lint",
    "signing": "Signing",
    "apk": "APK",
    "sha": "SHA-256",
}

MISSING = object()


def load_rows(root: str = "ci-report") -> dict[tuple[str, str], dict]:
    """Every row on disk, keyed by (edition, phase). A bad file is skipped LOUDLY."""
    rows: dict[tuple[str, str], dict] = {}
    for path in sorted(glob.glob(os.path.join(root, "**", "curio-*.json"), recursive=True)):
        try:
            with open(path, encoding="utf-8") as handle:
                data = json.load(handle)
        except Exception as error:  # noqa: BLE001 — a reader must never fail the run
            print(f"⚠️ could not read {path}: {error}", file=sys.stderr)
            continue
        edition = str(data.get("edition") or "").strip()
        # A row written before the phase split carries no `phase`, and its only
        # possible meaning is "this job built the APK" — so it reads as `build`
        # rather than being dropped as malformed.
        phase = str(data.get("phase") or "build").strip()
        if edition:
            rows[(edition, phase)] = data
    return rows


def verdict(outcome: str) -> str:
    return {"success": "✅ passed", "failure": "❌ failed"}.get(
        outcome, f"⚠️ {outcome or 'unknown'}"
    )


def column(rows: dict[tuple[str, str], dict], edition: str, phase: str) -> dict[str, str]:
    """One (edition, phase) column's cells — or an explicit 'missing' marker."""
    row = rows.get((edition, phase))
    if row is None:
        # The artifact never appeared: the job died before its summary step
        # wrote one, which is a real state and not the same as a failed build.
        return {key: "—" for key in ROWS} | {
            "outcome": "❌ no report (the job did not reach its summary step)"
        }
    cells = {key: str(row.get(key, "—")) for key in ROWS}
    if phase != "build":
        # The build-summary script only fills these on a build runner; say so
        # rather than printing the script's own em-dash as if it were a value.
        for key in BUILD_ONLY:
            cells[key] = "—"
    return cells


def ordered_columns(rows: dict[tuple[str, str], dict]) -> list[tuple[str, str]]:
    editions = EXPECTED_EDITIONS + sorted(
        {edition for edition, _ in rows if edition not in EXPECTED_EDITIONS}
    )
    phases = EXPECTED_PHASES + sorted(
        {phase for _, phase in rows if phase not in EXPECTED_PHASES}
    )
    return [(edition, phase) for edition in editions for phase in phases]


def render(rows: dict[tuple[str, str], dict], pr_body: bool) -> str:
    columns = ordered_columns(rows)
    out: list[str] = []
    if pr_body:
        out.append("<!-- curio-ci-report -->")
    out.append("## Curio Android CI")
    out.append("")
    if not rows:
        # Worth its own sentence: an empty report means EVERY row is missing,
        # which points at the workflow rather than at any one edition.
        out.append("**No report was produced.** No job reached its summary step —")
        out.append("check the run's startup steps before the build.")
        return "\n".join(out) + "\n"

    out.append("| | " + " | ".join(f"{e} · {p}" for e, p in columns) + " |")
    out.append("| --- | " + " | ".join("---" for _ in columns) + " |")

    cells = {key: column(rows, *key) for key in columns}
    for field in ROWS:
        values = []
        for key in columns:
            value = cells[key][field]
            if field == "outcome":
                # A missing column already carries its own sentence in `outcome`.
                value = (
                    value
                    if value.startswith("❌ no report")
                    else verdict(cells[key]["outcome"])
                )
            values.append(f"`{value}`" if field == "sha" else value)
        out.append(f"| {LABELS[field]} | " + " | ".join(values) + " |")

    out.append("")
    out.append(
        "Each edition builds and verifies its own APK on its **own runner**, and "
        "lints on a **second** runner beside it — lint is 40% of the work but was "
        "none of the critical path, so splitting it takes the run from ~17.6 "
        "minutes to ~10. The per-file Kotlin diagnostics are annotations on the "
        "job that hit them, and each job's Gradle log and reports are its "
        "artifacts."
    )
    missing = [key for key in columns if key not in rows]
    if missing:
        out.append("")
        out.append(
            "⚠️ No report artifact for: "
            + ", ".join(f"`{edition} · {phase}`" for edition, phase in missing)
            + " — that job did not reach its summary step."
        )
    return "\n".join(out) + "\n"


def main() -> int:
    pr_body = "--pr-body" in sys.argv[1:]
    text = render(load_rows(), pr_body)
    if pr_body:
        sys.stdout.write(text)
        return 0
    # Not a no-op failure: an unwritable summary path must not fail the run.
    destination = os.environ.get("GITHUB_STEP_SUMMARY")
    if not destination:
        sys.stdout.write(text)
        return 0
    try:
        with open(destination, "a", encoding="utf-8") as handle:
            handle.write("\n" + text)
    except OSError as error:
        print(f"could not write the summary: {error}", file=sys.stderr)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
