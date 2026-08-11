// Curio Web App - Torn Hero Banner
// Seeded tear algorithm matching Android's SoftTearParams + buildSoftTornPath

import React from 'react';
import { MaterialIcon } from './SharedComponents';

// ═══════════════════════════════════════════════════════════════════════════
// Seeded noise — exact port of Android hash2 + valueNoise
// ═══════════════════════════════════════════════════════════════════════════

function hash2(seed: number, x: number, y: number): number {
  let n = (x * 374761393 + y * 668265263 + seed * 1274126177) | 0;
  n = ((n ^ (n >>> 13)) * 1274126177) | 0;
  n = n ^ (n >>> 16);
  return (n & 0x7fffffff) / 0x7fffffff;
}

function valueNoise(seed: number, x: number, y: number): number {
  const xi = Math.floor(x);
  const yi = Math.floor(y);
  const xf = x - Math.floor(x);
  const yf = y - Math.floor(y);
  const a = hash2(seed, xi, yi);
  const b = hash2(seed, xi + 1, yi);
  const c = hash2(seed, xi, yi + 1);
  const d = hash2(seed, xi + 1, yi + 1);
  const u = xf * xf * (3 - 2 * xf);
  const v = yf * yf * (3 - 2 * yf);
  return a + (b - a) * u + (c - a) * v + (a - b - c + d) * u * v;
}

// Pseudo-random seeded generator
function seededRandom(seed: number): () => number {
  let s = seed * 31 + 0x0BADC0DE;
  return () => {
    s = ((s ^ (s >>> 13)) * 1274126177) | 0;
    s = s ^ (s >>> 16);
    return (s & 0x7fffffff) / 0x7fffffff;
  };
}

// ═══════════════════════════════════════════════════════════════════════════
// SoftTearParams port
// ═══════════════════════════════════════════════════════════════════════════

function buildTearParams(seed: number, bold: boolean, detail: boolean) {
  const patternSeed = detail ? seed ^ 0x4D3C2B1A : seed;
  const rnd = seededRandom(patternSeed);

  // 2-3 broad waves (a few more in bold)
  const waves = (detail ? 2.8 : 2.2) + rnd() * (detail ? 1.0 : 0.8);
  const waveScale = (bold ? 1.2 : 1) * (detail ? 1.08 : 1);

  // Tooth: ~6-8dp, bold ~35% more, detail ~8% more
  const tooth = (6.4 + rnd() * 2.2) * (bold ? 1.35 : 1) * (detail ? 1.08 : 1);
  const deep = (2.4 + rnd() * 1.5) * (bold ? 1.5 : 1) * (detail ? 1.10 : 1);
  const micro = (1.0 + rnd() * 0.8);
  const ripple = (1.3 + rnd() * 0.9) * (detail ? 1.12 : 1);
  const rippleWaves = (detail ? 8 : 7) + rnd() * (detail ? 4.5 : 4);

  // Tilt: -6 to +6dp
  const tilt = (rnd() - 0.5) * 2 * (6 + rnd() * 4);
  const phase = rnd() * 100;

  return {
    waves, waveScale, tooth, deep, micro, ripple, rippleWaves,
    tilt, phase, patternSeed,
  };
}

/**
 * Build an SVG path string for a torn bottom edge.
 * The hero is a rectangle with a straight top and torn bottom.
 * The tear extends DOWNWARD from the bottom edge.
 *
 * @param seed - Deterministic seed (same as Android)
 * @param width - SVG viewport width
 * @param height - SVG viewport height
 * @param bold - Rougher Home personality
 * @param detail - More expressive detail pattern
 */
export function buildTornHeroPath(
  seed: number,
  width: number,
  height: number,
  bold: boolean = false,
  detail: boolean = false,
): string {
  const p = buildTearParams(seed, bold, detail);
  const step = 4; // 4dp sampling

  function broadDisp(x: number, w: number): number {
    const nx = x / w;
    const slant = p.tilt * (nx - 0.5);
    const waveAngle = nx * p.waves * Math.PI * 2 + p.phase;
    const rhythmic = Math.sin(waveAngle) * p.tooth * 0.58;
    const main = (valueNoise(p.patternSeed, nx * p.waves, p.phase) - 0.5) * 2 * p.tooth * 0.58;
    const deepWave = (valueNoise(p.patternSeed + 101, nx * (p.waves * 0.42), p.phase + 17) - 0.5) * 2 * p.deep;
    const detailVar = detail
      ? Math.sin(nx * Math.PI * 5.6 + p.phase * 0.37) * p.tooth * 0.22
      : 0;
    const raw = slant + rhythmic * p.waveScale + main * p.waveScale + deepWave + detailVar;
    return raw > 0 ? raw * 0.55 : raw;
  }

  function disp(x: number, w: number): number {
    const nx = x / w;
    const rippleAngle = nx * p.rippleWaves * Math.PI * 2 + p.phase * 1.7;
    const smallRipple = Math.sin(rippleAngle) * p.ripple * 0.45 +
      (valueNoise(p.patternSeed + 71, nx * p.rippleWaves * 1.35, p.phase + 29) - 0.5) * 2 * p.ripple * 0.55;
    const fiber = (valueNoise(p.patternSeed + 47, x * 0.14, 3.5) - 0.5) * 2 * p.micro;
    const raw = broadDisp(x, w) + smallRipple + fiber;
    return raw > 0 ? raw * 0.55 : raw;
  }

  // Clockwise: top-left → top-right → right edge down → torn bottom (right to left) → left edge up
  let path = `M 0 0 L ${width} 0 L ${width} ${height + disp(width, width)}`;
  for (let x = width - step; x > 0; x -= step) {
    path += ` L ${x} ${height + disp(x, width)}`;
  }
  path += ` L 0 ${height + disp(0, width)} Z`;
  return path;
}

/**
 * Build the under-sheet SVG path — same torn top as hero, lower edge has restrained wobble.
 */
export function buildSheetPath(
  seed: number,
  width: number,
  _height: number,
  bold: boolean = false,
  detail: boolean = false,
): string {
  const p = buildTearParams(seed, bold, detail);
  const step = 4;
  const lipPx = 14; // sheet visible height

  function broadDisp(x: number, w: number): number {
    const nx = x / w;
    const slant = p.tilt * (nx - 0.5);
    const waveAngle = nx * p.waves * Math.PI * 2 + p.phase;
    const rhythmic = Math.sin(waveAngle) * p.tooth * 0.58;
    const main = (valueNoise(p.patternSeed, nx * p.waves, p.phase) - 0.5) * 2 * p.tooth * 0.58;
    const deepWave = (valueNoise(p.patternSeed + 101, nx * (p.waves * 0.42), p.phase + 17) - 0.5) * 2 * p.deep;
    const detailVar = detail
      ? Math.sin(nx * Math.PI * 5.6 + p.phase * 0.37) * p.tooth * 0.22
      : 0;
    const raw = slant + rhythmic * p.waveScale + main * p.waveScale + deepWave + detailVar;
    return raw > 0 ? raw * 0.55 : raw;
  }

  function bottomBump(x: number, w: number): number {
    const nx = x / w;
    const sharedShape = broadDisp(x, w) * (detail ? 0.28 : 0.18);
    const rippleAngle = nx * (p.rippleWaves * 0.86) * Math.PI * 2 + p.phase * 1.11;
    const extraRipple = Math.sin(rippleAngle) * (detail ? 0.60 : 0.45) +
      (valueNoise(detail ? seed ^ 0x4D3C2B1A : seed, nx * p.rippleWaves * 0.9, p.phase + 53) - 0.5) * 2 * (detail ? 0.40 : 0.30);
    return sharedShape + extraRipple;
  }

  // Top edge: same torn shape as hero, pushed up by baseline (hidden behind hero)
  const baseline = 14;
  let path = `M 0 ${-baseline}`;
  for (let x = 0; x <= width; x += step) {
    path += ` L ${x} ${-baseline + broadDisp(x, width)}`;
  }
  // Right edge down
  path += ` L ${width} ${lipPx + bottomBump(width, width)}`;
  // Bottom edge right to left
  for (let x = width - step; x >= 0; x -= step) {
    path += ` L ${x} ${lipPx + bottomBump(x, width)}`;
  }
  path += ' Z';
  return path;
}

// ═══════════════════════════════════════════════════════════════════════════
// TornHero Component
// ═══════════════════════════════════════════════════════════════════════════

export interface TornHeroSymbol {
  glyph: string;
  biasX: number;   // 0..1, where on the hero width
  biasY: number;   // 0..1, where on the hero height (0=top, 1=bottom)
  size: number;
  rotation: number;
  alpha: number;
}

export const TornHero: React.FC<{
  /** Hero height in px */
  height?: number;
  /** Fill color */
  fill: string;
  /** Readable ink color for text/icons on this fill */
  ink?: string;
  /** Tear seed — fixed per hero so it doesn't re-roll */
  tearSeed: number;
  /** Rougher tear for Home */
  bold?: boolean;
  /** More expressive detail pattern */
  detail?: boolean;
  /** SVG viewport width (default 400) */
  vpWidth?: number;
  /** Watermark symbols scattered around the banner edges */
  symbols?: TornHeroSymbol[];
  /** Content inside the hero */
  children: React.ReactNode;
  /** Under-sheet background (white by default) */
  sheetColor?: string;
  /** Dark mode flag for shadow color */
  isDark?: boolean;
}> = ({
  height = 300,
  fill,
  ink = '#fff',
  tearSeed,
  bold = false,
  detail = false,
  vpWidth = 400,
  symbols,
  children,
  sheetColor = '#FFFDF9',
  isDark = false,
}) => {
  const vpHeight = 400;
  const sheetH = 42;
  const sheetY = height - 18;

  const heroPath = buildTornHeroPath(tearSeed, vpWidth, vpHeight, bold, detail);
  const sheetPath = buildSheetPath(tearSeed, vpWidth, sheetH, bold, detail);

  const shadowAlpha = isDark ? 0.08 : 0.20;

  return (
    <div className="relative w-full" style={{ height: height + 24 }}>
      {/* White under-sheet — clipped with the same torn top edge */}
      <div className="absolute left-0 right-0 overflow-hidden" style={{ top: sheetY, height: sheetH, zIndex: 10 }}>
        <svg viewBox={`0 0 ${vpWidth} ${sheetH}`} preserveAspectRatio="none"
          className="w-full h-full" style={{ display: 'block' }}>
          <path d={sheetPath} fill={sheetColor} />
        </svg>
      </div>

      {/* Torn-edge shadow — same torn shape nudged down 1px */}
      <div className="absolute left-0 right-0" style={{ top: 1, height, zIndex: 5 }}>
        <svg viewBox={`0 0 ${vpWidth} ${vpHeight}`} preserveAspectRatio="none"
          className="w-full h-full" style={{ display: 'block' }}>
          <path d={heroPath} fill={`rgba(0,0,0,${shadowAlpha})`} />
        </svg>
      </div>

      {/* Solid hero banner with torn bottom */}
      <div className="absolute left-0 right-0 overflow-hidden" style={{ top: 0, height, zIndex: 20 }}>
        <svg viewBox={`0 0 ${vpWidth} ${vpHeight}`} preserveAspectRatio="none"
          className="w-full h-full" style={{ display: 'block' }}>
          <path d={heroPath} fill={fill} />
        </svg>

        {/* Watermark symbols */}
        {symbols && (
          <div className="absolute inset-0 pointer-events-none select-none" style={{ zIndex: 1 }}>
            {symbols.map((s, i) => (
              <div key={i} className="absolute"
                style={{
                  left: `${s.biasX * 100}%`,
                  top: `${s.biasY * 100}%`,
                  transform: `translate(-50%, -50%) rotate(${s.rotation}deg)`,
                  opacity: s.alpha,
                  color: ink,
                }}>
                <MaterialIcon name={s.glyph} size={s.size} />
              </div>
            ))}
          </div>
        )}

        {/* Content */}
        <div className="absolute inset-0 z-10" style={{ paddingTop: 'env(safe-area-inset-top, 0px)' }}>
          {children}
        </div>
      </div>
    </div>
  );
};

// ═══════════════════════════════════════════════════════════════════════════
// Pre-built symbol sets
// ═══════════════════════════════════════════════════════════════════════════

/** Home hero watermark symbols */
export const HOME_HERO_SYMBOLS: TornHeroSymbol[] = [
  { glyph: 'casino', biasX: 0.07, biasY: 0.15, size: 44, rotation: -12, alpha: 0.11 },
  { glyph: 'auto_awesome', biasX: 0.93, biasY: 0.15, size: 44, rotation: 12, alpha: 0.11 },
  { glyph: 'star', biasX: 0.45, biasY: 0.36, size: 48, rotation: -8, alpha: 0.13 },
  { glyph: 'local_fire_department', biasX: 0.55, biasY: 0.36, size: 48, rotation: 8, alpha: 0.13 },
  { glyph: 'psychology', biasX: 0.06, biasY: 0.88, size: 56, rotation: -14, alpha: 0.14 },
  { glyph: 'menu_book', biasX: 0.94, biasY: 0.88, size: 56, rotation: 14, alpha: 0.14 },
  { glyph: 'emoji_events', biasX: 0.44, biasY: 0.46, size: 50, rotation: -10, alpha: 0.13 },
  { glyph: 'palette', biasX: 0.56, biasY: 0.46, size: 50, rotation: 10, alpha: 0.13 },
  { glyph: 'explore', biasX: 0.06, biasY: 0.20, size: 44, rotation: -6, alpha: 0.11 },
  { glyph: 'travel_explore', biasX: 0.94, biasY: 0.20, size: 44, rotation: 6, alpha: 0.11 },
];

/** Profile hero watermark symbols */
export const PROFILE_HERO_SYMBOLS: TornHeroSymbol[] = [
  { glyph: 'person', biasX: 0.08, biasY: 0.18, size: 40, rotation: -10, alpha: 0.10 },
  { glyph: 'star', biasX: 0.92, biasY: 0.18, size: 40, rotation: 10, alpha: 0.10 },
  { glyph: 'auto_awesome', biasX: 0.48, biasY: 0.38, size: 44, rotation: -6, alpha: 0.12 },
  { glyph: 'emoji_events', biasX: 0.52, biasY: 0.38, size: 44, rotation: 6, alpha: 0.12 },
  { glyph: 'workspace_premium', biasX: 0.06, biasY: 0.82, size: 50, rotation: -12, alpha: 0.13 },
  { glyph: 'local_fire_department', biasX: 0.94, biasY: 0.82, size: 50, rotation: 12, alpha: 0.13 },
];

/** Settings hero watermark symbols */
export const SETTINGS_HERO_SYMBOLS: TornHeroSymbol[] = [
  { glyph: 'settings', biasX: 0.09, biasY: 0.22, size: 36, rotation: -8, alpha: 0.09 },
  { glyph: 'tune', biasX: 0.91, biasY: 0.22, size: 36, rotation: 8, alpha: 0.09 },
  { glyph: 'palette', biasX: 0.47, biasY: 0.42, size: 40, rotation: -5, alpha: 0.11 },
  { glyph: 'auto_awesome', biasX: 0.53, biasY: 0.42, size: 40, rotation: 5, alpha: 0.11 },
  { glyph: 'build', biasX: 0.07, biasY: 0.78, size: 44, rotation: -10, alpha: 0.12 },
  { glyph: 'psychology', biasX: 0.93, biasY: 0.78, size: 44, rotation: 10, alpha: 0.12 },
];

/** Cabinet hero watermark symbols */
export const CABINET_HERO_SYMBOLS: TornHeroSymbol[] = [
  { glyph: 'inventory_2', biasX: 0.08, biasY: 0.20, size: 38, rotation: -9, alpha: 0.10 },
  { glyph: 'book_5', biasX: 0.92, biasY: 0.20, size: 38, rotation: 9, alpha: 0.10 },
  { glyph: 'edit_note', biasX: 0.46, biasY: 0.40, size: 42, rotation: -7, alpha: 0.12 },
  { glyph: 'menu_book', biasX: 0.54, biasY: 0.40, size: 42, rotation: 7, alpha: 0.12 },
  { glyph: 'archive', biasX: 0.06, biasY: 0.80, size: 48, rotation: -11, alpha: 0.13 },
  { glyph: 'auto_stories', biasX: 0.94, biasY: 0.80, size: 48, rotation: 11, alpha: 0.13 },
];

/** Browser hero watermark symbols */
export const BROWSER_HERO_SYMBOLS: TornHeroSymbol[] = [
  { glyph: 'travel_explore', biasX: 0.10, biasY: 0.22, size: 36, rotation: -7, alpha: 0.09 },
  { glyph: 'search', biasX: 0.90, biasY: 0.22, size: 36, rotation: 7, alpha: 0.09 },
  { glyph: 'category', biasX: 0.45, biasY: 0.44, size: 40, rotation: -6, alpha: 0.11 },
  { glyph: 'auto_awesome', biasX: 0.55, biasY: 0.44, size: 40, rotation: 6, alpha: 0.11 },
  { glyph: 'map', biasX: 0.07, biasY: 0.76, size: 44, rotation: -9, alpha: 0.12 },
  { glyph: 'language', biasX: 0.93, biasY: 0.76, size: 44, rotation: 9, alpha: 0.12 },
];

/** Detail hero watermark symbols for a given category icon glyph */
export function makeDetailHeroSymbols(catGlyph: string): TornHeroSymbol[] {
  return [
    { glyph: 'person', biasX: 0.05, biasY: 0.10, size: 36, rotation: -8, alpha: 0.12 },
    { glyph: 'album', biasX: 0.88, biasY: 0.08, size: 40, rotation: 10, alpha: 0.12 },
    { glyph: 'movie', biasX: 0.50, biasY: 0.25, size: 44, rotation: -5, alpha: 0.12 },
    { glyph: 'edit_note', biasX: 0.92, biasY: 0.55, size: 38, rotation: 12, alpha: 0.12 },
    { glyph: 'brush', biasX: 0.08, biasY: 0.60, size: 42, rotation: -10, alpha: 0.12 },
    { glyph: 'science', biasX: 0.55, biasY: 0.70, size: 48, rotation: -6, alpha: 0.12 },
    { glyph: catGlyph, biasX: 0.90, biasY: 0.75, size: 40, rotation: 8, alpha: 0.12 },
    { glyph: 'menu_book', biasX: 0.15, biasY: 0.80, size: 36, rotation: -12, alpha: 0.12 },
    { glyph: 'palette', biasX: 0.92, biasY: 0.45, size: 42, rotation: 7, alpha: 0.12 },
    { glyph: 'smart_display', biasX: 0.10, biasY: 0.35, size: 44, rotation: -4, alpha: 0.12 },
  ];
}
