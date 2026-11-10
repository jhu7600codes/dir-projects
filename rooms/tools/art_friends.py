## the six entities of the "more friends" modifier, our own designs on the same mechanics:
##   A-45  (flies through once, hide)          charcoal smear face with speed streaks
##   A-85  (flies through, rebounds)           green face with afterimage mouths
##   A-30  (don't look at it)                  a purple knot of scribble full of eyes
##   A-130 (look at it when it whispers)       a small crumpled grey face with pinhole eyes
##   A-180 (blind, listens)                    a tall gaunt grey thing with no eyes
##   A-300 (the chase)                         black ink figure with one camera-lens eye,
##                                             plus the lens eyes that open on the walls
from PIL import Image, ImageDraw, ImageFilter, ImageChops
import random, math, sys
OUT = sys.argv[1]
S = 3

def sup(w, h):
    return Image.new("RGBA", (w * S, h * S), (0, 0, 0, 0))

def down(im, w, h):
    return im.resize((w, h), Image.LANCZOS)

def glow_under(img, color, blur, strength=1.0):
    a = img.split()[3]
    g = Image.new("RGBA", img.size, color + (0,))
    g.putalpha(a.filter(ImageFilter.GaussianBlur(blur)).point(lambda v: int(min(255, v * strength))))
    return Image.alpha_composite(g, img)

# ---- A-45: a charcoal smear face rushing at you ----
def a45():
    W = 420
    rnd = random.Random(45)
    img = sup(W, W)
    d = ImageDraw.Draw(img)
    # smeared head: overlapping dark blobs dragged to the left
    for i in range(14):
        ox = -i * 9
        a = max(30, 230 - i * 15)
        d.ellipse([(110 + ox) * S, 90 * S, (330 + ox) * S, 330 * S], fill=(26, 26, 30, a))
    d.ellipse([110 * S, 90 * S, 330 * S, 330 * S], fill=(30, 30, 34, 255))
    # hollow white eyes, stretched sideways
    for cx in (175, 265):
        d.ellipse([(cx - 30) * S, 160 * S, (cx + 30) * S, 196 * S], fill=(240, 240, 235, 255))
        d.ellipse([(cx - 8) * S, 170 * S, (cx + 8) * S, 186 * S], fill=(0, 0, 0, 255))
    # a long open mouth with a lot of small teeth
    d.ellipse([150 * S, 230 * S, 290 * S, 290 * S], fill=(0, 0, 0, 255))
    for i in range(12):
        x = 160 + i * 11
        d.polygon([(x * S, 238 * S), ((x + 8) * S, 238 * S), ((x + 4) * S, 252 * S)], fill=(230, 228, 220, 255))
        d.polygon([(x * S, 282 * S), ((x + 8) * S, 282 * S), ((x + 4) * S, 268 * S)], fill=(230, 228, 220, 255))
    img = down(img, W, W)
    # speed streaks
    st = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    sd = ImageDraw.Draw(st)
    for _ in range(40):
        y = rnd.randint(100, 320)
        x = rnd.randint(0, 150)
        sd.line([(x, y), (x + rnd.randint(40, 140), y)], fill=(70, 70, 80, rnd.randint(60, 160)), width=rnd.randint(1, 4))
    out = Image.alpha_composite(st.filter(ImageFilter.GaussianBlur(1)), img)
    return glow_under(out, (110, 110, 140), 14, 0.7)

# ---- A-85: green, with afterimages of its mouth from every rebound ----
def a85():
    W = 420
    rnd = random.Random(85)
    img = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    def face(ox, alpha):
        f = sup(W, W)
        d = ImageDraw.Draw(f)
        d.ellipse([(120 + ox) * S, 80 * S, (300 + ox) * S, 330 * S], fill=(20, 60, 30, alpha))
        for cx in (178, 242):
            d.polygon([((cx + ox - 26) * S, 175 * S), ((cx + ox) * S, 150 * S), ((cx + ox + 26) * S, 175 * S), ((cx + ox) * S, 190 * S)], fill=(200, 255, 180, alpha))
        d.rectangle([(150 + ox) * S, 238 * S, (270 + ox) * S, 262 * S], fill=(0, 0, 0, alpha))
        for i in range(10):
            x = 154 + ox + i * 12
            d.line([(x * S, 238 * S), (x * S, 262 * S)], fill=(160, 255, 150, alpha), width=3 * S)
        return down(f, W, W)
    for ox, a in ((-70, 50), (60, 70), (-35, 100), (30, 130)):
        img.alpha_composite(face(ox, a))
    img.alpha_composite(face(0, 255))
    # green static speckles
    px = img.load()
    for _ in range(2500):
        x, y = rnd.randrange(W), rnd.randrange(W)
        if px[x, y][3] > 60:
            px[x, y] = (80 + rnd.randint(0, 120), 255, 90, px[x, y][3])
    return glow_under(img, (60, 255, 100), 18, 0.9)

# ---- A-30: a knot of purple scribble full of eyes ----
def a30():
    W = 360
    rnd = random.Random(30)
    big = sup(W, W)
    d = ImageDraw.Draw(big)
    for _ in range(26):
        pts = []
        a0 = rnd.uniform(0, math.tau)
        rr = rnd.uniform(60, 140)
        for i in range(40):
            a = a0 + i / 39 * rnd.uniform(3, 6)
            k = rr * rnd.uniform(0.85, 1.1)
            pts.append(((180 + math.cos(a) * k) * S, (180 + math.sin(a) * k * 0.9) * S))
        d.line(pts, fill=(110, 40, 170, 255), width=rnd.randint(4, 9) * S, joint="curve")
    d.ellipse([70 * S, 80 * S, 290 * S, 280 * S], fill=(40, 10, 60, 255))
    # eyes all over it, looking different ways
    for _ in range(19):
        a = rnd.uniform(0, math.tau)
        r = rnd.uniform(0, 100)
        cx, cy = 180 + math.cos(a) * r, 180 + math.sin(a) * r * 0.85
        s = rnd.uniform(11, 20)
        d.ellipse([(cx - s) * S, (cy - s * 0.6) * S, (cx + s) * S, (cy + s * 0.6) * S], fill=(245, 235, 255, 255))
        px, py = cx + rnd.uniform(-s * 0.4, s * 0.4), cy + rnd.uniform(-s * 0.2, s * 0.2)
        d.ellipse([(px - s * 0.38) * S, (py - s * 0.38) * S, (px + s * 0.38) * S, (py + s * 0.38) * S], fill=(150, 60, 220, 255))
        d.ellipse([(px - s * 0.18) * S, (py - s * 0.18) * S, (px + s * 0.18) * S, (py + s * 0.18) * S], fill=(0, 0, 0, 255))
    img = down(big, W, W)
    return glow_under(img, (170, 80, 255), 20, 0.9)

# ---- A-130: a small crumpled grey face with pinhole eyes and a jagged grin ----
def a130():
    W = 300
    rnd = random.Random(130)
    big = sup(W, W)
    d = ImageDraw.Draw(big)
    pts = []
    for i in range(30):
        a = i / 30 * math.tau
        r = 110 * rnd.uniform(0.82, 1.0)
        pts.append(((150 + math.cos(a) * r) * S, (150 + math.sin(a) * r) * S))
    d.polygon(pts, fill=(150, 148, 142, 255))
    # crumple lines
    for _ in range(14):
        x, y = rnd.uniform(60, 240), rnd.uniform(60, 240)
        a = rnd.uniform(0, math.tau)
        ln = rnd.uniform(20, 50)
        d.line([(x * S, y * S), ((x + math.cos(a) * ln) * S, (y + math.sin(a) * ln) * S)], fill=(110, 108, 104, 255), width=2 * S)
    # pinhole eyes in big dark sockets
    for cx in (112, 188):
        d.ellipse([(cx - 26) * S, 100 * S, (cx + 26) * S, 148 * S], fill=(20, 18, 18, 255))
        d.ellipse([(cx - 3) * S, 121 * S, (cx + 3) * S, 127 * S], fill=(255, 255, 255, 255))
    # jagged grin
    top = [(84 + i * 12, 186 + (0 if i % 2 else 10)) for i in range(12)]
    bot = [(84 + i * 12, 214 - (0 if i % 2 else 10)) for i in range(12)]
    d.polygon([(x * S, y * S) for x, y in top + bot[::-1]], fill=(10, 8, 8, 255))
    img = down(big, W, W)
    return img

# ---- A-180: tall, gaunt, grey, no eyes, a stitched mouth ----
def a180():
    W, H = 300, 620
    big = sup(W, H)
    d = ImageDraw.Draw(big)
    grey = (120, 112, 104, 255)
    dark = (70, 64, 58, 255)
    # legs and long arms
    for x in (122, 178):
        d.line([(x * S, 380 * S), ((x + (x - 150) * 0.3) * S, 610 * S)], fill=grey, width=22 * S)
    for side in (-1, 1):
        d.line([((150 + side * 50) * S, 200 * S), ((150 + side * 80) * S, 340 * S), ((150 + side * 70) * S, 470 * S)], fill=grey, width=16 * S, joint="curve")
        for f in range(4):
            fx = 150 + side * (62 + f * 6)
            d.line([(fx * S, 465 * S), ((fx + side * 2) * S, 505 * S)], fill=grey, width=5 * S)
    # torso with ribs
    d.rounded_rectangle([96 * S, 180 * S, 204 * S, 400 * S], radius=40 * S, fill=grey)
    for i in range(6):
        y = 220 + i * 24
        d.arc([108 * S, y * S, 192 * S, (y + 30) * S], 200, 340, fill=dark, width=4 * S)
    # the head: long, no eyes at all, just a stitched-shut mouth
    d.ellipse([100 * S, 20 * S, 200 * S, 190 * S], fill=grey)
    d.line([(118 * S, 140 * S), (182 * S, 140 * S)], fill=(40, 30, 26, 255), width=6 * S)
    for i in range(7):
        x = 122 + i * 9
        d.line([(x * S, 128 * S), (x * S, 152 * S)], fill=(40, 30, 26, 255), width=3 * S)
    # a faint dent where eyes should be
    d.ellipse([118 * S, 70 * S, 182 * S, 100 * S], fill=dark)
    return down(big, W, H)

# ---- A-300: a black ink figure with one camera-lens eye ----
def lens(d, cx, cy, r):
    d.ellipse([(cx - r) * S, (cy - r) * S, (cx + r) * S, (cy + r) * S], fill=(30, 30, 34, 255))
    d.ellipse([(cx - r * 0.8) * S, (cy - r * 0.8) * S, (cx + r * 0.8) * S, (cy + r * 0.8) * S], fill=(10, 10, 14, 255))
    d.ellipse([(cx - r * 0.5) * S, (cy - r * 0.5) * S, (cx + r * 0.5) * S, (cy + r * 0.5) * S], fill=(140, 20, 30, 255))
    d.ellipse([(cx - r * 0.22) * S, (cy - r * 0.22) * S, (cx + r * 0.22) * S, (cy + r * 0.22) * S], fill=(255, 60, 60, 255))
    d.ellipse([(cx - r * 0.45) * S, (cy - r * 0.6) * S, (cx - r * 0.2) * S, (cy - r * 0.35) * S], fill=(255, 255, 255, 200))

def a300():
    W, H = 340, 600
    rnd = random.Random(300)
    big = sup(W, H)
    d = ImageDraw.Draw(big)
    ink = (8, 8, 10, 255)
    d.ellipse([110 * S, 30 * S, 230 * S, 170 * S], fill=ink)
    d.polygon([(100 * S, 150 * S), (240 * S, 150 * S), (270 * S, 520 * S), (70 * S, 520 * S)], fill=ink)
    # drips running off it
    for _ in range(16):
        x = rnd.uniform(80, 260)
        d.line([(x * S, 480 * S), (x * S, rnd.uniform(520, 595) * S)], fill=ink, width=rnd.randint(6, 14) * S)
        d.ellipse([(x - 7) * S, 585 * S, (x + 7) * S, 597 * S], fill=ink)
    for side in (-1, 1):
        d.line([((170 + side * 60) * S, 190 * S), ((170 + side * 110) * S, 380 * S)], fill=ink, width=26 * S)
    lens(d, 170, 100, 38)
    img = down(big, W, H)
    return glow_under(img, (120, 0, 0), 16, 0.5)

def a300_eye():
    W = 160
    big = sup(W, W)
    d = ImageDraw.Draw(big)
    d.ellipse([6 * S, 30 * S, 154 * S, 130 * S], fill=(8, 8, 10, 255))
    lens(d, 80, 80, 40)
    return down(big, W, W)

arts = {
    "a45": a45(), "a85": a85(), "a30": a30(), "a130": a130(),
    "a180": a180(), "a300": a300(), "a300_eye": a300_eye(),
}
for k, im in arts.items():
    im.save(f"{OUT}/{k}.png")
# a contact sheet
cell = 300
sheet = Image.new("RGBA", (cell * 4, cell * 2 + 60), (14, 14, 16, 255))
for i, (k, im) in enumerate(arts.items()):
    t = im.copy()
    t.thumbnail((cell - 20, cell - 20), Image.LANCZOS)
    x = (i % 4) * cell + (cell - t.width) // 2
    y = (i // 4) * (cell + 30) + (cell - t.height) // 2
    sheet.alpha_composite(t, (x, y))
    ImageDraw.Draw(sheet).text(((i % 4) * cell + 10, (i // 4) * (cell + 30) + cell - 4), k.upper().replace("_", " "), fill=(220, 220, 220, 255))
sheet.save(f"{OUT}/friends_sheet.png")
