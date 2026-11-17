/**
 * Contratos de pagos de venta. Reflejan exactamente los DTO del backend:
 * PagoVentaRequest, PagoVentaResponse y PagoVentaResumenResponse, más los
 * enums MetodoPago, EstadoPago y EstadoPagoVenta.
 *
 * El backend expone los pagos en endpoints propios bajo /ventas/{id}/pagos y
 * /pagos-venta/{id}: VentaResponse no incluye pagos y este módulo no altera
 * ese contrato. La fecha y el estado los fija el servidor (nacen REGISTRADO);
 * anular cambia el estado a ANULADO sin eliminar el registro.
 */

export type MetodoPago = 'EFECTIVO' | 'TARJETA' | 'TRANSFERENCIA' | 'YAPE' | 'PLIN' | 'OTRO';

export type EstadoPago = 'REGISTRADO' | 'ANULADO';

export type EstadoPagoVenta = 'PENDIENTE' | 'PARCIAL' | 'PAGADO';

export interface PagoVentaRequest {
  metodoPago: MetodoPago;
  monto: number;
  referencia: string | null;
}

export interface PagoVentaResponse {
  id: number;
  ventaId: number;
  fecha: string;
  metodoPago: MetodoPago;
  monto: number;
  referencia: string | null;
  estado: EstadoPago;
}

/** totalPagado y saldoPendiente solo consideran pagos REGISTRADO. */
export interface PagoVentaResumenResponse {
  ventaId: number;
  totalVenta: number;
  totalPagado: number;
  saldoPendiente: number;
  estadoPago: EstadoPagoVenta;
}

export const METODOS_PAGO: readonly MetodoPago[] = [
  'EFECTIVO',
  'TARJETA',
  'TRANSFERENCIA',
  'YAPE',
  'PLIN',
  'OTRO',
];

const ETIQUETAS_METODO: Record<MetodoPago, string> = {
  EFECTIVO: 'Efectivo',
  TARJETA: 'Tarjeta',
  TRANSFERENCIA: 'Transferencia',
  YAPE: 'Yape',
  PLIN: 'Plin',
  OTRO: 'Otro',
};

const ETIQUETAS_ESTADO_VENTA: Record<EstadoPagoVenta, string> = {
  PENDIENTE: 'Pendiente',
  PARCIAL: 'Parcial',
  PAGADO: 'Pagado',
};

const ETIQUETAS_ESTADO_PAGO: Record<EstadoPago, string> = {
  REGISTRADO: 'Registrado',
  ANULADO: 'Anulado',
};

export function etiquetaMetodoPago(metodo: MetodoPago): string {
  return ETIQUETAS_METODO[metodo] ?? metodo;
}

export function etiquetaEstadoPagoVenta(estado: EstadoPagoVenta): string {
  return ETIQUETAS_ESTADO_VENTA[estado] ?? estado;
}

export function etiquetaEstadoPago(estado: EstadoPago): string {
  return ETIQUETAS_ESTADO_PAGO[estado] ?? estado;
}
