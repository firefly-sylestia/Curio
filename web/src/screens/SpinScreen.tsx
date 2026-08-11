// Curio Web App - Spin Screen (Premium Version)
// Matches Android app's fan-deck carousel with orbit ring animation

import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useTheme, getBackgroundColor, getTextColor } from '../theme/ThemeContext';
import { ALL_CATEGORIES, getCategoryBySlug } from '../data/categories';
import { getRandomTopic } from '../data/topics';
import { getQuestSystem } from '../data/QuestSystem';
import type { CurioCategory, CurioTopic } from '../types';

// Orbit ring dots configuration
const ORBIT_DOTS = 12;
const ORBIT_RADIUS = 140; // px from center

// Spin duration range (ms)
const SPIN_MIN = 2800;
const SPIN_MAX = 3600;

// Peek card component
const PeekCard: React.FC<{
  topic: CurioTopic | null;
  category: CurioCategory;
  position: 'top' | 'bottom';
  scale: number;
  opacity: number;
}> = ({ topic, category, position, scale, opacity }) => {
  const { isDark } = useTheme();
  
  if (!topic) return null;
  
  return (
    <div
      className="absolute left-1/2 transform -translate-x-1/2 transition-all duration-300"
      style={{
        [position]: position === 'top' ? -60 : undefined,
        bottom: position === 'bottom' ? -60 : undefined,
        width: 280,
        height: 80,
        transform: `translateX(-50%) scale(${scale})`,
        opacity,
        zIndex: position === 'top' ? 1 : 0,
      }}
    >
      <div
        className="w-full h-full rounded-2xl p-3 flex items-center gap-3"
        style={{
          background: isDark
            ? `linear-gradient(135deg, ${category.accent}33 0%, ${category.accent}11 100%)`
            : `linear-gradient(135deg, ${category.tint} 0%, white 100%)`,
          boxShadow: '0 4px 12px rgba(0,0,0,0.1)',
        }}
      >
        <div
          className="w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0"
          style={{ background: `${category.accent}22` }}
        >
          <span className="text-lg">{category.iconGlyph}</span>
        </div>
        <div className="flex-1 min-w-0">
          <div className="text-sm font-semibold truncate" style={{ color: getTextColor(isDark) }}>
            {topic.name}
          </div>
          <div className="text-xs truncate" style={{ color: isDark ? 'rgba(255,255,255,0.5)' : 'rgba(59,10,23,0.5)' }}>
            {topic.subtype}
          </div>
        </div>
      </div>
    </div>
  );
};

// Hero ticket card
const HeroTicket: React.FC<{
  topic: CurioTopic | null;
  category: CurioCategory;
  isShuffling: boolean;
  onClick: () => void;
}> = ({ topic, category, isShuffling, onClick }) => {
  const { isDark, heroGradient } = useTheme();
  
  const getBackground = () => {
    if (heroGradient) {
      if (isDark) {
        return `linear-gradient(135deg, ${category.accent}44 0%, ${category.accent}22 50%, ${category.lightAccent}11 100%)`;
      }
      return `linear-gradient(135deg, ${category.accent} 0%, ${category.lightAccent} 100%)`;
    }
    return isDark ? category.accent : category.tint;
  };
  
  return (
    <div
      className="relative w-[286px] h-[310px] rounded-3xl overflow-hidden cursor-pointer transition-transform duration-300"
      style={{
        background: getBackground(),
        boxShadow: '0 8px 32px rgba(0,0,0,0.15)',
        transform: isShuffling ? 'scale(0.98)' : 'scale(1)',
        zIndex: 10,
      }}
      onClick={onClick}
    >
      {/* Category accent bar */}
      <div
        className="absolute top-0 left-0 right-0 h-1"
        style={{ background: category.accent }}
      />
      
      {/* Content */}
      <div className="flex flex-col items-center justify-center h-full p-6 text-center">
        {topic ? (
          <>
            {/* Category badge */}
            <div
              className="px-3 py-1 rounded-full text-xs font-medium mb-4"
              style={{
                background: 'rgba(255,255,255,0.2)',
                color: 'white',
              }}
            >
              {topic.subtype}
            </div>
            
            {/* Topic name */}
            <h2
              className="text-2xl font-bold text-white mb-2"
              style={{ fontFamily: 'Geom, sans-serif' }}
            >
              {topic.name}
            </h2>
            
            {/* Teaser */}
            <p className="text-sm text-white/80 line-clamp-3">
              {topic.teaser}
            </p>
            
            {/* Action hint */}
            <div className="mt-4 text-xs text-white/60">
              {isShuffling ? 'Spinning...' : 'Tap to explore'}
            </div>
          </>
        ) : (
          <>
            {/* Empty state */}
            <div className="text-6xl mb-4">🎰</div>
            <h2
              className="text-xl font-bold text-white"
              style={{ fontFamily: 'Geom, sans-serif' }}
            >
              {category.displayName}
            </h2>
            <p className="text-sm text-white/70 mt-2">
              Tap to spin
            </p>
          </>
        )}
      </div>
      
      {/* Watermark glyph */}
      <div
        className="absolute bottom-4 right-4 text-6xl opacity-10"
        style={{ color: 'white' }}
      >
        {category.iconGlyph}
      </div>
    </div>
  );
};

// Orbit ring animation
const OrbitRing: React.FC<{
  active: boolean;
  color: string;
}> = ({ active, color }) => {
  const [rotation, setRotation] = useState(0);
  
  useEffect(() => {
    if (!active) {
      setRotation(0);
      return;
    }
    
    const interval = setInterval(() => {
      setRotation(r => (r + 2) % 360);
    }, 16);
    
    return () => clearInterval(interval);
  }, [active]);
  
  return (
    <div
      className="absolute inset-0 pointer-events-none"
      style={{
        transform: `rotate(${rotation}deg)`,
      }}
    >
      {Array.from({ length: ORBIT_DOTS }).map((_, i) => {
        const angle = (i / ORBIT_DOTS) * Math.PI * 2;
        const x = Math.cos(angle) * ORBIT_RADIUS + 50; // 50% center
        const y = Math.sin(angle) * ORBIT_RADIUS + 50;
        const delay = i * 0.1;
        
        return (
          <div
            key={i}
            className="absolute w-2 h-2 rounded-full"
            style={{
              left: `${x}%`,
              top: `${y}%`,
              transform: 'translate(-50%, -50%)',
              background: color,
              opacity: active ? 0.6 : 0.2,
              transition: `opacity 0.3s ease ${delay}s`,
              animation: active ? `pulse 1.5s ease-in-out infinite ${delay}s` : 'none',
            }}
          />
        );
      })}
    </div>
  );
};

// Spin button
const SpinButton: React.FC<{
  isShuffling: boolean;
  onClick: () => void;
  color: string;
}> = ({ isShuffling, onClick, color }) => {
  
  return (
    <button
      onClick={onClick}
      disabled={isShuffling}
      className="relative z-20 transition-all duration-300"
      style={{
        width: isShuffling ? 100 : 118,
        height: isShuffling ? 100 : 118,
        borderRadius: '50%',
        background: `linear-gradient(135deg, ${color} 0%, ${color}CC 100%)`,
        boxShadow: `0 8px 32px ${color}44`,
        transform: isShuffling ? 'scale(0.92)' : 'scale(1)',
      }}
    >
      <div className="flex items-center justify-center h-full">
        <span
          className="text-4xl text-white"
          style={{
            animation: isShuffling ? 'tumble 1.6s linear infinite' : 'none',
          }}
        >
          🎲
        </span>
      </div>
    </button>
  );
};

// Category pill button
const CategoryPill: React.FC<{
  category: CurioCategory;
  isSelected: boolean;
  onClick: () => void;
}> = ({ category, isSelected, onClick }) => {
  const { isDark } = useTheme();
  
  return (
    <button
      onClick={onClick}
      className="flex items-center gap-2 px-4 py-2 rounded-full transition-all"
      style={{
        background: isSelected
          ? category.accent
          : isDark
            ? `${category.accent}33`
            : category.tint,
        color: isSelected
          ? 'white'
          : isDark
            ? category.lightAccent
            : category.accent,
        boxShadow: isSelected ? `0 4px 12px ${category.accent}44` : 'none',
      }}
    >
      <span className="text-lg">{category.iconGlyph}</span>
      <span className="text-sm font-medium whitespace-nowrap">{category.displayName}</span>
    </button>
  );
};

// Main SpinScreen component
export const SpinScreen: React.FC = () => {
  const navigate = useNavigate();
  const { categorySlug } = useParams();
  const { isDark, isAmoled } = useTheme();
  const [questSystem] = useState(() => getQuestSystem());
  
  const [selectedCategories, setSelectedCategories] = useState<CurioCategory[]>([]);
  const [isShuffling, setIsShuffling] = useState(false);
  const [currentTopic, setCurrentTopic] = useState<CurioTopic | null>(null);
  const [showPicker, setShowPicker] = useState(false);
  
  // Initialize from URL or default
  useEffect(() => {
    if (categorySlug) {
      const category = getCategoryBySlug(categorySlug);
      if (category) {
        setSelectedCategories([category]);
      }
    } else if (selectedCategories.length === 0) {
      // Default to first category
      setSelectedCategories([ALL_CATEGORIES[0]]);
    }
  }, [categorySlug]);
  
  const activeCategory = selectedCategories[0] || ALL_CATEGORIES[0];
  
  const toggleCategory = (category: CurioCategory) => {
    setSelectedCategories(prev => {
      const exists = prev.find(c => c.id === category.id);
      if (exists) {
        return prev.filter(c => c.id !== category.id);
      }
      if (prev.length >= 3) {
        return [category];
      }
      return [...prev, category];
    });
  };
  
  const handleSpin = useCallback(async () => {
    if (selectedCategories.length === 0 || isShuffling) return;
    
    setIsShuffling(true);
    
    // Get random topic
    const categoryIds = selectedCategories.map(c => c.id);
    const topic = await getRandomTopic(categoryIds[0]);
    
    // Random spin duration
    const duration = SPIN_MIN + Math.random() * (SPIN_MAX - SPIN_MIN);
    
    // Simulate spinning animation
    await new Promise(resolve => setTimeout(resolve, duration));
    
    setCurrentTopic(topic);
    setIsShuffling(false);
    
    // Track in quest system
    if (topic) {
      questSystem.onSpin(categoryIds[0]);
    }
  }, [selectedCategories, isShuffling, questSystem]);
  
  const handleTopicOpen = () => {
    if (currentTopic) {
      navigate(`/reveal/${activeCategory.id.toLowerCase()}/${currentTopic.id}`);
    }
  };
  
  return (
    <div
      className="min-h-screen pb-24 relative overflow-hidden"
      style={{ backgroundColor: getBackgroundColor(isDark, isAmoled) }}
    >
      {/* Watermark backdrop */}
      <div className="absolute inset-0 pointer-events-none overflow-hidden">
        {ALL_CATEGORIES.slice(0, 8).map((cat, i) => (
          <div
            key={cat.id}
            className="absolute text-8xl opacity-[0.03]"
            style={{
              left: `${10 + (i % 4) * 25}%`,
              top: `${10 + Math.floor(i / 4) * 50}%`,
              transform: `rotate(${-15 + i * 10}deg)`,
              color: isDark ? 'white' : cat.accent,
            }}
          >
            {cat.iconGlyph}
          </div>
        ))}
      </div>
      
      {/* Main content */}
      <div className="relative z-10 flex flex-col items-center justify-center min-h-[calc(100vh-96px)] px-4">
        {/* Deck section */}
        <div className="relative flex flex-col items-center">
          {/* Top peek card */}
          <PeekCard
            topic={currentTopic}
            category={activeCategory}
            position="top"
            scale={0.9}
            opacity={0.6}
          />
          
          {/* Hero ticket with orbit ring */}
          <div className="relative">
            <OrbitRing active={isShuffling} color={activeCategory.accent} />
            <HeroTicket
              topic={currentTopic}
              category={activeCategory}
              isShuffling={isShuffling}
              onClick={handleTopicOpen}
            />
          </div>
          
          {/* Bottom peek card */}
          <PeekCard
            topic={currentTopic}
            category={activeCategory}
            position="bottom"
            scale={0.9}
            opacity={0.6}
          />
          
          {/* Spin button */}
          <div className="mt-8">
            <SpinButton
              isShuffling={isShuffling}
              onClick={handleSpin}
              color={activeCategory.accent}
            />
          </div>
        </div>
        
        {/* Category selector */}
        <div className="mt-8 w-full max-w-md">
          <div className="flex gap-2 overflow-x-auto pb-2 justify-center">
            {ALL_CATEGORIES.filter(c => c.isReady).slice(0, 6).map((category) => (
              <CategoryPill
                key={category.id}
                category={category}
                isSelected={selectedCategories.some(c => c.id === category.id)}
                onClick={() => toggleCategory(category)}
              />
            ))}
          </div>
          
          {/* More categories button */}
          <button
            onClick={() => setShowPicker(true)}
            className="mt-4 w-full py-2 rounded-full text-sm font-medium transition-all"
            style={{
              background: isDark ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.02)',
              color: isDark ? 'rgba(255,255,255,0.5)' : 'rgba(59,10,23,0.5)',
            }}
          >
            All Categories →
          </button>
        </div>
      </div>
      
      {/* Full category picker modal */}
      {showPicker && (
        <div className="fixed inset-0 bg-black/50 z-[70] flex items-end justify-center">
          <div
            className="w-full max-w-lg rounded-t-3xl p-6 max-h-[70vh] overflow-y-auto"
            style={{ background: isDark ? '#1a1a2e' : 'white' }}
          >
            <div className="flex items-center justify-between mb-4">
              <h3
                className="text-lg font-bold"
                style={{ color: getTextColor(isDark), fontFamily: 'Geom, sans-serif' }}
              >
                Choose Categories
              </h3>
              <button
                onClick={() => setShowPicker(false)}
                className="text-2xl"
                style={{ color: isDark ? 'rgba(255,255,255,0.5)' : 'rgba(59,10,23,0.5)' }}
              >
                ×
              </button>
            </div>
            
            <div className="grid grid-cols-3 gap-3">
              {ALL_CATEGORIES.filter(c => c.isReady).map((category) => (
                <button
                  key={category.id}
                  onClick={() => {
                    toggleCategory(category);
                    setShowPicker(false);
                  }}
                  className="p-3 rounded-2xl transition-all"
                  style={{
                    background: selectedCategories.some(c => c.id === category.id)
                      ? category.accent
                      : isDark
                        ? `${category.accent}22`
                        : category.tint,
                    color: selectedCategories.some(c => c.id === category.id)
                      ? 'white'
                      : isDark
                        ? category.lightAccent
                        : category.accent,
                  }}
                >
                  <span className="text-2xl">{category.iconGlyph}</span>
                  <div className="text-xs font-medium mt-1">{category.displayName}</div>
                </button>
              ))}
            </div>
          </div>
        </div>
      )}
      
      {/* CSS Animations */}
      <style>{`
        @keyframes pulse {
          0%, 100% { transform: translate(-50%, -50%) scale(1); opacity: 0.6; }
          50% { transform: translate(-50%, -50%) scale(1.3); opacity: 1; }
        }
        @keyframes tumble {
          0% { transform: rotate(0deg) translateY(0); }
          25% { transform: rotate(90deg) translateY(-4px); }
          50% { transform: rotate(180deg) translateY(0); }
          75% { transform: rotate(270deg) translateY(4px); }
          100% { transform: rotate(360deg) translateY(0); }
        }
      `}</style>
    </div>
  );
};

export default SpinScreen;
