import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, from, switchMap, throwError } from 'rxjs';
import { SessionStore } from './session.store';

const API_PREFIX = '/api/';
const AUTH_PREFIX = '/api/auth/';

function isPrivateApiCall(request: HttpRequest<unknown>): boolean {
  const path = new URL(request.url, 'http://localhost').pathname;
  return path.startsWith(API_PREFIX) && !path.startsWith(AUTH_PREFIX);
}

/** Põe o token de acesso nas chamadas à API e, se ele venceu (401), renova uma vez e repete a chamada. */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  if (!isPrivateApiCall(request)) {
    return next(request);
  }
  const session = inject(SessionStore);
  const router = inject(Router);
  const withToken = (token: string | null) =>
    token ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : request;

  return next(withToken(session.accessToken())).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse) || error.status !== 401) {
        return throwError(() => error);
      }
      return from(session.refresh()).pipe(
        switchMap((renewed) => {
          if (!renewed) {
            session.clear();
            void router.navigateByUrl('/login');
            return throwError(() => error);
          }
          return next(withToken(session.accessToken()));
        }),
      );
    }),
  );
};
