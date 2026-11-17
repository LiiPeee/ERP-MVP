import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** Erro que diz o que fazer. Usado dentro dos campos e avulso. */
@Component({
  selector: 'erp-error-message',
  template: `
    @if (message()) {
      <p class="erp-error" role="alert" [id]="messageId()">{{ message() }}</p>
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ErrorMessageComponent {
  readonly message = input<string | null>(null);
  readonly messageId = input<string | null>(null);
}
