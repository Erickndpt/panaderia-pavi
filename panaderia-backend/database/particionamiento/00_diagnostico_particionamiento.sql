-- Panaderia Pavi - diagnostico previo al particionamiento
-- Script de solo lectura: no crea, modifica ni elimina objetos.

SET PAGESIZE 200
SET LINESIZE 220

PROMPT === 1. VERSION DE ORACLE ===
SELECT banner
FROM v$version
WHERE banner LIKE 'Oracle Database%';

PROMPT === 2. DISPONIBILIDAD DE PARTICIONAMIENTO ===
SELECT parameter, value
FROM v$option
WHERE parameter = 'Partitioning';

PROMPT === 3. ESTADO ACTUAL DE LAS TABLAS CANDIDATAS ===
SELECT owner,
       table_name,
       partitioned,
       num_rows
FROM all_tables
WHERE (owner = 'PAN_VENTAS' AND table_name IN ('VENTAS', 'DETALLE_VENTAS'))
   OR (owner = 'PAN_COMPRAS' AND table_name IN ('COMPRAS', 'DETALLE_COMPRAS'))
   OR (owner = 'PAN_PRODUCCION' AND table_name IN ('ORDENES_PRODUCCION', 'DETALLES_CONSUMO'))
   OR (owner = 'PAN_FINANZAS' AND table_name = 'MOVIMIENTOS_FINANCIEROS')
ORDER BY owner, table_name;

PROMPT === 4. COLUMNAS DE FECHA DISPONIBLES ===
SELECT owner,
       table_name,
       column_name,
       data_type,
       nullable
FROM all_tab_columns
WHERE column_name = 'FECHA'
  AND ((owner = 'PAN_VENTAS' AND table_name = 'VENTAS')
    OR (owner = 'PAN_COMPRAS' AND table_name = 'COMPRAS')
    OR (owner = 'PAN_PRODUCCION' AND table_name = 'ORDENES_PRODUCCION')
    OR (owner = 'PAN_FINANZAS' AND table_name = 'MOVIMIENTOS_FINANCIEROS'))
ORDER BY owner, table_name;

PROMPT === 5. CANTIDAD REAL DE FILAS ===
SELECT 'PAN_VENTAS.VENTAS' AS tabla, COUNT(*) AS filas FROM PAN_VENTAS.VENTAS
UNION ALL
SELECT 'PAN_VENTAS.DETALLE_VENTAS', COUNT(*) FROM PAN_VENTAS.DETALLE_VENTAS
UNION ALL
SELECT 'PAN_COMPRAS.COMPRAS', COUNT(*) FROM PAN_COMPRAS.COMPRAS
UNION ALL
SELECT 'PAN_COMPRAS.DETALLE_COMPRAS', COUNT(*) FROM PAN_COMPRAS.DETALLE_COMPRAS
UNION ALL
SELECT 'PAN_PRODUCCION.ORDENES_PRODUCCION', COUNT(*) FROM PAN_PRODUCCION.ORDENES_PRODUCCION
UNION ALL
SELECT 'PAN_PRODUCCION.DETALLES_CONSUMO', COUNT(*) FROM PAN_PRODUCCION.DETALLES_CONSUMO
UNION ALL
SELECT 'PAN_FINANZAS.MOVIMIENTOS_FINANCIEROS', COUNT(*) FROM PAN_FINANZAS.MOVIMIENTOS_FINANCIEROS
ORDER BY tabla;

PROMPT === 6. RESTRICCIONES ENTRE CABECERAS Y DETALLES ===
SELECT c.owner,
       c.table_name,
       c.constraint_name,
       c.constraint_type,
       c.r_owner,
       p.table_name AS tabla_referenciada
FROM all_constraints c
LEFT JOIN all_constraints p
  ON p.owner = c.r_owner
 AND p.constraint_name = c.r_constraint_name
WHERE (c.owner = 'PAN_VENTAS' AND c.table_name = 'DETALLE_VENTAS')
   OR (c.owner = 'PAN_COMPRAS' AND c.table_name = 'DETALLE_COMPRAS')
   OR (c.owner = 'PAN_PRODUCCION' AND c.table_name = 'DETALLES_CONSUMO')
ORDER BY c.owner, c.table_name, c.constraint_type, c.constraint_name;

PROMPT === FIN DEL DIAGNOSTICO: NO SE MODIFICO NINGUN DATO ===
