import AxeBuilder from '@axe-core/playwright';
import { expect, test } from '@playwright/test';

// Laboratório da Fase 0.5: roda contra o servidor do Angular (sem backend). Ex.: E2E_BASE_URL=http://127.0.0.1:4200
const directions = ['a', 'b'] as const;
const viewports = { desktop: { width: 1280, height: 800 }, celular: { width: 390, height: 844 } };

for (const direction of directions) {
  for (const [name, viewport] of Object.entries(viewports)) {
    test(`direção ${direction} (${name}): sem violações de acessibilidade WCAG AA`, async ({ page }) => {
      await page.setViewportSize(viewport);
      await page.goto(`/design/${direction}`);
      await page.getByRole('heading', { level: 1 }).waitFor();

      const results = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa']).analyze();

      expect(results.violations.map((v) => `${v.id}: ${v.help} (${v.nodes.length})`)).toEqual([]);
    });
  }

  test(`direção ${direction}: lançamento rápido completo pelo teclado`, async ({ page }) => {
    await page.goto(`/design/${direction}`);
    await page.getByRole('heading', { level: 1 }).waitFor();

    await page.keyboard.press('n');
    await expect(page.getByLabel('Descrição')).toBeFocused();

    await page.keyboard.type('Venda avulsa fictícia');
    await page.keyboard.press('Tab');
    await page.keyboard.type('1.500,00');
    await page.keyboard.press('Enter');

    await expect(page.getByLabel('Descrição')).toBeHidden();
    await expect(page.getByText('Venda avulsa fictícia')).toBeVisible();
  });

  test(`direção ${direction}: valor inválido explica o que fazer`, async ({ page }) => {
    await page.goto(`/design/${direction}`);
    await page.getByRole('heading', { level: 1 }).waitFor();

    await page.keyboard.press('n');
    await page.keyboard.type('Gás');
    await page.keyboard.press('Tab');
    await page.keyboard.type('abc');
    await page.keyboard.press('Enter');

    await expect(page.getByRole('alert')).toHaveText(/Digite o valor em reais/);
    await page.keyboard.press('Escape');
    await expect(page.getByLabel('Descrição')).toBeHidden();
  });
}

test('direção B: J e K mudam o dia selecionado', async ({ page }) => {
  await page.goto('/design/b');
  await page.getByRole('heading', { level: 1 }).waitFor();

  await page.keyboard.press('j');
  await expect(page.getByRole('heading', { level: 2, name: 'Terça-feira, 3 de março' })).toBeVisible();
  await page.keyboard.press('k');
  await expect(page.getByRole('heading', { level: 2, name: 'Segunda-feira, 2 de março' })).toBeVisible();
});
