// Curio Web App - Save Capture Screen
// Matches Android: category tint wash, format chips, paper editors with Patrick Hand

import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useTheme, getBackgroundColor, getTextColor } from '../theme/ThemeContext';
import { getCategoryBySlug } from '../data/categories';
import { CurioPaperCard, CurioBackButton, MaterialIcon, CurioWatermarkBackdrop } from '../components/SharedComponents';
import type { CurioCategory, CaptureFormat, CaptureData } from '../types';
import { captureRepository, generateId, serializeTags } from '../db/database';
import { ScreenEntrance } from '../animations';

// ─── Paper input styles shared across format editors ──────────────────
const paperInputClass = "w-full bg-transparent resize-none outline-none placeholder:opacity-40";
const paperLabelClass = "block text-xs font-medium mb-1.5 opacity-50 uppercase tracking-wider";

// ─── Note paper ink color ────────────────────────────────────────────
const notePaperInk = (isDark: boolean) => isDark ? '#E4D2BC' : '#2D140F';

// ─── SoundBite Editor (Voice Note) ────────────────────────────────────
const SoundBiteEditor: React.FC<{
  data: CaptureData; onChange: (data: CaptureData) => void; isDark: boolean;
}> = ({ data, onChange, isDark }) => {
  const d = data as { durationSeconds: number; notes: string };
  return (
    <div className="space-y-5" style={{ fontFamily: "'Patrick Hand', cursive", color: notePaperInk(isDark) }}>
      <div>
        <label className={paperLabelClass} style={{ color: notePaperInk(isDark) }}>Duration (seconds)</label>
        <input type="number" value={d.durationSeconds}
          onChange={e => onChange({ ...d, durationSeconds: parseInt(e.target.value) || 0 })}
          className={`${paperInputClass} text-lg`} />
      </div>
      <div>
        <label className={paperLabelClass} style={{ color: notePaperInk(isDark) }}>Notes</label>
        <textarea value={d.notes} onChange={e => onChange({ ...d, notes: e.target.value })}
          placeholder="What did you hear?"
          rows={4} className={paperInputClass} style={{ fontSize: '1.1rem', lineHeight: '1.7' }} />
      </div>
    </div>
  );
};

// ─── ReelNotes Editor (Review) ────────────────────────────────────────
const ReelNotesEditor: React.FC<{
  data: CaptureData; onChange: (data: CaptureData) => void; isDark: boolean;
}> = ({ data, onChange, isDark }) => {
  const d = data as { rating: number; review: string };
  const ink = notePaperInk(isDark);
  return (
    <div className="space-y-5" style={{ fontFamily: "'Patrick Hand', cursive", color: ink }}>
      <div>
        <label className={paperLabelClass} style={{ color: ink }}>Rating</label>
        <div className="flex gap-1.5">
          {[1, 2, 3, 4, 5].map(star => (
            <button key={star} onClick={() => onChange({ ...d, rating: star })}
              className="transition-transform hover:scale-115 active:scale-90">
              <MaterialIcon name={star <= d.rating ? 'star' : 'star'} size={30}
                filled={star <= d.rating}
                style={{ color: star <= d.rating ? '#E8A838' : isDark ? 'rgba(255,255,255,0.15)' : 'rgba(0,0,0,0.10)' }} />
            </button>
          ))}
        </div>
      </div>
      <div>
        <label className={paperLabelClass} style={{ color: ink }}>Review</label>
        <textarea value={d.review} onChange={e => onChange({ ...d, review: e.target.value })}
          placeholder="Write your review here..."
          rows={6} className={paperInputClass} style={{ fontSize: '1.1rem', lineHeight: '1.7' }} />
      </div>
    </div>
  );
};

// ─── Marginalia Editor (Journal + Quotes) ─────────────────────────────
const MarginaliaEditor: React.FC<{
  data: CaptureData; onChange: (data: CaptureData) => void; isDark: boolean;
}> = ({ data, onChange, isDark }) => {
  const d = data as { journalEntry: string; quotes: Array<{ text: string; context?: string }> };
  const [newQuote, setNewQuote] = useState('');
  const ink = notePaperInk(isDark);

  const addQuote = () => {
    if (newQuote.trim()) {
      onChange({ ...d, quotes: [...d.quotes, { text: newQuote.trim() }] });
      setNewQuote('');
    }
  };

  return (
    <div className="space-y-5" style={{ fontFamily: "'Patrick Hand', cursive", color: ink }}>
      <div>
        <label className={paperLabelClass} style={{ color: ink }}>Journal Entry</label>
        <textarea value={d.journalEntry} onChange={e => onChange({ ...d, journalEntry: e.target.value })}
          placeholder="Write your thoughts about this topic..."
          rows={6} className={paperInputClass} style={{ fontSize: '1.1rem', lineHeight: '1.7' }} />
      </div>
      <div>
        <label className={paperLabelClass} style={{ color: ink }}>Favorite Quotes</label>
        {d.quotes.length > 0 && (
          <div className="space-y-2 mb-3">
            {d.quotes.map((q, i) => (
              <div key={i} className="flex items-start gap-2 pl-2 border-l-2" style={{ borderColor: isDark ? 'rgba(255,255,255,0.12)' : 'rgba(0,0,0,0.08)' }}>
                <span className="text-lg leading-none opacity-50">&ldquo;</span>
                <p className="flex-1 text-sm leading-relaxed">{q.text}</p>
                <button onClick={() => onChange({ ...d, quotes: d.quotes.filter((_, j) => j !== i) })}
                  className="text-red-400/60 hover:text-red-400 text-lg leading-none">&times;</button>
              </div>
            ))}
          </div>
        )}
        <div className="flex gap-2">
          <input type="text" value={newQuote} onChange={e => setNewQuote(e.target.value)}
            onKeyDown={e => e.key === 'Enter' && addQuote()}
            placeholder="Add a quote..."
            className={`${paperInputClass} flex-1 text-sm`}
            style={{ borderBottom: `1px solid ${isDark ? 'rgba(255,255,255,0.08)' : 'rgba(0,0,0,0.06)'}` }} />
          <button onClick={addQuote}
            className="px-3 py-1 rounded-full text-xs font-medium transition-opacity hover:opacity-80"
            style={{ background: 'rgba(0,0,0,0.06)', color: ink }}>Add</button>
        </div>
      </div>
    </div>
  );
};

// ─── GalleryWall Editor (Mood Board) ──────────────────────────────────
const GalleryWallEditor: React.FC<{
  data: CaptureData; onChange: (data: CaptureData) => void; isDark: boolean;
}> = ({ data, onChange, isDark }) => {
  const d = data as { caption: string; images: string[] };
  const ink = notePaperInk(isDark);
  return (
    <div className="space-y-5" style={{ fontFamily: "'Patrick Hand', cursive", color: ink }}>
      <div>
        <label className={paperLabelClass} style={{ color: ink }}>Caption</label>
        <textarea value={d.caption} onChange={e => onChange({ ...d, caption: e.target.value })}
          placeholder="Describe your mood board..."
          rows={3} className={paperInputClass} style={{ fontSize: '1.1rem', lineHeight: '1.7' }} />
      </div>
      <div>
        <label className={paperLabelClass} style={{ color: ink }}>Images</label>
        <div className="grid grid-cols-3 gap-2">
          {d.images.map((_, i) => (
            <div key={i} className="aspect-square rounded-lg flex items-center justify-center"
              style={{ background: isDark ? 'rgba(255,255,255,0.04)' : 'rgba(0,0,0,0.02)' }}>
              <MaterialIcon name="image" size={32} style={{ color: isDark ? 'rgba(255,255,255,0.2)' : 'rgba(0,0,0,0.15)' }} />
            </div>
          ))}
          <button className="aspect-square rounded-lg border-2 border-dashed flex items-center justify-center"
            style={{ borderColor: isDark ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.08)' }}>
            <MaterialIcon name="add" size={28} style={{ color: isDark ? 'rgba(255,255,255,0.25)' : 'rgba(0,0,0,0.2)' }} />
          </button>
        </div>
      </div>
    </div>
  );
};

// ─── Field Notes Editor ──────────────────────────────────────────────
const FieldNotesEditor: React.FC<{
  data: CaptureData; onChange: (data: CaptureData) => void; isDark: boolean;
}> = ({ data, onChange, isDark }) => {
  const d = data as { observed: string; surprised: string; learnNext: string };
  const ink = notePaperInk(isDark);
  const fields: Array<{ key: keyof typeof d; icon: string; label: string; placeholder: string }> = [
    { key: 'observed', icon: 'visibility', label: 'Observed', placeholder: 'What did you observe?' },
    { key: 'surprised', icon: 'sentiment_surprised', label: 'Surprised Me', placeholder: 'What surprised you?' },
    { key: 'learnNext', icon: 'menu_book', label: 'Want to Learn Next', placeholder: 'What do you want to learn more about?' },
  ];
  return (
    <div className="space-y-5" style={{ fontFamily: "'Patrick Hand', cursive", color: ink }}>
      {fields.map(f => (
        <div key={f.key}>
          <label className={paperLabelClass} style={{ color: ink }}>
            <MaterialIcon name={f.icon} size={14} style={{ verticalAlign: 'middle', marginRight: 4 }} />
            {f.label}
          </label>
          <textarea value={d[f.key]} onChange={e => onChange({ ...d, [f.key]: e.target.value })}
            placeholder={f.placeholder}
            rows={3} className={paperInputClass} style={{ fontSize: '1.1rem', lineHeight: '1.7' }} />
        </div>
      ))}
    </div>
  );
};

// ─── Format Chip Selector ─────────────────────────────────────────────
const FormatSelector: React.FC<{
  selected: CaptureFormat; onSelect: (f: CaptureFormat) => void;
  accent: string; tint?: string;
}> = ({ selected, onSelect, accent }) => {
  const formats: Array<{ id: CaptureFormat; label: string; icon: string }> = [
    { id: 'SoundBite', label: 'Sound Bite', icon: 'mic' },
    { id: 'ReelNotes', label: 'Reel Notes', icon: 'movie' },
    { id: 'Marginalia', label: 'Marginalia', icon: 'edit_note' },
    { id: 'GalleryWall', label: 'Gallery Wall', icon: 'image' },
    { id: 'FieldNotes', label: 'Field Notes', icon: 'description' },
  ];
  return (
    <div className="flex gap-2 overflow-x-auto pb-1">
      {formats.map(({ id, label, icon }) => (
        <button key={id} onClick={() => onSelect(id)}
          className="flex-shrink-0 flex items-center gap-1.5 px-3.5 py-2 rounded-full text-xs font-semibold transition-all"
          style={{
            background: selected === id ? accent : `${accent}18`,
            color: selected === id ? 'white' : accent,
            border: selected === id ? 'none' : `1px solid ${accent}30`,
          }}>
          <MaterialIcon name={icon} size={16} />
          {label}
        </button>
      ))}
    </div>
  );
};

// ─── Main SaveCaptureScreen ───────────────────────────────────────────
export const SaveCaptureScreen: React.FC = () => {
  const navigate = useNavigate();
  const { categorySlug, topicName } = useParams();
  const { isDark, isAmoled } = useTheme();
  const [category, setCategory] = useState<CurioCategory | null>(null);
  const [selectedFormat, setSelectedFormat] = useState<CaptureFormat>('Marginalia');
  const [captureData, setCaptureData] = useState<CaptureData | null>(null);
  const [tags, setTags] = useState<string[]>([]);
  const [tagInput, setTagInput] = useState('');
  const [isSaving, setIsSaving] = useState(false);
  const [showSuccess, setShowSuccess] = useState(false);

  useEffect(() => {
    if (categorySlug) {
      const found = getCategoryBySlug(categorySlug);
      if (found) {
        setCategory(found);
        setSelectedFormat(found.defaultFormat === 'OpenNotebook' ? 'SoundBite' : found.defaultFormat);
        initData(found.defaultFormat === 'OpenNotebook' ? 'SoundBite' : found.defaultFormat);
      }
    }
  }, [categorySlug]);

  const initData = (fmt: CaptureFormat) => {
    const defaults: Record<string, CaptureData> = {
      SoundBite: { durationSeconds: 0, notes: '' },
      ReelNotes: { rating: 0, review: '' },
      Marginalia: { journalEntry: '', quotes: [] },
      GalleryWall: { caption: '', images: [] },
      FieldNotes: { observed: '', surprised: '', learnNext: '' },
    };
    setCaptureData(defaults[fmt] || defaults.Marginalia);
  };

  const handleFormatChange = (fmt: CaptureFormat) => {
    setSelectedFormat(fmt);
    initData(fmt);
  };

  const handleAddTag = () => {
    const clean = tagInput.trim().replace(/^#/, '');
    if (clean && clean.length <= 24 && tags.length < 12 && !tags.includes(clean)) {
      setTags([...tags, clean]);
      setTagInput('');
    }
  };

  const canSave = (): boolean => {
    if (!captureData) return false;
    switch (selectedFormat) {
      case 'SoundBite': {
        const d = captureData as { notes: string };
        return !!d.notes;
      }
      case 'ReelNotes': {
        const d = captureData as { rating: number; review: string };
        return !!d.review || d.rating > 0;
      }
      case 'Marginalia': {
        const d = captureData as { journalEntry: string; quotes: Array<{ text: string }> };
        return !!d.journalEntry || d.quotes.length > 0;
      }
      case 'GalleryWall': {
        const d = captureData as { caption: string };
        return !!d.caption;
      }
      case 'FieldNotes': {
        const d = captureData as { observed: string; surprised: string; learnNext: string };
        return !!d.observed || !!d.surprised || !!d.learnNext;
      }
      default: return false;
    }
  };

  const handleSave = async () => {
    if (!category || !captureData) return;
    setIsSaving(true);
    try {
      const topicDisplayName = topicName?.replace(/-/g, ' ') || 'Unknown Topic';
      const entry = {
        id: generateId(),
        topicId: `${category.id.toLowerCase()}-${Date.now()}`,
        categoryId: category.id,
        topicName: topicDisplayName,
        topicSubtype: 'Topic',
        topicTeaser: 'A fascinating topic to explore.',
        format: selectedFormat,
        capturedAtMillis: Date.now(),
        title: topicDisplayName,
        formatDataJson: JSON.stringify(captureData),
        tagsJson: serializeTags(tags),
        isLegacy: false,
      };
      await captureRepository.insert(entry);
      const prev = parseInt(localStorage.getItem('curio-total-topics') || '0');
      localStorage.setItem('curio-total-topics', String(prev + 1));
      const xp = parseInt(localStorage.getItem('curio-xp') || '0');
      localStorage.setItem('curio-xp', String(xp + 50));
      setShowSuccess(true);
      setTimeout(() => navigate(`/detail/${entry.id}`), 1500);
    } catch (err) {
      console.error('Save failed:', err);
    } finally {
      setIsSaving(false);
    }
  };

  if (!category) return (
    <div className="min-h-screen flex items-center justify-center" style={{ backgroundColor: getBackgroundColor(isDark, isAmoled) }}>
      <div className="w-8 h-8 border-4 border-t-transparent rounded-full animate-spin" style={{ borderColor: '#FF8FA3', borderTopColor: 'transparent' }} />
    </div>
  );

  const tintBg = `${category.accent}0E`;

  return (
    <div className="min-h-screen pb-24 relative" style={{ background: isDark ? `linear-gradient(180deg, ${tintBg}, ${getBackgroundColor(isDark, isAmoled)})` : `linear-gradient(180deg, ${category.tint || tintBg}, ${getBackgroundColor(isDark, isAmoled)})` }}>
      <CurioWatermarkBackdrop activeCatId={category.id} alphaScale={0.35} />

      <div className="relative z-10">
        {/* Top bar */}
        <div className="flex items-center justify-between px-4 pt-4 pb-1" style={{ paddingTop: 'env(safe-area-inset-top, 16px)' }}>
          <CurioBackButton onClick={() => navigate(-1)} />
          <h1 className="text-base font-bold" style={{ color: getTextColor(isDark), fontFamily: 'Geom, Inter, sans-serif' }}>
            Save your take
          </h1>
          <div className="w-10" />
        </div>
        <ScreenEntrance>
        {/* Topic reminder strip */}
        <div className="px-4 pb-3">
          <div className="flex items-center gap-3 px-4 py-3 rounded-2xl"
            style={{
              background: `${category.accent}14`,
              border: `1px solid ${category.accent}22`,
            }}>
            <div className="w-10 h-10 rounded-xl flex items-center justify-center" style={{ background: `${category.accent}20` }}>
              <MaterialIcon name={category.iconGlyph} size={22} style={{ color: category.accent }} />
            </div>
            <div>
              <p className="text-sm font-bold" style={{ color: getTextColor(isDark) }}>
                {topicName?.replace(/-/g, ' ') || 'Unknown Topic'}
              </p>
              <p className="text-xs opacity-60" style={{ color: getTextColor(isDark) }}>{category.displayName}</p>
            </div>
          </div>
        </div>

        {/* Format selector */}
        <div className="px-4 py-2">
          <p className="text-xs font-semibold mb-2 opacity-60" style={{ color: getTextColor(isDark) }}>
            How do you want to capture this one?
          </p>
          <FormatSelector selected={selectedFormat} onSelect={handleFormatChange}
            accent={category.accent} />
        </div>

        {/* Format editor on paper */}
        <div className="px-4 py-3 max-w-lg mx-auto">
          {captureData && (
            <CurioPaperCard variant="ruled" watermark={category.iconGlyph} accent={category.accent}>
              {selectedFormat === 'SoundBite' && <SoundBiteEditor data={captureData} onChange={setCaptureData} isDark={isDark} />}
              {selectedFormat === 'ReelNotes' && <ReelNotesEditor data={captureData} onChange={setCaptureData} isDark={isDark} />}
              {selectedFormat === 'Marginalia' && <MarginaliaEditor data={captureData} onChange={setCaptureData} isDark={isDark} />}
              {selectedFormat === 'GalleryWall' && <GalleryWallEditor data={captureData} onChange={setCaptureData} isDark={isDark} />}
              {selectedFormat === 'FieldNotes' && <FieldNotesEditor data={captureData} onChange={setCaptureData} isDark={isDark} />}
            </CurioPaperCard>
          )}
        </div>

        {/* Tags */}
        <div className="px-4 py-2 max-w-lg mx-auto">
          <p className="text-xs font-semibold mb-2 opacity-60" style={{ color: getTextColor(isDark) }}>Tags</p>
          {tags.length > 0 && (
            <div className="flex flex-wrap gap-1.5 mb-2">
              {tags.map(tag => (
                <span key={tag} className="flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-medium"
                  style={{ background: `${category.accent}18`, color: category.accent }}>
                  #{tag}
                  <button onClick={() => setTags(tags.filter(t => t !== tag))} className="hover:opacity-70">&times;</button>
                </span>
              ))}
            </div>
          )}
          <div className="flex gap-2">
            <input type="text" value={tagInput} onChange={e => setTagInput(e.target.value)}
              onKeyDown={e => e.key === 'Enter' && handleAddTag()}
              placeholder="Add a tag…" maxLength={24}
              className="flex-1 px-3 py-2 rounded-full text-sm outline-none"
              style={{ background: isDark ? 'rgba(255,255,255,0.06)' : 'rgba(0,0,0,0.03)', color: getTextColor(isDark),
                border: `1px solid ${isDark ? 'rgba(255,255,255,0.08)' : 'rgba(0,0,0,0.06)'}` }} />
            <button onClick={handleAddTag}
              className="px-4 py-2 rounded-full text-xs font-semibold transition-opacity hover:opacity-90"
              style={{ background: category.accent, color: 'white' }}>Add</button>
          </div>
        </div>

        {/* Save button */}
        <div className="px-4 py-4 max-w-lg mx-auto">
          <button onClick={handleSave} disabled={!canSave() || isSaving}
            className="w-full py-4 rounded-2xl font-bold text-base transition-all disabled:opacity-40 active:scale-[0.98]"
            style={{
              background: canSave()
                ? category.accent
                : isDark ? 'rgba(255,255,255,0.08)' : 'rgba(0,0,0,0.04)',
              color: canSave() ? 'white' : getTextColor(isDark),
            }}>
            {isSaving ? (
              <span className="flex items-center justify-center gap-2">
                <span className="w-5 h-5 border-2 border-t-transparent rounded-full animate-spin" style={{ borderColor: 'rgba(255,255,255,0.6)', borderTopColor: 'transparent' }} />
                Saving…
              </span>
            ) : (
              <span className="flex items-center justify-center gap-2">
                <MaterialIcon name="check" size={20} />
                Save entry
              </span>
            )}
          </button>
        </div>
        </ScreenEntrance>
      </div>

      {/* Success */}
      {showSuccess && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-[70]">
          <div className="p-8 rounded-3xl text-center mx-6" style={{ background: isDark ? '#1a1a2e' : 'white' }}>
            <MaterialIcon name="celebration" size={64} style={{ color: '#FF8FA3' }} />
            <h2 className="text-xl font-bold mt-3 mb-1" style={{ color: getTextColor(isDark) }}>Saved!</h2>
            <p className="text-sm opacity-60" style={{ color: getTextColor(isDark) }}>
              Your entry has been added to your cabinet.
            </p>
          </div>
        </div>
      )}
    </div>
  );
};

export default SaveCaptureScreen;
