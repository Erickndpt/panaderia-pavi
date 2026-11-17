# Tema de inicio de sesión de Panadería Pavi

Tema visual para las páginas de autenticación del realm `panaderia`. Hereda de
`keycloak.v2` y personaliza la presentación mediante CSS, mensajes y un SVG
local. No sobrescribe plantillas Freemarker ni agrega JavaScript.

## Estructura

```text
panaderia/
├── login/
│   ├── theme.properties
│   ├── messages/
│   │   ├── messages_en.properties
│   │   └── messages_es.properties
│   └── resources/
│       ├── css/login.css
│       └── img/
│           ├── panaderia-pavi-emblema.png
│           └── panaderia-pavi-logo.svg
└── README.md
```

## Montaje en Docker

`compose-dev.yml` monta la carpeta del tema en modo de solo lectura y conserva
el volumen persistente de Keycloak:

```yaml
volumes:
  - keycloak-data:/opt/keycloak/data
  - ./keycloak/themes/panaderia:/opt/keycloak/themes/panaderia:ro
```

El montaje se aplicará la próxima vez que se cree o inicie el servicio de
Keycloak mediante el flujo habitual del proyecto. No es necesario eliminar ni
recrear volúmenes.

## Activación manual

1. Abre la consola de administración de Keycloak.
2. Selecciona el realm `panaderia`.
3. Ve a **Realm settings → Themes**.
4. En **Login theme**, selecciona `panaderia`.
5. Guarda los cambios.

Para volver al diseño original, selecciona el tema predeterminado (`keycloak`)
en **Login theme** y guarda nuevamente.

## Alcance

Los cambios son exclusivamente visuales y se aplican solo a las páginas de
login de los realms que seleccionen este tema. No cambian PKCE, Direct Access
Grants, clientes, usuarios, roles, sesiones ni las reglas de autenticación.
