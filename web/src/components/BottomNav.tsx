// Curio Web App - Bottom Navigation
// Home · Spin · Cabinet — 3 tabs, matching Android

import React from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useTheme } from '../theme/ThemeContext';
import { MaterialIcon } from './SharedComponents';

const tabs = [
  { path: '/', label: 'Home', icon: 'cottage' },
  { path: '/spin', label: 'Spin', icon: 'casino' },
  { path: '/cabinet', label: 'Cabinet', icon: 'book_5' },
];

const BottomNav: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { isDark, isAmoled } = useTheme();

  // Highlight active tab: home is "/", spin starts with "/spin", cabinet starts with "/cabinet"
  const isActive = (path: string) => {
    if (path === '/') return location.pathname === '/' || location.pathname === '/home';
    return location.pathname.startsWith(path);
  };

  return (
    <div className="fixed bottom-0 left-0 right-0 z-50 pb-safe"
      style={{
        background: isAmoled ? '#000' : isDark ? '#1a1a2e' : '#FFFFFF',
        borderTop: `1px solid ${isDark ? 'rgba(255,255,255,0.06)' : 'rgba(0,0,0,0.06)'}`,
      }}>
      <div className="flex items-center justify-around h-16 px-2">
        {tabs.map(tab => {
          const active = isActive(tab.path);
          return (
            <button key={tab.path}
              onClick={() => navigate(tab.path)}
              className="flex flex-col items-center justify-center gap-0.5 flex-1 h-full transition-all"
              style={{ opacity: active ? 1 : 0.55 }}>
              <MaterialIcon
                name={tab.icon}
                size={24}
                filled={active}
                style={{
                  color: active
                    ? '#C46B7C'
                    : isDark ? 'rgba(255,255,255,0.55)' : 'rgba(59,10,23,0.55)',
                }}
              />
              <span className="text-[10px] font-semibold"
                style={{
                  color: active
                    ? '#C46B7C'
                    : isDark ? 'rgba(255,255,255,0.55)' : 'rgba(59,10,23,0.55)',
                }}>
                {tab.label}
              </span>
            </button>
          );
        })}
      </div>
    </div>
  );
};

export default BottomNav;
