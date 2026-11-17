import { validateQuickEntry } from './quick-entry';

describe('validateQuickEntry', () => {
  it('dado descrição, valor e data válidos, quando valida, então devolve o lançamento em centavos', () => {
    expect(validateQuickEntry({ description: ' Venda avulsa ', amount: '1.200,50', date: '2026-03-03' })).toEqual({
      entry: { description: 'Venda avulsa', amountCents: 120_050, date: '2026-03-03' },
    });
  });

  it('dado valor com sinal de menos, quando valida, então é saída', () => {
    const result = validateQuickEntry({ description: 'Gás', amount: '-80', date: '2026-03-03' });

    expect(result).toEqual({ entry: { description: 'Gás', amountCents: -8000, date: '2026-03-03' } });
  });

  it('dado descrição vazia, quando valida, então diz o que fazer', () => {
    expect(validateQuickEntry({ description: '  ', amount: '10', date: '2026-03-03' })).toEqual({
      error: 'Diga o que é este lançamento, por exemplo "Farinha".',
    });
  });

  it('dado valor inválido, quando valida, então explica o formato', () => {
    expect(validateQuickEntry({ description: 'x', amount: 'abc', date: '2026-03-03' })).toEqual({
      error: 'Digite o valor em reais, como 1.200,50. Use o sinal de menos para uma saída.',
    });
  });

  it('dado valor zero, quando valida, então recusa', () => {
    expect(validateQuickEntry({ description: 'x', amount: '0', date: '2026-03-03' })).toEqual({
      error: 'O valor precisa ser diferente de zero.',
    });
  });

  it('dado data inexistente, quando valida, então pede outra', () => {
    expect(validateQuickEntry({ description: 'x', amount: '10', date: '2026-02-31' })).toEqual({
      error: 'Escolha uma data que exista no calendário.',
    });
  });
});
