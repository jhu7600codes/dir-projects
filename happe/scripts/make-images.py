#!/usr/bin/env python3
# writes the launch images and app icon into the given .app directory.
# pure stdlib so it runs anywhere.
import struct
import sys
import zlib
from pathlib import Path


def png(path, w, h, pixel):
    rows = bytearray()
    for y in range(h):
        rows.append(0)
        for x in range(w):
            rows.extend(pixel(x, y))

    def chunk(tag, data):
        c = struct.pack(">I", len(data)) + tag + data
        return c + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    data = b"\x89PNG\r\n\x1a\n"
    data += chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 2, 0, 0, 0))
    data += chunk(b"IDAT", zlib.compress(bytes(rows), 9))
    data += chunk(b"IEND", b"")
    Path(path).write_bytes(data)


def icon_pixel(size):
    bg = (24, 38, 64)
    fg = (120, 200, 255)

    def px(x, y):
        # a "v": two thick strokes meeting at the bottom middle
        u, v = x / size, y / size
        if 0.25 <= v <= 0.75:
            t = (v - 0.25) / 0.5
            left = 0.22 + 0.28 * t
            right = 0.78 - 0.28 * t
            if abs(u - left) < 0.07 or abs(u - right) < 0.07:
                return fg
        return bg

    return px


def main():
    app = Path(sys.argv[1])
    white = lambda x, y: (255, 255, 255)
    png(app / "Default-568h@2x.png", 640, 1136, white)
    png(app / "Default-667h@2x.png", 750, 1334, white)
    png(app / "AppIcon60x60@2x.png", 120, 120, icon_pixel(120))
    png(app / "AppIcon60x60@3x.png", 180, 180, icon_pixel(180))


if __name__ == "__main__":
    main()
