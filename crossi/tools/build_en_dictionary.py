#!/usr/bin/env python3
"""
Builds app/src/main/assets/en/words.tsv from:
  - SCOWL (word list + commonness levels)       http://wordlist.aspell.net  (permissive, see SCOWL-COPYRIGHT)
  - Open English WordNet 2024 (clues)           CC BY 4.0, https://github.com/globalwordnet/english-wordnet
  - tools/en_clues_handwritten.tsv              hand-written clues, these win over generated ones
  - tools/en_blocklist.txt                      words that never go in a grid

usage: build_en_dictionary.py <scowl-final-dir> <english-wordnet.xml> <out.tsv>

output line: WORD \t commonness(1-5) \t hardness(1-3) \t clue1|clue2|clue3
"""
import re
import sys
import xml.etree.ElementTree as ET
from collections import defaultdict
from pathlib import Path

HERE = Path(__file__).parent
LEVELS = {10: (5, 1), 20: (4, 1), 35: (3, 2), 40: (2, 3), 50: (1, 3)}
OFFENSIVE = re.compile(r"offensive|vulgar|obscen|derogator|disparag|\bslur\b|sexual|genital|copulat|"
                       r"intercourse|penis|vagina|testicle|feces|excrement|urinat|prostitut|"
                       r"\bslang\b|racist|narcotic|masturbat|erotic|pornogra", re.I)


def read_scowl(final_dir):
    level_of = {}
    for lv in sorted(LEVELS):
        for name in ("english-words", "american-words"):
            for raw in (Path(final_dir) / f"{name}.{lv}").read_bytes().decode("latin1").splitlines():
                w = raw.strip()
                if re.fullmatch(r"[a-z]{3,15}", w) and w not in level_of:
                    level_of[w] = lv
    # levels 40/50 only for short words, they help fill without dragging in obscure long words
    return {w: lv for w, lv in level_of.items() if lv <= 35 or len(w) <= 5}


def read_wordnet(path):
    synsets = {}          # id -> (pos, definition, [member lemmas])
    senses = defaultdict(list)  # lemma -> [(pos, synset id)] in wordnet order
    irregular = {}        # form -> lemma
    entry_lemma = {}
    tree = ET.iterparse(path, events=("end",))
    for _, el in tree:
        if el.tag == "LexicalEntry":
            lemma_el = el.find("Lemma")
            lemma, pos = lemma_el.get("writtenForm"), lemma_el.get("partOfSpeech")
            entry_lemma[el.get("id")] = lemma
            for s in el.findall("Sense"):
                senses[lemma].append((pos, s.get("synset")))
            for f in el.findall("Form"):
                irregular.setdefault(f.get("writtenForm"), lemma)
            el.clear()
        elif el.tag == "Synset":
            d = el.find("Definition")
            synsets[el.get("id")] = (el.get("partOfSpeech"), d.text if d is not None else "", el.get("members", "").split())
            el.clear()
    for sid, (pos, d, members) in synsets.items():
        synsets[sid] = (pos, d, [entry_lemma.get(m, "") for m in members])
    return synsets, senses, irregular


# inflection kinds: (kind, base) guesses for a surface word
def lemmatize(word, senses, irregular):
    out = []
    if word in senses:
        out.append(("base", word))
    if word in irregular:
        out.append(("irregular", irregular[word]))
    rules = [
        ("plural", r"(.+[^s])s$", r"\1"), ("plural", r"(.+(?:s|x|z|ch|sh))es$", r"\1"), ("plural", r"(.+[^aeiou])ies$", r"\1y"),
        ("past", r"(.+e)d$", r"\1"), ("past", r"(.+)ed$", r"\1"), ("past", r"(.+[^aeiou])ied$", r"\1y"), ("past", r"(.+?)([bdgklmnprt])\2ed$", r"\1\2"),
        ("ing", r"(.+)ing$", r"\1"), ("ing", r"(.+)ing$", r"\1e"), ("ing", r"(.+?)([bdgklmnprt])\2ing$", r"\1\2"),
        ("er", r"(.+)er$", r"\1"), ("er", r"(.+e)r$", r"\1"), ("er", r"(.+)ier$", r"\1y"), ("er", r"(.+?)([bdgklmnprt])\2er$", r"\1\2"),
        ("est", r"(.+)est$", r"\1"), ("est", r"(.+e)st$", r"\1"), ("est", r"(.+)iest$", r"\1y"), ("est", r"(.+?)([bdgklmnprt])\2est$", r"\1\2"),
    ]
    for kind, pat, rep in rules:
        m = re.fullmatch(pat, word)
        if m:
            base = m.expand(rep)
            if base in senses and len(base) >= 2:
                out.append((kind, base))
    return out


def inflect(base, kind, wordset):
    """Applies the same inflection to a synonym, only if the result is a real common word."""
    if kind == "base":
        return base if base in wordset else None
    cands = {
        "plural": [base + "s", base + "es", base[:-1] + "ies"],
        "past": [base + "ed", base + "d", base[:-1] + "ied", base + base[-1] + "ed"],
        "ing": [base + "ing", base[:-1] + "ing", base + base[-1] + "ing"],
        "er": [base + "er", base + "r", base[:-1] + "ier", base + base[-1] + "er"],
        "est": [base + "est", base + "st", base[:-1] + "iest", base + base[-1] + "est"],
    }.get(kind, [])
    for c in cands:
        if c in wordset:
            return c
    return None


wordset_level = {}
POS_FOR_KIND = {"plural": {"n"}, "past": {"v"}, "ing": {"v"}, "er": {"a", "s"}, "est": {"a", "s"}}
TAG = {"plural": " (pl.)", "past": " (past)", "ing": " (-ing)", "er": " (more so)", "est": " (most so)"}


def stem_clash(clue, word, base):
    low = clue.lower()
    for w in {word, base}:
        if w in low or (len(w) >= 5 and w[:5] in low):
            return True
    return False


def clean_def(d):
    d = re.sub(r"\([^)]*\)", "", d).split(";")[0].strip(" ,.")
    d = re.sub(r"\s+", " ", d)
    if not d or len(d) > 70:
        return None
    return d[0].upper() + d[1:]


def clues_for(word, senses, synsets, irregular, wordset):
    syn_clues, def_clues = [], []
    for kind, base in lemmatize(word, senses, irregular):
        kind_pos = POS_FOR_KIND.get(kind)
        for pos, sid in senses[base][:3]:
            if kind_pos and pos not in kind_pos:
                continue
            spos, definition, members = synsets[sid]
            if OFFENSIVE.search(definition):
                continue
            tag = TAG.get(kind, "")
            if kind == "irregular":
                # irregular forms (ate, mice, ...) can't have synonyms inflected the same way
                tag = " (past)" if pos == "v" else " (pl.)" if pos == "n" else ""
            else:
                for m in members:
                    if m == base or " " in m or "-" in m or not m.islower():
                        continue
                    s = inflect(m, kind, wordset)
                    # only well known words make good synonym clues
                    if s and s != word and wordset_level.get(s, 99) <= 20 and not stem_clash(s, word, base):
                        syn_clues.append(s.capitalize())
            c = clean_def(definition)
            if c and not stem_clash(c, word, base):
                def_clues.append(c + tag)
        if syn_clues or def_clues:
            break
    # first clue = plainest (first sense definition), easy puzzles use it
    picked = []
    for c in def_clues[:1] + syn_clues[:2] + def_clues[1:2]:
        if c not in picked:
            picked.append(c)
    return picked[:3]


def main():
    scowl_dir, wn_path, out_path = sys.argv[1:4]
    levels = read_scowl(scowl_dir)
    wordset = set(levels)
    wordset_level.update(levels)
    blocklist = {l.strip() for l in (HERE / "en_blocklist.txt").read_text().splitlines() if l.strip() and not l.startswith("#")}
    hand = {}
    hand_path = HERE / "en_clues_handwritten.tsv"
    if hand_path.exists():
        for line in hand_path.read_text(encoding="utf-8").splitlines():
            if line.strip() and not line.startswith("#"):
                w, c = line.split("\t", 1)
                hand[w.strip().lower()] = [x.strip() for x in c.split("|") if x.strip()]
    synsets, senses, irregular = read_wordnet(wn_path)

    rows, skipped = [], 0
    for word in sorted(levels):
        if word in blocklist:
            continue
        lv = levels[word]
        commonness, hardness = LEVELS[lv]
        clues = hand.get(word) or clues_for(word, senses, synsets, irregular, wordset)
        if not clues:
            skipped += 1
            continue
        clues = [c.replace("\t", " ").replace("|", "/") for c in clues]
        rows.append(f"{word.upper()}\t{commonness}\t{hardness}\t{'|'.join(clues)}")
    for word, clues in hand.items():  # hand-written words not in scowl still get in
        if word not in levels and word not in blocklist and re.fullmatch(r"[a-z]{3,15}", word):
            rows.append(f"{word.upper()}\t3\t1\t{'|'.join(clues)}")
    Path(out_path).parent.mkdir(parents=True, exist_ok=True)
    Path(out_path).write_text("\n".join(sorted(rows)) + "\n", encoding="utf-8")
    print(f"{len(rows)} words written, {skipped} skipped (no clue)")


if __name__ == "__main__":
    main()
