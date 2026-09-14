package com.activehub.usecases.listarpenalizaciones;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ListarPenalizacionesResponse(
        UUID id,
        UUID usuarioId,
        String usuarioNombre,
        String usuarioEmail,
        String tipo,
        String motivo,
        BigDecimal monto,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        boolean vigente,
        UUID denunciaId,
        int cantidadPenalizacionesUsuario,
        Instant createdAt
) {
}
