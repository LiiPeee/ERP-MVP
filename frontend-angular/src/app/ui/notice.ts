import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

@Component({
  selector: 'erp-notice',
  template: '<ng-content />',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '[attr.role]': "role()", '[class.notice--alert]': "tone() === 'alert'" },
  styles: `
    :host { display: block; padding: var(--space-3) var(--space-4); border-left: var(--line-strong) solid var(--color-ink); background: var(--color-paper-sunk); }
    :host(.notice--alert) { border-left-color: var(--color-alert); color: var(--color-alert); font-weight: var(--weight-strong); }
  `,
})
export class NoticeComponent {
  readonly tone = input<'info' | 'alert'>('info');
  protected readonly role = computed(() => (this.tone() === 'alert' ? 'alert' : 'status'));
}
