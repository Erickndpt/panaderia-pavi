import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  OnDestroy,
  OnInit,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  FormArray,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { Router, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { Alerta } from '../../../shared/components/alerta';
import { NotificacionService } from '../../../shared/services/notificacion-service';
import { ConfirmacionService } from '../../../shared/services/confirmacion-service';
import {
  esConflicto,
  esNoEncontrado,
  estadoHttp,
  mensajeApi,
} from '../../../shared/utils/api-error';
import { formatearMoneda } from '../../../shared/utils/moneda';
import { normalizarTexto } from '../../../shared/utils/texto';
import { Producto } from '../../administracion/producto/producto.model';
import { ProductoService } from '../../administracion/producto/producto-service';
import { ExistenciaService } from '../../inventario/existencia/existencia-service';
import { Cliente } from '../cliente/cliente.model';
import { ClienteService } from '../cliente/cliente-service';
import { TipoVenta, VentaRequest } from './venta.model';
import { VentaService } from './venta-service';

interface LineaVentaValor {
  productoTexto: string;
  productoId: number | null;
  cantidad: number | null;
}

interface VentaFormValor {
  clienteTexto: string;
  clienteId: number | null;
  tipo: TipoVenta;
  paisDestino: string;
  detalles: LineaVentaValor[];
}

/** Estado de la consulta de existencia de un producto seleccionado en el detalle. */
type EstadoExistenciaLinea = 'sinProducto' | 'cargando' | 'disponible' | 'error' | 'sinExistencia';

interface EstadoExistencia {
  productoId: number;
  estado: EstadoExistenciaLinea;
  /** Solo tiene sentido cuando el estado es 'disponible'; la cero real incluida. */
  cantidad: number | null;
}

function numeroSeguro(valor: unknown): number {
  const numero = Number(valor);
  return Number.isFinite(numero) ? numero : 0;
}

/** La cantidad debe ser un entero mayor que cero (@Positive Integer en el backend). */
function cantidadEnteraPositiva(control: AbstractControl): ValidationErrors | null {
  const valor = control.value;

  if (valor === null || valor === undefined || valor === '') {
    return null;
  }

  const numero = Number(valor);
  if (!Number.isFinite(numero)) {
    return { numero: true };
  }

  if (!Number.isInteger(numero)) {
    return { entero: true };
  }

  return numero > 0 ? null : { positivo: true };
}

@Component({
  imports: [
    ReactiveFormsModule,
    RouterLink,
    Alerta,
    MatAutocompleteModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSelectModule,
  ],
  selector: 'app-venta-form',
  styleUrl: './venta-form.css',
  templateUrl: './venta-form.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class VentaForm implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly ventaService = inject(VentaService);
  private readonly clienteService = inject(ClienteService);
  private readonly productoService = inject(ProductoService);
  private readonly existenciaService = inject(ExistenciaService);
  private readonly notificacion = inject(NotificacionService);
  private readonly confirmacion = inject(ConfirmacionService);

  readonly saving = signal<boolean>(false);
  readonly error = signal<string | null>(null);

  readonly clientes = signal<Cliente[]>([]);
  readonly productos = signal<Producto[]>([]);
  readonly catalogosLoading = signal<boolean>(false);
  readonly catalogosError = signal<string | null>(null);

  /** Estado de la existencia consultada por productoId (una sola entrada por producto). */
  readonly existencias = signal<Map<number, EstadoExistencia>>(new Map());

  readonly tipoActual = signal<TipoVenta>('LOCAL');

  private pendientesCatalogos = 0;

  /** Protege contra respuestas tardías: cada producto solo acepta su última consulta. */
  private readonly secuenciasProducto = new Map<number, number>();
  private readonly suscripcionesExistencia = new Map<number, Subscription>();

  /** Valida que el texto coincida con un cliente activo del catálogo. */
  private readonly clienteValido = (control: AbstractControl): ValidationErrors | null => {
    const texto = normalizarTexto(String(control.value ?? ''));

    if (texto.length === 0) {
      return null;
    }

    return this.clientes().some(
      (cliente) =>
        cliente.activo &&
        (normalizarTexto(cliente.nombreRazonSocial) === texto ||
          normalizarTexto(cliente.numeroDocumento ?? '') === texto),
    )
      ? null
      : { clienteInvalido: true };
  };

  /** Valida que el texto coincida con un producto existente del catálogo. */
  private readonly productoValido = (control: AbstractControl): ValidationErrors | null => {
    const texto = normalizarTexto(String(control.value ?? ''));

    if (texto.length === 0) {
      return null;
    }

    return this.productos().some((producto) => normalizarTexto(producto.nombre) === texto)
      ? null
      : { productoInvalido: true };
  };

  /** El país es obligatorio solo cuando la venta es EXPORTACION. */
  private readonly paisValido = (control: AbstractControl): ValidationErrors | null => {
    if (this.tipoActual() === 'EXPORTACION' && String(control.value ?? '').trim().length === 0) {
      return { requerido: true };
    }

    return null;
  };

  readonly form = this.fb.group({
    clienteTexto: ['', [Validators.required, this.clienteValido]],
    clienteId: this.fb.control<number | null>(null, [Validators.required]),
    tipo: this.fb.control<TipoVenta>('LOCAL', [Validators.required]),
    paisDestino: this.fb.control<string>('', [this.paisValido, Validators.maxLength(80)]),
    detalles: this.fb.array([this.crearLinea()]),
  });

  readonly valorFormulario = signal<VentaFormValor>(this.form.getRawValue() as VentaFormValor);

  readonly formatearMoneda = formatearMoneda;

  readonly clientesDisponibles = computed(() =>
    this.clientes().filter((cliente) => cliente.activo),
  );

  readonly clientesFiltrados = computed(() => {
    const termino = normalizarTexto(this.valorFormulario().clienteTexto ?? '');
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

  readonly clienteSeleccionado = computed(() => {
    const id = this.valorFormulario().clienteId;
    return id === null ? undefined : this.clientes().find((cliente) => cliente.id === id);
  });

  readonly esExportacion = computed(() => this.valorFormulario().tipo === 'EXPORTACION');

  readonly productosFiltrados = computed(() => {
    const productos = this.productos();

    return this.valorFormulario().detalles.map((linea) => {
      const termino = normalizarTexto(linea.productoTexto ?? '');
      if (termino.length === 0) {
        return productos;
      }

      return productos.filter((producto) => normalizarTexto(producto.nombre).includes(termino));
    });
  });

  /** Total solicitado (todas las líneas) de cada producto del detalle. */
  readonly cantidadesPorProducto = computed(() => {
    const totales = new Map<number, number>();

    for (const linea of this.valorFormulario().detalles) {
      if (linea.productoId === null) {
        continue;
      }

      totales.set(
        linea.productoId,
        (totales.get(linea.productoId) ?? 0) + numeroSeguro(linea.cantidad),
      );
    }

    return totales;
  });

  readonly lineas = computed(() => {
    const productos = this.productos();
    const existencias = this.existencias();
    const totales = this.cantidadesPorProducto();

    return this.valorFormulario().detalles.map((linea, index) => {
      const cantidad = numeroSeguro(linea.cantidad);
      const producto =
        linea.productoId === null
          ? undefined
          : productos.find((item) => item.id === linea.productoId);
      const precio = producto === undefined ? 0 : numeroSeguro(producto.precio);

      const estadoExistencia: EstadoExistenciaLinea =
        producto === undefined
          ? 'sinProducto'
          : (existencias.get(producto.id)?.estado ?? 'cargando');

      const existencia =
        producto === undefined || estadoExistencia !== 'disponible'
          ? null
          : (existencias.get(producto.id)?.cantidad ?? null);

      const totalSolicitado = producto === undefined ? 0 : (totales.get(producto.id) ?? 0);
      const excedeStock =
        estadoExistencia === 'disponible' && existencia !== null && totalSolicitado > existencia;

      return {
        index,
        producto,
        cantidad,
        precio,
        subtotal: cantidad * precio,
        estadoExistencia,
        existencia,
        totalSolicitado,
        excedeStock,
      };
    });
  });

  readonly totalEstimado = computed(() =>
    this.lineas().reduce((acumulado, linea) => acumulado + linea.subtotal, 0),
  );

  /** Motivo por el cual no puede registrarse la venta relacionado con el stock, si lo hay. */
  readonly bloqueoStock = computed(() => {
    const lineas = this.lineas();

    if (lineas.some((linea) => linea.estadoExistencia === 'cargando')) {
      return 'Consultando la existencia de uno o más productos. Espere o reintente antes de registrar.';
    }

    if (lineas.some((linea) => linea.estadoExistencia === 'error')) {
      return 'No se pudo consultar la existencia de uno o más productos. Use "Reintentar carga" antes de registrar.';
    }

    if (lineas.some((linea) => linea.estadoExistencia === 'sinExistencia')) {
      return 'Uno o más productos no tienen existencia registrada en el sistema.';
    }

    const excedida = lineas.find((linea) => linea.excedeStock);
    if (excedida !== undefined && excedida.existencia !== null) {
      return `La cantidad solicitada supera la existencia disponible (${excedida.existencia}).`;
    }

    return null;
  });

  /** Todas las líneas tienen producto válido, cantidad válida y stock comprobado y suficiente. */
  readonly puedeRegistrar = computed(() => {
    this.valorFormulario();
    this.clientes();
    this.productos();

    if (this.saving() || this.catalogosLoading() || !this.form.valid) {
      return false;
    }

    const cliente = this.clienteSeleccionado();
    if (cliente === undefined || !cliente.activo) {
      return false;
    }

    return this.lineas().every(
      (linea) =>
        linea.producto !== undefined &&
        linea.estadoExistencia === 'disponible' &&
        !linea.excedeStock &&
        linea.cantidad > 0,
    );
  });

  constructor() {
    this.form.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => {
      const valor = this.form.getRawValue() as VentaFormValor;
      this.valorFormulario.set(valor);
      this.tipoActual.set(valor.tipo);
    });
  }

  get detalles(): FormArray {
    return this.form.controls.detalles;
  }

  get puedeQuitarLinea(): boolean {
    return this.detalles.length > 1;
  }

  ngOnInit(): void {
    this.cargarCatalogos();
  }

  ngOnDestroy(): void {
    for (const suscripcion of this.suscripcionesExistencia.values()) {
      suscripcion.unsubscribe();
    }
    this.suscripcionesExistencia.clear();
  }

  cargarCatalogos(): void {
    this.catalogosError.set(null);
    this.catalogosLoading.set(true);

    this.clientes.set([]);
    this.productos.set([]);
    this.existencias.set(new Map());
    this.pendientesCatalogos = 2;

    this.clienteService.listar().subscribe({
      next: (clientes) => {
        this.clientes.set(clientes);
        this.form.controls.clienteTexto.updateValueAndValidity();
        this.finalizarCarga();
      },
      error: () => {
        this.clientes.set([]);
        this.catalogosError.set(
          'No se pudo cargar el catálogo de clientes. Recárguelo antes de registrar la venta.',
        );
        this.finalizarCarga();
      },
    });

    this.productoService.listar().subscribe({
      next: (productos) => {
        this.productos.set(productos);
        this.detalles.controls.forEach((control) => {
          control.get('productoTexto')?.updateValueAndValidity();
          const productoId = control.get('productoId')?.value as number | null;
          if (productoId !== null) {
            this.consultarExistencia(productoId);
          }
        });
        this.finalizarCarga();
      },
      error: () => {
        this.productos.set([]);
        this.catalogosError.set(
          'No se pudo cargar el catálogo de productos. Recárguelo antes de registrar la venta.',
        );
        this.finalizarCarga();
      },
    });
  }

  private finalizarCarga(): void {
    this.pendientesCatalogos -= 1;
    if (this.pendientesCatalogos <= 0) {
      this.catalogosLoading.set(false);
    }
  }

  private crearLinea(): FormGroup {
    return this.fb.group({
      productoTexto: ['', [Validators.required, this.productoValido]],
      productoId: this.fb.control<number | null>(null, [Validators.required]),
      cantidad: this.fb.control<number | null>(null, [Validators.required, cantidadEnteraPositiva]),
    });
  }

  agregarLinea(): void {
    this.detalles.push(this.crearLinea());
  }

  quitarLinea(index: number): void {
    if (!this.puedeQuitarLinea) {
      return;
    }

    this.detalles.removeAt(index);
  }

  actualizarTipo(tipo: TipoVenta): void {
    this.form.controls.tipo.setValue(tipo);
    this.form.controls.tipo.markAsTouched();

    if (tipo === 'LOCAL') {
      this.form.controls.paisDestino.setValue('');
    }

    this.form.controls.paisDestino.updateValueAndValidity();
  }

  alSeleccionarCliente(cliente: Cliente): void {
    this.form.controls.clienteTexto.setValue(cliente.nombreRazonSocial);
    this.form.controls.clienteTexto.markAsTouched();
    this.form.controls.clienteId.setValue(cliente.id);
    this.form.controls.clienteId.markAsTouched();
  }

  actualizarTextoCliente(valor: string): void {
    const seleccionado = this.clienteSeleccionado();

    if (seleccionado === undefined || seleccionado.nombreRazonSocial !== valor) {
      this.form.controls.clienteId.setValue(null, { emitEvent: false });
    }
  }

  alSeleccionarProducto(index: number, producto: Producto): void {
    const grupo = this.detalles.at(index);
    grupo.get('productoTexto')?.setValue(producto.nombre);
    grupo.get('productoTexto')?.markAsTouched();
    grupo.get('productoId')?.setValue(producto.id);
    grupo.get('productoId')?.markAsTouched();

    this.consultarExistencia(producto.id);
  }

  actualizarTextoProducto(index: number, valor: string): void {
    const grupo = this.detalles.at(index);
    const id = grupo.get('productoId')?.value as number | null;
    const producto = id === null ? undefined : this.productoPorId(id);

    if (producto === undefined || producto.nombre !== valor) {
      grupo.get('productoId')?.setValue(null, { emitEvent: false });
    }
  }

  productoPorId(id: number): Producto | undefined {
    return this.productos().find((producto) => producto.id === id);
  }

  /**
   * Consulta la existencia vigente de un producto en el backend.
   * Una respuesta tardía de un producto que ya no es el último consultado se descarta.
   */
  private consultarExistencia(productoId: number): void {
    this.suscripcionesExistencia.get(productoId)?.unsubscribe();

    const secuencia = (this.secuenciasProducto.get(productoId) ?? 0) + 1;
    this.secuenciasProducto.set(productoId, secuencia);

    this.actualizarEstadoExistencia(productoId, { productoId, estado: 'cargando', cantidad: null });

    const suscripcion = this.existenciaService.obtenerPorProducto(productoId).subscribe({
      next: (existencia) => {
        if (this.secuenciasProducto.get(productoId) !== secuencia) {
          return;
        }

        this.actualizarEstadoExistencia(productoId, {
          productoId,
          estado: 'disponible',
          cantidad: existencia.cantidad,
        });
        this.suscripcionesExistencia.delete(productoId);
      },
      error: (error) => {
        if (this.secuenciasProducto.get(productoId) !== secuencia) {
          return;
        }

        this.actualizarEstadoExistencia(productoId, {
          productoId,
          estado: esNoEncontrado(error) ? 'sinExistencia' : 'error',
          cantidad: null,
        });
        this.suscripcionesExistencia.delete(productoId);
      },
    });

    this.suscripcionesExistencia.set(productoId, suscripcion);
  }

  /** Reintenta la consulta de la existencia del producto de una línea. */
  reintentarExistencia(index: number): void {
    const productoId = this.detalles.at(index).get('productoId')?.value as number | null;

    if (productoId === null) {
      return;
    }

    this.consultarExistencia(productoId);
  }

  private actualizarEstadoExistencia(productoId: number, estado: EstadoExistencia): void {
    const actualizado = new Map(this.existencias());
    actualizado.set(productoId, estado);
    this.existencias.set(actualizado);
  }

  guardar(): void {
    if (this.saving() || this.catalogosLoading()) {
      return;
    }

    this.form.markAllAsTouched();

    const cliente = this.clienteSeleccionado();
    if (cliente === undefined || !cliente.activo) {
      this.form.controls.clienteTexto.updateValueAndValidity();
      this.error.set('Seleccione un cliente activo de la lista.');
      return;
    }

    if (this.detalles.length === 0) {
      this.error.set('Agregue al menos un producto al detalle de la venta.');
      return;
    }

    const algunProductoInvalido = this.detalles.controls.some((control) => {
      const productoId = control.get('productoId')?.value as number | null;
      return productoId === null || this.productoPorId(productoId) === undefined;
    });

    if (algunProductoInvalido) {
      this.form.updateValueAndValidity();
      this.error.set('Seleccione un producto válido de la lista en cada línea del detalle.');
      return;
    }

    const motivoStock = this.bloqueoStock();
    if (motivoStock !== null) {
      this.error.set(motivoStock);
      return;
    }

    if (this.esExportacion() && (this.form.controls.paisDestino.value ?? '').trim().length === 0) {
      this.form.controls.paisDestino.updateValueAndValidity();
      this.error.set('El país de destino es obligatorio para una venta de exportación.');
      return;
    }

    if (this.form.invalid) {
      this.error.set(null);
      return;
    }

    this.confirmacion.solicitar(
      {
        titulo: 'Confirmar nueva venta',
        mensaje: 'Revisa los datos antes de registrar la venta.',
        detalle: this.construirResumen(),
        textoConfirmar: 'Registrar venta',
      },
      () => this.confirmarRegistro(),
    );
  }

  private confirmarRegistro(): void {
    const request = this.construirRequest();
    this.error.set(null);
    this.saving.set(true);

    this.ventaService.registrar(request).subscribe({
      next: () => {
        this.notificacion.mostrarExito('Venta registrada correctamente.');
        this.router.navigate(['/ventas/reporte']);
      },
      error: (error) => {
        this.saving.set(false);

        if (esConflicto(error)) {
          this.error.set(
            mensajeApi(error) ??
              'No se pudo registrar la venta por un conflicto con el estado actual.',
          );
          return;
        }

        if (esNoEncontrado(error)) {
          this.error.set(
            mensajeApi(error) ?? 'El cliente, un producto o su existencia ya no existe.',
          );
          return;
        }

        if (estadoHttp(error) === 400) {
          this.error.set(
            mensajeApi(error) ?? 'El formulario contiene datos inválidos para el registro.',
          );
          return;
        }

        this.error.set(mensajeApi(error) ?? 'No se pudo registrar la venta.');
      },
    });
  }

  construirRequest(): VentaRequest {
    const valor = this.form.getRawValue() as VentaFormValor;
    const pais = valor.paisDestino.trim();

    return {
      clienteId: Number(valor.clienteId),
      tipo: valor.tipo,
      paisDestino: valor.tipo === 'EXPORTACION' && pais.length > 0 ? pais : null,
      detalles: valor.detalles.map((linea) => ({
        productoId: Number(linea.productoId),
        cantidad: Number(linea.cantidad),
      })),
    };
  }

  private construirResumen(): string {
    const cliente = this.clienteSeleccionado();
    const lineas = this.lineas();
    const partes = [
      `Cliente: ${cliente?.nombreRazonSocial ?? ''}`,
      `Documento: ${cliente?.tipoDocumento ?? ''} ${cliente?.numeroDocumento ?? ''}`.trim(),
      `Tipo de venta: ${this.form.controls.tipo.value === 'EXPORTACION' ? 'Exportación' : 'Local'}`,
    ];

    if (this.esExportacion()) {
      partes.push(`País de destino: ${(this.form.controls.paisDestino.value ?? '').trim()}`);
    }

    partes.push(`Líneas: ${lineas.length}`);

    for (const linea of lineas) {
      const nombre = linea.producto?.nombre ?? 'Producto sin identificar';
      partes.push(
        `- ${nombre}: ${linea.cantidad} x ${formatearMoneda(linea.precio)} = ${formatearMoneda(linea.subtotal)}`,
      );
    }

    partes.push(`Total estimado: ${formatearMoneda(this.totalEstimado())}`);
    partes.push('');
    partes.push('¿Desea registrar esta venta?');

    return partes.join('\n');
  }
}
