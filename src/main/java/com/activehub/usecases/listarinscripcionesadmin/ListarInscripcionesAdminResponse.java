package com.activehub.usecases.listarinscripcionesadmin;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ListarInscripcionesAdminResponse(
        UUID id,
        UUID claseId,
        UUID actividadId,
        UUID alumnoId,
        String estado,
        Instant createdAt,
        Pago pago
) {
    public record Pago(UUID id, String estado, BigDecimal monto, String metodo) {
    }
}
