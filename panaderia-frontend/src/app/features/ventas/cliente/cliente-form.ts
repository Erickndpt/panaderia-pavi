import { Component, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatOptionModule } from '@angular/material/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Alerta } from '../../../shared/components/alerta';
import { NotificacionService } from '../../../shared/services/notificacion-service';
import { esConflicto, mensajeApi } from '../../../shared/utils/api-error';
import { ClienteRequest, DocumentoConsulta, TipoDocumento } from './cliente.model';
import { ClienteService } from './cliente-service';

interface OpcionTipoDocumento {
  valor: TipoDocumento;
  etiqueta: string;
}

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
    MatSlideToggleModule,
  ],
  selector: 'app-cliente-form',
  styleUrl: './cliente-form.css',
  templateUrl: './cliente-form.html',
})
export class ClienteForm implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly clienteService = inject(ClienteService);
  private readonly notificacion = inject(NotificacionService);

  readonly tipos: readonly OpcionTipoDocumento[] = [
    { valor: 'DNI', etiqueta: 'DNI' },
    { valor: 'RUC', etiqueta: 'RUC' },
    { valor: 'CE', etiqueta: 'Carné de extranjería' },
    { valor: 'PASAPORTE', etiqueta: 'Pasaporte' },
    { valor: 'SIN_DOCUMENTO', etiqueta: 'Sin documento' },
  ];

  readonly id = signal<number | null>(null);
  readonly loading = signal<boolean>(false);
  readonly saving = signal<boolean>(false);
  readonly error = signal<string | null>(null);
  readonly consultando = signal<boolean>(false);
  readonly consultaMensaje = signal<string | null>(null);
  readonly consultaDatos = signal<DocumentoConsulta | null>(null);

  readonly form = this.fb.nonNullable.group({
    tipoDocumento: ['DNI' as TipoDocumento, [Validators.required]],
    numeroDocumento: ['', [Validators.required, Validators.pattern(/^\d{8}$/)]],
    nombreRazonSocial: ['', [Validators.required, Validators.maxLength(150)]],
    telefono: ['', [Validators.maxLength(20)]],
    email: ['', [Validators.email, Validators.maxLength(120)]],
    direccion: ['', [Validators.maxLength(200)]],
    pais: ['', [Validators.maxLength(80)]],
    activo: [true, [Validators.required]],
  });


  constructor() {
    this.form.controls.tipoDocumento.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(() => {
        this.actualizarValidadoresDocumento();
        this.consultaMensaje.set(null);
        this.consultaDatos.set(null);
      });

    this.form.controls.numeroDocumento.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(() => {
        this.consultaMensaje.set(null);
        this.consultaDatos.set(null);
      });

    this.actualizarValidadoresDocumento();
  }

  get esEdicion(): boolean {
    return this.id() !== null;
  }

  get esSinDocumento(): boolean {
    return this.form.controls.tipoDocumento.value === 'SIN_DOCUMENTO';
  }

  get tipoActual(): TipoDocumento {
    return this.form.controls.tipoDocumento.value;
  }

  ngOnInit(): void {
    const param = this.route.snapshot.paramMap.get('id');

    if (param === null) {
      return;
    }

    const id = Number(param);
    if (!Number.isInteger(id) || id <= 0) {
      this.error.set('El identificador del cliente no es válido.');
      return;
    }

    this.id.set(id);
    this.loading.set(true);

    this.clienteService.obtener(id).subscribe({
      next: (cliente) => {
        this.form.patchValue({
          tipoDocumento: cliente.tipoDocumento,
          numeroDocumento: cliente.numeroDocumento ?? '',
          nombreRazonSocial: cliente.nombreRazonSocial,
          telefono: cliente.telefono ?? '',
          email: cliente.email ?? '',
          direccion: cliente.direccion ?? '',
          pais: cliente.pais ?? '',
          activo: cliente.activo,
        });
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.error.set('No se pudo cargar el cliente.');
      },
    });
  }

  /**
   * Ajusta los validadores del número de documento según el tipo, replicando las
   * reglas del backend (@AssertTrue): DNI 8 dígitos, RUC 11 dígitos, CE/PASAPORTE
   * entre 3 y 20 caracteres y SIN_DOCUMENTO sin número.
   */
  private actualizarValidadoresDocumento(): void {
    const control = this.form.controls.numeroDocumento;

    switch (this.form.controls.tipoDocumento.value) {
      case 'SIN_DOCUMENTO':
        control.setValidators([]);
        control.disable({ emitEvent: false });
        break;
      case 'DNI':
        control.enable({ emitEvent: false });
        control.setValidators([Validators.required, Validators.pattern(/^\d{8}$/)]);
        break;
      case 'RUC':
        control.enable({ emitEvent: false });
        control.setValidators([Validators.required, Validators.pattern(/^\d{11}$/)]);
        break;
      default:
        control.enable({ emitEvent: false });
        control.setValidators([
          Validators.required,
          Validators.minLength(3),
          Validators.maxLength(20),
        ]);
        break;
    }

    control.updateValueAndValidity({ emitEvent: false });
  }


  puedeConsultar(): boolean {
    if (this.esSinDocumento || this.consultando() || this.saving()) {
      return false;
    }
    const tipo = this.tipoActual;
    const numero = this.form.controls.numeroDocumento.value.trim();
    if (tipo === 'DNI') {
      return /^\d{8}$/.test(numero);
    }
    if (tipo === 'RUC') {
      return /^\d{11}$/.test(numero);
    }
    return false;
  }

  consultarDocumento(): void {
    if (!this.puedeConsultar()) {
      return;
    }
    const tipo = this.tipoActual;
    const numero = this.form.controls.numeroDocumento.value.trim();
    this.consultaMensaje.set(null);
    this.consultaDatos.set(null);
    this.consultando.set(true);
    this.error.set(null);

    this.clienteService.consultarDocumento(tipo, numero).subscribe({
      next: (datos) => {
        this.consultando.set(false);
        this.consultaDatos.set(datos);
        this.aplicarDatosConsultados(datos);
      },
      error: (err) => {
        this.consultando.set(false);
        const status = err?.status;
        if (status === 404) {
          this.consultaMensaje.set('No se encontró información para el documento ingresado.');
        } else if (status === 429) {
          this.consultaMensaje.set('Se alcanzó el límite temporal de consultas. Inténtalo más tarde.');
        } else if (status === 503) {
          this.consultaMensaje.set('El servicio de consulta no está disponible. Puedes completar los datos manualmente.');
        } else {
          this.consultaMensaje.set('El servicio de consulta no está disponible. Puedes completar los datos manualmente.');
        }
      },
    });
  }

  private aplicarDatosConsultados(datos: DocumentoConsulta): void {
    if (datos.nombreRazonSocial && datos.nombreRazonSocial.trim()) {
      this.form.controls.nombreRazonSocial.setValue(datos.nombreRazonSocial);
    }

    if (datos.tipoDocumento === 'RUC') {
      if (datos.direccion && datos.direccion.trim() && !this.form.controls.direccion.value.trim()) {
        this.form.controls.direccion.setValue(datos.direccion);
      }
    }

    if (!this.form.controls.pais.value.trim()) {
      this.form.controls.pais.setValue('Perú');
    }
  }
  guardar(): void {
    if (this.loading() || this.saving()) {
      return;
    }

    this.form.controls.nombreRazonSocial.setValue(
      this.form.controls.nombreRazonSocial.value.trim(),
    );
    this.form.controls.numeroDocumento.setValue(
      this.form.controls.numeroDocumento.value.trim(),
    );
    this.form.controls.telefono.setValue(this.form.controls.telefono.value.trim());
    this.form.controls.email.setValue(this.form.controls.email.value.trim());
    this.form.controls.direccion.setValue(this.form.controls.direccion.value.trim());
    this.form.controls.pais.setValue(this.form.controls.pais.value.trim());

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const valor = this.form.getRawValue();
    const esSinDocumento = valor.tipoDocumento === 'SIN_DOCUMENTO';
    const request: ClienteRequest = {
      tipoDocumento: valor.tipoDocumento,
      numeroDocumento: esSinDocumento ? null : valor.numeroDocumento || null,
      nombreRazonSocial: valor.nombreRazonSocial,
      telefono: valor.telefono.length > 0 ? valor.telefono : null,
      email: valor.email.length > 0 ? valor.email : null,
      direccion: valor.direccion.length > 0 ? valor.direccion : null,
      pais: valor.pais.length > 0 ? valor.pais : null,
      activo: valor.activo,
    };

    const id = this.id();
    this.error.set(null);
    this.saving.set(true);

    const respuesta$ =
      id !== null ? this.clienteService.actualizar(id, request) : this.clienteService.crear(request);

    respuesta$.subscribe({
      next: () => {
        this.notificacion.mostrarExito(
          id !== null ? 'Cliente actualizado correctamente.' : 'Cliente creado correctamente.',
        );
        this.router.navigate(['/ventas/clientes']);
      },
      error: (error) => {
        this.saving.set(false);
        if (esConflicto(error)) {
          this.error.set(mensajeApi(error) ?? 'El número de documento ya está registrado.');
          return;
        }
        this.error.set('No se pudo guardar el cliente.');
      },
    });
  }
}











