// Curio Web App - Shuffle
// A focused, card-first topic draw.

import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useTheme, getTextColor } from '../theme/ThemeContext';
import { MaterialIcon } from '../components/SharedComponents';
import { ALL_CATEGORIES, getCategoryBySlug } from '../data/categories';
import { loadTopicsForCategory } from '../data/topics';
import { getQuestSystem } from '../data/QuestSystem';
import type { CurioCategory, CurioTopic } from '../types';

const ACCENT = '#DA1E37';

export const SpinScreen: React.FC = () => {
  const navigate = useNavigate();
  const { categorySlug } = useParams();
  const { isDark, isAmoled } = useTheme();
  const [questSystem] = useState(() => getQuestSystem());
  const [activeCategory, setActiveCategory] = useState<CurioCategory>(() =>
    (categorySlug && getCategoryBySlug(categorySlug)) || ALL_CATEGORIES[0]
  );
  const [topics, setTopics] = useState<CurioTopic[]>([]);
  const [topic, setTopic] = useState<CurioTopic | null>(null);
  const [previousId, setPreviousId] = useState<string | null>(null);
  const [selectedSubtype, setSelectedSubtype] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [drawing, setDrawing] = useState(false);
  const [showCategories, setShowCategories] = useState(false);
  const [showFilters, setShowFilters] = useState(false);

  const dark = isDark || isAmoled;
  const ink = getTextColor(isDark);
  const muted = dark ? 'rgba(245,243,244,.62)' : 'rgba(23,27,38,.58)';
  const surface = dark ? '#161A1D' : '#FFFFFF';
  const border = dark ? 'rgba(255,255,255,.10)' : 'rgba(23,27,38,.09)';
  const backdrop = dark ? '#0B090A' : '#F6F7FB';

  useEffect(() => {
    if (!categorySlug) return;
    const category = getCategoryBySlug(categorySlug);
    if (category) setActiveCategory(category);
  }, [categorySlug]);

  useEffect(() => {
    let current = true;
    setLoading(true);
    setTopics([]);
    setTopic(null);
    setPreviousId(null);
    setSelectedSubtype(null);
    loadTopicsForCategory(activeCategory.id).then(result => {
      if (!current) return;
      setTopics(result);
      setLoading(false);
    }).catch(() => {
      if (current) setLoading(false);
    });
    return () => { current = false; };
  }, [activeCategory.id]);

  const subtypes = useMemo(
    () => Array.from(new Set(topics.map(item => item.subtype).filter(Boolean))).sort((a, b) => a.localeCompare(b)),
    [topics],
  );
  const eligibleTopics = useMemo(
    () => selectedSubtype ? topics.filter(item => item.subtype === selectedSubtype) : topics,
    [topics, selectedSubtype],
  );

  const drawDiscovery = useCallback(() => {
    if (drawing || loading || eligibleTopics.length === 0) return;
    setDrawing(true);
    const pool = eligibleTopics.length > 1
      ? eligibleTopics.filter(item => item.id !== (topic?.id || previousId))
      : eligibleTopics;
    const choice = pool[Math.floor(Math.random() * pool.length)] || eligibleTopics[0];
    window.setTimeout(() => {
      setPreviousId(topic?.id || previousId);
      setTopic(choice);
      setDrawing(false);
      questSystem.onSpin(activeCategory.id);
    }, 260);
  }, [drawing, loading, eligibleTopics, topic, previousId, questSystem, activeCategory.id]);

  const chooseCategory = (category: CurioCategory) => {
    setActiveCategory(category);
    setShowCategories(false);
  };

  const chooseSubtype = (subtype: string | null) => {
    setSelectedSubtype(subtype);
    setTopic(null);
    setPreviousId(null);
    setShowFilters(false);
  };

  const openTopic = () => {
    if (topic) navigate(`/reveal/${topic.categoryId.toLowerCase()}/${topic.id}`);
  };

  const closeSheets = () => {
    setShowCategories(false);
    setShowFilters(false);
  };

  return (
    <div className="curio-shuffle-screen curio-draw-screen min-h-screen relative pb-32"
      style={{ background: backdrop, color: ink }}>
      <main className="relative z-10 max-w-xl mx-auto px-5 pt-6 sm:pt-9">
        <header className="mb-6">
          <h1 className="text-[30px] sm:text-[34px] leading-tight font-extrabold tracking-tight"
            style={{ color: ink, fontFamily: 'Geom, Inter, sans-serif' }}>
            Shuffle<span style={{ color: ACCENT }}>.</span>
          </h1>
        </header>

        <section className="flex gap-2 mb-5">
          <button onClick={() => setShowCategories(true)}
            className="min-w-0 flex-1 rounded-xl px-4 py-3 text-left"
            style={{ background: surface, border: `1px solid ${border}` }}>
            <span className="block text-[10px] uppercase tracking-[.13em] font-bold mb-1" style={{ color: muted }}>Lane</span>
            <span className="block text-sm font-semibold truncate" style={{ color: ink }}>{activeCategory.displayName}</span>
          </button>
          <button onClick={() => setShowFilters(true)} disabled={loading || subtypes.length === 0}
            className="min-w-0 flex-1 rounded-xl px-4 py-3 text-left disabled:opacity-50"
            style={{ background: surface, border: `1px solid ${border}` }}>
            <span className="block text-[10px] uppercase tracking-[.13em] font-bold mb-1" style={{ color: muted }}>Filter</span>
            <span className="block text-sm font-semibold truncate" style={{ color: ink }}>{selectedSubtype || 'All topics'}</span>
          </button>
        </section>

        <section className="curio-draw-stage" aria-live="polite">
          <div key={topic?.id || (drawing ? 'drawing' : 'ready')}
            className="curio-draw-card relative overflow-hidden rounded-[22px] p-6 sm:p-8"
            style={{
              minHeight: 'clamp(290px, 43vh, 390px)',
              display: 'flex',
              flexDirection: 'column',
              justifyContent: 'space-between',
              background: surface,
              border: `1px solid ${border}`,
              boxShadow: dark ? '0 18px 44px -34px rgba(0,0,0,.9)' : '0 18px 44px -34px rgba(35,24,28,.28)',
            }}>
            <div className="absolute left-0 top-0 bottom-0 w-1" style={{ background: activeCategory.accent, opacity: .82 }} />
            <div className="absolute right-6 top-6 pointer-events-none" aria-hidden="true"
              style={{ color: activeCategory.accent, opacity: .16 }}>
              <MaterialIcon name={activeCategory.iconGlyph} size={64} />
            </div>
            <div className="relative z-10 flex items-center justify-between gap-3">
              <span className="text-[11px] font-bold tracking-[.12em] uppercase"
                style={{ color: activeCategory.accent }}>{activeCategory.displayName}</span>
              {topic?.subtype && <span className="text-xs truncate" style={{ color: muted }}>{topic.subtype}</span>}
            </div>

            <div className="relative z-10 py-10">
              {loading ? (
                <p className="text-base font-medium" style={{ color: muted }}>Loading topics…</p>
              ) : drawing ? (
                <div className="space-y-3" aria-label="Choosing a topic">
                  <div className="curio-draw-skeleton h-8 w-3/4 rounded-lg" />
                  <div className="curio-draw-skeleton h-3 w-full rounded" />
                  <div className="curio-draw-skeleton h-3 w-2/3 rounded" />
                </div>
              ) : topic ? (
                <>
                  <h2 className="text-[30px] sm:text-[36px] font-extrabold leading-tight tracking-tight"
                    style={{ color: ink, fontFamily: 'Geom, Inter, sans-serif' }}>{topic.name}</h2>
                  {topic.teaser && <p className="mt-4 text-sm leading-relaxed max-w-md" style={{ color: muted }}>{topic.teaser}</p>}
                </>
              ) : (
                <>
                  <p className="text-[11px] font-bold uppercase tracking-[.14em] mb-3" style={{ color: muted }}>Your next topic</p>
                  <h2 className="text-[28px] sm:text-[34px] font-extrabold leading-tight tracking-tight"
                    style={{ color: ink, fontFamily: 'Geom, Inter, sans-serif' }}>Ready to discover?</h2>
                </>
              )}
            </div>

            <div className="relative z-10 pt-4 flex items-center justify-between gap-3"
              style={{ borderTop: `1px solid ${border}` }}>
              <span className="text-xs" style={{ color: muted }}>
                {loading ? ' ' : `${eligibleTopics.length.toLocaleString()} topics available`}
              </span>
              <span className="text-[10px] uppercase tracking-[.12em] font-bold" style={{ color: muted }}>
                {topic ? 'Selected' : 'Random draw'}
              </span>
            </div>
          </div>

          <div className="mt-4 grid grid-cols-1 gap-2">
            <button onClick={drawDiscovery} disabled={loading || drawing || eligibleTopics.length === 0}
              className="curio-draw-primary w-full h-14 rounded-xl flex items-center justify-center text-sm font-bold text-white disabled:opacity-45"
              style={{ background: ACCENT, boxShadow: '0 10px 24px -16px rgba(218,30,55,.75)' }}>
              {loading ? 'Loading…' : drawing ? 'Choosing…' : topic ? 'Shuffle again' : 'Shuffle'}
            </button>
            {topic && (
              <button onClick={openTopic}
                className="w-full h-12 rounded-xl text-sm font-semibold"
                style={{ color: ink, background: surface, border: `1px solid ${border}` }}>
                Explore topic
              </button>
            )}
          </div>
        </section>
      </main>

      {showCategories && (
        <div className="fixed inset-0 z-[110] flex items-end sm:items-center justify-center bg-black/55 p-0 sm:p-5" onClick={closeSheets}>
          <div className="w-full max-w-xl max-h-[82dvh] overflow-hidden rounded-t-[22px] sm:rounded-[22px] p-5"
            style={{ background: surface, border: `1px solid ${border}` }} onClick={event => event.stopPropagation()}>
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-bold" style={{ color: ink }}>Choose a lane</h3>
              <button onClick={closeSheets} aria-label="Close" className="px-3 py-2 text-sm" style={{ color: muted }}>Close</button>
            </div>
            <div className="overflow-y-auto max-h-[65dvh] grid grid-cols-2 gap-2 pb-2">
              {ALL_CATEGORIES.filter(category => category.isReady && !category.isHidden).map(category => (
                <button key={category.id} onClick={() => chooseCategory(category)}
                  className="p-3 rounded-xl text-left text-sm font-semibold"
                  style={{
                    color: ink,
                    background: activeCategory.id === category.id ? `${category.accent}18` : (dark ? 'rgba(255,255,255,.045)' : 'rgba(23,27,38,.035)'),
                    border: `1px solid ${activeCategory.id === category.id ? `${category.accent}66` : border}`,
                  }}>
                  {category.displayName}
                </button>
              ))}
            </div>
          </div>
        </div>
      )}

      {showFilters && (
        <div className="fixed inset-0 z-[110] flex items-end sm:items-center justify-center bg-black/55 p-0 sm:p-5" onClick={closeSheets}>
          <div className="w-full max-w-lg max-h-[78dvh] overflow-hidden rounded-t-[22px] sm:rounded-[22px] p-5"
            style={{ background: surface, border: `1px solid ${border}` }} onClick={event => event.stopPropagation()}>
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-bold" style={{ color: ink }}>Filter topics</h3>
              <button onClick={closeSheets} aria-label="Close" className="px-3 py-2 text-sm" style={{ color: muted }}>Close</button>
            </div>
            <div className="overflow-y-auto max-h-[58dvh] flex flex-col gap-1 pb-2">
              <button onClick={() => chooseSubtype(null)} className="px-3.5 py-3 rounded-lg text-left text-sm font-semibold"
                style={{ background: !selectedSubtype ? `${ACCENT}14` : 'transparent', color: !selectedSubtype ? ACCENT : ink }}>All topics</button>
              {subtypes.map(subtype => (
                <button key={subtype} onClick={() => chooseSubtype(subtype)} className="px-3.5 py-3 rounded-lg text-left text-sm font-semibold"
                  style={{ background: selectedSubtype === subtype ? `${ACCENT}14` : 'transparent', color: selectedSubtype === subtype ? ACCENT : ink }}>{subtype}</button>
              ))}
            </div>
          </div>
        </div>
      )}

      <style>{`
        .curio-draw-card { animation: curio-draw-enter 260ms cubic-bezier(.2,.75,.25,1) both; }
        @keyframes curio-draw-enter { from { opacity: .65; transform: translateY(6px); } to { opacity: 1; transform: translateY(0); } }
        .curio-draw-skeleton { background: linear-gradient(90deg, rgba(120,120,120,.08), rgba(120,120,120,.16), rgba(120,120,120,.08)); background-size: 200% 100%; animation: curio-draw-shimmer 1.1s linear infinite; }
        @keyframes curio-draw-shimmer { to { background-position: -200% 0; } }
        @media (prefers-reduced-motion: reduce) { .curio-draw-card, .curio-draw-skeleton { animation: none; } }
      `}</style>
    </div>
  );
};

export default SpinScreen;
