import AxeBuilder from '@axe-core/playwright';
import { Browser, BrowserContext, devices, expect, Page, test } from '@playwright/test';
import { mkdirSync } from 'node:fs';

// Dados sintéticos e claramente falsos: cada execução cria uma oficina nova.
const PASSWORD = 'senha-sintetica-12345';
const CAPTURAS = process.env.E2E_CAPTURAS ? 'capturas/fase-1' : null;

let counter = 0;
function unique(): string {
  counter += 1;
  return `${Date.now()}-${counter}-${Math.random().toString(36).slice(2, 6)}`;
}

/** Telefone e placa únicos e sintéticos: o telefone fixo e o da placa vêm do contador. */
function syntheticPhone(): string {
  const digits = String(10_000_000 + (Date.now() % 80_000_000)).padStart(8, '0');
  return `(11) 9${digits.slice(0, 4)}-${digits.slice(4)}`;
}

function syntheticPlate(): string {
  const letters = 'ABCDEFGHJKLMNPQRSTUVWXYZ';
  const pick = () => letters[Math.floor(Math.random() * letters.length)];
  return `${pick()}${pick()}${pick()}${Math.floor(Math.random() * 10)}${pick()}${Math.floor(Math.random() * 90 + 10)}`;
}

async function capture(page: Page, name: string): Promise<void> {
  if (CAPTURAS) {
    mkdirSync(CAPTURAS, { recursive: true });
    await page.screenshot({ path: `${CAPTURAS}/${name}.png`, fullPage: true });
  }
}

async function expectNoAxeViolations(page: Page): Promise<void> {
  const result = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa']).analyze();
  expect(result.violations.map((violation) => `${violation.id}: ${violation.help}`)).toEqual([]);
}

async function registerOwner(page: Page): Promise<void> {
  const id = unique();
  await page.goto('/register');
  await page.getByLabel('Nome da empresa').fill(`Oficina Sintética ${id}`);
  await page.getByLabel('Seu nome').fill('Dono Sintético');
  await page.getByLabel('E-mail').fill(`e2e-${id}@exemplo.test`);
  await page.getByLabel('Senha (mínimo 12 caracteres)').fill(PASSWORD);
  await page.getByRole('button', { name: 'Cadastrar' }).click();
  await expect(page).toHaveURL(/\/fila$/);
}

/** Cria cliente, veículo e rascunho com dois itens pela tela, como o dono faria. */
async function createDraft(page: Page): Promise<void> {
  await page.getByRole('link', { name: 'Novo orçamento' }).click();
  await page.getByLabel('Nome', { exact: true }).fill('Cliente Sintético');
  await page.getByLabel('Telefone com DDD').fill(syntheticPhone());
  await page.getByRole('button', { name: 'Cadastrar cliente' }).click();
  await page.getByLabel('Placa').fill(syntheticPlate());
  await page.getByLabel('Marca').fill('Fiat');
  await page.getByLabel('Modelo').fill('Uno');
  await page.getByLabel('Ano').fill('2014');
  await page.getByRole('button', { name: 'Cadastrar veículo' }).click();
  await page.getByLabel('Item 1', { exact: true }).fill('Troca de óleo');
  await page.getByLabel('Preço de cada, em reais').fill('80');
  await page.getByRole('button', { name: 'Adicionar item' }).click();
  await page.getByLabel('Item 2', { exact: true }).fill('Filtro de óleo');
  await page.getByLabel('Quantidade').nth(1).fill('2');
  await page.getByLabel('Preço de cada, em reais').nth(1).fill('25,50');
  await page.getByRole('button', { name: 'Salvar rascunho' }).click();
  await expect(page).toHaveURL(/\/orcamento\/[0-9a-f-]{36}$/);
}

/** Confirma pelo diálogo e devolve o caminho do link (o host é o da própria aplicação). */
async function confirmAndGetLinkPath(page: Page): Promise<string> {
  await page.getByRole('button', { name: 'Confirmar e gerar link' }).click();
  await page.getByRole('dialog').getByRole('button', { name: 'Confirmar e gerar link' }).click();
  const link = await page.getByLabel('Link do orçamento').inputValue();
  return new URL(link).pathname;
}

async function clientContext(browser: Browser): Promise<BrowserContext> {
  return browser.newContext({ ...devices['Pixel 7'], locale: 'pt-BR' });
}

test('do rascunho à aprovação pelo link, aberto no celular do cliente', async ({ page, browser }) => {
  await registerOwner(page);
  await expect(page.getByText('Nada esperando por você.')).toBeVisible();
  await capture(page, 'fila-vazia-desktop');
  await createDraft(page);

  // O total vem do servidor: 1 × 80,00 + 2 × 25,50.
  await expect(page.getByText(/R\$\s131,00/).first()).toBeVisible();
  await expectNoAxeViolations(page);
  await capture(page, 'orcamento-rascunho-desktop');
  const path = await confirmAndGetLinkPath(page);
  await expect(page.getByRole('button', { name: 'Gerar novo link' })).toBeVisible();
  await capture(page, 'orcamento-enviado-desktop');

  const client = await clientContext(browser);
  const phone = await client.newPage();
  const violations: string[] = [];
  phone.on('console', (message) => {
    if (/content security policy/i.test(message.text())) {
      violations.push(message.text());
    }
  });
  await phone.goto(path);
  await expect(phone.getByRole('heading', { level: 1 })).toContainText('Fiat Uno 2014');
  await expect(phone.getByText('Oficina Sintética')).toBeVisible();
  await expect(phone.getByText(/R\$\s131,00/).first()).toBeVisible();
  await expect(phone.getByRole('navigation')).toHaveCount(0);
  await expectNoAxeViolations(phone);
  await capture(phone, 'publica-celular');

  // Abrir não decide nada: recarregar continua mostrando as duas ações.
  await phone.reload();
  await expect(phone.getByRole('button', { name: 'Aprovar orçamento' })).toBeVisible();
  await phone.getByRole('button', { name: 'Aprovar orçamento' }).click();
  await expect(phone.getByText(/Orçamento aprovado/)).toBeVisible();
  await capture(phone, 'publica-aprovada-celular');
  await phone.reload();
  await expect(phone.getByText(/Orçamento aprovado/)).toBeVisible();
  expect(violations).toEqual([]);
  await client.close();

  await page.goto('/fila');
  await expect(page.getByRole('region', { name: /Fechados/ }).getByText('aprovado')).toBeVisible();
  await capture(page, 'fila-com-orcamentos-desktop');
});

test('a fila no celular do dono: sem rolagem lateral e com alvos de toque grandes', async ({ page }) => {
  await registerOwner(page);
  await createDraft(page);
  // Celular: a mesma sessão do dono, com a janela do tamanho de um telefone.
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto('/fila');
  await expect(page.getByRole('region', { name: /Precisam de você/ }).getByRole('link')).toBeVisible();
  const overflow = await page.evaluate(() => document.documentElement.scrollWidth > document.documentElement.clientWidth);
  expect(overflow).toBe(false);
  const row = await page.getByRole('region', { name: /Precisam de você/ }).getByRole('link').boundingBox();
  expect(row!.height).toBeGreaterThanOrEqual(48);
  await expectNoAxeViolations(page);
  await capture(page, 'fila-com-orcamentos-celular');
  await page.goto('/orcamento/novo');
  await capture(page, 'novo-celular');
  await page.goto('/oficina');
  await expectNoAxeViolations(page);
  await capture(page, 'oficina-celular');
});

test('link que não existe e link de orçamento cancelado mostram a mesma mensagem, sem botões', async ({ page, browser }) => {
  await registerOwner(page);
  await createDraft(page);
  const path = await confirmAndGetLinkPath(page);
  await page.getByRole('button', { name: 'Cancelar orçamento' }).click();
  await page.getByRole('dialog').getByRole('button', { name: 'Cancelar orçamento' }).click();
  await expect(page.getByText('cancelado')).toBeVisible();

  const client = await clientContext(browser);
  const phone = await client.newPage();
  for (const target of [path, `/o/${'A'.repeat(43)}`, '/o/curto']) {
    await phone.goto(target);
    await expect(phone.getByText('Este link não está mais disponível')).toBeVisible();
    await expect(phone.getByRole('button')).toHaveCount(0);
  }
  await capture(phone, 'publica-indisponivel-celular');
  await client.close();
});

test('as respostas do link público não vão para cache, buscador nem Referer', async ({ page, request }) => {
  await registerOwner(page);
  await createDraft(page);
  const path = await confirmAndGetLinkPath(page);
  const token = path.split('/').pop();

  for (const url of [`/api/public/quotes/${token}`, path]) {
    const response = await request.get(url);
    expect(response.headers()['cache-control']).toContain('no-store');
    expect(response.headers()['x-robots-tag']).toContain('noindex');
    expect(response.headers()['referrer-policy']).toBe('no-referrer');
  }
});

test('o dono recusado vê o motivo na fila e no detalhe', async ({ page, browser }) => {
  await registerOwner(page);
  await createDraft(page);
  const path = await confirmAndGetLinkPath(page);

  const client = await clientContext(browser);
  const phone = await client.newPage();
  await phone.goto(path);
  await phone.getByRole('button', { name: 'Recusar' }).click();
  await phone.getByLabel(/Quer contar o motivo/).fill('Achei caro');
  await phone.getByRole('button', { name: 'Confirmar recusa' }).click();
  await expect(phone.getByText(/Orçamento recusado/)).toBeVisible();
  await client.close();

  await page.reload();
  await expect(page.getByText(/Motivo: Achei caro/)).toBeVisible();
  await page.goto('/fila');
  await expect(page.getByRole('region', { name: /Fechados/ }).getByText('recusado')).toBeVisible();
});

test('quem não entrou não abre as telas do dono, mas abre o link', async ({ page }) => {
  for (const path of ['/fila', '/orcamento/novo', '/oficina']) {
    await page.goto(path);
    await expect(page).toHaveURL(/\/login$/);
  }
  await page.goto(`/o/${'B'.repeat(43)}`);
  await expect(page).toHaveURL(/\/o\//);
});
