## A-25 "warp", redrawn after the portal in the game: a purple spiral with a bright rim.
## more detail (sharper arms, sparkles pulled into it). the tweak: a pale eye in the middle
from PIL import Image, ImageDraw, ImageFilter
import random, math, sys
N = 512

def old():
    n = 128
    img = Image.new("RGBA", (n, n))
    px = img.load()
    for y in range(n):
        for x in range(n):
            dx, dy = (x - n / 2) / (n / 2), (y - n / 2) / (n / 2)
            r = math.hypot(dx, dy)
            if r > 1:
                px[x, y] = (0, 0, 0, 0); continue
            a = math.atan2(dy, dx)
            arm = 0.5 + 0.5 * math.sin(a * 3 + r * 14)
            k = arm * r
            c = [0.04 + (0.6 - 0.04) * k, 0 + 0.25 * k, 0.08 + (1 - 0.08) * k]
            if r > 0.86:
                t = (r - 0.86) / 0.14
                c = [c[0] + (0.85 - c[0]) * t, c[1] + (0.7 - c[1]) * t, c[2] + (1 - c[2]) * t]
            px[x, y] = tuple(int(v * 255) for v in c) + (int(min(1, max(0, (1 - r) * 8)) * 255),)
    return img.resize((N, N), Image.BILINEAR)

def edge(a):
    return 1.0 - 0.06 * (0.5 + 0.5 * math.sin(a * 9)) - 0.035 * (0.5 + 0.5 * math.sin(a * 23 + 1.3)) - 0.02 * math.sin(a * 5 + 0.4) ** 2

def new():
    img = Image.new("RGBA", (N, N))
    px = img.load()
    for y in range(N):
        for x in range(N):
            dx, dy = (x - N / 2) / (N / 2), (y - N / 2) / (N / 2)
            a = math.atan2(dy, dx)
            r = math.hypot(dx, dy) / (edge(a) * 0.93)
            if r > 1:
                px[x, y] = (0, 0, 0, 0); continue
            arm = 0.5 + 0.5 * math.sin(a * 3 + r * 16 - r * r * 4)
            arm = arm ** 3  # thinner, sharper arms
            k = arm * (0.3 + 0.7 * r)
            c = [0.03 + 0.7 * k, 0.0 + 0.3 * k, 0.07 + 0.95 * k]
            # the inner dark, like a throat
            c = [v * min(1, r * 2.2) for v in c]
            if r > 0.84:
                t = (r - 0.84) / 0.16
                c = [c[0] + (0.9 - c[0]) * t, c[1] + (0.75 - c[1]) * t, c[2] + (1 - c[2]) * t]
            alpha = min(1, max(0, (1 - r) * 10))
            px[x, y] = tuple(int(min(1, v) * 255) for v in c) + (int(alpha * 255),)
    # sparkles being pulled in along the arms
    rnd = random.Random(25)
    sp = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    d = ImageDraw.Draw(sp)
    for _ in range(140):
        r = rnd.uniform(0.25, 0.95)
        a = rnd.uniform(0, math.tau)
        x, y = N / 2 + math.cos(a) * r * N / 2, N / 2 + math.sin(a) * r * N / 2
        ln = 6 + r * 14
        ta = a + math.pi / 2 + 0.6
        d.line([(x, y), (x + math.cos(ta) * ln, y + math.sin(ta) * ln)], fill=(230, 210, 255, rnd.randint(120, 230)), width=2)
    img.alpha_composite(sp.filter(ImageFilter.GaussianBlur(0.8)))
    # outer glow
    glow = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    rim = [(N / 2 + math.cos(i / 180 * math.pi) * edge(i / 180 * math.pi) * (N / 2 - 24), N / 2 + math.sin(i / 180 * math.pi) * edge(i / 180 * math.pi) * (N / 2 - 24)) for i in range(361)]
    ImageDraw.Draw(glow).line(rim, fill=(190, 140, 255, 255), width=14)
    img = Image.alpha_composite(glow.filter(ImageFilter.GaussianBlur(10)), img)
    # the tweak: a pale eye in the middle, looking straight out
    eye = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    e = ImageDraw.Draw(eye)
    cx = cy = N / 2
    e.ellipse([cx - 46, cy - 22, cx + 46, cy + 22], fill=(235, 225, 245, 255))
    e.ellipse([cx - 17, cy - 17, cx + 17, cy + 17], fill=(120, 40, 200, 255))
    e.ellipse([cx - 8, cy - 8, cx + 8, cy + 8], fill=(5, 0, 10, 255))
    e.ellipse([cx - 4, cy - 12, cx + 2, cy - 6], fill=(255, 255, 255, 255))
    lids = Image.new("L", (N, N), 0)
    ImageDraw.Draw(lids).ellipse([cx - 46, cy - 22, cx + 46, cy + 22], fill=255)
    eye.putalpha(Image.composite(eye.split()[3], Image.new("L", (N, N), 0), lids.filter(ImageFilter.GaussianBlur(1))))
    img.alpha_composite(Image.new("RGBA", (N, N), (0, 0, 0, 0)))
    eg = Image.new("RGBA", (N, N), (200, 150, 255, 0))
    eg.putalpha(lids.filter(ImageFilter.GaussianBlur(16)).point(lambda v: v // 2))
    eye_img = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    eye_img.alpha_composite(eg)
    eye_img.alpha_composite(eye)
    eye_img.crop((N // 2 - 96, N // 2 - 96, N // 2 + 96, N // 2 + 96)).save(sys.argv[1] + "/a25_eye.png")
    return img

o, n = old(), new()
n.save(sys.argv[1] + "/a25.png")
out = Image.new("RGBA", (N * 2 + 30, N), (12, 12, 14, 255))
out.alpha_composite(o, (0, 0))
out.alpha_composite(n, (N + 30, 0))
with_eye = n.copy()
ey = Image.open(sys.argv[1] + "/a25_eye.png")
with_eye.alpha_composite(ey, (N // 2 - 96, N // 2 - 96))
out = Image.new("RGBA", (N * 2 + 30, N), (12, 12, 14, 255))
out.alpha_composite(n, (0, 0))
out.alpha_composite(with_eye, (N + 30, 0))
out.resize((out.width * 2 // 3, out.height * 2 // 3), Image.LANCZOS).save(sys.argv[1] + "/a25_compare.png")
