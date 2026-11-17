import { computed, signal } from '@angular/core';
import { IsoDate } from '../cash/format';
import { CashEntry, daysOfCash, groupByDay, projectBalance } from '../cash/projection';
import { FIXTURE_ENTRIES, HORIZON_END, OPENING_BALANCE_CENTS, TODAY } from './fixture';

export interface QuickEntry {
  readonly date: IsoDate;
  readonly description: string;
  readonly amountCents: number;
}

/** Estado das telas de laboratório. Atrasados vencem "hoje" na projeção; a regra real é da Fase 3. */
export function createCashModel() {
  const entries = signal<readonly CashEntry[]>(FIXTURE_ENTRIES);
  let nextId = 1;

  const overdue = computed(() => entries().filter((entry) => entry.date < TODAY));
  const upcoming = computed(() => entries().filter((entry) => entry.date >= TODAY));
  const projected = computed(() => entries().map((entry) => (entry.date < TODAY ? { ...entry, date: TODAY } : entry)));
  const series = computed(() => projectBalance(OPENING_BALANCE_CENTS, projected(), TODAY, HORIZON_END));
  const cashDays = computed(() => daysOfCash(series()));

  return {
    today: TODAY,
    horizonEnd: HORIZON_END,
    entries,
    overdue,
    overdueTotalCents: computed(() => overdue().reduce((sum, entry) => sum + entry.amountCents, 0)),
    groups: computed(() => groupByDay(upcoming())),
    series,
    cashDays,
    add(input: QuickEntry): void {
      const entry: CashEntry = {
        id: `novo-${nextId++}`,
        date: input.date,
        description: input.description,
        amountCents: input.amountCents,
        kind: input.amountCents >= 0 ? 'receivable' : 'payable',
      };
      entries.update((list) => [...list, entry]);
    },
  };
}

export type CashModel = ReturnType<typeof createCashModel>;
