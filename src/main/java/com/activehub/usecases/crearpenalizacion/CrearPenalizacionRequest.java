package com.activehub.usecases.crearpenalizacion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Los campos condicionales (monto para Economica, fechas para Suspension) se validan en el
 * Service, no aca: dependen del valor de `tipo`, que es una regla de negocio.
 */
public record CrearPenalizacionRequest(
        @NotNull(message = "Elegí a qué usuario penalizar") UUID usuarioId,
        @NotBlank(message = "El tipo de penalización es obligatorio")
        @Pattern(regexp = "Económica|Suspensión temporal", message = "Tipo de penalización inválido") String tipo,
        @NotBlank(message = "El motivo es obligatorio") String motivo,
        BigDecimal monto,
        LocalDate fechaInicio,
        LocalDate fechaFin
) {
}
