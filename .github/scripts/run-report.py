#!/usr/bin/env python3
"""SURFACE 3 + 4: THE RUN REPORT — one row per edition.

────────────────────────────────────────────────────────────────────────────────
v465e — WHY THIS EXISTS AS A SCRIPT AND NOT AS A TABLE OF `needs.*.outputs`.

The report used to quote `needs.verify.outputs.*` directly in the workflow. That
worked while `verify` was ONE job building both editions. It is now a MATRIX —
one parallel job per edition — and **`needs.<matrixed job>.outputs` is an
aggregation whose winner GitHub does not specify**. A roll-up built on it would
print one edition's numbers under a heading that implied both had been reported,
which is worse than printing nothing: the whole point of this check is to answer
"did BOTH editions build?".

So each edition writes its own row to `ci-report/curio-<edition>.json` (from
`.github/scripts/build-summary.sh`), uploads it as an artifact, and this script
merges whatever it finds. **One row per edition by construction, not by luck** —
and if an edition's artifact is MISSING, that is reported as missing rather than
quietly dropped, because a run where one edition never got as far as writing its
row is exactly the run that needs saying so.

Contract:
    run-report.py                → markdown on $GITHUB_STEP_SUMMARY
    run-report.py --pr-body      → markdown on stdout, for the PR comment
"""

from __future__ import annotations

import glob
import json
import os
import sys

# The order editions are reported in, and the order they are EXPECTED in. A
# missing one is named; an unexpected one is still shown (a third flavor must
# not be invisible just because this list has not learned about it yet).
EXPECTED = ["core", "full"]

ROWS = ["outcome", "version", "catalogs", "lint", "signing", "apk", "sha"]


def load_rows(root: str = "ci-report") -> dict[str, dict]:
    """Every row on disk, keyed by edition. A malformed file is skipped LOUDLY."""
    rows: dict[str, dict] = {}
    for path in sorted(glob.glob(os.path.join(root, "**", "curio-*.json"), recursive=True)):
        try:
            with open(path, encoding="utf-8") as handle:
                data = json.load(handle)
        except Exception as error:  # noqa: BLE001 — a reader must never fail the run
            print(f"⚠️ could not read {path}: {error}", file=sys.stderr)
            continue
        edition = str(data.get("edition") or "").strip()
        if edition:
            rows[edition] = data
    return rows


def verdict(outcome: str) -> str:
    return {"success": "✅ passed", "failure": "❌ failed"}.get(outcome, f"⚠️ {outcome or 'unknown'}")


def edition_row(rows: dict[str, dict], edition: str) -> dict[str, str]:
    """One edition's cell values — or an explicit 'missing' marker."""
    row = rows.get(edition)
    if row is None:
        # The artifact never appeared: the job died before its summary step
        # wrote one, which is a real state and not the same as a failed build.
        return {key: "—" for key in ROWS} | {
            "outcome": "❌ no report (the job did not reach its summary step)"
        }
    return {key: str(row.get(key, "—")) for key in ROWS}


def render(rows: dict[str, dict], pr_body: bool) -> str:
    editions = EXPECTED + [name for name in sorted(rows) if name not in EXPECTED]
    out: list[str] = []
    if pr_body:
        out.append("<!-- curio-ci-report -->")
    out.append("## Curio Android CI")
    out.append("")
    if not rows:
        # Worth its own sentence: an empty report means BOTH artifacts are
        # missing, which points at the workflow rather than at either edition.
        out.append("**No per-edition report was produced.** Neither edition reached its")
        out.append("summary step — check the run's startup steps before the build.")
        return "\n".join(out) + "\n"

    out.append("| | " + " | ".join(editions) + " |")
    out.append("| --- | " + " | ".join("---" for _ in editions) + " |")

    cells = {edition: edition_row(rows, edition) for edition in editions}
    labels = {
        "outcome": "Build",
        "version": "Version",
        "catalogs": "Topic catalogs",
        "lint": "Lint",
        "signing": "Signing",
        "apk": "APK",
        "sha": "SHA-256",
    }
    for key in ROWS:
        values = []
        for edition in editions:
            value = cells[edition][key]
            if key == "outcome":
                # A missing row already carries its own sentence in `outcome`.
                value = value if value.startswith("❌ no report") else verdict(cells[edition]["outcome"])
            values.append(f"`{value}`" if key == "sha" else value)
        out.append(f"| {labels[key]} | " + " | ".join(values) + " |")

    out.append("")
    out.append(
        "Each edition builds, lints and verifies its own APK on its **own runner** — "
        "the per-file Kotlin diagnostics are annotations on that edition's **Checks** "
        "entry, and its Gradle log and lint reports are its artifacts."
    )
    missing = [e for e in editions if e not in rows]
    if missing:
        out.append("")
        out.append(
            "⚠️ No report artifact for: "
            + ", ".join(f"`{e}`" for e in missing)
            + " — that edition's job did not reach its summary step."
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
