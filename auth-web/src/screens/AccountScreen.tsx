import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTheme, getBackgroundColor, getTextColor } from '../theme/ThemeContext';
import { getQuestSystem } from '../data/QuestSystem';
import { MaterialIcon } from '../components/SharedComponents';

const AccountScreen: React.FC = () => {
  const navigate = useNavigate();
  const { isDark, isAmoled } = useTheme();
  const [displayName, setDisplayName] = useState(() => localStorage.getItem('curio-display-name') || 'Explorer');
  const [stats, setStats] = useState({ saved: 0, spins: 0, streak: 0 });
  useEffect(() => {
    const state = getQuestSystem().getState();
    setStats({ saved: state.lifetime?.saves || 0, spins: state.lifetime?.spins || 0, streak: state.bestStreak || 0 });
  }, []);
  const muted = isDark ? 'rgba(255,255,255,.58)' : 'rgba(59,10,23,.58)';
  const surface = isDark ? 'rgba(255,255,255,.055)' : 'rgba(255,255,255,.62)';
  const border = isDark ? 'rgba(255,255,255,.10)' : 'rgba(59,10,23,.08)';
  return (
    <main className="curio-account-screen min-h-screen px-4 pt-8 pb-32 relative" style={{ backgroundColor: getBackgroundColor(isDark, isAmoled), color: getTextColor(isDark) }}>
      <div className="max-w-2xl mx-auto">
        <header className="flex items-center justify-between mb-8">
          <div><p className="text-[10px] uppercase tracking-[.24em] font-bold" style={{ color: muted }}>Your Curio space</p><h1 className="text-3xl font-extrabold mt-2" style={{ fontFamily: 'Geom, Inter, sans-serif' }}>Account</h1></div>
          <button onClick={() => navigate('/settings')} className="w-12 h-12 rounded-full flex items-center justify-center" style={{ background: surface, border: '1px solid ' + border, backdropFilter: 'blur(20px)' }} aria-label="Settings"><MaterialIcon name="settings" size={21} /></button>
        </header>
        <section className="curio-account-hero rounded-[30px] p-6 mb-5 relative overflow-hidden" style={{ background: 'linear-gradient(135deg,#DA1E37 0%,#641220 100%)', color: '#fff', boxShadow: '0 24px 60px -30px rgba(100,18,32,.65)' }}>
          <div className="absolute -right-8 -top-12 w-52 h-52 rounded-full border border-white/15" /><div className="absolute -right-2 -top-5 w-36 h-36 rounded-full border border-white/10" />
          <div className="relative flex items-center gap-4"><div className="w-16 h-16 rounded-full flex items-center justify-center text-2xl font-bold" style={{ background: 'rgba(255,255,255,.2)', border: '1px solid rgba(255,255,255,.42)', backdropFilter: 'blur(12px)' }}>{displayName.trim().charAt(0).toUpperCase() || 'C'}</div><div className="min-w-0"><p className="text-[10px] uppercase tracking-[.2em] text-white/65">Curio explorer</p><h2 className="text-2xl font-extrabold truncate">{displayName}</h2><p className="text-xs text-white/70 mt-1">Your curiosity, collected locally.</p></div></div>
          <label className="block text-[10px] uppercase tracking-[.16em] text-white/70 mt-6 mb-2" htmlFor="curio-display-name">Display name</label>
          <input id="curio-display-name" value={displayName} onChange={e => { setDisplayName(e.target.value); localStorage.setItem('curio-display-name', e.target.value); }} className="w-full rounded-2xl px-4 py-3 text-sm text-white outline-none" style={{ background: 'rgba(255,255,255,.14)', border: '1px solid rgba(255,255,255,.24)' }} />
        </section>
        <section className="grid grid-cols-3 gap-3 mb-6">
          {[{ label: 'Saved', value: stats.saved, icon: 'bookmark' }, { label: 'Shuffles', value: stats.spins, icon: 'casino' }, { label: 'Best streak', value: stats.streak, icon: 'local_fire_department' }].map(item => <div key={item.label} className="rounded-2xl p-4 text-center" style={{ background: surface, border: '1px solid ' + border, backdropFilter: 'blur(18px)' }}><MaterialIcon name={item.icon} size={19} /><p className="text-xl font-extrabold mt-2">{item.value}</p><p className="text-[10px] mt-1" style={{ color: muted }}>{item.label}</p></div>)}
        </section>
        <section className="rounded-[26px] overflow-hidden mb-5" style={{ background: surface, border: '1px solid ' + border, backdropFilter: 'blur(20px)' }}>
          <button onClick={() => navigate('/settings')} className="w-full flex items-center gap-3 p-4 text-left"><span className="w-10 h-10 rounded-xl flex items-center justify-center" style={{ background: 'rgba(218,30,55,.14)', color: '#DA1E37' }}><MaterialIcon name="tune" size={20} /></span><span className="flex-1"><strong className="block text-sm">Preferences</strong><small className="text-xs" style={{ color: muted }}>Appearance and experience</small></span><MaterialIcon name="chevron_right" size={18} /></button>
          <div style={{ height: 1, background: border }} />
          <a href="/signin" className="flex items-center gap-3 p-4"><span className="w-10 h-10 rounded-xl flex items-center justify-center" style={{ background: 'rgba(127,160,200,.16)', color: '#7FA0C8' }}><MaterialIcon name="person" size={20} /></span><span className="flex-1"><strong className="block text-sm">Online account</strong><small className="text-xs" style={{ color: muted }}>Sign in or manage your account</small></span><MaterialIcon name="chevron_right" size={18} /></a>
          <div style={{ height: 1, background: border }} />
          <a href="/privacy" className="flex items-center gap-3 p-4"><span className="w-10 h-10 rounded-xl flex items-center justify-center" style={{ background: 'rgba(127,155,110,.16)', color: '#7F9B6E' }}><MaterialIcon name="shield" size={20} /></span><span className="flex-1"><strong className="block text-sm">Privacy</strong><small className="text-xs" style={{ color: muted }}>Your saved discoveries stay on this device</small></span><MaterialIcon name="chevron_right" size={18} /></a>
        </section>
        <p className="text-center text-[11px] px-6" style={{ color: muted }}>Curio is a quiet place to follow what makes you curious.</p>
      </div>
    </main>
  );
};

export default AccountScreen;