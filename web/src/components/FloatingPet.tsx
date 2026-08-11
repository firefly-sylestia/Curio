// Curio Web App - Floating Pet (Pixel Art, Draggable, Wander AI)
// Canvas-drawn pixel spirit with drag-to-move and autonomous wandering

import React, { useState, useEffect, useRef, useCallback } from 'react';
import { getPetSystem } from '../data/PetSystem';
import type { PetStage } from '../data/PetSystem';
import { getQuestSystem } from '../data/QuestSystem';
import { useTheme } from '../theme/ThemeContext';

// ─── Pixel palette ────────────────────────────────────────────────────
const CREAM = '#FFF8E7';
const INK = '#3D2B1F';
const CHEEK = '#FFB5B5';
const GOLD = '#FFD700';
const GOLD_DEEP = '#DAA520';
const BELLY = '#FFFBF0';
const CORAL = '#FF8FA3';
const LEAF = '#9CCB8B';
const WHITE = '#FFFFFF';
const BODY = [
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

// ─── Pixel sprite ─────────────────────────────────────────────────────
const PixelPet: React.FC<{
  stage: PetStage; mood: string; animKey: number; squish: number; hop: number; dizzy: boolean;
}> = React.memo(({ stage, mood, animKey, squish, hop, dizzy }) => {
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
      const dizzyWobble = dizzy ? Math.sin(t * 22) * 3 : 0;

      ctx.save();
      ctx.translate(size / 2, size / 2 + bob * px + h);
      ctx.scale(sx, sy);
      ctx.rotate(dizzyWobble * Math.PI / 180);
      ctx.translate(-size / 2, -size / 2);

      const dp = (c: number, r: number, color: string, a = 1) => {
        ctx.fillStyle = color; ctx.globalAlpha = a;
        ctx.fillRect((c + 2) * px, r * px, px, px);
        ctx.globalAlpha = 1;
      };
      BODY.forEach((l, r) => { for (let c = 0; c < l.length; c++) if (l[c] === '#') dp(c, r, CREAM); });
      for (let c = 4; c <= 7; c++) for (let r = 7; r <= 8; r++) dp(c, r, BELLY, 0.7);
      for (let c = 3; c <= 8; c++) dp(c, 8, CORAL);
      for (let c = 2; c <= 9; c++) dp(c, 9, CORAL);
      dp(3, 10, INK); dp(4, 10, INK); dp(7, 10, INK); dp(8, 10, INK);
      const wag = Math.sin(t * 6) * 0.5;
      dp(10, 9 - Math.round(wag), INK); dp(11, 8 - Math.round(wag), INK);
      dp(5, 2, INK); dp(5, 1, GOLD); dp(6, 1, GOLD);
      dp(4, 0, GOLD); dp(5, 0, GOLD_DEEP); dp(6, 0, GOLD); dp(7, 0, GOLD);
      const eyes = blinkPhase < 0.12 ? EYES_BLINK : dizzy ? EYES_OPEN : EYES_OPEN;
      const actualEyes = mood === 'sleepy' ? EYES_CLOSED : mood === 'excited' || mood === 'happy' ? EYES_HAPPY : eyes;
      actualEyes.forEach(([c, r, s]) => dp(c, r, s === 'white' ? WHITE : INK));
      if (mood !== 'sleepy') {
        dp(2, 7, CHEEK, 0.45); dp(3, 7, CHEEK, 0.4); dp(8, 7, CHEEK, 0.45); dp(9, 7, CHEEK, 0.4);
      }
      if (mood !== 'sleepy') {
        (mood === 'excited' ? [[4, 8], [5, 8], [6, 9], [7, 8], [8, 8]] as [number,number][] : [[4, 8], [7, 8], [5, 9], [6, 9]] as [number,number][])
          .forEach(([c, r]) => dp(c, r, INK));
      }
      if (stage === 'baby') { dp(3, 2, LEAF); dp(4, 1, LEAF); dp(4, 2, LEAF); dp(4, 3, LEAF); }
      else if (stage === 'mature') { dp(3, 0, GOLD, 0.6); dp(6, 2, GOLD_DEEP, 0.5); }
      if (mood === 'sleepy') {
        const zd = Math.floor((t * 1.5) % 3);
        [[8, 2 - zd, 0.8], [9, 3 - zd, 0.6], [10, 4 - zd, 0.4]].forEach(([c, r, a]) => { if (r >= 0) dp(c as number, r as number, INK, a as number); });
      }
      ctx.restore();
      t += 0.016;
      frameRef.current = requestAnimationFrame(draw);
    };
    frameRef.current = requestAnimationFrame(draw);
    return () => cancelAnimationFrame(frameRef.current);
  }, [stage, mood, animKey, squish, hop, dizzy]);
  const sz = stage === 'baby' ? 64 : stage === 'evolved' ? 80 : 96;
  return <canvas ref={canvasRef} width={sz} height={sz} style={{ width: sz, height: sz, imageRendering: 'pixelated' }} />;
});

// ─── Dialogue bubble ──────────────────────────────────────────────────
const DialogueBubble: React.FC<{ text: string; isDark: boolean }> = ({ text, isDark }) => {
  const bg = isDark ? 'rgba(30,25,20,0.94)' : 'rgba(255,255,255,0.95)';
  return (
    <div className="absolute bottom-full left-1/2 -translate-x-1/2 mb-2 px-3 py-2 rounded-xl max-w-[180px] text-center z-10"
      style={{ background: bg, boxShadow: '0 2px 8px rgba(0,0,0,0.12)', border: '1px solid rgba(255,143,163,0.3)', animation: 'curio-stagger-fade-up 0.3s ease-out both' }}>
      <span className="text-xs" style={{ color: isDark ? '#E4D2BC' : '#3B0A17' }}>{text}</span>
      <div className="absolute top-full left-1/2 -translate-x-1/2"
        style={{ width: 0, height: 0, borderLeft: '5px solid transparent', borderRight: '5px solid transparent', borderTop: `5px solid ${bg}` }} />
    </div>
  );
};

// ═══════════════════════════════════════════════════════════════════════
// FloatingPet — draggable + autonomous wander AI
// ═══════════════════════════════════════════════════════════════════════

export const FloatingPet: React.FC = () => {
  const { isDark } = useTheme();
  const [petSystem] = useState(() => getPetSystem());
  const [questSystem] = useState(() => getQuestSystem());
  const [, setRefresh] = useState(0);
  const [animKey, setAnimKey] = useState(0);
  const [squish, setSquish] = useState(0);
  const [hop, setHop] = useState(0);
  const [dizzy, setDizzy] = useState(false);

  // Position state
  const [pos, setPos] = useState({ x: window.innerWidth - 80, y: window.innerHeight - 200 });
  const posRef = useRef(pos);
  const [dragging, setDragging] = useState(false);
  const dragStart = useRef({ x: 0, y: 0, px: 0, py: 0 });
  const dragAt = useRef(0);
  const wanderTimer = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);

  posRef.current = pos;

  useEffect(() => {
    const unsub1 = petSystem.subscribe(() => setRefresh(k => k + 1));
    const unsub2 = questSystem.subscribe(() => { petSystem.updateStage(); setRefresh(k => k + 1); });
    return () => { unsub1(); unsub2(); };
  }, [petSystem, questSystem]);
  useEffect(() => { petSystem.updateStage(); }, [petSystem]);

  // ── Wander loop: pick random spot every 3-7s, walk toward it ─────
  useEffect(() => {
    let active = true;
    const wander = async () => {
      while (active) {
        const wait = 3000 + Math.random() * 4000;
        await new Promise(r => { wanderTimer.current = setTimeout(r, wait); });
        if (!active || dragging) continue;
        const tx = 40 + Math.random() * (window.innerWidth - 120);
        const ty = 60 + Math.random() * (window.innerHeight - 220);
        const steps = 40;
        const startPos = { ...posRef.current };
        for (let i = 1; i <= steps; i++) {
          if (dragging) break;
          const t = i / steps;
          const eased = t < 0.5 ? 2 * t * t : -1 + (4 - 2 * t) * t;
          setPos({
            x: startPos.x + (tx - startPos.x) * eased,
            y: startPos.y + (ty - startPos.y) * eased,
          });
          await new Promise(r => setTimeout(r, 24));
        }
      }
    };
    wander();
    return () => { active = false; clearTimeout(wanderTimer.current); };
  }, [dragging]);

  // ── Drag handlers ──────────────────────────────────────────────────
  const handlePointerDown = useCallback((e: React.PointerEvent) => {
    e.preventDefault();
    (e.target as HTMLElement).setPointerCapture(e.pointerId);
    setDragging(true);
    setDizzy(true);
    dragStart.current = { x: e.clientX, y: e.clientY, px: pos.x, py: pos.y };
    dragAt.current = Date.now();
  }, [pos]);

  const handlePointerMove = useCallback((e: React.PointerEvent) => {
    if (!dragging) return;
    const dx = e.clientX - dragStart.current.x;
    const dy = e.clientY - dragStart.current.y;
    setPos({
      x: Math.max(0, Math.min(window.innerWidth - 80, dragStart.current.px + dx)),
      y: Math.max(0, Math.min(window.innerHeight - 120, dragStart.current.py + dy)),
    });
  }, [dragging]);

  const handlePointerUp = useCallback(() => {
    setDragging(false);
    setDizzy(false);
    // Throw flick
    if (Date.now() - dragAt.current < 200) {
      setAnimKey(k => k + 1);
    }
  }, []);

  // ── Tap interaction ────────────────────────────────────────────────
  const handleClick = () => {
    if (dragging) return;
    petSystem.interact();
    setAnimKey(k => k + 1);
    setSquish(1);
    const iv = setInterval(() => setSquish(s => {
      if (s <= 0) { clearInterval(iv); return 0; } return s - 0.06;
    }), 16);
    const dur = 200, start = performance.now();
    const race = () => {
      const t = (performance.now() - start) / dur;
      if (t >= 1) { setHop(0); return; }
      setHop(Math.sin(t * Math.PI));
      requestAnimationFrame(race);
    };
    race();
  };

  const stage = petSystem.getStage();
  const mood = petSystem.getMood();
  const dialogue = petSystem.getCurrentDialogue();
  if (!petSystem.isFloatingVisible()) return null;

  const petSize = stage === 'baby' ? 64 : stage === 'evolved' ? 80 : 96;

  return (
    <div className="fixed z-30 select-none" style={{ left: pos.x, top: pos.y, pointerEvents: 'auto' }}>
      {dialogue && <DialogueBubble text={dialogue} isDark={isDark} />}
      <div
        onPointerDown={handlePointerDown}
        onPointerMove={handlePointerMove}
        onPointerUp={handlePointerUp}
        onPointerCancel={handlePointerUp}
        onClick={handleClick}
        className="cursor-grab active:cursor-grabbing touch-none"
        style={{
          width: petSize, height: petSize,
          transform: dragging ? 'scale(0.92, 1.08)' : 'scale(1)',
          transition: dragging ? 'none' : 'transform 0.3s cubic-bezier(0.1, 0.7, 0.2, 1.1)',
        }}>
        <PixelPet stage={stage} mood={mood} animKey={animKey} squish={squish} hop={hop} dizzy={dizzy} />
      </div>
    </div>
  );
};

export default FloatingPet;
