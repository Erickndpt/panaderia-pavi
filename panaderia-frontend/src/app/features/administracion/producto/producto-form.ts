import { Component, computed, inject, OnInit, signal } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
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
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Alerta } from '../../../shared/components/alerta';
import { NotificacionService } from '../../../shared/services/notificacion-service';
import { esNoEncontrado, mensajeApi } from '../../../shared/utils/api-error';
import { normalizarTexto } from '../../../shared/utils/texto';
import { Categoria } from '../categoria/categoria.model';
import { CategoriaService } from '../categoria/categoria-service';
import { ProductoRequest } from './producto.model';
import { ProductoService } from './producto-service';

/** El backend exige 8 enteros y 2 decimales como máximo en el precio. */
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
  selector: 'app-producto-form',
  styleUrl: './producto-form.css',
  templateUrl: './producto-form.html',
})
export class ProductoForm implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly productoService = inject(ProductoService);
  private readonly categoriaService = inject(CategoriaService);
  private readonly notificacion = inject(NotificacionService);

  readonly id = signal<number | null>(null);
  readonly loading = signal<boolean>(false);
  readonly saving = signal<boolean>(false);
  readonly error = signal<string | null>(null);

  readonly categorias = signal<Categoria[]>([]);
  readonly categoriasLoading = signal<boolean>(false);
  readonly categoriasError = signal<string | null>(null);
  readonly textoBusqueda = signal<string>('');

  /**
   * Valida que el texto escrito coincida con una categoría existente. Cuando el
   * catálogo aún no ha cargado se fuerza la revalidación al recibirlo.
   */
  private readonly categoriaValida = (control: AbstractControl): ValidationErrors | null => {
    const texto = normalizarTexto(String(control.value ?? ''));

    if (texto.length === 0) {
      return null;
    }

    return this.categorias().some((categoria) => normalizarTexto(categoria.nombre) === texto)
      ? null
      : { categoriaInvalida: true };
  };

  readonly form = this.fb.group({
    nombre: ['', [Validators.required, Validators.maxLength(120)]],
    precio: this.fb.control<number | null>(null, [
      Validators.required,
      Validators.min(0),
      precioValido,
    ]),
    categoriaTexto: ['', [Validators.required, this.categoriaValida]],
  });

  readonly categoriasFiltradas = computed(() => {
    const termino = normalizarTexto(this.textoBusqueda());
    const lista = this.categorias();

    if (termino.length === 0) {
      return lista;
    }

    return lista.filter((categoria) => normalizarTexto(categoria.nombre).includes(termino));
  });

  get esEdicion(): boolean {
    return this.id() !== null;
  }

  ngOnInit(): void {
    this.cargarCategorias();

    const param = this.route.snapshot.paramMap.get('id');
    if (param === null) {
      return;
    }

    const id = Number(param);
    if (!Number.isInteger(id) || id <= 0) {
      this.error.set('El identificador del producto no es válido.');
      return;
    }

    this.id.set(id);
    this.loading.set(true);

    this.productoService.obtener(id).subscribe({
      next: (producto) => {
        this.form.patchValue({
          nombre: producto.nombre,
          precio: producto.precio,
          categoriaTexto: producto.categoria.nombre,
        });
        this.textoBusqueda.set(producto.categoria.nombre);
        this.form.controls.categoriaTexto.updateValueAndValidity();
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.error.set('No se pudo cargar el producto.');
      },
    });
  }

  cargarCategorias(): void {
    this.categoriasLoading.set(true);
    this.categoriasError.set(null);

    this.categoriaService.listar().subscribe({
      next: (categorias) => {
        this.categorias.set(categorias);
        this.categoriasLoading.set(false);
        this.form.controls.categoriaTexto.updateValueAndValidity();
      },
      error: () => {
        this.categorias.set([]);
        this.categoriasLoading.set(false);
        this.categoriasError.set(
          'No se pudo cargar el catálogo de categorías. Recárguelo antes de guardar.',
        );
      },
    });
  }

  actualizarTexto(valor: string): void {
    this.textoBusqueda.set(valor);
  }

  alSeleccionarCategoria(nombre: string): void {
    this.textoBusqueda.set(nombre);
    this.form.controls.categoriaTexto.markAsTouched();
    this.form.controls.categoriaTexto.updateValueAndValidity();
  }

  guardar(): void {
    if (this.loading() || this.saving()) {
      return;
    }

    this.form.controls.nombre.setValue(this.form.controls.nombre.value?.trim() ?? '');
    this.form.markAllAsTouched();

    const nombreCategoria = normalizarTexto(this.form.controls.categoriaTexto.value ?? '');
    const categoria = this.categorias().find(
      (item) => normalizarTexto(item.nombre) === nombreCategoria,
    );

    if (categoria === undefined) {
      this.form.controls.categoriaTexto.updateValueAndValidity();
      this.error.set('Seleccione una categoría válida de la lista.');
      return;
    }

    if (this.form.invalid) {
      this.error.set(null);
      return;
    }

    const valor = this.form.getRawValue();
    const request: ProductoRequest = {
      nombre: valor.nombre ?? '',
      precio: Number(valor.precio),
      categoriaId: categoria.id,
    };

    const id = this.id();
    this.error.set(null);
    this.saving.set(true);

    const respuesta$ =
      id !== null
        ? this.productoService.actualizar(id, request)
        : this.productoService.crear(request);

    respuesta$.subscribe({
      next: () => {
        this.notificacion.mostrarExito(
          id !== null ? 'Producto actualizado correctamente.' : 'Producto creado correctamente.',
        );
        this.router.navigate(['/administracion/productos']);
      },
      error: (error) => {
        this.saving.set(false);
        if (esNoEncontrado(error)) {
          this.error.set(mensajeApi(error) ?? 'La categoría seleccionada ya no existe.');
          return;
        }
        this.error.set(mensajeApi(error) ?? 'No se pudo guardar el producto.');
      },
    });
  }
}
