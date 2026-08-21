package com.activehub.usecases.listarmisnotificaciones;

import java.time.Instant;
import java.util.UUID;

public record ListarMisNotificacionesResponse(
        UUID id,
        String tipo,
        String mensaje,
        UUID entidadId,
        boolean leida,
        Instant createdAt
) {
}
