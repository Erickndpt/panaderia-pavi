import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { Producto } from '../../administracion/producto/producto.model';
import { Cliente } from '../cliente/cliente.model';
import { VentaResponse } from './venta.model';
import { VentaForm } from './venta-form';

describe('VentaForm', () => {
  const ventasBase = 'http://localhost:8081/api/v1/ventas';
  const clientesBase = 'http://localhost:8081/api/v1/clientes';
  const productosBase = 'http://localhost:8081/api/v1/productos';
  const existenciasBase = 'http://localhost:8081/api/v1/existencias';

  const clientes: Cliente[] = [
    {
      id: 1,
      tipoDocumento: 'RUC',
      numeroDocumento: '20123456789',
      nombreRazonSocial: 'Panadería del Sur SAC',
      telefono: null,
      email: null,
      direccion: null,
      pais: null,
      activo: true,
    },
    {
      id: 2,
      tipoDocumento: 'DNI',
      numeroDocumento: '12345678',
      nombreRazonSocial: 'Cliente Inactivo',
      telefono: null,
      email: null,
      direccion: null,
      pais: null,
      activo: false,
    },
  ];

  const productos: Producto[] = [
    { id: 5, nombre: 'Pan francés', precio: 2.5, categoria: { id: 1, nombre: 'Panadería' } },
    { id: 6, nombre: 'Torta', precio: 20, categoria: { id: 2, nombre: 'Pastelería' } },
  ];

  const respuesta: VentaResponse = {
    id: 10,
    fecha: '2026-09-14T11:30:00',
    tipo: 'LOCAL',
    paisDestino: null,
    cliente: {
      id: 1,
      tipoDocumento: 'RUC',
      numeroDocumento: '20123456789',
      nombreRazonSocial: 'Panadería del Sur SAC',
    },
    estado: 'REGISTRADA',
    total: 25,
    vendedor: { id: 'usuario-1', username: 'ana', nombre: 'Ana Perez' },
    detalles: [
      {
        id: 100,
        productoId: 5,
        nombreProducto: 'Pan francés',
        precioUnitario: 2.5,
        cantidad: 10,
        subtotal: 25,
      },
    ],
  };

  let http: HttpTestingController;

  function urlExistenciaProducto(productoId: number): string {
    return `${existenciasBase}/producto/${productoId}`;
  }

  function flushExistencia(productoId: number, cantidad: number, id = productoId): void {
    const producto = productos.find((item) => item.id === productoId);
    http.expectOne(urlExistenciaProducto(productoId)).flush({
      id,
      producto: { id: productoId, nombre: producto?.nombre ?? `Producto ${productoId}` },
      cantidad,
    });
  }

  async function configurar(): Promise<ComponentFixture<VentaForm>> {
    await TestBed.configureTestingModule({
      imports: [VentaForm],
      providers: [
        provideRouter([{ path: 'ventas/reporte', children: [] }]),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(VentaForm);
    fixture.detectChanges();

    http = TestBed.inject(HttpTestingController);
    http.expectOne(clientesBase).flush(clientes);
    http.expectOne(productosBase).flush(productos);
    await fixture.whenStable();
    fixture.detectChanges();

    return fixture;
  }

  function completarValida(component: VentaForm, existencia = 100): void {
    component.alSeleccionarCliente(clientes[0]);
    component.alSeleccionarProducto(0, productos[0]);
    flushExistencia(5, existencia);
    component.detalles.at(0).get('cantidad')?.setValue(10);
  }

  function textoPlantilla(fixture: ComponentFixture<VentaForm>): string {
    return (fixture.nativeElement as HTMLElement).textContent ?? '';
  }

  function botonRegistrar(fixture: ComponentFixture<VentaForm>): HTMLButtonElement {
    return (fixture.nativeElement as HTMLElement).querySelector(
      'button[type="submit"]',
    ) as HTMLButtonElement;
  }

  afterEach(() => {
    http.verify();
  });

  it('should create', async () => {
    const fixture = await configurar();
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('debe iniciar con una línea, tipo LOCAL y sin total', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    expect(component.detalles.length).toBe(1);
    expect(component.form.controls.tipo.value).toBe('LOCAL');
    expect(component.totalEstimado()).toBe(0);
    expect(component.puedeQuitarLinea).toBe(false);
  });

  it('debe ofrecer únicamente clientes activos', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    expect(component.clientesDisponibles()).toEqual([clientes[0]]);
    expect(component.clientesFiltrados().map((cliente) => cliente.id)).toEqual([1]);
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

  it('no debe seleccionar un cliente inactivo', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    component.form.controls.clienteTexto.setValue('Cliente Inactivo');
    component.guardar();
    await fixture.whenStable();

    http.expectNone(ventasBase);
    expect(component.error()).toBe('Seleccione un cliente activo de la lista.');
  });

  it('debe validar el producto seleccionado de la lista', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    component.alSeleccionarCliente(clientes[0]);
    component.form.controls.detalles.at(0).get('productoTexto')?.setValue('Pan francés');
    component.form.controls.detalles.at(0).get('cantidad')?.setValue(10);
    component.guardar();
    await fixture.whenStable();

    http.expectNone(ventasBase);
    expect(component.error()).toBe(
      'Seleccione un producto válido de la lista en cada línea del detalle.',
    );
  });

  it('debe rechazar cantidades no enteras o no positivas', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;
    const cantidad = component.detalles.at(0).get('cantidad')!;

    cantidad.setValue(0);
    expect(cantidad.hasError('positivo')).toBe(true);

    cantidad.setValue(2.5);
    expect(cantidad.hasError('entero')).toBe(true);

    cantidad.setValue(3);
    expect(cantidad.valid).toBe(true);
  });

  it('debe limpiar el país al cambiar a LOCAL', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    component.actualizarTipo('EXPORTACION');
    component.form.controls.paisDestino.setValue('Chile');
    component.actualizarTipo('LOCAL');

    expect(component.form.controls.paisDestino.value).toBe('');
    expect(component.construirRequest().paisDestino).toBeNull();
  });

  it('debe exigir país de destino para EXPORTACION', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    component.actualizarTipo('EXPORTACION');
    completarValida(component);
    const confirmar = vi.spyOn(window, 'confirm').mockReturnValue(true);

    component.guardar();
    await fixture.whenStable();

    confirmar.mockRestore();
    http.expectNone(ventasBase);
    expect(component.error()).toBe(
      'El país de destino es obligatorio para una venta de exportación.',
    );
  });

  it('debe calcular subtotales y total, y advertir sobre el stock', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    completarValida(component);
    expect(component.lineas()[0].subtotal).toBe(25);
    expect(component.totalEstimado()).toBe(25);
    expect(component.lineas()[0].excedeStock).toBe(false);
    expect(component.lineas()[0].existencia).toBe(100);

    component.alSeleccionarProducto(0, productos[1]);
    flushExistencia(6, 3);
    component.detalles.at(0).get('cantidad')?.setValue(5);
    expect(component.lineas()[0].excedeStock).toBe(true);
    expect(component.lineas()[0].existencia).toBe(3);

    component.detalles.at(0).get('cantidad')?.setValue(2);
    expect(component.lineas()[0].excedeStock).toBe(false);
  });

  it('no debe enviar si el usuario cancela la confirmación', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    completarValida(component);
    const confirmar = vi.spyOn(window, 'confirm').mockReturnValue(false);
    component.guardar();
    await fixture.whenStable();

    confirmar.mockRestore();
    http.expectNone(ventasBase);
  });

  it('debe enviar el POST exacto sin total, precio ni subtotal y navegar', async () => {
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

    const req = http.expectOne(ventasBase);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      clienteId: 1,
      tipo: 'LOCAL',
      paisDestino: null,
      detalles: [{ productoId: 5, cantidad: 10 }],
    });
    const cuerpo = req.request.body as Record<string, unknown>;
    expect(Object.keys(cuerpo)).not.toContain('total');
    expect(Object.keys(cuerpo)).not.toContain('precioUnitario');
    expect(Object.keys(cuerpo)).not.toContain('subtotal');
    expect(Object.keys((cuerpo['detalles'] as Record<string, unknown>[])[0])).toEqual([
      'productoId',
      'cantidad',
    ]);
    expect(mensajeConfirmacion).toContain('Panadería del Sur SAC');
    expect(mensajeConfirmacion).toContain('S/ 25.00');

    req.flush(respuesta);
    await fixture.whenStable();

    confirmar.mockRestore();
    expect(TestBed.inject(Router).url).toBe('/ventas/reporte');
  });

  it('debe enviar el país de destino cuando es EXPORTACION', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    component.actualizarTipo('EXPORTACION');
    component.form.controls.paisDestino.setValue('Chile');
    completarValida(component);
    const confirmar = vi.spyOn(window, 'confirm').mockReturnValue(true);

    component.guardar();
    await fixture.whenStable();

    const req = http.expectOne(ventasBase);
    expect(req.request.body).toEqual({
      clienteId: 1,
      tipo: 'EXPORTACION',
      paisDestino: 'Chile',
      detalles: [{ productoId: 5, cantidad: 10 }],
    });
    req.flush({ ...respuesta, tipo: 'EXPORTACION', paisDestino: 'Chile' });
    await fixture.whenStable();

    confirmar.mockRestore();
  });

  it('no debe duplicar el envío mientras la venta se está registrando', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    completarValida(component);
    const confirmar = vi.spyOn(window, 'confirm').mockReturnValue(true);

    component.guardar();
    component.guardar();

    const req = http.expectOne(ventasBase);
    req.flush(respuesta);
    await fixture.whenStable();

    confirmar.mockRestore();
    http.verify();
  });

  it('debe mostrar el mensaje del backend ante un 409', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    completarValida(component);
    const confirmar = vi.spyOn(window, 'confirm').mockReturnValue(true);
    component.guardar();
    await fixture.whenStable();

    http
      .expectOne(ventasBase)
      .flush(
        { message: 'Stock insuficiente para el producto 5: se solicitó 10 y hay 3 disponibles' },
        { status: 409, statusText: 'Conflict' },
      );
    await fixture.whenStable();

    confirmar.mockRestore();
    expect(component.error()).toContain('Stock insuficiente');
  });

  it('debe mostrar el mensaje del backend ante un 404', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    completarValida(component);
    const confirmar = vi.spyOn(window, 'confirm').mockReturnValue(true);
    component.guardar();
    await fixture.whenStable();

    http
      .expectOne(ventasBase)
      .flush(
        { message: 'No existe existencia para el producto 5' },
        { status: 404, statusText: 'Not Found' },
      );
    await fixture.whenStable();

    confirmar.mockRestore();
    expect(component.error()).toBe('No existe existencia para el producto 5');
  });

  it('debe mostrar el mensaje del backend ante un 400', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    completarValida(component);
    const confirmar = vi.spyOn(window, 'confirm').mockReturnValue(true);
    component.guardar();
    await fixture.whenStable();

    http
      .expectOne(ventasBase)
      .flush(
        { message: 'Una venta EXPORTACION exige un país de destino' },
        { status: 400, statusText: 'Bad Request' },
      );
    await fixture.whenStable();

    confirmar.mockRestore();
    expect(component.error()).toBe('Una venta EXPORTACION exige un país de destino');
  });

  it('debe informar cuando falla la carga de catálogos', async () => {
    await TestBed.configureTestingModule({
      imports: [VentaForm],
      providers: [
        provideRouter([{ path: 'ventas/reporte', children: [] }]),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(VentaForm);
    fixture.detectChanges();

    http = TestBed.inject(HttpTestingController);
    http
      .expectOne(clientesBase)
      .flush({ message: 'fallo' }, { status: 500, statusText: 'Server Error' });
    http
      .expectOne(productosBase)
      .flush({ message: 'fallo' }, { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();

    expect(fixture.componentInstance.catalogosError()).toContain('No se pudo cargar');
    expect(fixture.componentInstance.catalogosLoading()).toBe(false);
  });

  it('debe consultar la existencia del producto seleccionado y mostrarla', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    component.alSeleccionarProducto(0, productos[0]);

    const req = http.expectOne(urlExistenciaProducto(5));
    expect(req.request.method).toBe('GET');
    req.flush({ id: 21, producto: { id: 5, nombre: 'Pan francés' }, cantidad: 110 });
    fixture.detectChanges();

    expect(component.lineas()[0].estadoExistencia).toBe('disponible');
    expect(component.lineas()[0].existencia).toBe(110);
    expect(textoPlantilla(fixture)).toContain('Existencia 110');
  });

  it('debe mostrar "Consultando existencia..." mientras responde el backend', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    component.alSeleccionarProducto(0, productos[0]);
    fixture.detectChanges();

    expect(component.lineas()[0].estadoExistencia).toBe('cargando');
    expect(textoPlantilla(fixture)).toContain('Consultando existencia...');
    expect(component.puedeRegistrar()).toBe(false);

    flushExistencia(5, 110);
    fixture.detectChanges();
    expect(textoPlantilla(fixture)).toContain('Existencia 110');
  });

  it('con existencia 110 y cantidad 22 no debe advertir ni bloquear el registro', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    completarValida(component, 110);
    component.detalles.at(0).get('cantidad')?.setValue(22);
    fixture.detectChanges();

    expect(component.lineas()[0].excedeStock).toBe(false);
    expect(component.bloqueoStock()).toBeNull();
    expect(component.puedeRegistrar()).toBe(true);
    expect(textoPlantilla(fixture)).not.toContain('La cantidad solicitada supera');
  });

  it('con existencia 110 y cantidad 110 no debe advertir ni bloquear el registro', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    completarValida(component, 110);
    component.detalles.at(0).get('cantidad')?.setValue(110);
    fixture.detectChanges();

    expect(component.lineas()[0].excedeStock).toBe(false);
    expect(component.puedeRegistrar()).toBe(true);
    expect(textoPlantilla(fixture)).not.toContain('La cantidad solicitada supera');
  });

  it('con existencia 110 y cantidad 120 debe advertir, bloquear el botón y impedir el POST', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    completarValida(component, 110);
    component.detalles.at(0).get('cantidad')?.setValue(120);
    fixture.detectChanges();

    expect(component.lineas()[0].excedeStock).toBe(true);
    expect(textoPlantilla(fixture)).toContain(
      'La cantidad solicitada supera la existencia disponible (110).',
    );
    expect(botonRegistrar(fixture).disabled).toBe(true);

    const confirmar = vi.spyOn(window, 'confirm').mockReturnValue(true);
    component.guardar();
    await fixture.whenStable();
    confirmar.mockRestore();

    http.expectNone(ventasBase);
    expect(component.error()).toBe('La cantidad solicitada supera la existencia disponible (110).');
  });

  it('cuando la consulta de existencia falla no debe mostrar Existencia 0 ni permitir registrar', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    component.alSeleccionarCliente(clientes[0]);
    component.alSeleccionarProducto(0, productos[0]);
    http
      .expectOne(urlExistenciaProducto(5))
      .flush({ message: 'fallo' }, { status: 500, statusText: 'Server Error' });
    component.detalles.at(0).get('cantidad')?.setValue(1);
    fixture.detectChanges();

    expect(component.lineas()[0].estadoExistencia).toBe('error');
    expect(component.lineas()[0].existencia).toBeNull();
    expect(component.lineas()[0].excedeStock).toBe(false);
    expect(component.puedeRegistrar()).toBe(false);

    const texto = textoPlantilla(fixture);
    expect(texto).toContain('Existencia no disponible');
    expect(texto).not.toContain('Existencia 0');
    expect(texto).not.toContain('La cantidad solicitada supera');
    expect(texto).toContain('Reintentar carga');
    expect(botonRegistrar(fixture).disabled).toBe(true);

    const confirmar = vi.spyOn(window, 'confirm').mockReturnValue(true);
    component.guardar();
    await fixture.whenStable();
    confirmar.mockRestore();

    http.expectNone(ventasBase);
    expect(component.error()).toContain('No se pudo consultar la existencia');
  });

  it('al reintentar y recibir 110 debe rehabilitar el registro', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    component.alSeleccionarCliente(clientes[0]);
    component.alSeleccionarProducto(0, productos[0]);
    http
      .expectOne(urlExistenciaProducto(5))
      .flush({ message: 'fallo' }, { status: 500, statusText: 'Server Error' });
    component.detalles.at(0).get('cantidad')?.setValue(22);
    fixture.detectChanges();

    expect(component.puedeRegistrar()).toBe(false);

    component.reintentarExistencia(0);
    flushExistencia(5, 110);
    fixture.detectChanges();

    expect(component.lineas()[0].estadoExistencia).toBe('disponible');
    expect(component.lineas()[0].existencia).toBe(110);
    expect(component.error()).toBeNull();
    expect(component.puedeRegistrar()).toBe(true);
    expect(textoPlantilla(fixture)).toContain('Existencia 110');
    expect(textoPlantilla(fixture)).not.toContain('Existencia no disponible');
  });

  it('con existencia real 0 debe mostrar Existencia 0 y considerar superado el stock', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    component.alSeleccionarProducto(0, productos[0]);
    flushExistencia(5, 0);
    component.detalles.at(0).get('cantidad')?.setValue(1);
    fixture.detectChanges();

    expect(component.lineas()[0].estadoExistencia).toBe('disponible');
    expect(component.lineas()[0].existencia).toBe(0);
    expect(textoPlantilla(fixture)).toContain('Existencia 0');
    expect(component.lineas()[0].excedeStock).toBe(true);
    expect(component.puedeRegistrar()).toBe(false);
  });

  it('dos líneas del mismo producto deben validar la cantidad acumulada', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    component.alSeleccionarCliente(clientes[0]);
    component.alSeleccionarProducto(0, productos[0]);
    flushExistencia(5, 110);
    component.detalles.at(0).get('cantidad')?.setValue(70);

    component.agregarLinea();
    component.alSeleccionarProducto(1, productos[0]);
    flushExistencia(5, 110, 21);
    component.detalles.at(1).get('cantidad')?.setValue(50);
    fixture.detectChanges();

    expect(component.lineas()[0].totalSolicitado).toBe(120);
    expect(component.lineas()[1].totalSolicitado).toBe(120);
    expect(component.lineas()[0].excedeStock).toBe(true);
    expect(component.lineas()[1].excedeStock).toBe(true);
    expect(component.puedeRegistrar()).toBe(false);

    const confirmar = vi.spyOn(window, 'confirm').mockReturnValue(true);
    component.guardar();
    await fixture.whenStable();
    confirmar.mockRestore();

    http.expectNone(ventasBase);
    expect(component.error()).toBe('La cantidad solicitada supera la existencia disponible (110).');
  });

  it('una respuesta tardía de un producto anterior no debe reemplazar el stock del actual', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    component.alSeleccionarProducto(0, productos[0]);
    component.alSeleccionarProducto(0, productos[1]);

    const reqTorta = http.expectOne(urlExistenciaProducto(6));
    reqTorta.flush({ id: 2, producto: { id: 6, nombre: 'Torta' }, cantidad: 3 });
    fixture.detectChanges();

    const reqPanTardio = http.expectOne(urlExistenciaProducto(5));
    reqPanTardio.flush({ id: 1, producto: { id: 5, nombre: 'Pan francés' }, cantidad: 999 });
    fixture.detectChanges();

    expect(component.lineas()[0].producto?.id).toBe(6);
    expect(component.lineas()[0].estadoExistencia).toBe('disponible');
    expect(component.lineas()[0].existencia).toBe(3);
  });
});
