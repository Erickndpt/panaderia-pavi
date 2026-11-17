import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { Producto } from './producto.model';
import { ProductoForm } from './producto-form';

describe('ProductoForm', () => {
  const productosBase = 'http://localhost:8081/api/v1/productos';
  const categoriasBase = 'http://localhost:8081/api/v1/categorias';

  const categorias = [
    { id: 1, nombre: 'Panes', descripcion: null },
    { id: 2, nombre: 'Bebidas', descripcion: null },
  ];

  const producto: Producto = {
    id: 3,
    nombre: 'Croissant',
    precio: 2.5,
    categoria: { id: 1, nombre: 'Panes' },
  };

  let http: HttpTestingController;

  async function configurar(id?: string): Promise<ComponentFixture<ProductoForm>> {
    await TestBed.configureTestingModule({
      imports: [ProductoForm],
      providers: [
        provideRouter([{ path: 'administracion/productos', children: [] }]),
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { paramMap: convertToParamMap(id === undefined ? {} : { id }) },
          },
        },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(ProductoForm);
    fixture.detectChanges();

    http = TestBed.inject(HttpTestingController);
    http.expectOne(categoriasBase).flush(categorias);

    if (id !== undefined && Number.isInteger(Number(id)) && Number(id) > 0) {
      http.expectOne(`${productosBase}/${id}`).flush(producto);
    }

    await fixture.whenStable();
    fixture.detectChanges();

    return fixture;
  }

  afterEach(() => {
    http.verify();
  });

  it('should create', async () => {
    const fixture = await configurar();
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('debe iniciar el formulario vacío al crear', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    expect(component.esEdicion).toBe(false);
    expect(component.form.getRawValue()).toEqual({
      nombre: '',
      precio: null,
      categoriaTexto: '',
    });
  });

  it('debe enviar el POST con el categoriaId resuelto al guardar', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    component.form.setValue({ nombre: 'Concha', precio: 2.5, categoriaTexto: 'Panes' });
    component.guardar();
    await fixture.whenStable();

    const req = http.expectOne(productosBase);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ nombre: 'Concha', precio: 2.5, categoriaId: 1 });

    req.flush({ ...producto, nombre: 'Concha' });
    await fixture.whenStable();
    expect(TestBed.inject(Router).url).toBe('/administracion/productos');
  });

  it('debe rechazar una categoría que no existe en el catálogo', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    component.form.setValue({ nombre: 'Concha', precio: 2.5, categoriaTexto: 'Inexistente' });
    component.guardar();
    await fixture.whenStable();

    http.expectNone(productosBase);
    expect(component.error()).toBe('Seleccione una categoría válida de la lista.');
  });

  it('debe rechazar un precio con más de dos decimales', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    component.form.setValue({ nombre: 'Concha', precio: 2.345, categoriaTexto: 'Panes' });
    component.guardar();
    await fixture.whenStable();

    http.expectNone(productosBase);
    expect(component.form.controls.precio.hasError('decimales')).toBe(true);
  });

  it('debe cargar el producto y enviar el PUT al editar', async () => {
    const fixture = await configurar('3');
    const component = fixture.componentInstance;

    expect(component.esEdicion).toBe(true);
    expect(component.form.getRawValue()).toEqual({
      nombre: 'Croissant',
      precio: 2.5,
      categoriaTexto: 'Panes',
    });

    component.form.patchValue({ precio: 3 });
    component.guardar();
    await fixture.whenStable();

    const req = http.expectOne(`${productosBase}/3`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ nombre: 'Croissant', precio: 3, categoriaId: 1 });

    req.flush({ ...producto, precio: 3 });
    await fixture.whenStable();
    expect(TestBed.inject(Router).url).toBe('/administracion/productos');
  });

  it('debe mostrar el mensaje del backend cuando la categoría ya no existe', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    component.form.setValue({ nombre: 'Concha', precio: 2.5, categoriaTexto: 'Panes' });
    component.guardar();
    await fixture.whenStable();

    http
      .expectOne(productosBase)
      .flush(
        { message: 'Categoría con id 1 no encontrada' },
        { status: 404, statusText: 'Not Found' },
      );
    await fixture.whenStable();

    expect(component.error()).toBe('Categoría con id 1 no encontrada');
  });

  it('debe rechazar un identificador inválido sin llamar al backend', async () => {
    const fixture = await configurar('abc');
    await fixture.whenStable();

    expect(fixture.componentInstance.error()).toBe('El identificador del producto no es válido.');
  });
});
