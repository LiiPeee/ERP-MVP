import { Router, provideRouter } from '@angular/router';
import { TestBed } from '@angular/core/testing';
import { render, screen } from '@testing-library/angular';
import userEvent from '@testing-library/user-event';
import { of } from 'rxjs';
import { ContactsService, QuotesService, VehiclesService } from '../api';
import { NovoPage } from './novo.page';

const CONTACT = { id: 'c1', name: 'Cliente Sintético', phoneMasked: '(11) *****-4321' };
const VEHICLE = { id: 'v1', contactId: 'c1', plateMasked: '****N34', brand: 'Fiat', model: 'Uno', year: 2014 };

function renderPage(overrides: { contacts?: object; vehicles?: object; quotes?: object } = {}) {
  const contacts = { listContacts: vi.fn(() => of([CONTACT])), createContact: vi.fn(() => of(CONTACT)), ...overrides.contacts };
  const vehicles = { listVehicles: vi.fn(() => of([VEHICLE])), registerVehicle: vi.fn(() => of(VEHICLE)), ...overrides.vehicles };
  const quotes = { createQuoteDraft: vi.fn(() => of({ id: 'q9' })), ...overrides.quotes };
  return {
    contacts, vehicles, quotes,
    rendered: render(NovoPage, {
      providers: [
        provideRouter([]),
        { provide: ContactsService, useValue: contacts },
        { provide: VehiclesService, useValue: vehicles },
        { provide: QuotesService, useValue: quotes },
      ],
    }),
  };
}

describe('NovoPage', () => {
  it('dado a tela aberta, quando carrega, então só o passo do cliente aparece', async () => {
    renderPage();

    expect(await screen.findByRole('heading', { name: '1. Cliente' })).toBeTruthy();
    expect(screen.queryByRole('heading', { name: '2. Veículo' })).toBeNull();
    expect(screen.queryByRole('heading', { name: '3. Itens' })).toBeNull();
  });

  it('dado um cliente da lista, quando escolhe e escolhe o veículo, então os passos seguintes abrem em ordem', async () => {
    renderPage();

    await userEvent.click(await screen.findByRole('button', { name: /Cliente Sintético/ }));
    expect(await screen.findByRole('heading', { name: '2. Veículo' })).toBeTruthy();
    expect(screen.queryByRole('heading', { name: '3. Itens' })).toBeNull();
    await userEvent.click(await screen.findByRole('button', { name: /Fiat Uno 2014/ }));

    expect(await screen.findByRole('heading', { name: '3. Itens' })).toBeTruthy();
  });

  it('dado cliente novo, quando cadastra, então manda nome e telefone e já passa ao veículo', async () => {
    const { contacts } = renderPage({ contacts: { listContacts: vi.fn(() => of([])) } });

    await userEvent.type(await screen.findByLabelText('Nome'), 'Cliente Sintético');
    await userEvent.type(screen.getByLabelText('Telefone com DDD'), '(11) 94321-0000');
    await userEvent.click(screen.getByRole('button', { name: 'Cadastrar cliente' }));

    expect(contacts.createContact).toHaveBeenCalledWith({ name: 'Cliente Sintético', phone: '(11) 94321-0000' });
    expect(await screen.findByRole('heading', { name: '2. Veículo' })).toBeTruthy();
  });

  it('dado veículo novo, quando cadastra, então a placa vai em maiúsculas e o ano como número', async () => {
    const { vehicles } = renderPage({ vehicles: { listVehicles: vi.fn(() => of([])) } });

    await userEvent.click(await screen.findByRole('button', { name: /Cliente Sintético/ }));
    await userEvent.type(await screen.findByLabelText('Placa'), 'abc1d23');
    await userEvent.type(screen.getByLabelText('Marca'), 'Fiat');
    await userEvent.type(screen.getByLabelText('Modelo'), 'Uno');
    await userEvent.type(screen.getByLabelText('Ano'), '2014');
    await userEvent.click(screen.getByRole('button', { name: 'Cadastrar veículo' }));

    expect(vehicles.registerVehicle).toHaveBeenCalledWith('c1', { plate: 'ABC1D23', brand: 'Fiat', model: 'Uno', year: 2014 });
  });

  it('dado cliente, veículo e itens, quando salva o rascunho, então cria e vai para o orçamento', async () => {
    const { quotes } = renderPage();
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    await userEvent.click(await screen.findByRole('button', { name: /Cliente Sintético/ }));
    await userEvent.click(await screen.findByRole('button', { name: /Fiat Uno 2014/ }));
    await userEvent.type(await screen.findByLabelText('Item 1'), 'Troca de óleo');
    await userEvent.type(screen.getByLabelText('Preço de cada, em reais'), '80');
    await userEvent.click(screen.getByRole('button', { name: 'Salvar rascunho' }));

    expect(quotes.createQuoteDraft).toHaveBeenCalledWith({
      contactId: 'c1', vehicleId: 'v1', note: undefined,
      items: [{ description: 'Troca de óleo', kind: 'LABOR', quantity: '1', unitPrice: '80' }],
    });
    await vi.waitFor(() => expect(navigate).toHaveBeenCalledWith(['/orcamento', 'q9']));
  });

  it('dado item sem preço, quando salva, então avisa qual item e não chama a API', async () => {
    const { quotes } = renderPage();

    await userEvent.click(await screen.findByRole('button', { name: /Cliente Sintético/ }));
    await userEvent.click(await screen.findByRole('button', { name: /Fiat Uno 2014/ }));
    await userEvent.type(await screen.findByLabelText('Item 1'), 'Troca de óleo');
    await userEvent.click(screen.getByRole('button', { name: 'Salvar rascunho' }));

    expect((await screen.findByRole('alert')).textContent).toContain('Item 1: o preço');
    expect(quotes.createQuoteDraft).not.toHaveBeenCalled();
  });
});
