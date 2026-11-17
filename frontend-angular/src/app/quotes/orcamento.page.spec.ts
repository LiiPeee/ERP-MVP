import { provideRouter } from '@angular/router';
import { render, screen } from '@testing-library/angular';
import userEvent from '@testing-library/user-event';
import { of } from 'rxjs';
import { QuotesService } from '../api';
import { ConfirmDialogService } from '../ui/confirm-dialog.service';
import { OrcamentoPage } from './orcamento.page';

const ITEMS = [{ description: 'Troca de óleo', kind: 'LABOR', quantity: '1.000', unitPrice: '80.00', lineTotal: '80.00' }];
const DRAFT = {
  id: 'q1', status: 'DRAFT', contactName: 'Cliente Sintético', contactPhoneMasked: '(11) *****-4321',
  vehicleLabel: 'Fiat Uno 2014 · ***N34', items: ITEMS, total: '80.00', note: 'Revisão simples',
};

function renderWith(quotes: Partial<QuotesService>, answer = true) {
  return render(OrcamentoPage, {
    inputs: { id: 'q1' },
    providers: [
      provideRouter([]),
      { provide: QuotesService, useValue: quotes },
      { provide: ConfirmDialogService, useValue: { confirm: vi.fn(() => Promise.resolve(answer)) } },
    ],
  });
}

describe('OrcamentoPage', () => {
  it('dado um rascunho, quando carrega, então mostra itens, total calculado pelo servidor e as ações de rascunho', async () => {
    await renderWith({ getQuote: () => of(DRAFT) as never });

    expect(await screen.findByText('Troca de óleo')).toBeTruthy();
    expect(screen.getByText('(11) *****-4321')).toBeTruthy();
    expect(screen.getAllByText(/R\$\s80,00/).length).toBeGreaterThan(0);
    expect(screen.getByRole('button', { name: 'Confirmar e gerar link' })).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Editar itens' })).toBeTruthy();
    expect(screen.queryByRole('button', { name: 'Gerar novo link' })).toBeNull();
  });

  it('dado o dono confirmando, quando o servidor devolve o link, então ele aparece para copiar e o estado é recarregado', async () => {
    const getQuote = vi.fn().mockReturnValueOnce(of(DRAFT)).mockReturnValue(of({ ...DRAFT, status: 'SENT', sentAt: '2026-11-17T15:00:00Z', expiresAt: '2026-11-24T02:59:59Z' }));
    const confirmQuote = vi.fn(() => of({ quoteId: 'q1', publicUrl: 'http://localhost:8080/o/token-sintetico', delivered: false }));
    await renderWith({ getQuote, confirmQuote } as never);

    await userEvent.click(await screen.findByRole('button', { name: 'Confirmar e gerar link' }));

    expect(((await screen.findByLabelText('Link do orçamento')) as HTMLInputElement).value).toBe('http://localhost:8080/o/token-sintetico');
    expect(await screen.findByRole('button', { name: 'Gerar novo link' })).toBeTruthy();
    expect(screen.queryByRole('button', { name: 'Editar itens' })).toBeNull();
  });

  it('dado o dono desistindo no diálogo, quando responde que não, então nada é enviado', async () => {
    const confirmQuote = vi.fn();
    await renderWith({ getQuote: () => of(DRAFT) as never, confirmQuote } as never, false);

    await userEvent.click(await screen.findByRole('button', { name: 'Confirmar e gerar link' }));

    expect(confirmQuote).not.toHaveBeenCalled();
  });

  it('dado um orçamento recusado, quando carrega, então mostra o motivo e não oferece ações', async () => {
    await renderWith({ getQuote: () => of({ ...DRAFT, status: 'REJECTED', decisionReason: 'Achei caro', decidedAt: '2026-11-17T15:00:00Z' }) as never });

    expect(await screen.findByText(/Motivo: Achei caro/)).toBeTruthy();
    expect(screen.queryByRole('button', { name: /Confirmar|Cancelar|Gerar/ })).toBeNull();
  });

  it('dado itens editados com número inválido, quando salva, então o erro aponta o item e nada vai à API', async () => {
    const reviseQuoteItems = vi.fn();
    await renderWith({ getQuote: () => of(DRAFT) as never, reviseQuoteItems } as never);

    await userEvent.click(await screen.findByRole('button', { name: 'Editar itens' }));
    const price = screen.getByLabelText('Preço de cada, em reais');
    await userEvent.clear(price);
    await userEvent.type(price, 'barato');
    await userEvent.click(screen.getByRole('button', { name: 'Salvar itens' }));

    expect((await screen.findByRole('alert')).textContent).toContain('Item 1: o preço');
    expect(reviseQuoteItems).not.toHaveBeenCalled();
  });
});
