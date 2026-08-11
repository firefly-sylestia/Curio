// Curio Web App - Save Capture Screen
// Format-specific editors for saving captures

import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useTheme, getBackgroundColor, getTextColor } from '../theme/ThemeContext';
import { getCategoryBySlug } from '../data/categories';
import { CurioPaperCard, CurioBackButton, MaterialIcon } from '../components/SharedComponents';
import type { CurioCategory, CurioTopic, CaptureFormat, CaptureData } from '../types';
import { captureRepository, generateId, serializeTags } from '../db/database';

// Format-specific editors
const SoundBiteEditor: React.FC<{
  data: CaptureData;
  onChange: (data: CaptureData) => void;
  category: CurioCategory;
}> = ({ data, onChange, category }) => {
  const { isDark } = useTheme();
  const soundData = data as { durationSeconds: number; notes: string };
  return (
    <div className="space-y-4">
      <div>
        <label className="block text-sm font-medium mb-2" style={{ color: getTextColor(isDark) }}>
          Duration (seconds)
        </label>
        <input
          type="number"
          value={soundData.durationSeconds}
          onChange={(e) => onChange({ ...soundData, durationSeconds: parseInt(e.target.value) || 0 })}
          className="w-full px-4 py-3 rounded-[16px] border"
          style={{
            borderColor: `${category.accent}44`,
            background: isDark ? 'rgba(255,255,255,0.06)' : 'white',
            color: getTextColor(isDark),
          }}
        />
      </div>
      <div>
        <label className="block text-sm font-medium mb-2" style={{ color: getTextColor(isDark) }}>
          Notes
        </label>
        <textarea
          value={soundData.notes}
          onChange={(e) => onChange({ ...soundData, notes: e.target.value })}
          placeholder="What did you hear?"
          rows={4}
          className="w-full px-4 py-3 rounded-[16px] border resize-none"
          style={{
            borderColor: `${category.accent}44`,
            background: isDark ? 'rgba(255,255,255,0.06)' : 'white',
            color: getTextColor(isDark),
          }}
        />
      </div>
    </div>
  );
};

const ReelNotesEditor: React.FC<{
  data: CaptureData;
  onChange: (data: CaptureData) => void;
  category: CurioCategory;
}> = ({ data, onChange, category }) => {
  const { isDark } = useTheme();
  const reelData = data as { rating: number; review: string; favoriteQuote?: number };
  return (
    <div className="space-y-4">
      <div>
        <label className="block text-sm font-medium mb-2" style={{ color: getTextColor(isDark) }}>
          Rating
        </label>
        <div className="flex gap-2">
          {[1, 2, 3, 4, 5].map((star) => (
            <button
              key={star}
              onClick={() => onChange({ ...reelData, rating: star })}
              className="text-3xl transition-transform hover:scale-110"
              style={{ color: star <= reelData.rating ? '#F59E0B' : (isDark ? 'rgba(255,255,255,0.25)' : 'rgba(59,10,23,0.25)') }}
            >
              {star <= reelData.rating ? '★' : '☆'}
            </button>
          ))}
        </div>
      </div>
      <div>
        <label className="block text-sm font-medium mb-2" style={{ color: getTextColor(isDark) }}>
          Review
        </label>
        <textarea
          value={reelData.review}
          onChange={(e) => onChange({ ...reelData, review: e.target.value })}
          placeholder="Write your review..."
          rows={6}
          className="w-full px-4 py-3 rounded-[16px] border resize-none"
          style={{
            borderColor: `${category.accent}44`,
            background: isDark ? 'rgba(255,255,255,0.06)' : 'white',
            color: getTextColor(isDark),
          }}
        />
      </div>
    </div>
  );
};

const MarginaliaEditor: React.FC<{
  data: CaptureData;
  onChange: (data: CaptureData) => void;
  category: CurioCategory;
}> = ({ data, onChange, category }) => {
  const { isDark } = useTheme();
  const marginaliaData = data as { journalEntry: string; quotes: Array<{ text: string; context?: string }> };
  const [newQuote, setNewQuote] = useState('');
  
  const addQuote = () => {
    if (newQuote.trim()) {
      onChange({
        ...marginaliaData,
        quotes: [...marginaliaData.quotes, { text: newQuote.trim() }],
      });
      setNewQuote('');
    }
  };
  
  const removeQuote = (index: number) => {
    onChange({
      ...marginaliaData,
      quotes: marginaliaData.quotes.filter((_, i) => i !== index),
    });
  };
  
  return (
    <div className="space-y-4">
      <div>
        <label className="block text-sm font-medium mb-2" style={{ color: getTextColor(isDark) }}>
          Journal Entry
        </label>
        <textarea
          value={marginaliaData.journalEntry}
          onChange={(e) => onChange({ ...marginaliaData, journalEntry: e.target.value })}
          placeholder="Write your thoughts..."
          rows={6}
          className="w-full px-4 py-3 rounded-[16px] border resize-none"
          style={{
            borderColor: `${category.accent}44`,
            background: isDark ? 'rgba(255,255,255,0.06)' : 'white',
            color: getTextColor(isDark),
          }}
        />
      </div>
      
      <div>
        <label className="block text-sm font-medium mb-2" style={{ color: getTextColor(isDark) }}>
          Favorite Quotes
        </label>
        <div className="space-y-2">
          {marginaliaData.quotes.map((quote, index) => (
            <div
              key={index}
              className="flex items-start gap-2 p-3 rounded-[12px]"
              style={{ background: `${category.tint}` }}
            >
              <span className="text-lg">"</span>
              <p className="flex-1 text-sm">{quote.text}</p>
              <button
                onClick={() => removeQuote(index)}
                className="text-red-400 hover:text-red-600"
              >
                ×
              </button>
            </div>
          ))}
        </div>
        
        <div className="flex gap-2 mt-3">
          <input
            type="text"
            value={newQuote}
            onChange={(e) => setNewQuote(e.target.value)}
            onKeyPress={(e) => e.key === 'Enter' && addQuote()}
            placeholder="Add a quote..."
            className="flex-1 px-4 py-2 rounded-[12px] border text-sm"
            style={{
              borderColor: `${category.accent}44`,
              background: isDark ? 'rgba(255,255,255,0.06)' : 'white',
              color: getTextColor(isDark),
            }}
          />
          <button
            onClick={addQuote}
            className="px-4 py-2 rounded-[12px] text-sm font-medium"
            style={{
              background: category.accent,
              color: 'white',
            }}
          >
            Add
          </button>
        </div>
      </div>
    </div>
  );
};

const GalleryWallEditor: React.FC<{
  data: CaptureData;
  onChange: (data: CaptureData) => void;
  category: CurioCategory;
}> = ({ data, onChange, category }) => {
  const { isDark } = useTheme();
  const galleryData = data as { caption: string; images: string[] };
  return (
    <div className="space-y-4">
      <div>
        <label className="block text-sm font-medium mb-2" style={{ color: getTextColor(isDark) }}>
          Caption
        </label>
        <textarea
          value={galleryData.caption}
          onChange={(e) => onChange({ ...galleryData, caption: e.target.value })}
          placeholder="Describe your moodboard..."
          rows={4}
          className="w-full px-4 py-3 rounded-[16px] border resize-none"
          style={{
            borderColor: `${category.accent}44`,
            background: isDark ? 'rgba(255,255,255,0.06)' : 'white',
            color: getTextColor(isDark),
          }}
        />
      </div>
      
      <div>
        <label className="block text-sm font-medium mb-2" style={{ color: getTextColor(false) }}>
          Images
        </label>
        <div className="grid grid-cols-3 gap-2">
          {galleryData.images.map((_, index) => (
            <div
              key={index}
              className="aspect-square rounded-[12px] flex items-center justify-center"
              style={{ background: `${category.tint}` }}
            >
              <MaterialIcon name="image" size={32} />
            </div>
          ))}
          <button
            className="aspect-square rounded-[12px] border-2 border-dashed flex items-center justify-center"
            style={{ borderColor: `${category.accent}44` }}
          >
            <span className="text-2xl">+</span>
          </button>
        </div>
      </div>
    </div>
  );
};

const FieldNotesEditor: React.FC<{
  data: CaptureData;
  onChange: (data: CaptureData) => void;
  category: CurioCategory;
}> = ({ data, onChange, category }) => {
  const { isDark } = useTheme();
  const fieldData = data as { observed: string; surprised: string; learnNext: string };
  return (
    <div className="space-y-4">
      <div>
        <label className="block text-sm font-medium mb-2" style={{ color: getTextColor(isDark) }}>
          <MaterialIcon name="search" size={16} /> Observed
        </label>
        <textarea
          value={fieldData.observed}
          onChange={(e) => onChange({ ...fieldData, observed: e.target.value })}
          placeholder="What did you observe?"
          rows={3}
          className="w-full px-4 py-3 rounded-[16px] border resize-none"
          style={{
            borderColor: `${category.accent}44`,
            background: isDark ? 'rgba(255,255,255,0.06)' : 'white',
            color: getTextColor(isDark),
          }}
        />
      </div>
      
      <div>
        <label className="block text-sm font-medium mb-2" style={{ color: getTextColor(isDark) }}>
          <MaterialIcon name="sentiment_surprised" size={16} /> Surprised Me
        </label>
        <textarea
          value={fieldData.surprised}
          onChange={(e) => onChange({ ...fieldData, surprised: e.target.value })}
          placeholder="What surprised you?"
          rows={3}
          className="w-full px-4 py-3 rounded-[16px] border resize-none"
          style={{
            borderColor: `${category.accent}44`,
            background: isDark ? 'rgba(255,255,255,0.06)' : 'white',
            color: getTextColor(isDark),
          }}
        />
      </div>
      
      <div>
        <label className="block text-sm font-medium mb-2" style={{ color: getTextColor(isDark) }}>
          <MaterialIcon name="menu_book" size={16} /> Want to Learn Next
        </label>
        <textarea
          value={fieldData.learnNext}
          onChange={(e) => onChange({ ...fieldData, learnNext: e.target.value })}
          placeholder="What do you want to learn more about?"
          rows={3}
          className="w-full px-4 py-3 rounded-[16px] border resize-none"
          style={{
            borderColor: `${category.accent}44`,
            background: isDark ? 'rgba(255,255,255,0.06)' : 'white',
            color: getTextColor(isDark),
          }}
        />
      </div>
    </div>
  );
};

// Format selector component
const FormatSelector: React.FC<{
  selectedFormat: CaptureFormat;
  onSelect: (format: CaptureFormat) => void;
  category: CurioCategory;
}> = ({ selectedFormat, onSelect, category }) => {
  const formats: Array<{ format: CaptureFormat; label: string; icon: string }> = [
    { format: 'SoundBite', label: 'Sound Bite', icon: 'mic' },
    { format: 'ReelNotes', label: 'Reel Notes', icon: 'movie' },
    { format: 'Marginalia', label: 'Marginalia', icon: 'edit_note' },
    { format: 'GalleryWall', label: 'Gallery Wall', icon: 'image' },
    { format: 'FieldNotes', label: 'Field Notes', icon: 'description' },
    { format: 'OpenNotebook', label: 'Open Notebook', icon: 'menu_book' },
  ];
  
  return (
    <div className="flex gap-2 overflow-x-auto pb-2">
      {formats.map(({ format, label, icon }) => (
        <button
          key={format}
          onClick={() => onSelect(format)}
          className="flex-shrink-0 flex items-center gap-2 px-4 py-2 rounded-full transition-all"
          style={{
            background: selectedFormat === format
              ? category.accent
              : `${category.accent}22`,
            color: selectedFormat === format ? 'white' : category.accent,
          }}
        >
          <MaterialIcon name={icon} size={18} />
          <span className="text-sm font-medium whitespace-nowrap">{label}</span>
        </button>
      ))}
    </div>
  );
};

// Main SaveCaptureScreen component
export const SaveCaptureScreen: React.FC = () => {
  const navigate = useNavigate();
  const { categorySlug, topicName } = useParams();
  const { isDark, isAmoled } = useTheme();
  const [category, setCategory] = useState<CurioCategory | null>(null);
  const [topic, setTopic] = useState<CurioTopic | null>(null);
  const [selectedFormat, setSelectedFormat] = useState<CaptureFormat>('Marginalia');
  const [captureData, setCaptureData] = useState<CaptureData | null>(null);
  const [tags, setTags] = useState<string[]>([]);
  const [tagInput, setTagInput] = useState('');
  const [isSaving, setIsSaving] = useState(false);
  const [showSuccess, setShowSuccess] = useState(false);

  useEffect(() => {
    if (categorySlug) {
      const foundCategory = getCategoryBySlug(categorySlug);
      if (foundCategory) {
        setCategory(foundCategory);
        setSelectedFormat(foundCategory.defaultFormat);
        
        // Initialize capture data based on format
        initializeCaptureData(foundCategory.defaultFormat);
        
        // Create mock topic
        setTopic({
          id: `mock-${categorySlug}`,
          categoryId: foundCategory.id,
          subtype: 'Topic',
          name: topicName?.replace(/-/g, ' ') || 'Unknown Topic',
          teaser: 'A fascinating topic to explore.',
          imageUrl: '',
          actionPrompt: {
            verb: 'Explore',
            targetName: 'this topic',
            durationMinutes: 30,
            instruction: 'Take time to learn something new.',
          },
          aliases: [],
          relatedTopicIds: [],
          difficulty: 1,
          weight: 100,
          curatedBy: 'human',
          curatedDate: '2026-01-15',
        });
      }
    }
  }, [categorySlug, topicName]);

  const initializeCaptureData = (format: CaptureFormat) => {
    switch (format) {
      case 'SoundBite':
        setCaptureData({ durationSeconds: 0, notes: '' });
        break;
      case 'ReelNotes':
        setCaptureData({ rating: 0, review: '' });
        break;
      case 'Marginalia':
        setCaptureData({ journalEntry: '', quotes: [] });
        break;
      case 'GalleryWall':
        setCaptureData({ caption: '', images: [] });
        break;
      case 'FieldNotes':
        setCaptureData({ observed: '', surprised: '', learnNext: '' });
        break;
      case 'OpenNotebook':
        setCaptureData({ subFormat: 'Marginalia', subData: { journalEntry: '', quotes: [] } });
        break;
      default:
        setCaptureData({ journalEntry: '', quotes: [] });
    }
  };

  const handleFormatChange = (format: CaptureFormat) => {
    setSelectedFormat(format);
    initializeCaptureData(format);
  };

  const handleAddTag = () => {
    const clean = tagInput.trim().replace(/^#/, '');
    if (clean && clean.length <= 24 && tags.length < 12) {
      setTags([...tags, clean].filter((v, i, a) => a.indexOf(v) === i));
      setTagInput('');
    }
  };

  const handleRemoveTag = (tag: string) => {
    setTags(tags.filter(t => t !== tag));
  };

  const handleSave = async () => {
    if (!topic || !category || !captureData) return;
    
    setIsSaving(true);
    try {
      const entry = {
        id: generateId(),
        topicId: topic.id,
        categoryId: category.id,
        topicName: topic.name,
        topicSubtype: topic.subtype,
        topicTeaser: topic.teaser,
        format: selectedFormat,
        capturedAtMillis: Date.now(),
        title: topic.name,
        formatDataJson: JSON.stringify(captureData),
        tagsJson: serializeTags(tags),
        isLegacy: false,
      };
      
      await captureRepository.insert(entry);
      
      // Update stats
      const currentEntries = parseInt(localStorage.getItem('curio-total-topics') || '0');
      localStorage.setItem('curio-total-topics', (currentEntries + 1).toString());
      
      // Add XP
      const currentXp = parseInt(localStorage.getItem('curio-xp') || '0');
      localStorage.setItem('curio-xp', (currentXp + 50).toString());
      
      setShowSuccess(true);
      setTimeout(() => {
        navigate(`/detail/${entry.id}`);
      }, 1500);
    } catch (error) {
      console.error('Failed to save:', error);
    } finally {
      setIsSaving(false);
    }
  };

  const canSave = () => {
    if (!captureData) return false;
    
    switch (selectedFormat) {
      case 'SoundBite':
        return (captureData as { notes: string }).notes.length > 0;
      case 'ReelNotes':
        return (captureData as { review: string }).review.length > 0 || (captureData as { rating: number }).rating > 0;
      case 'Marginalia':
        return (captureData as { journalEntry: string }).journalEntry.length > 0 || (captureData as { quotes: unknown[] }).quotes.length > 0;
      case 'GalleryWall':
        return (captureData as { caption: string }).caption.length > 0;
      case 'FieldNotes':
        const fieldData = captureData as { observed: string; surprised: string; learnNext: string };
        return fieldData.observed.length > 0 || fieldData.surprised.length > 0 || fieldData.learnNext.length > 0;
      default:
        return false;
    }
  };

  if (!category || !topic) {
    return (
      <div
        className="min-h-screen flex items-center justify-center"
        style={{ backgroundColor: getBackgroundColor(isDark, isAmoled) }}
      >
        <div className="text-center">
          <div className="w-8 h-8 border-4 border-t-transparent rounded-full animate-spin mx-auto" style={{ borderColor: '#FF8FA3', borderTopColor: 'transparent' }} />
          <p className="mt-4" style={{ color: isDark ? 'rgba(255,255,255,0.5)' : 'rgba(59,10,23,0.5)' }}>
            Loading...
          </p>
        </div>
      </div>
    );
  }

  return (
    <div
      className="min-h-screen pb-24"
      style={{ backgroundColor: getBackgroundColor(isDark, isAmoled) }}
    >
      {/* Header */}
      <header className="px-6 pt-6 pb-4 flex items-center justify-between">
        <CurioBackButton onClick={() => navigate(-1)} />
        <h1
          className="text-lg font-bold"
          style={{ color: getTextColor(isDark), fontFamily: 'Geom, sans-serif' }}
        >
          Save your take
        </h1>
        <div className="w-10" />
      </header>

      {/* Topic Reminder Strip */}
      <div className="px-6 py-2">
        <div
          className="flex items-center gap-3 p-3 rounded-[16px]"
          style={{
            background: isDark ? `${category.accent}22` : category.tint,
          }}
        >
          <div
            className="w-10 h-10 rounded-[12px] flex items-center justify-center"
            style={{ background: `${category.accent}22` }}
          >
            <MaterialIcon name={category.iconGlyph} size={24} />
          </div>
          <div>
            <h3 className="font-semibold text-sm" style={{ color: getTextColor(isDark) }}>
              {topic.name}
            </h3>
            <p className="text-xs" style={{ color: isDark ? 'rgba(255,255,255,0.6)' : 'rgba(59,10,23,0.6)' }}>
              {category.displayName}
            </p>
          </div>
        </div>
      </div>

      {/* Format Selector */}
      <div className="px-6 py-4">
        <FormatSelector
          selectedFormat={selectedFormat}
          onSelect={handleFormatChange}
          category={category}
        />
      </div>

      {/* Format Editor - Paper style */}
      <div className="px-5 py-2 max-w-lg mx-auto">
        <CurioPaperCard variant={selectedFormat === 'FieldNotes' ? 'plain' : 'ruled'} watermark={category.iconGlyph}>
          <div className="px-2">
            {selectedFormat === 'SoundBite' && captureData && (
              <SoundBiteEditor
                data={captureData}
                onChange={setCaptureData}
                category={category}
              />
            )}
            {selectedFormat === 'ReelNotes' && captureData && (
              <ReelNotesEditor
                data={captureData}
                onChange={setCaptureData}
                category={category}
              />
            )}
            {selectedFormat === 'Marginalia' && captureData && (
              <MarginaliaEditor
                data={captureData}
                onChange={setCaptureData}
                category={category}
              />
            )}
            {selectedFormat === 'GalleryWall' && captureData && (
              <GalleryWallEditor
                data={captureData}
                onChange={setCaptureData}
                category={category}
              />
            )}
            {selectedFormat === 'FieldNotes' && captureData && (
              <FieldNotesEditor
                data={captureData}
                onChange={setCaptureData}
                category={category}
              />
            )}
          </div>
        </CurioPaperCard>
      </div>

      {/* Tags Editor */}
      <div className="px-6 py-4">
        <label className="block text-sm font-medium mb-2" style={{ color: getTextColor(isDark) }}>
          Tags
        </label>
        <div className="flex flex-wrap gap-2 mb-2">
          {tags.map((tag) => (
            <span
              key={tag}
              className="flex items-center gap-1 px-3 py-1 rounded-full text-sm"
              style={{
                background: `${category.accent}22`,
                color: category.accent,
              }}
            >
              #{tag}
              <button
                onClick={() => handleRemoveTag(tag)}
                className="ml-1 hover:opacity-70"
              >
                ×
              </button>
            </span>
          ))}
        </div>
        <div className="flex gap-2">
          <input
            type="text"
            value={tagInput}
            onChange={(e) => setTagInput(e.target.value)}
            onKeyPress={(e) => e.key === 'Enter' && handleAddTag()}
            placeholder="Add a tag..."
            className="flex-1 px-4 py-2 rounded-[12px] border text-sm"
            style={{
              borderColor: `${category.accent}44`,
              background: 'white',
            }}
          />
          <button
            onClick={handleAddTag}
            className="px-4 py-2 rounded-[12px] text-sm font-medium"
            style={{
              background: category.accent,
              color: 'white',
            }}
          >
            Add
          </button>
        </div>
      </div>

      {/* Save Button */}
      <div className="px-6 py-4">
        <button
          onClick={handleSave}
          disabled={!canSave() || isSaving}
          className="w-full py-4 rounded-[24px] font-semibold transition-all disabled:opacity-50"
          style={{
            background: canSave()
              ? `linear-gradient(135deg, ${category.accent} 0%, ${category.lightAccent} 100%)`
              : isDark ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.05)',
            color: canSave() ? 'white' : getTextColor(isDark),
          }}
        >
          {isSaving ? 'Saving...' : 'Save entry'}
        </button>
      </div>

      {/* Success Modal */}
      {showSuccess && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-[70]">
          <div
            className="p-8 rounded-[24px] text-center mx-6"
            style={{ background: 'white' }}
          >
            <div className="text-6xl mb-4"><MaterialIcon name="celebration" size={64} /></div>
            <h2 className="text-xl font-bold mb-2" style={{ color: getTextColor(false) }}>
              Saved!
            </h2>
            <p className="text-sm" style={{ color: 'rgba(59,10,23,0.6)' }}>
              Your entry has been added to your cabinet.
            </p>
          </div>
        </div>
      )}
    </div>
  );
};

export default SaveCaptureScreen;
