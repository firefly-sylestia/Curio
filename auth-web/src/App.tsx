// Curio Web App - Main App Component
// Routes, theme, bottom nav, floating pet

import { Routes, Route, useLocation, Navigate } from 'react-router-dom';
import { ThemeProvider } from './theme/ThemeContext';
import BottomNav from './components/BottomNav';
import HomeScreen from './screens/HomeScreen';
import AccountScreen from './screens/AccountScreen';
import { SpinScreen } from './screens/SpinScreen';
import { SettingsScreen } from './screens/SettingsScreen';
import { TopicRevealScreen } from './screens/TopicRevealScreen';
import { SaveCaptureScreen } from './screens/SaveCaptureScreen';
import { EntryDetailScreen } from './screens/EntryDetailScreen';
import { QuestsScreen } from './screens/QuestsScreen';
import { TopicBrowserScreen } from './screens/TopicBrowserScreen';
import { OnboardingScreen } from './screens/OnboardingScreen';
import { PetDesignerScreen } from './screens/PetDesignerScreen';
import { TopicHistoryScreen } from './screens/TopicHistoryScreen';
import { ManageCategoriesScreen } from './screens/ManageCategoriesScreen';
import React from 'react';
import './index.css';
import './mcu-interface.css';

const App: React.FC = () => {
  const location = useLocation();
  // Read localStorage directly during render — avoids stale-state race condition
  // where navigate sets localStorage but useState hasn't updated yet.
  const isOnboardingComplete = localStorage.getItem('onboarding_complete') === 'true';

  const immersiveRoutes = ['/reveal/', '/capture/', '/detail/'];
  const isImmersiveRoute = immersiveRoutes.some(route => location.pathname.startsWith(route));
  const showBottomNav = isOnboardingComplete && location.pathname !== '/onboarding' && !isImmersiveRoute;
  // Force remount of routes when onboarding state changes
  const onboardKey = isOnboardingComplete ? 'done' : 'pending';

  return (
    <ThemeProvider>
      <div className="curio-web-app min-h-screen" key={onboardKey}>
        <Routes>
          <Route path="/onboarding" element={<OnboardingScreen />} />
          <Route path="/" element={isOnboardingComplete ? <HomeScreen /> : <Navigate to="/onboarding" replace />} />
          <Route path="/spin" element={<SpinScreen />} />
          <Route path="/spin/:categorySlug" element={<SpinScreen />} />
          <Route path="/cabinet" element={<Navigate to="/profile" replace />} />
          <Route path="/profile" element={<AccountScreen />} />
          <Route path="/settings" element={<SettingsScreen />} />
          <Route path="/reveal/:categorySlug/:topicName" element={<TopicRevealScreen />} />
          <Route path="/detail/:entryId" element={<EntryDetailScreen />} />
          <Route path="/capture/:categorySlug/:topicName" element={<SaveCaptureScreen />} />
          <Route path="/browse" element={<TopicBrowserScreen />} />
          <Route path="/pet-designer" element={<PetDesignerScreen />} />
          <Route path="/quests" element={<QuestsScreen />} />
          <Route path="/history" element={<TopicHistoryScreen />} />
          <Route path="/manage-categories" element={<ManageCategoriesScreen />} />
        </Routes>

        {showBottomNav && <BottomNav />}
      </div>
    </ThemeProvider>
  );
};
export default App;
