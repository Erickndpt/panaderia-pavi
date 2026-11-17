import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { CompraRequest, CompraResponse } from './compra.model';
import { CompraService } from './compra-service';

describe('CompraService', () => {
  const base = 'http://localhost:8081/api/v1/compras';

  const request: CompraRequest = {
    proveedorId: 1,
    detalles: [{ insumoId: 5, cantidad: 25, precioUnitario: 3.5 }],
  };

  const respuesta: CompraResponse = {
    id: 10,
    proveedor: { id: 1, razonSocial: 'Molinos del Sur SAC' },
    fecha: '2026-09-14T11:30:00',
    estado: 'REGISTRADA',
    total: 87.5,
    detalles: [
      {
        id: 100,
        insumoId: 5,
        nombreInsumo: 'Harina',
        unidadMedida: 'KILOGRAMO',
        cantidad: 25,
        precioUnitario: 3.5,
        subtotal: 87.5,
      },
    ],
  };

  let service: CompraService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(CompraService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
  });

  it('debe registrar una compra mediante POST', () => {
    service.registrar(request).subscribe();

    const req = http.expectOne(base);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(respuesta);
  });

  it('debe listar las compras', () => {
    service.listar().subscribe();

    const req = http.expectOne(base);
    expect(req.request.method).toBe('GET');
    req.flush([respuesta]);
  });

  it('debe obtener una compra por su identificador', () => {
    service.obtener(10).subscribe();

    const req = http.expectOne(`${base}/10`);
    expect(req.request.method).toBe('GET');
    req.flush(respuesta);
  });

  it('debe anular una compra con PATCH y sin cuerpo', () => {
    service.anular(10).subscribe();

    const req = http.expectOne(`${base}/10/anular`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toBeNull();
    req.flush({ ...respuesta, estado: 'ANULADA' });
  });
});
