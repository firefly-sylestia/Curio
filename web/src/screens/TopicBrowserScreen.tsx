// Curio Web App - Topic Browser Screen
// Matches Android TopicDatabaseScreen: search, category filters, sort, section headers

import React, { useState, useEffect, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTheme, getBackgroundColor, getTextColor } from '../theme/ThemeContext';
import { ALL_CATEGORIES } from '../data/categories';
import { loadTopicsForCategory } from '../data/topics';
import { MaterialIcon, CurioWatermarkBackdrop } from '../components/SharedComponents';
import type { CurioTopic, CurioCategory } from '../types';

export const TopicBrowserScreen: React.FC = () => {
  const navigate = useNavigate();
  const { isDark, isAmoled } = useTheme();
  const [query, setQuery] = useState('');
  const [selectedCat, setSelectedCat] = useState<string | null>(null);
  const [sortMode, setSortMode] = useState<'default' | 'az' | 'za'>('default');
  const [catalog, setCatalog] = useState<Array<{ category: CurioCategory; topics: CurioTopic[] }>>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      const results: Array<{ category: CurioCategory; topics: CurioTopic[] }> = [];
      const cats = ALL_CATEGORIES.filter(c => c.isReady);
      for (const cat of cats) {
        const topics = await loadTopicsForCategory(cat.id);
        results.push({ category: cat, topics });
      }
      setCatalog(results);
      setLoading(false);
    };
    load();
  }, []);

  const totalTopics = catalog.reduce((sum, c) => sum + c.topics.length, 0);

  const filtered = useMemo(() => {
    const needle = query.trim().toLowerCase();
    let rows: Array<{
      key: string;
      section?: CurioCategory;
      sectionCount?: number;
      topic?: CurioTopic;
      cat?: CurioCategory;
    }> = [];

    const visible = selectedCat
      ? catalog.filter(c => c.category.id === selectedCat)
      : catalog;

    if (sortMode === 'default') {
      visible.forEach(({ category, topics }) => {
        const filtered = topics.filter(t =>
          !needle || t.name.toLowerCase().includes(needle) || t.subtype.toLowerCase().includes(needle)
        );
        if (filtered.length === 0) return;
        if (!selectedCat) {
          rows.push({ key: `sec-${category.id}`, section: category, sectionCount: filtered.length });
        }
        filtered.forEach(t => rows.push({ key: t.id, topic: t, cat: category }));
      });
    } else {
      // A-Z or Z-A flat sort
      let flat: Array<{ topic: CurioTopic; cat: CurioCategory }> = [];
      visible.forEach(({ category, topics }) => {
        topics.forEach(t => {
          if (!needle || t.name.toLowerCase().includes(needle) || t.subtype.toLowerCase().includes(needle)) {
            flat.push({ topic: t, cat: category });
          }
        });
      });
      flat.sort((a, b) => {
        const cmp = a.topic.name.localeCompare(b.topic.name);
        return sortMode === 'za' ? -cmp : cmp;
      });
      rows = flat.map(f => ({ key: f.topic.id, topic: f.topic, cat: f.cat }));
    }
    return rows.slice(0, 400);
  }, [catalog, query, selectedCat, sortMode]);

  return (
    <div className="min-h-screen pb-24 relative" style={{ backgroundColor: getBackgroundColor(isDark, isAmoled) }}>
      <CurioWatermarkBackdrop alphaScale={0.45} />

      <div className="relative z-10">
        {/* Header */}
        <div className="sticky top-0 z-20 px-4 pt-6 pb-3"
          style={{
            background: getBackgroundColor(isDark, isAmoled),
            borderBottom: `1px solid ${isDark ? 'rgba(255,255,255,0.06)' : 'rgba(59,10,23,0.06)'}`,
          }}>
          <div className="flex items-center gap-3 mb-4">
            <div className="w-10 h-10 rounded-xl flex items-center justify-center"
              style={{ background: isDark ? 'rgba(255,143,163,0.15)' : 'rgba(255,143,163,0.12)' }}>
              <MaterialIcon name="travel_explore" size={22} style={{ color: '#FF8FA3' }} />
            </div>
            <div>
              <h1 className="text-xl font-bold" style={{ color: getTextColor(isDark), fontFamily: 'Geom, Inter, sans-serif' }}>
                Topic Database
              </h1>
              <p className="text-xs" style={{ color: isDark ? 'rgba(255,255,255,0.4)' : 'rgba(59,10,23,0.4)' }}>
                {totalTopics.toLocaleString()} topics across {catalog.length} lanes
              </p>
            </div>
          </div>

          {/* Search */}
          <div className="relative mb-3">
            <MaterialIcon name="search" size={18} className="absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none"
              style={{ color: isDark ? 'rgba(255,255,255,0.35)' : 'rgba(59,10,23,0.35)' }} />
            <input type="text" value={query} onChange={e => setQuery(e.target.value)}
              placeholder={`Search ${totalTopics} topics…`}
              className="w-full pl-10 pr-10 py-2.5 rounded-full text-sm outline-none transition-colors"
              style={{ background: isDark ? 'rgba(255,255,255,0.06)' : 'rgba(59,10,23,0.04)', color: getTextColor(isDark),
                border: `1px solid ${isDark ? 'rgba(255,255,255,0.08)' : 'rgba(59,10,23,0.08)'}` }} />
            {query && (
              <button onClick={() => setQuery('')}
                className="absolute right-2 top-1/2 -translate-y-1/2 w-7 h-7 rounded-full flex items-center justify-center"
                style={{ background: isDark ? 'rgba(255,255,255,0.12)' : 'rgba(59,10,23,0.08)' }}>
                <MaterialIcon name="close" size={14} style={{ color: isDark ? 'rgba(255,255,255,0.5)' : 'rgba(59,10,23,0.5)' }} />
              </button>
            )}
          </div>

          {/* Category filter chips */}
          <div className="flex gap-2 overflow-x-auto pb-2">
            <FilterChip label="All" count={totalTopics} accent="#FF8FA3" tint="rgba(255,143,163,0.15)"
              selected={selectedCat === null} onClick={() => setSelectedCat(null)} />
            {catalog.map(({ category, topics }) => (
              <FilterChip key={category.id} label={category.displayName} count={topics.length}
                accent={category.accent} tint={`${category.accent}18`
              } selected={selectedCat === category.id}
                onClick={() => setSelectedCat(selectedCat === category.id ? null : category.id)} />
            ))}
          </div>

          {/* Sort row */}
          <div className="flex gap-2 overflow-x-auto pb-1">
            <SortChip label="A–Z" icon="arrow_upward" selected={sortMode === 'az'}
              onClick={() => setSortMode(sortMode === 'az' ? 'default' : 'az')} />
            <SortChip label="Z–A" icon="arrow_downward" selected={sortMode === 'za'}
              onClick={() => setSortMode(sortMode === 'za' ? 'default' : 'za')} />
          </div>
        </div>

        {/* Topic list */}
        <div className="px-4 pt-4">
          {loading ? (
            <div className="flex items-center justify-center py-16">
              <div className="w-8 h-8 border-4 border-t-transparent rounded-full animate-spin"
                style={{ borderColor: '#FF8FA3', borderTopColor: 'transparent' }} />
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
              {filtered.map((row) =>
                row.section ? (
                  <div key={row.key} className="flex items-center gap-2 pt-4 pb-1 px-1">
                    <div className="w-2.5 h-2.5 rounded-full flex-shrink-0" style={{ background: row.section.accent }} />
                    <span className="text-xs font-bold uppercase tracking-wider"
                      style={{ color: isDark ? 'rgba(255,255,255,0.5)' : 'rgba(59,10,23,0.5)' }}>
                      {row.section.displayName}
                    </span>
                    <span className="text-[10px]" style={{ color: isDark ? 'rgba(255,255,255,0.25)' : 'rgba(59,10,23,0.25)' }}>
                      {row.sectionCount}
                    </span>
                  </div>
                ) : (
                  <TopicRow key={row.key} topic={row.topic!} category={row.cat!}
                    onClick={() => navigate(`/reveal/${row.cat!.id.toLowerCase()}/${row.topic!.id}`)} />
                )
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

// ─── Sub-components ──────────────────────────────────────────────────

const FilterChip: React.FC<{
  label: string; count: number; accent: string; tint: string; selected: boolean; onClick: () => void;
}> = ({ label, count, accent, tint, selected, onClick }) => {
  const { isDark } = useTheme();
  return (
    <button onClick={onClick}
      className="flex-shrink-0 px-3.5 py-2 rounded-full text-sm font-medium transition-all whitespace-nowrap"
      style={{
        background: selected ? tint : (isDark ? 'rgba(255,255,255,0.06)' : 'rgba(59,10,23,0.04)'),
        color: selected ? accent : (isDark ? 'rgba(255,255,255,0.6)' : 'rgba(59,10,23,0.6)'),
        border: selected ? `1px solid ${accent}40` : `1px solid ${isDark ? 'rgba(255,255,255,0.06)' : 'rgba(59,10,23,0.06)'}`,
      }}>
      {label} <span className="text-xs opacity-60 ml-0.5">{count}</span>
    </button>
  );
};

const SortChip: React.FC<{
  label: string; icon: string; selected: boolean; onClick: () => void;
}> = ({ label, icon, selected, onClick }) => {
  const { isDark } = useTheme();
  return (
    <button onClick={onClick}
      className="flex-shrink-0 flex items-center gap-1 px-3 py-1.5 rounded-full text-xs font-medium transition-all"
      style={{
        background: selected ? (isDark ? 'rgba(255,255,255,0.12)' : 'rgba(59,10,23,0.08)') : (isDark ? 'rgba(255,255,255,0.04)' : 'rgba(59,10,23,0.02)'),
        color: selected ? getTextColor(isDark) : (isDark ? 'rgba(255,255,255,0.45)' : 'rgba(59,10,23,0.45)'),
        border: `1px solid ${isDark ? 'rgba(255,255,255,0.06)' : 'rgba(59,10,23,0.06)'}`,
      }}>
      <MaterialIcon name={icon} size={14} />
      {label}
    </button>
  );
};

const TopicRow: React.FC<{ topic: CurioTopic; category: CurioCategory; onClick: () => void }> = ({ topic, category, onClick }) => {
  const { isDark } = useTheme();
  return (
    <button onClick={onClick}
      className="w-full text-left flex items-center gap-3 p-3 rounded-xl transition-all active:scale-[0.98]"
      style={{ background: isDark ? 'rgba(255,255,255,0.02)' : 'rgba(59,10,23,0.01)',
        border: `1px solid ${isDark ? 'rgba(255,255,255,0.04)' : 'rgba(59,10,23,0.03)'}` }}>
      <div className="w-9 h-9 rounded-lg flex items-center justify-center flex-shrink-0"
        style={{ background: `${category.accent}18` }}>
        <MaterialIcon name={category.iconGlyph} size={18} style={{ color: category.accent }} />
      </div>
      <div className="flex-1 min-w-0">
        <div className="text-sm font-semibold truncate" style={{ color: getTextColor(isDark) }}>
          {topic.name}
        </div>
        <div className="text-[11px] truncate mt-0.5 flex items-center gap-1.5"
          style={{ color: isDark ? 'rgba(255,255,255,0.35)' : 'rgba(59,10,23,0.35)' }}>
          <span>{category.displayName}</span>
          <span>·</span>
          <span>{topic.subtype}</span>
        </div>
        {topic.teaser && (
          <div className="text-xs mt-1 line-clamp-1" style={{ color: isDark ? 'rgba(255,255,255,0.25)' : 'rgba(59,10,23,0.25)' }}>
            {topic.teaser}
          </div>
        )}
      </div>
      <MaterialIcon name="chevron_right" size={16}
        style={{ color: isDark ? 'rgba(255,255,255,0.2)' : 'rgba(59,10,23,0.2)' }} />
    </button>
  );
};

export default TopicBrowserScreen;
