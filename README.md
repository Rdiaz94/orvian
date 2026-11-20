# Orvian

Aplicación web de gestión académica y colaboración estudiantil. Curso: Soluciones Web y Aplicaciones Distribuidas.

| Parte | Tecnología | Carpeta |
|---|---|---|
| API REST | Java 17, Spring Boot 3.5, Spring Security con JWT, JPA | `backend/orvian-api` |
| Interfaz | Angular 21 y Bootstrap 5.3 | `frontend/orvian-web` |
| Base de datos | PostgreSQL | `base_datos` (scripts SQL) |

**Para ejecutarlo en tu computadora sigue `docs/GUIA_INSTALACION.md`.**

Reglas del equipo:

- Nunca subas `application-local.properties`, contraseñas, tokens ni capturas con datos sensibles.
- No trabajes sobre `main`: crea una rama por tarea (`feature/nombre-de-la-tarea`).
- Antes de subir cambios, `mvn clean test` (backend) y `npm test -- --watch=false` (frontend) deben pasar.
