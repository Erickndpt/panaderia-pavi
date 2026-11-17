import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatSlideToggle, MatSlideToggleChange } from '@angular/material/slide-toggle';
import { provideRouter } from '@angular/router';
import { Cliente } from './cliente.model';
import { ClienteList } from './cliente-list';

describe('ClienteList', () => {
  const base = 'http://localhost:8081/api/v1/clientes';

  const clientes: Cliente[] = [
    {
      id: 1,
      tipoDocumento: 'DNI',
      numeroDocumento: '12345678',
      nombreRazonSocial: 'María López',
      telefono: null,
      email: null,
      direccion: null,
      pais: 'Perú',
      activo: true,
    },
    {
      id: 2,
      tipoDocumento: 'RUC',
      numeroDocumento: '20123456789',
      nombreRazonSocial: 'Panadería Central SAC',
      telefono: null,
      email: null,
      direccion: null,
      pais: 'Perú',
      activo: false,
    },
  ];

  let component: ClienteList;
  let fixture: ComponentFixture<ClienteList>;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ClienteList],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(ClienteList);
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

  it('debe listar los clientes al iniciar', async () => {
    const req = http.expectOne(base);
    expect(req.request.method).toBe('GET');
    req.flush(clientes);
    await refrescar();

    expect(component.clientes()).toEqual(clientes);
    expect(fixture.nativeElement.querySelectorAll('tbody tr').length).toBe(2);
    expect(fixture.nativeElement.textContent).toContain('María López');
  });

  it('debe enviar el filtro buscar al backend', async () => {
    http.expectOne(base).flush(clientes);
    await refrescar();

    component.actualizarFiltro('lópez');
    component.buscar();

    const req = http.expectOne((peticion) => peticion.url === base);
    expect(req.request.params.get('buscar')).toBe('lópez');
    req.flush([clientes[0]]);
    await refrescar();

    expect(component.clientes()).toEqual([clientes[0]]);
  });

  it('debe activar un cliente con el PUT existente', async () => {
    http.expectOne(base).flush(clientes);
    await refrescar();

    component.cambiarActivo(clientes[1], evento(true));
    await refrescar();

    const req = http.expectOne(`${base}/2`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body.activo).toBe(true);
    req.flush({ ...clientes[1], activo: true });
    await refrescar();

    expect(component.clientes()[1].activo).toBe(true);
    expect(component.exito()).toContain('activado');
  });

  it('no debe desactivar si el usuario cancela la confirmación', async () => {
    http.expectOne(base).flush(clientes);
    await refrescar();

    const confirmar = vi.spyOn(window, 'confirm').mockReturnValue(false);
    const cambio = evento(false);
    component.cambiarActivo(clientes[0], cambio);
    await refrescar();

    confirmar.mockRestore();
    http.expectNone(`${base}/1`);
    expect(cambio.source.checked).toBe(true);
  });

  it('debe desactivar un cliente tras confirmar', async () => {
    http.expectOne(base).flush(clientes);
    await refrescar();

    const confirmar = vi.spyOn(window, 'confirm').mockReturnValue(true);
    component.cambiarActivo(clientes[0], evento(false));
    await refrescar();

    const req = http.expectOne(`${base}/1`);
    expect(req.request.body.activo).toBe(false);
    req.flush({ ...clientes[0], activo: false });
    await refrescar();

    confirmar.mockRestore();
    expect(component.clientes()[0].activo).toBe(false);
    expect(component.exito()).toContain('desactivado');
  });

  it('debe informar cuando la carga falla', async () => {
    http
      .expectOne(base)
      .flush({ message: 'fallo' }, { status: 500, statusText: 'Server Error' });
    await refrescar();

    expect(component.error()).toBe('No se pudo cargar la lista de clientes.');
  });
});
