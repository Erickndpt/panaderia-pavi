import { DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { RouterLink } from '@angular/router';
import { Alerta } from '../../../shared/components/alerta';
import { NotificacionService } from '../../../shared/services/notificacion-service';
import { normalizarTexto } from '../../../shared/utils/texto';
import { OrdenProduccionResponse } from './orden-produccion.model';
import { OrdenProduccionService } from './orden-produccion-service';

type FiltroEstado = 'TODAS' | 'COMPLETADA';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    Alerta,
    DatePipe,
    DecimalPipe,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatTableModule,
  ],
  selector: 'app-orden-list',
  styleUrl: './orden-list.css',
  templateUrl: './orden-list.html',
})
export class OrdenList implements OnInit {
  private readonly ordenService = inject(OrdenProduccionService);
  private readonly notificacion = inject(NotificacionService);

  readonly ordenes = signal<OrdenProduccionResponse[]>([]);
  readonly loading = signal<boolean>(false);
  readonly error = signal<string | null>(null);
  readonly exito = signal<string | null>(null);

  readonly filtroProducto = signal<string>('');
  readonly filtroEstado = signal<FiltroEstado>('TODAS');
  readonly borradorDesde = signal<string>('');
  readonly borradorHasta = signal<string>('');
  readonly desdeAplicado = signal<string>('');
  readonly hastaAplicado = signal<string>('');
  readonly errorFechas = signal<string | null>(null);

  readonly columnasDetalle = ['insumo', 'unidad', 'cantidad'] as const;

  readonly ordenesFiltradas = computed(() => {
    const producto = normalizarTexto(this.filtroProducto());
    const estado = this.filtroEstado();
    const desde = this.desdeAplicado();
    const hasta = this.hastaAplicado();
    const desdeMs = desde.length > 0 ? new Date(`${desde}T00:00:00`).getTime() : null;
    const hastaMs = hasta.length > 0 ? new Date(`${hasta}T23:59:59.999`).getTime() : null;

    return this.ordenes().filter((orden) => {
      if (producto.length > 0 && !normalizarTexto(orden.nombreProducto).includes(producto)) {
        return false;
      }

      if (estado !== 'TODAS' && orden.estado !== estado) {
        return false;
      }

      const ms = new Date(orden.fecha).getTime();
      if (Number.isNaN(ms)) {
        return true;
      }

      if (desdeMs !== null && ms < desdeMs) {
        return false;
      }

      if (hastaMs !== null && ms > hastaMs) {
        return false;
      }

      return true;
    });
  });

  readonly resumen = computed(() => {
    const filtradas = this.ordenesFiltradas();
    const productos = new Set(filtradas.map((orden) => orden.productoId));
    const lineas = filtradas.reduce((acumulado, orden) => acumulado + orden.detalles.length, 0);
    const unicoProducto = productos.size === 1;
    const cantidadProducida = unicoProducto
      ? filtradas.reduce((acumulado, orden) => acumulado + Number(orden.cantidadProducida ?? 0), 0)
      : 0;

    return {
      ordenes: filtradas.length,
      productos: productos.size,
      lineas,
      unicoProducto,
      cantidadProducida,
      nombreProducto: unicoProducto ? (filtradas[0]?.nombreProducto ?? null) : null,
    };
  });

  ngOnInit(): void {
    this.exito.set(this.notificacion.consumirExito());
    this.cargar();
  }

  cargar(): void {
    this.loading.set(true);
    this.error.set(null);

    this.ordenService.listar().subscribe({
      next: (ordenes) => {
        this.ordenes.set(ordenes);
        this.loading.set(false);
      },
      error: () => {
        this.ordenes.set([]);
        this.loading.set(false);
        this.error.set('No se pudo cargar la lista de órdenes de producción.');
      },
    });
  }

  actualizarFiltroProducto(valor: string): void {
    this.filtroProducto.set(valor);
  }

  actualizarFiltroEstado(estado: FiltroEstado): void {
    this.filtroEstado.set(estado);
  }

  actualizarBorradorDesde(valor: string): void {
    this.borradorDesde.set(valor);
  }

  actualizarBorradorHasta(valor: string): void {
    this.borradorHasta.set(valor);
  }

  aplicarFechas(): void {
    const desde = this.borradorDesde();
    const hasta = this.borradorHasta();

    if (desde.length > 0 && hasta.length > 0 && desde > hasta) {
      this.errorFechas.set('La fecha inicial no puede ser posterior a la fecha final.');
      return;
    }

    this.errorFechas.set(null);
    this.desdeAplicado.set(desde);
    this.hastaAplicado.set(hasta);
  }

  limpiarFechas(): void {
    this.borradorDesde.set('');
    this.borradorHasta.set('');
    this.desdeAplicado.set('');
    this.hastaAplicado.set('');
    this.errorFechas.set(null);
  }
}
