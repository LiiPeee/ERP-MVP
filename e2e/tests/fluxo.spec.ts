import { expect, Page, test } from '@playwright/test';

// Dados sintéticos e claramente falsos: cada execução cria uma conta nova.
const PASSWORD = 'senha-sintetica-12345';

function newAccount() {
  const unique = `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
  return { email: `e2e-${unique}@exemplo.test`, company: `Padaria Fictícia ${unique}`, owner: 'Ana Fictícia' };
}

async function register(page: Page, account: { email: string; company: string; owner: string }) {
  await page.goto('/register');
  await page.getByLabel('Nome da empresa').fill(account.company);
  await page.getByLabel('Seu nome').fill(account.owner);
  await page.getByLabel('E-mail').fill(account.email);
  await page.getByLabel('Senha (mínimo 12 caracteres)').fill(PASSWORD);
  await page.getByRole('button', { name: 'Cadastrar' }).click();
}

async function login(page: Page, email: string, password: string) {
  await page.goto('/login');
  await page.getByLabel('E-mail').fill(email);
  await page.getByLabel('Senha').fill(password);
  await page.getByRole('button', { name: 'Entrar' }).click();
}

test('cadastrar, ver o contexto, recarregar, sair e entrar de novo', async ({ page }) => {
  const account = newAccount();

  await register(page, account);

  // Depois de entrar, a tela inicial é a fila de orçamentos; "Quem sou eu" fica em /me.
  await expect(page).toHaveURL(/\/fila$/);
  await page.goto('/me');
  await expect(page.getByTestId('email')).toHaveText(account.email);
  await expect(page.getByTestId('display-name')).toHaveText(account.owner);
  await expect(page.getByTestId('company-name')).toHaveText(account.company);
  await expect(page.getByTestId('role')).toHaveText('OWNER');

  // Recarregar perde o token em memória; o cookie HttpOnly do refresh token recupera a sessão.
  await page.reload();
  await expect(page).toHaveURL(/\/me$/);
  await expect(page.getByTestId('email')).toHaveText(account.email);

  await page.getByRole('button', { name: 'Sair' }).click();
  await expect(page).toHaveURL(/\/login$/);
  await page.goto('/me');
  await expect(page).toHaveURL(/\/login$/);

  await login(page, account.email, PASSWORD);
  await expect(page).toHaveURL(/\/fila$/);
  await page.goto('/me');
  await expect(page.getByTestId('company-name')).toHaveText(account.company);
});

test('senha errada mostra a mesma mensagem genérica', async ({ page }) => {
  const account = newAccount();
  await register(page, account);
  await page.getByRole('button', { name: 'Sair' }).click();

  await login(page, account.email, 'senha-errada-sintetica');

  await expect(page.getByTestId('error')).toHaveText('E-mail ou senha incorretos.');
  await expect(page).toHaveURL(/\/login$/);
});

test('e-mail já cadastrado é recusado', async ({ page }) => {
  const account = newAccount();
  await register(page, account);
  await page.getByRole('button', { name: 'Sair' }).click();

  await register(page, account);

  await expect(page.getByTestId('error')).toHaveText('Este e-mail já está cadastrado.');
});

test('quem não entrou não vê a tela de contexto', async ({ page }) => {
  await page.goto('/me');

  await expect(page).toHaveURL(/\/login$/);
});

test('o navegador guarda o token só onde deve e a página não depende de terceiros', async ({ page, context, baseURL }) => {
  const account = newAccount();
  const cspViolations: string[] = [];
  const hosts = new Set<string>();
  page.on('console', (message) => {
    if (/content security policy/i.test(message.text())) {
      cspViolations.push(message.text());
    }
  });
  page.on('request', (request) => hosts.add(new URL(request.url()).host));

  await register(page, account);
  await expect(page).toHaveURL(/\/fila$/);

  const storage = await page.evaluate(() => ({
    local: localStorage.length,
    session: sessionStorage.length,
    readableCookies: document.cookie,
    inlineStyles: document.querySelectorAll('style').length,
    inlineScripts: document.querySelectorAll('script:not([src])').length,
  }));
  expect(storage.local).toBe(0);
  expect(storage.session).toBe(0);
  expect(storage.readableCookies).not.toContain('refresh_token');
  expect(storage.readableCookies).toContain('XSRF-TOKEN');
  expect(storage.inlineStyles).toBe(0);
  expect(storage.inlineScripts).toBe(0);

  const refreshCookie = (await context.cookies()).find((cookie) => cookie.name === 'refresh_token');
  expect(refreshCookie).toMatchObject({ httpOnly: true, secure: true, sameSite: 'Strict', path: '/api/auth' });

  expect([...hosts]).toEqual([new URL(baseURL!).host]);
  expect(cspViolations).toEqual([]);
});
