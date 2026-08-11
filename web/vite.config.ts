import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  // SPA fallback — Vite handles client-side routing automatically with appType:'spa'
  appType: 'spa',
})
