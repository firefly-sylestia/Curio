// Curio Web App - Spin Screen
// Fan-deck carousel — exact match with Android app design
// Paper ticket hero card with category→surface gradient, peek cards, casino dice button

import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useTheme, getBackgroundColor, getTextColor } from '../theme/ThemeContext';
import { ALL_CATEGORIES, getCategoryBySlug } from '../data/categories';
import { getRandomTopic } from '../data/topics';
import { getQuestSystem } from '../data/QuestSystem';
import type { CurioCategory, CurioTopic } from '../types';
import { MaterialIcon } from '../components/SharedComponents';

const ORBIT_DOTS = 12;
const SPIN_MIN = 2200;
const SPIN_MAX = 3200;

// ─── Helper: hex to RGB for gradient building ─────────────────────────
const hexToRgb = (hex: string): [number, number, number] => {
  const r = parseInt(hex.slice(1, 3), 16);
  const g = parseInt(hex.slice(3, 5), 16);
  const b = parseInt(hex.slice(5, 7), 16);
  return [r, g, b];
};

// ─── Hero Ticket Card — Android paper-ticket design ───────────────────
const HeroTicket: React.FC<{
  topic: CurioTopic | null;
  category: CurioCategory;
  isSpinning: boolean;
  spinPhase: 'idle' | 'spinning' | 'landed';
  onClick: () => void;
}> = ({ topic, category, isSpinning, spinPhase, onClick }) => {
  const { isDark } = useTheme();
  const [shimmerPos, setShimmerPos] = useState(-100);

  // Subtle shimmer when idle
  useEffect(() => {
    if (spinPhase !== 'spinning') {
      const interval = setInterval(() => {
        setShimmerPos(prev => (prev >= 200 ? -100 : prev + 1.2));
      }, 35);
      return () => clearInterval(interval);
    }
  }, [spinPhase]);

  // Category → surface gradient (matches Android's categoryCardFill → themeSurface)
  const getCardGradient = () => {
    const [r, g, b] = hexToRgb(category.accent);
    const surfaceRgb = isDark ? [26, 26, 46] : [247, 240, 228]; // #1a1a2e / #F7F0E4 (Cream)

    // Top: category accent with slight darken for depth
    const top = `rgba(${r},${g},${b},0.95)`;
    // Mid: accent blending toward surface
    const mid = `rgba(${Math.round(r * 0.6 + surfaceRgb[0] * 0.4)},${Math.round(g * 0.6 + surfaceRgb[1] * 0.4)},${Math.round(b * 0.6 + surfaceRgb[2] * 0.4)},0.7)`;
    // Bottom: near-surface
    const bottom = `rgba(${Math.round(r * 0.25 + surfaceRgb[0] * 0.75)},${Math.round(g * 0.25 + surfaceRgb[1] * 0.75)},${Math.round(b * 0.25 + surfaceRgb[2] * 0.75)},0.85)`;

    return `linear-gradient(180deg, ${top} 0%, ${mid} 55%, ${bottom} 100%)`;
  };

  const cardBg = getCardGradient();

  return (
    <button
      onClick={onClick}
      disabled={isSpinning}
      className="relative w-[286px] h-[310px] rounded-[28px] overflow-hidden text-left transition-transform duration-500 flex-shrink-0"
      style={{
        background: cardBg,
        boxShadow: isSpinning
          ? '0 4px 16px rgba(0,0,0,0.08)'
          : `0 8px 32px rgba(0,0,0,0.12), 0 2px 8px rgba(0,0,0,0.06)`,
        cursor: isSpinning ? 'default' : 'pointer',
        border: isDark
          ? `1px solid ${category.accent}33`
          : `1px solid ${category.accent}22`,
      }}
    >
      {/* Category accent rule at top */}
      <div
        className="absolute top-0 left-3 right-3 h-[2px] rounded-full"
        style={{ background: category.accent, opacity: 0.5 }}
      />

      {/* Subtle shimmer on idle */}
      {spinPhase !== 'spinning' && (
        <div
          className="absolute inset-0 pointer-events-none"
          style={{
            background: `linear-gradient(105deg, transparent 35%, rgba(255,255,255,0.06) 48%, transparent 61%)`,
            transform: `translateX(${shimmerPos}%)`,
          }}
        />
      )}

      {/* Top-lit crown — a faint bright band across top edge */}
      <div
        className="absolute top-0 left-0 right-0 h-[60px] pointer-events-none"
        style={{
          background: `linear-gradient(180deg, rgba(255,255,255,0.08) 0%, transparent 100%)`,
        }}
      />

      {/* Watermark glyph — large, faded, in the background */}
      <div
        className="absolute right-2 bottom-2 pointer-events-none select-none"
        style={{
          color: isDark ? 'rgba(255,255,255,0.06)' : 'rgba(0,0,0,0.05)',
        }}
      >
        <MaterialIcon name={category.iconGlyph} size={140} />
      </div>

      {/* Content layer */}
      <div className="relative z-10 flex flex-col items-center justify-center h-full p-6 text-center">
        {spinPhase === 'spinning' ? (
          <div className="flex flex-col items-center gap-4">
            <MaterialIcon name="casino" size={40} className="text-white/50 animate-spin" />
            <p className="text-white/60 text-sm font-medium tracking-wide">SPINNING</p>
          </div>
        ) : topic ? (
          <>
            {/* Subtype badge */}
            <div
              className="flex items-center gap-1 px-3 py-0.5 rounded-full text-[11px] font-semibold tracking-wide uppercase mb-5"
              style={{
                background: 'rgba(255,255,255,0.15)',
                color: 'white',
                backdropFilter: 'blur(4px)',
              }}
            >
              <MaterialIcon name={category.iconGlyph} size={12} />
              {topic.subtype}
            </div>

            {/* Topic name */}
            <h2
              className="text-[22px] font-extrabold text-white leading-tight mb-2 px-2"
              style={{ fontFamily: 'Geom, Inter, sans-serif', textShadow: '0 1px 6px rgba(0,0,0,0.12)' }}
            >
              {topic.name}
            </h2>

            {/* Teaser */}
            <p className="text-[13px] text-white/75 leading-relaxed line-clamp-3 px-1">
              {topic.teaser}
            </p>

            {/* Tap hint */}
            <div className="mt-5 flex items-center gap-1.5 text-[11px] text-white/50 tracking-wide uppercase">
              <MaterialIcon name="touch_app" size={14} />
              Tap to open
            </div>
          </>
        ) : (
          <>
            <MaterialIcon name="casino" size={44} className="text-white/60 mb-4" />
            <h2
              className="text-xl font-bold text-white mb-2"
              style={{ fontFamily: 'Geom, Inter, sans-serif' }}
            >
              {category.displayName}
            </h2>
            <p className="text-[13px] text-white/65">Tap the dice to spin</p>
          </>
        )}
      </div>
    </button>
  );
};

// ─── Peek Card — slim card fanned above/below the hero ────────────────
const PeekCard: React.FC<{
  slot: number; // -2, -1, 1, or 2
  topic: CurioTopic | null;
  category: CurioCategory;
  isSpinning: boolean;
}> = ({ slot, topic, category, isSpinning }) => {
  const { isDark } = useTheme();
  const isTop = slot < 0;
  const isFar = Math.abs(slot) === 2;

  // Card sizing (matches Android: near 360×116, far 328×96 scaled down for web)
  const w = isFar ? 260 : 300;
  const h = isFar ? 78 : 94;
  const corner = isFar ? 14 : 18;

  // Y offsets for fan spread (matches Android fan layout)
  const yOff = (() => {
    switch (slot) {
      case -2: return -164;
      case -1: return -126;
      case 1: return 136;
      default: return 174; // +2
    }
  })();

  // Darker category → surface gradient for peek cards (the hero is brightest)
  const [r, g, b] = hexToRgb(category.accent);
  const surfaceRgb = isDark ? [26, 26, 46] : [247, 240, 228];
  const depth = isFar ? 0.52 : 0.40;
  const dr = Math.round(r * (1 - depth) + surfaceRgb[0] * depth);
  const dg = Math.round(g * (1 - depth) + surfaceRgb[1] * depth);
  const db = Math.round(b * (1 - depth) + surfaceRgb[2] * depth);

  const peekBg = `linear-gradient(180deg,
    rgba(${dr},${dg},${db},0.9) 0%,
    rgba(${Math.round(dr * 0.75 + surfaceRgb[0] * 0.25)},${Math.round(dg * 0.75 + surfaceRgb[1] * 0.25)},${Math.round(db * 0.75 + surfaceRgb[2] * 0.25)},0.88) 100%)`;

  if (!topic) return null;

  return (
    <div
      className="absolute left-1/2 transition-all duration-300 flex items-center"
      style={{
        width: w,
        height: h,
        borderRadius: corner,
        top: isTop ? yOff : undefined,
        bottom: !isTop ? yOff : undefined,
        transform: `translateX(-50%)`,
        background: peekBg,
        boxShadow: '0 2px 8px rgba(0,0,0,0.06)',
        border: isDark
          ? `1px solid ${category.accent}22`
          : `1px solid ${category.accent}15`,
        opacity: isSpinning ? 0.4 : 0.7,
        zIndex: isTop ? 1 : 0,
      }}
    >
      {/* Category accent line */}
      <div
        className="absolute left-3 top-0 bottom-0 w-[2px] rounded-full"
        style={{ background: category.accent, opacity: 0.35 }}
      />

      {/* Content */}
      <div className="flex items-center gap-3 px-4 flex-1 min-w-0 pl-6">
        <MaterialIcon
          name={category.iconGlyph}
          size={isFar ? 18 : 22}
          className="flex-shrink-0"
          style={{ color: isDark ? 'rgba(255,255,255,0.7)' : 'rgba(59,10,23,0.6)' }}
        />
        <div className="flex-1 min-w-0">
          <div
            className="text-[13px] font-semibold truncate"
            style={{ color: isDark ? 'rgba(255,255,255,0.85)' : 'rgba(59,10,23,0.85)' }}
          >
            {topic.name}
          </div>
          <div
            className="text-[11px] truncate"
            style={{ color: isDark ? 'rgba(255,255,255,0.45)' : 'rgba(59,10,23,0.45)' }}
          >
            {topic.subtype}
          </div>
        </div>
      </div>
    </div>
  );
};

// ─── Orbit Ring — orbiting dots around the spin button ────────────────
const OrbitRing: React.FC<{ active: boolean; color: string }> = ({ active, color }) => {
  const [rotation, setRotation] = useState(0);
  const frameRef = useRef<number>(0);

  useEffect(() => {
    if (!active) { setRotation(0); return; }
    let last = performance.now();
    const tick = (now: number) => {
      const delta = now - last;
      last = now;
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
        const x = 50 + Math.cos(angle) * 48;
        const y = 50 + Math.sin(angle) * 48;
        return (
          <div
            key={i}
            className="absolute rounded-full transition-all duration-500"
            style={{
              left: `${x}%`, top: `${y}%`,
              width: active ? 5 : 2.5, height: active ? 5 : 2.5,
              transform: 'translate(-50%, -50%)',
              background: color,
              opacity: active ? 0.7 : 0.12,
              animation: active ? `orbitPulse 1.5s ease-in-out ${i * 0.1}s infinite` : 'none',
            }}
          />
        );
      })}
    </div>
  );
};

// ─── Spin Button — casino dice button matching Android ────────────────
const SpinButton: React.FC<{
  isSpinning: boolean;
  hasLanded: boolean;
  onClick: () => void;
  color: string;
}> = ({ isSpinning, hasLanded, onClick, color }) => {
  const [isPressed, setIsPressed] = useState(false);
  const btnSize = isSpinning ? 92 : 112;

  return (
    <button
      onClick={onClick}
      disabled={isSpinning}
      onMouseDown={() => setIsPressed(true)}
      onMouseUp={() => setIsPressed(false)}
      onMouseLeave={() => setIsPressed(false)}
      className="relative z-20 flex items-center justify-center rounded-full transition-all duration-300"
      style={{
        width: btnSize,
        height: btnSize,
        background: `linear-gradient(135deg, ${color} 0%, ${color}DD 100%)`,
        boxShadow: isSpinning
          ? `0 4px 12px ${color}33`
          : `0 8px 28px ${color}44, 0 2px 6px rgba(0,0,0,0.12)`,
        transform: isPressed ? 'scale(0.9)' : 'scale(1)',
        cursor: isSpinning ? 'default' : 'pointer',
        border: `2px solid ${color}55`,
      }}
    >
      {/* Edge shine */}
      <div
        className="absolute inset-1 rounded-full pointer-events-none"
        style={{
          border: `1px solid rgba(255,255,255,0.15)`,
        }}
      />

      {/* Dice glyph */}
      <MaterialIcon
        name={isSpinning ? 'casino' : 'casino'}
        size={isSpinning ? 36 : 44}
        className="text-white"
        filled
        style={{
          animation: isSpinning ? 'diceTumble 1.6s linear infinite' : (hasLanded ? 'diceSettle 0.4s ease-out' : 'none'),
        }}
      />

      {/* Landing glow */}
      {hasLanded && !isSpinning && (
        <div
          className="absolute inset-0 rounded-full pointer-events-none animate-pulse-slow"
          style={{
            boxShadow: `0 0 20px ${color}33, 0 0 40px ${color}22`,
          }}
        />
      )}
    </button>
  );
};

// ─── Category Pill ────────────────────────────────────────────────────
const CategoryPill: React.FC<{
  category: CurioCategory;
  isSelected: boolean;
  onClick: () => void;
}> = ({ category, isSelected, onClick }) => {
  const { isDark } = useTheme();
  return (
    <button
      onClick={onClick}
      className="flex items-center gap-1.5 px-3.5 py-2 rounded-full text-sm font-medium whitespace-nowrap transition-all duration-200"
      style={{
        background: isSelected ? category.accent : isDark ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.03)',
        color: isSelected ? 'white' : isDark ? 'rgba(255,255,255,0.65)' : 'rgba(59,10,23,0.65)',
        boxShadow: isSelected ? `0 4px 14px ${category.accent}44` : 'none',
      }}
    >
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

  // Init from URL or default
  useEffect(() => {
    if (categorySlug) {
      const cat = getCategoryBySlug(categorySlug);
      if (cat) setSelectedCategories([cat]);
    } else if (selectedCategories.length === 0) {
      setSelectedCategories([ALL_CATEGORIES[0]]);
    }
  }, [categorySlug]);

  const activeCategory = selectedCategories[0] || ALL_CATEGORIES[0];

  // Build a deck hand for the fan (up to 6 topics)
  const buildHand = useCallback(async (catId: string) => {
    const topics: CurioTopic[] = [];
    for (let i = 0; i < 6; i++) {
      const t = await getRandomTopic(catId as any);
      if (t && !topics.find(x => x.id === t.id)) topics.push(t);
    }
    return topics;
  }, []);

  // On category change, build the deck hand
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
    const startTime = performance.now();

    // Animate deck reel — cycle through the hand
    const animateReel = () => {
      const elapsed = performance.now() - startTime;
      if (elapsed >= duration) return;
      const progress = elapsed / duration;
      const eased = Math.sin(progress * Math.PI / 2);
      const interval = 200 + (300 * eased);
      setDeckIndex(prev => (prev + 1) % (deckHand.length || 1));
      setTimeout(animateReel, interval);
    };
    animateReel();

    // Wait for spin to finish
    await new Promise(r => setTimeout(r, duration));

    // Land on a topic
    const topic = await getRandomTopic(catId as any);
    if (topic) {
      setDeckHand(prev => [topic, ...prev.filter(t => t.id !== topic.id)].slice(0, 6));
      setDeckIndex(0);
    }
    setCurrentTopic(topic);
    setSpinPhase('landed');

    if (topic) questSystem.onSpin(catId);

    setTimeout(() => setSpinPhase('idle'), 2500);
  }, [selectedCategories, spinPhase, deckHand, questSystem]);

  const handleTopicOpen = () => {
    if (currentTopic && (spinPhase === 'landed' || spinPhase === 'idle')) {
      navigate(`/reveal/${currentTopic.categoryId.toLowerCase()}/${currentTopic.id}`);
    }
  };

  // Resolve what each fan slot shows
  const getFanTopic = (slot: number): CurioTopic | null => {
    if (deckHand.length === 0) return null;
    const idx = ((deckIndex + slot) % deckHand.length + deckHand.length) % deckHand.length;
    return deckHand[idx] || null;
  };

  return (
    <div
      className="min-h-screen pb-24 relative overflow-hidden"
      style={{ backgroundColor: getBackgroundColor(isDark, isAmoled) }}
    >
      {/* Watermark backdrop — category glyphs scattered */}
      <div className="absolute inset-0 pointer-events-none overflow-hidden">
        {ALL_CATEGORIES.slice(0, 8).map((cat, i) => (
          <div
            key={cat.id}
            className="absolute opacity-[0.025]"
            style={{
              left: `${8 + (i % 4) * 26}%`,
              top: `${8 + Math.floor(i / 4) * 48}%`,
              transform: `rotate(${-15 + i * 10}deg)`,
              color: isDark ? 'white' : cat.accent,
            }}
          >
            <MaterialIcon name={cat.iconGlyph} size={110} />
          </div>
        ))}
      </div>

      {/* Main stage */}
      <div className="relative z-10 flex flex-col items-center justify-center min-h-[calc(100vh-96px)] px-4 pt-2">
        {/* Fan deck area */}
        <div className="relative flex flex-col items-center" style={{ height: 520 }}>
          {/* Top peek cards (slots -2, -1) */}
          <div className="relative w-[320px] h-[200px]">
            <PeekCard slot={-2} topic={getFanTopic(-2)} category={activeCategory} isSpinning={spinPhase === 'spinning'} />
            <PeekCard slot={-1} topic={getFanTopic(-1)} category={activeCategory} isSpinning={spinPhase === 'spinning'} />
          </div>

          {/* Hero ticket + orbit */}
          <div className="relative w-[300px] h-[330px] -mt-2 flex items-center justify-center">
            <OrbitRing active={spinPhase === 'spinning'} color={activeCategory.accent} />
            <HeroTicket
              topic={currentTopic || getFanTopic(0)}
              category={activeCategory}
              isSpinning={spinPhase === 'spinning'}
              spinPhase={spinPhase}
              onClick={handleTopicOpen}
            />
          </div>

          {/* Bottom peek cards (slots +1, +2) */}
          <div className="relative w-[320px] h-[180px] -mt-4">
            <PeekCard slot={1} topic={getFanTopic(1)} category={activeCategory} isSpinning={spinPhase === 'spinning'} />
            <PeekCard slot={2} topic={getFanTopic(2)} category={activeCategory} isSpinning={spinPhase === 'spinning'} />
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
              <CategoryPill
                key={cat.id}
                category={cat}
                isSelected={selectedCategories.some(c => c.id === cat.id)}
                onClick={() => toggleCategory(cat)}
              />
            ))}
          </div>

          <button
            onClick={() => setShowPicker(true)}
            className="mt-2 w-full py-2.5 rounded-full text-sm font-medium transition-all"
            style={{
              background: isDark ? 'rgba(255,255,255,0.03)' : 'rgba(0,0,0,0.015)',
              color: isDark ? 'rgba(255,255,255,0.35)' : 'rgba(59,10,23,0.35)',
            }}
          >
            All categories →
          </button>
        </div>
      </div>

      {/* Category picker modal */}
      {showPicker && (
        <div className="fixed inset-0 bg-black/50 z-[70] flex items-end justify-center" onClick={() => setShowPicker(false)}>
          <div
            className="w-full max-w-lg rounded-t-3xl p-6 max-h-[70vh] overflow-y-auto"
            style={{ background: isDark ? '#1a1a2e' : 'white' }}
            onClick={e => e.stopPropagation()}
          >
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-bold" style={{ color: getTextColor(isDark), fontFamily: 'Geom, Inter, sans-serif' }}>
                Choose Categories
              </h3>
              <button
                onClick={() => setShowPicker(false)}
                className="w-8 h-8 rounded-full flex items-center justify-center text-xl"
                style={{ color: isDark ? 'rgba(255,255,255,0.5)' : 'rgba(59,10,23,0.5)' }}
              >
                ×
              </button>
            </div>
            <div className="grid grid-cols-3 gap-3">
              {ALL_CATEGORIES.filter(c => c.isReady).map(cat => {
                const sel = selectedCategories.some(c => c.id === cat.id);
                return (
                  <button
                    key={cat.id}
                    onClick={() => { toggleCategory(cat); setShowPicker(false); }}
                    className="flex flex-col items-center gap-1.5 p-3 rounded-2xl transition-all duration-200"
                    style={{
                      background: sel ? cat.accent : isDark ? `${cat.accent}22` : cat.tint,
                      color: sel ? 'white' : isDark ? cat.lightAccent : cat.accent,
                    }}
                  >
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
          0%, 100% { transform: translate(-50%, -50%) scale(1); opacity: 0.7; }
          50% { transform: translate(-50%, -50%) scale(1.6); opacity: 1; }
        }
        @keyframes diceTumble {
          0%   { transform: rotate(0deg) translateY(0); }
          25%  { transform: rotate(90deg) translateY(-2px); }
          50%  { transform: rotate(180deg) translateY(0); }
          75%  { transform: rotate(270deg) translateY(2px); }
          100% { transform: rotate(360deg) translateY(0); }
        }
        @keyframes diceSettle {
          0%   { transform: scale(1.15) rotate(10deg); }
          60%  { transform: scale(0.95) rotate(-3deg); }
          100% { transform: scale(1) rotate(0deg); }
        }
        .animate-spin { animation: spin 1s linear infinite; }
        @keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
        .animate-pulse-slow { animation: pulse-slow 2s ease-in-out infinite; }
        @keyframes pulse-slow { 0%, 100% { opacity: 1; } 50% { opacity: 0.6; } }
      `}</style>
    </div>
  );
};

export default SpinScreen;
