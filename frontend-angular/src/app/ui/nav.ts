import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { UI_MESSAGES } from './messages';

export interface NavItem {
  readonly label: string;
  readonly link: string;
}

@Component({
  selector: 'erp-nav',
  imports: [RouterLink, RouterLinkActive],
  template: `
    <nav [attr.aria-label]="messages.navLabel">
      @for (item of items(); track item.link) {
        <a [routerLink]="item.link" routerLinkActive="active" ariaCurrentWhenActive="page">{{ item.label }}</a>
      }
    </nav>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    nav { display: flex; gap: var(--space-5); padding: var(--space-3) 0; border-bottom: var(--line) solid var(--color-rule); }
    a { padding: var(--space-1) 0; color: var(--color-ink-muted); font-weight: var(--weight-strong); text-decoration: none; border-bottom: var(--line-strong) solid transparent; }
    a:hover { color: var(--color-ink); }
    a.active { color: var(--color-ink); border-bottom-color: var(--color-ink); }
  `,
})
export class NavComponent {
  readonly items = input.required<readonly NavItem[]>();
  protected readonly messages = UI_MESSAGES;
}
