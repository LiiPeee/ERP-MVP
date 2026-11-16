import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterOutlet } from '@angular/router';
import { SessionStore } from './session/session.store';

/** Casca descartável, sem identidade visual: a identidade é decidida na fase seguinte. */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink],
  template: `
    <nav>
      @if (session.isAuthenticated()) {
        <a routerLink="/me">Quem sou eu</a>
        <button type="button" (click)="logout()">Sair</button>
      } @else {
        <a routerLink="/login">Entrar</a>
        <a routerLink="/register">Cadastrar</a>
      }
    </nav>
    <main>
      <router-outlet />
    </main>
  `,
})
export class App {
  protected readonly session = inject(SessionStore);
  private readonly router = inject(Router);

  protected async logout(): Promise<void> {
    await this.session.logout();
    await this.router.navigateByUrl('/login');
  }
}
