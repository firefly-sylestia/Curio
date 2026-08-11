// Curio Web App - Menu Drawer
// Matches Android HomeDrawer: torn rose hero, menu items with icons

import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTheme, getTextColor } from '../theme/ThemeContext';
import { MaterialIcon } from './SharedComponents';

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
        {/* Rose hero banner */}
        <div className="relative w-full overflow-hidden flex-shrink-0"
          style={{ background: heroFill, minHeight: 160, paddingTop: 'env(safe-area-inset-top, 16px)' }}>
          {/* Watermark glyphs */}
          <div className="absolute inset-0 pointer-events-none opacity-[0.1]">
            <MaterialIcon name="casino" size={80} className="absolute" style={{ right: -10, top: 20, transform: 'rotate(12deg)' }} />
            <MaterialIcon name="auto_awesome" size={60} className="absolute" style={{ left: -5, bottom: 30, transform: 'rotate(-8deg)' }} />
          </div>

          {/* Torn bottom edge */}
          <div className="absolute -bottom-px left-0 right-0 h-4" style={{ background: isDark ? '#1a1a2e' : '#FFFBF5' }}>
            <svg viewBox="0 0 300 16" preserveAspectRatio="none" className="w-full h-full">
              <path d="M0,0 Q15,12 30,2 T60,4 T90,1 T120,5 T150,2 T180,6 T210,3 T240,5 T270,1 T300,3 L300,16 L0,16 Z"
                fill={heroFill} />
            </svg>
          </div>

          {/* Hero content */}
          <div className="relative z-10 px-5 pt-8 pb-10">
            <p className="text-white/70 text-xs font-medium mb-1">Welcome back</p>
            <h2 className="text-xl font-extrabold text-white" style={{ fontFamily: 'Geom, Inter, sans-serif' }}>
              Hi Explorer
            </h2>
            <p className="text-white/60 text-xs mt-1.5">Spin it. Explore it. Capture it.</p>
          </div>
        </div>

        {/* Menu items */}
        <div className="flex-1 overflow-y-auto px-3 pt-4 pb-8 space-y-1">
          <MenuRow icon="travel_explore" label="Browse Topics" iconColor="#38BDF8"
            onClick={() => handleNavigate('/browse')} />
          <MenuRow icon="workspace_premium" label="Quests & Levels" iconColor="#E8A838"
            onClick={() => handleNavigate('/quests')} />
          <MenuRow icon="history" label="Topic History" iconColor="#6366F1"
            onClick={() => handleNavigate('/cabinet')} />

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
