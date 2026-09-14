package com.activehub.usecases.crearpenalizacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Una alta puede producir dos penalizaciones (multa + suspensión), así que la respuesta
 * devuelve la lista de lo aplicado y no una sola fila.
 */
public record CrearPenalizacionResponse(
        UUID usuarioId,
        int cantidadPenalizacionesUsuario,
        List<Aplicada> penalizaciones
) {
    public record Aplicada(
            UUID id,
            String tipo,
            String motivo,
            BigDecimal monto,
            LocalDate fechaInicio,
            LocalDate fechaFin
    ) {
    }
}
