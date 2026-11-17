import { HttpErrorResponse } from '@angular/common/http';
import { render, screen } from '@testing-library/angular';
import userEvent from '@testing-library/user-event';
import { of, throwError } from 'rxjs';
import { PublicQuoteLinkService } from '../api';
import { PublicoPage } from './publico.page';

const OPEN = {
  shopName: 'Oficina Sintética',
  vehicleLabel: 'Fiat Uno 2014 · ***N34',
  items: [{ description: 'Troca de óleo', kind: 'LABOR', quantity: '1.000', unitPrice: '80.00', lineTotal: '80.00' }],
  total: '131.00',
  status: 'SENT',
  expiresAt: '2026-11-24T02:59:59Z',
};

function renderWith(service: Partial<PublicQuoteLinkService>) {
  return render(PublicoPage, {
    inputs: { token: 'token-sintetico' },
    providers: [{ provide: PublicQuoteLinkService, useValue: service }],
  });
}

describe('PublicoPage', () => {
  it('dado um link aberto, quando carrega, então mostra oficina, veículo, itens, total e validade e nada decidiu ainda', async () => {
    const approve = vi.fn();
    await renderWith({ getPublicQuote: () => of(OPEN) as never, approveQuoteByLink: approve });

    expect(await screen.findByText('Oficina Sintética')).toBeTruthy();
    expect(screen.getByRole('heading', { level: 1 }).textContent).toContain('Fiat Uno 2014 · ***N34');
    expect(screen.getByText('Troca de óleo')).toBeTruthy();
    expect(screen.getByText(/R\$\s131,00/)).toBeTruthy();
    expect(screen.getByText('Vale até 23 de nov.')).toBeTruthy();
    expect(approve).not.toHaveBeenCalled();
  });

  it('dado o link aberto, quando o cliente aprova, então a página mostra que foi aprovado', async () => {
    await renderWith({
      getPublicQuote: () => of(OPEN) as never,
      approveQuoteByLink: () => of({ status: 'APPROVED' }) as never,
    });

    await userEvent.click(await screen.findByRole('button', { name: 'Aprovar orçamento' }));

    expect(await screen.findByText(/Orçamento aprovado/)).toBeTruthy();
    expect(screen.queryByRole('button', { name: 'Aprovar orçamento' })).toBeNull();
  });

  it('dado o link aberto, quando o cliente recusa com motivo, então manda o motivo e mostra a recusa', async () => {
    const reject = vi.fn(() => of({ status: 'REJECTED' }));
    await renderWith({ getPublicQuote: () => of(OPEN) as never, rejectQuoteByLink: reject as never });

    await userEvent.click(await screen.findByRole('button', { name: 'Recusar' }));
    await userEvent.type(screen.getByLabelText(/Quer contar o motivo/), 'Achei caro');
    await userEvent.click(screen.getByRole('button', { name: 'Confirmar recusa' }));

    expect(reject).toHaveBeenCalledWith('token-sintetico', { reason: 'Achei caro' });
    expect(await screen.findByText(/Orçamento recusado/)).toBeTruthy();
  });

  it('dado link indisponível, quando a API responde LINK_UNAVAILABLE, então há uma só mensagem e nenhum botão', async () => {
    await renderWith({
      getPublicQuote: () => throwError(() => new HttpErrorResponse({ status: 404, error: { code: 'LINK_UNAVAILABLE' } })),
    });

    expect(await screen.findByText('Este link não está mais disponível')).toBeTruthy();
    expect(screen.queryByRole('button')).toBeNull();
  });

  it('dado orçamento já fechado, quando abre o link, então não oferece decidir de novo', async () => {
    await renderWith({ getPublicQuote: () => of({ ...OPEN, status: 'APPROVED' }) as never });

    expect(await screen.findByText(/Orçamento aprovado/)).toBeTruthy();
    expect(screen.queryByRole('button', { name: 'Recusar' })).toBeNull();
  });

  it('dado link que a oficina cancelou depois de enviado, quando o estado não é enviado, então avisa que não está aberto', async () => {
    await renderWith({ getPublicQuote: () => of({ ...OPEN, status: 'EXPIRED' }) as never });

    expect(await screen.findByText(/não está mais aberto/)).toBeTruthy();
  });
});
