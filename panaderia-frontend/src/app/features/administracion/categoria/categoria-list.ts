import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth/auth-service';
import { Alerta } from '../../../shared/components/alerta';
import { NotificacionService } from '../../../shared/services/notificacion-service';
import { normalizarTexto } from '../../../shared/utils/texto';
import { Categoria } from './categoria.model';
import { CategoriaService } from './categoria-service';

@Component({
  imports: [
    RouterLink,
    Alerta,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatTableModule,
  ],
  selector: 'app-categoria-list',
  styleUrl: './categoria-list.css',
  templateUrl: './categoria-list.html',
})
export class CategoriaList implements OnInit {
  private readonly categoriaService = inject(CategoriaService);
  private readonly notificacion = inject(NotificacionService);
  private readonly auth = inject(AuthService);

  readonly puedeGestionar = computed(() => this.auth.tieneRol('ADMIN'));

  readonly categorias = signal<Categoria[]>([]);
  readonly loading = signal<boolean>(false);
  readonly error = signal<string | null>(null);
  readonly exito = signal<string | null>(null);
  readonly filtro = signal<string>('');

  readonly columnas = ['id', 'nombre', 'descripcion', 'acciones'];

  /**
   * El endpoint de categorías no admite búsqueda en el backend, por lo que el
   * filtro se resuelve en memoria sobre la lista ya cargada.
   */
  readonly categoriasFiltradas = computed(() => {
    const termino = normalizarTexto(this.filtro());

    if (termino.length === 0) {
      return this.categorias();
    }

    return this.categorias().filter(
      (categoria) =>
        normalizarTexto(categoria.nombre).includes(termino) ||
        normalizarTexto(categoria.descripcion ?? '').includes(termino),
    );
  });

  ngOnInit(): void {
    this.exito.set(this.notificacion.consumirExito());
    this.cargar();
  }

  cargar(): void {
    this.loading.set(true);
    this.error.set(null);

    this.categoriaService.listar().subscribe({
      next: (categorias) => {
        this.categorias.set(categorias);
        this.loading.set(false);
      },
      error: () => {
        this.categorias.set([]);
        this.error.set('No se pudo cargar la lista de categorías.');
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
}
