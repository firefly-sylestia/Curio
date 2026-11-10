// Curio Web App - Bottom Navigation
// Home · Explore · Shuffle · Cabinet — discovery-first navigation

import React from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useTheme } from '../theme/ThemeContext';
import { MaterialIcon } from './SharedComponents';

const tabs = [
  { path: '/', label: 'Home', icon: 'cottage' },
  { path: '/browse', label: 'Explore', icon: 'explore' },
  { path: '/spin', label: 'Shuffle', icon: 'auto_awesome' },
  { path: '/cabinet', label: 'Cabinet', icon: 'inventory_2' },
];

const BottomNav: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { isAmoled } = useTheme();

  // Highlight the current discovery destination.
  const isActive = (path: string) => {
    if (path === '/') return location.pathname === '/' || location.pathname === '/home';
    return location.pathname.startsWith(path);
  };

  const activeIndex = Math.max(0, tabs.findIndex(tab => isActive(tab.path)));

  return (
    <nav
      className="curio-bottom-nav fixed bottom-0 left-0 right-0 z-50"
      aria-label="Primary navigation"
      style={{
        '--nav-index': activeIndex,
        '--nav-count': tabs.length,
        background: isAmoled ? 'rgba(0,0,0,.88)' : undefined,
      } as React.CSSProperties}
    >
      <span className="curio-nav-blob" aria-hidden="true" />
      {tabs.map(tab => {
        const active = isActive(tab.path);
        return (
          <button
            key={tab.path}
            onClick={() => navigate(tab.path)}
            className={active ? 'active' : ''}
            aria-current={active ? 'page' : undefined}
          >
            <MaterialIcon name={tab.icon} size={22} filled={active} />
            <span>{tab.label}</span>
          </button>
        );
      })}
    </nav>
  );
};

export default BottomNav;
