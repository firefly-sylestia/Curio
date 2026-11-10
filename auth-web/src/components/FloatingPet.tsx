// Curio Web App - Floating Pet (Pixel Art, Draggable, Wander AI)
// Android-matching spark-spirit: round cream body, big eyes, gold antenna,
// coral scarf, waggy tail, feet, stage accessories, mood expressions.

import React, { useState, useEffect, useRef, useCallback } from 'react';
import { getPetSystem } from '../data/PetSystem';
import type { PetStage } from '../data/PetSystem';
import { getQuestSystem } from '../data/QuestSystem';
import { useTheme } from '../theme/ThemeContext';

// ─── Pixel palette (matching Android CurioPetSprite) ──────────────────
const CREAM    = '#FFF8E7';
const BODY_B   = '#F0E4C8'; // body shade
const INK      = '#3D2B1F';
const WHITE    = '#FFFFFF';
const CORAL    = '#FF8FA3';
const GOLD     = '#FFD700';
const GOLD_DEEP = '#DAA520';
const STAR_EYE = '#8B6914'; // warm brown for excited eyes
const BELLY    = '#FFFBF0';
const CHEEK    = '#FFB5B5';
const LEAF     = '#9CCB8B';
const EVO_RED  = '#FF6B4A';

// ─── Body grid (16×16 — matching Android default design) ─────────────
// The spark-spirit: round cream body with scarf
const BODY_GRID: string[] = [
  '....####....',  // 0
  '...######...',  // 1
  '..########..',  // 2
  '.##########.',  // 3
  '.##########.',  // 4
  '############',  // 5
  '############',  // 6
  '############',  // 7 - scarf top
  '############',  // 8 - scarf bottom
  '.##########.',  // 9
  '.##########.',  // 10 - feet top
  '..########..',  // 11 - feet bottom
  '...######...',  // 12
  '....####....',  // 13
];

// ─── Eye pixel maps (matching Android EYE_STYLE_PIXELS) ──────────────
// (col, row, 'ink'|'white'|'star') — in 16×16 space
type EyePixel = [number, number, 'ink'|'white'|'star'];

const EYES_OPEN: EyePixel[] = [
  [4,6,'ink'],[5,6,'ink'],[4,7,'ink'],[5,7,'ink'],
  [10,6,'ink'],[11,6,'ink'],[10,7,'ink'],[11,7,'ink'],
  [4,6,'white'],[10,6,'white'],
];
const EYES_BLINK: EyePixel[] = [
  [4,6,'ink'],[5,6,'ink'],[10,6,'ink'],[11,6,'ink'],
];
const EYES_CLOSED: EyePixel[] = [
  [4,7,'ink'],[5,7,'ink'],[10,7,'ink'],[11,7,'ink'],
];
const EYES_WIDE: EyePixel[] = [
  [4,5,'ink'],[5,5,'ink'],[4,6,'ink'],[5,6,'ink'],[4,7,'ink'],[5,7,'ink'],
  [10,5,'ink'],[11,5,'ink'],[10,6,'ink'],[11,6,'ink'],[10,7,'ink'],[11,7,'ink'],
  [4,6,'white'],[10,6,'white'],
];
const EYES_STAR: EyePixel[] = [
  [4,5,'star'],[5,5,'star'],
  [3,6,'star'],[4,6,'star'],[5,6,'star'],[6,6,'star'],
  [4,7,'star'],[5,7,'star'],
  [4,6,'white'],
  [10,5,'star'],[11,5,'star'],
  [9,6,'star'],[10,6,'star'],[11,6,'star'],[12,6,'star'],
  [10,7,'star'],[11,7,'star'],
  [10,6,'white'],
];
const EYES_DIZZY: EyePixel[] = [
  [4,5,'ink'],[5,5,'ink'],[4,6,'ink'],[5,6,'ink'],[4,7,'ink'],[5,7,'ink'],
  [3,6,'ink'],[6,6,'ink'],
  [4,6,'white'],[5,5,'white'],
  [10,5,'ink'],[11,5,'ink'],[10,6,'ink'],[11,6,'ink'],[10,7,'ink'],[11,7,'ink'],
  [9,6,'ink'],[12,6,'ink'],
  [10,6,'white'],[11,5,'white'],
];
const EYES_HAPPY: EyePixel[] = [
  [4,7,'ink'],[5,6,'ink'],[5,7,'ink'],
  [10,7,'ink'],[10,6,'ink'],[11,7,'ink'],
];

function eyePixelsFor(eyes: string): EyePixel[] {
  switch (eyes) {
    case 'blink': return EYES_BLINK;
    case 'closed': return EYES_CLOSED;
    case 'wide': return EYES_WIDE;
    case 'star': return EYES_STAR;
    case 'dizzy': return EYES_DIZZY;
    case 'happy': return EYES_HAPPY;
    default: return EYES_OPEN;
  }
}

// ─── Pixel Pet Sprite (canvas) ────────────────────────────────────────
const PixelPet: React.FC<{
  stage: PetStage;
  mood: string;
  animKey: number;
  squish: number;
  hop: number;
  dizzy: boolean;
  dragged: boolean;
  moving: boolean;
  thinking: boolean;
  sleeping: boolean;
}> = React.memo(({ stage, mood, animKey, squish, hop, dizzy, dragged, moving, thinking, sleeping }) => {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const frameRef = useRef(0);
  const startT = useRef(Date.now());

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;
    const size = canvas.width;
    const px = size / 16; // 16×16 grid

    let active = true;

    const render = () => {
      if (!active) return;
      const now = Date.now();
      const t = (now - startT.current) / 1000;
      ctx.clearRect(0, 0, size, size);
      ctx.imageSmoothingEnabled = false;

      const blinkPhase = (t * 0.6) % 2.4;
      const bobWave = Math.sin(t * (moving ? 5.2 : 2.6));
      const breatheScale = sleeping ? 1 + Math.sin(t * 1.3) * 0.035 : 1;

      // Screen-space helpers
      const dp = (c: number, r: number, color: string, a = 1) => {
        ctx.fillStyle = color; ctx.globalAlpha = a;
        ctx.fillRect(c * px, r * px, px, px);
        ctx.globalAlpha = 1;
      };
      const dr = (c: number, r: number, color: string, a = 1) => {
        ctx.fillStyle = color; ctx.globalAlpha = a;
        ctx.fillRect(Math.round(c * px), Math.round(r * px), Math.ceil(px), Math.ceil(px));
        ctx.globalAlpha = 1;
      };

      // Transform state
      const sx = squish > 0 ? 1 + squish * 0.08 : breatheScale;
      const sy = squish > 0 ? 1 - squish * 0.06 : breatheScale;
      const hj = hop > 0 ? -hop * 6 * (1 - hop * 0.3) : 0;
      const bobDp = bobWave * (moving ? 3.4 : sleeping ? 1.2 : 2.2);
      const dizzyWobble = dizzy ? Math.sin(t * 22) * 5 : 0;

      ctx.save();
      ctx.translate(size / 2, size / 2 + bobDp + hj);
      ctx.scale(sx, sy);
      ctx.rotate((dizzyWobble * Math.PI) / 180);
      ctx.translate(-size / 2, -size / 2);

      // ── Body ──────────────────────────────────────────────────────
      BODY_GRID.forEach((line, r) => {
        for (let c = 0; c < line.length; c++) {
          if (line[c] === '#') dp(c, r, CREAM);
        }
      });

      // Belly patch
      for (let c = 5; c <= 10; c++) for (let r = 9; r <= 10; r++) dp(c, r, BELLY, 0.85);

      // Scarf (coral, rows 7-8)
      for (let c = 3; c <= 11; c++) dp(c, 7, CORAL);
      for (let c = 2; c <= 11; c++) dp(c, 8, CORAL);

      // Feet (ink, row 10-11, narrower)
      dp(4, 10, INK); dp(5, 10, INK); dp(9, 10, INK); dp(10, 10, INK);
      dp(4, 11, INK); dp(10, 11, INK);

      // Tail — right side, wags when moving/excited
      const wagFreq = moving ? 18 : 10;
      const wag = moving ? Math.sin(t * wagFreq * Math.PI) : 0;
      if (!dragged) {
        dp(13, 10, BODY_B);
        dp(14, 10, BODY_B);
        dp(14, 11, BODY_B);
        if (wag > 0.4) dp(14, 9, BODY_B);
      }

      // ── Antenna (gold star-tipped) ────────────────────────────────
      dp(7, 1, INK); dp(8, 1, INK);
      dp(6, 0, GOLD); dp(7, 0, GOLD); dp(8, 0, GOLD); dp(9, 0, GOLD);
      // Star glint
      if (Math.sin(t * 3) > 0.78) dp(7, 0, WHITE, 0.9);

      // ── Face ──────────────────────────────────────────────────────
      const eyes = sleeping ? 'closed'
        : dizzy ? 'dizzy'
        : dragged ? 'wide'
        : mood === 'happy' || mood === 'excited' ? 'star'
        : mood === 'sleepy' ? 'closed'
        : blinkPhase > 0.93 ? 'blink'
        : 'open';

      const isHappy = mood === 'happy' || mood === 'excited';

      if (!sleeping) {
        // Eyes
        eyePixelsFor(eyes).forEach(([c, r, slot]) => {
          const color = slot === 'white' ? WHITE : slot === 'star' ? STAR_EYE : INK;
          dp(c, r, color);
        });

        // Cheeks (happy/concentrated)
        if (isHappy) {
          dr(2.5, 9, CHEEK, 0.5); dr(3.5, 9, CHEEK, 0.5);
          dr(11.5, 9, CHEEK, 0.5); dr(12.5, 9, CHEEK, 0.5);
        }

        // Mouth
        if (mood === 'excited') {
          // Wide open smile
          dp(6, 10, INK); dp(9, 10, INK);
          dp(6, 11, INK); dp(7, 11, INK); dp(8, 11, INK); dp(9, 11, INK);
        } else if (isHappy) {
          // Smile
          dp(6, 10, INK); dp(9, 10, INK);
          dp(7, 11, INK); dp(8, 11, INK);
        } else if (mood === 'grumpy' || dizzy || dragged) {
          // O mouth
          dp(7, 10, INK); dp(8, 10, INK);
          dp(7, 11, INK); dp(8, 11, INK);
        } else {
          // Default gentle smile
          dp(6, 10, INK); dp(9, 10, INK);
          dp(7, 11, INK); dp(8, 11, INK);
        }
      } else {
        // Sleep: closed eye arcs + nightcap
        dp(5, 4, INK); dp(6, 4, INK);
        dp(9, 4, INK); dp(10, 4, INK);
        // Z's
        const drift = Math.floor((t * 2) % 3);
        dr(10.5, 3 - drift, INK, 0.8); dr(11.5, 4 - drift, INK, 0.7); dr(10.5, 4 - drift, INK, 0.7);
        dr(12.5, 5 - drift, INK, 0.5); dr(13.5, 6 - drift, INK, 0.4);
      }

      // ── Stage accessories ─────────────────────────────────────────
      if (stage === 'baby') {
        // Baby: tiny leaf sprout on head
        dp(4, 2, LEAF); dp(5, 1, LEAF); dp(5, 2, LEAF); dp(5, 3, LEAF);
      } else if (stage === 'evolved') {
        // Evolved: element badge on chest
        const badge = EVO_RED;
        dp(6, 9, badge); dp(7, 9, badge); dp(8, 9, badge); dp(9, 9, badge);
        dp(7, 10, badge); dp(8, 10, badge);
      } else if (stage === 'mature') {
        // Final: subtle gold halo + sparkle
        dp(4, 0, GOLD_DEEP, 0.7); dp(9, 0, GOLD_DEEP, 0.7);
        if (Math.sin(t * 5) > 0.5) dp(6, 0, GOLD, 0.5);
      }

      // ── Dizzy whoosh marks ───────────────────────────────────────
      if (dizzy) {
        const whoosh = Math.sin(t * 20);
        const wa = 0.4 + whoosh * 0.3;
        dr(1, 6, INK, wa); dr(0.5, 7, INK, wa * 0.7); dr(1, 8, INK, wa);
        dr(13.5, 6, INK, wa); dr(14.5, 7, INK, wa * 0.7); dr(13.5, 8, INK, wa);
      }

      // ── Excited sparkles ──────────────────────────────────────────
      if (isHappy && !sleeping) {
        const twinkle = Math.sin(t * 16) * 0.5 + 0.5;
        dr(1.5, 2, GOLD, twinkle * 0.8);
        dr(13.5, 3, GOLD, (1 - twinkle) * 0.8);
      }

      ctx.restore();
      frameRef.current = requestAnimationFrame(render);
    };

    frameRef.current = requestAnimationFrame(render);
    return () => { active = false; cancelAnimationFrame(frameRef.current); };
  }, [stage, mood, animKey, squish, hop, dizzy, dragged, moving, thinking, sleeping]);

  const sz = stage === 'baby' ? 56 : stage === 'evolved' ? 72 : 88;
  return <canvas ref={canvasRef} width={sz} height={sz}
    style={{ width: sz, height: sz, imageRendering: 'pixelated' }} />;
});

// ─── Dialogue bubble ──────────────────────────────────────────────────
const DialogueBubble: React.FC<{ text: string; isDark: boolean }> = ({ text, isDark }) => {
  const bg = isDark ? 'rgba(30,25,20,0.94)' : 'rgba(255,255,255,0.95)';
  return (
    <div className="absolute bottom-full left-1/2 -translate-x-1/2 mb-2 px-3 py-2 rounded-xl max-w-[180px] text-center z-10"
      style={{ background: bg, boxShadow: '0 2px 8px rgba(0,0,0,0.12)', border: '1px solid rgba(255,143,163,0.3)', animation: 'curio-stagger-fade-up 0.3s ease-out both' }}>
      <span className="text-xs leading-snug" style={{ color: isDark ? '#E4D2BC' : '#3B0A17' }}>{text}</span>
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
  const [quietDizzy, setQuietDizzy] = useState(false);

  // Position state
  const [pos, setPos] = useState({ x: window.innerWidth - 80, y: window.innerHeight - 200 });
  const posRef = useRef(pos);
  const [dragging, setDragging] = useState(false);
  const dragStart = useRef({ x: 0, y: 0, px: 0, py: 0 });
  const dragAt = useRef(0);
  const wanderTimer = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);
  const moving = useRef(false);

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
        const wait = 3000 + Math.random() * 5000;
        await new Promise(r => { wanderTimer.current = setTimeout(r, wait); });
        if (!active || dragging) continue;
        const tx = 40 + Math.random() * (window.innerWidth - 120);
        const ty = 60 + Math.random() * (window.innerHeight - 220);
        const steps = 40;
        moving.current = true;
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
        moving.current = false;
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
    setQuietDizzy(false);
    dragStart.current = { x: e.clientX, y: e.clientY, px: pos.x, py: pos.y };
    dragAt.current = Date.now();
  }, [pos]);

  const handlePointerMove = useCallback((e: React.PointerEvent) => {
    if (!dragging) return;
    const dx = e.clientX - dragStart.current.x;
    const dy = e.clientY - dragStart.current.y;
    setPos({
      x: Math.max(-20, Math.min(window.innerWidth - 60, dragStart.current.px + dx)),
      y: Math.max(-20, Math.min(window.innerHeight - 100, dragStart.current.py + dy)),
    });
  }, [dragging]);

  const handlePointerUp = useCallback(() => {
    setDragging(false);
    setDizzy(false);
    // Brief recovery wobble after drag
    setQuietDizzy(true);
    setTimeout(() => setQuietDizzy(false), 900);
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
    petSystem.interact();
  };

  const stage = petSystem.getStage();
  const mood = petSystem.getMood();
  const dialogue = petSystem.getCurrentDialogue();
  const sleeping = mood === 'sleepy' && !dragging && !moving.current;
  if (!petSystem.isFloatingVisible()) return null;

  const petSize = stage === 'baby' ? 56 : stage === 'evolved' ? 72 : 88;

  return (
    <div className="fixed z-30 select-none" style={{
      left: pos.x, top: pos.y, pointerEvents: 'auto',
      transition: (dragging || moving.current) ? 'none' : 'transform 0.3s cubic-bezier(0.1, 0.7, 0.2, 1.1)',
    }}>
      {dialogue && !dragging && <DialogueBubble text={dialogue} isDark={isDark} />}
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
        <PixelPet
          stage={stage}
          mood={mood}
          animKey={animKey}
          squish={squish}
          hop={hop}
          dizzy={dizzy || quietDizzy}
          dragged={dragging}
          moving={moving.current}
          thinking={false}
          sleeping={sleeping}
        />
      </div>
    </div>
  );
};

export default FloatingPet;
