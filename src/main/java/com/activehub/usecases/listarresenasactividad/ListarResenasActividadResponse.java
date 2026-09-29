package com.activehub.usecases.listarresenasactividad;

import java.time.Instant;
import java.util.UUID;

public record ListarResenasActividadResponse(
        UUID id, UUID claseId, Alumno alumno, int puntaje, String comentario, Instant createdAt,
        /**
         * La respuesta pública del instructor (E2I-HU11 crit. 4). Null si todavía no respondió.
         * Sin esto la respuesta se guardaba y sólo la veía quien la escribió: el detalle de la
         * actividad —el único lugar donde la lee el alumno— no la pedía.
         */
        String respuestaInstructor,
        Instant respuestaInstructorAt
) {
    public record Alumno(UUID id, String nombre, String apellido) {
    }
}
