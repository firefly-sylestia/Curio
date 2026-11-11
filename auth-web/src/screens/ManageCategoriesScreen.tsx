// Curio Web App - Manage Categories Screen
// Shows/hides categories and reorders them via up/down arrows

import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTheme, getBackgroundColor, getTextColor } from '../theme/ThemeContext';
import { ALL_CATEGORIES, DEFAULT_ORDER } from '../data/categories';
import {
  MaterialIcon,
} from '../components/SharedComponents';
import { ScreenEntrance } from '../animations';
import type { CategoryId } from '../types';


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
    <div className="curio-manage-categories-screen min-h-screen pb-32 relative" style={{ backgroundColor: getBackgroundColor(isDark, isAmoled) }}>
      <header className="curio-manage-heading">
        <button onClick={() => navigate(-1)} aria-label="Go back"><MaterialIcon name="arrow_back" size={20} /></button>
        <div><p className="curio-screen-kicker">PERSONALISE YOUR DECK</p><h1>Manage categories<span>.</span></h1><p>Choose the subjects you want to see, and put them in your preferred order.</p></div>
        <button className="curio-manage-reset" onClick={resetToDefault}>Reset</button>
      </header>

      <ScreenEntrance>
        <div className="curio-manage-content relative z-10">
          {/* Category list */}
          <div className="space-y-1.5">
            {order.map((catId, index) => {
              const cat = ALL_CATEGORIES.find(c => c.id === catId);
              if (!cat) return null;
              const isVisible = !hidden.has(catId);

              return (
                <div key={catId}
                  className="curio-category-row flex items-center gap-3 p-4 rounded-2xl"
                  style={{
                    background: isDark ? 'rgba(255,255,255,0.05)' : 'rgba(23,27,38,0.03)',
                    opacity: isVisible ? 1 : 0.45,
                  }}>
                  {/* Category icon */}
                  <div className="curio-category-icon w-11 h-11 rounded-2xl flex items-center justify-center flex-shrink-0"
                    style={{ background: `${cat.accent}20` }}>
                    <MaterialIcon name={cat.iconGlyph} size={20} style={{ color: cat.accent }} />
                  </div>

                  {/* Category name */}
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-semibold truncate" style={{ color: getTextColor(isDark) }}>{cat.displayName}</p>
                    <p className="text-[11px] truncate" style={{ color: isDark ? 'rgba(255,255,255,0.4)' : 'rgba(23,27,38,0.4)' }}>{cat.family}</p>
                  </div>

                  {/* Move up */}
                  <button onClick={() => moveUp(index)} disabled={index === 0}
                    className="curio-category-move w-9 h-9 rounded-xl flex items-center justify-center flex-shrink-0"
                    style={{ opacity: index === 0 ? 0.25 : 1 }}>
                    <MaterialIcon name="keyboard_arrow_up" size={20} style={{ color: isDark ? 'rgba(255,255,255,0.6)' : 'rgba(23,27,38,0.6)' }} />
                  </button>

                  {/* Move down */}
                  <button onClick={() => moveDown(index)} disabled={index === order.length - 1}
                    className="w-8 h-8 rounded-lg flex items-center justify-center flex-shrink-0"
                    style={{ opacity: index === order.length - 1 ? 0.25 : 1 }}>
                    <MaterialIcon name="keyboard_arrow_down" size={20} style={{ color: isDark ? 'rgba(255,255,255,0.6)' : 'rgba(23,27,38,0.6)' }} />
                  </button>

                  {/* Toggle visibility */}
                  <button onClick={() => toggleVisibility(catId)}
                    className="curio-category-visibility w-11 h-11 rounded-2xl flex items-center justify-center flex-shrink-0">
                    <MaterialIcon name={isVisible ? 'visibility' : 'visibility_off'} size={20}
                      style={{ color: isVisible ? cat.accent : (isDark ? 'rgba(255,255,255,0.3)' : 'rgba(23,27,38,0.3)') }} />
                  </button>
                </div>
              );
            })}
          </div>

          <div className="mt-6 mb-4 text-center">
            <p className="text-xs" style={{ color: isDark ? 'rgba(255,255,255,0.35)' : 'rgba(23,27,38,0.35)' }}>
              {order.length - hidden.size} visible · {hidden.size} hidden
            </p>
          </div>
        </div>
      </ScreenEntrance>
    </div>
  );
};

export default ManageCategoriesScreen;
