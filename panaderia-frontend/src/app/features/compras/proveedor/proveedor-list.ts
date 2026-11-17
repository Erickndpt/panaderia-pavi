import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSlideToggleChange, MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatTableModule } from '@angular/material/table';
import { RouterLink } from '@angular/router';
import { Alerta } from '../../../shared/components/alerta';
import { NotificacionService } from '../../../shared/services/notificacion-service';
import { ConfirmacionService } from '../../../shared/services/confirmacion-service';
import { esConflicto, mensajeApi } from '../../../shared/utils/api-error';
import { normalizarTexto } from '../../../shared/utils/texto';
import { Proveedor, ProveedorRequest } from './proveedor.model';
import { ProveedorService } from './proveedor-service';

@Component({
  imports: [
    RouterLink,
    Alerta,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSlideToggleModule,
    MatTableModule,
  ],
  selector: 'app-proveedor-list',
  styleUrl: './proveedor-list.css',
  templateUrl: './proveedor-list.html',
})
export class ProveedorList implements OnInit {
  private readonly proveedorService = inject(ProveedorService);
  private readonly notificacion = inject(NotificacionService);
  private readonly confirmacion = inject(ConfirmacionService);

  readonly proveedores = signal<Proveedor[]>([]);
  readonly loading = signal<boolean>(false);
  readonly error = signal<string | null>(null);
  readonly exito = signal<string | null>(null);
  readonly filtro = signal<string>('');
  readonly actualizandoId = signal<number | null>(null);

  readonly columnas = ['id', 'ruc', 'razonSocial', 'contacto', 'activo', 'acciones'];

  readonly proveedoresFiltrados = computed(() => {
    const termino = normalizarTexto(this.filtro());

    if (termino.length === 0) {
      return this.proveedores();
    }

    return this.proveedores().filter(
      (proveedor) =>
        normalizarTexto(proveedor.ruc).includes(termino) ||
        normalizarTexto(proveedor.razonSocial).includes(termino),
    );
  });

  ngOnInit(): void {
    this.exito.set(this.notificacion.consumirExito());
    this.cargar();
  }

  cargar(): void {
    this.loading.set(true);
    this.error.set(null);

    this.proveedorService.listar().subscribe({
      next: (proveedores) => {
        this.proveedores.set(proveedores);
        this.loading.set(false);
      },
      error: () => {
        this.proveedores.set([]);
        this.error.set('No se pudo cargar la lista de proveedores.');
        this.loading.set(false);
      },
    });
  }

  actualizarFiltro(valor: string): void {
    this.filtro.set(valor);
  }

  limpiarFiltro(): void {
    this.filtro.set('');
  }

  /**
   * Cambia el estado del proveedor reutilizando el PUT existente. Nunca elimina:
   * conserva todos los campos y solo alterna `activo`. Pide confirmación al desactivar.
   */
  cambiarActivo(proveedor: Proveedor, evento: MatSlideToggleChange): void {
    const nuevoEstado = evento.checked;

    if (this.actualizandoId() !== null) {
      evento.source.checked = proveedor.activo;
      return;
    }

    if (!nuevoEstado) {
      evento.source.checked = proveedor.activo;
      this.confirmacion.solicitar(
        {
          titulo: 'Desactivar proveedor',
          mensaje: `¿Estás seguro de desactivar a “${proveedor.razonSocial}”?`,
          detalle:
            'No podrá seleccionarse en nuevas compras, pero podrás reactivarlo más adelante.',
          textoConfirmar: 'Sí, desactivar',
          peligro: true,
        },
        () => {
          evento.source.checked = false;
          this.actualizarEstado(proveedor, evento, false);
        },
      );
      return;
    }

    this.actualizarEstado(proveedor, evento, true);
  }

  private actualizarEstado(
    proveedor: Proveedor,
    evento: MatSlideToggleChange,
    nuevoEstado: boolean,
  ): void {
    const request: ProveedorRequest = {
      ruc: proveedor.ruc,
      razonSocial: proveedor.razonSocial,
      telefono: proveedor.telefono,
      email: proveedor.email,
      activo: nuevoEstado,
    };

    this.actualizandoId.set(proveedor.id);
    this.error.set(null);
    this.exito.set(null);

    this.proveedorService.actualizar(proveedor.id, request).subscribe({
      next: (actualizado) => {
        this.proveedores.update((lista) =>
          lista.map((item) => (item.id === actualizado.id ? actualizado : item)),
        );
        this.actualizandoId.set(null);
        this.exito.set(
          nuevoEstado
            ? `Proveedor "${actualizado.razonSocial}" activado.`
            : `Proveedor "${actualizado.razonSocial}" desactivado.`,
        );
      },
      error: (error) => {
        evento.source.checked = proveedor.activo;
        this.actualizandoId.set(null);
        if (esConflicto(error)) {
          this.error.set(mensajeApi(error) ?? 'No se pudo actualizar el estado del proveedor.');
          return;
        }
        this.error.set('No se pudo actualizar el estado del proveedor.');
      },
    });
  }
}
