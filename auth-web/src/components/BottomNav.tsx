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
            <span className={`curio-nav-icon ${tab.path === '/' ? 'curio-nav-home-icon' : ''}`}>{tab.path === '/' ? <svg viewBox="0 0 24 24" width="24" height="24" fill="none" aria-hidden="true"><path d="M3.5 10.7 12 3.8l8.5 6.9" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"/><path d="M5.8 9.5v9.1c0 .9.7 1.6 1.6 1.6h3.1v-5.6h3v5.6h3.1c.9 0 1.6-.7 1.6-1.6V9.5" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"/><path d="M9.8 7.4h4.4" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" opacity=".55"/></svg> : <MaterialIcon name={tab.icon} size={23} filled={active} /> }</span>
            <span>{tab.label}</span>
          </button>
        );
      })}
    </nav>
  );
};

export default BottomNav;
