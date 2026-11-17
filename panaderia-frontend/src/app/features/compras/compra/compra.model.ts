/**
 * Contratos del flujo transaccional de Compras. Reflejan exactamente los DTO del
 * backend: CompraRequest, DetalleCompraRequest, CompraResponse, DetalleCompraResponse
 * y el enum EstadoCompra. El backend no acepta id, fecha, estado, subtotal ni total
 * en el POST: se envían únicamente proveedorId y los detalles.
 */
export type EstadoCompra = 'REGISTRADA' | 'ANULADA';

/**
 * Proveedor embebido en la respuesta de compra: solo id y razón social.
 * El RUC no forma parte de este resumen; el reporte lo resuelve desde el
 * catálogo de proveedores (una sola petición, no por compra).
 */
export interface ProveedorResumen {
  id: number;
  razonSocial: string;
}

export interface DetalleCompraRequest {
  insumoId: number;
  cantidad: number;
  precioUnitario: number;
}

export interface CompraRequest {
  proveedorId: number;
  detalles: DetalleCompraRequest[];
}

export interface DetalleCompraResponse {
  id: number;
  insumoId: number;
  nombreInsumo: string;
  unidadMedida: string;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
}

export interface CompraResponse {
  id: number;
  proveedor: ProveedorResumen;
  fecha: string;
  estado: EstadoCompra;
  total: number;
  detalles: DetalleCompraResponse[];
}
