import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ResumenFinancieroResponse } from './movimiento-financiero.model';
import { ReporteFinanciero } from './reporte-financiero';

describe('ReporteFinanciero', () => {
  const resumenUrl = 'http://localhost:8081/api/v1/reportes/finanzas';

  const resumen: ResumenFinancieroResponse = {
    cantidadIngresos: 3,
    montoIngresos: 120.5,
    cantidadEgresos: 1,
    montoEgresos: 40,
    saldo: 80.5,
  };

  let component: ReporteFinanciero;
  let fixture: ComponentFixture<ReporteFinanciero>;
  let http: HttpTestingController;

  const porUrl = (url: string) => (req: { url: string }) => req.url === url;

  async function configurar(): Promise<void> {
    await TestBed.configureTestingModule({
      imports: [ReporteFinanciero],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(ReporteFinanciero);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  }

  async function refrescar(): Promise<void> {
    await fixture.whenStable();
    fixture.detectChanges();
  }

  it('should create', async () => {
    await configurar();
    http.expectOne(porUrl(resumenUrl)).flush(resumen);
    await refrescar();
    expect(component).toBeTruthy();
  });

  it('debe cargar el resumen del backend al iniciar y mostrar los indicadores', async () => {
    await configurar();

    const req = http.expectOne(porUrl(resumenUrl));
    expect(req.request.method).toBe('GET');
    req.flush(resumen);
    await refrescar();

    expect(component.resumen()).toEqual(resumen);

    const compilado = fixture.nativeElement as HTMLElement;
    expect(compilado.textContent).toContain('Cantidad de ingresos');
    expect(compilado.textContent).toContain('Monto de ingresos');
    expect(compilado.textContent).toContain('S/ 120.50');
    expect(compilado.textContent).toContain('Cantidad de egresos');
    expect(compilado.textContent).toContain('Monto de egresos');
    expect(compilado.textContent).toContain('S/ 40.00');
    expect(compilado.textContent).toContain('Saldo');
    expect(compilado.textContent).toContain('S/ 80.50');
    expect(compilado.textContent).toContain('Saldo positivo');
  });

  it('debe diferenciar un saldo negativo', async () => {
    await configurar();
    http
      .expectOne(porUrl(resumenUrl))
      .flush({ ...resumen, saldo: -15.5, montoEgresos: 136 });
    await refrescar();

    expect(component.saldoNegativo()).toBe(true);
    expect(component.saldoPositivo()).toBe(false);
    expect(component.signoSaldo()).toBe('Saldo negativo');
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Saldo negativo');
  });

  it('debe diferenciar un saldo en cero', async () => {
    await configurar();
    http
      .expectOne(porUrl(resumenUrl))
      .flush({ ...resumen, montoIngresos: 40, saldo: 0 });
    await refrescar();

    expect(component.saldoPositivo()).toBe(false);
    expect(component.saldoNegativo()).toBe(false);
    expect(component.signoSaldo()).toBe('Saldo en cero');
  });

  it('debe informar el error y permitir reintentar', async () => {
    await configurar();

    http
      .expectOne(porUrl(resumenUrl))
      .flush({ message: 'fallo' }, { status: 500, statusText: 'Server Error' });
    await refrescar();

    expect(component.error()).toBe('No se pudo cargar el resumen financiero.');
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Reintentar');

    component.cargar();
    await refrescar();
    http.expectOne(porUrl(resumenUrl)).flush(resumen);
    await refrescar();

    expect(component.error()).toBeNull();
    expect(component.resumen()?.saldo).toBe(80.5);
  });

  it('debe indicar que el resumen solo considera movimientos activos', async () => {
    await configurar();
    http.expectOne(porUrl(resumenUrl)).flush(resumen);
    await refrescar();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain(
      'movimientos activos',
    );
  });
});
