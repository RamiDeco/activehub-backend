package com.activehub.usecases.generarinformeactividad;

/**
 * Lo único que el cliente elige del informe: sobre qué clase de la actividad lo quiere.
 *
 * <p>{@code claseId} es opcional. El detalle de la actividad tiene una clase seleccionada y la manda
 * para que el informe hable de esa fecha y ese horario; si no viene, el service usa la próxima clase
 * vigente. No se recibe ningún dato del alumno por el cuerpo: <b>el perfil sale del token y de la
 * base</b>, nunca del cliente, porque si no cualquiera podría pedir un informe con la condición de
 * salud que se le ocurra inventar.
 */
public record GenerarInformeActividadRequest(String claseId) {
}
