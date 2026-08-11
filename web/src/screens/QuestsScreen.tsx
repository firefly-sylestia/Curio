// Curio Web App - Quests Screen
// Displays quest chains, daily quests, and weekly quests

import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTheme, getBackgroundColor, getTextColor } from '../theme/ThemeContext';
import { getQuestSystem } from '../data/QuestSystem';
import type { QuestChain, DailyQuest, WeeklyQuest } from '../data/QuestSystem';

// Quest Chain Card
const QuestChainCard: React.FC<{
  chain: QuestChain;
  questSystem: ReturnType<typeof getQuestSystem>;
}> = ({ chain, questSystem }) => {
  const { isDark } = useTheme();
  const completedStages = chain.stages.filter(s => questSystem.isStageDone(s)).length;
  const progress = completedStages / chain.stages.length;

  return (
    <div
      className="p-4 rounded-[16px]"
      style={{
        background: isDark ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.02)',
      }}
    >
      <div className="flex items-center gap-3 mb-3">
        <span className="text-2xl">{chain.glyph}</span>
        <div className="flex-1">
          <h3 className="font-semibold" style={{ color: getTextColor(isDark) }}>
            {chain.title}
          </h3>
          <p className="text-sm" style={{ color: isDark ? 'rgba(255,255,255,0.5)' : 'rgba(59,10,23,0.5)' }}>
            {chain.subtitle}
          </p>
        </div>
        <span className="text-sm font-medium" style={{ color: '#FF8FA3' }}>
          {completedStages}/{chain.stages.length}
        </span>
      </div>
      
      {/* Progress bar */}
      <div
        className="h-2 rounded-full overflow-hidden"
        style={{ background: isDark ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.1)' }}
      >
        <div
          className="h-full rounded-full transition-all duration-500"
          style={{
            width: `${progress * 100}%`,
            background: 'linear-gradient(90deg, #FF8FA3 0%, #FFC2CE 100%)',
          }}
        />
      </div>
      
      {/* Stages */}
      <div className="mt-3 space-y-2">
        {chain.stages.map((stage) => {
          const isDone = questSystem.isStageDone(stage);
          const stageProgress = questSystem.getStageProgress(stage);
          
          return (
            <div
              key={stage.id}
              className="flex items-center gap-3 p-2 rounded-[8px]"
              style={{
                background: isDone ? 'rgba(76, 175, 80, 0.1)' : 'transparent',
              }}
            >
              <div
                className="w-6 h-6 rounded-full flex items-center justify-center text-sm"
                style={{
                  background: isDone
                    ? '#4CAF50'
                    : isDark ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.05)',
                  color: isDone ? 'white' : getTextColor(isDark),
                }}
              >
                {isDone ? '✓' : ''}
              </div>
              <div className="flex-1">
                <div className="text-sm font-medium" style={{ color: getTextColor(isDark) }}>
                  {stage.title}
                </div>
                <div className="text-xs" style={{ color: isDark ? 'rgba(255,255,255,0.4)' : 'rgba(59,10,23,0.4)' }}>
                  {stage.description}
                </div>
              </div>
              <div className="text-xs" style={{ color: isDark ? 'rgba(255,255,255,0.4)' : 'rgba(59,10,23,0.4)' }}>
                {stageProgress}/{stage.target}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};

// Daily Quest Card
const DailyQuestCard: React.FC<{
  quest: DailyQuest;
  questSystem: ReturnType<typeof getQuestSystem>;
  onClaim: (questId: string) => void;
}> = ({ quest, questSystem, onClaim }) => {
  const { isDark } = useTheme();
  const progress = questSystem.getDailyProgress(quest.kind);
  const isComplete = questSystem.isDailyComplete(quest);
  const isClaimed = questSystem.isDailyClaimed(quest.id);

  return (
    <div
      className="p-4 rounded-[16px] flex items-center gap-4"
      style={{
        background: isDark ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.02)',
      }}
    >
      <div className="flex-1">
        <div className="flex items-center gap-2">
          <h4 className="font-medium" style={{ color: getTextColor(isDark) }}>
            {quest.title}
          </h4>
          {quest.bonus && (
            <span className="px-2 py-0.5 rounded-full text-xs font-medium bg-amber-100 text-amber-700">
              Bonus
            </span>
          )}
        </div>
        <div className="flex items-center gap-2 mt-1">
          <div
            className="flex-1 h-1.5 rounded-full overflow-hidden"
            style={{ background: isDark ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.1)' }}
          >
            <div
              className="h-full rounded-full transition-all"
              style={{
                width: `${Math.min(progress / quest.target, 1) * 100}%`,
                background: isComplete ? '#4CAF50' : '#FF8FA3',
              }}
            />
          </div>
          <span className="text-xs" style={{ color: isDark ? 'rgba(255,255,255,0.4)' : 'rgba(59,10,23,0.4)' }}>
            {progress}/{quest.target}
          </span>
        </div>
      </div>
      
      <div className="text-right">
        <div className="text-sm font-medium" style={{ color: '#FF8FA3' }}>
          +{quest.xpReward} XP
        </div>
        {isComplete && !isClaimed && (
          <button
            onClick={() => onClaim(quest.id)}
            className="mt-1 px-3 py-1 rounded-full text-xs font-medium text-white"
            style={{ background: '#FF8FA3' }}
          >
            Claim
          </button>
        )}
        {isClaimed && (
          <span className="text-xs text-green-500">✓ Claimed</span>
        )}
      </div>
    </div>
  );
};

// Weekly Quest Card
const WeeklyQuestCard: React.FC<{
  quest: WeeklyQuest;
  questSystem: ReturnType<typeof getQuestSystem>;
  onClaim: (questId: string) => void;
}> = ({ quest, questSystem, onClaim }) => {
  const { isDark } = useTheme();
  const progress = questSystem.getWeeklyProgress(quest);
  const isComplete = questSystem.isWeeklyComplete(quest);
  const isClaimed = questSystem.isWeeklyClaimed(quest.id);

  return (
    <div
      className="p-4 rounded-[16px]"
      style={{
        background: isDark ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.02)',
      }}
    >
      <div className="flex items-center justify-between mb-2">
        <h4 className="font-medium" style={{ color: getTextColor(isDark) }}>
          {quest.title}
        </h4>
        <span className="text-sm font-medium" style={{ color: '#FF8FA3' }}>
          +{quest.xpReward} XP
        </span>
      </div>
      
      <p className="text-sm mb-2" style={{ color: isDark ? 'rgba(255,255,255,0.5)' : 'rgba(59,10,23,0.5)' }}>
        {quest.description}
      </p>
      
      <div className="flex items-center gap-3">
        <div
          className="flex-1 h-2 rounded-full overflow-hidden"
          style={{ background: isDark ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.1)' }}
        >
          <div
            className="h-full rounded-full transition-all"
            style={{
              width: `${Math.min(progress / quest.target, 1) * 100}%`,
              background: isComplete ? '#4CAF50' : '#FF8FA3',
            }}
          />
        </div>
        <span className="text-xs" style={{ color: isDark ? 'rgba(255,255,255,0.4)' : 'rgba(59,10,23,0.4)' }}>
          {progress}/{quest.target}
        </span>
      </div>
      
      {isComplete && !isClaimed && (
        <button
          onClick={() => onClaim(quest.id)}
          className="w-full mt-3 py-2 rounded-[12px] text-sm font-medium text-white"
          style={{ background: '#FF8FA3' }}
        >
          Claim Reward
        </button>
      )}
      {isClaimed && (
        <div className="mt-3 text-center text-sm text-green-500">
          ✓ Claimed
        </div>
      )}
    </div>
  );
};

// Main QuestsScreen
export const QuestsScreen: React.FC = () => {
  const navigate = useNavigate();
  const { isDark, isAmoled } = useTheme();
  const [questSystem] = useState(() => getQuestSystem());
  const [activeTab, setActiveTab] = useState<'chains' | 'daily' | 'weekly'>('chains');
  const [, setRefreshKey] = useState(0);

  // Subscribe to quest state changes
  useEffect(() => {
    const unsubscribe = questSystem.subscribe(() => {
      setRefreshKey(k => k + 1);
    });
    return unsubscribe;
  }, [questSystem]);

  const chains = questSystem.getChains();
  const dailyQuests = questSystem.getDailyQuests();
  const weeklyQuests = questSystem.getWeeklyQuests();
  const level = questSystem.getLevel();
  const xp = questSystem.getXp();
  const { progress, nextThreshold } = questSystem.getXpProgress();

  const handleClaimDaily = (questId: string) => {
    questSystem.claimDaily(questId);
  };

  const handleClaimWeekly = (questId: string) => {
    questSystem.claimWeekly(questId);
  };

  return (
    <div
      className="min-h-screen pb-24"
      style={{ backgroundColor: getBackgroundColor(isDark, isAmoled) }}
    >
      {/* Header */}
      <header className="px-6 pt-12 pb-4 flex items-center justify-between">
        <button
          onClick={() => navigate(-1)}
          className="w-10 h-10 rounded-full flex items-center justify-center"
          style={{
            background: isDark ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.05)',
          }}
        >
          <span style={{ color: getTextColor(isDark) }}>←</span>
        </button>
        <h1
          className="text-2xl font-bold"
          style={{ color: getTextColor(isDark), fontFamily: 'Geom, sans-serif' }}
        >
          Quests
        </h1>
        <div className="w-10" />
      </header>

      {/* Level Card */}
      <div className="px-6 py-4">
        <div
          className="p-4 rounded-[20px]"
          style={{
            background: 'linear-gradient(135deg, #FF8FA3 0%, #FFC2CE 100%)',
          }}
        >
          <div className="flex items-center justify-between mb-2">
            <div>
              <div className="text-white/80 text-sm">Level {level}</div>
              <div className="text-white font-bold text-lg">{questSystem.getLevelTitle(level)}</div>
            </div>
            <div className="text-right">
              <div className="text-white/80 text-sm">{xp} XP</div>
              <div className="text-white text-sm">{nextThreshold} to next level</div>
            </div>
          </div>
          
          <div className="h-2 bg-white/20 rounded-full overflow-hidden">
            <div
              className="h-full bg-white rounded-full transition-all"
              style={{ width: `${progress * 100}%` }}
            />
          </div>
        </div>
      </div>

      {/* Tabs */}
      <div className="px-6 py-2">
        <div className="flex gap-2">
          {(['chains', 'daily', 'weekly'] as const).map((tab) => (
            <button
              key={tab}
              onClick={() => setActiveTab(tab)}
              className="flex-1 py-2 rounded-[12px] text-sm font-medium transition-all"
              style={{
                background: activeTab === tab
                  ? '#FF8FA3'
                  : isDark ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.02)',
                color: activeTab === tab ? 'white' : getTextColor(isDark),
              }}
            >
              {tab.charAt(0).toUpperCase() + tab.slice(1)}
            </button>
          ))}
        </div>
      </div>

      {/* Content */}
      <div className="px-6 py-4">
        {activeTab === 'chains' && (
          <div className="space-y-4">
            {chains.map((chain) => (
              <QuestChainCard
                key={chain.id}
                chain={chain}
                questSystem={questSystem}
              />
            ))}
          </div>
        )}

        {activeTab === 'daily' && (
          <div className="space-y-3">
            <h3 className="font-semibold" style={{ color: getTextColor(isDark) }}>
              Today's Quests
            </h3>
            {dailyQuests.map((quest) => (
              <DailyQuestCard
                key={quest.id}
                quest={quest}
                questSystem={questSystem}
                onClaim={handleClaimDaily}
              />
            ))}
          </div>
        )}

        {activeTab === 'weekly' && (
          <div className="space-y-3">
            <h3 className="font-semibold" style={{ color: getTextColor(isDark) }}>
              This Week's Quests
            </h3>
            {weeklyQuests.map((quest) => (
              <WeeklyQuestCard
                key={quest.id}
                quest={quest}
                questSystem={questSystem}
                onClaim={handleClaimWeekly}
              />
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default QuestsScreen;
