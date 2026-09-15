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
        /**
         * La misma fila contada en castellano. La pantalla muestra esto en "Detalle"; el id de
         * la entidad y la metadata cruda siguen viajando aparte para no perder la evidencia.
         * Ver {@link DescripcionAuditoria}.
         */
        String descripcion,
        Instant createdAt
) {
}
