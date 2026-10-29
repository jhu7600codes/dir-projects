"""downloads cmudict and writes app/src/main/assets/cmudict.txt.gz: one line per word,
`word ARPABET...`, first pronunciation only, plain a-z and ' words only, sorted by
bytes so the app can binary search it without loading it into a map.

cmudict is (c) carnegie mellon university, bsd-style license, see
https://github.com/cmusphinx/cmudict/blob/master/LICENSE"""
import gzip
import re
import sys
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
URL = "https://raw.githubusercontent.com/cmusphinx/cmudict/master/cmudict.dict"
OUT = ROOT / "app/src/main/assets/cmudict.txt.gz"


def main():
    src = sys.argv[1] if len(sys.argv) > 1 else None
    text = Path(src).read_text(encoding="utf-8") if src else urllib.request.urlopen(URL).read().decode("utf-8")
    words = {}
    for line in text.splitlines():
        line = line.split("#")[0].strip()
        if not line:
            continue
        word, *phones = line.split()
        if "(" in word or not re.fullmatch(r"[a-z']+", word) or word in words:
            continue
        words[word] = " ".join(phones)
    lines = sorted(f"{w} {p}" for w, p in words.items())
    lines.sort(key=lambda l: l.split(" ", 1)[0].encode())
    data = ("\n".join(lines) + "\n").encode("ascii")
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_bytes(gzip.compress(data, 9, mtime=0))
    print(f"{len(lines)} words, {len(data) // 1024} kb raw, {OUT.stat().st_size // 1024} kb gz -> {OUT.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
