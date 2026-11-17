# Panadería Pavi

Sistema integral para administrar catálogos, inventario, compras, producción,
ventas y finanzas de Panadería Pavi.

## Requisitos

- Git.
- Docker Desktop con el motor de Docker iniciado.
- Al menos 6 GB de memoria disponible para Docker.
- Conexión a Internet durante la primera ejecución.

Java, Maven, Node.js, npm, Oracle, PostgreSQL y Keycloak se ejecutan dentro de
contenedores; no es necesario instalarlos en la computadora anfitriona.

## Primera ejecución

```powershell
git clone https://github.com/Erickndpt/panaderia-pavi.git
Set-Location .\panaderia-pavi
powershell -ExecutionPolicy Bypass -File .\setup-dev.ps1
```

También puede ejecutarse directamente:

```powershell
docker compose up -d --build
```

La primera ejecución descarga imágenes, instala dependencias e inicializa
Oracle y Keycloak. Puede tardar varios minutos.

Los nombres predeterminados de los volúmenes conservan compatibilidad con el
entorno de desarrollo anterior del proyecto.

## Direcciones

| Servicio | Dirección |
|---|---|
| Frontend | <http://localhost:4200> |
| Backend | <http://localhost:8081> |
| Swagger | <http://localhost:8081/swagger-ui.html> |
| Keycloak | <http://localhost:38080> |
| Oracle | `localhost:1531/FREEPDB1` |

## Usuarios de demostración

Todos los usuarios utilizan la contraseña `Pavi2026*`.

| Usuario | Rol |
|---|---|
| `gerente1` | `GERENTE_GENERAL` |
| `admin1` | `ADMIN` |
| `supervisor1` | `SUPERVISOR` |
| `vendedor1` | `VENDEDOR` |

La consola administrativa de Keycloak utiliza `admin` / `admin123` solamente
en el entorno local de desarrollo.

## Uso diario

```powershell
powershell -ExecutionPolicy Bypass -File .\start-dev.ps1
```

Para detener los servicios sin borrar datos:

```powershell
powershell -ExecutionPolicy Bypass -File .\stop-dev.ps1
```

Nunca use `docker compose down -v` si desea conservar los datos.

## Desarrollo

Los directorios `src` del backend y frontend se montan dentro de los
contenedores. Angular detecta los cambios del frontend automáticamente.

Después de modificar código Java, reinicie el backend para recompilar:

```powershell
docker compose restart backend
```

Si modifica `pom.xml`, `package.json` o un Dockerfile, reconstruya el servicio:

```powershell
docker compose up -d --build backend
docker compose up -d --build frontend
```

## Diagnóstico

```powershell
docker compose ps
docker compose logs -f oracle
docker compose logs -f keycloak
docker compose logs -f backend
docker compose logs -f frontend
```

El backend utiliza `ddl-auto: validate`: valida la estructura creada por los
scripts SQL y nunca elimina ni modifica tablas automáticamente.

## Reinicio completo del entorno

Este procedimiento elimina todos los datos locales de Oracle y Keycloak y los
vuelve a crear desde los archivos del repositorio:

```powershell
docker compose down -v
docker compose up -d --build
```

Utilícelo únicamente cuando realmente desee borrar los datos locales.
