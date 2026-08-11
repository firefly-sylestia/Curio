// Curio Web App - Topic Browser Screen
// Browse all topics with search and category filters

import React, { useState, useEffect, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTheme, getBackgroundColor, getTextColor } from '../theme/ThemeContext';
import { ALL_CATEGORIES } from '../data/categories';
import { loadTopicsForCategory } from '../data/topics';
import { MaterialIcon, CurioChip } from '../components/SharedComponents';
import type { CurioTopic, CurioCategory } from '../types';

export const TopicBrowserScreen: React.FC = () => {
  const navigate = useNavigate();
  const { isDark, isAmoled } = useTheme();
  const [query, setQuery] = useState('');
  const [selectedCategoryId, setSelectedCategoryId] = useState<string | null>(null);
  const [allTopics, setAllTopics] = useState<Array<{ topic: CurioTopic; category: CurioCategory }>>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      const results: Array<{ topic: CurioTopic; category: CurioCategory }> = [];
      for (const cat of ALL_CATEGORIES.filter(c => c.isReady)) {
        const topics = await loadTopicsForCategory(cat.id);
        topics.forEach(t => results.push({ topic: t, category: cat }));
      }
      setAllTopics(results);
      setLoading(false);
    };
    load();
  }, []);

  const filtered = useMemo(() => {
    let r = allTopics;
    if (selectedCategoryId) {
      r = r.filter(x => x.category.id === selectedCategoryId);
    }
    if (query.trim()) {
      const q = query.toLowerCase();
      r = r.filter(x =>
        x.topic.name.toLowerCase().includes(q) ||
        x.topic.subtype.toLowerCase().includes(q) ||
        x.topic.teaser.toLowerCase().includes(q)
      );
    }
    return r.slice(0, 100);
  }, [allTopics, query, selectedCategoryId]);

  return (
    <div className="min-h-screen pb-24" style={{ backgroundColor: getBackgroundColor(isDark, isAmoled) }}>
      {/* Header */}
      <div className="sticky top-0 z-10 px-4 pt-6 pb-3"
        style={{
          background: getBackgroundColor(isDark, isAmoled),
          borderBottom: `1px solid ${isDark ? 'rgba(255,255,255,0.06)' : 'rgba(59,10,23,0.06)'}`,
        }}>
        <div className="flex items-center gap-3 mb-4">
          <MaterialIcon name="travel_explore" size={24} style={{ color: '#FF8FA3' }} />
          <h1 className="text-2xl font-bold" style={{ color: getTextColor(isDark), fontFamily: 'Geom, Inter, sans-serif' }}>
            Browse Topics
          </h1>
        </div>

        {/* Search */}
        <div className="relative mb-3">
          <div className="absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none">
            <MaterialIcon name="search" size={18}
              style={{ color: isDark ? 'rgba(255,255,255,0.35)' : 'rgba(59,10,23,0.35)' }} />
          </div>
          <input
            type="text"
            value={query}
            onChange={e => setQuery(e.target.value)}
            placeholder="Search topics..."
            className="w-full pl-10 pr-4 py-3 rounded-xl text-sm outline-none"
            style={{
              background: isDark ? 'rgba(255,255,255,0.05)' : 'rgba(59,10,23,0.03)',
              color: getTextColor(isDark),
              border: `1px solid ${isDark ? 'rgba(255,255,255,0.08)' : 'rgba(59,10,23,0.08)'}`,
            }}
          />
        </div>

        {/* Category filter chips */}
        <div className="flex gap-2 overflow-x-auto pb-1">
          <CurioChip label="All" isSelected={selectedCategoryId === null} onClick={() => setSelectedCategoryId(null)} />
          {ALL_CATEGORIES.filter(c => c.isReady).map(cat => (
            <CurioChip
              key={cat.id}
              label={cat.displayName}
              isSelected={selectedCategoryId === cat.id}
              onClick={() => setSelectedCategoryId(cat.id)}
              color={cat.accent}
            />
          ))}
        </div>
      </div>

      {/* Topic list */}
      <div className="px-4 pt-4">
        {loading ? (
          <div className="flex items-center justify-center py-12">
            <div className="w-8 h-8 border-4 border-t-transparent rounded-full animate-spin"
              style={{ borderColor: '#FF8FA3', borderTopColor: 'transparent' }} />
          </div>
        ) : filtered.length === 0 ? (
          <div className="text-center py-12">
            <MaterialIcon name="search_off" size={48} style={{ color: isDark ? 'rgba(255,255,255,0.3)' : 'rgba(59,10,23,0.3)' }} />
            <p className="mt-3 text-sm" style={{ color: isDark ? 'rgba(255,255,255,0.4)' : 'rgba(59,10,23,0.4)' }}>
              No topics found
            </p>
          </div>
        ) : (
          <div className="space-y-2 pb-8">
            {filtered.map(({ topic, category }) => (
              <button
                key={`${category.id}-${topic.id}`}
                onClick={() => navigate(`/reveal/${category.id.toLowerCase()}/${topic.id}`)}
                className="w-full text-left flex items-center gap-3 p-3 rounded-xl transition-all active:scale-[0.98]"
                style={{
                  background: isDark ? 'rgba(255,255,255,0.03)' : 'rgba(59,10,23,0.015)',
                  border: `1px solid ${isDark ? 'rgba(255,255,255,0.05)' : 'rgba(59,10,23,0.04)'}`,
                }}>
                <div className="w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0"
                  style={{ background: `${category.accent}18` }}>
                  <MaterialIcon name={category.iconGlyph} size={20} style={{ color: category.accent }} />
                </div>
                <div className="flex-1 min-w-0">
                  <div className="text-sm font-semibold truncate" style={{ color: getTextColor(isDark) }}>
                    {topic.name}
                  </div>
                  <div className="text-xs truncate mt-0.5" style={{ color: isDark ? 'rgba(255,255,255,0.4)' : 'rgba(59,10,23,0.4)' }}>
                    {category.displayName} · {topic.subtype}
                  </div>
                </div>
                <MaterialIcon name="chevron_right" size={18}
                  style={{ color: isDark ? 'rgba(255,255,255,0.3)' : 'rgba(59,10,23,0.3)' }} />
              </button>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default TopicBrowserScreen;
