## every sound effect and both music tracks of The ROOMS, made from scratch with numpy:
## oscillators, filtered noise, envelopes and a bit of distortion. no samples from anywhere.
## usage: python tools/make_sounds.py <out dir>   (writes <key>.wav, convert with ffmpeg)
## needs numpy + scipy.
import sys, os, math
import numpy as np
from scipy import signal
from scipy.io import wavfile

SR = 44100
OUT = sys.argv[1]
os.makedirs(OUT, exist_ok=True)
rng = np.random.default_rng(1000)


# ---------------- building blocks ----------------

def t_(dur):
    return np.arange(int(SR * dur)) / SR

def sine(f, dur, phase=0.0):
    f = np.broadcast_to(np.asarray(f, dtype=float), (int(SR * dur),))
    return np.sin(2 * np.pi * np.cumsum(f) / SR + phase)

def saw(f, dur):
    f = np.broadcast_to(np.asarray(f, dtype=float), (int(SR * dur),))
    ph = np.cumsum(f) / SR
    return 2 * (ph % 1.0) - 1

def square(f, dur, duty=0.5):
    f = np.broadcast_to(np.asarray(f, dtype=float), (int(SR * dur),))
    ph = np.cumsum(f) / SR
    return np.where((ph % 1.0) < duty, 1.0, -1.0)

def noise(dur):
    return rng.uniform(-1, 1, int(SR * dur))

def brown(dur):
    x = np.cumsum(rng.normal(0, 1, int(SR * dur)))
    x = signal.lfilter([1, -1], [1, -0.995], x)
    return x / (np.max(np.abs(x)) + 1e-9)

def pink(dur):
    w = rng.normal(0, 1, int(SR * dur))
    b = [0.049922035, -0.095993537, 0.050612699, -0.004408786]
    a = [1, -2.494956002, 2.017265875, -0.522189400]
    x = signal.lfilter(b, a, w)
    return x / (np.max(np.abs(x)) + 1e-9)

def lp(x, f, order=2):
    b, a = signal.butter(order, min(f, SR / 2 - 100) / (SR / 2), "low")
    return signal.lfilter(b, a, x)

def hp(x, f, order=2):
    b, a = signal.butter(order, f / (SR / 2), "high")
    return signal.lfilter(b, a, x)

def bp(x, lo, hi, order=2):
    b, a = signal.butter(order, [lo / (SR / 2), min(hi, SR / 2 - 100) / (SR / 2)], "band")
    return signal.lfilter(b, a, x)

def env_exp(dur, k):
    return np.exp(-t_(dur) * k)

def adsr(dur, a=0.01, d=0.1, s=0.7, r=0.2):
    n = int(SR * dur)
    e = np.ones(n) * s
    na, nd, nr = int(SR * a), int(SR * d), int(SR * r)
    na = max(1, min(na, n)); e[:na] = np.linspace(0, 1, na)
    nd = max(1, min(nd, n - na)); e[na:na + nd] = np.linspace(1, s, nd)
    nr = max(1, min(nr, n)); e[-nr:] *= np.linspace(1, 0, nr)
    return e

def fade(x, fi=0.005, fo=0.02):
    x = x.copy()
    a, b = int(SR * fi), int(SR * fo)
    if a: x[:a] *= np.linspace(0, 1, a)
    if b: x[-b:] *= np.linspace(1, 0, b)
    return x

def pad_to(x, dur):
    n = int(SR * dur)
    return np.pad(x, (0, max(0, n - len(x))))[:n]

def mx(*xs):
    """mix arrays of different lengths (pads the short ones with silence)"""
    n = max(len(x) for x in xs)
    return sum(np.pad(x, (0, n - len(x))) for x in xs)

def place(dst, src, at):
    i = int(SR * at)
    j = min(len(dst), i + len(src))
    dst[i:j] += src[:j - i]
    return dst

def dist(x, k):
    return np.tanh(x * k) / np.tanh(k)

def crush(x, bits=4, keep=4):
    x = np.repeat(x[::keep], keep)[:len(x)]
    q = 2 ** bits
    return np.round(x * q) / q

def norm(x, peak=0.9):
    m = np.max(np.abs(x))
    return x * (peak / m) if m > 0 else x

def loopify(x, xf=0.25):
    """seamless loop: crossfade the tail into the head"""
    n = int(SR * xf)
    head, body, tail = x[:n], x[n:-n] if n else x, x[-n:]
    w = np.linspace(0, 1, n)
    start = tail * (1 - w) + head * w
    return np.concatenate([start, x[n:-n]])

def click(dur=0.01, f=3000):
    return hp(noise(dur), f) * env_exp(dur, 600)

def thud(dur=0.3, f=90, k=18):
    return lp(noise(dur), 300) * env_exp(dur, k) * 0.6 + sine(np.linspace(f * 1.6, f, int(SR * dur)), dur) * env_exp(dur, k)

def metal(dur, base, k=6, parts=(1, 2.76, 5.4, 8.93, 13.3)):
    x = sum(sine(base * p, dur, rng.uniform(0, 6)) * env_exp(dur, k * (1 + i * 0.6)) / (1 + i) for i, p in enumerate(parts))
    return x + hp(noise(dur), 2000) * env_exp(dur, 60) * 0.3

def scream(dur, f0=420, f1=180, rough=0.6, formants=None):
    """a shriek: a high wavering voice made of a few sine partials, ragged breath noise
    around it, and some grit. (formants is unused, kept so the calls stay readable)"""
    n = int(SR * dur)
    f = np.maximum(np.linspace(f0, f1, n) * 1.8, 650.0)
    f = f * (1 + 0.035 * sine(7.5, dur) + 0.02 * lp(noise(dur), 30) * rough)
    tone = sine(f, dur) + 0.55 * sine(f * 2, dur, 1.0) + 0.3 * sine(f * 3, dur, 2.0) + 0.15 * sine(f * 4.02, dur)
    breath = bp(noise(dur), 1500, 7000) * (0.6 + 0.4 * rough)
    rasp = tone * (1 + rough * 0.6 * hp(noise(dur), 2000))
    y = rasp * 0.7 + breath * 0.5
    return dist(norm(y) * adsr(dur, 0.015, 0.08, 0.85, min(0.3, dur * 0.4)), 2.2)

VOWELS = {"a": (800, 1150, 2900), "i": (300, 2300, 3000), "o": (450, 800, 2830), "e": (530, 1850, 2500), "u": (325, 700, 2530)}

def vowel(v, f0, dur, f1=None, breath=0.15):
    """a sung / shouted vowel by additive synthesis: harmonics of the pitch, each one as loud
    as the vowel's formants are near it. clean, nothing buzzy"""
    f1 = f0 if f1 is None else f1
    f = np.linspace(f0, f1, int(SR * dur)) * (1 + 0.012 * sine(5.5, dur))
    fm = VOWELS[v]
    y = np.zeros(int(SR * dur))
    for h in range(1, 28):
        fh = f0 * h
        if fh > 7000:
            break
        amp = sum(math.exp(-((fh - F) / (80 + F * 0.08)) ** 2) * (1.0 / (1 + k)) for k, F in enumerate(fm)) + 0.02 / h
        y += sine(f * h, dur, h * 0.7) * amp
    y = y + bp(noise(dur), fm[1] * 0.7, fm[2] * 1.3) * breath
    return norm(y) * adsr(dur, 0.02, 0.05, 0.9, min(0.08, dur * 0.3))

def speak(sylls, base, gap=0.03, consonant=None):
    """sylls: [(vowel, dur, pitch multiplier)]. strings them together with tiny noise bursts"""
    total = sum(d + gap for _, d, _ in sylls) + 0.1
    out = np.zeros(int(SR * total))
    t = 0.0
    for i, (v, d, pm) in enumerate(sylls):
        if consonant and i in consonant:
            out = place(out, hp(noise(0.05), 3000) * env_exp(0.05, 50) * 0.5, t)
            t += 0.03
        out = place(out, vowel(v, base * pm, d, base * pm * 0.92), t)
        t += d + gap
    return out

def glitchy(dur, seed):
    r = np.random.default_rng(seed)
    out = np.zeros(int(SR * dur))
    t = 0.0
    while t < dur:
        seg = r.uniform(0.015, 0.07)
        kind = r.integers(0, 4)
        if kind == 0:
            s = square(r.uniform(200, 2400), seg)
        elif kind == 1:
            s = noise(seg)
        elif kind == 2:
            s = saw(r.uniform(60, 600), seg)
        else:
            s = np.zeros(int(SR * seg))
        out = place(out, s * r.uniform(0.3, 1.0), t)
        t += seg
    return crush(out, 3, 3)

def notes(seq, wave="sine", dec=8.0):
    """seq: [(time, freq, dur, amp)]"""
    end = max(a + d for a, _, d, _ in seq) + 0.05
    out = np.zeros(int(SR * end))
    for a, f, d, amp in seq:
        if wave == "sine":
            s = sine(f, d) + 0.3 * sine(f * 2, d)
        elif wave == "square":
            s = square(f, d) * 0.5
        else:
            s = saw(f, d) * 0.5
        out = place(out, s * env_exp(d, dec) * amp, a)
    return out

def hum(dur, f=60, harm=(1, 2, 3, 4, 6), air=0.15):
    x = sum(sine(f * h, dur, h) / h for h in harm)
    return x * 0.6 + lp(pink(dur), 800) * air

def steps_loop(dur, every, heavy=1.0, jitter=0.02, seed=1):
    r = np.random.default_rng(seed)
    out = np.zeros(int(SR * dur))
    t = 0.05
    while t < dur - 0.3:
        out = place(out, thud(0.25, r.uniform(60, 80) / heavy, 22) * heavy, t + r.uniform(-jitter, jitter))
        t += every
    return out

N = lambda x: norm(x, 0.85)
S = {}


# ---------------- the office ----------------
S["ambience"] = loopify(N(hum(6.25, 60, air=0.5) * 0.4 + lp(brown(6.25), 300) * 0.6))
S["room_tone"] = loopify(N(hum(6.25, 50, (1, 2, 3), 0.3) * 0.5), 0.25) * 0.6
def latch(at, total, f=2600):
    x = np.zeros(int(SR * total))
    return place(x, mx(metal(0.06, f, 70, (1, 1.7, 2.9)) * 0.5, hp(noise(0.012), 3000) * env_exp(0.012, 500) * 0.6), at)

def hinge(dur, f0, f1):
    """a little squeak: a high, slightly unsteady glide, like a dry hinge"""
    f = np.linspace(f0, f1, int(SR * dur)) * (1 + 0.03 * lp(noise(dur), 25) * 6)
    y = sine(f, dur) * 0.6 + sine(f * 2.01, dur) * 0.25 + bp(noise(dur), f0, f1 * 2) * 0.2
    return y * adsr(dur, 0.05, 0.1, 0.7, dur * 0.4)

D = 1.0
door = latch(0.0, D, 2400) + latch(0.045, D, 2900) * 0.7
door = place(door, hinge(0.38, 820, 1150) * 0.22, 0.09)
door = place(door, lp(noise(0.6), 700) * adsr(0.6, 0.18, 0.1, 0.6, 0.3) * 0.5, 0.08)   # the air as it swings
door = place(door, thud(0.25, 95, 18) * 0.25, 0.62)                                     # it bumps to a stop
S["door_open"] = N(fade(door))
lk = np.zeros(int(SR * 0.55))
for i, at in enumerate((0.0, 0.07, 0.16, 0.23)):   # the handle jiggles but the latch won't give
    lk = place(lk, mx(metal(0.06, 2200 + (i % 2) * 400, 80, (1, 1.6, 2.7)) * 0.5, thud(0.08, 140, 50) * 0.35), at)
S["door_locked"] = N(lk)
lo = latch(0.0, 0.7, 2100)
lo = place(lo, mx(metal(0.55, 170, 9, (1, 1.93, 3.1, 4.6)) * 0.45, metal(0.3, 760, 22, (1, 2.3)) * 0.2), 0.05)  # thin sheet metal
lo = place(lo, thud(0.3, 85, 16) * 0.35, 0.05)
S["locker"] = N(lo)
S["step"] = N(lp(noise(0.16), 900) * env_exp(0.16, 32) + thud(0.16, 70, 40) * 0.3) * 0.7
S["flashlight_click"] = N(mx(click(0.012, 2500), place(np.zeros(int(SR * 0.05)), click(0.01, 4000) * 0.6, 0.02)))
x = np.zeros(int(SR * 0.4))
for i in range(6):
    x = place(x, hp(noise(0.03), 1200) * env_exp(0.03, 120) * (0.6 + 0.4 * (i % 2)), i * 0.06)
S["shakelight"] = N(x + metal(0.4, 1300, 30) * 0.2)
x = np.zeros(int(SR * 0.55))
for i, f in enumerate((2600, 3300, 2900, 3700)):
    x = place(x, metal(0.35, f, 14, (1, 2.4, 3.9)) * (0.7 - i * 0.1), i * 0.045)
S["gold"] = N(x)
S["pickup"] = N(notes([(0, 660, 0.2, 0.7), (0.07, 990, 0.25, 0.8)], dec=10))
S["buy"] = N(notes([(0, 784, 0.2, 0.6), (0.08, 988, 0.2, 0.6), (0.16, 1319, 0.4, 0.8)], dec=7))
S["deny"] = N(pad_to(place(np.zeros(int(SR * 0.3)), square(160, 0.1) * 0.5, 0) + place(np.zeros(int(SR * 0.3)), square(120, 0.12) * 0.5, 0.14), 0.3))
S["hurt"] = N(mx(thud(0.4, 70, 10), place(np.zeros(int(SR * 0.4)), vowel("a", 230, 0.22, 190, 0.6) * 0.35, 0.03), hp(noise(0.15), 1500) * env_exp(0.15, 25) * 0.4))
x = np.zeros(int(SR * 0.6))
for i in range(5):
    x = place(x, hp(noise(0.02), 2500) * env_exp(0.02, 200) * 0.5, i * 0.035)
S["vitamins"] = N(x + place(np.zeros(int(SR * 0.6)), bp(noise(0.35), 300, 1200) * adsr(0.35, 0.1, 0.1, 0.6, 0.2) * 0.6, 0.22))
S["death"] = N(scream(1.4, 520, 120, 0.9) * 0.8 + thud(1.4, 50, 3) * 0.6)
S["achievement"] = N(notes([(0, 523, 0.3, 0.6), (0.09, 659, 0.3, 0.6), (0.18, 784, 0.3, 0.6), (0.27, 1047, 0.6, 0.8)], dec=5))
S["glitch"] = N(glitchy(0.8, 1))
S["exit_ambience"] = loopify(N(sum(sine(f, 8.25) * (0.6 + 0.4 * sine(0.25 + i * 0.05, 8.25)) for i, f in enumerate((220, 277.2, 329.6, 440, 554.4))) + lp(pink(8.25), 1500) * 0.1))
S["typing"] = None  # below
S["power_on"] = N(sine(np.geomspace(120, 900, int(SR * 2.5)), 2.5) * adsr(2.5, 0.3, 0.5, 0.7, 0.8) * 0.35 + hum(2.5, 60) * np.linspace(0, 0.6, int(SR * 2.5)))
S["elevator"] = N(hum(3.0, 45, (1, 2, 3, 5), 0.6) * adsr(3, 0.4, 0.2, 0.9, 0.6) + lp(brown(3.0), 200) * 0.6)
x = np.zeros(int(SR * 4.0))
r = np.random.default_rng(7)
t = 0.0
while t < 3.8:
    x = place(x, hp(noise(0.02), r.uniform(1500, 3000)) * env_exp(0.02, 250) * r.uniform(0.3, 0.8), t)
    t += r.uniform(0.07, 0.22) if r.random() > 0.08 else r.uniform(0.4, 0.8)
S["typing"] = loopify(N(x) * 0.6, 0.05)
S["switch_flip"] = N(mx(thud(0.35, 70, 14) + metal(0.35, 600, 20) * 0.5, click(0.02, 1500)))
S["achievement"] = S["achievement"]

# ---------------- A-60 / A-60B ----------------
S["a60_rumble"] = N(lp(brown(1.8), 120) * adsr(1.8, 0.3, 0.3, 0.8, 0.8) + sine(np.linspace(45, 30, int(SR * 1.8)), 1.8) * adsr(1.8, 0.2, 0.3, 0.8, 0.8))
def crackle(dur, rate, seed):
    r = np.random.default_rng(seed)
    x = np.zeros(int(SR * dur))
    idx = r.integers(0, len(x) - 200, int(dur * rate))
    for i in idx:
        x[i:i + 60] += r.uniform(-1, 1) * np.exp(-np.arange(60) / 10)
    return x
S["a60_far"] = loopify(N(hp(crackle(4.25, 900, 2), 800) * 0.8 + bp(noise(4.25), 1500, 5000) * 0.2))
S["a60_near"] = loopify(N(dist(bp(noise(3.25), 2500, 9000) * (0.7 + 0.3 * sine(11, 3.25)) + saw(np.linspace(900, 900, int(SR * 3.25)) * (1 + 0.1 * sine(9, 3.25)), 3.25) * 0.25 + crackle(3.25, 1500, 3) * 0.6, 3)))
S["a60b_far"] = loopify(N(lp(crackle(4.25, 700, 4), 3000) * 0.6 + square(55 * (1 + 0.02 * sine(0.5, 4.25)), 4.25) * 0.15 + bp(noise(4.25), 400, 1500) * 0.3))
S["a60b_near"] = loopify(N(dist(bp(noise(3.25), 900, 4000) + bp(brown(3.25), 200, 1200) * (0.6 + 0.4 * sine(6, 3.25)) * 0.8, 4)))

# ---------------- A-90 / A-90B ----------------
x = np.zeros(int(SR * 0.9))
for at in (0.0, 0.22):
    x = place(x, thud(0.25, 140, 25) + lp(noise(0.25), 2500) * env_exp(0.25, 40) * 0.5, at)
S["a90_knock"] = N(x)
S["a90_spawn"] = N(glitchy(0.5, 2) + sine(np.linspace(1800, 300, int(SR * 0.5)), 0.5) * env_exp(0.5, 5) * 0.4)
S["a90_jumpscare_sound"] = N(dist(scream(0.9, 900, 300, 1.2) + noise(0.9) * env_exp(0.9, 3) * 0.6, 5))
S["a90_despawn"] = N(sine(np.linspace(300, 1600, int(SR * 0.25)), 0.25) * adsr(0.25, 0.01, 0.05, 0.6, 0.1)[::-1] * 0.8)
S["a90b_spawn"] = N(glitchy(0.6, 3))
S["a90b_halt"] = N(pad_to(place(np.zeros(int(SR * 0.6)), square(440, 0.18) * 0.5, 0) + place(np.zeros(int(SR * 0.6)), square(330, 0.25) * 0.5, 0.22), 0.6))
S["a90b_proceed"] = N(pad_to(place(np.zeros(int(SR * 0.6)), sine(523, 0.18), 0) + place(np.zeros(int(SR * 0.6)), sine(784, 0.25), 0.2), 0.6) * 0.8)
S["a90b_attack_sound"] = N(dist(scream(0.8, 700, 200, 1.0, (600, 1500, 3200)), 4))

# ---------------- A-120 ----------------
x = np.zeros(int(SR * 6.25))
r = np.random.default_rng(12)
t = 0.1
while t < 5.8:
    x = place(x, metal(0.9, r.uniform(150, 420), 5) * r.uniform(0.3, 0.8), t)
    t += r.uniform(0.5, 1.4)
S["a120_far"] = loopify(N(x + lp(brown(6.25), 150) * 0.4))
x = np.zeros(int(SR * 2.25))
for i in range(9):
    x = place(x, metal(0.3, 300 + (i % 3) * 70, 14) * 0.7, i * 0.24)
S["a120_near"] = loopify(N(dist(x, 2) + bp(noise(2.25), 600, 2000) * 0.2), 0.05)

# ---------------- A-200 ----------------
mel = [(i * 0.12, f, 0.11, 0.7) for i, f in enumerate((523, 659, 784, 659, 523, 784, 1047, 1319, 1047, 784, 659, 523))]
S["a200_spawn"] = N(crush(notes(mel, "square", 4), 3, 6))
S["a200_far"] = loopify(N(crush(sine(330 * (1 + 0.3 * sine(0.7, 4.25)), 4.25) * 0.6 + square(165 * (1 + 0.05 * sine(3, 4.25)), 4.25) * 0.2, 3, 8)))
S["a200_near"] = loopify(N(bp(noise(3.25), 2000, 8000) * (0.5 + 0.5 * sine(5, 3.25)) * 0.6 + crush(saw(440 * (1 + 0.5 * sine(0.9, 3.25)), 3.25), 2, 10) * 0.3))
# "it's not over. it never is." whispered-sung, then crushed
v = speak([("i", 0.18, 1.0), ("o", 0.25, 0.94), ("o", 0.2, 1.06), ("e", 0.3, 0.9), ("i", 0.16, 1.0), ("e", 0.18, 1.08), ("e", 0.22, 0.95), ("i", 0.45, 0.85)], 330, 0.06, consonant=(1, 2, 4, 7))
S["a200_voice"] = N(crush(v * 0.8 + bp(noise(len(v) / SR), 2000, 6000) * 0.08, 5, 3))

# ---------------- the "more friends" six ----------------
S["rush_far"] = loopify(N(lp(brown(4.25), 900) * (0.6 + 0.4 * sine(0.5, 4.25)) + bp(noise(4.25), 200, 900) * 0.5 + crackle(4.25, 400, 5) * 0.3))
S["ambush_far"] = loopify(N(dist(bp(noise(4.25), 300, 2500) * (0.5 + 0.5 * square(8, 4.25)) + bp(noise(4.25), 2500, 7000) * 0.25, 2)))
S["eyes_ambience"] = loopify(N(sum(vowel("o", f, 6.25, f * 1.003, 0.05) * (0.7 + 0.3 * sine(0.2 + i * 0.07, 6.25)) for i, f in enumerate((220, 277.2, 329.6, 415.3)))))
S["eyes_hurt"] = N(glitchy(0.3, 4) + sine(2000, 0.3) * env_exp(0.3, 12) * 0.3)
S["screech_psst"] = N(pad_to(bp(noise(0.12), 900, 3000) * adsr(0.12, 0.01, 0.03, 0.6, 0.05) * 0.6, 0.6) + place(np.zeros(int(SR * 0.6)), bp(noise(0.3), 3500, 9000) * adsr(0.3, 0.05, 0.05, 0.8, 0.15), 0.12))
S["screech_bite"] = N(scream(0.5, 1200, 600, 1.0, (1500, 2800, 4200)) + thud(0.5, 90, 12) * 0.6 + hp(noise(0.5), 1500) * env_exp(0.5, 20) * 0.5)
S["screech_caught"] = N(glitchy(0.4, 5) + sine(np.linspace(600, 1800, int(SR * 0.4)), 0.4) * env_exp(0.4, 6) * 0.3)
S["figure_step"] = N(thud(0.45, 45, 9) + lp(noise(0.45), 400) * env_exp(0.45, 15) * 0.5)
S["figure_growl"] = N(dist(bp(brown(1.3), 80, 700) * (0.55 + 0.45 * sine(np.linspace(28, 18, int(SR * 1.3)), 1.3)) * adsr(1.3, 0.2, 0.2, 0.8, 0.4), 2.5) + bp(noise(1.3), 300, 900) * adsr(1.3, 0.2, 0.2, 0.6, 0.4) * 0.25)
S["figure_kill"] = N(mx(dist(bp(brown(1.1), 70, 900) * adsr(1.1, 0.02, 0.2, 0.8, 0.4), 4) * 0.8, scream(1.0, 500, 300, 1.2) * 0.5, thud(1.1, 40, 5)))
S["seek_eyes"] = N(lp(noise(1.0), 1200) * adsr(1.0, 0.3, 0.2, 0.7, 0.4) * 0.6 + sum(place(np.zeros(int(SR * 1.0)), sine(np.linspace(300, 120, int(SR * 0.15)), 0.15) * env_exp(0.15, 20) * 0.6, a) for a in (0.1, 0.3, 0.45, 0.6, 0.8)))
S["seek_scream"] = N(dist(scream(1.6, 600, 160, 1.0), 3))
S["seek_steps"] = loopify(N(steps_loop(2.25, 0.25, 1.3, 0.01, 6)), 0.02)
S["seek_kill"] = N(dist(scream(1.0, 800, 200, 1.2), 4) + thud(1.0, 50, 6))

# ---------------- The Wires ----------------
S["w10_spark"] = N(crackle(0.5, 3000, 8) * env_exp(0.5, 5) + hp(noise(0.5), 3000) * env_exp(0.5, 12) * 0.4)
S["w10_lunge"] = N(dist(saw(np.linspace(200, 1400, int(SR * 0.4)), 0.4) * 0.6 + crackle(0.4, 4000, 9), 3) * adsr(0.4, 0.01, 0.05, 0.8, 0.1))
S["w15_zap"] = N(dist(square(120 * (1 + 0.5 * noise(0.35)), 0.35) * 0.5 + crackle(0.35, 5000, 10), 3) * env_exp(0.35, 7))
S["w50_hum"] = loopify(N(hum(6.25, 50, (1, 2, 3, 4, 5, 7), 0.4) * (0.8 + 0.2 * sine(0.3, 6.25))))
S["wires_ambience"] = loopify(N(hum(6.25, 60, (1, 3, 5), 0.2) * 0.4 + crackle(6.25, 60, 11) * 0.5 + lp(brown(6.25), 200) * 0.4))
S["w30_hiss"] = N(hp(noise(2.2), 2500) * adsr(2.2, 0.4, 0.3, 0.7, 0.5))
S["w30_blast"] = N(hp(noise(1.2), 600) * adsr(1.2, 0.01, 0.2, 0.6, 0.6) + thud(1.2, 50, 6) * 0.6)

# ---------------- The Outside ----------------
S["city_ambience"] = loopify(N(lp(brown(8.25), 500) * 0.7 + bp(pink(8.25), 200, 1500) * 0.3 + sine(np.linspace(80, 110, int(SR * 8.25)), 8.25) * np.clip(sine(0.12, 8.25), 0, 1) * 0.15))
S["car_horn"] = N(dist((square(415, 0.9) + square(523, 0.9)) * 0.5, 2) * adsr(0.9, 0.01, 0.05, 0.9, 0.08) * 0.6)
S["headlights_far"] = loopify(N(bp(brown(4.25), 60, 500) * (0.75 + 0.25 * sine(4, 4.25)) + bp(noise(4.25), 800, 3000) * 0.15))
S["billboard_hum"] = loopify(N(hum(4.25, 120, (1, 2, 3, 5), 0.1) * (0.85 + 0.15 * (rng.random(int(SR * 4.25)) > 0.002))))
coo = np.zeros(int(SR * 0.7))
for a in (0.0, 0.32):
    coo = place(coo, lp(sine(np.linspace(260, 310, int(SR * 0.28)) * (1 + 0.15 * sine(18, 0.28)), 0.28), 900) * adsr(0.28, 0.04, 0.05, 0.8, 0.1), a)
S["c15_coo"] = N(coo)
x = np.zeros(int(SR * 1.5))
for i in range(14):
    x = place(x, lp(noise(0.05), 1800) * env_exp(0.05, 60) * (1 - i / 16), i * 0.09)
S["c15_flap"] = N(x)

# ---------------- the rest of the entities ----------------
# "IM AN INTROVERT": shouted, high and strained
S["introvert"] = N(dist(speak([("a", 0.14, 1.0), ("i", 0.08, 1.05), ("a", 0.14, 1.1), ("i", 0.1, 1.0), ("o", 0.14, 1.15), ("e", 0.24, 1.25)], 430, 0.02, consonant=(3, 4, 5)), 1.8))
S["a70_scream"] = N(scream(0.8, 520, 380, 0.9) + metal(0.8, 300, 4) * 0.4)
S["a25_hum"] = loopify(N(sum(sine(f * (1 + 0.01 * sine(0.4 + i * 0.13, 6.25)), 6.25) for i, f in enumerate((70, 105, 140.5, 211))) * (0.6 + 0.4 * sine(0.33, 6.25)) + bp(pink(6.25), 300, 900) * 0.3))
x = np.zeros(int(SR * 4.25))
r = np.random.default_rng(15)
for _ in range(40):
    x = place(x, sine(r.choice((1568, 1760, 2093, 2349, 2637)), 0.4) * env_exp(0.4, 9) * r.uniform(0.2, 0.6), r.uniform(0, 3.8))
S["a150_hum"] = loopify(N(x + sine(196, 4.25) * 0.2 + sine(294, 4.25) * 0.1))
S["a150_kill"] = N(dist(scream(0.6, 1000, 600, 0.6, (1200, 2400, 3600)), 3))
S["a220_march"] = loopify(N(steps_loop(2.25, 0.375, 1.0, 0.0, 16)), 0.02)
S["gate_beep"] = N(pad_to(sine(1400, 0.15) * adsr(0.15, 0.005, 0.02, 0.9, 0.02), 0.5) + place(np.zeros(int(SR * 0.5)), sine(1400, 0.15) * adsr(0.15, 0.005, 0.02, 0.9, 0.02), 0.25))
S["fire_alarm"] = loopify(N(square(np.where((t_(4.25) % 0.5) < 0.25, 950, 750), 4.25) * 0.4 + saw(np.where((t_(4.25) % 0.5) < 0.25, 950, 750), 4.25) * 0.2), 0.01)
br = np.zeros(int(SR * 4.25))
for a in (0.0, 2.1):
    br = place(br, bp(noise(1.0), 300, 1600) * adsr(1.0, 0.4, 0.2, 0.7, 0.4) * 0.7, a)
    br = place(br, bp(noise(1.0), 500, 2500) * adsr(1.0, 0.2, 0.2, 0.6, 0.5) * 0.5, a + 1.05)
S["anomaly_breath"] = loopify(N(br), 0.05)
S["anomaly_scream"] = N(dist(scream(0.7, 650, 300, 1.0), 4))
S["a10_kick"] = N(mx(thud(0.4, 60, 10) * 1.2 + metal(0.4, 240, 10) * 0.7, click(0.02, 1200)))
S["a20_burst"] = N(mx(lp(noise(0.25), 3000) * env_exp(0.25, 18) * 0.8, place(np.zeros(int(SR * 0.6)), dist(scream(0.45, 600, 350, 0.8), 3), 0.08)))
S["ghlicht_part"] = N(mx(glitchy(0.4, 6) * 0.6, pad_to(notes([(0.3, 1047, 0.3, 0.7), (0.4, 1568, 0.4, 0.7)], dec=6), 0.8)))

# ---------------- music ----------------
def menu_music():
    """slow, cold, a little sad: a minor pad, a soft piano-ish line, the office hum under it"""
    dur = 64.0
    bpm = 60
    beat = 60 / bpm
    out = np.zeros(int(SR * dur))
    chords = [(110, 130.8, 164.8), (87.3, 110, 130.8), (98, 123.5, 146.8), (82.4, 103.8, 123.5)]  # Am F G E
    for bar in range(16):
        ch = chords[bar % 4]
        seg = sum(saw(f * (1 + 0.003 * sine(0.2, beat * 4)), beat * 4) + saw(f * 2.003, beat * 4) * 0.5 for f in ch)
        seg = lp(seg, 900) * adsr(beat * 4, 0.8, 0.5, 0.8, 1.2) * 0.25
        out = place(out, seg, bar * beat * 4)
    line = [440, 523.3, 493.9, 440, 392, 440, 349.2, 329.6]
    for i in range(32):
        if i % 4 == 3:
            continue
        f = line[(i * 3) % len(line)]
        n = (sine(f, 1.6) + 0.4 * sine(f * 2, 1.6) + 0.15 * sine(f * 3, 1.6)) * env_exp(1.6, 3.5) * 0.22
        out = place(out, n, 2.0 + i * beat * 1.875)
    out += hum(dur, 60, (1, 2, 3), 0.2) * 0.05
    # a little echo
    d = int(SR * 0.45)
    out[d:] += out[:-d] * 0.35
    return loopify(N(out), 1.0)

def chase_music():
    """A-60B: fast and loud. kick on every beat, a driving bass, a stabbing lead, noise snares"""
    bpm = 170
    beat = 60 / bpm
    bars = 16
    dur = bars * 4 * beat
    out = np.zeros(int(SR * dur) + SR)
    kick = sine(np.geomspace(150, 45, int(SR * 0.25)), 0.25) * env_exp(0.25, 14)
    snare = hp(noise(0.18), 900) * env_exp(0.18, 22) * 0.6 + sine(190, 0.18) * env_exp(0.18, 30) * 0.4
    hat = hp(noise(0.05), 6000) * env_exp(0.05, 80) * 0.25
    roots = [55, 55, 65.4, 49]  # A A C G
    for b in range(bars * 4):
        at = b * beat
        out = place(out, kick, at)
        if b % 2 == 1:
            out = place(out, snare, at)
        for h in range(2):
            out = place(out, hat, at + h * beat / 2)
        root = roots[(b // 4) % 4]
        for s in range(2):
            note = dist(lp(saw(root * (2 if s else 1), beat / 2) + square(root * 1.005, beat / 2) * 0.5, 700), 2) * env_exp(beat / 2, 6) * 0.45
            out = place(out, note, at + s * beat / 2)
        if (b % 8) in (0, 3, 6):
            stab = dist(sum(saw(root * 4 * m, beat * 0.6) for m in (1, 1.189, 1.498)), 2) * env_exp(beat * 0.6, 7) * 0.18
            out = place(out, hp(stab, 400), at)
    out = out[:int(SR * dur)]
    return loopify(N(dist(out, 1.5)), 0.02)

S["menu_music"] = menu_music()
S["a60b_theme"] = chase_music()

for k, x in S.items():
    if x is None:
        continue
    y = np.clip(np.asarray(x, dtype=float), -1, 1)
    wavfile.write(os.path.join(OUT, k + ".wav"), SR, (y * 32767).astype(np.int16))
print(len([1 for v in S.values() if v is not None]), "sounds")
