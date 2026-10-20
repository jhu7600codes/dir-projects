"""minimal godot 4 binary resource reader + extractors for ctex / mp3str / oggvorbisstr / sample"""
import struct, sys, os, wave, io

class R:
    def __init__(s, b, p=0): s.b, s.p = b, p
    def u32(s): v = struct.unpack_from('<I', s.b, s.p)[0]; s.p += 4; return v
    def i32(s): v = struct.unpack_from('<i', s.b, s.p)[0]; s.p += 4; return v
    def u64(s): v = struct.unpack_from('<Q', s.b, s.p)[0]; s.p += 8; return v
    def i64(s): v = struct.unpack_from('<q', s.b, s.p)[0]; s.p += 8; return v
    def f32(s): v = struct.unpack_from('<f', s.b, s.p)[0]; s.p += 4; return v
    def f64(s): v = struct.unpack_from('<d', s.b, s.p)[0]; s.p += 8; return v
    def raw(s, n): v = s.b[s.p:s.p + n]; s.p += n; return v
    def ustr(s):
        n = s.u32()
        return s.raw(n).rstrip(b'\0').decode('utf8', 'replace')

def variant(r, strtab):
    t = r.u32()
    if t == 1: return None
    if t == 2: return bool(r.u32())
    if t == 3: return r.i32()
    if t == 40: return r.i64()
    if t == 4: return r.f32()
    if t == 41: return r.f64()
    if t in (5, 44):
        return r.ustr()
    if t == 10: return (r.f32(), r.f32())
    if t == 12: return (r.f32(), r.f32(), r.f32())
    if t == 20: return tuple(r.f32() for _ in range(4))
    if t == 24:
        st = r.u32()
        if st == 0: return None
        if st in (1,):
            return ('ext', r.ustr(), r.ustr())
        return ('res', r.u32())
    if t == 26:
        n = r.u32() & 0x7fffffff
        return {str(variant(r, strtab)): variant(r, strtab) for _ in range(n)}
    if t == 30:
        n = r.u32() & 0x7fffffff
        return [variant(r, strtab) for _ in range(n)]
    if t == 31:
        n = r.u32(); v = r.raw(n); r.p += (4 - n % 4) % 4; return bytes(v)
    if t == 32:
        n = r.u32(); return list(struct.unpack_from('<%di' % n, r.raw(4 * n)))
    if t == 33:
        n = r.u32(); return list(struct.unpack_from('<%df' % n, r.raw(4 * n)))
    if t == 48:
        n = r.u32(); return list(struct.unpack_from('<%dq' % n, r.raw(8 * n)))
    if t == 49:
        n = r.u32(); return list(struct.unpack_from('<%dd' % n, r.raw(8 * n)))
    raise ValueError('variant type %d at %d' % (t, r.p))

def load(path):
    b = open(path, 'rb').read()
    r = R(b, 4)
    r.u32(); r.u32(); r.u32(); r.u32(); fmt = r.u32()
    rtype = r.ustr()
    r.u64()  # import md ofs
    flags = r.u32(); r.u64()  # uid
    if flags & 8: r.ustr()  # script class
    r.p += 11 * 4
    strtab = [r.ustr() for _ in range(r.u32())]
    for _ in range(r.u32()):
        r.ustr(); r.ustr()
        if flags & 2: r.u64()
    offs = []
    for _ in range(r.u32()):
        r.ustr(); offs.append(r.u64())
    res = []
    for o in offs:
        r.p = o
        typ = r.ustr()
        props = {}
        for _ in range(r.u32()):
            name = strtab[r.u32()]
            props[name] = variant(r, strtab)
        res.append((typ, props))
    return res

def ogg_crc(data):
    crc = 0
    for byte in data:
        crc ^= byte << 24
        for _ in range(8):
            crc = ((crc << 1) ^ 0x04C11DB7) if crc & 0x80000000 else (crc << 1)
            crc &= 0xffffffff
    return crc

def ogg_page(serial, seq, granule, packets, htype):
    segs = []
    for p in packets:
        n = len(p)
        while n >= 255: segs.append(255); n -= 255
        segs.append(n)
    assert len(segs) <= 255
    hdr = b'OggS' + bytes([0, htype]) + struct.pack('<qIII', granule, serial, seq, 0) + bytes([len(segs)]) + bytes(segs)
    body = b''.join(packets)
    page = bytearray(hdr + body)
    struct.pack_into('<I', page, 22, ogg_crc(page))
    return bytes(page)

def write_ogg(seq_props, out):
    pages = seq_props['packet_data']
    gran = seq_props.get('granule_positions', [0] * len(pages))
    buf = io.BytesIO()
    serial = 0x5eed
    n = 0
    big = max((len(p) for pk in pages for p in pk), default=0) > 60000
    if big:
        # one packet at a time, spilling oversized packets over continued pages
        for i, pk in enumerate(pages):
            for j, p in enumerate(pk):
                last_pkt = j == len(pk) - 1
                parts = [p[k:k + 255 * 254] for k in range(0, len(p), 255 * 254)] or [b'']
                for k, part in enumerate(parts):
                    h = (2 if n == 0 else 0) | (1 if k > 0 else 0)
                    final = k == len(parts) - 1
                    if i == len(pages) - 1 and last_pkt and final: h |= 4
                    if final:
                        segs_part = part
                        g = gran[i] if last_pkt else -1
                        buf.write(ogg_page(serial, n, g, [segs_part], h))
                    else:
                        # exactly 254 full lacing values, packet continues
                        hdr = b'OggS' + bytes([0, h]) + struct.pack('<qIII', -1, serial, n, 0) + bytes([254]) + bytes([255] * 254)
                        pg = bytearray(hdr + part)
                        struct.pack_into('<I', pg, 22, ogg_crc(pg))
                        buf.write(bytes(pg))
                    n += 1
        open(out, 'wb').write(buf.getvalue())
        return
    for i, pk in enumerate(pages):
        # split pages that would overflow 255 segments
        chunk = []; segc = 0
        groups = []
        for p in pk:
            s = len(p) // 255 + 1
            if segc + s > 255 and chunk:
                groups.append(chunk); chunk = []; segc = 0
            chunk.append(p); segc += s
        groups.append(chunk)
        for gi, g in enumerate(groups):
            h = 0
            if n == 0: h |= 2
            if i == len(pages) - 1 and gi == len(groups) - 1: h |= 4
            g_pos = gran[i] if gi == len(groups) - 1 else -1
            buf.write(ogg_page(serial, n, g_pos, g, h)); n += 1
    open(out, 'wb').write(buf.getvalue())

def _rnd(x):
    return int(x + 0.5) if x >= 0 else -int(-x + 0.5)

QOA_DQ = [[_rnd(_rnd((s + 1) ** 2.75) * d) for d in (0.75, -0.75, 2.5, -2.5, 4.5, -4.5, 7, -7)] for s in range(16)]

def qoa_decode(b):
    """returns (channels, rate, interleaved int16 bytes)"""
    total = struct.unpack_from('>I', b, 4)[0]
    p = 8; out = []; ch = 1; rate = 44100
    while p < len(b) and len(out) // max(ch, 1) < total:
        ch = b[p]; rate = int.from_bytes(b[p + 1:p + 4], 'big')
        fs, fsize = struct.unpack_from('>HH', b, p + 4); p += 8
        hist, wts = [], []
        for c in range(ch):
            hist.append(list(struct.unpack_from('>4h', b, p))); p += 8
            wts.append(list(struct.unpack_from('>4h', b, p))); p += 8
        frame = [0] * (fs * ch)
        for si in range(0, fs, 20):
            for c in range(ch):
                sl = struct.unpack_from('>Q', b, p)[0]; p += 8
                dq = QOA_DQ[sl >> 60]
                h, w = hist[c], wts[c]
                for i in range(si, min(si + 20, fs)):
                    q = (sl >> 57) & 7; sl = (sl << 3) & 0xffffffffffffffff
                    pred = (h[0] * w[0] + h[1] * w[1] + h[2] * w[2] + h[3] * w[3]) >> 13
                    r = dq[q]
                    s = max(-32768, min(32767, pred + r))
                    d = r >> 4
                    for k in range(4): w[k] += -d if h[k] < 0 else d
                    h[0], h[1], h[2], h[3] = h[1], h[2], h[3], s
                    frame[i * ch + c] = s
        out.extend(frame)
    out = out[:total * ch]
    return ch, rate, struct.pack('<%dh' % len(out), *out)

def extract(src, dst_noext):
    if src.endswith('.ctex'):
        b = open(src, 'rb').read()
        i = b.find(b'RIFF')
        if i >= 0:
            n = struct.unpack_from('<I', b, i + 4)[0] + 8
            open(dst_noext + '.webp', 'wb').write(b[i:i + n]); return dst_noext + '.webp'
        i = b.find(b'\x89PNG')
        n = struct.unpack_from('<I', b, i - 4)[0]
        open(dst_noext + '.png', 'wb').write(b[i:i + n]); return dst_noext + '.png'
    res = load(src)
    if src.endswith('.mp3str'):
        data = [p for t, p in res if 'data' in p][0]['data']
        open(dst_noext + '.mp3', 'wb').write(data); return dst_noext + '.mp3'
    if src.endswith('.oggvorbisstr'):
        seq = [p for t, p in res if t == 'OggPacketSequence'][0]
        write_ogg(seq, dst_noext + '.ogg'); return dst_noext + '.ogg'
    if src.endswith('.sample'):
        p = [p for t, p in res if 'data' in p][0]
        fmt = p.get('format', 1)
        if fmt == 3:
            ch, rate, pcm = qoa_decode(p['data'])
            w = wave.open(dst_noext + '.wav', 'wb')
            w.setnchannels(ch); w.setsampwidth(2); w.setframerate(rate)
            w.writeframes(pcm); w.close(); return dst_noext + '.wav'
        if fmt not in (0, 1):
            raise ValueError('wav format %s (compressed) %s' % (fmt, src))
        ch = 2 if p.get('stereo') else 1
        w = wave.open(dst_noext + '.wav', 'wb')
        w.setnchannels(ch); w.setsampwidth(1 if fmt == 0 else 2); w.setframerate(p.get('mix_rate', 44100))
        d = p['data']
        if fmt == 0: d = bytes((x + 128) & 255 for x in d)
        w.writeframes(d); w.close(); return dst_noext + '.wav'

if __name__ == '__main__':
    if sys.argv[1] == 'dump':
        for t, p in load(sys.argv[2]):
            print(t, {k: (v if not isinstance(v, (bytes, list)) else '%s[%d]' % (type(v).__name__, len(v))) for k, v in p.items()})
    else:
        print(extract(sys.argv[1], sys.argv[2]))
