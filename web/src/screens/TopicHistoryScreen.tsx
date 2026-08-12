// Curio Web App - Topic History Screen
// Mirrors Android Topic History: torn hero, day-grouped rows, session time, category/format glyphs

import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTheme, getBackgroundColor, getTextColor } from '../theme/ThemeContext';
import { ALL_CATEGORIES } from '../data/categories';
import { captureRepository } from '../db/database';
import { CurioEmptyState, CurioSectionHeader, CurioWatermarkBackdrop, MaterialIcon } from '../components/SharedComponents';
import { TornHero, BROWSER_HERO_SYMBOLS } from '../components/TornHero';
import { ScreenEntrance, usePressable } from '../animations';
import type { CaptureEntity } from '../types';

const HISTORY_HERO_HEIGHT = 180;
const HISTORY_TEAR_SEED = 0xAB1E5;
const ROSE_WOOD = '#C46B7C';

const formatDay = (millis: number) => {
  const date = new Date(millis);
  const today = new Date();
  const start = new Date(today.getFullYear(), today.getMonth(), today.getDate()).getTime();
  const diff = Math.floor((start - new Date(date.getFullYear(), date.getMonth(), date.getDate()).getTime()) / 86400000);
  if (diff === 0) return 'Today';
  if (diff === 1) return 'Yesterday';
  return date.toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: today.getFullYear() === date.getFullYear() ? undefined : 'numeric' });
};

const formatSessionShort = (millis?: number) => {
  if (!millis || millis <= 0) return null;
  const seconds = Math.max(1, Math.round(millis / 1000));
  if (seconds < 60) return `${seconds}s`;
  const minutes = Math.round(seconds / 60);
  if (minutes < 60) return `${minutes}m`;
  const hours = Math.floor(minutes / 60);
  const rem = minutes % 60;
  return rem ? `${hours}h ${rem}m` : `${hours}h`;
};

const formatGlyph = (format: string) => {
  switch (format) {
    case 'SoundBite': return 'mic';
    case 'ReelNotes': return 'movie';
    case 'Marginalia': return 'edit_note';
    case 'GalleryWall': return 'image';
    case 'FieldNotes': return 'description';
    default: return 'note_stack';
  }
};

const HistoryRow: React.FC<{ entry: CaptureEntity; onClick: () => void }> = ({ entry, onClick }) => {
  const { isDark } = useTheme();
  const { handlers, pressStyle } = usePressable(0.98);
  const category = ALL_CATEGORIES.find(c => c.id === entry.categoryId);
  const session = formatSessionShort(entry.sessionTimeMillis);
  const muted = isDark ? 'rgba(255,255,255,0.46)' : 'rgba(59,10,23,0.46)';

  return (
    <button onClick={onClick} {...handlers} className="w-full flex items-center gap-3 p-3 rounded-2xl text-left"
      style={{
        background: isDark ? 'rgba(255,255,255,0.04)' : 'rgba(59,10,23,0.025)',
        border: `1px solid ${isDark ? 'rgba(255,255,255,0.06)' : 'rgba(59,10,23,0.045)'}`,
        ...pressStyle,
      }}>
      <div className="w-11 h-11 rounded-2xl flex items-center justify-center flex-shrink-0" style={{ background: `${category?.accent || ROSE_WOOD}1C` }}>
        <MaterialIcon name={category?.iconGlyph || 'travel_explore'} size={22} style={{ color: category?.accent || ROSE_WOOD }} />
      </div>
      <div className="flex-1 min-w-0">
        <p className="text-sm font-extrabold truncate" style={{ color: getTextColor(isDark), fontFamily: 'Geom, Inter, sans-serif' }}>{entry.topicName}</p>
        <p className="text-xs truncate mt-0.5" style={{ color: muted }}>{category?.displayName || 'Curio'} · {entry.topicSubtype}</p>
      </div>
      <div className="flex flex-col items-end gap-1 flex-shrink-0" style={{ color: muted }}>
        <div className="flex items-center gap-1.5">
          {session && <><MaterialIcon name="timer" size={13} /><span className="text-[11px] font-semibold">{session}</span></>}
          <MaterialIcon name={formatGlyph(entry.format)} size={15} />
        </div>
        <span className="text-[10px]">{new Date(entry.capturedAtMillis).toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' })}</span>
      </div>
    </button>
  );
};

export const TopicHistoryScreen: React.FC = () => {
  const navigate = useNavigate();
  const { isDark, isAmoled } = useTheme();
  const [entries, setEntries] = useState<CaptureEntity[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    captureRepository.getAll()
      .then(all => setEntries([...all].sort((a, b) => b.capturedAtMillis - a.capturedAtMillis)))
      .finally(() => setLoading(false));
  }, []);

  const grouped = useMemo(() => entries.reduce((acc, entry) => {
    const day = formatDay(entry.capturedAtMillis);
    acc[day] = acc[day] || [];
    acc[day].push(entry);
    return acc;
  }, {} as Record<string, CaptureEntity[]>), [entries]);

  return (
    <div className="min-h-screen pb-24 relative" style={{ backgroundColor: getBackgroundColor(isDark, isAmoled) }}>
      <CurioWatermarkBackdrop topClearance={HISTORY_HERO_HEIGHT + 30} alphaScale={0.45} />
      <TornHero height={HISTORY_HERO_HEIGHT} fill={ROSE_WOOD} ink="#fff" tearSeed={HISTORY_TEAR_SEED} bold symbols={BROWSER_HERO_SYMBOLS} isDark={isDark}>
        <div className="flex flex-col h-full px-5 pt-[68px] pb-[18px] justify-end">
          <button onClick={() => navigate(-1)} className="absolute top-0 left-5 w-10 h-10 rounded-full flex items-center justify-center"
            style={{ background: 'rgba(255,255,255,0.18)', border: '1px solid rgba(255,255,255,0.25)', marginTop: 'env(safe-area-inset-top, 12px)' }}>
            <MaterialIcon name="arrow_back" size={20} style={{ color: '#fff' }} />
          </button>
          <h1 className="text-xl font-extrabold text-white text-center" style={{ fontFamily: 'Geom, Inter, sans-serif' }}>Topic History</h1>
          <p className="text-xs text-white/70 text-center mt-0.5">{entries.length} explored topics</p>
        </div>
      </TornHero>

      <ScreenEntrance>
        <div className="px-4 pt-5 space-y-6 relative z-10">
          {loading ? (
            <div className="flex justify-center py-12"><div className="w-8 h-8 border-4 border-t-transparent rounded-full animate-spin" style={{ borderColor: ROSE_WOOD, borderTopColor: 'transparent' }} /></div>
          ) : entries.length === 0 ? (
            <CurioEmptyState icon="history" title="No history yet" description="Spin, explore, and save a topic to start your trail." action="Start exploring" onAction={() => navigate('/spin')} />
          ) : Object.entries(grouped).map(([day, dayEntries]) => (
            <section key={day}>
              <CurioSectionHeader title={day} action={`${dayEntries.length} ${dayEntries.length === 1 ? 'entry' : 'entries'}`} />
              <div className="space-y-2">{dayEntries.map(entry => <HistoryRow key={entry.id} entry={entry} onClick={() => navigate(`/detail/${entry.id}`)} />)}</div>
            </section>
          ))}
        </div>
      </ScreenEntrance>
    </div>
  );
};

export default TopicHistoryScreen;
