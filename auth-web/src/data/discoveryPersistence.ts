import type { CurioTopic, CategoryId } from '../types';
const STATE_KEY = 'curio.shuffle.state.v1';
const RECENTS_KEY = 'curio.shuffle.recents.v1';
export interface ShuffleState { categoryId: CategoryId; allCategories: boolean; subtype: string | null; drawCount: number; topicId: string | null; drawnTopicIds: string[]; previousId: string | null; }
export interface ShuffleRecent { id: string; categoryId: CategoryId; name: string; subtype: string; drawnAt: number; }
export const readShuffleState = (): Partial<ShuffleState> => { try { const value = localStorage.getItem(STATE_KEY); return value ? JSON.parse(value) as Partial<ShuffleState> : {}; } catch { return {}; } };
export const writeShuffleState = (state: ShuffleState): void => { try { localStorage.setItem(STATE_KEY, JSON.stringify(state)); } catch {} };
export const recordShuffleTopics = (topics: CurioTopic[]): void => {
  if (!topics.length) return;
  try {
    const existing = JSON.parse(localStorage.getItem(RECENTS_KEY) || '[]') as ShuffleRecent[];
    const now = Date.now();
    const additions = topics.map((topic, index) => ({ id: topic.id, categoryId: topic.categoryId, name: topic.name, subtype: topic.subtype, drawnAt: now + index }));
    const merged = [...additions, ...existing.filter(item => !additions.some(next => next.id === item.id && next.categoryId === item.categoryId))].slice(0, 30);
    localStorage.setItem(RECENTS_KEY, JSON.stringify(merged));
    window.dispatchEvent(new Event('curio:shuffle-recents-updated'));
  } catch {}
};
export const getShuffleRecents = (): ShuffleRecent[] => { try { const value = JSON.parse(localStorage.getItem(RECENTS_KEY) || '[]'); return Array.isArray(value) ? value.filter(item => item && typeof item.id === 'string' && typeof item.name === 'string' && typeof item.categoryId === 'string' && typeof item.drawnAt === 'number') : []; } catch { return []; } };
