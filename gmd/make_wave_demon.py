#!/usr/bin/env python3
"""original ~7 min wave demon, exported as .gmd (plist, k4 = gzip + urlsafe base64)"""
import base64, gzip, random
from xml.sax.saxutils import escape

U = 30
BLOCK, WAVE, MINI, BIG = 1, 660, 101, 99
SPEED = {1: 201, 2: 202, 3: 203}
BPS = {1: 311.58 / U, 2: 387.42 / U, 3: 468.0 / U}  # blocks per second
TOP_LIMIT = 9  # highest wall row, wave bounds are rows 0..10

# (mini?, speed, gap, seconds, max run length)
SECTIONS = [
    (0, 1, 4, 25, 6), (0, 1, 3, 30, 5), (0, 2, 3, 30, 5), (1, 2, 4, 30, 4),
    (0, 2, 2, 30, 4), (1, 1, 3, 30, 3), (0, 3, 3, 30, 4), (0, 2, 2, 30, 3),
    (1, 2, 3, 30, 3), (0, 3, 2, 30, 3), (1, 3, 3, 30, 3), (0, 2, 2, 30, 2),
    (1, 2, 3, 25, 2), (0, 3, 2, 25, 2), (0, 3, 2, 15, 1),
]

def o(oid, gx, gy, extra=""):
    return f"1,{oid},2,{gx*U+15},3,{gy*U+15}{extra};"

def build(seed=1337):
    rnd = random.Random(seed)
    objs = []
    x, c = 14, 0          # c = lowest free row of the corridor
    pb, pt = -1, 4        # previous bottom/top wall rows
    for i in range(14):   # intro: flat corridor on the ground
        objs.append(o(BLOCK, x + i, pt))
    x += 14
    for mini, spd, gap, secs, maxrun in SECTIONS:
        mid = c + gap / 2 - 0.5
        objs.append(o(SPEED[spd], x, round(mid)))
        objs.append(o(MINI if mini else BIG, x, round(mid)))
        slope = 2 if mini else 1
        cols = int(secs * BPS[spd])
        end, d, run = x + cols, 1, 0
        while x < end:
            if run <= 0:
                d = -d if rnd.random() < 0.8 else d
                run = rnd.randint(1, maxrun)
            nc = c + d * slope
            if nc < 0 or nc + gap > TOP_LIMIT:
                d, run = -d, rnd.randint(1, maxrun)
                nc = c + d * slope
                if nc < 0 or nc + gap > TOP_LIMIT:
                    nc = max(0, min(c, TOP_LIMIT - gap))
            c, run = nc, run - 1
            b, t = c - 1, c + gap
            for r in range(min(pb + 1, b), b + 1):      # floor, no holes
                if r >= 0:
                    objs.append(o(BLOCK, x, r))
            for r in range(t, max(pt - 1, t) + 1):      # ceiling, no holes
                objs.append(o(BLOCK, x, r))
            pb, pt, x = b, t, x + 1
    objs.append(o(BIG, x, c + 1))
    header = ("kS38,1_40_2_40_3_60_11_255_12_255_13_255_4_-1_6_1000_7_1_15_1_18_0_8_1|"
              "1_0_2_0_3_20_11_255_12_255_13_255_4_-1_6_1001_7_1_15_1_18_0_8_1|,"
              "kA13,0,kA15,0,kA16,0,kA14,,kA6,0,kA7,0,kA17,0,kA18,0,kS39,0,"
              "kA2,4,kA3,0,kA8,0,kA4,0,kA9,0,kA10,0,kA11,0;")
    return header + "".join(objs), len(objs), x

def to_gmd(name, desc, level):
    k4 = base64.urlsafe_b64encode(gzip.compress(level.encode())).decode()
    k3 = base64.urlsafe_b64encode(desc.encode()).decode()
    return ('<?xml version="1.0"?><plist version="1.0" gjver="2.0"><dict>'
            f'<k>kCEK</k><i>4</i><k>k2</k><s>{escape(name)}</s><k>k3</k><s>{k3}</s>'
            f'<k>k4</k><s>{k4}</s><k>k13</k><t /><k>k21</k><i>2</i><k>k50</k><i>35</i>'
            '</dict></plist>')

if __name__ == "__main__":
    lvl, n, cols = build()
    secs = sum(s[3] for s in SECTIONS)
    open("wave_demon.gmd", "w").write(to_gmd("wave demon", "7 min wave hell", lvl))
    print(f"wrote wave_demon.gmd: {n} objects, {cols} blocks long, ~{secs//60}m{secs%60}s")
