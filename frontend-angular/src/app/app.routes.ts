import { Routes } from '@angular/router';
import { authenticatedGuard } from './session/authenticated.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'me' },
  { path: 'register', loadComponent: () => import('./pages/register.page').then((m) => m.RegisterPage) },
  { path: 'login', loadComponent: () => import('./pages/login.page').then((m) => m.LoginPage) },
  {
    path: 'me',
    canActivate: [authenticatedGuard],
    loadComponent: () => import('./pages/me.page').then((m) => m.MePage),
  },
  // Laboratório da Fase 0.5: telas sem backend, só com a fixture. A direção descartada sai depois da escolha.
  { path: 'design/a', loadComponent: () => import('./design/direction-a.page').then((m) => m.DirectionAPage) },
  { path: 'design/b', loadComponent: () => import('./design/direction-b.page').then((m) => m.DirectionBPage) },
  { path: '**', redirectTo: 'me' },
];
