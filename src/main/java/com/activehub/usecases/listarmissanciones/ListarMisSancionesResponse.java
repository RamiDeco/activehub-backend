package com.activehub.usecases.listarmissanciones;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Lo que ve el instructor en "Pago de penalización" (V36).
 *
 * @param multaPendiente si tiene una multa que lo bloquea (pendiente de pago o en revision).
 * @param transferencia  a donde transferir: sale de la configuracion del servidor; vacio si no
 *                       se cargo, y la pantalla le dice que consulte con la administracion.
 */
public record ListarMisSancionesResponse(
        boolean multaPendiente,
        LocalDate suspendidoHasta,
        Transferencia transferencia,
        List<Sancion> sanciones
) {

    public record Transferencia(String alias, String cbu, String titular) {
    }

    /**
     * @param tipo       "Económica" o "Suspensión temporal".
     * @param estadoPago solo multas desde V36: PENDIENTE_PAGO, EN_REVISION o PAGADA.
     * @param vigente    solo suspensiones: si rige hoy.
     */
    public record Sancion(
            UUID id,
            String tipo,
            String motivo,
            BigDecimal monto,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            Instant createdAt,
            String estadoPago,
            boolean vigente,
            Instant levantadaAt,
            String motivoLevantamiento,
            List<Comprobante> comprobantes
    ) {
    }

    public record Comprobante(UUID id, String nombreArchivo, String estado, String motivoInvalidez, Instant createdAt) {
    }
}
