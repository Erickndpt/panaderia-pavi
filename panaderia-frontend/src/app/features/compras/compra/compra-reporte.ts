import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { RouterLink } from '@angular/router';
import { Alerta } from '../../../shared/components/alerta';
import { ConfirmacionService } from '../../../shared/services/confirmacion-service';
import { esConflicto, esNoEncontrado, mensajeApi } from '../../../shared/utils/api-error';
import { formatearMoneda } from '../../../shared/utils/moneda';
import { Proveedor } from '../proveedor/proveedor.model';
import { ProveedorService } from '../proveedor/proveedor-service';
import { CompraResponse } from './compra.model';
import { CompraService } from './compra-service';

type FiltroEstado = 'TODAS' | 'REGISTRADA' | 'ANULADA';

@Component({
  imports: [
    RouterLink,
    Alerta,
    DatePipe,
    DecimalPipe,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatTableModule,
  ],
  selector: 'app-compra-reporte',
  styleUrl: './compra-reporte.css',
  templateUrl: './compra-reporte.html',
})
export class CompraReporte implements OnInit {
  private readonly compraService = inject(CompraService);
  private readonly proveedorService = inject(ProveedorService);
  private readonly confirmacion = inject(ConfirmacionService);

  readonly compras = signal<CompraResponse[]>([]);
  readonly proveedores = signal<Proveedor[]>([]);
  readonly loading = signal<boolean>(false);
  readonly error = signal<string | null>(null);
  readonly exito = signal<string | null>(null);
  readonly anulandoId = signal<number | null>(null);

  readonly filtroEstado = signal<FiltroEstado>('TODAS');
  readonly borradorDesde = signal<string>('');
  readonly borradorHasta = signal<string>('');
  readonly desdeAplicado = signal<string>('');
  readonly hastaAplicado = signal<string>('');
  readonly errorFechas = signal<string | null>(null);

  readonly formatearMoneda = formatearMoneda;

  readonly columnasDetalle = [
    'insumo',
    'unidad',
    'cantidad',
    'precioUnitario',
    'subtotal',
  ] as const;

  readonly proveedoresPorId = computed(
    () => new Map(this.proveedores().map((proveedor) => [proveedor.id, proveedor])),
  );

  readonly comprasFiltradas = computed(() => {
    const estado = this.filtroEstado();
    const desde = this.desdeAplicado();
    const hasta = this.hastaAplicado();
    const desdeMs = desde.length > 0 ? new Date(`${desde}T00:00:00`).getTime() : null;
    const hastaMs = hasta.length > 0 ? new Date(`${hasta}T23:59:59.999`).getTime() : null;

    return this.compras().filter((compra) => {
      if (estado !== 'TODAS' && compra.estado !== estado) {
        return false;
      }

      const ms = new Date(compra.fecha).getTime();
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
    const filtradas = this.comprasFiltradas();
    const registradas = filtradas.filter((compra) => compra.estado === 'REGISTRADA');
    const montoVigente = registradas.reduce(
      (acumulado, compra) => acumulado + Number(compra.total ?? 0),
      0,
    );

    return {
      cantidad: filtradas.length,
      registradas: registradas.length,
      montoVigente,
      promedio: registradas.length === 0 ? 0 : montoVigente / registradas.length,
    };
  });

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.loading.set(true);
    this.error.set(null);

    this.compraService.listar().subscribe({
      next: (compras) => {
        this.compras.set(compras);
        this.loading.set(false);
      },
      error: () => {
        this.compras.set([]);
        this.loading.set(false);
        this.error.set('No se pudo cargar el reporte de compras.');
      },
    });

    this.proveedorService.listar().subscribe({
      next: (proveedores) => this.proveedores.set(proveedores),
      error: () => this.proveedores.set([]),
    });
  }

  rucDe(proveedorId: number): string {
    return this.proveedoresPorId().get(proveedorId)?.ruc ?? '—';
  }

  monto(valor: number | null | undefined): number {
    return Number(valor ?? 0);
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

  anular(compra: CompraResponse): void {
    if (this.anulandoId() !== null) {
      return;
    }

    const detalle = [
      `Proveedor: ${compra.proveedor.razonSocial}`,
      `Total: ${formatearMoneda(Number(compra.total ?? 0))}`,
    ].join('\n');

    this.confirmacion.solicitar(
      {
        titulo: `Anular compra #${compra.id}`,
        mensaje: '¿Estás seguro de anular esta compra?',
        detalle: `${detalle}\n\nSe revertirá el stock de los insumos y se registrará el ajuste financiero.`,
        textoConfirmar: 'Sí, anular compra',
        peligro: true,
      },
      () => this.confirmarAnulacion(compra),
    );
  }

  private confirmarAnulacion(compra: CompraResponse): void {
    this.error.set(null);
    this.exito.set(null);
    this.anulandoId.set(compra.id);

    this.compraService.anular(compra.id).subscribe({
      next: (actualizada) => {
        this.anulandoId.set(null);
        this.compras.update((lista) =>
          lista.map((item) => (item.id === actualizada.id ? actualizada : item)),
        );
        this.exito.set(`Compra #${actualizada.id} anulada correctamente.`);
      },
      error: (error) => {
        this.anulandoId.set(null);

        if (esConflicto(error)) {
          this.error.set(
            mensajeApi(error) ??
              'No se pudo anular la compra por un conflicto con el estado actual.',
          );
          return;
        }

        if (esNoEncontrado(error)) {
          this.error.set(mensajeApi(error) ?? 'La compra ya no existe.');
          return;
        }

        this.error.set(mensajeApi(error) ?? 'No se pudo anular la compra.');
      },
    });
  }
}
