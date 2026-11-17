import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  provideRouter,
  RouterStateSnapshot,
  UrlTree,
} from '@angular/router';
import { AuthService } from '../auth/auth-service';
import { rolGuard } from './rol-guard';

describe('rolGuard', () => {
  function ejecutar(rolesDeSesion: string[], rolesRequeridos: string[]) {
    const auth = {
      tieneRol: vi.fn((...pedidos: string[]) =>
        pedidos.some((rol) => rolesDeSesion.includes(rol)),
      ),
    };

    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [{ provide: AuthService, useValue: auth }, provideRouter([])],
    });

    const guard = rolGuard(...rolesRequeridos);

    return TestBed.runInInjectionContext(() =>
      guard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    );
  }

  it('permite el acceso cuando el usuario tiene el rol requerido', () => {
    expect(ejecutar(['ADMIN'], ['ADMIN'])).toBe(true);
  });

  it('permite el acceso cuando tiene al menos uno de los roles requeridos', () => {
    expect(ejecutar(['SUPERVISOR'], ['VENDEDOR', 'SUPERVISOR'])).toBe(true);
  });

  it('redirige a /sin-permiso cuando el rol no corresponde', () => {
    const resultado = ejecutar(['VENDEDOR'], ['ADMIN']);

    expect(resultado).toBeInstanceOf(UrlTree);
    expect(String(resultado)).toBe('/sin-permiso');
  });
});
