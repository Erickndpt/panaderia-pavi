import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatSlideToggle, MatSlideToggleChange } from '@angular/material/slide-toggle';
import { provideRouter } from '@angular/router';
import { Proveedor } from './proveedor.model';
import { ProveedorList } from './proveedor-list';

describe('ProveedorList', () => {
  const base = 'http://localhost:8081/api/v1/proveedores';

  const proveedores: Proveedor[] = [
    { id: 1, ruc: '20123456789', razonSocial: 'Molinos del Sur', telefono: null, email: null, activo: true },
    { id: 2, ruc: '20987654321', razonSocial: 'Azúcar Andina', telefono: null, email: null, activo: false },
  ];

  let component: ProveedorList;
  let fixture: ComponentFixture<ProveedorList>;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProveedorList],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(ProveedorList);
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

  function evento(checked: boolean): MatSlideToggleChange {
    const source = { checked: !checked } as unknown as MatSlideToggle;
    return { checked, source } as MatSlideToggleChange;
  }

  it('should create', () => {
    http.expectOne(base).flush([]);
    expect(component).toBeTruthy();
  });

  it('debe listar los proveedores y filtrar en memoria', async () => {
    http.expectOne(base).flush(proveedores);
    await refrescar();

    expect(component.proveedores()).toEqual(proveedores);
    expect(fixture.nativeElement.querySelectorAll('tbody tr').length).toBe(2);

    component.actualizarFiltro('andina');
    expect(component.proveedoresFiltrados()).toEqual([proveedores[1]]);
  });

  it('debe activar un proveedor con el PUT existente', async () => {
    http.expectOne(base).flush(proveedores);
    await refrescar();

    const cambio = evento(true);
    component.cambiarActivo(proveedores[1], cambio);
    await refrescar();

    const req = http.expectOne(`${base}/2`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body.activo).toBe(true);
    req.flush({ ...proveedores[1], activo: true });
    await refrescar();

    expect(component.proveedores()[1].activo).toBe(true);
    expect(component.exito()).toContain('activado');
  });

  it('no debe desactivar si el usuario cancela la confirmación', async () => {
    http.expectOne(base).flush(proveedores);
    await refrescar();

    const confirmar = vi.spyOn(window, 'confirm').mockReturnValue(false);
    const cambio = evento(false);
    component.cambiarActivo(proveedores[0], cambio);
    await refrescar();

    confirmar.mockRestore();
    http.expectNone(`${base}/1`);
    expect(cambio.source.checked).toBe(true);
  });

  it('debe desactivar un proveedor tras confirmar', async () => {
    http.expectOne(base).flush(proveedores);
    await refrescar();

    const confirmar = vi.spyOn(window, 'confirm').mockReturnValue(true);
    component.cambiarActivo(proveedores[0], evento(false));
    await refrescar();

    const req = http.expectOne(`${base}/1`);
    expect(req.request.body.activo).toBe(false);
    req.flush({ ...proveedores[0], activo: false });
    await refrescar();

    confirmar.mockRestore();
    expect(component.proveedores()[0].activo).toBe(false);
    expect(component.exito()).toContain('desactivado');
  });

  it('debe mostrar el mensaje del backend cuando el cambio responde 409', async () => {
    http.expectOne(base).flush(proveedores);
    await refrescar();

    const confirmar = vi.spyOn(window, 'confirm').mockReturnValue(true);
    component.cambiarActivo(proveedores[0], evento(false));
    await refrescar();

    http
      .expectOne(`${base}/1`)
      .flush(
        { message: 'Ya existe un proveedor con el RUC 20123456789' },
        { status: 409, statusText: 'Conflict' },
      );
    await refrescar();

    confirmar.mockRestore();
    expect(component.error()).toBe('Ya existe un proveedor con el RUC 20123456789');
  });

  it('debe informar cuando la carga falla', async () => {
    http
      .expectOne(base)
      .flush({ message: 'fallo' }, { status: 500, statusText: 'Server Error' });
    await refrescar();

    expect(component.error()).toBe('No se pudo cargar la lista de proveedores.');
  });
});
