import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { provideApi } from './api';
import { routes } from './app.routes';
import { authInterceptor } from './session/auth.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    // O X-XSRF-TOKEN sai do cookie XSRF-TOKEN: o Angular faz isso sozinho nas chamadas de mesma origem.
    provideHttpClient(withInterceptors([authInterceptor])),
    // Caminho relativo: o frontend e a API vivem na mesma origem (nada de CORS nem de terceiros).
    provideApi(''),
  ],
};
