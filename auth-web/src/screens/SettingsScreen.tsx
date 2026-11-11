import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTheme, getBackgroundColor, getTextColor } from '../theme/ThemeContext';
import { CurioSectionHeader, MaterialIcon } from '../components/SharedComponents';
import { ScreenEntrance } from '../animations';

const engines = [
  { value: 'google', label: 'Google' },
  { value: 'duckduckgo', label: 'DuckDuckGo' },
  { value: 'bing', label: 'Bing' },
  { value: 'brave', label: 'Brave' },
  { value: 'ecosia', label: 'Ecosia' },
  { value: 'startpage', label: 'Startpage' },
  { value: 'yahoo', label: 'Yahoo' },
];
const musicServices = [
  { value: 'youtube_music', label: 'YouTube Music' },
  { value: 'apple_music', label: 'Apple Music' },
  { value: 'spotify', label: 'Spotify' },
];

const ToggleRow: React.FC<{ label: string; description: string; checked: boolean; onChange: (value: boolean) => void }> = ({ label, description, checked, onChange }) => {
  const { isDark } = useTheme();
  return (
    <div className="flex items-center justify-between py-3">
      <div className="flex-1 mr-4">
        <div className="text-sm font-medium" style={{ color: getTextColor(isDark) }}>{label}</div>
        <div className="text-xs mt-0.5" style={{ color: isDark ? 'rgba(255,255,255,.54)' : 'rgba(23,27,38,.56)' }}>{description}</div>
      </div>
      <button type="button" role="switch" aria-checked={checked} aria-label={label}
        onClick={() => onChange(!checked)}
        className={'relative flex-none w-12 h-7 rounded-full p-[3px] transition-all duration-200 ' + (checked ? 'bg-[#DA1E37] shadow-md shadow-red-900/20' : (isDark ? 'bg-white/20' : 'bg-slate-900/15'))}>
        <span className={'block w-[22px] h-[22px] rounded-full bg-white shadow-sm transition-transform duration-200 ' + (checked ? 'translate-x-5' : 'translate-x-0')} />
      </button>
    </div>
  );
};

const SelectRow: React.FC<{ label: string; description: string; value: string; options: { value: string; label: string }[]; onChange: (value: string) => void }> = ({ label, description, value, options, onChange }) => {
  const { isDark } = useTheme();
  return (
    <label className="flex items-center justify-between gap-3 py-4">
      <span className="flex min-w-0 flex-1 flex-col gap-1">
        <strong className="text-sm font-semibold" style={{ color: getTextColor(isDark) }}>{label}</strong>
        <small className="text-xs" style={{ color: isDark ? 'rgba(255,255,255,.5)' : 'rgba(23,27,38,.52)' }}>{description}</small>
      </span>
      <select value={value} onChange={event => onChange(event.target.value)}
        className="max-w-[145px] rounded-xl px-3 py-2 text-xs font-semibold outline-none"
        style={{ background: isDark ? 'rgba(255,255,255,.09)' : 'rgba(23,27,38,.06)', color: getTextColor(isDark), border: 0 }}>
        {options.map(option => <option key={option.value} value={option.value}>{option.label}</option>)}
      </select>
    </label>
  );
};

export const SettingsScreen: React.FC = () => {
  const navigate = useNavigate();
  const { isDark, isAmoled, style, pastelColors, setThemeStyle, setDarkMode, setPastelColors } = useTheme();
  const [searchEngine, setSearchEngine] = useState(() => localStorage.getItem('curio-search-engine') || 'google');
  const [musicService, setMusicService] = useState(() => localStorage.getItem('curio-music-service') || 'youtube_music');
  const saveSearch = (value: string) => { setSearchEngine(value); localStorage.setItem('curio-search-engine', value); };
  const saveMusic = (value: string) => { setMusicService(value); localStorage.setItem('curio-music-service', value); };
  const panelStyle: React.CSSProperties = {
    background: isDark ? 'rgba(24,27,34,.78)' : 'rgba(255,255,255,.78)',
    boxShadow: isDark ? '0 20px 46px -30px rgba(0,0,0,.9), inset 0 1px 0 rgba(255,255,255,.07)' : '0 18px 42px -32px rgba(25,35,55,.34), 0 3px 12px -9px rgba(25,35,55,.18), inset 0 1px 0 rgba(255,255,255,.8)',
    backdropFilter: 'blur(22px) saturate(140%)',
    WebkitBackdropFilter: 'blur(22px) saturate(140%)',
    borderRadius: 22,
  };
  const themeOptions = [
    { id: 'curio', title: 'Curio', subtitle: 'Signature', colors: ['#DA1E37','#F6F7FB','#171B26'] },
    { id: 'material', title: 'Material', subtitle: 'Indigo', colors: ['#4338CA','#EEF0FF','#171B26'] },
    { id: 'amoled', title: 'AMOLED', subtitle: 'Pure black', colors: ['#000000','#242424','#DA1E37'] },
  ];
  return (
    <main className="curio-settings-screen min-h-screen pb-32 relative" style={{ backgroundColor: getBackgroundColor(isDark, isAmoled) }}>
      <ScreenEntrance>
        <div className="relative z-10 mx-auto w-full max-w-[760px] px-4 pt-8 pb-12">
          <header className="flex items-start gap-4 mb-9">
            <button onClick={() => navigate(-1)} aria-label="Go back" className="w-11 h-11 flex-none grid place-items-center rounded-2xl" style={panelStyle}>
              <MaterialIcon name="arrow_back" size={19} />
            </button>
            <div className="min-w-0 flex-1">
              <span className="block text-[9px] font-extrabold tracking-[.2em] mb-2" style={{ color: '#DA1E37' }}>CURIO / PREFERENCES</span>
              <h1 className="text-[clamp(36px,7vw,48px)] leading-none font-extrabold tracking-[-.06em]" style={{ color: getTextColor(isDark) }}>Make it yours<span style={{ color: '#DA1E37' }}>.</span></h1>
              <p className="text-sm mt-3" style={{ color: isDark ? 'rgba(255,255,255,.55)' : 'rgba(23,27,38,.58)' }}>A few thoughtful choices for your discovery space.</p>
            </div>
            <span className="w-11 h-11 flex-none grid place-items-center rounded-2xl" style={{ ...panelStyle, color: '#DA1E37' }}><MaterialIcon name="tune" size={21} /></span>
          </header>

          <section className="mb-7">
            <CurioSectionHeader title="Appearance" />
            <div className="p-3" style={panelStyle}>
              <div className="grid grid-cols-3 gap-2">
                {themeOptions.map(theme => {
                  const selected = style === theme.id;
                  return <button type="button" key={theme.id} onClick={() => setThemeStyle(theme.id)} aria-pressed={selected}
                    className="relative flex flex-col items-start gap-2 rounded-2xl p-3 text-left transition-all"
                    style={{ background: selected ? 'rgba(218,30,55,.09)' : (isDark ? 'rgba(255,255,255,.045)' : 'rgba(23,27,38,.035)'), color: getTextColor(isDark), outline: selected ? '1.5px solid rgba(218,30,55,.5)' : '1.5px solid transparent' }}>
                    <span className="flex w-full h-9 overflow-hidden rounded-xl">{theme.colors.map((color,index) => <i key={index} className="flex-1" style={{ background: color }} />)}</span>
                    <strong className="text-xs font-bold">{theme.title}</strong>
                    <small className="text-[10px]" style={{ color: isDark ? 'rgba(255,255,255,.5)' : 'rgba(23,27,38,.52)' }}>{theme.subtitle}</small>
                    {selected && <span className="absolute top-2 right-2 grid place-items-center w-5 h-5 rounded-full text-white" style={{ background: '#DA1E37' }}><MaterialIcon name="check" size={13} /></span>}
                  </button>;
                })}
              </div>
              <div className="h-px my-2 mx-1" style={{ background: isDark ? 'rgba(255,255,255,.09)' : 'rgba(23,27,38,.08)' }} />
              <div className="px-1">
                <ToggleRow label="Dark appearance" description="Use a darker interface in low light" checked={isDark} onChange={setDarkMode} />
                <div className="h-px" style={{ background: isDark ? 'rgba(255,255,255,.07)' : 'rgba(23,27,38,.06)' }} />
                <ToggleRow label="Soft category colours" description="Tone down category accents throughout Curio" checked={pastelColors} onChange={setPastelColors} />
              </div>
            </div>
          </section>

          <section className="mb-7">
            <CurioSectionHeader title="Discovery" />
            <div className="px-4" style={panelStyle}>
              <SelectRow label="Search engine" description="Opens when you research a topic" value={searchEngine} options={engines} onChange={saveSearch} />
              <div className="h-px" style={{ background: isDark ? 'rgba(255,255,255,.08)' : 'rgba(23,27,38,.07)' }} />
              <SelectRow label="Music service" description="Used for artists, albums and songs" value={musicService} options={musicServices} onChange={saveMusic} />
            </div>
          </section>

          <section className="mb-7">
            <CurioSectionHeader title="Your library" />
            <button onClick={() => navigate('/manage-categories')} className="w-full flex items-center gap-3 p-4 text-left" style={panelStyle}>
              <span className="w-10 h-10 rounded-xl grid place-items-center" style={{ color: '#DA1E37', background: 'rgba(218,30,55,.1)' }}><MaterialIcon name="category" size={20} /></span>
              <span className="flex-1 min-w-0"><strong className="block text-sm" style={{ color: getTextColor(isDark) }}>Manage categories</strong><small className="block text-xs mt-1" style={{ color: isDark ? 'rgba(255,255,255,.5)' : 'rgba(23,27,38,.52)' }}>Choose what appears in your discovery deck</small></span>
              <MaterialIcon name="chevron_right" size={19} />
            </button>
          </section>
          <p className="text-center text-[11px] mt-8" style={{ color: isDark ? 'rgba(255,255,255,.4)' : 'rgba(23,27,38,.42)' }}>Your journal and saved discoveries stay on this device.</p>
        </div>
      </ScreenEntrance>
    </main>
  );
};

export default SettingsScreen;
