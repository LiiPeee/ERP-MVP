import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterOutlet } from '@angular/router';
import { filter, map } from 'rxjs';
import { SessionStore } from './session/session.store';

/** Casca descartável das telas de conta. As telas de laboratório (/design) ocupam a página inteira. */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink],
  template: `
    @if (!isLab()) {
      <nav>
        @if (session.isAuthenticated()) {
          <a routerLink="/me">Quem sou eu</a>
          <button type="button" (click)="logout()">Sair</button>
        } @else {
          <a routerLink="/login">Entrar</a>
          <a routerLink="/register">Cadastrar</a>
        }
      </nav>
    }
    <main>
      <router-outlet />
    </main>
  `,
})
export class App {
  protected readonly session = inject(SessionStore);
  private readonly router = inject(Router);
  protected readonly isLab = toSignal(
    this.router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd),
      map((event) => event.urlAfterRedirects.startsWith('/caixa')),
    ),
    { initialValue: false },
  );

  protected async logout(): Promise<void> {
    await this.session.logout();
    await this.router.navigateByUrl('/login');
  }
}
