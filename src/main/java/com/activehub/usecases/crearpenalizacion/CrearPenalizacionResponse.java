package com.activehub.usecases.crearpenalizacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CrearPenalizacionResponse(
        UUID id,
        UUID usuarioId,
        String tipo,
        String motivo,
        BigDecimal monto,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        int cantidadPenalizacionesUsuario
) {
}
