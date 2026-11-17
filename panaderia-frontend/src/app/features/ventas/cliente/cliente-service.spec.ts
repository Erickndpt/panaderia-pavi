import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Cliente, ClienteRequest } from './cliente.model';
import { ClienteService } from './cliente-service';

describe('ClienteService', () => {
  const base = 'http://localhost:8081/api/v1/clientes';

  const cliente: Cliente = {
    id: 1,
    tipoDocumento: 'DNI',
    numeroDocumento: '12345678',
    nombreRazonSocial: 'María López',
    telefono: null,
    email: null,
    direccion: null,
    pais: 'Perú',
    activo: true,
  };

  const request: ClienteRequest = {
    tipoDocumento: 'DNI',
    numeroDocumento: '12345678',
    nombreRazonSocial: 'María López',
    telefono: null,
    email: null,
    direccion: null,
    pais: 'Perú',
    activo: true,
  };

  let service: ClienteService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(ClienteService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
  });

  it('debe listar los clientes sin filtro', () => {
    service.listar().subscribe();

    const req = http.expectOne(base);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.has('buscar')).toBe(false);
    req.flush([cliente]);
  });

  it('debe enviar el filtro buscar al backend', () => {
    service.listar('lópez').subscribe();

    const req = http.expectOne((peticion) => peticion.url === base);
    expect(req.request.params.get('buscar')).toBe('lópez');
    req.flush([cliente]);
  });

  it('debe crear un cliente mediante POST', () => {
    service.crear(request).subscribe();

    const req = http.expectOne(base);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(cliente);
  });

  it('debe actualizar un cliente mediante PUT', () => {
    service.actualizar(1, { ...request, activo: false }).subscribe();

    const req = http.expectOne(`${base}/1`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body.activo).toBe(false);
    req.flush({ ...cliente, activo: false });
  });
});
