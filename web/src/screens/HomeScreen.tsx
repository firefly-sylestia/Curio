// Curio Web App - Home Screen
// Premium design with hero card, category grid, quick actions, and progress stats

import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTheme, getBackgroundColor, getTextColor } from '../theme/ThemeContext';
import { ALL_CATEGORIES } from '../data/categories';
import { getRandomTopic } from '../data/topics';
import { getQuestSystem } from '../data/QuestSystem';
import {
  CurioHeroCard,
  CurioCategoryCard,
  CurioStatCard,
  CurioSectionHeader,
  MaterialIcon,
} from '../components/SharedComponents';
import type { CurioCategory } from '../types';

// ─── Streak Pill ──────────────────────────────────────────────────────
const StreakPill: React.FC<{ streak: number; bestStreak: number }> = ({ streak, bestStreak }) => {
  const { isDark } = useTheme();
  return (
    <div
      className="flex items-center gap-1.5 px-3 py-1.5 rounded-full"
      style={{ background: isDark ? 'rgba(255,143,163,0.12)' : 'rgba(255,143,163,0.08)' }}
    >
      <MaterialIcon name="local_fire_department" size={18} style={{ color: '#FF8FA3' }} />
      <span className="text-sm font-bold" style={{ color: '#FF8FA3', fontFamily: 'Geom, Inter, sans-serif' }}>
        {streak}
      </span>
      {bestStreak > 0 && (
        <span className="text-xs" style={{ color: isDark ? 'rgba(255,255,255,0.4)' : 'rgba(59,10,23,0.4)' }}>
          best {bestStreak}
        </span>
      )}
    </div>
  );
};

// ─── Quick Action Card ────────────────────────────────────────────────
const QuickActionCard: React.FC<{
  icon: string;
  title: string;
  subtitle: string;
  onClick: () => void;
  color?: string;
}> = ({ icon, title, subtitle, onClick, color = '#3B0A17' }) => {
  const { isDark } = useTheme();
  const [isPressed, setIsPressed] = useState(false);

  return (
    <button
      onClick={onClick}
      onMouseDown={() => setIsPressed(true)}
      onMouseUp={() => setIsPressed(false)}
      onMouseLeave={() => setIsPressed(false)}
      className="flex items-center gap-3 p-3.5 rounded-2xl transition-all duration-200 text-left"
      style={{
        background: isDark ? 'rgba(255,255,255,0.04)' : 'rgba(59,10,23,0.02)',
        transform: isPressed ? 'scale(0.97)' : 'scale(1)',
        border: isDark ? '1px solid rgba(255,255,255,0.06)' : '1px solid rgba(59,10,23,0.04)',
      }}
    >
      <div
        className="w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0"
        style={{ background: `${color}18` }}
      >
        <MaterialIcon name={icon} size={20} style={{ color }} />
      </div>
      <div className="flex-1 min-w-0">
        <div className="text-sm font-semibold truncate" style={{ color: getTextColor(isDark) }}>
          {title}
        </div>
        <div className="text-xs truncate" style={{ color: isDark ? 'rgba(255,255,255,0.4)' : 'rgba(59,10,23,0.4)' }}>
          {subtitle}
        </div>
      </div>
    </button>
  );
};

// ─── Main HomeScreen ──────────────────────────────────────────────────
export const HomeScreen: React.FC = () => {
  const navigate = useNavigate();
  const { isDark, isAmoled } = useTheme();
  const [questSystem] = useState(() => getQuestSystem());

  const [selectedCategory, setSelectedCategory] = useState<CurioCategory | null>(null);
  const [stats, setStats] = useState({ entries: 0, streak: 0, bestStreak: 0, spins: 0 });
  const [greeting, setGreeting] = useState('');

  useEffect(() => {
    const hour = new Date().getHours();
    if (hour < 12) setGreeting('Good morning');
    else if (hour < 17) setGreeting('Good afternoon');
    else setGreeting('Good evening');
  }, []);

  useEffect(() => {
    const state = questSystem.getState();
    setStats({
      entries: state.lifetime?.saves || 0,
      streak: state.bestStreak || 0,
      bestStreak: state.bestStreak || 0,
      spins: state.lifetime?.spins || 0,
    });
  }, [questSystem]);

  const handleShuffle = async () => {
    const catId = selectedCategory?.id || 'WILDCARD';
    const topic = await getRandomTopic(catId);
    if (topic) {
      questSystem.onSpin(catId);
      navigate(`/reveal/${catId}/${topic.id}`);
    }
  };

  const handleCategorySelect = (cat: CurioCategory) => {
    setSelectedCategory(prev => prev?.id === cat.id ? null : cat);
  };

  const activeCategory = selectedCategory || ALL_CATEGORIES[0];

  return (
    <div
      className="min-h-screen pb-24"
      style={{ backgroundColor: getBackgroundColor(isDark, isAmoled) }}
    >
      {/* Watermark backdrop */}
      <div className="absolute inset-0 pointer-events-none overflow-hidden">
        {ALL_CATEGORIES.slice(0, 6).map((cat, i) => (
          <div
            key={cat.id}
            className="absolute opacity-[0.02]"
            style={{
              left: `${5 + (i % 3) * 33}%`,
              top: `${12 + Math.floor(i / 3) * 40}%`,
              transform: `rotate(${-20 + i * 14}deg)`,
              color: isDark ? 'white' : cat.accent,
            }}
          >
            <MaterialIcon name={cat.iconGlyph} size={100} />
          </div>
        ))}
      </div>

      {/* Main content */}
      <div className="relative z-10 px-4 pt-6">
        {/* Header */}
        <div className="flex items-center justify-between mb-5">
          <div>
            <p
              className="text-sm mb-0.5"
              style={{ color: isDark ? 'rgba(255,255,255,0.45)' : 'rgba(59,10,23,0.45)' }}
            >
              {greeting}
            </p>
            <h1
              className="text-[26px] font-extrabold leading-tight"
              style={{ color: getTextColor(isDark), fontFamily: 'Geom, Inter, sans-serif' }}
            >
              Discover Curio
            </h1>
          </div>
          <StreakPill streak={stats.streak} bestStreak={stats.bestStreak} />
        </div>

        {/* Hero Card */}
        <div className="mb-6">
          <CurioHeroCard
            category={activeCategory}
            onClick={handleShuffle}
            subtitle={selectedCategory ? `Shuffle ${selectedCategory.displayName}` : 'Tap to discover something new'}
          />
        </div>

        {/* Category Chips */}
        <div className="mb-6">
          <CurioSectionHeader title="Categories" action="See all" onAction={() => navigate('/spin')} />
          <div className="flex gap-2.5 overflow-x-auto pb-2 -mx-4 px-4 scrollbar-hide">
            {ALL_CATEGORIES.filter(c => c.isReady).slice(0, 8).map(cat => (
              <CurioCategoryCard
                key={cat.id}
                category={cat}
                isSelected={selectedCategory?.id === cat.id}
                onClick={() => handleCategorySelect(cat)}
                size="small"
              />
            ))}
          </div>
        </div>

        {/* Quick Actions */}
        <div className="mb-6">
          <CurioSectionHeader title="Quick Actions" />
          <div className="grid grid-cols-2 gap-3">
            <QuickActionCard
              icon="casino"
              title="Spin"
              subtitle="Random topic"
              onClick={() => navigate('/spin')}
              color="#FF8FA3"
            />
            <QuickActionCard
              icon="book_5"
              title="Cabinet"
              subtitle="Saved entries"
              onClick={() => navigate('/cabinet')}
              color="#4338CA"
            />
            <QuickActionCard
              icon="emoji_events"
              title="Quests"
              subtitle="Daily goals"
              onClick={() => navigate('/quests')}
              color="#047857"
            />
            <QuickActionCard
              icon="pets"
              title="Companion"
              subtitle="Your pet"
              onClick={() => navigate('/pet-designer')}
              color="#B45309"
            />
          </div>
        </div>

        {/* Stats */}
        <div className="mb-6">
          <CurioSectionHeader title="Your Progress" />
          <div className="grid grid-cols-4 gap-3">
            <CurioStatCard label="Entries" value={stats.entries} icon="edit_note" color="#4338CA" />
            <CurioStatCard label="Spins" value={stats.spins} icon="casino" color="#FF8FA3" />
            <CurioStatCard label="Streak" value={stats.streak} icon="local_fire_department" color="#B45309" />
            <CurioStatCard label="Best" value={stats.bestStreak} icon="trophy" color="#047857" />
          </div>
        </div>
      </div>
    </div>
  );
};

export default HomeScreen;
