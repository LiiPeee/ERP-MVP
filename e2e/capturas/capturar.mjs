// Tira capturas das telas da Fase 0 (desktop e celular) contra a pilha do Compose.
// Uso: PLAYWRIGHT_CHANNEL=msedge node capturas/capturar.mjs   (dentro de e2e/)
import { chromium } from '@playwright/test';
import { mkdirSync } from 'node:fs';

const BASE = process.env.E2E_BASE_URL ?? 'http://localhost:8080';
const OUT = '../docs/capturas/fase-0';
const PASSWORD = 'senha-sintetica-12345';
const unique = `${Date.now()}`;
const account = { email: `captura-${unique}@exemplo.test`, company: `Padaria Fictícia ${unique}`, owner: 'Ana Fictícia' };
const viewports = { desktop: { width: 1280, height: 800 }, celular: { width: 390, height: 844 } };

mkdirSync(OUT, { recursive: true });
const browser = await chromium.launch({ channel: process.env.PLAYWRIGHT_CHANNEL || undefined });

for (const [name, viewport] of Object.entries(viewports)) {
  const context = await browser.newContext({ viewport, locale: 'pt-BR' });
  const page = await context.newPage();
  const shot = (screen) => page.screenshot({ path: `${OUT}/${screen}-${name}.png`, fullPage: true });
  const email = name === 'desktop' ? account.email : `celular-${account.email}`;

  await page.goto(`${BASE}/register`);
  await page.getByLabel('Nome da empresa').waitFor();
  await shot('cadastro');

  await page.getByLabel('Nome da empresa').fill(account.company);
  await page.getByLabel('Seu nome').fill(account.owner);
  await page.getByLabel('E-mail').fill(email);
  await page.getByLabel('Senha (mínimo 12 caracteres)').fill(PASSWORD);
  await page.getByRole('button', { name: 'Cadastrar' }).click();
  await page.getByTestId('email').waitFor();
  await shot('eu');

  await page.getByRole('button', { name: 'Sair' }).click();
  await page.getByLabel('E-mail').waitFor();
  await shot('entrar');

  await page.getByLabel('E-mail').fill(email);
  await page.getByLabel('Senha').fill('senha-errada-sintetica');
  await page.getByRole('button', { name: 'Entrar' }).click();
  await page.getByTestId('error').waitFor();
  await shot('entrar-erro');
  await context.close();
}
await browser.close();
console.log('capturas em docs/capturas/fase-0');
