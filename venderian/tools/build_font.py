"""builds venderian.ttf from glyphs/*.svg (made by make_glyphs.py), with the
ligatures from docs/venderian.md as an opentype liga feature:
  C + C      -> C.double
  C + ë      -> C.schwa
  C.double + ë -> C.double.schwa
the font goes to app/src/main/res/font/venderian.ttf."""
import json
import re
from pathlib import Path

from fontTools.feaLib.builder import addOpenTypeFeaturesFromString
from fontTools.fontBuilder import FontBuilder
from fontTools.pens.recordingPen import RecordingPen
from fontTools.pens.transformPen import TransformPen
from fontTools.pens.ttGlyphPen import TTGlyphPen
from fontTools.svgLib.path import SVGPath

ROOT = Path(__file__).resolve().parent.parent
GLYPHS = ROOT / "glyphs"
OUT = ROOT / "app/src/main/res/font/venderian.ttf"
data = json.loads((ROOT / "docs/alphabet.json").read_text(encoding="utf-8"))

UPM, ASCENT, DESCENT = 1000, 900, -250
SCHWA_CP = next(v["cp"] for v in data["vowels"] if v["id"] == "ë")


def svg_glyph(path):
    text = path.read_text(encoding="utf-8")
    advance = int(re.search(r'data-advance="(\d+)"', text).group(1))
    unicode = re.search(r'data-unicode="([0-9A-F]+)"', text)
    rec = RecordingPen()
    # svg y runs down from the ascent line, font y runs up from the baseline
    SVGPath.fromstring(text.encode()).draw(TransformPen(rec, (1, 0, 0, -1, 0, ASCENT)))
    pen = TTGlyphPen(None)
    rec.replay(pen)
    return pen.glyph(), advance, int(unicode.group(1), 16) if unicode else None


def notdef():
    pen = TTGlyphPen(None)
    for (x0, y0, x1, y1), cw in [((50, 0, 450, 600), True), ((100, 50, 400, 550), False)]:
        pts = [(x0, y0), (x0, y1), (x1, y1), (x1, y0)]
        if not cw:
            pts.reverse()
        pen.moveTo(pts[0])
        for p in pts[1:]:
            pen.lineTo(p)
        pen.closePath()
    return pen.glyph(), 500


def features():
    lines = ["languagesystem DFLT dflt;", "", "lookup double {"]
    cons = [f'uni{c["cp"]}' for c in data["consonants"]]
    for g in cons:
        lines.append(f"  sub {g} {g} by {g}.double;")
    lines += ["} double;", "", "lookup schwa {"]
    for g in cons:
        lines.append(f"  sub {g}.double uni{SCHWA_CP} by {g}.double.schwa;")
        lines.append(f"  sub {g} uni{SCHWA_CP} by {g}.schwa;")
    lines += ["} schwa;", "", "feature liga {", "  lookup double;", "  lookup schwa;", "} liga;", ""]
    return "\n".join(lines)


def main():
    order = [".notdef"]
    glyphs, metrics, cmap = {}, {}, {}
    glyphs[".notdef"], adv = notdef()
    metrics[".notdef"] = (adv, 50)
    for path in sorted(GLYPHS.glob("*.svg")):
        name = path.stem
        glyph, advance, cp = svg_glyph(path)
        glyph.recalcBounds(None)
        order.append(name)
        glyphs[name] = glyph
        metrics[name] = (advance, getattr(glyph, "xMin", 0))
        if cp is not None:
            cmap[cp] = name

    fb = FontBuilder(UPM, isTTF=True)
    fb.setupGlyphOrder(order)
    fb.setupCharacterMap(cmap)
    fb.setupGlyf(glyphs)
    fb.setupHorizontalMetrics(metrics)
    fb.setupHorizontalHeader(ascent=ASCENT, descent=DESCENT)
    fb.setupNameTable({"familyName": "Venderian", "styleName": "Regular",
                       "uniqueFontIdentifier": "Venderian-Regular", "fullName": "Venderian Regular",
                       "psName": "Venderian-Regular", "version": "Version 1.000"})
    fb.setupOS2(sTypoAscender=ASCENT, sTypoDescender=DESCENT, sTypoLineGap=150,
                usWinAscent=ASCENT, usWinDescent=-DESCENT, sxHeight=300, sCapHeight=635,
                fsSelection=0x40, achVendID="VNDR")
    fb.setupPost()
    fb.setupHead(unitsPerEm=UPM)
    addOpenTypeFeaturesFromString(fb.font, features())
    OUT.parent.mkdir(parents=True, exist_ok=True)
    fb.save(OUT)
    print(f"{len(order)} glyphs, {len(cmap)} mapped -> {OUT.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
