# CLAUDE.md — ActiveHub API (Spring Boot)

Convenciones de este repositorio. Leer **antes** de escribir código. El contexto funcional completo y los contratos originales están en `design_handoff_activehub/` (documento de la facultad — ver la sección "Desviaciones respecto al documento original" más abajo para lo que cambió en la práctica).

## Qué es
API REST de ActiveHub: plataforma de gestión de actividades deportivas/recreativas/formativas. Tres roles: **Alumno**, **Instructor**, **Administrador**. Repo hermano `activehub-frontend` (Vite + React + TypeScript) es el único consumidor.

## Stack
- Java 21 + **Spring Boot 4.1** (REST), Spring Security 7.1 (JWT stateless), Spring Data JPA, **Flyway** para migraciones.
- DB: **PostgreSQL 16** vía Docker local (`activehub-postgres`, puerto 5433 → 5432). Ver README para el `docker run` exacto.
- Pagos: interfaz `PaymentGateway` con **MockPaymentGateway** activo (sin integración real con Mercado Pago todavía — ítem 12 del roadmap).
- Sin Redis, sin deploy: todo corre en local por ahora (Docker Postgres + `./mvnw spring-boot:run`).

## Arquitectura — POR CASOS DE USO (vertical slice). NO negociable.
- **Cada caso de uso = un paquete bajo `com.activehub.usecases.<verbo+sustantivo>/`** con exactamente: `XxxController`, `XxxService`, `XxxRequest` (DTO in, cuando aplica), `XxxResponse` (DTO out).
- Agregar funcionalidad = **paquete nuevo**, no tocar los existentes (salvo que el caso de uso existente deba ganar un efecto secundario nuevo — ver ejemplo de notificaciones más abajo, ahí sí se edita el usecase existente).
- **Controller**: solo HTTP ↔ DTO, delega en Service. Sin lógica. Actor/usuario logueado siempre sale de `(UUID) authentication.getPrincipal()`, nunca del body.
- **Service**: única sede de reglas de negocio. `@Service`, `@Transactional` si escribe. Mapea entidad→DTO (las entidades JPA NUNCA salen por el Controller).
- **DTOs propios de cada slice.** No compartir DTOs entre casos de uso aunque se parezcan.
- **Entidades, enums y repositorios** compartidos viven en `com.activehub.domain.<agregado>/`. Los "sustantivos" se comparten; los "verbos" viven en su slice.
- Cross-cutting en `com.activehub.shared/` (`config`, `security`, `error`, `audit`, `notificacion`, `persistence`, `payments`).
- **No se inyecta un usecase Service dentro de otro.** Si dos casos de uso necesitan la misma cascada (ej. cancelar inscripciones + reintegrar pagos), cada uno la implementa inline — es la convención ya establecida en `cancelarclase`, `notificarausenciaprofesor` y `eliminaractividad`, preferida sobre acoplar slices entre sí aunque sea menos DRY en el papel.

Estructura detallada y ejemplos: `design_handoff_activehub/01-ARQUITECTURA.md` (contexto original; la lista de casos de uso ahí quedó desactualizada, la real está más abajo).

## Reglas obligatorias
- **Baja lógica (soft-delete):** nunca `DELETE` físico en entidades de catálogo/negocio (`Actividad`, `Clase`, `TipoActividad`, `Categoria`, `Usuario`...). `BaseEntity.deleted` + `@SQLRestriction("deleted = false")` + `marcarBorrado()`. Antes de un soft-delete, si la entidad tiene hijos activos (ej. Categoria con TipoActividad, TipoActividad con Actividad), el Service lanza una excepción `XxxEnUsoException` (409) en vez de borrar.
  - `Denuncia`, `Inscripcion`, `Notificacion`, `Penalizacion`, `AuditLog`, `ActividadFavorita` **no** extienden BaseEntity a propósito: son registros con su propio estado terminal o de solo-append/alta-baja dura — no hace falta un segundo mecanismo de soft-delete encima.
- **Auditoría:** toda operación mutante relevante llama a `AuditService.registrar(actorId, AuditAccion.X, "Entidad", entidadId, metadataOpcional)`. `actorId=null` = evento disparado por el sistema (ej. el scheduler de finalización de clases). Ver el enum `AuditAccion` para la lista completa de acciones ya cubiertas — al agregar un usecase nuevo que muta algo, sumar el valor correspondiente ahí.
- **Notificaciones:** cuando un usecase debe avisarle algo a un usuario sobre una clase/actividad, usa el sistema genérico en `shared/notificacion/`: `NotificacionService.notificar(usuarioId, TipoNotificacion.X, mensaje, entidadId)`. Regla de negocio explícita (no solo de estilo): **toda notificación sobre una clase debe incluir el nombre de la actividad y la fecha/hora de la clase** — para eso existe `NotificacionMensajes.formatFechaHora(instant)` (formatea en horario de Argentina), no reinventar el formato en cada usecase. El enum `TipoNotificacion` es deliberadamente abierto a crecer (la tabla no tiene `CHECK` en `tipo`) porque el mensaje humano ya viaja armado en el campo `mensaje` — el tipo es solo para que el frontend elija ícono/estilo, no para generar texto.
  - Quién compone el `mensaje`: si el usecase ya tiene toda la data cargada y no depende de formato de fecha ad-hoc del cliente, se compone en el Service (ej. `cancelarClase`, `crearClase`). Si el flujo ya dependía de que el frontend arme el texto (ej. motivo de una denuncia), seguí ese mismo criterio (ej. `notificarAusenciaProfesor` recibe el mensaje ya armado del frontend, que reusa sus propios helpers de formato de fecha).
- **Errores:** formato único `ApiError` (timestamp, status, code, message, fieldErrors, path) desde `GlobalExceptionHandler`. Códigos en `ApiErrorCode`: VALIDACION(400), CREDENCIALES_INVALIDAS(401), SIN_PERMISO(403), NO_ENCONTRADO(404), EMAIL_EN_USO(409), USUARIO_SUSPENDIDO(403), TIPO_ACTIVIDAD_EN_USO(409), CATEGORIA_EN_USO(409), SIN_CUPOS_DISPONIBLES(409), INSCRIPCION_YA_EXISTE(409), ERROR_INTERNO(500).
- **Seguridad:** JWT Bearer, contraseñas con BCrypt. Rutas públicas (`SecurityConfig.permitAll`): registro alumno/instructor, login, y GET de catálogo (`/api/categorias`, `/api/tipos-actividad`, `/api/actividades`, `/api/actividades/**`). Todo lo demás requiere autenticación (`anyRequest().authenticated()`); los endpoints de admin además llevan `@PreAuthorize("hasRole('ADMIN')")` a nivel método (no hace falta tocar `SecurityConfig` para eso).
- **Validación:** forma (campos, formato) con Bean Validation en el Request DTO; negocio (unicidad, cupos, ventanas de tiempo, "en uso") en el Service.
- **Duplicados por nombre** (Categoria, TipoActividad): chequeo case-insensitive contra filas no borradas (`existsByNombreIgnoreCaseAndDeletedFalse` o variante scoped) antes de guardar, tanto en crear como en actualizar (en actualizar, excluyendo el propio id).

## Estado actual del roadmap

Roadmap numerado por el usuario; los ítems 1 (auth/catálogo/actividades/inscripciones/reseñas/validación instructor/finalización automática de clases) ya estaban resueltos antes de este tramo de trabajo.

**Hechos (2 a 7):**

- **2. Denuncias** — `creardenuncia` (alumno, reporta inasistencia de un instructor sobre una clase que ya pasó, ≥1h), `listardenunciasadmin`, `resolverdenuncia` (admin, 4 acciones mutuamente excluyentes: `REINTEGRAR` cancela inscripción + reintegra pago retenido, `SUSPENDER` pone al **instructor** en SUSPENDIDO, `PENALIZAR` crea `Penalizacion` + incrementa contador, `DESESTIMAR` sin efecto), `listarmisdenuncias`. Dominio: `domain/denuncia/` (`Denuncia`, `EstadoDenuncia`: Pendiente·En Auditoría·Resuelta), `domain/penalizacion/` (`Penalizacion`, `TipoPenalizacion`). Migración V8.
- **3. Gestión de usuarios (admin)** — `listarusuariosadmin`, `actualizarestadousuario` (activar/suspender, con guardia anti-auto-suspensión). Sin ABM de rol/permiso (roles son un enum fijo, no una tabla) y sin endpoint para que el usuario edite sus propios datos — eso quedó fuera de alcance.
- **4. Auditoría consultable** — `listarauditoria` (GET, resuelve nombre/rol del actor o "Sistema"/"Usuario eliminado").
- **5. Dashboard y Reportes reales** — `listarinscripcionesadmin` (incluye `actividadId` directo en la respuesta — importante, ver nota de bug abajo), `listarclasesadmin`. El admin Dashboard/Reportes del frontend consume estos + `listarusuariosadmin`/`listarinstructores`/`listarDenunciasAdmin` reales. Instructor y alumno dashboards **siguen en mock** (fuera de alcance de este ítem).
- **6. Notificar ausencia de profesor** — sistema de notificaciones genérico (`shared/notificacion/`) + `notificarausenciaprofesor` (cascada igual a `cancelarClase` + notifica a cada alumno afectado), `listarmisnotificaciones`, `marcartodasnotificacionesleidas`. De yapa: `cancelarClase` y `crearClase` (nuevo horario en actividad favorita) también notifican ahora, reusando el mismo servicio.
- **7. ABM de Categoría** — `crearcategoria`, `actualizarcategoria`, `eliminarcategoria` (bloquea si hay `TipoActividad` activo referenciándola). Antes de esto, Categoria solo tenía lectura.

**Pendientes (8 a 12), sin empezar:**

- **8. Motor de recomendaciones en Búsqueda** — hoy el filtrado de `/alumno/explorar` es tradicional (texto, categoría, tipo, nivel, precio, cupos, instructor, orden), sin nada personalizado/inteligente.
- **9. Geolocalización real** — necesita API key de Google Maps. Hoy `ubicacion` en `Actividad` es un `String` libre; no hay coordenadas, distancia calculada ni mapa real.
- **10. Imágenes reales** — necesita cuenta de Supabase Storage. Hoy `photoTint` es un color/gradiente de placeholder; no hay endpoint de upload en ningún lado.
- **11. Asistente de IA y chatbot** — necesita credenciales de Groq. Hoy es una simulación de frontend con `setTimeout`.
- **12. Mercado Pago real** — al final, según lo acordado con el usuario. `PaymentGateway` ya tiene el contrato listo para enchufar el SDK real sin tocar los usecases que lo usan.

Diferido aparte, no numerado: **Redis (caché)** y **deploy** (Render/Vercel) — nada de esto existe todavía.

## Bugs reales encontrados y corregidos en el camino (no repetirlos)
- `EliminarActividadService` no cancelaba en cascada las clases/inscripciones/pagos asociados al eliminar una actividad, y el endpoint solo dejaba borrar al instructor dueño (un admin no podía). Corregido: cascada completa + `hasAnyRole('INSTRUCTOR','ADMIN')`.
- `ListarInstructoresController` bindeaba el query param de filtro como `estadoVerificacion`, pero el frontend mandaba `?estado=`. El mismatch hacía que el filtro se ignorara en silencio y devolviera *todos* los instructores. Corregido con `@RequestParam(value = "estado", ...)`. **Moraleja: si un filtro por query param "no hace nada", sospechar primero de un nombre de parámetro que no matchea, antes de asumir que la lógica de filtrado está mal.**
- El donut "Reservas por categoría" del Dashboard admin dependía de `DataContext.clases`, que en el frontend es una caché parcial (solo se llena cuando se visita el detalle de una actividad), no una lista completa — daba 0% siempre. Se resolvió agregando `actividadId` directo a la respuesta de `listarinscripcionesadmin` para no depender de esa caché.

## Reglas de negocio clave (ya implementadas, no reinterpretar)
- Pre-inscripción vs inscripción según faltan **>4 días** (`VentanaInscripcion.UMBRAL_PREINSCRIPCION = Duration.ofDays(4)`, solo PreInscripción, no ocupa cupo) o **≤4 días y ≥1h** (`UMBRAL_CIERRE = Duration.ofHours(1)`, inscripción definitiva si hay cupos). Comparación por `Duration.between(ahora, fechaHora)` sobre `Instant` UTC — nada de redondeo a día calendario ni zona horaria. `ahora` sale de un `Clock` inyectado (`ClockConfig`, `Clock.systemUTC()`), no de `Instant.now()` directo, para poder testear con reloj fijo.
- Pago Mercado Pago → Inscripto (Retenido→Liberado, vía `MockPaymentGateway`). Efectivo → PagoPendiente hasta que el instructor confirma el cobro (`confirmarcobroefectivo`) → Inscripto.
- Cupos: `ClaseRepository.ocuparCupo`/`liberarCupo` son `UPDATE` atómicos condicionales (`WHERE cuposOcupados < cuposMax`) — evitan la doble ocupación del último cupo sin lock explícito.
- `FinalizarClasesVencidasScheduler` (`@Scheduled(fixedRate=5min)`) pasa automáticamente las clases vencidas a Finalizada — no hace falta ninguna acción manual del instructor para eso.
- Estados con nombre EXACTO (etiqueta con tilde/espacio vía enum + `AttributeConverter`, constante Java en ASCII) — Inscripción: PreInscripción · PagoPendiente · Inscripto · Cancelada. Clase: Programada · Habilitada · Cancelada · Finalizada. Pago: Retenido · Liberado · Cancelado · Efectivo. Denuncia: Pendiente · En Auditoría · Resuelta.
- No confundir Categoría / TipoActividad / NivelIntensidad: tres cosas distintas, con ABM propio cada una salvo NivelIntensidad (enum fijo, sin ABM — no está pedido).

## Tests
Cada slice "andando" = test de Service (regla + caso de error, ej. duplicado/en-uso/no-encontrado/sin-permiso) y test de Controller (status + shape del DTO) en verde. `./mvnw test` no requiere Postgres levantado (mocks + `@WebMvcTest`). Antes de dar por terminado un cambio, correr la suite completa, no solo el paquete tocado.

## Idioma
Código y nombres de paquete/clase en español donde el dominio lo pida (entidades, casos de uso). Mensajes de usuario en español rioplatense.

## Frontend que consume esta API
`activehub-frontend` (repo hermano, Vite + React + TypeScript) ya tiene **auth real** (JWT vía `AuthContext.tsx`) y la mayoría de `DataContext.tsx` cableada a esta API. Quedan mock a propósito y sin tocar (fuera de alcance, documentado como tal en el propio `DataContext.tsx`): `inscripciones`/`pagos`/`penalizaciones` de alcance amplio (analítica histórica sin endpoint "todas las inscripciones/pagos de la plataforma" más allá de `listarinscripcionesadmin`), y los dashboards de instructor/alumno. Ver `activehub-frontend/CLAUDE.md` para el detalle de qué pantalla usa qué.
