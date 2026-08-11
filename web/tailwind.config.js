/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        // Warm pastel foundation
        coral: {
          blush: '#FF8FA3',
          DEFAULT: '#FF8FA3',
        },
        butter: '#FFD97D',
        gold: '#B8860B',
        mint: '#8FE3CF',
        cream: '#FFFBF5',
        'soft-cream': '#F7F0E4',
        'soft-sand': '#F6EFE4',
        'coral-red': '#E4626F',
        'deep-plum': '#3B0A17',
        
        // Category accents
        'cat-indigo': '#4338CA',
        'cat-album': '#5F4DCB',
        'cat-song': '#0E7490',
        'cat-rose': '#BE123C',
        'cat-series': '#BE185D',
        'cat-amber': '#B45309',
        'cat-teal': '#0F766E',
        'cat-sky': '#0369A1',
        'cat-violet': '#7E22CE',
        'cat-manga': '#5B21B6',
        'cat-manhwa': '#9333EA',
        'cat-fuchsia': '#A21CAF',
        'cat-orange': '#C2410C',
        'cat-emerald': '#047857',
        'cat-red': '#B91C1C',
        'cat-blue': '#1D4ED8',
        'cat-coral': '#FF8FA3',
        
        // Light ink variants
        'ink-indigo': '#A5B4FC',
        'ink-album': '#A5B4FC',
        'ink-song': '#67E8F9',
        'ink-rose': '#FDA4AF',
        'ink-series': '#F9A8D4',
        'ink-amber': '#FCD34D',
        'ink-teal': '#5EEAD4',
        'ink-sky': '#7DD3FC',
        'ink-violet': '#C4B5FD',
        'ink-manga': '#A78BFA',
        'ink-manhwa': '#D8B4FE',
        'ink-fuchsia': '#F0ABFC',
        'ink-emerald': '#6EE7B7',
        'ink-orange': '#FDBA74',
        'ink-red': '#FCA5A5',
        'ink-blue': '#93C5FD',
        'ink-coral': '#FFC2CE',
        
        // Home/Profile
        'home-rose': '#CF8B94',
        'home-rose-dark': '#713849',
      },
      fontFamily: {
        'geom': ['Geom', 'sans-serif'],
        'patrick': ['Patrick Hand', 'cursive'],
        'sans': ['Inter', 'system-ui', 'sans-serif'],
      },
      borderRadius: {
        'curio-sm': '16px',
        'curio': '24px',
        'curio-lg': '32px',
        'curio-xl': '48px',
      },
      boxShadow: {
        'curio': '0 4px 24px rgba(0, 0, 0, 0.08)',
        'curio-lg': '0 8px 40px rgba(0, 0, 0, 0.12)',
      },
    },
  },
  plugins: [],
}
