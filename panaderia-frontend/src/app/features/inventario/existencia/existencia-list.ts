import { DecimalPipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatOptionModule } from '@angular/material/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { RouterLink } from '@angular/router';
import { ProductoResumen } from '../../administracion/producto/producto.model';
import { ProductoService } from '../../administracion/producto/producto-service';
import { Alerta } from '../../../shared/components/alerta';
import { NotificacionService } from '../../../shared/services/notificacion-service';
import { ConfirmacionService } from '../../../shared/services/confirmacion-service';
import { esConflicto, esNoEncontrado, mensajeApi } from '../../../shared/utils/api-error';
import { ExistenciaResponse } from './existencia.model';
import { ExistenciaService } from './existencia-service';

@Component({
  imports: [
    RouterLink,
    Alerta,
    DecimalPipe,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatOptionModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatTableModule,
  ],
  selector: 'app-existencia-list',
  styleUrl: './existencia-list.css',
  templateUrl: './existencia-list.html',
})
export class ExistenciaList implements OnInit {
  private readonly existenciaService = inject(ExistenciaService);
  private readonly productoService = inject(ProductoService);
  private readonly notificacion = inject(NotificacionService);
  private readonly confirmacion = inject(ConfirmacionService);

  readonly existencias = signal<ExistenciaResponse[]>([]);
  readonly productos = signal<ProductoResumen[]>([]);

  readonly loading = signal<boolean>(false);
  readonly productosLoading = signal<boolean>(false);
  readonly error = signal<string | null>(null);
  readonly exito = signal<string | null>(null);
  readonly productosError = signal<string | null>(null);
  readonly eliminandoId = signal<number | null>(null);

  readonly columnas = ['id', 'producto', 'cantidad', 'acciones'];

  /** null significa "Todos los productos": el backend recibe la petici��n sin filtro. */
  readonly productoFiltro = signal<number | null>(null);

  ngOnInit(): void {
    this.exito.set(this.notificacion.consumirExito());
    this.cargarProductos();
    this.cargar();
  }

  cargarProductos(): void {
    this.productosLoading.set(true);
    this.productosError.set(null);

    this.productoService.listar().subscribe({
      next: (productos) => {
        this.productos.set(productos);
        this.productosLoading.set(false);
      },
      error: () => {
        this.productos.set([]);
        this.productosLoading.set(false);
        this.productosError.set(
          'No se pudo cargar el catálogo de productos. El filtro por producto no estará disponible.',
        );
      },
    });
  }

  cargar(): void {
    this.loading.set(true);
    this.error.set(null);

    const productoId = this.productoFiltro();

    this.existenciaService.listar(productoId ?? undefined).subscribe({
      next: (existencias) => {
        this.existencias.set(existencias);
        this.loading.set(false);
      },
      error: (error) => {
        this.existencias.set([]);
        this.error.set(mensajeDeCarga(error));
        this.loading.set(false);
      },
    });
  }

  filtrarPorProducto(productoId: number | null): void {
    this.productoFiltro.set(productoId);
    this.cargar();
  }

  eliminar(existencia: ExistenciaResponse): void {
    if (this.eliminandoId() !== null) {
      return;
    }

    this.confirmacion.solicitar(
      {
        titulo: 'Eliminar existencia',
        mensaje: `¿Estás seguro de eliminar la existencia de “${existencia.producto.nombre}”?`,
        detalle: 'El producto quedará sin un registro de stock disponible.',
        textoConfirmar: 'Sí, eliminar',
        peligro: true,
      },
      () => this.confirmarEliminacion(existencia),
    );
  }

  private confirmarEliminacion(existencia: ExistenciaResponse): void {
    this.eliminandoId.set(existencia.id);
    this.error.set(null);

    this.existenciaService.eliminar(existencia.id).subscribe({
      next: () => {
        this.eliminandoId.set(null);
        this.cargar();
      },
      error: (error) => {
        this.eliminandoId.set(null);
        if (esConflicto(error)) {
          this.error.set(
            mensajeApi(error) ??
              'No se pudo eliminar la existencia porque todavía tiene stock disponible.',
          );
          return;
        }
        if (esNoEncontrado(error)) {
          this.error.set('La existencia ya no existe.');
          return;
        }
        this.error.set('No se pudo eliminar la existencia.');
      },
    });
  }
}

function mensajeDeCarga(error: unknown): string {
  if (esNoEncontrado(error)) {
    return 'No se encontraron existencias para el producto seleccionado.';
  }
  return 'No se pudo cargar la lista de existencias.';
}
