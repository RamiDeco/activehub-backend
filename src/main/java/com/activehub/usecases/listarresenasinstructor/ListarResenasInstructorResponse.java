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
        /** Pendiente de aprobación del admin. NO significa "denunciada" — para eso está `denunciada`. */
        boolean enModeracion,
        String respuestaInstructor,
        Instant respuestaInstructorAt,
        /** El instructor ya la denunció y la denuncia sigue abierta. */
        boolean denunciada,
        boolean oculta,
        Instant createdAt
) {
    public record Alumno(UUID id, String nombre, String apellido) {
    }
}
