package com.activehub.usecases.listarmisresenas;

import java.time.Instant;
import java.util.UUID;

public record ListarMisResenasResponse(
        UUID id,
        UUID claseId,
        Instant claseFechaHora,
        UUID actividadId,
        String actividadNombre,
        String instructorNombre,
        int puntaje,
        String comentario,
        /**
         * Bajada por un administrador. Sigue apareciendo en la lista del alumno: se le notifica
         * que la ocultaron y el click de esa notificación tiene que llevarlo a algo que exista.
         */
        boolean oculta,
        /** La respuesta del instructor, si contestó: es lo que abre {@code RESENIA_RESPONDIDA}. */
        String respuestaInstructor,
        Instant respuestaInstructorAt,
        Instant createdAt
) {
}
