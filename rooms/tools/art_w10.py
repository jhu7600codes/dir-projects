## W-10 "need to get electrocuted?", redrawn after the outlet in the game: a wall outlet
## whose slots and ground hole make a face. now a proper universal outlet plate with screws,
## scorch marks and glitch slices. the tweak: blue electricity glowing inside the holes
from PIL import Image, ImageDraw, ImageFilter, ImageChops
import random, math, sys
N = 256
S = 3
rnd = random.Random(10)

def old():
    im = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.rectangle([10, 6, 53, 57], fill=(230, 224, 209, 255))
    d.rectangle([20, 18, 23, 31], fill=(20, 20, 20, 255))
    d.rectangle([40, 18, 43, 31], fill=(20, 20, 20, 255))
    d.ellipse([27, 38, 37, 48], fill=(20, 20, 20, 255))
    return im.resize((N, N), Image.NEAREST)

big = Image.new("RGBA", (N * S, N * S), (0, 0, 0, 0))
d = ImageDraw.Draw(big)
def r(v): return [x * S for x in v]
# the plate: off-white plastic with a bevel
d.rounded_rectangle(r([40, 16, 216, 240]), radius=16 * S, fill=(196, 190, 174, 255))
d.rounded_rectangle(r([46, 22, 210, 234]), radius=13 * S, fill=(232, 226, 211, 255))
# the socket face: a raised rounded square
d.rounded_rectangle(r([70, 58, 186, 196]), radius=22 * S, fill=(214, 208, 192, 255))
d.rounded_rectangle(r([74, 62, 182, 192]), radius=19 * S, fill=(238, 233, 220, 255))
# universal slots: each eye is a slot crossed with a round hole
holes = Image.new("L", (N * S, N * S), 0)
hd = ImageDraw.Draw(holes)
for cx in (101, 155):
    hd.rounded_rectangle(r([cx - 5, 84, cx + 5, 126]), radius=3 * S, fill=255)
    hd.ellipse(r([cx - 11, 96, cx + 11, 118]), fill=255)
# the ground hole: a mouth, a little lopsided
hd.ellipse(r([113, 143, 143, 175]), fill=255)
dark = Image.new("RGBA", big.size, (18, 16, 16, 255))
big.paste(dark, (0, 0), holes)
# screws
for y in (36, 220):
    d.ellipse(r([120, y - 8, 136, y + 8]), fill=(170, 168, 160, 255))
    d.line(r([122, y - 5, 134, y + 5]), fill=(90, 88, 84, 255), width=2 * S)
def render(live):
    rnd = random.Random(10)
    img = big.resize((N, N), Image.LANCZOS)
    # the tweak: blue electricity glowing in the holes
    hm = holes.resize((N, N), Image.LANCZOS)
    blue = Image.new("RGBA", (N, N), (80, 170, 255, 0))
    blue.putalpha(hm.filter(ImageFilter.GaussianBlur(7)).point(lambda v: min(255, v * 2)))
    core = Image.new("RGBA", (N, N), (190, 235, 255, 0))
    core.putalpha(hm.filter(ImageFilter.MinFilter(9)).filter(ImageFilter.GaussianBlur(2)).point(lambda v: int(v * 0.45)))
    if live:
        img.alpha_composite(blue)
        img.alpha_composite(core)
    # little sparks crackling out of the slots
    sd = ImageDraw.Draw(img)
    for cx in ((101, 155) if live else ()):
        for _ in range(3):
            x, y = cx, rnd.randint(86, 124)
            pts = [(x, y)]
            for _ in range(4):
                x += rnd.choice((-1, 1)) * rnd.randint(4, 9)
                y += rnd.randint(-7, 7)
                pts.append((x, y))
            sd.line(pts, fill=(170, 225, 255, 255), width=2)
    # scorch marks around the holes
    sc = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    scd = ImageDraw.Draw(sc)
    for _ in range(14):
        x, y = rnd.uniform(80, 176), rnd.uniform(70, 186)
        rr = rnd.uniform(6, 16)
        scd.ellipse([x - rr, y - rr, x + rr, y + rr], fill=(40, 30, 20, rnd.randint(40, 90)))
    sc = sc.filter(ImageFilter.GaussianBlur(6))
    plate = img.split()[3]
    sc.putalpha(ImageChops.multiply(sc.split()[3], plate))
    img = Image.alpha_composite(img, sc)
    dk = Image.new("RGBA", (N, N), (14, 14, 18, 0))
    dk.putalpha(hm.filter(ImageFilter.MinFilter(3)))
    img.alpha_composite(dk)
    if live:
        img.alpha_composite(core)
    # glitch slices and a cyan/red split
    for _ in range(3):
        y0 = rnd.randint(60, 200)
        h = rnd.randint(3, 9)
        sl = img.crop((0, y0, N, y0 + h))
        img.paste((0, 0, 0, 0), (0, y0, N, y0 + h))
        img.paste(sl, (rnd.randint(-12, 12), y0), sl)
    a = img.split()[3]
    cyan = Image.merge("RGBA", (Image.new("L", (N, N), 0), a.point(lambda v: v // 2), a.point(lambda v: v // 2), a.point(lambda v: v // 3)))
    img = Image.alpha_composite(ImageChops.offset(cyan, -4, 0), img)

    return img

dead = render(False)
live = render(True)
dead.save(sys.argv[1] + "/w10.png")
live.save(sys.argv[1] + "/w10_live.png")
out = Image.new("RGBA", (N * 3 + 40, N), (40, 42, 44, 255))
out.alpha_composite(old(), (0, 0))
out.alpha_composite(dead, (N + 20, 0))
out.alpha_composite(live, (N * 2 + 40, 0))
out.save(sys.argv[1] + "/w10_compare.png")
