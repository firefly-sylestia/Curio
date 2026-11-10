// Curio Web App - Home Screen
// Editorial discovery home with daily quest and recent entries

import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTheme, getBackgroundColor, getTextColor } from '../theme/ThemeContext';
import { ALL_CATEGORIES } from '../data/categories';
import { getQuestSystem } from '../data/QuestSystem';
import { MaterialIcon } from '../components/SharedComponents';
import { captureRepository } from '../db/database';
import { ScreenEntrance, usePressable } from '../animations';

const HomeScreen: React.FC = () => {
  const navigate = useNavigate();
  const { isDark, isAmoled } = useTheme();
  const [questSystem] = useState(() => getQuestSystem());
  const [stats, setStats] = useState({ streak: 0, saved: 0, topics: '6,480+' });
  const [recents, setRecents] = useState<Array<{ id: string; name: string; categoryId: string; subtype: string; daysAgo: number }>>([]);
  const [greeting, setGreeting] = useState('');
  const [displayName] = useState(() => localStorage.getItem('curio-display-name') || 'Explorer');

  useEffect(() => {
    const h = new Date().getHours();
    setGreeting(h < 12 ? 'Good morning' : h < 17 ? 'Good afternoon' : 'Good evening');
  }, []);

  useEffect(() => {
    const load = async () => {
      try {
        const entries = await captureRepository.getAll();
        const recent = entries.reverse().slice(0, 5).map(e => ({
          id: e.id, name: e.topicName, categoryId: e.categoryId,
          subtype: e.topicSubtype, daysAgo: Math.floor((Date.now() - e.capturedAtMillis) / 86400000),
        }));
        setRecents(recent);
        setStats(s => ({ ...s, saved: entries.length }));
      } catch {}
    };
    const qs = questSystem.getState();
    setStats({ streak: qs.bestStreak || 0, saved: 0, topics: '6,480+' });
    load();
  }, [questSystem]);

  const handleShuffle = () => {
    const cats = ALL_CATEGORIES.filter(c => c.isReady);
    const pick = cats[Math.floor(Math.random() * cats.length)];
    navigate(`/spin/${pick.id.toLowerCase()}`);
  };

  const heroAccent = '#DA1E37'; // Curio crimson
  const heroFill = isDark || isAmoled ? '#161A1D' : '#111820';

  return (
    <div className="min-h-screen pb-24 relative" style={{ backgroundColor: getBackgroundColor(isDark, isAmoled) }}>
      {/* Editorial home header, sharing Explore's visual language */}
      <div className="px-4 pt-5 sm:px-6 sm:pt-7 max-w-5xl mx-auto">
        <header className="relative overflow-hidden rounded-[24px] p-5 sm:p-7 text-white"
          style={{ background: 'linear-gradient(125deg, #111820 0%, #202831 58%, #38151D 100%)', boxShadow: '0 20px 48px -30px rgba(0,0,0,.65)' }}>
          <div className="absolute -right-7 -top-9 w-48 h-48 rounded-full pointer-events-none"
            style={{ background: 'radial-gradient(circle, rgba(218,30,55,.24), transparent 68%)' }} />
          <div className="absolute right-5 top-8 opacity-[.10] rotate-[-12deg] pointer-events-none" aria-hidden="true">
            <MaterialIcon name="auto_awesome" size={116} />
          </div>
          <div className="relative z-10">
            <div className="flex items-center justify-between mb-8">
              <div className="flex items-center gap-2 text-[10px] font-bold tracking-[.22em] uppercase text-white/55">
                <span className="w-1.5 h-1.5 rounded-full" style={{ background: heroAccent }} />
                Curio / Your space
              </div>
              <span className="text-[10px] tracking-widest uppercase text-white/45">01 / Home</span>
            </div>
            <p className="text-sm font-semibold text-white/65">{greeting || 'Welcome back'}</p>
            <h1 className="mt-1 text-[38px] sm:text-[46px] leading-[1.05] font-extrabold tracking-tight"
              style={{ fontFamily: 'Geom, Inter, sans-serif' }}>
              {displayName}<span style={{ color: '#F05265' }}>.</span>
            </h1>
            <p className="mt-3 max-w-md text-sm leading-relaxed text-white/60">
              A little curiosity goes a long way. Pick up where you left off, or find something new.
            </p>
            <div className="mt-7 flex items-end gap-6">
              <div>
                <div className="text-2xl font-bold tabular-nums">{stats.streak}</div>
                <div className="mt-0.5 text-[10px] uppercase tracking-[.16em] text-white/45">Streak</div>
              </div>
              <div className="h-8 w-px bg-white/15" />
              <div>
                <div className="text-2xl font-bold tabular-nums">{stats.saved}</div>
                <div className="mt-0.5 text-[10px] uppercase tracking-[.16em] text-white/45">Saved</div>
              </div>
              <div className="h-8 w-px bg-white/15" />
              <div>
                <div className="text-2xl font-bold tabular-nums">{stats.topics}</div>
                <div className="mt-0.5 text-[10px] uppercase tracking-[.16em] text-white/45">Topics</div>
              </div>
            </div>
          </div>
        </header>
      </div>

      <ScreenEntrance>
        {/* ── 2. Quest Block ─────────────────────────────────────────────── */}
        <div className="px-4 pt-6">
          <button onClick={handleShuffle}
            className="w-full flex items-center gap-3.5 p-3 rounded-[24px] active:scale-[0.98] transition-transform"
            style={{
              background: 'transparent',
            }}>
            {/* Quest icon */}
            <div className="w-[46px] h-[46px] rounded-full flex items-center justify-center flex-shrink-0"
              style={{ background: `${heroAccent}18` }}>
              <MaterialIcon name="pets" size={22} style={{ color: heroAccent }} />
            </div>
            <div className="flex-1 text-left min-w-0">
              <p className="text-[10px] font-extrabold tracking-[1.6px] uppercase mb-1"
                style={{ color: isDark ? '#F06A7B' : '#A4161A' }}>Today's Quest</p>
              <p className="text-lg font-extrabold truncate" style={{ color: getTextColor(isDark) }}>Shuffle the deck</p>
              <p className="text-xs truncate" style={{ color: isDark ? 'rgba(245,243,244,0.58)' : 'rgba(23,27,38,0.58)' }}>
                A fresh mix of ideas, picked for you
              </p>
            </div>
            <div className="w-[54px] h-[54px] rounded-full flex items-center justify-center flex-shrink-0"
              style={{ background: heroAccent }}>
              <MaterialIcon name="casino" size={25} style={{ color: '#fff' }} />
            </div>
          </button>
        </div>

        {/* ── 3. Recents ────────────────────────────────────────────────── */}
        <div className="px-4 mt-6">
          <div className="flex items-center justify-between mb-3">
            <h3 className="text-base font-semibold" style={{ color: getTextColor(isDark) }}>Recents</h3>
          </div>

          {recents.length === 0 ? (
            <div className="text-center py-10 rounded-2xl"
              style={{ background: isDark ? 'rgba(255,255,255,0.035)' : 'rgba(23,27,38,0.025)' }}>
              <MaterialIcon name="auto_awesome" size={36} style={{ color: heroAccent, opacity: 0.6 }} />
              <p className="mt-3 text-sm font-medium" style={{ color: getTextColor(isDark) }}>
                Your journey starts here
              </p>
              <p className="mt-1 text-xs" style={{ color: isDark ? 'rgba(245,243,244,0.48)' : 'rgba(23,27,38,0.48)' }}>
                Shuffle the deck to discover your first topic
              </p>
            </div>
          ) : (
            <div className="space-y-2">
              {recents.map(e => {
                const cat = ALL_CATEGORIES.find(c => c.id === e.categoryId);
                return (
                  <RecentEntry key={e.id} entry={e} cat={cat} isDark={isDark}
                    onClick={() => navigate(`/detail/${e.id}`)} />
                );
              })}
            </div>
          )}
        </div>
      </ScreenEntrance>

    </div>
  );
};

const RecentEntry: React.FC<{
  entry: { id: string; name: string; categoryId: string; subtype: string; daysAgo: number };
  cat: any; isDark: boolean; onClick: () => void;
}> = ({ entry, cat, isDark, onClick }) => {
  const { handlers, pressStyle } = usePressable();
  return (
    <button onClick={onClick} {...handlers}
      className="w-full text-left flex items-center gap-3 p-3 rounded-xl"
      style={{
        background: isDark ? 'rgba(255,255,255,0.03)' : 'rgba(59,10,23,0.015)',
        border: `1px solid ${isDark ? 'rgba(255,255,255,0.04)' : 'rgba(59,10,23,0.03)'}`,
        ...pressStyle,
      }}>
      <MaterialIcon name={cat?.iconGlyph || 'edit_note'} size={22} style={{ color: isDark ? '#AEB8C6' : '#586579' }} />
      <div className="flex-1 min-w-0">
        <p className="text-sm font-semibold truncate" style={{ color: getTextColor(isDark) }}>{entry.name}</p>
        <p className="text-xs" style={{ color: isDark ? 'rgba(255,255,255,0.4)' : 'rgba(59,10,23,0.4)' }}>
          {cat?.displayName} · {entry.subtype} · {entry.daysAgo === 0 ? 'today' : entry.daysAgo === 1 ? 'yesterday' : `${entry.daysAgo}d ago`}
        </p>
      </div>
      <MaterialIcon name="chevron_right" size={16} style={{ color: isDark ? 'rgba(255,255,255,0.3)' : 'rgba(59,10,23,0.3)' }} />
    </button>
  );
};

export default HomeScreen;
