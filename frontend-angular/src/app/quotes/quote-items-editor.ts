import { ChangeDetectionStrategy, Component, model } from '@angular/core';
import { QuoteItemRequest } from '../api';
import { toApiDecimal } from '../shared/money-text';
import { ButtonComponent } from '../ui/button';
import { SelectFieldComponent, SelectOption } from '../ui/select-field';
import { TextFieldComponent } from '../ui/text-field';

/** Uma linha do orçamento como a pessoa a digita: tudo texto, o servidor valida e calcula o total. */
export interface ItemRow {
  readonly description: string;
  readonly kind: string;
  readonly quantity: string;
  readonly unitPrice: string;
}

export const EMPTY_ROW: ItemRow = { description: '', kind: 'LABOR', quantity: '1', unitPrice: '' };

const KINDS: readonly SelectOption[] = [
  { value: 'LABOR', label: 'Mão de obra' },
  { value: 'PART', label: 'Peça' },
];

export type ItemsResult = { items: QuoteItemRequest[]; error: null } | { items: null; error: string };

/** Do que está na tela para o corpo da API. Texto que não é número vira um erro que aponta a linha e o campo. */
export function rowsToRequests(rows: readonly ItemRow[]): ItemsResult {
  const items: QuoteItemRequest[] = [];
  for (const [index, row] of rows.entries()) {
    const quantity = toApiDecimal(row.quantity);
    const unitPrice = toApiDecimal(row.unitPrice);
    if (row.description.trim() === '') {
      return { items: null, error: `Item ${index + 1}: descreva o serviço ou a peça.` };
    }
    if (quantity === null) {
      return { items: null, error: `Item ${index + 1}: a quantidade precisa ser um número, por exemplo 1 ou 2,5.` };
    }
    if (unitPrice === null) {
      return { items: null, error: `Item ${index + 1}: o preço precisa ser um valor, por exemplo 80 ou 25,50.` };
    }
    items.push({ description: row.description.trim(), kind: row.kind, quantity, unitPrice });
  }
  return { items, error: null };
}

/** As linhas do orçamento. Sem total aqui: quem calcula é o servidor, e a tela mostra o que ele devolve. */
@Component({
  selector: 'erp-quote-items-editor',
  imports: [TextFieldComponent, SelectFieldComponent, ButtonComponent],
  template: `
    <ol class="rows">
      @for (row of rows(); track $index) {
        <li class="row">
          <erp-text-field [label]="'Item ' + ($index + 1)" [value]="row.description"
            (valueChange)="change($index, 'description', $event)" />
          <erp-select-field label="Tipo" [options]="kinds" [value]="row.kind" (valueChange)="change($index, 'kind', $event)" />
          <erp-text-field label="Quantidade" [value]="row.quantity" (valueChange)="change($index, 'quantity', $event)" />
          <erp-text-field label="Preço de cada, em reais" [value]="row.unitPrice"
            (valueChange)="change($index, 'unitPrice', $event)" />
          @if (rows().length > 1) {
            <button erpButton type="button" (click)="remove($index)" [attr.aria-label]="'Tirar o item ' + ($index + 1)">Tirar</button>
          }
        </li>
      }
    </ol>
    <button erpButton type="button" (click)="add()">Adicionar item</button>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuoteItemsEditorComponent {
  readonly rows = model<readonly ItemRow[]>([EMPTY_ROW]);
  protected readonly kinds = KINDS;

  protected change(index: number, field: keyof ItemRow, value: string): void {
    this.rows.update((rows) => rows.map((row, position) => (position === index ? { ...row, [field]: value } : row)));
  }

  protected add(): void {
    this.rows.update((rows) => [...rows, EMPTY_ROW]);
  }

  protected remove(index: number): void {
    this.rows.update((rows) => rows.filter((_, position) => position !== index));
  }
}
