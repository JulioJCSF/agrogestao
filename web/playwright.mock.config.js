import { defineConfig } from '@playwright/test'
// F14: telas com o mock da API (MSW) ligado no dev server.
export default defineConfig({
  testDir: './tests/browser-mock',
  fullyParallel: false,
  workers: 1,
  use: {
    baseURL: 'http://127.0.0.1:4174',
    headless: true,
    ...(process.env.PLAYWRIGHT_CHANNEL
      ? { channel: process.env.PLAYWRIGHT_CHANNEL }
      : {}),
  },
  webServer: {
    command: 'npm run dev -- --host 127.0.0.1 --port 4174 --strictPort',
    url: 'http://127.0.0.1:4174/login',
    reuseExistingServer: !process.env.CI,
    env: { VITE_USAR_MOCK: 'true' },
  },
})
