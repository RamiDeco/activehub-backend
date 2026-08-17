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
        boolean enModeracion,
        Instant createdAt
) {
}
