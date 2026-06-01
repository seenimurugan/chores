/**
 * gen-icons.mjs — deterministic PWA icon generator for chores-frontend.
 *
 * Generates all required PWA icons from a programmatically-constructed SVG
 * using the `sharp` library (no external binary needed).
 *
 * Outputs into frontend/public/:
 *   icon-192.png           — standard 192×192 icon
 *   icon-512.png           — standard 512×512 icon
 *   icon-maskable-512.png  — 512×512 with safe-zone padding (purpose: maskable)
 *   apple-touch-icon.png   — 180×180 for iOS
 *   favicon.ico            — 32×32 favicon (PNG wrapped)
 *
 * Usage (from repo root or frontend/):
 *   node scripts/gen-icons.mjs
 *
 * Requires: sharp (devDependency)
 */

import sharp from 'sharp';
import { readFileSync, writeFileSync } from 'fs';
import { join, dirname } from 'path';
import { fileURLToPath } from 'url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const PUBLIC_DIR = join(__dirname, '..', 'public');

const THEME_COLOR = '#4263eb';   // indigo brand colour
const BG_COLOR    = '#ffffff';   // fallback (used in favicon bg)

// ---------------------------------------------------------------------------
// SVG builder: blue rounded-rect background + white check-mark glyph
// The check-mark path is hand-coded (relative coords) so it's reproducible
// with no external font/glyph dependency.
// ---------------------------------------------------------------------------
function buildSvg(size, paddingFactor = 0.1) {
  const pad    = Math.round(size * paddingFactor);
  const r      = Math.round(size * 0.18);          // corner radius
  const inner  = size - 2 * pad;                   // drawable area side length

  // Check-mark: two segments forming a ✓, scaled to ~60% of the inner area.
  // Coordinates are relative to the padded square's top-left corner.
  const cx = pad + inner / 2;
  const cy = pad + inner / 2;
  const unit = inner * 0.28;   // scaling unit

  // Short arm of the check (bottom-left to valley)
  const x1 = cx - unit * 1.05;
  const y1 = cy + unit * 0.1;
  const x2 = cx - unit * 0.25;
  const y2 = cy + unit * 0.9;

  // Long arm of the check (valley to top-right)
  const x3 = cx + unit * 1.05;
  const y3 = cy - unit * 0.85;

  const strokeW = Math.max(4, Math.round(inner * 0.11));

  return `<svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}" viewBox="0 0 ${size} ${size}">
  <rect x="${pad}" y="${pad}" width="${inner}" height="${inner}" rx="${r}" ry="${r}" fill="${THEME_COLOR}"/>
  <polyline
    points="${x1},${y1} ${x2},${y2} ${x3},${y3}"
    fill="none"
    stroke="white"
    stroke-width="${strokeW}"
    stroke-linecap="round"
    stroke-linejoin="round"
  />
</svg>`;
}

// Maskable icon needs extra padding so the glyph stays within the safe zone
// (centre 80% of the image). We use 15% padding on each side.
function buildMaskableSvg(size) {
  return buildSvg(size, 0.12);
}

// ---------------------------------------------------------------------------
// Favicon: 32×32 with coloured background (no transparency — .ico compat)
// ---------------------------------------------------------------------------
function buildFaviconSvg(size = 32) {
  const r = Math.round(size * 0.18);
  const unit = size * 0.22;
  const cx = size / 2;
  const cy = size / 2;

  const x1 = cx - unit * 1.05;
  const y1 = cy + unit * 0.1;
  const x2 = cx - unit * 0.25;
  const y2 = cy + unit * 0.9;
  const x3 = cx + unit * 1.05;
  const y3 = cy - unit * 0.85;

  const strokeW = Math.max(2, Math.round(size * 0.12));

  return `<svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}" viewBox="0 0 ${size} ${size}">
  <rect width="${size}" height="${size}" rx="${r}" ry="${r}" fill="${THEME_COLOR}"/>
  <polyline
    points="${x1},${y1} ${x2},${y2} ${x3},${y3}"
    fill="none"
    stroke="white"
    stroke-width="${strokeW}"
    stroke-linecap="round"
    stroke-linejoin="round"
  />
</svg>`;
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------
async function renderPng(svgStr, outPath, size) {
  await sharp(Buffer.from(svgStr))
    .resize(size, size)
    .png()
    .toFile(outPath);
  console.log(`  ✓ ${outPath} (${size}×${size})`);
}

// .ico is just a 32×32 PNG with .ico extension — most browsers accept it.
// For broader compat we write a proper ICO binary with a single 32×32 frame.
async function renderIco(svgStr, outPath) {
  const pngBuf = await sharp(Buffer.from(svgStr))
    .resize(32, 32)
    .png()
    .toBuffer();

  // Minimal ICO format: ICONDIR (6 bytes) + ICONDIRENTRY (16 bytes) + PNG data
  const iconDir = Buffer.alloc(6);
  iconDir.writeUInt16LE(0, 0);   // reserved
  iconDir.writeUInt16LE(1, 2);   // type = icon
  iconDir.writeUInt16LE(1, 4);   // count = 1

  const entry = Buffer.alloc(16);
  entry.writeUInt8(32, 0);         // width  (0 = 256; 32 for 32px)
  entry.writeUInt8(32, 1);         // height
  entry.writeUInt8(0, 2);          // colour count (0 = >256)
  entry.writeUInt8(0, 3);          // reserved
  entry.writeUInt16LE(1, 4);       // planes
  entry.writeUInt16LE(32, 6);      // bits per pixel
  entry.writeUInt32LE(pngBuf.length, 8);   // size of image data
  entry.writeUInt32LE(6 + 16, 12);         // offset to image data

  writeFileSync(outPath, Buffer.concat([iconDir, entry, pngBuf]));
  console.log(`  ✓ ${outPath} (32×32 ICO)`);
}

// ---------------------------------------------------------------------------
// Main
// ---------------------------------------------------------------------------
async function main() {
  console.log('Generating PWA icons into', PUBLIC_DIR);

  await renderPng(buildSvg(192),          join(PUBLIC_DIR, 'icon-192.png'),          192);
  await renderPng(buildSvg(512),          join(PUBLIC_DIR, 'icon-512.png'),          512);
  await renderPng(buildMaskableSvg(512),  join(PUBLIC_DIR, 'icon-maskable-512.png'), 512);
  await renderPng(buildSvg(180, 0.1),     join(PUBLIC_DIR, 'apple-touch-icon.png'),  180);
  await renderIco(buildFaviconSvg(32),    join(PUBLIC_DIR, 'favicon.ico'));

  console.log('Done — all icons written to frontend/public/');
}

main().catch(err => { console.error(err); process.exit(1); });
