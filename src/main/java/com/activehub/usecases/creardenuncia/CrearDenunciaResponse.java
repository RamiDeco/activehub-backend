package com.activehub.usecases.creardenuncia;

import java.time.Instant;
import java.util.UUID;

public record CrearDenunciaResponse(
        UUID id, UUID claseId, UUID alumnoId, String motivo, String estado, Instant createdAt
) {
}
