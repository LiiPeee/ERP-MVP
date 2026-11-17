import { Routes } from '@angular/router';
import { authenticatedGuard } from './session/authenticated.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'fila' },
  { path: 'register', loadComponent: () => import('./pages/register.page').then((m) => m.RegisterPage) },
  { path: 'login', loadComponent: () => import('./pages/login.page').then((m) => m.LoginPage) },
  {
    path: 'me',
    canActivate: [authenticatedGuard],
    loadComponent: () => import('./pages/me.page').then((m) => m.MePage),
  },
  {
    path: 'fila',
    canActivate: [authenticatedGuard],
    loadComponent: () => import('./quotes/fila.page').then((m) => m.FilaPage),
  },
  {
    path: 'orcamento/novo',
    canActivate: [authenticatedGuard],
    loadComponent: () => import('./quotes/novo.page').then((m) => m.NovoPage),
  },
  {
    path: 'orcamento/:id',
    canActivate: [authenticatedGuard],
    loadComponent: () => import('./quotes/orcamento.page').then((m) => m.OrcamentoPage),
  },
  {
    path: 'oficina',
    canActivate: [authenticatedGuard],
    loadComponent: () => import('./quotes/oficina.page').then((m) => m.OficinaPage),
  },
  // A página do cliente: sem login. O token vem do endereço e é a única credencial.
  { path: 'o/:token', loadComponent: () => import('./quotes/publico.page').then((m) => m.PublicoPage) },
  { path: '**', redirectTo: 'fila' },
];
