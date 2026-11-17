import { CashEntry, daysOfCash, groupByDay, projectBalance } from './projection';

function entry(id: string, date: string, amountCents: number, description = id): CashEntry {
  return { id, date, amountCents, description, kind: amountCents >= 0 ? 'receivable' : 'payable' };
}

describe('projectBalance', () => {
  it('dado saldo inicial e lançamentos, quando projeta, então acumula dia a dia incluindo dias vazios', () => {
    const series = projectBalance(1000, [entry('a', '2026-03-02', -400), entry('b', '2026-03-04', 900)], '2026-03-01', '2026-03-04');

    expect(series.map((day) => [day.date, day.balanceCents])).toEqual([
      ['2026-03-01', 1000],
      ['2026-03-02', 600],
      ['2026-03-03', 600],
      ['2026-03-04', 1500],
    ]);
  });

  it('dado lançamento antes do início, quando projeta, então ignora (já está no saldo inicial)', () => {
    const series = projectBalance(500, [entry('velho', '2026-02-20', -9999)], '2026-03-01', '2026-03-02');

    expect(series.map((day) => day.balanceCents)).toEqual([500, 500]);
  });

  it('dado lançamento depois do fim, quando projeta, então ignora', () => {
    const series = projectBalance(500, [entry('futuro', '2026-04-01', -9999)], '2026-03-01', '2026-03-02');

    expect(series.map((day) => day.balanceCents)).toEqual([500, 500]);
  });

  it('dado vários lançamentos no mesmo dia, quando projeta, então soma todos e guarda a lista', () => {
    const series = projectBalance(0, [entry('a', '2026-03-01', 300), entry('b', '2026-03-01', -100)], '2026-03-01', '2026-03-01');

    expect(series[0].balanceCents).toBe(200);
    expect(series[0].entries.map((e) => e.id)).toEqual(['a', 'b']);
  });

  it('dado fim antes do início, quando projeta, então rejeita', () => {
    expect(() => projectBalance(0, [], '2026-03-02', '2026-03-01')).toThrow();
  });
});

describe('daysOfCash', () => {
  const start = '2026-03-01';

  it('dado saldo que fica negativo no dia 5, quando calcula, então são 4 dias de caixa', () => {
    const series = projectBalance(1000, [entry('a', '2026-03-05', -1500)], start, '2026-03-10');

    expect(daysOfCash(series)).toBe(4);
  });

  it('dado saldo que nunca fica negativo no horizonte, quando calcula, então devolve nulo', () => {
    const series = projectBalance(1000, [entry('a', '2026-03-05', -500)], start, '2026-03-10');

    expect(daysOfCash(series)).toBeNull();
  });

  it('dado saldo que já começa negativo, quando calcula, então são zero dias', () => {
    const series = projectBalance(-1, [], start, '2026-03-03');

    expect(daysOfCash(series)).toBe(0);
  });

  it('dado saldo exatamente zero, quando calcula, então ainda não é negativo', () => {
    const series = projectBalance(500, [entry('a', '2026-03-03', -500)], start, '2026-03-05');

    expect(daysOfCash(series)).toBeNull();
  });
});

describe('groupByDay', () => {
  it('dado lançamentos fora de ordem, quando agrupa, então ordena por dia e só traz dias com lançamento', () => {
    const groups = groupByDay([entry('b', '2026-03-04', 100), entry('a', '2026-03-02', -50), entry('c', '2026-03-04', -30)]);

    expect(groups.map((g) => g.date)).toEqual(['2026-03-02', '2026-03-04']);
    expect(groups[1].entries.map((e) => e.id)).toEqual(['b', 'c']);
    expect(groups[1].totalCents).toBe(70);
  });
});
