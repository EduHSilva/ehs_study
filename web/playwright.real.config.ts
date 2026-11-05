import { defineConfig, devices } from '@playwright/test'

const PORT = 4174
const BASE_URL = `http://127.0.0.1:${PORT}`
const API_BASE_URL = process.env.E2E_API_BASE_URL ?? 'http://localhost'

export default defineConfig({
  testDir: './tests/e2e-real',
  fullyParallel: false,
  workers: 1,
  forbidOnly: !!process.env.CI,
  retries: 0,
  reporter: [['list']],
  use: {
    baseURL: BASE_URL,
    trace: 'on-first-retry',
    locale: 'pt-BR',
    timezoneId: 'America/Sao_Paulo'
  },
  projects: [{ name: 'chromium-real', use: { ...devices['Desktop Chrome'] } }],
  webServer: {
    command: `npm run dev -- --mode test --host 127.0.0.1 --port ${PORT} --strictPort`,
    url: BASE_URL,
    env: { ...process.env, VITE_API_BASE_URL: API_BASE_URL },
    reuseExistingServer: false,
    timeout: 120_000
  }
})
