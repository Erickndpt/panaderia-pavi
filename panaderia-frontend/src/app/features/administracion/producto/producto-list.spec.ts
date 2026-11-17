import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthService } from '../../../core/auth/auth-service';
import { Producto } from './producto.model';
import { ProductoList } from './producto-list';

describe('ProductoList', () => {
  const productosBase = 'http://localhost:8081/api/v1/productos';
  const categoriasBase = 'http://localhost:8081/api/v1/categorias';
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

  const categorias = [
    { id: 1, nombre: 'Panes', descripcion: null },
    { id: 2, nombre: 'Bebidas', descripcion: null },
  ];

  const productos: Producto[] = [
    { id: 1, nombre: 'Croissant', precio: 2.5, categoria: { id: 1, nombre: 'Panes' } },
    { id: 2, nombre: 'Chicha', precio: 3, categoria: { id: 2, nombre: 'Bebidas' } },
  ];

  let component: ProductoList;
  let fixture: ComponentFixture<ProductoList>;
  let http: HttpTestingController;

  beforeEach(async () => {
    roles.set(['ADMIN']);

    await TestBed.configureTestingModule({
      imports: [ProductoList],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: authFake },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ProductoList);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    http.verify();
  });

  function pedirCategorias() {
    return http.expectOne(categoriasBase);
  }

  function pedirProductos() {
    return http.expectOne((peticion) => peticion.url === productosBase);
  }

  async function refrescar(): Promise<void> {
    await fixture.whenStable();
    fixture.detectChanges();
  }

  it('should create', () => {
    pedirCategorias().flush(categorias);
    pedirProductos().flush([]);
    expect(component).toBeTruthy();
  });

  it('debe cargar el catálogo y listar los productos al iniciar', async () => {
    pedirCategorias().flush(categorias);
    const req = pedirProductos();
    expect(req.request.method).toBe('GET');
    req.flush(productos);
    await refrescar();

    expect(component.productos()).toEqual(productos);
    expect(component.categorias()).toEqual(categorias);
    expect(fixture.nativeElement.querySelectorAll('tbody tr').length).toBe(2);
    expect(fixture.nativeElement.textContent).toContain('S/ 2.50');
  });

  it('debe enviar nombre y categoría como filtros del backend', async () => {
    pedirCategorias().flush(categorias);
    pedirProductos().flush(productos);
    await refrescar();

    component.actualizarNombre('croi');
    component.buscar();

    const reqNombre = pedirProductos();
    expect(reqNombre.request.params.get('nombre')).toBe('croi');
    reqNombre.flush([productos[0]]);
    await refrescar();

    component.seleccionarCategoria(2);
    const reqCategoria = pedirProductos();
    expect(reqCategoria.request.params.get('categoriaId')).toBe('2');
    reqCategoria.flush([productos[1]]);
    await refrescar();

    expect(component.productos()).toEqual([productos[1]]);
  });

  it('debe informar cuando no hay productos registrados', async () => {
    pedirCategorias().flush(categorias);
    pedirProductos().flush([]);
    await refrescar();

    expect(fixture.nativeElement.textContent).toContain('No hay productos registrados');
  });

  it('debe informar cuando la carga falla', async () => {
    pedirCategorias().flush(categorias);
    pedirProductos().flush({ message: 'fallo' }, { status: 500, statusText: 'Server Error' });
    await refrescar();

    expect(component.error()).toBe('No se pudo cargar la lista de productos.');
  });

  it('muestra Nuevo producto y Editar solamente para ADMIN', async () => {
    pedirCategorias().flush(categorias);
    pedirProductos().flush(productos);
    await refrescar();

    expect(fixture.nativeElement.textContent).toContain('Nuevo producto');
    expect(fixture.nativeElement.querySelectorAll('a[href*="/editar"]').length).toBe(2);

    roles.set(['VENDEDOR']);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).not.toContain('Nuevo producto');
    expect(fixture.nativeElement.querySelector('a[href*="/editar"]')).toBeNull();
  });
});
