/**
 * Contratos de Producto. Reflejan ProductoRequest y ProductoResponse del backend.
 * El producto no tiene `activo`.
 */

/** Resumen de la categoría padre incrustado en la respuesta del producto. */
export interface CategoriaResumen {
  id: number;
  nombre: string;
}

/**
 * Proyección mínima del producto padre (módulo Administración) que consume el
 * módulo Inventario. No incluye precio ni categoría: la existencia solo necesita
 * identificar y nombrar el producto.
 */
export interface ProductoResumen {
  id: number;
  nombre: string;
}

/** Respuesta real del backend: incluye precio y categoría. */
export interface Producto {
  id: number;
  nombre: string;
  precio: number;
  categoria: CategoriaResumen;
}

/** Petición real del backend. */
export interface ProductoRequest {
  nombre: string;
  precio: number;
  categoriaId: number;
}

/** Filtros admitidos por el endpoint GET /api/v1/productos. */
export interface ProductoFiltros {
  nombre?: string;
  categoriaId?: number;
  precioMin?: number;
  precioMax?: number;
}
