package com.activehub.usecases.inscribirse;

import java.time.Instant;
import java.util.UUID;

public record InscribirseResponse(
        UUID id,
        UUID claseId,
        UUID alumnoId,
        String estado,
        Instant createdAt,
        UUID pagoId
) {
}
