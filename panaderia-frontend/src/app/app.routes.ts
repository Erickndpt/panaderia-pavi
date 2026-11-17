import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth-guard';
import { rolGuard } from './core/guards/rol-guard';

export const routes: Routes = [
  {
    path: 'auth/callback',
    loadComponent: () => import('./core/auth/auth-callback').then((m) => m.AuthCallback),
    title: 'Iniciando sesión | Panadería Pavi',
  },
  {
    path: '',
    loadComponent: () => import('./core/layout/layout').then((m) => m.Layout),
    children: [
      {
        path: '',
        loadComponent: () => import('./core/inicio/inicio').then((m) => m.Inicio),
        title: 'Inicio | Panadería Pavi',
      },
      {
        path: 'administracion/categorias',
        loadComponent: () =>
          import('./features/administracion/categoria/categoria-list').then((m) => m.CategoriaList),
        title: 'Categorías | Panadería Pavi',
        canActivate: [authGuard, rolGuard('SUPERVISOR', 'ADMIN')],
      },
      {
        path: 'administracion/categorias/nueva',
        loadComponent: () =>
          import('./features/administracion/categoria/categoria-form').then((m) => m.CategoriaForm),
        title: 'Nueva categoría | Panadería Pavi',
        canActivate: [authGuard, rolGuard('ADMIN')],
      },
      {
        path: 'administracion/categorias/:id/editar',
        loadComponent: () =>
          import('./features/administracion/categoria/categoria-form').then((m) => m.CategoriaForm),
        title: 'Editar categoría | Panadería Pavi',
        canActivate: [authGuard, rolGuard('ADMIN')],
      },
      {
        path: 'administracion/productos',
        loadComponent: () =>
          import('./features/administracion/producto/producto-list').then((m) => m.ProductoList),
        title: 'Productos | Panadería Pavi',
        canActivate: [authGuard, rolGuard('SUPERVISOR', 'ADMIN')],
      },
      {
        path: 'administracion/productos/nuevo',
        loadComponent: () =>
          import('./features/administracion/producto/producto-form').then((m) => m.ProductoForm),
        title: 'Nuevo producto | Panadería Pavi',
        canActivate: [authGuard, rolGuard('ADMIN')],
      },
      {
        path: 'administracion/productos/:id/editar',
        loadComponent: () =>
          import('./features/administracion/producto/producto-form').then((m) => m.ProductoForm),
        title: 'Editar producto | Panadería Pavi',
        canActivate: [authGuard, rolGuard('ADMIN')],
      },
      {
        path: 'inventario/insumos',
        loadComponent: () =>
          import('./features/inventario/insumo/insumo-list').then((m) => m.InsumoList),
        title: 'Insumos | Panadería Pavi',
        canActivate: [authGuard, rolGuard('SUPERVISOR', 'ADMIN')],
      },
      {
        path: 'inventario/insumos/nuevo',
        loadComponent: () =>
          import('./features/inventario/insumo/insumo-form').then((m) => m.InsumoForm),
        title: 'Nuevo insumo | Panadería Pavi',
        canActivate: [authGuard, rolGuard('ADMIN')],
      },
      {
        path: 'inventario/insumos/:id/editar',
        loadComponent: () =>
          import('./features/inventario/insumo/insumo-form').then((m) => m.InsumoForm),
        title: 'Editar insumo | Panadería Pavi',
        canActivate: [authGuard, rolGuard('ADMIN')],
      },
      {
        path: 'inventario/existencias',
        loadComponent: () =>
          import('./features/inventario/existencia/existencia-list').then((m) => m.ExistenciaList),
        title: 'Existencias | Panadería Pavi',
        canActivate: [authGuard, rolGuard('SUPERVISOR', 'ADMIN')],
      },
      {
        path: 'inventario/existencias/nuevo',
        loadComponent: () =>
          import('./features/inventario/existencia/existencia-form').then((m) => m.ExistenciaForm),
        title: 'Nueva existencia | Panadería Pavi',
        canActivate: [authGuard, rolGuard('ADMIN')],
      },
      {
        path: 'inventario/existencias/:id/editar',
        loadComponent: () =>
          import('./features/inventario/existencia/existencia-form').then((m) => m.ExistenciaForm),
        title: 'Editar existencia | Panadería Pavi',
        canActivate: [authGuard, rolGuard('ADMIN')],
      },
      {
        path: 'compras/proveedores',
        loadComponent: () =>
          import('./features/compras/proveedor/proveedor-list').then((m) => m.ProveedorList),
        title: 'Proveedores | Panadería Pavi',
        canActivate: [authGuard, rolGuard('SUPERVISOR', 'ADMIN')],
      },
      {
        path: 'compras/proveedores/nuevo',
        loadComponent: () =>
          import('./features/compras/proveedor/proveedor-form').then((m) => m.ProveedorForm),
        title: 'Nuevo proveedor | Panadería Pavi',
        canActivate: [authGuard, rolGuard('ADMIN')],
      },
      {
        path: 'compras/proveedores/:id/editar',
        loadComponent: () =>
          import('./features/compras/proveedor/proveedor-form').then((m) => m.ProveedorForm),
        title: 'Editar proveedor | Panadería Pavi',
        canActivate: [authGuard, rolGuard('ADMIN')],
      },
      {
        path: 'compras/nueva',
        loadComponent: () =>
          import('./features/compras/compra/compra-form').then((m) => m.CompraForm),
        title: 'Nueva compra | Panadería Pavi',
        canActivate: [authGuard, rolGuard('ADMIN')],
      },
      {
        path: 'compras/reporte',
        loadComponent: () =>
          import('./features/compras/compra/compra-reporte').then((m) => m.CompraReporte),
        title: 'Reporte de compras | Panadería Pavi',
        canActivate: [authGuard, rolGuard('SUPERVISOR', 'ADMIN')],
      },
      {
        path: 'produccion/nueva',
        loadComponent: () =>
          import('./features/produccion/orden/orden-form').then((m) => m.OrdenForm),
        title: 'Nueva orden de producción | Panadería Pavi',
        canActivate: [authGuard, rolGuard('ADMIN')],
      },
      {
        path: 'produccion/ordenes',
        loadComponent: () =>
          import('./features/produccion/orden/orden-list').then((m) => m.OrdenList),
        title: 'Órdenes de producción | Panadería Pavi',
        canActivate: [authGuard, rolGuard('SUPERVISOR', 'ADMIN')],
      },
      {
        path: 'ventas/clientes',
        loadComponent: () =>
          import('./features/ventas/cliente/cliente-list').then((m) => m.ClienteList),
        title: 'Clientes | Panadería Pavi',
        canActivate: [authGuard],
      },
      {
        path: 'ventas/clientes/nuevo',
        loadComponent: () =>
          import('./features/ventas/cliente/cliente-form').then((m) => m.ClienteForm),
        title: 'Nuevo cliente | Panadería Pavi',
        canActivate: [authGuard],
      },
      {
        path: 'ventas/clientes/:id/editar',
        loadComponent: () =>
          import('./features/ventas/cliente/cliente-form').then((m) => m.ClienteForm),
        title: 'Editar cliente | Panadería Pavi',
        canActivate: [authGuard],
      },
      {
        path: 'ventas/nueva',
        loadComponent: () => import('./features/ventas/venta/venta-form').then((m) => m.VentaForm),
        title: 'Nueva venta | Panadería Pavi',
        canActivate: [authGuard, rolGuard('VENDEDOR', 'SUPERVISOR')],
      },
      {
        path: 'ventas/reporte',
        loadComponent: () =>
          import('./features/ventas/venta/venta-reporte').then((m) => m.VentaReporte),
        title: 'Reporte de ventas | Panadería Pavi',
        canActivate: [authGuard],
      },
      {
        path: 'ventas/pagos',
        loadComponent: () => import('./features/ventas/pago/pago-venta').then((m) => m.PagoVenta),
        title: 'Pagos | Panadería Pavi',
        canActivate: [authGuard],
      },
      {
        path: 'finanzas/movimientos',
        loadComponent: () =>
          import('./features/finanzas/movimiento/movimiento-list').then((m) => m.MovimientoList),
        title: 'Movimientos financieros | Panadería Pavi',
        canActivate: [authGuard, rolGuard('SUPERVISOR', 'ADMIN')],
      },
      {
        path: 'finanzas/reporte',
        loadComponent: () =>
          import('./features/finanzas/movimiento/reporte-financiero').then(
            (m) => m.ReporteFinanciero,
          ),
        title: 'Reporte financiero | Panadería Pavi',
        canActivate: [authGuard, rolGuard('SUPERVISOR', 'ADMIN')],
      },
      {
        path: 'sin-permiso',
        loadComponent: () => import('./core/sin-permiso/sin-permiso').then((m) => m.SinPermiso),
        title: 'Sin permiso | Panadería Pavi',
      },
      {
        path: '**',
        loadComponent: () =>
          import('./core/no-encontrado/no-encontrado').then((m) => m.NoEncontrado),
        title: 'Página no encontrada | Panadería Pavi',
      },
    ],
  },
];
