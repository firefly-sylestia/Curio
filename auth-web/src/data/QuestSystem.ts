// Curio Web App - Quest System
// Mirrors Android CurioQuests functionality

import type { CategoryId, CaptureFormat } from '../types';

// Quest Types
export type QuestKind = 
  | 'SPIN' | 'EXPLORE' | 'SAVE' | 'QUOTE' | 'PIN' 
  | 'PROFILE' | 'LIKE' | 'DAILY' | 'ACHIEVEMENT' | 'STREAK'
  | 'FORMATS' | 'LANES' | 'XP';

export type DailyKind = 
  | 'SPIN' | 'EXPLORE' | 'SAVE' | 'QUOTE' | 'PIN' 
  | 'PROFILE' | 'LIKE' | 'DISCOVERY' | 'PLAY';

export type WeeklyKind = 
  | 'SPIN' | 'EXPLORE' | 'SAVE' | 'LANES' | 'QUOTE' | 'PIN' | 'LIKE' | 'PROFILE';

// Quest Stage (chain progression)
export interface QuestStage {
  id: string;
  title: string;
  description: string;
  hint: string;
  xpReward: number;
  kind: QuestKind;
  target: number;
  navRoute?: string;
}

// Quest Chain
export interface QuestChain {
  id: string;
  glyph: string;
  title: string;
  subtitle: string;
  stages: QuestStage[];
}

// Daily Quest
export interface DailyQuest {
  id: string;
  title: string;
  xpReward: number;
  kind: DailyKind;
  target: number;
  bonus: boolean;
}

// Weekly Quest
export interface WeeklyQuest {
  id: string;
  title: string;
  description: string;
  xpReward: number;
  kind: WeeklyKind;
  target: number;
}

// Lifetime Counters
export interface LifetimeCounters {
  spins: number;
  explores: number;
  saves: number;
  quotes: number;
  pins: number;
  likes: number;
  dislikes: number;
  profileVisits: number;
  settingsVisits: number;
  dailyCompleted: number;
}

// Quest State
export interface QuestState {
  xp: number;
  lifetime: LifetimeCounters;
  formats: Set<string>;
  categories: Set<string>;
  awardedStages: Set<string>;
  dailyDate: number;
  dailyProgress: Record<string, number>;
  dailyAwarded: Set<string>;
  bestStreak: number;
  weeklyDate: number;
  weeklyProgress: Record<string, number>;
  weeklyLanes: Set<string>;
  weeklyAwarded: Set<string>;
}

// Level curve - XP thresholds for each level
const XP_THRESHOLDS: number[] = (() => {
  const thresholds: number[] = [0, 15, 40, 80, 135, 205, 290, 390, 505, 635, 780, 940];
  let xp = 940;
  let step = 90;
  while (thresholds.length < 50) {
    step = Math.min(step + 8, 240);
    xp += step;
    thresholds.push(xp);
  }
  return thresholds;
})();

// Level titles
const LEVEL_TITLES: string[] = [
  "First Spark", "Curious Newcomer", "Tuned Ear", "Pattern Spotter", "Comparator",
  "Synthesizer", "Curator", "Master Curator", "Lore Keeper", "Lane Walker",
  "Archive Scholar", "Grand Curator",
  "Wandering Scholar", "Lantern Bearer", "Tome Walker", "Index Dreamer",
  "Shelf Maven", "Margin Dweller", "Footnote Follower", "Chapter Scout",
  "Card Cataloguer", "Stack Surfer", "Dust Jacket Dancer", "Bookplate Baron",
  "Archive Acolyte", "Footnote Philosopher", "Marginalia Monk", "Deck Diviner",
  "Lane Luminator", "Curiosity Conductor", "Wonder Warden", "Spark Shepherd",
  "Knowledge Keeper", "Scribe of Secrets", "Vault Walker", "Atlas Aficionado",
  "Chronology Keeper", "Labyrinth Listener", "Riddle Reader", "Insight Oracle",
  "Truth Tender", "Wisdom Weaver", "Grand Gatherer", "Lore Lord",
  "Myth Keeper", "Archive Sovereign", "Eternal Student", "Curio Champion",
  "Curio Legend", "Curio Sovereign"
];

// Quest Chains
const QUEST_CHAINS: QuestChain[] = [
  {
    id: "deck", glyph: "🎰", title: "The Deck", subtitle: "Spin your way up the ranks",
    stages: [
      { id: "deck-1", title: "First Spin", description: "Spin the deck once", hint: "Tap the Shuffle button on the Spin tab.", xpReward: 10, kind: "SPIN", target: 1, navRoute: "spin" },
      { id: "deck-3", title: "Warming Up", description: "Spin 3 times", hint: "Three shuffles. The deck is getting to know you.", xpReward: 5, kind: "SPIN", target: 3 },
      { id: "deck-5", title: "Deck Regular", description: "Spin 5 times", hint: "Keep shuffling.", xpReward: 15, kind: "SPIN", target: 5 },
      { id: "deck-10", title: "Deck Habit", description: "Spin 10 times", hint: "Double digits.", xpReward: 10, kind: "SPIN", target: 10 },
      { id: "deck-25", title: "Deck Master", description: "Spin 25 times", hint: "A quarter of a century of spins.", xpReward: 25, kind: "SPIN", target: 25 },
    ]
  },
  {
    id: "discovery", glyph: "🔍", title: "Discovery", subtitle: "Go find things in the world",
    stages: [
      { id: "disc-1", title: "First Discovery", description: "Explore your first topic", hint: "Open a topic and tap Explore.", xpReward: 10, kind: "EXPLORE", target: 1 },
      { id: "disc-3", title: "Three Steps Out", description: "Explore 3 topics", hint: "Three deep dives.", xpReward: 5, kind: "EXPLORE", target: 3 },
      { id: "disc-5", title: "Globe Trotter", description: "Explore 5 topics", hint: "Five deep dives under your belt.", xpReward: 15, kind: "EXPLORE", target: 5 },
      { id: "disc-10", title: "Trail Maker", description: "Explore 10 topics", hint: "A trail of your own making.", xpReward: 10, kind: "EXPLORE", target: 10 },
    ]
  },
  {
    id: "keepsakes", glyph: "📦", title: "Keepsakes", subtitle: "Fill your Cabinet",
    stages: [
      { id: "keep-1", title: "First Keepsake", description: "Save your first capture", hint: "Write down what you found.", xpReward: 10, kind: "SAVE", target: 1 },
      { id: "keep-3", title: "Souvenir Seeker", description: "Save 3 captures", hint: "Three keepsakes.", xpReward: 5, kind: "SAVE", target: 3 },
      { id: "keep-5", title: "Notebook Keeper", description: "Save 5 captures", hint: "Five keepsakes in the Cabinet.", xpReward: 15, kind: "SAVE", target: 5 },
      { id: "keep-10", title: "Memory Keeper", description: "Save 10 captures", hint: "A neat row of remembered moments.", xpReward: 10, kind: "SAVE", target: 10 },
    ]
  },
  {
    id: "shelf", glyph: "💬", title: "The Shelf", subtitle: "Save the lines you love",
    stages: [
      { id: "quote-1", title: "Quote Collector", description: "Bookmark your first quote", hint: "Tap the bookmark on any quote.", xpReward: 10, kind: "QUOTE", target: 1 },
      { id: "quote-3", title: "Quote Keeper", description: "Bookmark 3 quotes", hint: "Three lines worth keeping close.", xpReward: 5, kind: "QUOTE", target: 3 },
      { id: "quote-5", title: "Quote Hoarder", description: "Bookmark 5 quotes", hint: "Five lines worth keeping.", xpReward: 15, kind: "QUOTE", target: 5 },
    ]
  },
  {
    id: "flame", glyph: "🔥", title: "The Flame", subtitle: "Keep the streak alive",
    stages: [
      { id: "flame-1", title: "First Warmth", description: "Keep a 1-day streak", hint: "Come back tomorrow.", xpReward: 5, kind: "STREAK", target: 1 },
      { id: "flame-3", title: "Spark Streak", description: "Keep a 3-day streak", hint: "Come back tomorrow, and the day after.", xpReward: 15, kind: "STREAK", target: 3 },
      { id: "flame-7", title: "Week of Wonder", description: "Keep a 7-day streak", hint: "A full week of daily curiosity.", xpReward: 25, kind: "STREAK", target: 7 },
    ]
  },
  {
    id: "rank", glyph: "🏆", title: "The Ladder", subtitle: "Climb the 50 ranks",
    stages: [
      { id: "rank-5", title: "Five Rungs Up", description: "Reach Level 5", hint: "Earn XP from any curious act.", xpReward: 15, kind: "XP", target: XP_THRESHOLDS[4] },
      { id: "rank-10", title: "Lore Keeper", description: "Reach Level 10", hint: "Keep exploring, saving, and spinning.", xpReward: 25, kind: "XP", target: XP_THRESHOLDS[9] },
      { id: "rank-20", title: "Archive Scholar", description: "Reach Level 20", hint: "Halfway up the low ranks.", xpReward: 40, kind: "XP", target: XP_THRESHOLDS[19] },
    ]
  }
];

// Daily Quest Pool
const DAILY_POOL: DailyQuest[] = [
  { id: "d-spin-1", title: "Spin the deck once", xpReward: 15, kind: "SPIN", target: 1, bonus: false },
  { id: "d-spin-3", title: "Spin the deck 3 times", xpReward: 20, kind: "SPIN", target: 3, bonus: false },
  { id: "d-explore-1", title: "Explore a topic", xpReward: 20, kind: "EXPLORE", target: 1, bonus: false },
  { id: "d-save-1", title: "Save a capture", xpReward: 25, kind: "SAVE", target: 1, bonus: false },
  { id: "d-quote-1", title: "Bookmark a quote", xpReward: 15, kind: "QUOTE", target: 1, bonus: false },
  { id: "d-pin-1", title: "Pin a topic for later", xpReward: 15, kind: "PIN", target: 1, bonus: false },
  { id: "d-profile-1", title: "Visit your profile", xpReward: 15, kind: "PROFILE", target: 1, bonus: false },
  { id: "d-like-1", title: "Like a topic", xpReward: 15, kind: "LIKE", target: 1, bonus: false },
  { id: "d-play-1", title: "Play with your pet", xpReward: 15, kind: "PLAY", target: 1, bonus: false },
  // Bonus quests
  { id: "d-b-spin-5", title: "Spin the deck 5 times", xpReward: 30, kind: "SPIN", target: 5, bonus: true },
  { id: "d-b-explore-2", title: "Explore 2 topics", xpReward: 35, kind: "EXPLORE", target: 2, bonus: true },
  { id: "d-b-save-2", title: "Save 2 captures", xpReward: 40, kind: "SAVE", target: 2, bonus: true },
];

// Weekly Quest Pool
const WEEKLY_POOL: WeeklyQuest[] = [
  { id: "w-spin-15", title: "Deck Devotee", description: "Spin the deck 15 times", xpReward: 35, kind: "SPIN", target: 15 },
  { id: "w-explore-7", title: "Seven Days of Wonder", description: "Explore 7 topics", xpReward: 40, kind: "EXPLORE", target: 7 },
  { id: "w-save-3", title: "Keepsake Keeper", description: "Save 3 captures", xpReward: 50, kind: "SAVE", target: 3 },
  { id: "w-lanes-3", title: "Lane Wanderer", description: "Explore 3 different lanes", xpReward: 45, kind: "LANES", target: 3 },
];

// Storage key
const QUEST_STATE_KEY = 'curio-quest-state';

// Get today's epoch day (resets at 4 AM)
const getTodayEpochDay = (): number => {
  const now = new Date();
  if (now.getHours() < 4) {
    now.setDate(now.getDate() - 1);
  }
  return Math.floor(now.getTime() / 86400000);
};

// Get current week key
const getCurrentWeekKey = (): number => {
  const now = new Date();
  const startOfYear = new Date(now.getFullYear(), 0, 1);
  const days = Math.floor((now.getTime() - startOfYear.getTime()) / 86400000);
  const weekNumber = Math.ceil(days / 7);
  return now.getFullYear() * 100 + weekNumber;
};

// Default state
const getDefaultState = (): QuestState => ({
  xp: 0,
  lifetime: {
    spins: 0,
    explores: 0,
    saves: 0,
    quotes: 0,
    pins: 0,
    likes: 0,
    dislikes: 0,
    profileVisits: 0,
    settingsVisits: 0,
    dailyCompleted: 0,
  },
  formats: new Set(),
  categories: new Set(),
  awardedStages: new Set(),
  dailyDate: -1,
  dailyProgress: {},
  dailyAwarded: new Set(),
  bestStreak: 0,
  weeklyDate: -1,
  weeklyProgress: {},
  weeklyLanes: new Set(),
  weeklyAwarded: new Set(),
});

// Quest System Class
export class QuestSystem {
  private state: QuestState;
  private listeners: Set<() => void> = new Set();

  constructor() {
    this.state = this.loadState();
    this.ensureDaily();
    this.ensureWeekly();
    this.awardChainStages();
  }

  // Load state from localStorage
  private loadState(): QuestState {
    try {
      const saved = localStorage.getItem(QUEST_STATE_KEY);
      if (saved) {
        const parsed = JSON.parse(saved);
        return {
          ...parsed,
          formats: new Set(parsed.formats || []),
          categories: new Set(parsed.categories || []),
          awardedStages: new Set(parsed.awardedStages || []),
          dailyAwarded: new Set(parsed.dailyAwarded || []),
          weeklyLanes: new Set(parsed.weeklyLanes || []),
          weeklyAwarded: new Set(parsed.weeklyAwarded || []),
        };
      }
    } catch (error) {
      console.error('Failed to load quest state:', error);
    }
    return getDefaultState();
  }

  // Save state to localStorage
  private saveState(): void {
    try {
      const toSave = {
        ...this.state,
        formats: Array.from(this.state.formats),
        categories: Array.from(this.state.categories),
        awardedStages: Array.from(this.state.awardedStages),
        dailyAwarded: Array.from(this.state.dailyAwarded),
        weeklyLanes: Array.from(this.state.weeklyLanes),
        weeklyAwarded: Array.from(this.state.weeklyAwarded),
      };
      localStorage.setItem(QUEST_STATE_KEY, JSON.stringify(toSave));
    } catch (error) {
      console.error('Failed to save quest state:', error);
    }
  }

  // Subscribe to state changes
  subscribe(listener: () => void): () => void {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  }

  // Notify listeners
  private notify(): void {
    this.listeners.forEach(listener => listener());
  }

  // Get current state
  getState(): QuestState {
    return this.state;
  }

  // Get XP
  getXp(): number {
    return this.state.xp;
  }

  // Get level from XP
  getLevel(xp: number = this.state.xp): number {
    let level = 1;
    XP_THRESHOLDS.forEach((threshold, index) => {
      if (xp >= threshold) level = index + 1;
    });
    return Math.min(level, XP_THRESHOLDS.length);
  }

  // Get level title
  getLevelTitle(level: number = this.getLevel()): string {
    return LEVEL_TITLES[Math.min(level - 1, LEVEL_TITLES.length - 1)];
  }

  // Get XP progress toward next level
  getXpProgress(xp: number = this.state.xp): { progress: number; nextThreshold: number } {
    const level = this.getLevel(xp);
    const lastIndex = XP_THRESHOLDS.length - 1;
    if (level >= XP_THRESHOLDS.length) {
      return { progress: 1, nextThreshold: XP_THRESHOLDS[lastIndex] };
    }
    const from = XP_THRESHOLDS[level - 1];
    const to = XP_THRESHOLDS[level];
    return {
      progress: (xp - from) / Math.max(to - from, 1),
      nextThreshold: to,
    };
  }

  // Get max level
  getMaxLevel(): number {
    return XP_THRESHOLDS.length;
  }

  // Add XP
  private addXp(amount: number): void {
    this.state.xp += amount;
    this.awardChainStages();
    this.saveState();
    this.notify();
  }

  // Ensure daily quests are current
  private ensureDaily(): void {
    const today = getTodayEpochDay();
    if (this.state.dailyDate !== today) {
      this.state.dailyDate = today;
      this.state.dailyProgress = {};
      this.state.dailyAwarded = new Set();
      this.saveState();
    }
  }

  // Ensure weekly quests are current
  private ensureWeekly(): void {
    const weekKey = getCurrentWeekKey();
    if (this.state.weeklyDate !== weekKey) {
      this.state.weeklyDate = weekKey;
      this.state.weeklyProgress = {};
      this.state.weeklyLanes = new Set();
      this.state.weeklyAwarded = new Set();
      this.saveState();
    }
  }

  // Get today's daily quests
  getDailyQuests(): DailyQuest[] {
    const warmups = DAILY_POOL.filter(q => !q.bonus && (q.kind === "SPIN" || q.kind === "EXPLORE" || q.kind === "PROFILE"));
    const creations = DAILY_POOL.filter(q => !q.bonus && (q.kind === "SAVE" || q.kind === "QUOTE" || q.kind === "PIN" || q.kind === "LIKE" || q.kind === "PLAY"));
    const bonusPool = DAILY_POOL.filter(q => q.bonus);

    const today = getTodayEpochDay();
    const warmup = warmups[today % warmups.length];
    const creation = creations[today % creations.length];
    const b0 = bonusPool[today % bonusPool.length];
    const b1 = bonusPool[(today + 1) % bonusPool.length];

    return [warmup, creation, b0, b1];
  }

  // Get this week's weekly quests
  getWeeklyQuests(): WeeklyQuest[] {
    const weekKey = getCurrentWeekKey();
    return [
      WEEKLY_POOL[weekKey % WEEKLY_POOL.length],
      WEEKLY_POOL[(weekKey + 1) % WEEKLY_POOL.length],
      WEEKLY_POOL[(weekKey + 2) % WEEKLY_POOL.length],
    ];
  }

  // Get all quest chains
  getChains(): QuestChain[] {
    return QUEST_CHAINS;
  }

  // Get stage progress
  getStageProgress(stage: QuestStage): number {
    switch (stage.kind) {
      case "SPIN": return this.state.lifetime.spins;
      case "EXPLORE": return this.state.lifetime.explores;
      case "SAVE": return this.state.lifetime.saves;
      case "QUOTE": return this.state.lifetime.quotes;
      case "PIN": return this.state.lifetime.pins;
      case "PROFILE": return this.state.lifetime.profileVisits;
      case "LIKE": return this.state.lifetime.likes;
      case "DAILY": return this.state.lifetime.dailyCompleted;
      case "ACHIEVEMENT": return this.state.awardedStages.size;
      case "STREAK": return this.state.bestStreak;
      case "FORMATS": return this.state.formats.size;
      case "LANES": return this.state.categories.size;
      case "XP": return this.state.xp;
      default: return 0;
    }
  }

  // Check if stage is done
  isStageDone(stage: QuestStage): boolean {
    return this.state.awardedStages.has(stage.id);
  }

  // Award chain stages
  private awardChainStages(): void {
    let changed = true;
    while (changed) {
      changed = false;
      QUEST_CHAINS.forEach(chain => {
        chain.stages.forEach(stage => {
          if (!this.state.awardedStages.has(stage.id) && this.getStageProgress(stage) >= stage.target) {
            this.state.awardedStages.add(stage.id);
            this.state.xp += stage.xpReward;
            changed = true;
          }
        });
      });
    }
  }

  // Get daily progress
  getDailyProgress(kind: DailyKind): number {
    return this.state.dailyProgress[kind] || 0;
  }

  // Get weekly progress
  getWeeklyProgress(quest: WeeklyQuest): number {
    if (quest.kind === "LANES") {
      return this.state.weeklyLanes.size;
    }
    return this.state.weeklyProgress[quest.kind] || 0;
  }

  // Check if daily quest is claimed
  isDailyClaimed(questId: string): boolean {
    return this.state.dailyAwarded.has(questId);
  }

  // Check if weekly quest is claimed
  isWeeklyClaimed(questId: string): boolean {
    return this.state.weeklyAwarded.has(questId);
  }

  // Check if daily quest is complete
  isDailyComplete(quest: DailyQuest): boolean {
    return this.getDailyProgress(quest.kind) >= quest.target;
  }

  // Check if weekly quest is complete
  isWeeklyComplete(quest: WeeklyQuest): boolean {
    return this.getWeeklyProgress(quest) >= quest.target;
  }

  // Event hooks
  onSpin(_categoryId: CategoryId): void {
    this.state.lifetime.spins++;
    this.bumpDaily("SPIN");
    this.bumpWeekly("SPIN");
    this.saveState();
    this.addXp(2);
  }

  onExplore(categoryId: CategoryId): void {
    this.state.lifetime.explores++;
    this.state.categories.add(categoryId);
    this.bumpDaily("EXPLORE");
    this.bumpWeekly("EXPLORE", categoryId);
    this.saveState();
    this.addXp(5);
  }

  onSave(format: CaptureFormat): void {
    this.state.lifetime.saves++;
    this.state.formats.add(format);
    this.bumpDaily("SAVE");
    this.bumpWeekly("SAVE");
    this.saveState();
    this.addXp(10);
  }

  onQuoteSaved(): void {
    this.state.lifetime.quotes++;
    this.bumpDaily("QUOTE");
    this.bumpWeekly("QUOTE");
    this.saveState();
    this.addXp(3);
  }

  onTopicPinned(): void {
    this.state.lifetime.pins++;
    this.bumpDaily("PIN");
    this.bumpWeekly("PIN");
    this.saveState();
    this.addXp(3);
  }

  onTopicLiked(): void {
    this.state.lifetime.likes++;
    this.bumpDaily("LIKE");
    this.bumpWeekly("LIKE");
    this.saveState();
    this.addXp(2);
  }

  onProfileVisited(): void {
    this.state.lifetime.profileVisits++;
    this.bumpDaily("PROFILE");
    this.bumpWeekly("PROFILE");
    this.saveState();
    this.addXp(0);
  }

  onSettingsVisited(): void {
    this.state.lifetime.settingsVisits++;
    this.saveState();
    this.addXp(0);
  }

  onStreakRecorded(streak: number): void {
    if (streak > this.state.bestStreak) {
      this.state.bestStreak = streak;
    }
    this.saveState();
    this.notify();
  }

  // Bump daily progress
  private bumpDaily(kind: DailyKind): void {
    const current = this.state.dailyProgress[kind] || 0;
    this.state.dailyProgress[kind] = current + 1;
  }

  // Bump weekly progress
  private bumpWeekly(kind: WeeklyKind, _categoryId?: CategoryId): void {
    const current = this.state.weeklyProgress[kind] || 0;
    this.state.weeklyProgress[kind] = current + 1;
    if (_categoryId) {
      this.state.weeklyLanes.add(_categoryId);
    }
  }

  // Claim daily quest reward
  claimDaily(questId: string): boolean {
    const quest = this.getDailyQuests().find(q => q.id === questId);
    if (!quest) return false;
    if (this.state.dailyAwarded.has(questId)) return false;
    if (!this.isDailyComplete(quest)) return false;

    this.state.dailyAwarded.add(questId);
    this.state.lifetime.dailyCompleted++;
    this.saveState();
    this.addXp(quest.xpReward);
    return true;
  }

  // Claim weekly quest reward
  claimWeekly(questId: string): boolean {
    const quest = this.getWeeklyQuests().find(q => q.id === questId);
    if (!quest) return false;
    if (this.state.weeklyAwarded.has(questId)) return false;
    if (!this.isWeeklyComplete(quest)) return false;

    this.state.weeklyAwarded.add(questId);
    this.saveState();
    this.addXp(quest.xpReward);
    return true;
  }

  // Get current quest (first incomplete stage)
  getCurrentQuest(): QuestStage | null {
    for (const chain of QUEST_CHAINS) {
      for (const stage of chain.stages) {
        if (!this.isStageDone(stage) && this.getStageProgress(stage) < stage.target) {
          return stage;
        }
      }
    }
    return null;
  }

  // Get lifetime counters
  getLifetime(): LifetimeCounters {
    return this.state.lifetime;
  }

  // Get best streak
  getBestStreak(): number {
    return this.state.bestStreak;
  }

  // Get total XP earned (including from chains)
  getTotalXpEarned(): number {
    let total = this.state.xp;
    QUEST_CHAINS.forEach(chain => {
      chain.stages.forEach(stage => {
        if (this.isStageDone(stage)) {
          total += stage.xpReward;
        }
      });
    });
    return total;
  }
}

// Singleton instance
let questInstance: QuestSystem | null = null;

export const getQuestSystem = (): QuestSystem => {
  if (!questInstance) {
    questInstance = new QuestSystem();
  }
  return questInstance;
};
