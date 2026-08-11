// Curio Web App - Simplified Pet System
// Pet companion that grows with the user's level

import { getQuestSystem } from './QuestSystem';

// Pet Stages
export type PetStage = 'baby' | 'evolved' | 'mature';

// Pet State
export interface PetState {
  name: string;
  stage: PetStage;
  lastInteraction: number;
  mood: 'happy' | 'excited' | 'sleepy' | 'curious';
  lastDialogueIndex: Record<string, number>;
}

// Pet dialogues by stage and mood
const PET_DIALOGUES: Record<PetStage, Record<string, string[]>> = {
  baby: {
    happy: ['*happy wiggle*', '*bouncy bounce*', 'Play!', 'Yay!', 'Friend!'],
    excited: ['Ooh! Ooh!', '*spins around*', 'New thing!', 'Want!', 'More!'],
    sleepy: ['*yawns*', 'Nap time...', '*cuddles*', 'Soft...', 'Zzz...'],
    curious: ['What\'s that?', '*tilts head*', 'Hmm...', '*peeks*', 'Explore?'],
  },
  evolved: {
    happy: ['*happy chirp*', 'We did it!', 'Great find!', 'Wonderful!', 'Together!'],
    excited: ['Amazing!', '*hops excitedly*', 'Look at that!', 'So cool!', 'Wow!'],
    sleepy: ['*stretch*', 'Rest time...', 'Good work today...', '*settles in*', 'Peaceful...'],
    curious: ['Let\'s discover!', '*looks around*', 'What shall we find?', 'Interesting...', 'Explore more?'],
  },
  mature: {
    happy: ['*content sigh*', 'Well done.', 'A good journey.', 'Satisfying.', 'We grow together.'],
    excited: ['How fascinating!', '*nods wisely*', 'There\'s always more.', 'Wonder never fades.', 'Curiosity rewards.'],
    sleepy: ['*gentle yawn*', 'Time to reflect.', 'Rest feeds the mind.', '*peaceful silence*', 'Tomorrow brings new wonders.'],
    curious: ['The world is vast.', '*thoughtful pause*', 'What shall we learn?', 'Every topic teaches.', 'Shall we explore?'],
  },
};

// Pet emojis by stage
const PET_EMOJIS: Record<PetStage, string[]> = {
  baby: ['🐣', '🐥', '🐾', '✨'],
  evolved: ['🦊', '🦉', '🐱', '🌟'],
  mature: ['🐉', '🦅', '🐺', '💫'],
};

// Storage key
const PET_STATE_KEY = 'curio-pet-state';

// Get pet stage from level
const getStageFromLevel = (level: number): PetStage => {
  if (level < 7) return 'baby';
  if (level < 25) return 'evolved';
  return 'mature';
};

// Get random dialogue
const getRandomDialogue = (stage: PetStage, mood: string, lastIndex: number): { dialogue: string; newIndex: number } => {
  const dialogues = PET_DIALOGUES[stage][mood] || PET_DIALOGUES[stage].happy;
  const newIndex = (lastIndex + 1) % dialogues.length;
  return { dialogue: dialogues[newIndex], newIndex };
};

// Get random pet emoji
const getRandomEmoji = (stage: PetStage): string => {
  const emojis = PET_EMOJIS[stage];
  return emojis[Math.floor(Math.random() * emojis.length)];
};

// Default state
const getDefaultState = (): PetState => ({
  name: 'Curie',
  stage: 'baby',
  lastInteraction: Date.now(),
  mood: 'happy',
  lastDialogueIndex: {},
});

// Pet System Class
export class PetSystem {
  private state: PetState;
  private listeners: Set<() => void> = new Set();
  private floatingVisible: boolean = true;
  private currentDialogue: string | null = null;
  private dialogueTimeout: ReturnType<typeof setTimeout> | null = null;

  constructor() {
    this.state = this.loadState();
    this.updateStage();
  }

  // Load state from localStorage
  private loadState(): PetState {
    try {
      const saved = localStorage.getItem(PET_STATE_KEY);
      if (saved) {
        return JSON.parse(saved);
      }
    } catch (error) {
      console.error('Failed to load pet state:', error);
    }
    return getDefaultState();
  }

  // Save state to localStorage
  private saveState(): void {
    try {
      localStorage.setItem(PET_STATE_KEY, JSON.stringify(this.state));
    } catch (error) {
      console.error('Failed to save pet state:', error);
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

  // Update pet stage based on level
  updateStage(): void {
    const questSystem = getQuestSystem();
    const level = questSystem.getLevel();
    const newStage = getStageFromLevel(level);
    
    if (newStage !== this.state.stage) {
      this.state.stage = newStage;
      this.saveState();
      this.notify();
    }
  }

  // Get current state
  getState(): PetState {
    return this.state;
  }

  // Get pet stage
  getStage(): PetStage {
    return this.state.stage;
  }

  // Get pet emoji
  getEmoji(): string {
    return getRandomEmoji(this.state.stage);
  }

  // Get current dialogue
  getCurrentDialogue(): string | null {
    return this.currentDialogue;
  }

  // Show dialogue
  showDialogue(dialogue?: string): void {
    if (dialogue) {
      this.currentDialogue = dialogue;
    } else {
      const mood = this.getMood();
      const lastIndex = this.state.lastDialogueIndex[mood] || 0;
      const result = getRandomDialogue(this.state.stage, mood, lastIndex);
      this.currentDialogue = result.dialogue;
      this.state.lastDialogueIndex[mood] = result.newIndex;
    }
    
    this.state.lastInteraction = Date.now();
    this.saveState();
    this.notify();
    
    // Auto-hide dialogue after 3 seconds
    if (this.dialogueTimeout) {
      clearTimeout(this.dialogueTimeout);
    }
    this.dialogueTimeout = setTimeout(() => {
      this.currentDialogue = null;
      this.notify();
    }, 3000);
  }

  // Hide dialogue
  hideDialogue(): void {
    this.currentDialogue = null;
    if (this.dialogueTimeout) {
      clearTimeout(this.dialogueTimeout);
    }
    this.notify();
  }

  // Get mood based on time and activity
  getMood(): string {
    const hour = new Date().getHours();
    const lastInteraction = this.state.lastInteraction;
    const timeSinceInteraction = Date.now() - lastInteraction;
    
    // Night time - sleepy
    if (hour >= 22 || hour < 6) {
      return 'sleepy';
    }
    
    // Just interacted - excited
    if (timeSinceInteraction < 60000) { // Less than 1 minute
      return 'excited';
    }
    
    // Morning - curious
    if (hour >= 6 && hour < 12) {
      return 'curious';
    }
    
    // Default - happy
    return 'happy';
  }

  // Interact with pet (tap)
  interact(): void {
    this.state.lastInteraction = Date.now();
    this.showDialogue();
  }

  // Get floating visibility
  isFloatingVisible(): boolean {
    return this.floatingVisible;
  }

  // Toggle floating visibility
  toggleFloating(): void {
    this.floatingVisible = !this.floatingVisible;
    this.notify();
  }

  // Get stage name
  getStageName(): string {
    switch (this.state.stage) {
      case 'baby': return 'Baby';
      case 'evolved': return 'Evolved';
      case 'mature': return 'Mature';
    }
  }

  // Get next stage info
  getNextStageInfo(): { stage: PetStage; requiredLevel: number } | null {
    switch (this.state.stage) {
      case 'baby':
        return { stage: 'evolved', requiredLevel: 7 };
      case 'evolved':
        return { stage: 'mature', requiredLevel: 25 };
      case 'mature':
        return null;
    }
  }
}

// Singleton instance
let petInstance: PetSystem | null = null;

export const getPetSystem = (): PetSystem => {
  if (!petInstance) {
    petInstance = new PetSystem();
  }
  return petInstance;
};
