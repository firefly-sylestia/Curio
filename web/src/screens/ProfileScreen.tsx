// Curio Web App - Profile Screen
// Matches Android: torn rose hero (same seed as Home), watermark collage, stats + achievements
// v2 — edge-to-edge constellation section, lifetime totals with Favorites

import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTheme, getBackgroundColor, getTextColor } from '../theme/ThemeContext';
import { getQuestSystem } from '../data/QuestSystem';
import { getPetSystem } from '../data/PetSystem';
import {
  CurioWatermarkBackdrop,
  CurioStatCard,
  CurioSectionHeader,
  CurioProgressBar,
  CurioBadge,
  MaterialIcon,
} from '../components/SharedComponents';
import { TornHero, PROFILE_HERO_SYMBOLS } from '../components/TornHero';
import { Constellation } from '../components/Constellation';
import { ScreenEntrance } from '../animations';

const PROFILE_HERO_HEIGHT = 220;
const PROFILE_TEAR_SEED = 0xC0FEE; // Same as Home
const ROSE_WOOD = '#C46B7C';

const AchievementCard: React.FC<{
  title: string; description: string; icon: string; unlocked: boolean;
}> = ({ title, description, icon, unlocked }) => {
  const { isDark } = useTheme();
  return (
    <div className="flex items-center gap-3 p-3 rounded-2xl transition-all"
      style={{
        background: unlocked
          ? (isDark ? 'rgba(255,255,255,0.08)' : 'rgba(59,10,23,0.05)')
          : (isDark ? 'rgba(255,255,255,0.03)' : 'rgba(59,10,23,0.02)'),
        opacity: unlocked ? 1 : 0.5,
      }}>
      <div className="w-12 h-12 rounded-xl flex items-center justify-center flex-shrink-0"
        style={{ background: unlocked ? 'linear-gradient(135deg, #FFD97D 0%, #FF8FA3 100%)'
          : (isDark ? 'rgba(255,255,255,0.1)' : 'rgba(59,10,23,0.05)') }}>
        <MaterialIcon name={icon} size={24}
          style={{ color: unlocked ? '#fff' : isDark ? 'rgba(255,255,255,0.4)' : 'rgba(59,10,23,0.4)' }} />
      </div>
      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-2">
          <h4 className="text-sm font-semibold" style={{ color: getTextColor(isDark) }}>{title}</h4>
          {unlocked && <CurioBadge count={1} color="#FFD97D" size="small" />}
        </div>
        <p className="text-xs mt-0.5" style={{ color: isDark ? 'rgba(255,255,255,0.5)' : 'rgba(59,10,23,0.5)' }}>{description}</p>
      </div>
    </div>
  );
};

const PetDisplay: React.FC<{ pet: any }> = ({ pet }) => {
  const { isDark } = useTheme();
  return (
    <div className="w-full p-4 rounded-2xl"
      style={{ background: isDark ? 'rgba(255,255,255,0.05)' : 'rgba(59,10,23,0.03)' }}>
      <div className="flex items-center gap-4">
        <div className="w-16 h-16 rounded-2xl flex items-center justify-center"
          style={{ background: 'linear-gradient(135deg, #FF8FA3 0%, #FFD97D 100%)' }}>
          <MaterialIcon name="pets" size={32} style={{ color: '#fff' }} />
        </div>
        <div className="flex-1">
          <h4 className="text-lg font-bold" style={{ color: getTextColor(isDark), fontFamily: 'Geom, sans-serif' }}>
            {pet?.name || 'Your Companion'}
          </h4>
          <p className="text-sm" style={{ color: isDark ? 'rgba(255,255,255,0.5)' : 'rgba(59,10,23,0.5)' }}>
            Level {pet?.level || 1} &middot; {pet?.mood || 'Happy'}
          </p>
          <div className="mt-2">
            <CurioProgressBar
              progress={(pet?.xp || 0) / (pet?.xpToNext || 100) * 100}
              color="#FF8FA3" height={6} />
          </div>
        </div>
      </div>
    </div>
  );
};

/** Count favorites from localStorage keys: curio-fav-{cat}-{topicId} = 'true' */
const countFavorites = (): number => {
  let count = 0;
  for (let i = 0; i < localStorage.length; i++) {
    const key = localStorage.key(i);
    if (key && key.startsWith('curio-fav-') && localStorage.getItem(key) === 'true') {
      count++;
    }
  }
  return count;
};

export const ProfileScreen: React.FC = () => {
  const navigate = useNavigate();
  const { isDark, isAmoled } = useTheme();
  const [questSystem] = useState(() => getQuestSystem());
  const [petSystem] = useState(() => getPetSystem());

  const [stats, setStats] = useState({ totalEntries: 0, currentStreak: 0, bestStreak: 0, xp: 0, level: 1 });
  const [pet, setPet] = useState<any>(null);
  const [achievements, setAchievements] = useState<any[]>([]);
  const [xpToNext, setXpToNext] = useState(100);
  const [favoriteCount, setFavoriteCount] = useState(0);

  useEffect(() => {
    const qs = questSystem.getState();
    setStats({
      totalEntries: qs.lifetime?.saves || 0,
      currentStreak: qs.bestStreak || 0,
      bestStreak: qs.bestStreak || 0,
      xp: qs.xp || 0,
      level: questSystem.getLevel(),
    });
    setXpToNext((questSystem.getLevel() || 1) * 100);
    setPet(petSystem.getState());
    setFavoriteCount(countFavorites());
    setAchievements([
      { id: 'first_entry', title: 'First Entry', description: 'Save your first entry', icon: 'edit_note', unlocked: (qs.lifetime?.saves || 0) >= 1 },
      { id: 'streak_3', title: 'On Fire', description: 'Maintain a 3-day streak', icon: 'local_fire_department', unlocked: (qs.bestStreak || 0) >= 3 },
      { id: 'streak_7', title: 'Week Warrior', description: 'Maintain a 7-day streak', icon: 'bolt', unlocked: (qs.bestStreak || 0) >= 7 },
      { id: 'entries_10', title: 'Collector', description: 'Save 10 entries', icon: 'book_5', unlocked: (qs.lifetime?.saves || 0) >= 10 },
      { id: 'entries_50', title: 'Archivist', description: 'Save 50 entries', icon: 'account_balance', unlocked: (qs.lifetime?.saves || 0) >= 50 },
      { id: 'level_5', title: 'Rising Star', description: 'Reach level 5', icon: 'star', unlocked: questSystem.getLevel() >= 5 },
      { id: 'level_10', title: 'Expert', description: 'Reach level 10', icon: 'auto_awesome', unlocked: questSystem.getLevel() >= 10 },
    ]);
  }, [questSystem, petSystem]);

  const levelProgress = (stats.xp % xpToNext) / xpToNext * 100;

  const lifetime = questSystem.getState().lifetime;

  return (
    <div className="min-h-screen pb-24 relative" style={{ backgroundColor: getBackgroundColor(isDark, isAmoled) }}>
      <CurioWatermarkBackdrop topClearance={PROFILE_HERO_HEIGHT + 30} alphaScale={0.45} />

      {/* ── Torn Hero Banner ──────────────────────────────────────── */}
      <TornHero
        height={PROFILE_HERO_HEIGHT}
        fill={ROSE_WOOD}
        ink="#fff"
        tearSeed={PROFILE_TEAR_SEED}
        bold={true}
        symbols={PROFILE_HERO_SYMBOLS}
        isDark={isDark}
      >
        <div className="flex flex-col h-full px-5 pt-[68px] pb-[18px]">
          {/* Pill row: back left, settings right */}
          <div className="absolute top-0 left-0 right-0 flex justify-between px-5 items-center" style={{ marginTop: 'env(safe-area-inset-top, 12px)' }}>
            <button onClick={() => navigate(-1)} className="w-10 h-10 rounded-full flex items-center justify-center"
              style={{ background: 'rgba(255,255,255,0.18)', border: '1px solid rgba(255,255,255,0.25)' }}>
              <MaterialIcon name="arrow_back" size={20} style={{ color: '#fff' }} />
            </button>
            <button onClick={() => navigate('/settings')} className="w-10 h-10 rounded-full flex items-center justify-center"
              style={{ background: 'rgba(255,255,255,0.18)', border: '1px solid rgba(255,255,255,0.25)' }}>
              <MaterialIcon name="settings" size={20} style={{ color: '#fff' }} />
            </button>
          </div>
          {/* Avatar + name */}
          <div className="flex flex-col items-center">
            <div className="w-16 h-16 rounded-full flex items-center justify-center mb-2"
              style={{ background: 'rgba(255,255,255,0.22)', border: '2px solid rgba(255,255,255,0.5)' }}>
              <span className="text-2xl font-bold text-white">C</span>
            </div>
            <h1 className="text-xl font-extrabold text-white" style={{ fontFamily: 'Geom, Inter, sans-serif' }}>
              Curio Explorer
            </h1>
            <p className="text-xs text-white/80 mt-0.5">Level {stats.level} &middot; {stats.xp} XP</p>
          </div>
        </div>
      </TornHero>

      <ScreenEntrance>
        <div className="relative z-10">
          {/* Stats grid */}
          <div className="px-4 mb-6">
            <CurioSectionHeader title="Your Stats" />
            <div className="grid grid-cols-2 gap-3">
              <CurioStatCard label="Total Entries" value={stats.totalEntries} icon="edit_note" color="#4338CA" />
              <CurioStatCard label="Current Streak" value={stats.currentStreak} icon="local_fire_department" color="#FF8FA3" />
              <CurioStatCard label="Best Streak" value={stats.bestStreak} icon="star" color="#B45309" />
              <CurioStatCard label="Level" value={stats.level} icon="auto_awesome" color="#047857" />
            </div>
          </div>

          {/* ── Edge-to-edge constellation — full-width deep-space sky ── */}
          <div className="mb-6 overflow-hidden" style={{ margin: '0 -0px', marginBottom: '24px' }}>
            <div className="px-4 mb-2">
              <h3 className="text-lg font-bold" style={{ color: getTextColor(isDark), fontFamily: 'Geom, Inter, sans-serif' }}>Your Constellation</h3>
              <p className="text-xs" style={{ color: isDark ? 'rgba(255,255,255,0.5)' : 'rgba(59,10,23,0.5)' }}>Every star is a lane you've explored</p>
            </div>
            <Constellation isDark={isDark} />
          </div>

          {/* ── Lifetime totals — matches Android's LifetimeTotalsCard ── */}
          <div className="px-4 mb-6">
            <div className="p-4 rounded-2xl" style={{ background: isDark ? 'rgba(255,255,255,0.05)' : 'rgba(59,10,23,0.03)' }}>
              <h3 className="text-base font-extrabold" style={{ color: getTextColor(isDark), fontFamily: 'Geom, Inter, sans-serif' }}>Lifetime totals</h3>
              <p className="text-xs mb-3" style={{ color: isDark ? 'rgba(255,255,255,0.5)' : 'rgba(59,10,23,0.5)' }}>Everything your curiosity has collected</p>
              <div className="grid grid-cols-2 gap-2">
                <LifetimeStatItem icon="auto_awesome" label="Spins" value={lifetime.spins} color="#9B7BB8" />
                <LifetimeStatItem icon="travel_explore" label="Explores" value={lifetime.explores} color="#7FA0C8" />
                <LifetimeStatItem icon="bookmark" label="Saved" value={lifetime.saves} color="#B98A5E" />
                <LifetimeStatItem icon="format_quote" label="Quotes" value={lifetime.quotes} color="#7FA0C8" />
                <LifetimeStatItem icon="push_pin" label="Pins" value={lifetime.pins} color="#C96F4A" />
                <LifetimeStatItem icon="star" label="Favorites" value={favoriteCount} color="#D9A85C" />
                <LifetimeStatItem icon="task_alt" label="Daily quests" value={lifetime.dailyCompleted} color="#7F9B6E" />
              </div>
            </div>
          </div>

          {/* Pet */}
          <div className="px-4 mb-6">
            <CurioSectionHeader title="Your Companion" action="Customize" onAction={() => navigate('/pet-designer')} />
            <PetDisplay pet={pet} />
          </div>

          {/* Achievements */}
          <div className="px-4 mb-6">
            <CurioSectionHeader title="Achievements"
              action={`${achievements.filter(a => a.unlocked).length}/${achievements.length}`} />
            <div className="space-y-3">
              {achievements.map(a => (
                <AchievementCard key={a.id} title={a.title} description={a.description} icon={a.icon} unlocked={a.unlocked} />
              ))}
            </div>
          </div>

          {/* Level progress */}
          <div className="px-4 mb-6">
            <CurioSectionHeader title="Level Progress" />
            <div className="p-4 rounded-2xl" style={{ background: isDark ? 'rgba(255,255,255,0.05)' : 'rgba(59,10,23,0.03)' }}>
              <div className="flex items-center justify-between mb-2">
                <span className="text-sm font-semibold" style={{ color: getTextColor(isDark) }}>Level {stats.level}</span>
                <span className="text-sm" style={{ color: isDark ? 'rgba(255,255,255,0.5)' : 'rgba(59,10,23,0.5)' }}>
                  {stats.xp} / {xpToNext} XP
                </span>
              </div>
              <CurioProgressBar progress={levelProgress} color="#FF8FA3" height={8} />
            </div>
          </div>
        </div>
      </ScreenEntrance>
    </div>
  );
};

/** Single lifetime stat tile — icon + label + count, matching Android's rounded row */
const LifetimeStatItem: React.FC<{
  icon: string; label: string; value: number; color: string;
}> = ({ icon, label, value, color }) => {
  const { isDark } = useTheme();
  return (
    <div className="flex items-center gap-2.5 p-2.5 rounded-xl"
      style={{ background: isDark ? 'rgba(255,255,255,0.04)' : 'rgba(59,10,23,0.025)' }}>
      <div className="w-8 h-8 rounded-lg flex items-center justify-center flex-shrink-0"
        style={{ background: `${color}20` }}>
        <MaterialIcon name={icon} size={17} style={{ color }} />
      </div>
      <div className="flex-1 min-w-0">
        <p className="text-xs" style={{ color: isDark ? 'rgba(255,255,255,0.5)' : 'rgba(59,10,23,0.5)' }}>{label}</p>
        <p className="text-sm font-extrabold" style={{ color: getTextColor(isDark), fontFamily: 'Geom, Inter, sans-serif' }}>{value}</p>
      </div>
    </div>
  );
};

export default ProfileScreen;
