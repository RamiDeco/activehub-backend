package com.activehub.usecases.preinscribirse;

import java.time.Instant;
import java.util.UUID;

public record PreinscribirseResponse(
        UUID id,
        UUID claseId,
        UUID alumnoId,
        String estado,
        Instant createdAt,
        UUID pagoId
) {
}
