import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Proveedor } from '../proveedor/proveedor.model';
import { CompraResponse } from './compra.model';
import { CompraReporte } from './compra-reporte';

describe('CompraReporte', () => {
  const comprasBase = 'http://localhost:8081/api/v1/compras';
  const proveedoresBase = 'http://localhost:8081/api/v1/proveedores';

  const proveedores: Proveedor[] = [
    {
      id: 1,
      ruc: '20123456789',
      razonSocial: 'Molinos del Sur SAC',
      telefono: null,
      email: null,
      activo: true,
    },
  ];

  const compras: CompraResponse[] = [
    {
      id: 10,
      proveedor: { id: 1, razonSocial: 'Molinos del Sur SAC' },
      fecha: '2026-09-10T10:00:00',
      estado: 'REGISTRADA',
      total: 100,
      detalles: [
        {
          id: 100,
          insumoId: 5,
          nombreInsumo: 'Harina',
          unidadMedida: 'KILOGRAMO',
          cantidad: 10,
          precioUnitario: 10,
          subtotal: 100,
        },
      ],
    },
    {
      id: 11,
      proveedor: { id: 1, razonSocial: 'Molinos del Sur SAC' },
      fecha: '2026-09-20T10:00:00',
      estado: 'ANULADA',
      total: 50,
      detalles: [
        {
          id: 101,
          insumoId: 6,
          nombreInsumo: 'Azúcar',
          unidadMedida: 'KILOGRAMO',
          cantidad: 5,
          precioUnitario: 10,
          subtotal: 50,
        },
      ],
    },
    {
      id: 12,
      proveedor: { id: 1, razonSocial: 'Molinos del Sur SAC' },
      fecha: '2026-09-25T10:00:00',
      estado: 'REGISTRADA',
      total: 30,
      detalles: [
        {
          id: 102,
          insumoId: 5,
          nombreInsumo: 'Harina',
          unidadMedida: 'KILOGRAMO',
          cantidad: 3,
          precioUnitario: 10,
          subtotal: 30,
        },
      ],
    },
  ];

  let http: HttpTestingController;

  async function configurar(): Promise<ComponentFixture<CompraReporte>> {
    await TestBed.configureTestingModule({
      imports: [CompraReporte],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    const fixture = TestBed.createComponent(CompraReporte);
    fixture.detectChanges();

    http = TestBed.inject(HttpTestingController);
    http.expectOne(comprasBase).flush(compras);
    http.expectOne(proveedoresBase).flush(proveedores);
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

  it('debe listar todas las compras con su detalle siempre visible', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    expect(component.compras().length).toBe(3);
    expect(fixture.nativeElement.querySelectorAll('.compra').length).toBe(3);
    expect(fixture.nativeElement.textContent).toContain('Harina');
    expect(fixture.nativeElement.textContent).toContain('Azúcar');
  });

  it('debe resolver el RUC del proveedor desde el catálogo', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    expect(component.rucDe(1)).toBe('20123456789');
    expect(fixture.nativeElement.textContent).toContain('20123456789');
  });

  it('debe calcular los indicadores sobre las compras visibles', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    expect(component.resumen().cantidad).toBe(3);
    expect(component.resumen().registradas).toBe(2);
    expect(component.resumen().montoVigente).toBe(130);
    expect(component.resumen().promedio).toBe(65);
  });

  it('debe filtrar por estado sin peticiones adicionales', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    component.actualizarFiltroEstado('REGISTRADA');
    expect(component.comprasFiltradas().map((compra) => compra.id)).toEqual([10, 12]);
    expect(component.resumen().montoVigente).toBe(130);

    component.actualizarFiltroEstado('ANULADA');
    expect(component.comprasFiltradas().map((compra) => compra.id)).toEqual([11]);
  });

  it('debe filtrar por rango de fechas inclusivo', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    component.actualizarBorradorDesde('2026-09-20');
    component.actualizarBorradorHasta('2026-09-25');
    component.aplicarFechas();

    expect(component.comprasFiltradas().map((compra) => compra.id)).toEqual([11, 12]);
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
    expect(component.desdeAplicado()).toBe('');
    expect(component.comprasFiltradas().length).toBe(3);
  });

  it('debe limpiar los filtros de fecha', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    component.actualizarBorradorDesde('2026-09-20');
    component.aplicarFechas();
    component.limpiarFechas();

    expect(component.desdeAplicado()).toBe('');
    expect(component.borradorDesde()).toBe('');
    expect(component.comprasFiltradas().length).toBe(3);
  });

  it('debe anular una compra tras confirmar', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    const confirmar = vi.spyOn(window, 'confirm').mockReturnValue(true);
    component.anular(compras[0]);
    await fixture.whenStable();

    const req = http.expectOne(`${comprasBase}/10/anular`);
    expect(req.request.method).toBe('PATCH');
    req.flush({ ...compras[0], estado: 'ANULADA' });
    await fixture.whenStable();
    fixture.detectChanges();

    confirmar.mockRestore();
    expect(component.compras()[0].estado).toBe('ANULADA');
    expect(component.exito()).toContain('anulada');
  });

  it('no debe anular si el usuario cancela la confirmación', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    const confirmar = vi.spyOn(window, 'confirm').mockReturnValue(false);
    component.anular(compras[0]);
    await fixture.whenStable();

    confirmar.mockRestore();
    http.expectNone(`${comprasBase}/10/anular`);
    expect(component.compras()[0].estado).toBe('REGISTRADA');
  });

  it('debe mostrar el mensaje cuando la compra ya estaba anulada', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    const confirmar = vi.spyOn(window, 'confirm').mockReturnValue(true);
    component.anular(compras[0]);
    await fixture.whenStable();

    http
      .expectOne(`${comprasBase}/10/anular`)
      .flush(
        { message: 'La compra con id 10 ya está anulada' },
        { status: 409, statusText: 'Conflict' },
      );
    await fixture.whenStable();

    confirmar.mockRestore();
    expect(component.error()).toBe('La compra con id 10 ya está anulada');
  });

  it('debe mostrar el mensaje de stock insuficiente al anular', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    const confirmar = vi.spyOn(window, 'confirm').mockReturnValue(true);
    component.anular(compras[0]);
    await fixture.whenStable();

    http
      .expectOne(`${comprasBase}/10/anular`)
      .flush(
        {
          message:
            'Stock insuficiente para el insumo 5: se solicitó 10 y hay 3 disponibles',
        },
        { status: 409, statusText: 'Conflict' },
      );
    await fixture.whenStable();

    confirmar.mockRestore();
    expect(component.error()).toContain('Stock insuficiente');
  });

  it('debe informar cuando falla la carga del reporte', async () => {
    await TestBed.configureTestingModule({
      imports: [CompraReporte],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    const fixture = TestBed.createComponent(CompraReporte);
    fixture.detectChanges();

    http = TestBed.inject(HttpTestingController);
    http
      .expectOne(comprasBase)
      .flush({ message: 'fallo' }, { status: 500, statusText: 'Server Error' });
    http.expectOne(proveedoresBase).flush([]);
    await fixture.whenStable();

    expect(fixture.componentInstance.error()).toBe('No se pudo cargar el reporte de compras.');
  });
});
