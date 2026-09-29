package com.activehub.usecases.listarclasesinstructor;

import java.math.BigDecimal;
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
        int cuposOcupados,
        /** Precio congelado de esta clase (V23), no el actual de la actividad. */
        BigDecimal precio
) {
}
