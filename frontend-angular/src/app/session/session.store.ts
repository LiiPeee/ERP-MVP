import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../api';

/**
 * A sessão do usuário. O token de acesso fica só aqui, na memória desta aba: nunca em localStorage, sessionStorage
 * nem cookie legível. Ao recarregar a página ele se perde, e o refresh token (cookie HttpOnly) o recupera.
 */
@Injectable({ providedIn: 'root' })
export class SessionStore {
  private readonly auth = inject(AuthService);
  private readonly token = signal<string | null>(null);
  private refreshing: Promise<boolean> | null = null;

  readonly accessToken = this.token.asReadonly();
  readonly isAuthenticated = computed(() => this.token() !== null);

  async register(companyName: string, ownerName: string, email: string, password: string): Promise<void> {
    await firstValueFrom(this.auth.register({ companyName, ownerName, email, password }));
  }

  async login(email: string, password: string): Promise<void> {
    const session = await firstValueFrom(this.auth.login({ email, password }));
    this.token.set(session.accessToken ?? null);
  }

  /** Renova o token pelo cookie. Várias chamadas ao mesmo tempo compartilham uma só renovação. */
  refresh(): Promise<boolean> {
    this.refreshing ??= this.renew().finally(() => (this.refreshing = null));
    return this.refreshing;
  }

  async logout(): Promise<void> {
    try {
      await firstValueFrom(this.auth.logout());
    } finally {
      this.token.set(null);
    }
  }

  clear(): void {
    this.token.set(null);
  }

  private async renew(): Promise<boolean> {
    try {
      const session = await firstValueFrom(this.auth.refresh());
      this.token.set(session.accessToken ?? null);
      return this.token() !== null;
    } catch {
      this.token.set(null);
      return false;
    }
  }
}
