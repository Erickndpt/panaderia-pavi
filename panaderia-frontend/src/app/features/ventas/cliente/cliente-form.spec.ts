import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { Cliente } from './cliente.model';
import { ClienteForm } from './cliente-form';

describe('ClienteForm', () => {
  const base = 'http://localhost:8081/api/v1/clientes';

  const cliente: Cliente = {
    id: 1,
    tipoDocumento: 'DNI',
    numeroDocumento: '12345678',
    nombreRazonSocial: 'María López',
    telefono: '987654321',
    email: null,
    direccion: null,
    pais: 'Perú',
    activo: true,
  };

  let http: HttpTestingController;

  async function configurar(id?: string): Promise<ComponentFixture<ClienteForm>> {
    await TestBed.configureTestingModule({
      imports: [ClienteForm],
      providers: [
        provideRouter([{ path: 'ventas/clientes', children: [] }]),
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

    const fixture = TestBed.createComponent(ClienteForm);
    fixture.detectChanges();

    http = TestBed.inject(HttpTestingController);

    if (id !== undefined && Number.isInteger(Number(id)) && Number(id) > 0) {
      http.expectOne(`${base}/${id}`).flush(cliente);
      await fixture.whenStable();
      fixture.detectChanges();
    }

    return fixture;
  }

  afterEach(() => {
    http.verify();
  });

  it('should create', async () => {
    const fixture = await configurar();
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('debe iniciar con DNI y estado activo al crear', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    expect(component.esEdicion).toBe(false);
    expect(component.form.getRawValue()).toEqual({
      tipoDocumento: 'DNI',
      numeroDocumento: '',
      nombreRazonSocial: '',
      telefono: '',
      email: '',
      direccion: '',
      pais: '',
      activo: true,
    });
  });

  it('debe exigir ocho dígitos para el DNI', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    component.form.patchValue({ numeroDocumento: '123' });
    expect(component.form.controls.numeroDocumento.hasError('required')).toBe(false);
    expect(component.form.controls.numeroDocumento.hasError('pattern')).toBe(true);
  });

  it('debe deshabilitar el número de documento al elegir Sin documento', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    component.form.patchValue({ tipoDocumento: 'SIN_DOCUMENTO' });
    expect(component.form.controls.numeroDocumento.disabled).toBe(true);

    component.form.patchValue({ tipoDocumento: 'RUC' });
    expect(component.form.controls.numeroDocumento.disabled).toBe(false);
  });

  it('debe enviar el POST con los datos del cliente', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    component.form.patchValue({
      tipoDocumento: 'DNI',
      numeroDocumento: '12345678',
      nombreRazonSocial: 'María López',
      telefono: '',
      email: '',
      direccion: '',
      pais: 'Perú',
      activo: true,
    });
    component.guardar();
    await fixture.whenStable();

    const req = http.expectOne(base);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      tipoDocumento: 'DNI',
      numeroDocumento: '12345678',
      nombreRazonSocial: 'María López',
      telefono: null,
      email: null,
      direccion: null,
      pais: 'Perú',
      activo: true,
    });

    req.flush(cliente);
    await fixture.whenStable();
    expect(TestBed.inject(Router).url).toBe('/ventas/clientes');
  });

  it('debe enviar número de documento nulo cuando el tipo es Sin documento', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    component.form.patchValue({
      tipoDocumento: 'SIN_DOCUMENTO',
      nombreRazonSocial: 'Cliente ocasional',
    });
    component.guardar();
    await fixture.whenStable();

    const req = http.expectOne(base);
    expect(req.request.body.numeroDocumento).toBeNull();
    req.flush({ ...cliente, tipoDocumento: 'SIN_DOCUMENTO', numeroDocumento: null });
  });

  it('debe cargar el cliente y enviar el PUT al editar', async () => {
    const fixture = await configurar('1');
    const component = fixture.componentInstance;

    expect(component.esEdicion).toBe(true);
    expect(component.form.getRawValue()).toEqual({
      tipoDocumento: 'DNI',
      numeroDocumento: '12345678',
      nombreRazonSocial: 'María López',
      telefono: '987654321',
      email: '',
      direccion: '',
      pais: 'Perú',
      activo: true,
    });

    component.form.patchValue({ nombreRazonSocial: 'María López Torres' });
    component.guardar();
    await fixture.whenStable();

    const req = http.expectOne(`${base}/1`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body.nombreRazonSocial).toBe('María López Torres');
    req.flush({ ...cliente, nombreRazonSocial: 'María López Torres' });
  });

  it('debe mostrar el mensaje del backend cuando el documento está duplicado', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    component.form.patchValue({
      tipoDocumento: 'DNI',
      numeroDocumento: '12345678',
      nombreRazonSocial: 'María López',
    });
    component.guardar();
    await fixture.whenStable();

    http
      .expectOne(base)
      .flush(
        { message: 'Ya existe un cliente con el número de documento 12345678' },
        { status: 409, statusText: 'Conflict' },
      );
    await fixture.whenStable();

    expect(component.error()).toBe(
      'Ya existe un cliente con el número de documento 12345678',
    );
  });

  it('debe rechazar un identificador inválido sin llamar al backend', async () => {
    const fixture = await configurar('abc');
    await fixture.whenStable();

    expect(fixture.componentInstance.error()).toBe('El identificador del cliente no es válido.');
  });

  it('debe consultar un DNI válido y autocompletar el nombre y país', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    component.form.patchValue({ tipoDocumento: 'DNI', numeroDocumento: '60012345' });
    expect(component.puedeConsultar()).toBe(true);

    component.consultarDocumento();

    const req = http.expectOne(
      (request) =>
        request.url === [base, 'consulta-documento'].join('/') &&
        request.params.get('tipo') === 'DNI' &&
        request.params.get('numero') === '60012345',
    );
    expect(req.request.method).toBe('GET');
    req.flush({
      tipoDocumento: 'DNI',
      numeroDocumento: '60012345',
      nombreRazonSocial: 'QUISPE PEREZ JULIO FERNANDO',
      direccion: null,
      pais: 'Perú',
      estado: null,
      condicion: null,
    });

    expect(component.form.controls.nombreRazonSocial.value).toBe(
      'QUISPE PEREZ JULIO FERNANDO',
    );
    expect(component.form.controls.pais.value).toBe('Perú');
    expect(component.consultando()).toBe(false);
  });

  it('debe conservar la dirección escrita al consultar un RUC', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    component.form.patchValue({
      tipoDocumento: 'RUC',
      numeroDocumento: '20100017491',
      direccion: 'Dirección indicada por el usuario',
    });
    component.consultarDocumento();

    http
      .expectOne(
        (request) =>
          request.url === [base, 'consulta-documento'].join('/') &&
          request.params.get('tipo') === 'RUC',
      )
      .flush({
        tipoDocumento: 'RUC',
        numeroDocumento: '20100017491',
        nombreRazonSocial: 'INTEGRATEL PERÚ S.A.A.',
        direccion: 'DIRECCIÓN DEVUELTA POR LA API',
        pais: 'Perú',
        estado: 'ACTIVO',
        condicion: 'HABIDO',
      });

    expect(component.form.controls.nombreRazonSocial.value).toBe('INTEGRATEL PERÚ S.A.A.');
    expect(component.form.controls.direccion.value).toBe('Dirección indicada por el usuario');
  });
});

