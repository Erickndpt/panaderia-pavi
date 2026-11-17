import { inject } from '@angular/core';
import { CanActivateFn, RouterStateSnapshot } from '@angular/router';
import { AuthService } from '../auth/auth-service';

export const authGuard: CanActivateFn = (_ruta, estado: RouterStateSnapshot) => {
  const auth = inject(AuthService);

  if (auth.autenticado()) {
    return true;
  }

  void auth.iniciarLogin(estado.url);
  return false;
};
