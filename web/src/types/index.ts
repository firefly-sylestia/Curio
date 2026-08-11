// Curio Web App - Type Definitions
// Mirrors Android app's data models

// Category System
export type CategoryId = 
  | 'ARTISTS' | 'ALBUMS' | 'SONGS'
  | 'DIRECTORS' | 'FILMS' | 'SERIES'
  | 'AUTHORS' | 'BOOKS'
  | 'PAINTERS' | 'ARTWORKS'
  | 'SCIENTISTS' | 'DISCOVERIES'
  | 'ANIME' | 'MANGA' | 'MANHWA'
  | 'GAMES'
  | 'MYTHOLOGY'
  | 'SPORTS'
  | 'FOOD'
  | 'INTERNET'
  | 'WILDCARD';

export type CategoryFamily = 
  | 'MUSIC' | 'MOVIES' | 'BOOKS' | 'VISUAL_ART' | 'SCIENCE'
  | 'ANIME_COMICS' | 'GAMES' | 'MYTHOLOGY' | 'SPORTS' | 'FOOD'
  | 'INTERNET' | 'WILDCARD';

export interface CurioCategory {
  id: CategoryId;
  displayName: string;
  accent: string;
  lightAccent: string;
  tint: string;
  iconGlyph: string;
  family: CategoryFamily;
  defaultFormat: CaptureFormat;
  isHidden: boolean;
  isReady: boolean;
}

// Capture Formats
export type CaptureFormat = 
  | 'SoundBite' | 'ReelNotes' | 'Marginalia' 
  | 'GalleryWall' | 'FieldNotes' | 'OpenNotebook';

// Topic Data
export interface ExploreAction {
  verb: string;
  targetName: string;
  durationMinutes: number;
  instruction: string;
}

export interface CurioTopic {
  id: string;
  categoryId: CategoryId;
  subtype: string;
  name: string;
  teaser: string;
  imageUrl: string;
  actionPrompt: ExploreAction;
  aliases: string[];
  relatedTopicIds: string[];
  difficulty: number;
  weight: number;
  curatedBy: string;
  curatedDate: string;
}

// Journal mood — matches Android JournalMood enum
export type JournalMood = 'CALM' | 'HAPPY' | 'CURIOUS' | 'INSPIRED' | 'TIRED' | 'OVERWHELMED';

export const JOURNAL_MOODS: Array<{ id: JournalMood; label: string; icon: string }> = [
  { id: 'CALM', label: 'Calm', icon: 'self_improvement' },
  { id: 'HAPPY', label: 'Happy', icon: 'sentiment_satisfied' },
  { id: 'CURIOUS', label: 'Curious', icon: 'psychology' },
  { id: 'INSPIRED', label: 'Inspired', icon: 'emoji_objects' },
  { id: 'TIRED', label: 'Tired', icon: 'bedtime' },
  { id: 'OVERWHELMED', label: 'Overwhelmed', icon: 'waves' },
];

// Capture Data Types
export interface SoundBiteData {
  durationSeconds: number;
  title: string;
  note: string;
  audioFilePath: string | null;
  audioDataUrl: string | null;
  fileSizeBytes: number;
  mood: JournalMood | null;
  quotes: Array<{ text: string; context?: string }>;
  // Note paper styling
  titleStyle: 'ruled' | 'torn' | 'tornRuled';
  noteStyle: 'ruled' | 'torn' | 'tornRuled';
  titleColor: 'cream' | 'white' | 'kraft';
  noteColor: 'cream' | 'white' | 'kraft';
}

export interface ReelNotesData {
  rating: number;
  review: string;
  mood: JournalMood | null;
  favoriteQuote?: number;
}

export interface MarginaliaData {
  journalEntry: string;
  quotes: Array<{
    text: string;
    context?: string;
  }>;
  mood: JournalMood | null;
}

export interface GalleryWallData {
  caption: string;
  images: string[];
  mood: JournalMood | null;
}

export interface FieldNotesData {
  observed: string;
  surprised: string;
  learnNext: string;
  mood: JournalMood | null;
}

export interface OpenNotebookData {
  subFormat: CaptureFormat;
  subData: CaptureData;
}

export type CaptureData = 
  | SoundBiteData 
  | ReelNotesData 
  | MarginaliaData 
  | GalleryWallData 
  | FieldNotesData 
  | OpenNotebookData;

// Capture Entity
export interface CaptureEntity {
  id: string;
  topicId: string;
  categoryId: string;
  topicName: string;
  topicSubtype: string;
  topicTeaser: string;
  format: CaptureFormat;
  capturedAtMillis: number;
  title: string | null;
  formatDataJson: string;
  tagsJson: string;
  isLegacy: boolean;
}

// Curio Entry (domain object)
export interface CurioEntry {
  id: string;
  topic: CurioTopic;
  format: CaptureFormat;
  captureData: CaptureData;
  title: string | null;
  capturedAtMillis: number;
  tags: string[];
  isLegacy: boolean;
}

// Pet System
export type PetStage = 'BABY' | 'FIRST_EVO' | 'FINAL_EVO';

export interface PetDefinition {
  id: string;
  name: string;
  species: string;
  stage: PetStage;
  level: number;
  xp: number;
  evoPath: string;
  design: string; // Serialized pet design
}

// Quest System
export type QuestType = 'DAILY' | 'WEEKLY';

export interface Quest {
  id: string;
  type: QuestType;
  name: string;
  description: string;
  target: number;
  progress: number;
  completed: boolean;
  claimed: boolean;
  xpReward: number;
}

// Theme System
export type ThemeStyle = 'curio' | 'amoled' | 'material';

export interface ThemeSettings {
  style: ThemeStyle;
  isDark: boolean;
  pastelColors: boolean;
  tintWash: boolean;
  heroGradient: boolean;
}

// User Preferences
export interface UserPreferences {
  theme: ThemeSettings;
  hiddenCategories: CategoryId[];
  categoryOrder: CategoryId[];
  petChatter: 'quiet' | 'cozy' | 'talkative';
  petGameFrequency: 'relaxed' | 'normal' | 'eager';
}

// Navigation
export type Route = 
  | 'home' | 'spin' | 'cabinet'
  | 'profile' | 'settings'
  | 'reveal' | 'capture' | 'detail'
  | 'onboarding' | 'splash';
