## the billboard (The Outside): a lit advert over the street with a face that wants you to
## look at it. the tweak: the ad's line underneath just says "LOOK UP"
from PIL import Image, ImageDraw, ImageFilter, ImageFont
import math, random, sys
W, H = 640, 400
rnd = random.Random(7)
img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
d = ImageDraw.Draw(img)
# the board: a metal frame with a lit panel
d.rectangle([0, 0, W - 1, H - 1], fill=(40, 40, 46, 255))
panel = Image.new("RGBA", (W - 40, H - 40))
pp = panel.load()
for y in range(H - 40):
    for x in range(W - 40):
        t = y / (H - 40)
        r = int(150 - 90 * t + rnd.randint(-6, 6))
        pp[x, y] = (max(0, r), 18, 34, 255)
img.paste(panel, (20, 20))
# the face: pale, round, too happy
face = Image.new("RGBA", (W, H), (0, 0, 0, 0))
fd = ImageDraw.Draw(face)
fd.ellipse([200, 34, 440, 300], fill=(238, 222, 214, 255))
# eyes: wide open, small red pupils, looking right at you
for cx in (272, 368):
    fd.ellipse([cx - 34, 120, cx + 34, 170], fill=(255, 255, 255, 255))
    fd.ellipse([cx - 11, 134, cx + 11, 156], fill=(200, 10, 30, 255))
    fd.ellipse([cx - 5, 140, cx + 5, 150], fill=(10, 0, 4, 255))
    fd.arc([cx - 40, 108, cx + 40, 180], 200, 340, fill=(90, 40, 50, 255), width=3)
# a wide thin smile, almost ear to ear
pts = [(232 + i * 4.4, 222 + math.sin(i / 40 * math.pi) * 34 - (abs(i - 20) / 20) ** 4 * 10) for i in range(41)]
fd.line(pts, fill=(120, 20, 40, 255), width=6)
for i in range(3, 39, 3):
    x, y = pts[i]
    fd.line([(x, y - 2), (x, y + 8)], fill=(250, 245, 240, 255), width=3)
# a soft glow from behind
glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
ImageDraw.Draw(glow).ellipse([170, 10, 470, 320], fill=(255, 90, 110, 160))
img.alpha_composite(glow.filter(ImageFilter.GaussianBlur(30)))
img.alpha_composite(face)
# the line under it
try:
    font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 46)
    small = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 18)
except OSError:
    font = small = ImageFont.load_default()
txt = "LOOK UP"
tw = d.textlength(txt, font=font)
d.text(((W - tw) / 2, 312), txt, font=font, fill=(255, 240, 235, 255))
d.text((W - 110, H - 46), "MORSE", font=small, fill=(255, 200, 200, 200))
# scanlines and lamps along the top
for y in range(20, H - 20, 3):
    d.line([(20, y), (W - 21, y)], fill=(0, 0, 0, 40))
for x in range(60, W - 40, 130):
    d.rectangle([x, 0, x + 26, 10], fill=(70, 70, 76, 255))
img.save(sys.argv[1] + "/billboard.png")
old = Image.open(sys.argv[1] + "/billboard_old.png").convert("RGBA").resize((H, H))
out = Image.new("RGBA", (H + 20 + W, H), (25, 28, 40, 255))
out.alpha_composite(old, (0, 0))
out.alpha_composite(img, (H + 20, 0))
out.save(sys.argv[1] + "/billboard_compare.png")
