import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { AuthService } from '../auth/auth-service';
import { Layout } from './layout';

describe('Layout', () => {
  const autenticado = signal(true);
  const usuario = signal<string | null>('Ana Pérez');
  const roles = signal<string[]>(['ADMIN']);
  const authFake = {
    autenticado,
    usuario,
    roles,
    tieneRol: vi.fn(
      (...solicitados: string[]) =>
        roles().includes('GERENTE_GENERAL') || solicitados.some((rol) => roles().includes(rol)),
    ),
    iniciarLogin: vi.fn(async () => undefined),
    cerrarSesion: vi.fn(),
  };

  let component: Layout;
  let fixture: ComponentFixture<Layout>;

  beforeEach(async () => {
    autenticado.set(true);
    usuario.set('Ana Pérez');
    roles.set(['ADMIN']);
    authFake.iniciarLogin.mockClear();
    authFake.cerrarSesion.mockClear();

    await TestBed.configureTestingModule({
      imports: [Layout],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authFake },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({}) } },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Layout);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  function texto(): string {
    return (fixture.nativeElement as HTMLElement).textContent ?? '';
  }

  function destinos(): (string | null)[] {
    return Array.from(
      fixture.nativeElement.querySelectorAll('.menu-lateral__enlace') as NodeListOf<Element>,
    ).map((enlace) => enlace.getAttribute('href'));
  }

  function gruposMenu(): string[] {
    return Array.from(
      fixture.nativeElement.querySelectorAll('.menu-lateral__grupo') as NodeListOf<Element>,
    ).map((grupo) => grupo.querySelector('span')?.textContent?.trim() ?? '');
  }

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('debe mostrar la marca y el menú de módulos con sesión', () => {
    const compiled = fixture.nativeElement as HTMLElement;

    expect(compiled.querySelector('.encabezado__titulo')?.textContent).toContain('Panadería Pavi');
    expect(compiled.querySelector('.menu-lateral__nav')).not.toBeNull();
  });

  it('debe enlazar las entradas de menú de inventario', () => {
    expect(destinos()).toContain('/inventario/insumos');
  });

  it('debe enlazar pagos y finanzas sin marcarlos como pendientes', () => {
    const destinosMenu = destinos();

    expect(destinosMenu).toContain('/ventas/pagos');
    expect(destinosMenu).toContain('/finanzas/movimientos');
    expect(destinosMenu).toContain('/finanzas/reporte');
    expect(texto()).not.toContain('Pendiente');
  });

  it('sin sesión muestra Iniciar sesión y oculta los grupos funcionales', async () => {
    autenticado.set(false);
    usuario.set(null);
    roles.set([]);
    fixture.detectChanges();
    await fixture.whenStable();

    expect(texto()).toContain('Iniciar sesión');
    expect(texto()).not.toContain('Cerrar sesión');
    expect(texto()).toContain('Inicio');
    expect(texto()).toContain('Inicia sesión para acceder a los módulos.');
    expect(fixture.nativeElement.querySelectorAll('.menu-lateral__grupo').length).toBe(0);

    const boton = Array.from(
      fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>,
    ).find((elemento) => elemento.textContent?.includes('Iniciar sesión'));

    boton?.click();
    expect(authFake.iniciarLogin).toHaveBeenCalledTimes(1);
  });

  it('con sesión muestra el nombre real y Cerrar sesión', () => {
    expect(texto()).toContain('Ana Pérez');
    expect(texto()).toContain('Cerrar sesión');
    expect(fixture.nativeElement.querySelectorAll('.menu-lateral__grupo').length).toBeGreaterThan(
      0,
    );

    const boton = Array.from(
      fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>,
    ).find((elemento) => elemento.textContent?.includes('Cerrar sesión'));

    boton?.click();
    expect(authFake.cerrarSesion).toHaveBeenCalledTimes(1);
  });

  it('muestra Nueva venta para VENDEDOR y SUPERVISOR, pero no para ADMIN', () => {
    roles.set(['VENDEDOR']);
    fixture.detectChanges();
    expect(destinos()).toContain('/ventas/nueva');

    roles.set(['SUPERVISOR']);
    fixture.detectChanges();
    expect(destinos()).toContain('/ventas/nueva');

    roles.set(['ADMIN']);
    fixture.detectChanges();
    expect(destinos()).not.toContain('/ventas/nueva');
  });

  it('filtra los módulos del menú según el rol', () => {
    roles.set(['VENDEDOR']);
    fixture.detectChanges();

    expect(gruposMenu()).toEqual(['Ventas']);
    expect(destinos()).toEqual([
      '/',
      '/ventas/clientes',
      '/ventas/nueva',
      '/ventas/reporte',
      '/ventas/pagos',
    ]);
    expect(destinos()).not.toContain('/administracion/categorias');
    expect(destinos()).not.toContain('/administracion/productos');
    expect(destinos()).not.toContain('/inventario/insumos');
    expect(destinos()).not.toContain('/inventario/existencias');
    expect(destinos()).not.toContain('/compras/proveedores');
    expect(destinos()).not.toContain('/compras/reporte');
    expect(destinos()).not.toContain('/produccion/ordenes');
    expect(destinos()).not.toContain('/finanzas/movimientos');

    roles.set(['SUPERVISOR']);
    fixture.detectChanges();

    expect(gruposMenu()).toEqual([
      'Administración',
      'Inventario',
      'Compras',
      'Producción',
      'Ventas',
      'Finanzas',
    ]);
    expect(destinos()).toContain('/administracion/categorias');
    expect(destinos()).toContain('/administracion/productos');
    expect(destinos()).toContain('/inventario/insumos');
    expect(destinos()).toContain('/inventario/existencias');
    expect(destinos()).toContain('/compras/proveedores');
    expect(destinos()).toContain('/compras/reporte');
    expect(destinos()).toContain('/produccion/ordenes');
    expect(destinos()).toContain('/ventas/clientes');
    expect(destinos()).toContain('/ventas/nueva');
    expect(destinos()).toContain('/ventas/reporte');
    expect(destinos()).toContain('/ventas/pagos');
    expect(destinos()).toContain('/finanzas/movimientos');
    expect(destinos()).toContain('/finanzas/reporte');
    expect(destinos()).not.toContain('/compras/nueva');
    expect(destinos()).not.toContain('/produccion/nueva');

    roles.set(['ADMIN']);
    fixture.detectChanges();

    expect(gruposMenu()).toEqual([
      'Administración',
      'Inventario',
      'Compras',
      'Producción',
      'Ventas',
      'Finanzas',
    ]);
    expect(destinos()).toContain('/administracion/categorias');
    expect(destinos()).toContain('/administracion/productos');
    expect(destinos()).toContain('/inventario/insumos');
    expect(destinos()).toContain('/inventario/existencias');
    expect(destinos()).toContain('/compras/proveedores');
    expect(destinos()).toContain('/compras/nueva');
    expect(destinos()).toContain('/compras/reporte');
    expect(destinos()).toContain('/produccion/nueva');
    expect(destinos()).toContain('/produccion/ordenes');
    expect(destinos()).toContain('/ventas/clientes');
    expect(destinos()).toContain('/ventas/reporte');
    expect(destinos()).toContain('/ventas/pagos');
    expect(destinos()).toContain('/finanzas/movimientos');
    expect(destinos()).toContain('/finanzas/reporte');
    expect(destinos()).not.toContain('/ventas/nueva');

    roles.set(['GERENTE_GENERAL']);
    fixture.detectChanges();

    expect(gruposMenu()).toEqual([
      'Administración',
      'Inventario',
      'Compras',
      'Producción',
      'Ventas',
      'Finanzas',
    ]);
    expect(destinos()).toContain('/administracion/categorias');
    expect(destinos()).toContain('/inventario/insumos');
    expect(destinos()).toContain('/compras/nueva');
    expect(destinos()).toContain('/produccion/nueva');
    expect(destinos()).toContain('/ventas/nueva');
    expect(destinos()).toContain('/ventas/reporte');
    expect(destinos()).toContain('/finanzas/reporte');
  });
});
