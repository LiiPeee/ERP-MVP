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
  { path: '**', redirectTo: 'me' },
];
