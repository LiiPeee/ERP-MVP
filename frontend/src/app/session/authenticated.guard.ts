import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { SessionStore } from './session.store';

/** Só entra quem tem sessão; depois de recarregar a página, tenta recuperá-la pelo cookie antes de desistir. */
export const authenticatedGuard: CanActivateFn = async () => {
  const session = inject(SessionStore);
  if (session.isAuthenticated() || (await session.refresh())) {
    return true;
  }
  return inject(Router).parseUrl('/login');
};
