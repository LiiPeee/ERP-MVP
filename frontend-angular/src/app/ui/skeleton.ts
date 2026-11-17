import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

@Component({
  selector: 'erp-skeleton',
  template: `
    <div role="status" aria-busy="true" [attr.aria-label]="label()">
      @for (line of lineList(); track line) {
        <span class="erp-skeleton__line"></span>
      }
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .erp-skeleton__line { display: block; height: var(--row-height); margin-bottom: var(--space-1); background: var(--color-paper-sunk); animation: pulse 1.2s ease-in-out infinite alternate; }
    @keyframes pulse { to { opacity: 0.45; } }
  `,
})
export class SkeletonComponent {
  readonly lines = input(3);
  readonly label = input('Carregando');
  protected readonly lineList = computed(() => Array.from({ length: this.lines() }, (_, index) => index));
}
