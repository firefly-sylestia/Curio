// Curio Web App - Journal
// One quiet page for writing freely and attaching photographs.

import React, { useEffect, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useTheme, getBackgroundColor, getTextColor } from '../theme/ThemeContext';
import { getCategoryBySlug } from '../data/categories';
import { loadTopicsForCategory } from '../data/topics';
import { MaterialIcon } from '../components/SharedComponents';
import type { CurioCategory, CurioTopic } from '../types';
import { captureRepository, generateId, serializeTags } from '../db/database';

const ACCENT = '#DA1E37';
const MAX_IMAGE_EDGE = 1800;
const MAX_PHOTOS = 8;

const compressPhoto = (file: File): Promise<string> => new Promise((resolve, reject) => {
  const reader = new FileReader();
  reader.onerror = () => reject(new Error('Could not read this photo.'));
  reader.onload = () => {
    const image = new Image();
    image.onerror = () => reject(new Error('This photo could not be opened.'));
    image.onload = () => {
      const scale = Math.min(1, MAX_IMAGE_EDGE / Math.max(image.width, image.height));
      const canvas = document.createElement('canvas');
      canvas.width = Math.max(1, Math.round(image.width * scale));
      canvas.height = Math.max(1, Math.round(image.height * scale));
      const context = canvas.getContext('2d');
      if (!context) { resolve(String(reader.result)); return; }
      context.drawImage(image, 0, 0, canvas.width, canvas.height);
      resolve(canvas.toDataURL('image/jpeg', 0.84));
    };
    image.src = String(reader.result);
  };
  reader.readAsDataURL(file);
});

export const SaveCaptureScreen: React.FC = () => {
  const navigate = useNavigate();
  const { categorySlug, topicName } = useParams();
  const { isDark, isAmoled } = useTheme();
  const [category, setCategory] = useState<CurioCategory | null>(null);
  const [topic, setTopic] = useState<CurioTopic | null>(null);
  const [note, setNote] = useState('');
  const [photos, setPhotos] = useState<string[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [saved, setSaved] = useState(false);
  const fileInput = useRef<HTMLInputElement>(null);

  const dark = isDark || isAmoled;
  const ink = getTextColor(isDark);
  const muted = dark ? 'rgba(245,243,244,.56)' : 'rgba(23,27,38,.56)';
  const surface = dark ? '#161A1D' : '#FFFFFF';
  const border = dark ? 'rgba(255,255,255,.10)' : 'rgba(23,27,38,.10)';
  const topicTitle = topic?.name || topicName?.replace(/-/g, ' ') || 'A new discovery';

  useEffect(() => {
    let active = true;
    const load = async () => {
      setLoading(true);
      const found = categorySlug ? getCategoryBySlug(categorySlug) : null;
      if (!found) { if (active) setLoading(false); return; }
      const allTopics = await loadTopicsForCategory(found.id);
      if (!active) return;
      setCategory(found);
      setTopic(allTopics.find(item => item.id === topicName) || null);
      setLoading(false);
    };
    load();
    return () => { active = false; };
  }, [categorySlug, topicName]);

  const importPhotos = async (files: FileList | null) => {
    if (!files) return;
    setError('');
    const remaining = MAX_PHOTOS - photos.length;
    const selected = Array.from(files).filter(file => file.type.startsWith('image/')).slice(0, remaining);
    if (!selected.length) {
      if (remaining <= 0) setError(`You can add up to ${MAX_PHOTOS} photos.`);
      return;
    }
    try {
      const imported = await Promise.all(selected.map(compressPhoto));
      setPhotos(current => [...current, ...imported].slice(0, MAX_PHOTOS));
    } catch {
      setError('One of those photos could not be imported. Please try another image.');
    }
    if (fileInput.current) fileInput.current.value = '';
  };

  const saveJournal = async () => {
    if (!category || saving || (!note.trim() && photos.length === 0)) return;
    setSaving(true);
    setError('');
    try {
      const entry = {
        id: generateId(),
        topicId: topic?.id || `${category.id.toLowerCase()}-${Date.now()}`,
        categoryId: category.id,
        topicName: topicTitle,
        topicSubtype: topic?.subtype || 'Topic',
        topicTeaser: topic?.teaser || 'A discovery worth remembering.',
        format: 'GalleryWall' as const,
        capturedAtMillis: Date.now(),
        title: topicTitle,
        formatDataJson: JSON.stringify({ caption: note.trim(), images: photos, mood: null }),
        tagsJson: serializeTags([]),
        isLegacy: false,
        sessionTimeMillis: 0,
      };
      await captureRepository.insert(entry);
      const total = Number.parseInt(localStorage.getItem('curio-total-topics') || '0', 10);
      localStorage.setItem('curio-total-topics', String(total + 1));
      const xp = Number.parseInt(localStorage.getItem('curio-xp') || '0', 10);
      localStorage.setItem('curio-xp', String(xp + 50));
      setSaved(true);
      window.setTimeout(() => navigate(`/detail/${entry.id}`), 900);
    } catch (saveError) {
      console.error('Journal save failed:', saveError);
      setError('Your page could not be saved. Try removing a photo and saving again.');
    } finally {
      setSaving(false);
    }
  };

  if (loading || !category) {
    return (
      <div className="min-h-screen flex items-center justify-center" style={{ background: getBackgroundColor(isDark, isAmoled) }}>
        {loading ? <div className="w-7 h-7 rounded-full border-[3px] border-t-transparent animate-spin" style={{ borderColor: ACCENT, borderTopColor: 'transparent' }} /> :
          <p className="text-sm" style={{ color: muted }}>This journal page is unavailable.</p>}
      </div>
    );
  }

  return (
    <div className="curio-journal-screen min-h-screen relative pb-10" style={{ background: getBackgroundColor(isDark, isAmoled), color: ink }}>
      <div className="max-w-2xl mx-auto px-5 relative z-10">
        <header className="flex items-center justify-between pt-4 pb-7" style={{ paddingTop: 'max(16px, env(safe-area-inset-top))' }}>
          <button onClick={() => navigate(-1)} aria-label="Go back" className="w-10 h-10 rounded-full flex items-center justify-center"
            style={{ background: dark ? 'rgba(255,255,255,.07)' : 'rgba(23,27,38,.055)', border: `1px solid ${border}` }}>
            <MaterialIcon name="arrow_back" size={20} />
          </button>
          <div className="text-[10px] uppercase tracking-[.2em] font-bold" style={{ color: muted }}>Curio journal</div>
          <div className="w-10" />
        </header>

        <div className="mb-5">
          <div className="flex items-center gap-2 text-xs font-semibold" style={{ color: muted }}>
            <span className="w-2 h-2 rounded-full" style={{ background: category.accent }} />
            {category.displayName}{topic ? ` · ${topic.subtype}` : ''}
          </div>
          <h1 className="mt-3 text-[30px] sm:text-[36px] leading-tight font-extrabold tracking-tight" style={{ fontFamily: 'Geom, Inter, sans-serif' }}>
            Keep this thought<span style={{ color: ACCENT }}>.</span>
          </h1>
          <p className="mt-2 text-sm" style={{ color: muted }}>A page for your words, with room for a photograph.</p>
        </div>

        <section className="rounded-[22px] overflow-hidden" style={{ background: surface, border: `1px solid ${border}`, boxShadow: '0 20px 50px -42px rgba(0,0,0,.65)' }}>
          <div className="px-5 pt-5 pb-3 flex items-center justify-between" style={{ borderBottom: `1px solid ${border}` }}>
            <div>
              <div className="text-[10px] uppercase tracking-[.17em] font-bold" style={{ color: ACCENT }}>Today's page</div>
              <div className="mt-1 text-sm font-bold" style={{ color: ink }}>{topicTitle}</div>
            </div>
            <MaterialIcon name="edit_note" size={22} style={{ color: muted }} />
          </div>
          <div className="p-5">
            <textarea value={note} onChange={event => setNote(event.target.value)}
              placeholder="Start anywhere… What caught your attention? What did it make you think or feel?"
              rows={12} autoFocus={false}
              className="w-full min-h-[280px] resize-y outline-none bg-transparent text-[16px] leading-[1.9]"
              style={{ color: ink, caretColor: ACCENT }}
            />
            <div className="flex items-center justify-between pt-3 text-[10px]" style={{ borderTop: `1px solid ${border}`, color: muted }}>
              <span>{note.trim() ? `${note.trim().split(/\\s+/).length} words` : 'Your private space to write'}</span>
              <span>{new Date().toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' })}</span>
            </div>
          </div>
        </section>

        <section className="mt-5 rounded-[22px] p-5" style={{ background: surface, border: `1px solid ${border}` }}>
          <div className="flex items-center justify-between gap-3">
            <div>
              <h2 className="text-sm font-bold" style={{ color: ink }}>Photographs</h2>
              <p className="mt-1 text-xs" style={{ color: muted }}>Add up to {MAX_PHOTOS} images to this page.</p>
            </div>
            <button onClick={() => fileInput.current?.click()} disabled={photos.length >= MAX_PHOTOS}
              className="shrink-0 flex items-center gap-2 px-4 py-2.5 rounded-xl text-xs font-bold disabled:opacity-40"
              style={{ background: 'rgba(218,30,55,.10)', color: ACCENT }}>
              <MaterialIcon name="add_photo_alternate" size={17} /> Add photos
            </button>
            <input ref={fileInput} type="file" accept="image/*" multiple className="hidden" onChange={event => importPhotos(event.target.files)} />
          </div>
          {photos.length > 0 && (
            <div className="mt-4 grid grid-cols-2 sm:grid-cols-3 gap-2">
              {photos.map((photo, index) => (
                <div key={index} className="relative aspect-square rounded-xl overflow-hidden" style={{ background: dark ? '#242A30' : '#EEF0F4' }}>
                  <img src={photo} alt={`Journal attachment ${index + 1}`} className="w-full h-full object-cover" />
                  <button onClick={() => setPhotos(current => current.filter((_, i) => i !== index))}
                    aria-label={`Remove photo ${index + 1}`} className="absolute top-2 right-2 w-8 h-8 rounded-full flex items-center justify-center text-white"
                    style={{ background: 'rgba(0,0,0,.62)' }}>
                    <MaterialIcon name="close" size={16} />
                  </button>
                </div>
              ))}
            </div>
          )}
        </section>

        {error && <p className="mt-4 text-sm" role="alert" style={{ color: ACCENT }}>{error}</p>}

        <button onClick={saveJournal} disabled={saving || saved || (!note.trim() && photos.length === 0)}
          className="mt-6 w-full h-14 rounded-2xl flex items-center justify-center gap-2 text-sm font-bold text-white disabled:opacity-40"
          style={{ background: 'linear-gradient(135deg, #E12640, #A9132B)', boxShadow: '0 12px 26px -16px rgba(218,30,55,.75)' }}>
          <MaterialIcon name={saved ? 'check' : 'bookmark'} size={19} />
          {saving ? 'Saving page…' : saved ? 'Saved' : 'Save journal page'}
        </button>
      </div>

      {saved && (
        <div className="fixed inset-0 z-[80] flex items-center justify-center bg-black/45 p-6">
          <div className="w-full max-w-xs rounded-3xl p-7 text-center" style={{ background: surface, color: ink }}>
            <div className="w-14 h-14 rounded-2xl mx-auto flex items-center justify-center" style={{ background: 'rgba(218,30,55,.10)', color: ACCENT }}>
              <MaterialIcon name="check" size={28} />
            </div>
            <h2 className="mt-4 text-lg font-bold">Page saved</h2>
            <p className="mt-1 text-sm" style={{ color: muted }}>Your thought has a place in Curio.</p>
          </div>
        </div>
      )}
    </div>
  );
};

export default SaveCaptureScreen;
