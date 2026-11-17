import { createCashModel } from './cash-model';
import { FIXTURE_ENTRIES, OPENING_BALANCE_CENTS, TODAY } from './fixture';

describe('createCashModel (fixture)', () => {
  it('dado a fixture, quando cria, então o saldo de hoje já inclui os atrasados, que vencem hoje', () => {
    const model = createCashModel();
    const overdue = FIXTURE_ENTRIES.filter((entry) => entry.date < TODAY).reduce((sum, e) => sum + e.amountCents, 0);
    const today = FIXTURE_ENTRIES.filter((entry) => entry.date === TODAY).reduce((sum, e) => sum + e.amountCents, 0);

    expect(model.series()[0].date).toBe(TODAY);
    expect(model.series()[0].balanceCents).toBe(OPENING_BALANCE_CENTS + overdue + today);
  });

  it('dado a fixture, quando calcula, então o caixa acaba em 24 dias (26 de março)', () => {
    expect(createCashModel().cashDays()).toBe(24);
  });

  it('dado um lançamento rápido, quando adiciona, então entra na lista, no saldo e muda os dias de caixa', () => {
    const model = createCashModel();

    model.add({ date: '2026-03-03', description: 'Venda avulsa fictícia', amountCents: 500_000 });

    expect(model.entries().some((entry) => entry.description === 'Venda avulsa fictícia')).toBe(true);
    expect(model.cashDays()).toBeGreaterThan(24);
  });

  it('dado valor negativo no lançamento rápido, quando adiciona, então é conta a pagar', () => {
    const model = createCashModel();

    model.add({ date: '2026-03-03', description: 'Teste', amountCents: -1_000 });

    expect(model.entries().find((entry) => entry.description === 'Teste')?.kind).toBe('payable');
  });

  it('dado atrasados, quando agrupa, então ficam num grupo à parte com o total', () => {
    const model = createCashModel();

    expect(model.overdue().map((entry) => entry.id)).toEqual(['r00', 'p00']);
    expect(model.overdueTotalCents()).toBe(95_000 - 48_000);
  });
});
