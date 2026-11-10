// Curio Web App - Explore Session System
// Tracks exploration time and provides reminders

import type { CategoryId, CurioTopic } from '../types';
import { getQuestSystem } from './QuestSystem';

// Explore Session
export interface ExploreSession {
  id: string;
  topic: CurioTopic;
  categoryId: CategoryId;
  startMillis: number;
  paused: boolean;
  pausedAtMillis: number;
  elapsedMillis: number;
}

// Storage key
const SESSION_KEY = 'curio-explore-session';

// Generate unique ID
const generateId = (): string => {
  return `session-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
};

// Explore Session System
export class ExploreSessionSystem {
  private session: ExploreSession | null = null;
  private listeners: Set<() => void> = new Set();
  private timerInterval: ReturnType<typeof setInterval> | null = null;

  constructor() {
    this.loadSession();
    this.startTimer();
  }

  // Load session from localStorage
  private loadSession(): void {
    try {
      const saved = localStorage.getItem(SESSION_KEY);
      if (saved) {
        this.session = JSON.parse(saved);
        // If session was running, calculate elapsed time
        if (this.session && !this.session.paused) {
          const now = Date.now();
          this.session.elapsedMillis += now - this.session.startMillis;
          this.session.startMillis = now;
        }
      }
    } catch (error) {
      console.error('Failed to load explore session:', error);
    }
  }

  // Save session to localStorage
  private saveSession(): void {
    try {
      if (this.session) {
        localStorage.setItem(SESSION_KEY, JSON.stringify(this.session));
      } else {
        localStorage.removeItem(SESSION_KEY);
      }
    } catch (error) {
      console.error('Failed to save explore session:', error);
    }
  }

  // Start timer for elapsed time
  private startTimer(): void {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
    }
    
    this.timerInterval = setInterval(() => {
      if (this.session && !this.session.paused) {
        this.session.elapsedMillis = Date.now() - this.session.startMillis;
        this.notify();
      }
    }, 1000);
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

  // Get current session
  getSession(): ExploreSession | null {
    return this.session;
  }

  // Check if session is active
  isActive(): boolean {
    return this.session !== null;
  }

  // Start a new session
  startSession(topic: CurioTopic, categoryId: CategoryId): ExploreSession {
    // End any existing session
    this.endSession();
    
    const newSession: ExploreSession = {
      id: generateId(),
      topic,
      categoryId,
      startMillis: Date.now(),
      paused: false,
      pausedAtMillis: 0,
      elapsedMillis: 0,
    };
    
    this.session = newSession;
    this.saveSession();
    this.notify();
    
    // Track in quest system
    const questSystem = getQuestSystem();
    questSystem.onExplore(categoryId);
    
    return newSession;
  }

  // Pause the session
  pause(): void {
    if (this.session && !this.session.paused) {
      this.session.paused = true;
      this.session.pausedAtMillis = Date.now();
      this.saveSession();
      this.notify();
    }
  }

  // Resume the session
  resume(): void {
    if (this.session && this.session.paused) {
      const pausedDuration = Date.now() - this.session.pausedAtMillis;
      this.session.startMillis += pausedDuration;
      this.session.paused = false;
      this.session.pausedAtMillis = 0;
      this.saveSession();
      this.notify();
    }
  }

  // End the session
  endSession(): void {
    if (this.session) {
      // Final elapsed time calculation
      if (!this.session.paused) {
        this.session.elapsedMillis = Date.now() - this.session.startMillis;
      }
      
      // Save final state
      this.saveSession();
      
      // Clear session
      this.session = null;
      localStorage.removeItem(SESSION_KEY);
      
      this.notify();
    }
  }

  // Get formatted elapsed time
  getFormattedElapsed(): string {
    if (!this.session) return '0:00';
    
    const totalSeconds = Math.floor(this.session.elapsedMillis / 1000);
    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;
    
    return `${minutes}:${seconds.toString().padStart(2, '0')}`;
  }

  // Get elapsed minutes
  getElapsedMinutes(): number {
    if (!this.session) return 0;
    return Math.floor(this.session.elapsedMillis / 60000);
  }

  // Check if session exceeds target duration
  exceedsTarget(): boolean {
    if (!this.session) return false;
    return this.getElapsedMinutes() >= this.session.topic.actionPrompt.durationMinutes;
  }

  // Get progress toward target (0-1)
  getProgress(): number {
    if (!this.session) return 0;
    const target = this.session.topic.actionPrompt.durationMinutes;
    if (target <= 0) return 1;
    return Math.min(this.getElapsedMinutes() / target, 1);
  }

  // Cleanup
  destroy(): void {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
    }
  }
}

// Singleton instance
let sessionInstance: ExploreSessionSystem | null = null;

export const getExploreSessionSystem = (): ExploreSessionSystem => {
  if (!sessionInstance) {
    sessionInstance = new ExploreSessionSystem();
  }
  return sessionInstance;
};
