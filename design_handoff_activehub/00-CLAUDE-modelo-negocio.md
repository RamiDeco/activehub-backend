# ActiveHub — modelo de negocio (respetar siempre)

Plataforma web de gestión de actividades deportivas/recreativas/formativas. 3 roles: **Alumno**, **Instructor/Profesor** (usar "Instructor" en UI), **Administrador**.

## Taxonomía (jerarquía)
- **Categoría** agrupa tipos: Bienestar, Aventura, Formación Técnica, Defensa Personal.
- **TipoActividad** pertenece a una Categoría: Relajación, Bienestar, Aventura, Formativa, Técnica, Defensa Personal.
- **Actividad** = disciplina concreta (Running, Gimnasia, Trekking, Senderismo, Defensa Personal, Meditación). Tiene **un TipoActividad** y **un NivelIntensidad**.
- **NivelIntensidad** (esfuerzo físico): Física baja / Física media / Física alta.
- **AgendaClases**: programación recurrente de una actividad (día, hora inicio/fin, edad mín/máx, cupos máx, vigencia). Genera **Clases** concretas.
- **Clase**: instancia concreta de una actividad (fechaHora exacta) con su EstadoClase.

NO confundir Categoría / TipoActividad / NivelIntensidad — son tres cosas distintas.

## Reglas de negocio clave (preinscripción vs inscripción)
- Faltan **>4 días** → el alumno solo puede **PreInscribirse** (registra interés, NO ocupa cupo, NO genera pago, habilita recordatorio).
- Faltan **≤4 días** → **inscripción definitiva** si hay cupos, hasta **1 hora antes** del inicio.
- Pago **Mercado Pago** OK → inscripción queda **Inscripto** (pago Retenido→Liberado).
- Pago **Efectivo** → inscripción **PagoPendiente** hasta que el instructor confirme el cobro manualmente → pasa a Inscripto.
- Solo alumnos **Inscripto** o **PagoPendiente** aparecen en la lista de la clase del instructor.
- Inscripción **Cancelada** no puede volver a estados anteriores. Clase **Cancelada** no admite inscripciones. Clase **Finalizada** habilita historial, reseñas y denuncias.
- Evitar doble ocupación del último cupo.

## Estados (mostrar como badges con el nombre EXACTO)
- **EstadoInscripcion**: PreInscripción · PagoPendiente · Inscripto · Cancelada
- **EstadoClase**: Programada · Habilitada · Cancelada · Finalizada
- **EstadoPago**: Retenido · Liberado · Cancelado · Efectivo
- **EstadoDenuncia**: Pendiente · En Auditoría · Resuelta
- Disponibilidad de cupos (UI): Disponible · Últimos cupos · Sin cupos

## Otras entidades
- **ActividadFavorita** (alumno marca favoritos; notifica al habilitarse inscripción; ≠ inscribirse).
- **Reseña/ReseñaClase**: solo alumnos que estuvieron Inscripto en clase Finalizada; moderable por admin.
- **Denuncia + EstadoDenuncia**: gestionadas por admin; pueden derivar en reintegro/sanción/penalización.
- **Penalización + TipoPenalizacion** (Económica / Suspensión temporal); el usuario acumula `cantidad de penalizaciones`.
- **Rol / Permiso / ConfiguracionRol**: permisos por rol configurables sin tocar código.
- **Baja lógica**: nunca borra; conserva histórico para auditoría/trazabilidad.
- **Auditoría/Trazabilidad**: registrar quién, cuándo y sobre qué entidad en operaciones críticas.

## Implementación de las pantallas (ya hecho, en `activehub-frontend`)
- SPA en Vite + React + TypeScript, 36 pantallas recreadas del prototipo original (`ActiveHub.dc.html` + hijos `StatusBadge`, `ActivityCard`, `AlumnoNav`, `DashSidebar`).
- Identidad: azul marino #0E2A47 (institucional), turquesa #12B5A5/#0FB8A9 (positivo/disponibilidad), naranja #FF6A2B (CTA). Fuentes Space Grotesk (títulos) + Manrope (texto). Español rioplatense, datos realistas (Mendoza).
- Usar SIEMPRE "PreInscripción" (no "Reserva") para el estado de interés >4 días.
- Auth y datos actualmente **mockeados en el front** (`AuthContext`/`DataContext`, localStorage) — esta API es la que los va a reemplazar.

## IA del instructor (flujo de actividades y clases)
- **Mis actividades**: listado de actividades del instructor; puede **crear / modificar / eliminar** actividad (la actividad lleva descripción, categoría, tipo, nivel, precio, cupos, etc.).
- Al entrar a una actividad → **detalle de actividad (instructor)**: muestra **todas las Clases asociadas** + botón **Crear clase** (solo fija un horario/fecha para que la gente se inscriba; hereda datos de la actividad). Desde ahí puede **modificar o eliminar** una clase, y entrar a la gestión de la clase (inscriptos, asistencia, confirmar cobro).
- **Próximas clases**: listado cronológico de las clases que vienen (orden en que se van a dar) para que el instructor vea qué sigue.
- Sidebar instructor: Panel · Mis actividades · Próximas clases · Métricas · Reseñas · Ayuda. "Crear actividad", "detalle de actividad" y "gestión de clase" se alcanzan desde Mis actividades (resaltan ese ítem).
