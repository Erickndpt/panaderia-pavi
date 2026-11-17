import { Component, computed, inject, OnInit, signal } from '@angular/core';
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
import { Router, RouterLink } from '@angular/router';
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
import { Insumo } from '../../inventario/insumo/insumo.model';
import { InsumoService } from '../../inventario/insumo/insumo-service';
import { Proveedor } from '../proveedor/proveedor.model';
import { ProveedorService } from '../proveedor/proveedor-service';
import { CompraRequest } from './compra.model';
import { CompraService } from './compra-service';

interface LineaCompraValor {
  insumoTexto: string;
  insumoId: number | null;
  cantidad: number | null;
  precioUnitario: number | null;
}

interface CompraFormValor {
  proveedorTexto: string;
  proveedorId: number | null;
  detalles: LineaCompraValor[];
}

/** La cantidad debe ser mayor que cero (@Positive en el backend). */
function cantidadPositiva(control: AbstractControl): ValidationErrors | null {
  const valor = control.value;

  if (valor === null || valor === undefined || valor === '') {
    return null;
  }

  const numero = Number(valor);
  if (!Number.isFinite(numero)) {
    return { numero: true };
  }

  return numero > 0 ? null : { positivo: true };
}

/** La cantidad admite hasta 9 enteros y 3 decimales (precision 12,3). */
function cantidadValida(control: AbstractControl): ValidationErrors | null {
  const valor = control.value;

  if (valor === null || valor === undefined || valor === '') {
    return null;
  }

  const numero = Number(valor);
  if (!Number.isFinite(numero)) {
    return { numero: true };
  }

  if (numero > 999999999.999) {
    return { maximo: true };
  }

  const decimales = String(valor).split('.')[1];
  if (decimales !== undefined && decimales.length > 3) {
    return { decimales: true };
  }

  return null;
}

/** El precio no puede ser negativo (@DecimalMin("0.00") en el backend). */
function precioNoNegativo(control: AbstractControl): ValidationErrors | null {
  const valor = control.value;

  if (valor === null || valor === undefined || valor === '') {
    return null;
  }

  const numero = Number(valor);
  if (!Number.isFinite(numero)) {
    return { precio: true };
  }

  return numero >= 0 ? null : { negativo: true };
}

/** El precio admite hasta 8 enteros y 2 decimales (precision 10,2). */
function precioValido(control: AbstractControl): ValidationErrors | null {
  const valor = control.value;

  if (valor === null || valor === undefined || valor === '') {
    return null;
  }

  const numero = Number(valor);
  if (!Number.isFinite(numero)) {
    return { precio: true };
  }

  if (numero > 99999999.99) {
    return { entero: true };
  }

  const decimales = String(valor).split('.')[1];
  if (decimales !== undefined && decimales.length > 2) {
    return { decimales: true };
  }

  return null;
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
  ],
  selector: 'app-compra-form',
  styleUrl: './compra-form.css',
  templateUrl: './compra-form.html',
})
export class CompraForm implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly compraService = inject(CompraService);
  private readonly proveedorService = inject(ProveedorService);
  private readonly insumoService = inject(InsumoService);
  private readonly notificacion = inject(NotificacionService);
  private readonly confirmacion = inject(ConfirmacionService);

  readonly saving = signal<boolean>(false);
  readonly error = signal<string | null>(null);

  readonly proveedores = signal<Proveedor[]>([]);
  readonly insumos = signal<Insumo[]>([]);
  readonly catalogosLoading = signal<boolean>(false);
  readonly catalogosError = signal<string | null>(null);

  private pendientesCatalogos = 0;

  /**
   * Valida que el texto escrito coincida con un proveedor activo del catálogo.
   * Cuando el catálogo aún no ha cargado se fuerza la revalidación al recibirlo.
   */
  private readonly proveedorValido = (control: AbstractControl): ValidationErrors | null => {
    const texto = normalizarTexto(String(control.value ?? ''));

    if (texto.length === 0) {
      return null;
    }

    return this.proveedores().some(
      (proveedor) => proveedor.activo && normalizarTexto(proveedor.razonSocial) === texto,
    )
      ? null
      : { proveedorInvalido: true };
  };

  /** Valida que el texto escrito coincida con un insumo existente del catálogo. */
  private readonly insumoValido = (control: AbstractControl): ValidationErrors | null => {
    const texto = normalizarTexto(String(control.value ?? ''));

    if (texto.length === 0) {
      return null;
    }

    return this.insumos().some((insumo) => normalizarTexto(insumo.nombre) === texto)
      ? null
      : { insumoInvalido: true };
  };

  readonly form = this.fb.group({
    proveedorTexto: ['', [Validators.required, this.proveedorValido]],
    proveedorId: this.fb.control<number | null>(null, [Validators.required]),
    detalles: this.fb.array([this.crearLinea()]),
  });

  readonly valorFormulario = signal<CompraFormValor>(this.form.getRawValue() as CompraFormValor);

  readonly formatearMoneda = formatearMoneda;

  readonly proveedoresDisponibles = computed(() =>
    this.proveedores().filter((proveedor) => proveedor.activo),
  );

  readonly proveedoresFiltrados = computed(() => {
    const termino = normalizarTexto(this.valorFormulario().proveedorTexto ?? '');
    const lista = this.proveedoresDisponibles();

    if (termino.length === 0) {
      return lista;
    }

    return lista.filter(
      (proveedor) =>
        normalizarTexto(proveedor.razonSocial).includes(termino) ||
        normalizarTexto(proveedor.ruc).includes(termino),
    );
  });

  readonly proveedorSeleccionado = computed(() => {
    const id = this.valorFormulario().proveedorId;
    return id === null ? undefined : this.proveedores().find((proveedor) => proveedor.id === id);
  });

  readonly insumosFiltrados = computed(() => {
    const insumos = this.insumos();

    return this.valorFormulario().detalles.map((linea) => {
      const termino = normalizarTexto(linea.insumoTexto ?? '');
      if (termino.length === 0) {
        return insumos;
      }

      return insumos.filter((insumo) => normalizarTexto(insumo.nombre).includes(termino));
    });
  });

  readonly lineas = computed(() => {
    const insumos = this.insumos();

    return this.valorFormulario().detalles.map((linea, index) => {
      const cantidad = Number(linea.cantidad ?? 0);
      const precioUnitario = Number(linea.precioUnitario ?? 0);
      const cantidadSegura = Number.isFinite(cantidad) ? cantidad : 0;
      const precioSeguro = Number.isFinite(precioUnitario) ? precioUnitario : 0;
      const insumo =
        linea.insumoId === null ? undefined : insumos.find((item) => item.id === linea.insumoId);

      return {
        index,
        cantidad: cantidadSegura,
        precioUnitario: precioSeguro,
        subtotal: cantidadSegura * precioSeguro,
        insumo,
      };
    });
  });

  readonly totalEstimado = computed(() =>
    this.lineas().reduce((acumulado, linea) => acumulado + linea.subtotal, 0),
  );

  constructor() {
    this.form.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => {
      this.valorFormulario.set(this.form.getRawValue() as CompraFormValor);
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

  cargarCatalogos(): void {
    this.pendientesCatalogos = 0;
    this.catalogosError.set(null);
    this.catalogosLoading.set(true);

    this.proveedores.set([]);
    this.insumos.set([]);
    this.pendientesCatalogos = 2;

    this.proveedorService.listar().subscribe({
      next: (proveedores) => {
        this.proveedores.set(proveedores);
        this.form.controls.proveedorTexto.updateValueAndValidity();
        this.finalizarCarga();
      },
      error: () => {
        this.proveedores.set([]);
        this.catalogosError.set(
          'No se pudo cargar el catálogo de proveedores. Recárguelo antes de registrar la compra.',
        );
        this.finalizarCarga();
      },
    });

    this.insumoService.listar().subscribe({
      next: (insumos) => {
        this.insumos.set(insumos);
        this.detalles.controls.forEach((control) =>
          control.get('insumoTexto')?.updateValueAndValidity(),
        );
        this.finalizarCarga();
      },
      error: () => {
        this.insumos.set([]);
        this.catalogosError.set(
          'No se pudo cargar el catálogo de insumos. Recárguelo antes de registrar la compra.',
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
      insumoTexto: ['', [Validators.required, this.insumoValido]],
      insumoId: this.fb.control<number | null>(null, [Validators.required]),
      cantidad: this.fb.control<number | null>(null, [
        Validators.required,
        cantidadPositiva,
        cantidadValida,
      ]),
      precioUnitario: this.fb.control<number | null>(null, [
        Validators.required,
        precioNoNegativo,
        precioValido,
      ]),
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

  alSeleccionarProveedor(proveedor: Proveedor): void {
    this.form.controls.proveedorTexto.setValue(proveedor.razonSocial);
    this.form.controls.proveedorTexto.markAsTouched();
    this.form.controls.proveedorId.setValue(proveedor.id);
    this.form.controls.proveedorId.markAsTouched();
  }

  actualizarTextoProveedor(valor: string): void {
    const seleccionado = this.proveedorSeleccionado();

    if (seleccionado === undefined || seleccionado.razonSocial !== valor) {
      this.form.controls.proveedorId.setValue(null, { emitEvent: false });
    }
  }

  alSeleccionarInsumo(index: number, insumo: Insumo): void {
    const grupo = this.detalles.at(index);
    grupo.get('insumoTexto')?.setValue(insumo.nombre);
    grupo.get('insumoTexto')?.markAsTouched();
    grupo.get('insumoId')?.setValue(insumo.id);
    grupo.get('insumoId')?.markAsTouched();
  }

  actualizarTextoInsumo(index: number, valor: string): void {
    const grupo = this.detalles.at(index);
    const id = grupo.get('insumoId')?.value as number | null;
    const insumo = id === null ? undefined : this.insumoPorId(id);

    if (insumo === undefined || insumo.nombre !== valor) {
      grupo.get('insumoId')?.setValue(null, { emitEvent: false });
    }
  }

  insumoPorId(id: number): Insumo | undefined {
    return this.insumos().find((insumo) => insumo.id === id);
  }

  guardar(): void {
    if (this.saving() || this.catalogosLoading()) {
      return;
    }

    this.form.markAllAsTouched();

    const proveedor = this.proveedorSeleccionado();
    if (proveedor === undefined || !proveedor.activo) {
      this.form.controls.proveedorTexto.updateValueAndValidity();
      this.error.set('Seleccione un proveedor activo de la lista.');
      return;
    }

    if (this.detalles.length === 0) {
      this.error.set('Agregue al menos un insumo al detalle de la compra.');
      return;
    }

    const algunInsumoInvalido = this.detalles.controls.some((control) => {
      const insumoId = control.get('insumoId')?.value as number | null;
      return insumoId === null || this.insumoPorId(insumoId) === undefined;
    });

    if (algunInsumoInvalido) {
      this.form.updateValueAndValidity();
      this.error.set('Seleccione un insumo válido de la lista en cada línea del detalle.');
      return;
    }

    if (this.form.invalid) {
      this.error.set(null);
      return;
    }

    this.confirmacion.solicitar(
      {
        titulo: 'Confirmar nueva compra',
        mensaje: 'Revisa los datos antes de registrar la compra.',
        detalle: this.construirResumen(),
        textoConfirmar: 'Registrar compra',
      },
      () => this.confirmarRegistro(),
    );
  }

  private confirmarRegistro(): void {
    const request = this.construirRequest();
    this.error.set(null);
    this.saving.set(true);

    this.compraService.registrar(request).subscribe({
      next: () => {
        this.notificacion.mostrarExito('Compra registrada correctamente.');
        this.router.navigate(['/compras/reporte']);
      },
      error: (error) => {
        this.saving.set(false);

        if (esConflicto(error)) {
          this.error.set(
            mensajeApi(error) ??
              'No se pudo registrar la compra por un conflicto con el estado actual.',
          );
          return;
        }

        if (esNoEncontrado(error)) {
          this.error.set(mensajeApi(error) ?? 'El proveedor o un insumo ya no existe.');
          return;
        }

        if (estadoHttp(error) === 400) {
          this.error.set(
            mensajeApi(error) ?? 'El formulario contiene datos inválidos para el registro.',
          );
          return;
        }

        this.error.set(mensajeApi(error) ?? 'No se pudo registrar la compra.');
      },
    });
  }

  construirRequest(): CompraRequest {
    const valor = this.form.getRawValue() as CompraFormValor;

    return {
      proveedorId: Number(valor.proveedorId),
      detalles: valor.detalles.map((linea) => ({
        insumoId: Number(linea.insumoId),
        cantidad: Number(linea.cantidad),
        precioUnitario: Number(linea.precioUnitario),
      })),
    };
  }

  private construirResumen(): string {
    const proveedor = this.proveedorSeleccionado();
    const lineas = this.lineas();
    const partes = [
      `Proveedor: ${proveedor?.razonSocial ?? ''}`,
      `RUC: ${proveedor?.ruc ?? ''}`,
      `Detalles: ${lineas.length}`,
    ];

    for (const linea of lineas) {
      const nombre = linea.insumo?.nombre ?? 'Insumo sin identificar';
      const unidad = linea.insumo?.unidadMedida ?? '';
      partes.push(`- ${nombre} (${unidad}): ${linea.cantidad}`);
    }

    partes.push(`Total estimado: ${formatearMoneda(this.totalEstimado())}`);
    partes.push('');
    partes.push('¿Desea registrar esta compra?');

    return partes.join('\n');
  }
}
