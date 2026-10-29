"""renders docs/alphabet.svg (the alphabet chart) from docs/alphabet.json."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
data = json.loads((ROOT / "docs/alphabet.json").read_text(encoding="utf-8"))
marks = data["marks"]
letters = {g["id"]: g for g in data["consonants"] + data["vowels"]}

S = 9          # px per grid unit
ADVANCE = 5    # grid units per letter when writing words
CELL_W, CELL_H = 110, 140
COLS = 8
INK, SUB, BG, CARD = "#e8e6f0", "#9a96ad", "#16141c", "#211e2a"


def strokes_svg(strokes, x0, y0):
    sw = data["strokeWidth"] * S
    out = []
    for stroke in strokes:
        pts = " ".join(f"{x0 + x * S:.1f},{y0 - y * S:.1f}" for x, y in stroke)
        tag = "polygon" if stroke[0] == stroke[-1] else "polyline"
        out.append(f'<{tag} points="{pts}" fill="none" stroke="{INK}" '
                   f'stroke-width="{sw:.1f}" stroke-linejoin="miter" stroke-linecap="square"/>')
    return "".join(out)


def headline(x_from, x_to):
    return [[[x_from, 6], [x_to, 6]]]


def letter_strokes(g, extra=(), width=4):
    s = list(g["strokes"]) + list(extra)
    if g.get("headline"):
        s = headline(-0.5, width + 0.5) + s
    return s


def label(text, x, y, color=INK):
    return (f'<text x="{x}" y="{y}" fill="{color}" font-size="15" text-anchor="middle" '
            f'font-family="sans-serif">{text}</text>')


def section(title, items, top, name):
    parts = [f'<text x="20" y="{top + 24}" fill="{SUB}" font-size="16" font-family="sans-serif">{title}</text>']
    for i, it in enumerate(items):
        cx = 20 + (i % COLS) * CELL_W
        cy = top + 40 + (i // COLS) * CELL_H
        parts.append(f'<rect x="{cx}" y="{cy}" width="{CELL_W - 10}" height="{CELL_H - 10}" rx="10" fill="{CARD}"/>')
        parts.append(strokes_svg(letter_strokes(it), cx + 32, cy + 95))
        parts.append(label(name(it), cx + 50, cy + 122))
    rows = (len(items) + COLS - 1) // COLS
    return "".join(parts), top + 40 + rows * CELL_H


def word(units):
    """units: list of (letter ids, extra marks). returns strokes laid out left to right
    with one continuous headline."""
    out, x = [], 0
    for ids, extra in units:
        g = letters[ids]
        for st in list(g["strokes"]) + list(extra):
            out.append([[px + x, py] for px, py in st])
        x += ADVANCE
    return headline(-0.5, x - ADVANCE + 4.5) + out, x


def main():
    body, y = [], 0
    for title, key, name in [
        ("consonants (hang from the headline)", "consonants", lambda it: f'{it["roman"]}  /{it["ipa"]}/'),
        ("vowels (float under the headline)", "vowels", lambda it: f'{it["roman"]}  /{it["ipa"]}/'),
        ("digits (no headline)", "digits", lambda it: it["id"]),
        ("punctuation", "punctuation", lambda it: it["name"]),
    ]:
        part, y = section(title, data[key], y, name)
        body.append(part)

    # sample words: joined headline, ligatures, word gaps
    schwa, double = marks["schwa_bar"]["strokes"], marks["double_line"]["strokes"]
    samples = [
        ("vela mi", [[("v", []), ("e", []), ("l", []), ("a", [])], [("m", []), ("i", [])]]),
        ("kitë", [[("k", []), ("i", []), ("t", schwa)]]),
        ("dekkav", [[("d", []), ("e", []), ("k", double), ("a", []), ("v", [])]]),
    ]
    body.append(f'<text x="20" y="{y + 24}" fill="{SUB}" font-size="16" font-family="sans-serif">'
                f'words (headline joins letters, gap between words, ligatures)</text>')
    cy = y + 40
    w_total = 20 * 2 + COLS * CELL_W
    body.append(f'<rect x="20" y="{cy}" width="{w_total - 50}" height="{CELL_H + 10}" rx="10" fill="{CARD}"/>')
    cx = 50
    for text, words in samples:
        start = cx
        for units in words:
            st, wdt = word(units)
            body.append(strokes_svg(st, cx, cy + 100))
            cx += wdt * S + 3 * S
        body.append(label(text, (start + cx - 3 * S) / 2, cy + 138))
        cx += 4 * S
    y = cy + CELL_H + 20

    svg = (f'<svg xmlns="http://www.w3.org/2000/svg" width="{w_total}" height="{y}" viewBox="0 0 {w_total} {y}">'
           f'<rect width="100%" height="100%" fill="{BG}"/>' + "".join(body) + "</svg>")
    (ROOT / "docs/alphabet.svg").write_text(svg, encoding="utf-8")


if __name__ == "__main__":
    main()
