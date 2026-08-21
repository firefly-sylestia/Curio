// Curio Web App - Topic Reveal Screen
// Matches Android: hero card, floating Category+Favorite bar, byline for quotes,
// Express yourself + Start exploring buttons, teaser, action prompt

import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useTheme, getBackgroundColor, getTextColor } from '../theme/ThemeContext';
import { getCategoryBySlug } from '../data/categories';
import { loadTopicsForCategory } from '../data/topics';
import { CurioPaperCard, CurioMoodboardCard, CurioBackButton, MaterialIcon, CurioWatermarkBackdrop } from '../components/SharedComponents';
import type { CurioTopic, CurioCategory } from '../types';
import { captureRepository, generateId, serializeTags } from '../db/database';
import { MorphEntrance, ContentEntrance } from '../animations';

const hexToRgb = (hex: string): [number, number, number] => {
  const r = parseInt(hex.slice(1, 3), 16);
  const g = parseInt(hex.slice(3, 5), 16);
  const b = parseInt(hex.slice(5, 7), 16);
  return [r, g, b];
};

// ── Floating Category + Favorite Bar ──────────────────────────────
// Matches Android's RevealCategoryFavoriteBar:
// - Category pill: auto-expands from icon-only to showing name on entry
// - Favorite pill: expands to show "Favorite" label when tapped

const FloatingCategoryFavoriteBar: React.FC<{
  category: CurioCategory;
  isFavorited: boolean;
  onFavorite: () => void;
}> = ({ category, isFavorited, onFavorite }) => {
  const { isDark } = useTheme();
  const [expanded, setExpanded] = useState(false);

  // Auto-expand category pill on mount (matches Android LaunchedEffect)
  useEffect(() => {
    const t = setTimeout(() => setExpanded(true), 100);
    return () => clearTimeout(t);
  }, []);

  const containerBg = isDark ? 'rgba(30,30,50,0.92)' : 'rgba(255,253,249,0.92)';
  const accent = category.accent;
  const ink = isDark ? '#EDE7DC' : '#232A35';

  return (
    <div className="fixed bottom-6 left-1/2 -translate-x-1/2 z-40"
      style={{ paddingBottom: 'env(safe-area-inset-bottom, 0px)' }}>
      <div className="flex items-center gap-1.5 px-2 py-2 rounded-full"
        style={{
          background: containerBg,
          boxShadow: '0 4px 20px rgba(0,0,0,0.18)',
          backdropFilter: 'blur(12px)',
        }}>
        {/* Category pill — auto-expands on entry */}
        <div className="flex items-center justify-center rounded-full overflow-hidden transition-all duration-500"
          style={{
            width: expanded ? 160 : 56,
            height: 52,
            background: expanded ? accent : (isDark ? 'rgba(255,255,255,0.08)' : 'rgba(0,0,0,0.05)'),
            transitionTimingFunction: 'cubic-bezier(0.34, 1.56, 0.64, 1)',
          }}>
          <MaterialIcon name={category.iconGlyph} size={24}
            style={{ color: expanded ? ink : (isDark ? 'rgba(255,255,255,0.6)' : 'rgba(0,0,0,0.6)') }} />
          {expanded && (
            <span className="ml-1.5 text-[15px] font-medium truncate max-w-[120px]"
              style={{
                color: ink,
                fontFamily: 'Changa One, Inter, sans-serif',
                opacity: expanded ? 1 : 0,
                transition: 'opacity 0.3s ease 0.1s',
              }}>
              {category.displayName}
            </span>
          )}
        </div>

        {/* Favorite pill — expands when favorited */}
        <button onClick={onFavorite}
          className="flex items-center justify-center rounded-full overflow-hidden transition-all duration-500"
          style={{
            width: isFavorited ? 136 : 64,
            height: 52,
            background: isFavorited ? accent : 'transparent',
            transitionTimingFunction: 'cubic-bezier(0.34, 1.56, 0.64, 1)',
          }}>
          <MaterialIcon
            name={isFavorited ? 'star' : 'star_outline'}
            size={26}
            style={{ color: isFavorited ? ink : (isDark ? 'rgba(255,255,255,0.6)' : 'rgba(0,0,0,0.6)') }} />
          {isFavorited && (
            <span className="ml-1.5 text-[15px] font-medium"
              style={{
                color: ink,
                fontFamily: 'Changa One, Inter, sans-serif',
                opacity: isFavorited ? 1 : 0,
                transition: 'opacity 0.3s ease 0.1s',
              }}>
              Favorite
            </span>
          )}
        </button>
      </div>
    </div>
  );
};

// ═══════════════════════════════════════════════════════════════════════════

export const TopicRevealScreen: React.FC = () => {
  const navigate = useNavigate();
  const { categorySlug, topicName } = useParams();
  const { isDark, isAmoled } = useTheme();
  const [category, setCategory] = useState<CurioCategory | null>(null);
  const [topic, setTopic] = useState<CurioTopic | null>(null);
  const [saved, setSaved] = useState(false);
  const [isFavorited, setIsFavorited] = useState(false);

  useEffect(() => {
    const loadTopic = async () => {
      if (categorySlug) {
        const foundCategory = getCategoryBySlug(categorySlug);
        if (foundCategory) {
          setCategory(foundCategory);
          const topics = await loadTopicsForCategory(foundCategory.id);
          const foundTopic = topics.find(t => t.id === topicName) || topics[0];
          setTopic(foundTopic || null);
        }
      }
    };
    loadTopic();
  }, [categorySlug, topicName]);

  // Load favorite state from localStorage
  useEffect(() => {
    if (topic) {
      const key = `curio-fav-${topic.categoryId}-${topic.id}`;
      setIsFavorited(localStorage.getItem(key) === 'true');
    }
  }, [topic]);

  const handleExpressYourself = () => {
    if (topic && category) {
      navigate(`/capture/${category.id.toLowerCase()}/${topic.id}`);
    }
  };

  const handleStartExploring = () => {
    if (topic) {
      const query = encodeURIComponent(`${topic.name} ${topic.subtype}`);
      const isMusic = ['Album', 'Artist', 'Song'].includes(topic.subtype);
      if (isMusic) {
        const service = localStorage.getItem('curio-music-service') || 'youtube_music';
        const musicUrls: Record<string, string> = {
          youtube_music: `https://music.youtube.com/search?q=${query}`,
          apple_music: `https://music.apple.com/search?term=${query}`,
          spotify: `https://open.spotify.com/search/${query}`,
        };
        window.open(musicUrls[service] || musicUrls.youtube_music, '_blank');
        return;
      }
      const engine = localStorage.getItem('curio-search-engine') || 'google';
      const urls: Record<string, string> = {
        google: `https://www.google.com/search?q=${query}`,
        duckduckgo: `https://duckduckgo.com/?q=${query}`,
        bing: `https://www.bing.com/search?q=${query}`,
        brave: `https://search.brave.com/search?q=${query}`,
        ecosia: `https://www.ecosia.org/search?q=${query}`,
        startpage: `https://www.startpage.com/sp/search?query=${query}`,
        yahoo: `https://search.yahoo.com/search?p=${query}`,
      };
      window.open(urls[engine] || urls.google, '_blank');
    }
  };

  const handleQuickSave = async () => {
    if (!topic || !category || saved) return;
    try {
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
        formatDataJson: JSON.stringify({ notes: '' }),
        tagsJson: serializeTags([]),
        isLegacy: false,
        sessionTimeMillis: 0,
      };
      await captureRepository.insert(entry);
      setSaved(true);
    } catch (error) {
      console.error('Failed to save:', error);
    }
  };

  const handleToggleFavorite = () => {
    if (!topic) return;
    const key = `curio-fav-${topic.categoryId}-${topic.id}`;
    const next = !isFavorited;
    setIsFavorited(next);
    if (next) {
      localStorage.setItem(key, 'true');
    } else {
      localStorage.removeItem(key);
    }
  };

  if (!category || !topic) {
    return (
      <div className="min-h-screen flex items-center justify-center"
        style={{ backgroundColor: getBackgroundColor(isDark, isAmoled) }}>
        <div className="text-center">
          <div className="w-8 h-8 border-4 border-t-transparent rounded-full animate-spin mx-auto"
            style={{ borderColor: '#FF8FA3', borderTopColor: 'transparent' }} />
          <p className="mt-4" style={{ color: isDark ? 'rgba(255,255,255,0.5)' : 'rgba(59,10,23,0.5)' }}>Loading topic...</p>
        </div>
      </div>
    );
  }

  const action = topic.actionPrompt;
  const isQuotes = category.id === 'QUOTES';
  const [r, g, b] = hexToRgb(category.accent);
  const surfaceRgb = isDark ? [26, 26, 46] : [247, 240, 245];
  const deepen = isDark ? 0.28 : 0.10;
  const s1r = Math.round(r * (1 - deepen));
  const s1g = Math.round(g * (1 - deepen));
  const s1b = Math.round(b * (1 - deepen));
  const s2r = Math.round(s1r * 0.70 + surfaceRgb[0] * 0.30);
  const s2g = Math.round(s1g * 0.70 + surfaceRgb[1] * 0.30);
  const s2b = Math.round(s1b * 0.70 + surfaceRgb[2] * 0.30);
  const heroBg = `linear-gradient(180deg, rgb(${s1r},${s1g},${s1b}) 0%, rgb(${s2r},${s2g},${s2b}) 100%)`;

  // Hero title: byline for QUOTES, topic name otherwise
  const heroTitle = isQuotes && topic.byline ? topic.byline : topic.name;

  return (
    <div className="min-h-screen pb-24 relative"
      style={{ backgroundColor: getBackgroundColor(isDark, isAmoled) }}>
      <CurioWatermarkBackdrop activeCatId={category.id} />

      {/* Sticky top bar */}
      <div className="sticky top-0 z-20 px-4 pt-4 pb-3"
        style={{
          background: getBackgroundColor(isDark, isAmoled),
          borderBottom: `1px solid ${isDark ? 'rgba(255,255,255,0.06)' : 'rgba(59,10,23,0.06)'}`,
        }}>
        <div className="flex items-center gap-3">
          <CurioBackButton onClick={() => navigate(-1)} />
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2">
              <MaterialIcon name={category.iconGlyph} size={18}
                style={{ color: isDark ? 'rgba(255,255,255,0.5)' : 'rgba(59,10,23,0.5)' }} />
              <span className="text-xs font-medium"
                style={{ color: isDark ? 'rgba(255,255,255,0.5)' : 'rgba(59,10,23,0.5)' }}>
                {category.displayName} · {topic.subtype}
              </span>
            </div>
          </div>
          <button onClick={handleQuickSave}
            className="w-9 h-9 rounded-full flex items-center justify-center transition-all"
            style={{ background: saved ? `${category.accent}22` : (isDark ? 'rgba(255,255,255,0.08)' : 'rgba(0,0,0,0.05)') }}>
            <MaterialIcon name={saved ? 'bookmark' : 'bookmark_add'} size={20}
              style={{ color: saved ? category.accent : (isDark ? 'rgba(255,255,255,0.6)' : 'rgba(59,10,23,0.6)') }} />
          </button>
        </div>
      </div>

      {/* Content */}
      <div className="relative z-10 px-5 pt-6 max-w-lg mx-auto">
        <MorphEntrance>
          <div className="relative w-full h-[260px] rounded-[30px] overflow-hidden mb-5"
            style={{
              background: heroBg,
              boxShadow: '0 8px 32px rgba(0,0,0,0.10)',
              border: isDark ? `1px solid ${category.accent}30` : `1px solid ${category.accent}1A`,
            }}>
            <div className="absolute top-0 left-3 right-3 h-[2px] rounded-full"
              style={{ background: category.accent, opacity: 0.45 }} />
            <div className="absolute top-0 left-0 right-0 h-[80px] pointer-events-none"
              style={{ background: isDark ? 'linear-gradient(180deg, rgba(255,255,255,0.04) 0%, transparent 100%)' : 'linear-gradient(180deg, rgba(255,255,255,0.16) 0%, transparent 100%)' }} />
            <div className="absolute right-2 bottom-2 pointer-events-none"
              style={{ color: isDark ? 'rgba(255,255,255,0.06)' : 'rgba(0,0,0,0.05)' }}>
              <MaterialIcon name={category.iconGlyph} size={150} />
            </div>
            <div className="relative z-10 flex flex-col h-full p-5">
              <div className="flex items-center gap-2 px-3 py-2 rounded-full self-start"
                style={{ background: 'rgba(255,255,255,0.15)', backdropFilter: 'blur(4px)' }}>
                <div className="w-2 h-2 rounded-full bg-white" />
                <span className="text-xs font-bold text-white tracking-wide">
                  {action.verb} for ~{action.durationMinutes} min
                </span>
              </div>
              <div className="flex-1 flex items-center justify-center">
                <h1 className="text-[28px] font-extrabold text-white text-center leading-tight px-2"
                  style={{ fontFamily: 'Geom, Inter, sans-serif', textShadow: '0 2px 8px rgba(0,0,0,0.12)' }}>
                  {heroTitle}
                </h1>
              </div>
              {/* Byline for QUOTES */}
              {isQuotes && topic.byline && (
                <div className="self-center px-3 py-1 rounded-full"
                  style={{ background: 'rgba(255,255,255,0.12)' }}>
                  <span className="text-[11px] font-semibold text-white/80">— {topic.byline}</span>
                </div>
              )}
              {!isQuotes && (
                <div className="self-end px-2.5 py-1 rounded-full"
                  style={{ background: 'rgba(255,255,255,0.12)' }}>
                  <span className="text-[11px] font-semibold text-white/80 uppercase">{topic.subtype}</span>
                </div>
              )}
            </div>
          </div>
        </MorphEntrance>

        <ContentEntrance>
          <div className="flex gap-3 mb-5">
            <button onClick={handleExpressYourself}
              className="flex-1 flex items-center justify-center gap-2 h-[52px] rounded-full font-bold text-sm transition-all hover:opacity-90 active:scale-[0.97]"
              style={{
                background: isDark ? 'rgba(255,255,255,0.08)' : 'rgba(59,10,23,0.04)',
                color: getTextColor(isDark),
                border: `1px solid ${isDark ? 'rgba(255,255,255,0.12)' : 'rgba(59,10,23,0.10)'}`,
              }}>
              <MaterialIcon name="edit" size={18} />
              Express yourself
            </button>
            <button onClick={handleStartExploring}
              className="flex-1 flex items-center justify-center gap-2 h-[52px] rounded-full font-bold text-sm text-white transition-all hover:opacity-90 active:scale-[0.97]"
              style={{
                background: `linear-gradient(135deg, ${category.accent}, ${category.lightAccent})`,
                boxShadow: `0 4px 16px ${category.accent}44`,
              }}>
              <MaterialIcon name="auto_awesome" size={18} />
              Start exploring
            </button>
          </div>

          {/* Quick fact / Full quote for QUOTES */}
          <CurioPaperCard variant="ruled" watermark={category.iconGlyph} accent={category.accent} className="mb-4">
            <p className="leading-relaxed" style={{ fontSize: '1.15rem', lineHeight: 1.75 }}>
              {isQuotes ? topic.name : topic.teaser}
            </p>
          </CurioPaperCard>

          <CurioMoodboardCard accent={category.accent} className="mb-4">
            <div className="flex items-center gap-2 mb-2">
              <MaterialIcon name="auto_awesome" size={20} style={{ color: category.accent }} />
              <h3 className="font-semibold" style={{ color: getTextColor(isDark) }}>
                {action.verb} {action.targetName}
              </h3>
            </div>
            <p className="text-sm leading-relaxed"
              style={{ color: isDark ? 'rgba(255,255,255,0.7)' : 'rgba(59,10,23,0.7)' }}>
              {action.instruction}
            </p>
          </CurioMoodboardCard>
        </ContentEntrance>
      </div>

      {/* Floating Category + Favorite bar */}
      <FloatingCategoryFavoriteBar
        category={category}
        isFavorited={isFavorited}
        onFavorite={handleToggleFavorite}
      />
    </div>
  );
};

export default TopicRevealScreen;
