// Capturas das duas direções (laboratório da Fase 0.5), desktop e celular, e conferência de rede.
// Uso: E2E_BASE_URL=http://127.0.0.1:4200 PLAYWRIGHT_CHANNEL=msedge node capturas/capturar-design.mjs
import { chromium } from '@playwright/test';
import { mkdirSync } from 'node:fs';

const BASE = process.env.E2E_BASE_URL ?? 'http://localhost:8080';
const OUT = '../docs/capturas/fase-0.5';
const viewports = { desktop: { width: 1280, height: 800 }, celular: { width: 390, height: 844 } };
mkdirSync(OUT, { recursive: true });

const browser = await chromium.launch({ channel: process.env.PLAYWRIGHT_CHANNEL || undefined });
const external = new Set();
for (const direction of ['a', 'b']) {
  for (const [name, viewport] of Object.entries(viewports)) {
    const context = await browser.newContext({ viewport, locale: 'pt-BR' });
    const page = await context.newPage();
    page.on('request', (request) => {
      if (!request.url().startsWith(BASE) && !request.url().startsWith('data:')) external.add(request.url());
    });
    page.on('console', (message) => message.type() === 'error' && console.log(`console.error (${direction}/${name}):`, message.text()));
    await page.goto(`${BASE}/design/${direction}`);
    await page.waitForLoadState('networkidle');
    await page.evaluate(() => document.fonts.ready);
    await page.screenshot({ path: `${OUT}/direcao-${direction}-${name}.png`, fullPage: true });
    await context.close();
  }
}
await browser.close();
console.log('requisições fora da origem:', external.size === 0 ? 'nenhuma' : [...external]);
