import { Component, inject, OnInit, signal } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatOptionModule } from '@angular/material/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ProductoResumen } from '../../administracion/producto/producto.model';
import { ProductoService } from '../../administracion/producto/producto-service';
import { Alerta } from '../../../shared/components/alerta';
import { NotificacionService } from '../../../shared/services/notificacion-service';
import { esConflicto, esNoEncontrado, mensajeApi } from '../../../shared/utils/api-error';
import { ExistenciaRequest } from './existencia.model';
import { ExistenciaService } from './existencia-service';

const PRODUCTO_AUSENTE =
  'El producto seleccionado ya no existe. Recargue los productos y seleccione otro';
/** Valida el máximo de tres decimales de la columna NUMBER(12,3). */
function maximoTresDecimales(control: AbstractControl): ValidationErrors | null {
  const valor = control.value;

  if (valor === null || valor === undefined || valor === '') {
    return null;
  }

  const decimales = String(valor).split('.')[1];
  if (decimales !== undefined && decimales.length > 3) {
    return { decimales: true };
  }

  return null;
}

@Component({
  imports: [
    ReactiveFormsModule,
    RouterLink,
    Alerta,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatOptionModule,
    MatProgressSpinnerModule,
    MatSelectModule,
  ],
  selector: 'app-existencia-form',
  styleUrl: './existencia-form.css',
  templateUrl: './existencia-form.html',
})
export class ExistenciaForm implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly existenciaService = inject(ExistenciaService);
  private readonly productoService = inject(ProductoService);
  private readonly notificacion = inject(NotificacionService);

  readonly id = signal<number | null>(null);
  readonly productos = signal<ProductoResumen[]>([]);
  readonly loading = signal<boolean>(false);
  readonly productosLoading = signal<boolean>(false);
  readonly saving = signal<boolean>(false);
  readonly error = signal<string | null>(null);
  readonly productosError = signal<string | null>(null);

  /** El backend respondió 404: el producto dejó de existir mientras se editaba. */
  readonly productoAusente = signal<boolean>(false);

  /** Id del producto a preseleccionar; se reintenta cuando llega el catálogo. */
  private readonly productoPendiente = signal<number | null>(null);

  readonly form = this.fb.group({
    productoId: this.fb.control<number | null>(null, [Validators.required]),
    cantidad: this.fb.control<number | null>(null, [
      Validators.required,
      Validators.min(0),
      maximoTresDecimales,
    ]),
  });

  get esEdicion(): boolean {
    return this.id() !== null;
  }

  ngOnInit(): void {
    this.cargarProductos();

    const param = this.route.snapshot.paramMap.get('id');
    if (param === null) {
      return;
    }

    const id = Number(param);
    if (!Number.isInteger(id) || id <= 0) {
      this.error.set('El identificador de la existencia no es válido.');
      return;
    }

    this.id.set(id);
    this.cargarExistencia(id);
  }

  cargarProductos(): void {
    this.productosLoading.set(true);
    this.productosError.set(null);

    this.productoService.listar().subscribe({
      next: (productos) => {
        this.productos.set(productos);
        this.productosLoading.set(false);
        this.aplicarProductoPendiente();
      },
      error: () => {
        this.productos.set([]);
        this.productosLoading.set(false);
        this.productosError.set(
          'No se pudo cargar el catálogo de productos. Intente recargarlo antes de guardar.',
        );
      },
    });
  }

  private cargarExistencia(id: number): void {
    this.loading.set(true);
    this.error.set(null);

    this.existenciaService.obtener(id).subscribe({
      next: (existencia) => {
        this.productoPendiente.set(existencia.producto.id);
        this.form.patchValue({
          productoId: existencia.producto.id,
          cantidad: existencia.cantidad,
        });
        this.loading.set(false);
        this.aplicarProductoPendiente();
      },
      error: () => {
        this.loading.set(false);
        this.error.set('No se pudo cargar la existencia.');
      },
    });
  }

  /**
   * Reaplica la preselección del producto. Es idempotente: da igual si el catálogo
   * o la existencia llegan primero, porque el valor guardado es siempre un number.
   */
  private aplicarProductoPendiente(): void {
    const pendiente = this.productoPendiente();

    if (pendiente === null || this.productos().length === 0) {
      return;
    }

    const yaExiste = this.productos().some((producto) => producto.id === pendiente);
    if (!yaExiste || this.form.controls.productoId.value !== pendiente) {
      this.form.controls.productoId.setValue(pendiente);
    }
  }

  guardar(): void {
    if (this.loading() || this.saving()) {
      return;
    }

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const valor = this.form.getRawValue();
    const request: ExistenciaRequest = {
      productoId: Number(valor.productoId),
      cantidad: Number(valor.cantidad),
    };

    const id = this.id();
    this.error.set(null);
    this.productoAusente.set(false);
    this.saving.set(true);

    const respuesta$ =
      id !== null ? this.existenciaService.actualizar(id, request) : this.existenciaService.crear(request);

    respuesta$.subscribe({
      next: () => {
        this.notificacion.mostrarExito(
          id !== null
            ? 'Existencia actualizada correctamente.'
            : 'Existencia creada correctamente.',
        );
        this.router.navigate(['/inventario/existencias']);
      },
      error: (error) => {
        this.saving.set(false);
        if (esNoEncontrado(error)) {
          this.productoAusente.set(true);
          this.error.set(PRODUCTO_AUSENTE);
          return;
        }
        if (esConflicto(error)) {
          this.error.set(
            mensajeApi(error) ?? 'El producto seleccionado ya tiene una existencia registrada.',
          );
          return;
        }
        this.error.set('No se pudo guardar la existencia.');
      },
    });
  }

  recargarProductos(): void {
    this.productoAusente.set(false);
    this.cargarProductos();
  }
}
