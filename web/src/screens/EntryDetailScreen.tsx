// Curio Web App - Entry Detail Screen
// Displays saved capture details with edit and delete options

import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useTheme, getBackgroundColor, getTextColor } from '../theme/ThemeContext';
import { getCategoryById } from '../data/categories';
import { CurioPaperCard, CurioMoodboardCard, CurioBackButton, MaterialIcon, CurioWatermarkBackdrop } from '../components/SharedComponents';
import type { CurioEntry, CaptureData, CaptureFormat } from '../types';
import { captureRepository, deserializeTags } from '../db/database';

// Format-specific renderers

// Note paper ink for saved views
const getPaperInk = (isDark: boolean) => isDark ? 'rgba(228,210,188,0.92)' : 'rgba(45,20,15,0.92)';

// Theme-aware inner card helper
const InnerBox: React.FC<{ children: React.ReactNode; isDark: boolean }> = ({ children, isDark }) => {
  return (
    <div
      className="p-4 rounded-[16px]"
      style={{
        background: isDark ? 'rgba(255,255,255,0.04)' : 'rgba(0,0,0,0.02)',
        fontFamily: "'Patrick Hand', cursive",
        color: getPaperInk(isDark),
      }}
    >
      {children}
    </div>
  );
};

const SoundBiteRenderer: React.FC<{ data: CaptureData; isDark: boolean }> = ({ data, isDark }) => {
  const soundData = data as { durationSeconds: number; notes: string };
  const ink = getPaperInk(isDark);
  return (
    <div className="space-y-4" style={{ fontFamily: "'Patrick Hand', cursive", color: ink }}>
      <div className="flex items-center gap-3">
        <MaterialIcon name="mic" size={32} style={{ color: ink }} />
        <div>
          <div className="font-semibold text-lg" style={{ color: ink }}>Voice Note</div>
          <div className="text-sm opacity-50">{soundData.durationSeconds}s</div>
        </div>
      </div>
      {soundData.notes && (
        <InnerBox isDark={isDark}>
          <p className="whitespace-pre-wrap">{soundData.notes}</p>
        </InnerBox>
      )}
    </div>
  );
};

const ReelNotesRenderer: React.FC<{ data: CaptureData; isDark: boolean }> = ({ data, isDark }) => {
  const reelData = data as { rating: number; review: string };
  const ink = getPaperInk(isDark);
  return (
    <div className="space-y-4" style={{ fontFamily: "'Patrick Hand', cursive", color: ink }}>
      {reelData.rating > 0 && (
        <div className="flex gap-1">
          {[1,2,3,4,5].map(i => (
            <MaterialIcon key={i} name="star" size={28} filled={i <= reelData.rating}
              style={{ color: i <= reelData.rating ? '#E8A838' : isDark ? 'rgba(255,255,255,0.15)' : 'rgba(0,0,0,0.10)' }} />
          ))}
        </div>
      )}
      {reelData.review && (
        <InnerBox isDark={isDark}>
          <p className="whitespace-pre-wrap" style={{ fontSize: '1.1rem', lineHeight: 1.7 }}>{reelData.review}</p>
        </InnerBox>
      )}
    </div>
  );
};

const MarginaliaRenderer: React.FC<{ data: CaptureData; isDark: boolean; accent: string }> = ({ data, isDark, accent }) => {
  const marginaliaData = data as { journalEntry: string; quotes: Array<{ text: string; context?: string }> };
  const ink = getPaperInk(isDark);
  return (
    <div className="space-y-4" style={{ fontFamily: "'Patrick Hand', cursive", color: ink }}>
      {marginaliaData.journalEntry && (
        <InnerBox isDark={isDark}>
          <p className="whitespace-pre-wrap" style={{ fontSize: '1.1rem', lineHeight: 1.7 }}>{marginaliaData.journalEntry}</p>
        </InnerBox>
      )}
      {marginaliaData.quotes.length > 0 && (
        <div className="space-y-3">
          <h4 className="font-semibold text-sm opacity-60" style={{ color: ink }}>Favorite Quotes</h4>
          {marginaliaData.quotes.map((quote, index) => (
            <div key={index} className="p-4 rounded-[16px] border-l-4"
              style={{ borderColor: accent, background: isDark ? 'rgba(255,255,255,0.04)' : 'rgba(0,0,0,0.02)' }}>
              <p className="text-lg leading-relaxed">&ldquo;{quote.text}&rdquo;</p>
              {quote.context && (
                <p className="text-sm mt-2 opacity-50">— {quote.context}</p>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

const GalleryWallRenderer: React.FC<{ data: CaptureData; isDark: boolean }> = ({ data, isDark }) => {
  const galleryData = data as { caption: string; images: string[] };
  const ink = getPaperInk(isDark);
  return (
    <div className="space-y-4" style={{ fontFamily: "'Patrick Hand', cursive", color: ink }}>
      {galleryData.caption && (
        <InnerBox isDark={isDark}>
          <p className="whitespace-pre-wrap" style={{ fontSize: '1.1rem', lineHeight: 1.7 }}>{galleryData.caption}</p>
        </InnerBox>
      )}
      {galleryData.images.length > 0 && (
        <div className="grid grid-cols-2 gap-2">
          {galleryData.images.map((_, index) => (
            <div key={index} className="aspect-square rounded-[12px] flex items-center justify-center"
              style={{ background: isDark ? 'rgba(255,255,255,0.04)' : 'rgba(0,0,0,0.02)' }}>
              <MaterialIcon name="image" size={32} style={{ color: isDark ? 'rgba(255,255,255,0.2)' : 'rgba(0,0,0,0.15)' }} />
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

const FieldNotesRenderer: React.FC<{ data: CaptureData; isDark: boolean }> = ({ data, isDark }) => {
  const fieldData = data as { observed: string; surprised: string; learnNext: string };
  const ink = getPaperInk(isDark);
  return (
    <div className="space-y-4" style={{ fontFamily: "'Patrick Hand', cursive", color: ink }}>
      {fieldData.observed && (
        <InnerBox isDark={isDark}>
          <div className="font-semibold text-sm mb-2 flex items-center gap-1.5 opacity-60" style={{ color: ink }}>
            <MaterialIcon name="visibility" size={16} /> Observed
          </div>
          <p className="whitespace-pre-wrap" style={{ fontSize: '1.1rem', lineHeight: 1.7 }}>{fieldData.observed}</p>
        </InnerBox>
      )}
      {fieldData.surprised && (
        <InnerBox isDark={isDark}>
          <div className="font-semibold text-sm mb-2 flex items-center gap-1.5 opacity-60" style={{ color: ink }}>
            <MaterialIcon name="sentiment_surprised" size={16} /> Surprised Me
          </div>
          <p className="whitespace-pre-wrap" style={{ fontSize: '1.1rem', lineHeight: 1.7 }}>{fieldData.surprised}</p>
        </InnerBox>
      )}
      {fieldData.learnNext && (
        <InnerBox isDark={isDark}>
          <div className="font-semibold text-sm mb-2 flex items-center gap-1.5 opacity-60" style={{ color: ink }}>
            <MaterialIcon name="menu_book" size={16} /> Want to Learn Next
          </div>
          <p className="whitespace-pre-wrap" style={{ fontSize: '1.1rem', lineHeight: 1.7 }}>{fieldData.learnNext}</p>
        </InnerBox>
      )}
    </div>
  );
};

// Format renderer dispatcher
const FormatRenderer: React.FC<{ format: CaptureFormat; data: CaptureData; isDark: boolean; accent: string }> = ({ format, data, isDark, accent }) => {
  switch (format) {
    case 'SoundBite':
      return <SoundBiteRenderer data={data} isDark={isDark} />;
    case 'ReelNotes':
      return <ReelNotesRenderer data={data} isDark={isDark} />;
    case 'Marginalia':
      return <MarginaliaRenderer data={data} isDark={isDark} accent={accent} />;
    case 'GalleryWall':
      return <GalleryWallRenderer data={data} isDark={isDark} />;
    case 'FieldNotes':
      return <FieldNotesRenderer data={data} isDark={isDark} />;
    default:
      return (
        <div
          className="p-4 rounded-[16px]"
          style={{ background: isDark ? 'rgba(255,255,255,0.06)' : 'rgba(59,10,23,0.03)' }}
        >
          Format not supported
        </div>
      );
  }
};

// Format icon helper
const getFormatIcon = (format: CaptureFormat): string => {
  switch (format) {
    case 'SoundBite': return 'mic';
    case 'ReelNotes': return 'movie';
    case 'Marginalia': return 'edit_note';
    case 'GalleryWall': return 'image';
    case 'FieldNotes': return 'description';
    case 'OpenNotebook': return 'menu_book';
    default: return 'article';
  }
};

export const EntryDetailScreen: React.FC = () => {
  const navigate = useNavigate();
  const { entryId } = useParams();
  const { isDark, isAmoled } = useTheme();
  const [entry, setEntry] = useState<CurioEntry | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);

  useEffect(() => {
    loadEntry();
  }, [entryId]);

  const loadEntry = async () => {
    if (!entryId) return;
    
    setIsLoading(true);
    try {
      const entity = await captureRepository.getById(entryId);
      if (entity) {
        const parsedEntry: CurioEntry = {
          id: entity.id,
          topic: {
            id: entity.topicId,
            categoryId: entity.categoryId as any,
            subtype: entity.topicSubtype,
            name: entity.topicName,
            teaser: entity.topicTeaser,
            imageUrl: '',
            actionPrompt: { verb: '', targetName: '', durationMinutes: 0, instruction: '' },
            aliases: [],
            relatedTopicIds: [],
            difficulty: 1,
            weight: 100,
            curatedBy: 'human',
            curatedDate: '',
          },
          format: entity.format,
          captureData: JSON.parse(entity.formatDataJson),
          title: entity.title,
          capturedAtMillis: entity.capturedAtMillis,
          tags: deserializeTags(entity.tagsJson),
          isLegacy: entity.isLegacy,
        };
        setEntry(parsedEntry);
      }
    } catch (error) {
      console.error('Failed to load entry:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleDelete = async () => {
    if (!entry) return;
    
    try {
      await captureRepository.delete(entry.id);
      
      // Update stats
      const currentEntries = parseInt(localStorage.getItem('curio-total-topics') || '0');
      localStorage.setItem('curio-total-topics', Math.max(0, currentEntries - 1).toString());
      
      navigate('/cabinet');
    } catch (error) {
      console.error('Failed to delete entry:', error);
    }
  };

  const formatDate = (millis: number) => {
    return new Date(millis).toLocaleDateString('en-US', {
      weekday: 'long',
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  if (isLoading) {
    return (
      <div
        className="min-h-screen flex items-center justify-center"
        style={{ backgroundColor: getBackgroundColor(isDark, isAmoled) }}
      >
        <div className="text-center">
          <div className="w-8 h-8 border-4 border-t-transparent rounded-full animate-spin mx-auto" style={{ borderColor: '#FF8FA3', borderTopColor: 'transparent' }} />
          <p className="mt-4" style={{ color: isDark ? 'rgba(255,255,255,0.5)' : 'rgba(59,10,23,0.5)' }}>
            Loading entry...
          </p>
        </div>
      </div>
    );
  }

  if (!entry) {
    return (
      <div
        className="min-h-screen flex items-center justify-center"
        style={{ backgroundColor: getBackgroundColor(isDark, isAmoled) }}
      >
        <div className="text-center">
          <MaterialIcon name="edit_note" size={64} />
          <h2 className="text-xl font-bold mt-4" style={{ color: getTextColor(isDark) }}>
            Entry not found
          </h2>
          <button
            onClick={() => navigate('/cabinet')}
            className="mt-4 px-6 py-2 rounded-full"
            style={{ background: '#FF8FA3', color: 'white' }}
          >
            Back to Cabinet
          </button>
        </div>
      </div>
    );
  }

  const category = getCategoryById(entry.topic.categoryId);
  const captureData = entry.captureData;

  return (
    <div
      className="min-h-screen pb-24 relative"
      style={{ backgroundColor: getBackgroundColor(isDark, isAmoled) }}
    >
      <CurioWatermarkBackdrop activeCatId={category.id} alphaScale={0.45} />
      <div className="relative z-10">
      {/* Header */}
      <header className="px-6 pt-6 pb-4 flex items-center justify-between">
        <CurioBackButton onClick={() => navigate(-1)} />
        <div className="flex items-center gap-2">
          <button
            onClick={() => navigate(`/capture/${category.id.toLowerCase()}/${entry.topic.name.toLowerCase().replace(/\s+/g, '-')}`)}
            className="w-10 h-10 rounded-full flex items-center justify-center"
            style={{
              background: isDark ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.05)',
            }}
          >
            <MaterialIcon name="edit" size={20} style={{ color: getTextColor(isDark) }} />
          </button>
          <button
            onClick={() => setShowDeleteConfirm(true)}
            className="w-10 h-10 rounded-full flex items-center justify-center"
            style={{
              background: isDark ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.05)',
            }}
          >
            <MaterialIcon name="delete" size={20} style={{ color: '#EF4444' }} />
          </button>
        </div>
      </header>

      {/* Topic Header - Moodboard style */}
      <div className="px-5 pt-2 pb-4 max-w-lg mx-auto">
        <CurioMoodboardCard accent={category.accent}>
          <div className="flex items-center gap-3 mb-2">
            <div
              className="w-12 h-12 rounded-2xl flex items-center justify-center"
              style={{
                background: `${category.accent}18`,
              }}
            >
              <MaterialIcon name={category.iconGlyph} size={28} />
            </div>
            <div className="flex-1 min-w-0">
              <h1
                className="text-xl font-bold truncate"
                style={{ color: getTextColor(isDark), fontFamily: 'Geom, sans-serif' }}
              >
                {entry.topic.name}
              </h1>
              <p className="text-sm" style={{ color: isDark ? 'rgba(255,255,255,0.6)' : 'rgba(59,10,23,0.6)' }}>
                {category.displayName} · {entry.topic.subtype}
              </p>
            </div>
            <span
              className="text-xs px-2 py-1 rounded-full flex-shrink-0"
              style={{
                background: `${category.accent}18`,
                color: category.accent,
              }}
            >
              <MaterialIcon name={getFormatIcon(entry.format)} size={14} /> {entry.format}
            </span>
          </div>
        </CurioMoodboardCard>
      </div>

      {/* Meta Info */}
      <div className="px-6 py-2">
        <div className="flex items-center gap-4 text-sm" style={{ color: isDark ? 'rgba(255,255,255,0.5)' : 'rgba(59,10,23,0.5)' }}>
          <span className="flex items-center gap-1"><MaterialIcon name={getFormatIcon(entry.format)} size={14} /> {entry.format}</span>
          <span>·</span>
          <span>{formatDate(entry.capturedAtMillis)}</span>
        </div>
      </div>

      {/* Tags */}
      {entry.tags.length > 0 && (
        <div className="px-6 py-2">
          <div className="flex flex-wrap gap-2">
            {entry.tags.map((tag) => (
              <span
                key={tag}
                className="px-3 py-1 rounded-full text-sm"
                style={{
                  background: `${category.accent}22`,
                  color: category.accent,
                }}
              >
                #{tag}
              </span>
            ))}
          </div>
        </div>
      )}

      {/* Content - Paper style */}
      <div className="px-5 py-4 max-w-lg mx-auto">
        <CurioPaperCard variant="ruled" watermark={category.iconGlyph} accent={category.accent}>
          <FormatRenderer format={entry.format} data={captureData} isDark={isDark} accent={category.accent} />
        </CurioPaperCard>
      </div>

      {/* Topic Teaser */}
      {entry.topic.teaser && (
        <div className="px-6 py-4">
          <h3 className="font-semibold mb-2" style={{ color: getTextColor(isDark) }}>
            About this topic
          </h3>
          <p
            className="text-sm leading-relaxed"
            style={{ color: isDark ? 'rgba(255,255,255,0.7)' : 'rgba(59,10,23,0.7)' }}
          >
            {entry.topic.teaser}
          </p>
        </div>
      )}

      {/* Delete Confirmation Modal */}
      {showDeleteConfirm && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-[70]">
          <div
            className="p-6 rounded-[24px] mx-6 max-w-sm w-full"
            style={{ background: isDark ? '#1a1a2e' : 'white' }}
          >
            <h3 className="text-lg font-bold mb-2" style={{ color: getTextColor(isDark) }}>
              Delete this entry?
            </h3>
            <p className="text-sm mb-6" style={{ color: isDark ? 'rgba(255,255,255,0.6)' : 'rgba(59,10,23,0.6)' }}>
              This action cannot be undone. Your saved entry will be permanently removed.
            </p>
            <div className="flex gap-3">
              <button
                onClick={() => setShowDeleteConfirm(false)}
                className="flex-1 py-3 rounded-[16px] font-medium"
                style={{
                  background: isDark ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.05)',
                  color: getTextColor(isDark),
                }}
              >
                Cancel
              </button>
              <button
                onClick={handleDelete}
                className="flex-1 py-3 rounded-[16px] font-medium text-white"
                style={{
                  background: '#EF4444',
                }}
              >
                Delete
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
    </div>
  );
};

export default EntryDetailScreen;
