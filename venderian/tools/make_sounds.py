"""builds app/src/main/assets/sounds.json: the "english sounds" lessons, where you learn to
write english words in venderian letters by how they sound (what the keyboard's "spell by
sound" does). every word is spelled from cmudict with the same table as the app's converter
(docs/venderian.md section 5), so the lessons and the keyboard always agree."""
import gzip
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CMUDICT = ROOT / "app/src/main/assets/cmudict.dat"
OUT = ROOT / "app/src/main/assets/sounds.json"

ARPABET = {
    "AA": ["a"], "AE": ["a"], "AO": ["o"], "AW": ["a", "w"], "AY": ["a", "y"], "EH": ["e"], "ER": ["ë", "r"],
    "EY": ["e", "y"], "IH": ["i"], "IY": ["i"], "OW": ["o", "w"], "OY": ["o", "y"], "UH": ["u"], "UW": ["u"],
    "B": ["b"], "CH": ["ch"], "JH": ["ch"], "D": ["d"], "DH": ["th"], "TH": ["th"], "F": ["f"], "G": ["g"],
    "HH": ["h"], "K": ["k"], "L": ["l"], "M": ["m"], "N": ["n"], "NG": ["ng"], "P": ["p"], "R": ["r"],
    "S": ["s"], "SH": ["sh"], "ZH": ["sh"], "T": ["t"], "V": ["v"], "W": ["w"], "Y": ["y"], "Z": ["z"],
}
DIGRAPHS = {"ng", "th", "sh", "kh", "ch"}

LESSONS = [
    ("sounds you know", "most consonants sound just like english. one letter per sound, so a word has as many letters as sounds.",
     "map pen cat dog bed sun top milk"),
    ("long vowels", "english writes the same sound many ways: ee, ea, e. venderian always writes it i. oo, ue and o in two all become u.",
     "see tea two blue food moon free school"),
    ("ay and ey", "the sound in day, rain and name is ey (e + y). the one in my, time and nice is ay (a + y).",
     "day name play rain my time fly nice"),
    ("ow and oy", "go and home end in ow (o + w). now and house have aw (a + w). boy and toy have oy.",
     "go home phone boy toy now house cow"),
    ("the lazy vowel ë", "the quick uh sound at the end of banana or the start of about is ë. it's the most common vowel in english.",
     "banana sofa pizza panda camera about again open"),
    ("r sounds", "the er in water, bird and girl is ë + r. other r's are just r.",
     "car bird water teacher girl word door her"),
    ("th", "both th sounds, in think and in this, are written th.",
     "think three the this mother bath both thank"),
    ("sh, ch and j", "sh stays sh. ch and j both become ch: juice is chus, job is chob.",
     "shop fish cheese chair juice job watch sure"),
    ("ng, y and w", "the end of sing is one letter, ng. y in yes and w in we are letters too.",
     "sing king long yes you we window song"),
    ("silent letters", "venderian has no silent letters: knife starts with n, light has no g or h, hour has no h.",
     "knife write know light night lamb island hour"),
    ("c, k, q and x", "c is k or s by its sound. q is k + w, x is k + s. ice and six end in s.",
     "cake city queen box kiss ice six quick"),
    ("names", "names are spelled by sound like everything else. try your own name on the keyboard!",
     "anna max sofia david emma lucas olivia leo"),
    ("countries", "country names too: russia, america, china.",
     "russia america china japan germany france canada egypt"),
    ("tech words", "new words are easy: just write what you hear.",
     "google laptop music internet video robot computer android"),
    ("long words", "long words are just more sounds in a row. say them slowly.",
     "chocolate elephant important beautiful together everything tomorrow vegetable"),
    ("tricky ones", "english spelling is wild: colonel sounds like kernel, yacht like yot. venderian doesn't care how it's spelled.",
     "colonel receipt queue yacht women choir debris wednesday"),
]


def load_cmudict():
    words = {}
    for line in gzip.decompress(CMUDICT.read_bytes()).decode("ascii").splitlines():
        w, _, phones = line.partition(" ")
        words[w] = phones.split()
    return words


def to_sounds(phones):
    out = []
    for p in phones:
        base = p.rstrip("012")
        if base == "AH":
            out.append("a" if p[-1] in "12" else "ë")
        else:
            out += ARPABET[base]
    return out


def to_roman(sounds):
    out = ""
    for s in sounds:
        if out and out[-1] + s[0] in DIGRAPHS:
            out += "'"
        out += s
    return out


def main():
    cmu = load_cmudict()
    lessons = []
    for i, (title, tip, words) in enumerate(LESSONS, 1):
        items = []
        for w in words.split():
            if w not in cmu:
                raise SystemExit(f"{w} is not in cmudict")
            items.append({"en": w, "roman": to_roman(to_sounds(cmu[w]))})
        lessons.append({"id": f"snd-{i}", "title": title, "tip": tip, "words": items})
    OUT.write_text(json.dumps({"lessons": lessons}, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    print(f"{len(lessons)} lessons, {sum(len(l['words']) for l in lessons)} words -> {OUT.relative_to(ROOT)}")
    for l in lessons:
        print(" ", l["title"] + ":", ", ".join(f"{x['en']}={x['roman']}" for x in l["words"]))


if __name__ == "__main__":
    main()
