package com.activehub.usecases.actualizarclase;

import java.time.Instant;
import java.util.UUID;

public record ActualizarClaseResponse(UUID id, UUID actividadId, Instant fechaHora, String estado, int cuposMax, int cuposOcupados) {
}
