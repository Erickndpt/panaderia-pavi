import { TestBed } from '@angular/core/testing';
import { NotificacionService } from './notificacion-service';

describe('NotificacionService', () => {
  let service: NotificacionService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(NotificacionService);
  });

  it('debe iniciar sin mensaje pendiente', () => {
    expect(service.exito()).toBeNull();
  });

  it('debe publicar el mensaje de éxito', () => {
    service.mostrarExito('Categoría creada correctamente.');
    expect(service.exito()).toBe('Categoría creada correctamente.');
  });

  it('debe consumir el mensaje una sola vez', () => {
    service.mostrarExito('Insumo creado correctamente.');

    expect(service.consumirExito()).toBe('Insumo creado correctamente.');
    expect(service.exito()).toBeNull();
    expect(service.consumirExito()).toBeNull();
  });

  it('debe permitir limpiar el mensaje pendiente', () => {
    service.mostrarExito('Producto actualizado correctamente.');
    service.limpiar();
    expect(service.exito()).toBeNull();
  });
});
