#!/usr/bin/env python3
"""build an original gd level and export it as a .gmd file (gdshare format)"""
import base64, gzip, random, sys
from xml.sax.saxutils import escape

BLOCK, SPIKE, YPAD, YORB = 1, 8, 35, 36
U = 30  # one grid unit

def obj(oid, gx, gy, rot=0):
    s = f"1,{oid},2,{gx*U+15},3,{gy*U+15}"
    return s + (f",6,{rot}" if rot else "") + ";"

def build(seed=7, length=220):
    random.seed(seed)
    objs, x = [], 12
    while x < length:
        r = random.random()
        if r < 0.35:                       # spike row
            for i in range(random.choice([1, 1, 2, 3])):
                objs.append(obj(SPIKE, x + i, 0))
            x += 7
        elif r < 0.55:                     # block step with spike on top
            for i in range(3):
                objs.append(obj(BLOCK, x + i, 0))
            objs.append(obj(SPIKE, x + 3, 0))
            x += 8
        elif r < 0.75:                     # pad launch over a spike pit
            objs.append(obj(YPAD, x, 0))
            for i in range(2, 7):
                objs.append(obj(SPIKE, x + i, 0))
            x += 10
        else:                              # orb gap
            for i in range(4):
                objs.append(obj(SPIKE, x + i, 0))
            objs.append(obj(YORB, x + 1, 2))
            x += 9
    header = "kS38,1_40_2_125_3_255_11_255_12_255_13_255_4_-1_6_1000_7_1_15_1_18_0_8_1|1_0_2_102_3_255_11_255_12_255_13_255_4_-1_6_1001_7_1_15_1_18_0_8_1|,kA13,0,kA15,0,kA16,0,kA14,,kA6,0,kA7,0,kA17,0,kA18,0,kS39,0,kA2,0,kA3,0,kA8,0,kA4,0,kA9,0,kA10,0,kA11,0;"
    return header + "".join(objs)

def to_gmd(name, desc, level):
    data = base64.urlsafe_b64encode(gzip.compress(level.encode())).decode()
    d = base64.urlsafe_b64encode(desc.encode()).decode()
    return ('<?xml version="1.0"?><plist version="1.0" gjver="2.0"><dict>'
            f'<k>kCEK</k><i>4</i><k>k2</k><s>{escape(name)}</s><k>k3</k><s>{d}</s>'
            f'<k>k4</k><s>{data}</s><k>k13</k><t /><k>k21</k><i>2</i><k>k50</k><i>35</i>'
            '</dict></plist>')

if __name__ == "__main__":
    name = sys.argv[1] if len(sys.argv) > 1 else "jhu run"
    seed = int(sys.argv[2]) if len(sys.argv) > 2 else 7
    out = f"{name.replace(' ', '_')}.gmd"
    open(out, "w").write(to_gmd(name, "made with make_gmd.py", build(seed)))
    print("wrote", out)
