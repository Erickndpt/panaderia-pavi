-- Ejecutar como SYSTEM en FREEPDB1.
-- Agrega los datos legibles del vendedor que se muestran en la interfaz.
-- VENDEDOR_ID (claim sub) ya existe desde MOD_VENTAS_06; estas columnas
-- permiten mostrar username y nombre sin exponer el UUID.
-- El script es incremental y seguro ante ejecuciones repetidas.
-- Las ventas históricas conservan estas columnas en NULL; no se inventan
-- valores ni se intenta adivinar la identidad de usuarios anteriores.

-- ====================================================================
-- 1. PAN_VENTAS.VENTAS.VENDEDOR_USERNAME
-- ====================================================================

DECLARE
    v_existe NUMBER;
BEGIN
    SELECT COUNT(*)
    INTO v_existe
    FROM ALL_TAB_COLUMNS
    WHERE OWNER = 'PAN_VENTAS'
      AND TABLE_NAME = 'VENTAS'
      AND COLUMN_NAME = 'VENDEDOR_USERNAME';

    IF v_existe = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE PAN_VENTAS.VENTAS ADD (VENDEDOR_USERNAME VARCHAR2(100))';
        DBMS_OUTPUT.PUT_LINE('Columna PAN_VENTAS.VENTAS.VENDEDOR_USERNAME agregada.');
    ELSE
        DBMS_OUTPUT.PUT_LINE('La columna PAN_VENTAS.VENTAS.VENDEDOR_USERNAME ya existe; no se modifica.');
    END IF;
END;
/

-- ====================================================================
-- 2. PAN_VENTAS.VENTAS.VENDEDOR_NOMBRE
-- ====================================================================

DECLARE
    v_existe NUMBER;
BEGIN
    SELECT COUNT(*)
    INTO v_existe
    FROM ALL_TAB_COLUMNS
    WHERE OWNER = 'PAN_VENTAS'
      AND TABLE_NAME = 'VENTAS'
      AND COLUMN_NAME = 'VENDEDOR_NOMBRE';

    IF v_existe = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE PAN_VENTAS.VENTAS ADD (VENDEDOR_NOMBRE VARCHAR2(200))';
        DBMS_OUTPUT.PUT_LINE('Columna PAN_VENTAS.VENTAS.VENDEDOR_NOMBRE agregada.');
    ELSE
        DBMS_OUTPUT.PUT_LINE('La columna PAN_VENTAS.VENTAS.VENDEDOR_NOMBRE ya existe; no se modifica.');
    END IF;
END;
/

-- ====================================================================
-- 3. Comprobación del resultado
-- ====================================================================

SELECT OWNER, TABLE_NAME, COLUMN_NAME, DATA_TYPE, DATA_LENGTH, NULLABLE
FROM ALL_TAB_COLUMNS
WHERE OWNER = 'PAN_VENTAS'
  AND TABLE_NAME = 'VENTAS'
  AND COLUMN_NAME IN ('VENDEDOR_ID', 'VENDEDOR_USERNAME', 'VENDEDOR_NOMBRE')
ORDER BY COLUMN_NAME;

COMMIT;
