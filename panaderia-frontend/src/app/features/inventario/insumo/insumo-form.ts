import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatOptionModule } from '@angular/material/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Alerta } from '../../../shared/components/alerta';
import { NotificacionService } from '../../../shared/services/notificacion-service';
import { InsumoRequest } from './insumo.model';
import { InsumoService } from './insumo-service';

@Component({
  imports: [
    ReactiveFormsModule,
    RouterLink,
    Alerta,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatOptionModule,
    MatProgressSpinnerModule,
    MatSelectModule,
  ],
  selector: 'app-insumo-form',
  styleUrl: './insumo-form.css',
  templateUrl: './insumo-form.html',
})
export class InsumoForm implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly insumoService = inject(InsumoService);
  private readonly notificacion = inject(NotificacionService);

  readonly unidadesMedida = ['KILOGRAMO', 'GRAMO', 'LITRO', 'MILILITRO', 'UNIDAD'];

  readonly id = signal<number | null>(null);
  readonly loading = signal<boolean>(false);
  readonly saving = signal<boolean>(false);
  readonly error = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    nombre: ['', [Validators.required, Validators.maxLength(80)]],
    unidadMedida: ['', [Validators.required, Validators.maxLength(20)]],
    stockActual: [0, [Validators.required, Validators.min(0)]],
    stockMinimo: [0, [Validators.required, Validators.min(0)]],
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
      this.error.set('El identificador del insumo no es válido.');
      return;
    }

    this.id.set(id);
    this.loading.set(true);

    this.insumoService.obtener(id).subscribe({
      next: (insumo) => {
        this.form.patchValue(insumo);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.error.set('No se pudo cargar el insumo.');
      },
    });
  }

  guardar(): void {
    if (this.loading() || this.saving()) {
      return;
    }

    this.form.controls.nombre.setValue(this.form.controls.nombre.value.trim());
    this.form.controls.unidadMedida.setValue(this.form.controls.unidadMedida.value.trim());

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const valor = this.form.getRawValue();
    const request: InsumoRequest = {
      nombre: valor.nombre,
      unidadMedida: valor.unidadMedida,
      stockActual: valor.stockActual,
      stockMinimo: valor.stockMinimo,
    };

    const id = this.id();
    this.error.set(null);
    this.saving.set(true);

    const respuesta$ =
      id !== null ? this.insumoService.actualizar(id, request) : this.insumoService.crear(request);

    respuesta$.subscribe({
      next: () => {
        this.notificacion.mostrarExito(
          id !== null ? 'Insumo actualizado correctamente.' : 'Insumo creado correctamente.',
        );
        this.router.navigate(['/inventario/insumos']);
      },
      error: () => {
        this.saving.set(false);
        this.error.set('No se pudo guardar el insumo.');
      },
    });
  }
}
