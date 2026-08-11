// Curio Web App - Profile Screen
// Matches Android: watermark backdrop, Material icons, stats + achievements

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

const AchievementCard: React.FC<{
  title: string;
  description: string;
  icon: string;
  unlocked: boolean;
  progress?: number;
}> = ({ title, description, icon, unlocked, progress }) => {
  const { isDark } = useTheme();

  return (
    <div
      className="flex items-center gap-3 p-3 rounded-2xl transition-all duration-200"
      style={{
        background: unlocked 
          ? (isDark ? 'rgba(255,255,255,0.08)' : 'rgba(59,10,23,0.05)')
          : (isDark ? 'rgba(255,255,255,0.03)' : 'rgba(59,10,23,0.02)'),
        opacity: unlocked ? 1 : 0.5,
      }}
    >
      <div
        className="w-12 h-12 rounded-xl flex items-center justify-center flex-shrink-0"
        style={{
          background: unlocked 
            ? 'linear-gradient(135deg, #FFD97D 0%, #FF8FA3 100%)'
            : (isDark ? 'rgba(255,255,255,0.1)' : 'rgba(59,10,23,0.05)'),
        }}
      >
        <MaterialIcon name={icon} size={24} style={{ color: unlocked ? '#fff' : isDark ? 'rgba(255,255,255,0.4)' : 'rgba(59,10,23,0.4)' }} />
      </div>
      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-2">
          <h4 className="text-sm font-semibold" style={{ color: getTextColor(isDark) }}>{title}</h4>
          {unlocked && <CurioBadge count={1} color="#FFD97D" size="small" />}
        </div>
        <p className="text-xs mt-0.5" style={{ color: isDark ? 'rgba(255,255,255,0.5)' : 'rgba(59,10,23,0.5)' }}>
          {description}
        </p>
        {progress !== undefined && !unlocked && (
          <div className="mt-2">
            <CurioProgressBar progress={progress} color="#FF8FA3" height={4} />
          </div>
        )}
      </div>
    </div>
  );
};

const PetDisplay: React.FC<{ pet: any; onClick: () => void }> = ({ pet, onClick }) => {
  const { isDark } = useTheme();
  return (
    <button
      onClick={onClick}
      className="w-full p-4 rounded-2xl text-left transition-all duration-200"
      style={{ background: isDark ? 'rgba(255,255,255,0.05)' : 'rgba(59,10,23,0.03)' }}
    >
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
            Level {pet?.level || 1} • {pet?.mood || 'Happy'}
          </p>
          <div className="mt-2">
            <CurioProgressBar progress={(pet?.xp || 0) / (pet?.xpToNext || 100) * 100} color="#FF8FA3" height={6} />
          </div>
        </div>
      </div>
    </button>
  );
};

export const ProfileScreen: React.FC = () => {
  const navigate = useNavigate();
  const { isDark, isAmoled } = useTheme();
  const [questSystem] = useState(() => getQuestSystem());
  const [petSystem] = useState(() => getPetSystem());
  
  const [stats, setStats] = useState({
    totalEntries: 0, currentStreak: 0, bestStreak: 0, xp: 0, level: 1,
  });
  const [pet, setPet] = useState<any>(null);
  const [achievements, setAchievements] = useState<any[]>([]);

  useEffect(() => {
    const loadData = async () => {
      const questState = questSystem.getState();
      setStats({
        totalEntries: questState.lifetime?.saves || 0,
        currentStreak: questState.bestStreak || 0,
        bestStreak: questState.bestStreak || 0,
        xp: questState.xp || 0,
        level: questSystem.getLevel(),
      });
      const petData = petSystem.getState();
      setPet(petData);
      setAchievements([
        { id: 'first_entry', title: 'First Entry', description: 'Save your first entry', icon: 'edit_note', unlocked: (questState.lifetime?.saves || 0) >= 1 },
        { id: 'streak_3', title: 'On Fire', description: 'Maintain a 3-day streak', icon: 'local_fire_department', unlocked: (questState.bestStreak || 0) >= 3 },
        { id: 'streak_7', title: 'Week Warrior', description: 'Maintain a 7-day streak', icon: 'bolt', unlocked: (questState.bestStreak || 0) >= 7 },
        { id: 'entries_10', title: 'Collector', description: 'Save 10 entries', icon: 'book_5', unlocked: (questState.lifetime?.saves || 0) >= 10 },
        { id: 'entries_50', title: 'Archivist', description: 'Save 50 entries', icon: 'account_balance', unlocked: (questState.lifetime?.saves || 0) >= 50 },
        { id: 'level_5', title: 'Rising Star', description: 'Reach level 5', icon: 'star', unlocked: questSystem.getLevel() >= 5 },
        { id: 'level_10', title: 'Expert', description: 'Reach level 10', icon: 'auto_awesome', unlocked: questSystem.getLevel() >= 10 },
      ]);
    };
    loadData();
  }, [questSystem, petSystem]);

  return (
    <div className="min-h-screen pb-24 relative" style={{ backgroundColor: getBackgroundColor(isDark, isAmoled) }}>
      <CurioWatermarkBackdrop alphaScale={0.45} />

      <div className="relative z-10">
        <div className="px-4 pt-6 mb-6">
          <h1 className="text-2xl font-bold" style={{ color: getTextColor(isDark), fontFamily: 'Geom, sans-serif' }}>Profile</h1>
        </div>

        <div className="px-4 mb-6">
          <div className="flex items-center gap-4 p-4 rounded-2xl"
            style={{ background: isDark ? 'rgba(255,255,255,0.05)' : 'rgba(59,10,23,0.03)' }}>
            <div className="w-16 h-16 rounded-full flex items-center justify-center"
              style={{ background: 'linear-gradient(135deg, #FF8FA3 0%, #FFD97D 100%)' }}>
              <span className="text-2xl font-bold text-white">C</span>
            </div>
            <div>
              <h2 className="text-xl font-bold" style={{ color: getTextColor(isDark), fontFamily: 'Geom, sans-serif' }}>Curio Explorer</h2>
              <p className="text-sm" style={{ color: isDark ? 'rgba(255,255,255,0.5)' : 'rgba(59,10,23,0.5)' }}>Level {stats.level} • {stats.xp} XP</p>
            </div>
          </div>
        </div>

        <div className="px-4 mb-6">
          <CurioSectionHeader title="Your Stats" />
          <div className="grid grid-cols-2 gap-3">
            <CurioStatCard label="Total Entries" value={stats.totalEntries} icon="edit_note" color="#4338CA" />
            <CurioStatCard label="Current Streak" value={stats.currentStreak} icon="local_fire_department" color="#FF8FA3" />
            <CurioStatCard label="Best Streak" value={stats.bestStreak} icon="star" color="#B45309" />
            <CurioStatCard label="Level" value={stats.level} icon="auto_awesome" color="#047857" />
          </div>
        </div>

        <div className="px-4 mb-6">
          <CurioSectionHeader title="Your Companion" action="View" onAction={() => navigate('/pet-designer')} />
          <PetDisplay pet={pet} onClick={() => navigate('/pet-designer')} />
        </div>

        <div className="px-4 mb-6">
          <CurioSectionHeader title="Achievements" action={`${achievements.filter(a => a.unlocked).length}/${achievements.length}`} />
          <div className="space-y-3">
            {achievements.map((a) => (
              <AchievementCard key={a.id} title={a.title} description={a.description} icon={a.icon} unlocked={a.unlocked} progress={a.progress} />
            ))}
          </div>
        </div>

        <div className="px-4 mb-6">
          <CurioSectionHeader title="Level Progress" />
          <div className="p-4 rounded-2xl" style={{ background: isDark ? 'rgba(255,255,255,0.05)' : 'rgba(59,10,23,0.03)' }}>
            <div className="flex items-center justify-between mb-2">
              <span className="text-sm font-semibold" style={{ color: getTextColor(isDark) }}>Level {stats.level}</span>
              <span className="text-sm" style={{ color: isDark ? 'rgba(255,255,255,0.5)' : 'rgba(59,10,23,0.5)' }}>{stats.xp} / {(stats.level || 1) * 100} XP</span>
            </div>
            <CurioProgressBar progress={(stats.xp % ((stats.level || 1) * 100)) / ((stats.level || 1) * 100) * 100} color="#FF8FA3" height={8} />
          </div>
        </div>
      </div>
    </div>
  );
};

export default ProfileScreen;
