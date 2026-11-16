import { Component, OnInit, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { MeResponse, MeService } from '../api';
import { problemMessage } from '../session/problem-message';

@Component({
  selector: 'app-me-page',
  template: `
    <h1>Quem sou eu</h1>
    @if (context(); as me) {
      <dl data-testid="context">
        <dt>Usuário</dt>
        <dd data-testid="display-name">{{ me.displayName }}</dd>
        <dt>E-mail</dt>
        <dd data-testid="email">{{ me.email }}</dd>
        <dt>Empresa</dt>
        <dd data-testid="company-name">{{ me.companyName }}</dd>
        <dt>Papel</dt>
        <dd data-testid="role">{{ me.role }}</dd>
      </dl>
    } @else if (error()) {
      <p role="alert" data-testid="error">{{ error() }}</p>
    } @else {
      <p>Carregando…</p>
    }
  `,
})
export class MePage implements OnInit {
  private readonly me = inject(MeService);

  protected readonly context = signal<MeResponse | null>(null);
  protected readonly error = signal<string | null>(null);

  async ngOnInit(): Promise<void> {
    try {
      this.context.set(await firstValueFrom(this.me.me()));
    } catch (failure) {
      this.error.set(problemMessage(failure));
    }
  }
}
