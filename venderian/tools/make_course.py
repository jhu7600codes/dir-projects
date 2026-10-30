"""builds the course from course/s*.txt into app/src/main/assets/course.json, and the full
dictionary (starter words + every word the course defines) into dictionary.json.

course file format:
  section N | title | subtitle
  module N | title            (module N.5 for the second half)
  lesson title
  new: word, word, -suffix    words taught here (already in the dictionary)
  + word | meaning | pos      a new word, taught here
  tip: text                   a grammar note shown before the lesson
  venderian = english         a sentence

it checks that every word in a sentence was taught in this lesson or an earlier one (with
endings like -an, -ta, -or only once those endings were taught), that endings attach the
right way (-n after a vowel, -an after a consonant...), that new words follow the sound
rules, and that no ending turns one word into another word. a challenge is added after
every module N, and a gold lesson after every module N.5.
s1.txt holds lessons 1-5 of each module and s1x.txt lessons 6-10 (same for s2, s3)."""
import json
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from make_dictionary import VOWELS, legal, phonemes, starter  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
COURSE = ROOT / "course"
OUT_COURSE = ROOT / "app/src/main/assets/course.json"
OUT_DICT = ROOT / "app/src/main/assets/dictionary.json"

# endings: (suffix, needs a stem ending in a vowel (True), consonant (False), or either (None))
ENDINGS = {
    "n": True, "an": False, "ta": None, "va": None, "or": False, "r": True,
    "esh": False, "sh": True, "mo": None, "i": False, "ri": True, "tal": None, "ve": None,
}
# forms that are meant to look like a plural of something else
SAME_ON_PURPOSE = {"min", "tun", "san", "human"}
PUNCT = re.compile(r"[.,!?]")


def ends_in_vowel(word):
    return phonemes(word)[-1] in VOWELS


def parse():
    sections, errors = [], []
    # s1.txt, s1x.txt, s2.txt...: a file's base comes before its extra lessons
    defined = []  # (word, meaning, pos) defined by the course
    for path in sorted(COURSE.glob("s*.txt"), key=lambda p: (p.stem.rstrip("x"), p.stem)):
        section = module = lesson = None
        for n, raw in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
            line = raw.split("#")[0].strip() if not raw.strip().startswith("+") else raw.strip()
            if not line:
                continue
            where = f"{path.name}:{n}"
            # a section or module that already exists (from an earlier file) gets the new lessons
            # appended: s1.txt has lessons 1-5 of each module, s1x.txt lessons 6-10
            if line.startswith("section "):
                num, title, subtitle = (x.strip() for x in line[len("section "):].split("|"))
                section = next((x for x in sections if x["id"] == int(num)), None)
                if section is None:
                    section = {"id": int(num), "title": title, "subtitle": subtitle, "modules": []}
                    sections.append(section)
            elif line.startswith("module "):
                num, title = (x.strip() for x in line[len("module "):].split("|"))
                module = next((x for x in section["modules"] if x["id"] == num), None)
                if module is None:
                    module = {"id": num, "title": title, "half": num.endswith(".5"), "lessons": []}
                    section["modules"].append(module)
            elif line.startswith("lesson "):
                lesson = {"title": line[len("lesson "):].strip(), "tip": "", "words": [], "sentences": [], "where": where}
                module["lessons"].append(lesson)
            elif line.startswith("new:"):
                lesson["words"] += [w.strip() for w in line[4:].split(",") if w.strip()]
            elif line.startswith("+"):
                word, meaning, pos = (x.strip() for x in line[1:].split("|"))
                lesson["words"].append(word)
                defined.append((word, meaning, pos, where))
            elif line.startswith("tip:"):
                lesson["tip"] = line[4:].strip()
            elif " = " in line:
                v, e = (x.strip() for x in line.split(" = ", 1))
                lesson["sentences"].append({"v": v, "e": e, "where": where})
            else:
                errors.append(f"{where}: can't read: {line}")
    return sections, defined, errors


BANK = ROOT / "course/bank.txt"


def bank():
    """(word, meaning, pos, where) from course/bank.txt: words the course doesn't have to teach"""
    out = []
    for n, line in enumerate(BANK.read_text(encoding="utf-8").splitlines(), 1):
        if "|" not in line or line.startswith("#") or line.startswith("?"):
            continue
        w, m, pos = (x.strip() for x in line.split("|"))
        out.append((w, m, pos, f"bank.txt:{n}"))
    return out


STORIES = ROOT / "course/stories"


def parse_stories(sections, errors):
    """course/stories/s*.txt: duolingo-style stories, a conversation with questions in between.

      section N
      module N | title
      story title            in the middle of the module (after its 5th lesson)
      challenge story title  part of module N's challenge
      gold story title       part of module N.5's gold lesson
      name: venderian = english            a line someone says
      ? question | right* | wrong | wrong  a question about the story (* marks the answer)
    """
    for path in sorted(STORIES.glob("s*.txt")):
        section = module = story = None
        for n, raw in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
            line = raw.strip()
            if not line or line.startswith("#"):
                continue
            where = f"stories/{path.name}:{n}"
            if line.startswith("section "):
                section = next(x for x in sections if x["id"] == int(line.split()[1]))
            elif line.startswith("module "):
                num = line[len("module "):].split("|")[0].strip()
                module = next(x for x in section["modules"] if x["id"] == num)
            elif line.startswith(("story ", "challenge story ", "gold story ")):
                kind, _, title = line.partition("story ")
                story = {"kind": "story", "title": title.strip(), "parts": [], "where": where}
                slot = kind.strip() or "middle"
                if slot == "middle":
                    lessons = [l for l in module["lessons"] if l.get("kind") != "story"]
                    module["lessons"].insert(module["lessons"].index(lessons[4]) + 1, story)
                else:
                    module[f"{slot}_story"] = story
            elif line.startswith("?"):
                q, *options = (x.strip() for x in line[1:].split("|"))
                answer = [i for i, o in enumerate(options) if o.endswith("*")]
                if len(answer) != 1:
                    errors.append(f"{where}: a question needs exactly one answer marked with *")
                    continue
                story["parts"].append({"q": q, "options": [o.rstrip("*").strip() for o in options], "answer": answer[0]})
            elif ":" in line and " = " in line:
                who, _, rest = line.partition(":")
                v, e = (x.strip() for x in rest.split(" = ", 1))
                story["parts"].append({"who": who.strip(), "v": v, "e": e, "where": where})
            else:
                errors.append(f"{where}: can't read: {line}")


def build_dictionary(defined, errors, with_bank=True):
    entries = starter()
    known = {e["word"] for e in entries}
    if with_bank:
        defined = defined + bank()
    for word, meaning, pos, where in defined:
        if word in known:
            errors.append(f"{where}: {word} is already in the dictionary")
            continue
        try:
            ok = pos == "name" or legal(word)
        except ValueError as e:
            ok, _ = False, errors.append(f"{where}: {e}")
        if not ok:
            errors.append(f"{where}: {word} breaks the sound rules")
        known.add(word)
        entries.append({"word": word, "meaning": meaning, "pos": pos})
    return entries


def check_collisions(entries, errors):
    """an ending must never turn one word into another word"""
    words = {e["word"]: e["pos"] for e in entries if e["pos"] != "suffix"}
    forms = {"noun": ["n", "an"], "pronoun": ["n"], "verb": ["ta", "va", "ri", "ve"],
             "adjective": ["or", "r", "esh", "sh", "mo", "tal"], "number": ["i"]}
    for w, pos in words.items():
        for suffix in forms.get(pos, []):
            needs_vowel = ENDINGS[suffix]
            if needs_vowel is not None and needs_vowel != ends_in_vowel(w):
                continue
            form = w + suffix
            if form in words and form not in SAME_ON_PURPOSE and not (pos == "pronoun" and w in ("ve", "vo")):
                errors.append(f"{w} + -{suffix} = {form}, which is already a word ({words[form]})")


def word_ok(token, known, endings, depth=0):
    if token in known:
        return True
    if depth >= 3:
        return False
    for suffix in sorted(endings, key=len, reverse=True):
        if token.endswith(suffix) and len(token) > len(suffix):
            stem = token[: -len(suffix)]
            needs_vowel = ENDINGS[suffix]
            try:
                fits = needs_vowel is None or needs_vowel == ends_in_vowel(stem)
            except ValueError:
                continue
            if fits and word_ok(stem, known, endings, depth + 1):
                return True
    return False


def check_story(story, known, endings, errors):
    lines = [p for p in story["parts"] if "v" in p]
    if len(lines) < 4 or not any("q" in p for p in story["parts"]):
        errors.append(f"{story['where']}: a story needs at least 4 lines and a question")
    for p in lines:
        for token in PUNCT.sub(" ", p["v"]).lower().split():
            if not word_ok(token, known, endings):
                errors.append(f"{p['where']}: '{token}' wasn't taught yet ({p['v']})")


def check_lessons(sections, entries, errors):
    dictionary = {e["word"] for e in entries}
    known, endings = set(), set()
    for section in sections:
        for module in section["modules"]:
            for lesson in module["lessons"]:
                if lesson.get("kind") == "story":
                    check_story(lesson, known, endings, errors)
                    continue
                # a word listed again later is only taught the first time
                lesson["words"] = [w for w in lesson["words"] if w not in known and not (w.startswith("-") and w[1:] in endings)]
                for w in lesson["words"]:
                    if w not in dictionary:
                        errors.append(f"{lesson['where']}: {w} is not in the dictionary")
                    if w.startswith("-"):
                        endings.add(w[1:])
                    else:
                        known.add(w)
                if not lesson["sentences"]:
                    errors.append(f"{lesson['where']}: lesson has no sentences")
                for s in lesson["sentences"]:
                    for token in PUNCT.sub(" ", s["v"]).lower().split():
                        if not word_ok(token, known, endings):
                            errors.append(f"{s['where']}: '{token}' wasn't taught yet ({s['v']})")
            # the challenge / gold story comes after all of the module's lessons
            for slot in ("challenge_story", "gold_story"):
                if slot in module:
                    check_story(module[slot], known, endings, errors)


def story_parts(story):
    if not story:
        return []
    return [{k: v for k, v in p.items() if k != "where"} for p in story["parts"]]


def assemble(sections):
    """lesson ids, plus a challenge after module N and a gold lesson after module N.5"""
    out = []
    for section in sections:
        sec = {k: section[k] for k in ("id", "title", "subtitle")} | {"modules": []}
        first_half = []
        for module in section["modules"]:
            lessons = []
            i = 0
            for lesson in module["lessons"]:
                if lesson.get("kind") == "story":
                    # stories don't count as numbered lessons, so lesson ids (and progress) stay the same
                    lessons.append({"id": f"m{module['id']}-story", "kind": "story", "title": lesson["title"],
                                    "parts": story_parts(lesson)})
                    continue
                i += 1
                lessons.append({
                    "id": f"m{module['id']}-l{i}", "kind": "lesson", "title": lesson["title"], "tip": lesson["tip"],
                    "words": lesson["words"], "sentences": [{"v": s["v"], "e": s["e"]} for s in lesson["sentences"]],
                })
            numbered = [l["id"] for l in lessons if l["kind"] == "lesson"]
            if not module["half"]:
                first_half = numbered
                lessons.append({"id": f"m{module['id']}-challenge", "kind": "challenge", "title": "challenge", "from": first_half,
                                "parts": story_parts(module.get("challenge_story"))})
            else:
                lessons.append({"id": f"m{module['id']}-gold", "kind": "gold", "title": "gold lesson", "from": first_half + numbered,
                                "parts": story_parts(module.get("gold_story"))})
            sec["modules"].append({"id": module["id"], "title": module["title"], "half": module["half"], "lessons": lessons})
        out.append(sec)
    return {"sections": out}


def main():
    sections, defined, errors = parse()
    parse_stories(sections, errors)
    entries = build_dictionary(defined, errors)
    check_collisions(entries, errors)
    check_lessons(sections, entries, errors)
    if errors:
        sys.exit("\n".join(errors))
    course = assemble(sections)
    OUT_DICT.write_text(json.dumps(entries, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    OUT_COURSE.write_text(json.dumps(course, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    lessons = [l for s in course["sections"] for m in s["modules"] for l in m["lessons"]]
    sentences = sum(len(l.get("sentences", [])) for l in lessons)
    stories = sum(1 for l in lessons if l.get("parts"))
    print(f"{len(entries)} words ({len(defined)} from the course) -> {OUT_DICT.relative_to(ROOT)}")
    print(f"{len(course['sections'])} sections, {len(lessons)} lessons, {sentences} sentences, {stories} stories -> {OUT_COURSE.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
