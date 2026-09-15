package com.activehub.usecases.listarreseniaspublicadas;

import java.time.Instant;
import java.util.UUID;

/**
 * Misma forma que el listado de pendientes mas el estado {@code oculta}: la pantalla necesita
 * distinguir la que sigue visible de la que ya se bajo.
 */
public record ListarReseniasPublicadasResponse(
        UUID id,
        UUID claseId,
        Instant claseFechaHora,
        UUID actividadId,
        String actividadNombre,
        Alumno alumno,
        int puntaje,
        String comentario,
        boolean oculta,
        Instant createdAt
) {
    public record Alumno(UUID id, String nombre, String apellido) {
    }
}
