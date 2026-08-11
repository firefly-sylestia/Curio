// Curio Web App - Spin Screen
// Matches Android: 2 peek cards (above/below hero), receding deck colors, bottom Categories·Filter

import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useTheme, getBackgroundColor, getTextColor, getPastelCardFill } from '../theme/ThemeContext';
import { ALL_CATEGORIES, getCategoryBySlug } from '../data/categories';
import { getRandomTopic } from '../data/topics';
import { getQuestSystem } from '../data/QuestSystem';
import type { CurioCategory, CurioTopic } from '../types';
import { MaterialIcon, CurioWatermarkBackdrop } from '../components/SharedComponents';
import { usePressable } from '../animations';

const ORBIT_DOTS = 12;
const SPIN_MIN = 2200;
const SPIN_MAX = 3000;

const hexToRgb = (hex: string): [number, number, number] => {
  const r = parseInt(hex.slice(1, 3), 16);
  const g = parseInt(hex.slice(3, 5), 16);
  const b = parseInt(hex.slice(5, 7), 16);
  return [r, g, b];
};

// ─── CSS Dice ─────────────────────────────────────────────────────────
const CssDice: React.FC<{ size: number; tumbling: boolean; ink: string }> = ({ size, tumbling, ink }) => {
  const pipSize = size * 0.16; const gap = size * 0.22; const mid = size * 0.39;
  const pip = (x: number, y: number) => (
    <div key={`${x}-${y}`} className="absolute rounded-full"
      style={{ width: pipSize, height: pipSize, left: x - pipSize / 2, top: y - pipSize / 2,
        background: tumbling ? 'rgba(255,255,255,0.9)' : ink, transition: 'background 0.3s' }} />
  );
  return (
    <div className="relative rounded-[18%]" style={{ width: size, height: size,
      background: tumbling ? 'rgba(255,255,255,0.15)' : `${ink}10`,
      border: tumbling ? '2px solid rgba(255,255,255,0.25)' : `2px solid ${ink}30`,
      animation: tumbling ? 'curio-dice-tumble 1.6s linear infinite' : 'curio-dice-breathe 3s ease-in-out infinite' }}>
      {pip(gap, gap)}{pip(size - gap, gap)}{pip(mid, mid)}{pip(gap, size - gap)}{pip(size - gap, size - gap)}
    </div>
  );
};

// ─── Confetti ─────────────────────────────────────────────────────────
const ConfettiBurst: React.FC<{ trigger: number; color: string }> = ({ trigger, color }) => {
  const [particles, setParticles] = useState<Array<{ id: number; x: number; y: number; rot: number; size: number; delay: number }>>([]);
  useEffect(() => {
    if (trigger === 0) return;
    const p = Array.from({ length: 20 }, (_, i) => ({
      id: i, x: (Math.random() - 0.5) * 280, y: (Math.random() - 0.5) * 280 - 50,
      rot: Math.random() * 720 - 360, size: 4 + Math.random() * 7, delay: Math.random() * 0.12,
    }));
    setParticles(p);
    setTimeout(() => setParticles([]), 1100);
  }, [trigger]);
  if (particles.length === 0) return null;
  return (
    <div className="absolute inset-0 pointer-events-none z-30 flex items-center justify-center">
      {particles.map(p => (
        <div key={p.id} className="absolute rounded-sm"
          style={{ width: p.size, height: p.size * 1.6,
            background: p.id % 3 === 0 ? '#FFD700' : p.id % 3 === 1 ? color : '#FFFFFF',
            left: '50%', top: '50%', opacity: 0,
            animation: `curio-confetti-fly 0.7s ease-out ${p.delay}s forwards`,
            '--tx': `${p.x}px`, '--ty': `${p.y}px`, '--rot': `${p.rot}deg`,
          } as React.CSSProperties} />
      ))}
    </div>
  );
};

// ─── Hero Ticket Card ─────────────────────────────────────────────────
const HeroTicket: React.FC<{
  topic: CurioTopic | null; category: CurioCategory;
  isSpinning: boolean; spinPhase: 'idle' | 'spinning' | 'landed' | 'opening';
  onClick: () => void;
}> = ({ topic, category, isSpinning, spinPhase, onClick }) => {
  const { isDark, pastelColors } = useTheme();
  const [contentKey, setContentKey] = useState(0);
  const [shimmerPos, setShimmerPos] = useState(-100);

  useEffect(() => {
    if (spinPhase !== 'spinning') return;
    const iv = setInterval(() => setContentKey(k => k + 1), 260);
    return () => clearInterval(iv);
  }, [spinPhase]);

  useEffect(() => {
    if (spinPhase === 'spinning') return;
    const iv = setInterval(() => setShimmerPos(p => (p >= 200 ? -100 : p + 1.2)), 35);
    return () => clearInterval(iv);
  }, [spinPhase]);

  const baseAccent = pastelColors && !isDark ? getPastelCardFill(category.accent, isDark) : category.accent;
  const [r, g, b] = hexToRgb(baseAccent);
  const surfaceRgb = isDark ? [18, 18, 35] : [247, 240, 228];

  // Rich opaque gradient — deep accent at top, still visibly tinted at bottom
  const midR = Math.round(r * 0.48 + surfaceRgb[0] * 0.52);
  const midG = Math.round(g * 0.48 + surfaceRgb[1] * 0.52);
  const midB = Math.round(b * 0.48 + surfaceRgb[2] * 0.52);
  const botR = Math.round(r * 0.22 + surfaceRgb[0] * 0.78);
  const botG = Math.round(g * 0.22 + surfaceRgb[1] * 0.78);
  const botB = Math.round(b * 0.22 + surfaceRgb[2] * 0.78);
  const topColor = `rgb(${r},${g},${b})`;
  const midColor = `rgb(${midR},${midG},${midB})`;
  const botColor = `rgb(${botR},${botG},${botB})`;

  const hasLanded = spinPhase === 'landed' || spinPhase === 'opening';

  return (
    <button onClick={onClick} disabled={isSpinning}
      className="relative w-[286px] h-[310px] rounded-[28px] overflow-hidden text-left flex-shrink-0"
      style={{
        background: `linear-gradient(180deg, ${topColor} 0%, ${midColor} 50%, ${botColor} 100%)`,
        boxShadow: hasLanded
          ? `0 8px 32px ${category.accent}40, 0 2px 8px rgba(0,0,0,0.12)`
          : '0 4px 16px rgba(0,0,0,0.06), 0 1px 4px rgba(0,0,0,0.03)',
        cursor: isSpinning ? 'default' : 'pointer',
        border: isDark ? `1px solid ${category.accent}30` : `1px solid ${category.accent}1A`,
        transform: spinPhase === 'opening' ? 'scale(1.03)' : isSpinning ? 'scale(0.97)' : 'scale(1)',
        transition: 'transform 0.4s cubic-bezier(0.2,0.8,0.3,1), box-shadow 0.4s ease',
      }}>
      {/* Accent rule */}
      <div className="absolute top-0 left-3 right-3 h-[2px] rounded-full" style={{ background: category.accent, opacity: 0.45 }} />
      {/* Top-lit crown */}
      <div className="absolute top-0 left-0 right-0 h-[60px] pointer-events-none"
        style={{ background: 'linear-gradient(180deg, rgba(255,255,255,0.07) 0%, transparent 100%)' }} />
      {/* Shimmer */}
      {spinPhase !== 'spinning' && (
        <div className="absolute inset-0 pointer-events-none"
          style={{ background: 'linear-gradient(105deg, transparent 35%, rgba(255,255,255,0.05) 48%, transparent 61%)', transform: `translateX(${shimmerPos}%)` }} />
      )}
      {/* Watermark glyph */}
      <div className="absolute right-2 bottom-2 pointer-events-none select-none"
        style={{ color: isDark ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.04)' }}>
        <MaterialIcon name={category.iconGlyph} size={130} />
      </div>

      <div className="relative z-10 flex flex-col items-center justify-center h-full p-5 text-center">
        {spinPhase === 'spinning' ? (
          <div className="flex flex-col items-center gap-3 animate-reelFade" key={contentKey}>
            <div className="flex items-center gap-1 px-3 py-0.5 rounded-full text-[11px] font-semibold tracking-wide uppercase"
              style={{ background: 'rgba(255,255,255,0.15)', color: 'white', backdropFilter: 'blur(4px)' }}>
              <MaterialIcon name={category.iconGlyph} size={11} /> {topic?.subtype || 'Topic'}
            </div>
            <div className="text-[19px] font-extrabold text-white leading-tight px-2"
              style={{ fontFamily: 'Geom, Inter, sans-serif' }}>{topic?.name || '...'}</div>
            <div className="w-12 h-1 rounded-full bg-white/20" />
            <p className="text-white/45 text-[10px] tracking-widest uppercase">Spinning</p>
          </div>
        ) : topic ? (
          <>
            <div className="flex items-center gap-1 px-3 py-0.5 rounded-full text-[11px] font-semibold tracking-wide uppercase mb-4"
              style={{ background: 'rgba(255,255,255,0.15)', color: 'white', backdropFilter: 'blur(4px)' }}>
              <MaterialIcon name={category.iconGlyph} size={11} /> {topic.subtype}
            </div>
            {spinPhase === 'opening' ? (
              <div className="flex flex-col items-center gap-3">
                <h2 className="text-[20px] font-extrabold text-white leading-tight" style={{ fontFamily: 'Geom, Inter, sans-serif' }}>{topic.name}</h2>
                <div className="w-8 h-8 border-4 border-t-transparent rounded-full animate-spin"
                  style={{ borderColor: 'rgba(255,255,255,0.4)', borderTopColor: 'transparent' }} />
                <p className="text-white/60 text-xs">Opening...</p>
              </div>
            ) : (
              <>
                <h2 className="text-[20px] font-extrabold text-white leading-tight mb-1.5 px-2"
                  style={{ fontFamily: 'Geom, Inter, sans-serif', textShadow: '0 1px 6px rgba(0,0,0,0.12)' }}>{topic.name}</h2>
                <p className="text-[12px] text-white/70 leading-relaxed line-clamp-3 px-1">{topic.teaser}</p>
                <div className="mt-4 flex items-center gap-1.5 text-[10px] text-white/45 tracking-wide uppercase">
                  <MaterialIcon name="touch_app" size={13} /> Tap to open
                </div>
              </>
            )}
          </>
        ) : (
          <>
            <MaterialIcon name="casino" size={42} className="text-white/50 mb-3" />
            <h2 className="text-xl font-bold text-white mb-1" style={{ fontFamily: 'Geom, Inter, sans-serif' }}>{category.displayName}</h2>
            <p className="text-[12px] text-white/55">Tap the dice to spin</p>
          </>
        )}
      </div>
    </button>
  );
};

// ─── Peek Card (matching Android: 360×116dp near, 19dp corner, receding) ─
const PeekCard: React.FC<{ slot: -1 | 1; topic: CurioTopic | null; category: CurioCategory; isSpinning: boolean }> = ({ slot, topic, category, isSpinning }) => {
  const { isDark } = useTheme();
  if (!topic) return null;
  // Match Android near-peek: 360×116dp, corner 19dp
  const w = 360; const h = 116; const corner = 19;
  // Android yOff: -134 for top near, +146 for bottom near
  const yOff = slot === -1 ? -134 : 146;

  const [r, g, b] = hexToRgb(category.accent);
  const surfaceRgb = isDark ? [18, 18, 35] : [247, 240, 228];
  // Level-darkened: near peeks step one shade down from the hero (0.40 blend)
  const depth = 0.40;
  const dr = Math.round(r * (1 - depth) + surfaceRgb[0] * depth);
  const dg = Math.round(g * (1 - depth) + surfaceRgb[1] * depth);
  const db = Math.round(b * (1 - depth) + surfaceRgb[2] * depth);
  // Subtle second stop for gradient depth
  const dr2 = Math.round(dr * 0.78 + surfaceRgb[0] * 0.22);
  const dg2 = Math.round(dg * 0.78 + surfaceRgb[1] * 0.22);
  const db2 = Math.round(db * 0.78 + surfaceRgb[2] * 0.22);

  return (
    <div className="absolute left-1/2 flex items-center overflow-hidden"
      style={{
        width: w, height: h, borderRadius: corner,
        top: `calc(50% + ${yOff}px)`,
        transform: 'translate(-50%, -50%)',
        background: `linear-gradient(180deg, rgb(${dr},${dg},${db}), rgb(${dr2},${dg2},${db2}))`,
        boxShadow: '0 2px 8px rgba(0,0,0,0.05)',
        border: isDark ? `1px solid ${category.accent}18` : `1px solid ${category.accent}10`,
        opacity: isSpinning ? 0.35 : 0.55,
        transition: 'opacity 0.35s ease',
        zIndex: slot === -1 ? 2 : 5,
      }}>
      {/* Left accent strip */}
      <div className="absolute left-[12px] top-0 bottom-0 w-[3px] rounded-full" style={{ background: category.accent, opacity: 0.18 }} />
      <div className="flex items-center gap-3 px-5 flex-1 min-w-0 pl-7">
        <MaterialIcon name={category.iconGlyph} size={24} className="flex-shrink-0"
          style={{ color: isDark ? 'rgba(255,255,255,0.42)' : 'rgba(59,10,23,0.38)' }} />
        <div className="flex-1 min-w-0">
          <div className="text-[15px] font-semibold truncate leading-tight"
            style={{ color: isDark ? 'rgba(255,255,255,0.68)' : 'rgba(59,10,23,0.68)' }}>{topic.name}</div>
          <div className="text-[12px] truncate mt-0.5"
            style={{ color: isDark ? 'rgba(255,255,255,0.30)' : 'rgba(59,10,23,0.28)' }}>{topic.subtype}</div>
        </div>
      </div>
    </div>
  );
};

// ─── Orbit Ring ───────────────────────────────────────────────────────
const OrbitRing: React.FC<{ active: boolean; color: string }> = ({ active, color }) => {
  const [rotation, setRotation] = useState(0);
  const frameRef = useRef<number>(0);
  useEffect(() => {
    if (!active) { setRotation(0); return; }
    let last = performance.now();
    const tick = (now: number) => { setRotation(r => (r + (now - last) * 0.15) % 360); last = now; frameRef.current = requestAnimationFrame(tick); };
    frameRef.current = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(frameRef.current);
  }, [active]);
  return (
    <div className="absolute inset-0 pointer-events-none" style={{ transform: `rotate(${rotation}deg)` }}>
      {Array.from({ length: ORBIT_DOTS }).map((_, i) => {
        const a = (i / ORBIT_DOTS) * Math.PI * 2;
        return <div key={i} className="absolute rounded-full"
          style={{ left: `${50 + Math.cos(a) * 47}%`, top: `${50 + Math.sin(a) * 47}%`,
            width: active ? 4 : 2.5, height: active ? 4 : 2.5, transform: 'translate(-50%,-50%)',
            background: color, opacity: active ? 0.6 : 0.08,
            animation: active ? `curio-orbit-pulse 1.3s ease-in-out ${i * 0.09}s infinite` : 'none' }} />;
      })}
    </div>
  );
};

// ─── Spin Button (solid opaque fill, orbit ring wraps around it) ──────
const SpinButton: React.FC<{ isSpinning: boolean; hasLanded: boolean; onClick: () => void; color: string }> = ({ isSpinning, hasLanded, onClick, color }) => {
  const { handlers, pressStyle } = usePressable(0.88);
  const btnSize = isSpinning ? 90 : 108;
  const diceSize = isSpinning ? 48 : 56;
  const [r, g, b] = hexToRgb(color);
  const darkR = Math.round(r * 0.55);
  const darkG = Math.round(g * 0.55);
  const darkB = Math.round(b * 0.55);
  return (
    <div className="relative flex items-center justify-center" style={{ width: btnSize + 28, height: btnSize + 28 }}>
      {/* Orbit ring — wraps the spin button, matching Android */}
      <div className="absolute inset-0 flex items-center justify-center pointer-events-none" style={{ zIndex: 0 }}>
        <OrbitRing active={isSpinning} color={color} />
      </div>
      {/* Glow pulse when landed */}
      {hasLanded && !isSpinning && (
        <div className="absolute inset-0 rounded-full pointer-events-none"
          style={{ zIndex: 0, boxShadow: `0 0 22px ${color}44, 0 0 48px ${color}20`, animation: 'curio-breathe 1.8s ease-in-out infinite' }} />
      )}
      {/* The button itself */}
      <button onClick={onClick} disabled={isSpinning} {...handlers}
        className="relative flex items-center justify-center rounded-full touch-action-manipulation"
        style={{ width: btnSize, height: btnSize, zIndex: 1,
          background: `radial-gradient(circle at 40% 32%, rgb(${Math.round(r*1.15)},${Math.round(g*1.15)},${Math.round(b*1.15)}) 0%, ${color} 50%, rgb(${darkR},${darkG},${darkB}) 100%)`,
          boxShadow: isSpinning ? `0 4px 16px ${color}40, inset 0 0 0 1px rgba(255,255,255,0.08)`
            : `0 8px 32px ${color}50, 0 2px 8px rgba(0,0,0,0.10), inset 0 0 0 1px rgba(255,255,255,0.12)`,
          cursor: isSpinning ? 'default' : 'pointer', ...pressStyle }}>
        <CssDice size={diceSize} tumbling={isSpinning} ink="white" />
      </button>
    </div>
  );
};

// ─── Bottom Control Pill ──────────────────────────────────────────────
const BottomPill: React.FC<{ icon: string; label: string; isActive: boolean; accent: string; onClick: () => void }> = ({ icon, label, isActive, accent, onClick }) => {
  const { isDark } = useTheme();
  const { handlers, pressStyle } = usePressable(0.96);
  return (
    <button onClick={onClick} {...handlers}
      className="flex-1 flex items-center justify-center gap-2.5 py-3 px-4 rounded-2xl font-semibold text-sm touch-action-manipulation"
      style={{ background: isActive ? accent : isDark ? 'rgba(255,255,255,0.06)' : 'rgba(0,0,0,0.04)',
        color: isActive ? 'white' : isDark ? 'rgba(255,255,255,0.7)' : 'rgba(59,10,23,0.7)', ...pressStyle }}>
      <MaterialIcon name={icon} size={22} />{label}
    </button>
  );
};

// ─── Filter Sheet ─────────────────────────────────────────────────────
const FilterSheet: React.FC<{
  isOpen: boolean; onClose: () => void; category: CurioCategory;
  subtypes: string[]; sel: string | null; setSel: (s: string | null) => void;
}> = ({ isOpen, onClose, category, subtypes, sel, setSel }) => {
  const { isDark } = useTheme();
  if (!isOpen) return null;
  return (
    <div className="fixed inset-0 bg-black/40 z-50 flex items-end justify-center" onClick={onClose}>
      <div className="w-full max-w-lg rounded-t-3xl p-5 max-h-[60vh] overflow-y-auto"
        style={{ background: isDark ? '#1a1a2e' : 'white' }} onClick={e => e.stopPropagation()}>
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-lg font-bold" style={{ color: getTextColor(isDark), fontFamily: 'Geom, Inter, sans-serif' }}>Filter</h3>
          <button onClick={onClose} className="text-2xl" style={{ color: isDark ? 'rgba(255,255,255,0.5)' : 'rgba(0,0,0,0.4)' }}>×</button>
        </div>
        <div className="flex flex-wrap gap-2">
          <button onClick={() => { setSel(null); onClose(); }} className="px-3.5 py-2 rounded-full text-sm font-medium"
            style={{ background: !sel ? category.accent : isDark ? 'rgba(255,255,255,0.06)' : 'rgba(0,0,0,0.03)',
              color: !sel ? 'white' : isDark ? 'rgba(255,255,255,0.6)' : 'rgba(0,0,0,0.6)' }}>All</button>
          {subtypes.map(st => (
            <button key={st} onClick={() => { setSel(st === sel ? null : st); onClose(); }} className="px-3.5 py-2 rounded-full text-sm font-medium"
              style={{ background: sel === st ? category.accent : isDark ? 'rgba(255,255,255,0.06)' : 'rgba(0,0,0,0.03)',
                color: sel === st ? 'white' : isDark ? 'rgba(255,255,255,0.6)' : 'rgba(0,0,0,0.6)' }}>{st}</button>
          ))}
        </div>
      </div>
    </div>
  );
};

// ─── Category Picker Sheet ────────────────────────────────────────────
const CatPickerSheet: React.FC<{ isOpen: boolean; onClose: () => void; activeId: string; onSelect: (c: CurioCategory) => void }> = ({ isOpen, onClose, activeId, onSelect }) => {
  const { isDark } = useTheme();
  if (!isOpen) return null;
  return (
    <div className="fixed inset-0 bg-black/40 z-50 flex items-end justify-center" onClick={onClose}>
      <div className="w-full max-w-lg rounded-t-3xl p-5 max-h-[65vh] overflow-y-auto"
        style={{ background: isDark ? '#1a1a2e' : 'white' }} onClick={e => e.stopPropagation()}>
        <h3 className="text-lg font-bold mb-4" style={{ color: getTextColor(isDark), fontFamily: 'Geom, Inter, sans-serif' }}>What are we exploring?</h3>
        <div className="grid grid-cols-2 gap-2">
          {ALL_CATEGORIES.filter(c => c.isReady).map(cat => (
            <button key={cat.id} onClick={() => { onSelect(cat); onClose(); }}
              className="flex items-center gap-2.5 p-3 rounded-2xl text-left"
              style={{ background: cat.id === activeId ? `${cat.accent}18` : isDark ? 'rgba(255,255,255,0.04)' : 'rgba(0,0,0,0.02)',
                border: cat.id === activeId ? `1px solid ${cat.accent}40` : `1px solid ${isDark ? 'rgba(255,255,255,0.06)' : 'rgba(0,0,0,0.04)'}` }}>
              <MaterialIcon name={cat.iconGlyph} size={28} style={{ color: cat.accent }} />
              <span className="text-sm font-semibold" style={{ color: getTextColor(isDark) }}>{cat.displayName}</span>
            </button>
          ))}
        </div>
      </div>
    </div>
  );
};

// ═══════════════════════════════════════════════════════════════════════════
export const SpinScreen: React.FC = () => {
  const navigate = useNavigate();
  const { categorySlug } = useParams();
  const { isDark, isAmoled } = useTheme();
  const [questSystem] = useState(() => getQuestSystem());

  const [activeCategory, setActiveCategory] = useState<CurioCategory>(ALL_CATEGORIES[0]);
  const [deckHand, setDeckHand] = useState<CurioTopic[]>([]);
  const [deckIndex, setDeckIndex] = useState(0);
  const [currentTopic, setCurrentTopic] = useState<CurioTopic | null>(null);
  const [spinPhase, setSpinPhase] = useState<'idle' | 'spinning' | 'landed' | 'opening'>('idle');
  const [confettiTrigger, setConfettiTrigger] = useState(0);

  const [showFilter, setShowFilter] = useState(false);
  const [showPicker, setShowPicker] = useState(false);
  const [selectedSubtype, setSelectedSubtype] = useState<string | null>(null);
  const [subtypes, setSubtypes] = useState<string[]>([]);

  useEffect(() => { if (categorySlug) { const c = getCategoryBySlug(categorySlug); if (c) setActiveCategory(c); } }, [categorySlug]);

  const buildHand = useCallback(async (catId: string) => {
    const topics: CurioTopic[] = [];
    for (let i = 0; i < 4; i++) { const t = await getRandomTopic(catId as any); if (t && !topics.find(x => x.id === t.id)) topics.push(t); }
    setSubtypes(Array.from(new Set(topics.map(t => t.subtype))).sort().slice(0, 20));
    return topics;
  }, []);

  useEffect(() => { setDeckIndex(0); setCurrentTopic(null); setSpinPhase('idle'); setSelectedSubtype(null); buildHand(activeCategory.id).then(setDeckHand); }, [activeCategory.id, buildHand]);

  useEffect(() => {
    if (spinPhase !== 'landed' || !currentTopic) return;
    const t = setTimeout(() => { setSpinPhase('opening'); setTimeout(() => navigate(`/reveal/${currentTopic.categoryId.toLowerCase()}/${currentTopic.id}`), 400); }, 1200);
    return () => clearTimeout(t);
  }, [spinPhase, currentTopic, navigate]);

  const handleSpin = useCallback(async () => {
    if (spinPhase === 'spinning' || spinPhase === 'opening') return;
    setSpinPhase('spinning'); setCurrentTopic(null);
    const dur = SPIN_MIN + Math.random() * (SPIN_MAX - SPIN_MIN);
    const start = performance.now();
    const reel = () => { const e = performance.now() - start; if (e >= dur) return; const p = e / dur; setTimeout(() => { setDeckIndex(i => (i + 1) % (deckHand.length || 1)); reel(); }, 150 + 320 * Math.sin(p * Math.PI / 2)); };
    reel();
    await new Promise(r => setTimeout(r, dur));
    const topic = await getRandomTopic(activeCategory.id as any);
    if (topic) { setDeckHand(p => [topic, ...p.filter(t => t.id !== topic.id)].slice(0, 4)); setDeckIndex(0); }
    setCurrentTopic(topic); setSpinPhase('landed'); setConfettiTrigger(c => c + 1);
    if (topic) questSystem.onSpin(activeCategory.id);
  }, [spinPhase, deckHand, activeCategory, questSystem, navigate]);

  const handleTopicOpen = () => {
    if (currentTopic && (spinPhase === 'landed' || spinPhase === 'idle')) { setSpinPhase('opening'); setTimeout(() => navigate(`/reveal/${currentTopic.categoryId.toLowerCase()}/${currentTopic.id}`), 400); }
  };

  const getFanTopic = (slot: number): CurioTopic | null => {
    if (deckHand.length === 0) return null;
    return deckHand[((deckIndex + slot) % deckHand.length + deckHand.length) % deckHand.length] || null;
  };

  return (
    <div className="min-h-screen flex flex-col relative overflow-hidden" style={{ backgroundColor: getBackgroundColor(isDark, isAmoled) }}>
      <CurioWatermarkBackdrop activeCatId={activeCategory.id} />

      {/* ── 1. Fan Deck Section (Android: 444dp container) ──────────── */}
      <div className="flex-1 flex flex-col items-center justify-center px-4" style={{ paddingTop: 'env(safe-area-inset-top, 8px)', paddingBottom: '20px' }}>
        <div className="relative flex items-center justify-center" style={{ width: 380, height: 444 }}>
          {/* Top peek (slot -1) fanned above hero */}
          <PeekCard slot={-1} topic={getFanTopic(-1)} category={activeCategory} isSpinning={spinPhase === 'spinning'} />
          {/* Hero card (slot 0) at center, z-index above peeks */}
          <div className="relative" style={{ zIndex: 10 }}>
            <ConfettiBurst trigger={confettiTrigger} color={activeCategory.accent} />
            <HeroTicket topic={currentTopic || getFanTopic(0)} category={activeCategory}
              isSpinning={spinPhase === 'spinning'} spinPhase={spinPhase} onClick={handleTopicOpen} />
          </div>
          {/* Bottom peek (slot +1) fanned below hero */}
          <PeekCard slot={1} topic={getFanTopic(1)} category={activeCategory} isSpinning={spinPhase === 'spinning'} />
        </div>
        {/* ── 2. Spin Button ─────────────────────────────────────────── */}
        <div className="mb-4">
          <SpinButton isSpinning={spinPhase === 'spinning'} hasLanded={spinPhase === 'landed'} onClick={handleSpin} color={activeCategory.accent} />
        </div>
      </div>

      {/* ── 3. Bottom Bar: Categories · Filter ───────────────────────── */}
      <div className="w-full px-4" style={{ paddingBottom: 'calc(env(safe-area-inset-bottom, 0px) + 64px)' }}>
        <div className="flex gap-2.5">
          <BottomPill icon={activeCategory.iconGlyph} label={activeCategory.displayName} isActive={true} accent={activeCategory.accent} onClick={() => setShowPicker(true)} />
          <BottomPill icon="tune" label={selectedSubtype ? `Filter · ${selectedSubtype}` : 'Filter'} isActive={!!selectedSubtype} accent={activeCategory.accent} onClick={() => setShowFilter(true)} />
        </div>
      </div>

      <FilterSheet isOpen={showFilter} onClose={() => setShowFilter(false)} category={activeCategory} subtypes={subtypes} sel={selectedSubtype} setSel={setSelectedSubtype} />
      <CatPickerSheet isOpen={showPicker} onClose={() => setShowPicker(false)} activeId={activeCategory.id} onSelect={setActiveCategory} />

      <style>{`
        @keyframes curio-dice-tumble { 0%{transform:rotateX(0deg) rotateY(0deg) rotateZ(0deg)} 25%{transform:rotateX(90deg) rotateY(45deg) rotateZ(180deg)} 50%{transform:rotateX(180deg) rotateY(90deg) rotateZ(360deg)} 75%{transform:rotateX(270deg) rotateY(135deg) rotateZ(540deg)} 100%{transform:rotateX(360deg) rotateY(180deg) rotateZ(720deg)} }
        @keyframes curio-dice-breathe { 0%,100%{transform:scale(1)} 50%{transform:scale(1.04)} }
        @keyframes curio-orbit-pulse { 0%,100%{transform:translate(-50%,-50%) scale(1);opacity:0.4} 50%{transform:translate(-50%,-50%) scale(1.6);opacity:0.95} }
        @keyframes curio-confetti-fly { 0%{opacity:1;transform:translate(0,0) rotate(0deg)} 100%{opacity:0;transform:translate(var(--tx),var(--ty)) rotate(var(--rot))} }
        @keyframes reelFade { 0%{opacity:0.5;transform:translateY(4px)} 100%{opacity:1;transform:translateY(0)} }
        .animate-reelFade { animation: reelFade 0.35s ease-out; }
      `}</style>
    </div>
  );
};

export default SpinScreen;
