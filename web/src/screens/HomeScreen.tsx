// Curio Web App - Home Screen (Premium Version)
// Matches Android app's premium design with hero card and smooth animations

import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTheme, getBackgroundColor, getTextColor } from '../theme/ThemeContext';
import { ALL_CATEGORIES, getCategoryById } from '../data/categories';
import { getRandomTopic } from '../data/topics';
import { getQuestSystem } from '../data/QuestSystem';
import { 
  CurioHeroCard, 
  CurioCategoryCard, 
  CurioStatCard,
  CurioSectionHeader,
} from '../components/SharedComponents';
import type { CurioCategory } from '../types';

// ─── Streak Pill Component ────────────────────────────────────────────
const StreakPill: React.FC<{
  streak: number;
  bestStreak: number;
}> = ({ streak, bestStreak }) => {
  const { isDark } = useTheme();
  
  return (
    <div
      className="flex items-center gap-2 px-3 py-1.5 rounded-full"
      style={{
        background: isDark ? 'rgba(255,143,163,0.15)' : 'rgba(255,143,163,0.1)',
      }}
    >
      <span className="text-lg">🔥</span>
      <span
        className="text-sm font-bold"
        style={{ color: '#FF8FA3', fontFamily: 'Geom, sans-serif' }}
      >
        {streak}
      </span>
      {bestStreak > 0 && (
        <span
          className="text-xs"
          style={{ color: isDark ? 'rgba(255,255,255,0.5)' : 'rgba(59,10,23,0.5)' }}
        >
          best: {bestStreak}
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
      className="flex items-center gap-3 p-3 rounded-2xl transition-all duration-200 text-left"
      style={{
        background: isDark ? 'rgba(255,255,255,0.05)' : 'rgba(59,10,23,0.03)',
        transform: isPressed ? 'scale(0.98)' : 'scale(1)',
      }}
    >
      <div
        className="w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0"
        style={{ background: `${color}15` }}
      >
        <span className="text-lg">{icon}</span>
      </div>
      <div className="flex-1 min-w-0">
        <div
          className="text-sm font-semibold truncate"
          style={{ color: getTextColor(isDark) }}
        >
          {title}
        </div>
        <div
          className="text-xs truncate"
          style={{ color: isDark ? 'rgba(255,255,255,0.5)' : 'rgba(59,10,23,0.5)' }}
        >
          {subtitle}
        </div>
      </div>
    </button>
  );
};

// ─── Main HomeScreen Component ────────────────────────────────────────
export const HomeScreen: React.FC = () => {
  const navigate = useNavigate();
  const { isDark, isAmoled } = useTheme();
  const [questSystem] = useState(() => getQuestSystem());
  
  const [selectedCategory, setSelectedCategory] = useState<CurioCategory | null>(null);
  const [stats, setStats] = useState({ entries: 0, streak: 0, bestStreak: 0 });
  const [recentEntries] = useState<any[]>([]);
  
  // Load stats
  useEffect(() => {
    const loadStats = async () => {
      // In a real app, this would load from IndexedDB
      const state = questSystem.getState();
      setStats({
        entries: state.lifetime?.saves || 0,
        streak: state.bestStreak || 0,
        bestStreak: state.bestStreak || 0,
      });
    };
    loadStats();
  }, [questSystem]);

  const handleShuffle = async () => {
    const categoryId = selectedCategory?.id || 'WILDCARD';
    const topic = await getRandomTopic(categoryId);
    if (topic) {
      questSystem.onSpin(categoryId);
      navigate(`/reveal/${categoryId}/${topic.id}`);
    }
  };

  const handleCategorySelect = (category: CurioCategory) => {
    setSelectedCategory(prev => 
      prev?.id === category.id ? null : category
    );
  };

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
            className="absolute text-[100px] opacity-[0.02]"
            style={{
              left: `${5 + (i % 3) * 33}%`,
              top: `${10 + Math.floor(i / 3) * 40}%`,
              transform: `rotate(${-20 + i * 12}deg)`,
              color: isDark ? 'white' : cat.accent,
            }}
          >
            {cat.iconGlyph}
          </div>
        ))}
      </div>

      {/* Main content */}
      <div className="relative z-10 px-4 pt-6">
        {/* Header */}
        <div className="flex items-center justify-between mb-6">
          <div>
            <h1
              className="text-2xl font-bold"
              style={{ color: getTextColor(isDark), fontFamily: 'Geom, sans-serif' }}
            >
              Curio
            </h1>
            <p
              className="text-sm"
              style={{ color: isDark ? 'rgba(255,255,255,0.5)' : 'rgba(59,10,23,0.5)' }}
            >
              Discover something new
            </p>
          </div>
          <div className="flex items-center gap-3">
            <StreakPill streak={stats.streak} bestStreak={stats.bestStreak} />
          </div>
        </div>

        {/* Hero Card */}
        <div className="mb-6">
          <CurioHeroCard
            category={selectedCategory || ALL_CATEGORIES[0]}
            onClick={handleShuffle}
            subtitle={selectedCategory ? `Shuffle for ${selectedCategory.displayName}` : 'Tap to discover something new'}
          />
        </div>

        {/* Category Chips */}
        <div className="mb-6">
          <CurioSectionHeader title="Categories" action="See all" onAction={() => navigate('/spin')} />
          <div className="flex gap-2 overflow-x-auto pb-2 -mx-4 px-4">
            {ALL_CATEGORIES.filter(c => c.isReady).slice(0, 8).map((category) => (
              <CurioCategoryCard
                key={category.id}
                category={category}
                isSelected={selectedCategory?.id === category.id}
                onClick={() => handleCategorySelect(category)}
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
              icon="🎲"
              title="Spin"
              subtitle="Random topic"
              onClick={() => navigate('/spin')}
              color="#FF8FA3"
            />
            <QuickActionCard
              icon="📚"
              title="Cabinet"
              subtitle="Saved entries"
              onClick={() => navigate('/cabinet')}
              color="#4338CA"
            />
            <QuickActionCard
              icon="🎯"
              title="Quests"
              subtitle="Daily goals"
              onClick={() => navigate('/quests')}
              color="#047857"
            />
            <QuickActionCard
              icon="🐾"
              title="Pet"
              subtitle="Your companion"
              onClick={() => navigate('/profile')}
              color="#B45309"
            />
          </div>
        </div>

        {/* Stats */}
        <div className="mb-6">
          <CurioSectionHeader title="Your Progress" />
          <div className="grid grid-cols-3 gap-3">
            <CurioStatCard
              label="Entries"
              value={stats.entries}
              icon="📝"
              color="#4338CA"
            />
            <CurioStatCard
              label="Streak"
              value={stats.streak}
              icon="🔥"
              color="#FF8FA3"
            />
            <CurioStatCard
              label="Best"
              value={stats.bestStreak}
              icon="⭐"
              color="#B45309"
            />
          </div>
        </div>

        {/* Recent Entries */}
        {recentEntries.length > 0 && (
          <div className="mb-6">
            <CurioSectionHeader 
              title="Recent" 
              action="View all" 
              onAction={() => navigate('/cabinet')} 
            />
            <div className="space-y-3">
              {recentEntries.slice(0, 3).map((entry, index) => (
                <QuickActionCard
                  key={entry.id || index}
                  icon={getCategoryById(entry.categoryId)?.iconGlyph || '📝'}
                  title={entry.title || 'Untitled'}
                  subtitle={entry.subtitle || ''}
                  onClick={() => navigate(`/entry/${entry.id}`)}
                  color={getCategoryById(entry.categoryId)?.accent || '#3B0A17'}
                />
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default HomeScreen;
