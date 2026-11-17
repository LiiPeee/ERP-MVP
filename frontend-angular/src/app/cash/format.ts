/** Datas de calendário viajam como texto ISO (AAAA-MM-DD), sem hora nem fuso: o dia do vencimento não muda com o fuso. */
export type IsoDate = string;

const MS_PER_DAY = 86_400_000;
const MINUS = '−';
const MONTHS_SHORT = ['jan', 'fev', 'mar', 'abr', 'mai', 'jun', 'jul', 'ago', 'set', 'out', 'nov', 'dez'];

const currency = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
const longDay = new Intl.DateTimeFormat('pt-BR', { weekday: 'long', day: 'numeric', month: 'long', timeZone: 'UTC' });

export function formatBRL(cents: number, options: { explicitSign?: boolean } = {}): string {
  if (!Number.isInteger(cents)) {
    throw new Error('O valor deve ser um número inteiro de centavos.');
  }
  const body = currency.format(Math.abs(cents) / 100);
  if (cents < 0) {
    return `${MINUS}${body}`;
  }
  return options.explicitSign && cents > 0 ? `+${body}` : body;
}

const BRL_INPUT = /^([-−])?(\d{1,3}(?:\.\d{3})+|\d+)(?:,(\d{1,2}))?$/;

/** Lê o que a pessoa digita ("1.234,56", "-35", "R$ 10"). Devolve centavos, ou nulo se não for um valor. */
export function parseBRL(text: string): number | null {
  const match = BRL_INPUT.exec(text.replace(/R\$|\s/g, ''));
  if (!match) {
    return null;
  }
  const [, sign, integer, fraction = ''] = match;
  const cents = Number(integer.replaceAll('.', '')) * 100 + Number(fraction.padEnd(2, '0'));
  return sign ? -cents : cents;
}

function parse(date: IsoDate): number {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(date);
  if (!match) {
    throw new Error(`Data inválida: ${date}`);
  }
  const [year, month, day] = [Number(match[1]), Number(match[2]), Number(match[3])];
  const time = Date.UTC(year, month - 1, day);
  const check = new Date(time);
  if (check.getUTCFullYear() !== year || check.getUTCMonth() !== month - 1 || check.getUTCDate() !== day) {
    throw new Error(`Data inexistente: ${date}`);
  }
  return time;
}

export function addDays(date: IsoDate, days: number): IsoDate {
  return new Date(parse(date) + days * MS_PER_DAY).toISOString().slice(0, 10);
}

export function daysBetween(from: IsoDate, to: IsoDate): number {
  return Math.round((parse(to) - parse(from)) / MS_PER_DAY);
}

export function formatDayLong(date: IsoDate): string {
  return longDay.format(new Date(parse(date)));
}

export function formatDayShort(date: IsoDate): string {
  const d = new Date(parse(date));
  return `${d.getUTCDate()} ${MONTHS_SHORT[d.getUTCMonth()]}`;
}
