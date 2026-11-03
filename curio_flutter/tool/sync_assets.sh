#!/usr/bin/env bash
# Bundle the canonical Curio catalog into this app's assets.
#
# The topic JSON lives ONCE, at the repo root (`data/topics/*.json`), and every
# app copies it at build time — the Android CI does the same (`cp data/topics/*.json
# app/src/main/assets/topics/`, see .github/workflows/android.yml). Nothing is
# duplicated in git, so a content edit reaches the phone and this app together.
#
# Run this before `flutter run` / `flutter build` (CI runs it too):
#   ./tool/sync_assets.sh
set -euo pipefail

here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
root="$(cd "$here/../.." && pwd)"
src="$root/data/topics"
dst="$here/../assets/topics"

mkdir -p "$dst"
rm -f "$dst"/*.json
cp "$src"/*.json "$dst"/

count=$(ls -1 "$dst"/*.json 2>/dev/null | wc -l | tr -d ' ')
if [ "$count" -eq 0 ]; then
  echo "sync_assets: NO topic JSON found in $src" >&2
  exit 1
fi
echo "sync_assets: bundled $count topic files into assets/topics"
