import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { NoEncontrado } from './no-encontrado';

describe('NoEncontrado', () => {
  let fixture: ComponentFixture<NoEncontrado>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NoEncontrado],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(NoEncontrado);
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('debe explicar el error y ofrecer volver al inicio', () => {
    const compiled = fixture.nativeElement as HTMLElement;

    expect(compiled.querySelector('.no-encontrado__titulo')?.textContent).toContain(
      'Página no encontrada',
    );
    const enlace = compiled.querySelector('a') as HTMLAnchorElement | null;
    expect(enlace?.getAttribute('href')).toBe('/');
  });
});
