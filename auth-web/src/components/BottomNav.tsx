// Curio Web App - Bottom Navigation
// Home · Explore · Shuffle · Account — focused discovery navigation

import React from 'react';
import LiquidGlass from 'liquid-glass-react';
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
  const { isDark } = useTheme();

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
      } as React.CSSProperties}
    >
      <LiquidGlass
        className="curio-nav-liquid-active"
        displacementScale={34}
        blurAmount={0.09}
        saturation={isDark ? 145 : 135}
        aberrationIntensity={0.55}
        elasticity={0.12}
        cornerRadius={18}
        padding="0"
        overLight={!isDark}
        mode="standard"
        style={{
          position: 'absolute',
          top: '50%',
          left: `${((activeIndex + 0.5) / tabs.length) * 100}%`,
          zIndex: 1,
        }}
      >
        <span className="curio-nav-liquid-active-shape" aria-hidden="true" />
      </LiquidGlass>
      {tabs.map(tab => {
        const active = isActive(tab.path);
        return (
          <button
            key={tab.path}
            onClick={() => navigate(tab.path)}
            className={active ? 'active' : ''}
            aria-current={active ? 'page' : undefined}
          >
            <span className={`curio-nav-icon ${tab.path === '/' ? 'curio-nav-home-icon' : ''}`}>{tab.path === '/' ? <svg viewBox="0 0 24 24" width="24" height="24" fill="none" aria-hidden="true"><path d="M3.25 10.55 12 3.5l8.75 7.05" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" strokeLinejoin="round"/><path d="M5.65 9.25v9.1c0 1.05.85 1.9 1.9 1.9h2.3v-5.8h4.3v5.8h2.3c1.05 0 1.9-.85 1.9-1.9v-9.1" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" strokeLinejoin="round"/></svg> : <MaterialIcon name={tab.icon} size={23} filled={active} /> }</span>
            <span>{tab.label}</span>
          </button>
        );
      })}
    </nav>
  );
};

export default BottomNav;
