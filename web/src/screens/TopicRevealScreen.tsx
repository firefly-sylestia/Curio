// Curio Web App - Topic Reveal Screen (Premium Version)
// Shows topic details with explore action, paper card style, and moodboard

import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useTheme, getBackgroundColor, getTextColor } from '../theme/ThemeContext';
import { getCategoryBySlug } from '../data/categories';
import { loadTopicsForCategory } from '../data/topics';
import { getExploreSessionSystem } from '../data/ExploreSession';
import { CurioPaperCard, CurioMoodboardCard, CurioBackButton } from '../components/SharedComponents';
import type { CurioTopic, CurioCategory } from '../types';
import { captureRepository, generateId, serializeTags } from '../db/database';

export const TopicRevealScreen: React.FC = () => {
  const navigate = useNavigate();
  const { categorySlug, topicName } = useParams();
  const { isDark, isAmoled } = useTheme();
  const [category, setCategory] = useState<CurioCategory | null>(null);
  const [topic, setTopic] = useState<CurioTopic | null>(null);
  const [isSaving, setIsSaving] = useState(false);
  const [isExplorePressed, setIsExplorePressed] = useState(false);
  const [isSavePressed, setIsSavePressed] = useState(false);
  const [exploreSession] = useState(() => getExploreSessionSystem());
  const [sessionActive, setSessionActive] = useState(false);

  useEffect(() => {
    const loadTopic = async () => {
      if (categorySlug) {
        const foundCategory = getCategoryBySlug(categorySlug);
        if (foundCategory) {
          setCategory(foundCategory);
          
          // Load real topic data
          const topics = await loadTopicsForCategory(foundCategory.id);
          const foundTopic = topics.find(t => t.id === topicName) || topics[0];
          
          if (foundTopic) {
            setTopic(foundTopic);
          } else {
            // Fallback placeholder
            setTopic({
              id: `mock-${categorySlug}`,
              categoryId: foundCategory.id,
              subtype: 'Topic',
              name: topicName?.replace(/-/g, ' ') || 'Unknown Topic',
              teaser: 'A fascinating topic to explore.',
              imageUrl: '',
              actionPrompt: {
                verb: 'Explore',
                targetName: 'this topic',
                durationMinutes: 30,
                instruction: 'Take time to learn something new.',
              },
              aliases: [],
              relatedTopicIds: [],
              difficulty: 1,
              weight: 100,
              curatedBy: 'human',
              curatedDate: '2026-01-15',
            });
          }
        }
      }
    };
    
    loadTopic();
  }, [categorySlug, topicName]);

  const handleStartExplore = () => {
    if (topic && category) {
      exploreSession.startSession(topic, category.id);
      setSessionActive(true);
    }
  };

  const handleSave = async () => {
    if (!topic || !category) return;
    
    setIsSaving(true);
    try {
      const captureData = {
        durationSeconds: 0,
        notes: '',
      };
      
      const entry = {
        id: generateId(),
        topicId: topic.id,
        categoryId: category.id,
        topicName: topic.name,
        topicSubtype: topic.subtype,
        topicTeaser: topic.teaser,
        format: category.defaultFormat,
        capturedAtMillis: Date.now(),
        title: topic.name,
        formatDataJson: JSON.stringify(captureData),
        tagsJson: serializeTags([]),
        isLegacy: false,
      };
      
      await captureRepository.insert(entry);
      
      // Update stats
      const currentEntries = parseInt(localStorage.getItem('curio-total-topics') || '0');
      localStorage.setItem('curio-total-topics', (currentEntries + 1).toString());
      
      // Add XP
      const currentXp = parseInt(localStorage.getItem('curio-xp') || '0');
      localStorage.setItem('curio-xp', (currentXp + 50).toString());
      
      navigate('/cabinet');
    } catch (error) {
      console.error('Failed to save:', error);
    } finally {
      setIsSaving(false);
    }
  };

  if (!category || !topic) {
    return (
      <div
        className="min-h-screen flex items-center justify-center"
        style={{ backgroundColor: getBackgroundColor(isDark, isAmoled) }}
      >
        <div className="text-center">
          <div className="w-8 h-8 border-4 border-t-transparent rounded-full animate-spin mx-auto" style={{ borderColor: '#FF8FA3', borderTopColor: 'transparent' }} />
          <p className="mt-4" style={{ color: isDark ? 'rgba(255,255,255,0.5)' : 'rgba(59,10,23,0.5)' }}>
            Loading topic...
          </p>
        </div>
      </div>
    );
  }

  const actionEmoji = 
    topic.actionPrompt.verb === 'Listen' ? '🎵' :
    topic.actionPrompt.verb === 'Watch' ? '🎬' :
    topic.actionPrompt.verb === 'Read' ? '📖' :
    topic.actionPrompt.verb === 'Explore' ? '🔍' : '✨';

  return (
    <div
      className="min-h-screen pb-24 relative"
      style={{ backgroundColor: getBackgroundColor(isDark, isAmoled) }}
    >
      {/* Watermark backdrop */}
      <div className="absolute inset-0 pointer-events-none overflow-hidden">
        <div
          className="absolute text-[160px] opacity-[0.03]"
          style={{
            right: -20,
            top: 120,
            transform: 'rotate(-15deg)',
            color: isDark ? 'white' : category.accent,
          }}
        >
          {category.iconGlyph}
        </div>
      </div>

      {/* Sticky Hero Header */}
      <div
        className="sticky top-0 z-20 px-4 pt-4 pb-3"
        style={{
          background: getBackgroundColor(isDark, isAmoled),
          borderBottom: `1px solid ${isDark ? 'rgba(255,255,255,0.06)' : 'rgba(59,10,23,0.06)'}`,
        }}
      >
        <div className="flex items-center gap-3">
          <CurioBackButton onClick={() => navigate(-1)} />
          <div className="flex-1 min-w-0">
            <h1
              className="text-lg font-bold truncate"
              style={{ color: getTextColor(isDark), fontFamily: 'Geom, sans-serif' }}
            >
              {topic.name}
            </h1>
            <p
              className="text-xs"
              style={{ color: isDark ? 'rgba(255,255,255,0.5)' : 'rgba(59,10,23,0.5)' }}
            >
              {category.displayName} • {topic.subtype}
            </p>
          </div>
        </div>
      </div>

      {/* Content */}
      <div className="relative z-10 px-5 pt-6 max-w-lg mx-auto">
        {/* Moodboard Hero */}
        <CurioMoodboardCard accent={category.accent} className="mb-6">
          {/* Hero image or gradient */}
          <div
            className="relative -mx-5 -mt-5 mb-4 h-44 overflow-hidden rounded-t-2xl"
            style={{
              background: topic.imageUrl
                ? `url(${topic.imageUrl}) center/cover`
                : `linear-gradient(135deg, ${category.accent} 0%, ${category.lightAccent} 100%)`,
            }}
          >
            {/* Overlay */}
            <div
              className="absolute inset-0"
              style={{
                background: `linear-gradient(180deg, transparent 30%, ${isDark ? 'rgba(0,0,0,0.4)' : 'rgba(0,0,0,0.3)'} 100%)`,
              }}
            />
            
            {/* Category badge */}
            <div
              className="absolute top-3 right-3 px-3 py-1 rounded-full text-xs font-medium backdrop-blur-sm"
              style={{
                background: 'rgba(0,0,0,0.35)',
                color: 'white',
              }}
            >
              {category.iconGlyph} {category.displayName}
            </div>
            
            {/* Watermark glyph */}
            <div
              className="absolute -bottom-6 -right-4 text-[100px] opacity-20 pointer-events-none"
              style={{ color: 'white' }}
            >
              {category.iconGlyph}
            </div>
          </div>
          
          {/* Title */}
          <h2
            className="text-2xl font-bold mb-1"
            style={{ color: getTextColor(isDark), fontFamily: 'Geom, sans-serif' }}
          >
            {topic.name}
          </h2>
          
          {/* Meta row */}
          <div className="flex items-center gap-2 mb-3 flex-wrap">
            <span
              className="text-xs px-2 py-1 rounded-full font-medium"
              style={{
                background: `${category.accent}18`,
                color: category.accent,
              }}
            >
              {topic.subtype}
            </span>
            <span
              className="text-xs"
              style={{ color: isDark ? 'rgba(255,255,255,0.5)' : 'rgba(59,10,23,0.5)' }}
            >
              ⏱️ {topic.actionPrompt.durationMinutes} min • difficulty {topic.difficulty}/5
            </span>
          </div>
          
          {/* Teaser */}
          <p
            className="text-sm leading-relaxed mb-1"
            style={{ color: isDark ? 'rgba(255,255,255,0.8)' : 'rgba(59,10,23,0.85)' }}
          >
            {topic.teaser}
          </p>
        </CurioMoodboardCard>

        {/* Explore Action - Paper Card */}
        <div className="mb-6">
          <CurioPaperCard variant="ruled" watermark={category.iconGlyph} className="mb-2">
            <div className="flex items-center gap-2 mb-2 pl-2">
              <span className="text-xl">{actionEmoji}</span>
              <h3
                className="font-semibold"
                style={{ color: getTextColor(isDark), fontFamily: 'Patrick Hand, cursive' }}
              >
                {topic.actionPrompt.verb} {topic.actionPrompt.targetName}
              </h3>
            </div>
            
            <p
              className="text-sm leading-relaxed pl-2"
              style={{
                color: isDark ? 'rgba(255,255,255,0.75)' : 'rgba(59,10,23,0.75)',
                fontFamily: 'Patrick Hand, cursive',
                fontSize: '15px',
              }}
            >
              {topic.actionPrompt.instruction}
            </p>
          </CurioPaperCard>
        </div>

        {/* Action Buttons */}
        <div className="space-y-3 mb-8">
          {!sessionActive ? (
            <button
              onClick={handleStartExplore}
              onMouseDown={() => setIsExplorePressed(true)}
              onMouseUp={() => setIsExplorePressed(false)}
              onMouseLeave={() => setIsExplorePressed(false)}
              className="w-full py-4 rounded-[16px] font-semibold transition-all duration-200"
              style={{
                background: isDark ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.05)',
                color: getTextColor(isDark),
                transform: isExplorePressed ? 'scale(0.98)' : 'scale(1)',
                border: `1px solid ${isDark ? 'rgba(255,255,255,0.1)' : 'rgba(59,10,23,0.1)'}`,
              }}
            >
              🎧 Start Exploring ({topic.actionPrompt.durationMinutes} min)
            </button>
          ) : (
            <div
              className="p-4 rounded-[16px] text-center"
              style={{ background: `${category.accent}18`, border: `1px solid ${category.accent}30` }}
            >
              <div className="text-sm font-medium mb-1" style={{ color: category.accent }}>
                ✨ Exploring... ({exploreSession.getFormattedElapsed()})
              </div>
              <div
                className="text-xs"
                style={{ color: isDark ? 'rgba(255,255,255,0.5)' : 'rgba(59,10,23,0.5)' }}
              >
                Take your time, come back to save your notes
              </div>
            </div>
          )}
          
          <button
            onClick={handleSave}
            disabled={isSaving}
            onMouseDown={() => setIsSavePressed(true)}
            onMouseUp={() => setIsSavePressed(false)}
            onMouseLeave={() => setIsSavePressed(false)}
            className="w-full py-4 rounded-[16px] font-semibold transition-all duration-200 disabled:opacity-50"
            style={{
              background: `linear-gradient(135deg, ${category.accent} 0%, ${category.lightAccent} 100%)`,
              color: 'white',
              transform: isSavePressed ? 'scale(0.98)' : 'scale(1)',
              boxShadow: `0 8px 24px ${category.accent}33`,
            }}
          >
            {isSaving ? 'Saving...' : '💾 Save to Cabinet'}
          </button>
          
          <button
            onClick={() => navigate('/spin')}
            className="w-full py-4 rounded-[16px] font-semibold transition-all duration-200"
            style={{
              background: 'transparent',
              color: isDark ? 'rgba(255,255,255,0.6)' : 'rgba(59,10,23,0.6)',
              border: `1px solid ${isDark ? 'rgba(255,255,255,0.15)' : 'rgba(59,10,23,0.15)'}`,
            }}
          >
            🎲 Spin Again
          </button>
        </div>

        {/* Related Topics placeholder */}
        {topic.relatedTopicIds.length > 0 && (
          <div className="mb-8">
            <h3
              className="text-lg font-bold mb-3"
              style={{ color: getTextColor(isDark), fontFamily: 'Geom, sans-serif' }}
            >
              Related Topics
            </h3>
            <div className="flex gap-2 overflow-x-auto pb-2">
              {topic.relatedTopicIds.slice(0, 5).map((relatedId, i) => (
                <div
                  key={relatedId}
                  className="flex-shrink-0 px-3 py-2 rounded-full text-sm transition-all duration-200"
                  style={{
                    background: isDark ? 'rgba(255,255,255,0.05)' : 'rgba(59,10,23,0.03)',
                    color: getTextColor(isDark),
                    border: `1px solid ${isDark ? 'rgba(255,255,255,0.08)' : 'rgba(59,10,23,0.08)'}`,
                  }}
                >
                  {topic.relatedTopicIds[i] ? `#${topic.relatedTopicIds[i]}` : '✦'}
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default TopicRevealScreen;
