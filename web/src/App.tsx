// Curio Web App - Main App Component
// Sets up routing and theme provider

import React from 'react';
import { Routes, Route, useLocation, Navigate } from 'react-router-dom';
import { ThemeProvider, useTheme, getBackgroundColor, getTextColor } from './theme/ThemeContext';
import { BottomNav } from './components/BottomNav';
import { FloatingPet } from './components/FloatingPet';
import { HomeScreen } from './screens/HomeScreen';
import { SpinScreen } from './screens/SpinScreen';
import { CabinetScreen } from './screens/CabinetScreen';
import { ProfileScreen } from './screens/ProfileScreen';
import { SettingsScreen } from './screens/SettingsScreen';
import { TopicRevealScreen } from './screens/TopicRevealScreen';
import { SaveCaptureScreen } from './screens/SaveCaptureScreen';
import { EntryDetailScreen } from './screens/EntryDetailScreen';
import { QuestsScreen } from './screens/QuestsScreen';
import { OnboardingScreen } from './screens/OnboardingScreen';
import { getPetSystem } from './data/PetSystem';
import { getQuestSystem } from './data/QuestSystem';
import { useState, useEffect } from 'react';
import './index.css';

const App: React.FC = () => {
  const [isOnboardingComplete, setIsOnboardingComplete] = useState(() => {
    return localStorage.getItem('onboarding_complete') === 'true';
  });
  const location = useLocation();

  useEffect(() => {
    // Check if onboarding is complete whenever location changes
    const complete = localStorage.getItem('onboarding_complete') === 'true';
    setIsOnboardingComplete(complete);
  }, [location.pathname]);

  // Hide bottom nav on onboarding
  const showBottomNav = isOnboardingComplete && location.pathname !== '/onboarding';

  return (
    <ThemeProvider>
      <div className="min-h-screen">
          <Routes>
            {/* Onboarding */}
            <Route path="/onboarding" element={<OnboardingScreen />} />
            
            {/* Main Routes */}
            <Route path="/" element={isOnboardingComplete ? <HomeScreen /> : <Navigate to="/onboarding" replace />} />
            <Route path="/spin" element={<SpinScreen />} />
            <Route path="/spin/:categorySlug" element={<SpinScreen />} />
            <Route path="/cabinet" element={<CabinetScreen />} />
            <Route path="/profile" element={<ProfileScreen />} />
            <Route path="/settings" element={<SettingsScreen />} />
            
            {/* Topic Routes */}
            <Route path="/reveal/:categorySlug/:topicName" element={<TopicRevealScreen />} />
            <Route path="/detail/:entryId" element={<EntryDetailScreen />} />
            <Route path="/capture/:categorySlug/:topicName" element={<SaveCaptureScreen />} />
            <Route path="/pet-designer" element={<PetDesignerPlaceholder />} />
            <Route path="/quests" element={<QuestsScreen />} />
          </Routes>
          
          {/* Bottom Navigation */}
          {showBottomNav && <BottomNav />}
          
          {/* Floating Pet Companion */}
          {showBottomNav && <FloatingPet />}
        </div>
    </ThemeProvider>
  );
};

// Pet Designer placeholder
const PetDesignerPlaceholder: React.FC = () => {
  const { isDark } = useTheme();
  const [petSystem] = useState(() => getPetSystem());
  const [questSystem] = useState(() => getQuestSystem());
  
  const stage = petSystem.getStage();
  const level = questSystem.getLevel();
  const nextStage = petSystem.getNextStageInfo();
  
  return (
    <div className="min-h-screen flex items-center justify-center" style={{ backgroundColor: getBackgroundColor(isDark, false) }}>
      <div className="text-center px-6">
        <div className="text-8xl mb-4">
          {stage === 'baby' ? '🐣' : stage === 'evolved' ? '🦊' : '🐉'}
        </div>
        <h2 className="text-2xl font-bold mb-2" style={{ color: getTextColor(isDark) }}>
          {petSystem.getState().name}
        </h2>
        <p className="text-lg mb-4" style={{ color: isDark ? 'rgba(255,255,255,0.6)' : 'rgba(59,10,23,0.6)' }}>
          Stage: {stage.charAt(0).toUpperCase() + stage.slice(1)}
        </p>
        <p className="text-sm mb-6" style={{ color: isDark ? 'rgba(255,255,255,0.5)' : 'rgba(59,10,23,0.5)' }}>
          Level {level} • {nextStage ? `Evolves at level ${nextStage.requiredLevel}` : 'Fully evolved!'}
        </p>
        <div className="p-4 rounded-[16px]" style={{ background: isDark ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.02)' }}>
          <p className="text-sm" style={{ color: isDark ? 'rgba(255,255,255,0.5)' : 'rgba(59,10,23,0.5)' }}>
            Keep exploring and saving to help your pet grow!
          </p>
        </div>
      </div>
    </div>
  );
};

export default App;
