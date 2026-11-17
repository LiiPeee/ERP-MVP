import { ChangeDetectionStrategy, Component, input, model } from '@angular/core';
import { ErrorMessageComponent } from './error-message';
import { nextFieldId } from './field-id';

/** Data de calendário (AAAA-MM-DD), no seletor nativo do navegador. */
@Component({
  selector: 'erp-date-field',
  imports: [ErrorMessageComponent],
  template: `
    <label [for]="id">{{ label() }}</label>
    <input [id]="id" type="date" [value]="value()" (input)="value.set($any($event.target).value)"
      [attr.aria-invalid]="error() ? 'true' : null" [attr.aria-describedby]="error() ? errorId : null" />
    <erp-error-message [message]="error()" [messageId]="errorId" />
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DateFieldComponent {
  readonly label = input.required<string>();
  readonly error = input<string | null>(null);
  readonly value = model('');

  protected readonly id = nextFieldId('erp-date');
  protected readonly errorId = `${this.id}-error`;
}
