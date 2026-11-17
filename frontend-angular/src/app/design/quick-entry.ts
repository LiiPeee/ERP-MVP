import { addDays, IsoDate, parseBRL } from '../cash/format';
import { MESSAGES } from './messages';
import { QuickEntry } from './cash-model';

export interface QuickEntryInput {
  readonly description: string;
  readonly amount: string;
  readonly date: string;
}

export type QuickEntryResult = { readonly entry: QuickEntry } | { readonly error: string };

export function validateQuickEntry(input: QuickEntryInput): QuickEntryResult {
  const description = input.description.trim();
  if (description === '') {
    return { error: MESSAGES.quickEntry.descriptionRequired };
  }
  const amountCents = parseBRL(input.amount);
  if (amountCents === null) {
    return { error: MESSAGES.quickEntry.amountInvalid };
  }
  if (amountCents === 0) {
    return { error: MESSAGES.quickEntry.amountZero };
  }
  if (!isExistingDate(input.date)) {
    return { error: MESSAGES.quickEntry.dateInvalid };
  }
  return { entry: { description, amountCents, date: input.date } };
}

function isExistingDate(date: string): date is IsoDate {
  try {
    addDays(date, 0);
    return true;
  } catch {
    return false;
  }
}
