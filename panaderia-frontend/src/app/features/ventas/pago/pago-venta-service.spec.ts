import { HttpRequest, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { PagoVentaRequest, PagoVentaResponse, PagoVentaResumenResponse } from './pago-venta.model';
import { PagoVentaService } from './pago-venta-service';

describe('PagoVentaService', () => {
  const ventasBase = 'http://localhost:8081/api/v1/ventas';
  const pagosBase = 'http://localhost:8081/api/v1/pagos-venta';

  const request: PagoVentaRequest = {
    metodoPago: 'YAPE',
    monto: 10.5,
    referencia: 'YAPE-001',
  };

  const pago: PagoVentaResponse = {
    id: 50,
    ventaId: 10,
    fecha: '2026-09-14T11:30:00',
    metodoPago: 'YAPE',
    monto: 10.5,
    referencia: 'YAPE-001',
    estado: 'REGISTRADO',
  };

  const resumen: PagoVentaResumenResponse = {
    ventaId: 10,
    totalVenta: 25,
    totalPagado: 10.5,
    saldoPendiente: 14.5,
    estadoPago: 'PARCIAL',
  };

  let service: PagoVentaService;
  let http: HttpTestingController;

  const porUrl = (url: string) => (req: HttpRequest<unknown>) => req.url === url;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(PagoVentaService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
  });

  it('debe registrar un pago mediante POST a /ventas/{id}/pagos con el cuerpo exacto', () => {
    service.registrar(10, request).subscribe();

    const req = http.expectOne(porUrl(`${ventasBase}/10/pagos`));
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(pago);
  });

  it('debe listar los pagos de una venta con GET', () => {
    service.listarPorVenta(10).subscribe();

    const req = http.expectOne(porUrl(`${ventasBase}/10/pagos`));
    expect(req.request.method).toBe('GET');
    req.flush([pago]);
  });

  it('debe obtener el resumen de pagos con GET a /resumen', () => {
    service.resumen(10).subscribe();

    const req = http.expectOne(porUrl(`${ventasBase}/10/pagos/resumen`));
    expect(req.request.method).toBe('GET');
    req.flush(resumen);
  });

  it('debe obtener un pago por id con GET a /pagos-venta/{id}', () => {
    service.obtener(50).subscribe();

    const req = http.expectOne(porUrl(`${pagosBase}/50`));
    expect(req.request.method).toBe('GET');
    req.flush(pago);
  });

  it('debe anular un pago con PATCH a /pagos-venta/{id}/anular y sin cuerpo', () => {
    service.anular(50).subscribe();

    const req = http.expectOne(porUrl(`${pagosBase}/50/anular`));
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toBeNull();
    req.flush({ ...pago, estado: 'ANULADO' });
  });

  it('debe propagar los errores del backend', () => {
    let estado: number | null = null;

    service.registrar(99, request).subscribe({
      error: (error) => {
        estado = (error as { status: number }).status;
      },
    });

    http
      .expectOne(porUrl(`${ventasBase}/99/pagos`))
      .flush({ message: 'Venta no encontrada' }, { status: 404, statusText: 'Not Found' });

    expect(estado).toBe(404);
  });
});
