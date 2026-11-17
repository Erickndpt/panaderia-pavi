-- Ejecutar como SYSTEM en FREEPDB1, después de crear el esquema (01)
-- y la tabla (02). Migración idempotente: reproduce con NOT EXISTS la
-- regla automática del módulo Finanzas para las ventas y compras ya
-- registradas en la base antes de la implantación.
--   * Venta REGISTRADA      -> INGRESO  ACTIVO
--   * Venta ANULADA         -> INGRESO  ANULADO (conserva el histórico)
--   * Compra REGISTRADA     -> EGRESO   ACTIVO

INSERT INTO PAN_FINANZAS.MOVIMIENTOS_FINANCIEROS
    (FECHA, TIPO, ORIGEN, ORIGEN_ID, CONCEPTO, MONTO, ESTADO)
SELECT v.FECHA, 'INGRESO', 'VENTA', v.ID,
       'Ingreso por venta ' || v.ID || ' (histórico)',
       v.TOTAL, 'ACTIVO'
FROM PAN_VENTAS.VENTAS v
WHERE v.ESTADO = 'REGISTRADA'
  AND NOT EXISTS (
      SELECT 1
      FROM PAN_FINANZAS.MOVIMIENTOS_FINANCIEROS m
      WHERE m.ORIGEN = 'VENTA'
        AND m.ORIGEN_ID = v.ID
  );

INSERT INTO PAN_FINANZAS.MOVIMIENTOS_FINANCIEROS
    (FECHA, TIPO, ORIGEN, ORIGEN_ID, CONCEPTO, MONTO, ESTADO)
SELECT v.FECHA, 'INGRESO', 'VENTA', v.ID,
       'Ingreso por venta ' || v.ID || ' (histórico)',
       v.TOTAL, 'ANULADO'
FROM PAN_VENTAS.VENTAS v
WHERE v.ESTADO = 'ANULADA'
  AND NOT EXISTS (
      SELECT 1
      FROM PAN_FINANZAS.MOVIMIENTOS_FINANCIEROS m
      WHERE m.ORIGEN = 'VENTA'
        AND m.ORIGEN_ID = v.ID
  );

INSERT INTO PAN_FINANZAS.MOVIMIENTOS_FINANCIEROS
    (FECHA, TIPO, ORIGEN, ORIGEN_ID, CONCEPTO, MONTO, ESTADO)
SELECT c.FECHA, 'EGRESO', 'COMPRA', c.ID,
       'Egreso por compra ' || c.ID || ' (histórico)',
       c.TOTAL, 'ACTIVO'
FROM PAN_COMPRAS.COMPRAS c
WHERE c.ESTADO = 'REGISTRADA'
  AND NOT EXISTS (
      SELECT 1
      FROM PAN_FINANZAS.MOVIMIENTOS_FINANCIEROS m
      WHERE m.ORIGEN = 'COMPRA'
        AND m.ORIGEN_ID = c.ID
  );

-- ====================================================================
-- Consultas de comprobación
-- ====================================================================

-- Movimiento generado por la venta 1.
SELECT id, fecha, tipo, origen, origen_id, concepto, monto, estado
FROM PAN_FINANZAS.MOVIMIENTOS_FINANCIEROS
WHERE origen = 'VENTA' AND origen_id = 1;

-- Movimiento generado por la venta 2.
SELECT id, fecha, tipo, origen, origen_id, concepto, monto, estado
FROM PAN_FINANZAS.MOVIMIENTOS_FINANCIEROS
WHERE origen = 'VENTA' AND origen_id = 2;

-- Movimiento generado por la compra 1.
SELECT id, fecha, tipo, origen, origen_id, concepto, monto, estado
FROM PAN_FINANZAS.MOVIMIENTOS_FINANCIEROS
WHERE origen = 'COMPRA' AND origen_id = 1;

-- Totales por tipo y estado.
SELECT tipo, estado, COUNT(*) AS cantidad, SUM(monto) AS monto
FROM PAN_FINANZAS.MOVIMIENTOS_FINANCIEROS
GROUP BY tipo, estado
ORDER BY tipo, estado;

-- Resumen de movimientos activos (equivalente al endpoint
-- GET /api/v1/reportes/finanzas): excluye los anulados.
SELECT COUNT(CASE WHEN tipo = 'INGRESO' THEN 1 END) AS cantidad_ingresos,
       SUM(CASE WHEN tipo = 'INGRESO' THEN monto END) AS monto_ingresos,
       COUNT(CASE WHEN tipo = 'EGRESO' THEN 1 END) AS cantidad_egresos,
       SUM(CASE WHEN tipo = 'EGRESO' THEN monto END) AS monto_egresos,
       SUM(CASE WHEN tipo = 'INGRESO' THEN monto
                WHEN tipo = 'EGRESO' THEN -monto END) AS saldo
FROM PAN_FINANZAS.MOVIMIENTOS_FINANCIEROS
WHERE estado = 'ACTIVO';



COMMIT;