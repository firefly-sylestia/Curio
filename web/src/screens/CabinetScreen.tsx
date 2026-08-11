// Curio Web App - Cabinet Screen (Premium Version)
// Matches Android app's premium design with smooth filtering and grid

import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTheme, getBackgroundColor, getTextColor } from '../theme/ThemeContext';
import { ALL_CATEGORIES } from '../data/categories';
import { 
  CurioChip, 
  CurioEmptyState, 
  CurioSectionHeader
} from '../components/SharedComponents';


// Entry type
interface CurioCapture {
  id: string;
  title: string;
  content: string;
  categoryId: string;
  topicName?: string;
  format?: string;
  createdAt: string;
}

// Helper to safely get category by string ID
const getCategoryByIdSafe = (id: string) => {
  const category = ALL_CATEGORIES.find(c => c.id === id);
  return category || null;
};

// ─── Entry Card Component ─────────────────────────────────────────────
const EntryCard: React.FC<{
  entry: CurioCapture;
  onClick: () => void;
}> = ({ entry, onClick }) => {
  const { isDark } = useTheme();
  const [isPressed, setIsPressed] = useState(false);
  const category = getCategoryByIdSafe(entry.categoryId);

  return (
    <button
      onClick={onClick}
      onMouseDown={() => setIsPressed(true)}
      onMouseUp={() => setIsPressed(false)}
      onMouseLeave={() => setIsPressed(false)}
      className="w-full text-left rounded-2xl overflow-hidden transition-all duration-200"
      style={{
        background: isDark ? 'rgba(255,255,255,0.05)' : 'white',
        transform: isPressed ? 'scale(0.98)' : 'scale(1)',
        boxShadow: isPressed 
          ? `0 4px 12px ${category?.accent || '#3B0A17'}33`
          : '0 2px 8px rgba(0,0,0,0.08)',
      }}
    >
      {/* Category accent bar */}
      <div
        className="h-1"
        style={{ background: category?.accent || '#3B0A17' }}
      />
      
      {/* Content */}
      <div className="p-4">
        <div className="flex items-start gap-3">
          <div
            className="w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0"
            style={{ background: `${category?.accent || '#3B0A17'}15` }}
          >
            <span className="material-symbols-outlined text-lg">{category?.iconGlyph || 'edit_note'}</span>
          </div>
          <div className="flex-1 min-w-0">
            <h4
              className="text-sm font-semibold truncate"
              style={{ color: getTextColor(isDark) }}
            >
              {entry.title || 'Untitled'}
            </h4>
            <p
              className="text-xs mt-0.5 line-clamp-2"
              style={{ color: isDark ? 'rgba(255,255,255,0.5)' : 'rgba(59,10,23,0.5)' }}
            >
              {entry.content || entry.topicName || 'No content'}
            </p>
          </div>
        </div>
        
        {/* Format badge */}
        <div className="mt-3 flex items-center gap-2">
          <span
            className="text-xs px-2 py-0.5 rounded-full"
            style={{
              background: `${category?.accent || '#3B0A17'}15`,
              color: category?.accent || '#3B0A17',
            }}
          >
            {entry.format || 'Note'}
          </span>
          <span
            className="text-xs"
            style={{ color: isDark ? 'rgba(255,255,255,0.4)' : 'rgba(59,10,23,0.4)' }}
          >
            {new Date(entry.createdAt).toLocaleDateString()}
          </span>
        </div>
      </div>
    </button>
  );
};

// ─── Main CabinetScreen Component ─────────────────────────────────────
export const CabinetScreen: React.FC = () => {
  const navigate = useNavigate();
  const { isDark, isAmoled } = useTheme();
  
  const [entries, setEntries] = useState<CurioCapture[]>([]);
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [isLoading, setIsLoading] = useState(true);

  // Load entries
  useEffect(() => {
    const loadEntries = async () => {
      try {
        // In a real app, this would load from IndexedDB
        // For now, use empty array
        setEntries([]);
      } catch (error) {
        console.error('Failed to load entries:', error);
      } finally {
        setIsLoading(false);
      }
    };
    loadEntries();
  }, []);

  // Filter entries
  const filteredEntries = entries.filter(entry => {
    const matchesCategory = !selectedCategory || entry.categoryId === selectedCategory;
    const matchesSearch = !searchQuery || 
      entry.title?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      entry.content?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      entry.topicName?.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesCategory && matchesSearch;
  });

  // Group by category
  const groupedEntries = filteredEntries.reduce((acc, entry) => {
    const categoryId = entry.categoryId || 'uncategorized';
    if (!acc[categoryId]) {
      acc[categoryId] = [];
    }
    acc[categoryId].push(entry);
    return acc;
  }, {} as Record<string, CurioCapture[]>);

  const handleEntryClick = (entry: CurioCapture) => {
    navigate(`/entry/${entry.id}`);
  };

  return (
    <div
      className="min-h-screen pb-24"
      style={{ backgroundColor: getBackgroundColor(isDark, isAmoled) }}
    >
      {/* Header */}
      <div className="sticky top-0 z-10 px-4 pt-6 pb-4" style={{ background: getBackgroundColor(isDark, isAmoled) }}>
        <div className="flex items-center justify-between mb-4">
          <h1
            className="text-2xl font-bold"
            style={{ color: getTextColor(isDark), fontFamily: 'Geom, sans-serif' }}
          >
            Cabinet
          </h1>
          <span
            className="text-sm"
            style={{ color: isDark ? 'rgba(255,255,255,0.5)' : 'rgba(59,10,23,0.5)' }}
          >
            {entries.length} entries
          </span>
        </div>
        
        {/* Search */}
        <div className="mb-4">
          <CurioSearchField
            value={searchQuery}
            onChange={setSearchQuery}
            placeholder="Search entries..."
          />
        </div>
        
        {/* Category filter */}
        <div className="flex gap-2 overflow-x-auto pb-2 -mx-4 px-4">
          <CurioChip
            label="All"
            isSelected={selectedCategory === null}
            onClick={() => setSelectedCategory(null)}
          />
          {ALL_CATEGORIES.filter(c => c.isReady).map((category) => (
            <CurioChip
              key={category.id}
              label={category.displayName}
              isSelected={selectedCategory === category.id}
              onClick={() => setSelectedCategory(category.id)}
              color={category.accent}
            />
          ))}
        </div>
      </div>

      {/* Content */}
      <div className="px-4">
        {isLoading ? (
          <div className="flex items-center justify-center py-12">
            <div
              className="animate-spin w-8 h-8 rounded-full"
              style={{
                border: `2px solid ${isDark ? 'rgba(255,255,255,0.1)' : 'rgba(59,10,23,0.1)'}`,
                borderTopColor: '#3B0A17',
              }}
            />
          </div>
        ) : filteredEntries.length === 0 ? (
          <CurioEmptyState
            icon="book_5"
            title={searchQuery ? 'No results' : 'No entries yet'}
            description={searchQuery ? 'Try a different search term' : 'Start exploring to build your cabinet'}
            action={!searchQuery ? 'Start exploring' : undefined}
            onAction={!searchQuery ? () => navigate('/spin') : undefined}
          />
        ) : (
          <div className="space-y-6">
            {Object.entries(groupedEntries).map(([categoryId, categoryEntries]) => {
              const category = getCategoryByIdSafe(categoryId);
              return (
                <div key={categoryId}>
                  <CurioSectionHeader
                    title={category?.displayName || 'Uncategorized'}
                    action={`${categoryEntries.length} entries`}
                  />
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    {categoryEntries.map((entry) => (
                      <EntryCard
                        key={entry.id}
                        entry={entry}
                        onClick={() => handleEntryClick(entry)}
                      />
                    ))}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
};

// ─── CurioSearchField Component ───────────────────────────────────────
// This should be in SharedComponents, but adding here for completeness
export const CurioSearchField: React.FC<{
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
}> = ({ value, onChange, placeholder }) => {
  const { isDark } = useTheme();

  return (
    <div
      className="relative"
    >
      <div
        className="absolute left-3 top-1/2 transform -translate-y-1/2"
        style={{ color: isDark ? 'rgba(255,255,255,0.4)' : 'rgba(59,10,23,0.4)' }}
      >
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <circle cx="11" cy="11" r="8" />
          <path d="m21 21-4.35-4.35" />
        </svg>
      </div>
      <input
        type="text"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        className="w-full pl-10 pr-4 py-3 rounded-xl text-sm transition-all duration-200"
        style={{
          background: isDark ? 'rgba(255,255,255,0.05)' : 'rgba(59,10,23,0.03)',
          color: getTextColor(isDark),
          border: `1px solid ${isDark ? 'rgba(255,255,255,0.1)' : 'rgba(59,10,23,0.1)'}`,
        }}
      />
      {value && (
        <button
          onClick={() => onChange('')}
          className="absolute right-3 top-1/2 transform -translate-y-1/2"
          style={{ color: isDark ? 'rgba(255,255,255,0.4)' : 'rgba(59,10,23,0.4)' }}
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M18 6 6 18" />
            <path d="m6 6 12 12" />
          </svg>
        </button>
      )}
    </div>
  );
};

export default CabinetScreen;
