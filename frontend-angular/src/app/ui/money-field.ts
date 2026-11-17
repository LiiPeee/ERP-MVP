import { ChangeDetectionStrategy, Component, computed, input, model, signal } from '@angular/core';
import { formatBRL, parseBRL } from '../cash/format';
import { ErrorMessageComponent } from './error-message';
import { FIELD_STYLES, nextFieldId } from './field.styles';
import { UI_MESSAGES } from './messages';

const MINUS = '−';

/** Valor em reais: a pessoa digita "1.200,5" ou "-35"; o modelo guarda centavos inteiros. */
@Component({
  selector: 'erp-money-field',
  imports: [ErrorMessageComponent],
  template: `
    <label [for]="id">{{ label() }}</label>
    <input [id]="id" class="num" type="text" inputmode="decimal" autocomplete="off" [value]="text()" (input)="text.set($any($event.target).value)"
      (blur)="commit()" [attr.aria-invalid]="shownError() ? 'true' : null" [attr.aria-describedby]="describedBy()" />
    @if (hint()) {
      <p class="hint" [id]="hintId">{{ hint() }}</p>
    }
    <erp-error-message [message]="shownError()" [messageId]="errorId" />
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: FIELD_STYLES + ' input { text-align: right; }',
})
export class MoneyFieldComponent {
  readonly label = input.required<string>();
  readonly hint = input<string | null>(null);
  readonly error = input<string | null>(null);
  readonly cents = model<number | null>(null);

  protected readonly text = signal('');
  private readonly ownError = signal<string | null>(null);
  protected readonly shownError = computed(() => this.ownError() ?? this.error());
  protected readonly id = nextFieldId('erp-money');
  protected readonly hintId = `${this.id}-hint`;
  protected readonly errorId = `${this.id}-error`;
  protected readonly describedBy = computed(() => [this.hint() ? this.hintId : null, this.shownError() ? this.errorId : null].filter(Boolean).join(' ') || null);

  protected commit(): void {
    const typed = this.text().trim();
    if (typed === '') {
      this.cents.set(null);
      this.ownError.set(null);
      return;
    }
    const parsed = parseBRL(typed);
    if (parsed === null) {
      this.cents.set(null);
      this.ownError.set(UI_MESSAGES.moneyInvalid);
      return;
    }
    this.ownError.set(null);
    this.cents.set(parsed);
    this.text.set(`${parsed < 0 ? MINUS : ''}${formatBRL(Math.abs(parsed)).replace(/^R\$ /, '')}`);
  }
}
