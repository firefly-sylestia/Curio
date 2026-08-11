// Curio Web App - Theme Context
// Manages theme state (Curio/AMOLED/Material, dark/light, pastel)

import React, { createContext, useContext, useState, useEffect } from 'react';
import type { ReactNode } from 'react';
import type { ThemeStyle } from '../types';

interface ThemeSettings {
  style: ThemeStyle;
  isDark: boolean;
  pastelColors: boolean;
  tintWash: boolean;
  heroGradient: boolean;
}

interface ThemeContextType extends ThemeSettings {
  isAmoled: boolean;
  setStyle: (style: ThemeStyle) => void;
  setDark: (dark: boolean) => void;
  setDarkMode: (dark: boolean) => void;
  setPastelColors: (pastel: boolean) => void;
  setTintWash: (tint: boolean) => void;
  setHeroGradient: (gradient: boolean) => void;
  setThemeStyle: (style: string) => void;
}

const ThemeContext = createContext<ThemeContextType | undefined>(undefined);

const STORAGE_KEY = 'curio-theme-settings';

const defaultSettings: ThemeSettings = {
  style: 'curio',
  isDark: false,
  pastelColors: true,
  tintWash: true,
  heroGradient: true,
};

export const ThemeProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [settings, setSettings] = useState<ThemeSettings>(() => {
    const saved = localStorage.getItem(STORAGE_KEY);
    return saved ? { ...defaultSettings, ...JSON.parse(saved) } : defaultSettings;
  });

  useEffect(() => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(settings));
    
    // Apply theme to document
    const root = document.documentElement;
    root.classList.remove('dark', 'amoled', 'material', 'pastel');
    
    if (settings.isDark) {
      root.classList.add('dark');
    }
    if (settings.style === 'amoled') {
      root.classList.add('amoled');
    } else if (settings.style === 'material') {
      root.classList.add('material');
    }
    if (settings.pastelColors) {
      root.classList.add('pastel');
    }
  }, [settings]);

  const setStyle = (style: ThemeStyle) => setSettings((s: ThemeSettings) => ({ ...s, style }));
  const setDark = (isDark: boolean) => setSettings((s: ThemeSettings) => ({ ...s, isDark }));
  const setDarkMode = (isDark: boolean) => setSettings((s: ThemeSettings) => ({ ...s, isDark }));
  const setPastelColors = (pastelColors: boolean) => setSettings((s: ThemeSettings) => ({ ...s, pastelColors }));
  const setTintWash = (tintWash: boolean) => setSettings((s: ThemeSettings) => ({ ...s, tintWash }));
  const setHeroGradient = (heroGradient: boolean) => setSettings((s: ThemeSettings) => ({ ...s, heroGradient }));
  const setThemeStyle = (style: string) => setSettings((s: ThemeSettings) => ({ ...s, style: style as ThemeStyle }));

  return (
    <ThemeContext.Provider value={{
      ...settings,
      isAmoled: settings.style === 'amoled',
      setStyle,
      setDark,
      setDarkMode,
      setPastelColors,
      setTintWash,
      setHeroGradient,
      setThemeStyle,
    }}>
      {children}
    </ThemeContext.Provider>
  );
};

export const useTheme = () => {
  const context = useContext(ThemeContext);
  if (!context) {
    throw new Error('useTheme must be used within a ThemeProvider');
  }
  return context;
};

// Theme-aware color helpers
export const getBackgroundColor = (isDark: boolean, isAmoled: boolean): string => {
  if (isAmoled) return '#000000';
  if (isDark) return '#1a1a2e';
  return '#F7F0E4'; // Soft Cream
};

export const getSurfaceColor = (isDark: boolean, isAmoled: boolean): string => {
  if (isAmoled) return '#0a0a0a';
  if (isDark) return '#252535';
  return '#FFFBF5';
};

export const getCardColor = (isDark: boolean, isAmoled: boolean): string => {
  if (isAmoled) return '#111111';
  if (isDark) return '#2a2a3a';
  return '#FFFFFF';
};

export const getTextColor = (isDark: boolean): string => {
  return isDark ? '#FFFFFF' : '#3B0A17';
};

export const getSecondaryTextColor = (isDark: boolean): string => {
  return isDark ? 'rgba(255,255,255,0.7)' : 'rgba(59,10,23,0.7)';
};

// Pastel color helpers — softens category accents to airy pastels
const hexToHsl = (hex: string): [number, number, number] => {
  let r = parseInt(hex.slice(1, 3), 16) / 255;
  let g = parseInt(hex.slice(3, 5), 16) / 255;
  let b = parseInt(hex.slice(5, 7), 16) / 255;
  const max = Math.max(r, g, b), min = Math.min(r, g, b);
  let h = 0, s = 0, l = (max + min) / 2;
  if (max !== min) {
    const d = max - min;
    s = l > 0.5 ? d / (2 - max - min) : d / (max + min);
    h = max === r ? ((g - b) / d + (g < b ? 6 : 0)) / 6
      : max === g ? ((b - r) / d + 2) / 6
      : ((r - g) / d + 4) / 6;
  }
  return [h * 360, s, l];
};

const hslToHex = (h: number, s: number, l: number): string => {
  h = ((h % 360) + 360) % 360 / 360;
  const hue2rgb = (p: number, q: number, t: number) => {
    if (t < 0) t += 1; if (t > 1) t -= 1;
    if (t < 1/6) return p + (q - p) * 6 * t;
    if (t < 1/2) return q;
    if (t < 2/3) return p + (q - p) * (2/3 - t) * 6;
    return p;
  };
  const q = l < 0.5 ? l * (1 + s) : l + s - l * s;
  const p = 2 * l - q;
  const r = Math.round(hue2rgb(p, q, h + 1/3) * 255);
  const g = Math.round(hue2rgb(p, q, h) * 255);
  const b = Math.round(hue2rgb(p, q, h - 1/3) * 255);
  return `#${[r, g, b].map(c => c.toString(16).padStart(2, '0')).join('')}`;
};

/** Soften an accent color to its pastel twin */
export const getPastelAccent = (accent: string, isDark: boolean): string => {
  const [h, s, l] = hexToHsl(accent);
  if (isDark) {
    // Dark mode: deepen and mute
    return hslToHex(h, Math.min(s * 0.7, 0.55), Math.max(l * 0.65, 0.18));
  }
  // Light mode: airy pastel — lift lightness, reduce saturation
  return hslToHex(h, Math.min(s * 0.45, 0.35), Math.min(l * 1.15 + 0.15, 0.88));
};

/** Get the pastel card fill color (category → surface) */
export const getPastelCardFill = (accent: string, isDark: boolean): string => {
  const [h, s, l] = hexToHsl(accent);
  if (isDark) {
    return hslToHex(h, Math.min(s * 0.55, 0.4), Math.max(l * 0.55, 0.14));
  }
  return hslToHex(h, Math.min(s * 0.35, 0.25), Math.min(l + 0.12, 0.85));
};
