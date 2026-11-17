import { Injectable, signal } from '@angular/core';

/**
 * Estrategia única de retroalimentación: el componente Alerta muestra tanto los
 * errores persistentes como los mensajes de éxito. Los formularios publican el
 * mensaje de éxito aquí antes de navegar y el listado destino lo consume.
 */
@Injectable({ providedIn: 'root' })
export class NotificacionService {
  private readonly exitoInterno = signal<string | null>(null);

  readonly exito = this.exitoInterno.asReadonly();

  mostrarExito(mensaje: string): void {
    this.exitoInterno.set(mensaje);
  }

  /** Extrae el mensaje pendiente una sola vez y lo limpia. */
  consumirExito(): string | null {
    const mensaje = this.exitoInterno();
    this.exitoInterno.set(null);
    return mensaje;
  }

  limpiar(): void {
    this.exitoInterno.set(null);
  }
}
