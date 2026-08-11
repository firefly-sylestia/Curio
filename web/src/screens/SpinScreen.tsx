// Curio Web App - Spin Screen
// Fan-deck carousel with orbit ring animation and proper Material icons

import React, { useState, useEffect, useCallback } from 'react';
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

// ─── Hero Ticket Card ─────────────────────────────────────────────────
const HeroTicket: React.FC<{
  topic: CurioTopic | null;
  category: CurioCategory;
  isSpinning: boolean;
  spinPhase: 'idle' | 'spinning' | 'landed';
  onClick: () => void;
}> = ({ topic, category, isSpinning, spinPhase, onClick }) => {
  const { heroGradient } = useTheme();
  const [isPressed, setIsPressed] = useState(false);
  const [shimmerPos, setShimmerPos] = useState(-100);

  useEffect(() => {
    if (spinPhase !== 'spinning' && !isSpinning) {
      const interval = setInterval(() => {
        setShimmerPos(prev => (prev >= 200 ? -100 : prev + 1.5));
      }, 30);
      return () => clearInterval(interval);
    }
  }, [spinPhase, isSpinning]);

  const getBg = () => {
    if (heroGradient) {
      const base = `linear-gradient(145deg, ${category.accent} 0%,`;
      const mid = ` ${category.accent}DD 30%,`;
      const end = ` ${category.lightAccent || category.accent}88 100%)`;
      return base + mid + end;
    }
    return category.accent;
  };

  return (
    <button
      onClick={onClick}
      onMouseDown={() => setIsPressed(true)}
      onMouseUp={() => setIsPressed(false)}
      onMouseLeave={() => setIsPressed(false)}
      disabled={isSpinning}
      className="relative w-[280px] h-[340px] rounded-[32px] overflow-hidden text-left transition-all duration-500"
      style={{
        background: getBg(),
        transform: isPressed ? 'scale(0.96)' : 'scale(1)',
        boxShadow: `0 12px 48px ${category.accent}44, 0 2px 8px rgba(0,0,0,0.1)`,
        cursor: isSpinning ? 'default' : 'pointer',
        border: `2px solid ${category.accent}88`,
      }}
    >
      {/* Shimmer */}
      {spinPhase !== 'spinning' && (
        <div
          className="absolute inset-0 pointer-events-none"
          style={{
            background: 'linear-gradient(105deg, transparent 40%, rgba(255,255,255,0.08) 50%, transparent 60%)',
            transform: `translateX(${shimmerPos}%)`,
          }}
        />
      )}

      {/* Edge shine */}
      <div
        className="absolute top-0 left-4 right-4 h-[1px]"
        style={{ background: 'linear-gradient(90deg, transparent, rgba(255,255,255,0.3), transparent)' }}
      />

      {/* Watermark glyph */}
      <div
        className="absolute right-3 bottom-3 pointer-events-none select-none"
        style={{ color: 'rgba(255,255,255,0.12)' }}
      >
        <MaterialIcon name={category.iconGlyph} size={120} />
      </div>

      {/* Content */}
      <div className="relative z-10 flex flex-col items-center justify-center h-full p-6 text-center">
        {spinPhase === 'spinning' ? (
          <div className="flex flex-col items-center gap-3">
            <MaterialIcon name="casino" size={48} className="text-white/70 animate-spin" />
            <p className="text-white/60 text-sm font-medium">Spinning...</p>
          </div>
        ) : topic ? (
          <>
            {/* Category badge */}
            <div
              className="flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold mb-5"
              style={{ background: 'rgba(255,255,255,0.18)', color: 'white' }}
            >
              <MaterialIcon name={category.iconGlyph} size={14} />
              {topic.subtype}
            </div>

            {/* Topic name */}
            <h2
              className="text-2xl font-extrabold text-white leading-tight mb-3 px-2"
              style={{ fontFamily: 'Geom, Inter, sans-serif', textShadow: '0 2px 8px rgba(0,0,0,0.15)' }}
            >
              {topic.name}
            </h2>

            {/* Teaser */}
            <p className="text-sm text-white/80 line-clamp-2 leading-relaxed px-2">
              {topic.teaser}
            </p>

            {/* Tap hint */}
            <div className="mt-5 flex items-center gap-1.5 text-xs text-white/50">
              <MaterialIcon name="touch_app" size={16} />
              Tap to explore
            </div>
          </>
        ) : (
          <>
            <MaterialIcon name="casino" size={56} className="text-white/70 mb-4" />
            <h2
              className="text-xl font-bold text-white mb-2"
              style={{ fontFamily: 'Geom, Inter, sans-serif' }}
            >
              {category.displayName}
            </h2>
            <p className="text-sm text-white/70">Tap the button below to spin</p>
          </>
        )}
      </div>
    </button>
  );
};

// ─── Orbit Ring ───────────────────────────────────────────────────────
const OrbitRing: React.FC<{ active: boolean; color: string }> = ({ active, color }) => {
  const [rotation, setRotation] = useState(0);

  useEffect(() => {
    if (!active) { setRotation(0); return; }
    const interval = setInterval(() => setRotation(r => (r + 3) % 360), 16);
    return () => clearInterval(interval);
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
              width: active ? 6 : 3, height: active ? 6 : 3,
              transform: 'translate(-50%, -50%)',
              background: color,
              opacity: active ? 0.8 : 0.15,
              animation: active ? `orbitPulse 1.6s ease-in-out ${i * 0.12}s infinite` : 'none',
            }}
          />
        );
      })}
    </div>
  );
};

// ─── Spin Button ──────────────────────────────────────────────────────
const SpinButton: React.FC<{
  isSpinning: boolean;
  onClick: () => void;
  color: string;
  iconName: string;
}> = ({ isSpinning, onClick, color, iconName }) => {
  const [isPressed, setIsPressed] = useState(false);

  return (
    <button
      onClick={onClick}
      disabled={isSpinning}
      onMouseDown={() => setIsPressed(true)}
      onMouseUp={() => setIsPressed(false)}
      onMouseLeave={() => setIsPressed(false)}
      className="relative z-20 flex items-center justify-center rounded-full transition-all duration-300"
      style={{
        width: isSpinning ? 96 : 112,
        height: isSpinning ? 96 : 112,
        background: `linear-gradient(135deg, ${color} 0%, ${color}DD 100%)`,
        boxShadow: isSpinning
          ? `0 4px 16px ${color}33`
          : `0 8px 32px ${color}55, 0 2px 8px rgba(0,0,0,0.15)`,
        transform: isPressed ? 'scale(0.9)' : 'scale(1)',
        cursor: isSpinning ? 'default' : 'pointer',
      }}
    >
      <MaterialIcon
        name={isSpinning ? 'hourglass_top' : iconName}
        size={isSpinning ? 36 : 44}
        className="text-white"
        filled={!isSpinning}
      />
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
  const [isPressed, setIsPressed] = useState(false);

  return (
    <button
      onClick={onClick}
      onMouseDown={() => setIsPressed(true)}
      onMouseUp={() => setIsPressed(false)}
      onMouseLeave={() => setIsPressed(false)}
      className="flex items-center gap-1.5 px-3.5 py-2 rounded-full text-sm font-medium whitespace-nowrap transition-all duration-200"
      style={{
        background: isSelected ? category.accent : isDark ? 'rgba(255,255,255,0.06)' : 'rgba(0,0,0,0.04)',
        color: isSelected ? 'white' : isDark ? 'rgba(255,255,255,0.7)' : 'rgba(59,10,23,0.7)',
        boxShadow: isSelected ? `0 4px 16px ${category.accent}44` : 'none',
        transform: isPressed ? 'scale(0.94)' : 'scale(1)',
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

    // Fetch topic while spinning
    const topic = await getRandomTopic(catId);

    // Wait for spin to finish
    await new Promise(r => setTimeout(r, duration));

    setCurrentTopic(topic);
    setSpinPhase('landed');

    if (topic) questSystem.onSpin(catId);

    // Reset to idle after a beat
    setTimeout(() => setSpinPhase('idle'), 2000);
  }, [selectedCategories, spinPhase, questSystem]);

  const handleTopicOpen = () => {
    if (currentTopic && spinPhase === 'landed') {
      navigate(`/reveal/${currentTopic.categoryId.toLowerCase()}/${currentTopic.id}`);
    }
  };

  return (
    <div
      className="min-h-screen pb-24 relative overflow-hidden"
      style={{ backgroundColor: getBackgroundColor(isDark, isAmoled) }}
    >
      {/* Subtle backdrop */}
      <div className="absolute inset-0 pointer-events-none overflow-hidden">
        {ALL_CATEGORIES.slice(0, 8).map((cat, i) => (
          <div
            key={cat.id}
            className="absolute opacity-[0.03]"
            style={{
              left: `${8 + (i % 4) * 26}%`,
              top: `${8 + Math.floor(i / 4) * 48}%`,
              transform: `rotate(${-15 + i * 10}deg)`,
              color: isDark ? 'white' : cat.accent,
            }}
          >
            <MaterialIcon name={cat.iconGlyph} size={100} />
          </div>
        ))}
      </div>

      {/* Main */}
      <div className="relative z-10 flex flex-col items-center justify-center min-h-[calc(100vh-96px)] px-4 pt-4">
        {/* Deck area */}
        <div className="relative flex flex-col items-center">
          {/* Top peek */}
          {currentTopic && (
            <div
              className="w-[240px] h-16 rounded-2xl mb-[-20px] flex items-center gap-2 px-3 transition-all duration-500"
              style={{
                background: isDark
                  ? `linear-gradient(135deg, ${activeCategory.accent}22, ${activeCategory.accent}08)`
                  : `linear-gradient(135deg, ${activeCategory.tint}, rgba(255,255,255,0.8))`,
                opacity: spinPhase !== 'idle' ? 0 : 0.5,
                transform: `scale(0.9) translateY(${spinPhase === 'landed' ? -8 : 0}px)`,
              }}
            >
              <MaterialIcon name={activeCategory.iconGlyph} size={20} style={{ opacity: 0.6 }} />
              <div className="text-xs font-medium truncate" style={{ color: getTextColor(isDark), opacity: 0.6 }}>
                {currentTopic.name}
              </div>
            </div>
          )}

          {/* Hero ticket + orbit */}
          <div className="relative my-4">
            <div className="relative w-[300px] h-[360px] flex items-center justify-center">
              <OrbitRing active={spinPhase === 'spinning'} color={activeCategory.accent} />
              <HeroTicket
                topic={currentTopic}
                category={activeCategory}
                isSpinning={spinPhase === 'spinning'}
                spinPhase={spinPhase}
                onClick={handleTopicOpen}
              />
            </div>
          </div>

          {/* Spin button */}
          <div className="-mt-2 mb-6">
            <SpinButton
              isSpinning={spinPhase === 'spinning'}
              onClick={handleSpin}
              color={activeCategory.accent}
              iconName={activeCategory.iconGlyph}
            />
          </div>
        </div>

        {/* Category pills */}
        <div className="w-full max-w-lg">
          <div className="flex gap-2 overflow-x-auto pb-2 justify-center flex-wrap">
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
            className="mt-3 w-full py-2.5 rounded-full text-sm font-medium transition-all"
            style={{
              background: isDark ? 'rgba(255,255,255,0.04)' : 'rgba(0,0,0,0.02)',
              color: isDark ? 'rgba(255,255,255,0.4)' : 'rgba(59,10,23,0.4)',
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
          0%, 100% { transform: translate(-50%, -50%) scale(1); opacity: 0.8; }
          50% { transform: translate(-50%, -50%) scale(1.8); opacity: 1; }
        }
        @keyframes spin {
          from { transform: rotate(0deg); }
          to { transform: rotate(360deg); }
        }
        .animate-spin { animation: spin 1s linear infinite; }
      `}</style>
    </div>
  );
};

export default SpinScreen;
