// Curio Web App - Home Screen
// Matches Android: rose hero banner, quest block, recents feed

import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTheme, getBackgroundColor, getTextColor } from '../theme/ThemeContext';
import { useMenu } from '../App';
import { ALL_CATEGORIES } from '../data/categories';
import { getQuestSystem } from '../data/QuestSystem';
import { MaterialIcon, CurioWatermarkBackdrop } from '../components/SharedComponents';
import { captureRepository } from '../db/database';

const HomeScreen: React.FC = () => {
  const navigate = useNavigate();
  const { isDark, isAmoled } = useTheme();
  const { openMenu } = useMenu();
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
          id: e.id,
          name: e.topicName,
          categoryId: e.categoryId,
          subtype: e.topicSubtype,
          daysAgo: Math.floor((Date.now() - e.capturedAtMillis) / 86400000),
        }));
        setRecents(recent);
        setStats(s => ({ ...s, saved: entries.length }));
      } catch {}
    };
    const qs = questSystem.getState();
    setStats({ streak: qs.bestStreak || 0, saved: 0, topics: '6,480+' });
    load();
  }, [questSystem]);

  const handleShuffle = async () => {
    const cats = ALL_CATEGORIES.filter(c => c.isReady);
    const pick = cats[Math.floor(Math.random() * cats.length)];
    navigate(`/spin/${pick.id.toLowerCase()}`);
  };

  const heroAccent = '#C46B7C'; // Rose-wood like Android

  return (
    <div className="min-h-screen pb-24 relative" style={{ backgroundColor: getBackgroundColor(isDark, isAmoled) }}>
      <CurioWatermarkBackdrop topClearance={260} />
      {/* ── Rose Hero Banner ──────────────────────────────────────── */}
      <div className="relative w-full overflow-hidden"
        style={{
          background: `linear-gradient(180deg, ${heroAccent} 0%, ${heroAccent}DD 100%)`,
          minHeight: 260,
          paddingTop: 'env(safe-area-inset-top)',
        }}>
        {/* Watermark glyphs */}
        <div className="absolute inset-0 pointer-events-none opacity-[0.08]">
          <MaterialIcon name="casino" size={120} className="absolute" style={{ right: -20, top: 40, transform: 'rotate(12deg)' }} />
          <MaterialIcon name="auto_awesome" size={100} className="absolute" style={{ left: -10, top: 140, transform: 'rotate(-8deg)' }} />
          <MaterialIcon name="local_fire_department" size={90} className="absolute" style={{ right: 60, bottom: 20, transform: 'rotate(6deg)' }} />
        </div>

        {/* Content */}
        <div className="relative z-10 px-5 pt-14 pb-4 flex flex-col h-full">
          {/* Sticky pills row — menu opens drawer */}
          <div className="absolute top-3 right-4 flex gap-2 z-20">
            <button onClick={openMenu}
              className="w-10 h-10 rounded-full flex items-center justify-center"
              style={{ background: 'rgba(255,255,255,0.18)', border: '1px solid rgba(255,255,255,0.25)' }}>
              <MaterialIcon name="menu" size={20} style={{ color: '#fff' }} />
            </button>
          </div>

          {/* Greeting */}
          <p className="text-white/80 text-sm font-medium mb-1">{greeting}</p>
          <h1 className="text-3xl font-extrabold text-white mb-5" style={{ fontFamily: 'Geom, Inter, sans-serif' }}>
            {displayName}
          </h1>

          <div className="flex-1" />

          {/* Stat bar */}
          <div className="flex items-center rounded-2xl p-3"
            style={{ background: 'rgba(255,255,255,0.08)', border: '1px solid rgba(255,255,255,0.15)' }}>
            <Stat inline icon="local_fire_department" value={`${stats.streak}`} label="Streak" />
            <div className="w-px h-8 mx-3" style={{ background: 'rgba(255,255,255,0.2)' }} />
            <Stat inline icon="inventory_2" value={`${stats.saved}`} label="Cabinet" />
            <div className="w-px h-8 mx-3" style={{ background: 'rgba(255,255,255,0.2)' }} />
            <Stat inline icon="auto_awesome" value={stats.topics} label="Topics" />
          </div>
        </div>
      </div>

      {/* ── Quest Block ──────────────────────────────────────────────── */}
      <div className="px-4 -mt-2 relative z-10">
        <button onClick={handleShuffle}
          className="w-full flex items-center gap-3 p-4 rounded-2xl transition-all active:scale-[0.98]"
          style={{
            background: isDark ? 'rgba(255,255,255,0.04)' : 'white',
            boxShadow: '0 2px 12px rgba(0,0,0,0.04)',
            border: `1px solid ${isDark ? 'rgba(255,255,255,0.06)' : 'rgba(59,10,23,0.05)'}`,
          }}>
          {/* Pet bed placeholder */}
          <div className="w-11 h-11 rounded-full flex items-center justify-center flex-shrink-0"
            style={{ background: `${heroAccent}20` }}>
            <MaterialIcon name="pets" size={22} style={{ color: heroAccent }} />
          </div>
          <div className="flex-1 text-left min-w-0">
            <p className="text-[10px] font-extrabold tracking-widest uppercase mb-0.5"
              style={{ color: heroAccent, letterSpacing: '1.6px' }}>Today's Quest</p>
            <p className="text-lg font-extrabold truncate" style={{ color: getTextColor(isDark) }}>Shuffle the deck</p>
            <p className="text-xs truncate" style={{ color: isDark ? 'rgba(255,255,255,0.45)' : 'rgba(59,10,23,0.45)' }}>
              A fresh mix of ideas, picked for you
            </p>
          </div>
          <div className="w-[52px] h-[52px] rounded-full flex items-center justify-center flex-shrink-0"
            style={{ background: heroAccent }}>
            <MaterialIcon name="casino" size={24} style={{ color: '#fff' }} />
          </div>
        </button>
      </div>

      {/* ── Recents ──────────────────────────────────────────────── */}
      <div className="px-4 mt-6">
        <div className="flex items-center justify-between mb-3">
          <h3 className="text-base font-bold" style={{ color: getTextColor(isDark) }}>Recents</h3>
          {recents.length > 0 && (
            <button onClick={() => navigate('/cabinet')}
              className="flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-bold"
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
                <button key={e.id}
                  onClick={() => navigate(`/detail/${e.id}`)}
                  className="w-full text-left flex items-center gap-3 p-3 rounded-xl transition-all active:scale-[0.98]"
                  style={{
                    background: isDark ? 'rgba(255,255,255,0.03)' : 'rgba(59,10,23,0.015)',
                    border: `1px solid ${isDark ? 'rgba(255,255,255,0.04)' : 'rgba(59,10,23,0.03)'}`,
                  }}>
                  <MaterialIcon name={cat?.iconGlyph || 'edit_note'} size={22} style={{ color: cat?.accent || '#999' }} />
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-semibold truncate" style={{ color: getTextColor(isDark) }}>{e.name}</p>
                    <p className="text-xs" style={{ color: isDark ? 'rgba(255,255,255,0.4)' : 'rgba(59,10,23,0.4)' }}>
                      {cat?.displayName} · {e.subtype} · {e.daysAgo === 0 ? 'today' : e.daysAgo === 1 ? 'yesterday' : `${e.daysAgo}d ago`}
                    </p>
                  </div>
                  <MaterialIcon name="chevron_right" size={16} style={{ color: isDark ? 'rgba(255,255,255,0.3)' : 'rgba(59,10,23,0.3)' }} />
                </button>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
};

const Stat: React.FC<{ inline?: boolean; icon: string; value: string; label: string }> = ({ inline, icon, value, label }) => (
  <div className="flex flex-col items-center" style={inline ? { flex: 1 } : {}}>
    <MaterialIcon name={icon} size={16} style={{ color: 'rgba(255,255,255,0.85)', marginBottom: 2 }} />
    <span className="text-sm font-extrabold text-white">{value}</span>
    <span className="text-[10px] text-white/75">{label}</span>
  </div>
);

export default HomeScreen;
