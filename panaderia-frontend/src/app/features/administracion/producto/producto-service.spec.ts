import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ProductoService } from './producto-service';

describe('ProductoService', () => {
  const base = 'http://localhost:8081/api/v1/productos';

  let service: ProductoService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(ProductoService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
  });

  it('debe consultar el catálogo del producto padre', () => {
    service.listar().subscribe();

    const req = http.expectOne(base);
    expect(req.request.method).toBe('GET');
    req.flush([{ id: 1, nombre: 'Pan de crawit' }]);
  });

  it('debe obtener un producto por su identificador', () => {
    service.obtener(7).subscribe();

    const req = http.expectOne(`${base}/7`);
    expect(req.request.method).toBe('GET');
    req.flush({ id: 7, nombre: 'Croissant' });
  });
});
