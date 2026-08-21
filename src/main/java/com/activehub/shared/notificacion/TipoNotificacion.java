package com.activehub.shared.notificacion;

/**
 * Categorías de notificación. El mensaje mostrado al usuario ya viaja armado
 * en {@link Notificacion#getMensaje()} — este enum es solo para que el
 * frontend pueda elegir un ícono/estilo por tipo, no para generar texto.
 *
 * Deliberadamente abierto a crecer (nuevos horarios de actividades favoritas,
 * cancelación de clase, confirmación de operaciones, etc.) sin requerir una
 * migración nueva: la columna en base es un VARCHAR sin CHECK.
 */
public enum TipoNotificacion {
    AUSENCIA_PROFESOR,
    CLASE_CANCELADA,
    NUEVO_HORARIO_FAVORITO
}
