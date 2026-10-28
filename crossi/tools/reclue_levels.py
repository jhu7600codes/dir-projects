#!/usr/bin/env python3
"""
Swaps the clues in already-frozen levels for the current dictionary's clues, without
regenerating the grids. Run after adding hand-written clues.

usage: reclue_levels.py <lang>
"""
import json
import sys
from pathlib import Path

ROOT = Path(__file__).parent.parent / "app/src/main/assets"


def main():
    lang = sys.argv[1]
    clues = {}
    for line in (ROOT / lang / "words.tsv").read_text(encoding="utf-8").splitlines():
        parts = line.split("\t")
        if len(parts) >= 4:
            clues[parts[0]] = [c for c in parts[3].split("|") if c]
    for line in (ROOT / lang / "longwords.tsv").read_text(encoding="utf-8").splitlines():
        parts = line.split("\t")
        if len(parts) >= 4 and not line.startswith("#"):
            clues[parts[0]] = [c for c in parts[3].split("|") if c]
    path = ROOT / lang / "levels.json"
    levels = json.loads(path.read_text(encoding="utf-8"))
    changed = missing = 0
    for level in levels:
        for e in level["entries"]:
            if e.get("clueKind") == "TRICK":
                continue
            options = clues.get(e["answer"])
            if not options:
                missing += 1
                continue
            if e["clue"] != options[0]:
                changed += 1
            e["clue"] = options[0]
            e["altClues"] = options[1:4]
    path.write_text(json.dumps(levels, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    print(f"{lang}: {changed} clues updated, {missing} answers missing from the dictionary")


if __name__ == "__main__":
    main()
