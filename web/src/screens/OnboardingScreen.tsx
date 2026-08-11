// Curio Web App - Onboarding Screen (Premium Version)
// Smooth transitions and engaging welcome experience

import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTheme, getBackgroundColor, getTextColor } from '../theme/ThemeContext';
import { ALL_CATEGORIES } from '../data/categories';
import { CurioButton } from '../components/SharedComponents';

// ─── Onboarding Step Component ────────────────────────────────────────
const OnboardingStep: React.FC<{
  title: string;
  description: string;
  icon: string;
  children?: React.ReactNode;
}> = ({ title, description, icon, children }) => {
  const { isDark } = useTheme();
  const [isVisible, setIsVisible] = useState(false);

  useEffect(() => {
    const timer = setTimeout(() => setIsVisible(true), 100);
    return () => clearTimeout(timer);
  }, []);

  return (
    <div
      className="flex flex-col items-center justify-center min-h-[60vh] px-6 text-center transition-all duration-500"
      style={{
        opacity: isVisible ? 1 : 0,
        transform: `translateY(${isVisible ? 0 : 30}px)`,
      }}
    >
      <div className="text-8xl mb-8">{icon}</div>
      <h1
        className="text-3xl font-bold mb-4"
        style={{ color: getTextColor(isDark), fontFamily: 'Geom, sans-serif' }}
      >
        {title}
      </h1>
      <p
        className="text-lg mb-8 max-w-md"
        style={{ color: isDark ? 'rgba(255,255,255,0.7)' : 'rgba(59,10,23,0.7)' }}
      >
        {description}
      </p>
      {children}
    </div>
  );
};

// ─── Category Selection Step ──────────────────────────────────────────
const CategorySelectionStep: React.FC<{
  selectedCategories: string[];
  onToggle: (categoryId: string) => void;
}> = ({ selectedCategories, onToggle }) => {
  const { isDark } = useTheme();

  return (
    <div className="w-full max-w-md">
      <div className="grid grid-cols-3 gap-3">
        {ALL_CATEGORIES.filter(c => c.isReady).slice(0, 9).map((category) => {
          const isSelected = selectedCategories.includes(category.id);
          return (
            <button
              key={category.id}
              onClick={() => onToggle(category.id)}
              className="flex flex-col items-center gap-2 p-3 rounded-2xl transition-all duration-200"
              style={{
                background: isSelected 
                  ? `${category.accent}20`
                  : (isDark ? 'rgba(255,255,255,0.05)' : 'rgba(59,10,23,0.03)'),
                border: isSelected 
                  ? `2px solid ${category.accent}`
                  : '2px solid transparent',
                transform: isSelected ? 'scale(1.05)' : 'scale(1)',
              }}
            >
              <span className="text-2xl">{category.iconGlyph}</span>
              <span
                className="text-xs font-medium"
                style={{ color: getTextColor(isDark) }}
              >
                {category.displayName}
              </span>
            </button>
          );
        })}
      </div>
    </div>
  );
};

// ─── Main OnboardingScreen Component ──────────────────────────────────
export const OnboardingScreen: React.FC = () => {
  const navigate = useNavigate();
  const { isDark, isAmoled } = useTheme();
  
  const [currentStep, setCurrentStep] = useState(0);
  const [selectedCategories, setSelectedCategories] = useState<string[]>([]);
  const [isAnimating, setIsAnimating] = useState(false);

  const steps = [
    {
      icon: '🎲',
      title: 'Welcome to Curio',
      description: 'Discover fascinating topics from around the world. Spin the wheel and learn something new every day.',
    },
    {
      icon: '📚',
      title: 'Build Your Cabinet',
      description: 'Save the topics that interest you most. Your personal collection of knowledge grows with every spin.',
    },
    {
      icon: '🎯',
      title: 'Complete Quests',
      description: 'Earn XP and level up by completing daily challenges. Track your progress and unlock achievements.',
    },
    {
      icon: '🐾',
      title: 'Meet Your Companion',
      description: 'Your pet grows with you as you explore. Watch it evolve as you discover more topics.',
    },
  ];

  const handleNext = () => {
    if (isAnimating) return;
    
    setIsAnimating(true);
    
    if (currentStep < steps.length - 1) {
      setCurrentStep(prev => prev + 1);
    } else {
      // Complete onboarding
      localStorage.setItem('onboarding_complete', 'true');
      navigate('/');
    }
    
    setTimeout(() => setIsAnimating(false), 500);
  };

  const handleSkip = () => {
    localStorage.setItem('onboarding_complete', 'true');
    navigate('/');
  };

  const handleCategoryToggle = (categoryId: string) => {
    setSelectedCategories(prev => 
      prev.includes(categoryId)
        ? prev.filter(id => id !== categoryId)
        : [...prev, categoryId]
    );
  };

  return (
    <div
      className="min-h-screen flex flex-col"
      style={{ backgroundColor: getBackgroundColor(isDark, isAmoled) }}
    >
      {/* Header */}
      <div className="flex items-center justify-between px-6 pt-6">
        <div className="w-20" /> {/* Spacer */}
        <div className="flex gap-2">
          {steps.map((_, index) => (
            <div
              key={index}
              className="h-1 rounded-full transition-all duration-300"
              style={{
                width: index === currentStep ? 24 : 8,
                background: index === currentStep 
                  ? '#3B0A17'
                  : (isDark ? 'rgba(255,255,255,0.2)' : 'rgba(59,10,23,0.15)'),
              }}
            />
          ))}
        </div>
        <button
          onClick={handleSkip}
          className="text-sm font-medium w-20 text-right"
          style={{ color: isDark ? 'rgba(255,255,255,0.5)' : 'rgba(59,10,23,0.5)' }}
        >
          Skip
        </button>
      </div>

      {/* Content */}
      <div className="flex-1 flex flex-col">
        {currentStep < steps.length ? (
          <OnboardingStep
            title={steps[currentStep].title}
            description={steps[currentStep].description}
            icon={steps[currentStep].icon}
          />
        ) : (
          <OnboardingStep
            title="Choose Your Interests"
            description="Select categories you're interested in. You can always change this later."
            icon="🎯"
          >
            <CategorySelectionStep
              selectedCategories={selectedCategories}
              onToggle={handleCategoryToggle}
            />
          </OnboardingStep>
        )}
      </div>

      {/* Footer */}
      <div className="px-6 pb-8">
        <CurioButton
          onClick={handleNext}
          className="w-full"
        >
          {currentStep < steps.length - 1 ? 'Continue' : 'Get Started'}
        </CurioButton>
        
        {currentStep === steps.length - 1 && selectedCategories.length > 0 && (
          <p
            className="text-center text-sm mt-4"
            style={{ color: isDark ? 'rgba(255,255,255,0.5)' : 'rgba(59,10,23,0.5)' }}
          >
            {selectedCategories.length} categories selected
          </p>
        )}
      </div>
    </div>
  );
};

export default OnboardingScreen;
