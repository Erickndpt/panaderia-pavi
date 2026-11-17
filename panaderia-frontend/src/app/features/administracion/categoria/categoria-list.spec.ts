import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthService } from '../../../core/auth/auth-service';
import { Categoria } from './categoria.model';
import { CategoriaList } from './categoria-list';

describe('CategoriaList', () => {
  const base = 'http://localhost:8081/api/v1/categorias';
  const roles = signal<string[]>(['ADMIN']);
  const authFake = {
    autenticado: signal(true),
    usuario: signal('Admin'),
    roles,
    tieneRol: vi.fn((...solicitados: string[]) =>
      solicitados.some((rol) => roles().includes(rol)),
    ),
    iniciarLogin: vi.fn(),
    cerrarSesion: vi.fn(),
  };

  const categorias: Categoria[] = [
    { id: 1, nombre: 'Panes', descripcion: 'Panes salados' },
    { id: 2, nombre: 'Bebidas', descripcion: null },
  ];

  let component: CategoriaList;
  let fixture: ComponentFixture<CategoriaList>;
  let http: HttpTestingController;

  beforeEach(async () => {
    roles.set(['ADMIN']);

    await TestBed.configureTestingModule({
      imports: [CategoriaList],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: authFake },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CategoriaList);
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
    http.expectOne(base).flush([]);
    expect(component).toBeTruthy();
  });

  it('debe listar las categorías al iniciar', async () => {
    const req = http.expectOne(base);
    expect(req.request.method).toBe('GET');
    req.flush(categorias);
    await refrescar();

    expect(component.categorias()).toEqual(categorias);
    expect(fixture.nativeElement.querySelectorAll('tbody tr').length).toBe(2);
    expect(fixture.nativeElement.textContent).toContain('Panes');
  });

  it('debe filtrar las categorías en memoria por nombre o descripción', async () => {
    http.expectOne(base).flush(categorias);
    await refrescar();

    component.actualizarFiltro('bebi');
    expect(component.categoriasFiltradas()).toEqual([categorias[1]]);

    component.actualizarFiltro('salados');
    expect(component.categoriasFiltradas()).toEqual([categorias[0]]);

    component.limpiarFiltro();
    expect(component.categoriasFiltradas().length).toBe(2);
  });

  it('debe informar cuando no hay categorías registradas', async () => {
    http.expectOne(base).flush([]);
    await refrescar();

    expect(fixture.nativeElement.textContent).toContain('No hay categorías registradas');
  });

  it('debe informar cuando la carga falla', async () => {
    http
      .expectOne(base)
      .flush({ message: 'fallo' }, { status: 500, statusText: 'Server Error' });
    await refrescar();

    expect(component.error()).toBe('No se pudo cargar la lista de categorías.');
  });

  it('muestra Nueva categoría y Editar solamente para ADMIN', async () => {
    http.expectOne(base).flush(categorias);
    await refrescar();

    expect(fixture.nativeElement.textContent).toContain('Nueva categoría');
    expect(fixture.nativeElement.querySelectorAll('a[href*="/editar"]').length).toBe(2);

    roles.set(['VENDEDOR']);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).not.toContain('Nueva categoría');
    expect(fixture.nativeElement.querySelector('a[href*="/editar"]')).toBeNull();
  });
});
