// Curio Web App - Bottom Navigation
// Matches Android: Home, Spin, Cabinet + hamburger menu drawer trigger

import React from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useTheme } from '../theme/ThemeContext';
import { MaterialIcon } from './SharedComponents';

const NavItem: React.FC<{
  icon: string;
  label: string;
  isActive: boolean;
  onClick: () => void;
}> = ({ icon, label, isActive, onClick }) => {
  const { isDark } = useTheme();

  return (
    <button
      onClick={onClick}
      className="flex flex-col items-center gap-0.5 py-1.5 px-2 rounded-xl transition-all duration-200 min-w-[56px]"
      style={{
        background: isActive
          ? (isDark ? 'rgba(255,255,255,0.08)' : 'rgba(59,10,23,0.04)')
          : 'transparent',
      }}
    >
      <MaterialIcon
        name={icon}
        size={22}
        filled={isActive}
        style={{
          color: isActive ? '#FF8FA3' : (isDark ? 'rgba(255,255,255,0.45)' : 'rgba(59,10,23,0.45)'),
          transition: 'color 0.2s',
        }}
      />
      <span
        className="text-[10px] font-semibold transition-colors duration-200"
        style={{
          color: isActive ? '#FF8FA3' : (isDark ? 'rgba(255,255,255,0.45)' : 'rgba(59,10,23,0.45)'),
        }}
      >
        {label}
      </span>
    </button>
  );
};

export const BottomNav: React.FC<{ onMenuOpen: () => void }> = ({ onMenuOpen }) => {
  const navigate = useNavigate();
  const location = useLocation();
  const { isDark, isAmoled } = useTheme();

  const navItems = [
    { icon: 'home', label: 'Home', path: '/' },
    { icon: 'casino', label: 'Spin', path: '/spin' },
    { icon: 'book_5', label: 'Cabinet', path: '/cabinet' },
  ];

  return (
    <div
      className="fixed bottom-0 left-0 right-0 z-40 pb-[env(safe-area-inset-bottom)]"
      style={{
        background: isAmoled
          ? 'rgba(0,0,0,0.94)'
          : (isDark ? 'rgba(26,26,46,0.94)' : 'rgba(255,251,245,0.94)'),
        backdropFilter: 'blur(20px)',
        WebkitBackdropFilter: 'blur(20px)',
        borderTop: `1px solid ${isDark ? 'rgba(255,255,255,0.08)' : 'rgba(59,10,23,0.08)'}`,
      }}
    >
      <div className="flex items-center justify-between px-1 py-1.5 max-w-lg mx-auto">
        {/* Menu (hamburger) */}
        <button
          onClick={onMenuOpen}
          className="flex flex-col items-center gap-0.5 py-1.5 px-2 rounded-xl transition-all duration-200 min-w-[56px]"
        >
          <MaterialIcon name="menu" size={22}
            style={{ color: isDark ? 'rgba(255,255,255,0.45)' : 'rgba(59,10,23,0.45)' }} />
          <span className="text-[10px] font-semibold"
            style={{ color: isDark ? 'rgba(255,255,255,0.45)' : 'rgba(59,10,23,0.45)' }}>
            Menu
          </span>
        </button>

        {navItems.map((item) => {
          const isActive = location.pathname === item.path ||
            (item.path !== '/' && location.pathname.startsWith(item.path));
          return (
            <NavItem
              key={item.path}
              icon={item.icon}
              label={item.label}
              isActive={isActive}
              onClick={() => navigate(item.path)}
            />
          );
        })}

        {/* Spacer for balance */}
        <div className="min-w-[56px]" />
      </div>
    </div>
  );
};

export default BottomNav;
