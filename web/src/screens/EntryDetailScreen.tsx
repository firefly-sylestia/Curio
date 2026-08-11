// Curio Web App - Entry Detail Screen
// Matches Android: torn hero, frosted date bar, paper note cards, audio player, mood card, quotes

import React, { useState, useEffect, useRef } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useTheme, getBackgroundColor, getTextColor } from '../theme/ThemeContext';
import { ALL_CATEGORIES } from '../data/categories';
import { MaterialIcon, CurioWatermarkBackdrop } from '../components/SharedComponents';
import type { CurioCategory, CaptureFormat, CaptureData, JournalMood } from '../types';
import { JOURNAL_MOODS } from '../types';
import { captureRepository, deserializeTags } from '../db/database';
import { ScreenEntrance } from '../animations';
import { TornHero, makeDetailHeroSymbols } from '../components/TornHero';

// ─── Paper ink & font ────────────────────────────────────────────────
const paperFont = "'Patrick Hand', cursive";
const getPaperInk = (isDark: boolean) => isDark ? '#E4D2BC' : '#2D140F';
const HERO_H = 400;

// ─── Paper card for saved views ───────────────────────────────────────
const SavedPaperCard: React.FC<{ children: React.ReactNode; isDark: boolean; accent: string }> = ({ children, isDark, accent }) => (
  <div className="rounded-2xl p-5 relative overflow-hidden" style={{
    background: isDark ? '#2A2520' : '#FDF8F0',
    border: `1px solid ${accent}15`,
  }}>
    <div className="absolute top-0 left-4 w-8 h-[2px] rounded-full" style={{ background: accent, opacity: 0.6 }} />
    <div className="absolute inset-0 pointer-events-none opacity-[0.04]"
      style={{
        backgroundImage: `repeating-linear-gradient(transparent, transparent 27px, ${getPaperInk(isDark)} 27px, ${getPaperInk(isDark)} 28px)`,
        backgroundPosition: '0 38px',
      }} />
    <div className="relative" style={{ fontFamily: paperFont, color: getPaperInk(isDark), fontSize: '1.1rem', lineHeight: 1.75 }}>
      {children}
    </div>
  </div>
);

// ─── Mood badge ──────────────────────────────────────────────────────
const MoodBadge: React.FC<{ mood: JournalMood; accent: string }> = ({ mood, accent }) => {
  const m = JOURNAL_MOODS.find(j => j.id === mood);
  if (!m) return null;
  return (
    <div className="flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-semibold" style={{ background: `${accent}15`, color: accent }}>
      <MaterialIcon name={m.icon} size={14} /> {m.label}
    </div>
  );
};

// ═══════════════════════════════════════════════════════════════════════════
// SOUND BITE RENDER — audio waveform, player, title, note, mood, quotes
// ═══════════════════════════════════════════════════════════════════════════

const SoundBiteRender: React.FC<{ data: CaptureData; isDark: boolean; accent: string }> = ({ data, isDark, accent }) => {
  const d = data as any;
  const [isPlaying, setIsPlaying] = useState(false);
  const [currentTime, setCurrentTime] = useState(0);
  const [duration, setDuration] = useState(d.durationSeconds || 0);
  const audioRef = useRef<HTMLAudioElement>(null);
  const [bars] = useState(() => Array.from({ length: 40 }, () => 0.15 + Math.random() * 0.45).sort(() => Math.random() - 0.5));

  useEffect(() => {
    const audio = audioRef.current;
    if (!audio) return;
    const onTime = () => setCurrentTime(audio.currentTime);
    const onLoaded = () => setDuration(audio.duration || d.durationSeconds);
    const onEnded = () => setIsPlaying(false);
    audio.addEventListener('timeupdate', onTime);
    audio.addEventListener('loadedmetadata', onLoaded);
    audio.addEventListener('ended', onEnded);
    return () => { audio.removeEventListener('timeupdate', onTime); audio.removeEventListener('loadedmetadata', onLoaded); audio.removeEventListener('ended', onEnded); };
  }, [d.durationSeconds]);

  const togglePlay = () => {
    const audio = audioRef.current;
    if (!audio || !d.audioDataUrl) return;
    if (isPlaying) { audio.pause(); setIsPlaying(false); }
    else { audio.play().then(() => setIsPlaying(true)).catch(() => {}); }
  };

  const fmt = (s: number) => `${String(Math.floor(s / 60)).padStart(2, '0')}:${String(Math.floor(s % 60)).padStart(2, '0')}`;
  const progress = duration > 0 ? currentTime / duration : 0;
  const ink = getPaperInk(isDark);

  return (
    <div className="space-y-4">
      <SavedPaperCard isDark={isDark} accent={accent}>
        {/* Audio player */}
        {d.audioDataUrl && (
          <div className="mb-4">
            <div className="flex items-center gap-3 mb-2">
              <button onClick={togglePlay}
                className="w-12 h-12 rounded-full flex items-center justify-center active:scale-90 transition-transform"
                style={{ background: accent }}>
                <MaterialIcon name={isPlaying ? 'pause' : 'play_arrow'} size={24} style={{ color: 'white' }} />
              </button>
              <div className="flex-1">
                {/* Static waveform */}
                <div className="flex items-end justify-center gap-[2px] h-8">
                  {bars.map((h, i) => (
                    <div key={i} className="flex-1 rounded-full"
                      style={{
                        height: `${Math.max(3, h * 100)}%`,
                        background: accent,
                        opacity: progress > (i / bars.length) ? 0.7 : 0.18,
                        minWidth: 2,
                        transition: 'opacity 0.2s',
                      }} />
                  ))}
                </div>
                <div className="flex justify-between mt-1">
                  <span className="text-[10px] font-mono opacity-50" style={{ color: ink }}>{fmt(currentTime)}</span>
                  <span className="text-[10px] font-mono opacity-50" style={{ color: ink }}>{fmt(duration)}</span>
                </div>
              </div>
            </div>
            <audio ref={audioRef} src={d.audioDataUrl} className="hidden" />
            {/* Seek bar */}
            <div className="h-1 rounded-full cursor-pointer relative" style={{ background: isDark ? 'rgba(255,255,255,0.08)' : 'rgba(0,0,0,0.06)' }}
              onClick={(e) => {
                const rect = e.currentTarget.getBoundingClientRect();
                const pct = (e.clientX - rect.left) / rect.width;
                if (audioRef.current) { audioRef.current.currentTime = pct * duration; }
              }}>
              <div className="h-full rounded-full transition-all" style={{ width: `${progress * 100}%`, background: accent }} />
            </div>
            <div className="flex items-center gap-1 mt-2 opacity-50" style={{ color: ink }}>
              <MaterialIcon name="mic" size={16} />
              <span className="text-xs">{(d.fileSizeBytes / 1024).toFixed(0)} kB &middot; {(d.durationSeconds || 0)}s</span>
            </div>
          </div>
        )}

        {/* Title */}
        {d.title && (
          <h3 className="text-lg font-bold mb-3" style={{ fontFamily: paperFont, color: ink }}>{d.title}</h3>
        )}

        {/* Note */}
        {d.note && (
          <p className="whitespace-pre-wrap">{d.note}</p>
        )}

        {/* No content */}
        {!d.title && !d.note && !d.audioDataUrl && (
          <p className="opacity-40 italic" style={{ fontFamily: paperFont }}>Voice note with no written content.</p>
        )}
      </SavedPaperCard>

      {/* Mood */}
      {d.mood && (
        <div className="flex items-center gap-2">
          <span className="text-xs opacity-40 uppercase tracking-wider" style={{ color: getTextColor(isDark) }}>Mood</span>
          <MoodBadge mood={d.mood} accent={accent} />
        </div>
      )}

      {/* Quotes */}
      {d.quotes && d.quotes.length > 0 && (
        <SavedPaperCard isDark={isDark} accent={accent}>
          <div className="flex items-center gap-1.5 text-xs font-semibold mb-3 opacity-40 uppercase tracking-wider" style={{ fontFamily: paperFont, color: ink }}>
            <MaterialIcon name="format_quote" size={14} /> Quotes
          </div>
          <div className="space-y-3">
            {d.quotes.map((q: any, i: number) => (
              <div key={i} className="pl-3 border-l-2" style={{ borderColor: accent }}>
                <p className="text-lg leading-relaxed">&ldquo;{q.text}&rdquo;</p>
                {q.context && <p className="text-sm mt-1 opacity-50">— {q.context}</p>}
              </div>
            ))}
          </div>
        </SavedPaperCard>
      )}
    </div>
  );
};

// ═══════════════════════════════════════════════════════════════════════════
// OTHER FORMAT RENDERERS
// ═══════════════════════════════════════════════════════════════════════════

const ReelNotesRender: React.FC<{ data: CaptureData; isDark: boolean; accent: string }> = ({ data, isDark, accent }) => {
  const d = data as any;
  return (
    <div className="space-y-4">
      <SavedPaperCard isDark={isDark} accent={accent}>
        {d.rating > 0 && (
          <div className="flex gap-1.5 mb-3">
            {[1, 2, 3, 4, 5].map(i => (
              <MaterialIcon key={i} name="star" size={26} filled={i <= d.rating}
                style={{ color: i <= d.rating ? '#E8A838' : isDark ? 'rgba(255,255,255,0.12)' : 'rgba(0,0,0,0.08)' }} />
            ))}
          </div>
        )}
        {d.review && <p className="whitespace-pre-wrap">{d.review}</p>}
      </SavedPaperCard>
      {d.mood && <MoodBadge mood={d.mood} accent={accent} />}
    </div>
  );
};

const MarginaliaRender: React.FC<{ data: CaptureData; isDark: boolean; accent: string }> = ({ data, isDark, accent }) => {
  const d = data as any;
  const ink = getPaperInk(isDark);
  return (
    <div className="space-y-4">
      <SavedPaperCard isDark={isDark} accent={accent}>
        {d.journalEntry && <p className="whitespace-pre-wrap mb-4">{d.journalEntry}</p>}
        {d.quotes && d.quotes.length > 0 && (
          <div className="space-y-3 mt-4 pt-3 border-t" style={{ borderColor: isDark ? 'rgba(255,255,255,0.08)' : 'rgba(0,0,0,0.06)' }}>
            <p className="text-xs font-semibold opacity-40 uppercase tracking-wider" style={{ fontFamily: paperFont, color: ink }}>Quotes</p>
            {d.quotes.map((q: any, i: number) => (
              <div key={i} className="pl-3 border-l-2" style={{ borderColor: accent }}>
                <p className="text-lg leading-relaxed">&ldquo;{q.text}&rdquo;</p>
                {q.context && <p className="text-sm mt-1 opacity-50">— {q.context}</p>}
              </div>
            ))}
          </div>
        )}
      </SavedPaperCard>
      {d.mood && <MoodBadge mood={d.mood} accent={accent} />}
    </div>
  );
};

const FieldNotesRender: React.FC<{ data: CaptureData; isDark: boolean; accent: string }> = ({ data, isDark, accent }) => {
  const d = data as any;
  const ink = getPaperInk(isDark);
  const sections = [
    { k: 'observed', icon: 'visibility', label: 'Observed', v: d.observed },
    { k: 'surprised', icon: 'sentiment_surprised', label: 'Surprised Me', v: d.surprised },
    { k: 'learnNext', icon: 'menu_book', label: 'Want to Learn Next', v: d.learnNext },
  ].filter(s => s.v);
  return (
    <div className="space-y-3">
      {sections.map(s => (
        <SavedPaperCard key={s.k} isDark={isDark} accent={accent}>
          <div className="flex items-center gap-1.5 text-xs font-semibold mb-2 opacity-40 uppercase tracking-wider" style={{ fontFamily: paperFont, color: ink }}>
            <MaterialIcon name={s.icon} size={14} /> {s.label}
          </div>
          <p className="whitespace-pre-wrap">{s.v}</p>
        </SavedPaperCard>
      ))}
      {d.mood && <MoodBadge mood={d.mood} accent={accent} />}
    </div>
  );
};

const GalleryWallRender: React.FC<{ data: CaptureData; isDark: boolean; accent: string }> = ({ data, isDark, accent }) => {
  const d = data as any;
  return (
    <div className="space-y-4">
      <SavedPaperCard isDark={isDark} accent={accent}>
        {d.caption && <p className="whitespace-pre-wrap mb-4">{d.caption}</p>}
        <div className="grid grid-cols-2 gap-2">
          {Array.from({ length: Math.min(d.images?.length || 2, 4) }).map((_, i) => (
            <div key={i} className="aspect-square rounded-lg flex items-center justify-center"
              style={{ background: isDark ? 'rgba(255,255,255,0.03)' : 'rgba(0,0,0,0.02)' }}>
              <MaterialIcon name="image" size={28} style={{ color: isDark ? 'rgba(255,255,255,0.12)' : 'rgba(0,0,0,0.08)' }} />
            </div>
          ))}
        </div>
      </SavedPaperCard>
      {d.mood && <MoodBadge mood={d.mood} accent={accent} />}
    </div>
  );
};

// ═══════════════════════════════════════════════════════════════════════════
// FROSTED SEGMENT
// ═══════════════════════════════════════════════════════════════════════════

const FrostedSegment: React.FC<{ icon: string; title: string; subtitle: string; tiny?: string }> = ({ icon, title, subtitle, tiny }) => (
  <div className="flex flex-col items-center flex-1">
    <MaterialIcon name={icon} size={16} style={{ color: 'rgba(255,255,255,0.9)' }} />
    <span className="text-xs font-bold mt-0.5 text-white">{title}</span>
    <span className="text-[10px] text-white/80">{subtitle}</span>
    {tiny && <span className="text-[9px] text-white/70">{tiny}</span>}
  </div>
);

// ═══════════════════════════════════════════════════════════════════════════
// MAIN ENTRY DETAIL SCREEN
// ═══════════════════════════════════════════════════════════════════════════

export const EntryDetailScreen: React.FC = () => {
  const navigate = useNavigate();
  const { entryId } = useParams();
  const { isDark, isAmoled } = useTheme();
  const [entry, setEntry] = useState<any>(null);
  const [category, setCategory] = useState<CurioCategory | null>(null);
  const [loading, setLoading] = useState(true);
  const [deleteVisible, setDeleteVisible] = useState(false);

  useEffect(() => {
    if (!entryId) return;
    (async () => {
      setLoading(true);
      try {
        const e = await captureRepository.getById(entryId);
        if (e) {
          setEntry(e);
          const cat = ALL_CATEGORIES.find(c => c.id === e.categoryId);
          setCategory(cat || ALL_CATEGORIES[0]);
        }
      } catch {} finally { setLoading(false); }
    })();
  }, [entryId]);

  const handleDelete = async () => {
    if (!entry) return;
    await captureRepository.delete(entry.id);
    navigate('/cabinet');
  };

  if (loading) return <div className="min-h-screen flex items-center justify-center" style={{ backgroundColor: getBackgroundColor(isDark, isAmoled) }}><div className="w-8 h-8 border-4 border-t-transparent rounded-full animate-spin" style={{ borderColor: '#FF8FA3', borderTopColor: 'transparent' }} /></div>;
  if (!entry || !category) return <div className="min-h-screen flex items-center justify-center" style={{ backgroundColor: getBackgroundColor(isDark, isAmoled) }}><p style={{ color: getTextColor(isDark) }}>Entry not found</p></div>;

  const captureData = entry.formatDataJson ? JSON.parse(entry.formatDataJson) : {};
  const tags = entry.tagsJson ? deserializeTags(entry.tagsJson) : [];
  const format = (entry.format || 'Marginalia') as CaptureFormat;
  const heroFill = category.accent;
  const heroInk = '#fff';
  const tearSeed = (entry.id || 'detail').split('').reduce((a: number, c: string) => a * 31 + c.charCodeAt(0), 0x0BADC0DE);

  const formatDate = (millis: number) => {
    const d = new Date(millis);
    return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
  };
  const daysAgo = Math.floor((Date.now() - (entry.capturedAtMillis || 0)) / 86400000);
  const tiny = daysAgo === 0 ? new Date(entry.capturedAtMillis).toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit' }) : daysAgo === 1 ? 'yesterday' : `${daysAgo}d ago`;

  return (
    <div className="min-h-screen pb-24 relative" style={{ background: `${category.accent}0A` }}>
      <CurioWatermarkBackdrop activeCatId={category.id} topClearance={HERO_H + 30} alphaScale={0.45} />

      {/* ── Torn Hero Banner ──────────────────────────────────────── */}
      <TornHero
        height={HERO_H}
        fill={heroFill}
        ink={heroInk}
        tearSeed={tearSeed}
        detail={true}
        symbols={makeDetailHeroSymbols(category.iconGlyph)}
        isDark={isDark}
      >
        <div className="flex flex-col items-center justify-center h-full px-7" style={{ paddingTop: 80, paddingBottom: 16 }}>
          <MaterialIcon name={category.iconGlyph} size={64} style={{ color: 'rgba(255,255,255,0.92)' }} />
          <div className="h-3" />
          <h1 className="text-2xl font-extrabold text-center leading-tight px-4"
            style={{ color: heroInk, fontFamily: 'Geom, Inter, sans-serif', maxWidth: 300 }}>
            {entry.topicName || 'Untitled'}
          </h1>
          <div className="h-4" />
          <div className="flex items-center rounded-2xl px-4 py-2.5 gap-3"
            style={{ background: 'rgba(255,255,255,0.12)', backdropFilter: 'blur(8px)', border: '1px solid rgba(255,255,255,0.22)' }}>
            <FrostedSegment icon="calendar_today" title={formatDate(entry.capturedAtMillis)} subtitle="Date" tiny={tiny} />
            <div className="w-px h-8" style={{ background: 'rgba(255,255,255,0.25)' }} />
            <FrostedSegment icon="category" title={format} subtitle="Type" />
          </div>
        </div>
      </TornHero>

      {/* Sticky back + actions */}
      <div className="absolute top-0 left-0 right-0 z-30 flex justify-between px-4"
        style={{ paddingTop: 'env(safe-area-inset-top, 12px)' }}>
        <button onClick={() => navigate(-1)} className="w-10 h-10 rounded-full flex items-center justify-center"
          style={{ background: 'rgba(255,255,255,0.18)', border: '1px solid rgba(255,255,255,0.25)' }}>
          <MaterialIcon name="arrow_back" size={20} style={{ color: '#fff' }} />
        </button>
        <div className="flex gap-2">
          <button onClick={() => navigate(`/capture/${category.id.toLowerCase()}/${(entry.topicName || 'topic').replace(/\s+/g, '-')}`)}
            className="w-10 h-10 rounded-full flex items-center justify-center"
            style={{ background: 'rgba(255,255,255,0.18)', border: '1px solid rgba(255,255,255,0.25)' }}>
            <MaterialIcon name="edit" size={20} style={{ color: '#fff' }} />
          </button>
          <button onClick={() => setDeleteVisible(true)}
            className="w-10 h-10 rounded-full flex items-center justify-center"
            style={{ background: 'rgba(255,255,255,0.18)', border: '1px solid rgba(255,255,255,0.25)' }}>
            <MaterialIcon name="delete" size={20} style={{ color: '#FF6B6B' }} />
          </button>
        </div>
      </div>

      {/* ── Body ──────────────────────────────────────────────────── */}
      <ScreenEntrance>
      <div className="relative z-10 px-5 max-w-lg mx-auto">
        {/* Category label */}
        <div className="flex items-center gap-2.5 py-3 mt-1">
          <MaterialIcon name={category.iconGlyph} size={22} style={{ color: category.accent }} />
          <span className="text-lg font-extrabold" style={{ color: getTextColor(isDark), fontFamily: 'Geom, Inter, sans-serif' }}>{category.displayName}</span>
        </div>

        {/* Quick fact */}
        {entry.topicTeaser && (
          <div className="mb-4">
            <div className="flex items-center gap-1.5 mb-1.5">
              <MaterialIcon name="auto_awesome" size={14} style={{ color: isDark ? 'rgba(255,255,255,0.55)' : 'rgba(0,0,0,0.55)' }} />
              <span className="text-xs font-semibold opacity-60" style={{ color: getTextColor(isDark) }}>Quick fact</span>
            </div>
            <p className="text-sm leading-relaxed opacity-80" style={{ color: getTextColor(isDark) }}>{entry.topicTeaser}</p>
          </div>
        )}

        {/* Tags */}
        {tags.length > 0 && (
          <div className="flex flex-wrap gap-1.5 mb-4">
            {tags.map((tag: string) => (
              <span key={tag} className="px-2.5 py-1 rounded-full text-xs font-medium"
                style={{ background: `${category.accent}18`, color: category.accent }}>#{tag}</span>
            ))}
          </div>
        )}

        {/* Format body */}
        <div className="mt-3">
          {format === 'SoundBite' && <SoundBiteRender data={captureData} isDark={isDark} accent={category.accent} />}
          {format === 'ReelNotes' && <ReelNotesRender data={captureData} isDark={isDark} accent={category.accent} />}
          {format === 'Marginalia' && <MarginaliaRender data={captureData} isDark={isDark} accent={category.accent} />}
          {format === 'GalleryWall' && <GalleryWallRender data={captureData} isDark={isDark} accent={category.accent} />}
          {format === 'FieldNotes' && <FieldNotesRender data={captureData} isDark={isDark} accent={category.accent} />}
        </div>
      </div>
      </ScreenEntrance>

      {/* Delete dialog */}
      {deleteVisible && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-70">
          <div className="p-6 rounded-3xl mx-6 max-w-sm w-full" style={{ background: isDark ? '#1a1a2e' : 'white' }}>
            <h3 className="text-lg font-bold mb-2" style={{ color: getTextColor(isDark) }}>Delete this entry?</h3>
            <p className="text-sm mb-6 opacity-60" style={{ color: getTextColor(isDark) }}>This capture will be permanently removed from your Cabinet.</p>
            <div className="flex gap-3">
              <button onClick={() => setDeleteVisible(false)} className="flex-1 py-3 rounded-2xl font-medium"
                style={{ background: isDark ? 'rgba(255,255,255,0.08)' : 'rgba(0,0,0,0.04)', color: getTextColor(isDark) }}>Cancel</button>
              <button onClick={handleDelete} className="flex-1 py-3 rounded-2xl font-medium text-white"
                style={{ background: '#EF4444' }}>Delete</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default EntryDetailScreen;
