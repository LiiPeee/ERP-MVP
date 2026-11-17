const currency = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
const TYPED_DECIMAL = /^\d+([.,]\d+)?$/;

/** Só mostra: o servidor calcula todos os valores e os devolve como texto decimal exato ("131.00"). */
export function formatMoney(text: string | undefined): string {
  const amount = text === undefined ? Number.NaN : Number(text);
  return Number.isFinite(amount) ? currency.format(amount) : '—';
}

/** Do que a pessoa digita ("25,50", "R$ 80") para o texto decimal da API ("25.50"); nulo se não for um número simples. */
export function toApiDecimal(typed: string): string | null {
  const cleaned = typed.replace(/R\$|\s/g, '');
  return TYPED_DECIMAL.test(cleaned) ? cleaned.replace(',', '.') : null;
}

const quantity = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 3 });

/** A API manda a quantidade com 3 casas ("1.000"); na tela vai sem zeros inúteis ("1", "2,5"). */
export function formatQuantity(text: string | undefined): string {
  const amount = text === undefined ? Number.NaN : Number(text);
  return Number.isFinite(amount) ? quantity.format(amount) : '—';
}
