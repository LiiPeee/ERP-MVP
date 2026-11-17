import { formatDay, formatValidUntil } from './dates';

describe('datas do orçamento', () => {
  it('dado um instante, quando formata o dia, então usa dia e mês por extenso curto no fuso da oficina', () => {
    expect(formatDay('2026-11-17T15:00:00Z')).toBe('17 de nov.');
  });

  it('dado a meia-noite UTC, quando formata, então o dia é o de São Paulo (3 horas atrás)', () => {
    expect(formatDay('2026-11-18T01:00:00Z')).toBe('17 de nov.');
  });

  it('dado um prazo, quando formata a validade, então diz até quando vale', () => {
    expect(formatValidUntil('2026-11-24T02:59:59Z')).toBe('Vale até 23 de nov.');
  });

  it('dado nada, quando formata, então devolve texto vazio', () => {
    expect(formatDay(undefined)).toBe('');
    expect(formatValidUntil(undefined)).toBe('');
  });
});
