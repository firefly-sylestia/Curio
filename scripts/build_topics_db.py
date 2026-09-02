#!/usr/bin/env python3
"""Build the pre-populated Room topics database from repository JSON."""
from __future__ import annotations

import json
import sqlite3
import sys
from pathlib import Path


def build(source_dir: Path, output_db: Path) -> None:
    files = sorted(p for p in source_dir.glob("*.json") if p.name != "topic_index.json")
    if not files:
        raise RuntimeError(f"No topic JSON files found in {source_dir}")

    output_db.parent.mkdir(parents=True, exist_ok=True)
    if output_db.exists():
        output_db.unlink()

    with sqlite3.connect(output_db) as db:
        db.executescript("""
            CREATE TABLE topics (
                id TEXT NOT NULL PRIMARY KEY,
                categoryId TEXT NOT NULL,
                subtype TEXT NOT NULL,
                name TEXT NOT NULL,
                teaser TEXT NOT NULL,
                imageUrl TEXT NOT NULL DEFAULT '',
                byline TEXT NOT NULL DEFAULT '',
                tags TEXT NOT NULL DEFAULT '',
                tier INTEGER NOT NULL DEFAULT 1,
                exploreVerb TEXT NOT NULL DEFAULT '',
                exploreTargetName TEXT NOT NULL DEFAULT '',
                exploreDurationMinutes INTEGER NOT NULL DEFAULT 0,
                exploreInstruction TEXT NOT NULL DEFAULT '',
                pageCount INTEGER DEFAULT 0,
                episodeCount INTEGER DEFAULT 0,
                altPageLabel TEXT NOT NULL DEFAULT '',
                altPageCount INTEGER NOT NULL DEFAULT 0,
                synopsis TEXT NOT NULL DEFAULT '',
                chapters TEXT NOT NULL DEFAULT ''
            );
            CREATE INDEX index_topics_categoryId_name ON topics(categoryId, name);
            CREATE INDEX index_topics_name ON topics(name);
            CREATE INDEX index_topics_subtype ON topics(subtype);
        """)
        rows = []
        for path in files:
            data = json.loads(path.read_text(encoding="utf-8"))
            if not isinstance(data, list):
                raise RuntimeError(f"{path.name}: root must be a JSON array")
            for topic in data:
                if not isinstance(topic, dict):
                    raise RuntimeError(f"{path.name}: topic must be an object")
                action = topic.get("exploreAction") or {}
                tags = topic.get("tags", "")
                if isinstance(tags, list):
                    tags = json.dumps(tags, separators=(",", ":"))
                chapters = topic.get("chapters", "")
                if isinstance(chapters, (list, dict)):
                    chapters = json.dumps(chapters, separators=(",", ":"))
                rows.append((
                    topic.get("id", ""), topic.get("categoryId", ""),
                    topic.get("subtype", ""), topic.get("name", ""),
                    topic.get("teaser", ""), topic.get("imageUrl", ""),
                    topic.get("byline", ""), tags, topic.get("tier", 1),
                    action.get("verb", ""), action.get("targetName", ""),
                    action.get("durationMinutes", 0), action.get("instruction", ""),
                    topic.get("pageCount", 0), topic.get("episodeCount", 0),
                    topic.get("altPageLabel", ""), topic.get("altPageCount", 0),
                    topic.get("synopsis", ""), chapters,
                ))
        db.executemany("INSERT INTO topics VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", rows)
    print(f"topics.db: {len(rows)} topics from {len(files)} files")


if __name__ == "__main__":
    if len(sys.argv) != 3:
        raise SystemExit("usage: build_topics_db.py <source-dir> <output-db>")
    build(Path(sys.argv[1]), Path(sys.argv[2]))
