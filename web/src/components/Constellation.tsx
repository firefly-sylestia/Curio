// Curio Web App - Constellation Component
// Big Dipper + Polaris — exact reproduction of the Android SVG designs.
// Dark mode: deep-space palette (svgviewer-output 16)
// Light mode: muted cosmic palette (svgviewer-output 17)
// v2 — star-focused zoom, pinch-to-zoom, 3D perspective tilt

import React, { useState, useCallback, useRef, useEffect, useMemo } from 'react';

// ── Star data ──────────────────────────────────────────────────────

interface Star {
  name: string;
  x: number;  // 1400×1400 viewBox coords
  y: number;
  r: number;
  darkFill: string;
  lightFill: string;
}

const STARS: Star[] = [
  // Big Dipper (Ursa Major) — 7 main stars
  { name: 'Dubhe',  x: 872, y: 751, r: 5.8, darkFill: '#ffffff', lightFill: '#eef5fa' },
  { name: 'Merak',  x: 961, y: 689, r: 5.2, darkFill: '#ffffff', lightFill: '#eef5fa' },
  { name: 'Phecda', x: 894, y: 539, r: 5.1, darkFill: '#ffffff', lightFill: '#eef5fa' },
  { name: 'Megrez', x: 801, y: 556, r: 4.3, darkFill: '#ffffff', lightFill: '#e8f1f7' },
  { name: 'Alioth', x: 716, y: 483, r: 6.0, darkFill: '#ffffff', lightFill: '#f2f7fb' },
  { name: 'Mizar',  x: 642, y: 432, r: 5.2, darkFill: '#ffffff', lightFill: '#edf5fa' },
  { name: 'Alkaid', x: 597, y: 298, r: 5.7, darkFill: '#ffffff', lightFill: '#f2f7fb' },
  // Polaris — the Pole Star
  { name: 'Polaris', x: 441, y: 1101, r: 7.5, darkFill: '#f8fcff', lightFill: '#f5f8fa' },
  // Nearby stars
  { name: 'Alcor',   x: 633, y: 418, r: 3.8, darkFill: '#eaf5ff', lightFill: '#dbe8f2' },
  { name: 'Kochab',  x: 320, y: 1211, r: 5.2, darkFill: '#fffdf2', lightFill: '#e2dfd5' },
  { name: 'Pherkad', x: 561, y: 1265, r: 4.5, darkFill: '#f7f8ff', lightFill: '#dbe7f0' },
];

// ── Constellation lines ────────────────────────────────────────────

interface Line {
  x1: number; y1: number; x2: number; y2: number;
  stroke: string; width: number; opacity: number; dash?: string;
}

function getLines(isDark: boolean): Line[] {
  const c = isDark ? '#c9e5ff' : '#c7d9e8';
  const pointer = isDark ? '#9fc8ed' : '#b4c9db';
  const ursa = isDark ? '#9bbbd9' : '#afc3d4';
  const w = isDark ? 2.4 : 2.3;
  const po = isDark ? 0.22 : 0.28;
  const uo = isDark ? 0.12 : 0.17;

  return [
    // Big Dipper bowl
    { x1: 872, y1: 751, x2: 961, y2: 689, stroke: c, width: w, opacity: isDark ? 0.72 : 0.78 },
    { x1: 961, y1: 689, x2: 894, y2: 539, stroke: c, width: w, opacity: isDark ? 0.72 : 0.78 },
    { x1: 894, y1: 539, x2: 801, y2: 556, stroke: c, width: w, opacity: isDark ? 0.72 : 0.78 },
    { x1: 801, y1: 556, x2: 872, y2: 751, stroke: c, width: w, opacity: isDark ? 0.72 : 0.78 },
    // Big Dipper handle
    { x1: 801, y1: 556, x2: 716, y2: 483, stroke: c, width: w, opacity: isDark ? 0.78 : 0.82 },
    { x1: 716, y1: 483, x2: 642, y2: 432, stroke: c, width: w, opacity: isDark ? 0.78 : 0.82 },
    { x1: 642, y1: 432, x2: 597, y2: 298, stroke: c, width: w, opacity: isDark ? 0.78 : 0.82 },
    // Pointer line: Dubhe → Polaris
    { x1: 872, y1: 751, x2: 441, y2: 1101, stroke: pointer, width: isDark ? 1.3 : 1.2, opacity: po, dash: '6 13' },
    // Ursa Minor subtle lines
    { x1: 441, y1: 1101, x2: 320, y2: 1211, stroke: ursa, width: 1, opacity: uo, dash: '3 11' },
    { x1: 441, y1: 1101, x2: 561, y2: 1265, stroke: ursa, width: 1, opacity: uo, dash: '3 11' },
  ];
}

// ── Background star pattern (180×180 tile) ─────────────────────────

interface BgStar { cx: number; cy: number; r: number; fill: string; opacity: number; }

const DARK_BG_STARS: BgStar[] = [
  { cx: 18, cy: 22, r: 0.8, fill: '#fff', opacity: 0.60 },
  { cx: 61, cy: 70, r: 0.55, fill: '#c9e2ff', opacity: 0.65 },
  { cx: 128, cy: 29, r: 0.75, fill: '#fff', opacity: 0.55 },
  { cx: 157, cy: 91, r: 0.55, fill: '#fff', opacity: 0.60 },
  { cx: 39, cy: 130, r: 0.55, fill: '#9fc8ed', opacity: 0.55 },
  { cx: 101, cy: 151, r: 0.7, fill: '#fff', opacity: 0.55 },
  { cx: 151, cy: 164, r: 0.45, fill: '#fff', opacity: 0.65 },
  { cx: 87, cy: 103, r: 0.45, fill: '#fff', opacity: 0.55 },
  { cx: 12, cy: 166, r: 0.45, fill: '#8db9e5', opacity: 0.55 },
];

const LIGHT_BG_STARS: BgStar[] = [
  { cx: 18, cy: 22, r: 0.75, fill: '#d7e1ec', opacity: 0.48 },
  { cx: 61, cy: 70, r: 0.5, fill: '#c3d1df', opacity: 0.42 },
  { cx: 128, cy: 29, r: 0.7, fill: '#e0e8f0', opacity: 0.45 },
  { cx: 157, cy: 91, r: 0.5, fill: '#c5d4e2', opacity: 0.42 },
  { cx: 39, cy: 130, r: 0.5, fill: '#d4e0eb', opacity: 0.40 },
  { cx: 101, cy: 151, r: 0.65, fill: '#e0e8f0', opacity: 0.42 },
  { cx: 151, cy: 164, r: 0.4, fill: '#c4d2df', opacity: 0.45 },
  { cx: 87, cy: 103, r: 0.4, fill: '#e0e8f0', opacity: 0.40 },
  { cx: 12, cy: 166, r: 0.4, fill: '#c7d5e2', opacity: 0.42 },
];

// Distant + small field stars
const DARK_DISTANT = [
  { cx: 150, cy: 260, r: 1.3, opacity: 0.65 },
  { cx: 1160, cy: 260, r: 1.2, opacity: 0.60 },
  { cx: 1240, cy: 780, r: 1.3, opacity: 0.60 },
  { cx: 190, cy: 820, r: 1.1, opacity: 0.60 },
  { cx: 1080, cy: 1160, r: 1.2, opacity: 0.60 },
];
const DARK_FIELD = [
  { cx: 90, cy: 600, r: 0.8, opacity: 0.5 },
  { cx: 230, cy: 470, r: 0.7, opacity: 0.5 },
  { cx: 320, cy: 300, r: 0.8, opacity: 0.5 },
  { cx: 1050, cy: 420, r: 0.8, opacity: 0.5 },
  { cx: 1190, cy: 520, r: 0.7, opacity: 0.5 },
  { cx: 1130, cy: 980, r: 0.8, opacity: 0.5 },
  { cx: 920, cy: 1190, r: 0.7, opacity: 0.5 },
  { cx: 760, cy: 1260, r: 0.8, opacity: 0.5 },
  { cx: 300, cy: 1080, r: 0.7, opacity: 0.5 },
];

const LIGHT_DISTANT = [
  { cx: 150, cy: 260, r: 1.2, opacity: 0.55 },
  { cx: 1160, cy: 260, r: 1.1, opacity: 0.52 },
  { cx: 1240, cy: 780, r: 1.2, opacity: 0.52 },
  { cx: 190, cy: 820, r: 1.0, opacity: 0.50 },
  { cx: 1080, cy: 1160, r: 1.1, opacity: 0.52 },
];
const LIGHT_FIELD = [
  { cx: 90, cy: 600, r: 0.75, opacity: 0.35 },
  { cx: 230, cy: 470, r: 0.65, opacity: 0.35 },
  { cx: 320, cy: 300, r: 0.75, opacity: 0.35 },
  { cx: 1050, cy: 420, r: 0.75, opacity: 0.35 },
  { cx: 1190, cy: 520, r: 0.65, opacity: 0.35 },
  { cx: 1130, cy: 980, r: 0.75, opacity: 0.35 },
  { cx: 920, cy: 1190, r: 0.65, opacity: 0.35 },
  { cx: 760, cy: 1260, r: 0.75, opacity: 0.35 },
  { cx: 300, cy: 1080, r: 0.65, opacity: 0.35 },
];

// ── Zoom constants ─────────────────────────────────────────────────

const FULL_SIZE = 1400;
const ZOOM_SIZE = 700;   // 2× zoom level
const LERP = 0.1;        // animation smoothing (0–1, lower = smoother)

// ═══════════════════════════════════════════════════════════════════════════
// Constellation Component
// ═══════════════════════════════════════════════════════════════════════════

export const Constellation: React.FC<{
  isDark: boolean;
  height?: number;
  onStarTap?: (starName: string) => void;
  selectedStar?: string | null;
}> = ({ isDark, height = 280, onStarTap, selectedStar }) => {
  const [hoveredStar, setHoveredStar] = useState<string | null>(null);
  const [perspective, setPerspective] = useState({ rotateX: 0, rotateY: 0 });

  // Refs for animation (no re-renders)
  const svgRef = useRef<SVGSVGElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const viewBoxRef = useRef({ x: 0, y: 0, w: FULL_SIZE, h: FULL_SIZE });
  const targetRef = useRef({ x: 0, y: 0, w: FULL_SIZE, h: FULL_SIZE });
  const lastTapRef = useRef(0);
  const lastPinchDistRef = useRef(0);

  const is3D = useMemo(() => {
    try { return localStorage.getItem('curio-star-zoom-3d') === 'true'; }
    catch { return false; }
  }, []);

  // ── Smooth viewBox animation loop (runs once, lerps toward targetRef) ──
  useEffect(() => {
    let raf: number;
    const animate = () => {
      const cur = viewBoxRef.current;
      const tgt = targetRef.current;
      cur.x += (tgt.x - cur.x) * LERP;
      cur.y += (tgt.y - cur.y) * LERP;
      cur.w += (tgt.w - cur.w) * LERP;
      cur.h += (tgt.h - cur.h) * LERP;
      if (svgRef.current) {
        svgRef.current.setAttribute('viewBox',
          `${cur.x.toFixed(1)} ${cur.y.toFixed(1)} ${cur.w.toFixed(1)} ${cur.h.toFixed(1)}`);
      }
      raf = requestAnimationFrame(animate);
    };
    raf = requestAnimationFrame(animate);
    return () => cancelAnimationFrame(raf);
  }, []);

  // ── Pinch-to-zoom: native listener for { passive: false } ─────────
  useEffect(() => {
    const el = containerRef.current;
    if (!el) return;

    const onMove = (e: TouchEvent) => {
      if (e.touches.length === 2 && lastPinchDistRef.current > 0) {
        e.preventDefault();
        const dx = e.touches[0].clientX - e.touches[1].clientX;
        const dy = e.touches[0].clientY - e.touches[1].clientY;
        const dist = Math.hypot(dx, dy);
        const scale = dist / lastPinchDistRef.current;
        lastPinchDistRef.current = dist;

        const cur = targetRef.current;
        const cx = cur.x + cur.w / 2;
        const cy = cur.y + cur.h / 2;
        const newW = Math.max(200, Math.min(FULL_SIZE, cur.w / scale));
        const newH = Math.max(200, Math.min(FULL_SIZE, cur.h / scale));
        const nx = Math.max(0, Math.min(FULL_SIZE - newW, cx - newW / 2));
        const ny = Math.max(0, Math.min(FULL_SIZE - newH, cy - newH / 2));
        const snap = { x: nx, y: ny, w: newW, h: newH };
        targetRef.current = snap;
        // Skip lerp for immediate pinch response
        Object.assign(viewBoxRef.current, snap);
      }
    };

    el.addEventListener('touchmove', onMove, { passive: false });
    return () => el.removeEventListener('touchmove', onMove);
  }, []);

  // ── Star tap → zoom to that star's position ──────────────────────
  const handleStarClick = useCallback((name: string) => {
    onStarTap?.(name);
    const star = STARS.find(s => s.name === name);
    if (!star) return;

    const nx = Math.max(0, Math.min(FULL_SIZE - ZOOM_SIZE, star.x - ZOOM_SIZE / 2));
    const ny = Math.max(0, Math.min(FULL_SIZE - ZOOM_SIZE, star.y - ZOOM_SIZE / 2));
    targetRef.current = { x: nx, y: ny, w: ZOOM_SIZE, h: ZOOM_SIZE };

    // 3D perspective tilt based on star position relative to center
    if (is3D) {
      const dx = (star.x - FULL_SIZE / 2) / (FULL_SIZE / 2);
      const dy = (star.y - FULL_SIZE / 2) / (FULL_SIZE / 2);
      setPerspective({ rotateX: -dy * 8, rotateY: dx * 8 });
    }
  }, [onStarTap, is3D]);

  // ── Double-tap background → reset zoom ───────────────────────────
  const handleBgTap = useCallback(() => {
    const now = Date.now();
    if (now - lastTapRef.current < 350) {
      targetRef.current = { x: 0, y: 0, w: FULL_SIZE, h: FULL_SIZE };
      setPerspective({ rotateX: 0, rotateY: 0 });
    }
    lastTapRef.current = now;
  }, []);

  // ── Touch start / end for pinch tracking ──────────────────────────
  const handleTouchStart = useCallback((e: React.TouchEvent) => {
    if (e.touches.length === 2) {
      const dx = e.touches[0].clientX - e.touches[1].clientX;
      const dy = e.touches[0].clientY - e.touches[1].clientY;
      lastPinchDistRef.current = Math.hypot(dx, dy);
    }
  }, []);

  const handleTouchEnd = useCallback(() => {
    lastPinchDistRef.current = 0;
  }, []);

  const bgStars = isDark ? DARK_BG_STARS : LIGHT_BG_STARS;
  const distant = isDark ? DARK_DISTANT : LIGHT_DISTANT;
  const field = isDark ? DARK_FIELD : LIGHT_FIELD;
  const lines = getLines(isDark);

  // Gradient IDs must be unique per instance
  const uid = 'cst';

  // 3D tilt is active when the 3D experiment is on and a star is selected or hovered
  const active3D = is3D && (hoveredStar || selectedStar);

  return (
    <div
      ref={containerRef}
      className="relative w-full overflow-hidden rounded-none"
      style={{
        height,
        transform: active3D
          ? `perspective(800px) rotateX(${perspective.rotateX}deg) rotateY(${perspective.rotateY}deg)`
          : undefined,
        transition: 'transform 0.6s cubic-bezier(0.34, 1.56, 0.64, 1)',
        touchAction: 'pan-y',
      }}
      onTouchStart={handleTouchStart}
      onTouchEnd={handleTouchEnd}
      onClick={handleBgTap}
    >
      <svg
        ref={svgRef}
        viewBox="0 0 1400 1400"
        preserveAspectRatio="xMidYMid slice"
        className="w-full h-full"
        style={{ display: 'block' }}
      >
        <defs>
          {/* Space background gradient */}
          <radialGradient id={`${uid}-space`} cx="50%" cy="50%" r="78%">
            {isDark ? (
              <>
                <stop offset="0%" stopColor="#101d45" />
                <stop offset="38%" stopColor="#08132f" />
                <stop offset="72%" stopColor="#03091d" />
                <stop offset="100%" stopColor="#01030b" />
              </>
            ) : (
              <>
                <stop offset="0%" stopColor="#344b67" />
                <stop offset="35%" stopColor="#2d425c" />
                <stop offset="68%" stopColor="#26394f" />
                <stop offset="100%" stopColor="#202f42" />
              </>
            )}
          </radialGradient>

          {/* Purple nebula */}
          <radialGradient id={`${uid}-purple`}>
            {isDark ? (
              <>
                <stop offset="0%" stopColor="#8d45bd" stopOpacity="0.15" />
                <stop offset="45%" stopColor="#55277f" stopOpacity="0.06" />
                <stop offset="100%" stopColor="#18072d" stopOpacity="0" />
              </>
            ) : (
              <>
                <stop offset="0%" stopColor="#987da9" stopOpacity="0.14" />
                <stop offset="45%" stopColor="#806b92" stopOpacity="0.07" />
                <stop offset="100%" stopColor="#26394f" stopOpacity="0" />
              </>
            )}
          </radialGradient>

          {/* Blue nebula */}
          <radialGradient id={`${uid}-blue`}>
            {isDark ? (
              <>
                <stop offset="0%" stopColor="#3979c9" stopOpacity="0.13" />
                <stop offset="45%" stopColor="#24518c" stopOpacity="0.055" />
                <stop offset="100%" stopColor="#061128" stopOpacity="0" />
              </>
            ) : (
              <>
                <stop offset="0%" stopColor="#7197bd" stopOpacity="0.18" />
                <stop offset="45%" stopColor="#607f9f" stopOpacity="0.08" />
                <stop offset="100%" stopColor="#26394f" stopOpacity="0" />
              </>
            )}
          </radialGradient>

          {/* Background star pattern */}
          <pattern id={`${uid}-bgstars`} width="180" height="180" patternUnits="userSpaceOnUse">
            {bgStars.map((s, i) => (
              <circle key={i} cx={s.cx} cy={s.cy} r={s.r} fill={s.fill} opacity={s.opacity} />
            ))}
          </pattern>
        </defs>

        {/* ── Background ─────────────────────────────────────── */}
        <rect width="1400" height="1400" fill={`url(#${uid}-space)`} />

        {/* Purple nebula — ellipse at (220, 400) rotated -25° */}
        <ellipse cx="220" cy="400" rx="390" ry="500"
          fill={`url(#${uid}-purple)`}
          transform="rotate(-25 220 400)" />

        {/* Blue nebula — ellipse at (1160, 850) rotated 20° */}
        <ellipse cx="1160" cy="850" rx="430" ry="520"
          fill={`url(#${uid}-blue)`}
          transform="rotate(20 1160 850)" />

        {/* Purple nebula 2 — ellipse at (700, 1170) */}
        <ellipse cx="700" cy="1170" rx="470" ry="280"
          fill={`url(#${uid}-purple)`} />

        {/* Background star pattern */}
        <rect width="1400" height="1400" fill={`url(#${uid}-bgstars)`} />

        {/* ── Constellation lines ────────────────────────────── */}
        {lines.map((l, i) => (
          <line key={i}
            x1={l.x1} y1={l.y1} x2={l.x2} y2={l.y2}
            stroke={l.stroke} strokeWidth={l.width}
            strokeLinecap="round" strokeLinejoin="round"
            opacity={l.opacity}
            strokeDasharray={l.dash || undefined}
          />
        ))}

        {/* ── Distant + field stars ──────────────────────────── */}
        {distant.map((s, i) => (
          <circle key={`d${i}`} cx={s.cx} cy={s.cy} r={s.r}
            fill={isDark ? '#e8f4ff' : '#d5e1eb'} opacity={s.opacity} />
        ))}
        {field.map((s, i) => (
          <circle key={`f${i}`} cx={s.cx} cy={s.cy} r={s.r}
            fill={isDark ? '#ffffff' : '#d0deea'} opacity={s.opacity} />
        ))}

        {/* ── Main stars (tappable) ──────────────────────────── */}
        {STARS.map((star) => {
          const isSelected = selectedStar === star.name;
          const isHovered = hoveredStar === star.name;
          const fill = isDark ? star.darkFill : star.lightFill;
          const glowR = star.r * (isSelected ? 4 : isHovered ? 3 : 0);
          const glowAlpha = isSelected ? 0.35 : isHovered ? 0.20 : 0;

          return (
            <g key={star.name}
              onClick={(e) => { e.stopPropagation(); handleStarClick(star.name); }}
              onMouseEnter={() => setHoveredStar(star.name)}
              onMouseLeave={() => setHoveredStar(null)}
              style={{ cursor: 'pointer' }}
            >
              {/* Glow halo */}
              {glowAlpha > 0 && (
                <circle cx={star.x} cy={star.y} r={glowR}
                  fill="white" opacity={glowAlpha} />
              )}
              {/* Star circle */}
              <circle cx={star.x} cy={star.y} r={star.r}
                fill={fill}
                style={{
                  transition: 'transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1)',
                  transform: isSelected ? 'scale(1.3)' : isHovered ? 'scale(1.15)' : 'scale(1)',
                  transformOrigin: `${star.x}px ${star.y}px`,
                }}
              />
            </g>
          );
        })}
      </svg>
    </div>
  );
};

export default Constellation;
