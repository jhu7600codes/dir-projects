// Generates simple M3-styled PNG app icons for the PWA manifest without any
// external image/canvas dependency — just raw pixel math + Node's built-in
// zlib deflate, wrapped in a hand-rolled minimal PNG encoder.
import { deflateSync } from "node:zlib";
import { writeFileSync, mkdirSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";

const __dirname = dirname(fileURLToPath(import.meta.url));
const outDir = join(__dirname, "..", "public", "icons");
mkdirSync(outDir, { recursive: true });

// androdrop seed color — indigo/blue "drop" theme.
const PRIMARY = [76, 110, 245]; // #4C6EF5
const ON_PRIMARY = [255, 255, 255];

const CRC_TABLE = (() => {
  const table = new Uint32Array(256);
  for (let n = 0; n < 256; n++) {
    let c = n;
    for (let k = 0; k < 8; k++) {
      c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
    }
    table[n] = c >>> 0;
  }
  return table;
})();

function crc32(buf) {
  let crc = 0xffffffff;
  for (let i = 0; i < buf.length; i++) {
    crc = CRC_TABLE[(crc ^ buf[i]) & 0xff] ^ (crc >>> 8);
  }
  return (crc ^ 0xffffffff) >>> 0;
}

function chunk(type, data) {
  const typeBuf = Buffer.from(type, "ascii");
  const lenBuf = Buffer.alloc(4);
  lenBuf.writeUInt32BE(data.length, 0);
  const crcBuf = Buffer.alloc(4);
  crcBuf.writeUInt32BE(crc32(Buffer.concat([typeBuf, data])), 0);
  return Buffer.concat([lenBuf, typeBuf, data, crcBuf]);
}

/** @param {(x:number, y:number, size:number) => [number,number,number,number]} paint */
function encodePng(size, paint) {
  const raw = Buffer.alloc(size * (1 + size * 4));
  for (let y = 0; y < size; y++) {
    const rowStart = y * (1 + size * 4);
    raw[rowStart] = 0; // filter: none
    for (let x = 0; x < size; x++) {
      const [r, g, b, a] = paint(x, y, size);
      const px = rowStart + 1 + x * 4;
      raw[px] = r;
      raw[px + 1] = g;
      raw[px + 2] = b;
      raw[px + 3] = a;
    }
  }

  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(size, 0);
  ihdr.writeUInt32BE(size, 4);
  ihdr[8] = 8; // bit depth
  ihdr[9] = 6; // color type: RGBA
  ihdr[10] = 0;
  ihdr[11] = 0;
  ihdr[12] = 0;

  const idat = deflateSync(raw);
  const signature = Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]);

  return Buffer.concat([
    signature,
    chunk("IHDR", ihdr),
    chunk("IDAT", idat),
    chunk("IEND", Buffer.alloc(0)),
  ]);
}

function roundedSquareWithDrop({ cornerRatio, dropRatio, opaqueBg }) {
  return (x, y, size) => {
    const r = size * cornerRatio;
    const cx = Math.min(Math.max(x, r), size - r);
    const cy = Math.min(Math.max(y, r), size - r);
    const dx = x - cx;
    const dy = y - cy;
    const inRoundedSquare = dx * dx + dy * dy <= r * r;

    if (!inRoundedSquare) {
      return opaqueBg ? [...PRIMARY, 255] : [0, 0, 0, 0];
    }

    // A simple "drop" glyph: a filled circle with a small pointed tail,
    // reading as a single droplet — standing in for a shared/dropped file.
    const center = size / 2;
    const dropR = size * dropRatio;
    const px = x - center;
    const py = y - center * 0.92;

    const inCircle = px * px + py * py <= dropR * dropR;
    const inTail =
      py < 0 && Math.abs(px) < dropR * (1 + py / (dropR * 1.6)) && py > -dropR * 1.6;

    if (inCircle || inTail) {
      return [...ON_PRIMARY, 255];
    }
    return [...PRIMARY, 255];
  };
}

const targets = [
  { name: "icon-192.png", size: 192, cornerRatio: 0.22, dropRatio: 0.26, opaqueBg: true },
  { name: "icon-512.png", size: 512, cornerRatio: 0.22, dropRatio: 0.26, opaqueBg: true },
  // Maskable: full-bleed background, glyph kept inside the ~80% safe zone.
  { name: "maskable-512.png", size: 512, cornerRatio: 0, dropRatio: 0.2, opaqueBg: true },
  { name: "apple-touch-icon.png", size: 180, cornerRatio: 0.22, dropRatio: 0.26, opaqueBg: true },
];

for (const t of targets) {
  const png = encodePng(
    t.size,
    roundedSquareWithDrop({
      cornerRatio: t.cornerRatio,
      dropRatio: t.dropRatio,
      opaqueBg: t.opaqueBg,
    }),
  );
  writeFileSync(join(outDir, t.name), png);
  console.log(`wrote public/icons/${t.name} (${png.length} bytes)`);
}
