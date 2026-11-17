import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Alerta } from './alerta';

describe('Alerta', () => {
  let component: Alerta;
  let fixture: ComponentFixture<Alerta>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Alerta],
    }).compileComponents();

    fixture = TestBed.createComponent(Alerta);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('mensaje', 'No se pudo cargar la lista de insumos.');
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('debe mostrar el mensaje recibido como alerta accesible', () => {
    const compiled = fixture.nativeElement as HTMLElement;

    expect(compiled.querySelector('[role="alert"]')).not.toBeNull();
    expect(compiled.querySelector('.alerta__texto')?.textContent).toContain(
      'No se pudo cargar la lista de insumos.',
    );
  });
});