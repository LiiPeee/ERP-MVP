import { defineConfig } from '@playwright/test';

/**
 * Roda contra a aplicação inteira (Docker Compose): o Spring serve o Angular e a API na mesma origem.
 * Localmente, sem baixar navegador, use o Edge instalado: PLAYWRIGHT_CHANNEL=msedge. No CI, o Chromium do Playwright.
 */
export default defineConfig({
  testDir: './tests',
  workers: 1,
  fullyParallel: false,
  timeout: 45_000,
  expect: { timeout: 10_000 },
  reporter: [['list']],
  use: {
    baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:8080',
    channel: process.env.PLAYWRIGHT_CHANNEL || undefined,
    trace: 'retain-on-failure',
    locale: 'pt-BR',
  },
});
