/**
 * Contratos del flujo transaccional de Ventas. Reflejan exactamente los DTO del
 * backend: VentaRequest, DetalleVentaRequest, VentaResponse, DetalleVentaResponse,
 * ReporteVentaResponse y los enums TipoVenta y EstadoVenta.
 *
 * El backend no acepta id, fecha, estado, nombreProducto, precioUnitario, subtotal
 * ni total en el POST: solo clienteId, tipo, paisDestino y los detalles
 * (productoId + cantidad). El precio vigente, los subtotales, el total, la fecha y
 * el estado los resuelve el servidor.
 */
import { TipoDocumento } from '../cliente/cliente.model';

export type TipoVenta = 'LOCAL' | 'EXPORTACION';

export type EstadoVenta = 'REGISTRADA' | 'ANULADA';

/** Cliente embebido en la respuesta de venta (equivale a ClienteResumen del backend). */
export interface ClienteResumen {
  id: number;
  tipoDocumento: TipoDocumento;
  numeroDocumento: string | null;
  nombreRazonSocial: string;
}

export interface VendedorResumen {
  id: string | null;
  username: string | null;
  nombre: string | null;
}

export function nombreVendedor(vendedor: VendedorResumen | null | undefined): string {
  const nombre = vendedor?.nombre?.trim();
  if (nombre) {
    return nombre;
  }

  const username = vendedor?.username?.trim();
  return username ? username : 'Vendedor no identificado';
}

export interface DetalleVentaRequest {
  productoId: number;
  cantidad: number;
}

export interface VentaRequest {
  clienteId: number;
  tipo: TipoVenta;
  paisDestino: string | null;
  detalles: DetalleVentaRequest[];
}

export interface DetalleVentaResponse {
  id: number;
  productoId: number;
  nombreProducto: string;
  precioUnitario: number;
  cantidad: number;
  subtotal: number;
}

export interface VentaResponse {
  id: number;
  fecha: string;
  tipo: TipoVenta;
  paisDestino: string | null;
  cliente: ClienteResumen;
  estado: EstadoVenta;
  total: number;
  vendedor: VendedorResumen | null;
  detalles: DetalleVentaResponse[];
}

/** Reporte resumido: solo cuenta ventas REGISTRADA (excluye las ANULADA). */
export interface ReporteVentaResponse {
  cantidadVentas: number;
  montoTotal: number;
  cantidadLocales: number;
  montoLocal: number;
  cantidadExportaciones: number;
  montoExportacion: number;
}

/** Filtros admitidos por GET /api/v1/ventas. */
export interface VentaFiltros {
  tipo?: TipoVenta;
  estado?: EstadoVenta;
  fechaDesde?: string;
  fechaHasta?: string;
  paisDestino?: string;
  productoId?: number;
  clienteId?: number;
  ordenFecha?: 'ASC' | 'DESC';
}
