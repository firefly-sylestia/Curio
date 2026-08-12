// Curio Web App - Menu Drawer
// Matches Android HomeDrawer: torn rose hero, menu items with icons

import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTheme, getTextColor } from '../theme/ThemeContext';
import { MaterialIcon } from './SharedComponents';
import { TornHero, DRAWER_HERO_SYMBOLS } from './TornHero';

const MenuRow: React.FC<{
  icon: string;
  label: string;
  iconColor: string;
  onClick: () => void;
}> = ({ icon, label, iconColor, onClick }) => {
  const { isDark } = useTheme();
  const [pressed, setPressed] = useState(false);

  return (
    <button
      onClick={onClick}
      onMouseDown={() => setPressed(true)}
      onMouseUp={() => setPressed(false)}
      onMouseLeave={() => setPressed(false)}
      className="w-full flex items-center gap-3 px-5 py-3.5 rounded-xl transition-all duration-150 text-left"
      style={{
        background: pressed ? (isDark ? 'rgba(255,255,255,0.06)' : 'rgba(0,0,0,0.03)') : 'transparent',
      }}
    >
      <div className="w-9 h-9 rounded-xl flex items-center justify-center"
        style={{ background: `${iconColor}20` }}>
        <MaterialIcon name={icon} size={20} style={{ color: iconColor }} />
      </div>
      <span className="text-sm font-semibold flex-1" style={{ color: getTextColor(isDark) }}>{label}</span>
      <MaterialIcon name="chevron_right" size={18} style={{ color: isDark ? 'rgba(255,255,255,0.25)' : 'rgba(0,0,0,0.2)' }} />
    </button>
  );
};

export const MenuDrawer: React.FC<{
  isOpen: boolean;
  onClose: () => void;
}> = ({ isOpen, onClose }) => {
  const navigate = useNavigate();
  const { isDark } = useTheme();
  const [visible, setVisible] = useState(false);
  const [slideX, setSlideX] = useState(-320);

  useEffect(() => {
    if (isOpen) {
      setVisible(true);
      requestAnimationFrame(() => setSlideX(0));
    } else {
      setSlideX(-320);
      const t = setTimeout(() => setVisible(false), 350);
      return () => clearTimeout(t);
    }
  }, [isOpen]);

  if (!visible && !isOpen) return null;

  const heroFill = '#C46B7C'; // Rose-wood

  const handleNavigate = (path: string) => {
    onClose();
    setTimeout(() => navigate(path), 200);
  };

  return (
    <div
      className="fixed inset-0 z-50 transition-opacity duration-300"
      style={{ opacity: isOpen ? 1 : 0 }}
    >
      {/* Backdrop */}
      <div className="absolute inset-0 bg-black/40" onClick={onClose} />

      {/* Drawer sheet */}
      <div
        className="absolute top-0 left-0 bottom-0 w-[300px] max-w-[85vw] flex flex-col transition-transform duration-350"
        style={{
          transform: `translateX(${slideX}px)`,
          background: isDark ? '#1a1a2e' : '#FFFBF5',
          boxShadow: '4px 0 24px rgba(0,0,0,0.12)',
        }}
      >
        {/* Torn rose hero — matching Android HomeDrawerContent (186dp, seed 0xD2A7E) */}
        <div className="flex-shrink-0">
          <TornHero
            height={186}
            fill={heroFill}
            ink="#fff"
            tearSeed={0xD2A7E}
            bold={true}
            symbols={DRAWER_HERO_SYMBOLS}
            isDark={isDark}
            sheetColor={isDark ? '#1a1a2e' : '#FFFBF5'}
          >
            <div className="flex flex-col h-full px-5" style={{ paddingTop: 'calc(env(safe-area-inset-top, 8px) + 40px)' }}>
              <p className="text-white/70 text-xs font-medium mb-1">Welcome back</p>
              <h2 className="text-xl font-extrabold text-white" style={{ fontFamily: 'Geom, Inter, sans-serif' }}>
                Hi Explorer
              </h2>
              <p className="text-white/60 text-xs mt-1.5">Spin it. Explore it. Capture it.</p>
            </div>
          </TornHero>
        </div>

        {/* Menu items */}
        <div className="flex-1 overflow-y-auto px-3 pt-4 pb-8 space-y-1">
          <MenuRow icon="travel_explore" label="Browse Topics" iconColor="#38BDF8"
            onClick={() => handleNavigate('/browse')} />
          <MenuRow icon="workspace_premium" label="Quests & Levels" iconColor="#E8A838"
            onClick={() => handleNavigate('/quests')} />
          <MenuRow icon="history" label="Topic History" iconColor="#6366F1"
            onClick={() => handleNavigate('/history')} />

          <div className="my-3 mx-3 border-t" style={{ borderColor: isDark ? 'rgba(255,255,255,0.06)' : 'rgba(0,0,0,0.06)' }} />

          <MenuRow icon="settings" label="Settings" iconColor="rgba(0,0,0,0.45)"
            onClick={() => handleNavigate('/settings')} />
          <MenuRow icon="person" label="Profile" iconColor="rgba(0,0,0,0.45)"
            onClick={() => handleNavigate('/profile')} />
        </div>

        {/* Footer */}
        <div className="px-5 py-3 border-t" style={{ borderColor: isDark ? 'rgba(255,255,255,0.06)' : 'rgba(0,0,0,0.06)' }}>
          <p className="text-[10px] opacity-40" style={{ color: getTextColor(isDark) }}>
            Curio v1.0 — Keep exploring
          </p>
        </div>
      </div>
    </div>
  );
};

export default MenuDrawer;
