import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { OrdenProduccionResponse } from './orden-produccion.model';
import { OrdenList } from './orden-list';

describe('OrdenList', () => {
  const ordenesBase = 'http://localhost:8081/api/v1/ordenes-produccion';

  const ordenes: OrdenProduccionResponse[] = [
    {
      id: 1,
      fecha: '2026-09-10T08:00:00',
      estado: 'COMPLETADA',
      productoId: 1,
      nombreProducto: 'Pan francés',
      cantidadProducida: 100,
      detalles: [
        {
          id: 1,
          insumoId: 5,
          nombreInsumo: 'Harina',
          unidadMedida: 'KILOGRAMO',
          cantidadConsumida: 20,
        },
        {
          id: 2,
          insumoId: 6,
          nombreInsumo: 'Levadura',
          unidadMedida: 'KILOGRAMO',
          cantidadConsumida: 1,
        },
      ],
    },
    {
      id: 2,
      fecha: '2026-09-20T09:30:00',
      estado: 'COMPLETADA',
      productoId: 2,
      nombreProducto: 'Torta de chocolate',
      cantidadProducida: 5,
      detalles: [
        {
          id: 3,
          insumoId: 5,
          nombreInsumo: 'Harina',
          unidadMedida: 'KILOGRAMO',
          cantidadConsumida: 2,
        },
      ],
    },
    {
      id: 3,
      fecha: '2026-09-25T11:15:00',
      estado: 'COMPLETADA',
      productoId: 1,
      nombreProducto: 'Pan francés',
      cantidadProducida: 50,
      detalles: [
        {
          id: 4,
          insumoId: 5,
          nombreInsumo: 'Harina',
          unidadMedida: 'KILOGRAMO',
          cantidadConsumida: 10,
        },
        {
          id: 5,
          insumoId: 6,
          nombreInsumo: 'Levadura',
          unidadMedida: 'KILOGRAMO',
          cantidadConsumida: 0.5,
        },
        {
          id: 6,
          insumoId: 7,
          nombreInsumo: 'Sal',
          unidadMedida: 'KILOGRAMO',
          cantidadConsumida: 0.2,
        },
      ],
    },
  ];

  let http: HttpTestingController;

  async function configurar(): Promise<ComponentFixture<OrdenList>> {
    await TestBed.configureTestingModule({
      imports: [OrdenList],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    const fixture = TestBed.createComponent(OrdenList);
    fixture.detectChanges();

    http = TestBed.inject(HttpTestingController);
    http.expectOne(ordenesBase).flush(ordenes);
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

  it('debe cargar las órdenes con su detalle visible sin peticiones por orden', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    expect(component.ordenes().length).toBe(3);
    expect(fixture.nativeElement.querySelectorAll('.orden-card').length).toBe(3);
    expect(fixture.nativeElement.textContent).toContain('Harina');
    expect(fixture.nativeElement.textContent).toContain('Levadura');
    http.verify();
  });

  it('debe calcular los indicadores sin sumar productos diferentes', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    expect(component.resumen().ordenes).toBe(3);
    expect(component.resumen().productos).toBe(2);
    expect(component.resumen().lineas).toBe(6);
    expect(component.resumen().unicoProducto).toBe(false);
    expect(component.resumen().cantidadProducida).toBe(0);
  });

  it('debe totalizar la producción cuando el filtro deja un único producto', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    component.actualizarFiltroProducto('Pan');
    expect(component.ordenesFiltradas().map((orden) => orden.id)).toEqual([1, 3]);
    expect(component.resumen().productos).toBe(1);
    expect(component.resumen().unicoProducto).toBe(true);
    expect(component.resumen().cantidadProducida).toBe(150);
    expect(component.resumen().nombreProducto).toBe('Pan francés');
  });

  it('debe filtrar por estado según el enum real', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    component.actualizarFiltroEstado('COMPLETADA');
    expect(component.ordenesFiltradas().length).toBe(3);

    component.actualizarFiltroEstado('TODAS');
    expect(component.ordenesFiltradas().length).toBe(3);
  });

  it('debe filtrar por rango de fechas inclusivo', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    component.actualizarBorradorDesde('2026-09-20');
    component.actualizarBorradorHasta('2026-09-25');
    component.aplicarFechas();

    expect(component.ordenesFiltradas().map((orden) => orden.id)).toEqual([2, 3]);
  });

  it('debe rechazar un rango de fechas invertido', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    component.actualizarBorradorDesde('2026-09-25');
    component.actualizarBorradorHasta('2026-09-10');
    component.aplicarFechas();

    expect(component.errorFechas()).toBe(
      'La fecha inicial no puede ser posterior a la fecha final.',
    );
    expect(component.ordenesFiltradas().length).toBe(3);
  });

  it('debe limpiar los filtros de fecha', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    component.actualizarBorradorDesde('2026-09-20');
    component.aplicarFechas();
    component.limpiarFechas();

    expect(component.desdeAplicado()).toBe('');
    expect(component.ordenesFiltradas().length).toBe(3);
  });

  it('debe mostrar el estado vacío por filtros sin coincidencias', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    component.actualizarFiltroProducto('Producto inexistente');
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(component.ordenesFiltradas().length).toBe(0);
    expect(fixture.nativeElement.textContent).toContain(
      'No se encontraron órdenes que coincidan con los filtros aplicados',
    );
  });

  it('debe mostrar el mensaje de éxito pendiente de la notificación', async () => {
    const fixture = await configurar();
    // El mensaje lo consume ngOnInit desde NotificacionService; sin mensaje previo queda vacío.
    expect(fixture.componentInstance.exito()).toBeNull();
  });

  it('debe informar cuando falla la carga', async () => {
    await TestBed.configureTestingModule({
      imports: [OrdenList],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    const fixture = TestBed.createComponent(OrdenList);
    fixture.detectChanges();

    http = TestBed.inject(HttpTestingController);
    http
      .expectOne(ordenesBase)
      .flush({ message: 'fallo' }, { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();

    expect(fixture.componentInstance.error()).toBe(
      'No se pudo cargar la lista de órdenes de producción.',
    );
  });
});
