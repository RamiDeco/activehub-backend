package com.activehub.usecases.listarresenasinstructor;

import java.time.Instant;
import java.util.UUID;

public record ListarResenasInstructorResponse(
        UUID id,
        UUID claseId,
        Instant claseFechaHora,
        UUID actividadId,
        String actividadNombre,
        Alumno alumno,
        int puntaje,
        String comentario,
        boolean enModeracion,
        Instant createdAt
) {
    public record Alumno(UUID id, String nombre, String apellido) {
    }
}
