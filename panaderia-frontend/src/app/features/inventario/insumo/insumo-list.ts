import { DecimalPipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';
import { RouterLink } from '@angular/router';
import { Alerta } from '../../../shared/components/alerta';
import { NotificacionService } from '../../../shared/services/notificacion-service';
import { ConfirmacionService } from '../../../shared/services/confirmacion-service';
import { normalizarTexto } from '../../../shared/utils/texto';
import { Insumo } from './insumo.model';
import { InsumoService } from './insumo-service';

@Component({
  imports: [
    RouterLink,
    Alerta,
    DecimalPipe,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatTableModule,
  ],
  selector: 'app-insumo-list',
  styleUrl: './insumo-list.css',
  templateUrl: './insumo-list.html',
})
export class InsumoList implements OnInit {
  private readonly insumoService = inject(InsumoService);
  private readonly notificacion = inject(NotificacionService);
  private readonly confirmacion = inject(ConfirmacionService);

  readonly insumos = signal<Insumo[]>([]);
  readonly loading = signal<boolean>(false);
  readonly error = signal<string | null>(null);
  readonly exito = signal<string | null>(null);
  readonly eliminandoId = signal<number | null>(null);

  readonly filtro = signal<string>('');
  readonly columnas = ['id', 'nombre', 'unidadMedida', 'stockActual', 'stockMinimo', 'acciones'];

  /** El endpoint de insumos no admite búsqueda: el filtro se aplica en memoria. */
  readonly insumosFiltrados = computed(() => {
    const termino = normalizarTexto(this.filtro());

    if (termino.length === 0) {
      return this.insumos();
    }

    return this.insumos().filter(
      (insumo) =>
        normalizarTexto(insumo.nombre).includes(termino) ||
        normalizarTexto(insumo.unidadMedida).includes(termino),
    );
  });

  ngOnInit(): void {
    this.exito.set(this.notificacion.consumirExito());
    this.cargar();
  }

  actualizarFiltro(valor: string): void {
    this.filtro.set(valor);
  }

  limpiarFiltro(): void {
    this.filtro.set('');
  }

  cargar(): void {
    this.loading.set(true);
    this.error.set(null);

    this.insumoService.listar().subscribe({
      next: (data) => {
        this.insumos.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.insumos.set([]);
        this.error.set('No se pudo cargar la lista de insumos.');
        this.loading.set(false);
      },
    });
  }

  eliminar(insumo: Insumo): void {
    if (this.eliminandoId() !== null) {
      return;
    }

    this.confirmacion.solicitar(
      {
        titulo: 'Eliminar insumo',
        mensaje: `¿Estás seguro de eliminar el insumo “${insumo.nombre}”?`,
        detalle: 'Esta acción no se puede deshacer.',
        textoConfirmar: 'Sí, eliminar',
        peligro: true,
      },
      () => this.confirmarEliminacion(insumo),
    );
  }

  private confirmarEliminacion(insumo: Insumo): void {
    this.eliminandoId.set(insumo.id);
    this.error.set(null);

    this.insumoService.eliminar(insumo.id).subscribe({
      next: () => {
        this.eliminandoId.set(null);
        this.cargar();
      },
      error: () => {
        this.eliminandoId.set(null);
        this.error.set('No se pudo eliminar el insumo.');
      },
    });
  }
}
