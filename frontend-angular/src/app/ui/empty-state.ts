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
})
export class EmptyStateComponent {
  readonly title = input.required<string>();
  readonly guidance = input.required<string>();
}
