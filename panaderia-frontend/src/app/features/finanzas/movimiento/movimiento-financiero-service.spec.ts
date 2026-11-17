import { HttpRequest, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import {
  MovimientoFinancieroResponse,
  ResumenFinancieroResponse,
} from './movimiento-financiero.model';
import { MovimientoFinancieroService } from './movimiento-financiero-service';

describe('MovimientoFinancieroService', () => {
  const base = 'http://localhost:8081/api/v1/movimientos-financieros';
  const resumenUrl = 'http://localhost:8081/api/v1/reportes/finanzas';

  const movimiento: MovimientoFinancieroResponse = {
    id: 1,
    fecha: '2026-09-14T11:30:00',
    tipo: 'INGRESO',
    origen: 'VENTA',
    origenId: 10,
    concepto: 'Ingreso por venta #10',
    monto: 25,
    estado: 'ACTIVO',
  };

  const resumen: ResumenFinancieroResponse = {
    cantidadIngresos: 3,
    montoIngresos: 120,
    cantidadEgresos: 1,
    montoEgresos: 40,
    saldo: 80,
  };

  let service: MovimientoFinancieroService;
  let http: HttpTestingController;

  const porUrl = (url: string) => (req: HttpRequest<unknown>) => req.url === url;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(MovimientoFinancieroService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
  });

  it('debe listar los movimientos con GET a la URL exacta del backend', () => {
    service.listarMovimientos().subscribe();

    const req = http.expectOne(porUrl(base));
    expect(req.request.method).toBe('GET');
    req.flush([movimiento]);
  });

  it('debe obtener un movimiento por id', () => {
    service.obtenerMovimiento(1).subscribe();

    const req = http.expectOne(porUrl(`${base}/1`));
    expect(req.request.method).toBe('GET');
    req.flush(movimiento);
  });

  it('debe obtener el resumen con GET a /reportes/finanzas', () => {
    service.obtenerResumen().subscribe();

    const req = http.expectOne(porUrl(resumenUrl));
    expect(req.request.method).toBe('GET');
    req.flush(resumen);
  });

  it('debe propagar los errores del backend', () => {
    let estado: number | null = null;

    service.obtenerMovimiento(99).subscribe({
      error: (error) => {
        estado = (error as { status: number }).status;
      },
    });

    http
      .expectOne(porUrl(`${base}/99`))
      .flush({ message: 'Movimiento no encontrado' }, { status: 404, statusText: 'Not Found' });

    expect(estado).toBe(404);
  });
});
