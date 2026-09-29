package com.activehub.usecases.cerrarreportesoporte;

import java.time.Instant;
import java.util.UUID;

public record CerrarReporteSoporteResponse(UUID id, String estado, String respuesta, Instant cerradoAt) {
}
