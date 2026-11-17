# Guía manual para SQL Power Architect — Panadería Pavi

Esta guía permite construir manualmente las 13 tablas del modelo. En SQL Power Architect se usarán tipos genéricos para que el diagrama sea fácil de leer.

## Convenciones para crear las columnas

- Todos los campos `ID` principales: `INTEGER`, `Primary Key = Sí`, `Allows Nulls = No`, `Auto Increment = Sí`.
- Campos que empiezan con `ID_` y apuntan a otra tabla: `INTEGER`, `Allows Nulls = No`, `Auto Increment = No`.
- **Decisión para esta guía académica:** usar `INTEGER` en todos los identificadores, de acuerdo con la indicación del docente. Para el volumen de este proyecto es suficiente y evita mezclar tipos en las relaciones.
- Una clave primaria y todas las claves foráneas que la referencian deben tener exactamente el mismo tipo. Por ejemplo, `CATEGORIAS.ID` y `PRODUCTOS.ID_CATEGORIA` deben ser ambos `INTEGER`.
- Dinero: `DECIMAL`, con la precisión y escala indicadas.
- Cantidades con fracciones: `DECIMAL(12,3)`.
- Texto: `VARCHAR` con el tamaño indicado.
- Fechas con hora: `TIMESTAMP`.
- Estado activo/inactivo: `BOOLEAN`.
- No marcar como `Primary Key` ninguna columna distinta de `ID`.
- Crear primero las 13 tablas sin relaciones. Agregar las claves foráneas al final.

---

## 1. PAN_ADMINISTRACION.CATEGORIAS

Nombre de tabla en el diagrama: `CATEGORIAS`

| Columna | Tipo | Precisión/Tamaño | Null | PK | Auto Increment | Unique |
|---|---|---:|---|---|---|---|
| ID | INTEGER | — | No | Sí | Sí | Sí por PK |
| NOMBRE | VARCHAR | 80 | No | No | No | Sí |
| DESCRIPCION | VARCHAR | 200 | Sí | No | No | No |

## 2. PAN_ADMINISTRACION.PRODUCTOS

Nombre de tabla en el diagrama: `PRODUCTOS`

| Columna | Tipo | Precisión/Tamaño | Null | PK | Auto Increment | Unique |
|---|---|---:|---|---|---|---|
| ID | INTEGER | — | No | Sí | Sí | Sí por PK |
| ID_CATEGORIA | INTEGER | — | No | No | No | No |
| NOMBRE | VARCHAR | 120 | No | No | No | No |
| PRECIO | DECIMAL | 10,2 | No | No | No | No |

## 3. PAN_INVENTARIO.INSUMOS

Nombre de tabla en el diagrama: `INSUMOS`

| Columna | Tipo | Precisión/Tamaño | Null | PK | Auto Increment | Unique |
|---|---|---:|---|---|---|---|
| ID | INTEGER | — | No | Sí | Sí | Sí por PK |
| NOMBRE | VARCHAR | 80 | No | No | No | Sí |
| UNIDAD_MEDIDA | VARCHAR | 20 | No | No | No | No |
| STOCK_ACTUAL | DECIMAL | 12,3 | No | No | No | No |
| STOCK_MINIMO | DECIMAL | 12,3 | No | No | No | No |

## 4. PAN_INVENTARIO.EXISTENCIAS

Nombre de tabla en el diagrama: `EXISTENCIAS`

| Columna | Tipo | Precisión/Tamaño | Null | PK | Auto Increment | Unique |
|---|---|---:|---|---|---|---|
| ID | INTEGER | — | No | Sí | Sí | Sí por PK |
| ID_PRODUCTO | INTEGER | — | No | No | No | Sí |
| CANTIDAD | DECIMAL | 12,3 | No | No | No | No |

`ID_PRODUCTO` es único porque cada producto posee una sola existencia acumulada.

## 5. PAN_PRODUCCION.ORDENES_PRODUCCION

Nombre de tabla en el diagrama: `ORDENES_PRODUCCION`

| Columna | Tipo | Precisión/Tamaño | Null | PK | Auto Increment | Unique |
|---|---|---:|---|---|---|---|
| ID | INTEGER | — | No | Sí | Sí | Sí por PK |
| ID_PRODUCTO | INTEGER | — | No | No | No | No |
| NOMBRE_PRODUCTO | VARCHAR | 120 | No | No | No | No |
| FECHA | TIMESTAMP | — | No | No | No | No |
| ESTADO | VARCHAR | 20 | No | No | No | No |
| CANTIDAD_PRODUCIDA | DECIMAL | 12,3 | No | No | No | No |

## 6. PAN_PRODUCCION.DETALLES_CONSUMO

Nombre de tabla en el diagrama: `DETALLES_CONSUMO`

| Columna | Tipo | Precisión/Tamaño | Null | PK | Auto Increment | Unique |
|---|---|---:|---|---|---|---|
| ID | INTEGER | — | No | Sí | Sí | Sí por PK |
| ID_ORDEN | INTEGER | — | No | No | No | No |
| ID_INSUMO | INTEGER | — | No | No | No | No |
| NOMBRE_INSUMO | VARCHAR | 80 | No | No | No | No |
| UNIDAD_MEDIDA | VARCHAR | 20 | No | No | No | No |
| CANTIDAD_CONSUMIDA | DECIMAL | 12,3 | No | No | No | No |

## 7. PAN_COMPRAS.PROVEEDORES

Nombre de tabla en el diagrama: `PROVEEDORES`

| Columna | Tipo | Precisión/Tamaño | Null | PK | Auto Increment | Unique |
|---|---|---:|---|---|---|---|
| ID | INTEGER | — | No | Sí | Sí | Sí por PK |
| RUC | VARCHAR | 11 | No | No | No | Sí |
| RAZON_SOCIAL | VARCHAR | 120 | No | No | No | No |
| TELEFONO | VARCHAR | 20 | Sí | No | No | No |
| EMAIL | VARCHAR | 120 | Sí | No | No | No |
| ACTIVO | BOOLEAN | — | No | No | No | No |

Valor predeterminado de `ACTIVO`: `TRUE`.

## 8. PAN_COMPRAS.COMPRAS

Nombre de tabla en el diagrama: `COMPRAS`

| Columna | Tipo | Precisión/Tamaño | Null | PK | Auto Increment | Unique |
|---|---|---:|---|---|---|---|
| ID | INTEGER | — | No | Sí | Sí | Sí por PK |
| ID_PROVEEDOR | INTEGER | — | No | No | No | No |
| FECHA | TIMESTAMP | — | No | No | No | No |
| ESTADO | VARCHAR | 20 | No | No | No | No |
| TOTAL | DECIMAL | 12,2 | No | No | No | No |

## 9. PAN_COMPRAS.DETALLE_COMPRAS

Nombre de tabla en el diagrama: `DETALLE_COMPRAS`

| Columna | Tipo | Precisión/Tamaño | Null | PK | Auto Increment | Unique |
|---|---|---:|---|---|---|---|
| ID | INTEGER | — | No | Sí | Sí | Sí por PK |
| ID_COMPRA | INTEGER | — | No | No | No | No |
| ID_INSUMO | INTEGER | — | No | No | No | No |
| NOMBRE_INSUMO | VARCHAR | 80 | No | No | No | No |
| UNIDAD_MEDIDA | VARCHAR | 20 | No | No | No | No |
| CANTIDAD | DECIMAL | 12,3 | No | No | No | No |
| PRECIO_UNITARIO | DECIMAL | 10,2 | No | No | No | No |
| SUBTOTAL | DECIMAL | 12,2 | No | No | No | No |

## 10. PAN_VENTAS.CLIENTES

Nombre de tabla en el diagrama: `CLIENTES`

| Columna | Tipo | Precisión/Tamaño | Null | PK | Auto Increment | Unique |
|---|---|---:|---|---|---|---|
| ID | INTEGER | — | No | Sí | Sí | Sí por PK |
| TIPO_DOCUMENTO | VARCHAR | 20 | No | No | No | No |
| NUMERO_DOCUMENTO | VARCHAR | 20 | Sí | No | No | Sí |
| NOMBRE_RAZON_SOCIAL | VARCHAR | 150 | No | No | No | No |
| TELEFONO | VARCHAR | 20 | Sí | No | No | No |
| EMAIL | VARCHAR | 120 | Sí | No | No | No |
| DIRECCION | VARCHAR | 200 | Sí | No | No | No |
| PAIS | VARCHAR | 80 | Sí | No | No | No |
| ACTIVO | BOOLEAN | — | No | No | No | No |

Valor predeterminado de `ACTIVO`: `TRUE`.

## 11. PAN_VENTAS.VENTAS

Nombre de tabla en el diagrama: `VENTAS`

| Columna | Tipo | Precisión/Tamaño | Null | PK | Auto Increment | Unique |
|---|---|---:|---|---|---|---|
| ID | INTEGER | — | No | Sí | Sí | Sí por PK |
| ID_CLIENTE | INTEGER | — | No | No | No | No |
| FECHA | TIMESTAMP | — | No | No | No | No |
| TIPO | VARCHAR | 20 | No | No | No | No |
| PAIS_DESTINO | VARCHAR | 80 | Sí | No | No | No |
| ESTADO | VARCHAR | 20 | No | No | No | No |
| TOTAL | DECIMAL | 12,2 | No | No | No | No |

## 12. PAN_VENTAS.DETALLE_VENTAS

Nombre de tabla en el diagrama: `DETALLE_VENTAS`

| Columna | Tipo | Precisión/Tamaño | Null | PK | Auto Increment | Unique |
|---|---|---:|---|---|---|---|
| ID | INTEGER | — | No | Sí | Sí | Sí por PK |
| ID_VENTA | INTEGER | — | No | No | No | No |
| ID_PRODUCTO | INTEGER | — | No | No | No | No |
| NOMBRE_PRODUCTO | VARCHAR | 120 | No | No | No | No |
| PRECIO_UNITARIO | DECIMAL | 10,2 | No | No | No | No |
| CANTIDAD | INTEGER | — | No | No | No | No |
| SUBTOTAL | DECIMAL | 12,2 | No | No | No | No |

## 13. PAN_FINANZAS.MOVIMIENTOS_FINANCIEROS

Nombre de tabla en el diagrama: `MOVIMIENTOS_FINANCIEROS`

| Columna | Tipo | Precisión/Tamaño | Null | PK | Auto Increment | Unique |
|---|---|---:|---|---|---|---|
| ID | INTEGER | — | No | Sí | Sí | Sí por PK |
| FECHA | TIMESTAMP | — | No | No | No | No |
| TIPO | VARCHAR | 20 | No | No | No | No |
| ORIGEN | VARCHAR | 20 | No | No | No | Parte de índice único |
| ORIGEN_ID | INTEGER | — | No | No | No | Parte de índice único |
| CONCEPTO | VARCHAR | 150 | No | No | No | No |
| MONTO | DECIMAL | 12,2 | No | No | No | No |
| ESTADO | VARCHAR | 20 | No | No | No | No |

Crear un índice `UNIQUE` compuesto por `ORIGEN` y `ORIGEN_ID`.

---

# Relaciones para agregar al final

## A. Relaciones físicas obligatorias

Estas seis relaciones coinciden exactamente con las claves foráneas de la base Oracle real.

| N.º | Tabla padre y columna | Tabla hija y columna | Cardinalidad | Nombre sugerido |
|---:|---|---|---|---|
| 1 | CATEGORIAS.ID | PRODUCTOS.ID_CATEGORIA | 1:N | FK_PRODUCTO_CATEGORIA |
| 2 | CLIENTES.ID | VENTAS.ID_CLIENTE | 1:N | FK_VENTA_CLIENTE |
| 3 | VENTAS.ID | DETALLE_VENTAS.ID_VENTA | 1:N | FK_DETALLE_VENTA |
| 4 | PROVEEDORES.ID | COMPRAS.ID_PROVEEDOR | 1:N | FK_COMPRA_PROVEEDOR |
| 5 | COMPRAS.ID | DETALLE_COMPRAS.ID_COMPRA | 1:N | FK_DETALLE_COMPRA |
| 6 | ORDENES_PRODUCCION.ID | DETALLES_CONSUMO.ID_ORDEN | 1:N | FK_DETALLE_ORDEN |

En cada relación, la columna `ID` del padre se conecta con el campo `ID_...` de la tabla hija. La FK se coloca siempre en la tabla hija.

## B. Relaciones lógicas para mostrar el modelo completo

Estas conexiones cruzan módulos. No existen como FK física en Oracle, pero se pueden dibujar en Power Architect para explicar el funcionamiento académico del sistema. Si Power Architect obliga a crear una FK, se puede crear solo dentro del archivo del diagrama; no se debe generar ni ejecutar ese DDL sobre la base real.

| N.º | Tabla padre y columna | Tabla hija y columna | Cardinalidad | Explicación |
|---:|---|---|---|---|
| 1 | PRODUCTOS.ID | EXISTENCIAS.ID_PRODUCTO | 1:1 | Un producto tiene una existencia; `ID_PRODUCTO` es único. |
| 2 | PRODUCTOS.ID | ORDENES_PRODUCCION.ID_PRODUCTO | 1:N | Un producto puede fabricarse en muchas órdenes. |
| 3 | PRODUCTOS.ID | DETALLE_VENTAS.ID_PRODUCTO | 1:N | Un producto puede aparecer en muchos detalles de venta. |
| 4 | INSUMOS.ID | DETALLE_COMPRAS.ID_INSUMO | 1:N | Un insumo puede aparecer en muchas compras. |
| 5 | INSUMOS.ID | DETALLES_CONSUMO.ID_INSUMO | 1:N | Un insumo puede consumirse en muchas órdenes. |

`MOVIMIENTOS_FINANCIEROS.ORIGEN_ID` no lleva una FK directa porque puede identificar una venta o una compra según el valor de `ORIGEN`.

## C. Relaciones muchos a muchos resultantes

No se dibuja una línea N:M directa. Se representa mediante la tabla intermedia correspondiente:

1. `VENTAS N:M PRODUCTOS` mediante `DETALLE_VENTAS`.
   - `VENTAS 1:N DETALLE_VENTAS`.
   - `PRODUCTOS 1:N DETALLE_VENTAS`.
2. `COMPRAS N:M INSUMOS` mediante `DETALLE_COMPRAS`.
   - `COMPRAS 1:N DETALLE_COMPRAS`.
   - `INSUMOS 1:N DETALLE_COMPRAS`.
3. `ORDENES_PRODUCCION N:M INSUMOS` mediante `DETALLES_CONSUMO`.
   - `ORDENES_PRODUCCION 1:N DETALLES_CONSUMO`.
   - `INSUMOS 1:N DETALLES_CONSUMO`.

## Orden recomendado para construir el diagrama

1. CATEGORIAS
2. PRODUCTOS
3. EXISTENCIAS
4. INSUMOS
5. ORDENES_PRODUCCION
6. DETALLES_CONSUMO
7. PROVEEDORES
8. COMPRAS
9. DETALLE_COMPRAS
10. CLIENTES
11. VENTAS
12. DETALLE_VENTAS
13. MOVIMIENTOS_FINANCIEROS
14. Agregar las seis relaciones físicas.
15. Agregar las cinco relaciones lógicas si el docente solicita el modelo funcional completo.

