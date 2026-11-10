## A-200 "the happy scribble", redrawn after the one in the game: thick white marker
## scribbles, two balled-up eyes and a big looping grin. the tweak: a little pink blush
## scribbled on each cheek
from PIL import Image, ImageDraw
import random, math, sys
W = 700
S = 3  # supersample for smooth marker lines

def draw(seed):
    rnd = random.Random(seed)
    big = Image.new("RGBA", (W * S, W * S), (0, 0, 0, 0))
    d = ImageDraw.Draw(big)

    def stroke(pts, color, width):
        pts = [(x * S, y * S) for x, y in pts]
        d.line(pts, fill=color, width=width * S, joint="curve")
        r = width * S / 2
        for x, y in (pts[0], pts[-1]):
            d.ellipse([x - r, y - r, x + r, y + r], fill=color)

    def ball(cx, cy, rx, ry, loops, color=(255, 255, 255, 255), width=11):
        for _ in range(loops):
            ox, oy = rnd.uniform(-0.3, 0.3) * rx, rnd.uniform(-0.3, 0.3) * ry
            sc = rnd.uniform(0.35, 0.95)
            a0 = rnd.uniform(0, math.tau)
            sweep = rnd.uniform(1.2, 2.4) * math.pi
            pts = []
            for i in range(60):
                a = a0 + sweep * i / 59
                k = sc * (1 + 0.04 * math.sin(i * 0.7 + seed))
                pts.append((cx + ox + math.cos(a) * rx * k, cy + oy + math.sin(a) * ry * k))
            stroke(pts, color, width)

    def grin():
        for _ in range(10):
            pts = []
            sag = rnd.uniform(190, 260)
            yl = rnd.uniform(-10, 60)   # each stroke starts and ends at its own height,
            yr = rnd.uniform(-30, 40)   # so they cross like quick marker loops
            skew = rnd.uniform(-0.15, 0.15)
            for i in range(80):
                t = i / 79
                tt = min(1.0, max(0.0, t + skew * math.sin(t * math.pi)))
                x = 28 + t * 650
                y = 335 + yl * (1 - t) + yr * t + math.sin(tt * math.pi) ** 0.8 * sag
                pts.append((x, y))
            s = rnd.randint(0, 6)
            e = rnd.randint(70, 80)
            stroke(pts[s:e], (255, 255, 255, 255), 10)
        # the corners curl up into the cheeks
        for side in (0, 1):
            x = 28 if side == 0 else 680
            stroke([(x + (10 if side == 0 else -10), 380), (x, 330), (x + (6 if side == 0 else -6), 300)], (255, 255, 255, 255), 10)

    ball(210, 240, 125, 92, 20)
    ball(520, 250, 112, 98, 19)
    # the tweak: pink blush scribbles under the eyes
    for cx in (95, 625):
        for _ in range(4):
            y = 345 + rnd.uniform(-12, 12)
            pts = [(cx - 32 + i * 8, y + (5 if i % 2 else -5) + rnd.uniform(-2, 2)) for i in range(9)]
            stroke(pts, (255, 140, 175, 230), 6)
    grin()
    return big.resize((W, W), Image.LANCZOS)

img = draw(200)
img.save(sys.argv[1] + "/a200_face.png")
ref = Image.open(sys.argv[1] + "/a200_ref.png").convert("RGBA")
h = ref.height
mine = img.resize((h, h), Image.LANCZOS)
row = Image.new("RGBA", (h * 2 + 20, h), (12, 12, 14, 255))
row.alpha_composite(mine)
light = Image.new("RGBA", (h, h), (236, 235, 229, 255))
light.alpha_composite(mine)
row.alpha_composite(light, (h + 20, 0))
out = Image.new("RGBA", (max(ref.width, row.width), h * 2 + 10), (12, 12, 14, 255))
out.alpha_composite(ref, (0, 0))
out.alpha_composite(row, (0, h + 10))
out.save(sys.argv[1] + "/a200_compare.png")
