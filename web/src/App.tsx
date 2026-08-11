// Curio Web App - Main App Component
// Routes, theme, bottom nav, floating pet

import { Routes, Route, useLocation, Navigate } from 'react-router-dom';
import { ThemeProvider, useTheme, getBackgroundColor, getTextColor } from './theme/ThemeContext';
import BottomNav from './components/BottomNav';
import { FloatingPet } from './components/FloatingPet';
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
import React from 'react';
import './index.css';

const App: React.FC = () => {
  const location = useLocation();
  // Read localStorage directly during render — avoids stale-state race condition
  // where navigate sets localStorage but useState hasn't updated yet.
  const isOnboardingComplete = localStorage.getItem('onboarding_complete') === 'true';

  const showBottomNav = isOnboardingComplete && location.pathname !== '/onboarding';
  // Force remount of routes when onboarding state changes
  const onboardKey = isOnboardingComplete ? 'done' : 'pending';

  return (
    <ThemeProvider>
      <div className="min-h-screen" key={onboardKey}>
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

        {showBottomNav && <BottomNav />}
        {showBottomNav && <FloatingPet />}
      </div>
    </ThemeProvider>
  );
};

const PetDesignerPlaceholder: React.FC = () => {
  const { isDark } = useTheme();
  return (
    <div className="min-h-screen flex items-center justify-center" style={{ backgroundColor: getBackgroundColor(isDark, false) }}>
      <div className="text-center px-6">
        <div className="w-20 h-20 mx-auto mb-4 rounded-full flex items-center justify-center"
          style={{ background: 'linear-gradient(135deg, #FF8FA3 0%, #FFD97D 100%)' }}>
          <span className="material-symbols-outlined text-4xl" style={{ color: '#fff' }}>pets</span>
        </div>
        <h2 className="text-2xl font-bold mb-2" style={{ color: getTextColor(isDark), fontFamily: 'Geom, Inter, sans-serif' }}>Pet Designer</h2>
        <p className="text-sm" style={{ color: isDark ? 'rgba(255,255,255,0.5)' : 'rgba(59,10,23,0.5)' }}>
          Coming soon — customize your companion
        </p>
      </div>
    </div>
  );
};

export default App;
