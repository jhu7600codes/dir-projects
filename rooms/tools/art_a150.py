## A-150 "the star", redrawn after the one in the game: two star eyes over a round open
## mouth, olive gold. the tweak: a small white twinkle glinting off the top of each star
from PIL import Image, ImageDraw, ImageFilter
import math, sys
W, H = 600, 620
S = 3

def star(d, cx, cy, r, inner, fill):
    pts = []
    for i in range(10):
        rr = r if i % 2 == 0 else r * inner
        a = -math.pi / 2 + i * math.pi / 5
        pts.append(((cx + math.cos(a) * rr) * S, (cy + math.sin(a) * rr) * S))
    d.polygon(pts, fill=fill)

def layer(fill, grow=0):
    im = Image.new("RGBA", (W * S, H * S), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    for cx in (150, 450):
        star(d, cx, 175, 140 + grow, 0.45, fill)
    d.ellipse([(300 - 125 - grow) * S, (465 - 125 - grow) * S, (300 + 125 + grow) * S, (465 + 125 + grow) * S], fill=fill)
    return im.resize((W, H), Image.LANCZOS)

base = layer((128, 120, 36, 255))
rim = layer((96, 88, 24, 255), grow=5)
# a soft lighter middle on each shape, like the original's slight shading
hi = Image.new("RGBA", (W, H), (0, 0, 0, 0))
hd = ImageDraw.Draw(hi)
for cx in (150, 450):
    hd.ellipse([cx - 55, 140, cx + 55, 230], fill=(170, 160, 60, 140))
hd.ellipse([220, 390, 380, 520], fill=(160, 150, 55, 110))
hi = hi.filter(ImageFilter.GaussianBlur(28))
mask = base.split()[3]
hi.putalpha(Image.composite(hi.split()[3], Image.new("L", (W, H), 0), mask))
# inside of the mouth gets a little darker toward the bottom, so it reads as open
mo = Image.new("RGBA", (W, H), (0, 0, 0, 0))
ImageDraw.Draw(mo).ellipse([215, 440, 385, 580], fill=(60, 55, 12, 120))
mo = mo.filter(ImageFilter.GaussianBlur(22))
mo.putalpha(Image.composite(mo.split()[3], Image.new("L", (W, H), 0), mask))
img = Image.alpha_composite(rim, base)
img.alpha_composite(hi)
img.alpha_composite(mo)
# the tweak: a four point twinkle on the top tip of each star
tw = Image.new("RGBA", (W, H), (0, 0, 0, 0))
td = ImageDraw.Draw(tw)
for cx in (150, 450):
    x, y = cx + 8, 52
    td.polygon([(x, y - 26), (x + 5, y - 5), (x + 26, y), (x + 5, y + 5), (x, y + 26), (x - 5, y + 5), (x - 26, y), (x - 5, y - 5)], fill=(255, 255, 230, 255))
glow = tw.filter(ImageFilter.GaussianBlur(8))
img.alpha_composite(glow)
img.alpha_composite(tw)
img.save(sys.argv[1] + "/a150_face.png")
ref = Image.open(sys.argv[1] + "/a150_ref.png").convert("RGBA")
h = ref.height
mine = img.resize((int(W * h / H), h), Image.LANCZOS)
row = Image.new("RGBA", (mine.width * 2 + 20, h), (12, 12, 14, 255))
row.alpha_composite(mine)
light = Image.new("RGBA", mine.size, (236, 235, 229, 255))
light.alpha_composite(mine)
row.alpha_composite(light, (mine.width + 20, 0))
out = Image.new("RGBA", (max(ref.width, row.width), h * 2 + 10), (12, 12, 14, 255))
out.alpha_composite(ref, (0, 0))
out.alpha_composite(row, (0, h + 10))
out.save(sys.argv[1] + "/a150_compare.png")
