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
