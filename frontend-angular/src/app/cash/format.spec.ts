import { addDays, daysBetween, formatBRL, formatDayLong, formatDayShort, parseBRL } from './format';

const NBSP = ' ';
const MINUS = '−';

describe('formatBRL', () => {
  it('dado centavos positivos, quando formata, então usa ponto de milhar e vírgula decimal', () => {
    expect(formatBRL(123456)).toBe(`R$${NBSP}1.234,56`);
  });

  it('dado zero, quando formata, então mostra duas casas', () => {
    expect(formatBRL(0)).toBe(`R$${NBSP}0,00`);
  });

  it('dado valor negativo, quando formata, então usa o sinal de menos tipográfico antes de R$', () => {
    expect(formatBRL(-5)).toBe(`${MINUS}R$${NBSP}0,05`);
  });

  it('dado valor negativo e sinal explícito pedido, quando formata, então positivo ganha +', () => {
    expect(formatBRL(1000, { explicitSign: true })).toBe(`+R$${NBSP}10,00`);
    expect(formatBRL(-1000, { explicitSign: true })).toBe(`${MINUS}R$${NBSP}10,00`);
    expect(formatBRL(0, { explicitSign: true })).toBe(`R$${NBSP}0,00`);
  });

  it('dado centavos não inteiros, quando formata, então rejeita para não esconder erro de escala', () => {
    expect(() => formatBRL(10.5)).toThrow();
  });
});

describe('parseBRL', () => {
  it('dado texto em pt-BR, quando lê, então devolve centavos', () => {
    expect(parseBRL('1.234,56')).toBe(123456);
    expect(parseBRL('35')).toBe(3500);
    expect(parseBRL('0,05')).toBe(5);
    expect(parseBRL('12,5')).toBe(1250);
  });

  it('dado sinal de menos, quando lê, então vira saída', () => {
    expect(parseBRL('-350,00')).toBe(-35000);
    expect(parseBRL(`${MINUS}20`)).toBe(-2000);
  });

  it('dado prefixo R$ e espaços, quando lê, então ignora', () => {
    expect(parseBRL(` R$${NBSP}1.000 `)).toBe(100000);
  });

  it('dado texto inválido, quando lê, então devolve nulo', () => {
    for (const text of ['', 'abc', '1,234', '1.23', '--5', '12,345,6', '1,', ',50']) {
      expect(parseBRL(text), text).toBeNull();
    }
  });
});

describe('datas de calendário', () => {
  it('dado uma data e dias, quando soma, então atravessa mês e ano', () => {
    expect(addDays('2026-12-30', 3)).toBe('2027-01-02');
    expect(addDays('2026-03-01', -1)).toBe('2026-02-28');
  });

  it('dado duas datas, quando mede a diferença, então conta dias de calendário', () => {
    expect(daysBetween('2026-03-01', '2026-03-12')).toBe(11);
    expect(daysBetween('2026-03-12', '2026-03-01')).toBe(-11);
  });

  it('dado uma data, quando formata por extenso, então escreve em pt-BR', () => {
    expect(formatDayLong('2026-03-12')).toBe('quinta-feira, 12 de março');
  });

  it('dado uma data, quando formata curta, então mostra dia e mês abreviado', () => {
    expect(formatDayShort('2026-03-02')).toBe('2 mar');
  });

  it('dado texto que não é data ISO, quando usa, então rejeita', () => {
    expect(() => addDays('12/03/2026', 1)).toThrow();
    expect(() => addDays('2026-02-31', 1)).toThrow();
  });
});
