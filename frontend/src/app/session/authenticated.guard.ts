import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { SessionStore } from './session.store';

/** Só entra quem tem sessão; depois de recarregar a página, tenta recuperá-la pelo cookie antes de desistir. */
export const authenticatedGuard: CanActivateFn = async () => {
  // inject() só funciona antes do primeiro await: o contexto de injeção se perde depois dele.
  const session = inject(SessionStore);
  const router = inject(Router);
  if (session.isAuthenticated() || (await session.refresh())) {
    return true;
  }
  return router.parseUrl('/login');
};
