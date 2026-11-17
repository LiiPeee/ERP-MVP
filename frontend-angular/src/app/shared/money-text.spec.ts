import { formatMoney, formatQuantity, toApiDecimal } from './money-text';

describe('formatMoney', () => {
  it('dado o texto decimal da API, quando formata, então mostra reais no padrão brasileiro', () => {
    expect(formatMoney('131.00')).toMatch(/^R\$\s131,00$/);
    expect(formatMoney('1234.5')).toMatch(/^R\$\s1\.234,50$/);
  });

  it('dado nada ou texto que não é número, quando formata, então devolve travessão em vez de NaN', () => {
    expect(formatMoney(undefined)).toBe('—');
    expect(formatMoney('abc')).toBe('—');
  });
});

describe('toApiDecimal', () => {
  it.each([
    ['25,50', '25.50'],
    ['25.5', '25.5'],
    ['R$ 80', '80'],
    ['1,5', '1.5'],
    [' 3 ', '3'],
  ])('dado "%s", quando converte, então vira "%s"', (typed, expected) => {
    expect(toApiDecimal(typed)).toBe(expected);
  });

  it.each(['', 'abc', '1,2,3', '-5', '1.234,56'])('dado "%s", quando converte, então é recusado', (typed) => {
    expect(toApiDecimal(typed)).toBeNull();
  });
});

describe('formatQuantity', () => {
  it.each([
    ['1.000', '1'],
    ['2.500', '2,5'],
    ['0.250', '0,25'],
    ['12.000', '12'],
  ])('dado "%s" da API, quando formata, então mostra "%s" sem zeros inúteis', (api, shown) => {
    expect(formatQuantity(api)).toBe(shown);
  });

  it('dado nada ou texto inválido, quando formata, então devolve travessão', () => {
    expect(formatQuantity(undefined)).toBe('—');
    expect(formatQuantity('x')).toBe('—');
  });
});
