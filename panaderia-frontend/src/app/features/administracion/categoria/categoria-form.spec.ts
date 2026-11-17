import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { Categoria } from './categoria.model';
import { CategoriaForm } from './categoria-form';

describe('CategoriaForm', () => {
  const base = 'http://localhost:8081/api/v1/categorias';

  const categoria: Categoria = { id: 1, nombre: 'Panes', descripcion: 'Panes salados' };

  let http: HttpTestingController;

  async function configurar(id?: string): Promise<ComponentFixture<CategoriaForm>> {
    await TestBed.configureTestingModule({
      imports: [CategoriaForm],
      providers: [
        provideRouter([{ path: 'administracion/categorias', children: [] }]),
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

    const fixture = TestBed.createComponent(CategoriaForm);
    fixture.detectChanges();

    http = TestBed.inject(HttpTestingController);

    if (id !== undefined && Number.isInteger(Number(id)) && Number(id) > 0) {
      http.expectOne(`${base}/${id}`).flush(categoria);
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

  it('debe crear una categoría sin cargar datos previos', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    expect(component.esEdicion).toBe(false);
    expect(component.form.getRawValue()).toEqual({ nombre: '', descripcion: '' });
  });

  it('debe cargar la categoría y rellenar el formulario al editar', async () => {
    const fixture = await configurar('1');
    const component = fixture.componentInstance;

    expect(component.esEdicion).toBe(true);
    expect(component.form.getRawValue()).toEqual({ nombre: 'Panes', descripcion: 'Panes salados' });
  });

  it('debe enviar el POST con descripción nula al guardar sin descripción', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    component.form.setValue({ nombre: 'Bebidas', descripcion: '' });
    component.guardar();
    await fixture.whenStable();

    const req = http.expectOne(base);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ nombre: 'Bebidas', descripcion: null });

    req.flush({ id: 5, nombre: 'Bebidas', descripcion: null });
    await fixture.whenStable();
    expect(TestBed.inject(Router).url).toBe('/administracion/categorias');
  });

  it('debe bloquear el guardado sin nombre', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    component.form.setValue({ nombre: '   ', descripcion: '' });
    component.guardar();
    await fixture.whenStable();

    http.expectNone(() => true);
    expect(component.form.controls.nombre.hasError('required')).toBe(true);
  });

  it('debe mostrar el mensaje del backend al responder 409', async () => {
    const fixture = await configurar();
    const component = fixture.componentInstance;

    component.form.setValue({ nombre: 'Panes', descripcion: '' });
    component.guardar();
    await fixture.whenStable();

    http
      .expectOne(base)
      .flush(
        { message: 'Ya existe una categoría con el nombre Panes' },
        { status: 409, statusText: 'Conflict' },
      );
    await fixture.whenStable();

    expect(component.error()).toBe('Ya existe una categoría con el nombre Panes');
  });

  it('debe rechazar un identificador inválido sin llamar al backend', async () => {
    const fixture = await configurar('abc');
    await fixture.whenStable();

    expect(fixture.componentInstance.error()).toBe(
      'El identificador de la categoría no es válido.',
    );
  });
});
