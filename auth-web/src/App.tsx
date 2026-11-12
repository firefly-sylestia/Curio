// Curio Web App - Main App Component
// Routes, theme, bottom nav, floating pet

import { Routes, Route, useLocation, Navigate } from 'react-router-dom';
import { Analytics } from '@vercel/analytics/react';
import { ThemeProvider } from './theme/ThemeContext';
import BottomNav from './components/BottomNav';
import HomeScreen from './screens/HomeScreen';
import AccountScreen from './screens/AccountScreen';
import HelpSupportScreen from './screens/HelpSupportScreen';
import LegalScreen from './screens/LegalScreen';
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
import React, { useLayoutEffect } from 'react';
import './index.css';
import './mcu-interface.css';

const App: React.FC = () => {
  const location = useLocation();

  // Navigation should always open a page at its beginning, even when returning
  // to a route whose previous DOM state was scrolled near the bottom.
  useLayoutEffect(() => {
    window.scrollTo(0, 0);
    document.documentElement.scrollTop = 0;
    document.body.scrollTop = 0;

    document.querySelectorAll<HTMLElement>('*').forEach(element => {
      const overflowY = window.getComputedStyle(element).overflowY;
      if ((overflowY === 'auto' || overflowY === 'scroll') && element.scrollHeight > element.clientHeight) {
        element.scrollTop = 0;
      }
    });
  }, [location.pathname, location.search, location.key]);
  // Read localStorage directly during render — avoids stale-state race condition
  // where navigate sets localStorage but useState hasn't updated yet.
  const isOnboardingComplete = localStorage.getItem('onboarding_complete') === 'true';

  const immersiveRoutes = ['/reveal/', '/capture/', '/detail/'];
  const isImmersiveRoute = immersiveRoutes.some(route => location.pathname.startsWith(route));
  const isUtilityRoute = ['/support', '/privacy', '/terms', '/settings'].includes(location.pathname);
  const showBottomNav = isOnboardingComplete && location.pathname !== '/onboarding' && !isImmersiveRoute && !isUtilityRoute;
  // Force remount of routes when onboarding state changes
  const onboardKey = isOnboardingComplete ? 'done' : 'pending';

  return (
    <ThemeProvider>
      <div className="curio-web-app min-h-screen" key={onboardKey}>
        <div className="curio-route-transition" key={`${location.pathname}${location.search}`}>
        <Routes>
          <Route path="/onboarding" element={<OnboardingScreen />} />
          <Route path="/" element={isOnboardingComplete ? <HomeScreen /> : <Navigate to="/onboarding" replace />} />
          <Route path="/spin" element={<SpinScreen />} />
          <Route path="/spin/:categorySlug" element={<SpinScreen />} />
          <Route path="/cabinet" element={<Navigate to="/profile" replace />} />
          <Route path="/profile" element={<AccountScreen />} />
          <Route path="/settings" element={<SettingsScreen />} />
          <Route path="/support" element={<HelpSupportScreen />} />
          <Route path="/privacy" element={<LegalScreen type="privacy" />} />
          <Route path="/terms" element={<LegalScreen type="terms" />} />
          <Route path="/reveal/:categorySlug/:topicName" element={<TopicRevealScreen />} />
          <Route path="/detail/:entryId" element={<EntryDetailScreen />} />
          <Route path="/capture/:categorySlug/:topicName" element={<SaveCaptureScreen />} />
          <Route path="/browse" element={<TopicBrowserScreen />} />
          <Route path="/pet-designer" element={<PetDesignerScreen />} />
          <Route path="/quests" element={<QuestsScreen />} />
          <Route path="/history" element={<TopicHistoryScreen />} />
          <Route path="/manage-categories" element={<ManageCategoriesScreen />} />
        </Routes>
        </div>

        {showBottomNav && <BottomNav />}
        <Analytics />
      </div>
    </ThemeProvider>
  );
};
export default App;
