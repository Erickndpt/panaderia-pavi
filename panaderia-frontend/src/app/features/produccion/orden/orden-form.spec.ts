import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { Producto } from '../../administracion/producto/producto.model';
import { ExistenciaResponse } from '../../inventario/existencia/existencia.model';
import { Insumo } from '../../inventario/insumo/insumo.model';
import { OrdenProduccionResponse } from './orden-produccion.model';
import { OrdenForm } from './orden-form';

describe('OrdenForm', () => {
  const ordenesBase = 'http://localhost:8081/api/v1/ordenes-produccion';
  const productosBase = 'http://localhost:8081/api/v1/productos';
  const insumosBase = 'http://localhost:8081/api/v1/insumos';
  const existenciasBase = 'http://localhost:8081/api/v1/existencias';

  const productos: Producto[] = [
    { id: 1, nombre: 'Pan francés', precio: 1.5, categoria: { id: 3, nombre: 'Panes' } },
    { id: 2, nombre: 'Torta de chocolate', precio: 8, categoria: { id: 4, nombre: 'Pasteles' } },
  ];

  const insumos: Insumo[] = [
    { id: 5, nombre: 'Harina', unidadMedida: 'KILOGRAMO', stockActual: 100, stockMinimo: 10 },
    { id: 6, nombre: 'Levadura', unidadMedida: 'KILOGRAMO', stockActual: 5, stockMinimo: 1 },
  ];

  const existencias: ExistenciaResponse[] = [
    { id: 1, producto: { id: 1, nombre: 'Pan francés' }, cantidad: 20 },
  ];

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
        insumoId: 5,
        nombreInsumo: 'Harina',
        unidadMedida: 'KILOGRAMO',
        cantidadConsumida: 2.5,
      },
    ],
  };

  let http: HttpTestingController;

  async function configurar(): Promise<ComponentFixture<OrdenForm>> {
    await TestBed.configureTestingModule({
      imports: [OrdenForm],
      providers: [
        provideRouter([{ path: 'produccion/ordenes', children: [] }]),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(OrdenForm);
    fixture.detectChanges();

    http = TestBed.inject(HttpTestingController);
    http.expectOne(productosBase).flush(productos);
    http.expectOne(insumosBase).flush(insumos);
    http.expectOne(existenciasBase).flush(existencias);
    await fixture.whenStable();
    fixture.detectChanges();

    return fixture;
  }

  function completarValida(component: OrdenForm): void {
    component.alSeleccionarProducto(productos[0]);
    component.form.controls.cantidadProducida.setValue(12);
    component.alSeleccionarInsumo(0, insumos[0]);
    component.detalles.at(0).get('cantidadConsumida')?.setValue(2.5);
  }

  afterEach(() => {
    http.verify();
  });

  it('should create', async () => {
    const fixture = await configurar();
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('debe iniciar con una línea y sin consumos derivados', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    expect(component.detalles.length).toBe(1);
    expect(component.tiposInsumos()).toBe(0);
    expect(component.lineasValidas()).toBe(0);
    expect(component.puedeQuitarLinea).toBe(false);
  });

  it('debe agregar y quitar líneas respetando el mínimo de una', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    component.agregarLinea();
    fixture.detectChanges();
    expect(component.detalles.length).toBe(2);
    expect(component.puedeQuitarLinea).toBe(true);

    component.quitarLinea(1);
    fixture.detectChanges();
    expect(component.detalles.length).toBe(1);

    component.quitarLinea(0);
    expect(component.detalles.length).toBe(1);
  });

  it('debe resolver la existencia del producto desde el catálogo', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    expect(component.existenciaDe(1)).toBe(20);
    expect(component.existenciaDe(2)).toBeNull();
  });

  it('no debe enviar sin seleccionar un producto de la lista', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    component.form.controls.productoTexto.setValue('Pan francés');
    component.guardar();
    await fixture.whenStable();

    http.expectNone(ordenesBase);
    expect(component.error()).toBe('Seleccione un producto de la lista.');
  });

  it('no debe enviar si un insumo no fue seleccionado de la lista', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    component.alSeleccionarProducto(productos[0]);
    component.form.controls.cantidadProducida.setValue(12);
    component.detalles.at(0).get('insumoTexto')?.setValue('Harina');
    component.detalles.at(0).get('cantidadConsumida')?.setValue(2.5);
    component.guardar();
    await fixture.whenStable();

    http.expectNone(ordenesBase);
    expect(component.error()).toBe(
      'Seleccione un insumo válido de la lista en cada línea del detalle.',
    );
  });

  it('debe rechazar una cantidad producida que no sea mayor que cero', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    completarValida(component);
    component.form.controls.cantidadProducida.setValue(0);
    component.guardar();
    await fixture.whenStable();

    http.expectNone(ordenesBase);
    expect(component.form.controls.cantidadProducida.hasError('positivo')).toBe(true);
  });

  it('debe rechazar una cantidad consumida que no sea mayor que cero', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    completarValida(component);
    component.detalles.at(0).get('cantidadConsumida')?.setValue(0);
    component.guardar();
    await fixture.whenStable();

    http.expectNone(ordenesBase);
    expect(component.detalles.at(0).get('cantidadConsumida')?.hasError('positivo')).toBe(true);
  });

  it('debe detectar un insumo repetido y no enviar', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    component.alSeleccionarProducto(productos[0]);
    component.form.controls.cantidadProducida.setValue(12);
    component.alSeleccionarInsumo(0, insumos[0]);
    component.detalles.at(0).get('cantidadConsumida')?.setValue(2.5);
    component.agregarLinea();
    component.alSeleccionarInsumo(1, insumos[0]);
    component.detalles.at(1).get('cantidadConsumida')?.setValue(1);

    expect(component.detalles.hasError('insumoDuplicado')).toBe(true);

    component.guardar();
    await fixture.whenStable();

    http.expectNone(ordenesBase);
    expect(component.error()).toBe(
      'Cada insumo puede aparecer una sola vez en el detalle de consumo.',
    );
  });

  it('debe calcular los valores visuales sin sumar unidades incompatibles', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    component.alSeleccionarProducto(productos[0]);
    component.form.controls.cantidadProducida.setValue(100);
    component.alSeleccionarInsumo(0, insumos[0]);
    component.detalles.at(0).get('cantidadConsumida')?.setValue(20);
    component.agregarLinea();
    component.alSeleccionarInsumo(1, insumos[1]);
    component.detalles.at(1).get('cantidadConsumida')?.setValue(1);

    expect(component.tiposInsumos()).toBe(2);
    expect(component.lineasValidas()).toBe(2);
    expect(component.lineas()[0].consumoPorUnidad).toBeCloseTo(0.2);
    expect(component.lineas()[0].stockPosterior).toBe(80);
    expect(component.lineas()[1].stockPosterior).toBe(4);
    expect(component.resumenPorUnidad()).toEqual([{ unidad: 'KILOGRAMO', total: 21 }]);
  });

  it('no debe enviar datos derivados en el payload', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    completarValida(component);
    const confirmar = vi.spyOn(window, 'confirm').mockReturnValue(true);
    component.guardar();
    await fixture.whenStable();

    const req = http.expectOne(ordenesBase);
    expect(req.request.method).toBe('POST');
    expect(Object.keys(req.request.body)).toEqual(['productoId', 'cantidadProducida', 'insumos']);
    expect(Object.keys(req.request.body.insumos[0])).toEqual(['insumoId', 'cantidadConsumida']);
    expect(req.request.body).toEqual({
      productoId: 1,
      cantidadProducida: 12,
      insumos: [{ insumoId: 5, cantidadConsumida: 2.5 }],
    });

    req.flush(respuesta);
    await fixture.whenStable();
    confirmar.mockRestore();
  });

  it('no debe enviar si el usuario cancela la confirmación', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    completarValida(component);
    const confirmar = vi.spyOn(window, 'confirm').mockReturnValue(false);
    component.guardar();
    await fixture.whenStable();

    confirmar.mockRestore();
    http.expectNone(ordenesBase);
  });

  it('debe confirmar mostrando el stock estimado y navegar tras el éxito', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    completarValida(component);
    let mensajeConfirmacion = '';
    const confirmar = vi.spyOn(window, 'confirm').mockImplementation((mensaje) => {
      mensajeConfirmacion = String(mensaje);
      return true;
    });

    component.guardar();
    await fixture.whenStable();

    expect(mensajeConfirmacion).toContain('Pan francés');
    expect(mensajeConfirmacion).toContain('stock posterior estimado: 97.5');

    const req = http.expectOne(ordenesBase);
    req.flush(respuesta);
    await fixture.whenStable();

    confirmar.mockRestore();
    expect(TestBed.inject(Router).url).toBe('/produccion/ordenes');
  });

  it('no debe duplicar el envío mientras la orden se registra', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    completarValida(component);
    const confirmar = vi.spyOn(window, 'confirm').mockReturnValue(true);

    component.guardar();
    component.guardar();

    const req = http.expectOne(ordenesBase);
    req.flush(respuesta);
    await fixture.whenStable();

    confirmar.mockRestore();
    http.verify();
  });

  it('debe mostrar el mensaje real de stock insuficiente ante un 409', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    completarValida(component);
    const confirmar = vi.spyOn(window, 'confirm').mockReturnValue(true);
    component.guardar();
    await fixture.whenStable();

    http
      .expectOne(ordenesBase)
      .flush(
        {
          message:
            'Stock insuficiente para el insumo 5: se solicitó 200 y hay 20 disponibles',
        },
        { status: 409, statusText: 'Conflict' },
      );
    await fixture.whenStable();

    confirmar.mockRestore();
    expect(component.error()).toBe(
      'Stock insuficiente para el insumo 5: se solicitó 200 y hay 20 disponibles',
    );
  });

  it('debe mostrar el mensaje del backend ante un 400 por insumo repetido', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    completarValida(component);
    const confirmar = vi.spyOn(window, 'confirm').mockReturnValue(true);
    component.guardar();
    await fixture.whenStable();

    http
      .expectOne(ordenesBase)
      .flush(
        { message: 'El insumo 5 aparece más de una vez en la orden' },
        { status: 400, statusText: 'Bad Request' },
      );
    await fixture.whenStable();

    confirmar.mockRestore();
    expect(component.error()).toBe('El insumo 5 aparece más de una vez en la orden');
  });

  it('debe mostrar el mensaje del backend ante un 404 de producto inexistente', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    completarValida(component);
    const confirmar = vi.spyOn(window, 'confirm').mockReturnValue(true);
    component.guardar();
    await fixture.whenStable();

    http
      .expectOne(ordenesBase)
      .flush(
        { message: 'Producto con id 1 no encontrado' },
        { status: 404, statusText: 'Not Found' },
      );
    await fixture.whenStable();

    confirmar.mockRestore();
    expect(component.error()).toBe('Producto con id 1 no encontrado');
  });

  it('debe informar cuando falla la carga de catálogos', async () => {
    await TestBed.configureTestingModule({
      imports: [OrdenForm],
      providers: [
        provideRouter([{ path: 'produccion/ordenes', children: [] }]),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(OrdenForm);
    fixture.detectChanges();

    http = TestBed.inject(HttpTestingController);
    http
      .expectOne(productosBase)
      .flush({ message: 'fallo' }, { status: 500, statusText: 'Server Error' });
    http
      .expectOne(insumosBase)
      .flush({ message: 'fallo' }, { status: 500, statusText: 'Server Error' });
    http.expectOne(existenciasBase).flush([]);
    await fixture.whenStable();

    expect(fixture.componentInstance.catalogosError()).toContain('catálogo');
    expect(fixture.componentInstance.catalogosLoading()).toBe(false);
  });
});
