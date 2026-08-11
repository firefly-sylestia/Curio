// Curio Web App - Quests Screen
// Matches Android: torn hero, level card, chains/daily/weekly tabs

import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTheme, getBackgroundColor, getTextColor } from '../theme/ThemeContext';
import { getQuestSystem } from '../data/QuestSystem';
import type { QuestChain, DailyQuest, WeeklyQuest } from '../data/QuestSystem';
import { TornHero, HOME_HERO_SYMBOLS } from '../components/TornHero';
import { CurioWatermarkBackdrop, MaterialIcon } from '../components/SharedComponents';
import { ScreenEntrance } from '../animations';

const QUESTS_HERO_HEIGHT = 220;
const QUESTS_TEAR_SEED = 0x9E57; // Quests-specific seed
const ROSE_WOOD = '#C46B7C';

const QuestChainCard: React.FC<{ chain: QuestChain; questSystem: ReturnType<typeof getQuestSystem> }> = ({ chain, questSystem }) => {
  const { isDark } = useTheme();
  const completedStages = chain.stages.filter(s => questSystem.isStageDone(s)).length;
  const progress = completedStages / chain.stages.length;
  return (
    <div className="p-4 rounded-2xl" style={{ background: isDark ? 'rgba(255,255,255,0.04)' : 'rgba(59,10,23,0.02)' }}>
      <div className="flex items-center gap-3 mb-3">
        <div className="w-10 h-10 rounded-xl flex items-center justify-center" style={{ background: `${ROSE_WOOD}18` }}>
          <MaterialIcon name={chain.glyph || 'explore'} size={22} style={{ color: ROSE_WOOD }} />
        </div>
        <div className="flex-1">
          <h3 className="font-semibold text-sm" style={{ color: getTextColor(isDark) }}>{chain.title}</h3>
          <p className="text-xs" style={{ color: isDark ? 'rgba(255,255,255,0.4)' : 'rgba(59,10,23,0.4)' }}>{chain.subtitle}</p>
        </div>
        <span className="text-sm font-bold" style={{ color: ROSE_WOOD }}>{completedStages}/{chain.stages.length}</span>
      </div>
      <div className="h-1.5 rounded-full overflow-hidden" style={{ background: isDark ? 'rgba(255,255,255,0.08)' : 'rgba(0,0,0,0.06)' }}>
        <div className="h-full rounded-full transition-all duration-500" style={{ width: `${progress * 100}%`, background: ROSE_WOOD }} />
      </div>
      <div className="mt-3 space-y-1.5">
        {chain.stages.map(stage => {
          const isDone = questSystem.isStageDone(stage);
          const p = questSystem.getStageProgress(stage);
          return (
            <div key={stage.id} className="flex items-center gap-2.5 px-2 py-1.5 rounded-lg"
              style={{ background: isDone ? 'rgba(76,175,80,0.08)' : 'transparent' }}>
              <div className="w-5 h-5 rounded-full flex items-center justify-center text-[10px] font-bold"
                style={{ background: isDone ? '#4CAF50' : isDark ? 'rgba(255,255,255,0.08)' : 'rgba(0,0,0,0.05)', color: isDone ? 'white' : getTextColor(isDark) }}>
                {isDone ? '✓' : ''}
              </div>
              <div className="flex-1 min-w-0">
                <div className="text-xs font-medium truncate" style={{ color: getTextColor(isDark) }}>{stage.title}</div>
              </div>
              <span className="text-[10px] font-mono" style={{ color: isDark ? 'rgba(255,255,255,0.3)' : 'rgba(0,0,0,0.3)' }}>{p}/{stage.target}</span>
            </div>
          );
        })}
      </div>
    </div>
  );
};

const DailyQuestCard: React.FC<{ quest: DailyQuest; questSystem: ReturnType<typeof getQuestSystem>; onClaim: (id: string) => void }> = ({ quest, questSystem, onClaim }) => {
  const { isDark } = useTheme();
  const progress = questSystem.getDailyProgress(quest.kind);
  const isComplete = questSystem.isDailyComplete(quest);
  const isClaimed = questSystem.isDailyClaimed(quest.id);
  const pct = Math.min(progress / quest.target, 1) * 100;
  return (
    <div className="p-4 rounded-2xl" style={{ background: isDark ? 'rgba(255,255,255,0.04)' : 'rgba(59,10,23,0.02)' }}>
      <div className="flex items-center justify-between">
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2">
            <h4 className="text-sm font-semibold truncate" style={{ color: getTextColor(isDark) }}>{quest.title}</h4>
            {quest.bonus && <span className="px-1.5 py-0.5 rounded-full text-[10px] font-bold bg-amber-100 text-amber-700">Bonus</span>}
          </div>
          <div className="flex items-center gap-2 mt-2">
            <div className="flex-1 h-1.5 rounded-full overflow-hidden" style={{ background: isDark ? 'rgba(255,255,255,0.08)' : 'rgba(0,0,0,0.06)' }}>
              <div className="h-full rounded-full transition-all" style={{ width: `${pct}%`, background: isComplete ? '#4CAF50' : ROSE_WOOD }} />
            </div>
            <span className="text-[10px] font-mono" style={{ color: isDark ? 'rgba(255,255,255,0.3)' : 'rgba(0,0,0,0.3)' }}>{progress}/{quest.target}</span>
          </div>
        </div>
        <div className="ml-3 text-right flex-shrink-0">
          <div className="text-sm font-bold" style={{ color: ROSE_WOOD }}>+{quest.xpReward}</div>
          {isComplete && !isClaimed && (
            <button onClick={() => onClaim(quest.id)} className="mt-1 px-3 py-1 rounded-full text-xs font-bold text-white" style={{ background: ROSE_WOOD }}>Claim</button>
          )}
          {isClaimed && <span className="text-xs text-green-500 font-medium">✓ Done</span>}
        </div>
      </div>
    </div>
  );
};

const WeeklyQuestCard: React.FC<{ quest: WeeklyQuest; questSystem: ReturnType<typeof getQuestSystem>; onClaim: (id: string) => void }> = ({ quest, questSystem, onClaim }) => {
  const { isDark } = useTheme();
  const progress = questSystem.getWeeklyProgress(quest);
  const isComplete = questSystem.isWeeklyComplete(quest);
  const isClaimed = questSystem.isWeeklyClaimed(quest.id);
  const pct = Math.min(progress / quest.target, 1) * 100;
  return (
    <div className="p-4 rounded-2xl" style={{ background: isDark ? 'rgba(255,255,255,0.04)' : 'rgba(59,10,23,0.02)' }}>
      <div className="flex items-center justify-between mb-2">
        <h4 className="text-sm font-semibold" style={{ color: getTextColor(isDark) }}>{quest.title}</h4>
        <span className="text-sm font-bold" style={{ color: ROSE_WOOD }}>+{quest.xpReward} XP</span>
      </div>
      <p className="text-xs mb-2" style={{ color: isDark ? 'rgba(255,255,255,0.4)' : 'rgba(59,10,23,0.4)' }}>{quest.description}</p>
      <div className="flex items-center gap-2">
        <div className="flex-1 h-2 rounded-full overflow-hidden" style={{ background: isDark ? 'rgba(255,255,255,0.08)' : 'rgba(0,0,0,0.06)' }}>
          <div className="h-full rounded-full transition-all" style={{ width: `${pct}%`, background: isComplete ? '#4CAF50' : ROSE_WOOD }} />
        </div>
        <span className="text-[10px] font-mono" style={{ color: isDark ? 'rgba(255,255,255,0.3)' : 'rgba(0,0,0,0.3)' }}>{progress}/{quest.target}</span>
      </div>
      {isComplete && !isClaimed && (
        <button onClick={() => onClaim(quest.id)} className="w-full mt-3 py-2.5 rounded-xl text-sm font-bold text-white" style={{ background: ROSE_WOOD }}>Claim Reward</button>
      )}
      {isClaimed && <div className="mt-3 text-center text-sm font-medium text-green-500">✓ Claimed</div>}
    </div>
  );
};

export const QuestsScreen: React.FC = () => {
  const navigate = useNavigate();
  const { isDark, isAmoled } = useTheme();
  const [questSystem] = useState(() => getQuestSystem());
  const [activeTab, setActiveTab] = useState<'chains' | 'daily' | 'weekly'>('daily');
  const [, setRefresh] = useState(0);

  useEffect(() => { const u = questSystem.subscribe(() => setRefresh(k => k + 1)); return u; }, [questSystem]);

  const chains = questSystem.getChains();
  const dailyQuests = questSystem.getDailyQuests();
  const weeklyQuests = questSystem.getWeeklyQuests();
  const level = questSystem.getLevel();
  const xp = questSystem.getXp();
  const { progress, nextThreshold } = questSystem.getXpProgress();
  const claimedDaily = dailyQuests.filter(q => questSystem.isDailyClaimed(q.id)).length;
  const claimedWeekly = weeklyQuests.filter(q => questSystem.isWeeklyClaimed(q.id)).length;

  return (
    <div className="min-h-screen pb-24 relative" style={{ backgroundColor: getBackgroundColor(isDark, isAmoled) }}>
      <CurioWatermarkBackdrop topClearance={QUESTS_HERO_HEIGHT + 30} alphaScale={0.45} />

      <TornHero height={QUESTS_HERO_HEIGHT} fill={ROSE_WOOD} ink="#fff" tearSeed={QUESTS_TEAR_SEED} bold={true} symbols={HOME_HERO_SYMBOLS} isDark={isDark}>
        <div className="flex flex-col h-full px-5 pt-[68px] pb-[18px] justify-end">
          <button onClick={() => navigate(-1)} className="absolute top-0 left-5 w-10 h-10 rounded-full flex items-center justify-center"
            style={{ background: 'rgba(255,255,255,0.18)', border: '1px solid rgba(255,255,255,0.25)', marginTop: 'env(safe-area-inset-top, 12px)' }}>
            <MaterialIcon name="arrow_back" size={20} style={{ color: '#fff' }} />
          </button>
          <div className="rounded-2xl p-3 mt-auto" style={{ background: 'rgba(255,255,255,0.14)', border: '1px solid rgba(255,255,255,0.2)' }}>
            <div className="flex items-center justify-between mb-1">
              <span className="text-white/80 text-xs font-medium">Level {level}</span>
              <span className="text-white/80 text-xs font-medium">{xp} / {nextThreshold} XP</span>
            </div>
            <div className="h-1.5 rounded-full overflow-hidden bg-white/25">
              <div className="h-full bg-white rounded-full transition-all" style={{ width: `${progress * 100}%` }} />
            </div>
          </div>
        </div>
      </TornHero>

      <ScreenEntrance>
        <div className="relative z-10 px-4 pt-4">
          {/* Tabs */}
          <div className="flex gap-2 mb-4">
            {(['daily', 'weekly', 'chains'] as const).map(tab => (
              <button key={tab} onClick={() => setActiveTab(tab)}
                className="flex-1 py-2.5 rounded-xl text-sm font-semibold transition-all"
                style={{ background: activeTab === tab ? ROSE_WOOD : isDark ? 'rgba(255,255,255,0.04)' : 'rgba(0,0,0,0.02)', color: activeTab === tab ? 'white' : getTextColor(isDark) }}>
                {tab === 'daily' ? `Today (${claimedDaily}/${dailyQuests.length})` : tab === 'weekly' ? `This Week (${claimedWeekly}/${weeklyQuests.length})` : `Chains (${chains.length})`}
              </button>
            ))}
          </div>

          {/* Tab content */}
          <div className="space-y-3 pb-8">
            {activeTab === 'daily' && dailyQuests.map(q => <DailyQuestCard key={q.id} quest={q} questSystem={questSystem} onClaim={id => questSystem.claimDaily(id)} />)}
            {activeTab === 'weekly' && weeklyQuests.map(q => <WeeklyQuestCard key={q.id} quest={q} questSystem={questSystem} onClaim={id => questSystem.claimWeekly(id)} />)}
            {activeTab === 'chains' && chains.map(c => <QuestChainCard key={c.id} chain={c} questSystem={questSystem} />)}
          </div>
        </div>
      </ScreenEntrance>
    </div>
  );
};

export default QuestsScreen;
