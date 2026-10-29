"""renders docs/alphabet.svg (the alphabet chart) from docs/alphabet.json."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
data = json.loads((ROOT / "docs/alphabet.json").read_text(encoding="utf-8"))

S = 9          # px per grid unit
CELL_W, CELL_H = 110, 130
COLS = 8
INK, SUB, BG = "#e8e6f0", "#9a96ad", "#16141c"


def glyph(strokes, x0, y0, extra=()):
    sw = data["strokeWidth"] * S
    out = []
    for stroke in list(strokes) + list(extra):
        pts = " ".join(f"{x0 + x * S:.1f},{y0 - y * S:.1f}" for x, y in stroke)
        tag = "polygon" if stroke[0] == stroke[-1] else "polyline"
        out.append(f'<{tag} points="{pts}" fill="none" stroke="{INK}" '
                   f'stroke-width="{sw:.1f}" stroke-linejoin="miter" stroke-linecap="square"/>')
    return "".join(out)


def section(title, items, top, label):
    parts = [f'<text x="20" y="{top + 24}" fill="{SUB}" font-size="16" font-family="sans-serif">{title}</text>']
    for i, it in enumerate(items):
        cx = 20 + (i % COLS) * CELL_W
        cy = top + 40 + (i // COLS) * CELL_H
        parts.append(f'<rect x="{cx}" y="{cy}" width="{CELL_W - 10}" height="{CELL_H - 10}" rx="10" fill="#211e2a"/>')
        parts.append(glyph(it["strokes"], cx + 32, cy + 82))
        parts.append(f'<text x="{cx + 50}" y="{cy + 110}" fill="{INK}" font-size="15" text-anchor="middle" '
                     f'font-family="sans-serif">{label(it)}</text>')
    rows = (len(items) + COLS - 1) // COLS
    return "".join(parts), top + 40 + rows * CELL_H


def main():
    body, y = [], 0
    for title, key, label in [
        ("consonants", "consonants", lambda it: f'{it["roman"]}  /{it["ipa"]}/'),
        ("vowels", "vowels", lambda it: f'{it["roman"]}  /{it["ipa"]}/'),
        ("digits", "digits", lambda it: it["id"]),
        ("punctuation", "punctuation", lambda it: it["name"]),
    ]:
        part, y = section(title, data[key], y, label)
        body.append(part)

    # ligature examples: t + ë, and t + t
    marks = data["marks"]
    t = next(c for c in data["consonants"] if c["id"] == "t")
    lig = [
        {"strokes": t["strokes"], "extra": marks["schwa_bar"]["strokes"], "label": "t + ë → të"},
        {"strokes": t["strokes"], "extra": marks["double_ticks"]["strokes"], "label": "t + t → tt"},
    ]
    body.append(f'<text x="20" y="{y + 24}" fill="{SUB}" font-size="16" font-family="sans-serif">ligatures</text>')
    for i, it in enumerate(lig):
        cx, cy = 20 + i * CELL_W * 2, y + 40
        body.append(f'<rect x="{cx}" y="{cy}" width="{CELL_W * 2 - 10}" height="{CELL_H + 15}" rx="10" fill="#211e2a"/>')
        body.append(glyph(it["strokes"], cx + 82, cy + 92, it["extra"]))
        body.append(f'<text x="{cx + 100}" y="{cy + 133}" fill="{INK}" font-size="15" text-anchor="middle" '
                    f'font-family="sans-serif">{it["label"]}</text>')
    y += 40 + CELL_H + 25

    w = 20 * 2 + COLS * CELL_W
    svg = (f'<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{y + 10}" viewBox="0 0 {w} {y + 10}">'
           f'<rect width="100%" height="100%" fill="{BG}"/>' + "".join(body) + "</svg>")
    (ROOT / "docs/alphabet.svg").write_text(svg, encoding="utf-8")


if __name__ == "__main__":
    main()
