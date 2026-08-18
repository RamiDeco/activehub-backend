package com.activehub.usecases.listarauditoria;

import java.time.Instant;
import java.util.UUID;

public record ListarAuditoriaResponse(
        UUID id,
        UUID actorId,
        String actorNombre,
        String actorRol,
        String accion,
        String entidad,
        UUID entidadId,
        String metadata,
        Instant createdAt
) {
}
