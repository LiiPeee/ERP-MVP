import { addDays, daysBetween, IsoDate } from './format';

/**
 * Protótipo da Fase 0.5: a regra real de projeção e de "dias de caixa" é da Fase 3 e fica no backend.
 * Aqui só alimenta as telas de laboratório com a fixture.
 */
export interface CashEntry {
  readonly id: string;
  readonly date: IsoDate;
  readonly description: string;
  /** Positivo entra (a receber), negativo sai (a pagar), em centavos. */
  readonly amountCents: number;
  readonly kind: 'receivable' | 'payable';
  readonly overdue?: boolean;
}

export interface DayBalance {
  readonly date: IsoDate;
  readonly balanceCents: number;
  readonly entries: readonly CashEntry[];
}

export interface DayGroup {
  readonly date: IsoDate;
  readonly entries: readonly CashEntry[];
  readonly totalCents: number;
}

export function projectBalance(openingCents: number, entries: readonly CashEntry[], from: IsoDate, to: IsoDate): DayBalance[] {
  const span = daysBetween(from, to);
  if (span < 0) {
    throw new Error('O fim do período não pode ser antes do início.');
  }
  const byDate = new Map<IsoDate, CashEntry[]>();
  for (const entry of entries) {
    byDate.set(entry.date, [...(byDate.get(entry.date) ?? []), entry]);
  }
  let balance = openingCents;
  return Array.from({ length: span + 1 }, (_, index) => {
    const date = addDays(from, index);
    const dayEntries = byDate.get(date) ?? [];
    balance += dayEntries.reduce((sum, entry) => sum + entry.amountCents, 0);
    return { date, balanceCents: balance, entries: dayEntries };
  });
}

export function daysOfCash(series: readonly DayBalance[]): number | null {
  const firstNegative = series.find((day) => day.balanceCents < 0);
  return firstNegative ? daysBetween(series[0].date, firstNegative.date) : null;
}

export function groupByDay(entries: readonly CashEntry[]): DayGroup[] {
  const sorted = [...entries].sort((a, b) => a.date.localeCompare(b.date));
  const groups: { date: IsoDate; entries: CashEntry[] }[] = [];
  for (const entry of sorted) {
    const last = groups.at(-1);
    if (last?.date === entry.date) {
      last.entries.push(entry);
    } else {
      groups.push({ date: entry.date, entries: [entry] });
    }
  }
  return groups.map((group) => ({ ...group, totalCents: group.entries.reduce((sum, e) => sum + e.amountCents, 0) }));
}
