import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ExistenciaResponse } from './existencia.model';
import { ExistenciaList } from './existencia-list';

describe('ExistenciaList', () => {
  const existenciasBase = 'http://localhost:8081/api/v1/existencias';
  const productosBase = 'http://localhost:8081/api/v1/productos';

  const existencias: ExistenciaResponse[] = [
    { id: 1, producto: { id: 7, nombre: 'Pan de crawit' }, cantidad: 12.5 },
    { id: 2, producto: { id: 8, nombre: 'Croissant' }, cantidad: 0 },
  ];

  const productos = [
    { id: 7, nombre: 'Pan de crawit' },
    { id: 8, nombre: 'Croissant' },
  ];

  let component: ExistenciaList;
  let fixture: ComponentFixture<ExistenciaList>;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ExistenciaList],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(ExistenciaList);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
  });

  async function refrescar(): Promise<void> {
    await fixture.whenStable();
    fixture.detectChanges();
  }

  function responderCatalogo(): void {
    http.expectOne((peticion) => peticion.url === productosBase).flush(productos);
  }

  function esPeticionExistencias(productoId?: string) {
    return (peticion: { url: string; params: { has(nombre: string): boolean; get(nombre: string): string | null } }) =>
      peticion.url === existenciasBase &&
      (peticion.params.has('productoId') ? peticion.params.get('productoId') : undefined) === productoId;
  }

  function responderExistencias(productoId?: string): void {
    http.expectOne(esPeticionExistencias(productoId)).flush(existencias);
  }

  it('should create', async () => {
    fixture.detectChanges();
    responderCatalogo();
    responderExistencias(undefined);
    await refrescar();

    expect(component).toBeTruthy();
  });

  it('debe consultar el catálogo de productos y las existencias al iniciar', async () => {
    fixture.detectChanges();
    responderCatalogo();
    responderExistencias(undefined);
    await refrescar();

    expect(component.productos()).toEqual(productos);
    expect(component.existencias()).toEqual(existencias);
  });

  it('debe mostrar el nombre del producto de cada existencia', async () => {
    fixture.detectChanges();
    responderCatalogo();
    responderExistencias(undefined);
    await refrescar();

    const texto = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(texto).toContain('Pan de crawit');
    expect(texto).toContain('Croissant');
    expect((fixture.nativeElement as HTMLElement).querySelectorAll('tbody tr').length).toBe(2);
  });

  it('debe enviar productoId al backend al filtrar por producto', async () => {
    fixture.detectChanges();
    responderCatalogo();
    responderExistencias(undefined);
    await refrescar();

    component.filtrarPorProducto(7);

    const req = http.expectOne(esPeticionExistencias('7'));
    expect(req.request.method).toBe('GET');
    req.flush([existencias[0]]);
    await refrescar();

    expect(component.existencias()).toEqual([existencias[0]]);
  });

  it('debe volver a consultar sin filtro al elegir todos los productos', async () => {
    fixture.detectChanges();
    responderCatalogo();
    responderExistencias(undefined);
    await refrescar();

    component.filtrarPorProducto(7);
    http.expectOne(esPeticionExistencias('7')).flush([existencias[0]]);
    await refrescar();

    component.filtrarPorProducto(null);

    const req = http.expectOne(esPeticionExistencias(undefined));
    req.flush(existencias);
    await refrescar();

    expect(component.existencias()).toEqual(existencias);
  });

  it('debe informar cuando no hay existencias registradas', async () => {
    fixture.detectChanges();
    responderCatalogo();
    http.expectOne((peticion) => peticion.url === existenciasBase).flush([]);
    await refrescar();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain(
      'No hay existencias registradas',
    );
  });

  it('debe diferenciar el error de carga del backend', async () => {
    fixture.detectChanges();
    responderCatalogo();
    http
      .expectOne((peticion) => peticion.url === existenciasBase)
      .flush({ message: 'fallo' }, { status: 500, statusText: 'Server Error' });
    await refrescar();

    expect(component.error()).toBe('No se pudo cargar la lista de existencias.');
  });

  it('debe avisar cuando el catálogo de productos no se puede cargar', async () => {
    fixture.detectChanges();
    http
      .expectOne((peticion) => peticion.url === productosBase)
      .flush({ message: 'fallo' }, { status: 500, statusText: 'Server Error' });
    responderExistencias(undefined);
    await refrescar();

    expect(component.productosError()).toContain('No se pudo cargar el catálogo de productos');
  });

  it('debe eliminar la existencia y refrescar la lista', async () => {
    fixture.detectChanges();
    responderCatalogo();
    responderExistencias(undefined);
    await refrescar();

    const confirmar = vi.spyOn(window, 'confirm').mockReturnValue(true);
    component.eliminar(existencias[0]);

    const req = http.expectOne(`${existenciasBase}/1`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);

    responderExistencias(undefined);
    await refrescar();

    confirmar.mockRestore();
    expect(component.existencias()).toEqual(existencias);
  });

  it('debe mostrar el mensaje del backend cuando la eliminación responde 409', async () => {
    fixture.detectChanges();
    responderCatalogo();
    responderExistencias(undefined);
    await refrescar();

    const confirmar = vi.spyOn(window, 'confirm').mockReturnValue(true);
    component.eliminar(existencias[0]);

    http.expectOne(`${existenciasBase}/1`).flush(
      { message: 'No se puede eliminar la existencia del producto 7 porque tiene 12.500 disponibles.' },
      { status: 409, statusText: 'Conflict' },
    );
    await refrescar();

    confirmar.mockRestore();
    expect(component.error()).toBe(
      'No se puede eliminar la existencia del producto 7 porque tiene 12.500 disponibles.',
    );
  });

  it('no debe eliminar si el usuario cancela la confirmación', async () => {
    fixture.detectChanges();
    responderCatalogo();
    responderExistencias(undefined);
    await refrescar();

    const confirmar = vi.spyOn(window, 'confirm').mockReturnValue(false);
    component.eliminar(existencias[0]);
    await refrescar();

    confirmar.mockRestore();
    http.expectNone(`${existenciasBase}/1`);
  });
});
