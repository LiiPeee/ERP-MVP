import { ChangeDetectionStrategy, Component, computed, input, model } from '@angular/core';
import { ErrorMessageComponent } from './error-message';
import { nextFieldId } from './field-id';

@Component({
  selector: 'erp-text-field',
  imports: [ErrorMessageComponent],
  template: `
    <label [for]="id">{{ label() }}</label>
    <input [id]="id" type="text" autocomplete="off" [value]="value()" (input)="onInput($event)"
      [attr.aria-invalid]="error() ? 'true' : null" [attr.aria-describedby]="describedBy()" />
    @if (hint()) {
      <p class="hint" [id]="hintId">{{ hint() }}</p>
    }
    <erp-error-message [message]="error()" [messageId]="errorId" />
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TextFieldComponent {
  readonly label = input.required<string>();
  readonly hint = input<string | null>(null);
  readonly error = input<string | null>(null);
  readonly value = model('');

  protected readonly id = nextFieldId('erp-text');
  protected readonly hintId = `${this.id}-hint`;
  protected readonly errorId = `${this.id}-error`;
  protected readonly describedBy = computed(() => [this.hint() ? this.hintId : null, this.error() ? this.errorId : null].filter(Boolean).join(' ') || null);

  protected onInput(event: Event): void {
    this.value.set((event.target as HTMLInputElement).value);
  }
}
