import { DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth/auth-service';
import { Alerta } from '../../../shared/components/alerta';
import { NotificacionService } from '../../../shared/services/notificacion-service';
import { ConfirmacionService } from '../../../shared/services/confirmacion-service';
import { esConflicto, esNoEncontrado, mensajeApi } from '../../../shared/utils/api-error';
import { formatearMoneda } from '../../../shared/utils/moneda';
import { normalizarTexto } from '../../../shared/utils/texto';
import { Producto } from '../../administracion/producto/producto.model';
import { ProductoService } from '../../administracion/producto/producto-service';
import { Cliente } from '../cliente/cliente.model';
import { ClienteService } from '../cliente/cliente-service';
import {
  EstadoVenta,
  nombreVendedor,
  ReporteVentaResponse,
  TipoVenta,
  VentaFiltros,
  VentaResponse,
} from './venta.model';
import { VentaService } from './venta-service';

type FiltroTipo = 'TODAS' | TipoVenta;
type FiltroEstado = 'TODAS' | EstadoVenta;
type OrdenFecha = 'ASC' | 'DESC';

@Component({
  imports: [
    RouterLink,
    Alerta,
    DatePipe,
    MatAutocompleteModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatTableModule,
  ],
  selector: 'app-venta-reporte',
  styleUrl: './venta-reporte.css',
  templateUrl: './venta-reporte.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class VentaReporte implements OnInit {
  private readonly ventaService = inject(VentaService);
  private readonly clienteService = inject(ClienteService);
  private readonly productoService = inject(ProductoService);
  private readonly notificacion = inject(NotificacionService);
  private readonly confirmacion = inject(ConfirmacionService);
  private readonly auth = inject(AuthService);

  readonly puedeRegistrarVenta = computed(() => this.auth.tieneRol('VENDEDOR', 'SUPERVISOR'));
  readonly puedeAnular = computed(() => this.auth.tieneRol('SUPERVISOR', 'ADMIN'));
  readonly puedeVerResumen = computed(() => this.auth.tieneRol('SUPERVISOR', 'ADMIN'));

  readonly ventas = signal<VentaResponse[]>([]);
  readonly resumen = signal<ReporteVentaResponse | null>(null);
  readonly loading = signal<boolean>(false);
  readonly error = signal<string | null>(null);
  readonly exito = signal<string | null>(null);
  readonly anulandoId = signal<number | null>(null);

  readonly clientes = signal<Cliente[]>([]);
  readonly productos = signal<Producto[]>([]);

  readonly filtroTipo = signal<FiltroTipo>('TODAS');
  readonly filtroEstado = signal<FiltroEstado>('TODAS');
  readonly filtroPais = signal<string>('');
  readonly filtroProductoTexto = signal<string>('');
  readonly filtroProductoId = signal<number | null>(null);
  readonly filtroClienteTexto = signal<string>('');
  readonly filtroClienteId = signal<number | null>(null);
  readonly filtroOrden = signal<OrdenFecha>('DESC');
  readonly borradorDesde = signal<string>('');
  readonly borradorHasta = signal<string>('');
  readonly errorFechas = signal<string | null>(null);

  private readonly filtrosAplicados = signal<VentaFiltros>({ ordenFecha: 'DESC' });

  readonly formatearMoneda = formatearMoneda;
  readonly nombreVendedor = nombreVendedor;

  readonly columnasDetalle = ['producto', 'precioUnitario', 'cantidad', 'subtotal'] as const;

  readonly clientesDisponibles = computed(() =>
    this.clientes().filter((cliente) => cliente.activo),
  );

  readonly clientesFiltrados = computed(() => {
    const termino = normalizarTexto(this.filtroClienteTexto());
    const lista = this.clientesDisponibles();

    if (termino.length === 0) {
      return lista;
    }

    return lista.filter(
      (cliente) =>
        normalizarTexto(cliente.nombreRazonSocial).includes(termino) ||
        normalizarTexto(cliente.numeroDocumento ?? '').includes(termino),
    );
  });

  readonly productosFiltrados = computed(() => {
    const termino = normalizarTexto(this.filtroProductoTexto());
    const lista = this.productos();

    if (termino.length === 0) {
      return lista;
    }

    return lista.filter((producto) => normalizarTexto(producto.nombre).includes(termino));
  });

  readonly filtroClienteSeleccionado = computed(() => {
    const id = this.filtroClienteId();
    return id === null ? undefined : this.clientes().find((cliente) => cliente.id === id);
  });

  readonly filtroProductoSeleccionado = computed(() => {
    const id = this.filtroProductoId();
    return id === null ? undefined : this.productos().find((producto) => producto.id === id);
  });

  readonly hayFiltros = computed(() => {
    const filtros = this.filtrosAplicados();
    return (
      filtros.tipo !== undefined ||
      filtros.estado !== undefined ||
      filtros.fechaDesde !== undefined ||
      filtros.fechaHasta !== undefined ||
      filtros.paisDestino !== undefined ||
      filtros.productoId !== undefined ||
      filtros.clienteId !== undefined
    );
  });

  /** Cálculo visual: el backend no expone un ticket promedio. */
  readonly promedioVisual = computed(() => {
    const resumen = this.resumen();
    if (resumen === null || resumen.cantidadVentas === 0) {
      return 0;
    }
    return Number(resumen.montoTotal) / resumen.cantidadVentas;
  });

  ngOnInit(): void {
    this.exito.set(this.notificacion.consumirExito());
    this.cargarCatalogos();
    this.cargar();
  }

  cargarCatalogos(): void {
    this.clienteService.listar().subscribe({
      next: (clientes) => this.clientes.set(clientes),
      error: () => this.clientes.set([]),
    });

    this.productoService.listar().subscribe({
      next: (productos) => this.productos.set(productos),
      error: () => this.productos.set([]),
    });
  }

  cargar(): void {
    this.loading.set(true);
    this.error.set(null);

    const filtros = this.filtrosAplicados();

    this.ventaService.listar(filtros).subscribe({
      next: (ventas) => {
        this.ventas.set(ventas);
        this.loading.set(false);
      },
      error: () => {
        this.ventas.set([]);
        this.loading.set(false);
        this.error.set('No se pudo cargar el reporte de ventas.');
      },
    });

    this.cargarResumen();
  }

  private cargarResumen(): void {
    if (!this.puedeVerResumen()) {
      this.resumen.set(null);
      return;
    }

    const filtros = this.filtrosAplicados();
    this.ventaService.resumen(filtros.fechaDesde, filtros.fechaHasta).subscribe({
      next: (resumen) => this.resumen.set(resumen),
      error: () => this.resumen.set(null),
    });
  }

  private construirFiltros(): VentaFiltros {
    const tipo = this.filtroTipo();
    const estado = this.filtroEstado();
    const pais = this.filtroPais().trim();

    return {
      tipo: tipo === 'TODAS' ? undefined : tipo,
      estado: estado === 'TODAS' ? undefined : estado,
      fechaDesde: this.borradorDesde().length > 0 ? this.borradorDesde() : undefined,
      fechaHasta: this.borradorHasta().length > 0 ? this.borradorHasta() : undefined,
      paisDestino: pais.length > 0 ? pais : undefined,
      productoId: this.filtroProductoId() ?? undefined,
      clienteId: this.filtroClienteId() ?? undefined,
      ordenFecha: this.filtroOrden(),
    };
  }

  aplicar(): void {
    const desde = this.borradorDesde();
    const hasta = this.borradorHasta();

    if (desde.length > 0 && hasta.length > 0 && desde > hasta) {
      this.errorFechas.set('La fecha inicial no puede ser posterior a la fecha final.');
      return;
    }

    this.errorFechas.set(null);
    this.filtrosAplicados.set(this.construirFiltros());
    this.cargar();
  }

  limpiar(): void {
    this.filtroTipo.set('TODAS');
    this.filtroEstado.set('TODAS');
    this.filtroPais.set('');
    this.filtroProductoTexto.set('');
    this.filtroProductoId.set(null);
    this.filtroClienteTexto.set('');
    this.filtroClienteId.set(null);
    this.filtroOrden.set('DESC');
    this.borradorDesde.set('');
    this.borradorHasta.set('');
    this.errorFechas.set(null);
    this.filtrosAplicados.set({ ordenFecha: 'DESC' });
    this.cargar();
  }

  actualizarFiltroTipo(tipo: FiltroTipo): void {
    this.filtroTipo.set(tipo);
  }

  actualizarFiltroEstado(estado: FiltroEstado): void {
    this.filtroEstado.set(estado);
  }

  actualizarFiltroPais(valor: string): void {
    this.filtroPais.set(valor);
  }

  actualizarFiltroOrden(orden: OrdenFecha): void {
    this.filtroOrden.set(orden);
  }

  actualizarBorradorDesde(valor: string): void {
    this.borradorDesde.set(valor);
  }

  actualizarBorradorHasta(valor: string): void {
    this.borradorHasta.set(valor);
  }

  actualizarTextoProducto(valor: string): void {
    this.filtroProductoTexto.set(valor);
    const seleccionado = this.filtroProductoSeleccionado();
    if (seleccionado === undefined || seleccionado.nombre !== valor) {
      this.filtroProductoId.set(null);
    }
  }

  alSeleccionarProducto(producto: Producto): void {
    this.filtroProductoTexto.set(producto.nombre);
    this.filtroProductoId.set(producto.id);
  }

  actualizarTextoCliente(valor: string): void {
    this.filtroClienteTexto.set(valor);
    const seleccionado = this.filtroClienteSeleccionado();
    if (seleccionado === undefined || seleccionado.nombreRazonSocial !== valor) {
      this.filtroClienteId.set(null);
    }
  }

  alSeleccionarCliente(cliente: Cliente): void {
    this.filtroClienteTexto.set(cliente.nombreRazonSocial);
    this.filtroClienteId.set(cliente.id);
  }

  anular(venta: VentaResponse): void {
    if (this.anulandoId() !== null) {
      return;
    }

    const detalle = [
      `Cliente: ${venta.cliente.nombreRazonSocial}`,
      `Tipo: ${venta.tipo === 'EXPORTACION' ? 'Exportación' : 'Local'}`,
      `Total: ${formatearMoneda(Number(venta.total ?? 0))}`,
    ].join('\n');

    this.confirmacion.solicitar(
      {
        titulo: `Anular venta #${venta.id}`,
        mensaje: '¿Estás seguro de anular esta venta?',
        detalle: `${detalle}\n\nSe restaurarán las existencias y se anulará el ingreso financiero asociado.`,
        textoConfirmar: 'Sí, anular venta',
        peligro: true,
      },
      () => this.confirmarAnulacion(venta),
    );
  }

  private confirmarAnulacion(venta: VentaResponse): void {
    this.error.set(null);
    this.exito.set(null);
    this.anulandoId.set(venta.id);

    this.ventaService.anular(venta.id).subscribe({
      next: (actualizada) => {
        this.anulandoId.set(null);
        this.ventas.update((lista) =>
          lista.map((item) => (item.id === actualizada.id ? actualizada : item)),
        );
        this.exito.set(
          `Venta #${actualizada.id} anulada correctamente. Se restauraron las existencias.`,
        );
        this.cargarResumen();
      },
      error: (error) => {
        this.anulandoId.set(null);

        if (esConflicto(error)) {
          this.error.set(
            mensajeApi(error) ??
              'No se pudo anular la venta por un conflicto con el estado actual.',
          );
          return;
        }

        if (esNoEncontrado(error)) {
          this.error.set(mensajeApi(error) ?? 'La venta o una existencia ya no existe.');
          return;
        }

        this.error.set(mensajeApi(error) ?? 'No se pudo anular la venta.');
      },
    });
  }
}
