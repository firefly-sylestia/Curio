// Curio Web App - CurioCard Component
// Mirrors Android app's card components

import React from 'react';
import type { ReactNode } from 'react';
import type { CurioCategory } from '../types';
import { useTheme } from '../theme/ThemeContext';

interface CurioCardProps {
  children: ReactNode;
  category?: CurioCategory;
  onClick?: () => void;
  className?: string;
  variant?: 'default' | 'hero' | 'compact';
}

export const CurioCard: React.FC<CurioCardProps> = ({
  children,
  category,
  onClick,
  className = '',
  variant = 'default',
}) => {
  const { isDark } = useTheme();
  
  const getCardStyles = () => {
    const base = 'rounded-[24px] transition-all duration-300';
    
    if (variant === 'hero') {
      return `${base} p-6 shadow-lg ${className}`;
    }
    
    if (variant === 'compact') {
      return `${base} p-3 shadow-md ${className}`;
    }
    
    return `${base} p-4 shadow-curio ${className}`;
  };

  const getBackground = () => {
    if (category) {
      if (isDark) {
        return `linear-gradient(135deg, ${category.accent}22 0%, ${category.accent}11 100%)`;
      }
      return `linear-gradient(135deg, ${category.tint} 0%, white 100%)`;
    }
    
    if (isDark) {
      return 'linear-gradient(135deg, rgba(255,255,255,0.05) 0%, rgba(255,255,255,0.02) 100%)';
    }
    return 'linear-gradient(135deg, #FFFBF5 0%, white 100%)';
  };

  return (
    <div
      className={getCardStyles()}
      style={{ background: getBackground() }}
      onClick={onClick}
      role={onClick ? 'button' : undefined}
      tabIndex={onClick ? 0 : undefined}
    >
      {children}
    </div>
  );
};

// Category Card - for grid displays
interface CategoryCardProps {
  category: CurioCategory;
  selected?: boolean;
  onClick?: () => void;
}

export const CategoryCard: React.FC<CategoryCardProps> = ({
  category,
  selected = false,
  onClick,
}) => {
  const { isDark } = useTheme();
  
  const getCardBackground = () => {
    if (selected) {
      return `linear-gradient(135deg, ${category.accent} 0%, ${category.lightAccent} 100%)`;
    }
    
    if (isDark) {
      return `linear-gradient(135deg, ${category.accent}33 0%, ${category.accent}11 100%)`;
    }
    
    return category.tint;
  };

  const getTextColor = () => {
    if (selected) return 'white';
    if (isDark) return 'white';
    return category.accent;
  };

  return (
    <div
      className={`
        rounded-[24px] p-4 cursor-pointer transition-all duration-200
        ${selected ? 'ring-2 ring-offset-2' : 'hover:scale-105'}
      `}
      style={{
        background: getCardBackground(),
        outlineColor: selected ? category.accent : undefined,
      }}
      onClick={onClick}
    >
      <div className="flex flex-col items-center gap-2">
        <div
          className="w-12 h-12 rounded-full flex items-center justify-center"
          style={{ backgroundColor: selected ? 'rgba(255,255,255,0.2)' : category.tint }}
        >
          <span className="material-symbols-outlined text-2xl" style={{ color: getTextColor() }}>
            {category.iconGlyph}
          </span>
        </div>
        <span
          className="text-sm font-semibold text-center"
          style={{ color: getTextColor() }}
        >
          {category.displayName}
        </span>
      </div>
    </div>
  );
};

// Hero Card - for the main spin screen
interface HeroCardProps {
  category: CurioCategory;
  topicName?: string;
  onClick?: () => void;
}

export const HeroCard: React.FC<HeroCardProps> = ({
  category,
  topicName,
  onClick,
}) => {
  const { isDark, heroGradient } = useTheme();
  
  const getBackground = () => {
    if (heroGradient) {
      if (isDark) {
        return `linear-gradient(135deg, ${category.accent}44 0%, ${category.accent}22 50%, ${category.lightAccent}11 100%)`;
      }
      return `linear-gradient(135deg, ${category.accent} 0%, ${category.lightAccent} 100%)`;
    }
    
    if (isDark) {
      return category.accent;
    }
    return category.tint;
  };

  return (
    <div
      className="rounded-[32px] p-8 shadow-lg cursor-pointer transition-transform hover:scale-[1.02]"
      style={{ background: getBackground() }}
      onClick={onClick}
    >
      <div className="flex flex-col items-center text-center gap-4">
        <div
          className="w-20 h-20 rounded-full flex items-center justify-center"
          style={{ backgroundColor: 'rgba(255,255,255,0.2)' }}
        >
          <span className="material-symbols-outlined text-4xl text-white">{category.iconGlyph}</span>
        </div>
        
        {topicName ? (
          <h2 className="text-2xl font-bold text-white">{topicName}</h2>
        ) : (
          <h2 className="text-2xl font-bold text-white">{category.displayName}</h2>
        )}
        
        <div className="text-white/80 text-sm">
          Tap to spin
        </div>
      </div>
    </div>
  );
};

export default CurioCard;
