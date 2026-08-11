// Curio Web App - Floating Pet (Pixel Art)
// Canvas-drawn pixel spirit matching Android's CurioPetSprite

import React, { useState, useEffect, useRef } from 'react';
import { getPetSystem } from '../data/PetSystem';
import type { PetStage } from '../data/PetSystem';
import { getQuestSystem } from '../data/QuestSystem';
import { useTheme } from '../theme/ThemeContext';

// ─── Pixel color palette ──────────────────────────────────────────────
const CREAM = '#FFF8E7';
const INK = '#3D2B1F';
const CHEEK = '#FFB5B5';
const GOLD = '#FFD700';
const GOLD_DEEP = '#DAA520';
const BELLY = '#FFFBF0';
const CORAL = '#FF8FA3';
const LEAF = '#9CCB8B';
const WHITE = '#FFFFFF';

// ─── Body pixel grid (16x16) — cute round blob ────────────────────────
const BODY: string[] = [
  '....####....',
  '..########..',
  '.##########.',
  '.##########.',
  '.##########.',
  '############',
  '############',
  '############',
  '############',
  '.##########.',
  '.##########.',
  '..######....',
];

// ─── Eye pixel definitions ────────────────────────────────────────────
const EYES_OPEN: [number, number, string][] = [
  [3, 5, 'ink'], [4, 5, 'ink'], [3, 6, 'ink'], [4, 6, 'ink'],
  [7, 5, 'ink'], [8, 5, 'ink'], [7, 6, 'ink'], [8, 6, 'ink'],
  [3, 5, 'white'], [7, 5, 'white'],
];
const EYES_BLINK: [number, number, string][] = [
  [3, 5, 'ink'], [4, 5, 'ink'], [7, 5, 'ink'], [8, 5, 'ink'],
];
const EYES_CLOSED: [number, number, string][] = [
  [3, 6, 'ink'], [4, 6, 'ink'], [7, 6, 'ink'], [8, 6, 'ink'],
];
const EYES_HAPPY: [number, number, string][] = [
  [3, 6, 'ink'], [4, 5, 'ink'], [4, 6, 'ink'],
  [7, 6, 'ink'], [7, 5, 'ink'], [8, 6, 'ink'],
];

// ─── Pixel sprite canvas component ────────────────────────────────────
const PixelPet: React.FC<{
  stage: PetStage; mood: string; animKey: number; squish: number; hop: number;
}> = React.memo(({ stage, mood, animKey, squish, hop }) => {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const frameRef = useRef(0);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const size = canvas.width;
    const px = size / 16;
    let t = 0;

    const draw = () => {
      ctx.clearRect(0, 0, size, size);
      const bob = Math.sin(t * 2.5) * 1.5;
      const blinkPhase = (t * 0.6) % 2.4;
      const breathing = 1 + Math.sin(t * 1.3) * 0.015;
      const sx = squish > 0 ? 1 + squish * 0.08 : breathing;
      const sy = squish > 0 ? 1 - squish * 0.06 : breathing;
      const h = hop > 0 ? -hop * 6 * (1 - hop * 0.3) : 0;

      // Canvas transform for bob/squish/hop
      ctx.save();
      ctx.translate(size / 2, size / 2 + bob * px + h);
      ctx.scale(sx, sy);
      ctx.translate(-size / 2, -size / 2);

      const drawPx = (col: number, row: number, color: string, alpha = 1) => {
        ctx.fillStyle = color;
        ctx.globalAlpha = alpha;
        const ox = 2; // center the 12-wide body in 16 grid
        ctx.fillRect((col + ox) * px, row * px, px, px);
        ctx.globalAlpha = 1;
      };

      // ── Body ──────────────────────────────────────────────────────
      BODY.forEach((line, row) => {
        for (let col = 0; col < line.length; col++) {
          if (line[col] === '#') {
            drawPx(col, row, CREAM);
          }
        }
      });

      // ── Belly patch ───────────────────────────────────────────────
      for (let c = 4; c <= 7; c++) for (let r = 7; r <= 8; r++) drawPx(c, r, BELLY, 0.7);

      // ── Scarf (category coral) ────────────────────────────────────
      for (let c = 3; c <= 8; c++) drawPx(c, 8, CORAL);
      for (let c = 2; c <= 9; c++) drawPx(c, 9, CORAL);

      // ── Little feet ───────────────────────────────────────────────
      drawPx(3, 10, INK); drawPx(4, 10, INK);
      drawPx(7, 10, INK); drawPx(8, 10, INK);

      // ── Tail ──────────────────────────────────────────────────────
      const wag = Math.sin(t * 6) * 0.5;
      drawPx(10, 9 - Math.round(wag), INK);
      drawPx(11, 8 - Math.round(wag), INK);

      // ── Antenna + gold star ───────────────────────────────────────
      drawPx(5, 2, INK); drawPx(5, 1, GOLD); drawPx(6, 1, GOLD);
      drawPx(4, 0, GOLD); drawPx(5, 0, GOLD_DEEP); drawPx(6, 0, GOLD); drawPx(7, 0, GOLD);

      // ── Eyes ──────────────────────────────────────────────────────
      const eyes = blinkPhase < 0.12 ? EYES_BLINK : EYES_OPEN;
      const actualEyes = mood === 'sleepy' ? EYES_CLOSED : mood === 'excited' ? EYES_HAPPY : eyes;
      actualEyes.forEach(([c, r, slot]) => {
        drawPx(c, r, slot === 'white' ? WHITE : INK);
      });

      // ── Cheeks ────────────────────────────────────────────────────
      if (mood !== 'sleepy') {
        drawPx(2, 7, CHEEK, 0.45); drawPx(3, 7, CHEEK, 0.4);
        drawPx(8, 7, CHEEK, 0.45); drawPx(9, 7, CHEEK, 0.4);
      }

      // ── Mouth ─────────────────────────────────────────────────────
      if (mood !== 'sleepy') {
        const mouth = mood === 'excited' ? [[4, 8], [5, 8], [6, 9], [7, 8], [8, 8]]
          : [[4, 8], [7, 8], [5, 9], [6, 9]];
        mouth.forEach(([c, r]) => drawPx(c, r, INK));
      }

      // ── Growth accessories ────────────────────────────────────────
      if (stage === 'baby') {
        drawPx(3, 2, LEAF); drawPx(4, 1, LEAF); drawPx(4, 2, LEAF); drawPx(4, 3, LEAF);
      } else if (stage === 'mature') {
        drawPx(3, 0, GOLD, 0.6); drawPx(6, 2, GOLD_DEEP, 0.5);
      }

      // ── Sleep Z's ─────────────────────────────────────────────────
      if (mood === 'sleepy') {
        const zDrift = Math.floor((t * 1.5) % 3);
        const zs: [number, number, number][] = [[8, 2 - zDrift, 0.8], [9, 3 - zDrift, 0.6], [10, 4 - zDrift, 0.4]];
        zs.forEach(([c, r, a]) => { if (r >= 0) drawPx(c, r, INK, a); });
      }

      ctx.restore();
      t += 0.016;
      frameRef.current = requestAnimationFrame(draw);
    };

    frameRef.current = requestAnimationFrame(draw);
    return () => cancelAnimationFrame(frameRef.current);
  }, [stage, mood, animKey, squish, hop]);

  const size = stage === 'baby' ? 64 : stage === 'evolved' ? 80 : 96;
  return <canvas ref={canvasRef} width={size} height={size} style={{ width: size, height: size, imageRendering: 'pixelated' }} />;
});

// ─── Dialogue bubble ──────────────────────────────────────────────────
const DialogueBubble: React.FC<{ text: string; stage: PetStage; isDark: boolean }> = ({ text, stage, isDark }) => {
  const stageColor = stage === 'baby' ? '#FFB6C1' : stage === 'evolved' ? '#90EE90' : '#ADD8E6';
  const bg = isDark ? 'rgba(30,25,20,0.92)' : 'rgba(255,255,255,0.95)';
  return (
    <div className="absolute bottom-full left-1/2 -translate-x-1/2 mb-2 px-3 py-2 rounded-xl max-w-[180px] text-center animate-fade-in"
      style={{ background: bg, boxShadow: '0 2px 8px rgba(0,0,0,0.12)', border: `1px solid ${stageColor}40` }}>
      <span className="text-xs" style={{ color: isDark ? '#E4D2BC' : '#3B0A17' }}>{text}</span>
      <div className="absolute top-full left-1/2 -translate-x-1/2"
        style={{ width: 0, height: 0, borderLeft: '5px solid transparent', borderRight: '5px solid transparent', borderTop: `5px solid ${bg}` }} />
    </div>
  );
};

// ─── Main FloatingPet ─────────────────────────────────────────────────
export const FloatingPet: React.FC = () => {
  const { isDark } = useTheme();
  const [petSystem] = useState(() => getPetSystem());
  const [questSystem] = useState(() => getQuestSystem());
  const [, setRefresh] = useState(0);
  const [isHovered, setIsHovered] = useState(false);
  const [animKey, setAnimKey] = useState(0);
  const [squish, setSquish] = useState(0);
  const [hop, setHop] = useState(0);

  useEffect(() => {
    const u1 = petSystem.subscribe(() => setRefresh(k => k + 1));
    const u2 = questSystem.subscribe(() => { petSystem.updateStage(); setRefresh(k => k + 1); });
    return () => { u1(); u2(); };
  }, [petSystem, questSystem]);

  useEffect(() => { petSystem.updateStage(); }, [petSystem]);

  const handleClick = () => {
    petSystem.interact();
    setAnimKey(k => k + 1);
    // Squish animation
    setSquish(1);
    const interval = setInterval(() => setSquish(s => {
      if (s <= 0) { clearInterval(interval); return 0; }
      return s - 0.06;
    }), 16);
    // Hop
    const dur = 200;
    const start = performance.now();
    const race = () => {
      const t = (performance.now() - start) / dur;
      if (t >= 1) { setHop(0); return; }
      setHop(Math.sin(t * Math.PI));
      requestAnimationFrame(race);
    };
    race();
  };

  const state = petSystem.getState();
  const stage = petSystem.getStage();
  const mood = petSystem.getMood();
  const dialogue = petSystem.getCurrentDialogue();
  const isVisible = petSystem.isFloatingVisible();

  if (!isVisible) return null;

  return (
    <div className="fixed bottom-20 right-4 z-30 flex flex-col items-center" style={{ pointerEvents: 'auto' }}>
      {dialogue && <DialogueBubble text={dialogue} stage={stage} isDark={isDark} />}
      <div className="relative cursor-pointer" onMouseEnter={() => setIsHovered(true)} onMouseLeave={() => setIsHovered(false)}
        style={{ transform: isHovered ? 'scale(1.08)' : 'scale(1)', transition: 'transform 0.2s' }}>
        <button onClick={handleClick} className="block" title="Tap to interact">
          <PixelPet stage={stage} mood={mood} animKey={animKey} squish={squish} hop={hop} />
        </button>
        <div className="absolute -bottom-1 left-1/2 -translate-x-1/2 px-2 py-0.5 rounded-full text-[10px] font-medium"
          style={{ background: isDark ? 'rgba(0,0,0,0.5)' : 'rgba(255,255,255,0.9)', color: isDark ? '#ccc' : '#3B0A17' }}>
          {stage}
        </div>
      </div>
      <div className="mt-1 text-[10px] font-medium opacity-60" style={{ color: isDark ? '#ccc' : '#3B0A17' }}>
        {state.name}
      </div>
    </div>
  );
};

export default FloatingPet;
