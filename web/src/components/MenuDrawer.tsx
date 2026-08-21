// Curio Web App - Menu Drawer
// Matches Android HomeDrawer: sky hero with constellation background below,
// bio fallback when no 2nd/3rd name, scrollable content

import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTheme, getTextColor } from '../theme/ThemeContext';
import { MaterialIcon } from './SharedComponents';
import { TornHero, DRAWER_HERO_SYMBOLS } from './TornHero';
import { Constellation } from './Constellation';

const MenuRow: React.FC<{
  icon: string;
  label: string;
  subtitle?: string;
  iconColor: string;
  expanded?: boolean;
  onClick: () => void;
}> = ({ icon, label, subtitle, iconColor, expanded, onClick }) => {
  const { isDark } = useTheme();
  const [pressed, setPressed] = useState(false);

  return (
    <button
      onClick={onClick}
      onMouseDown={() => setPressed(true)}
      onMouseUp={() => setPressed(false)}
      onMouseLeave={() => setPressed(false)}
      className="w-full flex items-center gap-3 px-5 py-3.5 rounded-2xl transition-all duration-150 text-left"
      style={{
        background: pressed
          ? (isDark ? 'rgba(255,255,255,0.06)' : 'rgba(0,0,0,0.03)')
          : isDark ? 'rgba(255,255,255,0.03)' : 'rgba(0,0,0,0.015)',
      }}
    >
      <div className="w-10 h-10 rounded-xl flex items-center justify-center"
        style={{ background: `${iconColor}20` }}>
        <MaterialIcon name={icon} size={22} style={{ color: iconColor }} />
      </div>
      <div className="flex-1 min-w-0">
        <span className="text-sm font-bold block" style={{ color: getTextColor(isDark) }}>{label}</span>
        {subtitle && (
          <span className="text-[11px] block mt-0.5" style={{ color: isDark ? 'rgba(255,255,255,0.4)' : 'rgba(0,0,0,0.4)' }}>
            {subtitle}
          </span>
        )}
      </div>
      <MaterialIcon
        name={expanded === true ? 'keyboard_arrow_up' : expanded === false ? 'keyboard_arrow_down' : 'chevron_right'}
        size={18}
        style={{ color: isDark ? 'rgba(255,255,255,0.25)' : 'rgba(0,0,0,0.2)' }}
      />
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
  const [displayName] = useState(() => localStorage.getItem('curio-display-name') || 'Explorer');
  const [bio] = useState(() => localStorage.getItem('curio-custom-tagline') || '');

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

  // Sky colors — match Android drawerSkyColors()
  const skyTop = isDark ? '#12313A' : '#C2E8DE';
  const skyBottom = isDark ? '#1D4750' : '#E9F6F0';
  const skyInk = isDark ? '#F4F1E7' : '#2C5A53';

  // Bio fallback: if no 2nd/3rd name parts, show bio
  const nameParts = displayName.trim().split(/\s+/).filter(Boolean);
  const firstName = nameParts[0] || displayName;
  const restOfName = nameParts.slice(1).join(' ');
  const subtitle = restOfName || bio;

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
        className="absolute top-0 left-0 bottom-0 w-[336px] max-w-[85vw] flex flex-col transition-transform duration-350"
        style={{
          transform: `translateX(${slideX}px)`,
          background: isDark ? '#1a1a2e' : '#FFFBF5',
          boxShadow: '4px 0 24px rgba(0,0,0,0.12)',
        }}
      >
        {/* Sky hero with constellation background below */}
        <div className="flex-shrink-0 relative">
          <TornHero
            height={186}
            fill={skyTop}
            ink={skyInk}
            tearSeed={0xD2A7E}
            bold={true}
            symbols={DRAWER_HERO_SYMBOLS}
            isDark={isDark}
            sheetColor={isDark ? '#1a1a2e' : '#FFFBF5'}
          >
            <div className="flex flex-col h-full px-5" style={{ paddingTop: 'calc(env(safe-area-inset-top, 8px) + 40px)' }}>
              <p className="text-[10px] font-bold tracking-[2px] uppercase mb-1"
                style={{ color: `${skyInk}D9` }}>CURIO</p>
              <h2 className="text-[22px] font-extrabold leading-tight"
                style={{ fontFamily: 'Geom, Inter, sans-serif', color: skyInk }}>
                Hi {firstName}
              </h2>
              {subtitle && (
                <p className="text-[13px] mt-1.5 max-w-full truncate"
                  style={{ color: `${skyInk}CC` }}>
                  {subtitle}
                </p>
              )}
            </div>
          </TornHero>
        </div>

        {/* Scrollable content with constellation background */}
        <div className="flex-1 overflow-y-auto">
          {/* Constellation background fills the area below the hero */}
          <div className="relative">
            <Constellation isDark={isDark} height={260} />

            {/* Menu items floating over the constellation */}
            <div className="relative z-10 px-3 pt-3 pb-2 space-y-1.5">
              {/* Curiosity map tap target */}
              <button
                onClick={() => handleNavigate('/stats')}
                className="w-full text-left px-4 py-3 rounded-2xl transition-all duration-150"
                style={{
                  background: isDark ? 'rgba(255,255,255,0.04)' : 'rgba(0,0,0,0.02)',
                }}
              >
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-xl flex items-center justify-center"
                    style={{ background: '#7FA0C820' }}>
                    <MaterialIcon name="auto_awesome" size={22} style={{ color: '#7FA0C8' }} />
                  </div>
                  <div className="flex-1">
                    <span className="text-sm font-bold block" style={{ color: getTextColor(isDark) }}>
                      Your Curiosity
                    </span>
                    <span className="text-[11px] block mt-0.5"
                      style={{ color: isDark ? 'rgba(255,255,255,0.4)' : 'rgba(0,0,0,0.4)' }}>
                      Stats, streaks & insights
                    </span>
                  </div>
                  <MaterialIcon name="chevron_right" size={18}
                    style={{ color: isDark ? 'rgba(255,255,255,0.25)' : 'rgba(0,0,0,0.2)' }} />
                </div>
              </button>
            </div>
          </div>

          {/* Menu rows below the constellation */}
          <div className="px-3 pt-1 pb-8 space-y-1">
            <MenuRow icon="workspace_premium" label="Quests & Levels" subtitle="Track your journey"
              iconColor="#E8A838" onClick={() => handleNavigate('/quests')} />

            {/* Divider */}
            <div className="my-2 mx-3 border-t"
              style={{ borderColor: isDark ? 'rgba(255,255,255,0.06)' : 'rgba(0,0,0,0.06)' }} />

            <MenuRow icon="info" label="About" subtitle="App info & more"
              iconColor="#C46B7C" onClick={() => handleNavigate('/settings')} />
          </div>
        </div>

        {/* Footer — scrolls with content */}
        <div className="px-5 py-3 border-t flex-shrink-0"
          style={{ borderColor: isDark ? 'rgba(255,255,255,0.06)' : 'rgba(0,0,0,0.06)' }}>
          <p className="text-[10px] opacity-40" style={{ color: getTextColor(isDark) }}>
            v1.0 · Made with curiosity ❤️
          </p>
        </div>
      </div>
    </div>
  );
};

export default MenuDrawer;
