// Curio Web App - Entry Detail Screen
// Matches Android: torn hero with frosted date bar, paper note cards, Quick fact

import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useTheme, getBackgroundColor, getTextColor } from '../theme/ThemeContext';
import { ALL_CATEGORIES } from '../data/categories';
import { CurioPaperCard, MaterialIcon, CurioWatermarkBackdrop } from '../components/SharedComponents';
import type { CurioCategory, CaptureFormat, CaptureData } from '../types';
import { captureRepository, deserializeTags } from '../db/database';
import { ScreenEntrance } from '../animations';

// ─── Paper ink for saved views ──────────────────────────────────────
const getPaperInk = (isDark: boolean) => isDark ? '#E4D2BC' : '#2D140F';

// ─── Hero height ─────────────────────────────────────────────────────
const HERO_H = 360;

// ─── Format Renderers (Patrick Hand, paper ink, paper bg) ────────────
const SavedPaperCard: React.FC<{ children: React.ReactNode; isDark: boolean; accent: string }> = ({ children, isDark, accent }) => (
  <CurioPaperCard variant="ruled" accent={accent}>
    <div style={{ fontFamily: "'Patrick Hand', cursive", color: getPaperInk(isDark), fontSize: '1.1rem', lineHeight: 1.75 }}>
      {children}
    </div>
  </CurioPaperCard>
);

const SoundBiteRender: React.FC<{ data: CaptureData; isDark: boolean; accent: string }> = ({ data, isDark, accent }) => {
  const d = data as { durationSeconds: number; notes: string };
  return (
    <SavedPaperCard isDark={isDark} accent={accent}>
      <div className="flex items-center gap-3 mb-4">
        <MaterialIcon name="mic" size={28} />
        <div>
          <div className="font-semibold text-lg">Voice Note</div>
          <div className="text-sm opacity-50">{(d.durationSeconds || 0)}s</div>
        </div>
      </div>
      {d.notes && <p className="whitespace-pre-wrap">{d.notes}</p>}
    </SavedPaperCard>
  );
};

const ReelNotesRender: React.FC<{ data: CaptureData; isDark: boolean; accent: string }> = ({ data, isDark, accent }) => {
  const d = data as { rating: number; review: string };
  return (
    <SavedPaperCard isDark={isDark} accent={accent}>
      {d.rating > 0 && (
        <div className="flex gap-1.5 mb-3">
          {[1,2,3,4,5].map(i => (
            <MaterialIcon key={i} name="star" size={26} filled={i <= d.rating}
              style={{ color: i <= d.rating ? '#E8A838' : isDark ? 'rgba(255,255,255,0.12)' : 'rgba(0,0,0,0.08)' }} />
          ))}
        </div>
      )}
      {d.review && <p className="whitespace-pre-wrap">{d.review}</p>}
    </SavedPaperCard>
  );
};

const MarginaliaRender: React.FC<{ data: CaptureData; isDark: boolean; accent: string }> = ({ data, isDark, accent }) => {
  const d = data as { journalEntry: string; quotes: Array<{ text: string; context?: string }> };
  const ink = getPaperInk(isDark);
  return (
    <SavedPaperCard isDark={isDark} accent={accent}>
      {d.journalEntry && <p className="whitespace-pre-wrap mb-4">{d.journalEntry}</p>}
      {d.quotes.length > 0 && (
        <div className="space-y-3 mt-4 pt-3 border-t" style={{ borderColor: isDark ? 'rgba(255,255,255,0.08)' : 'rgba(0,0,0,0.06)' }}>
          <p className="text-sm font-semibold opacity-60" style={{ color: ink }}>Favorite Quotes</p>
          {d.quotes.map((q, i) => (
            <div key={i} className="pl-3 border-l-2" style={{ borderColor: accent }}>
              <p className="text-lg leading-relaxed">&ldquo;{q.text}&rdquo;</p>
              {q.context && <p className="text-sm mt-1 opacity-50">— {q.context}</p>}
            </div>
          ))}
        </div>
      )}
    </SavedPaperCard>
  );
};

const FieldNotesRender: React.FC<{ data: CaptureData; isDark: boolean; accent: string }> = ({ data, isDark, accent }) => {
  const d = data as { observed: string; surprised: string; learnNext: string };
  const ink = getPaperInk(isDark);
  const sections = [
    { k: 'observed', icon: 'visibility', label: 'Observed', v: d.observed },
    { k: 'surprised', icon: 'sentiment_surprised', label: 'Surprised Me', v: d.surprised },
    { k: 'learnNext', icon: 'menu_book', label: 'Want to Learn Next', v: d.learnNext },
  ].filter(s => s.v);
  if (sections.length === 0) return null;
  return (
    <div className="space-y-3">
      {sections.map(s => (
        <SavedPaperCard key={s.k} isDark={isDark} accent={accent}>
          <div className="flex items-center gap-1.5 text-sm font-semibold mb-2 opacity-60" style={{ color: ink }}>
            <MaterialIcon name={s.icon} size={16} /> {s.label}
          </div>
          <p className="whitespace-pre-wrap">{s.v}</p>
        </SavedPaperCard>
      ))}
    </div>
  );
};

const GalleryWallRender: React.FC<{ data: CaptureData; isDark: boolean; accent: string }> = ({ data, isDark, accent }) => {
  const d = data as { caption: string; images: string[] };
  return (
    <SavedPaperCard isDark={isDark} accent={accent}>
      {d.caption && <p className="whitespace-pre-wrap mb-4">{d.caption}</p>}
      {d.images.length > 0 && (
        <div className="grid grid-cols-2 gap-2 mt-3">
          {d.images.map((_, i) => (
            <div key={i} className="aspect-square rounded-lg flex items-center justify-center"
              style={{ background: isDark ? 'rgba(255,255,255,0.04)' : 'rgba(0,0,0,0.02)' }}>
              <MaterialIcon name="image" size={32} style={{ color: isDark ? 'rgba(255,255,255,0.2)' : 'rgba(0,0,0,0.12)' }} />
            </div>
          ))}
        </div>
      )}
    </SavedPaperCard>
  );
};

// ─── Main EntryDetailScreen ───────────────────────────────────────────
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
      <div className="relative w-full" style={{ height: HERO_H + 16 }}>
        {/* White under-sheet */}
        <div className="absolute left-0 right-0 h-4 z-10" style={{ top: HERO_H - 4, background: isDark ? '#17131D' : '#FFFDF9' }}>
          <svg viewBox="0 0 400 16" preserveAspectRatio="none" className="w-full h-full">
            <path d="M0,0 Q18,10 36,3 T72,5 T108,2 T144,6 T180,1 T216,4 T252,2 T288,5 T324,1 T360,4 T400,2 L400,16 L0,16 Z"
              fill={heroFill} />
          </svg>
        </div>

        {/* Torn edge shadow */}
        <div className="absolute left-0 right-0 z-0" style={{ top: 1, height: HERO_H, background: isDark ? 'rgba(255,253,249,0.08)' : 'rgba(0,0,0,0.15)' }}>
          <svg viewBox="0 0 400 400" preserveAspectRatio="none" className="w-full h-full">
            <path d="M0,0 L400,0 L400,380 Q382,392 364,383 T346,386 T328,381 T310,388 T292,384 T274,387 T256,382 T238,386 T220,383 T202,389 T184,384 T166,387 T148,382 T130,386 T112,383 T94,389 T76,384 T58,387 T40,382 T22,386 T4,383 L0,380 Z"
              fill={isDark ? 'rgba(255,253,249,0.94)' : 'rgba(0,0,0,0.12)'} />
          </svg>
        </div>

        {/* Solid hero color + torn bottom */}
        <div className="absolute left-0 right-0 z-20" style={{ height: HERO_H, background: heroFill }}>
          <svg viewBox="0 0 400 400" preserveAspectRatio="none" className="absolute bottom-0 left-0 right-0" style={{ height: 20, transform: 'translateY(100%)' }}>
            <path d="M0,0 Q18,10 36,3 T72,5 T108,2 T144,6 T180,1 T216,4 T252,2 T288,5 T324,1 T360,4 T400,2 L400,20 L0,20 Z"
              fill={heroFill} />
          </svg>

          {/* Hero watermark symbols */}
          <div className="absolute inset-0 pointer-events-none">
            {['person', 'album', 'movie', 'edit_note', 'brush', 'science', 'casino', 'menu_book', 'palette', 'smart_display'].map((s, i) => {
              const x = [5, 88, 50, 92, 8, 55, 90, 15, 92, 10][i];
              const y = [10, 8, 25, 55, 60, 70, 75, 80, 45, 35][i];
              const r = [-8, 10, -5, 12, -10, -6, 8, -12, 7, -4][i];
              return <span key={i} className="material-symbols-outlined absolute select-none"
                style={{ left: `${x}%`, top: `${y}%`, fontSize: [36, 40, 44, 38, 42, 48, 40, 36, 42, 44][i],
                  color: 'rgba(255,255,255,0.12)', transform: `rotate(${r}deg)` }}>{s}</span>;
            })}
          </div>

          {/* Centered content */}
          <div className="relative z-10 flex flex-col items-center justify-center h-full px-6" style={{ paddingTop: 72 }}>
            <MaterialIcon name={category.iconGlyph} size={64} style={{ color: 'rgba(255,255,255,0.92)' }} />
            <div className="h-3" />
            <h1 className="text-2xl font-extrabold text-center leading-tight px-4"
              style={{ color: heroInk, fontFamily: 'Geom, Inter, sans-serif', maxWidth: 300 }}>
              {entry.topicName || 'Untitled'}
            </h1>
            <div className="h-4" />
            {/* Frosted date/mood/type bar */}
            <div className="flex items-center rounded-2xl px-4 py-2.5 gap-3"
              style={{ background: 'rgba(255,255,255,0.12)', backdropFilter: 'blur(8px)', border: '1px solid rgba(255,255,255,0.22)' }}>
              <FrostedSegment icon="calendar_today" title={formatDate(entry.capturedAtMillis)} subtitle="Date" tiny={tiny} />
              <div className="w-px h-8" style={{ background: 'rgba(255,255,255,0.25)' }} />
              <FrostedSegment icon="category" title={format} subtitle="Type" />
            </div>
          </div>
        </div>

        {/* Sticky back + more */}
        <div className="absolute top-0 left-0 right-0 z-30 flex justify-between px-4" style={{ paddingTop: 'env(safe-area-inset-top, 12px)' }}>
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
      </div>

      {/* ── Body ──────────────────────────────────────────────────────── */}
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

const FrostedSegment: React.FC<{ icon: string; title: string; subtitle: string; tiny?: string }> = ({ icon, title, subtitle, tiny }) => (
  <div className="flex flex-col items-center flex-1">
    <MaterialIcon name={icon} size={16} style={{ color: 'rgba(255,255,255,0.9)' }} />
    <span className="text-xs font-bold mt-0.5 text-white">{title}</span>
    <span className="text-[10px] text-white/80">{subtitle}</span>
    {tiny && <span className="text-[9px] text-white/70">{tiny}</span>}
  </div>
);

export default EntryDetailScreen;
