/**
 * Contratos del módulo Producción. Reflejan exactamente los DTO del backend:
 * OrdenProduccionRequest, DetalleConsumoRequest, OrdenProduccionResponse,
 * DetalleConsumoResponse y el enum EstadoOrdenProduccion.
 *
 * El backend decide fecha, estado y los nombres históricos. El POST solo acepta
 * productoId, cantidadProducida e insumos (insumoId + cantidadConsumida).
 */

/** Enum real del backend: hoy solo existe COMPLETADA. */
export type EstadoOrdenProduccion = 'COMPLETADA';

export interface DetalleConsumoRequest {
  insumoId: number;
  cantidadConsumida: number;
}

export interface OrdenProduccionRequest {
  productoId: number;
  cantidadProducida: number;
  insumos: DetalleConsumoRequest[];
}

export interface DetalleConsumoResponse {
  id: number;
  insumoId: number;
  nombreInsumo: string;
  unidadMedida: string;
  cantidadConsumida: number;
}

export interface OrdenProduccionResponse {
  id: number;
  fecha: string;
  estado: EstadoOrdenProduccion;
  productoId: number;
  nombreProducto: string;
  cantidadProducida: number;
  detalles: DetalleConsumoResponse[];
}
