import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { ExistenciaForm } from './existencia-form';

describe('ExistenciaForm', () => {
  const existenciasBase = 'http://localhost:8081/api/v1/existencias';
  const productosBase = 'http://localhost:8081/api/v1/productos';

  const productos = [
    { id: 7, nombre: 'Pan de crawit' },
    { id: 8, nombre: 'Croissant' },
  ];

  const existencia = { id: 3, producto: { id: 7, nombre: 'Pan de crawit' }, cantidad: 12.5 };

  let http: HttpTestingController;

  async function configurar(id?: string): Promise<ComponentFixture<ExistenciaForm>> {
    await TestBed.configureTestingModule({
      imports: [ExistenciaForm],
      providers: [
        provideRouter([{ path: 'inventario/existencias', children: [] }]),
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

    const fixture = TestBed.createComponent(ExistenciaForm);
    fixture.detectChanges();

    http = TestBed.inject(HttpTestingController);

    return fixture;
  }

  function pedirProductos(): ReturnType<HttpTestingController['expectOne']> {
    return http.expectOne((peticion) => peticion.url === productosBase);
  }

  function pedirExistencia(id: string): ReturnType<HttpTestingController['expectOne']> {
    return http.expectOne(`${existenciasBase}/${id}`);
  }

  async function refrescar(fixture: ComponentFixture<ExistenciaForm>): Promise<void> {
    await fixture.whenStable();
    fixture.detectChanges();
  }

  afterEach(() => {
    http.verify();
  });

  it('should create', async () => {
    const fixture = await configurar();

    pedirProductos().flush(productos);
    await refrescar(fixture);

    expect(fixture.componentInstance).toBeTruthy();
  });

  it('debe crear sin cargar datos previos y pedir el catálogo de productos', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    expect(component.esEdicion).toBe(false);
    expect(component.form.getRawValue()).toEqual({ productoId: null, cantidad: null });

    const req = pedirProductos();
    expect(req.request.method).toBe('GET');
    req.flush(productos);
    await refrescar(fixture);
  });

  it('debe enviar el POST con productoId numérico al guardar', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    pedirProductos().flush(productos);
    await refrescar(fixture);

    component.form.setValue({ productoId: 7, cantidad: 12.5 });
    component.guardar();
    await refrescar(fixture);

    const req = http.expectOne(existenciasBase);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ productoId: 7, cantidad: 12.5 });
    expect(typeof req.request.body.productoId).toBe('number');

    req.flush(existencia);
    await refrescar(fixture);
    expect(TestBed.inject(Router).url).toBe('/inventario/existencias');
  });

  it('debe bloquear el envío si no se selecciona producto', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    pedirProductos().flush(productos);
    await refrescar(fixture);

    component.form.setValue({ productoId: null, cantidad: 5 });
    component.guardar();
    await refrescar(fixture);

    http.expectNone(existenciasBase);
    expect(component.form.controls.productoId.hasError('required')).toBe(true);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Debe seleccionar un producto');
  });

  it('debe rechazar una cantidad con más de tres decimales', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    pedirProductos().flush(productos);
    await refrescar(fixture);

    component.form.setValue({ productoId: 7, cantidad: 1.2345 });
    component.guardar();
    await refrescar(fixture);

    http.expectNone(existenciasBase);
    expect(component.form.controls.cantidad.hasError('decimales')).toBe(true);
  });

  it('debe rechazar una cantidad negativa', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    pedirProductos().flush(productos);
    await refrescar(fixture);

    component.form.setValue({ productoId: 7, cantidad: -1 });
    component.guardar();
    await refrescar(fixture);

    http.expectNone(existenciasBase);
    expect(component.form.controls.cantidad.hasError('min')).toBe(true);
  });

  it('debe precargar la existencia y preseleccionar el producto al editar', async () => {
    const fixture = await configurar('3');
    const component = fixture.componentInstance;

    pedirProductos().flush(productos);
    pedirExistencia('3').flush(existencia);
    await refrescar(fixture);

    expect(component.esEdicion).toBe(true);
    expect(component.form.getRawValue()).toEqual({ productoId: 7, cantidad: 12.5 });
    expect(typeof component.form.controls.productoId.value).toBe('number');
  });

  it('debe preseleccionar el producto aunque la existencia llegue antes que el catálogo', async () => {
    const fixture = await configurar('3');
    const component = fixture.componentInstance;

    pedirExistencia('3').flush(existencia);
    await refrescar(fixture);

    expect(component.form.controls.productoId.value).toBe(7);

    pedirProductos().flush(productos);
    await refrescar(fixture);

    expect(component.form.controls.productoId.value).toBe(7);
  });

  it('debe preseleccionar el producto aunque el catálogo llegue antes que la existencia', async () => {
    const fixture = await configurar('3');
    const component = fixture.componentInstance;

    pedirProductos().flush(productos);
    await refrescar(fixture);

    pedirExistencia('3').flush(existencia);
    await refrescar(fixture);

    expect(component.form.controls.productoId.value).toBe(7);
    expect(typeof component.form.controls.productoId.value).toBe('number');
  });

  it('debe enviar el PUT al editar', async () => {
    const fixture = await configurar('3');
    const component = fixture.componentInstance;

    pedirProductos().flush(productos);
    pedirExistencia('3').flush(existencia);
    await refrescar(fixture);

    component.form.patchValue({ cantidad: 0 });
    component.guardar();
    await refrescar(fixture);

    const req = http.expectOne(`${existenciasBase}/3`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ productoId: 7, cantidad: 0 });

    req.flush({ ...existencia, cantidad: 0 });
    await refrescar(fixture);
    expect(TestBed.inject(Router).url).toBe('/inventario/existencias');
  });

  it('debe mostrar el mensaje específico y la acción de recargar cuando el backend responde 404', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    pedirProductos().flush(productos);
    await refrescar(fixture);

    component.form.setValue({ productoId: 7, cantidad: 3 });
    component.guardar();
    await refrescar(fixture);

    http
      .expectOne(existenciasBase)
      .flush({ message: 'Producto con id 7 no encontrado' }, { status: 404, statusText: 'Not Found' });
    await refrescar(fixture);

    expect(component.productoAusente()).toBe(true);
    expect(component.error()).toBe(
      'El producto seleccionado ya no existe. Recargue los productos y seleccione otro',
    );

    const botones = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('button'),
    ) as HTMLButtonElement[];
    expect(botones.some((boton) => boton.textContent?.includes('Recargar productos'))).toBe(true);

    component.recargarProductos();
    const req = pedirProductos();
    expect(req.request.method).toBe('GET');
    req.flush([{ id: 8, nombre: 'Croissant' }]);
    await refrescar(fixture);
  });

  it('debe mostrar el mensaje del backend cuando la creación responde 409', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    pedirProductos().flush(productos);
    await refrescar(fixture);

    component.form.setValue({ productoId: 7, cantidad: 3 });
    component.guardar();
    await refrescar(fixture);

    http.expectOne(existenciasBase).flush(
      { message: 'El producto 7 ya tiene una existencia registrada' },
      { status: 409, statusText: 'Conflict' },
    );
    await refrescar(fixture);

    expect(component.productoAusente()).toBe(false);
    expect(component.error()).toBe('El producto 7 ya tiene una existencia registrada');
  });

  it('debe rechazar un identificador inválido sin llamar al backend', async () => {
    const fixture = await configurar('abc');

    pedirProductos().flush(productos);
    await refrescar(fixture);

    expect(fixture.componentInstance.error()).toBe('El identificador de la existencia no es válido.');
  });
});
