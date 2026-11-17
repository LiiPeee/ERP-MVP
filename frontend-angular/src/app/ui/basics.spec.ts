import { render, screen } from '@testing-library/angular';
import userEvent from '@testing-library/user-event';
import { ButtonComponent } from './button';
import { EmptyStateComponent } from './empty-state';
import { ErrorMessageComponent } from './error-message';
import { NoticeComponent } from './notice';
import { SkeletonComponent } from './skeleton';
import { StatusTagComponent } from './status-tag';
import { DataCurtaPipe, DataLongaPipe, MoedaPipe } from './format.pipes';

describe('erp-button', () => {
  it('dado um botão, quando o usuário clica, então a ação roda', async () => {
    const onClick = vi.fn();
    await render('<button erpButton (click)="onClick()">Lançar</button>', { imports: [ButtonComponent], componentProperties: { onClick } });

    await userEvent.click(screen.getByRole('button', { name: 'Lançar' }));

    expect(onClick).toHaveBeenCalledOnce();
  });

  it('dado botão desabilitado, quando o usuário clica, então nada acontece', async () => {
    const onClick = vi.fn();
    await render('<button erpButton disabled (click)="onClick()">Lançar</button>', { imports: [ButtonComponent], componentProperties: { onClick } });

    await userEvent.click(screen.getByRole('button', { name: 'Lançar' }));

    expect(onClick).not.toHaveBeenCalled();
  });

  it('dado variante primária, quando renderiza, então marca a classe da variante', async () => {
    await render('<button erpButton variant="primary">Lançar</button>', { imports: [ButtonComponent] });

    expect(screen.getByRole('button', { name: 'Lançar' }).classList.contains('erp-button--primary')).toBe(true);
  });
});

describe('erp-status-tag', () => {
  it('dado status a receber, quando renderiza, então mostra texto e sinal, nunca só cor', async () => {
    await render(StatusTagComponent, { inputs: { status: 'receivable' } });

    expect(screen.getByText('a receber')).toBeTruthy();
    expect(screen.getByText('▲')).toBeTruthy();
  });

  it.each([
    ['payable', 'a pagar', '▼'],
    ['overdue', 'atrasado', '!'],
  ] as const)('dado status %s, quando renderiza, então mostra "%s"', async (status, label, glyph) => {
    await render(StatusTagComponent, { inputs: { status } });

    expect(screen.getByText(label)).toBeTruthy();
    expect(screen.getByText(glyph)).toBeTruthy();
  });
});

describe('erp-notice', () => {
  it('dado aviso de alerta, quando renderiza, então é anunciado como alerta', async () => {
    await render('<erp-notice tone="alert">Não foi possível salvar.</erp-notice>', { imports: [NoticeComponent] });

    expect(screen.getByRole('alert').textContent).toContain('Não foi possível salvar.');
  });

  it('dado aviso informativo, quando renderiza, então é anunciado como status', async () => {
    await render('<erp-notice>Lançamento salvo.</erp-notice>', { imports: [NoticeComponent] });

    expect(screen.getByRole('status').textContent).toContain('Lançamento salvo.');
  });
});

describe('erp-error-message', () => {
  it('dado texto de erro, quando renderiza, então aparece como alerta', async () => {
    await render(ErrorMessageComponent, { inputs: { message: 'Escolha uma data que exista no calendário.' } });

    expect(screen.getByRole('alert').textContent).toContain('Escolha uma data que exista no calendário.');
  });

  it('dado nenhum erro, quando renderiza, então não mostra nada', async () => {
    await render(ErrorMessageComponent, { inputs: { message: null } });

    expect(screen.queryByRole('alert')).toBeNull();
  });
});

describe('erp-empty-state', () => {
  it('dado estado vazio, quando renderiza, então diz o que fazer e mostra a ação', async () => {
    await render(
      '<erp-empty-state title="Nenhuma conta a receber" guidance="Lance a primeira com N."><button erpButton>Novo lançamento</button></erp-empty-state>',
      { imports: [EmptyStateComponent, ButtonComponent] },
    );

    expect(screen.getByRole('heading', { name: 'Nenhuma conta a receber' })).toBeTruthy();
    expect(screen.getByText('Lance a primeira com N.')).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Novo lançamento' })).toBeTruthy();
  });
});

describe('erp-skeleton', () => {
  it('dado carregamento, quando renderiza, então avisa que está ocupado sem anunciar blocos vazios', async () => {
    const { container } = await render(SkeletonComponent, { inputs: { lines: 3, label: 'Carregando lançamentos' } });

    expect(screen.getByRole('status', { name: 'Carregando lançamentos' }).getAttribute('aria-busy')).toBe('true');
    expect(container.querySelectorAll('.erp-skeleton__line')).toHaveLength(3);
  });
});

describe('pipes de formatação pt-BR', () => {
  it('dado centavos, quando usa moeda, então formata em reais', () => {
    expect(new MoedaPipe().transform(123456)).toBe('R$ 1.234,56');
    expect(new MoedaPipe().transform(1000, true)).toBe('+R$ 10,00');
  });

  it('dado data ISO, quando usa dataCurta e dataLonga, então escreve em pt-BR', () => {
    expect(new DataCurtaPipe().transform('2026-03-02')).toBe('2 mar');
    expect(new DataLongaPipe().transform('2026-03-12')).toBe('Quinta-feira, 12 de março');
  });
});
