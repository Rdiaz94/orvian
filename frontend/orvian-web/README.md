# Orvian Web

Frontend de Orvian, hecho con Angular 21, Bootstrap 5.3 y Bootstrap Icons.

## Requisitos

- Node.js 20.19 o superior (probado con Node 22 y 24).
- La API de Orvian corriendo en `http://localhost:8080` (`mvn spring-boot:run`).

## Cómo ejecutarlo

1. `npm install` (si falla con un error de dependencias, usa `npm install --legacy-peer-deps`).
2. `npm start` y abre `http://localhost:4200`.

## Comandos útiles

| Comando | Qué hace |
|---|---|
| `npm start` | Servidor de desarrollo en el puerto 4200 |
| `npm test -- --watch=false` | Ejecuta las pruebas una vez |
| `npm run build` | Compila para producción en `dist/` |

## Estructura

| Carpeta | Contenido |
|---|---|
| `src/app/core` | Sesión, interceptores, guardas, modelos y servicios de la API |
| `src/app/layout` | Menú lateral y barra superior |
| `src/app/features` | Una carpeta por pantalla |
| `src/environments` | Dirección de la API según el entorno |

## Seguridad

- El token JWT se guarda en `sessionStorage` (se borra al cerrar la pestaña) y se envía solo a la API de Orvian.
- El rol del token solo sirve para mostrar u ocultar opciones; el servidor siempre valida los permisos.
- Si la API responde 401, la sesión se cierra y se vuelve al login.
