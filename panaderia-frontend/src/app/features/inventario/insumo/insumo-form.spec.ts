import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { Insumo } from './insumo.model';
import { InsumoForm } from './insumo-form';

describe('InsumoForm', () => {
  const base = 'http://localhost:8081/api/v1/insumos';

  const insumo: Insumo = {
    id: 1,
    nombre: 'Harina de trigo',
    unidadMedida: 'KILOGRAMO',
    stockActual: 20,
    stockMinimo: 5,
  };

  let http: HttpTestingController;
  let router: Router;

  async function configurar(id?: string): Promise<ComponentFixture<InsumoForm>> {
    await TestBed.configureTestingModule({
      imports: [InsumoForm],
      providers: [
        provideRouter([{ path: 'inventario/insumos', children: [] }]),
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

    const fixture = TestBed.createComponent(InsumoForm);
    fixture.detectChanges();

    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);

    if (id !== undefined && Number.isInteger(Number(id)) && Number(id) > 0) {
      http.expectOne(`${base}/${id}`).flush(insumo);
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

  it('debe crear un insumo sin cargar datos previos', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    expect(component.esEdicion).toBe(false);
    expect(component.form.getRawValue()).toEqual({
      nombre: '',
      unidadMedida: '',
      stockActual: 0,
      stockMinimo: 0,
    });
  });

  it('debe cargar el insumo y rellenar el formulario al editar', async () => {
    const fixture = await configurar('1');
    const component = fixture.componentInstance;

    expect(component.esEdicion).toBe(true);
    expect(component.form.getRawValue()).toEqual({
      nombre: 'Harina de trigo',
      unidadMedida: 'KILOGRAMO',
      stockActual: 20,
      stockMinimo: 5,
    });
  });

  it('debe enviar el POST al guardar un insumo válido', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    component.form.setValue({
      nombre: 'Harina de trigo',
      unidadMedida: 'KILOGRAMO',
      stockActual: 20,
      stockMinimo: 5,
    });
    component.guardar();
    await fixture.whenStable();

    const req = http.expectOne(base);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      nombre: 'Harina de trigo',
      unidadMedida: 'KILOGRAMO',
      stockActual: 20,
      stockMinimo: 5,
    });

    req.flush(insumo);
    await fixture.whenStable();
    expect(router.url).toBe('/inventario/insumos');
  });

  it('debe bloquear el guardado con datos inválidos', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    component.form.setValue({
      nombre: '',
      unidadMedida: 'KILOGRAMO',
      stockActual: -1,
      stockMinimo: 5,
    });
    component.guardar();
    await fixture.whenStable();

    http.expectNone(() => true);
    expect(component.form.controls.nombre.hasError('required')).toBe(true);
    expect(component.form.controls.stockActual.hasError('min')).toBe(true);
  });

  it('debe rechazar un identificador inválido sin llamar al backend', async () => {
    const fixture = await configurar('abc');
    await fixture.whenStable();

    expect(fixture.componentInstance.error()).toBe('El identificador del insumo no es válido.');
  });
});