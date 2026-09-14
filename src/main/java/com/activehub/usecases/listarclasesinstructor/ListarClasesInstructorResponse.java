package com.activehub.usecases.listarclasesinstructor;

import java.time.Instant;
import java.util.UUID;

public record ListarClasesInstructorResponse(
        UUID claseId,
        UUID actividadId,
        String actividadNombre,
        Instant fechaHora,
        Instant horaFin,
        String estado,
        int cuposMax,
        int cuposOcupados
) {
}
