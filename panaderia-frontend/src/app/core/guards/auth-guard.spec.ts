import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, provideRouter, RouterStateSnapshot } from '@angular/router';
import { AuthService } from '../auth/auth-service';
import { authGuard } from './auth-guard';

describe('authGuard', () => {
  function ejecutar(autenticado: boolean, url = '/ventas/reporte') {
    const auth = {
      autenticado: () => autenticado,
      iniciarLogin: vi.fn().mockResolvedValue(undefined),
    };

    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [{ provide: AuthService, useValue: auth }, provideRouter([])],
    });

    const resultado = TestBed.runInInjectionContext(() =>
      authGuard({} as ActivatedRouteSnapshot, { url } as RouterStateSnapshot),
    );

    return { resultado, auth };
  }

  it('permite el acceso cuando hay una sesión activa', () => {
    const { resultado, auth } = ejecutar(true);

    expect(resultado).toBe(true);
    expect(auth.iniciarLogin).not.toHaveBeenCalled();
  });

  it('inicia login conservando la url solicitada cuando no hay sesión', () => {
    const { resultado, auth } = ejecutar(false, '/administracion/categorias');

    expect(resultado).toBe(false);
    expect(auth.iniciarLogin).toHaveBeenCalledWith('/administracion/categorias');
  });
});
