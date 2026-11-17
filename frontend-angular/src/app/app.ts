import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter, map } from 'rxjs';
import { SessionStore } from './session/session.store';
import { NavComponent, NavItem } from './ui/nav';

const OWNER_NAV: readonly NavItem[] = [
  { label: 'Orçamentos', link: '/fila' },
  { label: 'Novo', link: '/orcamento/novo' },
  { label: 'Oficina', link: '/oficina' },
];

/** A casca das telas do dono. A página do cliente (/o/...) fica sem menu nenhum: ela só mostra o orçamento. */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet, NavComponent],
  template: `
    @if (!isClientPage()) {
      <header class="app-bar">
        @if (session.isAuthenticated()) {
          <erp-nav [items]="ownerNav" />
          <button type="button" class="link-button" (click)="logout()">Sair</button>
        }
      </header>
    }
    <main [class.client-page]="isClientPage()">
      <router-outlet />
    </main>
  `,
})
export class App {
  protected readonly session = inject(SessionStore);
  private readonly router = inject(Router);
  protected readonly ownerNav = OWNER_NAV;
  protected readonly isClientPage = toSignal(
    this.router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd),
      map((event) => event.urlAfterRedirects.startsWith('/o/')),
    ),
    { initialValue: typeof location !== 'undefined' && location.pathname.startsWith('/o/') },
  );

  protected async logout(): Promise<void> {
    await this.session.logout();
    await this.router.navigateByUrl('/login');
  }
}
