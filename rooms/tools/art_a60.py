## A-60, redrawn after the one in the game: a wide black mass in a red glowing static
## mist, two slanted burning eyes, white slashes for a grin. three flicker frames
from PIL import Image, ImageDraw, ImageFilter
import random, math, sys
W = 420

def blob_mask(rnd, cx, cy, rx, ry, wob, n=40, blur=4):
    m = Image.new("L", (W, W), 0)
    pts = []
    for i in range(n):
        a = i / n * math.tau
        k = 1 + rnd.uniform(-wob, wob)
        pts.append((cx + math.cos(a) * rx * k, cy + math.sin(a) * ry * k))
    ImageDraw.Draw(m).polygon(pts, fill=255)
    return m.filter(ImageFilter.GaussianBlur(blur))

def speckle(rnd, mask, density, colors, size=(1, 3)):
    layer = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    mp = mask.load()
    for _ in range(int(W * W * density)):
        x, y = rnd.randrange(W), rnd.randrange(W)
        v = mp[x, y]
        if v > 20 and rnd.random() < v / 255:
            s = rnd.randint(*size)
            c = rnd.choice(colors)
            d.rectangle([x, y, x + s, y + s], fill=c + (int(v * rnd.uniform(0.5, 1.0)),))
    return layer

def eye(img, cx, cy, tilt, core, halo, scale=1.0):
    g = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    d = ImageDraw.Draw(g)
    w, h = 44 * scale, 12 * scale
    pts = [(cx - w, cy + tilt * 2), (cx - w * 0.1, cy - h), (cx + w, cy - tilt * 2), (cx + w * 0.3, cy + h * 0.6)]
    d.polygon(pts, fill=halo + (255,))
    glow = g.filter(ImageFilter.GaussianBlur(16))
    img.alpha_composite(glow)
    img.alpha_composite(glow)
    img.alpha_composite(g.filter(ImageFilter.GaussianBlur(2)))
    c = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    cd = ImageDraw.Draw(c)
    cd.polygon([(cx - w * 0.6, cy + tilt), (cx, cy - h * 0.5), (cx + w * 0.6, cy - tilt), (cx + w * 0.1, cy + h * 0.35)], fill=core + (255,))
    img.alpha_composite(c.filter(ImageFilter.GaussianBlur(1.5)))

def slashes(img, rnd, color, count):
    d = ImageDraw.Draw(img)
    for i in range(count):
        x = 135 + i * (150 / max(1, count - 1)) + rnd.randint(-5, 5)
        t = (i / max(1, count - 1)) * 2 - 1
        y = 262 - (1 - t * t) * 22 + rnd.randint(-5, 5)
        ln = rnd.randint(30, 44)
        ang = math.radians(rnd.uniform(-60, -40))
        x2, y2 = x + math.cos(ang) * ln, y + math.sin(ang) * ln
        d.line([(x, y), (x2, y2)], fill=color + (255,), width=rnd.randint(8, 11))

def frame(seed, style):
    rnd = random.Random(seed)
    img = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    cx, cy = 210, 205
    mist = blob_mask(rnd, cx, cy, 150, 92, 0.22, blur=10)
    # two ear-like flares at the top corners, like the original
    for sx in (-1, 1):
        ImageDraw.Draw(mist).ellipse([cx + sx * 135 - 28, cy - 100, cx + sx * 135 + 28, cy - 48], fill=200)
    mist = mist.filter(ImageFilter.GaussianBlur(6))
    inner = blob_mask(rnd, cx, cy + 15, 120, 62, 0.1, blur=18)
    from PIL import ImageChops
    mist = ImageChops.subtract(mist, inner.point(lambda v: int(v * 0.8)))
    if style == "red":
        cols = [(255, 60, 60), (255, 110, 100), (220, 30, 40)]
        img.alpha_composite(speckle(rnd, mist, 0.9, cols, (1, 4)))
        g = Image.new("RGBA", (W, W), (255, 40, 40, 0))
        g.putalpha(mist.filter(ImageFilter.GaussianBlur(14)).point(lambda v: int(v * 0.55)))
        img = Image.alpha_composite(g, img)
    elif style == "pink":
        cols = [(230, 120, 120), (200, 110, 120), (120, 200, 160), (110, 130, 230)]
        img.alpha_composite(speckle(rnd, mist, 0.8, cols, (1, 3)))
    else:
        cols = [(60, 60, 70), (90, 40, 40)]
        img.alpha_composite(speckle(rnd, mist, 0.3, cols, (1, 2)))
    # the black mass: three lumps side by side, glossy
    body = Image.new("L", (W, W), 0)
    for ox, rx, ry in ((-70, 70, 62), (10, 66, 70), (85, 62, 74)):
        body = Image.fromarray if False else body
        m = blob_mask(rnd, cx + ox, cy + 18, rx, ry, 0.12, blur=3)
        body = Image.composite(m, body, m)
    black = Image.new("RGBA", (W, W), (6, 4, 6, 255))
    black.putalpha(body)
    img.alpha_composite(black)
    # a bit of shine on the lumps
    shine = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    sd = ImageDraw.Draw(shine)
    for ox in (-70, 10, 85):
        sd.arc([cx + ox - 50, cy - 40, cx + ox + 40, cy + 70], 200, 260, fill=(120, 90, 100, 140), width=4)
    img.alpha_composite(shine.filter(ImageFilter.GaussianBlur(1.5)))
    if style == "dark":
        eye(img, cx - 62, cy - 30, 5, (255, 255, 240), (255, 40, 20), 1.0)
        eye(img, cx + 55, cy - 34, -4, (255, 230, 220), (255, 30, 20), 0.85)
        slashes(img, rnd, (240, 240, 240), 6)
    elif style == "red":
        eye(img, cx - 62, cy - 28, 6, (255, 250, 230), (255, 70, 40), 1.1)
        eye(img, cx + 50, cy - 32, -5, (255, 220, 200), (255, 50, 40), 0.9)
        slashes(img, rnd, (255, 245, 245), 5)
    else:
        eye(img, cx - 60, cy - 28, 4, (255, 210, 210), (200, 60, 80), 0.8)
        eye(img, cx + 52, cy - 30, -3, (255, 210, 210), (200, 60, 80), 0.7)
    # glitch: a few slices shifted sideways
    for _ in range(rnd.randint(1, 3)):
        y0 = rnd.randint(90, 320)
        h = rnd.randint(2, 5)
        sl = img.crop((0, y0, W, y0 + h))
        img.paste((0, 0, 0, 0), (0, y0, W, y0 + h))
        img.paste(sl, (rnd.randint(-14, 14), y0), sl)
    return img

frames = [frame(11, "red"), frame(22, "pink"), frame(33, "dark")]
for i, f in enumerate(frames):
    f.save(f"{sys.argv[1]}/a60_{i + 1}.png")
gap = 20
prev = Image.new("RGBA", (W * 3 + gap * 2, W), (12, 12, 14, 255))
for i, f in enumerate(frames):
    prev.alpha_composite(f, (i * (W + gap), 0))
prev.resize((prev.width * 3 // 4, prev.height * 3 // 4), Image.LANCZOS).save(f"{sys.argv[1]}/a60_preview.png")
