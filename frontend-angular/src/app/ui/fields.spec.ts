import { render, screen } from '@testing-library/angular';
import userEvent from '@testing-library/user-event';
import { DateFieldComponent } from './date-field';
import { MoneyFieldComponent } from './money-field';
import { SelectFieldComponent } from './select-field';
import { TextFieldComponent } from './text-field';

describe('erp-text-field', () => {
  it('dado um campo, quando renderiza, então o rótulo está ligado ao campo', async () => {
    await render(TextFieldComponent, { inputs: { label: 'Descrição' } });

    expect(screen.getByLabelText('Descrição')).toBeTruthy();
  });

  it('dado texto digitado, quando o usuário escreve, então o valor do modelo acompanha', async () => {
    const { fixture } = await render(TextFieldComponent, { inputs: { label: 'Descrição' } });

    await userEvent.type(screen.getByLabelText('Descrição'), 'Farinha');

    expect(fixture.componentInstance.value()).toBe('Farinha');
  });

  it('dado erro, quando renderiza, então o campo é marcado inválido e o erro é lido junto com ele', async () => {
    await render(TextFieldComponent, { inputs: { label: 'Descrição', error: 'Diga o que é este lançamento.' } });

    const input = screen.getByLabelText('Descrição');
    expect(input.getAttribute('aria-invalid')).toBe('true');
    expect(screen.getByRole('alert').textContent).toContain('Diga o que é este lançamento.');
    expect(input.getAttribute('aria-describedby')).toContain(screen.getByRole('alert').id);
  });

  it('dado uma dica, quando renderiza, então a dica descreve o campo', async () => {
    await render(TextFieldComponent, { inputs: { label: 'Descrição', hint: 'Ex.: Farinha' } });

    const hint = screen.getByText('Ex.: Farinha');
    expect(screen.getByLabelText('Descrição').getAttribute('aria-describedby')).toContain(hint.id);
  });
});

describe('erp-money-field', () => {
  it('dado valor em reais digitado, quando o campo perde o foco, então guarda centavos e formata', async () => {
    const { fixture } = await render(MoneyFieldComponent, { inputs: { label: 'Valor' } });
    const input = screen.getByLabelText('Valor') as HTMLInputElement;

    await userEvent.type(input, '1200,5');
    await userEvent.tab();

    expect(fixture.componentInstance.cents()).toBe(120050);
    expect(input.value).toBe('1.200,50');
  });

  it('dado sinal de menos, quando perde o foco, então é saída', async () => {
    const { fixture } = await render(MoneyFieldComponent, { inputs: { label: 'Valor' } });

    await userEvent.type(screen.getByLabelText('Valor'), '-35');
    await userEvent.tab();

    expect(fixture.componentInstance.cents()).toBe(-3500);
  });

  it('dado texto inválido, quando perde o foco, então explica o formato e não guarda valor', async () => {
    const { fixture } = await render(MoneyFieldComponent, { inputs: { label: 'Valor' } });

    await userEvent.type(screen.getByLabelText('Valor'), 'abc');
    await userEvent.tab();

    expect(fixture.componentInstance.cents()).toBeNull();
    expect(screen.getByRole('alert').textContent).toContain('Digite o valor em reais, como 1.200,50.');
  });

  it('dado campo vazio, quando perde o foco, então não reclama', async () => {
    await render(MoneyFieldComponent, { inputs: { label: 'Valor' } });

    await userEvent.click(screen.getByLabelText('Valor'));
    await userEvent.tab();

    expect(screen.queryByRole('alert')).toBeNull();
  });
});

describe('erp-select-field', () => {
  const options = [
    { value: 'receivable', label: 'A receber' },
    { value: 'payable', label: 'A pagar' },
  ];

  it('dado opções, quando o usuário escolhe, então o modelo recebe o valor', async () => {
    const { fixture } = await render(SelectFieldComponent, { inputs: { label: 'Tipo', options } });

    await userEvent.selectOptions(screen.getByLabelText('Tipo'), 'A pagar');

    expect(fixture.componentInstance.value()).toBe('payable');
  });

  it('dado opções, quando renderiza, então lista todas com o texto de interface', async () => {
    await render(SelectFieldComponent, { inputs: { label: 'Tipo', options } });

    expect(screen.getAllByRole('option').map((o) => o.textContent)).toEqual(['A receber', 'A pagar']);
  });
});

describe('erp-date-field', () => {
  it('dado uma data, quando o usuário escolhe, então o modelo guarda a data ISO', async () => {
    const { fixture } = await render(DateFieldComponent, { inputs: { label: 'Vencimento' } });

    const input = screen.getByLabelText('Vencimento') as HTMLInputElement;
    await userEvent.type(input, '2026-03-12');

    expect(fixture.componentInstance.value()).toBe('2026-03-12');
  });
});
