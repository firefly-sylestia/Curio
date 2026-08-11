// Curio Web App — Animation System
// Matches Android CurioMotion specs: spring physics, durations, easing

import React, { useState } from 'react';

// ═══════════════════════════════════════════════════════════════════════════
// CurioMotion — duration + easing tokens (mirrors Android CurioMotion.kt)
// ═══════════════════════════════════════════════════════════════════════════

export const CurioMotion = {
  /** Quick interactions: chip toggles, button presses. 150ms */
  Quick: 150,
  /** Default transitions. 300ms */
  Standard: 300,
  /** Larger movements, sheet mounts. 500ms */
  Deliberate: 500,
  /** Shape morphing transitions. 450ms */
  Morph: 450,
  /** Dramatic reveal moments. 650ms */
  Reveal: 650,
  /** Confetti burst lifetime. 600ms */
  Confetti: 600,
  /** Extended confetti for big moments. 1200ms */
  ConfettiLong: 1200,
  /** Breathing ambient pulse. 1800ms */
  Breathe: 1800,
  /** Shimmer sweep. 2000ms */
  Shimmer: 2000,
} as const;

// ═══════════════════════════════════════════════════════════════════════════
// CSS keyframe injection — called once to inject all animations into <head>
// ═══════════════════════════════════════════════════════════════════════════

let _animationsInjected = false;

export function injectAnimationStyles() {
  if (_animationsInjected) return;
  _animationsInjected = true;

  const style = document.createElement('style');
  style.id = 'curio-animations';
  style.textContent = `
    /* ─── Screen Entrance — fade up (Android ScreenEntrance) ──────────── */
    @keyframes curio-screen-fade-in {
      from { opacity: 0; }
      to { opacity: 1; }
    }
    @keyframes curio-screen-slide-up {
      from { opacity: 0; transform: translateY(12.5vh); }
      to { opacity: 1; transform: translateY(0); }
    }

    /* ─── Morph Entrance — scale up + fade (Android MorphEntrance) ────── */
    @keyframes curio-morph-fade-in {
      from { opacity: 0; }
      to { opacity: 1; }
    }
    @keyframes curio-morph-scale-up {
      from { opacity: 0; transform: scale(0.85); }
      to { opacity: 1; transform: scale(1); }
    }

    /* ─── Content Entrance — crossfade + gentle scale (Android ContentEntrance) ── */
    @keyframes curio-content-fade-in {
      from { opacity: 0; transform: translateY(6px) scale(0.96); }
      to { opacity: 1; transform: translateY(0) scale(1); }
    }

    /* ─── Stagger Entrances ────────────────────────────────────────────── */
    @keyframes curio-stagger-fade-up {
      from { opacity: 0; transform: translateY(18px); }
      to { opacity: 1; transform: translateY(0); }
    }

    /* ─── Press Scale (Android Press spring: 0.65/800) ─────────────────── */
    @keyframes curio-press-spring-back {
      0% { transform: scale(0.94); }
      60% { transform: scale(1.03); }
      100% { transform: scale(1); }
    }

    /* ─── Breathing Pulse (Android rememberBreathingScale) ─────────────── */
    @keyframes curio-breathe {
      0%, 100% { transform: scale(0.97); }
      50% { transform: scale(1.03); }
    }

    /* ─── Shimmer Sweep (Android rememberShimmerBrush) ─────────────────── */
    @keyframes curio-shimmer {
      0% { transform: translateX(-100%); }
      100% { transform: translateX(100%); }
    }

    /* ─── Float (ambient pet float) ─────────────────────────────────────── */
    @keyframes curio-float {
      0%, 100% { transform: translateY(0px); }
      50% { transform: translateY(-8px); }
    }

    /* ─── Spin Celebration Pulse ───────────────────────────────────────── */
    @keyframes curio-landing-glow {
      0%, 100% { opacity: 0.6; transform: scale(1); }
      50% { opacity: 1; transform: scale(1.06); }
    }

    /* ─── Dice Tumble ──────────────────────────────────────────────────── */
    @keyframes curio-dice-tumble {
      0% { transform: rotateX(0deg) rotateY(0deg) rotateZ(0deg); }
      25% { transform: rotateX(90deg) rotateY(45deg) rotateZ(180deg); }
      50% { transform: rotateX(180deg) rotateY(90deg) rotateZ(360deg); }
      75% { transform: rotateX(270deg) rotateY(135deg) rotateZ(540deg); }
      100% { transform: rotateX(360deg) rotateY(180deg) rotateZ(720deg); }
    }

    /* ─── Confetti Fly ─────────────────────────────────────────────────── */
    @keyframes curio-confetti-fly {
      0% { opacity: 1; transform: translate(0, 0) rotate(0deg) scale(1); }
      100% { opacity: 0; transform: translate(var(--tx, 80px), var(--ty, -160px)) rotate(var(--rot, 360deg)) scale(0.2); }
    }

    /* ─── Elastic Bounce In (Android Elastic spring: 0.45/340) ─────────── */
    @keyframes curio-elastic-in {
      0% { transform: scale(0.7); opacity: 0; }
      55% { transform: scale(1.08); opacity: 1; }
      75% { transform: scale(0.96); }
      90% { transform: scale(1.02); }
      100% { transform: scale(1); }
    }

    /* ─── Bouncy Settle (Android Bouncy spring: 0.55/380) ──────────────── */
    @keyframes curio-bouncy-in {
      0% { transform: scale(0.8); opacity: 0; }
      50% { transform: scale(1.06); opacity: 1; }
      75% { transform: scale(0.97); }
      100% { transform: scale(1); }
    }

    /* ─── Snappy Scale In (Android Snappy spring: 1.0/1800) ────────────── */
    @keyframes curio-snappy-in {
      from { transform: scale(0.92); opacity: 0; }
      to { transform: scale(1); opacity: 1; }
    }

    /* ─── Slide In From Bottom (sheet / modal) ─────────────────────────── */
    @keyframes curio-slide-up-sheet {
      from { transform: translateY(100%); }
      to { transform: translateY(0); }
    }

    /* ─── Fade + Slide In From Right (nav push) ────────────────────────── */
    @keyframes curio-slide-right-in {
      from { opacity: 0; transform: translateX(30px); }
      to { opacity: 1; transform: translateX(0); }
    }

    /* ─── Spin Orbit Pulse ─────────────────────────────────────────────── */
    @keyframes curio-orbit-pulse {
      0%, 100% { opacity: 0.4; transform: translate(-50%, -50%) scale(1); }
      50% { opacity: 0.9; transform: translate(-50%, -50%) scale(1.5); }
    }

    /* ─── Utility: no-op when prefers-reduced-motion ──────────────────── */
    @media (prefers-reduced-motion: reduce) {
      .curio-animate-entrance,
      .curio-animate-morph,
      .curio-animate-content,
      .curio-animate-stagger,
      .curio-animate-press,
      .curio-animate-breathe,
      .curio-animate-shimmer,
      .curio-animate-float,
      .curio-animate-elastic,
      .curio-animate-bouncy,
      .curio-animate-snappy {
        animation: none !important;
      }
    }
  `;
  document.head.appendChild(style);
}

// ═══════════════════════════════════════════════════════════════════════════
// React Animation Hooks + Components
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Screen entrance — fade + slide up from 1/8 screen height.
 * Matches Android ScreenEntrance: fadeIn(300ms) + slideInVertically(spring 0.85/380, offset=1/8).
 * Wrap your screen's main content (below any sticky top bar).
 */
export const ScreenEntrance: React.FC<{ children: React.ReactNode; className?: string }> = ({ children, className = '' }) => {
  return (
    <div
      className={`curio-animate-entrance ${className}`}
      style={{
        animation: 'curio-screen-slide-up 0.45s cubic-bezier(0.1, 0.7, 0.3, 1) both',
      }}
    >
      {children}
    </div>
  );
};

/**
 * Morph entrance — scale up from 0.85 + fade in with elastic spring.
 * Matches Android MorphEntrance: fadeIn(650ms FastOutSlowIn) + scaleIn(0.85, elastic).
 * Use for hero screens: Topic Reveal, Spin landing.
 */
export const MorphEntrance: React.FC<{ children: React.ReactNode; className?: string }> = ({ children, className = '' }) => {
  return (
    <div
      className={`curio-animate-morph ${className}`}
      style={{
        animation: 'curio-morph-scale-up 0.65s cubic-bezier(0.1, 0.8, 0.2, 1.2) both',
      }}
    >
      {children}
    </div>
  );
};

/**
 * Content entrance — gentle fade + slight slide up + slight scale.
 * Matches Android ContentEntrance: fadeIn + scaleIn(0.92, morph spring).
 */
export const ContentEntrance: React.FC<{ children: React.ReactNode; className?: string }> = ({ children, className = '' }) => {
  return (
    <div
      className={`curio-animate-content ${className}`}
      style={{
        animation: 'curio-content-fade-in 0.4s cubic-bezier(0.1, 0.6, 0.3, 1) both',
      }}
    >
      {children}
    </div>
  );
};

/**
 * Bouncy entrance — scale from 0.8 with overshoot.
 * Matches Android Bouncy spring (0.55/380).
 */
export const BouncyEntrance: React.FC<{ children: React.ReactNode; className?: string }> = ({ children, className = '' }) => {
  return (
    <div
      className={`curio-animate-bouncy ${className}`}
      style={{
        animation: 'curio-bouncy-in 0.5s cubic-bezier(0.1, 0.8, 0.2, 1.2) both',
      }}
    >
      {children}
    </div>
  );
};

/**
 * Elastic entrance — scale from 0.7 with extreme overshoot.
 * Matches Android Elastic spring (0.45/340).
 */
export const ElasticEntrance: React.FC<{ children: React.ReactNode; className?: string }> = ({ children, className = '' }) => {
  return (
    <div
      className={`curio-animate-elastic ${className}`}
      style={{
        animation: 'curio-elastic-in 0.6s cubic-bezier(0.1, 0.9, 0.2, 1.3) both',
      }}
    >
      {children}
    </div>
  );
};

/**
 * Staggered list — wraps children with staggered entrance delays.
 * Each child gets `index * staggerMs` delay.
 */
export const StaggerList: React.FC<{
  children: React.ReactNode;
  staggerMs?: number;
  className?: string;
}> = ({ children, staggerMs = 60, className = '' }) => {
  const kids = React.Children.toArray(children);
  return (
    <>
      {kids.map((child, i) => (
        <div
          key={i}
          className={`curio-animate-stagger ${className}`}
          style={{
            animation: `curio-stagger-fade-up 0.35s cubic-bezier(0.1, 0.6, 0.3, 1) ${i * staggerMs}ms both`,
          }}
        >
          {child}
        </div>
      ))}
    </>
  );
};

/**
 * Pressable — wraps an element with press-scale animation.
 * On mousedown/touchstart → scales to pressedScale, springs back on release.
 * Matches Android Press spring (0.65/800): scale to 0.94 on press.
 */
export const usePressable = (pressedScale: number = 0.94) => {
  const [isPressed, setIsPressed] = useState(false);
  const [isAnimating, setIsAnimating] = useState(false);

  const handlers = {
    onMouseDown: () => { setIsPressed(true); setIsAnimating(false); },
    onMouseUp: () => { setIsPressed(false); setIsAnimating(true); },
    onMouseLeave: () => { if (isPressed) { setIsPressed(false); setIsAnimating(true); } },
    onTouchStart: () => { setIsPressed(true); setIsAnimating(false); },
    onTouchEnd: () => { setIsPressed(false); setIsAnimating(true); },
  };

  const style: React.CSSProperties = isPressed
    ? { transform: `scale(${pressedScale})`, transition: `transform ${CurioMotion.Quick}ms cubic-bezier(0.2, 0, 0, 1)` }
    : isAnimating
    ? {
        transform: 'scale(1)',
        transition: `transform ${CurioMotion.Standard}ms cubic-bezier(0.1, 0.7, 0.2, 1.1)`,
      }
    : { transition: `transform ${CurioMotion.Quick}ms cubic-bezier(0.2, 0, 0, 1)` };

  return { isPressed, handlers, pressStyle: style };
};

/**
 * Breathing — slow ambient pulse. Returns a CSS class and style.
 * Matches Android rememberBreathingScale: 0.97–1.03 over 1800ms.
 */
export const useBreathing = (active: boolean = true, _amplitude: number = 0.03) => {
  if (!active) return { style: {} as React.CSSProperties };
  return {
    style: {
      animation: `curio-breathe ${CurioMotion.Breathe}ms ease-in-out infinite both`,
    } as React.CSSProperties,
  };
};

/**
 * Shimmer sweep — returns style for a sweeping highlight.
 * Matches Android rememberShimmerBrush.
 */
export const useShimmer = (active: boolean = true) => {
  if (!active) return { style: {} as React.CSSProperties };
  return {
    style: {
      position: 'relative' as const,
      overflow: 'hidden',
    } as React.CSSProperties,
    shimmerElement: (
      <div
        className="curio-animate-shimmer absolute inset-0 pointer-events-none"
        style={{
          animation: `curio-shimmer ${CurioMotion.Shimmer}ms linear infinite`,
          background: 'linear-gradient(105deg, transparent 35%, rgba(255,255,255,0.08) 48%, transparent 61%)',
        }}
      />
    ),
  };
};

// ═══════════════════════════════════════════════════════════════════════════
// Auto-inject on import
// ═══════════════════════════════════════════════════════════════════════════

injectAnimationStyles();
