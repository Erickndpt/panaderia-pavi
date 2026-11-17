import { DecimalPipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
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
import { normalizarTexto } from '../../../shared/utils/texto';
import { Producto } from '../../administracion/producto/producto.model';
import { ProductoService } from '../../administracion/producto/producto-service';
import { ExistenciaResponse } from '../../inventario/existencia/existencia.model';
import { ExistenciaService } from '../../inventario/existencia/existencia-service';
import { Insumo } from '../../inventario/insumo/insumo.model';
import { InsumoService } from '../../inventario/insumo/insumo-service';
import { OrdenProduccionRequest } from './orden-produccion.model';
import { OrdenProduccionService } from './orden-produccion-service';

interface LineaConsumoValor {
  insumoTexto: string;
  insumoId: number | null;
  cantidadConsumida: number | null;
}

interface OrdenFormValor {
  productoTexto: string;
  productoId: number | null;
  cantidadProducida: number | null;
  insumos: LineaConsumoValor[];
}

/** Las cantidades deben ser mayores que cero (@Positive en el backend). */
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

/** Las cantidades admiten hasta 9 enteros y 3 decimales (@Digits(9,3)). */
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

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    Alerta,
    DecimalPipe,
    MatAutocompleteModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
  ],
  selector: 'app-orden-form',
  styleUrl: './orden-form.css',
  templateUrl: './orden-form.html',
})
export class OrdenForm implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly ordenService = inject(OrdenProduccionService);
  private readonly productoService = inject(ProductoService);
  private readonly insumoService = inject(InsumoService);
  private readonly existenciaService = inject(ExistenciaService);
  private readonly notificacion = inject(NotificacionService);
  private readonly confirmacion = inject(ConfirmacionService);

  readonly saving = signal<boolean>(false);
  readonly error = signal<string | null>(null);

  readonly productos = signal<Producto[]>([]);
  readonly insumos = signal<Insumo[]>([]);
  readonly existencias = signal<ExistenciaResponse[]>([]);
  readonly catalogosLoading = signal<boolean>(false);
  readonly catalogosError = signal<string | null>(null);

  private pendientesCatalogos = 0;

  /** Valida que el texto escrito coincida con un producto existente del catálogo. */
  private readonly productoValido = (control: AbstractControl): ValidationErrors | null => {
    const texto = normalizarTexto(String(control.value ?? ''));

    if (texto.length === 0) {
      return null;
    }

    return this.productos().some((producto) => normalizarTexto(producto.nombre) === texto)
      ? null
      : { productoInvalido: true };
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

  /** El backend prohíbe insumos repetidos; se replica en el FormArray. */
  private readonly sinInsumosDuplicados = (control: AbstractControl): ValidationErrors | null => {
    const array = control as FormArray;
    const ids = array.controls
      .map((linea) => linea.get('insumoId')?.value)
      .filter((id): id is number => typeof id === 'number');

    return new Set(ids).size === ids.length ? null : { insumoDuplicado: true };
  };

  readonly form = this.fb.group({
    productoTexto: ['', [Validators.required, this.productoValido]],
    productoId: this.fb.control<number | null>(null, [Validators.required]),
    cantidadProducida: this.fb.control<number | null>(null, [
      Validators.required,
      cantidadPositiva,
      cantidadValida,
    ]),
    insumos: this.fb.array([this.crearLinea()], {
      validators: [this.sinInsumosDuplicados],
    }),
  });

  readonly valorFormulario = signal<OrdenFormValor>(this.form.getRawValue() as OrdenFormValor);

  readonly productosFiltrados = computed(() => {
    const termino = normalizarTexto(this.valorFormulario().productoTexto ?? '');
    const lista = this.productos();

    if (termino.length === 0) {
      return lista;
    }

    return lista.filter((producto) => normalizarTexto(producto.nombre).includes(termino));
  });

  readonly productoSeleccionado = computed(() => {
    const id = this.valorFormulario().productoId;
    return id === null ? undefined : this.productos().find((producto) => producto.id === id);
  });

  readonly insumosFiltrados = computed(() => {
    const insumos = this.insumos();

    return this.valorFormulario().insumos.map((linea) => {
      const termino = normalizarTexto(linea.insumoTexto ?? '');
      if (termino.length === 0) {
        return insumos;
      }

      return insumos.filter((insumo) => normalizarTexto(insumo.nombre).includes(termino));
    });
  });

  readonly cantidadProducidaNum = computed(() => {
    const valor = Number(this.valorFormulario().cantidadProducida ?? 0);
    return Number.isFinite(valor) ? valor : 0;
  });

  readonly lineas = computed(() => {
    const insumos = this.insumos();
    const producida = this.cantidadProducidaNum();

    return this.valorFormulario().insumos.map((linea, index) => {
      const cantidadConsumida = Number(linea.cantidadConsumida ?? 0);
      const cantidadSegura = Number.isFinite(cantidadConsumida) ? cantidadConsumida : 0;
      const insumo =
        linea.insumoId === null ? undefined : insumos.find((item) => item.id === linea.insumoId);

      return {
        index,
        cantidadConsumida: cantidadSegura,
        insumo,
        consumoPorUnidad: producida > 0 ? cantidadSegura / producida : 0,
        stockPosterior: insumo === undefined ? null : Number(insumo.stockActual) - cantidadSegura,
      };
    });
  });

  readonly tiposInsumos = computed(() => {
    const ids = this.valorFormulario()
      .insumos.map((linea) => linea.insumoId)
      .filter((id): id is number => id !== null);
    return new Set(ids).size;
  });

  readonly lineasValidas = computed(
    () =>
      this.lineas().filter((linea) => linea.insumo !== undefined && linea.cantidadConsumida > 0)
        .length,
  );

  /** Agrupa el consumo por unidad de medida; nunca suma unidades incompatibles. */
  readonly resumenPorUnidad = computed(() => {
    const mapa = new Map<string, number>();

    for (const linea of this.lineas()) {
      if (linea.insumo === undefined || linea.cantidadConsumida <= 0) {
        continue;
      }

      const unidad = linea.insumo.unidadMedida;
      mapa.set(unidad, (mapa.get(unidad) ?? 0) + linea.cantidadConsumida);
    }

    return Array.from(mapa, ([unidad, total]) => ({ unidad, total }));
  });

  constructor() {
    this.form.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => {
      this.valorFormulario.set(this.form.getRawValue() as OrdenFormValor);
    });
  }

  get detalles(): FormArray {
    return this.form.controls.insumos;
  }

  get puedeQuitarLinea(): boolean {
    return this.detalles.length > 1;
  }

  ngOnInit(): void {
    this.cargarCatalogos();
  }

  cargarCatalogos(): void {
    this.catalogosError.set(null);
    this.catalogosLoading.set(true);
    this.productos.set([]);
    this.insumos.set([]);
    this.existencias.set([]);
    this.pendientesCatalogos = 3;

    this.productoService.listar().subscribe({
      next: (productos) => {
        this.productos.set(productos);
        this.form.controls.productoTexto.updateValueAndValidity();
        this.finalizarCarga();
      },
      error: () => {
        this.productos.set([]);
        this.catalogosError.set(
          'No se pudo cargar el catálogo de productos. Recárguelo antes de registrar la orden.',
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
          'No se pudo cargar el catálogo de insumos. Recárguelo antes de registrar la orden.',
        );
        this.finalizarCarga();
      },
    });

    this.existenciaService.listar().subscribe({
      next: (existencias) => {
        this.existencias.set(existencias);
        this.finalizarCarga();
      },
      error: () => {
        this.existencias.set([]);
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
      cantidadConsumida: this.fb.control<number | null>(null, [
        Validators.required,
        cantidadPositiva,
        cantidadValida,
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
    this.detalles.updateValueAndValidity();
  }

  alSeleccionarProducto(producto: Producto): void {
    this.form.controls.productoTexto.setValue(producto.nombre);
    this.form.controls.productoTexto.markAsTouched();
    this.form.controls.productoId.setValue(producto.id);
    this.form.controls.productoId.markAsTouched();
  }

  actualizarTextoProducto(valor: string): void {
    const seleccionado = this.productoSeleccionado();

    if (seleccionado === undefined || seleccionado.nombre !== valor) {
      this.form.controls.productoId.setValue(null, { emitEvent: false });
    }
  }

  alSeleccionarInsumo(index: number, insumo: Insumo): void {
    const grupo = this.detalles.at(index);
    grupo.get('insumoTexto')?.setValue(insumo.nombre);
    grupo.get('insumoTexto')?.markAsTouched();
    grupo.get('insumoId')?.setValue(insumo.id);
    grupo.get('insumoId')?.markAsTouched();
    this.detalles.updateValueAndValidity();
  }

  actualizarTextoInsumo(index: number, valor: string): void {
    const grupo = this.detalles.at(index);
    const id = grupo.get('insumoId')?.value as number | null;
    const insumo = id === null ? undefined : this.insumoPorId(id);

    if (insumo === undefined || insumo.nombre !== valor) {
      grupo.get('insumoId')?.setValue(null, { emitEvent: false });
      this.detalles.updateValueAndValidity();
    }
  }

  insumoPorId(id: number): Insumo | undefined {
    return this.insumos().find((insumo) => insumo.id === id);
  }

  existenciaDe(productoId: number): number | null {
    const existencia = this.existencias().find((item) => item.producto.id === productoId);
    return existencia === undefined ? null : Number(existencia.cantidad);
  }

  guardar(): void {
    if (this.saving() || this.catalogosLoading()) {
      return;
    }

    this.form.markAllAsTouched();

    const producto = this.productoSeleccionado();
    if (producto === undefined) {
      this.form.controls.productoTexto.updateValueAndValidity();
      this.error.set('Seleccione un producto de la lista.');
      return;
    }

    if (this.detalles.length === 0) {
      this.error.set('Agregue al menos un insumo consumido al detalle.');
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

    if (this.detalles.hasError('insumoDuplicado')) {
      this.error.set('Cada insumo puede aparecer una sola vez en el detalle de consumo.');
      return;
    }

    if (this.form.invalid) {
      this.error.set(null);
      return;
    }

    this.confirmacion.solicitar(
      {
        titulo: 'Confirmar orden de producción',
        mensaje: 'Revisa la producción y el consumo estimado antes de continuar.',
        detalle: this.construirResumen(),
        textoConfirmar: 'Registrar orden',
      },
      () => this.confirmarRegistro(),
    );
  }

  private confirmarRegistro(): void {
    const request = this.construirRequest();
    this.error.set(null);
    this.saving.set(true);

    this.ordenService.registrar(request).subscribe({
      next: () => {
        this.notificacion.mostrarExito(
          'Orden de producción registrada correctamente. El inventario fue actualizado.',
        );
        this.router.navigate(['/produccion/ordenes']);
      },
      error: (error) => {
        this.saving.set(false);

        if (esConflicto(error)) {
          this.error.set(
            mensajeApi(error) ??
              'No se pudo registrar la orden por un conflicto con el inventario.',
          );
          return;
        }

        if (esNoEncontrado(error)) {
          this.error.set(mensajeApi(error) ?? 'El producto o un insumo seleccionado ya no existe.');
          return;
        }

        if (estadoHttp(error) === 400) {
          this.error.set(mensajeApi(error) ?? 'La solicitud de orden de producción no es válida.');
          return;
        }

        this.error.set(mensajeApi(error) ?? 'No se pudo registrar la orden de producción.');
      },
    });
  }

  construirRequest(): OrdenProduccionRequest {
    const valor = this.form.getRawValue() as OrdenFormValor;

    return {
      productoId: Number(valor.productoId),
      cantidadProducida: Number(valor.cantidadProducida),
      insumos: valor.insumos.map((linea) => ({
        insumoId: Number(linea.insumoId),
        cantidadConsumida: Number(linea.cantidadConsumida),
      })),
    };
  }

  private construirResumen(): string {
    const producto = this.productoSeleccionado();
    const lineas = this.lineas();
    const partes = [
      `Producto: ${producto?.nombre ?? ''}`,
      `Cantidad a producir: ${this.cantidadProducidaNum()}`,
      '',
      'Consumos:',
    ];

    for (const linea of lineas) {
      const nombre = linea.insumo?.nombre ?? 'Insumo sin identificar';
      const unidad = linea.insumo?.unidadMedida ?? '';
      const stock = linea.insumo === undefined ? '—' : String(linea.insumo.stockActual);
      const posterior = linea.stockPosterior === null ? '—' : String(linea.stockPosterior);
      partes.push(
        `- ${nombre}: ${linea.cantidadConsumida} ${unidad}; stock actual: ${stock}; stock posterior estimado: ${posterior}`,
      );
    }

    partes.push('');
    partes.push('¿Desea registrar la orden de producción?');

    return partes.join('\n');
  }
}
