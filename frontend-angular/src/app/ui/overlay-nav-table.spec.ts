import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { render, screen, within } from '@testing-library/angular';
import userEvent from '@testing-library/user-event';
import { ConfirmDialogService } from './confirm-dialog.service';
import { DataTableComponent, TableColumn } from './data-table';
import { NavComponent } from './nav';

interface Row {
  id: string;
  name: string;
  cents: number;
}

const rows: Row[] = [
  { id: '1', name: 'Farinha', cents: -215000 },
  { id: '2', name: 'Maquininha', cents: 318000 },
  { id: '3', name: 'Aluguel', cents: -320000 },
];

const columns: TableColumn<Row>[] = [
  { key: 'name', label: 'Descrição', value: (r) => r.name },
  { key: 'cents', label: 'Valor', align: 'end', value: (r) => r.cents, display: (r) => `R$ ${(r.cents / 100).toFixed(2)}` },
];

function bodyNames(): string[] {
  return screen
    .getAllByRole('row')
    .slice(1)
    .map((row) => within(row).getAllByRole('cell')[0].textContent?.trim() ?? '');
}

describe('erp-data-table', () => {
  const inputs = { caption: 'Lançamentos', columns, rows, rowKey: (r: Row) => r.id };

  it('dado linhas e colunas, quando renderiza, então é uma tabela com legenda, cabeçalhos e células', async () => {
    await render(DataTableComponent<Row>, { inputs });

    expect(screen.getByRole('table', { name: 'Lançamentos' })).toBeTruthy();
    expect(screen.getAllByRole('columnheader').map((h) => h.textContent?.trim())).toEqual(['Descrição', 'Valor']);
    expect(bodyNames()).toEqual(['Farinha', 'Maquininha', 'Aluguel']);
  });

  it('dado coluna numérica, quando renderiza, então alinha à direita', async () => {
    await render(DataTableComponent<Row>, { inputs });

    const cell = within(screen.getAllByRole('row')[1]).getAllByRole('cell')[1];
    expect(cell.classList.contains('cell--end')).toBe(true);
  });

  it('dado texto no filtro, quando o usuário digita, então só ficam as linhas que combinam, sem diferenciar acento ou caixa', async () => {
    await render(DataTableComponent<Row>, { inputs });

    await userEvent.type(screen.getByLabelText('Filtrar'), 'MAQUINI');

    expect(bodyNames()).toEqual(['Maquininha']);
  });

  it('dado filtro sem resultado, quando digita, então diz o que fazer', async () => {
    await render(DataTableComponent<Row>, { inputs });

    await userEvent.type(screen.getByLabelText('Filtrar'), 'zzz');

    expect(screen.getByText('Nada encontrado com este filtro. Apague o filtro para ver tudo.')).toBeTruthy();
  });

  it('dado clique no cabeçalho, quando ordena, então alterna crescente e decrescente e informa aria-sort', async () => {
    await render(DataTableComponent<Row>, { inputs });
    const header = screen.getByRole('columnheader', { name: /Valor/ });

    await userEvent.click(within(header).getByRole('button'));
    expect(bodyNames()).toEqual(['Aluguel', 'Farinha', 'Maquininha']);
    expect(header.getAttribute('aria-sort')).toBe('ascending');

    await userEvent.click(within(header).getByRole('button'));
    expect(bodyNames()).toEqual(['Maquininha', 'Farinha', 'Aluguel']);
    expect(header.getAttribute('aria-sort')).toBe('descending');
  });
});

describe('erp-nav', () => {
  const items = [
    { label: 'Caixa', link: '/caixa' },
    { label: 'Quem sou eu', link: '/me' },
  ];

  it('dado itens, quando renderiza, então é uma navegação rotulada com links', async () => {
    await render(NavComponent, { inputs: { items }, routes: [{ path: 'caixa', children: [] }, { path: 'me', children: [] }] });

    const nav = screen.getByRole('navigation', { name: 'Principal' });
    expect(within(nav).getAllByRole('link').map((l) => l.textContent?.trim())).toEqual(['Caixa', 'Quem sou eu']);
  });

  it('dado a rota atual, quando navega, então só o link ativo marca aria-current', async () => {
    await render(NavComponent, { inputs: { items }, routes: [{ path: 'caixa', children: [] }, { path: 'me', children: [] }] });

    await TestBed.inject(Router).navigateByUrl('/caixa');

    expect(screen.getByRole('link', { name: 'Caixa' }).getAttribute('aria-current')).toBe('page');
    expect(screen.getByRole('link', { name: 'Quem sou eu' }).getAttribute('aria-current')).toBeNull();
  });
});

describe('diálogo de confirmação (CDK)', () => {
  const request = { title: 'Apagar lançamento?', message: 'Esta ação não pode ser desfeita.', confirmLabel: 'Apagar', cancelLabel: 'Manter' };

  it('dado um pedido, quando abre, então é um diálogo modal com título e foco dentro', async () => {
    await render('<div></div>', {});
    const service = TestBed.inject(ConfirmDialogService);

    const result = service.confirm(request);

    const dialog = await screen.findByRole('dialog', { name: 'Apagar lançamento?' });
    expect(dialog.textContent).toContain('Esta ação não pode ser desfeita.');
    expect(dialog.contains(document.activeElement)).toBe(true);
    await userEvent.click(screen.getByRole('button', { name: 'Manter' }));
    expect(await result).toBe(false);
  });

  it('dado o botão de confirmar, quando clica, então devolve verdadeiro e fecha', async () => {
    await render('<div></div>', {});
    const result = TestBed.inject(ConfirmDialogService).confirm(request);

    await userEvent.click(await screen.findByRole('button', { name: 'Apagar' }));

    expect(await result).toBe(true);
    expect(screen.queryByRole('dialog')).toBeNull();
  });

  it('dado Esc, quando o diálogo está aberto, então cancela', async () => {
    await render('<div></div>', {});
    const result = TestBed.inject(ConfirmDialogService).confirm(request);
    await screen.findByRole('dialog');

    // O CDK confere keyCode 27; o jsdom não o preenche sozinho, então o evento leva o código do Esc de um navegador real.
    (document.activeElement ?? document.body).dispatchEvent(
      new KeyboardEvent('keydown', { key: 'Escape', code: 'Escape', keyCode: 27, bubbles: true }),
    );

    expect(await result).toBe(false);
  });
});
