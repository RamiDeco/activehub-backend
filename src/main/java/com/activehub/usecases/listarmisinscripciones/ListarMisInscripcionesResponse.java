package com.activehub.usecases.listarmisinscripciones;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ListarMisInscripcionesResponse(
        UUID id,
        UUID claseId,
        Instant claseFechaHora,
        String claseEstado,
        UUID actividadId,
        String actividadNombre,
        UUID alumnoId,
        String estado,
        Instant createdAt,
        UUID pagoId,
        Pago pago
) {
    public record Pago(UUID id, String estado, BigDecimal monto, String metodo) {
    }
}
