package com.activehub.usecases.crearreportesoporte;

import java.time.Instant;
import java.util.UUID;

public record CrearReporteSoporteResponse(UUID id, String estado, Instant createdAt) {
}
