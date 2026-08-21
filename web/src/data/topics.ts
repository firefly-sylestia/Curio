// Curio Web App - Topic Data Loader
// Loads topic data from JSON assets

import type { CurioTopic, CategoryId } from '../types';

// Topic data cache
const topicCache: Map<CategoryId, CurioTopic[]> = new Map();

// Category ID to JSON filename mapping
const CATEGORY_FILE_MAP: Record<CategoryId, string> = {
  ARTISTS: 'artists',
  ALBUMS: 'albums',
  SONGS: 'songs',
  DIRECTORS: 'directors',
  FILMS: 'films',
  ANIMATED_MOVIES: 'animated-movies',
  SERIES: 'series',
  AUTHORS: 'authors',
  BOOKS: 'books',
  PAINTERS: 'painters',
  ARTWORKS: 'artworks',
  SCIENTISTS: 'scientists',
  DISCOVERIES: 'discoveries',
  ANIME: 'anime',
  MANGA: 'manga',
  MANHWA: 'manhwa',
  GAMES: 'games',
  MYTHOLOGY: 'mythology',
  SPORTS: 'sports',
  FOOD: 'food',
  INTERNET: 'internet',
  BIOLOGY: 'biology',
  CHEMISTRY: 'chemistry',
  ANIMALS: 'animals',
  PLANTS: 'plants',
  TECHNOLOGIES: 'technologies',
  ASTRONOMY: 'astronomy',
  HISTORY: 'history',
  GEOLOGY: 'geology',
  MEDICINE: 'medicine',
  PSYCHOLOGY: 'psychology',
  MATHEMATICS: 'mathematics',
  ECONOMICS: 'economics',
  LANGUAGE: 'language',
  ENGINEERING: 'engineering',
  OCEANS: 'oceans',
  QUOTES: 'quotes',
  WILDCARD: 'wildcard',
};

// Load topics for a specific category
export const loadTopicsForCategory = async (categoryId: CategoryId): Promise<CurioTopic[]> => {
  // Check cache first
  if (topicCache.has(categoryId)) {
    return topicCache.get(categoryId) || [];
  }

  const filename = CATEGORY_FILE_MAP[categoryId];
  if (!filename) {
    console.error(`No file mapping for category: ${categoryId}`);
    return [];
  }

  try {
    // Dynamic import of the JSON file
    const module = await import(`./topics/${filename}.json`);
    const rawData = module.default || module;
    
    // Transform to CurioTopic format
    const topics: CurioTopic[] = rawData.map((item: any) => ({
      id: item.id,
      categoryId: item.categoryId as CategoryId,
      subtype: item.subtype || 'Topic',
      name: item.name,
      teaser: item.teaser || '',
      imageUrl: item.imageUrl || '',
      actionPrompt: {
        verb: item.exploreAction?.verb || 'Explore',
        targetName: item.exploreAction?.targetName || item.name,
        durationMinutes: item.exploreAction?.durationMinutes || 30,
        instruction: item.exploreAction?.instruction || 'Take time to explore this topic.',
      },
      aliases: item.aliases || [],
      relatedTopicIds: item.relatedTopicIds || [],
      difficulty: item.tier || item.difficulty || 1,
      weight: item.weight || 100,
      curatedBy: item.curatedBy || 'human',
      curatedDate: item.curatedDate || '',
    }));

    // Cache the loaded topics
    topicCache.set(categoryId, topics);
    
    return topics;
  } catch (error) {
    console.error(`Failed to load topics for ${categoryId}:`, error);
    return [];
  }
};

// Get a random topic from a category
export const getRandomTopic = async (categoryId: CategoryId): Promise<CurioTopic | null> => {
  const topics = await loadTopicsForCategory(categoryId);
  if (topics.length === 0) return null;
  
  // Weighted random selection
  const totalWeight = topics.reduce((sum, t) => sum + t.weight, 0);
  let random = Math.random() * totalWeight;
  
  for (const topic of topics) {
    random -= topic.weight;
    if (random <= 0) return topic;
  }
  
  return topics[topics.length - 1];
};

// Get a random topic from multiple categories
export const getRandomTopicFromCategories = async (
  categoryIds: CategoryId[]
): Promise<CurioTopic | null> => {
  if (categoryIds.length === 0) return null;
  
  const randomCategoryId = categoryIds[Math.floor(Math.random() * categoryIds.length)];
  return getRandomTopic(randomCategoryId);
};

// Search topics by name or aliases
export const searchTopics = async (query: string): Promise<CurioTopic[]> => {
  const results: CurioTopic[] = [];
  const lowerQuery = query.toLowerCase();
  
  // Search across all loaded categories
  for (const [_categoryId, topics] of topicCache.entries()) {
    for (const topic of topics) {
      if (
        topic.name.toLowerCase().includes(lowerQuery) ||
        topic.aliases.some(alias => alias.toLowerCase().includes(lowerQuery)) ||
        topic.teaser.toLowerCase().includes(lowerQuery)
      ) {
        results.push(topic);
      }
    }
  }
  
  return results;
};

// Get topic count for a category
export const getTopicCount = async (categoryId: CategoryId): Promise<number> => {
  const topics = await loadTopicsForCategory(categoryId);
  return topics.length;
};

// Get total topic count across all categories
export const getTotalTopicCount = async (): Promise<number> => {
  let total = 0;
  for (const catId of Object.keys(CATEGORY_FILE_MAP) as CategoryId[]) {
    total += await getTopicCount(catId);
  }
  return total;
};

// Clear topic cache (useful for testing)
export const clearTopicCache = (): void => {
  topicCache.clear();
};
