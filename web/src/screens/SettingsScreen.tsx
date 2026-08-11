// Curio Web App - Settings Screen (Premium Version)
// Matches Android app's premium design with proper sections and toggles

import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTheme, getBackgroundColor, getTextColor } from '../theme/ThemeContext';
import { 
  CurioToggle, 
  CurioSectionHeader,
  CurioBackButton
} from '../components/SharedComponents';

// ─── Settings Item Component ──────────────────────────────────────────
const SettingsItem: React.FC<{
  icon: string;
  title: string;
  description?: string;
  onClick?: () => void;
  rightContent?: React.ReactNode;
}> = ({ icon, title, description, onClick, rightContent }) => {
  const { isDark } = useTheme();
  const [isPressed, setIsPressed] = useState(false);

  return (
    <button
      onClick={onClick}
      onMouseDown={() => setIsPressed(true)}
      onMouseUp={() => setIsPressed(false)}
      onMouseLeave={() => setIsPressed(false)}
      className="w-full flex items-center gap-3 p-3 rounded-xl transition-all duration-200 text-left"
      style={{
        background: isPressed 
          ? (isDark ? 'rgba(255,255,255,0.08)' : 'rgba(59,10,23,0.05)')
          : 'transparent',
      }}
      disabled={!onClick}
    >
      <div
        className="w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0"
        style={{
          background: isDark ? 'rgba(255,255,255,0.1)' : 'rgba(59,10,23,0.05)',
        }}
      >
        <span className="text-lg">{icon}</span>
      </div>
      <div className="flex-1 min-w-0">
        <div
          className="text-sm font-medium"
          style={{ color: getTextColor(isDark) }}
        >
          {title}
        </div>
        {description && (
          <div
            className="text-xs mt-0.5"
            style={{ color: isDark ? 'rgba(255,255,255,0.5)' : 'rgba(59,10,23,0.5)' }}
          >
            {description}
          </div>
        )}
      </div>
      {rightContent || (onClick && (
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{ color: isDark ? 'rgba(255,255,255,0.4)' : 'rgba(59,10,23,0.4)' }}>
          <path d="M9 18l6-6-6-6" />
        </svg>
      ))}
    </button>
  );
};

// ─── Theme Picker Component ───────────────────────────────────────────
const ThemePicker: React.FC<{
  selected: string;
  onSelect: (theme: string) => void;
}> = ({ selected, onSelect }) => {
  const { isDark } = useTheme();

  const themes = [
    { id: 'curio', name: 'Curio', color: '#FF8FA3' },
    { id: 'material', name: 'Material', color: '#4338CA' },
    { id: 'amoled', name: 'AMOLED', color: '#000000' },
  ];

  return (
    <div className="flex gap-3">
      {themes.map((theme) => (
        <button
          key={theme.id}
          onClick={() => onSelect(theme.id)}
          className="flex-1 p-3 rounded-xl text-center transition-all duration-200"
          style={{
            background: selected === theme.id 
              ? `${theme.color}20`
              : (isDark ? 'rgba(255,255,255,0.05)' : 'rgba(59,10,23,0.03)'),
            border: selected === theme.id 
              ? `2px solid ${theme.color}`
              : '2px solid transparent',
          }}
        >
          <div
            className="w-8 h-8 rounded-full mx-auto mb-2"
            style={{ background: theme.color }}
          />
          <div
            className="text-xs font-medium"
            style={{ color: getTextColor(isDark) }}
          >
            {theme.name}
          </div>
        </button>
      ))}
    </div>
  );
};

// ─── Main SettingsScreen Component ────────────────────────────────────
export const SettingsScreen: React.FC = () => {
  const navigate = useNavigate();
  const { isDark, isAmoled, setThemeStyle, setDarkMode, setPastelColors } = useTheme();
  
  const [settings, setSettings] = useState({
    darkMode: isDark,
    pastelColors: false,
    heroGradient: true,
    notifications: true,
    soundEffects: true,
    autoSave: true,
    showPet: true,
    showQuests: true,
  });

  const handleSettingChange = (key: string, value: boolean) => {
    setSettings(prev => ({ ...prev, [key]: value }));
    
    // Apply theme changes immediately
    if (key === 'darkMode') {
      setDarkMode(value);
    } else if (key === 'pastelColors') {
      setPastelColors(value);
    }
  };

  const handleThemeChange = (theme: string) => {
    setThemeStyle(theme);
  };

  return (
    <div
      className="min-h-screen pb-24"
      style={{ backgroundColor: getBackgroundColor(isDark, isAmoled) }}
    >
      {/* Header */}
      <div className="sticky top-0 z-10 px-4 pt-6 pb-4" style={{ background: getBackgroundColor(isDark, isAmoled) }}>
        <div className="flex items-center gap-3">
          <CurioBackButton onClick={() => navigate(-1)} />
          <h1
            className="text-xl font-bold"
            style={{ color: getTextColor(isDark), fontFamily: 'Geom, sans-serif' }}
          >
            Settings
          </h1>
        </div>
      </div>

      {/* Content */}
      <div className="px-4">
        {/* Appearance Section */}
        <div className="mb-6">
          <CurioSectionHeader title="Appearance" />
          <div
            className="rounded-2xl overflow-hidden"
            style={{
              background: isDark ? 'rgba(255,255,255,0.05)' : 'rgba(59,10,23,0.03)',
            }}
          >
            <div className="p-4">
              <ThemePicker
                selected={isAmoled ? 'amoled' : 'curio'}
                onSelect={handleThemeChange}
              />
            </div>
            
            <div className="px-4 pb-2">
              <CurioToggle
                checked={settings.darkMode}
                onChange={(value) => handleSettingChange('darkMode', value)}
                label="Dark Mode"
                description="Use dark color scheme"
              />
            </div>
            
            <div className="px-4 pb-2">
              <CurioToggle
                checked={settings.pastelColors}
                onChange={(value) => handleSettingChange('pastelColors', value)}
                label="Pastel Colors"
                description="Use softer color palette"
              />
            </div>
            
            <div className="px-4 pb-4">
              <CurioToggle
                checked={settings.heroGradient}
                onChange={(value) => handleSettingChange('heroGradient', value)}
                label="Hero Gradient"
                description="Show gradient on hero cards"
              />
            </div>
          </div>
        </div>

        {/* Notifications Section */}
        <div className="mb-6">
          <CurioSectionHeader title="Notifications" />
          <div
            className="rounded-2xl overflow-hidden"
            style={{
              background: isDark ? 'rgba(255,255,255,0.05)' : 'rgba(59,10,23,0.03)',
            }}
          >
            <div className="px-4 py-2">
              <CurioToggle
                checked={settings.notifications}
                onChange={(value) => handleSettingChange('notifications', value)}
                label="Enable Notifications"
                description="Receive reminders and updates"
              />
            </div>
          </div>
        </div>

        {/* Data Section */}
        <div className="mb-6">
          <CurioSectionHeader title="Data" />
          <div
            className="rounded-2xl overflow-hidden"
            style={{
              background: isDark ? 'rgba(255,255,255,0.05)' : 'rgba(59,10,23,0.03)',
            }}
          >
            <div className="px-4 py-2">
              <CurioToggle
                checked={settings.autoSave}
                onChange={(value) => handleSettingChange('autoSave', value)}
                label="Auto Save"
                description="Automatically save entries"
              />
            </div>
            
            <SettingsItem
              icon="📤"
              title="Export Data"
              description="Download your entries"
              onClick={() => {/* Export logic */}}
            />
            
            <SettingsItem
              icon="📥"
              title="Import Data"
              description="Import from backup"
              onClick={() => {/* Import logic */}}
            />
            
            <SettingsItem
              icon="🗑️"
              title="Clear All Data"
              description="Remove all entries and settings"
              onClick={() => {/* Clear logic */}}
            />
          </div>
        </div>

        {/* Features Section */}
        <div className="mb-6">
          <CurioSectionHeader title="Features" />
          <div
            className="rounded-2xl overflow-hidden"
            style={{
              background: isDark ? 'rgba(255,255,255,0.05)' : 'rgba(59,10,23,0.03)',
            }}
          >
            <div className="px-4 py-2">
              <CurioToggle
                checked={settings.showPet}
                onChange={(value) => handleSettingChange('showPet', value)}
                label="Show Pet"
                description="Display your companion"
              />
            </div>
            
            <div className="px-4 pb-2">
              <CurioToggle
                checked={settings.showQuests}
                onChange={(value) => handleSettingChange('showQuests', value)}
                label="Show Quests"
                description="Display quest progress"
              />
            </div>
            
            <div className="px-4 pb-2">
              <CurioToggle
                checked={settings.soundEffects}
                onChange={(value) => handleSettingChange('soundEffects', value)}
                label="Sound Effects"
                description="Play sounds on interactions"
              />
            </div>
          </div>
        </div>

        {/* About Section */}
        <div className="mb-6">
          <CurioSectionHeader title="About" />
          <div
            className="rounded-2xl overflow-hidden"
            style={{
              background: isDark ? 'rgba(255,255,255,0.05)' : 'rgba(59,10,23,0.03)',
            }}
          >
            <SettingsItem
              icon="ℹ️"
              title="About Curio"
              description="Version 1.0.0"
              onClick={() => {/* About */}}
            />
            
            <SettingsItem
              icon="📖"
              title="Changelog"
              description="What's new"
              onClick={() => {/* Changelog */}}
            />
            
            <SettingsItem
              icon="💬"
              title="Feedback"
              description="Share your thoughts"
              onClick={() => {/* Feedback */}}
            />
            
            <SettingsItem
              icon="⭐"
              title="Rate Curio"
              description="Help us grow"
              onClick={() => {/* Rate */}}
            />
          </div>
        </div>
      </div>
    </div>
  );
};

export default SettingsScreen;
