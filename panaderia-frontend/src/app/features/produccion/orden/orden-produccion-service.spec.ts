import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { OrdenProduccionRequest, OrdenProduccionResponse } from './orden-produccion.model';
import { OrdenProduccionService } from './orden-produccion-service';

describe('OrdenProduccionService', () => {
  const base = 'http://localhost:8081/api/v1/ordenes-produccion';

  const request: OrdenProduccionRequest = {
    productoId: 1,
    cantidadProducida: 12,
    insumos: [{ insumoId: 1, cantidadConsumida: 2.5 }],
  };

  const respuesta: OrdenProduccionResponse = {
    id: 1,
    fecha: '2026-09-14T10:30:00',
    estado: 'COMPLETADA',
    productoId: 1,
    nombreProducto: 'Pan francés',
    cantidadProducida: 12,
    detalles: [
      {
        id: 1,
        insumoId: 1,
        nombreInsumo: 'Harina de trigo',
        unidadMedida: 'KILOGRAMO',
        cantidadConsumida: 2.5,
      },
    ],
  };

  let service: OrdenProduccionService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(OrdenProduccionService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
  });

  it('debe registrar una orden mediante POST con el cuerpo exacto', () => {
    service.registrar(request).subscribe();

    const req = http.expectOne(base);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      productoId: 1,
      cantidadProducida: 12,
      insumos: [{ insumoId: 1, cantidadConsumida: 2.5 }],
    });
    req.flush(respuesta);
  });

  it('debe listar las órdenes de producción', () => {
    service.listar().subscribe();

    const req = http.expectOne(base);
    expect(req.request.method).toBe('GET');
    req.flush([respuesta]);
  });

  it('debe obtener una orden por su identificador', () => {
    service.obtener(1).subscribe();

    const req = http.expectOne(`${base}/1`);
    expect(req.request.method).toBe('GET');
    req.flush(respuesta);
  });

  it('debe propagar el error del backend al registrar', () => {
    let capturado: unknown;

    service.registrar(request).subscribe({
      error: (error) => {
        capturado = error;
      },
    });

    http
      .expectOne(base)
      .flush({ message: 'fallo' }, { status: 409, statusText: 'Conflict' });

    expect((capturado as { status: number }).status).toBe(409);
  });
});
