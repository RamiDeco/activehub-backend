package com.activehub.usecases.actualizarclase;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record ActualizarClaseRequest(
        @NotNull(message = "La fecha y hora son obligatorias") Instant fechaHora,
        @NotNull(message = "La hora de fin es obligatoria") Instant horaFin,
        @NotNull(message = "Los cupos máximos son obligatorios")
        @Min(value = 1, message = "El cupo debe ser un número entero mayor a 0") Integer cuposMax
) {
}
