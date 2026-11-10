// Curio Web App - Topic Detail
// A quiet editorial page for giving each discovery room to breathe.

import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useTheme, getBackgroundColor, getTextColor } from '../theme/ThemeContext';
import { getCategoryBySlug } from '../data/categories';
import { loadTopicsForCategory } from '../data/topics';
import { MaterialIcon, CurioWatermarkBackdrop } from '../components/SharedComponents';
import type { CurioTopic, CurioCategory } from '../types';

const ACCENT = '#DA1E37';

export const TopicRevealScreen: React.FC = () => {
  const navigate = useNavigate();
  const { categorySlug, topicName } = useParams();
  const { isDark, isAmoled } = useTheme();
  const [category, setCategory] = useState<CurioCategory | null>(null);
  const [topic, setTopic] = useState<CurioTopic | null>(null);
  const [loading, setLoading] = useState(true);
  const [isFavorited, setIsFavorited] = useState(false);

  const dark = isDark || isAmoled;
  const ink = getTextColor(isDark);
  const muted = dark ? 'rgba(245,243,244,.62)' : 'rgba(23,27,38,.62)';
  const surface = dark ? '#161A1D' : '#FFFFFF';
  const border = dark ? 'rgba(255,255,255,.09)' : 'rgba(23,27,38,.09)';

  useEffect(() => {
    let active = true;
    const load = async () => {
      setLoading(true);
      const foundCategory = categorySlug ? getCategoryBySlug(categorySlug) : null;
      if (!foundCategory) { if (active) setLoading(false); return; }
      const allTopics = await loadTopicsForCategory(foundCategory.id);
      const foundTopic = allTopics.find(item => item.id === topicName) || null;
      if (!active) return;
      setCategory(foundCategory);
      setTopic(foundTopic);
      setLoading(false);
      if (foundTopic) {
        setIsFavorited(localStorage.getItem(`curio-fav-${foundTopic.categoryId}-${foundTopic.id}`) === 'true');
      }
    };
    load();
    return () => { active = false; };
  }, [categorySlug, topicName]);

  const toggleFavorite = () => {
    if (!topic) return;
    const key = `curio-fav-${topic.categoryId}-${topic.id}`;
    const next = !isFavorited;
    setIsFavorited(next);
    if (next) localStorage.setItem(key, 'true');
    else localStorage.removeItem(key);
  };

  const startExploring = () => {
    if (!topic) return;
    const query = encodeURIComponent(`${topic.name} ${topic.subtype}`);
    const isMusic = ['Album', 'Artist', 'Song'].includes(topic.subtype);
    if (isMusic) {
      const service = localStorage.getItem('curio-music-service') || 'youtube_music';
      const links: Record<string, string> = {
        youtube_music: `https://music.youtube.com/search?q=${query}`,
        apple_music: `https://music.apple.com/search?term=${query}`,
        spotify: `https://open.spotify.com/search/${query}`,
      };
      window.open(links[service] || links.youtube_music, '_blank', 'noopener,noreferrer');
      return;
    }
    const engine = localStorage.getItem('curio-search-engine') || 'google';
    const links: Record<string, string> = {
      google: `https://www.google.com/search?q=${query}`,
      duckduckgo: `https://duckduckgo.com/?q=${query}`,
      bing: `https://www.bing.com/search?q=${query}`,
      brave: `https://search.brave.com/search?q=${query}`,
      ecosia: `https://www.ecosia.org/search?q=${query}`,
      startpage: `https://www.startpage.com/sp/search?query=${query}`,
      yahoo: `https://search.yahoo.com/search?p=${query}`,
    };
    window.open(links[engine] || links.google, '_blank', 'noopener,noreferrer');
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center" style={{ background: getBackgroundColor(isDark, isAmoled) }}>
        <div className="w-7 h-7 rounded-full border-[3px] border-t-transparent animate-spin" style={{ borderColor: ACCENT, borderTopColor: 'transparent' }} />
      </div>
    );
  }

  if (!category || !topic) {
    return (
      <div className="min-h-screen px-6 flex flex-col items-center justify-center text-center" style={{ background: getBackgroundColor(isDark, isAmoled), color: ink }}>
        <MaterialIcon name="search_off" size={38} />
        <h1 className="mt-4 text-xl font-bold">This discovery wandered off.</h1>
        <p className="mt-2 text-sm opacity-60">We couldn't find this topic in the collection.</p>
        <button onClick={() => navigate(-1)} className="mt-6 px-5 py-3 rounded-xl text-sm font-semibold text-white" style={{ background: ACCENT }}>Go back</button>
      </div>
    );
  }

  const isQuote = category.id === 'QUOTES';
  const title = isQuote && topic.byline ? topic.byline : topic.name;
  const action = topic.actionPrompt;

  return (
    <div className="curio-topic-detail min-h-screen relative pb-10" style={{ background: getBackgroundColor(isDark, isAmoled), color: ink }}>
      <CurioWatermarkBackdrop activeCatId={category.id} alphaScale={0.45} />
      <div className="relative z-10 max-w-2xl mx-auto px-5">
        <header className="flex items-center justify-between pt-4 pb-7" style={{ paddingTop: 'max(16px, env(safe-area-inset-top))' }}>
          <button onClick={() => navigate(-1)} aria-label="Go back" className="w-10 h-10 rounded-full flex items-center justify-center"
            style={{ background: dark ? 'rgba(255,255,255,.07)' : 'rgba(23,27,38,.055)', border: `1px solid ${border}` }}>
            <MaterialIcon name="arrow_back" size={20} />
          </button>
          <div className="text-[10px] uppercase tracking-[.2em] font-bold" style={{ color: muted }}>A Curio discovery</div>
          <button onClick={toggleFavorite} aria-label={isFavorited ? 'Remove from favorites' : 'Add to favorites'}
            className="w-10 h-10 rounded-full flex items-center justify-center"
            style={{ background: isFavorited ? 'rgba(218,30,55,.13)' : (dark ? 'rgba(255,255,255,.07)' : 'rgba(23,27,38,.055)'), border: `1px solid ${border}`, color: isFavorited ? ACCENT : muted }}>
            <MaterialIcon name={isFavorited ? 'favorite' : 'favorite_border'} size={19} />
          </button>
        </header>

        <main>
          <div className="flex items-center gap-2 mb-4">
            <span className="w-2 h-2 rounded-full" style={{ background: category.accent }} />
            <span className="text-xs font-semibold" style={{ color: muted }}>{category.displayName}</span>
            <span className="text-xs" style={{ color: muted }}>·</span>
            <span className="text-xs" style={{ color: muted }}>{topic.subtype}</span>
          </div>

          <section className="relative overflow-hidden rounded-[26px] p-6 sm:p-9 min-h-[290px] flex flex-col justify-between"
            style={{ background: `linear-gradient(145deg, ${dark ? '#20262D' : '#202A36'}, ${dark ? '#11161C' : '#111923'})`, boxShadow: '0 24px 60px -38px rgba(0,0,0,.7)' }}>
            <div className="absolute -right-8 -bottom-10 opacity-[.07] pointer-events-none text-white">
              <MaterialIcon name={category.iconGlyph} size={210} />
            </div>
            <div className="relative z-10 flex items-center justify-between">
              <span className="px-3 py-1.5 rounded-full text-[10px] font-bold uppercase tracking-[.14em] text-white"
                style={{ background: 'rgba(255,255,255,.12)', border: '1px solid rgba(255,255,255,.12)' }}>{category.displayName}</span>
              <span className="text-[10px] uppercase tracking-[.15em] text-white/50">{topic.subtype}</span>
            </div>
            <div className="relative z-10 py-12">
              <div className="w-9 h-[3px] rounded-full mb-5" style={{ background: ACCENT }} />
              <h1 className="text-[34px] sm:text-[44px] leading-[1.08] font-extrabold tracking-tight text-white"
                style={{ fontFamily: 'Geom, Inter, sans-serif' }}>{title}</h1>
              {isQuote && topic.byline && <p className="mt-3 text-sm text-white/60">A thought to keep</p>}
            </div>
            <div className="relative z-10 flex items-center justify-between">
              <span className="text-[10px] uppercase tracking-[.18em] text-white/45">Notice something new</span>
              <MaterialIcon name="north_east" size={17} className="text-white/55" />
            </div>
          </section>

          <section className="mt-7">
            <div className="text-[10px] font-bold uppercase tracking-[.2em] mb-3" style={{ color: ACCENT }}>A little context</div>
            <p className="text-[16px] sm:text-[17px] leading-[1.85]" style={{ color: muted }}>
              {isQuote ? topic.name : topic.teaser}
            </p>
          </section>

          <section className="mt-8 rounded-2xl p-5 sm:p-6" style={{ background: surface, border: `1px solid ${border}` }}>
            <div className="flex items-center gap-2 mb-3">
              <div className="w-8 h-8 rounded-lg flex items-center justify-center" style={{ background: 'rgba(218,30,55,.10)', color: ACCENT }}>
                <MaterialIcon name="lightbulb" size={18} />
              </div>
              <div>
                <div className="text-[10px] uppercase tracking-[.16em] font-bold" style={{ color: muted }}>A way in</div>
                <h2 className="text-sm font-bold" style={{ color: ink }}>{action.verb} {action.targetName}</h2>
              </div>
            </div>
            <p className="text-sm leading-relaxed" style={{ color: muted }}>{action.instruction}</p>
            <div className="mt-4 text-[10px] uppercase tracking-[.15em]" style={{ color: muted }}>A few minutes is enough · about {action.durationMinutes} min</div>
          </section>

          <section className="mt-8 grid grid-cols-1 sm:grid-cols-2 gap-3">
            <button onClick={() => navigate(`/capture/${category.id.toLowerCase()}/${topic.id}`)}
              className="h-14 rounded-2xl flex items-center justify-center gap-2 font-bold text-sm text-white"
              style={{ background: 'linear-gradient(135deg, #E12640, #A9132B)', boxShadow: '0 12px 26px -16px rgba(218,30,55,.75)' }}>
              <MaterialIcon name="edit_note" size={19} /> Express yourself
            </button>
            <button onClick={startExploring} className="h-14 rounded-2xl flex items-center justify-center gap-2 font-bold text-sm"
              style={{ background: dark ? 'rgba(255,255,255,.07)' : '#FFFFFF', color: ink, border: `1px solid ${border}` }}>
              <MaterialIcon name="open_in_new" size={18} /> Start exploring
            </button>
          </section>
        </main>
      </div>
    </div>
  );
};

export default TopicRevealScreen;
