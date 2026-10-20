"""pulls every official asset out of the-rooms build and drops it into app/src/main/assets.

usage: python3 tools/import_the_rooms.py <the-rooms.apk> <the-rooms gh-pages img dir>
needs ffmpeg on PATH and the `zstandard` pip package (for the font).
"""
import os, sys, glob, shutil, struct, subprocess, zipfile, tempfile

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gdres

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, 'app', 'src', 'main', 'assets')
RES = os.path.join(ROOT, 'app', 'src', 'main', 'res')

# anything longer than this gets a short sfx cut too, the full file stays as music
SFX_MAX = 6.0

# two textures share a base name in the original project
RENAME = {'a200_face.png': 'a200_face', 'a200_face.webp': 'a200_room'}


def dur(path):
    out = subprocess.run(['ffprobe', '-v', 'error', '-show_entries', 'format=duration', '-of', 'csv=p=0', path],
                         capture_output=True, text=True).stdout.strip().splitlines()
    return float(out[-1]) if out else 0.0


def ff(*args):
    subprocess.run(['ffmpeg', '-v', 'error', '-y', *args], check=True)


def font(src, dst):
    import zstandard
    b = open(src, 'rb').read()
    _, bs, total = struct.unpack_from('<III', b, 4)
    nb = (total + bs - 1) // bs
    sizes = struct.unpack_from('<%dI' % nb, b, 16)
    p, out = 16 + 4 * nb, b''
    for s in sizes:
        out += zstandard.ZstdDecompressor().decompress(b[p:p + s], max_output_size=bs)
        p += s
    i = -1
    while True:
        i = out.find(b'\x00\x01\x00\x00', i + 1)
        n = struct.unpack_from('<I', out, i - 4)[0]
        if 10000 < n <= len(out) - i:
            open(dst, 'wb').write(out[i:i + n])
            return


def main(apk, imgdir):
    tmp = tempfile.mkdtemp()
    zipfile.ZipFile(apk).extractall(tmp, [n for n in zipfile.ZipFile(apk).namelist() if n.startswith('assets/.godot/imported/')])
    imp = os.path.join(tmp, 'assets', '.godot', 'imported')
    for d in ('img', 'sfx', 'music', 'bg', 'font'):
        os.makedirs(os.path.join(OUT, d), exist_ok=True)

    for f in sorted(os.listdir(imp)):
        src = os.path.join(imp, f)
        orig = f.split('-')[0]
        name = RENAME.get(orig, orig.rsplit('.', 1)[0])
        if f.endswith('.ctex'):
            if name.startswith('android_') or name.startswith('icon'):
                continue
            got = gdres.extract(src, os.path.join(tmp, name))
            dst = os.path.join(OUT, 'bg' if name.startswith('floor_') else 'img', name + '.webp')
            if got.endswith('.png'):
                ff('-i', got, dst)
            else:
                shutil.copy(got, dst)
        elif f.endswith(('.mp3str', '.oggvorbisstr', '.sample')):
            got = gdres.extract(src, os.path.join(tmp, name))
            d = dur(got)
            if d > SFX_MAX:
                ff('-i', got, '-map_metadata', '-1', '-vn', '-ac', '2', '-c:a', 'libvorbis', '-q:a', '2',
                   os.path.join(OUT, 'music', name + '.ogg'))
                ff('-i', got, '-vn', '-t', '4', '-af', 'afade=t=out:st=3:d=1', '-ac', '1', '-ar', '44100',
                   '-c:a', 'libvorbis', '-q:a', '3', os.path.join(OUT, 'sfx', name + '.ogg'))
            else:
                ff('-i', got, '-vn', '-ac', '1', '-ar', '44100', '-c:a', 'libvorbis', '-q:a', '3',
                   os.path.join(OUT, 'sfx', name + '.ogg'))
        elif f.endswith('.fontdata'):
            font(src, os.path.join(OUT, 'font', 'caveat.ttf'))

    for f in glob.glob(os.path.join(imgdir, '*.jpg')):
        shutil.copy(f, os.path.join(OUT, 'bg', os.path.basename(f)))

    # launcher icon from the-rooms' own adaptive icon layers
    for layer in ('fg', 'bg'):
        got = gdres.extract(glob.glob(os.path.join(imp, 'android_adaptive_%s_432.png-*.ctex' % layer))[0],
                            os.path.join(tmp, layer))
        os.makedirs(os.path.join(RES, 'mipmap-xxxhdpi'), exist_ok=True)
        ff('-i', got, os.path.join(RES, 'mipmap-xxxhdpi', 'ic_launcher_%s.png' % layer))
    got = gdres.extract(glob.glob(os.path.join(imp, 'android_icon_192.png-*.ctex'))[0], os.path.join(tmp, 'icon'))
    ff('-i', got, os.path.join(RES, 'mipmap-xxxhdpi', 'ic_launcher.png'))
    shutil.rmtree(tmp)


if __name__ == '__main__':
    main(sys.argv[1], sys.argv[2])
