import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** Estado vazio com orientação e ação (o botão entra como conteúdo). Nunca só um ícone. */
@Component({
  selector: 'erp-empty-state',
  template: `
    <h3>{{ title() }}</h3>
    <p>{{ guidance() }}</p>
    <div class="action"><ng-content /></div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    :host { display: block; padding: var(--space-5) 0; border-top: var(--line) solid var(--color-rule); }
    h3 { margin: 0 0 var(--space-2); font-size: var(--text-l); font-weight: var(--weight-heavy); }
    p { margin: 0 0 var(--space-4); color: var(--color-ink-muted); }
  `,
})
export class EmptyStateComponent {
  readonly title = input.required<string>();
  readonly guidance = input.required<string>();
}
