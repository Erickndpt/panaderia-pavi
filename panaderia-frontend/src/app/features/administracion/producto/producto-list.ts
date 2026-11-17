import { DecimalPipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatOptionModule } from '@angular/material/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth/auth-service';
import { Alerta } from '../../../shared/components/alerta';
import { NotificacionService } from '../../../shared/services/notificacion-service';
import { Categoria } from '../categoria/categoria.model';
import { CategoriaService } from '../categoria/categoria-service';
import { Producto } from './producto.model';
import { ProductoService } from './producto-service';

@Component({
  imports: [
    DecimalPipe,
    RouterLink,
    Alerta,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatOptionModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatTableModule,
  ],
  selector: 'app-producto-list',
  styleUrl: './producto-list.css',
  templateUrl: './producto-list.html',
})
export class ProductoList implements OnInit {
  private readonly productoService = inject(ProductoService);
  private readonly categoriaService = inject(CategoriaService);
  private readonly notificacion = inject(NotificacionService);
  private readonly auth = inject(AuthService);

  readonly puedeGestionar = computed(() => this.auth.tieneRol('ADMIN'));

  readonly productos = signal<Producto[]>([]);
  readonly categorias = signal<Categoria[]>([]);
  readonly loading = signal<boolean>(false);
  readonly error = signal<string | null>(null);
  readonly exito = signal<string | null>(null);

  readonly filtroNombre = signal<string>('');
  readonly filtroCategoria = signal<number | null>(null);

  readonly columnas = ['id', 'nombre', 'categoria', 'precio', 'acciones'];

  readonly hayFiltros = computed(
    () => this.filtroNombre().trim().length > 0 || this.filtroCategoria() !== null,
  );

  ngOnInit(): void {
    this.exito.set(this.notificacion.consumirExito());
    this.cargarCategorias();
    this.buscar();
  }

  cargarCategorias(): void {
    this.categoriaService.listar().subscribe({
      next: (categorias) => this.categorias.set(categorias),
      error: () => this.categorias.set([]),
    });
  }

  buscar(): void {
    this.loading.set(true);
    this.error.set(null);

    this.productoService
      .listar({
        nombre: this.filtroNombre().trim() || undefined,
        categoriaId: this.filtroCategoria() ?? undefined,
      })
      .subscribe({
        next: (productos) => {
          this.productos.set(productos);
          this.loading.set(false);
        },
        error: () => {
          this.productos.set([]);
          this.error.set('No se pudo cargar la lista de productos.');
          this.loading.set(false);
        },
      });
  }

  actualizarNombre(valor: string): void {
    this.filtroNombre.set(valor);
  }

  seleccionarCategoria(categoriaId: number | null): void {
    this.filtroCategoria.set(categoriaId);
    this.buscar();
  }

  limpiarFiltros(): void {
    this.filtroNombre.set('');
    this.filtroCategoria.set(null);
    this.buscar();
  }
}
