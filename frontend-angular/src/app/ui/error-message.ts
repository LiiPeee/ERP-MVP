import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** Erro que diz o que fazer. Usado dentro dos campos e avulso. */
@Component({
  selector: 'erp-error-message',
  template: `
    @if (message()) {
      <p class="error" role="alert" [id]="messageId()">{{ message() }}</p>
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .error { margin: 0; color: var(--color-alert); font-size: var(--text-s); font-weight: var(--weight-strong); }
  `,
})
export class ErrorMessageComponent {
  readonly message = input<string | null>(null);
  readonly messageId = input<string | null>(null);
}
