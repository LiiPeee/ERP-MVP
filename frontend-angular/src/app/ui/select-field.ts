import { ChangeDetectionStrategy, Component, input, model } from '@angular/core';
import { ErrorMessageComponent } from './error-message';
import { FIELD_STYLES, nextFieldId } from './field.styles';

export interface SelectOption {
  readonly value: string;
  readonly label: string;
}

/** Seletor sobre o `<select>` nativo: teclado, leitor de tela e celular já funcionam; o CDK entra onde o nativo não basta. */
@Component({
  selector: 'erp-select-field',
  imports: [ErrorMessageComponent],
  template: `
    <label [for]="id">{{ label() }}</label>
    <select [id]="id" (change)="value.set($any($event.target).value)" [attr.aria-invalid]="error() ? 'true' : null" [attr.aria-describedby]="error() ? errorId : null">
      @for (option of options(); track option.value) {
        <option [value]="option.value" [selected]="option.value === value()">{{ option.label }}</option>
      }
    </select>
    <erp-error-message [message]="error()" [messageId]="errorId" />
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: FIELD_STYLES,
})
export class SelectFieldComponent {
  readonly label = input.required<string>();
  readonly options = input.required<readonly SelectOption[]>();
  readonly error = input<string | null>(null);
  readonly value = model('');

  protected readonly id = nextFieldId('erp-select');
  protected readonly errorId = `${this.id}-error`;
}
