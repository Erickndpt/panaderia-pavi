import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { Insumo } from './insumo.model';
import { InsumoList } from './insumo-list';

describe('InsumoList', () => {
  const base = 'http://localhost:8081/api/v1/insumos';

  const insumos: Insumo[] = [
    { id: 1, nombre: 'Harina de trigo', unidadMedida: 'KILOGRAMO', stockActual: 20, stockMinimo: 5 },
    { id: 2, nombre: 'Azúcar rubia', unidadMedida: 'KILOGRAMO', stockActual: 3, stockMinimo: 8 },
  ];

  let component: InsumoList;
  let fixture: ComponentFixture<InsumoList>;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [InsumoList],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({}) } },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(InsumoList);
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

  it('debe listar los insumos al iniciar', async () => {
    const req = http.expectOne(base);
    expect(req.request.method).toBe('GET');
    req.flush(insumos);
    await refrescar();

    expect(component.insumos()).toEqual(insumos);
    expect(fixture.nativeElement.querySelectorAll('tbody tr').length).toBe(2);
    expect(fixture.nativeElement.textContent).toContain('Harina de trigo');
  });

  it('debe informar cuando no hay insumos registrados', async () => {
    http.expectOne(base).flush([]);
    await refrescar();

    expect(fixture.nativeElement.textContent).toContain('No hay insumos registrados');
  });

  it('debe informar cuando la carga falla', async () => {
    http
      .expectOne(base)
      .flush({ message: 'fallo' }, { status: 500, statusText: 'Server Error' });
    await refrescar();

    expect(component.error()).toBe('No se pudo cargar la lista de insumos.');
  });
});