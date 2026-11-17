// Capturas da Fase 0.5 (caixa e componentes), desktop e celular, com os estados que importam.
// Uso: E2E_BASE_URL=http://127.0.0.1:4200 PLAYWRIGHT_CHANNEL=msedge node capturas/capturar-design.mjs
import { chromium } from '@playwright/test';
import { mkdirSync } from 'node:fs';

const BASE = process.env.E2E_BASE_URL ?? 'http://localhost:8080';
const OUT = '../docs/capturas/fase-0.5';
const viewports = { desktop: { width: 1280, height: 800 }, celular: { width: 390, height: 844 } };
mkdirSync(OUT, { recursive: true });

const browser = await chromium.launch({ channel: process.env.PLAYWRIGHT_CHANNEL || undefined });
const external = new Set();

for (const [name, viewport] of Object.entries(viewports)) {
  const context = await browser.newContext({ viewport, locale: 'pt-BR' });
  const page = await context.newPage();
  page.on('request', (request) => {
    if (!request.url().startsWith(BASE) && !request.url().startsWith('data:')) external.add(request.url());
  });
  page.on('console', (message) => message.type() === 'error' && console.log(`console.error (${name}):`, message.text()));
  const shot = (file, fullPage = true) => page.screenshot({ path: `${OUT}/${file}-${name}.png`, fullPage });
  const ready = async () => {
    await page.waitForLoadState('networkidle');
    await page.evaluate(() => document.fonts.ready);
  };

  await page.goto(`${BASE}/caixa`);
  await page.getByRole('heading', { level: 1 }).waitFor();
  await ready();
  await shot('caixa');

  await page.keyboard.press('n');
  await page.waitForFunction(() => document.activeElement?.id?.startsWith('erp-text'));
  await page.keyboard.type('Gás');
  await page.keyboard.press('Tab');
  await page.keyboard.type('abc');
  await page.keyboard.press('Tab');
  await shot('caixa-formulario-com-erro', false);

  await page.goto(`${BASE}/componentes`);
  await page.getByRole('heading', { level: 1 }).waitFor();
  await ready();
  await shot('componentes');

  await page.getByRole('button', { name: 'Apagar lançamento' }).click();
  await page.getByRole('dialog').waitFor();
  await shot('dialogo', false);
  await context.close();
}
await browser.close();
console.log('requisições fora da origem:', external.size === 0 ? 'nenhuma' : [...external]);
