import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Alerta } from '../../../shared/components/alerta';
import { esConflicto, mensajeApi } from '../../../shared/utils/api-error';
import { NotificacionService } from '../../../shared/services/notificacion-service';
import { CategoriaRequest } from './categoria.model';
import { CategoriaService } from './categoria-service';

@Component({
  imports: [
    ReactiveFormsModule,
    RouterLink,
    Alerta,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
  ],
  selector: 'app-categoria-form',
  styleUrl: './categoria-form.css',
  templateUrl: './categoria-form.html',
})
export class CategoriaForm implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly categoriaService = inject(CategoriaService);
  private readonly notificacion = inject(NotificacionService);

  readonly id = signal<number | null>(null);
  readonly loading = signal<boolean>(false);
  readonly saving = signal<boolean>(false);
  readonly error = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    nombre: ['', [Validators.required, Validators.maxLength(80)]],
    descripcion: ['', [Validators.maxLength(200)]],
  });

  get esEdicion(): boolean {
    return this.id() !== null;
  }

  ngOnInit(): void {
    const param = this.route.snapshot.paramMap.get('id');

    if (param === null) {
      return;
    }

    const id = Number(param);
    if (!Number.isInteger(id) || id <= 0) {
      this.error.set('El identificador de la categoría no es válido.');
      return;
    }

    this.id.set(id);
    this.loading.set(true);

    this.categoriaService.obtener(id).subscribe({
      next: (categoria) => {
        this.form.patchValue({
          nombre: categoria.nombre,
          descripcion: categoria.descripcion ?? '',
        });
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.error.set('No se pudo cargar la categoría.');
      },
    });
  }

  guardar(): void {
    if (this.loading() || this.saving()) {
      return;
    }

    this.form.controls.nombre.setValue(this.form.controls.nombre.value.trim());
    this.form.controls.descripcion.setValue(this.form.controls.descripcion.value.trim());

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const valor = this.form.getRawValue();
    const request: CategoriaRequest = {
      nombre: valor.nombre,
      descripcion: valor.descripcion.length > 0 ? valor.descripcion : null,
    };

    const id = this.id();
    this.error.set(null);
    this.saving.set(true);

    const respuesta$ =
      id !== null
        ? this.categoriaService.actualizar(id, request)
        : this.categoriaService.crear(request);

    respuesta$.subscribe({
      next: () => {
        this.notificacion.mostrarExito(
          id !== null ? 'Categoría actualizada correctamente.' : 'Categoría creada correctamente.',
        );
        this.router.navigate(['/administracion/categorias']);
      },
      error: (error) => {
        this.saving.set(false);
        if (esConflicto(error)) {
          this.error.set(mensajeApi(error) ?? 'La categoría entra en conflicto con otra registrada.');
          return;
        }
        this.error.set('No se pudo guardar la categoría.');
      },
    });
  }
}
