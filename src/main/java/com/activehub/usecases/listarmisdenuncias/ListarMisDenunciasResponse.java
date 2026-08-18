package com.activehub.usecases.listarmisdenuncias;

import java.time.Instant;
import java.util.UUID;

public record ListarMisDenunciasResponse(
        UUID id,
        UUID claseId,
        Instant claseFechaHora,
        UUID actividadId,
        String actividadNombre,
        String motivo,
        String estado,
        Instant createdAt
) {
}
