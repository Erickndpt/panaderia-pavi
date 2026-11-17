import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { signal } from '@angular/core';
import { AuthService } from '../../../core/auth/auth-service';
import { VentaResponse } from '../venta/venta.model';
import { MetodoPago, PagoVentaResponse, PagoVentaResumenResponse } from './pago-venta.model';
import { PagoVenta } from './pago-venta';

describe('PagoVenta', () => {
  const ventasBase = 'http://localhost:8081/api/v1/ventas';
  const pagosBase = 'http://localhost:8081/api/v1/pagos-venta';
  const pagosVentaBase = `${ventasBase}/10/pagos`;
  const resumenBase = `${ventasBase}/10/pagos/resumen`;
  const pagosVenta11Base = `${ventasBase}/11/pagos`;
  const resumenVenta11Base = `${ventasBase}/11/pagos/resumen`;
  const pagosVenta12Base = `${ventasBase}/12/pagos`;
  const resumenVenta12Base = `${ventasBase}/12/pagos/resumen`;
  const roles = signal<string[]>(['ADMIN']);
  const authFake = {
    autenticado: signal(true),
    usuario: signal('Admin'),
    roles,
    tieneRol: vi.fn((...solicitados: string[]) => solicitados.some((rol) => roles().includes(rol))),
    iniciarLogin: vi.fn(),
    cerrarSesion: vi.fn(),
  };

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
      vendedor: { id: 'usuario-2', username: 'luis', nombre: 'Luis Torres' },
      detalles: [],
    },
    {
      id: 12,
      fecha: '2026-09-20T10:00:00',
      tipo: 'LOCAL',
      paisDestino: null,
      cliente: {
        id: 2,
        tipoDocumento: 'DNI',
        numeroDocumento: '12345678',
        nombreRazonSocial: 'María López',
      },
      estado: 'REGISTRADA',
      total: 30,
      vendedor: null,
      detalles: [],
    },
  ];

  const pagos: PagoVentaResponse[] = [
    {
      id: 50,
      ventaId: 10,
      fecha: '2026-09-14T11:30:00',
      metodoPago: 'YAPE',
      monto: 10.5,
      referencia: 'YAPE-001',
      estado: 'REGISTRADO',
    },
    {
      id: 51,
      ventaId: 10,
      fecha: '2026-09-15T09:00:00',
      metodoPago: 'EFECTIVO',
      monto: 5,
      referencia: null,
      estado: 'ANULADO',
    },
  ];

  const resumen: PagoVentaResumenResponse = {
    ventaId: 10,
    totalVenta: 25,
    totalPagado: 10.5,
    saldoPendiente: 14.5,
    estadoPago: 'PARCIAL',
  };

  let fixture: ComponentFixture<PagoVenta>;
  let component: PagoVenta;
  let http: HttpTestingController;

  const porUrl = (url: string) => (req: { url: string }) => req.url === url;

  beforeEach(() => {
    roles.set(['ADMIN']);
  });

  async function flushPagosYResumen(
    urlPagos: string,
    urlResumen: string,
    listaPagos: PagoVentaResponse[],
    resumenPagos: PagoVentaResumenResponse,
  ): Promise<void> {
    http.expectOne(porUrl(urlPagos)).flush(listaPagos);
    http.expectOne(porUrl(urlResumen)).flush(resumenPagos);
    await refrescar();
  }

  async function refrescar(): Promise<void> {
    await fixture.whenStable();
    fixture.detectChanges();
  }

  async function configurar(opciones: { ventaId?: string } = {}): Promise<void> {
    await TestBed.configureTestingModule({
      imports: [PagoVenta],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: authFake },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              queryParamMap: convertToParamMap(
                opciones.ventaId !== undefined ? { ventaId: opciones.ventaId } : {},
              ),
            },
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PagoVenta);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();

    http.expectOne(porUrl(ventasBase)).flush(ventas);
    await refrescar();
  }

  afterEach(() => {
    http.verify({ ignoreCancelled: true });
  });

  it('should create', async () => {
    await configurar({ ventaId: '10' });
    await flushPagosYResumen(pagosVentaBase, resumenBase, pagos, resumen);
    expect(component).toBeTruthy();
  });

  it('debe cargar la venta del query param con sus pagos y resumen', async () => {
    await configurar({ ventaId: '10' });
    await flushPagosYResumen(pagosVentaBase, resumenBase, pagos, resumen);

    expect(component.ventaSeleccionada()?.id).toBe(10);
    expect(component.pagos()).toEqual(pagos);
    expect(component.resumen()).toEqual(resumen);

    const compilado = fixture.nativeElement as HTMLElement;
    expect(compilado.textContent).toContain('Saldo pendiente');
    expect(compilado.textContent).toContain('Parcial');
    expect(compilado.textContent).toContain('S/ 14.50');
    expect(compilado.textContent).toContain('Ana Perez');
  });

  it('debe cargar pagos y resumen al seleccionar una venta manualmente', async () => {
    await configurar();

    expect(component.ventaSeleccionada()).toBeNull();

    component.seleccionarVenta(ventas[0]);
    await flushPagosYResumen(pagosVentaBase, resumenBase, pagos, resumen);

    expect(component.ventaSeleccionada()?.id).toBe(10);
    expect(component.pagos().length).toBe(2);
    expect(component.resumen()?.totalPagado).toBe(10.5);
  });

  it('debe mostrar el estado vacío sin venta seleccionada', async () => {
    await configurar();

    const compilado = fixture.nativeElement as HTMLElement;
    expect(compilado.textContent).toContain(
      'Selecciona una venta para consultar y registrar sus pagos',
    );
    expect(component.pagos()).toEqual([]);
    expect(component.resumen()).toBeNull();
  });

  it('debe mostrar el estado vacío cuando la venta no tiene pagos', async () => {
    await configurar({ ventaId: '10' });
    await flushPagosYResumen(pagosVentaBase, resumenBase, [], {
      ...resumen,
      totalPagado: 0,
      saldoPendiente: 25,
      estadoPago: 'PENDIENTE',
    });

    const compilado = fixture.nativeElement as HTMLElement;
    expect(compilado.textContent).toContain('Esta venta aún no tiene pagos registrados');
  });

  it('debe renderizar los pagos registrados y los anulados', async () => {
    await configurar({ ventaId: '10' });
    await flushPagosYResumen(pagosVentaBase, resumenBase, pagos, resumen);

    const filas = (fixture.nativeElement as HTMLElement).querySelectorAll('tbody tr');
    expect(filas.length).toBe(2);

    const compilado = fixture.nativeElement as HTMLElement;
    expect(compilado.textContent).toContain('Yape');
    expect(compilado.textContent).toContain('YAPE-001');
    expect(compilado.textContent).toContain('S/ 10.50');
    expect(compilado.textContent).toContain('Registrado');
    expect(compilado.textContent).toContain('Anulado');
  });

  it('debe validar el método de pago y el monto antes de enviar', async () => {
    await configurar({ ventaId: '10' });
    await flushPagosYResumen(pagosVentaBase, resumenBase, pagos, resumen);

    component.registrar();
    await refrescar();

    http.expectNone(porUrl(pagosVentaBase));
    expect(component.form.invalid).toBe(true);
    expect(component.form.controls.metodoPago.touched).toBe(true);
    expect(component.form.controls.monto.touched).toBe(true);

    component.form.controls.metodoPago.setValue('YAPE');
    component.form.controls.monto.setValue(0);
    expect(component.form.controls.monto.hasError('montoNoPositivo')).toBe(true);

    component.form.controls.monto.setValue(10.555);
    expect(component.form.controls.monto.hasError('montoDecimales')).toBe(true);
  });

  it('debe impedir registrar un monto superior al saldo pendiente', async () => {
    await configurar({ ventaId: '10' });
    await flushPagosYResumen(pagosVentaBase, resumenBase, pagos, resumen);

    component.form.controls.metodoPago.setValue('YAPE');
    component.form.controls.monto.setValue(20);
    await refrescar();

    expect(component.montoExcedeSaldo()).toBe(true);
    expect(component.puedeRegistrar()).toBe(false);

    component.registrar();
    await refrescar();

    http.expectNone(porUrl(pagosVentaBase));
    expect(component.error()).toBe('El monto no puede superar el saldo pendiente de la venta.');
  });

  it('debe registrar un pago, bloquear el doble envío y recargar la información', async () => {
    await configurar({ ventaId: '10' });
    await flushPagosYResumen(pagosVentaBase, resumenBase, pagos, resumen);

    const nuevoPago: PagoVentaResponse = {
      id: 52,
      ventaId: 10,
      fecha: '2026-09-16T12:00:00',
      metodoPago: 'YAPE',
      monto: 14.5,
      referencia: 'YAPE-789',
      estado: 'REGISTRADO',
    };

    component.form.controls.metodoPago.setValue('YAPE');
    component.form.controls.monto.setValue(14.5);
    component.form.controls.referencia.setValue('  YAPE-789  ');
    component.registrar();
    component.registrar();
    await refrescar();

    const req = http.expectOne(porUrl(pagosVentaBase));
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      metodoPago: 'YAPE',
      monto: 14.5,
      referencia: 'YAPE-789',
    });
    req.flush(nuevoPago);
    await refrescar();

    await flushPagosYResumen(pagosVentaBase, resumenBase, [...pagos, nuevoPago], {
      ...resumen,
      totalPagado: 25,
      saldoPendiente: 0,
      estadoPago: 'PAGADO',
    });

    expect(component.exito()).toContain('registrado');
    expect(component.pagos().length).toBe(3);
    expect(component.form.getRawValue().metodoPago).toBe('');
    expect(component.resumen()?.estadoPago).toBe('PAGADO');
  });

  it('debe anular un pago y recargar lista y resumen', async () => {
    await configurar({ ventaId: '10' });
    await flushPagosYResumen(pagosVentaBase, resumenBase, pagos, resumen);

    const confirmar = vi.spyOn(window, 'confirm').mockReturnValue(true);
    component.anular(pagos[0]);
    await refrescar();

    const req = http.expectOne(porUrl(`${pagosBase}/50/anular`));
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toBeNull();
    req.flush({ ...pagos[0], estado: 'ANULADO' });
    await refrescar();

    await flushPagosYResumen(
      pagosVentaBase,
      resumenBase,
      [{ ...pagos[0], estado: 'ANULADO' }, pagos[1]],
      resumen,
    );

    confirmar.mockRestore();
    expect(component.exito()).toContain('anulado');
    expect(component.pagos()[0].estado).toBe('ANULADO');
  });

  it('no debe anular si el usuario cancela la confirmación', async () => {
    await configurar({ ventaId: '10' });
    await flushPagosYResumen(pagosVentaBase, resumenBase, pagos, resumen);

    const confirmar = vi.spyOn(window, 'confirm').mockReturnValue(false);
    component.anular(pagos[0]);
    await refrescar();

    confirmar.mockRestore();
    http.expectNone(porUrl(`${pagosBase}/50/anular`));
    expect(component.pagos()[0].estado).toBe('REGISTRADO');
  });

  it('VENDEDOR no debe ver ni ejecutar la anulación de pagos', async () => {
    roles.set(['VENDEDOR']);
    await configurar({ ventaId: '10' });
    await flushPagosYResumen(pagosVentaBase, resumenBase, pagos, resumen);

    const compilado = fixture.nativeElement as HTMLElement;
    expect(compilado.textContent).not.toContain('Anular');

    component.anular(pagos[0]);
    await refrescar();

    http.expectNone(porUrl(`${pagosBase}/50/anular`));
    expect(component.pagos()[0].estado).toBe('REGISTRADO');
  });

  it('debe bloquear el pago de una venta anulada', async () => {
    await configurar({ ventaId: '11' });
    await flushPagosYResumen(pagosVenta11Base, resumenVenta11Base, [], {
      ventaId: 11,
      totalVenta: 40,
      totalPagado: 0,
      saldoPendiente: 40,
      estadoPago: 'PENDIENTE',
    });

    const compilado = fixture.nativeElement as HTMLElement;
    expect(compilado.textContent).toContain('solo puede consultar sus pagos');

    expect(component.puedeRegistrar()).toBe(false);

    component.form.controls.metodoPago.setValue('EFECTIVO');
    component.form.controls.monto.setValue(5);
    component.registrar();
    await refrescar();

    http.expectNone(porUrl(pagosVenta11Base));
    expect(component.error()).toBe(
      'La venta seleccionada está anulada; no se pueden registrar pagos.',
    );
  });

  it('debe bloquear el pago cuando la venta está pagada', async () => {
    await configurar({ ventaId: '12' });
    await flushPagosYResumen(pagosVenta12Base, resumenVenta12Base, [], {
      ventaId: 12,
      totalVenta: 30,
      totalPagado: 30,
      saldoPendiente: 0,
      estadoPago: 'PAGADO',
    });

    const compilado = fixture.nativeElement as HTMLElement;
    expect(compilado.textContent).toContain('no se admiten nuevos pagos');

    expect(component.puedeRegistrar()).toBe(false);

    component.form.controls.metodoPago.setValue('EFECTIVO');
    component.form.controls.monto.setValue(1);
    component.registrar();
    await refrescar();

    http.expectNone(porUrl(pagosVenta12Base));
    expect(component.error()).toBe(
      'La venta ya está pagada en su totalidad; no se admiten más pagos.',
    );
  });

  it('debe mostrar el mensaje del backend cuando el registro falla', async () => {
    await configurar({ ventaId: '10' });
    await flushPagosYResumen(pagosVentaBase, resumenBase, pagos, resumen);

    component.form.controls.metodoPago.setValue('YAPE');
    component.form.controls.monto.setValue(10);
    component.registrar();
    await refrescar();

    http
      .expectOne(porUrl(pagosVentaBase))
      .flush(
        { message: 'El pago excede el saldo pendiente de la venta' },
        { status: 409, statusText: 'Conflict' },
      );
    await refrescar();

    expect(component.error()).toBe('El pago excede el saldo pendiente de la venta');
  });

  it('debe informar cuando falla la carga de pagos de la venta', async () => {
    await configurar();

    component.seleccionarVenta(ventas[0]);
    await refrescar();

    http
      .expectOne(porUrl(pagosVentaBase))
      .flush({ message: 'fallo' }, { status: 500, statusText: 'Server Error' });
    await refrescar();

    expect(component.error()).toBe('No se pudo cargar la información de pagos de la venta.');
    expect(component.pagos()).toEqual([]);
  });

  it('debe impedir registrar pagos con método inválido o monto no numérico del formulario', async () => {
    await configurar({ ventaId: '10' });
    await flushPagosYResumen(pagosVentaBase, resumenBase, pagos, resumen);

    component.form.controls.metodoPago.setValue('YAPE' as MetodoPago);
    component.form.controls.monto.setValue(14.5);
    await refrescar();

    expect(component.puedeRegistrar()).toBe(true);

    component.form.controls.metodoPago.setValue('');
    await refrescar();

    expect(component.puedeRegistrar()).toBe(false);
  });
});
