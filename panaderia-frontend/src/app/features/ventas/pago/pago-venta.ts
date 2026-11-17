import { DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { AuthService } from '../../../core/auth/auth-service';
import { Alerta } from '../../../shared/components/alerta';
import { ConfirmacionService } from '../../../shared/services/confirmacion-service';
import { esConflicto, esNoEncontrado, mensajeApi } from '../../../shared/utils/api-error';
import { formatearMoneda } from '../../../shared/utils/moneda';
import { nombreVendedor, VentaResponse } from '../venta/venta.model';
import { VentaService } from '../venta/venta-service';
import {
  etiquetaEstadoPago,
  etiquetaEstadoPagoVenta,
  etiquetaMetodoPago,
  EstadoPagoVenta,
  METODOS_PAGO,
  MetodoPago,
  PagoVentaRequest,
  PagoVentaResponse,
  PagoVentaResumenResponse,
} from './pago-venta.model';
import { PagoVentaService } from './pago-venta-service';

/**
 * El monto debe ser mayor que cero y admitir como máximo dos decimales,
 * igual que la validación @Digits(fraction = 2) del backend.
 */
function montoPagoValido(control: AbstractControl): ValidationErrors | null {
  const valor: unknown = control.value;

  if (valor === null || valor === undefined || valor === '') {
    return null;
  }

  const texto = String(valor).trim();
  const numero = Number(texto);

  if (!Number.isFinite(numero) || numero <= 0) {
    return { montoNoPositivo: true };
  }

  if (!/^\d+(\.\d{1,2})?$/.test(texto)) {
    return { montoDecimales: true };
  }

  return null;
}

@Component({
  imports: [
    ReactiveFormsModule,
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
  selector: 'app-pago-venta',
  styleUrl: './pago-venta.css',
  templateUrl: './pago-venta.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PagoVenta implements OnInit {
  private readonly confirmacion = inject(ConfirmacionService);
  private readonly pagoService = inject(PagoVentaService);
  private readonly ventaService = inject(VentaService);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);

  readonly ventas = signal<VentaResponse[]>([]);
  readonly ventaSeleccionada = signal<VentaResponse | null>(null);
  readonly pagos = signal<PagoVentaResponse[]>([]);
  readonly resumen = signal<PagoVentaResumenResponse | null>(null);

  readonly cargandoVentas = signal<boolean>(false);
  readonly cargandoPagos = signal<boolean>(false);
  readonly guardando = signal<boolean>(false);
  readonly anulandoId = signal<number | null>(null);

  readonly error = signal<string | null>(null);
  readonly exito = signal<string | null>(null);

  readonly metodosPago = METODOS_PAGO;

  readonly form = this.fb.nonNullable.group({
    metodoPago: ['', Validators.required],
    monto: [null as number | null, [Validators.required, montoPagoValido]],
    referencia: ['', [Validators.maxLength(100)]],
  });

  private readonly cambiosForm = toSignal(this.form.valueChanges, {
    initialValue: this.form.getRawValue(),
  });

  readonly formatearMoneda = formatearMoneda;
  readonly nombreVendedor = nombreVendedor;
  readonly etiquetaMetodo = etiquetaMetodoPago;
  readonly etiquetaEstadoPago = etiquetaEstadoPago;
  readonly etiquetaEstadoPagoVenta = etiquetaEstadoPagoVenta;

  readonly columnas = ['id', 'fecha', 'metodoPago', 'referencia', 'monto', 'estado', 'acciones'];

  /** true cuando el monto escrito supera el saldo pendiente informado por el backend. */
  readonly montoExcedeSaldo = computed(() => {
    const resumenActual = this.resumen();
    if (resumenActual === null) {
      return false;
    }

    const monto = Number(this.cambiosForm().monto);
    return Number.isFinite(monto) && monto > Number(resumenActual.saldoPendiente);
  });

  readonly ventaEsAnulada = computed(() => this.ventaSeleccionada()?.estado === 'ANULADA');

  readonly ventaEstaPagada = computed(() => this.resumen()?.estadoPago === 'PAGADO');

  readonly puedeRegistrar = computed(() => {
    this.cambiosForm();
    if (
      this.ventaSeleccionada() === null ||
      this.ventaEsAnulada() ||
      this.ventaEstaPagada() ||
      this.montoExcedeSaldo() ||
      this.guardando()
    ) {
      return false;
    }
    return this.form.valid;
  });

  readonly puedeRecargar = computed(
    () => this.ventaSeleccionada() !== null && !this.cargandoPagos(),
  );

  readonly puedeAnular = computed(() => this.auth.tieneRol('SUPERVISOR', 'ADMIN'));

  ngOnInit(): void {
    this.cargarVentas();
  }

  cargarVentas(): void {
    this.cargandoVentas.set(true);

    this.ventaService.listar().subscribe({
      next: (ventas) => {
        this.ventas.set(ventas);
        this.cargandoVentas.set(false);
        this.seleccionarDesdeQueryParam(ventas);
      },
      error: () => {
        this.ventas.set([]);
        this.cargandoVentas.set(false);
        this.error.set('No se pudo cargar la lista de ventas.');
      },
    });
  }

  /** Entrada desde el reporte de ventas: ?ventaId= selecciona esa venta automáticamente. */
  private seleccionarDesdeQueryParam(ventas: VentaResponse[]): void {
    const param = this.route.snapshot.queryParamMap.get('ventaId');

    if (param === null || this.ventaSeleccionada() !== null) {
      return;
    }

    const id = Number(param);
    const venta = ventas.find((item) => item.id === id);

    if (venta === undefined) {
      this.error.set('La venta solicitada no se encuentra en la lista de ventas.');
      return;
    }

    this.seleccionarVenta(venta);
  }

  seleccionarVenta(venta: VentaResponse | null): void {
    this.ventaSeleccionada.set(venta);
    this.error.set(null);
    this.exito.set(null);

    if (venta === null) {
      this.pagos.set([]);
      this.resumen.set(null);
      return;
    }

    this.cargarPagosYResumen();
  }

  /** Carga en paralelo el listado de pagos y su resumen de la venta seleccionada. */
  cargarPagosYResumen(): void {
    const venta = this.ventaSeleccionada();
    if (venta === null) {
      return;
    }

    this.cargandoPagos.set(true);
    this.error.set(null);

    forkJoin({
      pagos: this.pagoService.listarPorVenta(venta.id),
      resumen: this.pagoService.resumen(venta.id),
    }).subscribe({
      next: ({ pagos, resumen: resumenPagos }) => {
        this.pagos.set(pagos);
        this.resumen.set(resumenPagos);
        this.cargandoPagos.set(false);
      },
      error: () => {
        this.pagos.set([]);
        this.resumen.set(null);
        this.cargandoPagos.set(false);
        this.error.set('No se pudo cargar la información de pagos de la venta.');
      },
    });
  }

  recargar(): void {
    this.exito.set(null);
    this.cargarPagosYResumen();
  }

  registrar(): void {
    const venta = this.ventaSeleccionada();

    if (venta === null || this.guardando()) {
      return;
    }

    if (venta.estado === 'ANULADA') {
      this.error.set('La venta seleccionada está anulada; no se pueden registrar pagos.');
      return;
    }

    const resumenActual = this.resumen();
    if (resumenActual !== null && resumenActual.estadoPago === 'PAGADO') {
      this.error.set('La venta ya está pagada en su totalidad; no se admiten más pagos.');
      return;
    }

    const referencia = this.form.controls.referencia.value.trim();
    this.form.controls.referencia.setValue(referencia);

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const monto = Number(this.form.controls.monto.value);

    if (resumenActual !== null && monto > Number(resumenActual.saldoPendiente)) {
      this.error.set('El monto no puede superar el saldo pendiente de la venta.');
      return;
    }

    const request: PagoVentaRequest = {
      metodoPago: this.form.controls.metodoPago.value as MetodoPago,
      monto,
      referencia: referencia.length > 0 ? referencia : null,
    };

    this.error.set(null);
    this.exito.set(null);
    this.guardando.set(true);

    this.pagoService.registrar(venta.id, request).subscribe({
      next: (pago) => {
        this.guardando.set(false);
        this.form.reset({ metodoPago: '', monto: null, referencia: '' });
        this.exito.set(`Pago #${pago.id} registrado correctamente.`);
        this.cargarPagosYResumen();
      },
      error: (errorHttp) => {
        this.guardando.set(false);

        if (esConflicto(errorHttp)) {
          this.error.set(
            mensajeApi(errorHttp) ??
              'El pago entra en conflicto con el saldo pendiente de la venta.',
          );
          return;
        }

        if (esNoEncontrado(errorHttp)) {
          this.error.set(mensajeApi(errorHttp) ?? 'La venta ya no existe.');
          return;
        }

        this.error.set(mensajeApi(errorHttp) ?? 'No se pudo registrar el pago.');
      },
    });
  }

  anular(pago: PagoVentaResponse): void {
    if (!this.puedeAnular() || this.anulandoId() !== null) {
      return;
    }

    const detalle = [
      `Monto: ${formatearMoneda(Number(pago.monto ?? 0))}`,
      `Método: ${etiquetaMetodoPago(pago.metodoPago)}`,
    ].join('\n');

    this.confirmacion.solicitar(
      {
        titulo: `Anular pago #${pago.id}`,
        mensaje: '¿Estás seguro de anular este pago?',
        detalle: `${detalle}\n\nEl pago seguirá en el historial como anulado y dejará de contar en el saldo.`,
        textoConfirmar: 'Sí, anular pago',
        peligro: true,
      },
      () => this.confirmarAnulacion(pago),
    );
  }

  private confirmarAnulacion(pago: PagoVentaResponse): void {
    this.error.set(null);
    this.exito.set(null);
    this.anulandoId.set(pago.id);

    this.pagoService.anular(pago.id).subscribe({
      next: (pagoAnulado) => {
        this.anulandoId.set(null);
        this.exito.set(`Pago #${pagoAnulado.id} anulado correctamente.`);
        this.cargarPagosYResumen();
      },
      error: (errorHttp) => {
        this.anulandoId.set(null);

        if (esConflicto(errorHttp)) {
          this.error.set(
            mensajeApi(errorHttp) ?? 'No se pudo anular el pago por un conflicto con su estado.',
          );
          return;
        }

        if (esNoEncontrado(errorHttp)) {
          this.error.set(mensajeApi(errorHttp) ?? 'El pago ya no existe.');
          return;
        }

        this.error.set(mensajeApi(errorHttp) ?? 'No se pudo anular el pago.');
      },
    });
  }

  claseChipEstadoVenta(estado: EstadoPagoVenta): string {
    switch (estado) {
      case 'PAGADO':
        return 'chip-estado-pago chip-estado-pago--pagado';
      case 'PARCIAL':
        return 'chip-estado-pago chip-estado-pago--parcial';
      default:
        return 'chip-estado-pago chip-estado-pago--pendiente';
    }
  }

  etiquetaVenta(venta: VentaResponse): string {
    return `Venta #${venta.id} de ${venta.cliente.nombreRazonSocial} por ${formatearMoneda(
      Number(venta.total ?? 0),
    )}`;
  }
}
