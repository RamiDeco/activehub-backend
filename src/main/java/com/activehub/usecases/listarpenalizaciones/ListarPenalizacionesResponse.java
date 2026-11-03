package com.activehub.usecases.listarpenalizaciones;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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
        Instant createdAt,
        /**
         * V36, solo multas: PENDIENTE_PAGO, EN_REVISION o PAGADA. Null en las multas anteriores a
         * V36 (no se pagan por la plataforma) y en las suspensiones.
         */
        String estadoPago,
        Instant pagoAprobadoAt,
        /** V36, solo suspensiones: si un administrador la levanto antes de tiempo, cuando y por que. */
        Instant levantadaAt,
        String motivoLevantamiento,
        /** V36: los comprobantes enviados para la multa, del mas reciente al mas viejo. */
        List<Comprobante> comprobantes
) {

    public record Comprobante(
            UUID id, String nombreArchivo, String estado, String motivoInvalidez, Instant createdAt, Instant revisadoAt) {
    }
}
