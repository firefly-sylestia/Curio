// Curio Web App - Spin Screen
// Fan-deck carousel — exact match with Android app design
// Paper ticket hero card with category→surface gradient, CSS dice, confetti burst

import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useTheme, getBackgroundColor, getTextColor } from '../theme/ThemeContext';
import { ALL_CATEGORIES, getCategoryBySlug } from '../data/categories';
import { getRandomTopic } from '../data/topics';
import { getQuestSystem } from '../data/QuestSystem';
import type { CurioCategory, CurioTopic } from '../types';
import { MaterialIcon } from '../components/SharedComponents';

const ORBIT_DOTS = 12;
const SPIN_MIN = 2400;
const SPIN_MAX = 3400;

const hexToRgb = (hex: string): [number, number, number] => {
  const r = parseInt(hex.slice(1, 3), 16);
  const g = parseInt(hex.slice(3, 5), 16);
  const b = parseInt(hex.slice(5, 7), 16);
  return [r, g, b];
};

// ─── CSS Dice with pips ───────────────────────────────────────────────
const CssDice: React.FC<{ size: number; tumbling: boolean; ink: string }> = ({ size, tumbling, ink }) => {
  const pipSize = size * 0.16;
  const gap = size * 0.22;
  const mid = size * 0.39;

  const pip = (x: number, y: number) => (
    <div
      key={`${x}-${y}`}
      className="absolute rounded-full"
      style={{
        width: pipSize, height: pipSize,
        left: x - pipSize / 2, top: y - pipSize / 2,
        background: tumbling ? `rgba(255,255,255,0.9)` : ink,
        transition: 'background 0.3s',
      }}
    />
  );

  // 5-dot face pattern
  const pips = [
    pip(gap, gap),
    pip(size - gap, gap),
    pip(mid, mid),
    pip(gap, size - gap),
    pip(size - gap, size - gap),
  ];

  return (
    <div
      className="relative rounded-[18%]"
      style={{
        width: size, height: size,
        background: tumbling ? 'rgba(255,255,255,0.15)' : `${ink}10`,
        border: tumbling ? `2px solid rgba(255,255,255,0.25)` : `2px solid ${ink}30`,
        animation: tumbling ? 'diceTumble 1.6s linear infinite' : 'diceBreathe 3s ease-in-out infinite',
        transform: tumbling ? undefined : 'scale(1)',
      }}
    >
      {pips}
    </div>
  );
};

// ─── Confetti Burst ───────────────────────────────────────────────────
const ConfettiBurst: React.FC<{ trigger: number; color: string }> = ({ trigger, color }) => {
  const [particles, setParticles] = useState<Array<{ id: number; x: number; y: number; rot: number; size: number; delay: number }>>([]);

  useEffect(() => {
    if (trigger === 0) return;
    const newParticles = Array.from({ length: 24 }, (_, i) => ({
      id: i,
      x: (Math.random() - 0.5) * 300,
      y: (Math.random() - 0.5) * 300 - 60,
      rot: Math.random() * 720 - 360,
      size: 4 + Math.random() * 8,
      delay: Math.random() * 0.15,
    }));
    setParticles(newParticles);
    const timer = setTimeout(() => setParticles([]), 1200);
    return () => clearTimeout(timer);
  }, [trigger]);

  if (particles.length === 0) return null;

  return (
    <div className="absolute inset-0 pointer-events-none z-30 flex items-center justify-center">
      {particles.map(p => (
        <div
          key={p.id}
          className="absolute rounded-sm"
          style={{
            width: p.size, height: p.size * 1.6,
            background: p.id % 3 === 0 ? '#FFD700' : p.id % 3 === 1 ? color : '#FFFFFF',
            left: '50%', top: '50%',
            animation: `confettiFly 0.8s ease-out ${p.delay}s forwards`,
            opacity: 0,
            '--tx': `${p.x}px`,
            '--ty': `${p.y}px`,
            '--rot': `${p.rot}deg`,
          } as React.CSSProperties}
        />
      ))}
    </div>
  );
};

// ─── Hero Ticket Card ─────────────────────────────────────────────────
const HeroTicket: React.FC<{
  topic: CurioTopic | null;
  category: CurioCategory;
  isSpinning: boolean;
  spinPhase: 'idle' | 'spinning' | 'landed';
  onClick: () => void;
}> = ({ topic, category, isSpinning, spinPhase, onClick }) => {
  const { isDark } = useTheme();
  const [shimmerPos, setShimmerPos] = useState(-100);
  const [contentKey, setContentKey] = useState(0);

  useEffect(() => {
    if (spinPhase !== 'spinning') {
      const interval = setInterval(() => setShimmerPos(prev => (prev >= 200 ? -100 : prev + 1.2)), 35);
      return () => clearInterval(interval);
    }
  }, [spinPhase]);

  // Rapid content cycling during spin
  useEffect(() => {
    if (spinPhase !== 'spinning') return;
    const interval = setInterval(() => setContentKey(k => k + 1), 280);
    return () => clearInterval(interval);
  }, [spinPhase]);

  const [r, g, b] = hexToRgb(category.accent);
  const surfaceRgb = isDark ? [26, 26, 46] : [247, 240, 228];
  // Lighter gradient that truly blends into the page background like Android
  const top = `rgba(${r},${g},${b},0.92)`;
  const mid = `rgba(${Math.round(r * 0.42 + surfaceRgb[0] * 0.58)},${Math.round(g * 0.42 + surfaceRgb[1] * 0.58)},${Math.round(b * 0.42 + surfaceRgb[2] * 0.58)},0.65)`;
  const bottom = `rgba(${Math.round(r * 0.10 + surfaceRgb[0] * 0.90)},${Math.round(g * 0.10 + surfaceRgb[1] * 0.90)},${Math.round(b * 0.10 + surfaceRgb[2] * 0.90)},0.92)`;

  return (
    <button
      onClick={onClick}
      disabled={isSpinning}
      className="relative w-[286px] h-[310px] rounded-[32px] overflow-hidden text-left flex-shrink-0"
      style={{
        background: `linear-gradient(180deg, ${top} 0%, ${mid} 50%, ${bottom} 100%)`,
        boxShadow: isSpinning
          ? '0 4px 16px rgba(0,0,0,0.06)'
          : `0 8px 32px rgba(0,0,0,0.10), 0 2px 8px rgba(0,0,0,0.04)`,
        cursor: isSpinning ? 'default' : 'pointer',
        border: isDark ? `1px solid ${category.accent}30` : `1px solid ${category.accent}1A`,
        transform: isSpinning ? 'scale(0.98)' : 'scale(1)',
        transition: 'transform 0.5s cubic-bezier(0.2,0.8,0.3,1), box-shadow 0.5s ease',
      }}
    >
      {/* Category accent rule */}
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
        <MaterialIcon name={category.iconGlyph} size={140} />
      </div>

      {/* Content */}
      <div className="relative z-10 flex flex-col items-center justify-center h-full p-6 text-center">
        {spinPhase === 'spinning' ? (
          <div className="flex flex-col items-center gap-4 animate-reelFade" key={contentKey}>
            <div className="flex items-center gap-1 px-3 py-0.5 rounded-full text-[11px] font-semibold tracking-wide uppercase"
              style={{ background: 'rgba(255,255,255,0.15)', color: 'white', backdropFilter: 'blur(4px)' }}>
              <MaterialIcon name={category.iconGlyph} size={11} />
              {topic?.subtype || 'Topic'}
            </div>
            <div className="text-[20px] font-extrabold text-white leading-tight px-2"
              style={{ fontFamily: 'Geom, Inter, sans-serif', textShadow: '0 1px 4px rgba(0,0,0,0.1)' }}>
              {topic?.name || '...'}
            </div>
            <div className="w-16 h-1 rounded-full bg-white/20" />
            <p className="text-white/50 text-[11px] tracking-widest uppercase">Spinning</p>
          </div>
        ) : topic ? (
          <>
            <div className="flex items-center gap-1 px-3 py-0.5 rounded-full text-[11px] font-semibold tracking-wide uppercase mb-5"
              style={{ background: 'rgba(255,255,255,0.15)', color: 'white', backdropFilter: 'blur(4px)' }}>
              <MaterialIcon name={category.iconGlyph} size={11} />
              {topic.subtype}
            </div>
            <h2 className="text-[22px] font-extrabold text-white leading-tight mb-2 px-2"
              style={{ fontFamily: 'Geom, Inter, sans-serif', textShadow: '0 1px 6px rgba(0,0,0,0.12)' }}>
              {topic.name}
            </h2>
            <p className="text-[13px] text-white/75 leading-relaxed line-clamp-3 px-1">{topic.teaser}</p>
            <div className="mt-5 flex items-center gap-1.5 text-[11px] text-white/50 tracking-wide uppercase">
              <MaterialIcon name="touch_app" size={14} />
              Tap to open
            </div>
          </>
        ) : (
          <>
            <MaterialIcon name="casino" size={44} className="text-white/55 mb-4" />
            <h2 className="text-xl font-bold text-white mb-2" style={{ fontFamily: 'Geom, Inter, sans-serif' }}>
              {category.displayName}
            </h2>
            <p className="text-[13px] text-white/60">Tap the dice to spin</p>
          </>
        )}
      </div>
    </button>
  );
};

// ─── Peek Card ────────────────────────────────────────────────────────
const PeekCard: React.FC<{
  slot: number;
  topic: CurioTopic | null;
  category: CurioCategory;
  isSpinning: boolean;
}> = ({ slot, topic, category, isSpinning }) => {
  const { isDark } = useTheme();
  const isTop = slot < 0;
  const isFar = Math.abs(slot) === 2;
  const w = isFar ? 260 : 300;
  const h = isFar ? 78 : 94;
  const corner = isFar ? 14 : 18;
  const yOff = (() => { switch (slot) { case -2: return -164; case -1: return -126; case 1: return 136; default: return 174; } })();

  const [r, g, b] = hexToRgb(category.accent);
  const surfaceRgb = isDark ? [26, 26, 46] : [247, 240, 228];
  const depth = isFar ? 0.52 : 0.40;
  const dr = Math.round(r * (1 - depth) + surfaceRgb[0] * depth);
  const dg = Math.round(g * (1 - depth) + surfaceRgb[1] * depth);
  const db = Math.round(b * (1 - depth) + surfaceRgb[2] * depth);

  if (!topic) return null;

  return (
    <div
      className="absolute left-1/2 flex items-center"
      style={{
        width: w, height: h, borderRadius: corner,
        top: isTop ? yOff : undefined, bottom: !isTop ? yOff : undefined,
        transform: `translateX(-50%)`,
        background: `linear-gradient(180deg, rgba(${dr},${dg},${db},0.9), rgba(${Math.round(dr*0.75+surfaceRgb[0]*0.25)},${Math.round(dg*0.75+surfaceRgb[1]*0.25)},${Math.round(db*0.75+surfaceRgb[2]*0.25)},0.88))`,
        boxShadow: '0 2px 6px rgba(0,0,0,0.04)',
        border: isDark ? `1px solid ${category.accent}1F` : `1px solid ${category.accent}12`,
        opacity: isSpinning ? 0.35 : 0.65,
        transition: 'opacity 0.4s ease',
        zIndex: isTop ? 1 : 0,
      }}
    >
      <div className="absolute left-3 top-0 bottom-0 w-[2px] rounded-full" style={{ background: category.accent, opacity: 0.3 }} />
      <div className="flex items-center gap-3 px-4 flex-1 min-w-0 pl-6">
        <MaterialIcon name={category.iconGlyph} size={isFar ? 18 : 22} className="flex-shrink-0"
          style={{ color: isDark ? 'rgba(255,255,255,0.6)' : 'rgba(59,10,23,0.55)' }} />
        <div className="flex-1 min-w-0">
          <div className="text-[13px] font-semibold truncate"
            style={{ color: isDark ? 'rgba(255,255,255,0.8)' : 'rgba(59,10,23,0.8)' }}>{topic.name}</div>
          <div className="text-[11px] truncate"
            style={{ color: isDark ? 'rgba(255,255,255,0.4)' : 'rgba(59,10,23,0.4)' }}>{topic.subtype}</div>
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
    const tick = (now: number) => {
      const delta = now - last; last = now;
      setRotation(r => (r + delta * 0.12) % 360);
      frameRef.current = requestAnimationFrame(tick);
    };
    frameRef.current = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(frameRef.current);
  }, [active]);

  return (
    <div className="absolute inset-0 pointer-events-none" style={{ transform: `rotate(${rotation}deg)` }}>
      {Array.from({ length: ORBIT_DOTS }).map((_, i) => {
        const angle = (i / ORBIT_DOTS) * Math.PI * 2;
        const x = 50 + Math.cos(angle) * 47;
        const y = 50 + Math.sin(angle) * 47;
        return (
          <div key={i} className="absolute rounded-full"
            style={{
              left: `${x}%`, top: `${y}%`,
              width: active ? 4.5 : 2.5, height: active ? 4.5 : 2.5,
              transform: 'translate(-50%, -50%)',
              background: color,
              opacity: active ? 0.65 : 0.1,
              animation: active ? `orbitPulse 1.4s ease-in-out ${i * 0.1}s infinite` : 'none',
            }} />
        );
      })}
    </div>
  );
};

// ─── Spin Button with 3D radial gradient and CSS dice ─────────────────
const SpinButton: React.FC<{
  isSpinning: boolean;
  hasLanded: boolean;
  onClick: () => void;
  color: string;
}> = ({ isSpinning, hasLanded, onClick, color }) => {
  const [isPressed, setIsPressed] = useState(false);
  const btnSize = isSpinning ? 94 : 114;
  const diceSize = isSpinning ? 52 : 60;

  return (
    <button
      onClick={onClick}
      disabled={isSpinning}
      onMouseDown={() => setIsPressed(true)}
      onMouseUp={() => setIsPressed(false)}
      onMouseLeave={() => setIsPressed(false)}
      className="relative z-20 flex items-center justify-center rounded-full transition-all duration-300"
      style={{
        width: btnSize, height: btnSize,
        background: `radial-gradient(circle at 42% 33%, ${color}EE, ${color} 50%, ${color}88 100%)`,
        boxShadow: isSpinning
          ? `0 4px 12px ${color}33, inset 0 0 0 1px rgba(255,255,255,0.08)`
          : `0 8px 28px ${color}44, 0 2px 6px rgba(0,0,0,0.10), inset 0 0 0 1px rgba(255,255,255,0.1)`,
        transform: isPressed ? 'scale(0.88)' : 'scale(1)',
        cursor: isSpinning ? 'default' : 'pointer',
      }}
    >
      <CssDice size={diceSize} tumbling={isSpinning} ink="white" />
      {hasLanded && !isSpinning && (
        <div className="absolute inset-0 rounded-full pointer-events-none animate-pulse-slow"
          style={{ boxShadow: `0 0 24px ${color}33, 0 0 48px ${color}18` }} />
      )}
    </button>
  );
};

// ─── Category Pill ────────────────────────────────────────────────────
const CategoryPill: React.FC<{ category: CurioCategory; isSelected: boolean; onClick: () => void }> = ({ category, isSelected, onClick }) => {
  const { isDark } = useTheme();
  return (
    <button onClick={onClick}
      className="flex items-center gap-1.5 px-3.5 py-2 rounded-full text-sm font-medium whitespace-nowrap transition-all duration-200"
      style={{
        background: isSelected ? category.accent : isDark ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.03)',
        color: isSelected ? 'white' : isDark ? 'rgba(255,255,255,0.6)' : 'rgba(59,10,23,0.6)',
        boxShadow: isSelected ? `0 4px 14px ${category.accent}40` : 'none',
      }}>
      <MaterialIcon name={category.iconGlyph} size={18} />
      {category.displayName}
    </button>
  );
};

// ─── Main SpinScreen ──────────────────────────────────────────────────
export const SpinScreen: React.FC = () => {
  const navigate = useNavigate();
  const { categorySlug } = useParams();
  const { isDark, isAmoled } = useTheme();
  const [questSystem] = useState(() => getQuestSystem());

  const [selectedCategories, setSelectedCategories] = useState<CurioCategory[]>([]);
  const [deckHand, setDeckHand] = useState<CurioTopic[]>([]);
  const [deckIndex, setDeckIndex] = useState(0);
  const [currentTopic, setCurrentTopic] = useState<CurioTopic | null>(null);
  const [spinPhase, setSpinPhase] = useState<'idle' | 'spinning' | 'landed'>('idle');
  const [showPicker, setShowPicker] = useState(false);
  const [confettiTrigger, setConfettiTrigger] = useState(0);

  useEffect(() => {
    if (categorySlug) {
      const cat = getCategoryBySlug(categorySlug);
      if (cat) setSelectedCategories([cat]);
    } else if (selectedCategories.length === 0) {
      setSelectedCategories([ALL_CATEGORIES[0]]);
    }
  }, [categorySlug]);

  const activeCategory = selectedCategories[0] || ALL_CATEGORIES[0];

  const buildHand = useCallback(async (catId: string) => {
    const topics: CurioTopic[] = [];
    for (let i = 0; i < 6; i++) {
      const t = await getRandomTopic(catId as any);
      if (t && !topics.find(x => x.id === t.id)) topics.push(t);
    }
    return topics;
  }, []);

  useEffect(() => {
    setDeckIndex(0);
    setCurrentTopic(null);
    setSpinPhase('idle');
    buildHand(activeCategory.id).then(setDeckHand);
  }, [activeCategory.id, buildHand]);

  const toggleCategory = (category: CurioCategory) => {
    setSelectedCategories(prev => {
      const exists = prev.find(c => c.id === category.id);
      if (exists) return prev.filter(c => c.id !== category.id);
      if (prev.length >= 3) return [category];
      return [...prev, category];
    });
  };

  const handleSpin = useCallback(async () => {
    if (selectedCategories.length === 0 || spinPhase === 'spinning') return;
    setSpinPhase('spinning');
    setCurrentTopic(null);

    const catId = selectedCategories[Math.floor(Math.random() * selectedCategories.length)].id;
    const duration = SPIN_MIN + Math.random() * (SPIN_MAX - SPIN_MIN);
    const start = performance.now();

    // Deck reel animation — cycle through the hand with deceleration
    const reel = () => {
      const elapsed = performance.now() - start;
      if (elapsed >= duration) return;
      const progress = elapsed / duration;
      const eased = Math.sin(progress * Math.PI / 2);
      const interval = 180 + (340 * eased);
      setDeckIndex(prev => (prev + 1) % (deckHand.length || 1));
      setTimeout(reel, interval);
    };
    reel();

    await new Promise(r => setTimeout(r, duration));

    const topic = await getRandomTopic(catId as any);
    if (topic) {
      setDeckHand(prev => [topic, ...prev.filter(t => t.id !== topic.id)].slice(0, 6));
      setDeckIndex(0);
    }
    setCurrentTopic(topic);
    setSpinPhase('landed');
    setConfettiTrigger(c => c + 1);

    if (topic) questSystem.onSpin(catId);

    setTimeout(() => setSpinPhase('idle'), 2600);
  }, [selectedCategories, spinPhase, deckHand, questSystem]);

  const handleTopicOpen = () => {
    if (currentTopic && (spinPhase === 'landed' || spinPhase === 'idle')) {
      navigate(`/reveal/${currentTopic.categoryId.toLowerCase()}/${currentTopic.id}`);
    }
  };

  const getFanTopic = (slot: number): CurioTopic | null => {
    if (deckHand.length === 0) return null;
    const idx = ((deckIndex + slot) % deckHand.length + deckHand.length) % deckHand.length;
    return deckHand[idx] || null;
  };

  return (
    <div className="min-h-screen pb-24 relative overflow-hidden"
      style={{ backgroundColor: getBackgroundColor(isDark, isAmoled) }}>
      {/* Watermark backdrop */}
      <div className="absolute inset-0 pointer-events-none overflow-hidden">
        {ALL_CATEGORIES.slice(0, 8).map((cat, i) => (
          <div key={cat.id} className="absolute opacity-[0.025]"
            style={{ left: `${8+(i%4)*26}%`, top: `${8+Math.floor(i/4)*48}%`, transform: `rotate(${-15+i*10}deg)`, color: isDark ? 'white' : cat.accent }}>
            <MaterialIcon name={cat.iconGlyph} size={110} />
          </div>
        ))}
      </div>

      {/* Main stage */}
      <div className="relative z-10 flex flex-col items-center justify-center min-h-[calc(100vh-96px)] px-4 pt-2">
        <div className="relative flex flex-col items-center" style={{ height: 530 }}>
          {/* Top peeks */}
          <div className="relative w-[320px] h-[200px]">
            <PeekCard slot={-2} topic={getFanTopic(-2)} category={activeCategory} isSpinning={spinPhase==='spinning'} />
            <PeekCard slot={-1} topic={getFanTopic(-1)} category={activeCategory} isSpinning={spinPhase==='spinning'} />
          </div>

          {/* Hero + orbit + confetti */}
          <div className="relative w-[300px] h-[330px] -mt-2 flex items-center justify-center">
            <OrbitRing active={spinPhase === 'spinning'} color={activeCategory.accent} />
            <ConfettiBurst trigger={confettiTrigger} color={activeCategory.accent} />
            <HeroTicket
              topic={currentTopic || getFanTopic(0)}
              category={activeCategory}
              isSpinning={spinPhase === 'spinning'}
              spinPhase={spinPhase}
              onClick={handleTopicOpen}
            />
          </div>

          {/* Bottom peeks */}
          <div className="relative w-[320px] h-[180px] -mt-4">
            <PeekCard slot={1} topic={getFanTopic(1)} category={activeCategory} isSpinning={spinPhase==='spinning'} />
            <PeekCard slot={2} topic={getFanTopic(2)} category={activeCategory} isSpinning={spinPhase==='spinning'} />
          </div>
        </div>

        {/* Spin button */}
        <div className="-mt-4 mb-5">
          <SpinButton
            isSpinning={spinPhase === 'spinning'}
            hasLanded={spinPhase === 'landed'}
            onClick={handleSpin}
            color={activeCategory.accent}
          />
        </div>

        {/* Category pills */}
        <div className="w-full max-w-lg">
          <div className="flex gap-2 overflow-x-auto pb-2 justify-center flex-wrap px-2">
            {ALL_CATEGORIES.filter(c => c.isReady).slice(0, 8).map(cat => (
              <CategoryPill key={cat.id} category={cat}
                isSelected={selectedCategories.some(c => c.id === cat.id)}
                onClick={() => toggleCategory(cat)} />
            ))}
          </div>
          <button onClick={() => setShowPicker(true)} className="mt-2 w-full py-2.5 rounded-full text-sm font-medium transition-all"
            style={{ background: isDark ? 'rgba(255,255,255,0.03)' : 'rgba(0,0,0,0.015)', color: isDark ? 'rgba(255,255,255,0.35)' : 'rgba(59,10,23,0.35)' }}>
            All categories →
          </button>
        </div>
      </div>

      {/* Category picker modal */}
      {showPicker && (
        <div className="fixed inset-0 bg-black/50 z-[70] flex items-end justify-center" onClick={() => setShowPicker(false)}>
          <div className="w-full max-w-lg rounded-t-3xl p-6 max-h-[70vh] overflow-y-auto"
            style={{ background: isDark ? '#1a1a2e' : 'white' }} onClick={e => e.stopPropagation()}>
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-bold" style={{ color: getTextColor(isDark), fontFamily: 'Geom, Inter, sans-serif' }}>Choose Categories</h3>
              <button onClick={() => setShowPicker(false)} className="w-8 h-8 rounded-full flex items-center justify-center text-xl"
                style={{ color: isDark ? 'rgba(255,255,255,0.5)' : 'rgba(59,10,23,0.5)' }}>×</button>
            </div>
            <div className="grid grid-cols-3 gap-3">
              {ALL_CATEGORIES.filter(c => c.isReady).map(cat => {
                const sel = selectedCategories.some(c => c.id === cat.id);
                return (
                  <button key={cat.id} onClick={() => { toggleCategory(cat); setShowPicker(false); }}
                    className="flex flex-col items-center gap-1.5 p-3 rounded-2xl transition-all duration-200"
                    style={{ background: sel ? cat.accent : isDark ? `${cat.accent}22` : cat.tint, color: sel ? 'white' : isDark ? cat.lightAccent : cat.accent }}>
                    <MaterialIcon name={cat.iconGlyph} size={28} />
                    <span className="text-xs font-medium">{cat.displayName}</span>
                  </button>
                );
              })}
            </div>
          </div>
        </div>
      )}

      {/* Animations */}
      <style>{`
        @keyframes orbitPulse {
          0%, 100% { transform: translate(-50%, -50%) scale(1); opacity: 0.65; }
          50% { transform: translate(-50%, -50%) scale(1.5); opacity: 1; }
        }
        @keyframes diceTumble {
          0%   { transform: rotate(0deg) translateY(0); }
          20%  { transform: rotate(72deg) translateY(-3px); }
          40%  { transform: rotate(144deg) translateY(0); }
          60%  { transform: rotate(216deg) translateY(3px); }
          80%  { transform: rotate(288deg) translateY(0); }
          100% { transform: rotate(360deg) translateY(0); }
        }
        @keyframes diceBreathe {
          0%, 100% { transform: scale(1); }
          50% { transform: scale(1.04); }
        }
        @keyframes confettiFly {
          0%   { opacity: 1; transform: translate(0, 0) rotate(0deg); }
          100% { opacity: 0; transform: translate(var(--tx), var(--ty)) rotate(var(--rot)); }
        }
        @keyframes reelFade {
          0%   { opacity: 0.6; transform: translateY(4px); }
          100% { opacity: 1; transform: translateY(0); }
        }
        .animate-reelFade { animation: reelFade 0.4s ease-out; }
        .animate-pulse-slow { animation: pulse-slow 2s ease-in-out infinite; }
        @keyframes pulse-slow { 0%, 100% { opacity: 1; } 50% { opacity: 0.55; } }
      `}</style>
    </div>
  );
};

export default SpinScreen;
