# Modelo relacional — Panadería Pavi

## Herramienta recomendada

Usar **Oracle SQL Developer Data Modeler**. Evitar reconstruir las tablas manualmente en SQL Power Architect, porque ese programa antiguo convierte tipos Oracle como `NUMBER(10,2)` de forma imprecisa.

### Importación desde Oracle

1. Abrir Oracle SQL Developer Data Modeler.
2. Ir a `File > Import > Data Dictionary`.
3. Crear la conexión Oracle:
   - Host: `localhost`
   - Puerto: `1531`
   - Service name: `FREEPDB1`
   - Usuario con acceso al diccionario: `SYSTEM`
4. Probar y seleccionar la conexión.
5. Seleccionar estos esquemas:
   - `PAN_ADMINISTRACION`
   - `PAN_INVENTARIO`
   - `PAN_PRODUCCION`
   - `PAN_COMPRAS`
   - `PAN_VENTAS`
   - `PAN_FINANZAS`
6. Seleccionar las 13 tablas indicadas abajo y finalizar la importación.
7. Guardar el diseño como `Modelo_Relacional_Panaderia.dmd`.

También se puede usar `File > Import > DDL File` si se entrega un único archivo SQL con todas las tablas.

## Tablas maestras o no transaccionales

### PAN_ADMINISTRACION.CATEGORIAS

- `ID NUMBER` — PK, identidad
- `NOMBRE VARCHAR2(80)` — obligatorio, único
- `DESCRIPCION VARCHAR2(200)`

### PAN_ADMINISTRACION.PRODUCTOS

- `ID NUMBER` — PK, identidad
- `ID_CATEGORIA NUMBER` — FK a `CATEGORIAS.ID`
- `NOMBRE VARCHAR2(120)` — obligatorio
- `PRECIO NUMBER(10,2)` — obligatorio

### PAN_INVENTARIO.INSUMOS

- `ID NUMBER` — PK, identidad
- `NOMBRE VARCHAR2(80)` — obligatorio, único
- `UNIDAD_MEDIDA VARCHAR2(20)` — obligatorio
- `STOCK_ACTUAL NUMBER(12,3)` — obligatorio
- `STOCK_MINIMO NUMBER(12,3)` — obligatorio

### PAN_INVENTARIO.EXISTENCIAS

- `ID NUMBER` — PK, identidad
- `ID_PRODUCTO NUMBER` — identificador lógico de producto, único
- `CANTIDAD NUMBER(12,3)` — obligatorio

### PAN_COMPRAS.PROVEEDORES

- `ID NUMBER` — PK, identidad
- `RUC VARCHAR2(11)` — obligatorio, único
- `RAZON_SOCIAL VARCHAR2(120)` — obligatorio
- `TELEFONO VARCHAR2(20)`
- `EMAIL VARCHAR2(120)`
- `ACTIVO BOOLEAN` — obligatorio

### PAN_VENTAS.CLIENTES

- `ID NUMBER` — PK, identidad
- `TIPO_DOCUMENTO VARCHAR2(20)` — obligatorio
- `NUMERO_DOCUMENTO VARCHAR2(20)` — único cuando existe
- `NOMBRE_RAZON_SOCIAL VARCHAR2(150)` — obligatorio
- `TELEFONO VARCHAR2(20)`
- `EMAIL VARCHAR2(120)`
- `DIRECCION VARCHAR2(200)`
- `PAIS VARCHAR2(80)`
- `ACTIVO BOOLEAN` — obligatorio

## Tablas transaccionales

### PAN_VENTAS.VENTAS

- `ID NUMBER` — PK, identidad
- `ID_CLIENTE NUMBER` — FK a `CLIENTES.ID`
- `FECHA TIMESTAMP` — obligatorio
- `TIPO VARCHAR2(20)` — `LOCAL` o `EXPORTACION`
- `PAIS_DESTINO VARCHAR2(80)`
- `ESTADO VARCHAR2(20)` — obligatorio
- `TOTAL NUMBER(12,2)` — obligatorio

### PAN_VENTAS.DETALLE_VENTAS

- `ID NUMBER` — PK, identidad
- `ID_VENTA NUMBER` — FK a `VENTAS.ID`
- `ID_PRODUCTO NUMBER` — identificador lógico de producto
- `NOMBRE_PRODUCTO VARCHAR2(120)` — copia histórica
- `PRECIO_UNITARIO NUMBER(10,2)` — obligatorio
- `CANTIDAD NUMBER(10)` — obligatorio
- `SUBTOTAL NUMBER(12,2)` — obligatorio

### PAN_COMPRAS.COMPRAS

- `ID NUMBER` — PK, identidad
- `ID_PROVEEDOR NUMBER` — FK a `PROVEEDORES.ID`
- `FECHA TIMESTAMP` — obligatorio
- `ESTADO VARCHAR2(20)` — obligatorio
- `TOTAL NUMBER(12,2)` — obligatorio

### PAN_COMPRAS.DETALLE_COMPRAS

- `ID NUMBER` — PK, identidad
- `ID_COMPRA NUMBER` — FK a `COMPRAS.ID`
- `ID_INSUMO NUMBER` — identificador lógico de insumo
- `NOMBRE_INSUMO VARCHAR2(80)` — copia histórica
- `UNIDAD_MEDIDA VARCHAR2(20)` — copia histórica
- `CANTIDAD NUMBER(12,3)` — obligatorio
- `PRECIO_UNITARIO NUMBER(10,2)` — obligatorio
- `SUBTOTAL NUMBER(12,2)` — obligatorio

### PAN_PRODUCCION.ORDENES_PRODUCCION

- `ID NUMBER` — PK, identidad
- `ID_PRODUCTO NUMBER` — identificador lógico de producto
- `NOMBRE_PRODUCTO VARCHAR2(120)` — copia histórica
- `FECHA TIMESTAMP` — obligatorio
- `ESTADO VARCHAR2(20)` — obligatorio
- `CANTIDAD_PRODUCIDA NUMBER(12,3)` — obligatorio

### PAN_PRODUCCION.DETALLES_CONSUMO

- `ID NUMBER` — PK, identidad
- `ID_ORDEN NUMBER` — FK a `ORDENES_PRODUCCION.ID`
- `ID_INSUMO NUMBER` — identificador lógico de insumo
- `NOMBRE_INSUMO VARCHAR2(80)` — copia histórica
- `UNIDAD_MEDIDA VARCHAR2(20)` — copia histórica
- `CANTIDAD_CONSUMIDA NUMBER(12,3)` — obligatorio

### PAN_FINANZAS.MOVIMIENTOS_FINANCIEROS

- `ID NUMBER` — PK, identidad
- `FECHA TIMESTAMP` — obligatorio
- `TIPO VARCHAR2(20)` — `INGRESO` o `EGRESO`
- `ORIGEN VARCHAR2(20)` — `VENTA` o `COMPRA`
- `ORIGEN_ID NUMBER` — identificador de la venta o compra
- `CONCEPTO VARCHAR2(150)` — obligatorio
- `MONTO NUMBER(12,2)` — obligatorio
- `ESTADO VARCHAR2(20)` — obligatorio
- Restricción única: `(ORIGEN, ORIGEN_ID)`

## Relaciones físicas que Oracle dibuja automáticamente

| Padre | Cardinalidad | Hija | Clave foránea |
|---|---:|---|---|
| CATEGORIAS | 1:N | PRODUCTOS | `PRODUCTOS.ID_CATEGORIA` |
| CLIENTES | 1:N | VENTAS | `VENTAS.ID_CLIENTE` |
| VENTAS | 1:N | DETALLE_VENTAS | `DETALLE_VENTAS.ID_VENTA` |
| PROVEEDORES | 1:N | COMPRAS | `COMPRAS.ID_PROVEEDOR` |
| COMPRAS | 1:N | DETALLE_COMPRAS | `DETALLE_COMPRAS.ID_COMPRA` |
| ORDENES_PRODUCCION | 1:N | DETALLES_CONSUMO | `DETALLES_CONSUMO.ID_ORDEN` |

## Relaciones lógicas entre módulos

Estas relaciones existen funcionalmente, pero no tienen una FK física porque conectan módulos/esquemas distintos. Si el docente pide mostrarlas, se agregan manualmente al diagrama con una línea discontinua o se explican como relaciones lógicas.

| Origen | Cardinalidad | Destino | Campo lógico |
|---|---:|---|---|
| PRODUCTOS | 1:1 | EXISTENCIAS | `EXISTENCIAS.ID_PRODUCTO` es único |
| PRODUCTOS | 1:N | ORDENES_PRODUCCION | `ORDENES_PRODUCCION.ID_PRODUCTO` |
| PRODUCTOS | 1:N | DETALLE_VENTAS | `DETALLE_VENTAS.ID_PRODUCTO` |
| INSUMOS | 1:N | DETALLES_CONSUMO | `DETALLES_CONSUMO.ID_INSUMO` |
| INSUMOS | 1:N | DETALLE_COMPRAS | `DETALLE_COMPRAS.ID_INSUMO` |
| VENTAS o COMPRAS | 1:1 opcional | MOVIMIENTOS_FINANCIEROS | `ORIGEN + ORIGEN_ID` |

## Relaciones muchos a muchos solicitadas

Las relaciones muchos a muchos se resuelven con una tabla intermedia:

1. **VENTAS N:M PRODUCTOS**, resuelta por `DETALLE_VENTAS`.
   - Una venta contiene muchos productos.
   - Un producto puede aparecer en muchas ventas.
2. **COMPRAS N:M INSUMOS**, resuelta por `DETALLE_COMPRAS`.
   - Una compra contiene muchos insumos.
   - Un insumo puede aparecer en muchas compras.
3. **ORDENES_PRODUCCION N:M INSUMOS**, resuelta por `DETALLES_CONSUMO`.
   - Una orden consume muchos insumos.
   - Un insumo puede consumirse en muchas órdenes.

## Distribución sugerida del diagrama

- Izquierda: `CATEGORIAS -> PRODUCTOS -> EXISTENCIAS`.
- Centro superior: `CLIENTES -> VENTAS -> DETALLE_VENTAS -> PRODUCTOS`.
- Centro inferior: `PROVEEDORES -> COMPRAS -> DETALLE_COMPRAS -> INSUMOS`.
- Derecha: `PRODUCTOS -> ORDENES_PRODUCCION -> DETALLES_CONSUMO -> INSUMOS`.
- Parte inferior: `MOVIMIENTOS_FINANCIEROS`, conectado lógicamente con `VENTAS` y `COMPRAS`.

