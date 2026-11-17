import { DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { RouterLink } from '@angular/router';
import { Alerta } from '../../../shared/components/alerta';
import { formatearMoneda } from '../../../shared/utils/moneda';
import { normalizarTexto } from '../../../shared/utils/texto';
import {
  etiquetaEstadoMovimiento,
  etiquetaOrigenMovimiento,
  etiquetaTipoMovimiento,
  EstadoMovimiento,
  MovimientoFinancieroResponse,
  OrigenMovimiento,
  TipoMovimiento,
} from './movimiento-financiero.model';
import { MovimientoFinancieroService } from './movimiento-financiero-service';

type FiltroTipo = 'TODOS' | TipoMovimiento;
type FiltroOrigen = 'TODOS' | OrigenMovimiento;
type FiltroEstado = 'TODOS' | EstadoMovimiento;

@Component({
  imports: [
    RouterLink,
    Alerta,
    DatePipe,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatTableModule,
  ],
  selector: 'app-movimiento-list',
  styleUrl: './movimiento-list.css',
  templateUrl: './movimiento-list.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MovimientoList implements OnInit {
  private readonly movimientoService = inject(MovimientoFinancieroService);

  readonly movimientos = signal<MovimientoFinancieroResponse[]>([]);
  readonly loading = signal<boolean>(false);
  readonly error = signal<string | null>(null);

  /**
   * Filtros 100% visuales: el backend no recibe parámetros de búsqueda, solo
   * devuelve la lista completa de movimientos (incluidos los anulados).
   */
  readonly filtroTipo = signal<FiltroTipo>('TODOS');
  readonly filtroOrigen = signal<FiltroOrigen>('TODOS');
  readonly filtroEstado = signal<FiltroEstado>('TODOS');
  readonly filtroTexto = signal<string>('');

  readonly formatearMoneda = formatearMoneda;
  readonly etiquetaTipo = etiquetaTipoMovimiento;
  readonly etiquetaOrigen = etiquetaOrigenMovimiento;
  readonly etiquetaEstado = etiquetaEstadoMovimiento;

  readonly columnas = [
    'id',
    'fecha',
    'tipo',
    'origen',
    'origenId',
    'concepto',
    'monto',
    'estado',
  ];

  readonly movimientosFiltrados = computed(() => {
    const tipo = this.filtroTipo();
    const origen = this.filtroOrigen();
    const estado = this.filtroEstado();
    const termino = normalizarTexto(this.filtroTexto());

    return this.movimientos().filter((movimiento) => {
      if (tipo !== 'TODOS' && movimiento.tipo !== tipo) {
        return false;
      }
      if (origen !== 'TODOS' && movimiento.origen !== origen) {
        return false;
      }
      if (estado !== 'TODOS' && movimiento.estado !== estado) {
        return false;
      }
      if (termino.length > 0 && !normalizarTexto(movimiento.concepto).includes(termino)) {
        return false;
      }
      return true;
    });
  });

  readonly hayFiltros = computed(
    () =>
      this.filtroTipo() !== 'TODOS' ||
      this.filtroOrigen() !== 'TODOS' ||
      this.filtroEstado() !== 'TODOS' ||
      this.filtroTexto().trim().length > 0,
  );

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.loading.set(true);
    this.error.set(null);

    this.movimientoService.listarMovimientos().subscribe({
      next: (movimientos) => {
        this.movimientos.set(movimientos);
        this.loading.set(false);
      },
      error: () => {
        this.movimientos.set([]);
        this.loading.set(false);
        this.error.set('No se pudo cargar la lista de movimientos financieros.');
      },
    });
  }

  actualizarFiltroTipo(tipo: FiltroTipo): void {
    this.filtroTipo.set(tipo);
  }

  actualizarFiltroOrigen(origen: FiltroOrigen): void {
    this.filtroOrigen.set(origen);
  }

  actualizarFiltroEstado(estado: FiltroEstado): void {
    this.filtroEstado.set(estado);
  }

  actualizarFiltroTexto(valor: string): void {
    this.filtroTexto.set(valor);
  }

  limpiarFiltros(): void {
    this.filtroTipo.set('TODOS');
    this.filtroOrigen.set('TODOS');
    this.filtroEstado.set('TODOS');
    this.filtroTexto.set('');
  }
}
