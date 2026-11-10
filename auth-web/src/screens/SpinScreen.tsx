// Curio Web App - Shuffle
// A calm, card-based discovery draw. No wheel, dice, or carousel mechanics.

import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useTheme, getBackgroundColor, getTextColor } from '../theme/ThemeContext';
import { ALL_CATEGORIES, getCategoryBySlug } from '../data/categories';
import { loadTopicsForCategory } from '../data/topics';
import { getQuestSystem } from '../data/QuestSystem';
import type { CurioCategory, CurioTopic } from '../types';
import { MaterialIcon, CurioWatermarkBackdrop } from '../components/SharedComponents';

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
  const [drawCount, setDrawCount] = useState(0);

  const dark = isDark || isAmoled;
  const ink = getTextColor(isDark);
  const muted = dark ? 'rgba(245,243,244,.58)' : 'rgba(23,27,38,.58)';
  const subtle = dark ? 'rgba(245,243,244,.36)' : 'rgba(23,27,38,.38)';
  const surface = dark ? '#161A1D' : '#FFFFFF';
  const border = dark ? 'rgba(255,255,255,.09)' : 'rgba(23,27,38,.09)';

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
      setDrawCount(count => count + 1);
      setDrawing(false);
      questSystem.onSpin(activeCategory.id);
    }, 360);
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

  return (
    <div className="curio-shuffle-screen curio-draw-screen min-h-screen relative pb-32"
      style={{ backgroundColor: getBackgroundColor(isDark, isAmoled) }}>
      <CurioWatermarkBackdrop activeCatId={activeCategory.id} />

      <div className="relative z-10 max-w-2xl mx-auto px-5 pt-7 sm:pt-10">
        <header className="mb-7">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2 text-[10px] font-bold tracking-[.22em] uppercase" style={{ color: muted }}>
              <span className="w-1.5 h-1.5 rounded-full" style={{ background: ACCENT }} />
              Curio / Random discovery
            </div>
            <span className="text-[10px] uppercase tracking-widest" style={{ color: subtle }}>02 — Shuffle</span>
          </div>
          <h1 className="mt-5 text-[34px] sm:text-[40px] leading-none font-extrabold tracking-tight"
            style={{ color: ink, fontFamily: 'Geom, Inter, sans-serif' }}>
            Follow a thread<span style={{ color: ACCENT }}>.</span>
          </h1>
          <p className="mt-3 max-w-md text-sm leading-relaxed" style={{ color: muted }}>
            Let curiosity choose the subject. One small discovery can lead somewhere unexpected.
          </p>
        </header>

        <section className="rounded-2xl p-4 mb-5" style={{ background: surface, border: `1px solid ${border}` }}>
          <div className="flex items-center justify-between gap-3">
            <div className="min-w-0">
              <div className="text-[10px] font-bold uppercase tracking-[.18em]" style={{ color: ACCENT }}>Your discovery space</div>
              <div className="mt-1 text-sm font-bold truncate" style={{ color: ink }}>{activeCategory.displayName}</div>
              <div className="mt-0.5 text-[11px]" style={{ color: muted }}>
                {loading ? 'Preparing topics…' : `${eligibleTopics.length.toLocaleString()} topics in this draw`}
              </div>
            </div>
            <button onClick={() => setShowCategories(true)}
              className="shrink-0 flex items-center gap-2 px-3.5 py-2.5 rounded-xl text-xs font-bold"
              style={{ background: dark ? 'rgba(255,255,255,.07)' : 'rgba(23,27,38,.045)', color: ink, border: `1px solid ${border}` }}>
              <MaterialIcon name="layers" size={16} /> Change lane
            </button>
          </div>
          <div className="mt-4 pt-3 flex items-center justify-between gap-3" style={{ borderTop: `1px solid ${border}` }}>
            <div className="min-w-0">
              <div className="text-[10px] uppercase tracking-[.15em] font-bold" style={{ color: muted }}>Topic filter</div>
              <div className="text-xs mt-1 truncate" style={{ color: ink }}>{selectedSubtype || 'All types in this lane'}</div>
            </div>
            <button onClick={() => setShowFilters(true)} disabled={loading || subtypes.length === 0}
              className="flex items-center gap-1.5 px-3 py-2 rounded-xl text-xs font-semibold disabled:opacity-40"
              style={{ color: ACCENT, background: 'rgba(218,30,55,.08)' }}>
              <MaterialIcon name="sliders_horizontal" size={15} /> Filter
            </button>
          </div>
        </section>

        <section className="curio-draw-stage" aria-live="polite">
          <div className="flex items-center justify-between mb-3 px-1">
            <span className="text-[10px] uppercase tracking-[.18em] font-bold" style={{ color: muted }}>The next discovery</span>
            <span className="text-[10px] tabular-nums" style={{ color: subtle }}>{drawCount > 0 ? `Draw ${drawCount}` : 'Ready when you are'}</span>
          </div>

          <div key={topic?.id || 'empty'} className="curio-draw-card relative overflow-hidden rounded-[24px] p-6 sm:p-8"
            style={{ background: surface, border: `1px solid ${border}`, boxShadow: '0 20px 50px -38px rgba(0,0,0,.55)' }}>
            <div className="absolute -right-10 -top-12 w-44 h-44 rounded-full pointer-events-none"
              style={{ background: `radial-gradient(circle, ${activeCategory.accent}20, transparent 70%)` }} />
            <div className="absolute right-4 bottom-2 pointer-events-none" style={{ color: dark ? 'rgba(255,255,255,.045)' : 'rgba(23,27,38,.035)' }}>
              <MaterialIcon name={activeCategory.iconGlyph} size={150} />
            </div>
            <div className="relative z-10 min-h-[230px] flex flex-col">
              <div className="flex items-center justify-between gap-3">
                <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full text-[10px] font-bold uppercase tracking-[.12em]"
                  style={{ background: `${activeCategory.accent}18`, color: activeCategory.accent }}>
                  <MaterialIcon name={activeCategory.iconGlyph} size={13} /> {activeCategory.displayName}
                </span>
                {topic && <span className="text-[10px] uppercase tracking-widest" style={{ color: subtle }}>{topic.subtype}</span>}
              </div>

              {drawing ? (
                <div className="flex-1 flex flex-col justify-center">
                  <div className="curio-draw-skeleton w-3/4 h-7 rounded-lg" />
                  <div className="curio-draw-skeleton w-full h-3 rounded mt-4" />
                  <div className="curio-draw-skeleton w-5/6 h-3 rounded mt-2" />
                  <p className="mt-5 text-[10px] uppercase tracking-[.2em]" style={{ color: muted }}>Finding a thread…</p>
                </div>
              ) : topic ? (
                <div className="flex-1 flex flex-col justify-center py-8">
                  <div className="text-[10px] font-bold uppercase tracking-[.18em] mb-3" style={{ color: ACCENT }}>A little something to explore</div>
                  <h2 className="text-[28px] sm:text-[34px] font-extrabold leading-tight tracking-tight"
                    style={{ color: ink, fontFamily: 'Geom, Inter, sans-serif' }}>{topic.name}</h2>
                  <p className="mt-3 text-sm leading-relaxed max-w-md" style={{ color: muted }}>
                    {topic.teaser || topic.actionPrompt.instruction || 'Take a moment and see where this subject leads.'}
                  </p>
                </div>
              ) : (
                <div className="flex-1 flex flex-col justify-center py-8">
                  <div className="text-[10px] font-bold uppercase tracking-[.18em] mb-3" style={{ color: muted }}>A blank page, for now</div>
                  <h2 className="text-[27px] sm:text-[32px] font-extrabold leading-tight tracking-tight"
                    style={{ color: ink, fontFamily: 'Geom, Inter, sans-serif' }}>Something new is waiting.</h2>
                  <p className="mt-3 text-sm leading-relaxed max-w-md" style={{ color: muted }}>
                    Draw a topic from {activeCategory.displayName.toLowerCase()} and give your curiosity somewhere to go.
                  </p>
                </div>
              )}

              <div className="pt-4 flex items-center justify-between gap-3" style={{ borderTop: `1px solid ${border}` }}>
                <span className="text-[10px] uppercase tracking-[.13em]" style={{ color: subtle }}>
                  {topic ? 'Picked for you, at random' : 'No pressure. Just curiosity.'}
                </span>
                {topic && <span className="text-[10px] font-bold" style={{ color: activeCategory.accent }}>DISCOVERY FOUND</span>}
              </div>
            </div>
          </div>

          <div className="mt-5 flex flex-col sm:flex-row gap-3">
            <button onClick={drawDiscovery} disabled={loading || drawing || eligibleTopics.length === 0}
              className="curio-draw-primary flex-1 h-14 rounded-2xl flex items-center justify-center gap-2 text-sm font-bold text-white disabled:opacity-45"
              style={{ background: 'linear-gradient(135deg, #E12640, #A9132B)', boxShadow: '0 12px 26px -15px rgba(218,30,55,.7)' }}>
              <MaterialIcon name={drawing ? 'hourglass_top' : 'auto_awesome'} size={19} />
              {loading ? 'Loading topics…' : drawing ? 'Finding a discovery…' : topic ? 'Find another discovery' : 'Surprise me'}
            </button>
            {topic && (
              <button onClick={openTopic}
                className="h-14 px-5 rounded-2xl flex items-center justify-center gap-2 text-sm font-bold"
                style={{ color: ink, background: dark ? 'rgba(255,255,255,.07)' : '#FFFFFF', border: `1px solid ${border}` }}>
                Explore topic <MaterialIcon name="arrow_up_right" size={17} />
              </button>
            )}
          </div>
          <p className="mt-3 text-center text-[10px]" style={{ color: subtle }}>
            {selectedSubtype ? `Only ${selectedSubtype.toLowerCase()} topics will be drawn.` : 'Each draw avoids repeating the previous topic when possible.'}
          </p>
        </section>
      </div>

      {showCategories && (
        <div className="fixed inset-0 z-[70] flex items-end sm:items-center justify-center bg-black/55 p-0 sm:p-5" onClick={() => setShowCategories(false)}>
          <div className="w-full max-w-xl max-h-[82dvh] overflow-hidden rounded-t-[24px] sm:rounded-[24px] p-5"
            style={{ background: surface, border: `1px solid ${border}` }} onClick={event => event.stopPropagation()}>
            <div className="flex items-center justify-between mb-4">
              <div><div className="text-[10px] uppercase tracking-[.18em] font-bold" style={{ color: ACCENT }}>Discovery lanes</div><h3 className="text-lg font-bold mt-1" style={{ color: ink }}>Choose a subject area</h3></div>
              <button onClick={() => setShowCategories(false)} aria-label="Close categories" className="w-9 h-9 rounded-full flex items-center justify-center" style={{ background: dark ? 'rgba(255,255,255,.08)' : 'rgba(23,27,38,.06)', color: ink }}><MaterialIcon name="close" size={18} /></button>
            </div>
            <div className="overflow-y-auto max-h-[65dvh] grid grid-cols-2 gap-2 pb-2">
              {ALL_CATEGORIES.filter(category => category.isReady && !category.isHidden).map(category => (
                <button key={category.id} onClick={() => chooseCategory(category)}
                  className="flex items-center gap-2.5 p-3 rounded-xl text-left"
                  style={{ background: activeCategory.id === category.id ? `${category.accent}18` : (dark ? 'rgba(255,255,255,.045)' : 'rgba(23,27,38,.035)'), border: `1px solid ${activeCategory.id === category.id ? `${category.accent}55` : border}` }}>
                  <MaterialIcon name={category.iconGlyph} size={20} style={{ color: category.accent }} />
                  <span className="text-xs font-semibold" style={{ color: ink }}>{category.displayName}</span>
                </button>
              ))}
            </div>
          </div>
        </div>
      )}

      {showFilters && (
        <div className="fixed inset-0 z-[70] flex items-end sm:items-center justify-center bg-black/55 p-0 sm:p-5" onClick={() => setShowFilters(false)}>
          <div className="w-full max-w-lg max-h-[78dvh] overflow-hidden rounded-t-[24px] sm:rounded-[24px] p-5"
            style={{ background: surface, border: `1px solid ${border}` }} onClick={event => event.stopPropagation()}>
            <div className="flex items-center justify-between mb-4">
              <div><div className="text-[10px] uppercase tracking-[.18em] font-bold" style={{ color: ACCENT }}>Refine the draw</div><h3 className="text-lg font-bold mt-1" style={{ color: ink }}>Choose a topic type</h3></div>
              <button onClick={() => setShowFilters(false)} aria-label="Close filters" className="w-9 h-9 rounded-full flex items-center justify-center" style={{ background: dark ? 'rgba(255,255,255,.08)' : 'rgba(23,27,38,.06)', color: ink }}><MaterialIcon name="close" size={18} /></button>
            </div>
            <div className="overflow-y-auto max-h-[58dvh] flex flex-wrap gap-2 pb-2">
              <button onClick={() => chooseSubtype(null)} className="px-3.5 py-2.5 rounded-xl text-xs font-semibold"
                style={{ background: !selectedSubtype ? ACCENT : (dark ? 'rgba(255,255,255,.06)' : 'rgba(23,27,38,.045)'), color: !selectedSubtype ? '#FFFFFF' : ink }}>All types</button>
              {subtypes.map(subtype => (
                <button key={subtype} onClick={() => chooseSubtype(subtype)} className="px-3.5 py-2.5 rounded-xl text-xs font-semibold"
                  style={{ background: selectedSubtype === subtype ? ACCENT : (dark ? 'rgba(255,255,255,.06)' : 'rgba(23,27,38,.045)'), color: selectedSubtype === subtype ? '#FFFFFF' : ink }}>{subtype}</button>
              ))}
            </div>
          </div>
        </div>
      )}

      <style>{`
        .curio-draw-card { animation: curio-draw-enter 360ms cubic-bezier(.2,.75,.25,1) both; }
        @keyframes curio-draw-enter { from { opacity: .55; transform: translateY(9px) scale(.99); } to { opacity: 1; transform: translateY(0) scale(1); } }
        .curio-draw-skeleton { background: linear-gradient(90deg, rgba(120,120,120,.10), rgba(120,120,120,.18), rgba(120,120,120,.10)); background-size: 200% 100%; animation: curio-draw-shimmer 1.1s linear infinite; }
        @keyframes curio-draw-shimmer { to { background-position: -200% 0; } }
        @media (prefers-reduced-motion: reduce) { .curio-draw-card, .curio-draw-skeleton { animation: none; } }
      `}</style>
    </div>
  );
};

export default SpinScreen;
