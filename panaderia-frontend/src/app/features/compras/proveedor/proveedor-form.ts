import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Alerta } from '../../../shared/components/alerta';
import { NotificacionService } from '../../../shared/services/notificacion-service';
import { esConflicto, mensajeApi } from '../../../shared/utils/api-error';
import { ProveedorRequest } from './proveedor.model';
import { ProveedorService } from './proveedor-service';

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
    MatSlideToggleModule,
  ],
  selector: 'app-proveedor-form',
  styleUrl: './proveedor-form.css',
  templateUrl: './proveedor-form.html',
})
export class ProveedorForm implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly proveedorService = inject(ProveedorService);
  private readonly notificacion = inject(NotificacionService);

  readonly id = signal<number | null>(null);
  readonly loading = signal<boolean>(false);
  readonly saving = signal<boolean>(false);
  readonly error = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    ruc: ['', [Validators.required, Validators.pattern(/^\d{11}$/)]],
    razonSocial: ['', [Validators.required, Validators.maxLength(120)]],
    telefono: ['', [Validators.maxLength(20)]],
    email: ['', [Validators.email, Validators.maxLength(120)]],
    activo: [true, [Validators.required]],
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
      this.error.set('El identificador del proveedor no es válido.');
      return;
    }

    this.id.set(id);
    this.loading.set(true);

    this.proveedorService.obtener(id).subscribe({
      next: (proveedor) => {
        this.form.patchValue({
          ruc: proveedor.ruc,
          razonSocial: proveedor.razonSocial,
          telefono: proveedor.telefono ?? '',
          email: proveedor.email ?? '',
          activo: proveedor.activo,
        });
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.error.set('No se pudo cargar el proveedor.');
      },
    });
  }

  guardar(): void {
    if (this.loading() || this.saving()) {
      return;
    }

    this.form.controls.ruc.setValue(this.form.controls.ruc.value.trim());
    this.form.controls.razonSocial.setValue(this.form.controls.razonSocial.value.trim());
    this.form.controls.telefono.setValue(this.form.controls.telefono.value.trim());
    this.form.controls.email.setValue(this.form.controls.email.value.trim());

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const valor = this.form.getRawValue();
    const request: ProveedorRequest = {
      ruc: valor.ruc,
      razonSocial: valor.razonSocial,
      telefono: valor.telefono.length > 0 ? valor.telefono : null,
      email: valor.email.length > 0 ? valor.email : null,
      activo: valor.activo,
    };

    const id = this.id();
    this.error.set(null);
    this.saving.set(true);

    const respuesta$ =
      id !== null
        ? this.proveedorService.actualizar(id, request)
        : this.proveedorService.crear(request);

    respuesta$.subscribe({
      next: () => {
        this.notificacion.mostrarExito(
          id !== null ? 'Proveedor actualizado correctamente.' : 'Proveedor creado correctamente.',
        );
        this.router.navigate(['/compras/proveedores']);
      },
      error: (error) => {
        this.saving.set(false);
        if (esConflicto(error)) {
          this.error.set(mensajeApi(error) ?? 'El RUC ya pertenece a otro proveedor.');
          return;
        }
        this.error.set('No se pudo guardar el proveedor.');
      },
    });
  }
}
