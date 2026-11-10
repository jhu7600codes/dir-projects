## A-120, redrawn after the one in the game: a glowing white scribbled smiley with coloured
## dead pixels. the tweak: a bright pinprick pupil staring out of each scribbled eye
from PIL import Image, ImageDraw, ImageFilter
import random, math, sys
W = 350
def draw(seed):
    global rnd, d
    rnd = random.Random(seed)
    lines = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    d = ImageDraw.Draw(lines)

    def scribble_circle(cx, cy, rx, ry, loops=7):
        for _ in range(loops):
            pts = []
            a0 = rnd.uniform(0, math.tau)
            sweep = rnd.uniform(1.4, 2.2) * math.pi
            ox, oy = rnd.uniform(-12, 12), rnd.uniform(-8, 8)
            sc = rnd.uniform(0.45, 1.0)
            for i in range(30):
                a = a0 + sweep * i / 29
                k = sc * rnd.uniform(0.85, 1.1)
                pts.append((cx + ox + math.cos(a) * rx * k + rnd.uniform(-3, 3), cy + oy + math.sin(a) * ry * k + rnd.uniform(-3, 3)))
            d.line(pts, fill=(255, 255, 255, rnd.randint(170, 255)), width=rnd.choice((1, 2, 2, 3)))

    def scribble_smile():
        for _ in range(10):
            pts = []
            sag = rnd.uniform(52, 60)
            y0 = rnd.uniform(-5, 5)
            x0 = rnd.uniform(-8, 8)
            for i in range(30):
                t = i / 29
                x = 52 + x0 + t * 252
                y = 192 + y0 + math.sin(t * math.pi) * sag + rnd.uniform(-4, 4)
                # the ends hook up and back in
                if t < 0.08:
                    x += (0.08 - t) * 120
                    y -= (0.08 - t) * 260
                if t > 0.9:
                    x -= (t - 0.9) * 110
                    y -= (t - 0.9) * 230
                pts.append((x, y))
            s = rnd.randint(0, 10)
            e = rnd.randint(18, 30)
            d.line(pts[s:e], fill=(255, 255, 255, rnd.randint(150, 255)), width=rnd.choice((1, 1, 2)))

    scribble_circle(100, 118, 38, 26, 9)
    scribble_circle(245, 118, 38, 25, 9)
    scribble_smile()
    lines = lines.resize((W // 2, W // 2), Image.BOX).resize((W, W), Image.NEAREST)
    lines = lines.point(lambda v: v)  # keep
    # glow: blur copies under the sharp lines
    glow = lines.filter(ImageFilter.GaussianBlur(5))
    glow2 = lines.filter(ImageFilter.GaussianBlur(14))
    img = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    img.alpha_composite(glow2)
    img.alpha_composite(glow)
    img.alpha_composite(lines)
    # the tweak: pinprick pupils, bright and staring
    for cx, cy in ((100, 120), (245, 120)):
        p = Image.new("RGBA", (W, W), (0, 0, 0, 0))
        ImageDraw.Draw(p).ellipse([cx - 6, cy - 6, cx + 6, cy + 6], fill=(0, 0, 0, 255))
        img.alpha_composite(p)
        s = Image.new("RGBA", (W, W), (0, 0, 0, 0))
        ImageDraw.Draw(s).ellipse([cx - 2, cy - 2, cx + 2, cy + 2], fill=(255, 255, 255, 255))
        img.alpha_composite(s.filter(ImageFilter.GaussianBlur(3)))
        img.alpha_composite(s)
    # coloured dead pixels scattered on the lines
    px = img.load()
    for _ in range(520):
        x, y = rnd.randrange(W), rnd.randrange(W)
        if px[x, y][3] > 90:
            c = rnd.choice([(255, 40, 60), (60, 255, 120), (80, 120, 255), (255, 230, 60), (220, 80, 255)])
            for dx in range(2):
                for dy in range(2):
                    if x + dx < W and y + dy < W:
                        px[x + dx, y + dy] = c + (255,)
    return img

frames = [draw(seed) for seed in (120, 121, 122, 123)]  # frame 1 is the approved one
for i, f in enumerate(frames):
    f.save(f"{sys.argv[1]}/a120_{i + 1}.png")
prev = Image.new("RGBA", (W * 4 + 60, W), (12, 12, 14, 255))
for i, f in enumerate(frames):
    prev.alpha_composite(f, (i * (W + 20), 0))
prev.resize((prev.width * 2 // 3, prev.height * 2 // 3), Image.LANCZOS).save(f"{sys.argv[1]}/a120_frames_preview.png")
