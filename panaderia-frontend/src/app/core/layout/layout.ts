import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { AuthService } from '../auth/auth-service';

interface ItemMenu {
  nombre: string;
  ruta?: string;
  icono: string;
  roles?: readonly string[];
  pendiente?: boolean;
  exacto?: boolean;
}

interface GrupoMenu {
  nombre: string;
  icono: string;
  items: ItemMenu[];
}

const GRUPOS: readonly GrupoMenu[] = [
  {
    nombre: 'Administración',
    icono: 'category',
    items: [
      {
        nombre: 'Categorías',
        ruta: '/administracion/categorias',
        icono: 'sell',
        roles: ['SUPERVISOR', 'ADMIN'],
      },
      {
        nombre: 'Productos',
        ruta: '/administracion/productos',
        icono: 'bakery_dining',
        roles: ['SUPERVISOR', 'ADMIN'],
      },
    ],
  },
  {
    nombre: 'Inventario',
    icono: 'inventory_2',
    items: [
      {
        nombre: 'Insumos',
        ruta: '/inventario/insumos',
        icono: 'grain',
        roles: ['SUPERVISOR', 'ADMIN'],
      },
      {
        nombre: 'Existencias',
        ruta: '/inventario/existencias',
        icono: 'warehouse',
        roles: ['SUPERVISOR', 'ADMIN'],
      },
    ],
  },
  {
    nombre: 'Compras',
    icono: 'shopping_cart',
    items: [
      {
        nombre: 'Proveedores',
        ruta: '/compras/proveedores',
        icono: 'local_shipping',
        roles: ['SUPERVISOR', 'ADMIN'],
      },
      {
        nombre: 'Nueva compra',
        ruta: '/compras/nueva',
        icono: 'add_business',
        roles: ['ADMIN'],
      },
      {
        nombre: 'Reporte de compras',
        ruta: '/compras/reporte',
        icono: 'receipt_long',
        roles: ['SUPERVISOR', 'ADMIN'],
      },
    ],
  },
  {
    nombre: 'Producción',
    icono: 'precision_manufacturing',
    items: [
      {
        nombre: 'Nueva orden',
        ruta: '/produccion/nueva',
        icono: 'playlist_add',
        roles: ['ADMIN'],
      },
      {
        nombre: 'Órdenes de producción',
        ruta: '/produccion/ordenes',
        icono: 'assignment',
        roles: ['SUPERVISOR', 'ADMIN'],
      },
    ],
  },
  {
    nombre: 'Ventas',
    icono: 'point_of_sale',
    items: [
      { nombre: 'Clientes', ruta: '/ventas/clientes', icono: 'groups' },
      {
        nombre: 'Nueva venta',
        ruta: '/ventas/nueva',
        icono: 'add_shopping_cart',
        roles: ['VENDEDOR', 'SUPERVISOR'],
      },
      { nombre: 'Reporte de ventas', ruta: '/ventas/reporte', icono: 'bar_chart' },
      { nombre: 'Pagos', ruta: '/ventas/pagos', icono: 'payments' },
    ],
  },
  {
    nombre: 'Finanzas',
    icono: 'account_balance',
    items: [
      {
        nombre: 'Movimientos',
        ruta: '/finanzas/movimientos',
        icono: 'swap_horiz',
        roles: ['SUPERVISOR', 'ADMIN'],
      },
      {
        nombre: 'Reporte financiero',
        ruta: '/finanzas/reporte',
        icono: 'insights',
        roles: ['SUPERVISOR', 'ADMIN'],
      },
    ],
  },
];

@Component({
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatButtonModule,
    MatIconModule,
    MatListModule,
    MatSidenavModule,
    MatToolbarModule,
  ],
  selector: 'app-layout',
  styleUrl: './layout.css',
  templateUrl: './layout.html',
})
export class Layout {
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);

  readonly grupos = GRUPOS;
  readonly autenticado = this.auth.autenticado;
  readonly usuario = this.auth.usuario;

  readonly gruposVisibles = computed(() =>
    GRUPOS.map((grupo) => ({
      ...grupo,
      items: grupo.items.filter(
        (item) => item.roles === undefined || this.auth.tieneRol(...item.roles),
      ),
    })).filter((grupo) => grupo.items.length > 0),
  );

  private readonly expandidosInterno = signal<ReadonlySet<string>>(
    new Set(GRUPOS.map((grupo) => grupo.nombre)),
  );

  readonly abierto = signal(true);

  /** true en pantallas pequeñas (≤ 860px): el panel se superpone al contenido. */
  readonly modoMovil = signal(this.esPantallaPequena());

  readonly modo = computed(() => (this.modoMovil() ? 'over' : 'side'));

  constructor() {
    if (typeof window !== 'undefined' && typeof window.matchMedia === 'function') {
      const consulta = window.matchMedia('(max-width: 860px)');
      consulta.addEventListener('change', (evento) => {
        this.modoMovil.set(evento.matches);
        this.abierto.set(!evento.matches);
      });
    }

    this.router.events
      .pipe(
        filter((evento): evento is NavigationEnd => evento instanceof NavigationEnd),
        takeUntilDestroyed(),
      )
      .subscribe(() => {
        if (this.modoMovil()) {
          this.abierto.set(false);
        }
      });
  }

  estaExpandido(nombre: string): boolean {
    return this.expandidosInterno().has(nombre);
  }

  alternarGrupo(nombre: string): void {
    const actuales = this.expandidosInterno();
    const siguiente = new Set(actuales);

    if (siguiente.has(nombre)) {
      siguiente.delete(nombre);
    } else {
      siguiente.add(nombre);
    }

    this.expandidosInterno.set(siguiente);
  }

  alternarPanel(): void {
    this.abierto.update((valor) => !valor);
  }

  iniciarSesion(): void {
    void this.auth.iniciarLogin();
  }

  cerrarSesion(): void {
    this.auth.cerrarSesion();
  }

  private esPantallaPequena(): boolean {
    if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') {
      return false;
    }
    return window.matchMedia('(max-width: 860px)').matches;
  }
}
