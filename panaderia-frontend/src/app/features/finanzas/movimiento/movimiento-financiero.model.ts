/**
 * Contratos de consulta del módulo financiero. Reflejan exactamente los DTO
 * del backend: MovimientoFinancieroResponse y ResumenFinancieroResponse, más
 * los enums TipoMovimiento, OrigenMovimiento y EstadoMovimiento.
 *
 * Los movimientos se crean automáticamente desde ventas y compras: el backend
 * no expone POST, PUT ni DELETE y este módulo es de solo consulta. Los
 * movimientos anulados siguen visibles; el resumen solo cuenta los ACTIVO.
 */

export type TipoMovimiento = 'INGRESO' | 'EGRESO';

export type OrigenMovimiento = 'VENTA' | 'COMPRA';

export type EstadoMovimiento = 'ACTIVO' | 'ANULADO';

export interface MovimientoFinancieroResponse {
  id: number;
  fecha: string;
  tipo: TipoMovimiento;
  origen: OrigenMovimiento;
  origenId: number;
  concepto: string;
  monto: number;
  estado: EstadoMovimiento;
}

/** El saldo es montoIngresos - montoEgresos considering solo movimientos ACTIVO. */
export interface ResumenFinancieroResponse {
  cantidadIngresos: number;
  montoIngresos: number;
  cantidadEgresos: number;
  montoEgresos: number;
  saldo: number;
}

const ETIQUETAS_TIPO: Record<TipoMovimiento, string> = {
  INGRESO: 'Ingreso',
  EGRESO: 'Egreso',
};

const ETIQUETAS_ORIGEN: Record<OrigenMovimiento, string> = {
  VENTA: 'Venta',
  COMPRA: 'Compra',
};

const ETIQUETAS_ESTADO: Record<EstadoMovimiento, string> = {
  ACTIVO: 'Activo',
  ANULADO: 'Anulado',
};

export function etiquetaTipoMovimiento(tipo: TipoMovimiento): string {
  return ETIQUETAS_TIPO[tipo] ?? tipo;
}

export function etiquetaOrigenMovimiento(origen: OrigenMovimiento): string {
  return ETIQUETAS_ORIGEN[origen] ?? origen;
}

export function etiquetaEstadoMovimiento(estado: EstadoMovimiento): string {
  return ETIQUETAS_ESTADO[estado] ?? estado;
}
