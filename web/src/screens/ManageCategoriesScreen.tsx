// Curio Web App - Manage Categories Screen
// Shows/hides categories and reorders them via up/down arrows

import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTheme, getBackgroundColor, getTextColor } from '../theme/ThemeContext';
import { ALL_CATEGORIES, DEFAULT_ORDER } from '../data/categories';
import {
  CurioWatermarkBackdrop,
  MaterialIcon,
} from '../components/SharedComponents';
import { TornHero, SETTINGS_HERO_SYMBOLS } from '../components/TornHero';
import { ScreenEntrance } from '../animations';
import type { CategoryId } from '../types';

const SETTINGS_HERO_HEIGHT = 180;
const SETTINGS_TEAR_SEED = 0x5EED;
const ROSE_WOOD = '#C46B7C';

const STORAGE_KEY_ORDER = 'curio-category-order';
const STORAGE_KEY_HIDDEN = 'curio-hidden-categories';

const loadOrder = (): CategoryId[] => {
  try {
    const raw = localStorage.getItem(STORAGE_KEY_ORDER);
    if (raw) {
      const parsed = JSON.parse(raw) as string[];
      // Merge with DEFAULT_ORDER to handle newly added categories
      const merged = [...parsed, ...DEFAULT_ORDER.filter(c => !parsed.includes(c))];
      return merged as CategoryId[];
    }
  } catch {}
  return [...DEFAULT_ORDER];
};

const loadHidden = (): Set<string> => {
  try {
    const raw = localStorage.getItem(STORAGE_KEY_HIDDEN);
    if (raw) return new Set(JSON.parse(raw));
  } catch {}
  return new Set();
};

export const ManageCategoriesScreen: React.FC = () => {
  const navigate = useNavigate();
  const { isDark, isAmoled } = useTheme();
  const [order, setOrder] = useState<CategoryId[]>(loadOrder);
  const [hidden, setHidden] = useState<Set<string>>(loadHidden);

  // Persist on change
  useEffect(() => {
    localStorage.setItem(STORAGE_KEY_ORDER, JSON.stringify(order));
  }, [order]);

  useEffect(() => {
    localStorage.setItem(STORAGE_KEY_HIDDEN, JSON.stringify([...hidden]));
  }, [hidden]);

  const toggleVisibility = (id: CategoryId) => {
    setHidden(prev => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const moveUp = (index: number) => {
    if (index <= 0) return;
    setOrder(prev => {
      const next = [...prev];
      [next[index - 1], next[index]] = [next[index], next[index - 1]];
      return next;
    });
  };

  const moveDown = (index: number) => {
    setOrder(prev => {
      if (index >= prev.length - 1) return prev;
      const next = [...prev];
      [next[index], next[index + 1]] = [next[index + 1], next[index]];
      return next;
    });
  };

  const resetToDefault = () => {
    setOrder([...DEFAULT_ORDER]);
    setHidden(new Set());
  };

  return (
    <div className="min-h-screen pb-24 relative" style={{ backgroundColor: getBackgroundColor(isDark, isAmoled) }}>
      <CurioWatermarkBackdrop topClearance={SETTINGS_HERO_HEIGHT + 30} alphaScale={0.45} />

      <TornHero
        height={SETTINGS_HERO_HEIGHT}
        fill={ROSE_WOOD}
        ink="#fff"
        tearSeed={SETTINGS_TEAR_SEED}
        bold={true}
        symbols={SETTINGS_HERO_SYMBOLS}
        isDark={isDark}
      >
        <div className="flex flex-col h-full px-5 pt-[68px] pb-[18px] justify-end">
          <button onClick={() => navigate(-1)} className="absolute top-0 left-5 w-10 h-10 rounded-full flex items-center justify-center"
            style={{ background: 'rgba(255,255,255,0.18)', border: '1px solid rgba(255,255,255,0.25)', marginTop: 'env(safe-area-inset-top, 12px)' }}>
            <MaterialIcon name="arrow_back" size={20} style={{ color: '#fff' }} />
          </button>
          <h1 className="text-xl font-extrabold text-white text-center" style={{ fontFamily: 'Geom, Inter, sans-serif' }}>
            Manage Categories
          </h1>
          <p className="text-xs text-white/70 text-center mt-0.5">Show, hide and reorder your lanes</p>
        </div>
      </TornHero>

      <ScreenEntrance>
        <div className="relative z-10 px-4 pt-4">
          {/* Reset button */}
          <div className="flex justify-end mb-3">
            <button onClick={resetToDefault}
              className="text-xs font-semibold px-3 py-1.5 rounded-lg"
              style={{ color: isDark ? 'rgba(255,255,255,0.6)' : 'rgba(59,10,23,0.6)' }}>
              Reset to default
            </button>
          </div>

          {/* Category list */}
          <div className="space-y-1.5">
            {order.map((catId, index) => {
              const cat = ALL_CATEGORIES.find(c => c.id === catId);
              if (!cat) return null;
              const isVisible = !hidden.has(catId);

              return (
                <div key={catId}
                  className="flex items-center gap-3 p-3 rounded-xl"
                  style={{
                    background: isDark ? 'rgba(255,255,255,0.05)' : 'rgba(59,10,23,0.03)',
                    opacity: isVisible ? 1 : 0.45,
                  }}>
                  {/* Category icon */}
                  <div className="w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0"
                    style={{ background: `${cat.accent}20` }}>
                    <MaterialIcon name={cat.iconGlyph} size={20} style={{ color: cat.accent }} />
                  </div>

                  {/* Category name */}
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-semibold truncate" style={{ color: getTextColor(isDark) }}>{cat.displayName}</p>
                    <p className="text-[11px] truncate" style={{ color: isDark ? 'rgba(255,255,255,0.4)' : 'rgba(59,10,23,0.4)' }}>{cat.family}</p>
                  </div>

                  {/* Move up */}
                  <button onClick={() => moveUp(index)} disabled={index === 0}
                    className="w-8 h-8 rounded-lg flex items-center justify-center flex-shrink-0"
                    style={{ opacity: index === 0 ? 0.25 : 1 }}>
                    <MaterialIcon name="keyboard_arrow_up" size={20} style={{ color: isDark ? 'rgba(255,255,255,0.6)' : 'rgba(59,10,23,0.6)' }} />
                  </button>

                  {/* Move down */}
                  <button onClick={() => moveDown(index)} disabled={index === order.length - 1}
                    className="w-8 h-8 rounded-lg flex items-center justify-center flex-shrink-0"
                    style={{ opacity: index === order.length - 1 ? 0.25 : 1 }}>
                    <MaterialIcon name="keyboard_arrow_down" size={20} style={{ color: isDark ? 'rgba(255,255,255,0.6)' : 'rgba(59,10,23,0.6)' }} />
                  </button>

                  {/* Toggle visibility */}
                  <button onClick={() => toggleVisibility(catId)}
                    className="w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0">
                    <MaterialIcon name={isVisible ? 'visibility' : 'visibility_off'} size={20}
                      style={{ color: isVisible ? cat.accent : (isDark ? 'rgba(255,255,255,0.3)' : 'rgba(59,10,23,0.3)') }} />
                  </button>
                </div>
              );
            })}
          </div>

          <div className="mt-6 mb-4 text-center">
            <p className="text-xs" style={{ color: isDark ? 'rgba(255,255,255,0.35)' : 'rgba(59,10,23,0.35)' }}>
              {order.length - hidden.size} visible · {hidden.size} hidden
            </p>
          </div>
        </div>
      </ScreenEntrance>
    </div>
  );
};

export default ManageCategoriesScreen;
