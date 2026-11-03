import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { msw } from 'msw/vite'

export default defineConfig({
  plugins: [
    react(),
    tailwindcss(),
    // F14: o plugin do MSW só serve /mockServiceWorker.js no dev server. Com apply 'serve',
    // ele nem roda no build, então o worker nunca chega ao dist/ (sem cópia em public/).
    { ...msw({ mode: 'worker-only' }), apply: 'serve' },
  ],
  server: { proxy: { '/api': process.env.AGROGESTAO_API_PROXY || 'http://localhost:8080' } },
})
