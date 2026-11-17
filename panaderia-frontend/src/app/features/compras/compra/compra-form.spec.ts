import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { Insumo } from '../../inventario/insumo/insumo.model';
import { Proveedor } from '../proveedor/proveedor.model';
import { CompraResponse } from './compra.model';
import { CompraForm } from './compra-form';

describe('CompraForm', () => {
  const comprasBase = 'http://localhost:8081/api/v1/compras';
  const proveedoresBase = 'http://localhost:8081/api/v1/proveedores';
  const insumosBase = 'http://localhost:8081/api/v1/insumos';

  const proveedores: Proveedor[] = [
    {
      id: 1,
      ruc: '20123456789',
      razonSocial: 'Molinos del Sur SAC',
      telefono: null,
      email: null,
      activo: true,
    },
    {
      id: 2,
      ruc: '20987654321',
      razonSocial: 'Insumos Andinos EIRL',
      telefono: null,
      email: null,
      activo: false,
    },
  ];

  const insumos: Insumo[] = [
    { id: 5, nombre: 'Harina', unidadMedida: 'KILOGRAMO', stockActual: 100, stockMinimo: 10 },
    { id: 6, nombre: 'Azúcar', unidadMedida: 'KILOGRAMO', stockActual: 50, stockMinimo: 5 },
  ];

  const respuesta: CompraResponse = {
    id: 10,
    proveedor: { id: 1, razonSocial: 'Molinos del Sur SAC' },
    fecha: '2026-09-14T11:30:00',
    estado: 'REGISTRADA',
    total: 87.5,
    detalles: [
      {
        id: 100,
        insumoId: 5,
        nombreInsumo: 'Harina',
        unidadMedida: 'KILOGRAMO',
        cantidad: 25,
        precioUnitario: 3.5,
        subtotal: 87.5,
      },
    ],
  };

  let http: HttpTestingController;

  async function configurar(): Promise<ComponentFixture<CompraForm>> {
    await TestBed.configureTestingModule({
      imports: [CompraForm],
      providers: [
        provideRouter([{ path: 'compras/reporte', children: [] }]),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(CompraForm);
    fixture.detectChanges();

    http = TestBed.inject(HttpTestingController);
    http.expectOne(proveedoresBase).flush(proveedores);
    http.expectOne(insumosBase).flush(insumos);
    await fixture.whenStable();
    fixture.detectChanges();

    return fixture;
  }

  function completarValida(component: CompraForm): void {
    component.alSeleccionarProveedor(proveedores[0]);
    component.alSeleccionarInsumo(0, insumos[0]);
    const linea = component.detalles.at(0);
    linea.get('cantidad')?.setValue(25);
    linea.get('precioUnitario')?.setValue(3.5);
  }

  afterEach(() => {
    http.verify();
  });

  it('should create', async () => {
    const fixture = await configurar();
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('debe iniciar con una línea y sin total', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    expect(component.detalles.length).toBe(1);
    expect(component.totalEstimado()).toBe(0);
    expect(component.puedeQuitarLinea).toBe(false);
  });

  it('debe ofrecer únicamente proveedores activos', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    expect(component.proveedoresDisponibles()).toEqual([proveedores[0]]);
    expect(component.proveedoresFiltrados()).toEqual([proveedores[0]]);
  });

  it('debe calcular el subtotal y el total al completar la línea', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    completarValida(component);

    expect(component.lineas()[0].subtotal).toBe(87.5);
    expect(component.totalEstimado()).toBe(87.5);
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

  it('no debe enviar la compra sin seleccionar un proveedor activo', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    component.form.controls.proveedorTexto.setValue('Molinos del Sur SAC');
    component.guardar();
    await fixture.whenStable();

    http.expectNone(comprasBase);
    expect(component.error()).toBe('Seleccione un proveedor activo de la lista.');
  });

  it('no debe enviar la compra si un insumo no fue seleccionado de la lista', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    component.alSeleccionarProveedor(proveedores[0]);
    component.form.controls.detalles.at(0).get('insumoTexto')?.setValue('Harina');
    component.form.controls.detalles.at(0).get('cantidad')?.setValue(10);
    component.form.controls.detalles.at(0).get('precioUnitario')?.setValue(2);
    component.guardar();
    await fixture.whenStable();

    http.expectNone(comprasBase);
    expect(component.error()).toBe(
      'Seleccione un insumo válido de la lista en cada línea del detalle.',
    );
  });

  it('debe rechazar una cantidad menor o igual a cero', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    completarValida(component);
    component.detalles.at(0).get('cantidad')?.setValue(0);
    component.guardar();
    await fixture.whenStable();

    http.expectNone(comprasBase);
    expect(component.detalles.at(0).get('cantidad')?.hasError('positivo')).toBe(true);
  });

  it('no debe enviar si el usuario cancela la confirmación', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    completarValida(component);
    const confirmar = vi.spyOn(window, 'confirm').mockReturnValue(false);
    component.guardar();
    await fixture.whenStable();

    confirmar.mockRestore();
    http.expectNone(comprasBase);
  });

  it('debe enviar el POST con proveedorId y detalles y navegar al reporte', async () => {
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

    const req = http.expectOne(comprasBase);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      proveedorId: 1,
      detalles: [{ insumoId: 5, cantidad: 25, precioUnitario: 3.5 }],
    });
    expect(mensajeConfirmacion).toContain('Molinos del Sur SAC');
    expect(mensajeConfirmacion).toContain('S/ 87.50');

    req.flush(respuesta);
    await fixture.whenStable();

    confirmar.mockRestore();
    expect(TestBed.inject(Router).url).toBe('/compras/reporte');
  });

  it('no debe duplicar el envío mientras la compra se está registrando', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    completarValida(component);
    const confirmar = vi.spyOn(window, 'confirm').mockReturnValue(true);

    component.guardar();
    component.guardar();

    const req = http.expectOne(comprasBase);
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
      .expectOne(comprasBase)
      .flush(
        { message: 'El proveedor con id 1 está inactivo' },
        { status: 409, statusText: 'Conflict' },
      );
    await fixture.whenStable();

    confirmar.mockRestore();
    expect(component.error()).toBe('El proveedor con id 1 está inactivo');
  });

  it('debe mostrar el mensaje del backend ante un 400', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    completarValida(component);
    const confirmar = vi.spyOn(window, 'confirm').mockReturnValue(true);
    component.guardar();
    await fixture.whenStable();

    http
      .expectOne(comprasBase)
      .flush(
        { message: 'La compra es inválida' },
        { status: 400, statusText: 'Bad Request' },
      );
    await fixture.whenStable();

    confirmar.mockRestore();
    expect(component.error()).toBe('La compra es inválida');
  });

  it('debe informar cuando falla la carga de catálogos', async () => {
    await TestBed.configureTestingModule({
      imports: [CompraForm],
      providers: [
        provideRouter([{ path: 'compras/reporte', children: [] }]),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(CompraForm);
    fixture.detectChanges();

    http = TestBed.inject(HttpTestingController);
    http
      .expectOne(proveedoresBase)
      .flush({ message: 'fallo' }, { status: 500, statusText: 'Server Error' });
    http
      .expectOne(insumosBase)
      .flush({ message: 'fallo' }, { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();

    expect(fixture.componentInstance.catalogosError()).toContain('catálogo');
    expect(fixture.componentInstance.catalogosLoading()).toBe(false);
  });
});
