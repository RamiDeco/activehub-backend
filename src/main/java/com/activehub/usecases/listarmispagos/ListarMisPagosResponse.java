package com.activehub.usecases.listarmispagos;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ListarMisPagosResponse(
        UUID pagoId,
        UUID inscripcionId,
        UUID claseId,
        UUID actividadId,
        String actividadNombre,
        Instant claseFechaHora,
        String claseEstado,
        String inscripcionEstado,
        String estado,
        String metodo,
        BigDecimal monto,
        Instant createdAt
) {
}
