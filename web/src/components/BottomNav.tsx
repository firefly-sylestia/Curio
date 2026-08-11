// Curio Web App - Bottom Navigation (Premium Version)
// Matches Android app's premium design with smooth animations

import React from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useTheme } from '../theme/ThemeContext';

// ─── Navigation Item Component ────────────────────────────────────────
const NavItem: React.FC<{
  icon: React.ReactNode;
  label: string;
  isActive: boolean;
  onClick: () => void;
}> = ({ icon, label, isActive, onClick }) => {
  const { isDark } = useTheme();

  return (
    <button
      onClick={onClick}
      className="flex flex-col items-center gap-1 py-2 px-3 rounded-xl transition-all duration-200"
      style={{
        background: isActive 
          ? (isDark ? 'rgba(255,255,255,0.1)' : 'rgba(59,10,23,0.05)')
          : 'transparent',
      }}
    >
      <div
        className="transition-colors duration-200"
        style={{
          color: isActive 
            ? '#FF8FA3'
            : (isDark ? 'rgba(255,255,255,0.5)' : 'rgba(59,10,23,0.5)'),
        }}
      >
        {icon}
      </div>
      <span
        className="text-xs font-medium transition-colors duration-200"
        style={{
          color: isActive 
            ? '#FF8FA3'
            : (isDark ? 'rgba(255,255,255,0.5)' : 'rgba(59,10,23,0.5)'),
        }}
      >
        {label}
      </span>
    </button>
  );
};

// ─── Main BottomNav Component ─────────────────────────────────────────
export const BottomNav: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { isDark, isAmoled } = useTheme();

  const navItems = [
    {
      icon: (
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="m3 9 9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
          <polyline points="9,22 9,12 15,12 15,22" />
        </svg>
      ),
      label: 'Home',
      path: '/',
    },
    {
      icon: (
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <circle cx="12" cy="12" r="10" />
          <polygon points="10,8 16,12 10,16 10,8" />
        </svg>
      ),
      label: 'Spin',
      path: '/spin',
    },
    {
      icon: (
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M4 19.5v-15A2.5 2.5 0 0 1 6.5 2H20v20H6.5a2.5 2.5 0 0 1 0-5H20" />
        </svg>
      ),
      label: 'Cabinet',
      path: '/cabinet',
    },
    {
      icon: (
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2" />
          <circle cx="12" cy="7" r="4" />
        </svg>
      ),
      label: 'Profile',
      path: '/profile',
    },
    {
      icon: (
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <circle cx="12" cy="12" r="3" />
          <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z" />
        </svg>
      ),
      label: 'Settings',
      path: '/settings',
    },
  ];

  return (
    <div
      className="fixed bottom-0 left-0 right-0 z-40"
      style={{
        background: isAmoled 
          ? 'rgba(0,0,0,0.95)'
          : (isDark ? 'rgba(26,26,46,0.95)' : 'rgba(255,251,245,0.95)'),
        backdropFilter: 'blur(20px)',
        borderTop: `1px solid ${isDark ? 'rgba(255,255,255,0.1)' : 'rgba(59,10,23,0.1)'}`,
      }}
    >
      <div className="flex items-center justify-around px-2 py-2">
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
      </div>
      
      {/* Safe area spacer for iOS */}
      <div className="h-[env(safe-area-inset-bottom)]" />
    </div>
  );
};

export default BottomNav;
