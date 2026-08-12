// Curio Web App - Pet Picker Screen
// Simple pet picker: choose a look for your companion

import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTheme, getBackgroundColor, getTextColor } from '../theme/ThemeContext';
import { getPetSystem } from '../data/PetSystem';
import { getQuestSystem } from '../data/QuestSystem';
import { TornHero, HOME_HERO_SYMBOLS } from '../components/TornHero';
import { CurioWatermarkBackdrop, MaterialIcon } from '../components/SharedComponents';
import { ScreenEntrance } from '../animations';

const PET_HERO_HEIGHT = 200;
const PET_TEAR_SEED = 0xDE70; // Pet-specific seed
const ROSE_WOOD = '#C46B7C';
const CORAL = '#FF8FA3';

// ─── Pixel Pet Sprite (simple canvas render) ─────────────────────────
const BODY_GRID = [
  '....####....',
  '..########..',
  '.##########.',
  '.##########.',
  '.##########.',
  '############',
  '############',
  '############',
  '############',
  '.##########.',
  '.##########.',
  '..######....',
];

const PixelPetSprite: React.FC<{ colors: string[]; size?: number }> = ({ colors, size = 100 }) => {
  const canvasRef = React.useRef<HTMLCanvasElement>(null);
  const bodyColor = colors[0] || '#FFF8E7';

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;
    const px = size / 16;
    ctx.clearRect(0, 0, size, size);

    const dp = (c: number, r: number, color: string, a = 1) => {
      ctx.fillStyle = color; ctx.globalAlpha = a;
      ctx.fillRect((c + 2) * px, r * px, px, px);
      ctx.globalAlpha = 1;
    };

    // Body
    BODY_GRID.forEach((l, r) => { for (let c = 0; c < l.length; c++) if (l[c] === '#') dp(c, r, bodyColor); });
    // Belly
    for (let c = 4; c <= 7; c++) for (let r = 7; r <= 8; r++) dp(c, r, '#FFFBF0', 0.6);
    // Scarf
    for (let c = 3; c <= 8; c++) dp(c, 8, CORAL);
    for (let c = 2; c <= 9; c++) dp(c, 9, CORAL);
    // Feet
    dp(3, 10, '#3D2B1F'); dp(4, 10, '#3D2B1F'); dp(7, 10, '#3D2B1F'); dp(8, 10, '#3D2B1F');
    // Tail
    dp(10, 9, '#3D2B1F'); dp(11, 8, '#3D2B1F');
    // Crown
    dp(5, 2, '#3D2B1F');
    dp(5, 1, '#FFD700'); dp(6, 1, '#FFD700');
    dp(4, 0, '#FFD700'); dp(5, 0, '#DAA520'); dp(6, 0, '#FFD700'); dp(7, 0, '#FFD700');
    // Eyes
    dp(3, 5, 'white'); dp(7, 5, 'white');
    dp(3, 5, '#3D2B1F'); dp(4, 5, '#3D2B1F'); dp(3, 6, '#3D2B1F'); dp(4, 6, '#3D2B1F');
    dp(7, 5, '#3D2B1F'); dp(8, 5, '#3D2B1F'); dp(7, 6, '#3D2B1F'); dp(8, 6, '#3D2B1F');
    // Cheeks
    dp(2, 7, '#FFB5B5', 0.4); dp(3, 7, '#FFB5B5', 0.35);
    dp(8, 7, '#FFB5B5', 0.4); dp(9, 7, '#FFB5B5', 0.35);
    // Mouth
    dp(4, 8, '#3D2B1F'); dp(7, 8, '#3D2B1F'); dp(5, 9, '#3D2B1F'); dp(6, 9, '#3D2B1F');
  }, [colors, size]);

  return <canvas ref={canvasRef} width={size} height={size} style={{ width: size, height: size, imageRendering: 'pixelated' }} />;
};

// ─── Pet look presets ─────────────────────────────────────────────────
const PET_LOOKS = [
  { id: 'classic', name: 'Classic Curio', body: '#FFF8E7', description: 'The original cream-colored companion' },
  { id: 'coral', name: 'Coral Blush', body: '#FFC9C9', description: 'A warm rosy pink companion' },
  { id: 'lavender', name: 'Lavender Dream', body: '#E0D4F5', description: 'A soft purple dreamer' },
  { id: 'mint', name: 'Mint Fresh', body: '#C9F0DD', description: 'A cool green friend' },
  { id: 'sky', name: 'Sky Blue', body: '#C9E0F5', description: 'A bright blue buddy' },
  { id: 'butter', name: 'Butter Cream', body: '#FFF3D4', description: 'A warm golden companion' },
];

export const PetDesignerScreen: React.FC = () => {
  const navigate = useNavigate();
  const { isDark, isAmoled } = useTheme();
  const [petSystem] = useState(() => getPetSystem());
  const [questSystem] = useState(() => getQuestSystem());
  const [selectedLook, setSelectedLook] = useState('classic');
  const [petName, setPetName] = useState(() => localStorage.getItem('curio-pet-name') || 'Curio');
  const [petMood] = useState(() => petSystem.getMood());
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    const look = localStorage.getItem('curio-pet-look') || 'classic';
    setSelectedLook(look);
  }, []);

  const stage = petSystem.getStage();
  const level = questSystem.getLevel();

  const handleSave = () => {
    localStorage.setItem('curio-pet-name', petName);
    localStorage.setItem('curio-pet-look', selectedLook);
    setSaved(true);
    setTimeout(() => setSaved(false), 2000);
  };

  const currentLook = PET_LOOKS.find(l => l.id === selectedLook) || PET_LOOKS[0];
  const spriteSize = stage === 'baby' ? 80 : stage === 'evolved' ? 100 : 120;

  return (
    <div className="min-h-screen pb-24 relative" style={{ backgroundColor: getBackgroundColor(isDark, isAmoled) }}>
      <CurioWatermarkBackdrop topClearance={PET_HERO_HEIGHT + 30} alphaScale={0.45} />

      <TornHero height={PET_HERO_HEIGHT} fill={ROSE_WOOD} ink="#fff" tearSeed={PET_TEAR_SEED} bold={true} symbols={HOME_HERO_SYMBOLS} isDark={isDark}>
        <div className="flex flex-col h-full px-5 pt-[68px] pb-[18px] justify-end items-center">
          <button onClick={() => navigate(-1)} className="absolute top-0 left-5 w-10 h-10 rounded-full flex items-center justify-center"
            style={{ background: 'rgba(255,255,255,0.18)', border: '1px solid rgba(255,255,255,0.25)', marginTop: 'env(safe-area-inset-top, 12px)' }}>
            <MaterialIcon name="arrow_back" size={20} style={{ color: '#fff' }} />
          </button>
          <PixelPetSprite colors={[currentLook.body]} size={spriteSize} />
          <h1 className="text-lg font-extrabold text-white mt-2" style={{ fontFamily: 'Geom, Inter, sans-serif' }}>{petName}</h1>
          <p className="text-xs text-white/70">Level {level} &middot; {stage} &middot; {petMood}</p>
        </div>
      </TornHero>

      <ScreenEntrance>
        <div className="relative z-10 px-4 pt-4 space-y-4 pb-8">
          {/* Name */}
          <div>
            <label className="text-xs font-semibold opacity-50 uppercase tracking-wider mb-1 block" style={{ color: getTextColor(isDark) }}>Name your companion</label>
            <input type="text" value={petName} onChange={e => setPetName(e.target.value.slice(0, 20))}
              className="w-full px-4 py-3 rounded-xl text-sm font-semibold outline-none"
              style={{ background: isDark ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.02)', color: getTextColor(isDark),
                border: `1px solid ${isDark ? 'rgba(255,255,255,0.06)' : 'rgba(0,0,0,0.06)'}` }} />
          </div>

          {/* Look picker */}
          <div>
            <p className="text-xs font-semibold opacity-50 uppercase tracking-wider mb-2" style={{ color: getTextColor(isDark) }}>Pick a look</p>
            <div className="grid grid-cols-3 gap-2">
              {PET_LOOKS.map(look => (
                <button key={look.id} onClick={() => setSelectedLook(look.id)}
                  className="p-3 rounded-2xl flex flex-col items-center gap-2 transition-all active:scale-95"
                  style={{
                    background: selectedLook === look.id ? `${ROSE_WOOD}15` : isDark ? 'rgba(255,255,255,0.03)' : 'rgba(0,0,0,0.015)',
                    border: selectedLook === look.id ? `2px solid ${ROSE_WOOD}` : `1px solid ${isDark ? 'rgba(255,255,255,0.06)' : 'rgba(0,0,0,0.04)'}`,
                  }}>
                  <PixelPetSprite colors={[look.body]} size={48} />
                  <span className="text-[11px] font-semibold text-center leading-tight" style={{ color: getTextColor(isDark) }}>{look.name}</span>
                </button>
              ))}
            </div>
            <p className="text-[11px] mt-2 opacity-40" style={{ color: getTextColor(isDark) }}>{currentLook.description}</p>
          </div>

          {/* Save button */}
          <button onClick={handleSave}
            className="w-full py-4 rounded-2xl font-bold text-base transition-all active:scale-[0.98]"
            style={{ background: saved ? '#4CAF50' : ROSE_WOOD, color: 'white' }}>
            {saved ? (
              <span className="flex items-center justify-center gap-2"><MaterialIcon name="check" size={20} /> Saved!</span>
            ) : (
              <span className="flex items-center justify-center gap-2"><MaterialIcon name="pets" size={20} /> Save companion</span>
            )}
          </button>
        </div>
      </ScreenEntrance>
    </div>
  );
};

export default PetDesignerScreen;
