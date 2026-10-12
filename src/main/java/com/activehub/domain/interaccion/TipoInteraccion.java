package com.activehub.domain.interaccion;

/**
 * Qué hizo el alumno. Es deliberadamente abierto a crecer (la columna no tiene CHECK), igual
 * que {@code TipoNotificacion}: sumar una señal nueva no deberia costar una migracion.
 *
 * <p>Las dos que hay son las que <b>no</b> quedaban registradas en ninguna otra tabla. Las
 * demas señales del motor ya viven en el modelo: inscripciones, favoritos y reseñas.
 */
public enum TipoInteraccion {

    /** Escribio algo en el buscador. Lleva {@code termino}, no {@code actividadId}. */
    BUSQUEDA,

    /** Abrio el detalle de una actividad. Lleva {@code actividadId}, no {@code termino}. */
    VISTA_ACTIVIDAD
}
