import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  CanActivateFn,
  provideRouter,
  Route,
  RouterStateSnapshot,
  UrlTree,
} from '@angular/router';
import { AuthService } from './core/auth/auth-service';
import { routes } from './app.routes';

describe('rutas por rol', () => {
  const layout = routes.find((ruta) => ruta.children?.length) as Route;
  const rutasHijas = layout.children ?? [];

  function obtenerRuta(path: string): Route {
    const ruta = rutasHijas.find((hija) => hija.path === path);

    if (!ruta) {
      throw new Error(`No se encontró la ruta ${path}`);
    }

    return ruta;
  }

  function evaluar(path: string, rolesSesion: string[]) {
    const ruta = obtenerRuta(path);
    const auth = {
      autenticado: () => true,
      tieneRol: vi.fn((...solicitados: string[]) =>
        rolesSesion.includes('GERENTE_GENERAL') || solicitados.some((rol) => rolesSesion.includes(rol)),
      ),
      iniciarLogin: vi.fn(),
    };

    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [{ provide: AuthService, useValue: auth }, provideRouter([])],
    });

    const guards = (ruta.canActivate ?? []).filter(
      (guard): guard is CanActivateFn => typeof guard === 'function',
    );

    return guards.map((guard) =>
      TestBed.runInInjectionContext(() =>
        guard({} as ActivatedRouteSnapshot, { url: `/${path}` } as RouterStateSnapshot),
      ),
    );
  }

  function permite(path: string, rolesSesion: string[]): boolean {
    const resultados = evaluar(path, rolesSesion);
    return resultados.length > 0 && resultados.every((resultado) => resultado === true);
  }

  function redirigeASinPermiso(path: string, rolesSesion: string[]): boolean {
    return evaluar(path, rolesSesion).some(
      (resultado) => resultado instanceof UrlTree && String(resultado) === '/sin-permiso',
    );
  }

  describe('VENDEDOR', () => {
    it('solo puede abrir el área de ventas', () => {
      const permitidas = ['ventas/clientes', 'ventas/nueva', 'ventas/reporte', 'ventas/pagos'];

      for (const path of permitidas) {
        expect(permite(path, ['VENDEDOR']), path).toBe(true);
      }
    });

    it.each([
      'administracion/categorias',
      'administracion/productos',
      'inventario/insumos',
      'inventario/existencias',
      'compras/proveedores',
      'compras/reporte',
      'produccion/ordenes',
      'finanzas/movimientos',
      'finanzas/reporte',
    ])('redirige la ruta directa %s a /sin-permiso', (path) => {
      expect(redirigeASinPermiso(path, ['VENDEDOR'])).toBe(true);
    });

    it('puede gestionar clientes dentro del área de ventas', () => {
      expect(permite('ventas/clientes/nuevo', ['VENDEDOR'])).toBe(true);
      expect(permite('ventas/clientes/:id/editar', ['VENDEDOR'])).toBe(true);
    });
  });

  describe('SUPERVISOR', () => {
    it('puede abrir las pantallas de consulta', () => {
      const consultas = [
        'administracion/categorias',
        'administracion/productos',
        'inventario/insumos',
        'inventario/existencias',
        'compras/proveedores',
        'compras/reporte',
        'produccion/ordenes',
        'ventas/clientes',
        'ventas/nueva',
        'ventas/reporte',
        'ventas/pagos',
        'finanzas/movimientos',
        'finanzas/reporte',
      ];

      for (const path of consultas) {
        expect(permite(path, ['SUPERVISOR']), path).toBe(true);
      }
    });

    it.each([
      'administracion/categorias/nueva',
      'administracion/categorias/:id/editar',
      'administracion/productos/nuevo',
      'administracion/productos/:id/editar',
      'inventario/insumos/nuevo',
      'inventario/insumos/:id/editar',
      'inventario/existencias/nuevo',
      'inventario/existencias/:id/editar',
      'compras/proveedores/nuevo',
      'compras/proveedores/:id/editar',
      'compras/nueva',
      'produccion/nueva',
    ])('no puede abrir la acción administrativa %s', (path) => {
      expect(redirigeASinPermiso(path, ['SUPERVISOR'])).toBe(true);
    });
  });

  describe('ADMIN', () => {
    it('puede abrir la gestión administrativa', () => {
      const administrativas = [
        'administracion/categorias',
        'administracion/categorias/nueva',
        'administracion/categorias/:id/editar',
        'administracion/productos',
        'administracion/productos/nuevo',
        'administracion/productos/:id/editar',
        'inventario/insumos',
        'inventario/insumos/nuevo',
        'inventario/insumos/:id/editar',
        'inventario/existencias',
        'inventario/existencias/nuevo',
        'inventario/existencias/:id/editar',
        'compras/proveedores',
        'compras/proveedores/nuevo',
        'compras/proveedores/:id/editar',
        'compras/nueva',
        'compras/reporte',
        'produccion/nueva',
        'produccion/ordenes',
        'ventas/clientes',
        'ventas/reporte',
        'ventas/pagos',
        'finanzas/movimientos',
        'finanzas/reporte',
      ];

      for (const path of administrativas) {
        expect(permite(path, ['ADMIN']), path).toBe(true);
      }
    });

    it('no puede abrir Nueva venta', () => {
      expect(redirigeASinPermiso('ventas/nueva', ['ADMIN'])).toBe(true);
    });
  });

  describe('GERENTE_GENERAL', () => {
    it('puede abrir todas las rutas funcionales', () => {
      const funcionales = rutasHijas
        .map((ruta) => ruta.path)
        .filter(
          (path): path is string => Boolean(path) && path !== 'sin-permiso' && path !== '**',
        );

      for (const path of funcionales) {
        expect(permite(path, ['GERENTE_GENERAL']), path).toBe(true);
      }
    });
  });
});
