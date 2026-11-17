import { HttpClient, HttpErrorResponse, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AuthService } from '../api';
import { authInterceptor } from './auth.interceptor';
import { authenticatedGuard } from './authenticated.guard';
import { SessionStore } from './session.store';

const route = {} as ActivatedRouteSnapshot;
const state = {} as RouterStateSnapshot;

function configure(auth: Partial<AuthService>) {
  TestBed.configureTestingModule({
    providers: [
      provideRouter([{ path: 'login', children: [] }]),
      provideHttpClient(withInterceptors([authInterceptor])),
      provideHttpClientTesting(),
      { provide: AuthService, useValue: auth },
    ],
  });
}

describe('authInterceptor e o link público', () => {
  it('não põe o token do dono nem tenta renovar a sessão nas chamadas do link público', async () => {
    configure({ refresh: () => { throw new Error('não deveria renovar'); } });
    TestBed.inject(SessionStore)['token'].set('token-do-dono');
    const http = TestBed.inject(HttpClient);
    const backend = TestBed.inject(HttpTestingController);

    const result = http.get('/api/public/quotes/abc').subscribe({ error: () => undefined });
    const request = backend.expectOne('/api/public/quotes/abc');

    expect(request.request.headers.has('Authorization')).toBe(false);
    request.flush({}, { status: 401, statusText: 'Unauthorized' });
    result.unsubscribe();
    backend.verify();
  });
});

describe('SessionStore', () => {
  it('guarda o token só em memória, nunca em localStorage ou sessionStorage', async () => {
    configure({ login: () => of({ accessToken: 'token-sintetico', expiresAt: '2026-01-01T00:00:00Z' }) as never });
    const store = TestBed.inject(SessionStore);

    await store.login('ana@exemplo.test', 'senha-sintetica-12345');

    expect(store.isAuthenticated()).toBe(true);
    expect(store.accessToken()).toBe('token-sintetico');
    expect(localStorage.length).toBe(0);
    expect(sessionStorage.length).toBe(0);
  });

  it('compartilha uma só renovação entre várias chamadas simultâneas', async () => {
    let calls = 0;
    configure({ refresh: () => (calls++, of({ accessToken: 'novo-token' })) as never });
    const store = TestBed.inject(SessionStore);

    const results = await Promise.all([store.refresh(), store.refresh(), store.refresh()]);

    expect(results).toEqual([true, true, true]);
    expect(calls).toBe(1);
  });

  it('limpa o token quando a renovação falha', async () => {
    configure({ refresh: () => throwError(() => new Error('sem cookie')) });
    const store = TestBed.inject(SessionStore);

    expect(await store.refresh()).toBe(false);
    expect(store.isAuthenticated()).toBe(false);
  });
});

describe('authenticatedGuard', () => {
  it('deixa passar quem já tem sessão', async () => {
    configure({ login: () => of({ accessToken: 't' }) as never });
    await TestBed.inject(SessionStore).login('a@exemplo.test', 'x');

    expect(await TestBed.runInInjectionContext(() => authenticatedGuard(route, state))).toBe(true);
  });

  it('recupera a sessão pelo cookie depois de recarregar a página', async () => {
    configure({ refresh: () => of({ accessToken: 'recuperado' }) as never });

    expect(await TestBed.runInInjectionContext(() => authenticatedGuard(route, state))).toBe(true);
  });

  it('manda para o login quando não há sessão nem cookie', async () => {
    configure({ refresh: () => throwError(() => new Error('sem cookie')) });

    const result = await TestBed.runInInjectionContext(() => authenticatedGuard(route, state));

    expect(result instanceof UrlTree).toBe(true);
    expect((result as UrlTree).toString()).toBe('/login');
  });
});

describe('authInterceptor', () => {
  it('põe o token só nas chamadas da API que não são de autenticação', async () => {
    configure({ login: () => of({ accessToken: 'token-sintetico' }) as never });
    await TestBed.inject(SessionStore).login('a@exemplo.test', 'x');
    const http = TestBed.inject(HttpClient);
    const backend = TestBed.inject(HttpTestingController);

    http.get('/api/me').subscribe();
    http.post('/api/auth/login', {}).subscribe();

    expect(backend.expectOne('/api/me').request.headers.get('Authorization')).toBe('Bearer token-sintetico');
    expect(backend.expectOne('/api/auth/login').request.headers.has('Authorization')).toBe(false);
  });

  it('renova uma vez e repete a chamada quando recebe 401', async () => {
    configure({
      login: () => of({ accessToken: 'vencido' }) as never,
      refresh: () => of({ accessToken: 'renovado' }) as never,
    });
    await TestBed.inject(SessionStore).login('a@exemplo.test', 'x');
    const http = TestBed.inject(HttpClient);
    const backend = TestBed.inject(HttpTestingController);
    let body: unknown;

    http.get('/api/me').subscribe((value) => (body = value));
    backend.expectOne('/api/me').flush({}, { status: 401, statusText: 'Unauthorized' });
    await Promise.resolve();
    await new Promise((resolve) => setTimeout(resolve));
    const retry = backend.expectOne('/api/me');
    expect(retry.request.headers.get('Authorization')).toBe('Bearer renovado');
    retry.flush({ email: 'ana@exemplo.test' });

    expect(body).toEqual({ email: 'ana@exemplo.test' });
  });

  it('desiste e limpa a sessão quando a renovação falha', async () => {
    configure({
      login: () => of({ accessToken: 'vencido' }) as never,
      refresh: () => throwError(() => new Error('sem cookie')),
    });
    const store = TestBed.inject(SessionStore);
    await store.login('a@exemplo.test', 'x');
    const http = TestBed.inject(HttpClient);
    const backend = TestBed.inject(HttpTestingController);
    let failure: unknown;

    http.get('/api/me').subscribe({ error: (error) => (failure = error) });
    backend.expectOne('/api/me').flush({}, { status: 401, statusText: 'Unauthorized' });
    await new Promise((resolve) => setTimeout(resolve));

    expect(failure instanceof HttpErrorResponse).toBe(true);
    expect(store.isAuthenticated()).toBe(false);
  });
});
