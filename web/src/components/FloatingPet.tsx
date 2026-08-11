// Curio Web App - Floating Pet Component
// A floating companion that reacts to user actions

import React, { useState, useEffect } from 'react';
import { getPetSystem } from '../data/PetSystem';
import type { PetStage } from '../data/PetSystem';
import { getQuestSystem } from '../data/QuestSystem';
import { useTheme } from '../theme/ThemeContext';

// Pet sprite component
const PetSprite: React.FC<{
  stage: PetStage;
  mood: string;
  onClick: () => void;
  isHovered: boolean;
}> = ({ stage, mood, onClick, isHovered }) => {
  const { isDark } = useTheme();
  
  // Get emoji based on stage and mood
  const getEmoji = (): string => {
    const emojis: Record<PetStage, Record<string, string>> = {
      baby: {
        happy: '🐣',
        excited: '🐥',
        sleepy: '😴',
        curious: '🐣',
      },
      evolved: {
        happy: '🦊',
        excited: '🦉',
        sleepy: '😴',
        curious: '🦊',
      },
      mature: {
        happy: '🐉',
        excited: '🦅',
        sleepy: '😴',
        curious: '🐉',
      },
    };
    return emojis[stage]?.[mood] || emojis[stage]?.happy || '🐣';
  };

  // Get size based on stage
  const getSize = (): number => {
    switch (stage) {
      case 'baby': return 48;
      case 'evolved': return 56;
      case 'mature': return 64;
    }
  };

  // Get animation class based on mood
  const getAnimationClass = (): string => {
    switch (mood) {
      case 'excited': return 'animate-bounce';
      case 'sleepy': return 'animate-pulse';
      case 'curious': return 'animate-pulse';
      default: return 'animate-float';
    }
  };

  return (
    <button
      onClick={onClick}
      className={`relative ${getAnimationClass()} transition-transform ${isHovered ? 'scale-110' : ''}`}
      style={{
        width: getSize(),
        height: getSize(),
        fontSize: getSize() * 0.6,
        lineHeight: `${getSize()}px`,
        textAlign: 'center',
        filter: isDark ? 'drop-shadow(0 0 8px rgba(255,255,255,0.3))' : 'drop-shadow(0 2px 4px rgba(0,0,0,0.2))',
      }}
      title="Tap to interact with Curie"
    >
      {getEmoji()}
    </button>
  );
};

// Dialogue bubble component
const DialogueBubble: React.FC<{
  text: string;
  stage: PetStage;
}> = ({ text, stage }) => {
  const { isDark } = useTheme();
  
  // Get bubble color based on stage
  const getBubbleColor = (): string => {
    switch (stage) {
      case 'baby': return isDark ? 'rgba(255,182,193,0.9)' : 'rgba(255,255,255,0.95)';
      case 'evolved': return isDark ? 'rgba(144,238,144,0.9)' : 'rgba(255,255,255,0.95)';
      case 'mature': return isDark ? 'rgba(173,216,230,0.9)' : 'rgba(255,255,255,0.95)';
    }
  };

  return (
    <div
      className="absolute bottom-full left-1/2 transform -translate-x-1/2 mb-2 px-3 py-2 rounded-xl whitespace-nowrap animate-fade-in"
      style={{
        background: getBubbleColor(),
        boxShadow: '0 2px 8px rgba(0,0,0,0.15)',
        maxWidth: '200px',
        whiteSpace: 'normal',
        textAlign: 'center',
      }}
    >
      <span className="text-sm" style={{ color: isDark ? 'white' : '#3B0A17' }}>
        {text}
      </span>
      {/* Bubble tail */}
      <div
        className="absolute top-full left-1/2 transform -translate-x-1/2"
        style={{
          width: 0,
          height: 0,
          borderLeft: '6px solid transparent',
          borderRight: '6px solid transparent',
          borderTop: `6px solid ${getBubbleColor()}`,
        }}
      />
    </div>
  );
};

// Stage progress indicator
const StageProgress: React.FC<{
  currentLevel: number;
  nextStage: { stage: PetStage; requiredLevel: number } | null;
}> = ({ currentLevel, nextStage }) => {
  if (!nextStage) return null;
  
  const progress = currentLevel / nextStage.requiredLevel;
  
  return (
    <div className="text-xs text-center mt-1 opacity-70">
      <div className="mb-1">Next: {nextStage.stage}</div>
      <div className="w-16 h-1 bg-white/20 rounded-full overflow-hidden">
        <div
          className="h-full bg-white/60 rounded-full transition-all"
          style={{ width: `${Math.min(progress * 100, 100)}%` }}
        />
      </div>
    </div>
  );
};

// Main FloatingPet component
export const FloatingPet: React.FC = () => {
  const { isDark } = useTheme();
  const [petSystem] = useState(() => getPetSystem());
  const [questSystem] = useState(() => getQuestSystem());
  const [, setRefreshKey] = useState(0);
  const [isHovered, setIsHovered] = useState(false);

  // Subscribe to state changes
  useEffect(() => {
    const unsubscribePet = petSystem.subscribe(() => {
      setRefreshKey(k => k + 1);
    });
    const unsubscribeQuest = questSystem.subscribe(() => {
      petSystem.updateStage();
      setRefreshKey(k => k + 1);
    });
    return () => {
      unsubscribePet();
      unsubscribeQuest();
    };
  }, [petSystem, questSystem]);

  // Update pet stage on mount
  useEffect(() => {
    petSystem.updateStage();
  }, [petSystem]);

  // Handle pet interaction
  const handlePetClick = () => {
    petSystem.interact();
  };

  // Get current state
  const state = petSystem.getState();
  const stage = petSystem.getStage();
  const mood = petSystem.getMood();
  const dialogue = petSystem.getCurrentDialogue();
  const isVisible = petSystem.isFloatingVisible();
  const level = questSystem.getLevel();
  const nextStage = petSystem.getNextStageInfo();

  if (!isVisible) return null;

  return (
    <div
      className="fixed bottom-24 right-4 z-30 flex flex-col items-center"
      style={{
        pointerEvents: 'auto',
      }}
    >
      {/* Dialogue bubble */}
      {dialogue && (
        <DialogueBubble text={dialogue} stage={stage} />
      )}
      
      {/* Pet sprite */}
      <div
        className="relative"
        onMouseEnter={() => setIsHovered(true)}
        onMouseLeave={() => setIsHovered(false)}
      >
        <PetSprite
          stage={stage}
          mood={mood}
          onClick={handlePetClick}
          isHovered={isHovered}
        />
        
        {/* Stage indicator */}
        <div
          className="absolute -bottom-1 left-1/2 transform -translate-x-1/2 px-2 py-0.5 rounded-full text-xs"
          style={{
            background: isDark ? 'rgba(0,0,0,0.5)' : 'rgba(255,255,255,0.9)',
            color: isDark ? 'white' : '#3B0A17',
            fontSize: '10px',
          }}
        >
          {stage}
        </div>
      </div>
      
      {/* Stage progress */}
      <StageProgress currentLevel={level} nextStage={nextStage} />
      
      {/* Name */}
      <div
        className="mt-1 text-xs font-medium"
        style={{
          color: isDark ? 'rgba(255,255,255,0.7)' : 'rgba(59,10,23,0.7)',
        }}
      >
        {state.name}
      </div>
    </div>
  );
};

export default FloatingPet;
