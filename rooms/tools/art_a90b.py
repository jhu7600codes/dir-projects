## A-90B, redrawn after the one in the game: a grainy black-and-white face, a red HALT sign
## with a white hand, a green PROCEED sign with a pointing hand, and the tall attack face.
## the tweak: a small eye open in the palm of each hand
from PIL import Image, ImageDraw, ImageFilter
import random, math, sys
OUT = sys.argv[1]
S = 4

def shaded_head(w, h, mouth_open, seed):
    """a lit skull-ish head, thresholded against blurred noise so the white comes in clumps"""
    rnd = random.Random(seed)
    noise = Image.new("L", (w, h))
    npx = noise.load()
    for y in range(h):
        for x in range(w):
            npx[x, y] = rnd.randint(0, 255)
    noise = noise.filter(ImageFilter.GaussianBlur(1.6))
    # stretch the blurred noise back to the full range
    lo, hi = noise.getextrema()
    noise = noise.point(lambda v: int((v - lo) * 255 / max(1, hi - lo)))
    npx = noise.load()
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    px = img.load()
    for y in range(h):
        for x in range(w):
            nx, ny = (x - w / 2) / (w * 0.38), (y - h / 2) / (h * 0.47)
            # narrower toward the chin, a bump for the cheekbones
            narrow = 1 - max(0, ny) * 0.35 + 0.08 * math.exp(-((ny - 0.05) ** 2) / 0.01)
            nx2 = nx / narrow
            r = nx2 * nx2 + ny * ny
            if r > 1 + (rnd.random() - 0.5) * 0.08:
                continue
            z = math.sqrt(max(0, 1 - r))
            l = max(0, 0.7 * nx2 - 0.3 * ny + 0.45 * z)
            for ex in (-0.36, 0.36):
                dd = math.hypot((nx2 - ex) / 0.27, (ny + 0.2) / 0.24)
                if dd < 1:
                    l *= dd ** 4 * 0.2
            # cheekbones and brow catch the light
            l += 0.35 * math.exp(-((ny + 0.42) ** 2) / 0.006) * max(0, nx2 + 0.3)
            l += 0.3 * math.exp(-((ny - 0.08) ** 2) / 0.005) * max(0, abs(nx2) - 0.35)
            mh = 0.3 if mouth_open else 0.07
            dd = math.hypot(nx2 / 0.36, (ny - 0.45) / mh)
            if dd < 1:
                l *= dd ** 3 * 0.15
            if mouth_open and abs(ny - 0.2) < 0.045 and abs(nx2) < 0.3 and int((nx2 + 1) * 16) % 2 == 0:
                l = 1.2
            if l * 1.25 > npx[x, y] / 255 + 0.35:
                px[x, y] = (255, 255, 255, 255)
            else:
                g = int(3 + min(1, l) * 14)
                px[x, y] = (g, g, g, 255)
    return img

def palm_eye(d, cx, cy, s, iris):
    d.ellipse([(cx - s) * S, (cy - s * 0.55) * S, (cx + s) * S, (cy + s * 0.55) * S], fill=(20, 20, 20, 255))
    d.ellipse([(cx - s * 0.85) * S, (cy - s * 0.42) * S, (cx + s * 0.85) * S, (cy + s * 0.42) * S], fill=(250, 250, 250, 255))
    d.ellipse([(cx - s * 0.35) * S, (cy - s * 0.35) * S, (cx + s * 0.35) * S, (cy + s * 0.35) * S], fill=iris)
    d.ellipse([(cx - s * 0.16) * S, (cy - s * 0.16) * S, (cx + s * 0.16) * S, (cy + s * 0.16) * S], fill=(0, 0, 0, 255))

def grunge(img, seed, amount=0.18):
    rnd = random.Random(seed)
    px = img.load()
    w, h = img.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a:
                k = 1 - rnd.random() * amount
                px[x, y] = (int(r * k), int(g * k), int(b * k), a)
    return img

def halt():
    N = 256
    big = Image.new("RGBA", (N * S, N * S), (0, 0, 0, 0))
    d = ImageDraw.Draw(big)
    def octa(r):
        return [((128 + r * math.cos(math.radians(22.5 + 45 * i))) * S, (128 + r * math.sin(math.radians(22.5 + 45 * i))) * S) for i in range(8)]
    d.polygon(octa(124), fill=(150, 10, 14, 255))
    d.polygon(octa(116), fill=(245, 245, 245, 255))
    d.polygon(octa(108), fill=(215, 20, 24, 255))
    # open hand, white
    d.rounded_rectangle([88 * S, 112 * S, 168 * S, 200 * S], radius=26 * S, fill=(250, 250, 250, 255))
    for x, top in ((92, 56), (110, 44), (128, 48), (146, 62)):
        d.rounded_rectangle([x * S, top * S, (x + 16) * S, 130 * S], radius=8 * S, fill=(250, 250, 250, 255))
    d.polygon([(160 * S, 150 * S), (188 * S, 110 * S), (200 * S, 120 * S), (172 * S, 172 * S)], fill=(250, 250, 250, 255))
    d.ellipse([186 * S, 104 * S, 204 * S, 124 * S], fill=(250, 250, 250, 255))
    palm_eye(d, 128, 160, 22, (200, 20, 20, 255))
    return grunge(big.resize((N, N), Image.LANCZOS), 1)

def proceed():
    N = 256
    big = Image.new("RGBA", (N * S, N * S), (0, 0, 0, 0))
    d = ImageDraw.Draw(big)
    d.ellipse([4 * S, 4 * S, 252 * S, 252 * S], fill=(20, 90, 20, 255))
    d.ellipse([12 * S, 12 * S, 244 * S, 244 * S], fill=(245, 245, 245, 255))
    d.ellipse([20 * S, 20 * S, 236 * S, 236 * S], fill=(40, 150, 30, 255))
    # pointing hand: a fist with the index finger out to the right, thumb up
    d.rounded_rectangle([70 * S, 112 * S, 150 * S, 182 * S], radius=14 * S, fill=(250, 250, 250, 255))
    d.rounded_rectangle([140 * S, 112 * S, 204 * S, 132 * S], radius=10 * S, fill=(250, 250, 250, 255))
    d.rounded_rectangle([108 * S, 72 * S, 130 * S, 126 * S], radius=10 * S, fill=(250, 250, 250, 255))
    d.rectangle([44 * S, 120 * S, 70 * S, 176 * S], fill=(250, 250, 250, 255))
    for y in (140, 156):
        d.line([(120 * S, y * S), (150 * S, y * S)], fill=(200, 200, 200, 255), width=2 * S)
    palm_eye(d, 104, 152, 18, (40, 160, 40, 255))
    return grunge(big.resize((N, N), Image.LANCZOS), 2)

face = shaded_head(150, 150, False, 3)
attack = shaded_head(376, 486, True, 4)
arts = {"a90b_face": face, "a90b_halt": halt(), "a90b_proceed": proceed(), "a90b_attack": attack}
for k, im in arts.items():
    im.save(f"{OUT}/{k}.png")
H = 260
row = [im.resize((int(im.width * H / im.height), H), Image.NEAREST) for im in arts.values()]
o = Image.new("RGBA", (sum(r.width for r in row) + 60, H), (12, 12, 14, 255))
x = 0
for r in row:
    o.alpha_composite(r, (x, 0))
    x += r.width + 20
ref = Image.open(f"{OUT}/a90b_ref.png").convert("RGBA")
out = Image.new("RGBA", (max(ref.width, o.width), H * 2 + 10), (12, 12, 14, 255))
out.alpha_composite(ref, (0, 0))
out.alpha_composite(o, (0, H + 10))
out.save(f"{OUT}/a90b_compare.png")
