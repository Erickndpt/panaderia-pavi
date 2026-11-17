import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { Proveedor } from './proveedor.model';
import { ProveedorForm } from './proveedor-form';

describe('ProveedorForm', () => {
  const base = 'http://localhost:8081/api/v1/proveedores';

  const proveedor: Proveedor = {
    id: 1,
    ruc: '20123456789',
    razonSocial: 'Molinos del Sur SAC',
    telefono: '987654321',
    email: 'ventas@molinos.pe',
    activo: true,
  };

  let http: HttpTestingController;

  async function configurar(id?: string): Promise<ComponentFixture<ProveedorForm>> {
    await TestBed.configureTestingModule({
      imports: [ProveedorForm],
      providers: [
        provideRouter([{ path: 'compras/proveedores', children: [] }]),
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

    const fixture = TestBed.createComponent(ProveedorForm);
    fixture.detectChanges();

    http = TestBed.inject(HttpTestingController);

    if (id !== undefined && Number.isInteger(Number(id)) && Number(id) > 0) {
      http.expectOne(`${base}/${id}`).flush(proveedor);
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

  it('debe iniciar el formulario activo al crear', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    expect(component.esEdicion).toBe(false);
    expect(component.form.getRawValue()).toEqual({
      ruc: '',
      razonSocial: '',
      telefono: '',
      email: '',
      activo: true,
    });
  });

  it('debe enviar el POST con campos opcionales nulos al guardar', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    component.form.setValue({
      ruc: '20123456789',
      razonSocial: 'Molinos del Sur SAC',
      telefono: '',
      email: '',
      activo: true,
    });
    component.guardar();
    await fixture.whenStable();

    const req = http.expectOne(base);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      ruc: '20123456789',
      razonSocial: 'Molinos del Sur SAC',
      telefono: null,
      email: null,
      activo: true,
    });

    req.flush(proveedor);
    await fixture.whenStable();
    expect(TestBed.inject(Router).url).toBe('/compras/proveedores');
  });

  it('debe rechazar un RUC que no tenga once dígitos', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    component.form.patchValue({ ruc: '123', razonSocial: 'Molinos del Sur SAC' });
    component.guardar();
    await fixture.whenStable();

    http.expectNone(base);
    expect(component.form.controls.ruc.hasError('pattern')).toBe(true);
  });

  it('debe cargar el proveedor y enviar el PUT al editar', async () => {
    const fixture = await configurar('1');
    const component = fixture.componentInstance;

    expect(component.esEdicion).toBe(true);
    expect(component.form.getRawValue()).toEqual({
      ruc: '20123456789',
      razonSocial: 'Molinos del Sur SAC',
      telefono: '987654321',
      email: 'ventas@molinos.pe',
      activo: true,
    });

    component.form.patchValue({ razonSocial: 'Molinos del Sur EIRL' });
    component.guardar();
    await fixture.whenStable();

    const req = http.expectOne(`${base}/1`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body.razonSocial).toBe('Molinos del Sur EIRL');
    req.flush({ ...proveedor, razonSocial: 'Molinos del Sur EIRL' });
  });

  it('debe mostrar el mensaje del backend cuando el RUC está duplicado', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    component.form.setValue({
      ruc: '20123456789',
      razonSocial: 'Molinos del Sur SAC',
      telefono: '',
      email: '',
      activo: true,
    });
    component.guardar();
    await fixture.whenStable();

    http
      .expectOne(base)
      .flush(
        { message: 'Ya existe un proveedor con el RUC 20123456789' },
        { status: 409, statusText: 'Conflict' },
      );
    await fixture.whenStable();

    expect(component.error()).toBe('Ya existe un proveedor con el RUC 20123456789');
  });

  it('debe rechazar un identificador inválido sin llamar al backend', async () => {
    const fixture = await configurar('abc');
    await fixture.whenStable();

    expect(fixture.componentInstance.error()).toBe('El identificador del proveedor no es válido.');
  });
});
