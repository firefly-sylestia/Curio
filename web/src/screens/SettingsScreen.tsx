// Curio Web App - Settings Screen
// Matches Android: compact torn hero (SETTINGS_TEAR_SEED), watermark collage, back pill, themed sections

import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTheme, getBackgroundColor, getTextColor } from '../theme/ThemeContext';
import {
  CurioWatermarkBackdrop,
  CurioToggle,
  CurioSectionHeader,
  MaterialIcon,
} from '../components/SharedComponents';
import { TornHero, SETTINGS_HERO_SYMBOLS } from '../components/TornHero';
import { ScreenEntrance } from '../animations';

// ─── Segmented Row (matching Android's segmented control) ─────────────
const SegmentedRow: React.FC<{
  label: string;
  description?: string;
  options: { value: string; label: string }[];
  selected: string;
  onChange: (value: string) => void;
}> = ({ label, description, options, selected, onChange }) => {
  const { isDark } = useTheme();
  return (
    <div className="py-3">
      {(label || description) && (
        <div className="mb-2.5">
          {label && <div className="text-sm font-medium" style={{ color: getTextColor(isDark) }}>{label}</div>}
          {description && <div className="text-xs mt-0.5" style={{ color: isDark ? 'rgba(255,255,255,0.5)' : 'rgba(59,10,23,0.5)' }}>{description}</div>}
        </div>
      )}
      <div className="flex rounded-xl p-0.5" style={{ background: isDark ? 'rgba(255,255,255,0.06)' : 'rgba(59,10,23,0.04)' }}>
        {options.map(opt => (
          <button key={opt.value} onClick={() => onChange(opt.value)}
            className="flex-1 py-2 px-3 rounded-[10px] text-xs font-semibold transition-all"
            style={{
              background: selected === opt.value ? (isDark ? 'rgba(255,255,255,0.15)' : '#fff') : 'transparent',
              color: selected === opt.value ? getTextColor(isDark) : (isDark ? 'rgba(255,255,255,0.45)' : 'rgba(59,10,23,0.45)'),
              boxShadow: selected === opt.value ? (isDark ? '0 1px 3px rgba(0,0,0,0.3)' : '0 1px 3px rgba(0,0,0,0.08)') : 'none',
            }}>
            {opt.label}
          </button>
        ))}
      </div>
    </div>
  );
};

const SETTINGS_HERO_HEIGHT = 180;
const SETTINGS_TEAR_SEED = 0x5EED; // Android's SETTINGS_HERO_TEAR_SEED
const ROSE_WOOD = '#C46B7C';

const SettingsItem: React.FC<{
  icon: string; title: string; description?: string; onClick?: () => void; rightContent?: React.ReactNode;
}> = ({ icon, title, description, onClick, rightContent }) => {
  const { isDark } = useTheme();
  const [isPressed, setIsPressed] = useState(false);
  return (
    <button onClick={onClick} onMouseDown={() => setIsPressed(true)} onMouseUp={() => setIsPressed(false)}
      onMouseLeave={() => setIsPressed(false)}
      className="w-full flex items-center gap-3 p-3 rounded-xl transition-all duration-200 text-left"
      style={{ background: isPressed ? (isDark ? 'rgba(255,255,255,0.08)' : 'rgba(59,10,23,0.05)') : 'transparent' }}
      disabled={!onClick}>
      <div className="w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0"
        style={{ background: isDark ? 'rgba(255,255,255,0.1)' : 'rgba(59,10,23,0.05)' }}>
        <MaterialIcon name={icon} size={20} style={{ color: isDark ? 'rgba(255,255,255,0.7)' : 'rgba(59,10,23,0.7)' }} />
      </div>
      <div className="flex-1 min-w-0">
        <div className="text-sm font-medium" style={{ color: getTextColor(isDark) }}>{title}</div>
        {description && <div className="text-xs mt-0.5" style={{ color: isDark ? 'rgba(255,255,255,0.5)' : 'rgba(59,10,23,0.5)' }}>{description}</div>}
      </div>
      {rightContent || (onClick && <MaterialIcon name="chevron_right" size={18} style={{ color: isDark ? 'rgba(255,255,255,0.4)' : 'rgba(59,10,23,0.4)' }} />)}
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
      {themes.map(t => (
        <button key={t.id} onClick={() => onSelect(t.id)} className="flex-1 p-3 rounded-xl text-center transition-all"
          style={{
            background: selected === t.id ? `${t.color}20` : (isDark ? 'rgba(255,255,255,0.05)' : 'rgba(59,10,23,0.03)'),
            border: selected === t.id ? `2px solid ${t.color}` : '2px solid transparent',
          }}>
          <div className="w-8 h-8 rounded-full mx-auto mb-2" style={{ background: t.color }} />
          <div className="text-xs font-medium" style={{ color: getTextColor(isDark) }}>{t.name}</div>
        </button>
      ))}
    </div>
  );
};

export const SettingsScreen: React.FC = () => {
  const navigate = useNavigate();
  const { isDark, isAmoled, style, setThemeStyle, setDarkMode, setPastelColors } = useTheme();
  const [settings, setSettings] = useState({
    darkMode: isDark, pastelColors: false, heroGradient: true,
    notifications: true, soundEffects: true, autoSave: true, showPet: true, showQuests: true,
    voiceToText: localStorage.getItem('curio-voice-to-text') === 'true',
  });
  const [petChatter, setPetChatter] = useState(localStorage.getItem('curio-pet-chatter') || 'cozy');
  const [petGames, setPetGames] = useState(localStorage.getItem('curio-pet-games') || 'normal');

  const handleSettingChange = (key: string, value: boolean) => {
    setSettings(prev => ({ ...prev, [key]: value }));
    if (key === 'darkMode') setDarkMode(value);
    else if (key === 'pastelColors') setPastelColors(value);
    else if (key === 'voiceToText') localStorage.setItem('curio-voice-to-text', value ? 'true' : 'false');
  };

  const handlePetChatter = (val: string) => {
    setPetChatter(val);
    localStorage.setItem('curio-pet-chatter', val);
  };

  const handlePetGames = (val: string) => {
    setPetGames(val);
    localStorage.setItem('curio-pet-games', val);
  };

  return (
    <div className="min-h-screen pb-24 relative" style={{ backgroundColor: getBackgroundColor(isDark, isAmoled) }}>
      <CurioWatermarkBackdrop topClearance={SETTINGS_HERO_HEIGHT + 30} alphaScale={0.45} />

      {/* ── Torn Hero Banner ──────────────────────────────────────── */}
      <TornHero
        height={SETTINGS_HERO_HEIGHT}
        fill={ROSE_WOOD}
        ink="#fff"
        tearSeed={SETTINGS_TEAR_SEED}
        bold={true}
        symbols={SETTINGS_HERO_SYMBOLS}
        isDark={isDark}
      >
        <div className="flex flex-col h-full px-5 pt-[68px] pb-[18px] justify-end">
          {/* Back pill */}
          <button onClick={() => navigate(-1)} className="absolute top-0 left-5 w-10 h-10 rounded-full flex items-center justify-center"
            style={{ background: 'rgba(255,255,255,0.18)', border: '1px solid rgba(255,255,255,0.25)', marginTop: 'env(safe-area-inset-top, 12px)' }}>
            <MaterialIcon name="arrow_back" size={20} style={{ color: '#fff' }} />
          </button>
          <h1 className="text-xl font-extrabold text-white text-center" style={{ fontFamily: 'Geom, Inter, sans-serif' }}>
            Settings
          </h1>
          <p className="text-xs text-white/70 text-center mt-0.5">Customize your experience</p>
        </div>
      </TornHero>

      <ScreenEntrance>
        <div className="relative z-10 px-4">
          <div className="mb-6">
            <CurioSectionHeader title="Appearance" />
            <div className="rounded-2xl overflow-hidden" style={{ background: isDark ? 'rgba(255,255,255,0.05)' : 'rgba(59,10,23,0.03)' }}>
              <div className="p-4"><ThemePicker selected={style} onSelect={setThemeStyle} /></div>
              <div className="px-4 pb-2"><CurioToggle checked={settings.darkMode} onChange={(v) => handleSettingChange('darkMode', v)} label="Dark Mode" description="Use dark color scheme" /></div>
              <div className="px-4 pb-2"><CurioToggle checked={settings.pastelColors} onChange={(v) => handleSettingChange('pastelColors', v)} label="Pastel Colors" description="Use softer color palette" /></div>
              <div className="px-4 pb-4"><CurioToggle checked={settings.heroGradient} onChange={(v) => handleSettingChange('heroGradient', v)} label="Hero Gradient" description="Show gradient on hero cards" /></div>
            </div>
          </div>
          <div className="mb-6">
            <CurioSectionHeader title="Notifications" />
            <div className="rounded-2xl overflow-hidden" style={{ background: isDark ? 'rgba(255,255,255,0.05)' : 'rgba(59,10,23,0.03)' }}>
              <div className="px-4 py-2"><CurioToggle checked={settings.notifications} onChange={(v) => handleSettingChange('notifications', v)} label="Enable Notifications" description="Receive reminders and updates" /></div>
            </div>
          </div>
          <div className="mb-6">
            <CurioSectionHeader title="Data" />
            <div className="rounded-2xl overflow-hidden" style={{ background: isDark ? 'rgba(255,255,255,0.05)' : 'rgba(59,10,23,0.03)' }}>
              <div className="px-4 py-2"><CurioToggle checked={settings.autoSave} onChange={(v) => handleSettingChange('autoSave', v)} label="Auto Save" description="Automatically save entries" /></div>
              <SettingsItem icon="upload" title="Export Data" description="Download your entries" onClick={() => {}} />
              <SettingsItem icon="download" title="Import Data" description="Import from backup" onClick={() => {}} />
              <SettingsItem icon="delete_forever" title="Clear All Data" description="Remove all entries and settings" onClick={() => {}} />
            </div>
          </div>
          <div className="mb-6">
            <CurioSectionHeader title="Pet" />
            <div className="rounded-2xl overflow-hidden" style={{ background: isDark ? 'rgba(255,255,255,0.05)' : 'rgba(59,10,23,0.03)' }}>
              <div className="px-4 py-2"><CurioToggle checked={settings.showPet} onChange={(v) => handleSettingChange('showPet', v)} label="Show Pet" description="Display your companion on screen" /></div>
              <div className="px-4">
                <SegmentedRow
                  label="Pet Chatter"
                  description="How often your pet speaks"
                  options={[
                    { value: 'quiet', label: 'Quiet' },
                    { value: 'cozy', label: 'Cozy' },
                    { value: 'talkative', label: 'Talkative' },
                  ]}
                  selected={petChatter}
                  onChange={handlePetChatter}
                />
              </div>
              <div className="px-4 pb-2">
                <SegmentedRow
                  label="Pet Games"
                  description="How often your pet wants to play"
                  options={[
                    { value: 'relaxed', label: 'Relaxed' },
                    { value: 'normal', label: 'Normal' },
                    { value: 'eager', label: 'Eager' },
                  ]}
                  selected={petGames}
                  onChange={handlePetGames}
                />
              </div>
            </div>
          </div>
          <div className="mb-6">
            <CurioSectionHeader title="Features" />
            <div className="rounded-2xl overflow-hidden" style={{ background: isDark ? 'rgba(255,255,255,0.05)' : 'rgba(59,10,23,0.03)' }}>
              <div className="px-4 py-2"><CurioToggle checked={settings.showQuests} onChange={(v) => handleSettingChange('showQuests', v)} label="Show Quests" description="Display quest progress" /></div>
              <div className="px-4 py-2"><CurioToggle checked={settings.soundEffects} onChange={(v) => handleSettingChange('soundEffects', v)} label="Sound Effects" description="Play sounds on interactions" /></div>
              <div className="px-4 pb-2"><CurioToggle checked={settings.voiceToText} onChange={(v) => handleSettingChange('voiceToText', v)} label="Voice-to-Text" description="Use voice input for entry notes" /></div>
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
      </ScreenEntrance>
    </div>
  );
};

export default SettingsScreen;
