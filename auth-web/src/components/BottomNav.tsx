// Curio Web App - Bottom Navigation
// Home · Explore · Shuffle · Account — focused discovery navigation

import React from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useTheme } from '../theme/ThemeContext';
import { MaterialIcon } from './SharedComponents';

const tabs = [
  { path: '/', label: 'Home', icon: 'home' },
  { path: '/browse', label: 'Explore', icon: 'explore' },
  { path: '/spin', label: 'Shuffle', icon: 'auto_awesome' },
  { path: '/profile', label: 'Account', icon: 'person' },
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
            <span className={`curio-nav-icon ${tab.path === '/' ? 'curio-nav-home-icon' : ''}`}><MaterialIcon name={tab.icon} size={23} filled={active} /></span>
            <span>{tab.label}</span>
          </button>
        );
      })}
    </nav>
  );
};

export default BottomNav;
