## steam store art for The ROOMS: the dark A-160-ish hallway with A-60 at the end, graded
## darker, our A-60 drawing composited big in the doorway, and the logo
from PIL import Image, ImageDraw, ImageFilter, ImageEnhance, ImageChops
import sys, os
SP = sys.argv[1]
ROOT = sys.argv[2]
OUT = sys.argv[3]
os.makedirs(OUT, exist_ok=True)
bg_src = Image.open(f"{SP}/steam/bg4k_161.png").convert("RGB")
logo = Image.open(f"{ROOT}/assets/branding/logo.png").convert("RGBA")
a60 = Image.open(f"{ROOT}/assets/art/a60_1.png").convert("RGBA")

def grade(im):
    im = ImageEnhance.Brightness(im).enhance(0.38)
    im = ImageEnhance.Contrast(im).enhance(1.35)
    im = ImageEnhance.Color(im).enhance(1.15)
    # vignette
    w, h = im.size
    v = Image.new("L", (w, h), 0)
    ImageDraw.Draw(v).ellipse([-w * 0.15, -h * 0.25, w * 1.15, h * 1.25], fill=255)
    v = v.filter(ImageFilter.GaussianBlur(min(w, h) * 0.28))
    black = Image.new("RGB", (w, h), (0, 0, 0))
    return Image.composite(im, black, v)

def scene(w, h, focus_x=0.5, a60_scale=0.42, zoom=1.9, focus_y=0.5):
    """the hallway cropped to w x h, vanishing point at focus_x, A-60 big at the far end"""
    sw, sh = bg_src.size
    # the hallway's vanishing point in the source is the middle
    k = max(w / sw, h / sh) * zoom
    big = bg_src.resize((int(sw * k), int(sh * k)), Image.LANCZOS)
    cx, cy = big.width / 2, big.height * 0.55
    x0 = int(cx - w * focus_x)
    y0 = int(cy - h * focus_y)
    x0 = max(0, min(big.width - w, x0))
    y0 = max(0, min(big.height - h, y0))
    im = big.crop((x0, y0, x0 + w, y0 + h))
    im = grade(im).convert("RGBA")
    # red glow + A-60 at the far end
    px, py = cx - x0, cy - y0
    s = int(h * a60_scale)
    glow = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    ImageDraw.Draw(glow).ellipse([px - s, py - s * 0.8, px + s, py + s * 0.8], fill=(255, 20, 20, 140))
    im.alpha_composite(glow.filter(ImageFilter.GaussianBlur(s * 0.5)))
    f = a60.resize((s, s), Image.LANCZOS)
    im.alpha_composite(f, (int(px - s / 2), int(py - s * 0.55)))
    return im

def with_logo(im, lw_frac, pos="left", margin=0.05):
    w, h = im.size
    lw = int(w * lw_frac)
    lg = logo.resize((lw, int(lw * logo.height / logo.width)), Image.LANCZOS)
    # dark soft shadow under the logo so it reads
    sh = Image.new("RGBA", im.size, (0, 0, 0, 0))
    m = int(w * margin)
    x = m if pos == "left" else (w - lw) // 2
    y = (h - lg.height) // 2 if pos == "left" else int(h * 0.06)
    if pos == "bottom":
        x, y = (w - lw) // 2, h - lg.height - int(h * 0.06)
    a = lg.split()[3].point(lambda v: int(v * 0.85))
    shadow = Image.new("RGBA", lg.size, (0, 0, 0, 255))
    shadow.putalpha(a)
    sh.alpha_composite(shadow, (x + 4, y + 6))
    im.alpha_composite(sh.filter(ImageFilter.GaussianBlur(10)))
    im.alpha_composite(lg, (x, y))
    return im

def save(im, name, fmt="PNG"):
    path = f"{OUT}/{name}"
    if fmt == "JPEG":
        im.convert("RGB").save(path, "JPEG", quality=92)
    else:
        im.save(path)

def logo_at(im, lw_frac, x_frac, y_frac):
    w, h = im.size
    lw = int(w * lw_frac)
    lg = logo.resize((lw, int(lw * logo.height / logo.width)), Image.LANCZOS)
    x = int(w * x_frac - lw / 2)
    y = int(h * y_frac - lg.height / 2)
    sh = Image.new("RGBA", im.size, (0, 0, 0, 0))
    shadow = Image.new("RGBA", lg.size, (0, 0, 0, 255))
    shadow.putalpha(lg.split()[3].point(lambda v: int(v * 0.85)))
    sh.alpha_composite(shadow, (x + 4, y + 6))
    im.alpha_composite(sh.filter(ImageFilter.GaussianBlur(max(4, w // 90))))
    im.alpha_composite(lg, (x, y))
    return im

# store capsules (logo on them)
save(logo_at(scene(920, 430, focus_x=0.74, a60_scale=0.5), 0.5, 0.29, 0.5).convert("RGB"), "header_capsule_920x430.png")
save(logo_at(scene(462, 174, focus_x=0.82, a60_scale=0.6, zoom=2.4), 0.6, 0.34, 0.5).convert("RGB"), "small_capsule_462x174.png")
save(logo_at(scene(1232, 706, focus_x=0.72, a60_scale=0.42), 0.48, 0.28, 0.5).convert("RGB"), "main_capsule_1232x706.png")
save(logo_at(scene(748, 896, focus_x=0.5, a60_scale=0.32, focus_y=0.42), 0.86, 0.5, 0.17).convert("RGB"), "vertical_capsule_748x896.png")
# library (the hero has no logo or text, steam lays the library logo over it)
save(logo_at(scene(600, 900, focus_x=0.5, a60_scale=0.3, focus_y=0.45), 0.88, 0.5, 0.16).convert("RGB"), "library_capsule_600x900.png")
save(scene(3840, 1240, focus_x=0.68, a60_scale=0.38, zoom=1.15).convert("RGB"), "library_hero_3840x1240.png")
ll = logo.copy()
ll.thumbnail((1280, 720), Image.LANCZOS)
save(ll, "library_logo_1280x%d.png" % ll.height)
save(logo.resize((640, int(640 * logo.height / logo.width)), Image.LANCZOS), "library_logo_640.png")
# page background: the same hallway, darker and softer so the store text stays readable
pb = scene(1438, 810, focus_x=0.7, a60_scale=0.32)
pb = ImageEnhance.Brightness(pb.convert("RGB")).enhance(0.6).filter(ImageFilter.GaussianBlur(2))
save(pb, "page_background_1438x810.png")
# icons from the game icon
icon = Image.open(f"{ROOT}/assets/branding/icon.png").convert("RGBA")
ci = Image.new("RGBA", icon.size, (12, 12, 14, 255))
ci.alpha_composite(icon)
save(ci.resize((184, 184), Image.LANCZOS), "community_icon_184x184.jpg", "JPEG")
icon.resize((256, 256), Image.LANCZOS).save(f"{OUT}/client_icon_256.png")
icon.save(f"{OUT}/client_icon.ico", sizes=[(16, 16), (24, 24), (32, 32), (48, 48), (64, 64), (128, 128), (256, 256)])
print("done")
