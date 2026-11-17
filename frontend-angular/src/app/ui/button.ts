import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'button[erpButton]',
  template: '<ng-content />',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'erp-button', '[class.erp-button--primary]': "variant() === 'primary'" },
  styles: `
    :host {
      min-height: var(--control-height);
      padding: 0 var(--space-4);
      border: var(--line) solid var(--color-ink);
      border-radius: var(--radius);
      background: transparent;
      color: var(--color-ink);
      font: inherit;
      font-weight: var(--weight-strong);
      cursor: pointer;
      transition: background var(--move);
    }
    :host(:hover:not(:disabled)) { background: var(--color-paper-sunk); }
    :host(.erp-button--primary) { background: var(--color-ink); color: var(--color-paper); }
    :host(.erp-button--primary:hover:not(:disabled)) { background: var(--color-ink-muted); }
    :host(:disabled) { border-color: var(--color-ink-muted); color: var(--color-ink-muted); cursor: not-allowed; }
  `,
})
export class ButtonComponent {
  readonly variant = input<'primary' | 'secondary'>('secondary');
}
