import AxeBuilder from '@axe-core/playwright';
import { expect, test } from '@playwright/test';

// Protótipo da Fase 0.5 (sem backend). Ex.: E2E_BASE_URL=http://127.0.0.1:4200 npx playwright test tests/design.spec.ts
const pages = ['/caixa', '/componentes'] as const;
const viewports = { desktop: { width: 1280, height: 800 }, celular: { width: 390, height: 844 } };

for (const path of pages) {
  for (const [name, viewport] of Object.entries(viewports)) {
    test(`${path} (${name}): sem violações de acessibilidade WCAG AA`, async ({ page }) => {
      await page.setViewportSize(viewport);
      await page.goto(path);
      await page.getByRole('heading', { level: 1 }).waitFor();

      const results = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa']).analyze();

      expect(results.violations.map((v) => `${v.id}: ${v.help} (${v.nodes.length})`)).toEqual([]);
    });
  }

  test(`${path}: nenhuma requisição sai da própria origem`, async ({ page, baseURL }) => {
    const external: string[] = [];
    page.on('request', (request) => {
      if (!request.url().startsWith(baseURL!) && !request.url().startsWith('data:')) external.push(request.url());
    });

    await page.goto(path);
    await page.getByRole('heading', { level: 1 }).waitFor();
    await page.waitForLoadState('networkidle');

    expect(external).toEqual([]);
  });
}

test.describe('tela do caixa', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/caixa');
    await page.getByRole('heading', { level: 1 }).waitFor();
  });

  test('lançamento rápido completo pelo teclado', async ({ page }) => {
    await page.keyboard.press('n');
    await expect(page.getByLabel('Descrição')).toBeFocused();

    await page.keyboard.type('Venda avulsa fictícia');
    await page.keyboard.press('Tab');
    await page.keyboard.type('1.500,00');
    await page.keyboard.press('Enter');

    await expect(page.getByLabel('Descrição')).toBeHidden();
    await expect(page.getByText('Venda avulsa fictícia')).toBeVisible();
    await expect(page.getByText('+R$ 1.500,00')).toBeVisible();
  });

  test('valor inválido explica o que fazer e Esc fecha o formulário', async ({ page }) => {
    await page.keyboard.press('n');
    await page.keyboard.type('Gás');
    await page.keyboard.press('Tab');
    await page.keyboard.type('abc');
    await page.keyboard.press('Tab');

    await expect(page.getByRole('alert')).toHaveText(/Digite o valor em reais/);

    await page.keyboard.press('Escape');
    await expect(page.getByLabel('Descrição')).toBeHidden();
  });

  test('J e K mudam o dia selecionado', async ({ page }) => {
    await page.keyboard.press('j');
    await expect(page.getByRole('heading', { level: 2, name: 'Terça-feira, 3 de março' })).toBeVisible();
    await page.keyboard.press('k');
    await expect(page.getByRole('heading', { level: 2, name: 'Segunda-feira, 2 de março' })).toBeVisible();
  });

  test('a resposta mostra os dias de caixa e o dia em que o caixa acaba', async ({ page }) => {
    await expect(page.getByRole('heading', { level: 1 })).toContainText('24');
    await expect(page.getByText('O caixa acaba aqui')).toBeVisible();
  });
});

test('com a CSP do backend: nenhuma violação em /caixa, /componentes e com o diálogo aberto', async ({ page }) => {
  // Só vale contra um servidor que envie a CSP (o Spring ou o servidor de teste); no ng serve não há CSP e o teste passa vazio.
  const violations: string[] = [];
  page.on('console', (message) => /content security policy/i.test(message.text()) && violations.push(message.text()));

  await page.goto('/caixa');
  await page.getByRole('heading', { level: 1 }).waitFor();
  await page.goto('/componentes');
  await page.getByRole('heading', { level: 1 }).waitFor();
  await page.getByRole('button', { name: 'Apagar lançamento' }).click();
  await page.getByRole('dialog').waitFor();

  expect(violations).toEqual([]);
  expect(await page.evaluate(() => document.querySelectorAll('style').length)).toBe(0);
});

test.describe('componentes', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/componentes');
    await page.getByRole('heading', { level: 1 }).waitFor();
  });

  test('diálogo: abre, prende o foco, Esc cancela e o foco volta ao botão', async ({ page }) => {
    const opener = page.getByRole('button', { name: 'Apagar lançamento' });
    await opener.click();

    const dialog = page.getByRole('dialog', { name: 'Apagar lançamento?' });
    await expect(dialog).toBeVisible();
    // O foco começa no primeiro botão (o de cancelar, o mais seguro) e, enquanto o diálogo está aberto, nunca chega à página por trás.
    await expect(dialog.getByRole('button', { name: 'Manter' })).toBeFocused();
    for (let i = 0; i < 6; i++) {
      await page.keyboard.press('Tab');
      const behind = await page.evaluate(() => document.activeElement !== document.body && !document.activeElement?.closest('dialog'));
      expect(behind, `Tab ${i + 1} chegou à página por trás do diálogo`).toBe(false);
    }

    await page.keyboard.press('Escape');
    await expect(dialog).toBeHidden();
    await expect(opener).toBeFocused();
    await expect(page.getByText('Você manteve o lançamento.')).toBeVisible();
  });

  test('diálogo aberto: sem violações de acessibilidade WCAG AA', async ({ page }) => {
    await page.getByRole('button', { name: 'Apagar lançamento' }).click();
    await page.getByRole('dialog').waitFor();

    const results = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa']).analyze();

    expect(results.violations.map((v) => `${v.id}: ${v.help} (${v.nodes.length})`)).toEqual([]);
  });

  test('diálogo: confirmar apaga', async ({ page }) => {
    await page.getByRole('button', { name: 'Apagar lançamento' }).click();
    await page.getByRole('button', { name: 'Apagar', exact: true }).click();

    await expect(page.getByText('Você apagou o lançamento.')).toBeVisible();
  });

  test('tabela: filtro e ordenação pelo teclado', async ({ page }) => {
    await page.getByLabel('Filtrar').fill('maquininha');
    const rows = page.getByRole('table').getByRole('row');
    await expect(rows).toHaveCount(1 + 3);

    await page.getByRole('button', { name: /Valor/ }).focus();
    await page.keyboard.press('Enter');
    await expect(page.getByRole('columnheader', { name: /Valor/ })).toHaveAttribute('aria-sort', 'ascending');
  });
});
