import { provideRouter } from '@angular/router';
import { render, screen, within } from '@testing-library/angular';
import { of, throwError } from 'rxjs';
import { QuotesService } from '../api';
import { FilaPage } from './fila.page';

function renderWith(listQuotes: () => unknown) {
  return render(FilaPage, {
    providers: [provideRouter([]), { provide: QuotesService, useValue: { listQuotes } }],
  });
}

describe('FilaPage', () => {
  it('dado orçamentos nos três blocos, quando carrega, então cada um aparece no seu bloco com valor e estado', async () => {
    await renderWith(() =>
      of({
        needsOwner: [{ id: 'a', status: 'DRAFT', contactName: 'Cliente Sintético', vehicleLabel: 'Fiat Uno 2014 · ***N34', total: '131.00', createdAt: '2026-11-17T15:00:00Z' }],
        awaitingClient: [{ id: 'b', status: 'SENT', contactName: 'Outro', vehicleLabel: 'VW Gol 2010 · ***K88', total: '80.00', sentAt: '2026-11-16T15:00:00Z', expiresAt: '2026-11-24T02:59:59Z' }],
        closed: [],
      }),
    );

    const needs = within(await screen.findByRole('region', { name: /Precisam de você/ }));
    expect(needs.getByText('Fiat Uno 2014 · ***N34')).toBeTruthy();
    expect(needs.getByText(/R\$\s131,00/)).toBeTruthy();
    expect(needs.getByText('rascunho')).toBeTruthy();
    const waiting = within(screen.getByRole('region', { name: /Esperando o cliente/ }));
    expect(waiting.getByText('vale até 23 de nov.')).toBeTruthy();
    expect(waiting.getByText('enviado')).toBeTruthy();
  });

  it('dado cada orçamento, quando renderiza, então a linha é um link para o detalhe', async () => {
    await renderWith(() => of({ needsOwner: [{ id: 'abc', status: 'DRAFT', vehicleLabel: 'Fiat Uno' }], awaitingClient: [], closed: [] }));

    expect((await screen.findByRole('link', { name: /Fiat Uno/ })).getAttribute('href')).toBe('/orcamento/abc');
  });

  it('dado a fila vazia, quando carrega, então cada bloco orienta o que fazer em vez de ficar em branco', async () => {
    await renderWith(() => of({ needsOwner: [], awaitingClient: [], closed: [] }));

    expect(await screen.findByText('Nada esperando por você.')).toBeTruthy();
    expect(screen.getByText('Nenhum orçamento esperando resposta.')).toBeTruthy();
    expect(screen.getByRole('link', { name: 'Novo orçamento' }).getAttribute('href')).toBe('/orcamento/novo');
  });

  it('dado falha ao carregar, quando a API erra, então mostra um aviso de alerta', async () => {
    await renderWith(() => throwError(() => new Error('rede')));

    expect((await screen.findByRole('alert')).textContent).toContain('Algo deu errado');
  });
});
