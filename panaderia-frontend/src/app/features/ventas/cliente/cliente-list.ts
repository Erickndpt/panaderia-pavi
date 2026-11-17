import { Component, inject, OnInit, signal } from '@angular/core';
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
import { Cliente, ClienteRequest } from './cliente.model';
import { ClienteService } from './cliente-service';

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
  selector: 'app-cliente-list',
  styleUrl: './cliente-list.css',
  templateUrl: './cliente-list.html',
})
export class ClienteList implements OnInit {
  private readonly clienteService = inject(ClienteService);
  private readonly notificacion = inject(NotificacionService);
  private readonly confirmacion = inject(ConfirmacionService);

  readonly clientes = signal<Cliente[]>([]);
  readonly loading = signal<boolean>(false);
  readonly error = signal<string | null>(null);
  readonly exito = signal<string | null>(null);
  readonly filtro = signal<string>('');
  readonly actualizandoId = signal<number | null>(null);

  readonly columnas = [
    'id',
    'documento',
    'nombreRazonSocial',
    'contacto',
    'pais',
    'activo',
    'acciones',
  ];

  ngOnInit(): void {
    this.exito.set(this.notificacion.consumirExito());
    this.buscar();
  }

  buscar(): void {
    this.loading.set(true);
    this.error.set(null);

    this.clienteService.listar(this.filtro().trim() || undefined).subscribe({
      next: (clientes) => {
        this.clientes.set(clientes);
        this.loading.set(false);
      },
      error: () => {
        this.clientes.set([]);
        this.error.set('No se pudo cargar la lista de clientes.');
        this.loading.set(false);
      },
    });
  }

  actualizarFiltro(valor: string): void {
    this.filtro.set(valor);
  }

  limpiarFiltro(): void {
    this.filtro.set('');
    this.buscar();
  }

  /**
   * Cambia el estado del cliente reutilizando el PUT existente. Nunca elimina:
   * conserva el registro y solo alterna `activo`, pidiendo confirmación al desactivar.
   */
  cambiarActivo(cliente: Cliente, evento: MatSlideToggleChange): void {
    const nuevoEstado = evento.checked;

    if (this.actualizandoId() !== null) {
      evento.source.checked = cliente.activo;
      return;
    }

    if (!nuevoEstado) {
      evento.source.checked = cliente.activo;
      this.confirmacion.solicitar(
        {
          titulo: 'Desactivar cliente',
          mensaje: `¿Estás seguro de desactivar a “${cliente.nombreRazonSocial}”?`,
          detalle: 'No podrá seleccionarse en nuevas ventas, pero podrás reactivarlo más adelante.',
          textoConfirmar: 'Sí, desactivar',
          peligro: true,
        },
        () => {
          evento.source.checked = false;
          this.actualizarEstado(cliente, evento, false);
        },
      );
      return;
    }

    this.actualizarEstado(cliente, evento, true);
  }

  private actualizarEstado(
    cliente: Cliente,
    evento: MatSlideToggleChange,
    nuevoEstado: boolean,
  ): void {
    const request: ClienteRequest = {
      tipoDocumento: cliente.tipoDocumento,
      numeroDocumento: cliente.numeroDocumento,
      nombreRazonSocial: cliente.nombreRazonSocial,
      telefono: cliente.telefono,
      email: cliente.email,
      direccion: cliente.direccion,
      pais: cliente.pais,
      activo: nuevoEstado,
    };

    this.actualizandoId.set(cliente.id);
    this.error.set(null);
    this.exito.set(null);

    this.clienteService.actualizar(cliente.id, request).subscribe({
      next: (actualizado) => {
        this.clientes.update((lista) =>
          lista.map((item) => (item.id === actualizado.id ? actualizado : item)),
        );
        this.actualizandoId.set(null);
        this.exito.set(
          nuevoEstado
            ? `Cliente "${actualizado.nombreRazonSocial}" activado.`
            : `Cliente "${actualizado.nombreRazonSocial}" desactivado.`,
        );
      },
      error: (error) => {
        evento.source.checked = cliente.activo;
        this.actualizandoId.set(null);
        if (esConflicto(error)) {
          this.error.set(mensajeApi(error) ?? 'No se pudo actualizar el estado del cliente.');
          return;
        }
        this.error.set('No se pudo actualizar el estado del cliente.');
      },
    });
  }
}
