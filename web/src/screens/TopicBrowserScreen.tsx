// Curio Web App - Topic Browser Screen
// Matches Android topic database: torn hero, search, category filters, sort

import React, { useState, useEffect, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTheme, getBackgroundColor, getTextColor } from '../theme/ThemeContext';
import { ALL_CATEGORIES } from '../data/categories';
import { loadTopicsForCategory } from '../data/topics';
import { MaterialIcon, CurioWatermarkBackdrop } from '../components/SharedComponents';
import { TornHero, BROWSER_HERO_SYMBOLS } from '../components/TornHero';
import { ScreenEntrance } from '../animations';
import type { CurioTopic, CurioCategory } from '../types';

const BROWSER_HERO_HEIGHT = 180;
const BROWSER_TEAR_SEED = 0xBD0DE; // Browser-specific seed
const ROSE_WOOD = '#C46B7C';

export const TopicBrowserScreen: React.FC = () => {
  const navigate = useNavigate();
  const { isDark, isAmoled } = useTheme();
  const [query, setQuery] = useState('');
  const [selectedCat, setSelectedCat] = useState<string | null>(null);
  const [sortMode, setSortMode] = useState<'default' | 'az' | 'za'>('default');
  const [catalog, setCatalog] = useState<Array<{ category: CurioCategory; topics: CurioTopic[] }>>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    (async () => {
      setLoading(true);
      const results: Array<{ category: CurioCategory; topics: CurioTopic[] }> = [];
      for (const cat of ALL_CATEGORIES.filter(c => c.isReady)) {
        const topics = await loadTopicsForCategory(cat.id);
        results.push({ category: cat, topics });
      }
      setCatalog(results);
      setLoading(false);
    })();
  }, []);

  const totalTopics = catalog.reduce((s, c) => s + c.topics.length, 0);

  const filtered = useMemo(() => {
    const needle = query.trim().toLowerCase();
    let rows: Array<{ key: string; section?: CurioCategory; sectionCount?: number; topic?: CurioTopic; cat?: CurioCategory }> = [];
    const visible = selectedCat ? catalog.filter(c => c.category.id === selectedCat) : catalog;
    if (sortMode === 'default') {
      visible.forEach(({ category, topics }) => {
        const ft = topics.filter(t => !needle || t.name.toLowerCase().includes(needle) || t.subtype.toLowerCase().includes(needle));
        if (ft.length === 0) return;
        if (!selectedCat) rows.push({ key: `sec-${category.id}`, section: category, sectionCount: ft.length });
        ft.forEach(t => rows.push({ key: t.id, topic: t, cat: category }));
      });
    } else {
      let flat: Array<{ topic: CurioTopic; cat: CurioCategory }> = [];
      visible.forEach(({ category, topics }) => {
        topics.forEach(t => { if (!needle || t.name.toLowerCase().includes(needle) || t.subtype.toLowerCase().includes(needle)) flat.push({ topic: t, cat: category }); });
      });
      flat.sort((a, b) => { const cmp = a.topic.name.localeCompare(b.topic.name); return sortMode === 'za' ? -cmp : cmp; });
      rows = flat.map(f => ({ key: f.topic.id, topic: f.topic, cat: f.cat }));
    }
    return rows.slice(0, 400);
  }, [catalog, query, selectedCat, sortMode]);

  return (
    <div className="min-h-screen pb-24 relative" style={{ backgroundColor: getBackgroundColor(isDark, isAmoled) }}>
      <CurioWatermarkBackdrop topClearance={BROWSER_HERO_HEIGHT + 30} alphaScale={0.45} />

      {/* ── Torn Hero Banner ──────────────────────────────────────── */}
      <TornHero
        height={BROWSER_HERO_HEIGHT}
        fill={ROSE_WOOD}
        ink="#fff"
        tearSeed={BROWSER_TEAR_SEED}
        bold={true}
        symbols={BROWSER_HERO_SYMBOLS}
        isDark={isDark}
      >
        <div className="flex flex-col h-full px-5 pt-[68px] pb-[18px] justify-end">
          <button onClick={() => navigate(-1)} className="absolute top-0 left-5 w-10 h-10 rounded-full flex items-center justify-center"
            style={{ background: 'rgba(255,255,255,0.18)', border: '1px solid rgba(255,255,255,0.25)', marginTop: 'env(safe-area-inset-top, 12px)' }}>
            <MaterialIcon name="arrow_back" size={20} style={{ color: '#fff' }} />
          </button>
          <h1 className="text-xl font-extrabold text-white text-center" style={{ fontFamily: 'Geom, Inter, sans-serif' }}>
            Topic Database
          </h1>
          <p className="text-xs text-white/70 text-center mt-0.5">{totalTopics.toLocaleString()} topics across {catalog.length} lanes</p>
        </div>
      </TornHero>

      <ScreenEntrance>
        {/* Sticky search + facets */}
        <div className="sticky top-0 z-20 px-4 pt-4 pb-2"
          style={{ background: getBackgroundColor(isDark, isAmoled) }}>
          <div className="relative mb-3">
            <MaterialIcon name="search" size={18} className="absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none"
              style={{ color: isDark ? 'rgba(255,255,255,0.35)' : 'rgba(59,10,23,0.35)' }} />
            <input type="text" value={query} onChange={e => setQuery(e.target.value)}
              placeholder={`Search ${totalTopics} topics…`}
              className="w-full pl-10 pr-10 py-2.5 rounded-full text-sm outline-none"
              style={{ background: isDark ? 'rgba(255,255,255,0.06)' : 'rgba(59,10,23,0.04)', color: getTextColor(isDark),
                border: `1px solid ${isDark ? 'rgba(255,255,255,0.08)' : 'rgba(59,10,23,0.08)'}` }} />
            {query && (
              <button onClick={() => setQuery('')} className="absolute right-2 top-1/2 -translate-y-1/2 w-7 h-7 rounded-full flex items-center justify-center"
                style={{ background: isDark ? 'rgba(255,255,255,0.12)' : 'rgba(59,10,23,0.08)' }}>
                <MaterialIcon name="close" size={14} style={{ color: isDark ? 'rgba(255,255,255,0.5)' : 'rgba(59,10,23,0.5)' }} />
              </button>
            )}
          </div>
          <div className="flex gap-2 overflow-x-auto pb-2">
            <FilterChip label="All" count={totalTopics} accent="#FF8FA3" selected={selectedCat === null} onClick={() => setSelectedCat(null)} />
            {catalog.map(({ category, topics }) => (
              <FilterChip key={category.id} label={category.displayName} count={topics.length} accent={category.accent}
                selected={selectedCat === category.id} onClick={() => setSelectedCat(selectedCat === category.id ? null : category.id)} />
            ))}
          </div>
          <div className="flex gap-2 overflow-x-auto pb-1">
            <SortChip label="A–Z" icon="arrow_upward" selected={sortMode === 'az'} onClick={() => setSortMode(sortMode === 'az' ? 'default' : 'az')} />
            <SortChip label="Z–A" icon="arrow_downward" selected={sortMode === 'za'} onClick={() => setSortMode(sortMode === 'za' ? 'default' : 'za')} />
          </div>
        </div>

        {/* Topic list */}
        <div className="px-4 pt-2">
          {loading ? (
            <div className="flex items-center justify-center py-16">
              <div className="w-8 h-8 border-4 border-t-transparent rounded-full animate-spin" style={{ borderColor: '#FF8FA3', borderTopColor: 'transparent' }} />
            </div>
          ) : filtered.length === 0 ? (
            <div className="text-center py-16">
              <MaterialIcon name="search_off" size={48} style={{ color: isDark ? 'rgba(255,255,255,0.25)' : 'rgba(59,10,23,0.25)' }} />
              <p className="mt-3 text-sm font-medium" style={{ color: isDark ? 'rgba(255,255,255,0.4)' : 'rgba(59,10,23,0.4)' }}>
                {query ? 'No topics match your search' : 'No topics in this category'}
              </p>
            </div>
          ) : (
            <div className="space-y-1 pb-8">
              {filtered.map(row => row.section ? (
                <div key={row.key} className="flex items-center gap-2 pt-2 pb-1 px-1">
                  <div className="w-2.5 h-2.5 rounded-full flex-shrink-0" style={{ background: row.section.accent }} />
                  <span className="text-xs font-bold uppercase tracking-wider" style={{ color: isDark ? 'rgba(255,255,255,0.5)' : 'rgba(59,10,23,0.5)' }}>
                    {row.section.displayName}
                  </span>
                  <span className="text-[10px]" style={{ color: isDark ? 'rgba(255,255,255,0.25)' : 'rgba(59,10,23,0.25)' }}>{row.sectionCount}</span>
                </div>
              ) : (
                <button key={row.key} onClick={() => navigate(`/reveal/${row.cat!.id.toLowerCase()}/${row.topic!.id}`)}
                  className="w-full text-left flex items-center gap-3 p-3 rounded-xl transition-all active:scale-[0.98]"
                  style={{ background: isDark ? 'rgba(255,255,255,0.02)' : 'rgba(59,10,23,0.01)',
                    border: `1px solid ${isDark ? 'rgba(255,255,255,0.04)' : 'rgba(59,10,23,0.03)'}` }}>
                  <div className="w-9 h-9 rounded-lg flex items-center justify-center flex-shrink-0" style={{ background: `${row.cat!.accent}18` }}>
                    <MaterialIcon name={row.cat!.iconGlyph} size={18} style={{ color: row.cat!.accent }} />
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="text-sm font-semibold truncate" style={{ color: getTextColor(isDark) }}>{row.topic!.name}</div>
                    <div className="text-[11px] truncate mt-0.5 flex items-center gap-1.5" style={{ color: isDark ? 'rgba(255,255,255,0.35)' : 'rgba(59,10,23,0.35)' }}>
                      <span>{row.cat!.displayName}</span><span>·</span><span>{row.topic!.subtype}</span>
                    </div>
                    {row.topic!.teaser && (
                      <div className="text-xs mt-1 line-clamp-1" style={{ color: isDark ? 'rgba(255,255,255,0.25)' : 'rgba(59,10,23,0.25)' }}>{row.topic!.teaser}</div>
                    )}
                  </div>
                  <MaterialIcon name="chevron_right" size={16} style={{ color: isDark ? 'rgba(255,255,255,0.2)' : 'rgba(59,10,23,0.2)' }} />
                </button>
              ))}
            </div>
          )}
        </div>
      </ScreenEntrance>
    </div>
  );
};

const FilterChip: React.FC<{ label: string; count: number; accent: string; selected: boolean; onClick: () => void }> = ({ label, count, accent, selected, onClick }) => {
  const { isDark } = useTheme();
  return (
    <button onClick={onClick} className="flex-shrink-0 px-3.5 py-2 rounded-full text-sm font-medium transition-all whitespace-nowrap"
      style={{
        background: selected ? `${accent}18` : (isDark ? 'rgba(255,255,255,0.06)' : 'rgba(59,10,23,0.04)'),
        color: selected ? accent : (isDark ? 'rgba(255,255,255,0.6)' : 'rgba(59,10,23,0.6)'),
        border: selected ? `1px solid ${accent}40` : `1px solid ${isDark ? 'rgba(255,255,255,0.06)' : 'rgba(59,10,23,0.06)'}`,
      }}>
      {label} <span className="text-xs opacity-60 ml-0.5">{count}</span>
    </button>
  );
};

const SortChip: React.FC<{ label: string; icon: string; selected: boolean; onClick: () => void }> = ({ label, icon, selected, onClick }) => {
  const { isDark } = useTheme();
  return (
    <button onClick={onClick} className="flex-shrink-0 flex items-center gap-1 px-3 py-1.5 rounded-full text-xs font-medium transition-all"
      style={{
        background: selected ? (isDark ? 'rgba(255,255,255,0.12)' : 'rgba(59,10,23,0.08)') : (isDark ? 'rgba(255,255,255,0.04)' : 'rgba(59,10,23,0.02)'),
        color: selected ? getTextColor(isDark) : (isDark ? 'rgba(255,255,255,0.45)' : 'rgba(59,10,23,0.45)'),
        border: `1px solid ${isDark ? 'rgba(255,255,255,0.06)' : 'rgba(59,10,23,0.06)'}`,
      }}>
      <MaterialIcon name={icon} size={14} /> {label}
    </button>
  );
};

export default TopicBrowserScreen;
