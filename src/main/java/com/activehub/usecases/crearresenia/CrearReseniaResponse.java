package com.activehub.usecases.crearresenia;

import java.time.Instant;
import java.util.UUID;

public record CrearReseniaResponse(
        UUID id, UUID claseId, UUID alumnoId, int puntaje, String comentario, boolean enModeracion, Instant createdAt
) {
}
