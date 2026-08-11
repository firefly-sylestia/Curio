// Curio Web App - IndexedDB Database
// Mirrors Android Room database

import { openDB } from 'idb';
import type { DBSchema, IDBPDatabase } from 'idb';
import type { CaptureEntity } from '../types';

interface CurioDBSchema extends DBSchema {
  captures: {
    key: string;
    value: CaptureEntity;
    indexes: {
      'by-category': string;
      'by-date': number;
      'by-topic': string;
    };
  };
}

let dbPromise: Promise<IDBPDatabase<CurioDBSchema>> | null = null;

export const getDatabase = (): Promise<IDBPDatabase<CurioDBSchema>> => {
  if (!dbPromise) {
    dbPromise = openDB<CurioDBSchema>('curio-database', 1, {
      upgrade(db) {
        // Create captures store
        const capturesStore = db.createObjectStore('captures', { keyPath: 'id' });
        capturesStore.createIndex('by-category', 'categoryId');
        capturesStore.createIndex('by-date', 'capturedAtMillis');
        capturesStore.createIndex('by-topic', 'topicId');
      },
    });
  }
  return dbPromise;
};

// Capture operations
export const captureRepository = {
  async insert(capture: CaptureEntity): Promise<void> {
    const db = await getDatabase();
    await db.put('captures', capture);
  },

  async delete(id: string): Promise<void> {
    const db = await getDatabase();
    await db.delete('captures', id);
  },

  async getById(id: string): Promise<CaptureEntity | undefined> {
    const db = await getDatabase();
    return db.get('captures', id);
  },

  async getAll(): Promise<CaptureEntity[]> {
    const db = await getDatabase();
    return db.getAllFromIndex('captures', 'by-date');
  },

  async getAllFlow(): Promise<CaptureEntity[]> {
    const db = await getDatabase();
    return db.getAllFromIndex('captures', 'by-date');
  },

  async getByCategory(categoryId: string): Promise<CaptureEntity[]> {
    const db = await getDatabase();
    return db.getAllFromIndex('captures', 'by-category', categoryId);
  },

  async deleteByIds(ids: string[]): Promise<number> {
    const db = await getDatabase();
    const tx = db.transaction('captures', 'readwrite');
    await Promise.all(ids.map(id => tx.store.delete(id)));
    await tx.done;
    return ids.length;
  },

  async count(): Promise<number> {
    const db = await getDatabase();
    return db.count('captures');
  },

  async clearAll(): Promise<number> {
    const db = await getDatabase();
    const tx = db.transaction('captures', 'readwrite');
    const count = await tx.store.count();
    await tx.store.clear();
    await tx.done;
    return count;
  },
};

// Utility to generate unique IDs
export const generateId = (): string => {
  return `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
};

// Parse tags JSON safely
export const deserializeTags = (tagsJson: string): string[] => {
  if (!tagsJson || tagsJson === '[]') return [];
  try {
    const parsed = JSON.parse(tagsJson);
    return Array.isArray(parsed) ? parsed.filter(t => t && t.trim()) : [];
  } catch {
    return [];
  }
};

// Serialize tags to JSON
export const serializeTags = (tags: string[]): string => {
  return JSON.stringify(tags.filter(t => t && t.trim()));
};
