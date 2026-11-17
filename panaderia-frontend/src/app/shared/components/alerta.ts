import { Component, computed, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

export type TipoAlerta = 'error' | 'exito';

@Component({
  imports: [MatIconModule],
  selector: 'app-alerta',
  styleUrl: './alerta.css',
  templateUrl: './alerta.html',
})
export class Alerta {
  readonly mensaje = input.required<string>();
  readonly tipo = input<TipoAlerta>('error');

  readonly icono = computed(() => (this.tipo() === 'exito' ? 'check_circle' : 'error'));
}
