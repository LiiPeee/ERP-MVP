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
})
export class NavComponent {
  readonly items = input.required<readonly NavItem[]>();
  protected readonly messages = UI_MESSAGES;
}
