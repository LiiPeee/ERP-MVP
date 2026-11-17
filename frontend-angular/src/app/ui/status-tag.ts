import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

export type Status = 'receivable' | 'payable' | 'overdue';

const LABELS: Record<Status, { text: string; glyph: string }> = {
  receivable: { text: 'a receber', glyph: '▲' },
  payable: { text: 'a pagar', glyph: '▼' },
  overdue: { text: 'atrasado', glyph: '!' },
};

/** Cor sempre acompanhada de sinal e texto, para não depender só da cor. */
@Component({
  selector: 'erp-status-tag',
  template: '<span class="glyph" aria-hidden="true">{{ label().glyph }}</span>{{ label().text }}',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '[class]': "'status status--' + status()" },
})
export class StatusTagComponent {
  readonly status = input.required<Status>();
  protected readonly label = computed(() => LABELS[this.status()]);
}
