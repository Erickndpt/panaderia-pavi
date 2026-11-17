# Guía del backend panaderia-backend — Consolidación S6

Backend modular de la panadería construido con Spring Boot 4 y Spring Modulith. El código se organiza en módulos por paquete y cada módulo de negocio expone su propia API bajo `/api/v1`.

La última corrida de `.\mvnw.cmd clean test` fue de **187 pruebas, 0 fallos y 0 errores** (incluye los tests de `ModularityTests`, que verifican que ningún módulo depende de las implementaciones de otro).

## Módulos del backend

| Módulo | Responsabilidad | Base de la API |
|---|---|---|
| `administracion` | Catálogo: categorías y productos | `/api/v1/categorias`, `/api/v1/productos` |
| `inventario` | Materias primas (insumos) y existencias de producto terminado | `/api/v1/insumos`, `/api/v1/existencias` |
| `compras` | Proveedores y compras de insumos | `/api/v1/proveedores`, `/api/v1/compras` |
| `produccion` | Órdenes de producción ya completadas | `/api/v1/ordenes-produccion` |
| `ventas` | Ventas (LOCAL/EXPORTACION), sus detalles y reporte | `/api/v1/ventas`, `/api/v1/reportes/ventas` |
| `finanzas` | Movimientos financieros derivados (ingresos y egresos) y reporte | `/api/v1/movimientos-financieros`, `/api/v1/reportes/finanzas` |
| `exception` | Excepciones y mapeo a HTTP (módulo abierto, no es de negocio) | — |
| `config` | Configuración técnica (CORS) con límites abiertos | — |
| `filter` | Cabecera de traza `X-Trace-ID` (sin paquete de módulo) | — |

## Recorrido de extremo a extremo: compra → stock → producción → venta → finanzas

1. **Compra de harina.** `POST /api/v1/compras` → `CompraServiceImpl.registrar`:
   - Valida que el proveedor exista y esté activo (proveedor inactivo → 409).
   - Por cada detalle obtiene el insumo y **aumenta su stock** (`insumoService.aumentarStock`), calcula subtotales y total.
   - Persiste la compra con estado `REGISTRADA` y **registra el egreso financiero** (`finanzasService.registrarEgresoCompra`, movimiento `EGRESO`/`COMPRA` en estado `ACTIVO`).
   - Devuelve 201 con `CompraResponse`.
2. **Stock de insumos.** `GET /api/v1/insumos` muestra el `stockActual` de harina aumentado por la compra.
3. **Orden de producción completada.** `POST /api/v1/ordenes-produccion` → `OrdenProduccionServiceImpl.registrar`:
   - Registra una producción **ya realizada** (no programa ni ejecuta producción): **descuenta los insumos consumidos** (`insumoService.descontarStock`; si falta stock → 409) y **repone las existencias del producto terminado** (`existenciaService.reponer`).
   - Persiste la orden con estado `COMPLETADA` (único estado) y devuelve 201.
   - En este alcance **no hay anulación de órdenes de producción**: la producción ya ocurrió y sus efectos (insumos y existencias) no se revierten.
4. **Existencias de pan.** `GET /api/v1/existencias` (o `GET /api/v1/existencias/producto/{productoId}`) muestra el stock de pan repuesto por la orden.
5. **Venta local o de exportación.** `POST /api/v1/ventas` → `VentaServiceImpl.registrar`:
   - Valida el tipo (`LOCAL`/`EXPORTACION`): `EXPORTACION` exige `paisDestino` (si falta → 400) y `LOCAL` no debe incluirlo (si lo trae → 400).
   - Por cada detalle obtiene el producto y **descuenta su stock** (`existenciaService.descontar`, con bloqueo pesimista), calculando subtotales.
   - Persiste la venta con estado `REGISTRADA` y **registra el ingreso financiero** (`finanzasService.registrarIngresoVenta`, movimiento `INGRESO`/`VENTA` en estado `ACTIVO`).
   - Devuelve 201 con `VentaResponse`.
6. **Movimiento financiero.** `GET /api/v1/movimientos-financieros` lista los movimientos generados automáticamente por ventas y compras; `GET /api/v1/reportes/finanzas` resume **solo movimientos `ACTIVO`** (ingresos, egresos y saldo).

Todo ocurre en una sola transacción por operación: si un descuento o el movimiento fallan, se revierte todo.

## Anulaciones: qué revierte cada una

Las anulaciones **no borran** el documento original (venta/compra ni sus detalles) ni el movimiento financiero: lo conservan como histórico y marcan estados. Si falla cualquier paso, `@Transactional` revierte toda la operación.

### Anular venta
`PATCH /api/v1/ventas/{id}/anular` (sin cuerpo) → `VentaServiceImpl.anular`:
1. Busca la venta con `buscarConBloqueoPorId` (bloqueo pesimista sobre la fila y sus detalles).
2. Si no existe → 404; si ya está `ANULADA` → 409 (`VentaYaAnuladaException`).
3. Por cada detalle **repone el stock del producto** (`existenciaService.reponer`).
4. Cambia el estado a `ANULADA` y **anula el ingreso financiero** (`finanzasService.anularIngresoVenta`: el movimiento `INGRESO`/`VENTA` pasa de `ACTIVO` a `ANULADO`).
5. Devuelve 200 con `VentaResponse`. La venta anulada deja de contar en `GET /api/v1/ventas/resumen` y su movimiento en `GET /api/v1/reportes/finanzas`.

### Anular compra
`PATCH /api/v1/compras/{id}/anular` (sin cuerpo) → `CompraServiceImpl.anular`:
1. Busca la compra con `buscarConBloqueoPorId` (bloqueo pesimista sobre la fila y sus detalles).
2. Si no existe → 404; si ya está `ANULADA` → 409 (`CompraYaAnuladaException`).
3. Por cada detalle **descuenta del stock del insumo la cantidad de sus detalles** (`insumoService.descontarStock`); si el stock no alcanza → 409 (`InsumoStockInsuficienteException`).
4. Cambia el estado a `ANULADA` y **anula el egreso financiero** (`finanzasService.anularEgresoCompra`: el movimiento `EGRESO`/`COMPRA` pasa de `ACTIVO` a `ANULADO`).
5. Devuelve 200 con `CompraResponse`. La compra anulada deja de aportar su egreso a `GET /api/v1/reportes/finanzas`.

### Producción: sin anulación en este alcance
Las órdenes se registran **ya completadas** (estado único `COMPLETADA`). Se descuentan insumos y se reponen existencias al registrar, y este módulo **no ofrece un endpoint de anulación**.

## Módulos en detalle

### Administración
- **Categorías** (`/api/v1/categorias`): listar, obtener, crear (201), actualizar, eliminar (204).
- **Productos** (`/api/v1/productos`): listar con filtros (ver sección filtros), obtener, crear (201), actualizar, eliminar (204).

### Inventario
- **Insumos** (`/api/v1/insumos`): CRUD con `stockActual`, `stockMinimo` y `unidadMedida`. Los movimientos de stock (`aumentarStock`/`descontarStock`) son internos (los invocan compras y producción) y usan bloqueo pesimista; `descontarStock` lanza 409 si el stock no alcanza.
- **Existencias** (`/api/v1/existencias`): listar y obtener por producto. `descontar`/`reponer` son internos (los invocan ventas y producción) con bloqueo pesimista.

### Compras
- **Proveedores** (`/api/v1/proveedores`): CRUD con RUC único (409 si se duplica o hay conflicto). Un proveedor inactivo no puede participar en compras.
- **Compras** (`/api/v1/compras`): `POST` registrar, `GET` listar, `GET /{id}` obtener, `PATCH /{id}/anular` (ver anulaciones). Estados: `REGISTRADA`, `ANULADA`.

### Producción
- **Órdenes** (`/api/v1/ordenes-produccion`): `POST` registrar una producción ya completada (descuenta insumos + repone existencias), `GET` listar, `GET /{id}` obtener. Estado único `COMPLETADA`.

### Ventas
- **Ventas** (`/api/v1/ventas`): `GET` listar con filtros y orden por fecha, `GET /{id}` obtener, `POST` registrar (LOCAL/EXPORTACION), `PATCH /{id}/anular`, `GET /resumen`. Estados: `REGISTRADA`, `ANULADA`.
- **Reporte** (`/api/v1/reportes/ventas`): resumen de ventas `REGISTRADA` con filtros de fecha opcionales; devuelve el mismo `ReporteVentaResponse` que el `resumen` de ventas.

### Finanzas
- Los movimientos se **derivan automáticamente** de las operaciones de negocio:
  - Registrar venta → `INGRESO`/`VENTA` `ACTIVO`.
  - Anular venta → el ingreso pasa a `ANULADO`.
  - Registrar compra → `EGRESO`/`COMPRA` `ACTIVO`.
  - Anular compra → el egreso pasa a `ANULADO`.
- **Endpoints** (`/api/v1/movimientos-financieros`): `GET` listar y `GET /{id}` obtener; **reporte** (`/api/v1/reportes/finanzas`): cantidad y monto de movimientos `ACTIVO` (ingresos y egresos) y saldo = ingresos − egresos. Los movimientos `ANULADO` se excluyen.

## @Transactional

`@Transactional` agrupa la operación en una sola transacción de base de datos. Spring abre la transacción al entrar al método `@Transactional` y:
- al terminar normalmente, **commitea** (persiste estado, stock, detalles y movimientos);
- al lanzarse una excepción (RuntimeException), **revierte todo** (rollback), dejando la base en el estado anterior.

Ejemplos: registrar/anular una venta, registrar/anular una compra y registrar una orden de producción modifican stock, documento y movimiento dentro de una misma transacción.

## Bloqueo pesimista

`@Lock(PESSIMISTIC_WRITE)` genera `SELECT ... FOR UPDATE`: bloquea la fila mientras dura la transacción. Evita que dos operaciones concurrentes lean y modifiquen el mismo stock o el mismo documento al mismo tiempo. Se usa en:
- `ExistenciaRepository.buscarConBloqueoPorProducto` (descontar/reponer stock de producto).
- `InsumoRepository.buscarConBloqueoPorId` (aumentar/descontar stock de insumo).
- `VentaRepository.buscarConBloqueoPorId` (anular venta).
- `CompraRepository.buscarConBloqueoPorId` (anular compra).

## Filtros disponibles

Productos: `GET /api/v1/productos?nombre=&categoriaId=&precioMin=&precioMax=` (opcionales y combinables; `nombre` es búsqueda parcial sin distinguir mayúsculas/minúsculas).

Ventas: `GET /api/v1/ventas?tipo=&estado=&fechaDesde=&fechaHasta=&paisDestino=&productoId=&ordenFecha=`.
- `tipo`: `LOCAL` | `EXPORTACION`.
- `estado`: `REGISTRADA` | `ANULADA`.
- Fechas en ISO-8601, ej. `2026-09-01T00:00:00`.
- `paisDestino`: búsqueda parcial sin distinguir mayúsculas/minúsculas.
- `productoId`: ventas que contienen el producto (join con detalles + `DISTINCT`).
- `ordenFecha`: `ASC` o `DESC` (por defecto `DESC`), ordena solo por fecha.

## Reportes

- `GET /api/v1/ventas/resumen` y `GET /api/v1/reportes/ventas`: mismo resumen de ventas.
- Filtros opcionales `fechaDesde` y `fechaHasta`.
- Contabiliza **solo ventas `REGISTRADA`**: las `ANULADA` no cuentan en cantidades ni montos.
- `GET /api/v1/reportes/finanzas`: resumen de movimientos financieros **solo `ACTIVO`**; cantidad y monto de ingresos, cantidad y monto de egresos, y saldo (ingresos − egresos).

## Observabilidad con Prometheus

La aplicación expone métricas de Micrometer en formato Prometheus. Para verificarlas:

- **Métricas de la aplicación:** `http://localhost:8081/actuator/prometheus` devuelve las métricas en texto plano (formato Prometheus). El endpoint se habilita con la dependencia `micrometer-registry-prometheus` (runtime) y la exposición `prometheus` en `management.endpoints.web.exposure.include` de `application-dev.yml`.
- **Prometheus (Docker):** `http://localhost:39090/targets` muestra el estado del scrape; el objetivo `panaderia-backend` (job `panaderia-backend`, `metrics_path /actuator/prometheus`) debe aparecer como `UP`. El contenedor usa `obs/prometheus/prometheus-dev.yml` (`scrape_interval: 15s`) contra `host.docker.internal:8081`.

Para levantar Prometheus con Docker:

```bash
docker compose -f obs/compose-dev.yml up -d
```

El proyecto de compose es `panaderia-obs-dev` y publica el puerto del host `39090` hacia el `9090` del contenedor.

## CORS

Origen permitido `http://localhost:4200` para `/api/**`, expone la cabecera `X-Trace-ID` (la API devuelve esta cabecera en cada respuesta).

## Comandos

Requisitos: Docker encendido y la aplicación Spring Boot detenida.

```bash
# 1) Levantar Oracle y sus servicios con Docker
docker compose -f compose-dev.yml up -d

# 2) Ejecutar las pruebas (último resultado: 187 pruebas, 0 fallos, 0 errores)
.\mvnw.cmd clean test

# 3) Iniciar la aplicación (perfil dev)
.\mvnw.cmd spring-boot:run
```

## Enlaces

- Swagger UI: http://localhost:8081/swagger-ui.html
- Documento OpenAPI: http://localhost:8081/v3/api-docs
- Estado de salud: http://localhost:8081/actuator/health

## Secuencia corta de demostración

1. Abrir Swagger UI (`/swagger-ui.html`) o Postman.
2. `GET /api/v1/insumos` — ver la harina y su `stockActual`.
3. `POST /api/v1/compras` — registrar la compra de harina (proveedorId + detalles con insumoId, cantidad y precio).
4. `GET /api/v1/insumos` — confirmar el `stockActual` de harina aumentado.
5. `POST /api/v1/ordenes-produccion` — registrar una producción completada (productoId, cantidadProducida e insumos consumidos).
6. `GET /api/v1/existencias` — confirmar las existencias del pan repuestas.
7. `POST /api/v1/ventas` — registrar una venta LOCAL (o EXPORTACION con país) con un detalle (productoId + cantidad).
8. `GET /api/v1/ventas` — confirmar la venta con estado `REGISTRADA`.
9. `GET /api/v1/movimientos-financieros` — ver el ingreso por venta y el egreso por compra en estado `ACTIVO`.
10. `PATCH /api/v1/ventas/{id}/anular` — anular la venta: repone stock del pan y pasa el ingreso a `ANULADO`.
11. `PATCH /api/v1/compras/{id}/anular` — anular la compra: descuenta la harina de vuelta y pasa el egreso a `ANULADO`.
12. `GET /api/v1/ventas/resumen` y `GET /api/v1/reportes/finanzas` — verificar que ventas anuladas y movimientos anulados ya no se cuentan.