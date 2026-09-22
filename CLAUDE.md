# CLAUDE.md — ActiveHub API (Spring Boot)

Convenciones de este repositorio. Leer **antes** de escribir código. El contexto funcional completo y los contratos originales están en `design_handoff_activehub/` (documento de la facultad — ver la sección "Desviaciones respecto al documento original" más abajo para lo que cambió en la práctica).

## Qué es
API REST de ActiveHub: plataforma de gestión de actividades deportivas/recreativas/formativas. Tres roles: **Alumno**, **Instructor**, **Administrador**. Repo hermano `activehub-frontend` (Vite + React + TypeScript) es el único consumidor.

## Stack
- Java 21 + **Spring Boot 4.1** (REST), Spring Security 7.1 (JWT stateless), Spring Data JPA, **Flyway** para migraciones.
- DB: **PostgreSQL 16 en Supabase**. El `.env` del repo apunta al pooler de Supabase y ésa es la base que usan todos los integrantes y los `@SpringBootTest` — ver "Los tests corren contra la base REAL" y "Migraciones" más abajo. El README documenta además un `docker run` de Postgres local (`activehub-postgres`, 5433 → 5432) como alternativa para trabajo aislado; si lo usás, cambiá `DATABASE_URL` en tu `.env`.
- Correo: `spring-boot-starter-mail` contra Gmail SMTP (verificación de dirección por código). Sin credenciales, `EmailSender` cae a modo log.
- Identidad federada: Google Identity Services, con el ID token validado localmente (ver "Continuar con Google").
- Pagos: interfaz `PaymentGateway` con **MockPaymentGateway** activo (sin integración real con Mercado Pago todavía — ítem 12 del roadmap).
- **Redis quedó descartado, no diferido.** Su única función prevista era el bloqueo temporal de cupo; el control de concurrencia se resolvió con un `UPDATE` condicional atómico en Postgres (ver `ClaseRepository.ocuparCupo`), que es más simple y no agrega un segundo almacén que pueda desincronizarse. No volver a proponerlo sin un caso de uso nuevo que lo justifique.
- Sin deploy: todo corre en local (`./mvnw spring-boot:run`) contra la base de Supabase. Render/Vercel siguen pendientes.
- Archivos subidos: interfaz `AlmacenamientoArchivos` (`shared/storage/`) con dos implementaciones — **disco** (por defecto) y **Supabase Storage** (se activa solo si hay credenciales). Ver "Almacenamiento de archivos" más abajo.
- Tamaño actual: **107 casos de uso**, **26 migraciones** (V1 a V26), **18 claves de permiso**.

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
- **Notificaciones:** cuando un usecase debe avisarle algo a un usuario sobre una clase/actividad, usa el sistema genérico en `shared/notificacion/`: `NotificacionService.notificar(usuarioId, TipoNotificacion.X, mensaje, entidadId, Destino.y(id))`. **El quinto parámetro no es opcional ni decorativo: es a dónde lleva el click** — ver "Notificaciones clickeables" más abajo antes de agregar una. Regla de negocio explícita (no solo de estilo): **toda notificación sobre una clase debe incluir el nombre de la actividad y la fecha/hora de la clase** — para eso existe `NotificacionMensajes.formatFechaHora(instant)` (formatea en horario de Argentina), no reinventar el formato en cada usecase. El enum `TipoNotificacion` es deliberadamente abierto a crecer (la tabla no tiene `CHECK` en `tipo`) porque el mensaje humano ya viaja armado en el campo `mensaje` — el tipo es solo para que el frontend elija ícono/estilo, no para generar texto.
  - Quién compone el `mensaje`: si el usecase ya tiene toda la data cargada y no depende de formato de fecha ad-hoc del cliente, se compone en el Service (ej. `cancelarClase`, `crearClase`). Si el flujo ya dependía de que el frontend arme el texto (ej. motivo de una denuncia), seguí ese mismo criterio (ej. `notificarAusenciaProfesor` recibe el mensaje ya armado del frontend, que reusa sus propios helpers de formato de fecha).
- **Errores:** formato único `ApiError` (timestamp, status, code, message, fieldErrors, path) desde `GlobalExceptionHandler`. Códigos en `ApiErrorCode`: VALIDACION(400), CREDENCIALES_INVALIDAS(401), SIN_PERMISO(403), NO_ENCONTRADO(404), EMAIL_EN_USO(409), USUARIO_SUSPENDIDO(403), TIPO_ACTIVIDAD_EN_USO(409), CATEGORIA_EN_USO(409), SIN_CUPOS_DISPONIBLES(409), CLASE_CON_INSCRIPTOS(409), ACTIVIDAD_CON_INSCRIPTOS(409), INSCRIPCION_YA_EXISTE(409), ERROR_INTERNO(500).
  - Un pedido **incompleto** del cliente es 400, no 500: `MissingServletRequestPartException` y `MissingServletRequestParameterException` tienen su handler (`handleParteFaltante`, devuelve VALIDACION con el campo faltante en `fieldErrors`). Sin él caían en el catch-all `Exception` y un multipart sin la parte `documentos` se reportaba como falla del servidor. Al agregar una excepción de framework que sea culpa del cliente, sumarle handler propio en vez de dejarla llegar al catch-all.
- **Seguridad:** JWT Bearer, contraseñas con BCrypt. Rutas públicas (`SecurityConfig.permitAll`): registro alumno/instructor, login, y GET de catálogo (`/api/categorias`, `/api/tipos-actividad`, `/api/niveles-intensidad`, `/api/actividades`, `/api/actividades/**`, `/api/fotos/**`). **Si agregás un endpoint que el frontend pide al arrancar, sumalo acá** — ver la trampa del catálogo público más abajo. Todo lo demás requiere autenticación (`anyRequest().authenticated()`); los endpoints con módulo detrás llevan `@PreAuthorize("@permisos.puede('clave')")` a nivel método (no hace falta tocar `SecurityConfig` para eso) — ver "Roles y permisos". **Nunca `hasRole`**: hay un test que lo prohíbe.
- **Validación:** forma (campos, formato) con Bean Validation en el Request DTO; negocio (unicidad, cupos, ventanas de tiempo, "en uso") en el Service.
- **Duplicados por nombre** (Categoria, TipoActividad): chequeo case-insensitive contra filas no borradas (`existsByNombreIgnoreCaseAndDeletedFalse` o variante scoped) antes de guardar, tanto en crear como en actualizar (en actualizar, excluyendo el propio id).

## Estado actual del roadmap

Roadmap numerado por el usuario; los ítems 1 (auth/catálogo/actividades/inscripciones/reseñas/validación instructor/finalización automática de clases) ya estaban resueltos antes de este tramo de trabajo.

**Hechos (2 a 7):**

- **2. Denuncias** — `creardenuncia` (alumno, reporta inasistencia de un instructor sobre una clase que ya pasó, ≥1h), `listardenunciasadmin`, `resolverdenuncia` (admin, 4 acciones mutuamente excluyentes: `REINTEGRAR` cancela la inscripción y reintegra el pago de **todos los inscriptos a la clase**; `SUSPENDER` inhabilita al instructor por N días, cancela sus clases del período y reintegra a todos esos inscriptos; `PENALIZAR` sólo crea una `Penalizacion` económica + incrementa contador; `DESESTIMAR` sin efecto), `listarmisdenuncias`. Dominio: `domain/denuncia/` (`Denuncia`, `EstadoDenuncia`: Pendiente·En Auditoría·Resuelta), `domain/penalizacion/` (`Penalizacion`, `TipoPenalizacion`). Migración V8.
- **3. Gestión de usuarios (admin)** — `listarusuariosadmin`, `actualizarestadousuario` (activar/suspender, con guardia anti-auto-suspensión). El ABM de rol/permiso, que en su momento quedó fuera, **hoy existe** (ver "Roles y permisos"). Sin endpoint para que el usuario edite sus propios datos — eso quedó fuera de alcance.
- **4. Auditoría consultable** — `listarauditoria` (GET, resuelve nombre/rol del actor o "Sistema"/"Usuario eliminado").
- **5. Dashboard y Reportes reales** — `listarinscripcionesadmin` (incluye `actividadId` directo en la respuesta — importante, ver nota de bug abajo), `listarclasesadmin`. El admin Dashboard/Reportes del frontend consume estos + `listarusuariosadmin`/`listarinstructores`/`listarDenunciasAdmin` reales. **Los dashboards de instructor y alumno también son reales hoy** (se cablearon después, con `listarmisclases` / `listarinscripcionesmisclases` para el instructor y `listarmisinscripciones` / `listarmispagos` para el alumno): ya no queda ningún panel leyendo datos simulados.
- **6. Notificar ausencia de profesor** — sistema de notificaciones genérico (`shared/notificacion/`) + `notificarausenciaprofesor` (cascada igual a `cancelarClase` + notifica a cada alumno afectado), `listarmisnotificaciones`, `marcartodasnotificacionesleidas`. De yapa: `cancelarClase` y `crearClase` (nuevo horario en actividad favorita) también notifican ahora, reusando el mismo servicio.
- **7. ABM de Categoría** — `crearcategoria`, `actualizarcategoria`, `eliminarcategoria` (baja en cascada sobre sus Tipos; bloquea sólo si alguno tiene Actividades — ver la sección propia más abajo). Antes de esto, Categoria solo tenía lectura.

**Ítem 9 — Geolocalización: HECHO, pero no como estaba planteado.**

Se descartó Google Maps Platform (exige cuenta de facturación con tarjeta incluso dentro de sus topes gratuitos) y se reemplazó por **OpenStreetMap + Leaflet + Nominatim**, que no piden clave de API. Qué existe hoy:

- `Actividad.latitud` / `Actividad.longitud` (`DOUBLE PRECISION`, V12), y ambas viajan en los DTO de `crearactividad`, `actualizaractividad`, `listaractividades` y `obteneractividad`.
- El instructor fija la ubicación buscando la dirección (Nominatim) y confirmando el punto sobre el mapa; el alumno la ve en el detalle de la actividad.
- **La distancia y el filtro por radio están implementados del lado del cliente**, con la fórmula de Haversine contra la geolocalización del navegador (`lib/geo.ts` del frontend). Alimentan "Cerca de tu ubicación" del Home, el orden "Cercanas" y el filtro de radio de Explorar. **No hay consulta espacial en el backend ni extensión PostGIS**, y no hace falta: es coherente con la convención de "el backend devuelve todo sin paginar y el frontend agrega/filtra/ordena".
- Lo único que queda del ítem: si alguna vez el catálogo crece lo suficiente como para que filtrar en el cliente deje de ser viable, habrá que mover el filtro por radio a una consulta del servidor. Hoy no es un problema.

**Ítem 10 — Imágenes: HECHO, y el destino ya puede ser Supabase Storage.**

`photoTint` dejó de ser lo único que había. Existen los endpoints de subida y lectura (`subirfotoactividad`, `agregarimagenactividad`, `eliminarimagenactividad`, `verfotoactividad`, `verimagenactividad`, `subirfotoperfil`, `verfotoperfil`, `subirdocumento`, `descargardocumentoinstructor`), con validación de tipo MIME, extensión y tamaño (5MB), renombrado con id propio y reversión de la escritura en disco si la transacción hace rollback. Los nueve usecases ya no tocan el filesystem: escriben y leen por `AlmacenamientoArchivos`, y el destino lo decide la configuración (disco o Supabase Storage). Ver la sección propia más abajo. **Lo que queda del ítem** es operativo, no de código: crear el bucket en el proyecto de Supabase, cargar las variables de entorno en el despliegue y migrar los archivos que hoy están en `./uploads`. Sigue faltando también la optimización de imágenes (redimensionado/compresión al subir), que nunca se implementó.

**Ítem 11 — IA y chatbot: la UI existe, el modelo no.**

El widget flotante de chat funciona y responde por coincidencia de palabras contra `lib/faqs.ts` (la misma fuente que la pantalla pública de Ayuda), y la sección "Asistente de beneficios y prevenciones" del detalle de actividad existe con su botón, su estado de carga y su resultado, generado con plantillas por nivel de intensidad. **Nada de esto consume un modelo de lenguaje**, y el copy no promete IA en ningún lado. Falta: credenciales de Groq, el endpoint del backend que arme el prompt controlado y la persistencia del informe generado por alumno + actividad + versión del perfil de salud.

**Pendientes de verdad:**

- **8. Motor de recomendaciones en Búsqueda** — hoy el filtrado de `/alumno/explorar` es tradicional (texto sin tildes, categoría, tipo en cascada, nivel, precio, cupos, fecha, franja horaria, radio de cercanía, instructor, orden). El "Recomendado para vos" del Home cruza los intereses declarados del alumno contra el `tipoActividadId` de cada actividad (V19), pero no aprende de búsquedas ni de inscripciones previas.
- **12. Mercado Pago real** — al final, según lo acordado con el usuario. `PaymentGateway` ya tiene el contrato listo para enchufar el SDK real sin tocar los usecases que lo usan; toda la máquina de estados (`Retenido` → `Liberado`/`Cancelado`), la acreditación automática y los reintegros ya funcionan contra el mock.
- **Deploy** (Render/Vercel) — la aplicación ya está lista (puerto, DB, CORS, secreto JWT, credenciales externas y ahora el bucket de Supabase Storage son variables de entorno), pero el despliegue no se hizo.

**Recuperación de contraseña por olvido: HECHA** (ver la sección propia más abajo). Era el único camino de credenciales que faltaba; el usuario autenticado ya podía cambiar su contraseña y su correo.

## Las 7 decisiones del usuario (resueltas — no reabrirlas sin que él lo pida)

1. **Período de denuncias: 24 h**, no 48. Vive en `VentanaPagos.PERIODO_DENUNCIAS` y es lo único que hay que tocar si vuelve a cambiar.
2. **Asistencia: fuera.** La spec la retiró explícitamente ("el registro de asistencia manual fue retirado de esta vista, no debe implementarse", E2I-HU07; y no cuenta como métrica, E2I-HU10). Se borró el slice `marcarasistencia`, el campo `Inscripcion.presente`, la columna (`V16__quitar_asistencia.sql`), el `AuditAccion.ASISTENCIA_MARCADA` y la columna del roster. **No volver a agregarla.**
3. **Roles y permisos: implementado de verdad** (RN-19). Ver la sección propia más abajo.
4. **NivelIntensidad: la decisión se revirtió.** En su momento se cerró como "sigue siendo un enum, sin ABM ni tabla"; hoy **es una entidad con ABM propio** (V21). No fue un cambio de opinión sino una imposibilidad: E4Ad-HU05 pide crear, editar y eliminar niveles desde la pantalla, con control de duplicados y bloqueo si tiene actividades asociadas, y ninguno de esos 8 criterios se puede cumplir sobre un enum sin recompilar. Ver la sección propia más abajo.
5. **Borrar actividad con inscriptos: se bloquea**, no se cascadea (E2I-HU06 criterio 7). `EliminarActividadService` lanza `ActividadConInscriptosException` (409 `ACTIVIDAD_CON_INSCRIPTOS`) si alguna clase vigente tiene inscripciones no canceladas, con el mensaje textual de la spec. Si las clases vigentes están vacías, las cancela y da de baja la actividad. El camino para una clase con gente anotada sigue siendo `cancelarclase`.
6. **Al cancelar una clase, el pago en Efectivo también se reintegra** (E2I-HU08 criterio 5): pasa a `Cancelado` igual que el `Retenido`. No hay gateway al que pedirle nada — la plata la cobró el instructor en mano — así que la notificación al alumno le dice que coordine la devolución con él. Antes el efectivo no se tocaba y el alumno seguía viendo un pago válido de una clase que ya no existe. **Cancelar la inscripción por decisión del alumno sigue sin tocar el efectivo**: ese caso es la ambigüedad 1 de la sección 12 de la spec y no está definido.
7. **El comentario de la reseña es opcional** (E3A-HU10 criterio 3); solo el puntaje es obligatorio. `V17__resenia_comentario_opcional.sql` saca el `NOT NULL`, y crear/editar normalizan el blanco a `null` (no a cadena vacía).

## Roles y permisos (RN-19 / E4Ad-HU08)

`permiso` (catálogo de **18 claves**) + `configuracion_rol` (rol × permiso, con `habilitado`), migraciones `V18__roles_permisos.sql`, `V20__permisos_faltantes.sql`, `V22__permisos_coherentes.sql` y `V24__soporte.sql`. La configuración inicial replica lo que cada rol ya podía hacer, así que activar el motor no le sacó nada a nadie.

Las 18 claves: `actividades.publicar`, `actividades.moderar`, `auditoria.ver`, `catalogo.explorar`, `clases.gestionar`, `cobros.confirmar`, `denuncias.crear`, `denuncias.resolver`, `inscripciones.gestionar`, `instructores.validar`, `penalizaciones.gestionar`, `reportes.ver`, `resenias.escribir`, `resenias.responder`, `roles.configurar`, `soporte.gestionar`, `taxonomia.gestionar`, `usuarios.gestionar`.

- **Cómo se aplica un permiso:** `@PreAuthorize("@permisos.puede('clave')")`, sin nombre de rol. El bean es `shared/security/PermisosService`, registrado con el nombre **`permisos`** — la SpEL lo busca por ese nombre, así que renombrar la clase sin mantenerlo rompe las guardas en runtime, no en compilación. Está cableado en **73 de los 100 controllers**; los 27 restantes son catálogo público, endpoints de auth o endpoints de identidad propia (ver más abajo por qué esos no llevan permiso).
- **Sin caché a propósito:** el criterio 2 pide que el cambio impacte "de inmediato". Una caché dejaría a los usuarios ya logueados con la configuración vieja. Es un índice `(rol_id, permiso_id)`, el costo por request es despreciable.
- **Sin fila = sin permiso.** Si agregás una clave nueva al código, agregala también a la migración; si no, el permiso queda denegado para todos (que es el default seguro).
- **Toda clave del catálogo gatea algo, y toda guarda usa una clave del catálogo.** Lo verifica `GuardasDePermisoTest`, que compara los `@PreAuthorize` del código contra los `INSERT INTO permiso` de las migraciones en las dos direcciones. `catalogo.explorar` era la excepción — estaba en la tabla sin gatear nada — y ahora protege los favoritos (el catálogo en sí sigue siendo `permitAll`, porque es público).
- **Permisos implícitos (`Permiso.IMPLICITOS`, V22).** `catalogo.explorar` dejó de ser una decisión del administrador: lo tiene todo rol, presente o futuro, `ActualizarPermisosRolService` lo fuerza encendido venga o no en el payload, y `ListarRolesPermisosResponse.Permiso.configurable` viaja en `false` para que la pantalla no lo ofrezca como checkbox. La clave, la fila y las guardas se conservan (los favoritos la siguen usando) — lo único que cambia es que nadie puede apagarla. Si mañana otra clave califica como implícita, se agrega a ese `Set` y no hay que tocar nada más.
- **Al Administrador no se le pueden quitar los permisos críticos** (`usuarios.gestionar`, `roles.configurar`): `ActualizarPermisosRolService` lo rechaza con el mensaje de la spec (criterio 6). El resto de los roles sí pueden quedarse sin nada.
- **El ADMIN nace sólo con permisos de administración** (V22). En V18 recibía todos menos `inscripciones.gestionar`, o sea también `actividades.publicar`, `clases.gestionar`, `cobros.confirmar`, `resenias.escribir`, `denuncias.crear` y `resenias.responder`. Efecto visible: al administrador le aparecían los botones "Ir a Instructor" e "Ir a Alumno", porque el frontend decide el área por permisos y el admin cumplía los requisitos de las tres.
- **`Rol.nombre` ya no es un enum** sino texto libre, porque el admin puede crear roles nuevos (criterio 3). Los tres del sistema se reconocen con `Rol.getNombreSistema()`, que devuelve el `RolNombre` o `null`. Un rol creado por el admin **no se le puede asignar a una cuenta todavía**: el motor de seguridad y el JWT solo entienden ALUMNO/INSTRUCTOR/ADMIN. Nace sin permisos y con sus filas de `ConfiguracionRol` en false.
- Slices: `listarrolespermisos` (GET `/api/admin/roles`, devuelve la matriz completa), `actualizarpermisosrol` (PUT `/api/admin/roles/{id}/permisos`, **manda la foto completa**: lo que no viene queda apagado) y `crearrol` (POST). Los tres auditan.

### Las tres guardas que impiden dejar la plataforma sin administración

"¿Qué pasa si le quito los permisos de administrador al administrador?" es la pregunta correcta: si el sistema lo permitiera, no habría ninguna pantalla desde la cual volver atrás y la única salida sería tocar la base a mano. Hay **tres** caminos posibles y los tres están cerrados, cada uno en su usecase:

1. **Sacarle los permisos al rol ADMIN** → `ActualizarPermisosRolService` rechaza si el rol se queda sin alguno de los `critico` (`usuarios.gestionar`, `roles.configurar`). La pantalla además los muestra bloqueados, para no enterarse recién al guardar.
2. **Cambiarle el rol a la última cuenta ADMIN** → `AsignarRolUsuarioService`: no podés cambiarte el rol a vos mismo, y no se puede mover al último ADMIN a otro rol.
3. **Que el último ADMIN se dé de baja** → `DarmeDeBajaService` (era el único que faltaba). Se cuentan las cuentas **vivas** del rol, no las activas: un admin suspendido puede volver (el scheduler levanta la suspensión), uno borrado no.

La suspensión no necesita guarda propia: `actualizarestadousuario` ya impide auto-suspenderse, así que quien suspenda a otro admin sigue él mismo activo.

### Roles asignables y el fin de `hasRole` como guarda

Segunda tanda del ítem: un rol creado por el admin **ya se puede asignar a una cuenta**.

- **`@PreAuthorize` ya no mira el rol en ningún lado.** Quedaban 14 endpoints con `hasRole`/`hasAnyRole` y 4 que resolvían un `ROLE_ADMIN` a mano dentro del método; ese resto era exactamente el bug reportado como *"los permisos no están andando"* — darle `actividades.publicar` a un rol nuevo no le habilitaba nada porque la guarda real seguía preguntando por el nombre del rol. Cómo quedó cada grupo:
  - **Favoritos** (agregar / quitar / listar) → `catalogo.explorar`.
  - **Reseñas que recibe el instructor** (listar / responder / denunciar) → `resenias.responder`, clave nueva de V20.
  - **Moderar la actividad de otro instructor** (eliminar, galería, foto) → `actividades.moderar`, clave nueva de V20. Se resuelve con `permisosService.puede(actorId, "actividades.moderar")` porque necesita el id del actor; el parámetro del service se llama `puedeModerar`, no `esAdmin`, que era mentira desde el momento en que dejó de mirar el rol.
  - **"Mis datos" del instructor** (`obtenermiperfilinstructor`, `subirdocumento`, `reabrirsolicitud`) → **sin permiso**: son endpoints de identidad, no módulos. RN-16 dice que un instructor no verificado sólo accede a "Mis datos"; el service resuelve el `PerfilInstructor` de quien llama y devuelve 404 a quien no tenga uno.
  - **`listarmisdenuncias`** → `denuncias.crear or resenias.responder`: devuelve sólo las denuncias propias, y las hacen los dos roles por caminos distintos.
- **`actividades.moderar` estaba muerto y no lo vio ningún test.** Los cuatro endpoints que moderan la actividad de otro instructor (`eliminaractividad`, `subirfotoactividad`, `agregarimagenactividad`, `eliminarimagenactividad`) tenían `@PreAuthorize("@permisos.puede('actividades.publicar')")` y recién adentro preguntaban por `actividades.moderar`. Funcionó mientras el ADMIN tenía las dos claves; cuando V22 le sacó `actividades.publicar` —correctamente, el admin no publica— la guarda empezó a frenarlo antes de llegar a la línea que decide el alcance, y el administrador dejó de poder borrar la actividad de nadie. Hoy la guarda es `publicar or moderar` y el Service sigue decidiendo el alcance con `puedeModerar`. **Moraleja: si un endpoint pregunta por dos permisos, el `@PreAuthorize` tiene que dejar pasar a los dos.** `GuardasDePermisoTest` no lo detecta: comprueba que cada clave exista y se use, no que sea alcanzable.
- **La regresión está trabada por test.** `shared/security/GuardasDePermisoTest` falla si alguien vuelve a escribir `hasRole(` en `src/main`. Hacía falta un test de arquitectura porque esto no rompe ningún test funcional: el rol "correcto" sigue entrando.
- **El JWT lleva el rol como texto** (`JwtService.emitir(..., String rol)`) y el filtro arma `ROLE_<nombre>`. Un rol nuevo entra al sistema sin tocar el enum.
- **`asignarrolusuario`** (`PUT /api/admin/usuarios/{id}/rol`, permiso `roles.configurar`): dos guardas — no podés cambiarte el rol a vos mismo, y no se puede dejar la plataforma sin su último ADMIN. El perfil (alumno/instructor) no se toca: un instructor que cambia de rol conserva su `PerfilInstructor`, y los usecases que lo exigen lo siguen exigiendo.
- **`GET /api/auth/me` devuelve `permisos`** (las claves habilitadas del rol) además de `intereses`. El frontend arma el menú con eso y no muestra módulos que el backend va a rechazar.

### Intereses del alumno

`actualizarmisintereses` (`PUT /api/usuarios/me/intereses`): reemplaza la lista completa, deduplica y recorta (la tabla tiene `UNIQUE (perfil_alumno_id, interes)` y sin deduplicar reventaba con error de base). Antes no había endpoint y el frontend los guardaba en su store local: se perdían al cambiar de navegador y el "Recomendado para vos" del Home no tenía de dónde leerlos.

## Intereses del alumno: son TipoActividad, no texto (V19)

`perfil_alumno_interes` dejó de guardar strings y pasó a apuntar a `tipo_actividad`. `PerfilAlumno.intereses` es un `@ManyToMany<TipoActividad>` sobre esa misma tabla (que conserva su `id` propio con default en la base, así que Hibernate solo escribe las dos FK).

- **Por qué:** un interés suelto ("Trekking") no sabía nada de la taxonomía, y el "Recomendado para vos" del Home tenía que adivinar comparando texto contra el nombre de la actividad. Ahora cada interés arrastra su categoría y el cruce es por id.
- **La migración no perdió datos:** primero crea los tipos que solo existían como texto (Gimnasia, Ciclismo, Senderismo, Meditación), después mapea por nombre, y recién ahí borra la columna vieja. Los que no encontraran tipo se descartan (después del primer paso no queda ninguno).
- **Contrato:** `PUT /api/usuarios/me/intereses` recibe `tiposActividadId: [uuid]` (antes `intereses: [string]`), `GET /api/auth/me` devuelve `intereses` como objetos `{tipoActividadId, nombre, categoriaId, categoria}`, y `POST /api/auth/registro/alumno` manda ids en `intereses`. El alta ignora en silencio un id que ya no exista; el endpoint de edición sí lo rechaza (400), porque ahí el alumno lo acaba de elegir de una lista.

## Nivel de intensidad: es una entidad con ABM, no un enum (V21)

E4Ad-HU05 pide crear, editar y eliminar niveles desde la pantalla, con control de duplicados y bloqueo si tiene actividades asociadas. Era un `enum` de tres valores fijos: los 8 criterios de la HU no se podían cumplir sin recompilar.

- **Modelo:** tabla `nivel_intensidad` (`nombre` único entre los vivos, `descripcion`), y `actividad.nivel_intensidad_id` como FK. La columna de texto vieja guardaba la etiqueta ("Física baja"), así que el backfill de V21 se hace por nombre sin tabla de traducción.
- **Son tres cosas distintas** — Categoría agrupa Tipos, el Tipo dice QUÉ es la actividad, el Nivel dice CUÁNTO esfuerzo pide (sección 2 del backlog). Ahora las tres tienen el mismo tratamiento: entidad + ABM + baja lógica + "en uso" que bloquea.
- **Cambio de contrato:** `POST/PUT /api/instructor/actividades` reciben `nivelIntensidadId` (UUID), no `nivelIntensidad` (etiqueta), y ya no hay `@Pattern` con la lista de tres valores — la existencia se valida contra la base. Las respuestas de actividad devuelven `nivelIntensidad: {id, nombre}` en vez de un string. El filtro del catálogo es `?nivelIntensidadId=`.
- **Slices:** `listarnivelesintensidad` (GET `/api/niveles-intensidad`, público como categorías y tipos, incluye el conteo de actividades que muestra la tabla del admin), `crearnivelintensidad`, `actualizarnivelintensidad` y `eliminarnivelintensidad` (los tres bajo `/api/admin/niveles-intensidad`, permiso `taxonomia.gestionar`, los tres auditan).
- **Ojo con el seed:** `seed-datos-demo.sql` resuelve el nivel por JOIN contra `nivel_intensidad`. Si se agrega una actividad al seed con un nombre de nivel que no existe, la fila simplemente no entra (el JOIN la descarta) en vez de fallar ruidosamente.

## La trampa del catálogo público

Los `@WebMvcTest` de cada slice corren con `addFilters = false`: **no ven la cadena de seguridad**. Un endpoint que se olvidó de la lista `permitAll` de `SecurityConfig` pasa todos sus tests y recién falla en el navegador.

Pasó al sumar `/api/niveles-intensidad` (E4Ad-HU05). El frontend pide las cuatro listas del catálogo en un mismo `Promise.all` al montar `DataProvider`, antes de que haya sesión — y `Promise.all` se rechaza entero si una sola promesa falla, así que ese 401 dejaba **la landing completa** en estado de error, con el catálogo invisible para cualquier visitante. Los 547 tests seguían en verde.

Lo cubre `shared/security/CatalogoPublicoTest`, que es `@SpringBootTest` + `@AutoConfigureMockMvc` justamente porque es la única forma de ejercitar los filtros. Prueba las dos direcciones: que el catálogo responda sin sesión, y que lo que no es catálogo siga dando 401.

## Los tests corren contra la base REAL

No hay `src/test/resources/application.yml`, así que `@SpringBootTest` levanta con la configuración de `application.yml` + `.env` — es decir, **contra Supabase**. Dos consecuencias que conviene tener presentes:

- **`./mvnw test` aplica las migraciones pendientes.** Flyway corre al levantar el contexto. No es necesario arrancar la aplicación aparte para migrar; correr la suite ya lo hace.
- Los tests de contexto tardan ~25 s por la latencia de red, y **no se pueden correr sin conexión ni con la base caída**. Si algún día hace falta aislarlos, el camino es un `application.yml` de test con H2 o Testcontainers, no tocar los tests.

## Seguridad de las entradas: anti-inyección

Las reglas de seguridad de la sección 4 piden validación anti-inyección (SQL y XSS) en todas las entradas.

- **SQL: cubierto por construcción.** Todas las consultas son derivadas de método, JPQL o nativas con parámetros nombrados. No hay una sola concatenación de input en un `@Query` ni uso de `EntityManager.createNativeQuery`. Si alguna vez hace falta SQL dinámico, va por `Specification` (`ActividadSpecifications`), no por strings.
- **XSS: `@SinHtml`** (`shared/error`), aplicada a los campos de texto libre que escribe un usuario y lee otro — nombre, descripción, motivo, comentario, ubicación, detalle, respuesta, especialidad. **Rechaza, no sanea**: sanear cambiaría en silencio lo que la persona escribió, y para el motivo de una denuncia eso es peor que un 400. Deliberadamente **no** rechaza un `<` suelto ("cupo < 10" es texto legítimo); busca etiquetas (`</?tag…>`) y protocolos peligrosos (`javascript:`, `data:`). Es la segunda línea: React ya escapa al renderizar, pero eso no cubre un mail, un PDF o un export.

## Moderar reseñas: tres caminos, y el tercero faltaba

Una reseña se puede sacar de circulación por tres vías distintas, y confundirlas lleva a buscar el botón donde no está:

| Quién | Cuándo | Qué hace |
|---|---|---|
| El alumno autor | siempre, sólo la suya | `eliminarresenia` → baja lógica |
| El admin | mientras está `enModeracion` | `rechazarresenia` (cola de moderación) → baja lógica |
| El admin | ya publicada | **`ocultarresenia`** → `oculta = true` |

**El tercero es nuevo y cierra un agujero real.** Antes, una reseña publicada sólo se podía bajar si el instructor dueño de la actividad la denunciaba (`denunciarresenia`) y el admin resolvía esa denuncia con `OCULTAR_RESENIA`. O sea: si a la moderación se le colaba un insulto, el admin que lo veía después **no tenía ningún botón** — dependía de que el instructor estuviera mirando. Peor, el instructor sólo puede denunciar una vez la misma reseña (`existsByReseniaIdAndDenuncianteId`), así que un caso desestimado por error quedaba cerrado para siempre.

- **`POST /api/admin/resenas/{id}/ocultar`** (permiso `denuncias.resolver`, el mismo que moderar). **Motivo obligatorio**: ocultar el contenido de otro es una decisión que hay que poder justificar, y es lo que queda en `AuditAccion.RESENIA_OCULTADA`.
- **Oculta, no borra — y es a propósito.** La fila queda entera con su autor, su texto y su fecha. Si el contenido llega a ser algo en lo que deba intervenir la justicia (una amenaza, una difamación), la evidencia y la identidad de quien la escribió tienen que seguir existiendo; borrarla destruiría justamente lo que haría falta. Lo que cambia es que `findVisiblesPorActividad` y `recalcularRating` la excluyen.
- **Rechaza una reseña `enModeracion`** con un mensaje que manda a la cola: ocultar algo que nadie vio no tiene sentido y dejaría dos mecanismos para el mismo estado.
- **`listarreseniaspublicadas`** (`GET /api/admin/resenas/publicadas`) es el listado que hacía falta para poder ejercerlo: `listarresenaspendientes` sólo devuelve `enModeracion = true`. Incluye las ya ocultas a propósito — el admin tiene que ver qué bajó, no sólo lo que sigue visible.

## Auditoría: la fila se lee en castellano, y los 403 también se registran

- **`listarauditoria` devuelve `descripcion`**, armada por `DescripcionAuditoria` a partir de la acción, el actor y la metadata. La columna "Detalle" de la pantalla mostraba `"cb2ce9c9-… · -roles.configurar"`: trazable pero ilegible. Se calcula **al leer**, no se guarda, así que las filas viejas también quedan legibles.
  - Va en el backend y no en la pantalla porque nombrar el permiso y el rol necesita dos catálogos que viven en la base; resolverlo en el cliente obligaría a duplicar las 18 claves.
  - El `switch` es **exhaustivo sobre el enum** (sin `default`): agregar un `AuditAccion` sin describirlo no compila. Si la frase específica es difícil, poné una genérica — siempre es mejor que un UUID.
  - **La evidencia técnica no se pierde**: `entidadId` y `metadata` siguen viajando en la respuesta y el id tiene su propia columna.
- **`ACCESO_DENEGADO`**: un intento de entrar a algo sin permiso ahora deja rastro (RN-14). Que un alumno le pegue a `/api/admin/roles` es justo lo que una auditoría quiere ver, y antes el 403 se devolvía y ahí moría. Se registra en `GlobalExceptionHandler.handleAccessDenied` — el único punto por el que pasan todas, porque `@PreAuthorize` deniega vía AOP dentro del DispatcherServlet y nunca llega al `AccessDeniedHandler` de filtro. La metadata guarda método + ruta.
  - El `AuditService` se inyecta ahí con `ObjectProvider`, no por constructor: ese advice se importa en ~50 `@WebMvcTest` que levantan un contexto mínimo sin ese bean. Si no está, no se audita y el 403 se devuelve igual.

## La clase se congela, y tiene precio propio (V23)

Decisión del usuario: **una vez que la clase entró en período de inscripción y tiene al menos un inscripto, queda congelada** — no se editan sus datos ni la alcanza un cambio de la actividad. A esa altura hay gente que pagó por unos datos concretos.

- **`VentanaInscripcion.estaCongelada(ahora, fechaHora, cuposOcupados)`** es la regla: `cuposOcupados > 0` y faltan ≤ 4 días (`UMBRAL_PREINSCRIPCION`). Mira ese umbral y **no** `esVentanaInscripcion`, que devuelve false en la última hora previa y después de dictada: congelada es un estado sin vuelta atrás.
- **`actualizarclase` la rechaza** con 400 y el mensaje que dice qué hacer en su lugar. El camino para una clase congelada que no se va a dictar es **cancelarla**, que reintegra y avisa.
- **`Clase.precio`** (V23): cada clase nace con el precio vigente de su actividad y `inscribirse` cobra **ese**. Antes el cobro leía `clase.getActividad().getPrecio()` en el momento de inscribirse, así que editar el precio de la actividad lo reescribía retroactivamente para todas sus clases — dos alumnos de la misma clase podían terminar pagando distinto según cuándo se anotaron. `actualizaractividad` propaga el precio nuevo **sólo a las clases no congeladas**.
- Si agregás otro camino que cree una `Clase` (hoy son `crearclase` y `materializaragendas`), tenés que setearle el precio: la columna es `NOT NULL`.
- **`Clase.precio` viaja en los seis DTO de clase.** El campo existía desde V23 pero **ningún endpoint lo devolvía**, así que el frontend seguía mostrando `actividad.precio` en todas partes: el panel de inscripción del alumno prometía un importe y `inscribirse` cobraba otro apenas la actividad cambiaba de precio. Hoy lo llevan `obteneractividad` (dentro de `Clase`), `listarclasesadmin`, `listarclasesinstructor`, `listarrosterclase`, `crearclase` y `actualizarclase`. `listarmisclases` ya lo tenía. **Al agregar un DTO nuevo que describa una clase, incluilo**: sin el precio de la clase, la pantalla no tiene forma de saber cuánto vale y cae al de la actividad, que es el precio de lista y no lo que se cobra.

## Eliminar una Categoría: cascada sobre sus Tipos, bloqueo por Actividades

`eliminarcategoria` bloqueaba con **cualquier** `TipoActividad` activo colgando. Eso obligaba al administrador a borrar los Tipos uno por uno para poder borrar la Categoría que los agrupa, aunque ninguno se estuviera usando — y un Tipo sin actividades no es un dato que haya que preservar, es parte de la taxonomía que se está dando de baja.

Hoy la regla es la de `eliminartipoactividad`, elevada un nivel: **decide la Actividad, no el Tipo**.

- Si **alguno** de los Tipos de la categoría tiene una `Actividad` activa → `CategoriaEnUsoException` (409, `CATEGORIA_EN_USO`) y **no se borra nada**. El mensaje **nombra los tipos culpables**: con varios colgando, "está en uso" no le dice al admin cuál tiene que vaciar primero. Para eso `CategoriaEnUsoException` ganó un constructor con mensaje; el sin argumentos se conserva.
- Si ninguno tiene actividades → se da de baja lógica **la categoría y todos sus Tipos**, en la misma transacción, y cada Tipo deja su propio `AuditAccion.TIPO_ACTIVIDAD_ELIMINADO` con el motivo de la cascada.
- **Se revisan todos los Tipos ANTES de borrar el primero.** Con la comprobación intercalada en el loop, un segundo Tipo en uso dejaba al primero ya marcado dentro de la transacción; el rollback lo salvaba, pero la lógica quedaba dependiendo de él. Hay un test que fija justamente eso (`eliminar_conUnTipoConActividades_lanzaCategoriaEnUsoYNoBorraNada`).
- Del lado del frontend, la confirmación avisa cuántos Tipos se van a llevar puestos — ver `activehub-frontend/CLAUDE.md`.

## `listarresenasactividad` devuelve la respuesta del instructor

`responderresenia` guardaba `respuesta_instructor` desde V15, pero el **único listado que lee el alumno** (`GET /api/actividades/{id}/resenas`, el que alimenta el detalle de la actividad) no lo devolvía. Resultado: el instructor respondía, lo veía en su propia pantalla, y el destinatario de la respuesta no la veía nunca. Hoy el DTO lleva `respuestaInstructor` y `respuestaInstructorAt`.

## Soporte: el formulario de Ayuda ahora tiene backend (V24)

Reportado: *"toda la sección de Soporte no funciona"*. El formulario "Reportar un problema" de `/ayuda` existía desde el primer día, pero **no había nada detrás**: ni tabla, ni endpoint, ni caso de uso. La pantalla hacía `setReportSent(true)` y le decía "¡Gracias! Recibimos tu reporte" a alguien cuyo reporte se descartaba.

- **Dominio:** `domain/soporte/` (`ReporteSoporte`, `EstadoReporteSoporte`: Abierto·Cerrado, con su `AttributeConverter` como el resto de los enums con etiqueta). **No extiende `BaseEntity`**, por el mismo criterio que `Denuncia` e `Inscripcion`: "Cerrado" ya es un estado terminal y cumple el rol del soft-delete.
- **`usuario_id` es NULLABLE a propósito y el `email` es obligatorio.** `/ayuda` es pública y quien necesita soporte muchas veces es justamente alguien que **no pudo registrarse o entrar** — exigirle sesión lo dejaría sin forma de avisar. Por eso `crearreportesoporte` es el cuarto endpoint de la lista `permitAll` de `SecurityConfig` (el primero que no es auth ni catálogo), su Controller acepta `Authentication` en **null**, y el Service guarda el reporte como anónimo. Un `autorId` que ya no existe (token vivo de una cuenta dada de baja) tampoco lo invalida: se guarda sin autor.
- **Slices:** `crearreportesoporte` (POST `/api/soporte/reportes`, **público**), `listarreportessoporte` (GET `/api/admin/soporte/reportes`) y `cerrarreportesoporte` (POST `/api/admin/soporte/reportes/{id}/cerrar`, respuesta opcional). Los tres auditan (`REPORTE_SOPORTE_CREADO` / `REPORTE_SOPORTE_CERRADO`).
- **Clave de permiso nueva: `soporte.gestionar`** (V24), sólo para ADMIN. Es un módulo con pantalla propia, así que le corresponde su casilla, igual que `taxonomia.gestionar` o `penalizaciones.gestionar`. Recordá que `GuardasDePermisoTest` compara las claves de los `@PreAuthorize` contra los `INSERT INTO permiso` **en las dos direcciones**: la fila de la migración y las dos guardas van juntas o el test falla.
- **El cierre no se deshace:** volver a cerrar pisaría la respuesta y la fecha del cierre original, así que `cerrarreportesoporte` rechaza un reporte ya `Cerrado` con un 400. Mismo criterio de "el estado sólo avanza" que `tomardenuncia`.
- **`findAllConDetalle` usa LEFT JOIN FETCH** en `usuario` y `cerradoPor`: las dos relaciones son opcionales (un reporte anónimo no tiene autor, uno abierto no tiene quién lo cerró) y con INNER desaparecerían del listado — la misma trampa que ya había costado las denuncias de reseña.
- **El listado devuelve `autorNombre` en null, no "Anónimo".** La etiqueta que ve el administrador la elige la pantalla; el backend manda el dato.

## La hora de fin de una clase es derivada, no un campo

`CrearClaseRequest` y `ActualizarClaseRequest` **ya no llevan `horaFin`**: el Service la calcula como `fechaHora + actividad.duracionMin`. La duración es un dato de la actividad (E2I-HU03 criterio 1), así que pedirla otra vez por clase era el mismo dato dos veces — y dejaba crear una clase de 90 minutos para una actividad que promete 60. El criterio 4 ("fin posterior al inicio") pasa a estar garantizado por construcción: `duracion_min` tiene un `CHECK > 0`.

## Denunciar tiene fecha límite, no sólo espera mínima

`creardenuncia` ya exigía ≥ 1 h desde el inicio y clase `Finalizada`. Ahora también rechaza pasadas `VentanaPagos.PERIODO_DENUNCIAS` (24 h) desde el inicio: es la misma ventana que retiene el pago, así que después de eso `LiberarPagosScheduler` ya acreditó la plata y la denuncia no tiene nada que reintegrar ni que frenar. Antes no había tope y una denuncia de una clase de hace meses se aceptaba, quedaba `Pendiente` y el admin no tenía ninguna acción útil.

## Penalizaciones: plazo mínimo, dos tipos combinables y suspensión con datos

- **Sólo se penaliza a quien dicta clases.** La sanción existe por la inasistencia del profesor (E4Ad-HU06 / RN-13): aplicarla a un alumno o a un administrador no significa nada, y el formulario los ofrecía igual. `CrearPenalizacionService` lo rechaza preguntando por el **permiso** `clases.gestionar`, no por el nombre del rol (RN-19), así que un rol nuevo que dicte clases también es penalizable. Del lado del listado, `ListarUsuariosAdminResponse.puedeDarClases` sale de la misma consulta y es lo que filtra el selector de la pantalla.
- **Una suspensión temporal NO bloquea el login.** Decisión del usuario: el penalizado entra —tiene que poder ver su sanción, sus clases canceladas y sus datos— pero no puede operar. Antes `crearpenalizacion` y `resolverdenuncia` ponían la cuenta en `SUSPENDIDO`, que es justo la condición con la que `iniciarsesion` y `renovarsesion` rechazan: la sanción lo dejaba afuera de la plataforma. Hoy **ninguno de los dos toca `EstadoUsuario`** — ese estado vuelve a significar sólo lo que dice `actualizarestadousuario`, suspensión administrativa de la cuenta.
  - Lo que hace cumplir la sanción es **`shared/security/PenalizacionVigenteGuard`**, que consulta la vigencia real (`PenalizacionRepository.findSuspensionVigente`) en fecha de calendario de `Zonas.AR`. Está cableado en los seis slices que crean o modifican oferta: crear/editar/eliminar actividad y crear/editar/eliminar clase. **Quedan afuera a propósito** `confirmarcobroefectivo` y `listarrosterclase` (cortarlos dejaría plata sin registrar), `responderresenia`/`denunciarresenia` (derecho a réplica sobre lo ya publicado) y `cancelarclase` (es la salida, no un alta).
  - En `eliminaractividad` la guarda aplica **sólo al dueño**: un moderador no está operando su propia oferta, y un admin penalizado no debería quedarse sin moderar la plataforma.
- **Una suspensión cancela las clases del período y reintegra.** Un instructor que no puede dictar no puede dejar a sus inscriptos esperando: `crearpenalizacion` y `resolverdenuncia` cancelan sus clases dentro de la vigencia con la cascada completa (inscripción cancelada, pago `Retenido` y `Efectivo` a `Cancelado`, notificación a cada alumno). La ventana se arma en `Zonas.AR` y el último día cuenta entero. Las dos cascadas van **inline**, por la convención de no inyectar el Service de un usecase dentro de otro.
- **La penalización es irreversible y no hay endpoint para deshacerla**, a propósito: es un acto sancionatorio con constancia en auditoría. La pantalla lo avisa y pide una confirmación explícita antes de aplicar.
- **Mínimo 15 días** para toda Suspensión temporal (`domain/penalizacion/VentanaPenalizacion.MINIMO_DIAS_SUSPENSION`). Vale para el alta manual del admin y para la que sale de resolver una denuncia. El frontend replica el número en dos pantallas; si lo cambiás, cambialo en las tres.
- **Los dos tipos se pueden aplicar en una sola operación.** `crearpenalizacion` recibe `tipos: [...]` (no `tipo`) y guarda **una fila por tipo**, no una fila "mixta": el enum de la base sigue teniendo dos valores. La respuesta devuelve la lista de lo aplicado y el contador del usuario suma una por cada una. Decisión del usuario, tomada para no tocar el modelo.
- **`resolverdenuncia` con SUSPENDER ahora exige `diasSuspension`** (≥ 15) y acepta `montoMulta` opcional. Crea la Penalización de Suspensión temporal con vigencia `hoy → hoy + días` y, si el monto es mayor a cero, además una Económica; las dos quedan atadas a la denuncia. Antes solo ponía `SUSPENDIDO` y listo: sin vigencia, el scheduler que levanta suspensiones vencidas no tenía qué vencer y la suspensión era de hecho permanente. El Service usa `Clock` inyectado, como el resto.
- **`REINTEGRAR` alcanza a TODA la clase**, no sólo a quien denunció (decisión del usuario). Lo que se denuncia es un hecho de la clase —el instructor faltó, hubo una situación grave— y eso afecta a todos los que pagaron, no al que además se tomó el trabajo de reportarlo. Antes `reintegrar()` buscaba una sola inscripción, la del denunciante: a los demás se les liberaba el pago al instructor apenas vencía el período de denuncias. Se reintegran los pagos `Retenido` **y** `Efectivo` (mismo criterio que `cancelarclase`) y se le notifica a cada alumno. Ojo con la decisión 6: ahí el efectivo queda intacto **cuando cancela el propio alumno**; acá el reintegro lo ordena la plataforma.
- **Monto cero = sin multa**, no una multa de $0: si `montoMulta` viene en 0 o null, la sanción es solo la suspensión.
- **Orden del listado:** `findAllConDetalle` devuelve de la **más antigua a la más reciente** (pedido del usuario). Era DESC.

## Notificaciones clickeables: el destino lo resuelve el usecase (V25)

La campana mostraba texto muerto. *"Nueva inscripción en la clase de Yoga del 12/03"* no llevaba a ningún lado: había que ir a buscar la clase a mano. Ahora cada `Notificacion` guarda **a dónde lleva el click** en `destino_tipo` + `destino_id`, y el frontend lo mapea a una ruta (`lib/notificaciones.ts`).

**`entidadId` no alcanzaba, y no hay que intentar reusarlo para esto.** Dos razones:

1. Su significado cambia según el `tipo` — a veces es una inscripción, a veces una clase, una reseña, una denuncia o un usuario —, así que el cliente no puede interpretarlo sin una tabla de casos especiales que ya se había desincronizado sola (`INSCRIPCION_CANCELADA` guardaba la inscripción en `cancelarinscripcion` y la clase en `resolverdenuncia`).
2. **El destino útil muchas veces NO es esa entidad.** Al instructor una inscripción nueva le sirve abierta en el roster de la **clase**, no en la fila de la inscripción; y un horario nuevo de un favorito le sirve al alumno abierto en la **actividad**, que es donde puede anotarse. Esa navegación (inscripción → clase → actividad) el usecase ya la tiene cargada y el cliente tendría que salir a pedirla.

`entidadId` se conservó tal cual estaba: es trazabilidad, apunta al registro que originó el aviso. El destino es otra cosa y viaja aparte.

Cómo se agrega una notificación nueva:

- Se pasa un `Destino` (`shared/notificacion/Destino.java`) armado con sus fábricas: `Destino.clase(id)`, `Destino.actividad(id)`, `Destino.inscripcion(id)`, `Destino.resenia(id)`, `Destino.denuncia(id)`, `Destino.perfilInstructor(id)`, `Destino.ninguno()`. El record existe para que no se pueda guardar un tipo de destino con el id de otra cosa.
- **El id tiene que ser el parámetro de ruta de la pantalla**, ya resuelto acá. Si el destino es la actividad, va el `actividadId`, no el de la clase.
- **El destino se elige por DESTINATARIO, no por notificación.** El mismo hecho manda dos avisos distintos: en `inscribirse` el alumno recibe `Destino.inscripcion(...)` y el instructor `Destino.clase(...)`.
- **`Destino.ninguno()` es una respuesta válida y hay que usarla cuando corresponde**: `PENALIZACION_APLICADA` (el sancionado no tiene pantalla donde ver sus penalizaciones; el ABM es del admin) y `ACTIVIDAD_ELIMINADA` (la actividad ya no existe). El frontend la muestra sin link — un click que rebota al home es peor que ninguno.
- El instructor no tiene bandeja de denuncias, así que todo lo que le llega por una denuncia de clase (`DENUNCIA_RECIBIDA`, `DENUNCIA_DESESTIMADA`, `INSTRUCTOR_SUSPENDIDO`) va a `Destino.clase(...)`, que es el contexto del reclamo. `DENUNCIA` sólo se usa para el alumno, que sí tiene "Mis denuncias".

Los tests de cada usecase verifican el destino, no lo dan por `any()`: es la parte que se rompe en silencio si alguien copia un `notificar` de otro slice sin revisar a dónde apunta.

**`listarmisresenas` ganó `oculta`, `respuestaInstructor` y `respuestaInstructorAt`** como parte de esto. `RESENIA_RESPONDIDA` no tenía a dónde llevar: "Mis reseñas" del alumno no mostraba la respuesta del instructor en ningún lado (sólo se veía en la página pública de la actividad), así que el click aterrizaba en una fila idéntica a las demás.

## Verificación de correo por código: qué reserva una dirección (V26)

Al registrarse sale un mail con un **código de 6 dígitos**. Ingresarlo valida la cuenta. La regla de negocio que ordena todo lo demás:

> **Un correo lo reserva la CONFIRMACIÓN, no el tipeo.** Mientras el código está enviado y sin ingresar, esa dirección sigue libre y otra persona puede registrarse con ella. Cuando alguien ingresa su código, queda tomada; sólo se libera si el dueño la cambia desde su perfil.

Eso es, literalmente, un **índice único parcial**: `ux_usuario_email_verificado ON usuario (lower(email)) WHERE deleted = false AND email_verificado = true`. El índice viejo exigía unicidad sobre toda cuenta viva, así que equivocarse al tipear el correo de un tercero se lo inutilizaba para siempre.

Consecuencias que **no son obvias** y que hay que respetar al tocar esto:

- **"Buscar el usuario por email" dejó de tener una única respuesta.** El login usa `findAllByEmailConRol` (varias filas) y desambigua con la **contraseña**: dos personas que tipearon el mismo correo tienen claves distintas. El orden pone primero a la verificada, que es la dueña. `findByEmailConRol` (singular) quedó sólo para los casos en que el correo ya está verificado.
- **La pregunta correcta es `existsVerificadoConEmail(email, excluirUsuarioId)`**, no `existsByEmailIgnoreCaseAndDeletedFalse`. La usan las dos altas, el cambio de correo y la edición de admin.
- **El chequeo se repite al confirmar.** Entre el alta y la confirmación otra persona pudo haberlo verificado, así que `verificaremail` vuelve a preguntar justo antes de escribir y devuelve 409 en vez de dejar explotar el índice.
- **`actualizarmiperfil` sí carga el DNI, y es la única forma de hacerlo después del alta.** Quien se registró con Google nunca pasó por un formulario que lo pidiera, así que sin esto no había manera de cargarlo nunca. Sigue siendo clave de unicidad (`existsByDniAndIdNotAndDeletedFalse`, que excluye a la propia cuenta) y habilita el login por DNI apenas se guarda.
- **`actualizarmiperfil` ya NO cambia el correo: lo rechaza.** Era un agujero — el correo es la credencial verificada, y cambiarlo con un PUT sin confirmar dejaría tomar la casilla de cualquiera. Lo mismo vale para `actualizarusuarioadmin`, que sí lo cambia (es una corrección administrativa) pero **lo deja en `email_verificado = false`**: un admin no puede reservar la dirección de otro.
- **El cambio de correo no toca la cuenta hasta confirmarse.** `solicitarcambioemail` sólo emite el código contra la dirección nueva (y pide la contraseña actual, porque es un cambio de credencial); el `usuario.email` lo reemplaza `verificaremail`. Un error de tipeo en el correo nuevo no deja al usuario sin el viejo, que además sigue reservado mientras duda.

### Sin confirmar no se puede hacer NADA: `EmailVerificadoFilter`

La cuenta existe y tiene token desde el alta, pero hasta ingresar el código lo único que
responde son los dos endpoints del código, `/api/auth/me` y `/api/auth/refresh`. Todo lo demás
es `403 EMAIL_SIN_VERIFICAR`.

- **Está en el backend a propósito, no sólo en las rutas de React.** La guarda del cliente
  evita que el usuario *navegue*; sin el filtro, el token que recibe al registrarse serviría
  para inscribirse con un `fetch`. "No podés hacer nada hasta verificar" tiene que ser una
  afirmación sobre el sistema, no sobre la pantalla.
- **El flag viaja en el JWT (`emailVerificado`), no se consulta la base en cada request.** El
  claim alcanza porque **sólo puede pasar de false a true**: un token viejo nunca habilita de
  más. Y por eso **`verificaremail` devuelve un token NUEVO** — si el cliente siguiera con el
  anterior, confirmar el código no desbloquearía nada hasta que venciera.
- **Claim ausente = verificado.** Los tokens emitidos antes de que existiera quedarían
  encerrados en la pantalla del código sin haber hecho nada.

Lo demás vive en `shared/email/`: `VerificacionEmailService` (emitir/reemitir/validar) y `PlantillaEmail` (el HTML). Reglas que hace cumplir el service: **un solo código vigente** (se invalidan los pendientes antes de emitir), **espera mínima entre reenvíos**, **tope de intentos fallidos** (6 dígitos son 10^6) y el **código guardado hasheado**. El incremento de intentos se persiste con `saveAndFlush` antes de lanzar, por la misma razón que `iniciarsesion` no es `@Transactional`: si no, el rollback de la excepción se lleva el contador.

### El mail: Gmail SMTP, y qué pasa sin credenciales

`spring-boot-starter-mail` contra `smtp.gmail.com:587` con **contraseña de aplicación** (ver README). Dos decisiones:

- **Sin `MAIL_USERNAME`, `EmailSender` escribe el mail en el log en vez de fallar.** Es lo que permite usar el repo sin secretos: si no, un checkout limpio no podría registrar a nadie y `ActivehubApiApplicationTests` dependería de una casilla real. **En ese modo el código aparece en el log del backend**, que es cómo se prueba el flujo en desarrollo.
- **`enviar` nunca propaga: devuelve `false`.** Gmail puede estar caído o tardar más que el timeout, y ninguna de esas cosas justifica perder un alta ya guardada. Por eso las respuestas de registro traen `mailEnviado` y existe el botón de reenviar.

`PlantillaEmail` es HTML de tablas con todo el CSS inline a propósito: Outlook renderiza con Word y Gmail borra el `<head>`. No agregar Thymeleaf para interpolar tres variables. Los colores son los mismos hex del frontend. `PlantillaEmailTest` escribe los dos mails en `target/mails-preview/` para poder abrirlos en el navegador — es la única forma real de revisar un diseño de mail sin mandarlo.

## Recuperar la contraseña olvidada: el código habilita UNA cosa

Dos slices públicos, sin sesión, con el mismo código de 6 dígitos de V26: `solicitarrecuperacionpassword` (`POST /api/auth/recuperar-password`) y `restablecerpassword` (`POST /api/auth/recuperar-password/confirmar`). **No hay tabla ni migración nueva**: se reusa `verificacion_email` con un `PropositoVerificacion` nuevo, `RECUPERACION_PASSWORD`. Los dos endpoints están en la lista `permitAll` de `SecurityConfig`, por la razón obvia: los pide quien no puede entrar.

Lo que no es obvio y hay que respetar:

- **La respuesta de "pedir el código" es la MISMA exista o no la cuenta.** Si contestara distinto sería un oráculo de enumeración: probando direcciones se averigua quién está registrado. Por eso los tres desenlaces que no emiten nada —no hay cuenta, es de Google, el correo no está verificado— devuelven el mismo `{envioHabilitado, ttlMin}` que el caso feliz, y por eso **se traga el `DemasiadosIntentosException`** de la espera entre reenvíos: dejar salir el 429 sólo para los correos registrados vuelve a delatar cuáles existen. El mismo criterio en el paso 2: correo inexistente, cuenta de Google y código de otro propósito devuelven el **mismo texto** que un código equivocado.
- **Sólo se recupera una cuenta verificada y `LOCAL`.** Un correo sin confirmar puede ser de cualquiera (es la regla de V26), así que mandarle un código sería mandárselo a quien lo tipeó y no a quien lo tiene; y una cuenta de Google no tiene contraseña utilizable, así que "recuperarla" no le devuelve el acceso.
- **El propósito se verifica ANTES de consumir el código.** `VerificacionEmailService.validar` valida el **último** código del usuario sea cual sea su propósito, y al validarlo lo marca como usado: si el propósito se mirara después, un código de alta o de cambio de correo se consumiría acá antes de ser rechazado y el usuario lo perdería sin haberlo usado. Se lee primero con `findFirstByUsuarioIdOrderByCreatedAtDesc` y recién entonces se valida.
- **No devuelve token: restablecer no inicia sesión.** Escribir la contraseña una vez en el login es lo que confirma que se la guardó.
- **Las sesiones abiertas siguen abiertas.** El JWT es stateless y no hay tabla de sesiones; un token emitido antes del cambio vale hasta que venza (30 min). Está asumido: lo que cierra el acceso es que la contraseña vieja ya no sirve para sacar uno nuevo. Si algún día hace falta cortar en el acto, el camino es un claim de versión de credencial en el token, **no** una tabla de sesiones.
- Audita con `AuditAccion.PASSWORD_RESTABLECIDA`, distinta de `PASSWORD_CAMBIADA`: esa la hace alguien que ya estaba adentro y sabía la anterior.
- La política de la contraseña nueva es la misma RN-20 de siempre, en el Request DTO.

## Almacenamiento de archivos: una interfaz, disco o Supabase Storage

`shared/storage/AlmacenamientoArchivos` es la misma clase de costura que `PaymentGateway`: los **nueve** usecases que suben o leen archivos (`subirfotoperfil`, `verfotoperfil`, `subirfotoactividad`, `verfotoactividad`, `agregarimagenactividad`, `eliminarimagenactividad`, `verimagenactividad`, `subirdocumento`, `descargardocumentoinstructor`, más `registrarinstructor`) **ya no conocen el filesystem**. Ninguno vuelve a importar `java.nio.file`.

- **Tres métodos y un enum.** `guardar` / `leer` / `borrarSiExiste`, y `CarpetaArchivos` (`FOTOS_PERFIL`, `FOTOS_ACTIVIDAD`, `DOCUMENTOS_INSTRUCTOR`) decide el directorio en disco o el prefijo dentro del bucket.
- **`borrarSiExiste` NUNCA lanza**, y es parte del contrato: se llama para limpiar una foto ya reemplazada o un huérfano de un rollback, y fallar ahí convertiría una operación exitosa en un error para el usuario.
- **En la base se guarda sólo el NOMBRE del archivo**, nunca una ruta ni una URL. Es exactamente lo que permitió cambiar el destino sin migrar una fila, y hay que mantenerlo: una URL de Supabase ata las filas a un proyecto concreto y deja de resolver el día que el bucket cambie.
- **La implementación se elige por configuración presente, no por un flag** (`AlmacenamientoConfig`). Con `SUPABASE_STORAGE_URL` y `SUPABASE_STORAGE_SERVICE_KEY` cargadas se usa Supabase; con alguna vacía, disco. Un `modo=supabase` sería una tercera cosa que puede quedar desincronizada de las credenciales (encendido sin clave: todo falla; apagado con clave: el despliegue escribe en un disco que se borra solo). **El modo elegido se loguea al arrancar** — es la forma de darse cuenta de que a un despliegue no le llegó una variable.
- **El bucket es PRIVADO y los archivos se siguen sirviendo por nuestra API.** No se generan URLs públicas ni firmadas. La documentación del instructor son DNI y certificados de terceros: en un bucket público, adivinar la URL alcanza para leerlos salteando el permiso que hoy los protege. Efecto colateral bueno: el frontend no cambió ni una línea. El costo es que cada imagen viaja dos veces (Supabase → backend → navegador), despreciable con el `Cache-Control` de una hora que ya mandan esos endpoints.
- **`AlmacenamientoSupabase` habla REST a mano con `HttpClient`** (PUT/GET/DELETE sobre `/storage/v1/object/{bucket}/{carpeta}/{nombre}`, con `Authorization: Bearer` y `x-upsert: true`), sin SDK: mismo criterio que `GoogleIdTokenVerifier`. El cliente HTTP se **inyecta** para poder testearlo sin un proyecto de Supabase.
- **La clave es la `service_role`**, que saltea RLS: sólo backend, nunca en el frontend ni versionada.
- **Disco sigue siendo el modo por defecto y el de desarrollo**, y es efímero en la nube: en Render el contenedor pierde su disco en cada despliegue. Ése es el motivo de todo esto.

## "Continuar con Google": el ID token se valida localmente

`POST /api/auth/google` recibe el ID token de Google Identity Services y **es alta y login a la vez**: nadie que aprieta ese botón sabe si "ya tiene cuenta", eligió una identidad.

- **`GoogleIdTokenVerifier` valida a mano con jjwt**, sin `google-api-client` (arrastra media docena de dependencias transitivas para bajar unas claves públicas y verificar una firma) y sin el endpoint `tokeninfo` (un viaje de red por login). Verifica **firma** contra el JWKS de Google, **`aud` == nuestro client id** (un token legítimo emitido para otra app está bien firmado: sin este chequeo serviría para entrar acá), `iss`, `exp` y **`email_verified`**. Las claves se cachean 6 h y se refrescan una vez si la firma no valida, para sobrevivir a la rotación.
- **La cuenta de Google nace con `emailVerificado = true`** y no recibe código: Google ya confirmó la dirección. Como es el flag que reserva el correo, queda tomado en el acto.
- **Si ya existía una cuenta verificada con ese correo, se entra a esa**, aunque se haya creado con contraseña: es la misma persona. Las cuentas sin verificar con esa dirección se ignoran — no probaron nada.
- **El request lleva `rol`, y eso cambia el desenlace.** Tres modos:
  - **`SESION`** — la cuenta ya existe (se entra siempre, venga el rol que venga), o el rol es `ALUMNO` y se crea en el acto.
  - **`SIN_CUENTA`** — **el rol viene vacío**, que es el botón del *login*. Ahí no se crea nada: quien aprieta "Iniciar sesión con Google" espera entrar a su cuenta, no que le aparezca una nueva a medio llenar. La pantalla lo manda a registrarse.
  - **`COMPLETAR_INSTRUCTOR`** — ver abajo.

  Con `INSTRUCTOR` y sin cuenta previa **no se crea nada**: el alta de instructor exige documentación (RN-12) y Google no la trae, así que vuelve `modo: "COMPLETAR_INSTRUCTOR"` con la identidad para precargar el formulario. La cuenta la crea `registrarinstructor` cuando llegan los archivos, con el `googleIdToken` adjunto — **que se vuelve a verificar allá**, porque entre una cosa y la otra no se guarda estado y dar por bueno un "ya lo validamos" del cliente es confiar en el cliente. Ahí también se exige que el correo del formulario sea el de Google: si no, el token de una casilla serviría para registrar otra.
- Con `googleIdToken`, `password` deja de ser obligatoria en el alta de instructor (por eso su `@NotBlank` pasó a ser una validación del Service, que es quien ve los dos campos juntos) y la cuenta nace `GOOGLE` + verificada.
- La cuenta nueva por el camino de alumno queda sin teléfono ni fecha de nacimiento (Google no los da) y con un **`passwordHash` aleatorio**: `password_hash` es NOT NULL y esta cuenta no tiene contraseña. Por eso existe `usuario.auth_proveedor`: una cuenta `GOOGLE` no puede cambiar su contraseña ni su correo desde acá.

## Buscar ignora tildes: `translate()`, no `unaccent`

Reportado: *"en TODAS las búsquedas del sistema, que no distinga entre tildes y no tildes"*. Buscar `natacion` no encontraba **Natación**: para SQL `ó` y `o` son caracteres distintos y `lower()` no cambia eso. En castellano no es un detalle — casi toda palabra larga lleva tilde, y escribir sin acento es lo normal al tipear rápido.

`ActividadSpecifications.conTexto` aplana las dos puntas antes de comparar: el nombre en la base con `translate(lower(nombre), ACENTOS, SIN_ACENTOS)` y el término buscado con el mismo mapeo en Java. **La ñ también se aplana a `n`**, que es lo que la gente espera al buscar.

- **No se usa la extensión `unaccent`** aunque sea lo canónico: habría que instalarla con una migración, la base es de Supabase y tocarle las extensiones desde Flyway es un riesgo que no paga por una sola consulta. `translate()` es una función built-in y no necesita nada.
- **Las dos cadenas tienen que quedar del mismo largo.** `translate()` mapea posición a posición y lo que sobra en la primera lo **elimina** del texto: un carácter de más y la búsqueda empieza a comer letras.
- **`BusquedaSinTildesTest` ejecuta la consulta contra la base real.** Los tests de usecase son unitarios con mocks, así que la Specification se arma pero nunca se traduce a SQL — una función mal escrita pasaría todos esos tests y explotaría recién en el buscador del navegador. Ese test no afirma nada sobre las filas (el contenido de la base cambia): lo que prueba es que la consulta **corre**.
- El espejo del lado del cliente es `lib/texto.ts` (`normalizar` / `incluye`), que hace lo mismo con NFD. Si tocás uno, mirá el otro.

## Migraciones: la base es Supabase, no un Postgres local

`.env` apunta a un pooler de Supabase y **los tests (`ActivehubApiApplicationTests`) migran contra esa base real**. El pooler no garantiza DDL transaccional: una migración que falla a mitad puede dejar objetos creados y Flyway después reintenta el script entero. Ya pasó con V18. **Escribí toda migración nueva idempotente** (`CREATE TABLE IF NOT EXISTS`, `ADD COLUMN IF NOT EXISTS`, `ON CONFLICT DO NOTHING`); si igual queda a medias, hay que limpiar a mano lo que creó antes de reintentar.

## Bugs reales encontrados y corregidos en el camino (no repetirlos)
- **Aprobar una reseña devolvía 500 y rechazarla no.** `ActividadRepository.recalcularRating` es un `@Modifying` con `clearAutomatically = true`: limpia el contexto de persistencia, la entidad queda *detached* y cualquier relación LAZY sin tocar explota con `LazyInitializationException`. `AprobarReseniaService` leía `resenia.getAlumno()` **después** del recálculo; `RechazarReseniaService` ya leía todo antes. **Regla: en un Service que llame a un `@Modifying` con `clearAutomatically`, leé todo lo que necesites ANTES.** Ningún test lo veía porque con mocks la entidad nunca se detacha — el que lo fija ahora simula el detach con un spy.
- `EliminarActividadService` no cancelaba en cascada las clases/inscripciones/pagos asociados al eliminar una actividad, y el endpoint solo dejaba borrar al instructor dueño (un admin no podía). Se corrigió la cascada y el `hasAnyRole('INSTRUCTOR','ADMIN')`; **después, por decisión 5, la cascada se reemplazó por un bloqueo** — hoy con inscriptos lanza 409 y no borra nada.
- `ListarInstructoresController` bindeaba el query param de filtro como `estadoVerificacion`, pero el frontend mandaba `?estado=`. El mismatch hacía que el filtro se ignorara en silencio y devolviera *todos* los instructores. Corregido con `@RequestParam(value = "estado", ...)`. **Moraleja: si un filtro por query param "no hace nada", sospechar primero de un nombre de parámetro que no matchea, antes de asumir que la lógica de filtrado está mal.**
- El donut "Reservas por categoría" del Dashboard admin dependía de `DataContext.clases`, que en el frontend es una caché parcial (solo se llena cuando se visita el detalle de una actividad), no una lista completa — daba 0% siempre. Se resolvió agregando `actividadId` directo a la respuesta de `listarinscripcionesadmin` para no depender de esa caché.

## Reglas de negocio clave (ya implementadas, no reinterpretar)
- Pre-inscripción vs inscripción según faltan **>4 días** (`VentanaInscripcion.UMBRAL_PREINSCRIPCION = Duration.ofDays(4)`, solo PreInscripción, no ocupa cupo) o **≤4 días y ≥1h** (`UMBRAL_CIERRE = Duration.ofHours(1)`, inscripción definitiva si hay cupos). Comparación por `Duration.between(ahora, fechaHora)` sobre `Instant` UTC — nada de redondeo a día calendario ni zona horaria. `ahora` sale de un `Clock` inyectado (`ClockConfig`, `Clock.systemUTC()`), no de `Instant.now()` directo, para poder testear con reloj fijo.
- Pago Mercado Pago → Inscripto + Pago `Retenido`. Efectivo → PagoPendiente hasta que el instructor confirma el cobro (`confirmarcobroefectivo`) → Inscripto.
- **Liberación de pagos (RN-04, segunda mitad):** `LiberarPagosScheduler` (`@Scheduled(fixedRate=15min)`) pasa `Retenido`→`Liberado` los pagos cuya clase está `Finalizada`, ya pasó el **período de denuncias** y **no tiene ninguna denuncia abierta**. El umbral vive en `domain/inscripcion/VentanaPagos.PERIODO_DENUNCIAS` (**24 h**, decisión del usuario; la spec nombra el período pero nunca definió su duración — ambigüedad 2 de la sección 12 de `HISTORIAS-DE-USUARIO.md`). Se cambia en esa única constante. Antes de esto **ningún pago pasaba nunca a `Liberado`** y el instructor no cobraba jamás.
- **Endpoints de cuenta propia** (`/api/usuarios/me`): `actualizarmiperfil` (PUT, valida email duplicado y teléfono solo-números), `cambiarmicontrasenia` (POST `/password`, pide la contraseña actual y aplica la misma política RN-20 que el registro) y `darmedebaja` (DELETE, **baja lógica**). Sirven a los tres roles: un instructor no verificado también puede editar sus datos, que es lo único que la spec le habilita mientras espera. Antes nada de esto existía y la pantalla de Perfil solo mutaba un array mock.
- **Otros endpoints que faltaban:** `listarmispagos` (`GET /api/alumno/pagos`, historial real del alumno con estado de pago), `actualizarresenia` (`PUT /api/alumno/resenas/{id}` — antes "editar" era borrar y recrear en dos requests sin transacción, y si el alta fallaba el alumno perdía la reseña), `reabrirsolicitudinstructor` (`POST /api/instructor/solicitud/reabrir`, único cambio de `EstadoVerificacion` que no dispara el admin; **sin** la guarda de instructor verificado, porque es justamente para el que no lo está) y `actualizarusuarioadmin` (`PUT /api/admin/usuarios/{id}`, no toca rol ni estado: el estado va por `actualizarestadousuario`, que tiene la guarda anti-auto-suspensión).
- **Penalizaciones con datos completos (migración V14).** `Penalizacion` ganó `monto` (para Económica), `fechaInicio`/`fechaFin` (vigencia de la Suspensión temporal) y `denuncia_id` (origen). Endpoints nuevos: `listarpenalizaciones` (`GET /api/admin/penalizaciones`) y `crearpenalizacion` (`POST`, alta manual del admin). Antes las penalizaciones solo nacían dentro de `resolverdenuncia` y **no existía ningún GET**, así que la pantalla leía el dataset mock del frontend: lo que el admin aplicaba no se guardaba y lo que el backend sí creaba no se veía. La validación condicional por tipo (monto vs fechas) vive en el Service, no en el Request, porque depende del valor de `tipo`.
- **Las suspensiones temporales se levantan solas:** `LevantarSuspensionesVencidasScheduler` (`@Scheduled(fixedRate=1h)`) devuelve a `ACTIVO` a los usuarios cuya vigencia venció, salvo que tengan otra suspensión superpuesta todavía vigente. Sin esto "temporal" era decorativo.
- **Transición Programada → Habilitada:** la dispara `HabilitarClasesProximasScheduler` (`@Scheduled(fixedRate=5min)`) cuando faltan ≤4 días (`VentanaInscripcion.UMBRAL_PREINSCRIPCION`, ahora `public`). Responde la ambigüedad 3 de la spec: antes **nadie** asignaba `Habilitada` y toda clase se quedaba en `Programada` hasta dictarse. El estado se persiste **solo para los badges de la UI**; la ventana de negocio para inscribirse se sigue calculando al vuelo con `VentanaInscripcion`, que es la fuente de verdad y no depende de que el job haya corrido.
- **Transición Pendiente → En Auditoría:** usecase `tomardenuncia` (`POST /api/admin/denuncias/{id}/auditar`), que dispara el frontend al abrir una denuncia. Es idempotente (abrir dos veces no re-audita) y rechaza tomar una ya `RESUELTA`: el estado solo avanza. Antes `EN_AUDITORIA` estaba en el enum y en el CHECK pero no lo escribía nadie, así que la denuncia saltaba de Pendiente a Resuelta y el alumno nunca la veía progresar.
- **RN-16 se aplica con `shared/security/InstructorVerificadoGuard`, no copiando el chequeo.** `exigirVerificado(instructorId, accion)` lanza `SinPermisoException` si el perfil no está `APROBADO`. Antes estaba copiado en solo 3 usecases, así que un perfil Pendiente o Rechazado podía operar llamando directo a la API. En `eliminaractividad` la guarda **no** aplica si el actor es ADMIN.
  - Hoy la llaman **10 usecases**: `crearclase`, `actualizarclase`, `eliminarclase`, `cancelarclase`, `confirmarcobroefectivo`, `listarrosterclase`, `notificarausenciaprofesor`, `eliminaractividad`, `responderresenia` y `denunciarresenia`.
  - **Deuda conocida:** `crearactividad` y `actualizaractividad` hacen el mismo chequeo **inline** (`perfil.getEstadoVerificacion() != APROBADO`) en vez de usar la guarda. El comportamiento es correcto, pero es exactamente el patrón que la guarda vino a eliminar. Si tocás alguno de los dos, pasalos a `exigirVerificado`.
  - La asistencia **no** está en esta lista y no debe volver: su slice se borró (decisión 2).
- **Un instructor rechazado pierde su oferta publicada.** `ActividadSpecifications.deInstructorVerificado()` filtra el catálogo público por perfil `APROBADO`, y `exigirOfertaVigente(...)` bloquea inscripción y preinscripción a clases de instructores no verificados. Antes, rechazar a un instructor ya aprobado no revocaba nada: sus actividades seguían visibles y aceptando inscripciones.
- **Bloqueo por intentos fallidos de login:** `shared/security/IntentosLoginService` (5 intentos / ventana de 15 min / bloqueo de 15 min) → `DemasiadosIntentosException` (429). El contador vive **en memoria**, por instancia: alcanza para el despliegue actual (una sola instancia), pero si alguna vez se escala horizontalmente hay que moverlo a un almacén compartido — la base alcanza, y es el camino coherente con haber descartado Redis. La clave es el email normalizado, así que también frena la fuerza bruta contra correos inexistentes sin revelar si existen.
- **`IniciarSesionService.login` NO puede ser `@Transactional`.** Los caminos de fallo auditan y después lanzan una excepción; como `ApiException` extiende `RuntimeException`, Spring hacía rollback y **se borraba el propio INSERT de auditoría**: en el log solo quedaban los `LOGIN_OK`. Sin transacción ambiente cada save commitea por su cuenta. Si alguien vuelve a poner `@Transactional` ahí, se pierden otra vez los intentos fallidos.
- **Política de contraseña (RN-20):** `^(?=.*[A-Z])(?=.*\d).{8,}$` en los tres Request de registro — 8 caracteres, una **mayúscula** y un número. Antes pedía "una letra cualquiera", así que `password1` pasaba. El helper `passwordStrength` del frontend replica el mismo criterio; si cambiás uno, cambiá el otro.
- **Eliminar clase exige que NO tenga inscriptos.** `EliminarClaseService` lanza `ClaseConInscriptosException` (409, `CLASE_CON_INSCRIPTOS`) si existe alguna inscripción no Cancelada. El borrado de clase es una baja lógica pelada: no cancela inscripciones, no reintegra y no notifica. Como `Clase` tiene `@SQLRestriction("deleted = false")`, dejarlo pasar con alumnos anotados les borraba del historial una clase que habían pagado, sin pasar nunca por Cancelada. **Para una clase con gente anotada el camino es `cancelarclase`**, que sí hace la cascada completa.
- **El instructor tiene endpoints propios para sus clases e inscripciones:** `GET /api/instructor/clases` (`listarmisclases`, incluye Finalizadas y Canceladas para el historial) y `GET /api/instructor/inscripciones` (`listarinscripcionesmisclases`, con alumno, estado y pago). Antes el único listado por instructor era `listarclasesinstructor`, que vive bajo `/api/admin` con el permiso `usuarios.gestionar`: el instructor **no podía pedir lo suyo**, y por eso el frontend lo derivaba de una caché parcial y de datos mock. Si agregás una pantalla del instructor que necesite clases o inscripciones, usá estos dos y **no** `DataContext.clases`.
- **Denunciar exige clase `Finalizada`** (RN-18), igual que reseñar: `CrearDenunciaService` valida el estado **además** del umbral de 1 hora desde el inicio. Antes solo miraba el tiempo, así que se podía denunciar una clase que seguía en Programada/Habilitada, inconsistente con `crearresenia`, que sí validaba el estado.
- **Alta de instructor transaccional** (E1A-HU04 crit. 9 / RN-12): `POST /api/auth/registro/instructor` es **multipart** (parte `datos` en JSON + parte `documentos`) y la documentación es **obligatoria**. Usuario, PerfilInstructor y DocumentoInstructor se crean en la misma transacción; si falla la validación o la escritura en disco, no queda ninguna cuenta creada. Como el filesystem no es transaccional, el service registra un `TransactionSynchronization` que borra los archivos ya escritos si hay rollback. `subirdocumento` sigue existiendo para adjuntar documentación *después* del alta. El límite de subida (`spring.servlet.multipart.max-file-size`) está en **5MB** para coincidir con lo que promete la UI: el default de Spring (1MB) rechazaba adjuntos que la pantalla daba por válidos.
- **Cuenta propia — los 7 endpoints que faltaban** (todos con su slice y su test): `PUT /api/usuarios/me` (`actualizarmiperfil`), `POST /api/usuarios/me/password` (`cambiarmicontrasenia`, misma regex RN-20 que el registro), `DELETE /api/usuarios/me` (`darmedebaja`), `GET /api/alumno/pagos` (`listarmispagos`), `PUT /api/alumno/resenas/{id}` (`actualizarresenia`), `POST /api/instructor/solicitud/reabrir` (`reabrirsolicitudinstructor`) y `PUT /api/admin/usuarios/{id}` (`actualizarusuarioadmin`). Antes el perfil era de solo lectura, editar una reseña era *borrar y volver a crear* en dos requests sin transacción, y "volver a postularme" solo parcheaba el directorio mock del frontend. Dos detalles que no hay que romper: `actualizarmiperfil` y `actualizarusuarioadmin` **normalizan el email a minúsculas** (el login busca con `lower()`), y `actualizarresenia` vuelve a poner `enModeracion = true` porque el contenido cambió.
- **El modelo de la sección 2 se completó en `V15__modelo_faltante.sql`.** Lo que agrega y por qué importa:
  - `usuario.dni` (nullable, índice único parcial). Es la **segunda credencial de login**: `IniciarSesionRequest` ya no tiene campo `email` sino **`identificador`**, sin `@Email` — con esa anotación un DNI válido se rechazaba por formato antes de llegar al service. `IniciarSesionService` discrimina con `[0-9]{7,8}` y busca por `findByDniConRol` o `findByEmailConRol`. Los tres registros lo aceptan como opcional y validan unicidad con `DniEnUsoException` (409).
  - `agenda_clases` + `clase.agenda_clases_id` + `clase.hora_fin`. `crearclase` ahora exige `horaFin` y `cuposMax`, valida **fin > inicio** (crit. 4) y **solapamiento** (crit. 8, vía `ClaseRepository.existeSolapamiento`, que excluye Canceladas y la propia clase al editar). Con `repetirSemanalmente` se guarda una `AgendaClases` y **`MaterializarAgendasScheduler`** (`@Scheduled(fixedRate=1h)`) instancia cada `Clase` **una semana antes** de dictarse, como pide el criterio 6 — no se generan N clases de una. `actualizarclase` aplica las mismas dos validaciones de horario.
  - `actividad.duracion_min` y **se eliminó `actividad.cupos_max`**. Ese campo era un fantasma peligroso: el formulario le mandaba 20 fijo y `CrearClaseService` caía a él cuando no venía cupo, así que ese 20 terminaba siendo el cupo real. El cupo vive en `Clase` y en `AgendaClases`, nunca en `Actividad`.
  - `actividad_imagen` (galería, sección 2 pide `imagenes[]`). `actividad.foto_path` queda como **portada**; `agregarimagenactividad` la promueve sola si no había, y `eliminarimagenactividad` promueve la siguiente si borrás la portada. Máximo 6, y la escritura en disco se revierte con un `TransactionSynchronization` si hay rollback.
  - `audit_log.ip`: la resuelve **`AuditService` solo**, desde `RequestContextHolder` (honra `X-Forwarded-For`), en vez de agregarle un parámetro a las ~60 llamadas a `registrar(...)`. Null cuando no hay request, que es lo correcto para los schedulers.
  - `denuncia`: `clase_id` pasa a **nullable**, entra `resenia_id` y `denunciante_id`, y un CHECK exige exactamente uno de los dos objetos. Con `clase_id NOT NULL`, denunciar una reseña (E2I-HU11 crit. 4) era imposible de guardar. Ojo: `findAllConDetalle` usa **LEFT JOIN** en todo el camino de la clase — con INNER, las denuncias de reseña existían en la base y no las veía nadie. También entran `resolucion` y `detalle`, que es lo que el denunciante ve al cerrarse el caso (E3A-HU11 crit. 2 y 7). `AccionResolucion` se movió al dominio como `ResolucionDenuncia` (suma `OCULTAR_RESENIA`).
  - `resenia.respuesta_instructor` + `oculta`. `responderresenia` (`POST /api/instructor/resenas/{id}/respuesta`) y `denunciarresenia` (`POST /api/instructor/resenas/{id}/denuncia`). **`enModeracion` ≠ denunciada**: la primera es "el admin todavía no la aprobó", la segunda "el instructor la reportó". `findVisiblesPorActividad` y `recalcularRating` ahora excluyen las ocultas.
- **Sesión con expiración por inactividad** (E1A-HU02 crit. 3), sin entidad `Sesion`: el TTL del JWT (`app.jwt.expiration-min`, ahora **30 min**) *es* la ventana de inactividad, y `POST /api/auth/refresh` (`renovarsesion`) reemite el token. El frontend lo llama con la actividad del usuario (throttle de 5 min). Antes el token duraba 2h fijas y no se renovaba: la sesión moría por antigüedad aunque estuvieras trabajando, y sobrevivía a la inactividad — exactamente al revés de lo que pide el criterio. La renovación revalida el estado: un usuario suspendido o dado de baja no sigue estirando su sesión.
- Cupos: `ClaseRepository.ocuparCupo`/`liberarCupo` son `UPDATE` atómicos condicionales (`WHERE cuposOcupados < cuposMax`) — evitan la doble ocupación del último cupo sin lock explícito.
- `FinalizarClasesVencidasScheduler` (`@Scheduled(fixedRate=5min)`) pasa automáticamente las clases vencidas a Finalizada — no hace falta ninguna acción manual del instructor para eso.
- Estados con nombre EXACTO (etiqueta con tilde/espacio vía enum + `AttributeConverter`, constante Java en ASCII) — Inscripción: PreInscripción · PagoPendiente · Inscripto · Cancelada. Clase: Programada · Habilitada · Cancelada · Finalizada. Pago: Retenido · Liberado · Cancelado · Efectivo. Denuncia: Pendiente · En Auditoría · Resuelta.
- No confundir Categoría / TipoActividad / NivelIntensidad: **tres cosas distintas, y las tres tienen ABM propio, baja lógica y bloqueo por "en uso"**. Categoría agrupa Tipos, el Tipo dice QUÉ es la actividad y el Nivel dice CUÁNTO esfuerzo pide.
- **Cancelación de clase y pagos:** el reintegro alcanza a los pagos `Retenido` **y** a los `Efectivo`; ambos terminan en `Cancelado` (E2I-HU08 crit. 5). El efectivo no pasa por el gateway y el alumno recibe una notificación que le dice que coordine la devolución con el instructor.
- **Zona horaria:** todo se guarda como `Instant` (UTC), pero cuando hay que pasar a calendario ("qué día de la semana es", "venció hoy") la respuesta depende de la zona **del negocio**, no del servidor: usar `shared/time/Zonas.AR`. La constante estaba duplicada en tres servicios; la última copia que quedaba (`LevantarSuspensionesVencidasService`) también se sacó.

## Tests
Cada slice "andando" = test de Service (regla + caso de error, ej. duplicado/en-uso/no-encontrado/sin-permiso) y test de Controller (status + shape del DTO) en verde. Antes de dar por terminado un cambio, correr la suite completa, no solo el paquete tocado. **Hoy: 743 tests en verde en 175 clases, y ningún caso de uso sin test de Service.**

**`./mvnw test` SÍ necesita la base.** La mayoría de los tests son unitarios con mocks o `@WebMvcTest` y no la tocan, pero los `@SpringBootTest` (`ActivehubApiApplicationTests`, `CatalogoPublicoTest`, `BusquedaSinTildesTest`) levantan el contexto completo contra Supabase — ver la sección "Los tests corren contra la base REAL". Sin conexión, la suite no pasa.

**El `java` del PATH es 17 y el proyecto compila con 21** (`<java.version>21</java.version>`). Correr la suite así:

```
JAVA_HOME="$HOME/.jdks/corretto-21.0.10" ./mvnw test
```

Sin eso, `mvnw` falla con `release version 21 not supported`, y si en `target/` quedaron clases de una corrida con 21, el error que se ve es otro y despista: `UnsupportedClassVersionError … class file version 65.0` desde surefire.

Estado de la deuda de tests (sección cerrada):
- `registrarinstructor` era el slice grande sin **ningún** test pese a ser transaccional y multipart. Ahora tiene 12 de Service (el foco: que la validación de la documentación corra **antes** de tocar la base, para que un archivo inválido no deje una cuenta creada) y 6 de Controller (incluido el multipart sin la parte `documentos` → 400).
- También se cubrieron los otros tres slices mutantes que estaban sin test: `actualizartipoactividad`, `registraradmin` y el scheduler `levantarsuspensionesvencidas`.
- **Concurrencia de cupos:** `InscribirseConcurrenciaTest` lanza 40 pedidos simultáneos contra 5 cupos y verifica que entren exactamente 5, sin inscripciones ni pagos de más. El mock de `ocuparCupo` reproduce la semántica del UPDATE condicional con un `AtomicInteger`, así que **lo que se prueba es el Service** (que no decida el cupo en Java con un read-then-write y que respete el "0 filas afectadas"); la atomicidad de Postgres en sí no se testea sin base levantada, y eso está asumido.
- Se cerró la lista de slices sin test: `listarcategorias`, `listartiposactividad`, `listarmisinscripciones`, `listarpenalizaciones`, `obtenerinstructor`, `obtenermiperfilinstructor`, `obtenerusuarioactual` y `verimagenactividad` ya tienen Service test. Dos que valen la pena mirar como referencia: `ListarPenalizacionesServiceTest` fija el reloj con `Clock.fixed` para probar los bordes de la vigencia (el último día cuenta), y `VerImagenActividadServiceTest` usa un `@TempDir` real en vez de mockear `Files`, porque lo que importa es que la lectura y el Content-Type funcionen juntos.
- **Tests de arquitectura** (`shared/security/GuardasDePermisoTest`): comparan el código contra las migraciones. Prueban invariantes que ningún test funcional puede detectar — que nadie vuelva a guardar por nombre de rol, y que las claves del catálogo y las del código sean el mismo conjunto. Varios slices mutantes siguen teniendo Service pero no Controller test.

## Idioma
Código y nombres de paquete/clase en español donde el dominio lo pida (entidades, casos de uso). Mensajes de usuario en español rioplatense.

## Frontend que consume esta API
`activehub-frontend` (repo hermano, Vite + React 19 + TypeScript) tiene **auth real** (JWT vía `AuthContext.tsx`) y **todo `DataContext.tsx` cableado a esta API**. Los arrays mock de `inscripciones`/`pagos`/`penalizaciones` que estaban documentados como fuera de alcance **ya se borraron**, y los tres dashboards (admin, instructor y alumno) leen datos reales.

Lo único que sigue simulado del lado del cliente, y está acotado: el "directorio" de usuarios de `AuthContext` en `localStorage` (sobrevive porque la API no expone un listado público de personas; las pantallas admin ya usan `listarusuariosadmin`), el catálogo de demostración de `lib/mockData.ts` que lo alimenta, y el "Asistente de beneficios" de `Detalle.tsx` hasta que exista el ítem 11. **Los helpers de formato de fecha/hora de `mockData.ts` no son mock** y se usan en todas las pantallas. Ver `activehub-frontend/CLAUDE.md` para el detalle de qué pantalla usa qué.

Dos cambios de contrato que rompen clientes viejos y conviene tener presentes desde este lado: el login manda **`identificador`** (correo o DNI), no `email`; y `DenunciaAdmin.alumno` puede venir **null** en las denuncias sobre una reseña — el campo para "quién reportó" es `denunciante`.
