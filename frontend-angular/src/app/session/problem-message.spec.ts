import { HttpErrorResponse } from '@angular/common/http';
import { problemCode, problemMessage } from './problem-message';

function failure(status: number, body: unknown): HttpErrorResponse {
  return new HttpErrorResponse({ status, error: body });
}

describe('problemMessage', () => {
  it('dado o código de conflito de telefone, quando traduz, então diz o que fazer', () => {
    expect(problemMessage(failure(409, { code: 'PHONE_ALREADY_REGISTERED' }))).toContain('Procure pelo nome');
  });

  it('dado erro de campo, quando traduz, então nomeia o campo e o problema, sem repetir o valor', () => {
    expect(problemMessage(failure(422, { code: 'INVALID_FORMAT', field: 'phone' }))).toBe('Telefone: formato inválido.');
  });

  it('dado erro num item, quando traduz, então conta o item a partir de 1', () => {
    expect(problemMessage(failure(422, { code: 'INVALID_FORMAT', field: 'items[1].quantity' }))).toBe(
      'Item 2, quantidade: formato inválido.',
    );
  });

  it('dado campo ou código desconhecido, quando traduz, então cai em texto neutro', () => {
    expect(problemMessage(failure(422, { code: 'NOVO', field: 'estranho' }))).toBe('Um campo: valor inválido.');
    expect(problemMessage(failure(500, { code: 'INTERNAL_ERROR' }))).toBe('Algo deu errado. Tente novamente.');
    expect(problemMessage(new Error('rede'))).toBe('Algo deu errado. Tente novamente.');
  });
});

describe('problemCode', () => {
  it('dado erro da API, quando lê o código, então devolve o código estável', () => {
    expect(problemCode(failure(404, { code: 'LINK_UNAVAILABLE' }))).toBe('LINK_UNAVAILABLE');
  });

  it('dado qualquer outra coisa, quando lê o código, então devolve nulo', () => {
    expect(problemCode(new Error('x'))).toBeNull();
    expect(problemCode(failure(0, null))).toBeNull();
  });
});
