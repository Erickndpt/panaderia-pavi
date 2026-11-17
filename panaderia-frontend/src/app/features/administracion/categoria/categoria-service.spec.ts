import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { CategoriaRequest } from './categoria.model';
import { CategoriaService } from './categoria-service';

describe('CategoriaService', () => {
  const base = 'http://localhost:8081/api/v1/categorias';

  const request: CategoriaRequest = { nombre: 'Panes', descripcion: null };

  let service: CategoriaService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(CategoriaService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
  });

  it('debe listar las categorías', () => {
    service.listar().subscribe();

    const req = http.expectOne(base);
    expect(req.request.method).toBe('GET');
    req.flush([{ id: 1, nombre: 'Panes', descripcion: null }]);
  });

  it('debe obtener una categoría por su identificador', () => {
    service.obtener(4).subscribe();

    const req = http.expectOne(`${base}/4`);
    expect(req.request.method).toBe('GET');
    req.flush({ id: 4, nombre: 'Bebidas', descripcion: 'Bebidas frías' });
  });

  it('debe crear una categoría mediante POST', () => {
    service.crear(request).subscribe();

    const req = http.expectOne(base);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush({ id: 9, ...request });
  });

  it('debe actualizar una categoría mediante PUT', () => {
    service.actualizar(4, request).subscribe();

    const req = http.expectOne(`${base}/4`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(request);
    req.flush({ id: 4, ...request });
  });
});
