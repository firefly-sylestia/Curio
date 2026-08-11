// Curio Web App - Save Capture Screen
// Matches Android: mood chips, real voice recording (MediaRecorder), waveform, paper cards

import React, { useState, useEffect, useRef, useCallback } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useTheme, getBackgroundColor, getTextColor } from '../theme/ThemeContext';
import { getCategoryBySlug } from '../data/categories';
import { MaterialIcon, CurioWatermarkBackdrop } from '../components/SharedComponents';
import type { CurioCategory, CaptureFormat, CaptureData, JournalMood } from '../types';
import { JOURNAL_MOODS } from '../types';
import { captureRepository, generateId, serializeTags } from '../db/database';
import { ScreenEntrance } from '../animations';

// ─── Paper ink & font ────────────────────────────────────────────────
const paperFont = "'Patrick Hand', cursive";
const getPaperInk = (isDark: boolean) => isDark ? '#E4D2BC' : '#2D140F';
const paperInputClass = "w-full bg-transparent resize-none outline-none placeholder:opacity-30";

// ─── Paper color palette ─────────────────────────────────────────────
const PAPER_COLORS: Record<string, { bg: string; bgDark: string; ink: string; inkDark: string }> = {
  cream: { bg: '#FDF8F0', bgDark: '#2A2520', ink: '#2D140F', inkDark: '#E4D2BC' },
  white: { bg: '#FFFFFF', bgDark: '#1E1E24', ink: '#1A1A1A', inkDark: '#E0E0E0' },
  kraft: { bg: '#E8D5B7', bgDark: '#3A3028', ink: '#2D140F', inkDark: '#DBC8A8' },
};

// ─── Format Glyphs ───────────────────────────────────────────────────
const FORMAT_OPTIONS: Array<{ id: CaptureFormat; label: string; icon: string }> = [
  { id: 'SoundBite', label: 'Sound Bite', icon: 'mic' },
  { id: 'ReelNotes', label: 'Reel Notes', icon: 'movie' },
  { id: 'Marginalia', label: 'Marginalia', icon: 'edit_note' },
  { id: 'GalleryWall', label: 'Gallery Wall', icon: 'image' },
  { id: 'FieldNotes', label: 'Field Notes', icon: 'description' },
];

// ═══════════════════════════════════════════════════════════════════════════
// WAVEFORM — live animated bars while recording, static after stop
// ═══════════════════════════════════════════════════════════════════════════

const LiveWaveform: React.FC<{ isRecording: boolean; isPaused: boolean; accent: string; barCount?: number }> = ({ isRecording, isPaused, accent, barCount = 40 }) => {
  const [bars, setBars] = useState<number[]>(() => Array.from({ length: barCount }, () => 0.12 + Math.random() * 0.08));
  const intervalRef = useRef<ReturnType<typeof setInterval> | undefined>(undefined);

  useEffect(() => {
    if (isRecording && !isPaused) {
      intervalRef.current = setInterval(() => {
        setBars(prev => prev.map((b, i) => {
          const target = 0.1 + Math.random() * 0.8;
          return b + (target - b) * (0.3 + (i / barCount) * 0.4);
        }));
      }, 80);
    } else if (!isRecording) {
      clearInterval(intervalRef.current);
      setBars(prev => prev.map(() => 0.12 + Math.random() * 0.08));
    }
    return () => clearInterval(intervalRef.current);
  }, [isRecording, isPaused, barCount]);

  return (
    <div className="flex items-end justify-center gap-[2px] h-12">
      {bars.map((h, i) => (
        <div key={i} className="flex-1 rounded-full transition-all duration-75"
          style={{
            height: `${Math.max(4, h * 100)}%`,
            background: accent,
            opacity: isRecording && !isPaused ? 0.7 + h * 0.3 : 0.25,
            minWidth: 2,
          }} />
      ))}
    </div>
  );
};

// ═══════════════════════════════════════════════════════════════════════════
// VOICE RECORDER — MediaRecorder API with timer, waveform, controls
// ═══════════════════════════════════════════════════════════════════════════

type RecorderState = 'IDLE' | 'RECORDING' | 'PAUSED' | 'STOPPED';

const VoiceRecorder: React.FC<{
  accent: string; isDark: boolean;
  onStateChange: (state: RecorderState) => void;
  onAudioReady: (data: { blob: Blob; dataUrl: string; durationSeconds: number; fileSizeBytes: number }) => void;
  initialState?: RecorderState;
}> = ({ accent, isDark, onStateChange, onAudioReady, initialState = 'IDLE' }) => {
  const [state, setState] = useState<RecorderState>(initialState);
  const [seconds, setSeconds] = useState(0);
  const mediaRecorderRef = useRef<MediaRecorder | null>(null);
  const chunksRef = useRef<Blob[]>([]);
  const timerRef = useRef<ReturnType<typeof setInterval> | undefined>(undefined);
  const startTimeRef = useRef(0);
  const pausedMsRef = useRef(0);

  const setRecorderState = useCallback((s: RecorderState) => { setState(s); onStateChange(s); }, [onStateChange]);

  // Timer
  useEffect(() => {
    if (state === 'RECORDING') {
      startTimeRef.current = Date.now();
      timerRef.current = setInterval(() => {
        setSeconds(Math.floor((Date.now() - startTimeRef.current + pausedMsRef.current) / 1000));
      }, 200);
    } else {
      clearInterval(timerRef.current);
      if (state === 'PAUSED') pausedMsRef.current += Date.now() - startTimeRef.current;
    }
    return () => clearInterval(timerRef.current);
  }, [state]);

  const formatTime = (s: number) => `${String(Math.floor(s / 60)).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`;

  const startRecording = async () => {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      const mediaRecorder = new MediaRecorder(stream, { mimeType: MediaRecorder.isTypeSupported('audio/webm;codecs=opus') ? 'audio/webm;codecs=opus' : 'audio/webm' });
      mediaRecorderRef.current = mediaRecorder;
      chunksRef.current = [];

      mediaRecorder.ondataavailable = (e) => { if (e.data.size > 0) chunksRef.current.push(e.data); };
      mediaRecorder.onstop = async () => {
        stream.getTracks().forEach(t => t.stop());
        const blob = new Blob(chunksRef.current, { type: mediaRecorder.mimeType });
        const dataUrl = await new Promise<string>((resolve) => {
          const reader = new FileReader();
          reader.onloadend = () => resolve(reader.result as string);
          reader.readAsDataURL(blob);
        });
        const dur = Math.floor((Date.now() - startTimeRef.current + pausedMsRef.current) / 1000);
        onAudioReady({ blob, dataUrl, durationSeconds: dur, fileSizeBytes: blob.size });
        setRecorderState('STOPPED');
      };

      mediaRecorder.start(250);
      pausedMsRef.current = 0;
      setSeconds(0);
      setRecorderState('RECORDING');
    } catch (err) {
      console.error('Mic access denied:', err);
    }
  };

  const pauseRecording = () => {
    mediaRecorderRef.current?.pause();
    setRecorderState('PAUSED');
  };

  const resumeRecording = () => {
    mediaRecorderRef.current?.resume();
    setRecorderState('RECORDING');
  };

  const stopRecording = () => {
    if (mediaRecorderRef.current?.state !== 'inactive') {
      mediaRecorderRef.current?.stop();
    }
  };

  const discardRecording = () => {
    clearInterval(timerRef.current);
    mediaRecorderRef.current?.stream?.getTracks().forEach(t => t.stop());
    chunksRef.current = [];
    pausedMsRef.current = 0;
    setSeconds(0);
    setRecorderState('IDLE');
  };

  return (
    <div className="space-y-4">
      {/* Waveform + timer */}
      <div className="relative">
        <LiveWaveform isRecording={state === 'RECORDING'} isPaused={state === 'PAUSED'} accent={accent} />
        {(state === 'RECORDING' || state === 'PAUSED') && (
          <div className="text-center mt-2">
            <span className="text-2xl font-bold tracking-wider font-mono" style={{ color: accent }}>
              {formatTime(seconds)}
            </span>
            {state === 'PAUSED' && (
              <span className="ml-2 text-sm font-medium opacity-60" style={{ color: accent }}>Paused</span>
            )}
          </div>
        )}
        {state === 'STOPPED' && (
          <div className="text-center mt-1">
            <span className="text-sm font-semibold opacity-50" style={{ color: getPaperInk(isDark) }}>
              {formatTime(seconds)} recorded
            </span>
          </div>
        )}
      </div>

      {/* Controls */}
      <div className="flex items-center justify-center gap-4">
        {state === 'IDLE' && (
          <button onClick={startRecording}
            className="w-16 h-16 rounded-full flex items-center justify-center transition-transform active:scale-90"
            style={{ background: accent, boxShadow: `0 4px 20px ${accent}40` }}>
            <MaterialIcon name="mic" size={28} style={{ color: 'white' }} />
          </button>
        )}
        {state === 'RECORDING' && (
          <>
            <button onClick={pauseRecording}
              className="w-12 h-12 rounded-full flex items-center justify-center active:scale-90"
              style={{ background: isDark ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.05)' }}>
              <MaterialIcon name="pause" size={24} style={{ color: accent }} />
            </button>
            <button onClick={stopRecording}
              className="w-14 h-14 rounded-full flex items-center justify-center transition-all animate-pulse"
              style={{ background: accent, boxShadow: `0 0 24px ${accent}60` }}>
              <MaterialIcon name="stop" size={24} style={{ color: 'white' }} />
            </button>
            <button onClick={discardRecording}
              className="w-10 h-10 rounded-full flex items-center justify-center active:scale-90"
              style={{ background: isDark ? 'rgba(255,255,255,0.08)' : 'rgba(0,0,0,0.04)' }}>
              <MaterialIcon name="delete" size={20} style={{ color: isDark ? 'rgba(255,255,255,0.4)' : 'rgba(0,0,0,0.3)' }} />
            </button>
          </>
        )}
        {state === 'PAUSED' && (
          <>
            <button onClick={discardRecording}
              className="w-10 h-10 rounded-full flex items-center justify-center active:scale-90"
              style={{ background: isDark ? 'rgba(255,255,255,0.08)' : 'rgba(0,0,0,0.04)' }}>
              <MaterialIcon name="delete" size={20} style={{ color: isDark ? 'rgba(255,255,255,0.4)' : 'rgba(0,0,0,0.3)' }} />
            </button>
            <button onClick={resumeRecording}
              className="w-14 h-14 rounded-full flex items-center justify-center transition-all"
              style={{ background: accent, boxShadow: `0 4px 20px ${accent}40` }}>
              <MaterialIcon name="mic" size={28} style={{ color: 'white' }} />
            </button>
            <button onClick={stopRecording}
              className="w-12 h-12 rounded-full flex items-center justify-center active:scale-90"
              style={{ background: isDark ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.05)' }}>
              <MaterialIcon name="stop" size={22} style={{ color: accent }} />
            </button>
          </>
        )}
        {state === 'STOPPED' && (
          <>
            <button onClick={discardRecording}
              className="px-4 py-2 rounded-full text-xs font-semibold active:scale-95"
              style={{ background: isDark ? 'rgba(255,255,255,0.06)' : 'rgba(0,0,0,0.03)', color: getPaperInk(isDark) }}>
              Discard
            </button>
            <button onClick={startRecording}
              className="px-4 py-2 rounded-full text-xs font-semibold active:scale-95 flex items-center gap-1.5"
              style={{ background: accent, color: 'white' }}>
              <MaterialIcon name="mic" size={14} /> Re-record
            </button>
          </>
        )}
      </div>
    </div>
  );
};

// ═══════════════════════════════════════════════════════════════════════════
// MOOD CHIPS — "How did it make you feel?"
// ═══════════════════════════════════════════════════════════════════════════

const MoodChips: React.FC<{
  mood: JournalMood | null; onChange: (m: JournalMood | null) => void; accent: string; isDark: boolean;
}> = ({ mood, onChange, accent, isDark }) => (
  <div className="space-y-2">
    <p className="text-xs font-semibold opacity-50 uppercase tracking-wider" style={{ color: getTextColor(isDark) }}>
      How did it make you feel?
    </p>
    <div className="flex flex-wrap gap-2">
      {JOURNAL_MOODS.map(m => (
        <button key={m.id} onClick={() => onChange(mood === m.id ? null : m.id)}
          className="flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-semibold transition-all active:scale-95"
          style={{
            background: mood === m.id ? accent : isDark ? 'rgba(255,255,255,0.04)' : 'rgba(0,0,0,0.03)',
            color: mood === m.id ? 'white' : isDark ? 'rgba(255,255,255,0.55)' : 'rgba(0,0,0,0.5)',
            border: mood === m.id ? 'none' : `1px solid ${isDark ? 'rgba(255,255,255,0.06)' : 'rgba(0,0,0,0.06)'}`,
          }}>
          <MaterialIcon name={m.icon} size={14} /> {m.label}
        </button>
      ))}
    </div>
  </div>
);

// ═══════════════════════════════════════════════════════════════════════════
// PAPER CARD — styled like the Android paper cards with Patrick Hand
// ═══════════════════════════════════════════════════════════════════════════

const PaperCard: React.FC<{
  children: React.ReactNode; isDark: boolean; accent: string;
  paperColor?: 'cream' | 'white' | 'kraft'; paperStyle?: 'ruled' | 'torn' | 'tornRuled';
}> = ({ children, isDark, accent, paperColor = 'cream', paperStyle = 'ruled' }) => {
  const pc = PAPER_COLORS[paperColor];
  const bg = isDark ? pc.bgDark : pc.bg;
  const ink = isDark ? pc.inkDark : pc.ink;

  return (
    <div className="rounded-2xl p-5 relative overflow-hidden" style={{ background: bg, border: `1px solid ${accent}18` }}>
      {/* Category accent at the top-left */}
      <div className="absolute top-0 left-4 w-8 h-[2px] rounded-full" style={{ background: accent, opacity: 0.6 }} />
      {/* Ruled lines for 'ruled' style */}
      {paperStyle !== 'torn' && (
        <div className="absolute inset-0 pointer-events-none opacity-[0.06]"
          style={{
            backgroundImage: `repeating-linear-gradient(transparent, transparent 27px, ${ink} 27px, ${ink} 28px)`,
            backgroundPosition: '0 38px',
          }} />
      )}
      <div className="relative" style={{ fontFamily: paperFont, color: ink, fontSize: '1.1rem', lineHeight: 1.75 }}>
        {children}
      </div>
    </div>
  );
};

// ═══════════════════════════════════════════════════════════════════════════
// SOUND BITE EDITOR — voice recorder + title + note + mood + quotes
// ═══════════════════════════════════════════════════════════════════════════

const SoundBiteEditor: React.FC<{
  accent: string; isDark: boolean; onCanSave: (v: boolean) => void;
}> = ({ accent, isDark, onCanSave }) => {
  const [title, setTitle] = useState('');
  const [note, setNote] = useState('');
  const [mood, setMood] = useState<JournalMood | null>(null);
  const [quotes, setQuotes] = useState<Array<{ text: string; context?: string }>>([]);
  const [newQuote, setNewQuote] = useState('');
  const [recorderState, setRecorderState] = useState<RecorderState>('IDLE');
  const [audioData, setAudioData] = useState<{ blob: Blob; dataUrl: string; durationSeconds: number; fileSizeBytes: number } | null>(null);
  const audioRef = useRef<HTMLAudioElement>(null);

  const canSave = (recorderState === 'STOPPED' && !!audioData) || !!note.trim();
  useEffect(() => { onCanSave(canSave); }, [canSave, onCanSave]);

  // Expose data to parent
  const dataRef = useRef<any>(null);
  dataRef.current = {
    durationSeconds: audioData?.durationSeconds || 0,
    title, note, mood,
    audioFilePath: audioData ? `recording-${Date.now()}.webm` : null,
    audioDataUrl: audioData?.dataUrl || null,
    fileSizeBytes: audioData?.fileSizeBytes || 0,
    quotes,
    titleStyle: 'ruled' as const, noteStyle: 'ruled' as const,
    titleColor: 'cream' as const, noteColor: 'cream' as const,
  };

  // Store dataRef on window so parent can access it
  useEffect(() => { (window as any).__soundBiteData = dataRef; return () => { delete (window as any).__soundBiteData; }; }, []);

  const addQuote = () => {
    if (newQuote.trim()) { setQuotes([...quotes, { text: newQuote.trim() }]); setNewQuote(''); }
  };

  const ink = getPaperInk(isDark);

  return (
    <div className="space-y-5">
      {/* Voice Recorder */}
      <VoiceRecorder accent={accent} isDark={isDark}
        onStateChange={setRecorderState}
        onAudioReady={setAudioData} />

      {/* Audio preview after stop */}
      {recorderState === 'STOPPED' && audioData && (
        <div className="flex items-center gap-3">
          <button onClick={() => audioRef.current?.play()}
            className="w-10 h-10 rounded-full flex items-center justify-center"
            style={{ background: accent }}>
            <MaterialIcon name="play_arrow" size={22} style={{ color: 'white' }} />
          </button>
          <div className="flex-1 h-1.5 rounded-full" style={{ background: isDark ? 'rgba(255,255,255,0.08)' : 'rgba(0,0,0,0.06)' }}>
            <div className="h-full rounded-full transition-all" style={{ width: '100%', background: accent }} />
          </div>
          <span className="text-xs font-mono opacity-60" style={{ color: ink }}>
            {String(Math.floor(audioData.durationSeconds / 60)).padStart(2, '0')}:{String(audioData.durationSeconds % 60).padStart(2, '0')}
          </span>
        </div>
      )}
      <audio ref={audioRef} src={audioData?.dataUrl} className="hidden" />

      {/* Title */}
      <div>
        <label className="block text-xs font-semibold mb-1 opacity-40 uppercase tracking-wider" style={{ fontFamily: paperFont, color: ink }}>Title</label>
        <input type="text" value={title} onChange={e => setTitle(e.target.value)}
          placeholder="What's this recording about?"
          className={`${paperInputClass} text-lg font-semibold`}
          style={{ fontFamily: paperFont, color: ink, borderBottom: `1px solid ${isDark ? 'rgba(255,255,255,0.06)' : 'rgba(0,0,0,0.06)'}` }} />
      </div>

      {/* Note */}
      <div>
        <label className="block text-xs font-semibold mb-1 opacity-40 uppercase tracking-wider" style={{ fontFamily: paperFont, color: ink }}>Note</label>
        <textarea value={note} onChange={e => setNote(e.target.value)}
          placeholder="Write down your thoughts..."
          rows={4} className={paperInputClass} style={{ fontFamily: paperFont, color: ink, fontSize: '1.1rem', lineHeight: 1.7 }} />
      </div>

      {/* Mood */}
      <MoodChips mood={mood} onChange={setMood} accent={accent} isDark={isDark} />

      {/* Quotes */}
      <div>
        <label className="block text-xs font-semibold mb-1.5 opacity-40 uppercase tracking-wider" style={{ fontFamily: paperFont, color: ink }}>
          <MaterialIcon name="format_quote" size={13} style={{ verticalAlign: 'middle', marginRight: 4 }} /> Quotes
        </label>
        {quotes.length > 0 && (
          <div className="space-y-2 mb-3">
            {quotes.map((q, i) => (
              <div key={i} className="flex items-start gap-2 pl-3 border-l-2" style={{ borderColor: accent }}>
                <span className="text-lg leading-none opacity-40" style={{ fontFamily: paperFont }}>&ldquo;</span>
                <p className="flex-1 text-sm leading-relaxed" style={{ fontFamily: paperFont, color: ink }}>{q.text}</p>
                <button onClick={() => setQuotes(quotes.filter((_, j) => j !== i))}
                  className="text-red-400/60 hover:text-red-400 text-lg leading-none">&times;</button>
              </div>
            ))}
          </div>
        )}
        <div className="flex gap-2">
          <input type="text" value={newQuote} onChange={e => setNewQuote(e.target.value)}
            onKeyDown={e => e.key === 'Enter' && addQuote()}
            placeholder="Add a quote..." className={`${paperInputClass} flex-1 text-sm`}
            style={{ fontFamily: paperFont, color: ink, borderBottom: `1px solid ${isDark ? 'rgba(255,255,255,0.06)' : 'rgba(0,0,0,0.06)'}` }} />
          <button onClick={addQuote}
            className="px-3 py-1 rounded-full text-xs font-semibold active:scale-95"
            style={{ background: accent, color: 'white' }}>Add</button>
        </div>
      </div>
    </div>
  );
};

// ═══════════════════════════════════════════════════════════════════════════
// OTHER FORMAT EDITORS (simplified, matching Android)
// ═══════════════════════════════════════════════════════════════════════════

const ReelNotesEditor: React.FC<{ accent: string; isDark: boolean; onCanSave: (v: boolean) => void }> = ({ accent, isDark, onCanSave }) => {
  const [rating, setRating] = useState(0);
  const [review, setReview] = useState('');
  const [mood, setMood] = useState<JournalMood | null>(null);
  const canSave = rating > 0 || !!review.trim();
  useEffect(() => { onCanSave(canSave); }, [canSave, onCanSave]);
  const ink = getPaperInk(isDark);
  return (
    <PaperCard isDark={isDark} accent={accent}>
      <div className="space-y-5">
        <div>
          <label className="block text-xs font-semibold mb-2 opacity-40 uppercase tracking-wider" style={{ fontFamily: paperFont, color: ink }}>Rating</label>
          <div className="flex gap-1.5">
            {[1, 2, 3, 4, 5].map(star => (
              <button key={star} onClick={() => setRating(star)}
                className="transition-transform hover:scale-115 active:scale-90">
                <MaterialIcon name="star" size={28} filled={star <= rating}
                  style={{ color: star <= rating ? '#E8A838' : isDark ? 'rgba(255,255,255,0.12)' : 'rgba(0,0,0,0.08)' }} />
              </button>
            ))}
          </div>
        </div>
        <div>
          <label className="block text-xs font-semibold mb-1 opacity-40 uppercase tracking-wider" style={{ fontFamily: paperFont, color: ink }}>Review</label>
          <textarea value={review} onChange={e => setReview(e.target.value)}
            placeholder="Write your review here..." rows={5} className={paperInputClass}
            style={{ fontFamily: paperFont, color: ink, fontSize: '1.1rem', lineHeight: 1.7 }} />
        </div>
        <MoodChips mood={mood} onChange={setMood} accent={accent} isDark={isDark} />
      </div>
    </PaperCard>
  );
};

const MarginaliaEditor: React.FC<{ accent: string; isDark: boolean; onCanSave: (v: boolean) => void }> = ({ accent, isDark, onCanSave }) => {
  const [journalEntry, setJournalEntry] = useState('');
  const [quotes, setQuotes] = useState<Array<{ text: string; context?: string }>>([]);
  const [newQuote, setNewQuote] = useState('');
  const [mood, setMood] = useState<JournalMood | null>(null);
  const canSave = !!journalEntry.trim() || quotes.length > 0;
  useEffect(() => { onCanSave(canSave); }, [canSave, onCanSave]);
  const addQuote = () => { if (newQuote.trim()) { setQuotes([...quotes, { text: newQuote.trim() }]); setNewQuote(''); } };
  const ink = getPaperInk(isDark);
  return (
    <PaperCard isDark={isDark} accent={accent}>
      <div className="space-y-5">
        <div>
          <label className="block text-xs font-semibold mb-1 opacity-40 uppercase tracking-wider" style={{ fontFamily: paperFont, color: ink }}>Journal Entry</label>
          <textarea value={journalEntry} onChange={e => setJournalEntry(e.target.value)}
            placeholder="Write your thoughts about this topic..." rows={5} className={paperInputClass}
            style={{ fontFamily: paperFont, color: ink, fontSize: '1.1rem', lineHeight: 1.7 }} />
        </div>
        <div>
          <label className="block text-xs font-semibold mb-1.5 opacity-40 uppercase tracking-wider" style={{ fontFamily: paperFont, color: ink }}>Favorite Quotes</label>
          {quotes.length > 0 && (
            <div className="space-y-2 mb-3">
              {quotes.map((q, i) => (
                <div key={i} className="flex items-start gap-2 pl-3 border-l-2" style={{ borderColor: accent }}>
                  <span className="text-lg leading-none opacity-40">&ldquo;</span>
                  <p className="flex-1 text-sm leading-relaxed" style={{ fontFamily: paperFont, color: ink }}>{q.text}</p>
                  <button onClick={() => setQuotes(quotes.filter((_, j) => j !== i))} className="text-red-400/60 text-lg">&times;</button>
                </div>
              ))}
            </div>
          )}
          <div className="flex gap-2">
            <input type="text" value={newQuote} onChange={e => setNewQuote(e.target.value)}
              onKeyDown={e => e.key === 'Enter' && addQuote()} placeholder="Add a quote..."
              className={`${paperInputClass} flex-1 text-sm`}
              style={{ fontFamily: paperFont, color: ink, borderBottom: `1px solid ${isDark ? 'rgba(255,255,255,0.06)' : 'rgba(0,0,0,0.06)'}` }} />
            <button onClick={addQuote} className="px-3 py-1 rounded-full text-xs font-semibold active:scale-95"
              style={{ background: accent, color: 'white' }}>Add</button>
          </div>
        </div>
        <MoodChips mood={mood} onChange={setMood} accent={accent} isDark={isDark} />
      </div>
    </PaperCard>
  );
};

const GalleryWallEditor: React.FC<{ accent: string; isDark: boolean; onCanSave: (v: boolean) => void }> = ({ accent, isDark, onCanSave }) => {
  const [caption, setCaption] = useState('');
  const [mood, setMood] = useState<JournalMood | null>(null);
  const canSave = !!caption.trim();
  useEffect(() => { onCanSave(canSave); }, [canSave, onCanSave]);
  const ink = getPaperInk(isDark);
  return (
    <PaperCard isDark={isDark} accent={accent}>
      <div className="space-y-5">
        <div>
          <label className="block text-xs font-semibold mb-1 opacity-40 uppercase tracking-wider" style={{ fontFamily: paperFont, color: ink }}>Caption</label>
          <textarea value={caption} onChange={e => setCaption(e.target.value)}
            placeholder="Describe your mood board..." rows={3} className={paperInputClass}
            style={{ fontFamily: paperFont, color: ink, fontSize: '1.1rem', lineHeight: 1.7 }} />
        </div>
        <div>
          <label className="block text-xs font-semibold mb-2 opacity-40 uppercase tracking-wider" style={{ fontFamily: paperFont, color: ink }}>Images</label>
          <div className="grid grid-cols-3 gap-2">
            {Array.from({ length: 2 }).map((_, i) => (
              <div key={i} className="aspect-square rounded-lg flex items-center justify-center"
                style={{ background: isDark ? 'rgba(255,255,255,0.03)' : 'rgba(0,0,0,0.02)' }}>
                <MaterialIcon name="image" size={28} style={{ color: isDark ? 'rgba(255,255,255,0.15)' : 'rgba(0,0,0,0.1)' }} />
              </div>
            ))}
            <div className="aspect-square rounded-lg border-2 border-dashed flex items-center justify-center"
              style={{ borderColor: isDark ? 'rgba(255,255,255,0.08)' : 'rgba(0,0,0,0.06)' }}>
              <MaterialIcon name="add" size={24} style={{ color: isDark ? 'rgba(255,255,255,0.2)' : 'rgba(0,0,0,0.15)' }} />
            </div>
          </div>
        </div>
        <MoodChips mood={mood} onChange={setMood} accent={accent} isDark={isDark} />
      </div>
    </PaperCard>
  );
};

const FieldNotesEditor: React.FC<{ accent: string; isDark: boolean; onCanSave: (v: boolean) => void }> = ({ accent, isDark, onCanSave }) => {
  const [observed, setObserved] = useState('');
  const [surprised, setSurprised] = useState('');
  const [learnNext, setLearnNext] = useState('');
  const [mood, setMood] = useState<JournalMood | null>(null);
  const canSave = !!observed.trim() || !!surprised.trim() || !!learnNext.trim();
  useEffect(() => { onCanSave(canSave); }, [canSave, onCanSave]);
  const ink = getPaperInk(isDark);
  const fields = [
    { k: 'observed', v: observed, set: setObserved, icon: 'visibility', label: 'Observed', ph: 'What did you observe?' },
    { k: 'surprised', v: surprised, set: setSurprised, icon: 'sentiment_surprised', label: 'Surprised Me', ph: 'What surprised you?' },
    { k: 'learnNext', v: learnNext, set: setLearnNext, icon: 'menu_book', label: 'Want to Learn Next', ph: 'What do you want to learn more about?' },
  ];
  return (
    <div className="space-y-4">
      {fields.map(f => (
        <PaperCard key={f.k} isDark={isDark} accent={accent}>
          <div className="flex items-center gap-1.5 text-xs font-semibold mb-2 opacity-50 uppercase tracking-wider" style={{ fontFamily: paperFont, color: ink }}>
            <MaterialIcon name={f.icon} size={14} /> {f.label}
          </div>
          <textarea value={f.v} onChange={e => f.set(e.target.value)}
            placeholder={f.ph} rows={3} className={paperInputClass}
            style={{ fontFamily: paperFont, color: ink, fontSize: '1.1rem', lineHeight: 1.7 }} />
        </PaperCard>
      ))}
      <MoodChips mood={mood} onChange={setMood} accent={accent} isDark={isDark} />
    </div>
  );
};

// ═══════════════════════════════════════════════════════════════════════════
// MAIN SCREEN
// ═══════════════════════════════════════════════════════════════════════════

export const SaveCaptureScreen: React.FC = () => {
  const navigate = useNavigate();
  const { categorySlug, topicName } = useParams();
  const { isDark, isAmoled } = useTheme();
  const [category, setCategory] = useState<CurioCategory | null>(null);
  const [selectedFormat, setSelectedFormat] = useState<CaptureFormat>('SoundBite');
  const [canSave, setCanSave] = useState(false);
  const [tags, setTags] = useState<string[]>([]);
  const [tagInput, setTagInput] = useState('');
  const [isSaving, setIsSaving] = useState(false);
  const [showSuccess, setShowSuccess] = useState(false);
  const [formatKey, setFormatKey] = useState(0);

  useEffect(() => {
    if (categorySlug) {
      const found = getCategoryBySlug(categorySlug);
      if (found) setCategory(found);
    }
  }, [categorySlug]);

  // Reset canSave on format change
  useEffect(() => { setCanSave(false); }, [selectedFormat]);

  const handleAddTag = () => {
    const clean = tagInput.trim().replace(/^#/, '');
    if (clean && clean.length <= 24 && tags.length < 12 && !tags.includes(clean)) {
      setTags([...tags, clean]); setTagInput('');
    }
  };

  const buildCaptureData = (): CaptureData => {
    // For SoundBite, use the ref set on window
    const sbData = (window as any).__soundBiteData?.current;
    switch (selectedFormat) {
      case 'SoundBite': return {
        durationSeconds: sbData?.durationSeconds || 0,
        title: sbData?.title || '',
        note: sbData?.note || '',
        audioFilePath: sbData?.audioFilePath || null,
        audioDataUrl: sbData?.audioDataUrl || null,
        fileSizeBytes: sbData?.fileSizeBytes || 0,
        mood: sbData?.mood || null,
        quotes: sbData?.quotes || [],
        titleStyle: 'ruled', noteStyle: 'ruled',
        titleColor: 'cream', noteColor: 'cream',
      } as CaptureData;
      default: return {} as CaptureData;
    }
  };

  const handleSave = async () => {
    if (!category) return;
    setIsSaving(true);
    try {
      const topicDisplayName = topicName?.replace(/-/g, ' ') || 'Unknown Topic';
      const captureData = buildCaptureData();
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
    } catch (err) { console.error('Save failed:', err); }
    finally { setIsSaving(false); }
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
        {/* ── Top bar ──────────────────────────────────────────────── */}
        <div className="flex items-center justify-between px-4 pt-4 pb-1" style={{ paddingTop: 'env(safe-area-inset-top, 16px)' }}>
          <button onClick={() => navigate(-1)}
            className="w-10 h-10 rounded-full flex items-center justify-center"
            style={{ background: isDark ? 'rgba(255,255,255,0.06)' : 'rgba(0,0,0,0.03)' }}>
            <MaterialIcon name="arrow_back" size={22} style={{ color: getTextColor(isDark) }} />
          </button>
          <h1 className="text-base font-bold" style={{ color: getTextColor(isDark), fontFamily: 'Geom, Inter, sans-serif' }}>
            Save your take
          </h1>
          <div className="w-10" />
        </div>

        <ScreenEntrance>
        {/* ── Topic reminder strip ─────────────────────────────────── */}
        <div className="px-4 pb-4">
          <div className="flex items-center gap-3 px-4 py-3 rounded-2xl"
            style={{ background: `${category.accent}14`, border: `1px solid ${category.accent}22` }}>
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

        {/* ── Mood Chips ───────────────────────────────────────────── */}
        <div className="px-4 pb-3">
          <MoodChips mood={null} onChange={() => {}} accent={category.accent} isDark={isDark} />
        </div>

        {/* ── "How do you want to capture this one?" ────────────────── */}
        <div className="px-4 pb-3">
          <p className="text-sm font-semibold mb-2.5" style={{ color: getTextColor(isDark) }}>
            How do you want to capture this one?
          </p>
          {/* Format chips */}
          <div className="flex gap-2 overflow-x-auto pb-1">
            {FORMAT_OPTIONS.map(({ id, label, icon }) => (
              <button key={id} onClick={() => { setSelectedFormat(id); setFormatKey(k => k + 1); }}
                className="flex-shrink-0 flex items-center gap-1.5 px-3.5 py-2 rounded-full text-xs font-semibold transition-all active:scale-95"
                style={{
                  background: selectedFormat === id ? category.accent : `${category.accent}18`,
                  color: selectedFormat === id ? 'white' : category.accent,
                  border: selectedFormat === id ? 'none' : `1px solid ${category.accent}30`,
                }}>
                <MaterialIcon name={icon} size={16} /> {label}
              </button>
            ))}
          </div>
        </div>

        {/* ── Format Body ──────────────────────────────────────────── */}
        <div className="px-4 py-2 max-w-lg mx-auto" key={formatKey}>
          {selectedFormat === 'SoundBite' && (
            <PaperCard isDark={isDark} accent={category.accent} paperColor="cream" paperStyle="ruled">
              <SoundBiteEditor accent={category.accent} isDark={isDark} onCanSave={setCanSave} />
            </PaperCard>
          )}
          {selectedFormat === 'ReelNotes' && (
            <ReelNotesEditor accent={category.accent} isDark={isDark} onCanSave={setCanSave} />
          )}
          {selectedFormat === 'Marginalia' && (
            <MarginaliaEditor accent={category.accent} isDark={isDark} onCanSave={setCanSave} />
          )}
          {selectedFormat === 'GalleryWall' && (
            <GalleryWallEditor accent={category.accent} isDark={isDark} onCanSave={setCanSave} />
          )}
          {selectedFormat === 'FieldNotes' && (
            <FieldNotesEditor accent={category.accent} isDark={isDark} onCanSave={setCanSave} />
          )}
        </div>

        {/* ── Tags ─────────────────────────────────────────────────── */}
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
              className="px-4 py-2 rounded-full text-xs font-semibold active:scale-95"
              style={{ background: category.accent, color: 'white' }}>Add</button>
          </div>
        </div>

        {/* ── Save CTA ─────────────────────────────────────────────── */}
        <div className="px-4 py-4 max-w-lg mx-auto">
          <button onClick={handleSave} disabled={!canSave || isSaving}
            className="w-full py-4 rounded-2xl font-bold text-base transition-all disabled:opacity-40 active:scale-[0.98]"
            style={{
              background: canSave ? category.accent : isDark ? 'rgba(255,255,255,0.08)' : 'rgba(0,0,0,0.04)',
              color: canSave ? 'white' : getTextColor(isDark),
            }}>
            {isSaving ? (
              <span className="flex items-center justify-center gap-2">
                <span className="w-5 h-5 border-2 border-t-transparent rounded-full animate-spin" style={{ borderColor: 'rgba(255,255,255,0.6)', borderTopColor: 'transparent' }} />
                Saving…
              </span>
            ) : (
              <span className="flex items-center justify-center gap-2">
                <MaterialIcon name="check" size={20} /> Save entry
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
