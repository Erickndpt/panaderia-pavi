import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ProveedorRequest } from './proveedor.model';
import { ProveedorService } from './proveedor-service';

describe('ProveedorService', () => {
  const base = 'http://localhost:8081/api/v1/proveedores';

  const request: ProveedorRequest = {
    ruc: '20123456789',
    razonSocial: 'Molinos del Sur SAC',
    telefono: '987654321',
    email: null,
    activo: true,
  };

  let service: ProveedorService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(ProveedorService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
  });

  it('debe listar los proveedores', () => {
    service.listar().subscribe();

    const req = http.expectOne(base);
    expect(req.request.method).toBe('GET');
    req.flush([{ id: 1, ...request }]);
  });

  it('debe obtener un proveedor por su identificador', () => {
    service.obtener(1).subscribe();

    const req = http.expectOne(`${base}/1`);
    expect(req.request.method).toBe('GET');
    req.flush({ id: 1, ...request });
  });

  it('debe crear un proveedor mediante POST', () => {
    service.crear(request).subscribe();

    const req = http.expectOne(base);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush({ id: 1, ...request });
  });

  it('debe actualizar un proveedor mediante PUT', () => {
    service.actualizar(1, { ...request, activo: false }).subscribe();

    const req = http.expectOne(`${base}/1`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ ...request, activo: false });
    req.flush({ id: 1, ...request, activo: false });
  });
});
