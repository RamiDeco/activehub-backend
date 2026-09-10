package com.activehub.usecases.listarclasesadmin;

import java.time.Instant;
import java.util.UUID;

public record ListarClasesAdminResponse(
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
