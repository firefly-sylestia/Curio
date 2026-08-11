// Curio Web App - Main App Component
// Sets up routing, theme, bottom nav, and menu drawer

import React from 'react';
import { Routes, Route, useLocation, Navigate } from 'react-router-dom';
import { ThemeProvider, useTheme, getBackgroundColor, getTextColor } from './theme/ThemeContext';
import { BottomNav } from './components/BottomNav';
import { FloatingPet } from './components/FloatingPet';
import MenuDrawer from './components/MenuDrawer';
import HomeScreen from './screens/HomeScreen';
import { SpinScreen } from './screens/SpinScreen';
import { CabinetScreen } from './screens/CabinetScreen';
import { ProfileScreen } from './screens/ProfileScreen';
import { SettingsScreen } from './screens/SettingsScreen';
import { TopicRevealScreen } from './screens/TopicRevealScreen';
import { SaveCaptureScreen } from './screens/SaveCaptureScreen';
import { EntryDetailScreen } from './screens/EntryDetailScreen';
import { QuestsScreen } from './screens/QuestsScreen';
import { TopicBrowserScreen } from './screens/TopicBrowserScreen';
import { OnboardingScreen } from './screens/OnboardingScreen';
import { useState, useEffect, createContext, useContext } from 'react';
import './index.css';

// Menu drawer context
export const MenuContext = createContext<{ openMenu: () => void }>({ openMenu: () => {} });
export const useMenu = () => useContext(MenuContext);

const App: React.FC = () => {
  const [isOnboardingComplete, setIsOnboardingComplete] = useState(() => {
    return localStorage.getItem('onboarding_complete') === 'true';
  });
  const [menuOpen, setMenuOpen] = useState(false);
  const location = useLocation();

  useEffect(() => {
    const complete = localStorage.getItem('onboarding_complete') === 'true';
    setIsOnboardingComplete(complete);
  }, [location.pathname]);

  const showBottomNav = isOnboardingComplete && location.pathname !== '/onboarding';

  return (
    <ThemeProvider>
      <MenuContext.Provider value={{ openMenu: () => setMenuOpen(true) }}>
        <div className="min-h-screen">
          <Routes>
            <Route path="/onboarding" element={<OnboardingScreen />} />
            <Route path="/" element={isOnboardingComplete ? <HomeScreen /> : <Navigate to="/onboarding" replace />} />
            <Route path="/spin" element={<SpinScreen />} />
            <Route path="/spin/:categorySlug" element={<SpinScreen />} />
            <Route path="/cabinet" element={<CabinetScreen />} />
            <Route path="/profile" element={<ProfileScreen />} />
            <Route path="/settings" element={<SettingsScreen />} />
            <Route path="/reveal/:categorySlug/:topicName" element={<TopicRevealScreen />} />
            <Route path="/detail/:entryId" element={<EntryDetailScreen />} />
            <Route path="/capture/:categorySlug/:topicName" element={<SaveCaptureScreen />} />
            <Route path="/browse" element={<TopicBrowserScreen />} />
            <Route path="/pet-designer" element={<PetDesignerPlaceholder />} />
            <Route path="/quests" element={<QuestsScreen />} />
          </Routes>

          {showBottomNav && <BottomNav onMenuOpen={() => setMenuOpen(true)} />}
          {showBottomNav && <FloatingPet />}

          {/* Menu drawer */}
          <MenuDrawer isOpen={menuOpen} onClose={() => setMenuOpen(false)} />
        </div>
      </MenuContext.Provider>
    </ThemeProvider>
  );
};

const PetDesignerPlaceholder: React.FC = () => {
  const { isDark } = useTheme();
  return (
    <div className="min-h-screen flex items-center justify-center" style={{ backgroundColor: getBackgroundColor(isDark, false) }}>
      <div className="text-center px-6">
        <h2 className="text-2xl font-bold mb-2" style={{ color: getTextColor(isDark) }}>Pet Designer</h2>
        <p className="text-sm" style={{ color: isDark ? 'rgba(255,255,255,0.5)' : 'rgba(59,10,23,0.5)' }}>
          Coming soon — customize your companion
        </p>
      </div>
    </div>
  );
};

export default App;
