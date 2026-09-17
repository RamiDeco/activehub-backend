package com.activehub.shared.notificacion;

/**
 * A qué pantalla lleva el click sobre una notificación.
 *
 * <p>Es distinto de {@link TipoNotificacion} y de {@code Notificacion.entidadId} a propósito:
 * el tipo elige ícono/estilo y la entidad es el registro que originó el aviso (trazabilidad),
 * mientras que el destino es <b>a dónde se navega</b>. No siempre coinciden — a una nueva
 * inscripción el instructor no quiere ir a la fila de inscripción sino al roster de la clase,
 * y a un horario nuevo de un favorito el alumno no quiere ir a la clase sino a la actividad,
 * que es donde puede anotarse.
 *
 * <p>El id que acompaña al destino es siempre el <b>parámetro de la ruta</b> de la pantalla
 * correspondiente en el frontend, ya resuelto acá (donde el usecase tiene la entidad cargada)
 * y no en el cliente, que tendría que salir a pedir la cadena clase→actividad para saberlo.
 * El mapeo destino→ruta depende del rol y vive en {@code lib/notificaciones.ts}.
 */
public enum DestinoNotificacion {
    /** Detalle de la actividad. Alumno: el catálogo; instructor: su actividad. */
    ACTIVIDAD,
    /** Alumno: su listado de clases, con la fila resaltada. Instructor: el roster de la clase. */
    CLASE,
    /** La inscripción del alumno dentro de "Mis clases". Solo tiene sentido para el alumno. */
    INSCRIPCION,
    /** La reseña, en "Mis reseñas" (alumno) o en la pantalla de reseñas del instructor. */
    RESENIA,
    /** La denuncia, en "Mis denuncias" (alumno). */
    DENUNCIA,
    /** "Mis datos" del instructor: el estado de su solicitud de verificación. */
    PERFIL_INSTRUCTOR,
    /** No hay pantalla a dónde ir: la notificación se muestra, pero no es clickeable. */
    NINGUNO
}
