import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { RouterLink } from '@angular/router';
import { Alerta } from '../../../shared/components/alerta';
import { formatearMoneda } from '../../../shared/utils/moneda';
import { ResumenFinancieroResponse } from './movimiento-financiero.model';
import { MovimientoFinancieroService } from './movimiento-financiero-service';

@Component({
  imports: [RouterLink, Alerta, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  selector: 'app-reporte-financiero',
  styleUrl: './reporte-financiero.css',
  templateUrl: './reporte-financiero.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ReporteFinanciero implements OnInit {
  private readonly movimientoService = inject(MovimientoFinancieroService);

  readonly resumen = signal<ResumenFinancieroResponse | null>(null);
  readonly loading = signal<boolean>(false);
  readonly error = signal<string | null>(null);

  readonly formatearMoneda = formatearMoneda;

  /** Diferencia visual del saldo: positivo, cero o negativo. */
  readonly saldoPositivo = computed(() => Number(this.resumen()?.saldo ?? 0) > 0);
  readonly saldoNegativo = computed(() => Number(this.resumen()?.saldo ?? 0) < 0);

  readonly signoSaldo = computed(() => {
    if (this.saldoPositivo()) {
      return 'Saldo positivo';
    }
    if (this.saldoNegativo()) {
      return 'Saldo negativo';
    }
    return 'Saldo en cero';
  });

  ngOnInit(): void {
    this.cargar();
  }

  /** Usa el resumen del backend: no se recalcula desde la lista de movimientos. */
  cargar(): void {
    this.loading.set(true);
    this.error.set(null);

    this.movimientoService.obtenerResumen().subscribe({
      next: (resumen) => {
        this.resumen.set(resumen);
        this.loading.set(false);
      },
      error: () => {
        this.resumen.set(null);
        this.loading.set(false);
        this.error.set('No se pudo cargar el resumen financiero.');
      },
    });
  }
}
