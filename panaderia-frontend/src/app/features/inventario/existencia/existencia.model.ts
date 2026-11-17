/**
 * Resumen del producto padre incrustado en la respuesta de la existencia.
 * Es distinto del modelo del padre: aquí solo viaja id y nombre.
 */
export interface ExistenciaProducto {
  id: number;
  nombre: string;
}

/** Respuesta del backend: incluye el nombre del producto, no solo su id. */
export interface ExistenciaResponse {
  id: number;
  producto: ExistenciaProducto;
  cantidad: number;
}

/** Petición del backend: producto seleccionado y cantidad, sin datos de respuesta. */
export interface ExistenciaRequest {
  productoId: number;
  cantidad: number;
}
