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
        /**
         * KAN-63: si Mercado Pago ya lo cobro. Un pago Retenido abarca dos momentos para el alumno:
         * reservado en la tarjeta (todavia no se le cobro) y cobrado esperando el periodo de
         * denuncias. Sin este dato la pantalla no puede decirle cual de los dos es.
         */
        boolean capturado,
        String metodo,
        BigDecimal monto,
        Instant createdAt
) {
}
