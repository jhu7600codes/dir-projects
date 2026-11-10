## A-90, redrawn after the one in the game: a face made of white static speckles, the red
## stop sign with the black hand, and the screaming face. the tweak: two tiny red eyes
## that are always there in the idle face
from PIL import Image, ImageDraw, ImageFilter
import random, math, sys
W = 200

def head_light(scream):
    """a brightness map of a head lit from the top left (0..1), with sockets and a mouth"""
    img = [[0.0] * W for _ in range(W)]
    for y in range(W):
        for x in range(W):
            nx, ny = (x - 100) / 72, (y - 102) / 95
            r = nx * nx + ny * ny
            if r > 1:
                continue
            z = math.sqrt(1 - r)
            l = max(0, -0.5 * nx - 0.45 * ny + 0.75 * z)
            # eye sockets
            for ex in (-0.38, 0.38):
                d = math.hypot((nx - ex) / 0.28, (ny + 0.15) / 0.2)
                if d < 1:
                    l *= d * d * 0.6
            # mouth: a slit, or a huge open hole when it screams
            if scream:
                d = math.hypot(nx / 0.42, (ny - 0.42) / 0.45)
            else:
                d = math.hypot(nx / 0.4, (ny - 0.5) / 0.06)
            if d < 1:
                l *= d * d * 0.5
            # cheek and brow ridges catch more light
            l += 0.25 * math.exp(-((ny + 0.42) ** 2) / 0.01) * z
            img[y][x] = min(1.0, l)
    return img

def speckle(light, rnd, colors, density=1.0, jitter=3):
    out = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    px = out.load()
    for y in range(W):
        for x in range(W):
            l = light[y][x]
            if l > 0 and rnd.random() < (l ** 3.2) * density * 0.75:
                xx = min(W - 1, max(0, x + rnd.randint(-jitter, jitter)))
                px[xx, y] = rnd.choice(colors) + (255,)
    # horizontal streaks, like a bad signal
    d = ImageDraw.Draw(out)
    for _ in range(90):
        y = rnd.randint(20, 180)
        x = rnd.randint(20, 160)
        if light[y][min(W - 1, x + 10)] > 0.45:
            d.line([(x, y), (x + rnd.randint(4, 18), y)], fill=rnd.choice(colors) + (255,), width=rnd.choice((1, 1, 2)))
    return out

rnd = random.Random(90)
idle = speckle(head_light(False), rnd, [(255, 255, 255)], 1.0)
dd = ImageDraw.Draw(idle)
for ex in (100 - 30, 100 + 30):  # the tweak: two tiny red eyes
    dd.rectangle([ex - 2, 85, ex + 2, 89], fill=(255, 30, 30, 255))
glow = Image.new("RGBA", (W, W), (0, 0, 0, 0))
ImageDraw.Draw(glow).rectangle([66, 83, 74, 91], fill=(255, 0, 0, 160))
ImageDraw.Draw(glow).rectangle([126, 83, 134, 91], fill=(255, 0, 0, 160))
idle = Image.alpha_composite(glow.filter(ImageFilter.GaussianBlur(3)), idle)

scream = speckle(head_light(True), random.Random(91), [(255, 255, 255), (255, 255, 255), (230, 20, 20), (255, 40, 40), (230, 20, 20)], 1.2, 4)

# the stop sign: a chunky pixel octagon with a black hand
S = 4
big = Image.new("RGBA", (W * S, W * S), (0, 0, 0, 0))
bd = ImageDraw.Draw(big)
def octa(r):
    return [(400 + r * math.cos(math.radians(22.5 + 45 * i)), 400 + r * math.sin(math.radians(22.5 + 45 * i))) for i in range(8)]
bd.polygon(octa(390), fill=(255, 0, 0, 255))
bd.polygon(octa(340), fill=(20, 0, 0, 255))
bd.polygon(octa(318), fill=(255, 0, 0, 255))
# the hand: palm + four fingers + thumb
bd.rounded_rectangle([270, 340, 520, 640], radius=70, fill=(10, 10, 10, 255))
for i, (x, top) in enumerate(((282, 175), (340, 130), (398, 150), (456, 200))):
    bd.rounded_rectangle([x, top, x + 52, 420], radius=26, fill=(10, 10, 10, 255))
bd.polygon([(490, 420), (575, 300), (625, 330), (560, 470), (520, 540)], fill=(10, 10, 10, 255))
bd.ellipse([570, 285, 630, 345], fill=(10, 10, 10, 255))
stop = big.resize((W, W), Image.NEAREST if False else Image.BOX)
# pixel it up like the original: snap to a coarse grid, and chip the edges
stop = stop.resize((W // 2, W // 2), Image.BOX).resize((W, W), Image.NEAREST)
sp = stop.load()
r2 = random.Random(92)
for _ in range(220):
    x, y = r2.randrange(W), r2.randrange(W)
    if sp[x, y][3] > 0 and r2.random() < 0.5:
        sp[x, y] = (255, 255, 255, 255) if r2.random() < 0.3 else sp[x, y]

stop.save(f"{sys.argv[1]}/a90_stop.png")  # the clean sign
stop = stop.copy()
# glitch the sign: torn slices, a cyan/red split, dead pixels and a few scanlines
from PIL import ImageChops
g = random.Random(93)
for _ in range(9):
    y0 = g.randint(10, 185)
    h = g.randint(2, 9)
    sl = stop.crop((0, y0, W, y0 + h))
    stop.paste((0, 0, 0, 0), (0, y0, W, y0 + h))
    stop.paste(sl, (g.randint(-16, 16), y0), sl)
r_, g_, b_, a_ = stop.split()
cyan = Image.merge("RGBA", (Image.new("L", (W, W), 0), a_.point(lambda v: v // 2), a_.point(lambda v: v * 3 // 4), a_.point(lambda v: v // 2)))
stop = Image.alpha_composite(ImageChops.offset(cyan, -5, 1), stop)
red = Image.merge("RGBA", (a_, Image.new("L", (W, W), 0), Image.new("L", (W, W), 0), a_.point(lambda v: v // 3)))
stop = Image.alpha_composite(ImageChops.offset(red, 6, -1), stop)
sp = stop.load()
for _ in range(700):
    x, y = g.randrange(W), g.randrange(W)
    if sp[x, y][3] > 0:
        sp[x, y] = g.choice([(255, 255, 255, 255), (0, 0, 0, 255), (255, 255, 255, 255), (60, 255, 255, 255)])
sd = ImageDraw.Draw(stop)
for _ in range(14):
    y = g.randint(5, 195)
    x = g.randint(0, 150)
    sd.line([(x, y), (x + g.randint(10, 60), y)], fill=g.choice([(255, 255, 255, 220), (0, 0, 0, 255), (255, 40, 40, 255)]))
# blocky corrupted chunks
for _ in range(6):
    x, y = g.randint(20, 170), g.randint(20, 170)
    w, h = g.randint(6, 20), g.randint(3, 8)
    chunk = stop.crop((x, y, x + w, y + h))
    stop.paste(chunk, (x + g.randint(-25, 25), y + g.randint(-6, 6)))

for name, im in (("a90_face", idle), ("a90_stop_glitch", stop), ("a90_jumpscare", scream)):
    im.save(f"{sys.argv[1]}/{name}.png")
H = 300
prev = Image.new("RGBA", (H * 3 + 40, H), (12, 12, 14, 255))
for i, im in enumerate((idle, stop, scream)):
    prev.alpha_composite(im.resize((H, H), Image.NEAREST), (i * (H + 20), 0))
prev.save(f"{sys.argv[1]}/a90_preview.png")
