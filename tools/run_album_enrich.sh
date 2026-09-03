#!/bin/bash
# Run the 5 album-enrichment batches, each writing the FULL albums.json.
# Usage: bash tools/run_album_enrich.sh
set -u
cd "$(dirname "$0")/.." || exit 1

for i in 0 1 2 3 4; do
  off=$((i * 200))
  echo "=== batch offset $off ==="
  python3 tools/enrich_albums_art_genius.py --offset "$off" --limit 200 --apply
  if [ $? -ne 0 ]; then
    echo "batch $off FAILED" >&2
    exit 1
  fi
done
echo "ALL BATCHES DONE"