// Curio Web App - Shuffle
// A focused, card-first topic draw.

import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
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
  const [rollingTopic, setRollingTopic] = useState<CurioTopic | null>(null);
  const intervalRef = useRef<number | null>(null);
  const timeoutRef = useRef<number | null>(null);
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
    if (intervalRef.current !== null) window.clearTimeout(intervalRef.current);
    if (timeoutRef.current !== null) window.clearTimeout(timeoutRef.current);
    setDrawing(true);
    setRollingTopic(null);
    const pool = eligibleTopics.length > 1 ? eligibleTopics.filter(item => item.id !== (topic?.id || previousId)) : eligibleTopics;
    const candidates = pool.length ? pool : eligibleTopics;
    const choice = candidates[Math.floor(Math.random() * candidates.length)] || eligibleTopics[0];
    const startedAt = Date.now();
    const rollFor = 4100;
    const tick = () => {
      const progress = Math.min(1, (Date.now() - startedAt) / rollFor);
      if (progress >= 1) {
        intervalRef.current = null;
        setRollingTopic(null);
        timeoutRef.current = window.setTimeout(() => {
          setPreviousId(topic?.id || previousId);
          setTopic(choice);
          setDrawing(false);
          timeoutRef.current = null;
          questSystem.onSpin(activeCategory.id);
        }, 760);
        return;
      }
      setRollingTopic(eligibleTopics[Math.floor(Math.random() * eligibleTopics.length)]);
      const easedProgress = progress * progress * progress;
      intervalRef.current = window.setTimeout(tick, 58 + 560 * easedProgress);
    };
    intervalRef.current = window.setTimeout(tick, 58);
  }, [drawing, loading, eligibleTopics, topic, previousId, questSystem, activeCategory.id]);

  useEffect(() => () => {
    if (intervalRef.current !== null) window.clearTimeout(intervalRef.current);
    if (timeoutRef.current !== null) window.clearTimeout(timeoutRef.current);
  }, []);

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
      <main className="curio-shuffle-layout relative z-10 mx-auto px-5 pt-7 sm:pt-10">
        <header className="curio-shuffle-header">
          <div>
            <p className="curio-shuffle-kicker">A LITTLE ROOM FOR THE UNEXPECTED</p>
            <h1 className="curio-shuffle-title">Shuffle<span>.</span></h1>
            <p className="curio-shuffle-intro">Let curiosity choose what you explore next.</p>
          </div>
          <div className="curio-shuffle-index" aria-hidden="true"><span>CURIO</span><strong>01</strong><i /></div>
        </header>
        <section className="curio-shuffle-workspace" aria-live="polite">
          <div className="curio-shuffle-toolbar">
            <button onClick={() => setShowCategories(true)} className="curio-shuffle-select">
              <span className="curio-shuffle-select-icon"><MaterialIcon name={activeCategory.iconGlyph} size={19} /></span>
              <span className="curio-shuffle-select-copy"><small>YOUR LANE</small><strong>{activeCategory.displayName}</strong></span>
              <MaterialIcon name="expand_more" size={19} />
            </button>
            <button onClick={() => setShowFilters(true)} disabled={loading || subtypes.length === 0} className="curio-shuffle-filter">
              <MaterialIcon name="filter_list" size={18} /><span>{selectedSubtype || 'All topics'}</span><MaterialIcon name="expand_more" size={17} />
            </button>
          </div>
          <div className={"curio-shuffle-canvas " + (drawing ? "is-drawing" : topic ? "has-result" : "is-idle")}>
            <div className="curio-shuffle-canvas-top">
              <span><i />{drawing ? 'SEARCHING THE DECK' : topic ? 'YOUR DISCOVERY' : 'THE DAILY DRAW'}</span>
              <span>{eligibleTopics.length.toLocaleString()} TOPICS</span>
            </div>
            <div className="curio-shuffle-art" aria-hidden="true">
              <div className="curio-shuffle-orbit orbit-one" /><div className="curio-shuffle-orbit orbit-two" />
              <div className="curio-shuffle-glyph"><MaterialIcon name={activeCategory.iconGlyph} size={112} /></div>
              <div className="curio-shuffle-cross cross-one" /><div className="curio-shuffle-cross cross-two" />
            </div>
            <div className="curio-shuffle-copy">
              {loading ? (
                <><p className="curio-shuffle-overline">PREPARING YOUR DECK</p><h2>Gathering curiosities<span>…</span></h2><p className="curio-shuffle-description">A moment. Good discoveries are worth the wait.</p></>
              ) : drawing ? (
                <><p className="curio-shuffle-overline">SOMETHING IS TAKING SHAPE</p><h2 key={rollingTopic?.id || 'rolling'} className="curio-shuffle-rolling-title">{rollingTopic?.name || 'Turning the pages…'}</h2><p className="curio-shuffle-description">{rollingTopic?.subtype || 'The next discovery is almost here.'}</p></>
              ) : topic ? (
                <><p className="curio-shuffle-overline">PICKED JUST FOR THIS MOMENT</p><h2>{topic.name}</h2>{topic.teaser && <p className="curio-shuffle-description">{topic.teaser}</p>}</>
              ) : (
                <><p className="curio-shuffle-overline">A WORLD OF IDEAS, ONE DRAW AWAY</p><h2>Where will curiosity take you?</h2><p className="curio-shuffle-description">No endless scrolling. Just one unexpected thing to discover.</p></>
              )}
            </div>
            <div className="curio-shuffle-canvas-bottom">
              <span>{topic?.subtype || activeCategory.displayName}</span><span>{drawing ? 'LET IT UNFOLD' : topic ? 'READY WHEN YOU ARE' : 'FOLLOW THE THREAD'}</span>
            </div>
            {drawing && <div className="curio-shuffle-progress" aria-hidden="true"><span /></div>}
          </div>
          <div className="curio-shuffle-actions">
            <button onClick={drawDiscovery} disabled={loading || drawing || eligibleTopics.length === 0} className="curio-shuffle-draw-button">
              <span>{drawing ? 'Finding your curiosity' : topic ? 'Draw again' : 'Surprise me'}</span>
              <span className="curio-shuffle-button-icon"><MaterialIcon name={drawing ? 'hourglass_top' : 'arrow_forward'} size={20} /></span>
            </button>
            {topic && !drawing && <button onClick={openTopic} className="curio-shuffle-open-button">Explore this topic <MaterialIcon name="north_east" size={17} /></button>}
          </div>
          <p className="curio-shuffle-footnote"><MaterialIcon name="auto_awesome" size={14} /> {(selectedSubtype ? 'A focused draw from ' + selectedSubtype : 'A fresh draw from ' + activeCategory.displayName) + '. No repeats until you change your lane.'}</p>
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
        .curio-draw-card { animation: curio-draw-enter 320ms cubic-bezier(.2,.75,.25,1) both; transform-origin: 50% 58%; }
        .curio-draw-card.is-drawing { animation: curio-deck-shake 180ms ease-in-out infinite alternate; }
        .curio-draw-card.has-result { animation: curio-card-land 520ms cubic-bezier(.16,1,.3,1) both; }
        .curio-rolling-title { animation: curio-title-flicker 100ms linear both; }
        .curio-shuffle-track { height: 3px; display: flex; gap: 5px; align-items: center; overflow: hidden; }
        .curio-shuffle-track span { height: 3px; flex: 1; border-radius: 9px; background: #DA1E37; opacity: .25; animation: curio-track 650ms ease-in-out infinite alternate; }
        .curio-shuffle-track span:nth-child(2n) { animation-delay: 90ms; }
        .curio-shuffle-track span:nth-child(3n) { animation-delay: 180ms; }
        @keyframes curio-draw-enter { from { opacity: .55; transform: translateY(10px) scale(.985); } to { opacity: 1; transform: translateY(0) scale(1); } }
        @keyframes curio-deck-shake { from { transform: translateX(-2px) rotate(-.25deg) scale(.995); } to { transform: translateX(2px) rotate(.25deg) scale(1.005); } }
        @keyframes curio-card-land { 0% { opacity: .72; transform: translateY(12px) scale(.97); } 65% { opacity: 1; transform: translateY(-3px) scale(1.008); } 100% { transform: translateY(0) scale(1); } }
        @keyframes curio-title-flicker { from { opacity: .45; filter: blur(3px); transform: translateY(4px); } to { opacity: 1; filter: blur(0); transform: translateY(0); } }
        @keyframes curio-track { from { opacity: .2; transform: scaleX(.55); } to { opacity: .9; transform: scaleX(1); } }
        @media (prefers-reduced-motion: reduce) { .curio-draw-card, .curio-draw-card.is-drawing, .curio-draw-card.has-result, .curio-rolling-title, .curio-shuffle-track span { animation: none; } }
      `}</style>
    </div>
  );
};

export default SpinScreen;
