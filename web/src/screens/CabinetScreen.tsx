// Curio Web App - Cabinet Screen
// Matches Android: torn rose hero, sticky filter chips, search, grouped entries

import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTheme, getBackgroundColor, getTextColor } from '../theme/ThemeContext';
import { ALL_CATEGORIES } from '../data/categories';
import { ScreenEntrance, usePressable } from '../animations';
import { CurioChip, CurioEmptyState, CurioSectionHeader, CurioWatermarkBackdrop, MaterialIcon } from '../components/SharedComponents';
import { captureRepository } from '../db/database';
import { TornHero, CABINET_HERO_SYMBOLS } from '../components/TornHero';

const CABINET_HERO_HEIGHT = 180;
const CABINET_TEAR_SEED = 0xCAB1E; // Android's CABINET_TEAR_SEED
const ROSE_WOOD = '#C46B7C';

interface CurioCapture { id: string; title: string; content: string; categoryId: string; topicName?: string; format?: string; createdAt: string; }
const getCategoryByIdSafe = (id: string) => ALL_CATEGORIES.find(c => c.id === id) || null;

const EntryCard: React.FC<{ entry: CurioCapture; onClick: () => void }> = ({ entry, onClick }) => {
  const { isDark } = useTheme();
  const { handlers, pressStyle } = usePressable();
  const category = getCategoryByIdSafe(entry.categoryId);
  return (
    <button onClick={onClick} {...handlers} className="w-full text-left rounded-2xl overflow-hidden"
      style={{ background: isDark ? 'rgba(255,255,255,0.05)' : 'white', boxShadow: '0 2px 8px rgba(0,0,0,0.08)', ...pressStyle }}>
      <div className="h-1" style={{ background: category?.accent || '#3B0A17' }} />
      <div className="p-4">
        <div className="flex items-start gap-3">
          <div className="w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0" style={{ background: `${category?.accent || '#3B0A17'}15` }}>
            <MaterialIcon name={category?.iconGlyph || 'edit_note'} size={20} style={{ color: category?.accent }} />
          </div>
          <div className="flex-1 min-w-0">
            <h4 className="text-sm font-semibold truncate" style={{ color: getTextColor(isDark) }}>{entry.title || 'Untitled'}</h4>
            <p className="text-xs mt-0.5 line-clamp-2" style={{ color: isDark ? 'rgba(255,255,255,0.5)' : 'rgba(59,10,23,0.5)' }}>{entry.content || entry.topicName || 'No content'}</p>
          </div>
        </div>
        <div className="mt-3 flex items-center gap-2">
          <span className="text-xs px-2 py-0.5 rounded-full" style={{ background: `${category?.accent || '#3B0A17'}15`, color: category?.accent }}>{entry.format || 'Note'}</span>
          <span className="text-xs" style={{ color: isDark ? 'rgba(255,255,255,0.4)' : 'rgba(59,10,23,0.4)' }}>{new Date(entry.createdAt).toLocaleDateString()}</span>
        </div>
      </div>
    </button>
  );
};

const CurioSearchField: React.FC<{ value: string; onChange: (v: string) => void; placeholder?: string }> = ({ value, onChange, placeholder }) => {
  const { isDark } = useTheme();
  return (
    <div className="relative">
      <div className="absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" style={{ color: isDark ? 'rgba(255,255,255,0.4)' : 'rgba(59,10,23,0.4)' }}>
        <MaterialIcon name="search" size={18} />
      </div>
      <input type="text" value={value} onChange={e => onChange(e.target.value)} placeholder={placeholder}
        className="w-full pl-10 pr-4 py-3 rounded-xl text-sm outline-none"
        style={{ background: isDark ? 'rgba(255,255,255,0.05)' : 'rgba(59,10,23,0.03)', color: getTextColor(isDark),
          border: `1px solid ${isDark ? 'rgba(255,255,255,0.06)' : 'rgba(59,10,23,0.06)'}` }} />
      {value && (
        <button onClick={() => onChange('')} className="absolute right-3 top-1/2 -translate-y-1/2">
          <MaterialIcon name="close" size={16} style={{ color: isDark ? 'rgba(255,255,255,0.4)' : 'rgba(59,10,23,0.4)' }} />
        </button>
      )}
    </div>
  );
};

export const CabinetScreen: React.FC = () => {
  const navigate = useNavigate();
  const { isDark, isAmoled } = useTheme();
  const [entries, setEntries] = useState<CurioCapture[]>([]);
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    (async () => {
      try {
        const all = await captureRepository.getAll();
        setEntries(all.map(e => ({
          id: e.id, title: e.title || e.topicName || 'Untitled', content: e.topicTeaser || '',
          categoryId: e.categoryId, topicName: e.topicName, format: e.format,
          createdAt: new Date(e.capturedAtMillis).toISOString(),
        })).reverse());
      } catch { setEntries([]); }
      finally { setIsLoading(false); }
    })();
  }, []);

  const filtered = entries.filter(e => {
    const mCat = !selectedCategory || e.categoryId === selectedCategory;
    const mSearch = !searchQuery ||
      e.title?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      e.content?.toLowerCase().includes(searchQuery.toLowerCase());
    return mCat && mSearch;
  });

  const grouped = filtered.reduce((acc, e) => {
    const cid = e.categoryId || 'uncategorized';
    if (!acc[cid]) acc[cid] = [];
    acc[cid].push(e);
    return acc;
  }, {} as Record<string, CurioCapture[]>);

  // Category-matched hero fill
  const activeCat = selectedCategory ? getCategoryByIdSafe(selectedCategory) : null;
  const heroFill = activeCat?.accent || ROSE_WOOD;

  return (
    <div className="min-h-screen pb-24 relative" style={{ backgroundColor: getBackgroundColor(isDark, isAmoled) }}>
      <CurioWatermarkBackdrop topClearance={CABINET_HERO_HEIGHT + 30} alphaScale={0.45} />

      {/* ── Torn Hero Banner ──────────────────────────────────────── */}
      <TornHero
        height={CABINET_HERO_HEIGHT}
        fill={heroFill}
        ink="#fff"
        tearSeed={CABINET_TEAR_SEED}
        bold={true}
        symbols={CABINET_HERO_SYMBOLS}
        isDark={isDark}
      >
        <div className="flex flex-col h-full px-5 pt-[68px] pb-[18px] justify-end">
          <h1 className="text-xl font-extrabold text-white text-center" style={{ fontFamily: 'Geom, Inter, sans-serif' }}>
            Cabinet
          </h1>
          <p className="text-xs text-white/70 text-center mt-0.5">{entries.length} entries saved</p>
        </div>
      </TornHero>

      <ScreenEntrance>
        <div className="relative z-10">
          {/* Sticky search + filter bar */}
          <div className="sticky top-0 z-20 px-4 pt-4 pb-3"
            style={{ background: getBackgroundColor(isDark, isAmoled) }}>
            <CurioSearchField value={searchQuery} onChange={setSearchQuery} placeholder="Search entries…" />
            <div className="flex gap-2 overflow-x-auto pb-1 mt-3">
              <CurioChip label="All" isSelected={selectedCategory === null} onClick={() => setSelectedCategory(null)} />
              {ALL_CATEGORIES.filter(c => c.isReady).map(cat => (
                <CurioChip key={cat.id} label={cat.displayName} isSelected={selectedCategory === cat.id}
                  onClick={() => setSelectedCategory(cat.id)} color={cat.accent} />
              ))}
            </div>
          </div>

          {/* Content */}
          <div className="px-4">
            {isLoading ? (
              <div className="flex items-center justify-center py-12">
                <div className="w-8 h-8 border-4 border-t-transparent rounded-full animate-spin"
                  style={{ borderColor: '#FF8FA3', borderTopColor: 'transparent' }} />
              </div>
            ) : filtered.length === 0 ? (
              <CurioEmptyState icon="inventory_2" title={searchQuery ? 'No results' : 'No entries yet'}
                description={searchQuery ? 'Try a different search term' : 'Start exploring to build your cabinet'}
                action={!searchQuery ? 'Start exploring' : undefined}
                onAction={!searchQuery ? () => navigate('/spin') : undefined} />
            ) : (
              <div className="space-y-6 pb-8">
                {Object.entries(grouped).map(([catId, catEntries]) => {
                  const cat = getCategoryByIdSafe(catId);
                  return (
                    <div key={catId}>
                      <CurioSectionHeader title={cat?.displayName || 'Uncategorized'} action={`${catEntries.length} entries`} />
                      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                        {catEntries.map(e => <EntryCard key={e.id} entry={e} onClick={() => navigate(`/detail/${e.id}`)} />)}
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </div>
      </ScreenEntrance>
    </div>
  );
};

export default CabinetScreen;
