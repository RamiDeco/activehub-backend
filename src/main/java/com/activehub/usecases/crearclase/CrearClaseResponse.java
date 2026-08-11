package com.activehub.usecases.crearclase;

import java.time.Instant;
import java.util.UUID;

public record CrearClaseResponse(UUID id, UUID actividadId, Instant fechaHora, String estado, int cuposMax, int cuposOcupados) {
}
