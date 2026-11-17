import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, from, switchMap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthService } from '../auth/auth-service';

function esUrlDelBackend(url: string): boolean {
  const base = environment.apiBaseUrl.replace(/\/+$/, '');
  return url === base || url.startsWith(`${base}/`);
}

export const authInterceptor: HttpInterceptorFn = (peticion, siguiente) => {
  if (!esUrlDelBackend(peticion.url)) {
    return siguiente(peticion);
  }

  const auth = inject(AuthService);
  const router = inject(Router);
  const token = auth.accessToken();
  const conToken = token
    ? peticion.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : peticion;

  return siguiente(conToken).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse) || error.status !== 401) {
        return throwError(() => error);
      }

      return from(auth.refrescar()).pipe(
        switchMap((refrescado) => {
          const nuevoToken = auth.accessToken();

          if (!refrescado || !nuevoToken) {
            void auth.iniciarLogin(router.url);
            return throwError(() => error);
          }

          return siguiente(
            peticion.clone({ setHeaders: { Authorization: `Bearer ${nuevoToken}` } }),
          );
        }),
      );
    }),
  );
};
