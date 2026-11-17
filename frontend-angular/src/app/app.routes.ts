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
  // Protótipo da Fase 0.5: sem backend, só com a fixture. A tela real do caixa é da Fase 3.
  { path: 'componentes', loadComponent: () => import('./ui/gallery.page').then((m) => m.GalleryPage) },
  { path: 'caixa', loadComponent: () => import('./caixa/caixa.page').then((m) => m.CaixaPage) },
  { path: '**', redirectTo: 'me' },
];
