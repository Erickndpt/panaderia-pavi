import {
  ApplicationConfig,
  importProvidersFrom,
  provideBrowserGlobalErrorListeners,
  provideZonelessChangeDetection,
} from '@angular/core';
import { MatDialogModule } from '@angular/material/dialog';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { authInterceptor } from './core/interceptors/auth-interceptor';
import { traceIdInterceptor } from './core/interceptors/trace-id-interceptor';
import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    importProvidersFrom(MatDialogModule),
    provideRouter(routes),
    provideZonelessChangeDetection(),
    provideHttpClient(withInterceptors([authInterceptor, traceIdInterceptor])),
  ],
};
