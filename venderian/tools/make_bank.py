"""gives every "? | meaning | pos" line in course/bank.txt a new venderian word, in place.

words are made from syllables that follow the sound rules, with common consonants more
likely, verbs ending in a vowel like the hand-made ones, and short words for little words.
each meaning always gets the same word (the random generator is seeded by the meaning),
a meaning the dictionary already has is skipped, and no word (or word + ending) may equal
another word. after this, make_course.py adds the bank to the dictionary."""
import random
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from make_dictionary import VOWELS, legal  # noqa: E402
import make_course  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
BANK = ROOT / "course/bank.txt"

CONSONANTS = {"t": 5, "k": 5, "s": 5, "n": 5, "l": 5, "r": 5, "m": 4, "d": 4, "p": 4, "v": 4,
              "b": 3, "g": 3, "f": 2, "z": 2, "sh": 2, "ch": 1, "kh": 1, "th": 1, "h": 1, "y": 1, "w": 1}
VOWEL_W = {"a": 5, "e": 4, "i": 4, "o": 4, "u": 3, "ë": 1}
CODAS = ["n", "l", "r", "s", "k", "t", "m", "sh", "ng"]
CAN_GLIDE = {"p", "b", "t", "d", "k", "g", "f", "v"}
FORMS = {"noun": ["n", "an"], "pronoun": ["n"], "verb": ["ta", "va", "ri", "ve"],
         "adjective": ["or", "r", "esh", "sh", "mo", "tal"], "number": ["i"]}


# words that would confuse: real english words (a learner reads "that" as english), and the
# endings themselves ("ta" looks like the past ending -ta)
ENGLISH = set("""a an the and or but if so as at by for from in into of off on onto out over to up with
i me my we us our you your he him his she her it its they them their this that these those who what
is am are was be been do did does done go went get got make made take took see saw say said tell come
can could will would shall should may might must have has had not no yes all any some one two ten
man men son sun bat bad bed big boy car cat cut dog dot ear eat end eye far fat few fit fun gas god
gun hat hit hot ice kid kit law leg let lie lip lot low mad map mom net new nod nut odd old pan pen
pet pig pin pit pot put ram ran rat red rib rid rob rod rot row rub run sad sat sea set sin sit six
ski sky son tab tag tan tap tar tea ten tie tin tip toe top toy tub van vet war wet win won yet zoo
push pull shed shin ship shop shot slim slip slot spin spot stem step stop such than then thin this
thus time tone took tool tube tune turn upon vast very vote wake walk wall want warm wash wave weak
wear week well went were west wide wife wild will wind wing wire wise wish wood word wore work worm
wrap yard yarn year more less most mule sole lobe dune rune lone lore mole poke rope robe""".split())
ENDINGS_LOOKALIKE = {"n", "an", "ta", "va", "or", "r", "esh", "sh", "mo", "i", "ri", "tal", "ve"}


def pick(rng, weights):
    return rng.choices(list(weights), list(weights.values()))[0]


def make_word(rng, pos):
    short = pos in ("adverb", "preposition", "conjunction", "interjection")
    n = rng.choices([1, 2, 3], [5, 4, 1] if short else [1, 7, 2])[0]
    out = []
    for i in range(n):
        last = i == n - 1
        if i == 0 and rng.random() < 0.12:
            onset = ""  # words can start with a vowel
        else:
            onset = pick(rng, CONSONANTS)
            if onset in CAN_GLIDE and rng.random() < 0.12:
                onset += rng.choice(["r", "l"])
        coda = ""
        coda_chance = 0.0 if (last and pos == "verb") else (0.45 if last else 0.2)
        if rng.random() < coda_chance:
            coda = rng.choice(CODAS)
        out.append(onset + pick(rng, VOWEL_W) + coda)
    return "".join(out)


def forms(word, pos):
    out = set()
    for s in FORMS.get(pos, []):
        needs_vowel = make_course.ENDINGS.get(s)
        try:
            ends_vowel = make_course.ends_in_vowel(word)
        except ValueError:
            continue
        if needs_vowel is not None and needs_vowel != ends_vowel:
            continue
        out.add(word + s)
    return out


def key(meaning, pos):
    """what a meaning is about: a verb "to stop" and a noun "stop" are different words"""
    kind = "verb" if pos == "verb" else "other"
    return kind, re.sub(r"\(.*?\)", "", meaning).strip().lower().removeprefix("to ").strip()


def main():
    sections, defined, errors = make_course.parse()
    entries = make_course.build_dictionary(defined, errors, with_bank=False)
    taken = {e["word"] for e in entries}
    taken_forms = set()
    for e in entries:
        taken_forms |= forms(e["word"], e["pos"])
    meanings = {key(p, e["pos"]) for e in entries for p in e["meaning"].split(",")}

    lines = BANK.read_text(encoding="utf-8").splitlines()
    out, made, skipped = [], 0, []
    for line in lines:
        if not line.startswith("?"):
            if "|" in line and not line.startswith("#"):
                w, m, pos = (x.strip() for x in line.split("|"))
                taken.add(w)
                taken_forms |= forms(w, pos)
                meanings |= {key(p, pos) for p in m.split(",")}
            out.append(line)
            continue
        _, meaning, pos = (x.strip() for x in line.split("|"))
        if key(meaning.split(",")[0], pos) in meanings:
            skipped.append(meaning)
            continue
        rng = random.Random(meaning)
        for _ in range(2000):
            w = make_word(rng, pos)
            if w in taken or w in taken_forms or forms(w, pos) & taken:
                continue
            # no one-sound words like "e", no tiny content words, nothing that reads as english
            if len(w) < 2 or w[0] in VOWELS and len(w) < 3:
                continue
            if pos in ("noun", "verb", "adjective") and len(w) < 3:
                continue
            if w in ENGLISH or w in ENDINGS_LOOKALIKE:
                continue
            try:
                if not legal(w):
                    continue
            except ValueError:
                continue
            break
        else:
            sys.exit(f"no word found for {meaning}")
        taken.add(w)
        taken_forms |= forms(w, pos)
        meanings |= {key(p, pos) for p in meaning.split(",")}
        out.append(f"{w} | {meaning} | {pos}")
        made += 1
    BANK.write_text("\n".join(out) + "\n", encoding="utf-8")
    print(f"{made} new words, {len(skipped)} skipped (the dictionary has them): {', '.join(skipped)}")


if __name__ == "__main__":
    main()
