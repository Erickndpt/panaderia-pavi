WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

ALTER SESSION SET CONTAINER = FREEPDB1;

PROMPT === Administracion ===
@/scripts/S01_01_esquemas.sql
@/scripts/S01_02_tablas.sql

PROMPT === Inventario y Ventas ===
@/scripts/S04_01_esquemas.sql
@/scripts/S04_02_tablas.sql
@/scripts/S04_03_tablas_insumos.sql

PROMPT === Compras ===
@/scripts/MOD_COMPRAS_01_esquema.sql
@/scripts/MOD_COMPRAS_02_proveedores.sql
@/scripts/MOD_COMPRAS_03_compras.sql

PROMPT === Produccion ===
@/scripts/MOD_PRODUCCION_01_esquema.sql
@/scripts/MOD_PRODUCCION_02_ordenes.sql

PROMPT === Ventas incrementales ===
@/scripts/MOD_VENTAS_05_pagos.sql
@/scripts/MOD_VENTAS_06_seguridad_vendedor.sql
@/scripts/MOD_VENTAS_07_identidad_vendedor.sql

PROMPT === Finanzas ===
@/scripts/MOD_FINANZAS_01_esquema.sql
@/scripts/MOD_FINANZAS_02_movimientos.sql
@/scripts/MOD_FINANZAS_03_historico.sql

COMMIT;

PROMPT === Estructura de Panaderia Pavi creada correctamente ===
