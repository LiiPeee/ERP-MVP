import { ChangeDetectionStrategy, Component, computed, input, signal } from '@angular/core';
import { nextFieldId } from './field.styles';
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
  styles: `
    :host { display: block; }
    .filter { display: flex; align-items: center; gap: var(--space-2); margin-bottom: var(--space-3); }
    .filter label { font-size: var(--text-s); font-weight: var(--weight-strong); }
    .filter input { min-height: var(--control-height); padding: 0 var(--space-3); border: var(--line) solid var(--color-ink); border-radius: var(--radius); background: var(--color-paper); color: var(--color-ink); font: inherit; }
    table { width: 100%; border-collapse: collapse; }
    caption { padding-bottom: var(--space-2); text-align: left; font-size: var(--text-s); font-weight: var(--weight-heavy); letter-spacing: 0.04em; text-transform: uppercase; }
    th, td { height: var(--row-height); padding: 0 var(--space-3); border-bottom: var(--line) solid var(--color-rule); text-align: left; font-size: var(--text-s); }
    th { border-bottom-color: var(--color-ink); }
    th button { padding: 0; border: 0; background: none; color: inherit; font: inherit; font-weight: var(--weight-strong); cursor: pointer; }
    .cell--end { text-align: right; }
    th.cell--end button { text-align: right; }
    .none { margin: var(--space-3) 0 0; color: var(--color-ink-muted); }
    @media (max-width: 40rem) {
      thead { position: absolute; width: 1px; height: 1px; overflow: hidden; clip-path: inset(50%); }
      tr, td { display: block; }
      tr { padding: var(--space-2) 0; border-bottom: var(--line) solid var(--color-rule); }
      td { display: flex; justify-content: space-between; height: auto; padding: var(--space-1) 0; border: 0; }
      td::before { content: attr(data-label); color: var(--color-ink-muted); }
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
