package com.activehub.usecases.listarinscripcionesmisclases;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ListarInscripcionesMisClasesResponse(
        UUID inscripcionId,
        UUID claseId,
        UUID actividadId,
        String actividadNombre,
        Instant claseFechaHora,
        String claseEstado,
        UUID alumnoId,
        String alumnoNombre,
        String estado,
        Instant createdAt,
        String pagoEstado,
        BigDecimal pagoMonto
) {
}
