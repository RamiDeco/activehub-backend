package com.activehub.shared.notificacion;

/**
 * Categorías de notificación. El mensaje mostrado al usuario ya viaja armado
 * en {@link Notificacion#getMensaje()} — este enum es solo para que el
 * frontend pueda elegir un ícono/estilo por tipo, no para generar texto.
 *
 * Deliberadamente abierto a crecer (nuevos horarios de actividades favoritas,
 * cancelación de clase, confirmación de operaciones, etc.) sin requerir una
 * migración nueva: la columna en base es un VARCHAR sin CHECK.
 *
 * <p>El booleano de cada valor dice si el aviso <b>además</b> se manda por correo (ver
 * {@link #porCorreo()}).
 */
public enum TipoNotificacion {
    AUSENCIA_PROFESOR(true),
    CLASE_CANCELADA(true),
    NUEVO_HORARIO_FAVORITO(false),

    // Alumno
    INSCRIPCION_CONFIRMADA(false),
    COBRO_EFECTIVO_CONFIRMADO(false),
    INSCRIPCION_CANCELADA(true),
    /**
     * Ya no se emite: la moderacion previa de resenias se retiro (V33). Se conserva porque hay
     * notificaciones guardadas con este tipo y la campana las tiene que seguir mostrando.
     */
    RESENIA_APROBADA(false),
    /** Hoy la emite ocultarresenia: "Un administrador ocultó tu reseña…". */
    RESENIA_RECHAZADA(false),
    DENUNCIA_RESUELTA(true),
    ACTIVIDAD_ELIMINADA(true),
    RESENIA_RESPONDIDA(false),

    // Instructor
    INSTRUCTOR_APROBADO(true),
    INSTRUCTOR_RECHAZADO(true),
    DENUNCIA_RECIBIDA(false),
    INSTRUCTOR_SUSPENDIDO(true),
    PENALIZACION_APLICADA(true),
    DENUNCIA_DESESTIMADA(false),
    NUEVA_INSCRIPCION(false),
    ALUMNO_CANCELO_INSCRIPCION(false),
    NUEVA_RESENIA(false),
    MERCADOPAGO_TOKEN_VENCIDO(true),

    // Administrador
    CAPTURA_PAGO_FALLIDA(true);

    /**
     * Sólo lo importante va por correo: lo que le cambia a alguien la agenda o la plata
     * (clase cancelada, inscripción cancelada con su reintegro, actividad eliminada, ausencia
     * del profesor), el desenlace de una denuncia, una sanción, el estado de su cuenta de
     * instructor y las alertas de cobro. Lo rutinario (una inscripción nueva, una reseña, un
     * horario de un favorito) se queda en la campana: un mail por cada evento termina en la
     * carpeta de spam y arrastra a los importantes con él.
     */
    private final boolean porCorreo;

    TipoNotificacion(boolean porCorreo) {
        this.porCorreo = porCorreo;
    }

    public boolean porCorreo() {
        return porCorreo;
    }
}
