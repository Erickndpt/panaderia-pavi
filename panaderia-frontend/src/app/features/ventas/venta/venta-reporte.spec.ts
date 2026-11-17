import { HttpRequest, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthService } from '../../../core/auth/auth-service';
import { Producto } from '../../administracion/producto/producto.model';
import { Cliente } from '../cliente/cliente.model';
import { ReporteVentaResponse, VentaResponse } from './venta.model';
import { VentaReporte } from './venta-reporte';

describe('VentaReporte', () => {
  const ventasBase = 'http://localhost:8081/api/v1/ventas';
  const resumenBase = 'http://localhost:8081/api/v1/ventas/resumen';
  const clientesBase = 'http://localhost:8081/api/v1/clientes';
  const productosBase = 'http://localhost:8081/api/v1/productos';
  const roles = signal<string[]>(['ADMIN']);
  const authFake = {
    autenticado: signal(true),
    usuario: signal('Admin'),
    roles,
    tieneRol: vi.fn((...solicitados: string[]) => solicitados.some((rol) => roles().includes(rol))),
    iniciarLogin: vi.fn(),
    cerrarSesion: vi.fn(),
  };

  const clientes: Cliente[] = [
    {
      id: 1,
      tipoDocumento: 'RUC',
      numeroDocumento: '20123456789',
      nombreRazonSocial: 'Panadería del Sur SAC',
      telefono: null,
      email: null,
      direccion: null,
      pais: null,
      activo: true,
    },
  ];

  const productos: Producto[] = [
    { id: 5, nombre: 'Pan francés', precio: 2.5, categoria: { id: 1, nombre: 'Panadería' } },
  ];

  const ventas: VentaResponse[] = [
    {
      id: 10,
      fecha: '2026-09-10T10:00:00',
      tipo: 'LOCAL',
      paisDestino: null,
      cliente: {
        id: 1,
        tipoDocumento: 'RUC',
        numeroDocumento: '20123456789',
        nombreRazonSocial: 'Panadería del Sur SAC',
      },
      estado: 'REGISTRADA',
      total: 25,
      vendedor: { id: 'usuario-1', username: 'ana', nombre: 'Ana Perez' },
      detalles: [
        {
          id: 100,
          productoId: 5,
          nombreProducto: 'Pan francés',
          precioUnitario: 2.5,
          cantidad: 10,
          subtotal: 25,
        },
      ],
    },
    {
      id: 11,
      fecha: '2026-09-12T10:00:00',
      tipo: 'EXPORTACION',
      paisDestino: 'Chile',
      cliente: {
        id: 1,
        tipoDocumento: 'RUC',
        numeroDocumento: '20123456789',
        nombreRazonSocial: 'Panadería del Sur SAC',
      },
      estado: 'ANULADA',
      total: 40,
      vendedor: { id: 'usuario-2', username: 'luis', nombre: null },
      detalles: [
        {
          id: 101,
          productoId: 5,
          nombreProducto: 'Pan francés',
          precioUnitario: 2,
          cantidad: 20,
          subtotal: 40,
        },
      ],
    },
    {
      id: 12,
      fecha: '2026-09-20T10:00:00',
      tipo: 'EXPORTACION',
      paisDestino: 'Bolivia',
      cliente: {
        id: 1,
        tipoDocumento: 'RUC',
        numeroDocumento: '20123456789',
        nombreRazonSocial: 'Panadería del Sur SAC',
      },
      estado: 'REGISTRADA',
      total: 30,
      vendedor: null,
      detalles: [
        {
          id: 102,
          productoId: 5,
          nombreProducto: 'Pan francés',
          precioUnitario: 3,
          cantidad: 10,
          subtotal: 30,
        },
      ],
    },
  ];

  const resumen: ReporteVentaResponse = {
    cantidadVentas: 2,
    montoTotal: 55,
    cantidadLocales: 1,
    montoLocal: 25,
    cantidadExportaciones: 1,
    montoExportacion: 30,
  };

  let http: HttpTestingController;

  const porUrl = (url: string) => (req: HttpRequest<unknown>) => req.url === url;

  beforeEach(() => {
    roles.set(['ADMIN']);
  });

  async function configurar(): Promise<ComponentFixture<VentaReporte>> {
    await TestBed.configureTestingModule({
      imports: [VentaReporte],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: authFake },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(VentaReporte);
    fixture.detectChanges();

    http = TestBed.inject(HttpTestingController);
    http.expectOne(porUrl(clientesBase)).flush(clientes);
    http.expectOne(porUrl(productosBase)).flush(productos);
    http.expectOne(porUrl(ventasBase)).flush(ventas);

    if (roles().some((rol) => rol === 'SUPERVISOR' || rol === 'ADMIN')) {
      http.expectOne(porUrl(resumenBase)).flush(resumen);
    } else {
      http.expectNone(porUrl(resumenBase));
    }

    await fixture.whenStable();
    fixture.detectChanges();

    return fixture;
  }

  afterEach(() => {
    http.verify();
  });

  it('should create', async () => {
    const fixture = await configurar();
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('debe listar las ventas con su detalle visible', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    expect(component.ventas().length).toBe(3);
    expect(fixture.nativeElement.querySelectorAll('.venta').length).toBe(3);
    expect(fixture.nativeElement.textContent).toContain('Pan francés');
    expect(fixture.nativeElement.textContent).toContain('20123456789');
    expect(fixture.nativeElement.textContent).toContain('Ana Perez');
    expect(fixture.nativeElement.textContent).toContain('luis');
    expect(fixture.nativeElement.textContent).toContain('Vendedor no identificado');
  });

  it('debe cargar el resumen y calcular el promedio visual', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    expect(component.resumen()).toEqual(resumen);
    expect(component.promedioVisual()).toBe(27.5);
  });

  it('debe enviar los filtros combinados al backend', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    component.actualizarFiltroTipo('EXPORTACION');
    component.actualizarFiltroEstado('REGISTRADA');
    component.actualizarFiltroPais('Chile');
    component.actualizarTextoProducto('Pan francés');
    component.alSeleccionarProducto(productos[0]);
    component.actualizarTextoCliente('Panadería del Sur SAC');
    component.alSeleccionarCliente(clientes[0]);
    component.actualizarBorradorDesde('2026-09-01');
    component.actualizarBorradorHasta('2026-09-30');
    component.actualizarFiltroOrden('ASC');
    component.aplicar();
    await fixture.whenStable();

    const req = http.expectOne(porUrl(ventasBase));
    expect(req.request.params.get('tipo')).toBe('EXPORTACION');
    expect(req.request.params.get('estado')).toBe('REGISTRADA');
    expect(req.request.params.get('paisDestino')).toBe('Chile');
    expect(req.request.params.get('productoId')).toBe('5');
    expect(req.request.params.get('clienteId')).toBe('1');
    expect(req.request.params.get('fechaDesde')).toBe('2026-09-01T00:00:00');
    expect(req.request.params.get('fechaHasta')).toBe('2026-09-30T23:59:59.999');
    expect(req.request.params.get('ordenFecha')).toBe('ASC');
    req.flush([ventas[2]]);

    const reqResumen = http.expectOne(porUrl(resumenBase));
    expect(reqResumen.request.params.get('fechaDesde')).toBe('2026-09-01T00:00:00');
    expect(reqResumen.request.params.get('fechaHasta')).toBe('2026-09-30T23:59:59.999');
    reqResumen.flush(resumen);
    await fixture.whenStable();

    expect(component.hayFiltros()).toBe(true);
  });

  it('debe rechazar un rango de fechas invertido', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    component.actualizarBorradorDesde('2026-09-30');
    component.actualizarBorradorHasta('2026-09-01');
    component.aplicar();
    await fixture.whenStable();

    http.expectNone(porUrl(ventasBase));
    expect(component.errorFechas()).toBe(
      'La fecha inicial no puede ser posterior a la fecha final.',
    );
  });

  it('debe limpiar los filtros y recargar sin parámetros de filtro', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    component.actualizarFiltroEstado('ANULADA');
    component.aplicar();
    await fixture.whenStable();
    http.expectOne(porUrl(ventasBase)).flush([ventas[1]]);
    http.expectOne(porUrl(resumenBase)).flush(resumen);
    await fixture.whenStable();

    component.limpiar();
    await fixture.whenStable();
    const req = http.expectOne(porUrl(ventasBase));
    expect(req.request.params.get('estado')).toBeNull();
    expect(req.request.params.get('ordenFecha')).toBe('DESC');
    req.flush(ventas);
    http.expectOne(porUrl(resumenBase)).flush(resumen);
    await fixture.whenStable();

    expect(component.hayFiltros()).toBe(false);
  });

  it('debe anular una venta tras confirmar y recargar el resumen', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    const confirmar = vi.spyOn(window, 'confirm').mockReturnValue(true);
    component.anular(ventas[0]);
    await fixture.whenStable();

    const req = http.expectOne(porUrl(`${ventasBase}/10/anular`));
    expect(req.request.method).toBe('PATCH');
    req.flush({ ...ventas[0], estado: 'ANULADA' });
    await fixture.whenStable();

    const reqResumen = http.expectOne(porUrl(resumenBase));
    reqResumen.flush({
      ...resumen,
      cantidadVentas: 1,
      montoTotal: 30,
      cantidadLocales: 0,
      montoLocal: 0,
    });
    await fixture.whenStable();
    fixture.detectChanges();

    confirmar.mockRestore();
    expect(component.ventas()[0].estado).toBe('ANULADA');
    expect(component.exito()).toContain('anulada');
    expect(component.resumen()?.cantidadVentas).toBe(1);
  });

  it('no debe anular si el usuario cancela la confirmación', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    const confirmar = vi.spyOn(window, 'confirm').mockReturnValue(false);
    component.anular(ventas[0]);
    await fixture.whenStable();

    confirmar.mockRestore();
    http.expectNone(porUrl(`${ventasBase}/10/anular`));
    expect(component.ventas()[0].estado).toBe('REGISTRADA');
  });

  it('no debe mostrar el botón anular en ventas ANULADA pero sí el enlace a pagos', async () => {
    const fixture = await configurar();
    const articulos = fixture.nativeElement.querySelectorAll('.venta') as NodeListOf<HTMLElement>;

    expect(articulos[0].textContent).toContain('Anular');
    expect(articulos[1].textContent).not.toContain('Anular');
    expect(articulos[1].textContent).toContain('Gestionar pagos');

    const enlaceAnulada = articulos[1].querySelector('a[href*="/ventas/pagos"]');
    expect(enlaceAnulada).not.toBeNull();
    expect(enlaceAnulada?.getAttribute('href')).toContain('ventaId=11');

    const enlaceRegistrada = articulos[0].querySelector('a[href*="/ventas/pagos"]');
    expect(enlaceRegistrada?.getAttribute('href')).toContain('ventaId=10');
  });

  it('debe mostrar el mensaje cuando la venta tiene pagos registrados', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    const confirmar = vi.spyOn(window, 'confirm').mockReturnValue(true);
    component.anular(ventas[0]);
    await fixture.whenStable();

    http
      .expectOne(porUrl(`${ventasBase}/10/anular`))
      .flush(
        {
          message:
            'La venta 10 tiene pagos registrados y no puede anularse; anule los pagos primero',
        },
        { status: 409, statusText: 'Conflict' },
      );
    await fixture.whenStable();

    confirmar.mockRestore();
    expect(component.error()).toContain('pagos registrados');
  });

  it('debe mostrar el mensaje cuando la venta ya estaba anulada', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    const confirmar = vi.spyOn(window, 'confirm').mockReturnValue(true);
    component.anular(ventas[0]);
    await fixture.whenStable();

    http
      .expectOne(porUrl(`${ventasBase}/10/anular`))
      .flush(
        { message: 'La venta con id 10 ya está anulada' },
        { status: 409, statusText: 'Conflict' },
      );
    await fixture.whenStable();

    confirmar.mockRestore();
    expect(component.error()).toBe('La venta con id 10 ya está anulada');
  });

  it('debe mostrar el estado vacío cuando no hay ventas ni filtros', async () => {
    await TestBed.configureTestingModule({
      imports: [VentaReporte],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: authFake },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(VentaReporte);
    fixture.detectChanges();

    http = TestBed.inject(HttpTestingController);
    http.expectOne(porUrl(clientesBase)).flush([]);
    http.expectOne(porUrl(productosBase)).flush([]);
    http.expectOne(porUrl(ventasBase)).flush([]);
    http.expectOne(porUrl(resumenBase)).flush({
      cantidadVentas: 0,
      montoTotal: 0,
      cantidadLocales: 0,
      montoLocal: 0,
      cantidadExportaciones: 0,
      montoExportacion: 0,
    });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('No hay ventas registradas');
  });

  it('debe informar cuando falla la carga del reporte', async () => {
    await TestBed.configureTestingModule({
      imports: [VentaReporte],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: authFake },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(VentaReporte);
    fixture.detectChanges();

    http = TestBed.inject(HttpTestingController);
    http.expectOne(porUrl(clientesBase)).flush([]);
    http.expectOne(porUrl(productosBase)).flush([]);
    http
      .expectOne(porUrl(ventasBase))
      .flush({ message: 'fallo' }, { status: 500, statusText: 'Server Error' });
    http.expectOne(porUrl(resumenBase)).flush({
      cantidadVentas: 0,
      montoTotal: 0,
      cantidadLocales: 0,
      montoLocal: 0,
      cantidadExportaciones: 0,
      montoExportacion: 0,
    });
    await fixture.whenStable();

    expect(fixture.componentInstance.error()).toBe('No se pudo cargar el reporte de ventas.');
  });

  it('muestra Nueva venta para VENDEDOR/SUPERVISOR y Anular para SUPERVISOR/ADMIN', async () => {
    roles.set(['VENDEDOR']);
    const fixture = await configurar();
    const compiled = fixture.nativeElement as HTMLElement;

    expect(compiled.textContent).toContain('Nueva venta');
    expect(compiled.textContent).not.toContain('Anular');
    expect(compiled.textContent).not.toContain('Monto total');
    expect(compiled.textContent).toContain('Ana Perez');
    expect(fixture.componentInstance.resumen()).toBeNull();

    roles.set(['SUPERVISOR']);
    fixture.detectChanges();
    expect(compiled.textContent).toContain('Nueva venta');
    expect(compiled.querySelectorAll('.venta')[0].textContent).toContain('Anular');

    roles.set(['ADMIN']);
    fixture.detectChanges();
    expect(compiled.textContent).not.toContain('Nueva venta');
    expect(compiled.querySelectorAll('.venta')[0].textContent).toContain('Anular');
  });
});
