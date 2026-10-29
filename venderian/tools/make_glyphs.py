"""draws every glyph (letters, ligatures, digits, punctuation) from docs/alphabet.json
into glyphs/*.svg as filled outlines in font units. build_font.py turns those into the ttf.

svg coordinates are font units flipped vertically (svg y = ASCENT - font y)."""
import json
import shutil
from pathlib import Path

from shapely.affinity import translate
from shapely.geometry import LineString, Polygon, box
from shapely.geometry.polygon import orient
from shapely.ops import unary_union

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "glyphs"
data = json.loads((ROOT / "docs/alphabet.json").read_text(encoding="utf-8"))

UNIT = 100                 # font units per grid unit
ASCENT, DESCENT = 900, -250
HALF = data["strokeWidth"] / 2
LETTER_SHIFT = 0.75        # left side bearing of letters, in grid units
LETTER_ADVANCE = 5.5       # letters are all this wide so headlines join up
HEADLINE_Y = 6


def stroke_shape(points):
    if points[0] == points[-1]:
        ring = Polygon(points)
        return ring.exterior.buffer(HALF, join_style="mitre", mitre_limit=3)
    return LineString(points).buffer(HALF, cap_style="square", join_style="mitre", mitre_limit=3)


def shift(strokes, dx):
    return [[[x + dx, y] for x, y in s] for s in strokes]


def letter(base, marks=(), headline=True):
    """strokes that live between baseline and headline are clipped to that band, so
    square caps don't poke out. anything reaching outside it (the voicing spark,
    schwa bar, double line) is left alone."""
    band = box(-10, -HALF, 20, HEADLINE_Y + HALF)
    parts = []
    for s in shift(base, LETTER_SHIFT):
        shape = stroke_shape(s)
        inside = all(0 <= y <= HEADLINE_Y for _, y in s)
        parts.append(shape.intersection(band) if headline and inside else shape)
    if headline:
        parts.append(box(0, HEADLINE_Y - HALF, LETTER_ADVANCE, HEADLINE_Y + HALF))
    body = unary_union(parts)
    extra = [stroke_shape(s) for s in shift(marks, LETTER_SHIFT)]
    return unary_union([body] + extra), LETTER_ADVANCE


def free(strokes, bearing=0.6):
    """digits and punctuation: no headline, width from their own bounds."""
    if not strokes:
        return None, 3.0
    shape = unary_union([stroke_shape(s) for s in strokes])
    minx, _, maxx, _ = shape.bounds
    shape = translate(shape, bearing - minx, 0)
    return shape, (maxx - minx) + 2 * bearing


def path_d(shape):
    if shape is None or shape.is_empty:
        return ""
    polys = [shape] if shape.geom_type == "Polygon" else list(shape.geoms)
    parts = []
    for poly in polys:
        poly = orient(poly, sign=-1.0)  # clockwise outer, counter-clockwise holes (truetype)
        for ring in [poly.exterior] + list(poly.interiors):
            pts = list(ring.coords)[:-1]
            parts.append("M" + " L".join(f"{x * UNIT:.0f} {ASCENT - y * UNIT:.0f}" for x, y in pts) + " Z")
    return " ".join(parts)


def write(name, shape, advance, meta):
    w = round(advance * UNIT)
    h = ASCENT - DESCENT
    attrs = " ".join(f'data-{k}="{v}"' for k, v in meta.items())
    svg = (f'<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" viewBox="0 0 {w} {h}" '
           f'data-advance="{w}" {attrs}>\n'
           f'  <path d="{path_d(shape)}" fill="#000"/>\n</svg>\n')
    (OUT / f"{name}.svg").write_text(svg, encoding="utf-8")


def main():
    shutil.rmtree(OUT, ignore_errors=True)
    OUT.mkdir()
    marks = data["marks"]
    schwa, double = marks["schwa_bar"]["strokes"], marks["double_line"]["strokes"]

    for g in data["consonants"]:
        base = f'uni{g["cp"]}'
        write(base, *letter(g["strokes"]), {"unicode": g["cp"], "id": g["id"]})
        write(f"{base}.schwa", *letter(g["strokes"], schwa), {"ligature": f'{g["id"]}+ë'})
        write(f"{base}.double", *letter(g["strokes"], double), {"ligature": f'{g["id"]}+{g["id"]}'})
        write(f"{base}.double.schwa", *letter(g["strokes"], double + schwa),
              {"ligature": f'{g["id"]}+{g["id"]}+ë'})
    for g in data["vowels"]:
        write(f'uni{g["cp"]}', *letter(g["strokes"]), {"unicode": g["cp"], "id": g["id"]})
    for g in data["digits"]:
        cp = f'{ord(g["id"]):04X}'
        write(f"uni{cp}", *free(g["strokes"]), {"unicode": cp, "id": g["id"]})
    for g in data["punctuation"]:
        cp = f'{ord(g["char"]):04X}'
        write(f"uni{cp}", *free(g["strokes"]), {"unicode": cp, "id": g["id"]})
    print(f"{len(list(OUT.glob('*.svg')))} glyphs -> glyphs/")


if __name__ == "__main__":
    main()
