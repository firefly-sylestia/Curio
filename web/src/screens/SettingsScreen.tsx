// Curio Web App - Settings Screen
// Matches Android: watermark backdrop, Material icons, theming

import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTheme, getBackgroundColor, getTextColor } from '../theme/ThemeContext';
import { 
  CurioWatermarkBackdrop,
  CurioToggle, 
  CurioSectionHeader,
  CurioBackButton,
  MaterialIcon,
} from '../components/SharedComponents';

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
      <div className="w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0"
        style={{ background: isDark ? 'rgba(255,255,255,0.1)' : 'rgba(59,10,23,0.05)' }}>
        <MaterialIcon name={icon} size={20} style={{ color: isDark ? 'rgba(255,255,255,0.7)' : 'rgba(59,10,23,0.7)' }} />
      </div>
      <div className="flex-1 min-w-0">
        <div className="text-sm font-medium" style={{ color: getTextColor(isDark) }}>{title}</div>
        {description && (
          <div className="text-xs mt-0.5" style={{ color: isDark ? 'rgba(255,255,255,0.5)' : 'rgba(59,10,23,0.5)' }}>{description}</div>
        )}
      </div>
      {rightContent || (onClick && (
        <MaterialIcon name="chevron_right" size={18} style={{ color: isDark ? 'rgba(255,255,255,0.4)' : 'rgba(59,10,23,0.4)' }} />
      ))}
    </button>
  );
};

const ThemePicker: React.FC<{ selected: string; onSelect: (theme: string) => void }> = ({ selected, onSelect }) => {
  const { isDark } = useTheme();
  const themes = [
    { id: 'curio', name: 'Curio', color: '#FF8FA3' },
    { id: 'material', name: 'Material', color: '#4338CA' },
    { id: 'amoled', name: 'AMOLED', color: '#000000' },
  ];

  return (
    <div className="flex gap-3">
      {themes.map((theme) => (
        <button key={theme.id} onClick={() => onSelect(theme.id)}
          className="flex-1 p-3 rounded-xl text-center transition-all duration-200"
          style={{
            background: selected === theme.id ? `${theme.color}20` : (isDark ? 'rgba(255,255,255,0.05)' : 'rgba(59,10,23,0.03)'),
            border: selected === theme.id ? `2px solid ${theme.color}` : '2px solid transparent',
          }}>
          <div className="w-8 h-8 rounded-full mx-auto mb-2" style={{ background: theme.color }} />
          <div className="text-xs font-medium" style={{ color: getTextColor(isDark) }}>{theme.name}</div>
        </button>
      ))}
    </div>
  );
};

export const SettingsScreen: React.FC = () => {
  const navigate = useNavigate();
  const { isDark, isAmoled, style, setThemeStyle, setDarkMode, setPastelColors } = useTheme();
  
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
    if (key === 'darkMode') setDarkMode(value);
    else if (key === 'pastelColors') setPastelColors(value);
  };

  return (
    <div className="min-h-screen pb-24 relative" style={{ backgroundColor: getBackgroundColor(isDark, isAmoled) }}>
      <CurioWatermarkBackdrop alphaScale={0.45} />

      <div className="relative z-10">
        <div className="sticky top-0 z-10 px-4 pt-6 pb-4" style={{ background: getBackgroundColor(isDark, isAmoled) }}>
          <div className="flex items-center gap-3">
            <CurioBackButton onClick={() => navigate(-1)} />
            <h1 className="text-xl font-bold" style={{ color: getTextColor(isDark), fontFamily: 'Geom, sans-serif' }}>Settings</h1>
          </div>
        </div>

        <div className="px-4">
          <div className="mb-6">
            <CurioSectionHeader title="Appearance" />
            <div className="rounded-2xl overflow-hidden" style={{ background: isDark ? 'rgba(255,255,255,0.05)' : 'rgba(59,10,23,0.03)' }}>
              <div className="p-4">
                <ThemePicker selected={style} onSelect={setThemeStyle} />
              </div>
              <div className="px-4 pb-2">
                <CurioToggle checked={settings.darkMode} onChange={(v) => handleSettingChange('darkMode', v)} label="Dark Mode" description="Use dark color scheme" />
              </div>
              <div className="px-4 pb-2">
                <CurioToggle checked={settings.pastelColors} onChange={(v) => handleSettingChange('pastelColors', v)} label="Pastel Colors" description="Use softer color palette" />
              </div>
              <div className="px-4 pb-4">
                <CurioToggle checked={settings.heroGradient} onChange={(v) => handleSettingChange('heroGradient', v)} label="Hero Gradient" description="Show gradient on hero cards" />
              </div>
            </div>
          </div>

          <div className="mb-6">
            <CurioSectionHeader title="Notifications" />
            <div className="rounded-2xl overflow-hidden" style={{ background: isDark ? 'rgba(255,255,255,0.05)' : 'rgba(59,10,23,0.03)' }}>
              <div className="px-4 py-2">
                <CurioToggle checked={settings.notifications} onChange={(v) => handleSettingChange('notifications', v)} label="Enable Notifications" description="Receive reminders and updates" />
              </div>
            </div>
          </div>

          <div className="mb-6">
            <CurioSectionHeader title="Data" />
            <div className="rounded-2xl overflow-hidden" style={{ background: isDark ? 'rgba(255,255,255,0.05)' : 'rgba(59,10,23,0.03)' }}>
              <div className="px-4 py-2">
                <CurioToggle checked={settings.autoSave} onChange={(v) => handleSettingChange('autoSave', v)} label="Auto Save" description="Automatically save entries" />
              </div>
              <SettingsItem icon="upload" title="Export Data" description="Download your entries" onClick={() => {}} />
              <SettingsItem icon="download" title="Import Data" description="Import from backup" onClick={() => {}} />
              <SettingsItem icon="delete_forever" title="Clear All Data" description="Remove all entries and settings" onClick={() => {}} />
            </div>
          </div>

          <div className="mb-6">
            <CurioSectionHeader title="Features" />
            <div className="rounded-2xl overflow-hidden" style={{ background: isDark ? 'rgba(255,255,255,0.05)' : 'rgba(59,10,23,0.03)' }}>
              <div className="px-4 py-2">
                <CurioToggle checked={settings.showPet} onChange={(v) => handleSettingChange('showPet', v)} label="Show Pet" description="Display your companion" />
              </div>
              <div className="px-4 pb-2">
                <CurioToggle checked={settings.showQuests} onChange={(v) => handleSettingChange('showQuests', v)} label="Show Quests" description="Display quest progress" />
              </div>
              <div className="px-4 pb-2">
                <CurioToggle checked={settings.soundEffects} onChange={(v) => handleSettingChange('soundEffects', v)} label="Sound Effects" description="Play sounds on interactions" />
              </div>
            </div>
          </div>

          <div className="mb-6">
            <CurioSectionHeader title="About" />
            <div className="rounded-2xl overflow-hidden" style={{ background: isDark ? 'rgba(255,255,255,0.05)' : 'rgba(59,10,23,0.03)' }}>
              <SettingsItem icon="info" title="About Curio" description="Version 1.0.0" onClick={() => {}} />
              <SettingsItem icon="description" title="Changelog" description="What's new" onClick={() => {}} />
              <SettingsItem icon="chat" title="Feedback" description="Share your thoughts" onClick={() => {}} />
              <SettingsItem icon="star" title="Rate Curio" description="Help us grow" onClick={() => {}} />
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default SettingsScreen;
