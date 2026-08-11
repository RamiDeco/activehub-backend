# CLAUDE.md — ActiveHub API (Spring Boot)

Convenciones de este repositorio. Leer **antes** de escribir código. El contexto funcional completo y los contratos están en `design_handoff_activehub/`.

## Qué es
API REST de ActiveHub: plataforma de gestión de actividades deportivas/recreativas/formativas. Tres roles: **Alumno**, **Instructor**, **Administrador**.

## Stack
- Java + **Spring Boot** (REST), Spring Security (JWT), Spring Data JPA, **Flyway** para migraciones.
- DB: **PostgreSQL** (Supabase). Deploy en **Render**. Front separado en Next.js (Vercel) — en la práctica, el front real de este proyecto es un SPA en Vite/React (`activehub-frontend`, repo hermano), no Next.js; la API no cambia por eso.
- Pagos: interfaz `PaymentGateway` con **MockPaymentGateway** activo ahora; Mercado Pago real después (mismo contrato, otro perfil).

## Arquitectura — POR CASOS DE USO (vertical slice). NO negociable.
- **Cada caso de uso = un paquete bajo `com.activehub.usecases.<verbo+sustantivo>/`** con exactamente: `XxxController`, `XxxService`, `XxxRequest` (DTO in), `XxxResponse` (DTO out).
- Agregar funcionalidad = **paquete nuevo**, no tocar los existentes.
- **Controller**: solo HTTP ↔ DTO, delega en Service. Sin lógica.
- **Service**: única sede de reglas de negocio. `@Service`, `@Transactional` si escribe. Mapea entidad→DTO (las entidades JPA NUNCA salen por el Controller).
- **DTOs propios de cada slice.** No compartir DTOs entre casos de uso aunque se parezcan.
- **Entidades, enums y repositorios** compartidos viven en `com.activehub.domain.<agregado>/`. Los "sustantivos" se comparten; los "verbos" viven en su slice.
- Cross-cutting en `com.activehub.shared/` (`config`, `security`, `error`, `audit`, `persistence`, `payments`).

Estructura detallada y ejemplos: `design_handoff_activehub/01-ARQUITECTURA.md`.

## Reglas obligatorias
- **Baja lógica (soft-delete):** nunca `DELETE` físico. `BaseEntity.deleted` + filtro `deleted=false` por defecto. Conserva histórico para auditoría.
- **Auditoría:** toda operación crítica (alta de usuario, login, creación de admin y, a futuro, cobros/cancelaciones/denuncias) crea un `AuditLog` (actor, acción, entidad, entidadId, timestamp, metadata) vía `AuditService`.
- **Errores:** formato único `ApiError` (timestamp, status, code, message, fieldErrors, path) desde `GlobalExceptionHandler`. Códigos: VALIDACION(400), CREDENCIALES_INVALIDAS(401), SIN_PERMISO(403), NO_ENCONTRADO(404), EMAIL_EN_USO(409), ERROR_INTERNO(500).
- **Seguridad:** JWT Bearer, contraseñas con BCrypt. Rutas públicas: registro alumno/instructor y login. Crear admin: solo ADMIN.
- **Validación:** forma (campos, formato) con Bean Validation en el Request DTO; negocio (unicidad, cupos, ventanas de tiempo) en el Service.

## Estado actual / orden de trabajo
**Slice 1 (en curso):** Auth → Registrar Alumno, Registrar Instructor, Login, seed de un Admin inicial, y "un admin crea otro admin" (`POST /api/admin/usuarios/admin`). No hay registro público de admin. Specs: `design_handoff_activehub/03-CASOS-DE-USO-AUTH.md`.

**Siguientes:** catálogo/explorar, detalle de actividad, pre-inscripción/inscripción + pago (sobre el mock), gestión del instructor, reseñas, denuncias, panel admin.

## Reglas de negocio clave (para slices futuros — no implementar aún)
- Pre-inscripción vs inscripción según faltan **>4 días** (solo PreInscripción, no ocupa cupo) o **≤4 días** (inscripción definitiva si hay cupos, hasta 1h antes).
- Pago Mercado Pago → Inscripto (Retenido→Liberado). Efectivo → PagoPendiente hasta que el instructor confirma el cobro → Inscripto.
- Evitar doble ocupación del último cupo (control de concurrencia).
- Estados con nombre EXACTO — Inscripción: PreInscripción · PagoPendiente · Inscripto · Cancelada. Clase: Programada · Habilitada · Cancelada · Finalizada. Pago: Retenido · Liberado · Cancelado · Efectivo. Denuncia: Pendiente · En Auditoría · Resuelta.
- No confundir Categoría / TipoActividad / NivelIntensidad: son tres cosas distintas.

## Tests
Cada slice "andando" = test de Service (regla + caso de error como email duplicado) y test de Controller (status + shape del DTO) en verde.

## Idioma
Código y nombres de paquete/clase en español donde el dominio lo pida (entidades, casos de uso). Mensajes de usuario en español rioplatense.

## Frontend que consume esta API
`activehub-frontend` (repo hermano, Vite + React + TypeScript) ya implementa las 36 pantallas del prototipo con **auth y datos mockeados en memoria/localStorage** (`src/context/AuthContext.tsx`, `src/context/DataContext.tsx`). Cuando el slice de Auth de esta API esté andando, ese `AuthContext` se puede reemplazar por llamadas HTTP reales sin tocar las pantallas — respetá los contratos de `03-CASOS-DE-USO-AUTH.md` (rutas, shape de request/response, códigos de error) para que ese swap sea directo.
