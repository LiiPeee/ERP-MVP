import { ChangeDetectionStrategy, Component, computed, input, signal } from '@angular/core';
import { nextFieldId } from './field-id';
import { UI_MESSAGES } from './messages';

export interface TableColumn<T> {
  readonly key: string;
  readonly label: string;
  readonly align?: 'start' | 'end';
  /** Valor usado para ordenar. */
  readonly value: (row: T) => string | number;
  /** Texto mostrado na célula; se faltar, usa o valor. */
  readonly display?: (row: T) => string;
}

function normalize(text: string): string {
  return text.normalize('NFD').replace(/\p{Diacritic}/gu, '').toLowerCase();
}

/**
 * Tabela densa: valores à direita, filtro e ordenação na própria tabela.
 * No celular vira lista (cada célula traz o próprio rótulo).
 */
@Component({
  selector: 'erp-data-table',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="filter">
      <label [for]="filterId">{{ messages.tableFilter }}</label>
      <input [id]="filterId" type="search" autocomplete="off" [value]="filter()" (input)="filter.set($any($event.target).value)" />
    </div>
    <table>
      <caption>{{ caption() }}</caption>
      <thead>
        <tr>
          @for (column of columns(); track column.key) {
            <th scope="col" [class.cell--end]="column.align === 'end'" [attr.aria-sort]="ariaSort(column.key)">
              <button type="button" (click)="sortBy(column.key)">
                {{ column.label }}<span aria-hidden="true">{{ indicator(column.key) }}</span>
              </button>
            </th>
          }
        </tr>
      </thead>
      <tbody>
        @for (row of visibleRows(); track rowKey()(row)) {
          <tr>
            @for (column of columns(); track column.key) {
              <td [class.cell--end]="column.align === 'end'" [class.num]="column.align === 'end'" [attr.data-label]="column.label">{{ cellText(column, row) }}</td>
            }
          </tr>
        }
      </tbody>
    </table>
    @if (visibleRows().length === 0) {
      <p class="none">{{ messages.tableEmpty }}</p>
    }
  `,
})
export class DataTableComponent<T> {
  readonly caption = input.required<string>();
  readonly columns = input.required<readonly TableColumn<T>[]>();
  readonly rows = input.required<readonly T[]>();
  readonly rowKey = input.required<(row: T) => string>();

  protected readonly messages = UI_MESSAGES;
  protected readonly filterId = nextFieldId('erp-table-filter');
  protected readonly filter = signal('');
  private readonly sortKey = signal<string | null>(null);
  private readonly sortDirection = signal<'ascending' | 'descending'>('ascending');

  protected readonly visibleRows = computed(() => {
    const needle = normalize(this.filter().trim());
    const columns = this.columns();
    let result = this.rows().filter((row) => needle === '' || columns.some((column) => normalize(this.cellText(column, row)).includes(needle)));
    const key = this.sortKey();
    const column = columns.find((c) => c.key === key);
    if (column) {
      const direction = this.sortDirection() === 'ascending' ? 1 : -1;
      result = [...result].sort((a, b) => direction * this.compare(column.value(a), column.value(b)));
    }
    return result;
  });

  protected cellText(column: TableColumn<T>, row: T): string {
    return column.display ? column.display(row) : String(column.value(row));
  }

  protected sortBy(key: string): void {
    if (this.sortKey() === key) {
      this.sortDirection.update((d) => (d === 'ascending' ? 'descending' : 'ascending'));
    } else {
      this.sortKey.set(key);
      this.sortDirection.set('ascending');
    }
  }

  protected ariaSort(key: string): 'ascending' | 'descending' | null {
    return this.sortKey() === key ? this.sortDirection() : null;
  }

  protected indicator(key: string): string {
    const sort = this.ariaSort(key);
    return sort === 'ascending' ? ' ▲' : sort === 'descending' ? ' ▼' : '';
  }

  private compare(a: string | number, b: string | number): number {
    return typeof a === 'number' && typeof b === 'number' ? a - b : String(a).localeCompare(String(b), 'pt-BR');
  }
}
