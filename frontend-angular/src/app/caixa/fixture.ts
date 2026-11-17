import { IsoDate } from '../cash/format';
import { CashEntry } from '../cash/projection';

/** Dados sintéticos e claramente falsos, só para as telas de laboratório. Nenhuma pessoa ou empresa real. */
export const TODAY: IsoDate = '2026-03-02';
export const HORIZON_END: IsoDate = '2026-04-15';
export const OPENING_BALANCE_CENTS = 842_000;

function receivable(id: string, date: IsoDate, description: string, cents: number, overdue = false): CashEntry {
  return { id, date, description, amountCents: cents, kind: 'receivable', overdue };
}

function payable(id: string, date: IsoDate, description: string, cents: number, overdue = false): CashEntry {
  return { id, date, description, amountCents: -cents, kind: 'payable', overdue };
}

export const FIXTURE_ENTRIES: readonly CashEntry[] = [
  receivable('r00', '2026-02-26', 'Cliente Fictício C, encomenda', 95_000, true),
  payable('p00', '2026-02-27', 'Fornecedor Fictício 3, embalagens', 48_000, true),
  payable('p01', '2026-03-02', 'Fornecedor Fictício 1, farinha', 215_000),
  receivable('r01', '2026-03-03', 'Maquininha, semana anterior', 318_000),
  payable('p02', '2026-03-05', 'Energia elétrica', 69_000),
  payable('p03', '2026-03-05', 'Aluguel da loja', 320_000),
  receivable('r02', '2026-03-06', 'Cliente Fictício A, buffet', 240_000),
  payable('p04', '2026-03-09', 'Embalagens', 78_000),
  receivable('r03', '2026-03-10', 'Maquininha, semana anterior', 305_000),
  payable('p05', '2026-03-10', 'Folha de pagamento (exemplo)', 540_000),
  payable('p06', '2026-03-12', 'Gás', 42_000),
  receivable('r04', '2026-03-13', 'Cliente Fictício D, casamento', 180_000),
  payable('p07', '2026-03-16', 'Fornecedor Fictício 2, laticínios', 230_000),
  receivable('r05', '2026-03-17', 'Maquininha, semana anterior', 290_000),
  payable('p08', '2026-03-18', 'Guia de imposto (exemplo)', 135_000),
  payable('p09', '2026-03-20', 'Manutenção do forno', 160_000),
  payable('p10', '2026-03-23', 'Fornecedor Fictício 1, farinha', 220_000),
  receivable('r06', '2026-03-24', 'Maquininha, semana anterior', 275_000),
  payable('p11', '2026-03-25', 'Reforma da vitrine, parcela', 400_000),
  payable('p12', '2026-03-26', 'Fornecedor Fictício 2, laticínios', 210_000),
  receivable('r07', '2026-03-31', 'Maquininha, semana anterior', 260_000),
  payable('p13', '2026-04-02', 'Embalagens', 90_000),
  payable('p14', '2026-04-05', 'Aluguel da loja', 320_000),
  payable('p15', '2026-04-06', 'Energia elétrica', 71_000),
  receivable('r08', '2026-04-07', 'Maquininha, semana anterior', 250_000),
  payable('p16', '2026-04-10', 'Folha de pagamento (exemplo)', 540_000),
  payable('p17', '2026-04-13', 'Fornecedor Fictício 1, farinha', 230_000),
  receivable('r09', '2026-04-14', 'Maquininha, semana anterior', 240_000),
];
