import { addDays } from '../cash/format';
import { UI_MESSAGES } from '../ui/messages';
import { QuickEntry } from './cash-model';
import { MESSAGES } from './messages';

export interface QuickEntryInput {
  readonly description: string;
  /** Já lido pelo campo de valor; nulo se vazio ou inválido. */
  readonly amountCents: number | null;
  readonly date: string;
}

export type QuickEntryResult = { readonly entry: QuickEntry } | { readonly error: string };

export function validateQuickEntry(input: QuickEntryInput): QuickEntryResult {
  const description = input.description.trim();
  if (description === '') {
    return { error: MESSAGES.quickEntry.descriptionRequired };
  }
  if (input.amountCents === null) {
    return { error: UI_MESSAGES.moneyInvalid };
  }
  if (input.amountCents === 0) {
    return { error: MESSAGES.quickEntry.amountZero };
  }
  if (!isExistingDate(input.date)) {
    return { error: MESSAGES.quickEntry.dateInvalid };
  }
  return { entry: { description, amountCents: input.amountCents, date: input.date } };
}

function isExistingDate(date: string): boolean {
  try {
    addDays(date, 0);
    return true;
  } catch {
    return false;
  }
}
