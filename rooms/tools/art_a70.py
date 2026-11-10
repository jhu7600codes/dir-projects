## A-70, the locker mimic: the face pressed into a fake locker door from the inside.
## drawn the size of a door (0.8 x 1.9), meant to be laid over it very faintly
from PIL import Image, ImageDraw, ImageFilter
import random, math, sys
W, H = 256, 608
S = 3
rnd = random.Random(70)
big = Image.new("L", (W * S, H * S), 0)  # darkness map: 255 = darkest
d = ImageDraw.Draw(big)
def sc(v): return [x * S for x in v]
# the face: a long skull-ish shape bulging through the metal
d.ellipse(sc([28, 70, 228, 520]), fill=70)
# hollow eyes, set high, dragged down at the inner corners
for cx, tilt in ((88, 1), (168, -1)):
    d.polygon(sc([cx - 38, 175 - tilt * 6, cx + 34, 170 + tilt * 6, cx + 26, 236, cx - 28, 240]), fill=255)
# a long, thin grin right across, curling up at the ends
pts = []
for i in range(41):
    t = i / 40
    x = 40 + t * 176
    y = 392 + math.sin(t * math.pi) * 38 - (abs(t - 0.5) ** 3) * 160
    pts.append((x * S, y * S))
d.line(pts, fill=255, width=16 * S, joint="curve")
# teeth marks along the grin
for i in range(3, 38, 3):
    x, y = pts[i]
    d.line([(x, y - 9 * S), (x, y + 9 * S)], fill=120, width=3 * S)
# dents where it pushed against the door
for _ in range(9):
    x, y = rnd.uniform(30, 226), rnd.uniform(60, 560)
    r = rnd.uniform(6, 16)
    d.ellipse(sc([x - r, y - r * 0.7, x + r, y + r * 0.7]), fill=rnd.randint(90, 160))
img = big.resize((W, H), Image.LANCZOS).filter(ImageFilter.GaussianBlur(2.5))
# grime noise so it reads as stained metal, not a clean drawing
px = img.load()
for y in range(H):
    for x in range(W):
        v = px[x, y]
        if v:
            px[x, y] = max(0, min(255, int(v * rnd.uniform(0.8, 1.05))))
out = Image.new("RGBA", (W, H), (10, 10, 12, 0))
out.putalpha(img)
out.save(sys.argv[1] + "/a70_face.png")

# previews: a locker door with the face at the game's strength and at a strong strength
door = Image.new("RGBA", (W, H), (88, 92, 98, 255))
dd = ImageDraw.Draw(door)
for i in range(5):
    dd.rectangle([50, 50 + i * 18, 206, 56 + i * 18], fill=(20, 20, 22, 255))
def over(alpha):
    o = door.copy()
    f = out.copy()
    f.putalpha(f.split()[3].point(lambda v: int(v * alpha)))
    o.alpha_composite(f)
    return o
prev = Image.new("RGBA", (W * 3 + 40, H), (12, 12, 14, 255))
prev.alpha_composite(over(0.0), (0, 0))
prev.alpha_composite(over(0.18), (W + 20, 0))
prev.alpha_composite(over(0.75), (W * 2 + 40, 0))
prev.resize((prev.width // 2, prev.height // 2), Image.LANCZOS).save(sys.argv[1] + "/a70_preview.png")
