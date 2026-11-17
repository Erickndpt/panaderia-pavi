-- Ejecutar como SYSTEM en FREEPDB1.
-- Incorpora el propietario de la venta (claim sub del JWT de Keycloak)
-- al módulo Ventas.
-- El script es incremental y seguro ante ejecuciones repetidas: agrega
-- VENDEDOR_ID solo si todavía no existe.
-- NO borra ni actualiza las ventas existentes: las ventas históricas
-- conservan VENDEDOR_ID en NULL.
-- No ejecutar automáticamente; se aplica de forma manual.

-- ====================================================================
-- 1. PAN_VENTAS.VENTAS.VENDEDOR_ID
-- ====================================================================

DECLARE
    v_existe NUMBER;
BEGIN
    SELECT COUNT(*)
    INTO v_existe
    FROM ALL_TAB_COLUMNS
    WHERE OWNER = 'PAN_VENTAS'
      AND TABLE_NAME = 'VENTAS'
      AND COLUMN_NAME = 'VENDEDOR_ID';

    IF v_existe = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE PAN_VENTAS.VENTAS ADD (VENDEDOR_ID VARCHAR2(36))';
        DBMS_OUTPUT.PUT_LINE('Columna PAN_VENTAS.VENTAS.VENDEDOR_ID agregada.');
    ELSE
        DBMS_OUTPUT.PUT_LINE('La columna PAN_VENTAS.VENTAS.VENDEDOR_ID ya existe; no se modifica.');
    END IF;
END;
/

-- ====================================================================
-- 2. Comprobación del resultado
-- ====================================================================

SELECT OWNER, TABLE_NAME, COLUMN_NAME, DATA_TYPE, DATA_LENGTH, NULLABLE
FROM ALL_TAB_COLUMNS
WHERE OWNER = 'PAN_VENTAS'
  AND TABLE_NAME = 'VENTAS'
  AND COLUMN_NAME = 'VENDEDOR_ID';

COMMIT;
