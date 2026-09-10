package com.activehub.usecases.denunciarresenia;

import java.time.Instant;
import java.util.UUID;

public record DenunciarReseniaResponse(UUID id, UUID reseniaId, String estado, Instant createdAt) {
}
