import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'button[erpButton]',
  template: '<ng-content />',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'erp-button', '[class.erp-button--primary]': "variant() === 'primary'" },
})
export class ButtonComponent {
  readonly variant = input<'primary' | 'secondary'>('secondary');
}
