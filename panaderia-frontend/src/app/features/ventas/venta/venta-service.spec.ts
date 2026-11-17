import { HttpRequest, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ReporteVentaResponse, VentaFiltros, VentaRequest, VentaResponse } from './venta.model';
import { VentaService } from './venta-service';

describe('VentaService', () => {
  const base = 'http://localhost:8081/api/v1/ventas';

  const request: VentaRequest = {
    clienteId: 1,
    tipo: 'LOCAL',
    paisDestino: null,
    detalles: [{ productoId: 5, cantidad: 10 }],
  };

  const respuesta: VentaResponse = {
    id: 10,
    fecha: '2026-09-14T11:30:00',
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
  };

  const resumen: ReporteVentaResponse = {
    cantidadVentas: 1,
    montoTotal: 25,
    cantidadLocales: 1,
    montoLocal: 25,
    cantidadExportaciones: 0,
    montoExportacion: 0,
  };

  let service: VentaService;
  let http: HttpTestingController;

  const porUrl = (url: string) => (req: HttpRequest<unknown>) => req.url === url;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(VentaService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
  });

  it('debe registrar una venta mediante POST con el cuerpo exacto', () => {
    service.registrar(request).subscribe();

    const req = http.expectOne(porUrl(base));
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(respuesta);
  });

  it('debe listar las ventas sin filtros', () => {
    service.listar().subscribe();

    const req = http.expectOne(porUrl(base));
    expect(req.request.method).toBe('GET');
    expect(req.request.params.keys().length).toBe(0);
    req.flush([respuesta]);
  });

  it('debe enviar los filtros combinados con fechas ISO-8601', () => {
    const filtros: VentaFiltros = {
      tipo: 'EXPORTACION',
      estado: 'REGISTRADA',
      fechaDesde: '2026-09-01',
      fechaHasta: '2026-09-30',
      paisDestino: 'Chile',
      productoId: 5,
      clienteId: 1,
      ordenFecha: 'ASC',
    };

    service.listar(filtros).subscribe();

    const req = http.expectOne(porUrl(base));
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('tipo')).toBe('EXPORTACION');
    expect(req.request.params.get('estado')).toBe('REGISTRADA');
    expect(req.request.params.get('fechaDesde')).toBe('2026-09-01T00:00:00');
    expect(req.request.params.get('fechaHasta')).toBe('2026-09-30T23:59:59.999');
    expect(req.request.params.get('paisDestino')).toBe('Chile');
    expect(req.request.params.get('productoId')).toBe('5');
    expect(req.request.params.get('clienteId')).toBe('1');
    expect(req.request.params.get('ordenFecha')).toBe('ASC');
    req.flush([respuesta]);
  });

  it('debe obtener el resumen con el rango de fechas', () => {
    service.resumen('2026-09-01', '2026-09-30').subscribe();

    const req = http.expectOne(porUrl(`${base}/resumen`));
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('fechaDesde')).toBe('2026-09-01T00:00:00');
    expect(req.request.params.get('fechaHasta')).toBe('2026-09-30T23:59:59.999');
    req.flush(resumen);
  });

  it('debe obtener una venta por su identificador', () => {
    service.obtener(10).subscribe();

    const req = http.expectOne(porUrl(`${base}/10`));
    expect(req.request.method).toBe('GET');
    req.flush(respuesta);
  });

  it('debe anular una venta con PATCH y sin cuerpo', () => {
    service.anular(10).subscribe();

    const req = http.expectOne(porUrl(`${base}/10/anular`));
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toBeNull();
    req.flush({ ...respuesta, estado: 'ANULADA' });
  });

  it('debe propagar los errores del backend', () => {
    let estado: number | null = null;

    service.obtener(999).subscribe({
      error: (error) => {
        estado = (error as { status: number }).status;
      },
    });

    http
      .expectOne(porUrl(`${base}/999`))
      .flush({ message: 'Venta no encontrada' }, { status: 404, statusText: 'Not Found' });

    expect(estado).toBe(404);
  });
});
