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
})
export class SkeletonComponent {
  readonly lines = input(3);
  readonly label = input('Carregando');
  protected readonly lineList = computed(() => Array.from({ length: this.lines() }, (_, index) => index));
}
