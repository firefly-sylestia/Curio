// Curio Web App - Home Screen
// Android-matching: seeded torn hero, sticky menu top-left + profile top-right, quest block, recents

import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTheme, getBackgroundColor, getTextColor } from '../theme/ThemeContext';
import { ALL_CATEGORIES } from '../data/categories';
import { getQuestSystem } from '../data/QuestSystem';
import { MaterialIcon, CurioWatermarkBackdrop } from '../components/SharedComponents';
import { captureRepository } from '../db/database';
import { TornHero, HOME_HERO_SYMBOLS } from '../components/TornHero';
import { ScreenEntrance, usePressable } from '../animations';
import { MenuDrawer } from '../components/MenuDrawer';

const HOME_HERO_HEIGHT = 300;
const HOME_TEAR_SEED = 0xC0FEE; // Fixed seed — matches Android

const HomeScreen: React.FC = () => {
  const navigate = useNavigate();
  const { isDark, isAmoled } = useTheme();
  const [questSystem] = useState(() => getQuestSystem());
  const [stats, setStats] = useState({ streak: 0, saved: 0, topics: '6,480+' });
  const [recents, setRecents] = useState<Array<{ id: string; name: string; categoryId: string; subtype: string; daysAgo: number }>>([]);
  const [greeting, setGreeting] = useState('');
  const [displayName] = useState(() => localStorage.getItem('curio-display-name') || 'Explorer');
  const [menuOpen, setMenuOpen] = useState(false);

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

  const heroAccent = '#C46B7C'; // Rose-wood
  const heroInk = '#fff';

  return (
    <div className="min-h-screen pb-24 relative" style={{ backgroundColor: getBackgroundColor(isDark, isAmoled) }}>
      <CurioWatermarkBackdrop topClearance={HOME_HERO_HEIGHT + 30} />

      {/* ── 1. Torn Hero Banner ────────────────────────────────────────── */}
      <TornHero
        height={HOME_HERO_HEIGHT}
        fill={heroAccent}
        ink={heroInk}
        tearSeed={HOME_TEAR_SEED}
        bold={true}
        symbols={HOME_HERO_SYMBOLS}
        isDark={isDark}
      >
        <div className="flex flex-col h-full px-5 pt-[68px] pb-[18px]">
          {/* Greeting + Name */}
          <p className="text-white/90 text-[20px] font-extrabold leading-tight"
            style={{ fontFamily: 'Geom, Inter, sans-serif' }}>{greeting}</p>
          <h1 className="text-[36px] font-extrabold text-white leading-[44px] mt-1"
            style={{ fontFamily: 'Geom, Inter, sans-serif' }}>{displayName}</h1>

          <div className="flex-1" />

          {/* Streak · Cabinet · Topics stat bar */}
          <div className="rounded-[20px] border border-white/25" style={{
            background: 'linear-gradient(180deg, rgba(196,107,124,0.12) 0%, rgba(196,107,124,0.02) 100%)',
          }}>
            <div className="flex items-center px-1.5 py-2.5">
              <div className="flex flex-col items-center flex-1 gap-0.5">
                <MaterialIcon name="local_fire_department" size={18} style={{ color: 'rgba(255,255,255,0.9)' }} />
                <span className="text-sm font-extrabold text-white">{stats.streak}</span>
                <span className="text-[10px] text-white/80">Streak</span>
              </div>
              <div className="w-px h-[34px] bg-white/20" />
              <div className="flex flex-col items-center flex-1 gap-0.5">
                <MaterialIcon name="inventory_2" size={18} style={{ color: 'rgba(255,255,255,0.9)' }} />
                <span className="text-sm font-extrabold text-white">{stats.saved}</span>
                <span className="text-[10px] text-white/80">Cabinet</span>
              </div>
              <div className="w-px h-[34px] bg-white/20" />
              <div className="flex flex-col items-center flex-1 gap-0.5">
                <MaterialIcon name="auto_awesome" size={18} style={{ color: 'rgba(255,255,255,0.9)' }} />
                <span className="text-sm font-extrabold text-white">{stats.topics}</span>
                <span className="text-[10px] text-white/80">Topics</span>
              </div>
            </div>
          </div>
        </div>
      </TornHero>

      <ScreenEntrance>
        {/* ── 2. Quest Block ─────────────────────────────────────────────── */}
        <div className="px-4 pt-6">
          <button onClick={handleShuffle}
            className="w-full flex items-center gap-3.5 p-3 rounded-[24px] active:scale-[0.98] transition-transform"
            style={{
              background: 'transparent',
            }}>
            {/* Pet bed placeholder */}
            <div className="w-[46px] h-[46px] rounded-full flex items-center justify-center flex-shrink-0"
              style={{ background: `${heroAccent}18` }}>
              <MaterialIcon name="pets" size={22} style={{ color: heroAccent }} />
            </div>
            <div className="flex-1 text-left min-w-0">
              <p className="text-[10px] font-extrabold tracking-[1.6px] uppercase mb-1"
                style={{ color: '#8B5E6B' }}>Today's Quest</p>
              <p className="text-lg font-extrabold truncate" style={{ color: getTextColor(isDark) }}>Shuffle the deck</p>
              <p className="text-xs truncate" style={{ color: isDark ? 'rgba(255,255,255,0.45)' : 'rgba(59,10,23,0.45)' }}>
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
            {recents.length > 0 && (
              <button onClick={() => navigate('/cabinet')}
                className="flex items-center gap-1 px-2.5 py-1.5 rounded-full text-xs font-bold"
                style={{ background: isDark ? 'rgba(255,255,255,0.06)' : 'rgba(59,10,23,0.04)', color: getTextColor(isDark) }}>
                View all
                <MaterialIcon name="chevron_right" size={14} />
              </button>
            )}
          </div>

          {recents.length === 0 ? (
            <div className="text-center py-10 rounded-2xl"
              style={{ background: isDark ? 'rgba(255,255,255,0.03)' : 'rgba(59,10,23,0.015)' }}>
              <MaterialIcon name="auto_awesome" size={36} style={{ color: heroAccent, opacity: 0.6 }} />
              <p className="mt-3 text-sm font-medium" style={{ color: getTextColor(isDark) }}>
                Your journey starts here
              </p>
              <p className="mt-1 text-xs" style={{ color: isDark ? 'rgba(255,255,255,0.4)' : 'rgba(59,10,23,0.4)' }}>
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

      {/* ── Sticky top bar — menu (top-left) + profile (top-right) ────── */}
      <div className="fixed top-0 left-0 right-0 z-50 flex justify-between px-4 items-center"
        style={{ paddingTop: 'env(safe-area-inset-top, 12px)' }}>
        <button onClick={() => setMenuOpen(true)}
          className="w-[42px] h-[42px] rounded-full flex items-center justify-center"
          style={{
            background: `${heroAccent}CC`,
            border: `1px solid rgba(255,255,255,0.35)`,
          }}>
          <MaterialIcon name="menu" size={22} style={{ color: '#fff' }} />
        </button>
        <button onClick={() => navigate('/profile')}
          className="w-[42px] h-[42px] rounded-full flex items-center justify-center"
          style={{
            background: `${heroAccent}CC`,
            border: `1px solid rgba(255,255,255,0.35)`,
          }}>
          <MaterialIcon name="person" size={22} style={{ color: '#fff' }} />
        </button>
      </div>

      {/* Menu Drawer */}
      <MenuDrawer isOpen={menuOpen} onClose={() => setMenuOpen(false)} />
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
      <MaterialIcon name={cat?.iconGlyph || 'edit_note'} size={22} style={{ color: cat?.accent || '#999' }} />
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
