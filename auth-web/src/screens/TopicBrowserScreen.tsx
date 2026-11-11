// Curio Web App - Explore
// A paginated, searchable catalogue designed for quick browsing.

import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTheme, getBackgroundColor, getTextColor } from '../theme/ThemeContext';
import { ALL_CATEGORIES } from '../data/categories';
import { loadTopicsForCategory } from '../data/topics';
import { MaterialIcon } from '../components/SharedComponents';
import { ScreenEntrance } from '../animations';
import type { CurioTopic, CurioCategory } from '../types';

type CatalogLane = { category: CurioCategory; topics: CurioTopic[] };
type TopicRow = { topic: CurioTopic; category: CurioCategory };

const PAGE_SIZE = 18;
const ACCENT = '#DA1E37';

export const TopicBrowserScreen: React.FC = () => {
  const navigate = useNavigate();
  const { isDark, isAmoled } = useTheme();
  const [query, setQuery] = useState('');
  const [selectedCat, setSelectedCat] = useState<string | null>(null);
  const [sortMode, setSortMode] = useState<'curated' | 'az' | 'za'>('curated');
  const [page, setPage] = useState(1);
  const [catalog, setCatalog] = useState<CatalogLane[]>([]);
  const [loading, setLoading] = useState(true);

  const muted = isDark ? 'rgba(245,243,244,.56)' : 'rgba(23,27,38,.58)';
  const subtle = isDark ? 'rgba(245,243,244,.36)' : 'rgba(23,27,38,.38)';
  const panel = isDark ? '#14181D' : '#FFFFFF';
  const border = isDark ? 'rgba(255,255,255,.075)' : 'rgba(23,27,38,.075)';

  useEffect(() => {
    let active = true;
    const readyCategories = ALL_CATEGORIES.filter(category => category.isReady && !category.isHidden);
    Promise.all(readyCategories.map(async category => ({
      category,
      topics: await loadTopicsForCategory(category.id),
    }))).then(results => {
      if (!active) return;
      setCatalog(results);
      setLoading(false);
    });
    return () => { active = false; };
  }, []);

  useEffect(() => { setPage(1); }, [query, selectedCat, sortMode]);

  const totalTopics = catalog.reduce((sum, lane) => sum + lane.topics.length, 0);
  const filtered = useMemo<TopicRow[]>(() => {
    const needle = query.trim().toLocaleLowerCase();
    const lanes = selectedCat ? catalog.filter(lane => lane.category.id === selectedCat) : catalog;
    const rows: TopicRow[] = [];
    lanes.forEach(({ category, topics }) => {
      topics.forEach(topic => {
        const matches = !needle ||
          topic.name.toLocaleLowerCase().includes(needle) ||
          topic.subtype.toLocaleLowerCase().includes(needle) ||
          topic.teaser.toLocaleLowerCase().includes(needle) ||
          topic.aliases.some(alias => alias.toLocaleLowerCase().includes(needle)) ||
          (topic.tags || []).some(tag => tag.toLocaleLowerCase().includes(needle));
        if (matches) rows.push({ topic, category });
      });
    });
    if (sortMode === 'az') rows.sort((a, b) => a.topic.name.localeCompare(b.topic.name));
    if (sortMode === 'za') rows.sort((a, b) => b.topic.name.localeCompare(a.topic.name));
    return rows;
  }, [catalog, query, selectedCat, sortMode]);

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const safePage = Math.min(page, totalPages);
  const pageRows = useMemo(
    () => filtered.slice((safePage - 1) * PAGE_SIZE, safePage * PAGE_SIZE),
    [filtered, safePage],
  );
  const pageNumbers = useMemo(() => {
    const start = Math.max(1, Math.min(safePage - 2, totalPages - 4));
    return Array.from({ length: Math.min(5, totalPages) }, (_, index) => start + index);
  }, [safePage, totalPages]);

  const selectCategory = (id: string | null) => {
    setSelectedCat(current => current === id ? null : id);
    setPage(1);
  };

  return (
    <div className="curio-explore-screen min-h-screen relative pb-32" style={{ backgroundColor: getBackgroundColor(isDark, isAmoled) }}>
      <ScreenEntrance>
        <div className="curio-explore-content w-full mx-auto px-4 pt-4 sm:px-6 sm:pt-7">
          <header className="relative overflow-hidden rounded-[24px] p-5 sm:p-7 text-white"
            style={{
              background: 'linear-gradient(125deg, #111820 0%, #202831 58%, #38151D 100%)',
              boxShadow: '0 20px 48px -30px rgba(0,0,0,.65)',
            }}>
            <div className="absolute -right-7 -top-9 w-44 h-44 rounded-full pointer-events-none"
              style={{ background: 'radial-gradient(circle, rgba(218,30,55,.30), transparent 68%)' }} />
            <div className="absolute right-5 top-5 opacity-[.12] rotate-[-12deg] pointer-events-none">
              <MaterialIcon name="travel_explore" size={104} />
            </div>
            <div className="relative z-10">
              <div className="flex items-center justify-between mb-7">
                <div className="flex items-center gap-2 text-[10px] font-bold tracking-[.22em] uppercase text-white/55">
                  <span className="w-1.5 h-1.5 rounded-full" style={{ background: ACCENT }} />
                  Curio / Discovery index
                </div>
                <span className="text-[10px] tracking-widest uppercase text-white/45">01 — Explore</span>
              </div>
              <h1 className="text-[34px] sm:text-[42px] leading-none font-extrabold tracking-tight" style={{ fontFamily: 'Geom, Inter, sans-serif' }}>
                Explore<span style={{ color: '#F05265' }}>.</span>
              </h1>
              <p className="mt-3 max-w-2xl text-sm leading-relaxed text-white/65">
                Follow a curiosity. Find a new favourite. There is always something worth discovering.
              </p>
              <div className="mt-6 flex items-end gap-6">
                <div>
                  <div className="text-2xl font-bold tabular-nums">{loading ? '—' : totalTopics.toLocaleString()}</div>
                  <div className="mt-0.5 text-[10px] uppercase tracking-[.16em] text-white/45">Topics to explore</div>
                </div>
                <div className="h-8 w-px bg-white/15" />
                <div>
                  <div className="text-2xl font-bold tabular-nums">{loading ? '—' : catalog.length}</div>
                  <div className="mt-0.5 text-[10px] uppercase tracking-[.16em] text-white/45">Curated lanes</div>
                </div>
              </div>
            </div>
          </header>

          <section className="mt-6" aria-label="Search and filters">
            <div className="flex items-center justify-between mb-3">
              <div>
                <div className="text-[10px] font-bold uppercase tracking-[.2em]" style={{ color: ACCENT }}>The catalogue</div>
                <h2 className="mt-1 text-xl font-bold tracking-tight" style={{ color: getTextColor(isDark) }}>Find your next rabbit hole</h2>
              </div>
              <button onClick={() => { setQuery(''); setSelectedCat(null); setSortMode('curated'); setPage(1); }}
                className="text-xs font-semibold px-3 py-2 rounded-full transition-colors"
                style={{ color: muted, background: isDark ? 'rgba(255,255,255,.055)' : 'rgba(23,27,38,.045)' }}>
                Reset
              </button>
            </div>

            <div className="relative">
              <MaterialIcon name="search" size={19} className="absolute left-4 top-1/2 -translate-y-1/2 pointer-events-none" style={{ color: subtle }} />
              <input value={query} onChange={event => setQuery(event.target.value)}
                placeholder={loading ? 'Preparing the discovery index…' : `Search ${totalTopics.toLocaleString()} topics, aliases and ideas`}
                aria-label="Search topics"
                className="w-full pl-11 pr-11 py-4 rounded-2xl text-sm outline-none"
                style={{ background: panel, color: getTextColor(isDark), border: `1px solid ${border}`, boxShadow: '0 8px 26px -22px rgba(0,0,0,.5)' }} />
              {query && <button onClick={() => setQuery('')} aria-label="Clear search"
                className="absolute right-3 top-1/2 -translate-y-1/2 w-8 h-8 rounded-full flex items-center justify-center"
                style={{ background: isDark ? 'rgba(255,255,255,.09)' : 'rgba(23,27,38,.07)', color: muted }}>
                <MaterialIcon name="close" size={16} />
              </button>}
            </div>

            <div className="mt-4">
              <div className="flex items-center justify-between mb-2">
                <span className="text-[10px] font-bold uppercase tracking-[.17em]" style={{ color: muted }}>Browse by lane</span>
                <span className="text-[10px]" style={{ color: subtle }}>{selectedCat ? '1 lane selected' : 'All lanes'}</span>
              </div>
              <div className="flex gap-2 overflow-x-auto pb-2 curio-explore-lanes">
                <LaneButton label="Everything" count={totalTopics} selected={selectedCat === null}
                  onClick={() => selectCategory(null)} isDark={isDark} />
                {catalog.map(({ category, topics }) => (
                  <LaneButton key={category.id} label={category.displayName} count={topics.length}
                    selected={selectedCat === category.id} accent={category.accent}
                    onClick={() => selectCategory(category.id)} isDark={isDark} />
                ))}
              </div>
            </div>
          </section>

          <section className="mt-5" aria-label="Topic results">
            <div className="flex items-end justify-between gap-3 mb-3">
              <div>
                <h3 className="text-base font-bold" style={{ color: getTextColor(isDark) }}>
                  {selectedCat ? catalog.find(lane => lane.category.id === selectedCat)?.category.displayName : query ? 'Search results' : 'All discoveries'}
                </h3>
                <p className="text-xs mt-1" style={{ color: subtle }}>
                  {loading ? 'Loading the catalogue…' : `${filtered.length.toLocaleString()} ${filtered.length === 1 ? 'result' : 'results'} · showing ${filtered.length ? (safePage - 1) * PAGE_SIZE + 1 : 0}–${Math.min(safePage * PAGE_SIZE, filtered.length)}`}
                </p>
              </div>
              <div className="flex items-center gap-1 p-1 rounded-xl shrink-0" style={{ background: isDark ? 'rgba(255,255,255,.055)' : 'rgba(23,27,38,.045)' }}>
                <SortButton label="Curated" active={sortMode === 'curated'} onClick={() => setSortMode('curated')} isDark={isDark} />
                <SortButton label="A–Z" active={sortMode === 'az'} onClick={() => setSortMode('az')} isDark={isDark} />
                <SortButton label="Z–A" active={sortMode === 'za'} onClick={() => setSortMode('za')} isDark={isDark} />
              </div>
            </div>

            {loading ? (
              <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-3 sm:gap-4">
                {Array.from({ length: 6 }, (_, index) => <div key={index} className="h-32 rounded-2xl animate-pulse"
                  style={{ background: isDark ? 'rgba(255,255,255,.055)' : 'rgba(23,27,38,.055)' }} />)}
              </div>
            ) : filtered.length === 0 ? (
              <div className="rounded-2xl p-10 text-center" style={{ background: panel, border: `1px solid ${border}` }}>
                <div className="mx-auto w-14 h-14 rounded-2xl flex items-center justify-center" style={{ background: 'rgba(218,30,55,.10)', color: ACCENT }}>
                  <MaterialIcon name="search_off" size={26} />
                </div>
                <h4 className="mt-4 font-bold" style={{ color: getTextColor(isDark) }}>Nothing found this time</h4>
                <p className="mt-1 text-sm" style={{ color: muted }}>Try another phrase or choose a different lane.</p>
                <button onClick={() => { setQuery(''); setSelectedCat(null); }} className="mt-4 text-sm font-bold" style={{ color: ACCENT }}>Clear filters</button>
              </div>
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                {pageRows.map(({ topic, category }, index) => (
                  <button key={topic.id} onClick={() => navigate(`/reveal/${category.id.toLowerCase()}/${topic.id}`)}
                    className="group relative min-w-0 text-left rounded-2xl p-4 overflow-hidden transition-all duration-200 active:scale-[.985] curio-explore-topic"
                    style={{ background: panel, border: `1px solid ${border}`, animationDelay: `${Math.min(index, 8) * 22}ms` }}>
                    <div className="absolute right-0 top-0 w-24 h-24 rounded-full -translate-y-1/2 translate-x-1/2 pointer-events-none"
                      style={{ background: `radial-gradient(circle, ${category.accent}18, transparent 70%)` }} />
                    <div className="relative flex items-start gap-3">
                      <div className="w-11 h-11 shrink-0 rounded-xl flex items-center justify-center"
                        style={{ background: `${category.accent}18`, color: category.accent }}>
                        <MaterialIcon name={category.iconGlyph} size={21} />
                      </div>
                      <div className="min-w-0 flex-1">
                        <div className="text-[10px] font-bold uppercase tracking-[.13em] truncate" style={{ color: category.accent }}>{category.displayName}</div>
                        <div className="mt-1 text-sm font-bold leading-snug line-clamp-2" style={{ color: getTextColor(isDark) }}>{topic.name}</div>
                      </div>
                      <MaterialIcon name="arrow_outward" size={16} className="shrink-0 mt-1 transition-transform group-hover:-translate-y-0.5 group-hover:translate-x-0.5"
                        style={{ color: subtle }} />
                    </div>
                    <p className="relative mt-3 text-xs leading-relaxed line-clamp-2" style={{ color: muted }}>
                      {topic.teaser || topic.subtype || 'A new discovery is waiting.'}
                    </p>
                  </button>
                ))}
              </div>
            )}

            {!loading && filtered.length > 0 && (
              <nav className="mt-6 mb-4 flex flex-col sm:flex-row items-center justify-between gap-3" aria-label="Explore pages">
                <div className="text-xs font-medium" style={{ color: muted }}>Page <strong style={{ color: getTextColor(isDark) }}>{safePage}</strong> of {totalPages}</div>
                <div className="flex items-center gap-1.5">
                  <PageButton label="Previous" icon="chevron_left" disabled={safePage === 1}
                    onClick={() => { setPage(current => Math.max(1, current - 1)); window.scrollTo({ top: 0, behavior: 'smooth' }); }} isDark={isDark} />
                  {pageNumbers.map(number => (
                    <button key={number} onClick={() => { setPage(number); window.scrollTo({ top: 0, behavior: 'smooth' }); }}
                      aria-label={`Go to page ${number}`} aria-current={safePage === number ? 'page' : undefined}
                      className="w-9 h-9 rounded-xl text-xs font-bold transition-colors"
                      style={{ background: safePage === number ? ACCENT : (isDark ? 'rgba(255,255,255,.055)' : 'rgba(23,27,38,.045)'), color: safePage === number ? '#fff' : muted }}>
                      {number}
                    </button>
                  ))}
                  <PageButton label="Next" icon="chevron_right" disabled={safePage === totalPages}
                    onClick={() => { setPage(current => Math.min(totalPages, current + 1)); window.scrollTo({ top: 0, behavior: 'smooth' }); }} isDark={isDark} />
                </div>
              </nav>
            )}
          </section>
        </div>
      </ScreenEntrance>
    </div>
  );
};

const LaneButton: React.FC<{ label: string; count: number; selected: boolean; accent?: string; onClick: () => void; isDark: boolean }> = ({ label, count, selected, accent = ACCENT, onClick, isDark }) => (
  <button onClick={onClick} aria-pressed={selected}
    className="shrink-0 flex items-center gap-2 px-3.5 py-2.5 rounded-xl text-xs font-semibold transition-all"
    style={{
      background: selected ? (isDark ? 'rgba(218,30,55,.18)' : 'rgba(218,30,55,.09)') : (isDark ? 'rgba(255,255,255,.045)' : 'rgba(23,27,38,.035)'),
      color: selected ? (selected && accent !== ACCENT ? accent : ACCENT) : (isDark ? 'rgba(245,243,244,.65)' : 'rgba(23,27,38,.66)'),
      border: `1px solid ${selected ? `${accent}55` : (isDark ? 'rgba(255,255,255,.06)' : 'rgba(23,27,38,.06)')}`,
    }}>
    {label}<span className="text-[10px] opacity-55 tabular-nums">{count.toLocaleString()}</span>
  </button>
);

const SortButton: React.FC<{ label: string; active: boolean; onClick: () => void; isDark: boolean }> = ({ label, active, onClick, isDark }) => (
  <button onClick={onClick} className="px-2.5 py-1.5 rounded-lg text-[10px] font-bold transition-all"
    style={{ background: active ? (isDark ? 'rgba(255,255,255,.13)' : '#FFFFFF') : 'transparent', color: active ? (isDark ? '#FFFFFF' : '#171B26') : (isDark ? 'rgba(245,243,244,.5)' : 'rgba(23,27,38,.5)'), boxShadow: active && !isDark ? '0 1px 4px rgba(0,0,0,.08)' : 'none' }}>
    {label}
  </button>
);

const PageButton: React.FC<{ label: string; icon: string; disabled: boolean; onClick: () => void; isDark: boolean }> = ({ label, icon, disabled, onClick, isDark }) => (
  <button onClick={onClick} disabled={disabled} aria-label={label}
    className="h-9 px-2.5 rounded-xl flex items-center gap-1 text-xs font-semibold disabled:opacity-30 transition-opacity"
    style={{ background: isDark ? 'rgba(255,255,255,.055)' : 'rgba(23,27,38,.045)', color: isDark ? '#F5F3F4' : '#171B26' }}>
    <MaterialIcon name={icon} size={16} /><span className="hidden sm:inline">{label}</span>
  </button>
);

export default TopicBrowserScreen;
