// Curio Web App - Shared UI Components Library
// Premium components matching Android app's design system

import React, { useState, useEffect } from 'react';
import { useTheme, getTextColor, getPastelAccent } from '../theme/ThemeContext';
import { ALL_CATEGORIES } from '../data/categories';
import type { CurioCategory } from '../types';

// ─── Material Icon ──────────────────────────────────────────────────
export const MaterialIcon: React.FC<{
  name: string;
  size?: number;
  filled?: boolean;
  className?: string;
  style?: React.CSSProperties;
}> = ({ name, size = 24, filled = false, className = '', style: outerStyle }) => (
  <span
    className={`material-symbols-outlined ${className}`}
    style={{
      fontSize: size,
      fontVariationSettings: `'FILL' ${filled ? 1 : 0}, 'wght' 400, 'GRAD' 0, 'opsz' 24`,
      width: size,
      height: size,
      display: 'inline-flex',
      alignItems: 'center',
      justifyContent: 'center',
      ...outerStyle,
    }}
  >
    {name}
  </span>
);

// ─── Animation Constants ──────────────────────────────────────────────
export const SPRINGS = {
  snappy: 'cubic-bezier(0.2, 0.9, 0.3, 1)',
  bouncy: 'cubic-bezier(0.34, 1.56, 0.64, 1)',
  deliberate: 'cubic-bezier(0.25, 0.1, 0.25, 1)',
  morph: 'cubic-bezier(0.22, 0.61, 0.36, 1)',
  elastic: 'cubic-bezier(0.68, -0.55, 0.265, 1.55)',
  press: 'cubic-bezier(0.2, 0.8, 0.32, 1.2)',
} as const;

export const DURATIONS = {
  quick: 150,
  standard: 300,
  deliberate: 500,
  morph: 450,
  reveal: 650,
  spinMin: 2800,
  spinMax: 3600,
  confetti: 600,
  confettiLong: 1200,
} as const;

// ─── Curio Back Button ────────────────────────────────────────────────
export const CurioBackButton: React.FC<{
  onClick: () => void;
  size?: number;
  containerColor?: string;
  contentColor?: string;
}> = ({ onClick, size = 40, containerColor, contentColor }) => {
  const { isDark } = useTheme();
  const bgColor = containerColor || (isDark ? 'rgba(255,255,255,0.1)' : 'rgba(59,10,23,0.05)');
  const txtColor = contentColor || (isDark ? 'rgba(255,255,255,0.9)' : '#3B0A17');
  
  return (
    <button
      onClick={onClick}
      className="flex items-center justify-center transition-all duration-200"
      style={{
        width: size,
        height: size,
        borderRadius: '50%',
        background: bgColor,
        color: txtColor,
      }}
    >
      <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <path d="M15 18l-6-6 6-6" />
      </svg>
    </button>
  );
};

// ─── Curio Forward Arrow ──────────────────────────────────────────────
export const CurioForwardArrow: React.FC<{
  size?: number;
  tint?: string;
}> = ({ size = 18, tint }) => {
  const { isDark } = useTheme();
  
  return (
    <svg 
      width={size} 
      height={size} 
      viewBox="0 0 24 24" 
      fill="none" 
      stroke={tint || (isDark ? 'rgba(255,255,255,0.5)' : 'rgba(59,10,23,0.5)')} 
      strokeWidth="2" 
      strokeLinecap="round" 
      strokeLinejoin="round"
    >
      <path d="M9 18l6-6-6-6" />
    </svg>
  );
};

// ─── Curio Hero Card ─────────────────────────────────────────────────
export const CurioHeroCard: React.FC<{
  category: CurioCategory;
  onClick: () => void;
  title?: string;
  subtitle?: string;
  showShimmer?: boolean;
}> = ({ category, onClick, title = 'SHUFFLE', subtitle, showShimmer = true }) => {
  const { isDark, isAmoled } = useTheme();
  const [isPressed, setIsPressed] = useState(false);
  const [shimmerPos, setShimmerPos] = useState(-100);

  // Shimmer animation
  useEffect(() => {
    if (!showShimmer) return;
    const interval = setInterval(() => {
      setShimmerPos(prev => (prev >= 200 ? -100 : prev + 1.5));
    }, 35);
    return () => clearInterval(interval);
  }, [showShimmer]);

  // Category → surface gradient (matches Android categoryCardFill → themeSurface)
  const getBackground = () => {
    const hexToRgb = (hex: string): [number, number, number] => {
      const r = parseInt(hex.slice(1, 3), 16);
      const g = parseInt(hex.slice(3, 5), 16);
      const b = parseInt(hex.slice(5, 7), 16);
      return [r, g, b];
    };
    const [r, g, b] = hexToRgb(category.accent);

    if (isAmoled) {
      return `linear-gradient(180deg, rgba(${r},${g},${b},0.24) 0%, rgba(${r},${g},${b},0.06) 60%, rgba(0,0,0,1) 100%)`;
    }
    const surfaceRgb = isDark ? [26, 26, 46] : [247, 240, 228];
    const top = `rgba(${r},${g},${b},0.92)`;
    const mid = `rgba(${Math.round(r * 0.55 + surfaceRgb[0] * 0.45)},${Math.round(g * 0.55 + surfaceRgb[1] * 0.45)},${Math.round(b * 0.55 + surfaceRgb[2] * 0.45)},0.75)`;
    const bottom = `rgba(${Math.round(r * 0.2 + surfaceRgb[0] * 0.8)},${Math.round(g * 0.2 + surfaceRgb[1] * 0.8)},${Math.round(b * 0.2 + surfaceRgb[2] * 0.8)},0.9)`;
    return `linear-gradient(180deg, ${top} 0%, ${mid} 50%, ${bottom} 100%)`;
  };

  return (
    <button
      onClick={onClick}
      onMouseDown={() => setIsPressed(true)}
      onMouseUp={() => setIsPressed(false)}
      onMouseLeave={() => setIsPressed(false)}
      className="relative w-full h-[220px] rounded-[28px] overflow-hidden text-left transition-transform duration-200"
      style={{
        background: getBackground(),
        transform: isPressed ? 'scale(0.97)' : 'scale(1)',
        boxShadow: isPressed 
          ? `0 4px 16px ${category.accent}33`
          : `0 8px 32px rgba(0,0,0,0.12), 0 2px 8px rgba(0,0,0,0.06)`,
        border: isDark
          ? `1px solid ${category.accent}28`
          : `1px solid ${category.accent}18`,
      }}
    >
      {/* Category accent rule at top */}
      <div
        className="absolute top-0 left-3 right-3 h-[2px] rounded-full"
        style={{ background: category.accent, opacity: 0.5 }}
      />

      {/* Top-lit crown */}
      <div
        className="absolute top-0 left-0 right-0 h-[60px] pointer-events-none"
        style={{
          background: `linear-gradient(180deg, rgba(255,255,255,0.06) 0%, transparent 100%)`,
        }}
      />

      {/* Shimmer effect */}
      {showShimmer && (
        <div
          className="absolute inset-0 pointer-events-none"
          style={{
            background: `linear-gradient(105deg, transparent 35%, rgba(255,255,255,0.07) 48%, transparent 61%)`,
            transform: `translateX(${shimmerPos}%)`,
          }}
        />
      )}
      
      {/* Watermark glyph */}
      <div
        className="absolute right-2 bottom-2 pointer-events-none select-none"
        style={{
          color: isDark ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.04)',
        }}
      >
        <span className="material-symbols-outlined" style={{ fontSize: 130 }}>{category.iconGlyph}</span>
      </div>
      
      {/* Content */}
      <div className="relative z-10 flex flex-col justify-between h-full p-6">
        <div className="flex items-start justify-between">
          <h2
            className="text-3xl font-extrabold text-white"
            style={{ fontFamily: 'Geom, Inter, sans-serif', textShadow: '0 1px 4px rgba(0,0,0,0.1)' }}
          >
            {title}
          </h2>
          <MaterialIcon name="casino" size={22} className="text-white/50" />
        </div>
        
        <div>
          <p
            className="text-lg text-white/85"
            style={{ fontFamily: 'Geom, Inter, sans-serif' }}
          >
            the wheel
          </p>
          <p className="text-sm text-white/65 mt-1">
            {subtitle || `Shuffle for ${category.displayName}`}
          </p>
        </div>
      </div>
    </button>
  );
};

// ─── Curio Category Card ──────────────────────────────────────────────
export const CurioCategoryCard: React.FC<{
  category: CurioCategory;
  isSelected: boolean;
  onClick: () => void;
  size?: 'small' | 'medium' | 'large';
}> = ({ category, isSelected, onClick, size = 'medium' }) => {
  const { isDark, isAmoled } = useTheme();
  const [isPressed, setIsPressed] = useState(false);

  const sizes = {
    small: { width: 60, height: 60, borderRadius: 16, iconSize: 24 },
    medium: { width: 80, height: 80, borderRadius: 20, iconSize: 32 },
    large: { width: 100, height: 100, borderRadius: 24, iconSize: 40 },
  };

  const { width, height, borderRadius, iconSize } = sizes[size];

  const getBackground = () => {
    if (isSelected) {
      return category.accent;
    }
    if (isAmoled) {
      return `${category.accent}22`;
    }
    return isDark ? `${category.accent}33` : category.tint || `${category.accent}20`;
  };

  return (
    <button
      onClick={onClick}
      onMouseDown={() => setIsPressed(true)}
      onMouseUp={() => setIsPressed(false)}
      onMouseLeave={() => setIsPressed(false)}
      className="flex flex-col items-center gap-2 transition-all duration-200"
      style={{
        width,
        height,
        borderRadius,
        background: getBackground(),
        transform: isPressed ? 'scale(0.92)' : 'scale(1)',
        boxShadow: isSelected ? `0 4px 16px ${category.accent}44` : 'none',
      }}
    >
      <span className="material-symbols-outlined" style={{ fontSize: iconSize, width: iconSize, height: iconSize }}>{category.iconGlyph}</span>
    </button>
  );
};

// ─── Curio Paper Card ─────────────────────────────────────────────────
// Note-paper card matching Android's PaperCard: warm off-white, ruled lines,
// red margin line (school notebook), Patrick Hand font context, watermark glyphs.
export const CurioPaperCard: React.FC<{
  children: React.ReactNode;
  variant?: 'ruled' | 'torn' | 'plain' | 'coffee';
  className?: string;
  showMargin?: boolean;
  watermark?: string;
  /** Category accent for red margin line tint */
  accent?: string;
}> = ({ children, variant = 'ruled', className = '', showMargin = true, watermark, accent: _accent = '#D45050' }) => {
  const { isDark } = useTheme();

  // Paper ink color — warm brown/plum for handwriting on paper (matching Android's notePaperInk)
  const paperInk = isDark ? 'rgba(228,210,188,0.92)' : 'rgba(45,20,15,0.92)';
  // Warm paper background (like real notebook paper, never pure white)
  const paperBg = isDark ? 'rgba(28,22,16,0.92)' : '#FFFDF7';
  // Red margin line (school notebook style)
  const marginColor = isDark ? 'rgba(220,120,120,0.28)' : 'rgba(210,70,70,0.25)';
  // Ruled line color
  const ruleColor = isDark ? 'rgba(180,160,140,0.10)' : 'rgba(180,160,140,0.18)';

  const getBackground = () => {
    if (variant === 'ruled') {
      return `repeating-linear-gradient(0deg, transparent, transparent 27px, ${ruleColor} 27px, ${ruleColor} 28px)`;
    }
    if (variant === 'coffee') {
      return isDark 
        ? 'radial-gradient(ellipse at 80% 80%, rgba(139,90,43,0.15) 0%, transparent 50%)'
        : 'radial-gradient(ellipse at 80% 80%, rgba(139,90,43,0.08) 0%, transparent 50%)';
    }
    return 'none';
  };

  return (
    <div
      className={`relative paper-card ${className}`}
      style={{
        filter: 'drop-shadow(0 2px 6px rgba(0,0,0,0.08))',
        fontFamily: "'Patrick Hand', cursive",
        color: paperInk,
      }}
    >
      {/* Torn edge top */}
      {variant === 'torn' && (
        <div className="absolute -top-1 left-0 right-0 h-2 overflow-hidden">
          <svg viewBox="0 0 200 8" preserveAspectRatio="none" className="w-full h-full">
            <path
              d="M0,4 Q10,0 20,4 T40,4 T60,4 T80,4 T100,4 T120,4 T140,4 T160,4 T180,4 T200,4 L200,8 L0,8 Z"
              fill={paperBg}
            />
          </svg>
        </div>
      )}
      
      {/* Main paper surface */}
      <div
        className="relative rounded-lg p-5 overflow-hidden"
        style={{
          background: paperBg,
          backgroundImage: getBackground(),
        }}
      >
        {/* Red margin line (school notebook) */}
        {showMargin && variant === 'ruled' && (
          <div
            className="absolute top-0 bottom-0"
            style={{
              left: 44,
              width: 1,
              background: marginColor,
            }}
          />
        )}
        
        {/* Watermark glyph */}
        {watermark && (
          <div
            className="absolute bottom-2 right-3 pointer-events-none select-none"
            style={{
              fontFamily: "'Material Symbols Outlined'",
              fontSize: 80,
              opacity: isDark ? 0.04 : 0.03,
              color: isDark ? '#fff' : '#3B0A17',
            }}
          >
            {watermark}
          </div>
        )}
        
        {/* Content — inset past the margin line */}
        <div
          className="relative z-10"
          style={{
            paddingLeft: showMargin && variant === 'ruled' ? 52 : 0,
            fontFamily: "'Patrick Hand', cursive",
            color: paperInk,
          }}
        >
          {children}
        </div>
      </div>
      
      {/* Torn edge bottom */}
      {variant === 'torn' && (
        <div className="absolute -bottom-1 left-0 right-0 h-2 overflow-hidden">
          <svg viewBox="0 0 200 8" preserveAspectRatio="none" className="w-full h-full">
            <path
              d="M0,0 L200,0 L200,4 Q190,8 180,4 T160,4 T140,4 T120,4 T100,4 T80,4 T60,4 T40,4 T20,4 T0,4 Z"
              fill={paperBg}
            />
          </svg>
        </div>
      )}
    </div>
  );
};

// ─── Curio Moodboard Card ─────────────────────────────────────────────
export const CurioMoodboardCard: React.FC<{
  children: React.ReactNode;
  className?: string;
  accent?: string;
}> = ({ children, className = '', accent = '#3B0A17' }) => {
  const { isDark } = useTheme();

  return (
    <div
      className={`relative overflow-hidden rounded-2xl ${className}`}
      style={{
        background: isDark 
          ? `linear-gradient(135deg, ${accent}15 0%, ${accent}08 100%)`
          : `linear-gradient(135deg, ${accent}10 0%, white 100%)`,
        boxShadow: isDark
          ? `0 4px 20px ${accent}10, inset 0 1px 0 rgba(255,255,255,0.05)`
          : `0 4px 20px rgba(0,0,0,0.06), inset 0 1px 0 rgba(255,255,255,0.8)`,
      }}
    >
      {/* Accent edge shine */}
      <div
        className="absolute top-0 left-0 right-0 h-px"
        style={{
          background: `linear-gradient(90deg, transparent 0%, ${accent}44 50%, transparent 100%)`,
        }}
      />
      
      {/* Content */}
      <div className="relative z-10 p-5">
        {children}
      </div>
    </div>
  );
};

// ─── Curio Chip ───────────────────────────────────────────────────────
export const CurioChip: React.FC<{
  label: string;
  isSelected: boolean;
  onClick: () => void;
  color?: string;
}> = ({ label, isSelected, onClick, color }) => {
  const { isDark } = useTheme();
  const [isPressed, setIsPressed] = useState(false);

  return (
    <button
      onClick={onClick}
      onMouseDown={() => setIsPressed(true)}
      onMouseUp={() => setIsPressed(false)}
      onMouseLeave={() => setIsPressed(false)}
      className="px-4 py-2 rounded-full text-sm font-medium transition-all duration-200 whitespace-nowrap"
      style={{
        background: isSelected 
          ? (color || '#3B0A17')
          : isDark 
            ? 'rgba(255,255,255,0.1)' 
            : 'rgba(59,10,23,0.05)',
        color: isSelected 
          ? 'white'
          : isDark 
            ? 'rgba(255,255,255,0.8)' 
            : '#3B0A17',
        transform: isPressed ? 'scale(0.95)' : 'scale(1)',
        boxShadow: isSelected ? `0 2px 8px ${color || '#3B0A17'}44` : 'none',
      }}
    >
      {label}
    </button>
  );
};

// ─── Curio Button ─────────────────────────────────────────────────────
export const CurioButton: React.FC<{
  children: React.ReactNode;
  onClick: () => void;
  variant?: 'primary' | 'secondary' | 'ghost';
  color?: string;
  disabled?: boolean;
  className?: string;
}> = ({ children, onClick, variant = 'primary', color, disabled = false, className = '' }) => {
  const { isDark } = useTheme();
  const [isPressed, setIsPressed] = useState(false);

  const getBackground = () => {
    if (disabled) return isDark ? 'rgba(255,255,255,0.05)' : 'rgba(59,10,23,0.05)';
    if (variant === 'primary') return color || '#3B0A17';
    if (variant === 'secondary') return isDark ? 'rgba(255,255,255,0.1)' : 'rgba(59,10,23,0.05)';
    return 'transparent';
  };

  const getTextColor = () => {
    if (disabled) return isDark ? 'rgba(255,255,255,0.3)' : 'rgba(59,10,23,0.3)';
    if (variant === 'primary') return 'white';
    if (variant === 'secondary') return isDark ? 'rgba(255,255,255,0.9)' : '#3B0A17';
    return color || '#3B0A17';
  };

  return (
    <button
      onClick={onClick}
      onMouseDown={() => setIsPressed(true)}
      onMouseUp={() => setIsPressed(false)}
      onMouseLeave={() => setIsPressed(false)}
      disabled={disabled}
      className={`px-6 py-3 rounded-full font-medium transition-all duration-200 ${className}`}
      style={{
        background: getBackground(),
        color: getTextColor(),
        transform: isPressed ? 'scale(0.95)' : 'scale(1)',
        opacity: disabled ? 0.5 : 1,
        boxShadow: variant === 'primary' && !disabled 
          ? `0 4px 12px ${color || '#3B0A17'}44`
          : 'none',
      }}
    >
      {children}
    </button>
  );
};

// ─── Curio Toggle ─────────────────────────────────────────────────────
export const CurioToggle: React.FC<{
  checked: boolean;
  onChange: (checked: boolean) => void;
  label?: string;
  description?: string;
}> = ({ checked, onChange, label, description }) => {
  const { isDark } = useTheme();

  return (
    <div className="flex items-center justify-between py-3">
      {(label || description) && (
        <div className="flex-1 mr-4">
          {label && (
            <div className="text-sm font-medium" style={{ color: getTextColor(isDark) }}>
              {label}
            </div>
          )}
          {description && (
            <div className="text-xs mt-0.5" style={{ color: isDark ? 'rgba(255,255,255,0.5)' : 'rgba(59,10,23,0.5)' }}>
              {description}
            </div>
          )}
        </div>
      )}
      <button
        onClick={() => onChange(!checked)}
        className="relative w-12 h-7 rounded-full transition-colors duration-200"
        style={{
          background: checked ? '#3B0A17' : isDark ? 'rgba(255,255,255,0.2)' : 'rgba(59,10,23,0.15)',
        }}
      >
        <div
          className="absolute top-1 w-5 h-5 rounded-full transition-transform duration-200"
          style={{
            background: 'white',
            transform: checked ? 'translateX(24px)' : 'translateX(4px)',
            boxShadow: '0 2px 4px rgba(0,0,0,0.2)',
          }}
        />
      </button>
    </div>
  );
};

// ─── Curio Progress Bar ───────────────────────────────────────────────
export const CurioProgressBar: React.FC<{
  progress: number;
  color?: string;
  height?: number;
}> = ({ progress, color = '#3B0A17', height = 8 }) => {
  const { isDark } = useTheme();

  return (
    <div
      className="w-full rounded-full overflow-hidden"
      style={{
        height,
        background: isDark ? 'rgba(255,255,255,0.1)' : 'rgba(59,10,23,0.1)',
      }}
    >
      <div
        className="h-full rounded-full transition-all duration-500"
        style={{
          width: `${Math.min(100, Math.max(0, progress))}%`,
          background: `linear-gradient(90deg, ${color} 0%, ${color}CC 100%)`,
        }}
      />
    </div>
  );
};

// ─── Curio Badge ──────────────────────────────────────────────────────
export const CurioBadge: React.FC<{
  count: number;
  color?: string;
  size?: 'small' | 'medium' | 'large';
}> = ({ count, color = '#FF8FA3', size = 'medium' }) => {
  const sizes = {
    small: { width: 16, height: 16, fontSize: 10 },
    medium: { width: 20, height: 20, fontSize: 12 },
    large: { width: 24, height: 24, fontSize: 14 },
  };

  const { width, height, fontSize } = sizes[size];

  if (count <= 0) return null;

  return (
    <div
      className="flex items-center justify-center rounded-full text-white font-bold"
      style={{
        width,
        height,
        fontSize,
        background: color,
      }}
    >
      {count > 99 ? '99+' : count}
    </div>
  );
};

// ─── Curio Avatar ─────────────────────────────────────────────────────
export const CurioAvatar: React.FC<{
  initial: string;
  color?: string;
  size?: number;
}> = ({ initial, color = '#3B0A17', size = 40 }) => {
  return (
    <div
      className="flex items-center justify-center rounded-full text-white font-bold"
      style={{
        width: size,
        height: size,
        fontSize: size * 0.4,
        background: `linear-gradient(135deg, ${color} 0%, ${color}CC 100%)`,
      }}
    >
      {initial}
    </div>
  );
};

// ─── Curio Section Header ─────────────────────────────────────────────
export const CurioSectionHeader: React.FC<{
  title: string;
  action?: string;
  onAction?: () => void;
}> = ({ title, action, onAction }) => {
  const { isDark } = useTheme();

  return (
    <div className="flex items-center justify-between mb-4">
      <h3
        className="text-lg font-bold"
        style={{ color: getTextColor(isDark), fontFamily: 'Geom, sans-serif' }}
      >
        {title}
      </h3>
      {action && onAction && (
        <button
          onClick={onAction}
          className="text-sm font-medium transition-colors duration-200"
          style={{ color: isDark ? 'rgba(255,255,255,0.6)' : 'rgba(59,10,23,0.6)' }}
        >
          {action}
        </button>
      )}
    </div>
  );
};

// ─── Curio Stat Card ──────────────────────────────────────────────────
export const CurioStatCard: React.FC<{
  label: string;
  value: string | number;
  icon?: string;
  color?: string;
}> = ({ label, value, icon, color = '#3B0A17' }) => {
  const { isDark } = useTheme();

  return (
    <div
      className="flex flex-col items-center p-4 rounded-2xl"
      style={{
        background: isDark ? 'rgba(255,255,255,0.05)' : 'rgba(59,10,23,0.03)',
      }}
    >
      {icon && (
        <div
          className="w-10 h-10 rounded-full flex items-center justify-center mb-2"
          style={{ background: `${color}20` }}
        >
          <span className="material-symbols-outlined" style={{ fontSize: 20, color }}>{icon}</span>
        </div>
      )}
      <div
        className="text-2xl font-bold"
        style={{ color, fontFamily: 'Geom, sans-serif' }}
      >
        {value}
      </div>
      <div
        className="text-xs mt-1"
        style={{ color: isDark ? 'rgba(255,255,255,0.5)' : 'rgba(59,10,23,0.5)' }}
      >
        {label}
      </div>
    </div>
  );
};

// ─── Curio Empty State ────────────────────────────────────────────────
export const CurioEmptyState: React.FC<{
  icon: string;
  title: string;
  description: string;
  action?: string;
  onAction?: () => void;
}> = ({ icon, title, description, action, onAction }) => {
  const { isDark } = useTheme();

  return (
    <div className="flex flex-col items-center justify-center py-12 px-6 text-center">
      <div className="text-6xl mb-4">{icon}</div>
      <h3
        className="text-xl font-bold mb-2"
        style={{ color: getTextColor(isDark), fontFamily: 'Geom, sans-serif' }}
      >
        {title}
      </h3>
      <p
        className="text-sm mb-6"
        style={{ color: isDark ? 'rgba(255,255,255,0.5)' : 'rgba(59,10,23,0.5)' }}
      >
        {description}
      </p>
      {action && onAction && (
        <CurioButton onClick={onAction} variant="primary">
          {action}
        </CurioButton>
      )}
    </div>
  );
};

// ─── Curio Loading Spinner ────────────────────────────────────────────
export const CurioLoadingSpinner: React.FC<{
  size?: number;
  color?: string;
}> = ({ size = 24, color = '#3B0A17' }) => {
  return (
    <div
      className="animate-spin"
      style={{
        width: size,
        height: size,
        border: `2px solid ${color}22`,
        borderTopColor: color,
        borderRadius: '50%',
      }}
    />
  );
};

// ─── Curio Toast ──────────────────────────────────────────────────────
export const CurioToast: React.FC<{
  message: string;
  type?: 'success' | 'error' | 'info';
  onClose: () => void;
}> = ({ message, type = 'success', onClose }) => {
  const [isVisible, setIsVisible] = useState(true);

  useEffect(() => {
    const timer = setTimeout(() => {
      setIsVisible(false);
      setTimeout(onClose, 300);
    }, 3000);
    return () => clearTimeout(timer);
  }, [onClose]);

  const colors = {
    success: '#047857',
    error: '#DC2626',
    info: '#3B82F6',
  };

  return (
    <div
      className="fixed bottom-24 left-1/2 transform -translate-x-1/2 z-[70] transition-all duration-300"
      style={{
        opacity: isVisible ? 1 : 0,
        transform: `translateX(-50%) translateY(${isVisible ? 0 : 20}px)`,
      }}
    >
      <div
        className="px-4 py-3 rounded-full shadow-lg flex items-center gap-2"
        style={{ background: colors[type], color: 'white' }}
      >
        <span>{message}</span>
        <button onClick={onClose} className="ml-2 opacity-70 hover:opacity-100">
          ×
        </button>
      </div>
    </div>
  );
};

// ─── CurioWatermarkBackdrop ──────────────────────────────────────────
/**
 * Decorative backdrop matching Android's CurioWatermarkBackdrop.
 * Scatters all category glyphs around the screen edges, each tinted
 * with its category's accent at low alpha. The active category's glyph
 * gets a stronger whisper.
 */
export const CurioWatermarkBackdrop: React.FC<{
  activeCatId?: string;
  alphaScale?: number;
  topClearance?: number; // px — when set, glyphs stay below this line
}> = ({ activeCatId, alphaScale = 1, topClearance = 0 }) => {
  const { isDark, pastelColors } = useTheme();

  const slots = [
    { glyph: 'person', x: -2, y: 5, size: 72, rot: -12 },
    { glyph: 'album', x: 55, y: 3, size: 52, rot: 10 },
    { glyph: 'movie', x: 88, y: 20, size: 64, rot: -8 },
    { glyph: 'menu_book', x: -4, y: 35, size: 58, rot: 8 },
    { glyph: 'brush', x: -3, y: 62, size: 66, rot: -6 },
    { glyph: 'palette', x: -5, y: 82, size: 56, rot: 14 },
    { glyph: 'science', x: 70, y: 65, size: 62, rot: -12 },
    { glyph: 'smart_display', x: 82, y: 48, size: 50, rot: 16 },
    { glyph: 'sports_esports', x: 65, y: 85, size: 54, rot: -10 },
    { glyph: 'casino', x: 22, y: 78, size: 68, rot: 6 },
    { glyph: 'edit_note', x: 85, y: 75, size: 48, rot: -14 },
  ];

  // Alpha per theme (matches Android's watermarkAlpha)
  const inactiveAlpha = isDark
    ? (pastelColors ? 0.15 : 0.11)
    : (pastelColors ? 0.22 : 0.15);
  const activeAlpha = isDark
    ? (pastelColors ? 0.28 : 0.22)
    : (pastelColors ? 0.38 : 0.30);

  return (
    <div className="absolute inset-0 pointer-events-none overflow-hidden" style={{ zIndex: 0 }}>
      {slots.map((s, i) => {
        let cat = ALL_CATEGORIES.find(c => c.iconGlyph === s.glyph);
        if (!cat) cat = ALL_CATEGORIES[i % ALL_CATEGORIES.length];
        const isActive = cat.iconGlyph === activeCatId || cat.id === activeCatId;
        const rawAccent = cat.accent;
        const tint = pastelColors ? getPastelAccent(rawAccent, isDark) : rawAccent;
        const alpha = (isActive ? activeAlpha : inactiveAlpha) * alphaScale;
        const topOffset = topClearance > 0 ? topClearance : 0;

        return (
          <span
            key={i}
            className="material-symbols-outlined absolute select-none"
            style={{
              left: `${s.x}%`,
              top: topOffset > 0
                ? `${topOffset + (100 - topOffset) * (s.y / 100)}px`
                : `${s.y}%`,
              fontSize: s.size,
              color: tint,
              opacity: alpha,
              transform: `rotate(${s.rot}deg)`,
            }}
          >
            {s.glyph}
          </span>
        );
      })}
    </div>
  );
};

// ─── Curio Modal ──────────────────────────────────────────────────────
export const CurioModal: React.FC<{
  isOpen: boolean;
  onClose: () => void;
  title?: string;
  children: React.ReactNode;
}> = ({ isOpen, onClose, title, children }) => {
  const { isDark } = useTheme();
  const [isVisible, setIsVisible] = useState(false);

  useEffect(() => {
    if (isOpen) {
      setIsVisible(true);
    } else {
      setIsVisible(false);
    }
  }, [isOpen]);

  if (!isOpen) return null;

  return (
    <div
      className="fixed inset-0 z-[70] flex items-end sm:items-center justify-center transition-opacity duration-300"
      style={{
        opacity: isVisible ? 1 : 0,
        background: 'rgba(0,0,0,0.5)',
      }}
      onClick={onClose}
    >
      <div
        className="w-full max-w-lg rounded-t-3xl sm:rounded-3xl p-6 max-h-[80vh] overflow-y-auto transition-all duration-300"
        style={{
          background: isDark ? '#1a1a2e' : 'white',
          transform: isVisible ? 'translateY(0)' : 'translateY(100%)',
        }}
        onClick={(e) => e.stopPropagation()}
      >
        {title && (
          <div className="flex items-center justify-between mb-4">
            <h3
              className="text-lg font-bold"
              style={{ color: getTextColor(isDark), fontFamily: 'Geom, sans-serif' }}
            >
              {title}
            </h3>
            <button
              onClick={onClose}
              className="text-2xl"
              style={{ color: isDark ? 'rgba(255,255,255,0.5)' : 'rgba(59,10,23,0.5)' }}
            >
              ×
            </button>
          </div>
        )}
        {children}
      </div>
    </div>
  );
};
