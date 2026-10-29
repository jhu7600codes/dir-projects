#!/usr/bin/env python3
"""
Builds app/src/main/assets/ru/words.tsv from:
  - tools/ru_words_src.txt   hand-written nouns + clues (these win)
  - Russian Wiktionary nouns via kaikki.org (CC BY-SA 4.0), glosses become clues
  - hermitdave/FrequencyWords ru_50k (CC BY-SA 4.0), keeps only common words

usage: build_ru_dictionary.py [<kaikki ru jsonl> <ru_300k.txt (top 300k of hermitdave ru_full)>]
Russian crosswords traditionally use nouns in the nominative singular, so that's all we take.
"""
import json
import re
import sys
from pathlib import Path

HERE = Path(__file__).parent
OUT = HERE.parent / "app/src/main/assets/ru/words.tsv"
ALPHABET = set("АБВГДЕЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ")
SKIP_LABELS = re.compile(r"^(устар|истор|диал|жарг|груб|бран|вульг|обсц|прост|арго|сниж|неодобр|презр|уничиж|пренебр|рег|спец|книжн|поэт|церк|офиц|проф)\.?", re.I)
BAD_GLOSS = re.compile(r"форма |падеж|по значению|то же, что|то же что|уменьш\.|ласк\.|увеличит\.|мн\. ч\.|женск\. к|к сущ\.|субстантив", re.I)
OFFENSIVE = re.compile(r"половой|половые|интим|секс|наркот|проститу|экскремент|кал\b|моча|оскорб|бранн|ругатель|непристойн|похаб|детородн|соитие|убий|казн|пытк", re.I)
# детский режим: никакого алкоголя, курения, азарта, оружия, смерти, тела, ругани
KID_GLOSS = re.compile(r"алкогол|спирт|пьян|водк|\bвин[оа]|пив[оа]|самогон|наркот|табак|сигар|курен|казино|азарт|"
                       r"убий|убит|смерт|\bумер|труп|покойн|казн|оружи|пистолет|винтовк|бомб|взрыв|войн|крови|кровь|"
                       r"секс|эрот|интим|грудь|ягодиц|беремен|тюрьм|преступ|\bвор\b|краж|\bчерт\b|чёрт|дьявол|бран|руга|груб|презр|"
                       r"глуп|дурак|жарг|сленг|прост\.", re.I)
ABBREV = re.compile(r"^((?:[а-яё]{1,8}\.)[,\s]*)+", re.I)


def hand_written():
    rows = {}
    bad = []
    for n, line in enumerate((HERE / "ru_words_src.txt").read_text(encoding="utf-8").splitlines(), 1):
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        if " — " not in line:
            bad.append(f"{n}: no dash: {line}")
            continue
        word, clues = line.split(" — ", 1)
        hard = word.endswith("*")
        word = word.rstrip("*").strip().upper().replace("Ё", "Е")
        if not (3 <= len(word) <= 15) or not set(word) <= ALPHABET:
            bad.append(f"{n}: bad word: {word}")
            continue
        clue_list = [c.strip() for c in clues.split(" / ") if c.strip()]
        if len(word) >= 4 and any(word.lower()[:4] in c.lower().replace("ё", "е") for c in clue_list):
            bad.append(f"{n}: clue contains the answer: {line}")
        # commonness 6: the filler prefers hand-written words, their clues are the good ones
        rows.setdefault(word, (6, 2 if hard else 1, clue_list))
    return rows, bad


def clean_gloss(gloss, word):
    g = gloss.strip()
    if SKIP_LABELS.match(g) or BAD_GLOSS.search(g) or OFFENSIVE.search(g) or KID_GLOSS.search(g):
        return None
    g = ABBREV.sub("", g).strip()
    g = re.sub(r"\([^)]*\)", "", g)
    g = re.split(r"[;:]", g)[0].strip(" ,.—-")
    g = re.sub(r"\s+", " ", g)
    # crossword clues are short: cut long definitions at the first comma when that still says something
    if len(g) > 45 and "," in g and len(g.split(",")[0]) >= 12:
        g = g.split(",")[0]
    if len(g) < 4 or len(g) > 60:
        return None
    low = g.lower().replace("ё", "е")
    stem = word.lower()[: max(3, min(5, len(word) - 1))]
    if stem in low:
        return None
    return g[0].upper() + g[1:]


def wiktionary(jsonl_path, freq_path):
    rank = {}
    for i, line in enumerate(Path(freq_path).read_text(encoding="utf-8").splitlines()):
        w = line.split(" ")[0].upper().replace("Ё", "Е")
        rank.setdefault(w, i)
    rows = {}
    with open(jsonl_path, encoding="utf-8") as f:
        for line in f:
            if '"pos": "noun"' not in line[:200]:
                continue
            d = json.loads(line)
            raw = d.get("word", "")
            if not raw.islower() or " " in raw or "-" in raw:
                continue
            word = raw.upper().replace("Ё", "Е")
            if not (3 <= len(word) <= 15) or not set(word) <= ALPHABET or word not in rank:
                continue
            # short words are where the junk hides (names, word forms), so they must be fairly common
            if rank[word] > {3: 150000, 4: 200000}.get(len(word), 300000):
                continue
            tags = set(d.get("tags", []))
            if tags & {"plural", "plural-only", "abbreviation", "proper-noun"}:
                continue
            clues = []
            for sense in d.get("senses", []):
                stags = set(sense.get("tags", []))
                if stags & {"obsolete", "dialectal", "vulgar", "offensive", "slang", "archaic", "rare", "derogatory"}:
                    continue
                for g in sense.get("glosses", [])[:1]:
                    c = clean_gloss(g, word)
                    if c and c not in clues:
                        clues.append(c)
                if len(clues) >= 2:
                    break
            if not clues:
                continue
            r = rank[word]
            commonness = 5 if r < 5000 else 4 if r < 15000 else 3 if r < 40000 else 2 if r < 100000 else 1
            hardness = 1 if r < 12000 else 2 if r < 40000 else 3
            if word not in rows or rows[word][0] < commonness:
                rows[word] = (commonness, hardness, clues)
    return rows


# мат, грубости, наркотики, смерть: слова с таким началом не попадают в сетку
BAD_PREFIXES = ("АЛКОГОЛ", "ВОДК", "ПЬЯН", "СИГАР", "КАЗИНО", "БОМБ", "ПИСТОЛЕТ", "ВИНТОВК", "ГРОБ", "КАЗН",
                "САМОУБ", "ХУЙ", "ХУЕ", "ПИЗД", "ЕБ", "БЛЯ", "СУКА", "СУКИ", "СУЧ", "МУДА", "МУДО", "ЖОП", "СРАК", "ДЕРЬМ",
                "ГОВН", "ПЕДИК", "ПИДОР", "ШЛЮХ", "ПРОСТИТУ", "МРАЗ", "ГАНДОН", "ЗАЛУП", "ДРОЧ", "ЧЛЕН", "СЕКС",
                "ПОРН", "НАРКО", "УБИЙ", "ТРУП")


def is_bad(word):
    return word.startswith(BAD_PREFIXES)


def main():
    hand, bad = hand_written()
    rows = {}
    # only hand-written clues ship: wiktionary glosses are too confusing for kids.
    # pass --wiktionary <jsonl> <freq> to mix them back in.
    if len(sys.argv) >= 4 and sys.argv[1] == "--wiktionary":
        rows.update(wiktionary(sys.argv[2], sys.argv[3]))
    blocklist = {l.strip().upper() for l in (HERE / "ru_blocklist.txt").read_text(encoding="utf-8").splitlines()
                 if l.strip() and not l.startswith("#")}
    rows.update(hand)
    lines = [f"{w}\t{c}\t{h}\t{'|'.join(cl)}" for w, (c, h, cl) in sorted(rows.items()) if w not in blocklist and not is_bad(w)]
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"{len(lines)} words ({len(hand)} hand-written)")
    for b in bad:
        print("WARN", b)


if __name__ == "__main__":
    main()
