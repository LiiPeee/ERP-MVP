import { render, screen } from '@testing-library/angular';
import userEvent from '@testing-library/user-event';
import { EMPTY_ROW, QuoteItemsEditorComponent, rowsToRequests } from './quote-items-editor';

describe('rowsToRequests', () => {
  it('dado linhas digitadas em reais, quando converte, então manda texto decimal exato', () => {
    const result = rowsToRequests([
      { description: ' Troca de óleo ', kind: 'LABOR', quantity: '1', unitPrice: '80' },
      { description: 'Filtro', kind: 'PART', quantity: '2', unitPrice: '25,50' },
    ]);

    expect(result.error).toBeNull();
    expect(result.items).toEqual([
      { description: 'Troca de óleo', kind: 'LABOR', quantity: '1', unitPrice: '80' },
      { description: 'Filtro', kind: 'PART', quantity: '2', unitPrice: '25.50' },
    ]);
  });

  it.each([
    [{ ...EMPTY_ROW, description: '  ', unitPrice: '10' }, 'Item 1: descreva'],
    [{ ...EMPTY_ROW, description: 'X', quantity: 'dois', unitPrice: '10' }, 'Item 1: a quantidade'],
    [{ ...EMPTY_ROW, description: 'X', unitPrice: 'barato' }, 'Item 1: o preço'],
  ])('dado uma linha incompleta, quando converte, então o erro diz qual linha e o que fazer', (row, start) => {
    const result = rowsToRequests([row]);

    expect(result.items).toBeNull();
    expect(result.error).toContain(start);
  });

  it('dado erro na segunda linha, quando converte, então o erro cita o item 2', () => {
    const result = rowsToRequests([
      { description: 'A', kind: 'PART', quantity: '1', unitPrice: '1' },
      { description: 'B', kind: 'PART', quantity: '1', unitPrice: '' },
    ]);

    expect(result.error).toContain('Item 2');
  });
});

describe('erp-quote-items-editor', () => {
  it('dado uma linha, quando o usuário adiciona e tira itens, então a lista acompanha', async () => {
    const { fixture } = await render(QuoteItemsEditorComponent);

    await userEvent.click(screen.getByRole('button', { name: 'Adicionar item' }));
    expect(fixture.componentInstance.rows().length).toBe(2);
    await userEvent.click(screen.getByRole('button', { name: 'Tirar o item 2' }));

    expect(fixture.componentInstance.rows().length).toBe(1);
    expect(screen.queryByRole('button', { name: /Tirar o item/ })).toBeNull();
  });

  it('dado a linha, quando o usuário digita a descrição e o preço, então o modelo recebe os textos', async () => {
    const { fixture } = await render(QuoteItemsEditorComponent);

    await userEvent.type(screen.getByLabelText('Item 1'), 'Pastilha de freio');
    await userEvent.type(screen.getByLabelText('Preço de cada, em reais'), '150');
    await userEvent.selectOptions(screen.getByLabelText('Tipo'), 'Peça');

    expect(fixture.componentInstance.rows()[0]).toEqual({
      description: 'Pastilha de freio',
      kind: 'PART',
      quantity: '1',
      unitPrice: '150',
    });
  });
});
