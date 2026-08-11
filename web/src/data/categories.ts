// Curio Web App - Category Data
// Mirrors Android app's CurioCategories.all

import type { CurioCategory, CategoryId, CategoryFamily, CaptureFormat } from '../types';

const CATEGORY_COLORS: Record<string, { accent: string; ink: string; tint: string }> = {
  ARTISTS:     { accent: '#4338CA', ink: '#A5B4FC', tint: 'rgba(67, 56, 202, 0.20)' },
  ALBUMS:      { accent: '#5F4DCB', ink: '#A5B4FC', tint: 'rgba(95, 77, 203, 0.20)' },
  SONGS:       { accent: '#0E7490', ink: '#67E8F9', tint: 'rgba(14, 116, 144, 0.20)' },
  DIRECTORS:   { accent: '#BE123C', ink: '#FDA4AF', tint: 'rgba(190, 18, 60, 0.20)' },
  FILMS:       { accent: '#BE123C', ink: '#FDA4AF', tint: 'rgba(190, 18, 60, 0.20)' },
  SERIES:      { accent: '#BE185D', ink: '#F9A8D4', tint: 'rgba(190, 24, 93, 0.20)' },
  AUTHORS:     { accent: '#B45309', ink: '#FCD34D', tint: 'rgba(180, 83, 9, 0.20)' },
  BOOKS:       { accent: '#B45309', ink: '#FCD34D', tint: 'rgba(180, 83, 9, 0.20)' },
  PAINTERS:    { accent: '#0F766E', ink: '#5EEAD4', tint: 'rgba(15, 118, 110, 0.20)' },
  ARTWORKS:    { accent: '#0F766E', ink: '#5EEAD4', tint: 'rgba(15, 118, 110, 0.20)' },
  SCIENTISTS:  { accent: '#0369A1', ink: '#7DD3FC', tint: 'rgba(3, 105, 161, 0.20)' },
  DISCOVERIES: { accent: '#0369A1', ink: '#7DD3FC', tint: 'rgba(3, 105, 161, 0.20)' },
  ANIME:       { accent: '#7E22CE', ink: '#C4B5FD', tint: 'rgba(126, 34, 206, 0.20)' },
  MANGA:       { accent: '#5B21B6', ink: '#A78BFA', tint: 'rgba(91, 33, 182, 0.20)' },
  MANHWA:      { accent: '#9333EA', ink: '#D8B4FE', tint: 'rgba(147, 51, 234, 0.20)' },
  GAMES:       { accent: '#A21CAF', ink: '#F0ABFC', tint: 'rgba(162, 28, 175, 0.20)' },
  MYTHOLOGY:   { accent: '#C2410C', ink: '#FDBA74', tint: 'rgba(194, 65, 12, 0.20)' },
  SPORTS:      { accent: '#047857', ink: '#6EE7B7', tint: 'rgba(4, 120, 87, 0.20)' },
  FOOD:        { accent: '#B91C1C', ink: '#FCA5A5', tint: 'rgba(185, 28, 28, 0.20)' },
  INTERNET:    { accent: '#1D4ED8', ink: '#93C5FD', tint: 'rgba(29, 78, 216, 0.20)' },
  WILDCARD:    { accent: '#FF8FA3', ink: '#FFC2CE', tint: 'rgba(255, 143, 163, 0.20)' },
};

const CATEGORY_ICONS: Record<CategoryId, string> = {
  ARTISTS: 'person',
  ALBUMS: 'album',
  SONGS: 'queue_music',
  DIRECTORS: 'videocam',
  FILMS: 'movie',
  SERIES: 'tv',
  AUTHORS: 'edit_note',
  BOOKS: 'menu_book',
  PAINTERS: 'brush',
  ARTWORKS: 'palette',
  SCIENTISTS: 'science',
  DISCOVERIES: 'lightbulb',
  ANIME: 'smart_display',
  MANGA: 'auto_stories',
  MANHWA: 'import_contacts',
  GAMES: 'sports_esports',
  MYTHOLOGY: 'auto_awesome',
  SPORTS: 'sports_soccer',
  FOOD: 'restaurant',
  INTERNET: 'public',
  WILDCARD: 'casino',
};

const CATEGORY_FAMILIES: Record<CategoryId, CategoryFamily> = {
  ARTISTS: 'MUSIC',
  ALBUMS: 'MUSIC',
  SONGS: 'MUSIC',
  DIRECTORS: 'MOVIES',
  FILMS: 'MOVIES',
  SERIES: 'MOVIES',
  AUTHORS: 'BOOKS',
  BOOKS: 'BOOKS',
  PAINTERS: 'VISUAL_ART',
  ARTWORKS: 'VISUAL_ART',
  SCIENTISTS: 'SCIENCE',
  DISCOVERIES: 'SCIENCE',
  ANIME: 'ANIME_COMICS',
  MANGA: 'ANIME_COMICS',
  MANHWA: 'ANIME_COMICS',
  GAMES: 'GAMES',
  MYTHOLOGY: 'MYTHOLOGY',
  SPORTS: 'SPORTS',
  FOOD: 'FOOD',
  INTERNET: 'INTERNET',
  WILDCARD: 'WILDCARD',
};

const DEFAULT_FORMATS: Record<CategoryId, CaptureFormat> = {
  ARTISTS: 'SoundBite',
  ALBUMS: 'ReelNotes',
  SONGS: 'SoundBite',
  DIRECTORS: 'ReelNotes',
  FILMS: 'Marginalia',
  SERIES: 'Marginalia',
  AUTHORS: 'Marginalia',
  BOOKS: 'Marginalia',
  PAINTERS: 'GalleryWall',
  ARTWORKS: 'GalleryWall',
  SCIENTISTS: 'FieldNotes',
  DISCOVERIES: 'FieldNotes',
  ANIME: 'Marginalia',
  MANGA: 'Marginalia',
  MANHWA: 'Marginalia',
  GAMES: 'ReelNotes',
  MYTHOLOGY: 'FieldNotes',
  SPORTS: 'ReelNotes',
  FOOD: 'GalleryWall',
  INTERNET: 'OpenNotebook',
  WILDCARD: 'OpenNotebook',
};

const CATEGORY_DISPLAY_NAMES: Record<CategoryId, string> = {
  ARTISTS: 'Artists',
  ALBUMS: 'Albums',
  SONGS: 'Songs',
  DIRECTORS: 'Directors',
  FILMS: 'Films',
  SERIES: 'Series',
  AUTHORS: 'Authors',
  BOOKS: 'Books',
  PAINTERS: 'Painters',
  ARTWORKS: 'Artworks',
  SCIENTISTS: 'Scientists',
  DISCOVERIES: 'Discoveries',
  ANIME: 'Anime',
  MANGA: 'Manga',
  MANHWA: 'Manhwa',
  GAMES: 'Games',
  MYTHOLOGY: 'Mythology',
  SPORTS: 'Sports',
  FOOD: 'Food',
  INTERNET: 'Internet',
  WILDCARD: 'Wildcard',
};

export const DEFAULT_ORDER: CategoryId[] = [
  'ARTISTS', 'ALBUMS', 'SONGS',
  'DIRECTORS', 'FILMS', 'SERIES',
  'AUTHORS', 'BOOKS',
  'PAINTERS', 'ARTWORKS',
  'SCIENTISTS', 'DISCOVERIES',
  'ANIME', 'MANGA', 'MANHWA',
  'GAMES',
  'MYTHOLOGY',
  'SPORTS',
  'FOOD',
  'INTERNET',
  'WILDCARD',
];

export const ALL_CATEGORIES: CurioCategory[] = DEFAULT_ORDER.map(id => ({
  id,
  displayName: CATEGORY_DISPLAY_NAMES[id],
  accent: CATEGORY_COLORS[id].accent,
  lightAccent: CATEGORY_COLORS[id].ink,
  tint: CATEGORY_COLORS[id].tint,
  iconGlyph: CATEGORY_ICONS[id],
  family: CATEGORY_FAMILIES[id],
  defaultFormat: DEFAULT_FORMATS[id],
  isHidden: false,
  isReady: true, // All categories ready in web version
}));

export const getCategoryById = (id: CategoryId): CurioCategory => {
  const category = ALL_CATEGORIES.find(c => c.id === id);
  if (!category) throw new Error(`Category ${id} not found`);
  return category;
};

export const getCategoryBySlug = (slug: string): CurioCategory | undefined => {
  return ALL_CATEGORIES.find(c => c.id.toLowerCase() === slug);
};

export const getFamilyCategories = (family: CategoryFamily): CurioCategory[] => {
  return ALL_CATEGORIES.filter(c => c.family === family);
};

// Gradient helpers
export const getCategoryGradient = (category: CurioCategory): string => {
  return `linear-gradient(135deg, ${category.accent} 0%, ${category.lightAccent} 100%)`;
};

export const getCategoryBgClass = (category: CurioCategory): string => {
  return `bg-gradient-to-br from-[${category.accent}] to-[${category.lightAccent}]`;
};
