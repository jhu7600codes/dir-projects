## headlights (The Outside): two blinding headlights coming down the street with nothing
## behind them. lens flares, a light streak, a car-shaped dark that isn't really there.
## the tweak: the grille between them is a thin grin
from PIL import Image, ImageDraw, ImageFilter
import math, sys
W, H = 640, 360

img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
# the barely-there car: a dark rounded shape, mostly transparent
car = Image.new("RGBA", (W, H), (0, 0, 0, 0))
ImageDraw.Draw(car).rounded_rectangle([70, 110, 570, 290], radius=70, fill=(8, 8, 12, 150))
img.alpha_composite(car.filter(ImageFilter.GaussianBlur(20)))
# the grin: a thin pale grille line curving up at the ends
g = Image.new("RGBA", (W, H), (0, 0, 0, 0))
gd = ImageDraw.Draw(g)
pts = [(200 + i * 6, 238 + math.sin(i / 40 * math.pi) * 16 - (abs(i - 20) / 20) ** 3 * 18) for i in range(41)]
gd.line(pts, fill=(200, 190, 160, 230), width=4)
for i in range(4, 40, 4):
    x, y = pts[i]
    gd.line([(x, y - 6), (x, y + 6)], fill=(200, 190, 160, 160), width=2)
img.alpha_composite(g.filter(ImageFilter.GaussianBlur(0.6)))
# the lights: huge soft glow, a bright core, rings and spikes
for cx in (165, 475):
    cy = 175
    glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    gd.ellipse([cx - 120, cy - 100, cx + 120, cy + 100], fill=(255, 240, 190, 120))
    img.alpha_composite(glow.filter(ImageFilter.GaussianBlur(45)))
    mid = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(mid).ellipse([cx - 62, cy - 50, cx + 62, cy + 50], fill=(255, 248, 220, 230))
    img.alpha_composite(mid.filter(ImageFilter.GaussianBlur(14)))
    core = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    cd = ImageDraw.Draw(core)
    cd.ellipse([cx - 40, cy - 32, cx + 40, cy + 32], fill=(255, 255, 250, 255))
    # star spikes
    for a in (0, math.pi / 2, math.pi / 4, -math.pi / 4):
        dx, dy = math.cos(a), math.sin(a)
        ln = 150 if a in (0,) else 70
        cd.polygon([(cx + dx * ln, cy + dy * ln), (cx - dy * 4, cy + dx * 4), (cx - dx * ln, cy - dy * ln), (cx + dy * 4, cy - dx * 4)], fill=(255, 250, 230, 200))
    img.alpha_composite(core.filter(ImageFilter.GaussianBlur(2)))
    # a faint lens ring
    ring = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(ring).ellipse([cx - 84, cy - 70, cx + 84, cy + 70], outline=(255, 230, 170, 90), width=3)
    img.alpha_composite(ring.filter(ImageFilter.GaussianBlur(2)))
# one long horizontal streak across both
st = Image.new("RGBA", (W, H), (0, 0, 0, 0))
ImageDraw.Draw(st).rectangle([0, 172, W, 178], fill=(255, 245, 210, 150))
img.alpha_composite(st.filter(ImageFilter.GaussianBlur(3)))
img.save(sys.argv[1] + "/headlights.png")
old = Image.open(sys.argv[1] + "/headlights_old.png").convert("RGBA").resize((H, H))
out = Image.new("RGBA", (H + 20 + W, H), (25, 28, 40, 255))
out.alpha_composite(old, (0, 0))
out.alpha_composite(img, (H + 20, 0))
out.save(sys.argv[1] + "/headlights_compare.png")
