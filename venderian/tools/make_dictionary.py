"""builds app/src/main/assets/dictionary.json from the word list below and checks
every word against the phonotactics in docs/venderian.md. run it after editing."""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent

# word | english | part of speech. edit here, then run this script.
WORDS = """
mi|I, me|pronoun
tu|you|pronoun
sa|he, she, it|pronoun
min|we, us|pronoun
tun|you (plural)|pronoun
san|they, them|pronoun
ve|this|pronoun
vo|that|pronoun
kwo|what|pronoun
kwi|who|pronoun
kwe|where|adverb
kwan|when|adverb
kwu|how|adverb
kwa|why|adverb
ka|question marker (ends a yes/no question)|particle
ne|not (before the verb)|particle
da|of, belonging to|particle
-ta|past tense (verb suffix)|suffix
-va|future tense (verb suffix)|suffix
-an|plural after a consonant (noun suffix)|suffix
-n|plural after a vowel (noun suffix)|suffix
i|and|conjunction
o|or|conjunction
shom|but|conjunction
kel|with|preposition
un|in, inside|preposition
ra|to, toward|preposition
dos|from|preposition
sur|on, on top of|preposition
vela|hello|interjection
dano|goodbye|interjection
shevan|thank you|interjection
kiro|please|interjection
ya|yes|interjection
nay|no|interjection
zen|zero|number
ek|one|number
dor|two|number
tis|three|number
kav|four|number
pam|five|number
shel|six|number
sib|seven|number
okh|eight|number
nir|nine|number
dek|ten|number
kar|house, home|noun
nami|water|noun
fos|fire|noun
tera|earth, ground|noun
khal|wind, air|noun
sol|sun|noun
mera|moon|noun
stel|star|noun
vin|sky|noun
bor|mountain|noun
ruma|river|noun
sheva|sea|noun
tal|tree|noun
filo|leaf|noun
pira|stone|noun
gala|flower|noun
zor|road, path|noun
bren|bread|noun
mikh|milk|noun
yamë|food|noun
kitë|book|noun
thel|word|noun
thelor|language|noun
ishe|name|noun
dey|day|noun
noch|night|noun
kron|time|noun
yer|year|noun
vren|friend|noun
folka|family|noun
ama|mother|noun
aba|father|noun
kiti|child|noun
dar|man|noun
vira|woman|noun
hum|person|noun
vuf|dog|noun
mish|cat|noun
pita|bird|noun
glob|fish|noun
kor|heart|noun
kop|head|noun
khan|hand|noun
vis|eye|noun
mun|mouth|noun
urba|city|noun
skol|school|noun
dur|door|noun
vindë|window|noun
zil|money|noun
rab|work, job|noun
lum|light|noun
lir|song|noun
ludo|game|noun
shi|thing|noun
es|to be|verb
ha|to have|verb
gol|to go|verb
ven|to come|verb
sen|to see|verb
ota|to hear|verb
thela|to speak|verb
kenë|to know|verb
lerna|to learn|verb
dosha|to teach|verb
ridë|to read|verb
skri|to write|verb
yama|to eat|verb
sipa|to drink|verb
nema|to sleep|verb
do|to give|verb
pren|to take|verb
fasa|to make, to do|verb
vola|to want|verb
liva|to love|verb
goda|to like|verb
pasa|to walk|verb
kura|to run|verb
seda|to sit|verb
sta|to stand|verb
viva|to live|verb
mora|to die|verb
apra|to open|verb
klu|to close|verb
ayda|to help|verb
luda|to play|verb
lira|to sing|verb
kopa|to buy|verb
trova|to find|verb
penka|to think|verb
senta|to feel|verb
atë|to wait|verb
kweta|to ask|verb
repa|to answer|verb
rabo|to work|verb
bon|good|adjective
mal|bad|adjective
gran|big|adjective
pik|small|adjective
nov|new|adjective
vet|old|adjective
kal|hot, warm|adjective
frid|cold|adjective
rap|fast|adjective
lenë|slow|adjective
bela|beautiful|adjective
fela|happy|adjective
tris|sad|adjective
rud|red|adjective
blu|blue|adjective
verdë|green|adjective
alba|white|adjective
mog|black, dark|adjective
gel|yellow|adjective
lon|long|adjective
bresh|short|adjective
multa|many, much|adjective
pok|few, a little|adjective
tot|all, every|adjective
forë|strong|adjective
lekh|easy|adjective
khar|hard, difficult|adjective
ver|true|adjective
fal|false|adjective
ik|here|adverb
lë|there|adverb
nun|now|adverb
vedey|today|adverb
posdey|tomorrow|adverb
predey|yesterday|adverb
mol|very|adverb
tam|also, too|adverb
"""

CONS = ["ng", "th", "sh", "kh", "ch", "p", "b", "t", "d", "k", "g", "f", "v",
        "s", "z", "h", "m", "n", "r", "l", "y", "w"]
VOWELS = ["a", "e", "i", "o", "u", "ë"]
GLIDES = {"r", "l", "y", "w"}


def phonemes(word):
    out, i = [], 0
    while i < len(word):
        for p in CONS + VOWELS:
            if word.startswith(p, i):
                out.append(p)
                i += len(p)
                break
        else:
            raise ValueError(f"{word}: unknown letter at {word[i:]}")
    return out


def legal(word):
    """syllable = (s)(C)(G)V(C), checked by splitting each consonant run between
    vowels into a coda of 0-1 consonants plus a legal onset."""
    ph = phonemes(word.strip("-"))
    if word.startswith("-"):  # suffixes attach to a stem, only check letters
        return True
    vowel_at = [i for i, p in enumerate(ph) if p in VOWELS]
    if not vowel_at:
        return False

    def onset_ok(run, initial):
        if not run:
            return True
        if run[0] == "s" and len(run) > 1 and initial:
            run = run[1:]
        if len(run) == 1:
            return True
        return len(run) == 2 and run[1] in GLIDES and run[0] not in GLIDES

    if not onset_ok(ph[:vowel_at[0]], True):
        return False
    tail = ph[vowel_at[-1] + 1:]
    if len(tail) > 1:
        return False
    for a, b in zip(vowel_at, vowel_at[1:]):
        run = ph[a + 1:b]
        if not any(onset_ok(run[c:], True) for c in range(0, min(1, len(run)) + 1)):
            return False
    return True


def main():
    entries, seen, bad = [], set(), []
    for line in WORDS.strip().splitlines():
        word, meaning, pos = (x.strip() for x in line.split("|"))
        if word in seen:
            bad.append(f"duplicate: {word}")
        seen.add(word)
        if not legal(word):
            bad.append(f"breaks phonotactics: {word}")
        entries.append({"word": word, "meaning": meaning, "pos": pos})
    if bad:
        sys.exit("\n".join(bad))
    out = ROOT / "app/src/main/assets/dictionary.json"
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(entries, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    print(f"{len(entries)} words -> {out.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
