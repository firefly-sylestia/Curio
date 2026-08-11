// Curio Web App - Main App Component
// Routes, theme, bottom nav, floating pet

import { Routes, Route, useLocation, Navigate } from 'react-router-dom';
import { ThemeProvider } from './theme/ThemeContext';
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
import { PetDesignerScreen } from './screens/PetDesignerScreen';
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
          <Route path="/pet-designer" element={<PetDesignerScreen />} />
          <Route path="/quests" element={<QuestsScreen />} />
        </Routes>

        {showBottomNav && <BottomNav />}
        {showBottomNav && <FloatingPet />}
      </div>
    </ThemeProvider>
  );
};
export default App;
