import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { MovimientoFinancieroResponse } from './movimiento-financiero.model';
import { MovimientoList } from './movimiento-list';

describe('MovimientoList', () => {
  const base = 'http://localhost:8081/api/v1/movimientos-financieros';

  const movimientos: MovimientoFinancieroResponse[] = [
    {
      id: 1,
      fecha: '2026-09-10T10:00:00',
      tipo: 'INGRESO',
      origen: 'VENTA',
      origenId: 10,
      concepto: 'Ingreso por venta #10',
      monto: 25,
      estado: 'ACTIVO',
    },
    {
      id: 2,
      fecha: '2026-09-12T09:00:00',
      tipo: 'EGRESO',
      origen: 'COMPRA',
      origenId: 5,
      concepto: 'Egreso por compra #5',
      monto: 40,
      estado: 'ACTIVO',
    },
    {
      id: 3,
      fecha: '2026-09-13T15:00:00',
      tipo: 'INGRESO',
      origen: 'VENTA',
      origenId: 11,
      concepto: 'Ingreso por venta #11 anulada',
      monto: 40,
      estado: 'ANULADO',
    },
  ];

  let component: MovimientoList;
  let fixture: ComponentFixture<MovimientoList>;
  let http: HttpTestingController;

  const porUrl = (url: string) => (req: { url: string }) => req.url === url;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MovimientoList],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(MovimientoList);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    http.verify();
  });

  async function refrescar(): Promise<void> {
    await fixture.whenStable();
    fixture.detectChanges();
  }

  it('should create', () => {
    http.expectOne(porUrl(base)).flush([]);
    expect(component).toBeTruthy();
  });

  it('debe listar los movimientos con sus chips de tipo y estado', async () => {
    http.expectOne(porUrl(base)).flush(movimientos);
    await refrescar();

    expect(component.movimientos().length).toBe(3);
    expect(fixture.nativeElement.querySelectorAll('tbody tr').length).toBe(3);

    const compilado = fixture.nativeElement as HTMLElement;
    expect(compilado.textContent).toContain('Ingreso por venta #10');
    expect(compilado.textContent).toContain('S/ 25.00');
    expect(compilado.textContent).toContain('S/ 40.00');
  });

  it('debe mostrar el estado vacío cuando no hay movimientos', async () => {
    http.expectOne(porUrl(base)).flush([]);
    await refrescar();

    expect(fixture.nativeElement.textContent).toContain(
      'No hay movimientos financieros registrados',
    );
  });

  it('debe informar cuando falla la carga', async () => {
    http
      .expectOne(porUrl(base))
      .flush({ message: 'fallo' }, { status: 500, statusText: 'Server Error' });
    await refrescar();

    expect(component.error()).toBe(
      'No se pudo cargar la lista de movimientos financieros.',
    );
  });

  it('debe filtrar localmente por tipo sin enviar parámetros al backend', async () => {
    http.expectOne(porUrl(base)).flush(movimientos);
    await refrescar();

    component.actualizarFiltroTipo('EGRESO');
    await refrescar();

    http.expectNone(porUrl(base));
    expect(component.movimientosFiltrados().length).toBe(1);
    expect(component.movimientosFiltrados()[0].id).toBe(2);
    expect(component.hayFiltros()).toBe(true);
  });

  it('debe filtrar localmente por origen, estado y texto del concepto', async () => {
    http.expectOne(porUrl(base)).flush(movimientos);
    await refrescar();

    component.actualizarFiltroOrigen('VENTA');
    component.actualizarFiltroEstado('ANULADO');
    component.actualizarFiltroTexto('anulada');
    await refrescar();

    http.expectNone(porUrl(base));
    expect(component.movimientosFiltrados().length).toBe(1);
    expect(component.movimientosFiltrados()[0].id).toBe(3);

    component.limpiarFiltros();
    await refrescar();

    expect(component.movimientosFiltrados().length).toBe(3);
    expect(component.hayFiltros()).toBe(false);
  });

  it('debe mostrar el estado vacío cuando los filtros no coinciden', async () => {
    http.expectOne(porUrl(base)).flush(movimientos);
    await refrescar();

    component.actualizarFiltroTexto('no coincide con nada');
    await refrescar();

    expect(fixture.nativeElement.textContent).toContain(
      'No hay movimientos que coincidan con los filtros aplicados',
    );
  });
});
