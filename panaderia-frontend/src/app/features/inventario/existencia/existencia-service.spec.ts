import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ExistenciaService } from './existencia-service';

describe('ExistenciaService', () => {
  const base = 'http://localhost:8081/api/v1/existencias';

  let service: ExistenciaService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(ExistenciaService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
  });

  it('debe listar sin filtro cuando no se indica productoId', () => {
    service.listar().subscribe();

    const req = http.expectOne((peticion) => peticion.url === base);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.has('productoId')).toBe(false);
    req.flush([]);
  });

  it('debe enviar productoId como query param para filtrar en el backend', () => {
    service.listar(7).subscribe();

    const req = http.expectOne((peticion) => peticion.url === base);
    expect(req.request.params.get('productoId')).toBe('7');
    req.flush([]);
  });

  it('debe obtener una existencia por su identificador', () => {
    service.obtener(3).subscribe();

    const req = http.expectOne(`${base}/3`);
    expect(req.request.method).toBe('GET');
    req.flush({ id: 3, producto: { id: 7, nombre: 'Pan' }, cantidad: 10 });
  });

  it('debe consultar la existencia de un producto por productoId', () => {
    service.obtenerPorProducto(5).subscribe();

    const req = http.expectOne(`${base}/producto/5`);
    expect(req.request.method).toBe('GET');
    req.flush({ id: 21, producto: { id: 5, nombre: 'Chocotón' }, cantidad: 110 });
  });

  it('debe crear enviando el modelo de petición', () => {
    service.crear({ productoId: 7, cantidad: 12.5 }).subscribe();

    const req = http.expectOne(base);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ productoId: 7, cantidad: 12.5 });
    req.flush({ id: 1, producto: { id: 7, nombre: 'Pan' }, cantidad: 12.5 });
  });

  it('debe actualizar sobre el identificador de la existencia', () => {
    service.actualizar(3, { productoId: 7, cantidad: 0 }).subscribe();

    const req = http.expectOne(`${base}/3`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ productoId: 7, cantidad: 0 });
    req.flush({ id: 3, producto: { id: 7, nombre: 'Pan' }, cantidad: 0 });
  });

  it('debe eliminar sobre el identificador de la existencia', () => {
    service.eliminar(3).subscribe();

    const req = http.expectOne(`${base}/3`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
