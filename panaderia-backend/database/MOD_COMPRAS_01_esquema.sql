-- Ejecutar como SYSTEM en FREEPDB1.
-- Crear el esquema del módulo Compras.
-- PANADERIA_APP ya fue creado por Docker; no volver a crearlo.

CREATE USER PAN_COMPRAS
    IDENTIFIED BY "123456"
    DEFAULT TABLESPACE USERS
    TEMPORARY TABLESPACE TEMP
    QUOTA UNLIMITED ON USERS;

GRANT CREATE SESSION, CREATE TABLE
TO PAN_COMPRAS;

-- Comprobar el usuario.
SELECT username, account_status
FROM dba_users
WHERE username = 'PAN_COMPRAS';