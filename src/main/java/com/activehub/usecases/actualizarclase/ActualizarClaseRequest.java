package com.activehub.usecases.actualizarclase;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record ActualizarClaseRequest(
        @NotNull(message = "La fecha y hora son obligatorias") Instant fechaHora,
        @NotNull(message = "Los cupos máximos son obligatorios")
        @Min(value = 1, message = "Los cupos máximos deben ser al menos 1") Integer cuposMax
) {
}
