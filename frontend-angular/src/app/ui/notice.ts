import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

@Component({
  selector: 'erp-notice',
  template: '<ng-content />',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '[attr.role]': "role()", '[class.notice--alert]': "tone() === 'alert'" },
})
export class NoticeComponent {
  readonly tone = input<'info' | 'alert'>('info');
  protected readonly role = computed(() => (this.tone() === 'alert' ? 'alert' : 'status'));
}
